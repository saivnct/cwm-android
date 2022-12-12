package com.lgt.cwm.business.message

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
 * Created by giangtpu on 12/09/2022.
 */
@HiltWorker
class WorkerDefaultNotification @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val notificationHandler: NotificationHandler,
    private val debugConfig: DebugConfig,
) : CoroutineWorker(context, params){
    private val TAG = WorkerDefaultNotification::class.simpleName.toString()

    companion object {
        const val INPUT_THREAD_TITLE = "title"
        const val INPUT_MSG_BODY = "body"
    }

    override suspend fun doWork(): Result{
        val title = inputData.getString(INPUT_THREAD_TITLE) ?: return Result.failure()
        val body = inputData.getString(INPUT_MSG_BODY) ?: return Result.failure()

//        debugConfig.log(TAG, "doWork !!!!!!!!!")

        notificationHandler.sendDefaultNotification(title, body)

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