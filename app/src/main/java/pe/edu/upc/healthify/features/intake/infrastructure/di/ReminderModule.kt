package pe.edu.upc.healthify.features.intake.infrastructure.di

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
import pe.edu.upc.healthify.features.intake.domain.repository.ReminderScheduler
import pe.edu.upc.healthify.features.intake.domain.repository.ReminderSettingsRepository
import pe.edu.upc.healthify.features.intake.infrastructure.local.DataStoreReminderSettingsRepository
import pe.edu.upc.healthify.features.intake.infrastructure.local.ReminderDataStore
import pe.edu.upc.healthify.features.intake.infrastructure.reminder.WorkManagerReminderScheduler
import javax.inject.Singleton

private val Context.reminderDataStore: DataStore<Preferences> by preferencesDataStore(name = "reminders")

/** PT22 · recordatorios locales. */
@Module
@InstallIn(SingletonComponent::class)
object ReminderProvidesModule {

    @Provides
    @Singleton
    @ReminderDataStore
    fun provideReminderDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        context.reminderDataStore
}

@Module
@InstallIn(SingletonComponent::class)
interface ReminderBindingsModule {

    @Binds
    fun bindReminderSettingsRepository(impl: DataStoreReminderSettingsRepository): ReminderSettingsRepository

    @Binds
    fun bindReminderScheduler(impl: WorkManagerReminderScheduler): ReminderScheduler
}
