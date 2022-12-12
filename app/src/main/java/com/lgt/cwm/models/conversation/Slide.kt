package com.lgt.cwm.models.conversation

import android.content.Context
import android.content.res.Resources.Theme
import android.net.Uri
import android.os.Build
import androidx.annotation.DrawableRes
import com.lgt.cwm.ui.components.blurhash.BlurHash
import java.util.*

abstract class Slide(val context: Context, val attachment: Attachment) {
    val contentType: String = attachment.contentType
    val uri: Uri? = attachment.getUri()
    val publicUri: Uri?
        get() {
            return if (Build.VERSION.SDK_INT >= 28) {
                attachment.getPublicUri()
            } else {
                attachment.getUri()
            }
        }
    val body: Optional<String>  = Optional.empty()
    val caption: Optional<String> = Optional.ofNullable(attachment.caption)
    val fileName: Optional<String> = Optional.ofNullable(attachment.fileName)
    val fastPreflightId: String? = attachment.fastPreflightId
    val fileSize: Long = attachment.size

    open fun hasImage(): Boolean {
        return false
    }

    fun hasSticker(): Boolean {
        return false
    }

    fun hasVideo(): Boolean {
        return false
    }

    fun hasAudio(): Boolean {
        return false
    }

    fun hasDocument(): Boolean {
        return false
    }

    fun hasLocation(): Boolean {
        return false
    }

    fun hasViewOnce(): Boolean {
        return false
    }

    open fun isBorderless(): Boolean {
        return false
    }

    fun isVideoGif(): Boolean {
        return hasVideo() && attachment.isVideoGif
    }

    open val contentDescription: String = ""

    fun asAttachment(): Attachment {
        return attachment
    }

    @DrawableRes
    open fun getPlaceholderRes(theme: Theme): Int {
        throw AssertionError("getPlaceholderRes() called for non-drawable slide")
    }

    fun getPlaceholderBlur(): BlurHash? {
        return attachment.blurHash
    }

    open fun hasPlaceholder(): Boolean {
        return false
    }

    fun hasPlayOverlay(): Boolean {
        return false
    }
}