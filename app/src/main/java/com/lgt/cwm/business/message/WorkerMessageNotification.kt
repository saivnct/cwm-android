package com.lgt.cwm.business.message

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.lgt.cwm.models.SignalMsgExt
import com.lgt.cwm.business.notification.NotificationHandler
import com.lgt.cwm.util.DebugConfig
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Created by giangtpu on 12/09/2022.
 */
@HiltWorker
class WorkerMessageNotification @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val notificationHandler: NotificationHandler,
    private val messageRepository: MessageRepository,
    private val debugConfig: DebugConfig,
) : CoroutineWorker(context, params){
    private val TAG = WorkerMessageNotification::class.simpleName.toString()

    companion object {
        const val INPUT_THREAD_ID = "threadId"
        const val INPUT_MSG_IDs = "msgIds"
    }

    override suspend fun doWork(): Result{
        val msgIds =
            inputData.getStringArray(INPUT_MSG_IDs) ?: return Result.failure()
        val msgIdsList = msgIds.asList()

        val threadId = inputData.getString(INPUT_THREAD_ID) ?: return Result.failure()

//        debugConfig.log(TAG, "doWork !!!!!!!!! ${threadId} - ${msgIdsList}")

        val signalThread = messageRepository.findSignalThreadByThreadId(threadId) ?: return Result.failure()

        val signalMsgList = messageRepository.findSignalMsgsByListMsgId(msgIdsList)

        val signalMsgExtList = signalMsgList.map { signalMsg -> SignalMsgExt(signalMsg) }

        notificationHandler.checkAndSendChatNotification(signalThread, signalMsgExtList)

//        debugConfig.log(TAG, "doWork DONE !!!!!!!!!")
        return Result.success()
    }

    //Expedited worker need this - To maintain backwards compatibility for expedited jobs, WorkManager might run a foreground service on platform versions older than Android 12. Foreground services can display a notification to the user
    override suspend fun getForegroundInfo(): ForegroundInfo {
        return ForegroundInfo(
            this.hashCode(), notificationHandler.getDefaultWorkerNotification()
        )
    }
}