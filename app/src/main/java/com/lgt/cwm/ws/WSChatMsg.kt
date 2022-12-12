package com.lgt.cwm.ws

import com.google.gson.annotations.SerializedName
import cwmSignalMsgPb.CwmSignalMsg

/**
 * Created by giangtpu on 04/07/2022.
 */
data class WSChatMsg(
    @SerializedName("from") val from: String,
    @SerializedName("to") val to: String,
    val threadId: String,
    val msgId: String,
    val threadType: CwmSignalMsg.SIGNAL_THREAD_TYPE,
    val imType: CwmSignalMsg.SIGNAL_IM_TYPE,
    val msgDate: Long,
    val serverDate: Long,
    val data: ByteArray,
){
    override fun toString(): String {
        return "WSChatMsg(from='$from', to='$to', threadId='$threadId', msgId='$msgId', threadType=$threadType, imType=$imType, msgDate=$msgDate, serverDate=$serverDate, data=${data.contentToString()})"
    }
}
