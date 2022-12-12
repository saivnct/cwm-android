package com.lgt.cwm.di

import android.content.Context
import com.lgt.cwm.util.DebugConfig
import com.lyft.kronos.AndroidClockFactory
import com.lyft.kronos.KronosClock
import com.lyft.kronos.SyncListener
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Created by giangtpu on 9/19/22.
 */
@InstallIn(SingletonComponent::class)
@Module
object UtilModule {
    @Singleton
    @Provides
    fun provideKronosClock(@ApplicationContext applicationContext: Context, debugConfig: DebugConfig): KronosClock {
        return AndroidClockFactory.createKronosClock(
            context = applicationContext,
            syncListener = object : SyncListener{
                override fun onError(host: String, throwable: Throwable) {
                    debugConfig.log("Kronos","SyncListener - onError host: ${host}, error: ${throwable}")
                }

                override fun onStartSync(host: String) {
                    debugConfig.log("Kronos","SyncListener - onStartSync - host: ${host}")
                }

                override fun onSuccess(ticksDelta: Long, responseTimeMs: Long) {
                    debugConfig.log("Kronos","SyncListener - onSuccess - ticksDelta: ${ticksDelta}, responseTimeMs: ${responseTimeMs}")
                }
            }
        )
    }
}