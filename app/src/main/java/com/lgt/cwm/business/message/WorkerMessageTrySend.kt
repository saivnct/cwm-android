package com.lgt.cwm.business.message

import android.content.Context
import android.net.Uri
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.lgt.cwm.business.account.AccountRepository
import com.lgt.cwm.business.notification.NotificationHandler
import com.lgt.cwm.business.ws.WSRepository
import com.lgt.cwm.db.entity.SignalMsgStatus
import com.lgt.cwm.util.*
import com.lyft.kronos.KronosClock
import cwmSignalMsgPb.CwmSignalMsg
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import grpcCWMPb.CwmRqResMsg

/**
 * Created by giangtpu on 9/12/22.
 */
@HiltWorker
class WorkerMessageTrySend @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val notificationHandler: NotificationHandler,
    private val accountRepository: AccountRepository,
    private val messageRepository: MessageRepository,
    private val wsRepository: WSRepository,
    private val kronosClock: KronosClock,
    private val debugConfig: DebugConfig,
) : CoroutineWorker(context, params){
    private val TAG = WorkerMessageTrySend::class.simpleName.toString()

    companion object {
        const val Progress = "Progress"
    }

    override suspend fun doWork(): Result{
//        debugConfig.log(TAG, "doWork !!!!!!!!!")
        setProgress(workDataOf(Progress to 0))

        val account = accountRepository.getActiveAccount()

        if (account == null){
            debugConfig.log(TAG, "Invalid active account")
            setProgress(workDataOf(WorkerMessageDownloadFile.Progress to 100))
            return Result.failure()
        }

        var cannotSend = false
        var continuteLoop = true
        while (continuteLoop) {
            val signalMsg = messageRepository.getFirstSendingMsg()
            if (signalMsg != null){
//                 debugConfig.log(TAG, "checkToSendPendingMsgOfThread - try sendMsg ${signalMsg.msgId}")


                if (signalMsg.imType == CwmSignalMsg.SIGNAL_IM_TYPE.MULTIMEDIA.number){
                    val signalMultimediaMessage = try{
                        CwmSignalMsg.SignalMultimediaMessage.parseFrom(signalMsg.content)
                    }catch (e: Throwable){null}

//                             http://192.168.1.39:9000/media/get/(signalMsg.msgId)/(signalMultimediaMessage.fileId)

                    if (signalMultimediaMessage == null){
                        debugConfig.log(TAG, "checkToSendPendingMsgOfThread - cannot parse signalMultimediaMessage, mark msg send failed ${signalMsg.msgId}")
                        messageRepository.updateMsgStatus(signalMsg.msgId, SignalMsgStatus.SENT_FAIL.code, signalMsg.threadId)
                        continuteLoop = true
                        continue
                    }

                    if ((signalMultimediaMessage.multimediaFileInfosList.firstOrNull { x -> x.fileUri.isNullOrEmpty() }) != null){
                        debugConfig.log(TAG, "checkToSendPendingMsgOfThread - invalid fileUri, mark msg send failed ${signalMsg.msgId}")
                        messageRepository.updateMsgStatus(signalMsg.msgId, SignalMsgStatus.SENT_FAIL.code, signalMsg.threadId)
                        continuteLoop = true
                        continue
                    }

                    var shouldUpdateContent = false
                    val multimediaFileInfosList = mutableListOf<CwmSignalMsg.MultimediaFileInfo>()
                    signalMultimediaMessage.multimediaFileInfosList.forEach { multimediaFileInfo ->
                        if (multimediaFileInfo.fileStatus == CwmSignalMsg.SIGNAL_MEDIA_FILE_STATUS.SENDING){
                            shouldUpdateContent = true

                            //TODO - THIS IS ONLY TEMPORARY SOLUTION: save file to internal storage
                            val multimediaFileInfoStandardlizerBuilder = CwmSignalMsg.MultimediaFileInfo.newBuilder(multimediaFileInfo)
//                            if (multimediaFileInfo.mediaType == CwmSignalMsg.SIGNAL_MEDIA_TYPE.IMAGE || multimediaFileInfo.mediaType == CwmSignalMsg.SIGNAL_MEDIA_TYPE.VIDEO){
//                                val writeToInternalStorageResult = messageRepository.writeGalleryFileToInternal(Uri.parse(multimediaFileInfo.fileUri), multimediaFileInfo.mimeType, multimediaFileInfo.checksum)
//                                when (writeToInternalStorageResult) {
//                                    is com.lgt.cwm.util.Result.Success<Uri> -> {
//                                        debugConfig.log(TAG, "checkToSendPendingMsgOfThread - write multi media msg to local storage success")
//                                        multimediaFileInfoStandardlizerBuilder
//                                            .setFileUri(writeToInternalStorageResult.data.toString())
//                                    }
//                                    is com.lgt.cwm.util.Result.Error -> {
//                                        debugConfig.log(TAG, "checkToSendPendingMsgOfThread - failed to write multi media msg to local storage ${writeToInternalStorageResult.exception}")
//                                        multimediaFileInfoStandardlizerBuilder
//                                            .setFileStatus(CwmSignalMsg.SIGNAL_MEDIA_FILE_STATUS.SEND_FAILED)
//                                    }
//                                }
//                            }
                            val writeToInternalStorageResult = messageRepository.writeGalleryFileToInternal(Uri.parse(multimediaFileInfo.fileUri), multimediaFileInfo.mimeType, multimediaFileInfo.checksum)
                            when (writeToInternalStorageResult) {
                                is com.lgt.cwm.util.Result.Success<Uri> -> {
//                                    debugConfig.log(TAG, "checkToSendPendingMsgOfThread - write multi media msg to local storage success")
                                    multimediaFileInfoStandardlizerBuilder
                                        .setFileUri(writeToInternalStorageResult.data.toString())
                                }
                                is com.lgt.cwm.util.Result.Error -> {
                                    debugConfig.log(TAG, "checkToSendPendingMsgOfThread - failed to write multi media msg to local storage ${writeToInternalStorageResult.exception}")
                                    multimediaFileInfoStandardlizerBuilder
                                        .setFileStatus(CwmSignalMsg.SIGNAL_MEDIA_FILE_STATUS.SEND_FAILED)
                                }
                            }



                            val multimediaFileInfoStandardlizer = multimediaFileInfoStandardlizerBuilder.build()
                            if (multimediaFileInfoStandardlizer.fileStatus == CwmSignalMsg.SIGNAL_MEDIA_FILE_STATUS.SEND_FAILED){
                                multimediaFileInfosList.add(multimediaFileInfoStandardlizer)
                            }
                            else{
                                val uploadResult = messageRepository.uploadMediaMsg(signalMsg.msgId, multimediaFileInfoStandardlizer)

                                val updatedMultimediaFileInfoBuilder = CwmSignalMsg.MultimediaFileInfo.newBuilder(multimediaFileInfoStandardlizer)
                                when (uploadResult) {
                                    is com.lgt.cwm.util.Result.Success<CwmRqResMsg.UploadMediaMsgResponse> -> {
                                        val data = uploadResult.data
                                        updatedMultimediaFileInfoBuilder
                                            .setFileStatus(CwmSignalMsg.SIGNAL_MEDIA_FILE_STATUS.SENT)
                                            .setFileId(data.fileId)
                                            .setFileName(data.fileName)
                                            .setFileSize(data.fileSize)
                                            .setChecksum(data.checkSum)
                                    }
                                    is com.lgt.cwm.util.Result.Error -> {
                                        debugConfig.log(TAG, "checkToSendPendingMsgOfThread - failed to upload multi media msg ${uploadResult.exception}")
                                        updatedMultimediaFileInfoBuilder
                                            .setFileStatus(CwmSignalMsg.SIGNAL_MEDIA_FILE_STATUS.SEND_FAILED)
                                            //no fileId, fileName, fileSize
                                    }
                                }

                                multimediaFileInfosList.add(updatedMultimediaFileInfoBuilder.build())
                            }

                        }
                        else{
                            multimediaFileInfosList.add(multimediaFileInfo)
                        }
                    }

                    var sendFailedCount = 0
                    multimediaFileInfosList.forEach{ multimediaFileInfo ->
                        if (multimediaFileInfo.fileStatus == CwmSignalMsg.SIGNAL_MEDIA_FILE_STATUS.SEND_FAILED){
                            sendFailedCount++
                        }
                    }



                    if (sendFailedCount >= signalMultimediaMessage.multimediaFileInfosList.size){
                        debugConfig.log(TAG, "checkToSendPendingMsgOfThread - all file upload failed, mark msg send failed ${signalMsg.msgId}")
                        if (shouldUpdateContent){
                            val updatedSignalMultimediaMessage = CwmSignalMsg.SignalMultimediaMessage.newBuilder()
                                .addAllMultimediaFileInfos(multimediaFileInfosList)
                                .build()
                            val content = updatedSignalMultimediaMessage.toByteArray()
                            val checksum = content.md5()
                            messageRepository.updateMsgStatusAndContent(signalMsg.msgId, SignalMsgStatus.SENT_FAIL.code, content, checksum, signalMsg.threadId)
                        }else{
                            messageRepository.updateMsgStatus(signalMsg.msgId, SignalMsgStatus.SENT_FAIL.code, signalMsg.threadId)
                        }
                        continuteLoop = true
                        continue
                    }

                    if (shouldUpdateContent){
                        val updatedSignalMultimediaMessage = CwmSignalMsg.SignalMultimediaMessage.newBuilder()
                            .addAllMultimediaFileInfos(multimediaFileInfosList)
                            .build()
                        val content = updatedSignalMultimediaMessage.toByteArray()
                        val checksum = content.md5()
                        messageRepository.updateMsgContent(signalMsg.msgId, content, checksum, signalMsg.threadId)
                        continuteLoop = true
                        continue
                    }

                }


                val cwmRequest = SIPHelper.getCwmSignalMsg(signalMsg, account.sessionId)
                val result : com.lgt.cwm.util.Result<Long>

                if (wsRepository.isConnected()){
//                    debugConfig.log(TAG,"send msg by ws")
                    result = wsRepository.sendMsg(cwmRequest)
                }else{
                    debugConfig.log(TAG,"send msg by grpc")
                    result = messageRepository.sendMsg(cwmRequest)
                }


                when (result) {
                    is com.lgt.cwm.util.Result.Success<Long> -> {
                        val serverDate = result.data
                        //TODO - set MsgDate: += diffWithServerTime
                        messageRepository.updateMsgStatusAndServerDate(signalMsg.msgId, SignalMsgStatus.SENT.code, serverDate, signalMsg.threadId)
                        continuteLoop = true
                    }
                    is com.lgt.cwm.util.Result.Error -> {
                        val now = kronosClock.getCurrentTimeMs()
                        val diffSecond = (now - signalMsg.msgDate) / 1000
                        if (diffSecond > 30){
                            debugConfig.log(TAG, "checkToSendPendingMsgOfThread - mark msg send failed ${signalMsg.msgId}")
                            messageRepository.updateMsgStatus(signalMsg.msgId, SignalMsgStatus.SENT_FAIL.code, signalMsg.threadId)
                            continuteLoop = true
//                            continuteLoop = false   // make this worker stop to start new worker
//                            cannotSend = false
                            continue
                        }

                        continuteLoop = false
                        cannotSend = true
                    }
                }
            }
            else{
//                 debugConfig.log(TAG, "checkToSendPendingMsgOfThread - Not found pending msg")
                continuteLoop = false
            }

        }


        setProgress(workDataOf(WorkerMessageDownloadFile.Progress to 100))

        if (cannotSend){
            debugConfig.log(TAG, "doWork FAILED -> retry later !!!!!!!!!")
            return Result.retry()
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