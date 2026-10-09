package pe.edu.upc.healthify.core.network.auth

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** [SessionTokenStore] en memoria y seguro entre hilos (los usan los hilos de OkHttp en los tests). */
class FakeSessionTokenStore(
    accessToken: String? = null,
    refreshToken: String? = null,
) : SessionTokenStore {

    private val tokens = MutableStateFlow(accessToken to refreshToken)

    @Volatile
    var saveCount = 0
        private set

    override val hasSession: Flow<Boolean> = tokens.map { it.second != null }

    override suspend fun accessToken(): String? = tokens.value.first

    override suspend fun refreshToken(): String? = tokens.value.second

    override suspend fun save(accessToken: String, refreshToken: String) {
        synchronized(this) { saveCount++ }
        tokens.value = accessToken to refreshToken
    }

    override suspend fun clear() {
        tokens.value = null to null
    }

    val currentAccessToken: String? get() = tokens.value.first
    val currentRefreshToken: String? get() = tokens.value.second
}
