package com.lgt.cwm.business.message

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.lgt.cwm.db.MyPreference
import com.lgt.cwm.util.DebugConfig
import com.lgt.cwm.util.Result
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Created by giangtpu on 13/09/2022.
 */
@HiltWorker
class WorkerPeriodicFetchMessage  @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val messageRepository: MessageRepository,
    private val myPreference: MyPreference,
    private val debugConfig: DebugConfig,
) : CoroutineWorker(context, params){
    private val TAG = WorkerPeriodicFetchMessage::class.simpleName.toString()

    override suspend fun doWork(): Result{
//        debugConfig.log(TAG, "doWork !!!!!!!!!")

        try{
            val result : com.lgt.cwm.util.Result<Any>
            if (myPreference.getInitialSyncMsg()){
                result = messageRepository.fetchAllUnreceivedMsg()
            }else{
                result = messageRepository.initialSyncMsg()
            }

            if (result is com.lgt.cwm.util.Result.Error){
                debugConfig.log(TAG,"WorkerPeriodicFetchMessage error ${result.exception.toString()}")
            }

        }catch (e: Throwable){
            e.printStackTrace()
        }

//        debugConfig.log(TAG, "doWork DONE !!!!!!!!!")
        return Result.success()
    }

}