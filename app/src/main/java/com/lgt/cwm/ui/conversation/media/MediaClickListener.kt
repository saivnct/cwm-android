package com.lgt.cwm.ui.conversation.media

import android.view.View
import cwmSignalMsgPb.CwmSignalMsg

interface MediaClickListener {
    fun onClick(v: View, mediaFile: CwmSignalMsg.MultimediaFileInfo, mediaFileList: List<CwmSignalMsg.MultimediaFileInfo>? = null)
}