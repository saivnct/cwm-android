package com.lgt.cwm.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.lgt.cwm.activity.conversation.ConversationActivity
import com.lgt.cwm.util.DebugConfig
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Created by giangtpu on 9/14/22.
 */
@AndroidEntryPoint
class NotificationMessageDismissReceiver: BroadcastReceiver() {
    private val TAG = NotificationMessageDismissReceiver::class.simpleName.toString()

    @Inject
    lateinit var debugConfig: DebugConfig

    override fun onReceive(context: Context?, intent: Intent?) {
        val threadId = intent?.getStringExtra(ConversationActivity.EXTRA_THREAD_ID)
        if (threadId.isNullOrEmpty()){
            return
        }
//        Log.d(TAG, "onReceive: threadId ${threadId}")
        debugConfig.log(TAG, "onReceive!!!!!!!!!!!! threadId ${threadId}")

        //TODO
    }
}