package com.lgt.cwm.ui.components.voice

import android.content.Context
import com.google.android.exoplayer2.*

class VoiceNotePlayer @JvmOverloads constructor(
    context: Context,
    val internalPlayer: ExoPlayer = ExoPlayer.Builder(context)
        .setLoadControl(
            DefaultLoadControl.Builder()
                .setBufferDurationsMs(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE)
                .build()
        ).build()
) : ForwardingPlayer(internalPlayer) {

    override fun seekTo(windowIndex: Int, positionMs: Long) {
        super.seekTo(windowIndex, positionMs)

        val isQueueToneIndex = windowIndex % 2 == 1
        val isSeekingToStart = positionMs == C.TIME_UNSET

        return if (isQueueToneIndex && isSeekingToStart) {
            val nextVoiceNoteWindowIndex = if (currentWindowIndex < windowIndex) windowIndex + 1 else windowIndex - 1
            if (mediaItemCount <= nextVoiceNoteWindowIndex) {
                super.seekTo(windowIndex, positionMs)
            } else {
                super.seekTo(nextVoiceNoteWindowIndex, positionMs)
            }
        } else {
            super.seekTo(windowIndex, positionMs)
        }
    }
}
