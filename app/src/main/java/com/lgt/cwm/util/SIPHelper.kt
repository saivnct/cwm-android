package com.lgt.cwm.util

import com.lgt.cwm.db.entity.SignalMsg
import cwmSIPPb.CwmSIP
import cwmSignalMsgPb.CwmSignalMsg

/**
 * Created by giangtpu on 7/7/22.
 */
object SIPHelper {
    fun getCWMRequestHeader(method : CwmSIP.REQUEST_METHOD, from: String, fromSession: String, to: String): CwmSIP.CWMRequestHeader{
        return CwmSIP.CWMRequestHeader
            .newBuilder()
            .setMethod(method)
            .setFrom(from)
            .setFromSession(fromSession)
            .setTo(to)
            .build()
    }

    fun getCWMRequest(header: CwmSIP.CWMRequestHeader, content: Any?): CwmSIP.CWMRequest{
        val cwmRequestBuilder = CwmSIP.CWMRequest
                                .newBuilder()
                                .setHeader(header)

        content?.let {
            when (it) {
                is ByteArray -> cwmRequestBuilder.setContent(com.google.protobuf.ByteString.copyFrom(it))
                is com.google.protobuf.ByteString -> cwmRequestBuilder.setContent(it)
                else -> {}
            }
        }
        return cwmRequestBuilder.build()
    }

    fun getCwmSignalMsg(signalMsg: SignalMsg, fromSession : String): CwmSIP.CWMRequest{
        val cwmRequestHeader = getCWMRequestHeader(
            method = CwmSIP.REQUEST_METHOD.METHOD_MESSAGE,
            from = signalMsg.from,
            fromSession = fromSession,
            to = signalMsg.to,
        )

        val signalMsgProto = CwmSignalMsg.SignalMessage
            .newBuilder()
            .setMsgId(signalMsg.msgId)
            .setThreadType(CwmSignalMsg.SIGNAL_THREAD_TYPE.forNumber(signalMsg.threadType))
            .setImType(CwmSignalMsg.SIGNAL_IM_TYPE.forNumber(signalMsg.imType))
            .setThreadId(signalMsg.threadId)
            .setReplyMsgId(signalMsg.replyMsgId)
            .setMsgDate(signalMsg.msgDate)
            .setData(com.google.protobuf.ByteString.copyFrom(signalMsg.content))
            .setChecksum(signalMsg.content?.md5())
            .build()


        val cwmRequest = getCWMRequest(
            header = cwmRequestHeader,
            content = signalMsgProto.toByteString()
        )

        return cwmRequest
    }

}