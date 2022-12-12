package com.lgt.cwm.business.contact

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
 * Created by giangtpu on 13/09/2022.
 */
@HiltWorker
class WorkerSyncContact @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val notificationHandler: NotificationHandler,
    private val contactRepository: ContactRepository,
    private val debugConfig: DebugConfig,
) : CoroutineWorker(context, params){
    private val TAG = WorkerSyncContact::class.simpleName.toString()


    override suspend fun doWork(): Result{
//        debugConfig.log(TAG, "doWork !!!!!!!!!")

        try{
            val result = contactRepository.syncContact()
            when (result) {
                is com.lgt.cwm.util.Result.Success<Boolean> -> debugConfig.log(TAG, "syncContact Success")
                is com.lgt.cwm.util.Result.Error -> debugConfig.log(TAG, "syncContact Failed: ${result.exception.toString()}")
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
            TAG.hashCode(), notificationHandler.getDefaultWorkerNotification()
        )
    }
}