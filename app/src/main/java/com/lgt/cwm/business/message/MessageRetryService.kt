//package com.lgt.cwm.business.message
//
//import android.content.Context
//import android.net.Uri
//import com.google.errorprone.annotations.concurrent.GuardedBy
//import com.lgt.cwm.business.account.AccountRepository
//import com.lgt.cwm.business.ws.WSRepository
//import com.lgt.cwm.db.entity.SignalMsgStatus
//import com.lgt.cwm.di.AppCoroutineScope
//import com.lgt.cwm.di.IODispatcher
//import com.lgt.cwm.models.FileMetaData
//import com.lgt.cwm.util.*
//import cwmSignalMsgPb.CwmSignalMsg
//import dagger.hilt.android.qualifiers.ApplicationContext
//import grpcCWMPb.CwmRqResMsg
//import kotlinx.coroutines.CoroutineDispatcher
//import kotlinx.coroutines.CoroutineScope
//import kotlinx.coroutines.launch
//import kotlinx.coroutines.withContext
//import java.util.*
//import javax.inject.Inject
//import javax.inject.Singleton
//
///**
// * Created by giangtpu on 29/07/2022.
// */
//@Singleton
//class MessageRetryService @Inject constructor(
//    @ApplicationContext private val applicationContext: Context,
//    @AppCoroutineScope private val appCoroutineScope: CoroutineScope,
//    @IODispatcher private val ioDispatcher: CoroutineDispatcher,
//    private val accountRepository: AccountRepository,
//    private val messageRepository: MessageRepository,
//    private val wsRepository: WSRepository,
//    private val debugConfig: DebugConfig,
//){
//    private val TAG = MessageRetryService::class.simpleName.toString()
//
//
//
//    private val RETRY_TIMER_PERIOD: Long = 5000 //Milliseconds
//
//    private var timer: Timer? = null
//    private var retryTimerTask: TimerTask? = null
//
//    fun checkTimerTask(shouldStart: Boolean) {
//        if (shouldStart) {
//            startTimer()
//        } else {
//            cancelTimer()
//        }
//    }
//
//    @GuardedBy("retryTimer")
//    @Synchronized
//    private fun cancelTimer() {
//        debugConfig.log(TAG,"cancelTimer")
//
//        if (retryTimerTask != null) {
//            retryTimerTask!!.cancel()
//            retryTimerTask = null
//        }
//        if (timer != null) {
//            timer!!.cancel()
//            timer = null
//        }
//    }
//
//    @GuardedBy("retryTimer")
//    @Synchronized
//    private fun startTimer() {
//        debugConfig.log(TAG,"startTimer")
//
//        if (timer == null) {
//            debugConfig.log(TAG,"start new Timer")
//            if (retryTimerTask != null) {
//                retryTimerTask!!.cancel()
//                retryTimerTask = null
//            }
//            retryTimerTask = object: TimerTask() {
//                override fun run() {
//                    appCoroutineScope.launch(){
//                        checkToSendPendingMsg()
//                    }
//                }
//            }
//            timer = Timer()
//            timer!!.scheduleAtFixedRate(retryTimerTask!!, 0L, RETRY_TIMER_PERIOD)
//        }
//    }
//
//
//     suspend fun checkToSendPendingMsg(){
//         withContext(ioDispatcher){
//             accountRepository.currentActiveAccount?.also { account ->
//
//                 var continuteLoop = true
//                 while (continuteLoop) {
//                     val signalMsg = messageRepository.getFirstSignalMsgByStatus(SignalMsgStatus.SENDING.code)
//                     if (signalMsg != null){
////                         debugConfig.log(TAG, "checkToSendPendingMsgOfThread - try sendMsg ${signalMsg.msgId}")
//                         val now = DateUtil.now().time
//                         val diffSecond = (now - signalMsg.msgDate) / 1000
//                         if (diffSecond > 30){
//                             debugConfig.log(TAG, "checkToSendPendingMsgOfThread - mark msg send failed ${signalMsg.msgId}")
//                             messageRepository.updateMsgStatus(signalMsg.msgId, SignalMsgStatus.SENT_FAIL.code)
//                             continuteLoop = true
//                             continue
//                         }
//
//                         if (signalMsg.imType == CwmSignalMsg.SIGNAL_IM_TYPE.MULTIMEDIA.number){
//                             val signalMultimediaMessage = try{
//                                 CwmSignalMsg.SignalMultimediaMessage.parseFrom(signalMsg.content)
//                             }catch (e: Throwable){null}
//
////                             http://192.168.1.39:9000/media/get/(signalMsg.msgId)/(signalMultimediaMessage.fileId)
//
//                             if (signalMultimediaMessage == null){
//                                 debugConfig.log(TAG, "checkToSendPendingMsgOfThread - cannot parse signalMultimediaMessage, mark msg send failed ${signalMsg.msgId}")
//                                 messageRepository.updateMsgStatus(signalMsg.msgId, SignalMsgStatus.SENT_FAIL.code)
//                                 continuteLoop = true
//                                 continue
//                             }
//
//                             if ((signalMultimediaMessage.multimediaFileInfosList.firstOrNull { x -> x.fileUri.isNullOrEmpty() }) != null){
//                                 debugConfig.log(TAG, "checkToSendPendingMsgOfThread - invalid fileUri, mark msg send failed ${signalMsg.msgId}")
//                                 messageRepository.updateMsgStatus(signalMsg.msgId, SignalMsgStatus.SENT_FAIL.code)
//                                 continuteLoop = true
//                                 continue
//                             }
//
//                             var shouldUpdateContent = false
//                             var sendFailedCount = 0
//
//                             val multimediaFileInfosList = mutableListOf<CwmSignalMsg.MultimediaFileInfo>()
//                             signalMultimediaMessage.multimediaFileInfosList.forEach { multimediaFileInfo ->
//                                 if (multimediaFileInfo.fileStatus == CwmSignalMsg.SIGNAL_MEDIA_FILE_STATUS.SENDING){
//                                     shouldUpdateContent = true
//
//                                     val uploadResult = messageRepository.uploadMediaMsg(signalMsg.msgId, multimediaFileInfo)
//
//                                     when (uploadResult) {
//                                         is Result.Success<CwmRqResMsg.UploadMediaMsgResponse> -> {
//                                             val data = uploadResult.data
//                                             val updatedMultimediaFileInfo = CwmSignalMsg.MultimediaFileInfo.newBuilder(multimediaFileInfo)
//                                                 .setFileStatus(CwmSignalMsg.SIGNAL_MEDIA_FILE_STATUS.SENT)
//                                                 .setFileId(data.fileId)
//                                                 .setFileName(data.fileName)
//                                                 .setFileSize(data.fileSize)
//                                                 .setChecksum(data.checkSum)
//                                                 .build()
//                                             multimediaFileInfosList.add(updatedMultimediaFileInfo)
//                                         }
//                                         is Result.Error -> {
//                                             debugConfig.log(TAG, "checkToSendPendingMsgOfThread - failed to upload multi media msg ${uploadResult.exception}")
//                                             val updatedMultimediaFileInfo = CwmSignalMsg.MultimediaFileInfo.newBuilder(multimediaFileInfo)
//                                                 .setFileStatus(CwmSignalMsg.SIGNAL_MEDIA_FILE_STATUS.SEND_FAILED)
//                                                 //no fileId, fileName, fileSize
//                                                 .build()
//                                             multimediaFileInfosList.add(updatedMultimediaFileInfo)
//                                         }
//                                     }
//                                 }else{
//                                     multimediaFileInfosList.add(multimediaFileInfo)
//                                 }
//
//                                 if (multimediaFileInfo.fileStatus == CwmSignalMsg.SIGNAL_MEDIA_FILE_STATUS.SEND_FAILED){
//                                     sendFailedCount ++
//                                 }
//                             }
//
//
//                             if (sendFailedCount == signalMultimediaMessage.multimediaFileInfosList.size){
//                                 debugConfig.log(TAG, "checkToSendPendingMsgOfThread - all file upload failed, mark msg send failed ${signalMsg.msgId}")
//                                 messageRepository.updateMsgStatus(signalMsg.msgId, SignalMsgStatus.SENT_FAIL.code)
//                                 continuteLoop = true
//                                 continue
//                             }
//
//                             if (shouldUpdateContent){
//                                 val updatedSignalMultimediaMessage = CwmSignalMsg.SignalMultimediaMessage.newBuilder()
//                                     .addAllMultimediaFileInfos(multimediaFileInfosList)
//                                     .build()
//                                 val content = updatedSignalMultimediaMessage.toByteArray()
//                                 val checksum = content.md5()
//                                 messageRepository.updateMsgContent(signalMsg.msgId, content, checksum)
//                                 continuteLoop = true
//                                 continue
//                             }
//
//                         }
//
//
//
//                         val cwmRequest = SIPHelper.getCwmSignalMsg(signalMsg, account.sessionId)
//                         val result = wsRepository.sendMsg(cwmRequest)
//
//                         when (result) {
//                             is Result.Success<Long> -> {
//                                 val serverDate = result.data
//                                 //TODO - set MsgDate: += diffWithServerTime
//                                 messageRepository.updateMsgStatus(signalMsg.msgId, SignalMsgStatus.SENT.code, serverDate)
//                                 continuteLoop = true
//                             }
//                             is Result.Error -> continuteLoop = false
//                         }
//                     }else{
////                         debugConfig.log(TAG, "checkToSendPendingMsgOfThread - Not found pending msg")
//                         continuteLoop = false
////                         cancelTimer()
//                     }
//
//                 }
//
//             }?: run {
//                 debugConfig.log(TAG, "checkToSendPendingMsgOfThread - Not found Active account")
//                 cancelTimer()
//             }
//         }
//     }
//
//}