package pe.edu.upc.healthify.core.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock

/**
 * Device clock. The patient's "clinical day" is their phone's: use cases and ViewModels
 * compute "today" with this [Clock] (fixed in tests).
 */
@Module
@InstallIn(SingletonComponent::class)
object TimeModule {
    @Provides
    fun provideClock(): Clock = Clock.systemDefaultZone()
}
