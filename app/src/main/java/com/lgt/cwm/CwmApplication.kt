package com.lgt.cwm

import androidx.hilt.work.HiltWorkerFactory
import androidx.multidex.MultiDexApplication
import androidx.work.Configuration
import com.lgt.cwm.business.global.GlobalRepository
import com.lgt.cwm.business.notification.NotificationHandler
import com.lyft.kronos.KronosClock
import com.vanniktech.emoji.EmojiManager
import com.vanniktech.emoji.ios.IosEmojiProvider
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Created by giangtpu on 6/29/22.
 */
@HiltAndroidApp
class CwmApplication : MultiDexApplication(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory
    override fun getWorkManagerConfiguration() =
        Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    @Inject
    lateinit var globalRepository: GlobalRepository

    @Inject
    lateinit var notificationHandler: NotificationHandler

    @Inject
    lateinit var kronosClock: KronosClock


    override fun onCreate() {
        super.onCreate()

        notificationHandler.registerNotificationChannels()

        globalRepository.observeGlobal()

        kronosClock.syncInBackground()

        EmojiManager.install(IosEmojiProvider())

//        Intent(this, WSService::class.java).also { intent ->
//            startService(intent)
//        }
    }


}