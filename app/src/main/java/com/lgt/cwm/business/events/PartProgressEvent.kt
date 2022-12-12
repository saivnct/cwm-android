package com.lgt.cwm.business.events

import com.lgt.cwm.db.entity.SignalMsg
import cwmSignalMsgPb.CwmSignalMsg


class PartProgressEvent(fileInfo: CwmSignalMsg.MultimediaFileInfo, type: Type, total: Long, progress: Long) {
    val fileInfo: CwmSignalMsg.MultimediaFileInfo
    val type: Type
    val total: Long
    val progress: Long

    enum class Type {
        COMPRESSION, NETWORK
    }

    init {
        this.fileInfo = fileInfo
        this.type = type
        this.total = total
        this.progress = progress
    }
}