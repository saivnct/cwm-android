package com.lgt.cwm.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import javax.inject.Qualifier
import javax.inject.Singleton

/**
 * Created by giangtpu on 6/29/22.
 */
@Qualifier
annotation class AppCoroutineScope

@InstallIn(SingletonComponent::class)
@Module
object ScopeModule {

    @AppCoroutineScope
    @Singleton
    @Provides
    fun provideAppCoroutineScope(): CoroutineScope {
        return CoroutineScope(SupervisorJob())
    }
}