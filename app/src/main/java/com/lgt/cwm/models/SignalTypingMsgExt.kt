package com.lgt.cwm.models

import cwmSignalMsgPb.CwmSignalMsg

/**
 * Created by giangtpu on 02/10/2022.
 */
data class SignalTypingMsgExt (
    var threadId: String,
    var from: String,
    var fromFirstName: String,
    var fromLastName: String?,
    var fromUserName: String,
    var typingType: CwmSignalMsg.SIGNAL_TYPING_MSG_TYPE
    )
{

}