package com.lgt.cwm.business.account

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.lgt.cwm.business.notification.NotificationHandler
import com.lgt.cwm.util.DebugConfig
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Created by giangtpu on 15/09/2022.
 */
@HiltWorker
class WorkerLogin  @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val notificationHandler: NotificationHandler,
    private val accountRepository: AccountRepository,
    private val debugConfig: DebugConfig,
) : CoroutineWorker(context, params){
    private val TAG = WorkerLogin::class.simpleName.toString()
    companion object {
        const val INPUT_NEW_NONCE = "newNonce"
    }

    override suspend fun doWork(): Result{
        debugConfig.log(TAG, "doWork !!!!!!!!!")

        val newNonce =
            inputData.getString(INPUT_NEW_NONCE)

        try{
            val account = accountRepository.getActiveAccount()
            account?.let {
                accountRepository.grpcLogin(it, newNonce)
            }
        }catch (e: Throwable){
            e.printStackTrace()
        }

        debugConfig.log(TAG, "doWork DONE !!!!!!!!!")
        return Result.success()
    }

    //Expedited worker need this - To maintain backwards compatibility for expedited jobs, WorkManager might run a foreground service on platform versions older than Android 12. Foreground services can display a notification to the user
    override suspend fun getForegroundInfo(): ForegroundInfo {
        return ForegroundInfo(
            TAG.hashCode(), notificationHandler.getDefaultWorkerNotification()
        )
    }
}