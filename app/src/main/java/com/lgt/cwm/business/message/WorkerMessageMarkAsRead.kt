package com.lgt.cwm.business.message

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.lgt.cwm.business.account.AccountRepository
import com.lgt.cwm.business.notification.NotificationHandler
import com.lgt.cwm.db.entity.SignalMsgStatus
import com.lgt.cwm.util.DebugConfig
import com.lgt.cwm.util.SignalMsgHelper
import com.lyft.kronos.KronosClock
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Created by giangtpu on 9/15/22.
 */
@HiltWorker
class WorkerMessageMarkAsRead @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val notificationHandler: NotificationHandler,
    private val accountRepository: AccountRepository,
    private val messageRepository: MessageRepository,
    private val kronosClock: KronosClock,
    private val debugConfig: DebugConfig,
) : CoroutineWorker(context, params){
    private val TAG = WorkerMessageMarkAsRead::class.simpleName.toString()

    companion object {
        const val INPUT_THREAD_ID = "threadId"
    }

    override suspend fun doWork(): Result{
        val threadId = inputData.getString(INPUT_THREAD_ID) ?: return Result.failure()

//        debugConfig.log(TAG, "doWork !!!!!!!!! ${threadId}")

        try{
            notificationHandler.clearChatNotidication(threadId)

            val account = accountRepository.getActiveAccount() ?: return Result.failure()
            val signalThread = messageRepository.findSignalThreadByThreadId(threadId) ?: return Result.failure()

            val unreadMsgIds = messageRepository.getAllMsgIdByThreadIdAndStatus(threadId, SignalMsgStatus.RECEIVED_UNREAD.code)
            if (unreadMsgIds.isNotEmpty()){
                val signalMsg = SignalMsgHelper.createSeenStateMessage(
                    from = account,
                    to = signalThread.phoneFull,
                    threadId = signalThread.threadId,
                    threadType = signalThread.threadType,
                    msgIdList = unreadMsgIds,
                    msgDate = kronosClock.getCurrentTimeMs()
                )
                messageRepository.saveSignalMsg(signalMsg)

//                messageRepository.updateListMsgStatus(unreadMsgIds, SignalMsgStatus.RECEIVED_SEEN.code)
                messageRepository.updateListMsgSeenState(
                    msgIds = unreadMsgIds,
                    status = SignalMsgStatus.RECEIVED_SEEN.code,
                    sendSeenState = 1
                )


                messageRepository.updateThreadUnreadMsgs(signalThread.threadId)

                messageRepository.startWorkerMessageTrySend()
            }

        }catch (e : Throwable){
            e.printStackTrace()
        }


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