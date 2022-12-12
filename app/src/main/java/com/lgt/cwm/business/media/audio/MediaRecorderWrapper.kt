package com.lgt.cwm.business.media.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.ParcelFileDescriptor
import android.util.Log
import java.io.IOException

/**
 * Wrap Android's [MediaRecorder] for use with voice notes.
 */
class MediaRecorderWrapper : Recorder {

    private var recorder: MediaRecorder? = null

    companion object {
        private val TAG = MediaRecorderWrapper::class.simpleName.toString()
        private const val SAMPLE_RATE = 44100
        private const val CHANNELS = 1
        private const val BIT_RATE = 32000
    }

    @Throws(IOException::class)
    override fun start(context: Context, fileDescriptor: ParcelFileDescriptor) {
        Log.i(TAG, "Recording voice note using MediaRecorderWrapper.")

        recorder = MediaRecorder()

        try {
            recorder?.let {
                it.setAudioSource(MediaRecorder.AudioSource.MIC)
                it.setOutputFormat(MediaRecorder.OutputFormat.AAC_ADTS)
                it.setOutputFile(fileDescriptor.fileDescriptor)
                it.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                it.setAudioSamplingRate(SAMPLE_RATE)
                it.setAudioEncodingBitRate(BIT_RATE)
                it.setAudioChannels(CHANNELS)
                it.prepare()
                it.start()
            }
        } catch (e: IllegalStateException) {
            Log.w(TAG, "Unable to start recording", e)
            recorder?.release()
            recorder = null
            throw IOException(e)
        }
    }

    override fun stop() {
        if (recorder == null) {
            return
        }
        try {
            recorder?.stop()
        } catch (e: RuntimeException) {
            if (e.javaClass != RuntimeException::class.java) {
                throw e
            } else {
                Log.d(TAG, "Recording stopped with no data captured.")
            }
        } finally {
            recorder?.release()
            recorder = null
        }
    }


}