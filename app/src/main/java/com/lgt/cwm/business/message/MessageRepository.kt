package com.lgt.cwm.business.message

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.core.net.toUri
import androidx.work.*
import androidx.work.PeriodicWorkRequest.MIN_PERIODIC_INTERVAL_MILLIS
import com.lgt.cwm.R
import com.lgt.cwm.business.account.AccountRepository
import com.lgt.cwm.business.cwmUser.CWMUserRepository
import com.lgt.cwm.db.MyPreference
import com.lgt.cwm.db.dao.SignalMsgDao
import com.lgt.cwm.db.dao.SignalThreadDao
import com.lgt.cwm.db.entity.*
import com.lgt.cwm.di.AppCoroutineScope
import com.lgt.cwm.di.IODispatcher
import com.lgt.cwm.models.SignalMsgExt
import com.lgt.cwm.models.SignalMsgProcessResult
import com.lgt.cwm.models.SignalTypingMsgExt
import com.lgt.cwm.util.*
import com.lgt.cwm.util.Constants.S3NamePrefxix
import com.lyft.kronos.KronosClock
import cwmSIPPb.CwmSIP
import cwmSignalMsgPb.CwmSignalMsg
import dagger.hilt.android.qualifiers.ApplicationContext
import grpcCWMPb.CwmRqResMsg
import grpcCWMPb.CwmRqResThread
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Created by giangtpu on 09/07/2022.
 */
@Singleton
class MessageRepository @Inject constructor(
    @ApplicationContext private val applicationContext: Context,
    @AppCoroutineScope private val appCoroutineScope: CoroutineScope,
    @IODispatcher private val ioDispatcher: CoroutineDispatcher,
    private val accountRepository: AccountRepository,
    private val cwmUserRepository: CWMUserRepository,
    private val messageGrpcDataSource: MessageGrpcDataSource,
    private val signalMsgDao: SignalMsgDao,
    private val signalThreadDao: SignalThreadDao,
    private val myPreference: MyPreference,
    private val kronosClock: KronosClock,
    private val debugConfig: DebugConfig,
){
    private val TAG = MessageRepository::class.simpleName.toString()

    private val _typingStateFlow = MutableSharedFlow<SignalTypingMsgExt>()
    val typingStateFlow = _typingStateFlow.asSharedFlow()

    val allVerifiedSignalThreadFlow = signalThreadDao.getAllVerifiedThreadFlow()
    fun getThreadByThreadIdFlow(threadId: String) = signalThreadDao.getByThreadIdDistinctUntilChanged(threadId)

    val countAllUnreadMsgFlow = signalMsgDao.countAllUnreadMsgFlow()

    //region Msg

    fun allMsgHaveContentByThreadIdFlow(threadId: String) = signalMsgDao.getAllMsgHaveContentByThreadIdFlow(threadId)
    fun countAllMsgIdByThreadIdAndStatusFlow(threadId: String, status: Int) = signalMsgDao.countAllMsgIdByThreadIdAndStatusFlowDistinctUntilChanged(threadId, status)
    fun allMsgIdByThreadIdAndNotSendSeenStateFlow(threadId: String) = signalMsgDao.getAllMsgIdByThreadIdAndNotSendSeenStateFlowUntilChanged(threadId)
    suspend fun getAllMsgIdByThreadIdAndStatus(threadId: String, status: Int): List<String>{
        return withContext(ioDispatcher) {
            return@withContext signalMsgDao.getAllMsgIdByThreadIdAndStatus(threadId, status)
        }
    }

    suspend fun countAllUnreadMsg(exceptThreadId: String? = null): Long{
        return withContext(ioDispatcher) {
            if (exceptThreadId != null){
                return@withContext signalMsgDao.countAllUnreadMsgExceptThread(exceptThreadId)
            }else{
                return@withContext signalMsgDao.countAllUnreadMsg()
            }
        }
    }

    suspend fun countAllUnreadThread(exceptThreadId: String? = null): Long{
        return withContext(ioDispatcher) {
            if (exceptThreadId != null){
                return@withContext signalMsgDao.countAllUnreadThreadExceptThread(exceptThreadId)
            }else{
                return@withContext signalMsgDao.countAllUnreadThread()
            }
        }
    }


    suspend fun countAllUnreadMsgOfThread(threadId: String): Long{
        return withContext(ioDispatcher) {
            return@withContext signalMsgDao.countAllUnreadMsgOfThreadId(threadId)
        }
    }

    suspend fun findSignalMsgByMsgId(msgId: String) : SignalMsg?{
        return withContext(ioDispatcher) {
            return@withContext signalMsgDao.getByMsgId(msgId)
        }
    }

    suspend fun findSignalMsgsByListMsgId(msgIds: List<String>) : List<SignalMsg>{
        return withContext(ioDispatcher) {
            return@withContext signalMsgDao.getByListMsgId(msgIds)
        }
    }

    suspend fun getLastMsgHaveContentByThreadId(threadId: String) : SignalMsg?{
        return withContext(ioDispatcher) {
            return@withContext signalMsgDao.getLastMsgHaveContentByThreadId(threadId)
        }
    }

    val countAllSendingMsgFlow = signalMsgDao.countAllByStatusFlowDistinctUntilChanged(SignalMsgStatus.SENDING.code)
    suspend fun getFirstSendingMsg(): SignalMsg?{
        return withContext(ioDispatcher) {
            return@withContext signalMsgDao.getFirstByStatus(SignalMsgStatus.SENDING.code)
        }
    }

    val countAllUnhanldedEventMsgFlow = signalMsgDao.countAllUnhanldedEventMsgFlowDistinctUntilChanged()
    suspend fun getFirstUnhanldedEventMsg() : SignalMsg?{
        return withContext(ioDispatcher) {
            return@withContext  signalMsgDao.getFirstUnhanldedEventMsg()
        }
    }
    suspend fun setHandledEventMsg(msgId: String) {
        withContext(ioDispatcher) {
            appCoroutineScope.launch {
                try {
                    signalMsgDao.setHandledEventMsg(msgId)
                }catch (e: Throwable){
                    e.printStackTrace()
                }
            }.join()
        }
    }

    suspend fun setConfirmReceiveMsgs(msgIds: List<String>) {
        withContext(ioDispatcher) {
            appCoroutineScope.launch {
                try {
                    signalMsgDao.setConfirmReceiveMsgs(msgIds)
                }catch (e: Throwable){
                    e.printStackTrace()
                }
            }.join()
        }
    }

    suspend fun setHandledMultiMediaDownloadMsg(msgId: String) {
        withContext(ioDispatcher) {
            appCoroutineScope.launch {
                try {
                    signalMsgDao.setHandledMultiMediaDownloadMsg(msgId)
                }catch (e: Throwable){
                    e.printStackTrace()
                }
            }.join()
        }
    }
    suspend fun setHandledMultiMediaDownloadMsg(msgId: String, content: ByteArray?, checksum: String) {
        withContext(ioDispatcher) {
            appCoroutineScope.launch {
                try {
                    signalMsgDao.setHandledMultiMediaDownloadMsg(msgId, content, checksum)
                }catch (e: Throwable){
                    e.printStackTrace()
                }
            }.join()
        }
    }



    suspend fun saveSignalMsg(signalMsg: SignalMsg) {
        withContext(ioDispatcher) {
            appCoroutineScope.launch {
                try {
                    signalMsgDao.insertSignalMsg(signalMsg)
                }catch (e: Throwable){
                    e.printStackTrace()
                }
            }.join()
        }
    }

    suspend fun updateMsgSeenState(msgId: String, status: Int, seenBy: List<String>){
        withContext(ioDispatcher) {
            appCoroutineScope.launch {
                try {
                    signalMsgDao.updateMsgSeenState(msgId,status,seenBy)
                }catch (e: Throwable){
                    e.printStackTrace()
                }
            }.join()
        }
    }

    suspend fun updateMsgSeenState(msgId: String, status: Int, sendSeenState: Int ){
        withContext(ioDispatcher) {
            appCoroutineScope.launch {
                try {
                    signalMsgDao.updateMsgSeenState(msgId,status,sendSeenState)
                }catch (e: Throwable){
                    e.printStackTrace()
                }
            }.join()
        }
    }

    suspend fun updateListMsgSeenState(msgIds: List<String>, status: Int, sendSeenState: Int){
        withContext(ioDispatcher) {
            appCoroutineScope.launch {
                try {
                    signalMsgDao.updateListMsgSeenState(msgIds,status,sendSeenState)
                }catch (e: Throwable){
                    e.printStackTrace()
                }
            }.join()
        }
    }

    suspend fun updateListMsgSendSeenState(msgIds: List<String>, sendSeenState: Int){
        withContext(ioDispatcher) {
            appCoroutineScope.launch {
                try {
                    signalMsgDao.updateListMsgSendSeenState(msgIds,sendSeenState)
                }catch (e: Throwable){
                    e.printStackTrace()
                }
            }.join()
        }
    }

    suspend fun updateMsgStatus(msgId: String, status: Int, threadId: String) {
        withContext(ioDispatcher) {
            appCoroutineScope.launch {
                try {
                    signalMsgDao.updateMsgStatus(msgId, status)

                    val signalThread = signalThreadDao.getByThreadId(threadId)
                    if (signalThread != null && !signalThread.lastMsgId.isNullOrEmpty() && signalThread.lastMsgId.equals(msgId)){
                        signalThreadDao.updateLastMsgInfo(
                            threadId = signalThread.threadId,
                            lastMsgStatus =  status,
                        )
                    }

                }catch (e: Throwable){
                    e.printStackTrace()
                }
            }.join()
        }
    }
    suspend fun updateMsgStatusAndServerDate(msgId: String, status: Int, serverDate: Long, threadId: String) {
        withContext(ioDispatcher) {
            appCoroutineScope.launch {
                try {
                    signalMsgDao.updateMsgStatusAndServerDate(msgId, status, serverDate)

                    val signalThread = signalThreadDao.getByThreadId(threadId)
                    if (signalThread != null && !signalThread.lastMsgId.isNullOrEmpty() && signalThread.lastMsgId.equals(msgId)){
                        signalThreadDao.updateLastMsgInfo(
                            threadId = signalThread.threadId,
                            lastMsgStatus =  status,
                            lastMsgServerDate = serverDate
                        )
                    }

                }catch (e: Throwable){
                    e.printStackTrace()
                }
            }.join()
        }
    }
    suspend fun updateMsgStatusAndMsgDate(msgId: String, status: Int, msgDate: Long) {
        withContext(ioDispatcher) {
            appCoroutineScope.launch {
                try {
                    signalMsgDao.updateMsgStatusAndMsgDate(msgId, status, msgDate)
                }catch (e: Throwable){
                    e.printStackTrace()
                }
            }.join()
        }
    }
    suspend fun updateMsgStatusAndContent(msgId: String, status: Int, content: ByteArray?, checksum: String, threadId: String) {
        withContext(ioDispatcher) {
            appCoroutineScope.launch {
                try {
                    signalMsgDao.updateMsgStatusAndContent(msgId, status, content, checksum)

                    val signalThread = signalThreadDao.getByThreadId(threadId)
                    if (signalThread != null && !signalThread.lastMsgId.isNullOrEmpty() && signalThread.lastMsgId.equals(msgId)){
                        signalThreadDao.updateLastMsgInfo(
                            threadId = signalThread.threadId,
                            lastMsg = content,
                            lastMsgStatus =  status
                        )
                    }
                }catch (e: Throwable){
                    e.printStackTrace()
                }
            }.join()
        }
    }
    suspend fun updateMsgContent(msgId: String, content: ByteArray?, checksum: String, threadId: String) {
        withContext(ioDispatcher) {
            appCoroutineScope.launch {
                try {
                    signalMsgDao.updateMsgContent(msgId, content, checksum)

                    val signalThread = signalThreadDao.getByThreadId(threadId)
                    if (signalThread != null && !signalThread.lastMsgId.isNullOrEmpty() && signalThread.lastMsgId.equals(msgId)){
                        signalThreadDao.updateLastMsgInfo(
                            threadId = signalThread.threadId,
                            lastMsg = content,
                        )
                    }
                }catch (e: Throwable){
                    e.printStackTrace()
                }
            }.join()
        }
    }
    
    

    suspend fun updateMsgRecieveSeen(threadId: String, serverDate: Long){
        withContext(ioDispatcher) {
            appCoroutineScope.launch {
                try {
                    signalMsgDao.updateMsgRecieveSeen(threadId, serverDate)
                }catch (e: Throwable){
                    e.printStackTrace()
                }
            }.join()
        }
    }

    suspend fun deleteSignalMsgByListMsgId(msgIds: List<String>){
        withContext(ioDispatcher) {
            appCoroutineScope.launch {
                try {
                    signalMsgDao.deleteByMsgIds(msgIds)
                }catch (e: Throwable){
                    e.printStackTrace()
                }
            }.join()
        }
    }

    suspend fun clearSignalMsgByThreadId(threadId: String, toServerDate: Long){
        withContext(ioDispatcher) {
            appCoroutineScope.launch {
                try {
                    debugConfig.log(TAG, "clearSignalMsgByThreadId - ${threadId}")
                    signalMsgDao.clearByThreadId(threadId, toServerDate)
                }catch (e: Throwable){
                    e.printStackTrace()
                }
            }.join()
        }
    }



    suspend fun writeGalleryFileToInternal(galleryUri: Uri, mimetype: String, checksum: String) : Result<Uri>{
        return withContext(ioDispatcher){
            //TODO - THIS IS ONLY TEMPORARY SOLUTION: save file to internal storage
            val contentResolver = applicationContext.getContentResolver()
//            val fileName = galleryUri.getName(contentResolver)

            val mime = MimeTypeMap.getSingleton();
            val extension = mime.getExtensionFromMimeType(mimetype);

            val fileName = "${S3NamePrefxix}${checksum}.${extension}"


            val fileOutputStream = applicationContext.openFileOutput(fileName, Context.MODE_PRIVATE)

            contentResolver.openInputStream(galleryUri).use { inputStream ->

                inputStream?.let {
                    it.copyTo(fileOutputStream)
                }
            }

            val file = File(applicationContext.filesDir, fileName)
            if (file.exists()){
                val uri = file.toUri()
                var fileChecksum = ""
                contentResolver.openInputStream(uri).use { inputStream ->
                    inputStream?.let {
                        fileChecksum = inputStream.md5()
                    }
                }

                if (fileChecksum.equals(checksum)){
                    return@withContext Result.Success(uri)
                }
            }

            return@withContext Result.Error(Exception("Cannot write to internal storage!"))

        }
    }

    //api
    suspend fun uploadMediaMsg(msgId: String, multimediaFileInfo: CwmSignalMsg.MultimediaFileInfo) : Result<CwmRqResMsg.UploadMediaMsgResponse>{
        return withContext(ioDispatcher){
            val acc = accountRepository.getActiveAccount() ?: return@withContext Result.Error(Exception("Invalid Active Acount!"))
            if (!acc.isLogin()){
                debugConfig.log(TAG, "uploadMediaMsg - Not Login Acount")
                return@withContext Result.Error(Exception("Not Login Acount!"))
            }
            try {
//                    debugConfig.log(TAG, "uploadMediaMsg")
                val result = messageGrpcDataSource.uploadMediaMsg(acc, msgId, multimediaFileInfo)
                accountRepository.checkAndHandleSessionExpired(result, acc)
                return@withContext result
            }catch (e: Throwable){
                e.printStackTrace()
                return@withContext Result.Error(e)
            }
        }
    }

    //api
    suspend fun downloadMediaMsg(msgId: String, fileId: String, fileName: String, checksum: String) : Result<Uri>{
        return withContext(ioDispatcher){
            val acc = accountRepository.getActiveAccount()
                ?: return@withContext Result.Error(Exception("Invalid Active Acount!"))

            if (!acc.isLogin()){
                debugConfig.log(TAG, "downloadMediaMsg - Not Login Acount")
                return@withContext Result.Error(Exception("Not Login Acount!"))
            }
            try {
//                    debugConfig.log(TAG, "downloadMediaMsg")
                val result = messageGrpcDataSource.downloadMediaMsg(acc, msgId, fileId, checksum)
                accountRepository.checkAndHandleSessionExpired(result, acc)

                when(result){
                    is Result.Success<ByteArray> -> {
                        val fileOutputStream = applicationContext.openFileOutput(fileName, Context.MODE_PRIVATE)
                        fileOutputStream.write(result.data)

                        val file = File(applicationContext.filesDir, fileName)

                        if (file.exists()){
//                                val uri = FileProvider.getUriForFile(applicationContext, BuildConfig.APPLICATION_ID  + ".provider", file)
                            val uri = file.toUri()
                            return@withContext Result.Success(uri)
                        }
                        return@withContext Result.Error(Exception("Save file error"))
                    }
                    is Result.Error -> { return@withContext result}
                }

            }catch (e: Throwable){
                e.printStackTrace()
                return@withContext Result.Error(e)
            }
        }
    }

    //api
    suspend fun initialSyncMsg() : Result<Boolean>{
        return withContext(ioDispatcher){
            debugConfig.log(TAG, "initialSyncMsgRequest")

            val acc = accountRepository.getActiveAccount() ?: return@withContext Result.Error(Exception("Invalid Active Acount!"))


            if (!acc.isLogin()){
                debugConfig.log(TAG, "initialSyncMsg - Not Login Acount")
                return@withContext Result.Error(Exception("Not Login Acount!"))
            }
            try {
                val result = messageGrpcDataSource.initialSyncMsg(acc)

//                debugConfig.log(TAG, "Done initialSyncMsgRequest")

                if (result is Result.Success<List<CwmRqResMsg.InitialSyncMsgResponse>>){
                    val allConfirmReceiveSignalMsgIdList = handleInitialSyncMsgResponse(result.data)
                    startWorkerConfirmRecieved(allConfirmReceiveSignalMsgIdList)
                    myPreference.setInitialSyncMsg(true)
                }


                debugConfig.log(TAG, "Done handle initialSyncMsgRequest")
                accountRepository.checkAndHandleSessionExpired(result, acc)
                return@withContext Result.Success(true)
            }catch (e: Throwable){
                debugConfig.log(TAG, "initialSyncMsgRequest - Exception")
                e.printStackTrace()
                return@withContext Result.Error(e)
            }

        }
    }

    //api
    suspend fun fetchAllUnreceivedMsg() : Result<Boolean>{
        return withContext(ioDispatcher){
//            debugConfig.log(TAG, "fetchAllUnreceivedMsg")

            val acc = accountRepository.getActiveAccount() ?: return@withContext Result.Error(Exception("Invalid Active Acount!"))

            if (!acc.isLogin()){
                debugConfig.log(TAG, "fetchAllUnreceivedMsg - Not Login Acount")
                return@withContext Result.Error(Exception("Not Login Acount!"))
            }
            try {
                val lastFetchMsgServerDate = myPreference.getLastFetchMsgServerDate()

                val result = messageGrpcDataSource.fetchAllUnreceivedMsg(acc,lastFetchMsgServerDate)

//                debugConfig.log(TAG, "Done fetchAllUnreceivedMsg")
                if (result is Result.Success<List<CwmSIP.CWMRequest>>){
                    val allConfirmReceiveSignalMsgIdList = handleListChatMsg(result.data, true)
                    startWorkerConfirmRecieved(allConfirmReceiveSignalMsgIdList)
                }
//                debugConfig.log(TAG, "Done handleListChatMsg")
                accountRepository.checkAndHandleSessionExpired(result, acc)

                return@withContext Result.Success(true)
            }catch (e: Throwable){
                debugConfig.log(TAG, "fetchAllUnreceivedMsg - Exception")
                e.printStackTrace()
                return@withContext Result.Error(e)
            }

        }
    }

    //api
    suspend fun fetchOldMsgOfThread(threadId: String) : Result<Boolean>{
        return withContext(ioDispatcher){
            val acc = accountRepository.getActiveAccount() ?: return@withContext Result.Error(Exception("Invalid Active Acount!"))

            if (!acc.isLogin()){
                debugConfig.log(TAG, "fetchOldMsgOfThread - Not Login Acount")
                return@withContext Result.Error(Exception("Not Login Acount!"))
            }
            try {
                debugConfig.log(TAG, "fetchOldMsgOfThread ${threadId}")
                val oldestSignalMsgOfThread = signalMsgDao.getOldestMsgByThreadId(threadId)

                val result = messageGrpcDataSource.fetchOldMsgOfThread(acc, threadId, oldestSignalMsgOfThread?.serverDate ?: 0)
                if (result is Result.Success<List<CwmSIP.CWMRequest>>){
                    val allConfirmReceiveSignalMsgIdList = handleListOldChatMsg(result.data)
                    startWorkerConfirmRecieved(allConfirmReceiveSignalMsgIdList)
                }

                accountRepository.checkAndHandleSessionExpired(result, acc)

                return@withContext Result.Success(true)
            }catch (e: Throwable){
                return@withContext Result.Error(e)
            }
        }
    }

    //api
    suspend fun deleteMsgsOfThread(threadId: String, msgIds: List<String>, deleteForAllMembers: Boolean) : Result<Boolean>{
        return withContext(ioDispatcher){
            val acc = accountRepository.getActiveAccount()
                ?: return@withContext Result.Error(Exception("Invalid Active Acount!"))

            if (!acc.isLogin()){
                return@withContext Result.Error(Exception("Not Login Acount!"))
            }
            try {
                debugConfig.log(TAG, "deleteMsgsOfThread")
                val result = messageGrpcDataSource.deleteMsgsOfThread(acc, threadId, msgIds, deleteForAllMembers)
                accountRepository.checkAndHandleSessionExpired(result, acc)

                if (result is Result.Error){
                    if (deleteForAllMembers){
                        return@withContext result
                    }
                }

                deleteSignalMsgByListMsgId(msgIds)

                val signalThread = findSignalThreadByThreadId(threadId)
                signalThread?.lastMsgId?.let {
                    if (msgIds.contains(it)){
                        debugConfig.log(TAG, "deleteMsgsOfThread - update last msg of thread")
                        updateThreadLastMsg(threadId)
                    }
                }



                return@withContext Result.Success(true)
            }catch (e: Throwable){
                return@withContext Result.Error(e)
            }

        }
    }

    //api
    suspend fun clearAllMsgOfThread(threadId: String, deleteForAllMembers: Boolean) : Result<Boolean>{
        return withContext(ioDispatcher){
            val acc = accountRepository.getActiveAccount()
                ?: return@withContext Result.Error(Exception("Invalid Active Acount!"))

            if (!acc.isLogin()){
                return@withContext Result.Error(Exception("Not Login Acount!"))
            }
            try {
                debugConfig.log(TAG, "clearAllMsgOfThread")
                val result = messageGrpcDataSource.clearAllMsgOfThread(acc,threadId,deleteForAllMembers)

                accountRepository.checkAndHandleSessionExpired(result, acc)

                if (result is Result.Error){
                    if (deleteForAllMembers){
                        return@withContext result
                    }
                }

                val now = kronosClock.getCurrentTimeMs()
                clearSignalMsgByThreadId(threadId, now)
                updateThreadLastMsg(threadId)

                return@withContext Result.Success(true)
            }catch (e: Throwable){
                return@withContext Result.Error(e)
            }
        }
    }

    //api
    suspend fun deleteThread(threadId: String, isGroup: Boolean, deleteForAllMembers: Boolean) : Result<Boolean>{
        return withContext(ioDispatcher){
            val acc = accountRepository.getActiveAccount()
                ?: return@withContext Result.Error(Exception("Invalid Active Acount!"))

            if (!acc.isLogin()){
                return@withContext Result.Error(Exception("Not Login Acount!"))
            }
            try {
                debugConfig.log(TAG, "deleteThread")
                val result : Result<Any>
                if (isGroup) {
                    result = messageGrpcDataSource.deleteAndLeaveGroupThread(acc,threadId)
                    accountRepository.checkAndHandleSessionExpired(result, acc)
                }else{
                    result = messageGrpcDataSource.deleteSoloThread(acc,threadId,deleteForAllMembers)
                    accountRepository.checkAndHandleSessionExpired(result, acc)
                    if (result is Result.Error){
                        if (deleteForAllMembers){
                            return@withContext result
                        }
                    }
                }

                delSignalThread(threadId, null)

                return@withContext Result.Success(true)
            }catch (e: Throwable){
                return@withContext Result.Error(e)
            }
        }
    }

    //api
    suspend fun sendConfirmRecievedListMsgId(msgIds: List<String>) : Result<Boolean>{
        return withContext(ioDispatcher){
            val acc = accountRepository.getActiveAccount() ?: return@withContext Result.Error(Exception("Invalid Active Acount!"))

            if (!acc.isLogin()){
                debugConfig.log(TAG, "sendConfirmRecievedListMsgId - Not Login Acount")
                return@withContext Result.Error(Exception("Not Login Acount!"))
            }
            try {
//                debugConfig.log(TAG, "sendConfirmRecievedListMsgId")
                val result = messageGrpcDataSource.confirmReceivedMsgs(acc, msgIds)
                accountRepository.checkAndHandleSessionExpired(result, acc)

                return@withContext Result.Success(true)
            }catch (e: Throwable){
                return@withContext Result.Error(e)
            }
        }
    }

    //api
    suspend fun sendMsg(cwmRequest: CwmSIP.CWMRequest) : Result<Long>{
        return withContext(ioDispatcher){
            val acc = accountRepository.getActiveAccount() ?: return@withContext Result.Error(Exception("Invalid Active Acount!"))

            if (!acc.isLogin()){
                debugConfig.log(TAG, "sendMsg - Not Login Acount")
                return@withContext Result.Error(Exception("Not Login Acount!"))
            }
            try {
//                debugConfig.log(TAG, "sendMsg")
                val result = messageGrpcDataSource.sendMsg(acc, cwmRequest)
                accountRepository.checkAndHandleSessionExpired(result, acc)

                when (result){
                    is Result.Success -> {
                        val response = result.data.msgResponse
                        val status = response.code
                        val message = response.message
                        val serverDate = response.content.toByteArray().toLong()

                        if (status == SIPCode.OK.code) {
                            return@withContext Result.Success(serverDate)
                        }else{
                            return@withContext Result.Error(Exception(message))
                        }
                    }
                    is Result.Error -> {
                        return@withContext result
                    }
                }

            }catch (e: Throwable){
                return@withContext Result.Error(e)
            }
        }
    }



    //endregion

    //region worker
    fun startWorkerConfirmRecieved(msgIds: List<String>){
        if (msgIds.isEmpty()){
            return
        }

//        debugConfig.log(TAG,"startWorkerConfirmRecieved")

        val request = OneTimeWorkRequestBuilder<WorkerMessageConfirmReceive>()
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .setInputData(workDataOf(WorkerMessageConfirmReceive.INPUT_MSGID_LIST to msgIds.toTypedArray()))
            .addTag("WorkerMessageConfirmReceive")
            .build()

        WorkManager.getInstance(applicationContext)
            .enqueue(request)
    }

    fun startWorkerMessageDownloadFileService(msgId: String){
        if (msgId.isNullOrEmpty()){
            return
        }

        debugConfig.log(TAG,"startWorkerMessageDownloadFileService")

        val request = OneTimeWorkRequestBuilder<WorkerMessageDownloadFile>()
//            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .setInputData(workDataOf(WorkerMessageDownloadFile.INPUT_MSGID to msgId))
            .addTag("WorkerMessageDownloadFile")
            .build()

        WorkManager.getInstance(applicationContext)
            .enqueueUniqueWork(
                "WorkerMessageDownloadFile-${msgId}",
                ExistingWorkPolicy.KEEP,
                request
            )
    }

    fun startWorkerMessageEventHanlde(){
        debugConfig.log(TAG,"startWorkerMessageEventHanlde")

        val request = OneTimeWorkRequestBuilder<WorkerMessageEventHanlde>()
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .addTag("WorkerMessageEventHanlde")
            .build()

        WorkManager.getInstance(applicationContext)
            .enqueueUniqueWork(
                "WorkerMessageEventHanlde",
                ExistingWorkPolicy.KEEP,
                request
            )
    }

    fun startWorkerMessageTrySend(){
        debugConfig.log(TAG,"startWorkerMessageTrySend")

        val request = OneTimeWorkRequestBuilder<WorkerMessageTrySend>()
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .addTag("WorkerMessageTrySend")
            .setBackoffCriteria(
                BackoffPolicy.LINEAR,
                OneTimeWorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(applicationContext)
            .enqueueUniqueWork(
                "WorkerMessageTrySend",
                ExistingWorkPolicy.KEEP,
                request
            )
    }

    fun startWorkerMessageMarkAsRead(threadId: String){
        if (threadId.isNullOrEmpty()){
            return
        }
        debugConfig.log(TAG,"startWorkerMessageMarkAsRead")

        val request = OneTimeWorkRequestBuilder<WorkerMessageMarkAsRead>()
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .setInputData(workDataOf(WorkerMessageMarkAsRead.INPUT_THREAD_ID to threadId))
            .addTag("WorkerMessageMarkAsRead")
            .build()

        WorkManager.getInstance(applicationContext)
            .enqueue(request)
    }

    fun startWorkerFetchMessage(){
        debugConfig.log(TAG,"startWorkerFetchMessage")
        val request = OneTimeWorkRequestBuilder<WorkerFetchMessage>()
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .addTag("WorkerFetchMessage")
            .build()

        WorkManager.getInstance(applicationContext)
            .enqueueUniqueWork(
                "WorkerFetchMessage",
                ExistingWorkPolicy.KEEP,
                request
            )
    }

    fun startPeriodicWorkerFetchMessage(){
        debugConfig.log(TAG,"startWorkerPeriodicFetchMessage")

        val request =
            PeriodicWorkRequestBuilder<WorkerPeriodicFetchMessage>(MIN_PERIODIC_INTERVAL_MILLIS, TimeUnit.MILLISECONDS)
                .setConstraints(Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
                )
                .addTag("WorkerPeriodicFetchMessage")
                .build()


        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            "WorkerPeriodicFetchMessage",
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    fun startWorkerMessageNotification(threadId: String, msgIds: List<String>){
        val request = OneTimeWorkRequestBuilder<WorkerMessageNotification>()
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .setInputData(
                workDataOf(
                    WorkerMessageNotification.INPUT_THREAD_ID to threadId,
                    WorkerMessageNotification.INPUT_MSG_IDs to msgIds.toTypedArray()
                )
            )
            .addTag("WorkerMessageNotification")
            .build()

        WorkManager.getInstance(applicationContext)
            .enqueueUniqueWork(
                "WorkerMessageNotification",
                ExistingWorkPolicy.APPEND_OR_REPLACE,
                request
            )
    }

    fun startWorkerDefaultNotification(title: String,body: String){
        val request = OneTimeWorkRequestBuilder<WorkerDefaultNotification>()
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .setInputData(
                workDataOf(
                    WorkerDefaultNotification.INPUT_THREAD_TITLE to title,
                    WorkerDefaultNotification.INPUT_MSG_BODY to body
                )
            )
            .addTag("WorkerDefaultNotification")
            .build()

        WorkManager.getInstance(applicationContext)
            .enqueueUniqueWork(
                "WorkerDefaultNotification",
                ExistingWorkPolicy.APPEND_OR_REPLACE,
                request
            )
    }

    //endregion

    //region old logic handle Msg
    //    val cwmRequestChannel = Channel<CwmSIP.CWMRequest>()
//
//    suspend fun onWSChatMsg(cwmRequest: CwmSIP.CWMRequest){
//        withContext(ioDispatcher) {
//            cwmRequestChannel.send(cwmRequest)
//        }
//    }
//
////    suspend fun testProduceChatMsg(){
////        withContext(ioDispatcher){
////            var x = 0;
////            while (true){
////                x++
////                debugConfig.log(TAG, "produce ${x}")
////                cwmRequestChannel.send(x)
////            }
////        }
////    }
//
//    suspend fun handleChannelChatMsg(){
//        withContext(ioDispatcher){
//            for (cwmRequest in cwmRequestChannel) {
//
//            }
//        }
//    }
    //endregion

    //region handle Msg

    suspend fun handleInitialSyncMsgResponse(initialSyncMsgResponseList: List<CwmRqResMsg.InitialSyncMsgResponse>): List<String>{
        val allConfirmReceiveSignalMsgIdList = arrayListOf<String>()
        for (initialSyncMsgResponse in initialSyncMsgResponseList){
            val groupThreadInfo = initialSyncMsgResponse.groupThreadInfo
            val cwmRequestList = initialSyncMsgResponse.msgList
            if (!cwmRequestList.isEmpty()){
                val signalMsgIdList = handleListChatMsg(cwmRequestList, false)
                allConfirmReceiveSignalMsgIdList.addAll(signalMsgIdList)
            }else{
                if (groupThreadInfo.threadType == CwmSignalMsg.SIGNAL_THREAD_TYPE.GROUP){
                    val signalThread = findGroupSignalThreadInfo(groupThreadInfo.threadId, null)
                    signalThread?.let {
                        saveSignalThread(it)
                    }
                }
            }
        }

        return allConfirmReceiveSignalMsgIdList
    }

    suspend fun handleListOldChatMsg(cwmRequestList: List<CwmSIP.CWMRequest>): List<String>{

        val allConfirmReceiveSignalMsgIdList = arrayListOf<String>()

        try{
            for (cwmRequest in cwmRequestList) {
                val verifyChatMsgResultPair = verifyChatMsg(cwmRequest)

                val signalMessageProto = verifyChatMsgResultPair.first
                if (signalMessageProto != null){
                    val signalThread = findSignalThreadByThreadId(signalMessageProto.threadId) ?: continue

                    val processChatMsgResultPair = processChatMsg(cwmRequest, signalMessageProto, signalThread)
                    processChatMsgResultPair.second?.let { msgId ->
                        allConfirmReceiveSignalMsgIdList.add(msgId)
                    }
                }else{
                    verifyChatMsgResultPair.second?.let{ msgId ->
                        allConfirmReceiveSignalMsgIdList.add(msgId)
                    }
                }
            }
        }catch (e: Throwable){
            e.printStackTrace()
        }

        return allConfirmReceiveSignalMsgIdList
    }

    suspend fun handleListChatMsg(cwmRequestList: List<CwmSIP.CWMRequest>, shouldShowNotification: Boolean): List<String>{
        val threadMap = hashMapOf<String, SignalThread>()   // threadId - SignalThread
        val threadMsgMap = hashMapOf<String, ArrayList<SignalMsg>>()    // threadId - List<SignalMsg>
        val allConfirmReceiveSignalMsgIdList = arrayListOf<String>()

        try {
            var latestServerDate: Long = 0
            for (cwmRequest in cwmRequestList) {
                val verifyChatMsgResultPair = verifyChatMsg(cwmRequest)
                val signalMessageProto = verifyChatMsgResultPair.first
                if (signalMessageProto != null){
                    if (signalMessageProto.serverDate > latestServerDate && signalMessageProto.imType != CwmSignalMsg.SIGNAL_IM_TYPE.TYPING){
                        latestServerDate = signalMessageProto.serverDate
                    }

                    var signalThread = threadMap.get(signalMessageProto.threadId)
                    if (signalThread == null){
                        signalThread = findSignalThreadByThreadId(signalMessageProto.threadId)
                    }

                    val processChatMsgResultPair = processChatMsg(cwmRequest, signalMessageProto, signalThread)
                    processChatMsgResultPair.second?.let { msgId ->
                        allConfirmReceiveSignalMsgIdList.add(msgId)
                    }
                    val signalMsgProcessResult =  processChatMsgResultPair.first
                    signalMsgProcessResult?.let {
                        threadMap.put(it.signalThread.threadId, it.signalThread)
                        var signalMsgList = threadMsgMap.get(it.signalThread.threadId)
                        if (signalMsgList == null){
                            signalMsgList = arrayListOf<SignalMsg>()
                            threadMsgMap.put(it.signalThread.threadId, signalMsgList)
                        }
                        if (it.signalMsg != null){
                            signalMsgList.add(it.signalMsg)
                        }
                    }

                }else{
                    verifyChatMsgResultPair.second?.let{ msgId ->
                        allConfirmReceiveSignalMsgIdList.add(msgId)
                    }
                }
            }

            val lastFetchMsgServerDate = myPreference.getLastFetchMsgServerDate()
            if (latestServerDate > lastFetchMsgServerDate){
                myPreference.setLastFetchMsgServerDate(latestServerDate)
            }



            threadMap.forEach { entry ->
//            debugConfig.log(TAG,"save thread ${entry.value.threadName}")
                saveSignalThread(entry.value)
            }


            if (shouldShowNotification){
                threadMsgMap.forEach{ entry ->
                    val signalThread = threadMap.get(entry.key)
                    signalThread?.let{
//                debugConfig.log(TAG, "thread ${it.threadName} - total msg ${entry.value.size}")
                        showNotification(it, entry.value)
                    }
                }
            }
        }catch (e: Throwable){
            e.printStackTrace()
        }



        return allConfirmReceiveSignalMsgIdList
    }

    suspend fun handleChatMsg(cwmRequest: CwmSIP.CWMRequest ){
        val verifyChatMsgResultPair = verifyChatMsg(cwmRequest)
        val signalMessageProto = verifyChatMsgResultPair.first
        if (signalMessageProto != null){
            val signalThread = findSignalThreadByThreadId(signalMessageProto.threadId)

            val processChatMsgResultPair = processChatMsg(cwmRequest, signalMessageProto,signalThread)
            val signalMsgProcessResult =  processChatMsgResultPair.first
            signalMsgProcessResult?.let {
                saveSignalThread(it.signalThread)
                val signalMsgList = arrayListOf<SignalMsg>()
                if (it.signalMsg != null){
                    signalMsgList.add(it.signalMsg)
                }
                showNotification(it.signalThread, signalMsgList)
            }

            processChatMsgResultPair.second?.let { msgId ->
                startWorkerConfirmRecieved(arrayListOf(msgId))
            }
        }else{
            verifyChatMsgResultPair.second?.let{ msgId ->
                startWorkerConfirmRecieved(arrayListOf(msgId))
            }
        }
    }

    suspend fun verifyChatMsg(cwmRequest: CwmSIP.CWMRequest): Pair<CwmSignalMsg.SignalMessage?, String?>{
        val currentAcc = accountRepository.getActiveAccount() ?: return Pair(null, null)

        val signalMessageProto = try{
            CwmSignalMsg.SignalMessage.parseFrom(cwmRequest.content)
        }catch (e: Throwable){null} ?: return Pair(null, null)

        var confirmReceiveMsgId : String? = signalMessageProto.msgId
        if (signalMessageProto.imType == CwmSignalMsg.SIGNAL_IM_TYPE.TYPING) {
            confirmReceiveMsgId = null
        }




        if (cwmRequest.header.method != CwmSIP.REQUEST_METHOD.METHOD_MESSAGE){
//                debugConfig.log(TAG, "verifyChatMsg - Invalid REQUEST_METHOD")
            //send confirm received, because server will query and send this message again
            return Pair(null, confirmReceiveMsgId)
        }

        if (cwmRequest.header.from.equals(currentAcc.phoneFull) && cwmRequest.header.fromSession.equals(currentAcc.sessionId)){
//                debugConfig.log(TAG, "verifyChatMsg - signalMessage from me, not process")
            return Pair(null, null)
        }


        if (!signalMessageProto.data.toByteArray().md5().equals(signalMessageProto.checksum) ){
//                debugConfig.log(TAG, "verifyChatMsg - Invalid signalMessage checksum")
            //send confirm received, because server will query and send this message again
            return Pair(null, confirmReceiveMsgId)
        }

        if (signalMessageProto.threadType == CwmSignalMsg.SIGNAL_THREAD_TYPE.SOLO){
            if (!cwmRequest.header.from.equals(currentAcc.phoneFull) && !cwmRequest.header.to.equals(currentAcc.phoneFull)){
//                    debugConfig.log(TAG,"verifyChatMsg - Invalid msg source/destination !!!")
                //send confirm received, because server will query and send this message again
                return Pair(null, confirmReceiveMsgId)
            }

            val threadId = StringUtil.getThreadId(cwmRequest.header.from, cwmRequest.header.to)
            if (!signalMessageProto.threadId.equals(threadId)){
//                    debugConfig.log(TAG,"verifyChatMsg - Invalid threadId !!!")
                //send confirm received, because server will query and send this message again
                return Pair(null, confirmReceiveMsgId)
            }

            if (cwmRequest.header.from.equals(currentAcc.phoneFull) &&
                cwmRequest.header.fromSessionsWhiteListCount > 0 &&
                !cwmRequest.header.fromSessionsWhiteListList.contains(currentAcc.sessionId)
            ){
//                    debugConfig.log(TAG,"processChatMsg - Msg from me, but not include this session in msg whitelist !!!")
                //send confirm received, because server will query and send this message again
                return Pair(null, confirmReceiveMsgId)
            }

            if (cwmRequest.header.to.equals(currentAcc.phoneFull) &&
                cwmRequest.header.toSessionsWhiteListCount > 0 &&
                !cwmRequest.header.toSessionsWhiteListList.contains(currentAcc.sessionId)
            ){
//                    debugConfig.log(TAG,"processChatMsg - Msg to me, but not include this session in msg whitelist !!!")
                //send confirm received, because server will query and send this message again
                return Pair(null, confirmReceiveMsgId)
            }


        }
        else if (signalMessageProto.threadType == CwmSignalMsg.SIGNAL_THREAD_TYPE.GROUP){
            // TODO - should check for valid group msg ?
        }


        return Pair(signalMessageProto, confirmReceiveMsgId)
    }

    suspend fun processChatMsg(cwmRequest: CwmSIP.CWMRequest,
                               signalMessageProto: CwmSignalMsg.SignalMessage,
                               signalThread: SignalThread?): Pair<SignalMsgProcessResult?, String?>{
        val currentAcc = accountRepository.getActiveAccount() ?: return Pair(null, null)

        var fromName = cwmRequest.header.fromFirstName
        if (!cwmRequest.header.fromLastName.isNullOrEmpty()){
            fromName += " ${cwmRequest.header.fromLastName}"
        }


        var resultSignalThread = signalThread
        if (resultSignalThread == null){
            val hiddenThread = (cwmRequest.header.from.equals(Constants.ServerName.ServerEventName) && signalMessageProto.threadType == CwmSignalMsg.SIGNAL_THREAD_TYPE.SOLO) //Msg from  server

            var threadName = ""
            var threadPhoneFull = ""
            var verifiedThread = false
            var admin = false
            val participants = mutableSetOf<String>()
            if (signalMessageProto.threadType == CwmSignalMsg.SIGNAL_THREAD_TYPE.SOLO){

                if (cwmRequest.header.from.equals(currentAcc.phoneFull)){
                    threadPhoneFull = cwmRequest.header.to
                    threadName = cwmRequest.header.to
                }else{
                    threadPhoneFull = cwmRequest.header.from
                    threadName = fromName
                }

                if (threadPhoneFull.equals(currentAcc.phoneFull)){
                    threadName = applicationContext.getString(R.string.thread_Me)
                }


                verifiedThread = true
                admin = true
                participants.add(cwmRequest.header.from)
                participants.add(cwmRequest.header.to)

            }
            else if (signalMessageProto.threadType == CwmSignalMsg.SIGNAL_THREAD_TYPE.GROUP){
                threadName = ""
            }

            resultSignalThread = SignalThread(
                threadId = signalMessageProto.threadId,
                threadName = threadName,
                phoneFull = threadPhoneFull,
                hidden = hiddenThread,
                active = true,
                verified = verifiedThread,
                threadType = signalMessageProto.threadType.number,
                participants = participants.toList(),
                admin = admin,
                createdAt = kronosClock.getCurrentTimeMs(),
            )
        }
        else {
            if (signalMessageProto.threadType == CwmSignalMsg.SIGNAL_THREAD_TYPE.SOLO &&
                !cwmRequest.header.from.equals(currentAcc.phoneFull) &&
                !resultSignalThread.threadName.equals(fromName)){

                resultSignalThread.threadName = fromName
            }
        }

        when (signalMessageProto.imType) {
            CwmSignalMsg.SIGNAL_IM_TYPE.TYPING -> {
                handleTypingMsg(currentAcc, resultSignalThread, cwmRequest, signalMessageProto)
                return Pair(null, null)
            }
            CwmSignalMsg.SIGNAL_IM_TYPE.SEENSTATE -> {
                return Pair(handleSeenStateMsg(currentAcc, resultSignalThread, cwmRequest, signalMessageProto), signalMessageProto.msgId)
            }
            else -> {
                return Pair(handleIMMsg(currentAcc, resultSignalThread, cwmRequest, signalMessageProto), signalMessageProto.msgId)
            }
        }
    }

    suspend fun handleTypingMsg(currentAcc: Account, signalThread: SignalThread, cwmRequest: CwmSIP.CWMRequest, signalMessageProto: CwmSignalMsg.SignalMessage){

        if (currentAcc.phoneFull.equals(cwmRequest.header.from)){
            debugConfig.log(TAG, "Typing msg from me!")
            return
        }

        val signalTypingMessageProto = try{
            CwmSignalMsg.SignalTypingMessage.parseFrom(signalMessageProto.data)
        }catch (e: Throwable){null} ?: return


        _typingStateFlow.emit(
            SignalTypingMsgExt(
                threadId = signalThread.threadId,
                from = cwmRequest.header.from,
                fromFirstName = cwmRequest.header.fromFirstName,
                fromLastName = cwmRequest.header.fromLastName,
                fromUserName = cwmRequest.header.fromUserName,
                typingType = signalTypingMessageProto.type,
            )
        )
    }


    suspend fun handleSeenStateMsg(currentAcc: Account, signalThread: SignalThread, cwmRequest: CwmSIP.CWMRequest, signalMessageProto: CwmSignalMsg.SignalMessage): SignalMsgProcessResult?{
//        debugConfig.log(TAG, "handleSeenStateMsg")
        val signalSeenStateMessageProto = try{
            CwmSignalMsg.SignalSeenStateMessage.parseFrom(signalMessageProto.data)
        }catch (e: Throwable){null} ?: return null

        val msgIdList = signalSeenStateMessageProto.msgIdList
        for (msgId in msgIdList){
            try {
                val signalMsg = findSignalMsgByMsgId(msgId)
                if (signalMsg != null &&
                    (signalMsg.direction == SignalMsgDirection.OUTGOING.code || signalMsg.direction == SignalMsgDirection.OUTGOING_DIFF_SESSION.code) &&
                    signalMsg.status != SignalMsgStatus.SENT_SEEN_ALL.code){
                    when (signalSeenStateMessageProto.seenStateType) {
                        //only support Seen status
//                        CwmSignalMsg.SIGNAL_SEENSTATE_MSG_TYPE.DELIVER -> {
//                        }
                        CwmSignalMsg.SIGNAL_SEENSTATE_MSG_TYPE.SEEN -> {
                            when (signalMsg.threadType) {
                                CwmSignalMsg.SIGNAL_THREAD_TYPE.SOLO.number -> {
                                    signalMsg.status = SignalMsgStatus.SENT_SEEN_ALL.code
                                }
                                CwmSignalMsg.SIGNAL_THREAD_TYPE.GROUP.number -> {
                                    if (!signalMsg.seenBy.contains(cwmRequest.header.from)){
                                        signalMsg.seenBy.add(cwmRequest.header.from)
                                    }

                                    if (signalMsg.seenBy.size == signalThread.participants.size - 1) {
                                        signalMsg.status = SignalMsgStatus.SENT_SEEN_ALL.code
                                    }else{
                                        signalMsg.status = SignalMsgStatus.SENT_SEEN.code
                                    }
                                }
                            }
                            updateMsgSeenState(
                                msgId = signalMsg.msgId,
                                status = signalMsg.status,
                                seenBy = signalMsg.seenBy,
                            )
                        }
                        else -> {}
                    }
                }
                else if (signalMsg != null && signalMsg.direction == SignalMsgDirection.INCOMING.code &&
                    signalMsg.status == SignalMsgStatus.RECEIVED_UNREAD.code){
                    when (signalSeenStateMessageProto.seenStateType) {
                        //only support Seen status
//                        CwmSignalMsg.SIGNAL_SEENSTATE_MSG_TYPE.DELIVER -> {
//                        }
                        CwmSignalMsg.SIGNAL_SEENSTATE_MSG_TYPE.SEEN -> {
                            when (signalMsg.threadType) {
                                CwmSignalMsg.SIGNAL_THREAD_TYPE.SOLO.number -> {
                                    signalMsg.status = SignalMsgStatus.RECEIVED_SEEN.code
                                    signalMsg.sendSeenState = true
                                }
                                CwmSignalMsg.SIGNAL_THREAD_TYPE.GROUP.number -> {
                                    if (cwmRequest.header.from.equals(currentAcc.phoneFull)){
                                        signalMsg.status = SignalMsgStatus.RECEIVED_SEEN.code
                                        signalMsg.sendSeenState = true
                                    }
                                }
                            }
                            updateMsgSeenState(
                                msgId = signalMsg.msgId,
                                status = signalMsg.status,
                                sendSeenState = if (signalMsg.sendSeenState) 1 else 0
                            )
                        }
                        else -> {}
                    }
                }


                if (signalMsg != null && !signalThread.lastMsgId.isNullOrEmpty() && signalThread.lastMsgId.equals(msgId)){
                    signalThread.lastMsgStatus = signalMsg.status
                }

            }catch (e: Throwable){
                e.printStackTrace()
            }
        }

        val newUnreadMsgs = signalMsgDao.countAllUnreadMsgOfThreadId(signalThread.threadId)
        debugConfig.log(TAG,"handleSeenStateMsg - unreadMsgs of ${signalThread.threadId} = ${newUnreadMsgs}")
        if (signalThread.unreadMsgs != newUnreadMsgs){
            signalThread.unreadMsgs =  newUnreadMsgs
//            signalThread.lastModified = kronosClock.getCurrentTimeMs()
        }

        return SignalMsgProcessResult(signalThread,null)
    }

    suspend fun handleIMMsg(currentAcc: Account, signalThread: SignalThread, cwmRequest: CwmSIP.CWMRequest, signalMessageProto: CwmSignalMsg.SignalMessage): SignalMsgProcessResult?{
//        debugConfig.log(TAG, "handleIMMsg - msgId ${signalMessageProto.msgId} - threadId ${signalMessageProto.threadId}")
        try {
            var signalMsg = findSignalMsgByMsgId(signalMessageProto.msgId)
            if (signalMsg == null){
                signalMsg = SignalMsg(
                    from = cwmRequest.header.from,
                    fromFirstName = cwmRequest.header.fromFirstName,
                    fromLastName = cwmRequest.header.fromLastName,
                    fromUserName = cwmRequest.header.fromUserName,
                    to = cwmRequest.header.to,
                    msgId = signalMessageProto.msgId,
                    threadId = signalMessageProto.threadId,
                    threadType = signalMessageProto.threadType.number,
                    replyMsgId = signalMessageProto.replyMsgId,
                    imType = signalMessageProto.imType.number,
                    msgDate = signalMessageProto.serverDate,
                    confirmReceive = false,
                    sendSeenState = true,
                    eventHandled = true,
                    multiMediaDownloadHandled = true,
                    serverDate = signalMessageProto.serverDate,
                    checksum = signalMessageProto.checksum,
                    content = signalMessageProto.data.toByteArray(),
                    threadVerified = signalThread.verified
                )
            }
            else {  //incase edit message -> must diff checksum && accept only IM Type
                if (
                    !signalMsg.checksum.equals(signalMessageProto.checksum) &&
                    signalMsg.imType > CwmSignalMsg.SIGNAL_IM_TYPE.EVENT.number
                 ){
                    signalMsg.imType = signalMessageProto.imType.number
                    signalMsg.content = signalMessageProto.data.toByteArray()
                    signalMsg.checksum = signalMessageProto.checksum
                    signalMsg.confirmReceive = false
                }else{  //receive the same msg -> not process more
                    return null
                }
            }



            if (cwmRequest.header.from.equals(currentAcc.phoneFull)){
                if (cwmRequest.header.fromSession.equals(currentAcc.sessionId)){
                    signalMsg.direction = SignalMsgDirection.OUTGOING.code
                }else{
                    signalMsg.direction = SignalMsgDirection.OUTGOING_DIFF_SESSION.code
                }

                if (
                    signalMsg.status != SignalMsgStatus.SENT_SEEN.code &&
                    signalMsg.status != SignalMsgStatus.SENT_SEEN_ALL.code
                ){
                    for (seenby in signalMessageProto.seenbyList){
                        if (!signalMsg.seenBy.contains(seenby)){
                            signalMsg.seenBy.add(seenby)
                        }
                    }

                    if (signalMsg.seenBy.size == signalThread.participants.size - 1) {
                        signalMsg.status = SignalMsgStatus.SENT_SEEN_ALL.code
                    }else if (signalMsg.seenBy.size > 0){
                        signalMsg.status = SignalMsgStatus.SENT_SEEN.code
                    }else{
                        signalMsg.status = SignalMsgStatus.SENT.code
                    }
                }
            }
            else{
                signalMsg.direction = SignalMsgDirection.INCOMING.code

                if (signalMsg.imType == CwmSignalMsg.SIGNAL_IM_TYPE.GROUP_THREAD_NOTIFICATION.number ||
                    signalMsg.imType == CwmSignalMsg.SIGNAL_IM_TYPE.EVENT.number) {
                    signalMsg.status = SignalMsgStatus.RECEIVED_SEEN.code

                }else if (signalMsg.status != SignalMsgStatus.RECEIVED_SEEN.code){
                    if (signalMessageProto.seenbyList.contains(currentAcc.phoneFull)){
                        signalMsg.status = SignalMsgStatus.RECEIVED_SEEN.code
                    }else{
                        signalMsg.sendSeenState = false
                        signalMsg.status = SignalMsgStatus.RECEIVED_UNREAD.code
                    }
                }
            }



            if (signalMsg.imType == CwmSignalMsg.SIGNAL_IM_TYPE.EVENT.number){
                signalMsg.eventHandled = false
            }
            else if (signalMsg.imType == CwmSignalMsg.SIGNAL_IM_TYPE.MULTIMEDIA.number){
                signalMsg.multiMediaDownloadHandled = false
                val signalMultimediaMessage = try {
                    CwmSignalMsg.SignalMultimediaMessage.parseFrom(signalMessageProto.data)
                }catch (e: Throwable){
                    null
                } ?: return null

                val multimediaFileInfosList = mutableListOf<CwmSignalMsg.MultimediaFileInfo>()
                signalMultimediaMessage.multimediaFileInfosList.forEach { multimediaFileInfo ->
                    if (multimediaFileInfo.fileStatus == CwmSignalMsg.SIGNAL_MEDIA_FILE_STATUS.SENT){
                        //change file status when receiving
                        val updatedMultimediaFileInfo = CwmSignalMsg.MultimediaFileInfo.newBuilder(multimediaFileInfo)
                            .setFileUri("")
                            .setFileStatus(CwmSignalMsg.SIGNAL_MEDIA_FILE_STATUS.DOWNLOADING)
                            .build()
                        multimediaFileInfosList.add(updatedMultimediaFileInfo)
                    }
                }

                val updatedSignalMultimediaMessage = CwmSignalMsg.SignalMultimediaMessage.newBuilder()
                    .addAllMultimediaFileInfos(multimediaFileInfosList)
                    .build()
                val content = updatedSignalMultimediaMessage.toByteArray()
                val checksum = content.md5()
                signalMsg.content = content
                signalMsg.checksum = checksum

            }

            saveSignalMsg(signalMsg)

            if (signalMsg.imType == CwmSignalMsg.SIGNAL_IM_TYPE.EVENT.number){
                startWorkerMessageEventHanlde()
            }else if (signalMsg.imType == CwmSignalMsg.SIGNAL_IM_TYPE.MULTIMEDIA.number){
                startWorkerMessageDownloadFileService(signalMsg.msgId)
            }


            var resultSignalThread = signalThread


            if (!signalThread.verified || signalMsg.imType == CwmSignalMsg.SIGNAL_IM_TYPE.GROUP_THREAD_NOTIFICATION.number){    //Unknow GROUP Thread
                var signalGroupThreadNotificationMessageProto : CwmSignalMsg.SignalGroupThreadNotificationMessage? = null
                if (signalMsg.imType == CwmSignalMsg.SIGNAL_IM_TYPE.GROUP_THREAD_NOTIFICATION.number){
                    try {
                        signalGroupThreadNotificationMessageProto = CwmSignalMsg.SignalGroupThreadNotificationMessage.parseFrom(signalMessageProto.data)
                    }catch (e: Throwable){
                        e.printStackTrace()
                    }
                }

                val groupSignalThreadInfo = findGroupSignalThreadInfo(signalThread.threadId, signalGroupThreadNotificationMessageProto)
                groupSignalThreadInfo?.let {
                    resultSignalThread = it
                }
            }


            resultSignalThread.unreadMsgs =  signalMsgDao.countAllUnreadMsgOfThreadId(resultSignalThread.threadId)
            if (
                signalMsg.imType != CwmSignalMsg.SIGNAL_IM_TYPE.EVENT.number &&
                (resultSignalThread.lastMsgServerDate == null || resultSignalThread.lastMsgServerDate!! < signalMsg.serverDate)
            ){
                val now = kronosClock.getCurrentTimeMs()

                resultSignalThread.lastMsgId = signalMsg.msgId
                resultSignalThread.lastMsg = signalMsg.content
                resultSignalThread.lastMsgImType = signalMsg.imType
                resultSignalThread.lastMsgStatus = signalMsg.status
                resultSignalThread.lastMsgDate = signalMsg.msgDate
                resultSignalThread.lastMsgServerDate = signalMsg.serverDate
                resultSignalThread.lastModified = now
            }


            return SignalMsgProcessResult(resultSignalThread,signalMsg)
        }catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    suspend fun showNotification(signalThread: SignalThread, signalMsgList: List<SignalMsg>){
        try {
//            debugConfig.log(TAG,"showNotification ${signalThread.threadName}")
            if ( ! signalThread.verified || !signalThread.active || signalThread.hidden){
                return
            }
            val currentAcc = accountRepository.getActiveAccount() ?: return

            val msgIdList = arrayListOf<String>()
            for (signalMsg in signalMsgList){
                if (signalMsg.from.equals(currentAcc.phoneFull)){
                    msgIdList.clear()
                }else{
                    if (signalMsg.imType != CwmSignalMsg.SIGNAL_IM_TYPE.EVENT.number &&
                        signalMsg.status == SignalMsgStatus.RECEIVED_UNREAD.code
                    ){
                        msgIdList.add(signalMsg.msgId)
                    }
                }
            }

            if (msgIdList.isNotEmpty() || signalThread.unreadMsgs == 0L) {
                startWorkerMessageNotification(signalThread.threadId, msgIdList)
            }
        }catch (e: Throwable){
            e.printStackTrace()
        }
    }
    //endregion

    //region Thread
    suspend fun findSignalThreadByThreadId(threadId: String) : SignalThread?{
        return withContext(ioDispatcher) {
            return@withContext signalThreadDao.getByThreadId(threadId)
        }
    }

    suspend fun findOrCreateSoloSignalThread(contact: Contact) : SignalThread?{
        return withContext(ioDispatcher) {
            val currentAcc = accountRepository.getActiveAccount() ?: return@withContext null

            val now = kronosClock.getCurrentTimeMs()
            val threadId = StringUtil.getThreadId(currentAcc.phoneFull, contact.standardizedPhoneNumber)
            var signalThread = findSignalThreadByThreadId(threadId)

            signalThread?.also { signalThread ->
                return@withContext signalThread
            }?: run{
                signalThread = SignalThread(
                    threadId = threadId,
                    threadName = contact.name,
                    phoneFull = contact.standardizedPhoneNumber,
                    active = true,
                    verified = true,
                    hidden = false,
                    threadType = CwmSignalMsg.SIGNAL_THREAD_TYPE.SOLO.number,
                    participants = arrayListOf(currentAcc.phoneFull, contact.standardizedPhoneNumber),
                    admin = true,
                    createdAt = now,
                )
                saveSignalThread(signalThread!!)
                return@withContext signalThread
            }

        }
    }



    //api
    suspend fun findGroupSignalThreadInfo(threadId: String, signalGroupThreadNotificationMessageProto: CwmSignalMsg.SignalGroupThreadNotificationMessage?): SignalThread?{
        return withContext(ioDispatcher) {
            val account = accountRepository.getActiveAccount() ?: return@withContext null

            if (!account.isLogin()){
                return@withContext null
            }

            val result = messageGrpcDataSource.checkGroupThreadInfo(account, threadId)
            accountRepository.checkAndHandleSessionExpired(result, account)
            when (result) {
                is Result.Success<CwmRqResThread.CheckGroupThreadInfoResponse> -> {
//                        debugConfig.log(TAG, "checkGroupThreadInfo Success: ${result.data.groupThreadInfo.groupName} - ${result.data.groupThreadInfo.threadId}")

                    var signalThread = findSignalThreadByThreadId(threadId)
                    val admin = result.data.groupThreadInfo.adminsList.contains(account.phoneFull)
                    val now = kronosClock.getCurrentTimeMs()

                    if (result.data.groupThreadInfo.participantInfosList.isNotEmpty()){
                        cwmUserRepository.handleParticipantInfosList(result.data.groupThreadInfo.participantInfosList,account)
                    }

                    if (result.data.groupThreadInfo.participantsList.contains(account.phoneFull)){
                        if (signalThread == null){
                            signalThread = SignalThread(
                                threadId = result.data.groupThreadInfo.threadId,
                                threadName = result.data.groupThreadInfo.groupName,
                                phoneFull = "",
                                active = true,
                                verified = true,
                                hidden = false,
                                threadType = CwmSignalMsg.SIGNAL_THREAD_TYPE.GROUP.number,
                                creator =  result.data.groupThreadInfo.creator,
                                participants = result.data.groupThreadInfo.participantsList,
                                admin = admin,
                                admins = result.data.groupThreadInfo.adminsList,
                                createdAt = now,
                                lastModified = now,
                                lastServerModified = result.data.groupThreadInfo.lastModified
                            )
                        }
                        else if (signalThread.lastServerModified < result.data.groupThreadInfo.lastModified){
                            signalThread.threadName = result.data.groupThreadInfo.groupName
                            signalThread.active = true
                            signalThread.verified = true
                            signalThread.participants = result.data.groupThreadInfo.participantsList
                            signalThread.admin = admin
                            signalThread.admins = result.data.groupThreadInfo.adminsList
                            signalThread.creator = result.data.groupThreadInfo.creator
                            signalThread.lastModified = now
                            signalThread.lastServerModified = result.data.groupThreadInfo.lastModified
                        }
                        return@withContext signalThread
                    }
                    else if (signalThread != null &&
                        signalThread.lastServerModified < result.data.groupThreadInfo.lastModified &&
                        signalThread.active) { // account.phoneFull not included in thread => has been removed from group
                        signalThread.participants = result.data.groupThreadInfo.participantsList
                        signalThread.admin = admin
                        signalThread.admins = result.data.groupThreadInfo.adminsList
                        signalThread.lastModified = now
                        signalThread.lastServerModified = result.data.groupThreadInfo.lastModified
                        signalThread.active = false
                        return@withContext signalThread
                    }

                }
                is Result.Error -> {
                    debugConfig.log(TAG, "findGroupSignalThreadInfo Failed: ${result.exception.toString()}")
                    if (signalGroupThreadNotificationMessageProto != null){
                        var signalThread = findSignalThreadByThreadId(threadId)
                        val admin = signalGroupThreadNotificationMessageProto.adminsList.contains(account.phoneFull)
                        val now = kronosClock.getCurrentTimeMs()

                        if (signalGroupThreadNotificationMessageProto.participantsList.contains(account.phoneFull)){
                            if (signalThread == null){
                                signalThread = SignalThread(
                                    threadId = signalGroupThreadNotificationMessageProto.threadId,
                                    threadName = signalGroupThreadNotificationMessageProto.groupName,
                                    phoneFull = "",
                                    active = true,
                                    verified = true,
                                    hidden = false,
                                    threadType = CwmSignalMsg.SIGNAL_THREAD_TYPE.GROUP.number,
                                    creator =  signalGroupThreadNotificationMessageProto.creator,
                                    participants = signalGroupThreadNotificationMessageProto.participantsList,
                                    admin = admin,
                                    admins = signalGroupThreadNotificationMessageProto.adminsList,
                                    createdAt = now,
                                    lastModified = now,
                                    lastServerModified = signalGroupThreadNotificationMessageProto.lastModified
                                )

                                cwmUserRepository.findByListPhoneFull(signalGroupThreadNotificationMessageProto.participantsList)

                            }
                            else if (signalThread.lastServerModified < signalGroupThreadNotificationMessageProto.lastModified){
                                signalThread.threadName = signalGroupThreadNotificationMessageProto.groupName
                                signalThread.active = true
                                signalThread.verified = true
                                signalThread.participants = signalGroupThreadNotificationMessageProto.participantsList
                                signalThread.admin = admin
                                signalThread.admins = signalGroupThreadNotificationMessageProto.adminsList
                                signalThread.creator = signalGroupThreadNotificationMessageProto.creator
                                signalThread.lastModified = now
                                signalThread.lastServerModified = signalGroupThreadNotificationMessageProto.lastModified
                            }
                            return@withContext signalThread
                        }
                        else if (signalThread != null &&
                            signalThread.lastServerModified < signalGroupThreadNotificationMessageProto.lastModified &&
                            signalThread.active) { // account.phoneFull not included in thread => has been removed from group

                            signalThread.participants = signalGroupThreadNotificationMessageProto.participantsList
                            signalThread.admin = admin
                            signalThread.admins = signalGroupThreadNotificationMessageProto.adminsList
                            signalThread.lastModified = now
                            signalThread.lastServerModified = signalGroupThreadNotificationMessageProto.lastModified
                            signalThread.active = false
                            return@withContext signalThread
                        }

                    }
                }
            }

            return@withContext null
        }
    }

    //api
    suspend fun createNewGroupThread(groupName: String, participants: List<String>?): Result<CwmRqResThread.CreateGroupThreadResponse>{
        return withContext(ioDispatcher) {
            if (participants == null){
                return@withContext Result.Error(Exception("Invalid participants!"))
            }

            val account = accountRepository.getActiveAccount()
                ?: return@withContext Result.Error(Exception("Invalid Active Acount!"))

            if (!account.isLogin()){
                return@withContext Result.Error(Exception("Not Login Acount!"))
            }

            val submitParticipants = mutableListOf<String>()
            submitParticipants.add(account.phoneFull)
            submitParticipants.addAll(participants)

            val result = messageGrpcDataSource.createGroupThread(account, groupName, submitParticipants)
            when (result) {
                is Result.Success<CwmRqResThread.CreateGroupThreadResponse> -> {
                    debugConfig.log(TAG, "CreateGroupThreadResponse Success: ${result.data.groupThreadInfo.groupName} - ${result.data.groupThreadInfo.threadId}")
                    val now = kronosClock.getCurrentTimeMs()

                    val signalThread = SignalThread(
                        threadId = result.data.groupThreadInfo.threadId,
                        threadName = result.data.groupThreadInfo.groupName,
                        phoneFull = "",
                        active = true,
                        verified = true,
                        hidden = false,
                        threadType = CwmSignalMsg.SIGNAL_THREAD_TYPE.GROUP.number,
                        participants = result.data.groupThreadInfo.participantsList,
                        admin = true,
                        admins = result.data.groupThreadInfo.adminsList,
                        creator = result.data.groupThreadInfo.creator,
                        createdAt = now,
                        lastModified = now,
                        lastServerModified = result.data.groupThreadInfo.lastModified
                    )
                    saveSignalThread(signalThread)

                }
                is Result.Error -> {
                    debugConfig.log(TAG, "Failed: ${result.exception.toString()}")
                }
            }

            accountRepository.checkAndHandleSessionExpired(result, account)

            return@withContext result
        }
    }


    suspend fun saveSignalThread(signalThread: SignalThread) {
        withContext(ioDispatcher) {
            appCoroutineScope.launch {
                try {
                    signalThreadDao.insert(signalThread)
                }catch (e: Throwable){
                    e.printStackTrace()
                }
            }.join()
        }
    }

    suspend fun saveUpdateSignalThreadLastMsgInfo(threadId: String, lastMsgId: String?, lastMsg: ByteArray?,
                                                  lastMsgImType: Int?,
                                                  lastMsgStatus: Int?, lastMsgDate: Long?, lastMsgServerDate: Long?, unreadMsgs: Long,
                                                  lastModified: Long) {
        withContext(ioDispatcher) {
            appCoroutineScope.launch {
                try {
                    signalThreadDao.updateLastMsgInfo(
                        threadId = threadId,
                        lastMsgId = lastMsgId,
                        lastMsg = lastMsg,
                        lastMsgImType = lastMsgImType,
                        lastMsgStatus = lastMsgStatus,
                        lastMsgDate = lastMsgDate,
                        lastMsgServerDate = lastMsgServerDate,
                        unreadMsgs = unreadMsgs,
                        lastModified = lastModified,
                    )
                }catch (e: Throwable){
                    e.printStackTrace()
                }
            }.join()
        }
    }

    suspend fun saveUpdateSignalThreadLastMsgInfo(threadId: String, lastMsgId: String?, lastMsg: ByteArray?,
                                                  lastMsgImType: Int?,
                                                  lastMsgStatus: Int?, lastMsgDate: Long?,
                                                  lastModified: Long) {
        withContext(ioDispatcher) {
            appCoroutineScope.launch {
                try {
                    signalThreadDao.updateLastMsgInfo(
                        threadId = threadId,
                        lastMsgId = lastMsgId,
                        lastMsg = lastMsg,
                        lastMsgImType = lastMsgImType,
                        lastMsgStatus = lastMsgStatus,
                        lastMsgDate = lastMsgDate,
                        lastModified = lastModified,
                    )
                }catch (e: Throwable){
                    e.printStackTrace()
                }
            }.join()
        }
    }

    suspend fun delSignalThread(threadId: String,serverDate: Long?) {
        withContext(ioDispatcher) {
            appCoroutineScope.launch {
                try {
                    debugConfig.log(TAG, "delSignalThread - ${threadId} - to serverDate ${serverDate} ")
                    if (serverDate != null){
                        signalMsgDao.clearByThreadId(threadId, serverDate)
                        if (signalMsgDao.countAllMsgHaveContentByThreadIdFlow(threadId) == 0L){
                            signalThreadDao.deleteSignalThread(threadId)
                        }
                    }else{
                        signalMsgDao.deleteByThreadId(threadId)
                        signalThreadDao.deleteSignalThread(threadId)
                    }

                }catch (e: Throwable){
                    e.printStackTrace()
                }
            }.join()
        }
    }

    suspend fun updateThreadUnreadMsgs(threadId: String){
        withContext(ioDispatcher) {
            appCoroutineScope.launch {
                try {
                    val threadUnreadMsgs =  signalMsgDao.countAllUnreadMsgOfThreadId(threadId)
                    signalThreadDao.updateUnreadMsgs(threadId, threadUnreadMsgs)
                }catch (e: Throwable){
                    e.printStackTrace()
                }
            }.join()
        }
    }

    suspend fun updateThreadLastViewPos(threadId: String, pos: Int){
        withContext(ioDispatcher) {
            appCoroutineScope.launch {
                try {
                    signalThreadDao.updateLastViewPos(threadId, pos)
                }catch (e: Throwable){
                    e.printStackTrace()
                }
            }.join()
        }
    }

    suspend fun resetThreadLastViewPosByThreadId(threadId: String){
        withContext(ioDispatcher) {
            appCoroutineScope.launch {
                try {
                    signalThreadDao.resetLastViewPos(threadId)
                }catch (e: Throwable){
                    e.printStackTrace()
                }
            }.join()
        }
    }

    suspend fun updateThreadLastMsg(threadId: String){
        val signalThread = findSignalThreadByThreadId(threadId)
        if (signalThread == null) {return}

        val lastSignalMsg = getLastMsgHaveContentByThreadId(signalThread.threadId)
        val now = kronosClock.getCurrentTimeMs()
        if (lastSignalMsg != null){

//            val signalMsgExt = SignalMsgExt(lastSignalMsg)

            saveUpdateSignalThreadLastMsgInfo(
                threadId = threadId,
                lastMsgId = lastSignalMsg.msgId,
                lastMsg = lastSignalMsg.content,
                lastMsgImType = lastSignalMsg.imType,
                lastMsgStatus = lastSignalMsg.status,
                lastMsgDate = lastSignalMsg.msgDate,
                lastMsgServerDate = lastSignalMsg.serverDate,
                unreadMsgs = countAllUnreadMsgOfThread(threadId),
                lastModified = now,
            )

        }else{
            saveUpdateSignalThreadLastMsgInfo(
                threadId = threadId,
                lastMsgId = null,
                lastMsg = null,
                lastMsgImType = null,
                lastMsgStatus = null,
                lastMsgDate = null,
                lastMsgServerDate = null,
                unreadMsgs = 0,
                lastModified = now,
            )
        }
    }
    //endregion





}