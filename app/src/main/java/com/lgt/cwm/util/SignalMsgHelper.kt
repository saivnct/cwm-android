package com.lgt.cwm.util

import com.lgt.cwm.db.entity.Account
import com.lgt.cwm.db.entity.SignalMsg
import com.lgt.cwm.db.entity.SignalMsgDirection
import com.lgt.cwm.db.entity.SignalMsgStatus
import com.lgt.cwm.models.FileMetaData
import com.lgt.cwm.models.SignalMsgExt
import cwmSignalMsgPb.CwmSignalMsg
import java.util.*

/**
 * Created by giangtpu on 7/27/22.
 */
object SignalMsgHelper {
    fun createForwardMessage(from: Account, to: String,
                             threadId: String, threadType: Int,
                             signalMsgToForward: SignalMsgExt, msgDate: Long,


    ): SignalMsg?{

        if (signalMsgToForward.content == null){
            return null
        }

        val signalForwardMessage = CwmSignalMsg.SignalForwardMessage.newBuilder()
            .setMsgId(signalMsgToForward.msgId)
            .setFrom(signalMsgToForward.from)
            .setFromFirstName(signalMsgToForward.fromFirstName)
            .setFromLastName(signalMsgToForward.fromLastName)
            .setFromUserName(signalMsgToForward.fromUserName)
            .setThreadId(signalMsgToForward.threadId)
            .setThreadType(CwmSignalMsg.SIGNAL_THREAD_TYPE.forNumber(signalMsgToForward.threadType))
            .setImType(CwmSignalMsg.SIGNAL_IM_TYPE.forNumber(signalMsgToForward.imType))
            .setChecksum(signalMsgToForward.checksum)
            .setData(com.google.protobuf.ByteString.copyFrom(signalMsgToForward.content))
            .build()

        val contentByteArray = signalForwardMessage.toByteArray()
        val checksum = contentByteArray.md5()


        var toReceiver = to
        if (threadType == CwmSignalMsg.SIGNAL_THREAD_TYPE.GROUP.number){
            toReceiver = threadId
        }

        return SignalMsg(
            from = from.phoneFull,
            fromFirstName = from.firstName,
            fromLastName = from.lastName,
            fromUserName = from.username,
            to = toReceiver,
            msgId = UUID.randomUUID().toString(),
            replyMsgId = "",
            threadId = threadId,
            threadType = threadType,
            imType = CwmSignalMsg.SIGNAL_IM_TYPE.FORWARD.number,
            msgDate = msgDate,
            confirmReceive = true,
            sendSeenState = true,
            eventHandled = true,
            multiMediaDownloadHandled = true,
            checksum = checksum,
            content = contentByteArray,
            status = SignalMsgStatus.SENDING.code,
            direction =  SignalMsgDirection.OUTGOING.code,
            threadVerified = true
        )
    }

    fun createIMMessage(from: Account, to: String,
                        threadId: String, threadType: Int,
                        content: String, msgDate: Long,
                        replyMsgId: String? = null): SignalMsg{

        val contentByteArray = content.toByteArray()
        val checksum = contentByteArray.md5()

        var toReceiver = to
        if (threadType == CwmSignalMsg.SIGNAL_THREAD_TYPE.GROUP.number){
            toReceiver = threadId
        }

        return SignalMsg(
            from = from.phoneFull,
            fromFirstName = from.firstName,
            fromLastName = from.lastName,
            fromUserName = from.username,
            to = toReceiver,
            msgId = UUID.randomUUID().toString(),
            replyMsgId = replyMsgId ?: "",
            threadId = threadId,
            threadType = threadType,
            imType = CwmSignalMsg.SIGNAL_IM_TYPE.IM.number,
            msgDate = msgDate,
            confirmReceive = true,
            sendSeenState = true,
            eventHandled = true,
            multiMediaDownloadHandled = true,
            checksum = checksum,
            content = contentByteArray,
            status = SignalMsgStatus.SENDING.code,
            direction =  SignalMsgDirection.OUTGOING.code,
            threadVerified = true
        )
    }

    fun createURLMessage(from: Account, to: String,
                         threadId: String, threadType: Int,
                         content: String,
                         url: String, urlTitle: String,
                         urlDescription: String, urlThumbnail: String?,
                         msgDate: Long, replyMsgId: String? = null): SignalMsg{
        var toReceiver = to
        if (threadType == CwmSignalMsg.SIGNAL_THREAD_TYPE.GROUP.number){
            toReceiver = threadId
        }


        val contentByteArray = content.toByteArray()
        val signalURLMessage = CwmSignalMsg.SignalURLMessage.newBuilder()
            .setUrl(url)
            .setUrlTitle(urlTitle)
            .setUrlDescription(urlDescription)
            .setUrlThumbnail(urlThumbnail ?: "")
            .setData(com.google.protobuf.ByteString.copyFrom(contentByteArray))
            .build()

        val signalMsgContent = signalURLMessage.toByteArray()
        val checksum = signalMsgContent.md5()

        return SignalMsg(
            from = from.phoneFull,
            fromFirstName = from.firstName,
            fromLastName = from.lastName,
            fromUserName = from.username,
            to = toReceiver,
            msgId = UUID.randomUUID().toString(),
            replyMsgId = replyMsgId ?: "",
            threadId = threadId,
            threadType = threadType,
            imType = CwmSignalMsg.SIGNAL_IM_TYPE.URL.number,
            msgDate = msgDate,
            confirmReceive = true,
            sendSeenState = true,
            eventHandled = true,
            multiMediaDownloadHandled = true,
            checksum = checksum,
            content = signalMsgContent,
            status = SignalMsgStatus.SENDING.code,
            direction =  SignalMsgDirection.OUTGOING.code,
            threadVerified = true
        )
    }

    fun createMultiMediaMessage(from: Account, to: String,
                                threadId: String, threadType: Int,
                                fileMetaDatas: List<FileMetaData>, msgDate: Long,
                                replyMsgId: String? = null): SignalMsg{
        var toReceiver = to
        if (threadType == CwmSignalMsg.SIGNAL_THREAD_TYPE.GROUP.number){
            toReceiver = threadId
        }

        val multimediaFileInfosList = mutableListOf<CwmSignalMsg.MultimediaFileInfo>()
        fileMetaDatas.forEach { fileMetaData ->
            val multimediaFileInfo = CwmSignalMsg.MultimediaFileInfo.newBuilder()
                .setChecksum(fileMetaData.checksum)
                .setMediaType(fileMetaData.mediaType)
                .setMimeType(fileMetaData.mimeType)
                .setFileUri(fileMetaData.uri.toString())
                .setFileStatus(CwmSignalMsg.SIGNAL_MEDIA_FILE_STATUS.SENDING)
                //fileName, fileSize
                .build()
            multimediaFileInfosList.add(multimediaFileInfo)
        }

        val signalMultimediaMessage = CwmSignalMsg.SignalMultimediaMessage.newBuilder()
            .addAllMultimediaFileInfos(multimediaFileInfosList)
            .build()

        val content = signalMultimediaMessage.toByteArray()
        val checksum = content.md5()

        return SignalMsg(
            from = from.phoneFull,
            fromFirstName = from.firstName,
            fromLastName = from.lastName,
            fromUserName = from.username,
            to = toReceiver,
            msgId = UUID.randomUUID().toString(),
            replyMsgId = replyMsgId ?: "",
            threadId = threadId,
            threadType = threadType,
            imType = CwmSignalMsg.SIGNAL_IM_TYPE.MULTIMEDIA.number,
            msgDate = msgDate,
            confirmReceive = true,
            sendSeenState = true,
            eventHandled = true,
            multiMediaDownloadHandled = true,
            checksum = checksum,
            content = content,
            status = SignalMsgStatus.SENDING.code,
            direction =  SignalMsgDirection.OUTGOING.code,
            threadVerified = true
        )
    }

    fun createSeenStateMessage(from: Account, to: String,
                               threadId: String, threadType: Int,
                               msgIdList: List<String>, msgDate: Long): SignalMsg{

        val signalSeenStateMessageBuilder = CwmSignalMsg.SignalSeenStateMessage.newBuilder()
            .setSeenStateType(CwmSignalMsg.SIGNAL_SEENSTATE_MSG_TYPE.SEEN)
        signalSeenStateMessageBuilder.addAllMsgId(msgIdList)

        val contentByteArray = signalSeenStateMessageBuilder.build().toByteArray()
        val checksum = contentByteArray.md5()

        var toReceiver = to
        if (threadType == CwmSignalMsg.SIGNAL_THREAD_TYPE.GROUP.number){
            toReceiver = threadId
        }

        return SignalMsg(
            from = from.phoneFull,
            fromFirstName = from.firstName,
            fromLastName = from.lastName,
            fromUserName = from.username,
            to = toReceiver,
            msgId = UUID.randomUUID().toString(),
            replyMsgId = "",
            threadId = threadId,
            threadType = threadType,
            imType = CwmSignalMsg.SIGNAL_IM_TYPE.SEENSTATE.number,
            msgDate = msgDate,
            confirmReceive = true,
            sendSeenState = true,
            eventHandled = true,
            multiMediaDownloadHandled = true,
            checksum = checksum,
            content = contentByteArray,
            status = SignalMsgStatus.SENDING.code,
            direction =  SignalMsgDirection.OUTGOING.code,
            threadVerified = true
        )
    }

    fun createTypingMessage(from: Account, to: String,
                               threadId: String, threadType: Int, isTyping: Boolean,
                            msgDate: Long): SignalMsg{

        val signalTypingMessageBuilder = CwmSignalMsg.SignalTypingMessage.newBuilder()
        if (isTyping){
            signalTypingMessageBuilder.setType(CwmSignalMsg.SIGNAL_TYPING_MSG_TYPE.M_TYPING)
        }else{
            signalTypingMessageBuilder.setType(CwmSignalMsg.SIGNAL_TYPING_MSG_TYPE.M_UNTYPING)
        }



        val contentByteArray = signalTypingMessageBuilder.build().toByteArray()
        val checksum = contentByteArray.md5()

        var toReceiver = to
        if (threadType == CwmSignalMsg.SIGNAL_THREAD_TYPE.GROUP.number){
            toReceiver = threadId
        }

        return SignalMsg(
            from = from.phoneFull,
            fromFirstName = from.firstName,
            fromLastName = from.lastName,
            fromUserName = from.username,
            to = toReceiver,
            msgId = UUID.randomUUID().toString(),
            replyMsgId = "",
            threadId = threadId,
            threadType = threadType,
            imType = CwmSignalMsg.SIGNAL_IM_TYPE.TYPING.number,
            msgDate = msgDate,
            confirmReceive = true,
            sendSeenState = true,
            eventHandled = true,
            multiMediaDownloadHandled = true,
            checksum = checksum,
            content = contentByteArray,
            status = SignalMsgStatus.SENDING.code,
            direction =  SignalMsgDirection.OUTGOING.code,
            threadVerified = true
        )
    }
}