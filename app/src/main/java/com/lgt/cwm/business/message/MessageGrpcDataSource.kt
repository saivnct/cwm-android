package com.lgt.cwm.business.message

import android.content.Context
import android.net.Uri
import com.lgt.cwm.db.entity.Account
import com.lgt.cwm.grpc.GrpcUtils
import com.lgt.cwm.util.Config
import com.lgt.cwm.util.DebugConfig
import com.lgt.cwm.util.Result
import com.lgt.cwm.util.md5
import cwmSIPPb.CwmSIP
import cwmSignalMsgPb.CwmSignalMsg
import dagger.hilt.android.qualifiers.ApplicationContext
import grpcCWMPb.CWMServiceGrpcKt
import grpcCWMPb.CwmModel
import grpcCWMPb.CwmRqResMsg
import grpcCWMPb.CwmRqResThread
import io.grpc.ManagedChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeoutOrNull
import java.io.ByteArrayOutputStream
import javax.inject.Inject


/**
 * Created by giangtpu on 30/07/2022.
 */
class MessageGrpcDataSource  @Inject constructor(
    @ApplicationContext private val applicationContext: Context,
    private val debugConfig: DebugConfig
) {
    private val TAG = MessageGrpcDataSource::class.simpleName.toString()

    suspend fun createGroupThread(account: Account, groupName: String, participants: List<String>): Result<CwmRqResThread.CreateGroupThreadResponse> {
        val channel = GrpcUtils.getChannel(applicationContext, account)
            ?: return Result.Error(Exception("Cannot create grpc channel"))

        return try {
            val cwmStub = CWMServiceGrpcKt.CWMServiceCoroutineStub(channel)
            val request = CwmRqResThread.CreateGroupThreadRequest.newBuilder()
                .setGroupName(groupName)
                .addAllParticipants(participants)
                .build()

//            debugConfig.log(TAG, "call createGroupThread")
            val response = cwmStub.createGroupThread(request)
//            debugConfig.log(TAG, "call createGroupThread done")

            Result.Success(response)

        }catch (ex: Throwable){
//            ex.printStackTrace()
            Result.Error(ex)
        }finally {
            channel.shutdown()
        }
    }

    suspend fun checkGroupThreadInfo(account: Account, threadID: String): Result<CwmRqResThread.CheckGroupThreadInfoResponse> {
        val channel = GrpcUtils.getChannel(applicationContext, account)
            ?: return Result.Error(Exception("Cannot create grpc channel"))

        return try {
            val cwmStub = CWMServiceGrpcKt.CWMServiceCoroutineStub(channel)
            val request = CwmRqResThread.CheckGroupThreadInfoRequest.newBuilder()
                .setThreadId(threadID)
                .build()

//            debugConfig.log(TAG, "call checkGroupThreadInfo")
            val response = cwmStub.checkGroupThreadInfo(request)
//            debugConfig.log(TAG, "call checkGroupThreadInfo done")

            Result.Success(response)

        }catch (ex: Throwable){
            ex.printStackTrace()
            Result.Error(ex)
        }finally {
            channel.shutdown()
        }
    }

    private fun generateMediaMsgInfoRequestFlow(msgId: String, multimediaFileInfo: CwmSignalMsg.MultimediaFileInfo): Flow<CwmRqResMsg.UploadMediaMsgRequest> = flow {
        val mediaMsgInfo = CwmModel.MediaMsgInfo.newBuilder()
            .setChecksum(multimediaFileInfo.checksum)
            .setMsgId(msgId)
            .setMediaType(multimediaFileInfo.mediaType)
            .build()
        var mediaMsgInfoRequest = CwmRqResMsg.UploadMediaMsgRequest.newBuilder()
            .setMediaMsgInfo(mediaMsgInfo)
            .build()

        emit(mediaMsgInfoRequest)

        //https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.io/use.html

        applicationContext.getContentResolver().openInputStream(Uri.parse(multimediaFileInfo.fileUri)).use { inputStream ->
            inputStream?.let {
                val buffer = ByteArray(1024) //bufferSize = 1024
                var len = 0
                while (inputStream.read(buffer).also { len = it } != -1) {
                    mediaMsgInfoRequest = CwmRqResMsg.UploadMediaMsgRequest.newBuilder()
                        .setChunkData(com.google.protobuf.ByteString.copyFrom(buffer, 0, len))
                        .build()
                    emit(mediaMsgInfoRequest)
                }
            }
        }
    }

    suspend fun uploadMediaMsg(account: Account, msgId: String, multimediaFileInfo: CwmSignalMsg.MultimediaFileInfo): Result<CwmRqResMsg.UploadMediaMsgResponse>{
//        debugConfig.log(TAG, "call uploadMediaMsg")

        val channel = GrpcUtils.getChannel(applicationContext, account) ?: return Result.Error(Exception("Cannot create grpc channel"))

        try {
            val cwmStub = CWMServiceGrpcKt.CWMServiceCoroutineStub(channel)

            val uploadMediaMsgResponse = cwmStub.uploadMediaMsg(generateMediaMsgInfoRequestFlow(msgId, multimediaFileInfo))

            return Result.Success(uploadMediaMsgResponse)
        }catch (ex: Throwable){
            ex.printStackTrace()
            return Result.Error(ex)
        }finally {
            channel.shutdown()
        }
    }

    suspend fun downloadMediaMsg(account: Account, msgId: String, fileId: String, checksum: String) : Result<ByteArray>{
//        debugConfig.log(TAG, "call downloadMediaMsg")
        val channel = GrpcUtils.getChannel(applicationContext, account) ?: return Result.Error(Exception("Cannot create grpc channel"))

        try {
            val byteBuffer = ByteArrayOutputStream()

            val request = CwmRqResMsg.DownloadMediaMsgRequest.newBuilder()
                .setMsgId(msgId)
                .setFileId(fileId)
                .build()

            val cwmStub = CWMServiceGrpcKt.CWMServiceCoroutineStub(channel)

            cwmStub.downloadMediaMsg(request)
//                .catch { exception ->
//                    debugConfig.log(TAG, "downloadMediaMsg error at flow!!!!!!!!")
//                    exception.printStackTrace()
//                }
                .collect{ downloadMediaMsgResponse ->
                    try {
                        downloadMediaMsgResponse?.let {
                            byteBuffer.write(downloadMediaMsgResponse.chunkData.toByteArray())
                        }
                    }catch (ex: Throwable){
                        ex.printStackTrace()
                    }
                }

            val data = byteBuffer.toByteArray()
            val dataChecksum = data.md5()
//            debugConfig.log(TAG, "downloadMediaMsg Stream Complete, file size: ${data.size}, checksum: ${dataChecksum}")
            if (checksum.equals(dataChecksum)){
//                debugConfig.log(TAG, "downloadMediaMsg success!!!")
                return Result.Success(data)
            }else{
//                debugConfig.log(TAG, "downloadMediaMsg failed invalid checksum: ${dataChecksum} <> ${checksum}!!!")
                return Result.Error(Exception("Download file failed Error"))
            }


        }catch (ex: Throwable){
            return Result.Error(ex)
        }finally {
            channel.shutdown()
        }
    }

    suspend fun initialSyncMsg(account: Account) : Result<List<CwmRqResMsg.InitialSyncMsgResponse>> {
        var result : Result<List<CwmRqResMsg.InitialSyncMsgResponse>>? = null
        val channel = GrpcUtils.getChannel(applicationContext, account)
            ?: return Result.Error(Exception("Cannot create grpc channel"))


        withTimeoutOrNull(Config.GRPC.TIMEOUT*1000){
            result = initialSyncMsg(channel)
        }

        if (result == null) {
            if (!channel.isShutdown){
                channel.shutdown()
            }
            result = Result.Error(Exception("Timeout grpc call"))
        }

        return result!!
    }

    suspend fun initialSyncMsg(channel: ManagedChannel) : Result<List<CwmRqResMsg.InitialSyncMsgResponse>>{
//        debugConfig.log(TAG, "call initialSyncMsg")
        try {
            val request = CwmRqResMsg.InitialSyncMsgRequest.newBuilder().build()

            val msgList = arrayListOf<CwmRqResMsg.InitialSyncMsgResponse>()

            val cwmStub = CWMServiceGrpcKt.CWMServiceCoroutineStub(channel)
            cwmStub.initialSyncMsg(request)
//                .catch { e ->
//                    debugConfig.log(TAG, "initialSyncMsg error at flow!!!!!!!!")
//                    e.printStackTrace()
//                }
                .collect{ response ->
                    msgList.add(response)
                }

           return Result.Success(msgList)

        }catch (ex: Throwable){
            return Result.Error(ex)
        }finally {
            channel.shutdown()
        }
    }


    suspend fun fetchAllUnreceivedMsg(account: Account, fromDate: Long) : Result<List<CwmSIP.CWMRequest>> {
        var result : Result<List<CwmSIP.CWMRequest>>? = null
        val channel = GrpcUtils.getChannel(applicationContext, account)
            ?: return Result.Error(Exception("Cannot create grpc channel"))


        withTimeoutOrNull(Config.GRPC.TIMEOUT*1000){
            result = fetchAllUnreceivedMsg(channel, fromDate)
        }

        if (result == null) {
            if (!channel.isShutdown){
                channel.shutdown()
            }
            result = Result.Error(Exception("Timeout grpc call"))
        }

        return result!!
    }

    suspend fun fetchAllUnreceivedMsg(channel: ManagedChannel, fromDate: Long) : Result<List<CwmSIP.CWMRequest>>{
//        debugConfig.log(TAG, "call fetchAllUnreceivedMsg")
        try {
            val request = CwmRqResMsg.FetchAllUnreceivedMsgRequest
                .newBuilder()
                .setFromDate(fromDate)
                .build()

            val cwmStub = CWMServiceGrpcKt.CWMServiceCoroutineStub(channel)

            val msgList = arrayListOf<CwmSIP.CWMRequest>()

            cwmStub.fetchAllUnreceivedMsg(request)
//                    .catch {    e ->
//                        debugConfig.log(TAG, "fetchAllUnreceivedMsg error at flow!!!!!!!!")
//                        e.printStackTrace()
//                    }
                .collect{ response ->
                    msgList.add(response.msg)
                }



            return Result.Success(msgList)
        }catch (ex: Throwable){
            return Result.Error(ex)
        }finally {
            channel.shutdown()
        }
    }

    suspend fun fetchOldMsgOfThread(account: Account, threadId: String, toDate: Long) : Result<List<CwmSIP.CWMRequest>>{
//        debugConfig.log(TAG, "call fetchOldMsgOfThread")

        val channel = GrpcUtils.getChannel(applicationContext, account) ?: return Result.Error(Exception("Cannot create grpc channel"))

        try {
            val request = CwmRqResMsg.FetchOldMsgOfThreadRequest.newBuilder()
                .setThreadId(threadId)
                .setToDate(toDate)
                .setLimit(10)
                .build()

            val cwmStub = CWMServiceGrpcKt.CWMServiceCoroutineStub(channel)

            val msgList = arrayListOf<CwmSIP.CWMRequest>()
            cwmStub.fetchOldMsgOfThread(request)
//                .catch { e ->
//                    debugConfig.log(TAG, "fetchOldMsgOfThread error at flow!!!!!!!!")
//                    e.printStackTrace()
//                }
                .collect{ response ->
                    msgList.add(response.msg)
                }
            return Result.Success(msgList)

        }catch (ex: Throwable){
            return Result.Error(ex)
        }finally {
            channel.shutdown()
        }
    }

    suspend fun sendMsg(account: Account, cwmRequest: CwmSIP.CWMRequest):  Result<CwmRqResMsg.SendMsgResponse>{
        val channel = GrpcUtils.getChannel(applicationContext, account)
            ?: return Result.Error(Exception("Cannot create grpc channel"))

        return try {
            val cwmStub = CWMServiceGrpcKt.CWMServiceCoroutineStub(channel)
            val request = CwmRqResMsg.SendMsgRequest.newBuilder()
                .setMsg(cwmRequest)
                .build()

//            debugConfig.log(TAG, "call confirmReceivedMsgs")
            val response = cwmStub.sendMsg(request)
//            debugConfig.log(TAG, "call confirmReceivedMsgs done")

            Result.Success(response)

        }catch (ex: Throwable){
            ex.printStackTrace()
            Result.Error(ex)
        }finally {
            channel.shutdown()
        }
    }

    suspend fun confirmReceivedMsgs(account: Account, msgIds: List<String>):  Result<CwmRqResMsg.ConfirmReceivedMsgsResponse>{
        val channel = GrpcUtils.getChannel(applicationContext, account)
            ?: return Result.Error(Exception("Cannot create grpc channel"))

        return try {
            val cwmStub = CWMServiceGrpcKt.CWMServiceCoroutineStub(channel)
            val request = CwmRqResMsg.ConfirmReceivedMsgsRequest.newBuilder()
                .addAllMsgIds(msgIds)
                .build()

//            debugConfig.log(TAG, "call confirmReceivedMsgs")
            val response = cwmStub.confirmReceivedMsgs(request)
//            debugConfig.log(TAG, "call confirmReceivedMsgs done")

            Result.Success(response)

        }catch (ex: Throwable){
            ex.printStackTrace()
            Result.Error(ex)
        }finally {
            channel.shutdown()
        }
    }


    suspend fun deleteMsgsOfThread(account: Account, threadId: String, msgIds: List<String>, deleteForAllMembers: Boolean):  Result<CwmRqResMsg.DeleteMsgsOfThreadResponse>{
        val channel = GrpcUtils.getChannel(applicationContext, account)
            ?: return Result.Error(Exception("Cannot create grpc channel"))

        return try {
            val cwmStub = CWMServiceGrpcKt.CWMServiceCoroutineStub(channel)
            val request = CwmRqResMsg.DeleteMsgsOfThreadRequest.newBuilder()
                .setThreadId(threadId)
                .addAllMsgIds(msgIds)
                .setDeleteForAllMembers(deleteForAllMembers)
                .build()

//            debugConfig.log(TAG, "call deleteMsgsOfThread")
            val response = cwmStub.deleteMsgsOfThread(request)
//            debugConfig.log(TAG, "call deleteMsgsOfThread done")

            Result.Success(response)

        }catch (ex: Throwable){
            ex.printStackTrace()
            Result.Error(ex)
        }finally {
            channel.shutdown()
        }
    }

    suspend fun clearAllMsgOfThread(account: Account, threadId: String, deleteForAllMembers: Boolean):  Result<CwmRqResMsg.ClearAllMsgOfThreadResponse>{
        val channel = GrpcUtils.getChannel(applicationContext, account)
            ?: return Result.Error(Exception("Cannot create grpc channel"))

        return try {
            val cwmStub = CWMServiceGrpcKt.CWMServiceCoroutineStub(channel)
            val request = CwmRqResMsg.ClearAllMsgOfThreadRequest.newBuilder()
                .setThreadId(threadId)
                .setDeleteForAllMembers(deleteForAllMembers)
                .build()

//            debugConfig.log(TAG, "call clearAllMsgOfThread")
            val response = cwmStub.clearAllMsgOfThread(request)
//            debugConfig.log(TAG, "call clearAllMsgOfThread done")

            Result.Success(response)

        }catch (ex: Throwable){
            ex.printStackTrace()
            Result.Error(ex)
        }finally {
            channel.shutdown()
        }
    }


    suspend fun deleteAndLeaveGroupThread(account: Account, threadId: String):  Result<CwmRqResThread.DeleteAndLeaveGroupThreadResponse>{
        val channel = GrpcUtils.getChannel(applicationContext, account)
            ?: return Result.Error(Exception("Cannot create grpc channel"))

        return try {
            val cwmStub = CWMServiceGrpcKt.CWMServiceCoroutineStub(channel)
            val request = CwmRqResThread.DeleteAndLeaveGroupThreadRequest.newBuilder()
                .setThreadId(threadId)
                .build()

//            debugConfig.log(TAG, "call deleteAndLeaveGroupThread")
            val response = cwmStub.deleteAndLeaveGroupThread(request)
//            debugConfig.log(TAG, "call deleteAndLeaveGroupThread done")

            Result.Success(response)

        }catch (ex: Throwable){
            ex.printStackTrace()
            Result.Error(ex)
        }finally {
            channel.shutdown()
        }
    }

    suspend fun deleteSoloThread(account: Account, threadId: String, deleteForAllMembers: Boolean):  Result<CwmRqResMsg.DeleteSoloThreadResponse>{
        val channel = GrpcUtils.getChannel(applicationContext, account)
            ?: return Result.Error(Exception("Cannot create grpc channel"))

        return try {
            val cwmStub = CWMServiceGrpcKt.CWMServiceCoroutineStub(channel)
            val request = CwmRqResMsg.DeleteSoloThreadRequest.newBuilder()
                .setThreadId(threadId)
                .setDeleteForAllMembers(deleteForAllMembers)
                .build()

//            debugConfig.log(TAG, "call deleteSoloThread")
            val response = cwmStub.deleteSoloThread(request)
//            debugConfig.log(TAG, "call deleteSoloThread done")

            Result.Success(response)

        }catch (ex: Throwable){
            ex.printStackTrace()
            Result.Error(ex)
        }finally {
            channel.shutdown()
        }
    }



}