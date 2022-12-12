package com.lgt.cwm.models

import android.net.Uri
import cwmSignalMsgPb.CwmSignalMsg

/**
 * Created by giangtpu on 22/08/2022.
 */
data class FileMetaData (
    val uri: Uri,
    val checksum: String,
    val mimeType: String,
    val mediaType: CwmSignalMsg.SIGNAL_MEDIA_TYPE,
    val isGallery: Boolean
){
    override fun toString(): String {
        return "FileMetaData(uri=$uri, checksum='$checksum', mimeType='$mimeType', mediaType=$mediaType)"
    }
}