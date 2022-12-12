package com.lgt.cwm.ui.conversation.media

import android.view.View
import cwmSignalMsgPb.CwmSignalMsg

interface MediaListClickListener {
    fun onClick(v: View, mediaFileList: List<CwmSignalMsg.MultimediaFileInfo>)
}