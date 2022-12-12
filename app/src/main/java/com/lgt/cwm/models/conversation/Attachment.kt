package com.lgt.cwm.models.conversation

import android.net.Uri
import com.lgt.cwm.ui.components.blurhash.BlurHash

abstract class Attachment(
    val contentType: String,
    val transferState: Int,
    val size: Long,
    val fileName: String?,
    val cdnNumber: Int,
    val location: String?,
    val key: String?,
    val relay: String?,
    val digest: ByteArray?,
    val fastPreflightId: String?,
    val isVoiceNote: Boolean,
    val isBorderless: Boolean,
    val isVideoGif: Boolean,
    val width: Int,
    val height: Int,
    val isQuote: Boolean,
    val uploadTimestamp: Long,
    val caption: String?,
    val blurHash: BlurHash?,
) {

    abstract fun getUri(): Uri?

    abstract fun getPublicUri(): Uri?
}