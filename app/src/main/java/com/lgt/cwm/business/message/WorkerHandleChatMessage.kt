package com.lgt.cwm.business.message

import android.content.Context
import android.util.Base64
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.lgt.cwm.business.notification.NotificationHandler
import com.lgt.cwm.util.DebugConfig
import cwmSIPPb.CwmSIP
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Created by giangtpu on 14/09/2022.
 */
@HiltWorker
class WorkerHandleChatMessage @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val notificationHandler: NotificationHandler,
    private val messageRepository: MessageRepository,
    private val debugConfig: DebugConfig,
) : CoroutineWorker(context, params){
    private val TAG = WorkerHandleChatMessage::class.simpleName.toString()


    companion object {
        const val INPUT_CWM_MSG = "cwmMsgDataBase64"
        const val INPUT_SHOULD_FETCH_ALL = "shouldFetchAll"
    }

    override suspend fun doWork(): Result{
//        debugConfig.log(TAG, "doWork !!!!!!!!!")
        val cwmMsgDataBase64 =
            inputData.getString(INPUT_CWM_MSG) ?: return Result.failure()

        val shouldFetchAll =
            inputData.getBoolean(INPUT_SHOULD_FETCH_ALL, false)

        val cwmRequest = try {
            CwmSIP.CWMRequest.parseFrom(Base64.decode(cwmMsgDataBase64, Base64.DEFAULT))
        }catch (e: Throwable) {null} ?: return Result.failure()


        try{
            messageRepository.handleChatMsg(cwmRequest)

//            debugConfig.log(TAG, "shouldFetchAll: ${shouldFetchAll}")
            if (shouldFetchAll){
                messageRepository.startWorkerFetchMessage()
            }

        }catch (e: Throwable){
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