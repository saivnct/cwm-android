package com.lgt.cwm.activity.conversation.fragments.models

import cwmSignalMsgPb.CwmSignalMsg

data class QuoteModel (
    var replyId: String,
    var author: String,
    var content: CharSequence?,
    var isSelf: Boolean,
    var attachments: List<CwmSignalMsg.MultimediaFileInfo>?
)