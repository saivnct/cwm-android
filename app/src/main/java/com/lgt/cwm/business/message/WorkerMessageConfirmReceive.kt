package com.lgt.cwm.business.message

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.lgt.cwm.business.ws.WSRepository
import com.lgt.cwm.business.notification.NotificationHandler
import com.lgt.cwm.util.DebugConfig
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Created by giangtpu on 11/09/2022.
 */
@HiltWorker
class WorkerMessageConfirmReceive @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val notificationHandler: NotificationHandler,
    private val messageRepository: MessageRepository,
    private val wsRepository: WSRepository,
    private val debugConfig: DebugConfig,
) : CoroutineWorker(context, params){
    private val TAG = WorkerMessageConfirmReceive::class.simpleName.toString()

    companion object {
        const val INPUT_MSGID_LIST = "msgIds"
    }

    override suspend fun doWork(): Result {
        val msgIds =
            inputData.getStringArray(INPUT_MSGID_LIST) ?: return Result.failure()

        val msgIdsList = msgIds.asList()
//        debugConfig.log(TAG, "doWork !!!!!!!!! ${msgIdsList}")


        if (msgIdsList.isEmpty()){
//            debugConfig.log(TAG, "empty msgIdsList")
            return Result.success()
        }

        val result : com.lgt.cwm.util.Result<Boolean>
        if (wsRepository.isConnected() && msgIdsList.size < 20){
            result = wsRepository.sendConfirmRecieved(msgIdsList)
        }else{
            result = messageRepository.sendConfirmRecievedListMsgId(msgIdsList)
        }

        when (result) {
            is com.lgt.cwm.util.Result.Success<Boolean> -> {
                messageRepository.setConfirmReceiveMsgs(msgIdsList)
            }
            is com.lgt.cwm.util.Result.Error -> {
                debugConfig.log(TAG, "error when sendConfirmRecieved ${result.exception}")
            }
        }

//        debugConfig.log(TAG, "WorkerMessageConfirmReceive - doWork DONE !!!!!!!!!")
        return Result.success()
    }

    //Expedited worker need this - To maintain backwards compatibility for expedited jobs, WorkManager might run a foreground service on platform versions older than Android 12. Foreground services can display a notification to the user
    override suspend fun getForegroundInfo(): ForegroundInfo {
        return ForegroundInfo(
            TAG.hashCode(), notificationHandler.getDefaultWorkerNotification()
        )
    }
}