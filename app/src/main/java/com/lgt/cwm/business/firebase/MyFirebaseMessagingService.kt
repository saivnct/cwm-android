package com.lgt.cwm.business.firebase

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.lgt.cwm.business.account.AccountRepository
import com.lgt.cwm.business.message.MessageRepository
import com.lgt.cwm.business.ws.WSRepository
import com.lgt.cwm.db.MyPreference
import com.lgt.cwm.util.DebugConfig
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Created by giangtpu on 6/29/22.
 */
@AndroidEntryPoint
class MyFirebaseMessagingService: FirebaseMessagingService() {
    @Inject
    lateinit var debugConfig: DebugConfig
    @Inject
    lateinit var myPreference: MyPreference
    @Inject
    lateinit var accountRepository: AccountRepository
    @Inject
    lateinit var messageRepository: MessageRepository
    @Inject
    lateinit var wsRepository: WSRepository

//    @Inject
//    @AppCoroutineScope
//    lateinit var appCoroutineScope: CoroutineScope


    val TAG = MyFirebaseMessagingService::class.java.simpleName.toString()

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        debugConfig.log(TAG,"Receive FCM From: ${remoteMessage.from}")

        // Check if message contains a data payload.
        if (remoteMessage.data.isNotEmpty())  {
            debugConfig.log(TAG,"Receive FCM data payload: ${remoteMessage.data}")

//            if ( /* Check if data needs to be processed by long running job */ ) {
//                // For long-running tasks (10 seconds or more) use WorkManager.
//                scheduleJob()
//            } else {
//                // Handle message within 10 seconds
//                handleNow()
//            }
            val cwmMsgDataBase64 = remoteMessage.data["msg"]
            cwmMsgDataBase64?.let {
                wsRepository.startWorkerHandleChatMessage(it, true)
            }
        }

        // Check if message contains a notification payload.
        remoteMessage.notification?.let {
            messageRepository.startWorkerDefaultNotification(it.title?:"cwm", it.body?:"")
        }
    }

    override fun onNewToken(token: String) {
        val dbToken = myPreference.getFCMToken()

        if (dbToken.isNullOrEmpty() || !token.equals(dbToken)){
            debugConfig.log(TAG, "FCM onNewToken: ${token}")
            accountRepository.startWorkerSendFCMToken(token)
        }
    }

}