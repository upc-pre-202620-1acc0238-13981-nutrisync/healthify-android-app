package pe.edu.upc.healthify.core.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock

/**
 * Reloj del dispositivo. El «día clínico» del paciente es el de su teléfono (F21): los use cases y ViewModels
 * calculan «hoy» con este [Clock] (en tests se fija).
 */
@Module
@InstallIn(SingletonComponent::class)
object TimeModule {
    @Provides
    fun provideClock(): Clock = Clock.systemDefaultZone()
}
