package pe.edu.upc.healthify.core.network.auth

import android.content.Context
import android.util.Base64
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeyTemplates
import com.google.crypto.tink.RegistryConfiguration
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import pe.edu.upc.healthify.core.di.IoDispatcher
import pe.edu.upc.healthify.core.di.SessionDataStore
import java.security.GeneralSecurityException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Tokens cifrados con AES-256-GCM (Tink). El keyset vive en SharedPreferences cifrado por una clave
 * maestra del Android Keystore; DataStore solo guarda el texto cifrado. Nunca se loguean.
 */
@Singleton
class EncryptedSessionTokenStore @Inject constructor(
    @ApplicationContext private val context: Context,
    @SessionDataStore private val dataStore: DataStore<Preferences>,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : SessionTokenStore {

    private val aeadMutex = Mutex()
    private var aead: Aead? = null

    override val hasSession: Flow<Boolean> =
        dataStore.data.map { prefs -> prefs[REFRESH_TOKEN] != null }.distinctUntilChanged()

    override suspend fun accessToken(): String? = read(ACCESS_TOKEN)

    override suspend fun refreshToken(): String? = read(REFRESH_TOKEN)

    override suspend fun save(accessToken: String, refreshToken: String) {
        val cipher = aead()
        val encryptedAccess = cipher.encryptToString(accessToken, ACCESS_TOKEN.name)
        val encryptedRefresh = cipher.encryptToString(refreshToken, REFRESH_TOKEN.name)
        dataStore.edit { prefs ->
            prefs[ACCESS_TOKEN] = encryptedAccess
            prefs[REFRESH_TOKEN] = encryptedRefresh
        }
    }

    override suspend fun clear() {
        dataStore.edit { prefs ->
            prefs.remove(ACCESS_TOKEN)
            prefs.remove(REFRESH_TOKEN)
        }
    }

    private suspend fun read(key: Preferences.Key<String>): String? {
        val stored = dataStore.data.first()[key] ?: return null
        return try {
            aead().decryptFromString(stored, key.name)
        } catch (_: GeneralSecurityException) {
            // Keyset rotado o datos corruptos (p. ej. reinstalación): la sesión ya no es recuperable.
            clear()
            null
        } catch (_: IllegalArgumentException) {
            clear()
            null
        }
    }

    private suspend fun aead(): Aead = aeadMutex.withLock {
        aead ?: withContext(ioDispatcher) { createAead() }.also { aead = it }
    }

    private fun createAead(): Aead {
        AeadConfig.register()
        val keysetHandle = AndroidKeysetManager.Builder()
            .withSharedPref(context, KEYSET_NAME, KEYSET_PREFS)
            .withKeyTemplate(KeyTemplates.get("AES256_GCM"))
            .withMasterKeyUri(MASTER_KEY_URI)
            .build()
            .keysetHandle
        return keysetHandle.getPrimitive(RegistryConfiguration.get(), Aead::class.java)
    }

    private fun Aead.encryptToString(plain: String, associatedData: String): String =
        Base64.encodeToString(encrypt(plain.toByteArray(), associatedData.toByteArray()), Base64.NO_WRAP)

    private fun Aead.decryptFromString(encoded: String, associatedData: String): String =
        String(decrypt(Base64.decode(encoded, Base64.NO_WRAP), associatedData.toByteArray()))

    private companion object {
        val ACCESS_TOKEN = stringPreferencesKey("access_token")
        val REFRESH_TOKEN = stringPreferencesKey("refresh_token")
        const val KEYSET_NAME = "healthify_session_keyset"
        const val KEYSET_PREFS = "healthify_session_keyset_prefs"
        const val MASTER_KEY_URI = "android-keystore://healthify_session_master_key"
    }
}
