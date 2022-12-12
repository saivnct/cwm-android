package com.lgt.cwm.business.media.audio

import android.content.Context
import android.os.ParcelFileDescriptor
import java.io.IOException

/**
 * Simple abstraction of the interface for the original voice note recording and the new.
 */
interface Recorder {
    @Throws(IOException::class)
    fun start(context: Context, fileDescriptor: ParcelFileDescriptor)

    fun stop()
}