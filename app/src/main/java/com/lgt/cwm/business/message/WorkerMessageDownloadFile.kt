package com.lgt.cwm.business.message

import android.content.Context
import android.net.Uri
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.lgt.cwm.business.account.AccountRepository
import com.lgt.cwm.business.notification.NotificationHandler
import com.lgt.cwm.util.DebugConfig
import com.lgt.cwm.util.md5
import cwmSignalMsgPb.CwmSignalMsg
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Created by giangtpu on 9/12/22.
 */
@HiltWorker
class WorkerMessageDownloadFile @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val notificationHandler: NotificationHandler,
    private val accountRepository: AccountRepository,
    private val messageRepository: MessageRepository,
    private val debugConfig: DebugConfig,
) : CoroutineWorker(context, params){
    private val TAG = WorkerMessageDownloadFile::class.simpleName.toString()

    companion object {
        const val INPUT_MSGID = "msgId"
        const val Progress = "Progress"
    }

    override suspend fun doWork(): Result {
//        debugConfig.log(TAG, "doWork !!!!!!!!!")
        setProgress(workDataOf(Progress to 0))

        val account = accountRepository.getActiveAccount()

        if (account == null){
//            debugConfig.log(TAG, "Invalid active account")
            setProgress(workDataOf(Progress to 100))
            return Result.failure()
        }


        val msgId =
            inputData.getString(INPUT_MSGID) ?: return Result.failure()

        if (msgId.isNullOrEmpty()){
            debugConfig.log(TAG, "Invalid input data")
            setProgress(workDataOf(Progress to 100))
            return Result.failure()
        }



        val signalMsg = messageRepository.findSignalMsgByMsgId(msgId)
        if (signalMsg == null || signalMsg.imType != CwmSignalMsg.SIGNAL_IM_TYPE.MULTIMEDIA.number){
            debugConfig.log(TAG, "Invalid signalMsg")
            setProgress(workDataOf(Progress to 100))
            return Result.failure()
        }

        val signalMultimediaMessage = try{
            CwmSignalMsg.SignalMultimediaMessage.parseFrom(signalMsg.content)
        }catch (e: Throwable){null}

        if (signalMultimediaMessage == null){
            debugConfig.log(TAG, "checkToHandleNotDownloadMsg - cannot parse signalMultimediaMessage: ${signalMsg.msgId}")
            messageRepository.setHandledMultiMediaDownloadMsg(signalMsg.msgId)
            setProgress(workDataOf(Progress to 100))
            return Result.failure()
        }

        debugConfig.log(TAG, "checkToHandleNotDownloadMsg - try download files msg ${signalMsg.msgId}")

        val multimediaFileInfosList = mutableListOf<CwmSignalMsg.MultimediaFileInfo>()

        signalMultimediaMessage.multimediaFileInfosList.forEach{ multimediaFileInfo ->
            //TODO - implement set progress
            if (multimediaFileInfo.fileStatus == CwmSignalMsg.SIGNAL_MEDIA_FILE_STATUS.DOWNLOADING){
//                debugConfig.log(TAG, "checkToHandleNotDownloadMsg - try download files msg ${signalMsg.msgId} - fileName ${multimediaFileInfo.fileName}")

                val downloadResult = messageRepository.downloadMediaMsg(signalMsg.msgId, multimediaFileInfo.fileId, multimediaFileInfo.fileName, multimediaFileInfo.checksum)
                when(downloadResult){
                    is com.lgt.cwm.util.Result.Success<Uri> -> {
                        val fileUri = downloadResult.data.toString()
//                        debugConfig.log(TAG, "checkToHandleNotDownloadMsg - try download files msg ${signalMsg.msgId} - fileName ${multimediaFileInfo.fileName} - success - Uri ${fileUri}")

                        val updatedMultimediaFileInfo = CwmSignalMsg.MultimediaFileInfo
                            .newBuilder(multimediaFileInfo)
                            .setFileStatus(CwmSignalMsg.SIGNAL_MEDIA_FILE_STATUS.DOWNLOADED)
                            .setFileUri(fileUri)
                            .build()
                        multimediaFileInfosList.add(updatedMultimediaFileInfo)
                    }
                    is com.lgt.cwm.util.Result.Error -> {
//                        debugConfig.log(TAG, "checkToHandleNotDownloadMsg - try download files msg ${signalMsg.msgId} - fileName ${multimediaFileInfo.fileName} - failed")
                        val updatedMultimediaFileInfo = CwmSignalMsg.MultimediaFileInfo.newBuilder(multimediaFileInfo)
                            .setFileStatus(CwmSignalMsg.SIGNAL_MEDIA_FILE_STATUS.DOWNLOAD_FAILED)
                            //no fileId, fileName, fileSize
                            .build()
                        multimediaFileInfosList.add(updatedMultimediaFileInfo)
                    }
                }

            }else{
                multimediaFileInfosList.add(multimediaFileInfo)
            }

        }

        val updatedSignalMultimediaMessage = CwmSignalMsg.SignalMultimediaMessage.newBuilder()
            .addAllMultimediaFileInfos(multimediaFileInfosList)
            .build()
        val content = updatedSignalMultimediaMessage.toByteArray()
        val checksum = content.md5()
        messageRepository.setHandledMultiMediaDownloadMsg(signalMsg.msgId, content, checksum)

//        debugConfig.log(TAG, "doWork DONE !!!!!!!!!")
        setProgress(workDataOf(Progress to 100))
        return Result.success()
    }

//Expedited worker need this - To maintain backwards compatibility for expedited jobs, WorkManager might run a foreground service on platform versions older than Android 12. Foreground services can display a notification to the user
//    override suspend fun getForegroundInfo(): ForegroundInfo {
//        return ForegroundInfo(
//            this.hashCode(), notificationHandler.getDefaultWorkerNotification()
//        )
//    }


}