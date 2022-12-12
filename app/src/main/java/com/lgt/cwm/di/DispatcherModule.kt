package com.lgt.cwm.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ServiceComponent
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Qualifier

/**
 * Created by giangtpu on 6/29/22.
 */
@Qualifier
annotation class DefaultDispatcher

@Qualifier
annotation class IODispatcher

@Qualifier
annotation class MainDispatcher

@InstallIn(SingletonComponent::class)
@Module
class DispatcherModule {
    @DefaultDispatcher
    @Provides
    fun provideDefaultDispatcher(): CoroutineDispatcher {
        return Dispatchers.Default
    }

    @IODispatcher
    @Provides
    fun provideIODispatcher(): CoroutineDispatcher {
        return Dispatchers.IO
    }

    @MainDispatcher
    @Provides
    fun provideMainDispatcher(): CoroutineDispatcher {
        return Dispatchers.Main
    }
}