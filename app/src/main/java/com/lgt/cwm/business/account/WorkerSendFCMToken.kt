package com.lgt.cwm.business.account

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.lgt.cwm.business.notification.NotificationHandler
import com.lgt.cwm.util.DebugConfig
import com.lgt.cwm.util.Result
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import grpcCWMPb.CwmRqResAccount

/**
 * Created by giangtpu on 9/14/22.
 */
@HiltWorker
class WorkerSendFCMToken @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val notificationHandler: NotificationHandler,
    private val accountRepository: AccountRepository,
    private val debugConfig: DebugConfig,
) : CoroutineWorker(context, params){
    private val TAG = WorkerSendFCMToken::class.simpleName.toString()

    companion object {
        const val INPUT_FCM_TOKEN = "fcmToken"
    }

    override suspend fun doWork(): Result{
//        debugConfig.log(TAG, "doWork !!!!!!!!!")

        val fcmToken =
            inputData.getString(INPUT_FCM_TOKEN) ?: return Result.failure()

        try{
            val result = accountRepository.updateFCMPushToken(fcmToken)

            when (result){
                is com.lgt.cwm.util.Result.Success<CwmRqResAccount.UpdatePushTokenResponse> -> {
//                    debugConfig.log(TAG, "doWork DONE !!!!!!!!!")
                    return Result.success()
                }
                is com.lgt.cwm.util.Result.Error -> {
                    debugConfig.log(TAG, "WorkerSendFCMToken Failed: ${result.exception.toString()}")
                    return Result.failure()
                }
            }

        }catch (e: Throwable){
            e.printStackTrace()
        }

        debugConfig.log(TAG, "doWork FAILED -> retry !!!!!!!!!")
        return Result.retry()
    }

    //Expedited worker need this - To maintain backwards compatibility for expedited jobs, WorkManager might run a foreground service on platform versions older than Android 12. Foreground services can display a notification to the user
    override suspend fun getForegroundInfo(): ForegroundInfo {
        return ForegroundInfo(
            TAG.hashCode(), notificationHandler.getDefaultWorkerNotification()
        )
    }
}