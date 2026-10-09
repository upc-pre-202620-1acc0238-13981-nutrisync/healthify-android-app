package pe.edu.upc.healthify.core.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import pe.edu.upc.healthify.core.network.auth.EncryptedSessionTokenStore
import pe.edu.upc.healthify.core.network.auth.SessionTokenStore
import javax.inject.Singleton

private val Context.sessionDataStore: DataStore<Preferences> by preferencesDataStore(name = "session")

@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {
    @Provides
    @Singleton
    @SessionDataStore
    fun provideSessionDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        context.sessionDataStore
}

@Module
@InstallIn(SingletonComponent::class)
interface SessionBindingsModule {
    @Binds
    fun bindSessionTokenStore(impl: EncryptedSessionTokenStore): SessionTokenStore
}
