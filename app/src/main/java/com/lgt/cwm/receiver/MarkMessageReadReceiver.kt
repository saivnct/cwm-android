package com.lgt.cwm.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.lgt.cwm.activity.conversation.ConversationActivity
import com.lgt.cwm.business.message.MessageRepository
import com.lgt.cwm.di.AppCoroutineScope
import com.lgt.cwm.util.DebugConfig
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import javax.inject.Inject

/**
 * Created by giangtpu on 9/14/22.
 */
@AndroidEntryPoint
class MarkMessageReadReceiver: BroadcastReceiver() {
    private val TAG = MarkMessageReadReceiver::class.simpleName.toString()

    @Inject
    lateinit var debugConfig: DebugConfig

    @Inject
    lateinit var messageRepository: MessageRepository

    @Inject
    @AppCoroutineScope
    lateinit var appCoroutineScope: CoroutineScope

    override fun onReceive(context: Context?, intent: Intent?) {
        val threadId = intent?.getStringExtra(ConversationActivity.EXTRA_THREAD_ID)
        if (threadId.isNullOrEmpty()){
            return
        }
        debugConfig.log(TAG, "onReceive!!!!!!!!!!!! threadId ${threadId}")

        messageRepository.startWorkerMessageMarkAsRead(threadId)
    }

}