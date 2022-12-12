package com.lgt.cwm.ui.conversation

import android.content.Context
import android.content.res.TypedArray
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.lifecycle.Observer
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.SimpleColorFilter
import com.airbnb.lottie.model.KeyPath
import com.airbnb.lottie.value.LottieValueCallback
import com.lgt.cwm.R
import com.lgt.cwm.activity.media.models.MediaFileInfo
import com.lgt.cwm.models.conversation.audio.AudioWaveForm
import com.lgt.cwm.ui.components.AnimatingToggle
import com.lgt.cwm.ui.components.WaveFormSeekBarView
import com.lgt.cwm.ui.components.voice.VoiceNotePlaybackState
import com.lgt.cwm.ui.conversation.media.MediaClickListener
import com.pnikosis.materialishprogress.ProgressWheel
import cwmSignalMsgPb.CwmSignalMsg
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.roundToLong

class AudioView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : FrameLayout(context, attrs, defStyleAttr) {
    private lateinit var controlToggle: AnimatingToggle
    private lateinit var progressAndPlay: View
    private lateinit var playPauseButton: LottieAnimationView
    private lateinit var downloadButton: ImageView
    private var circleProgress: ProgressWheel? = null
    private lateinit var seekBar: SeekBar
    private var smallView = false
    private var autoRewind = false
    private var duration: TextView? = null

    @ColorInt private var waveFormPlayedBarsColor = 0
    @ColorInt private var waveFormUnplayedBarsColor = 0
    @ColorInt private var waveFormThumbTint = 0

    private var downloadListener: MediaClickListener? = null
    private var backwardsCounter = 0
    private var lottieDirection = 0
    private var isPlaying = false
    private var durationMillis: Long = 0
    private var audioFile: CwmSignalMsg.MultimediaFileInfo? = null
    private var callbacks: Callbacks? = null
    val playbackStateObserver: Observer<VoiceNotePlaybackState> = Observer<VoiceNotePlaybackState> { onPlaybackState(it) }

    companion object {
        private val TAG: String = AudioView::class.java.simpleName.toString()
        private const val MODE_NORMAL = 0
        private const val MODE_SMALL = 1
        private const val MODE_DRAFT = 2
        private const val FORWARDS = 1
        private const val REVERSE = -1
    }

    init {
        var typedArray: TypedArray? = null
        try {
            typedArray = context.theme.obtainStyledAttributes(attrs, R.styleable.AudioView, 0, 0)
            val mode = typedArray.getInteger(R.styleable.AudioView_audioView_mode, MODE_NORMAL)
            smallView = mode == MODE_SMALL
            autoRewind = typedArray.getBoolean(R.styleable.AudioView_autoRewind, false)
            when (mode) {
                MODE_NORMAL -> inflate(context, R.layout.audio_view, this)
                MODE_SMALL -> inflate(context, R.layout.audio_view_small, this)
                MODE_DRAFT -> inflate(context, R.layout.audio_view_draft, this)
                else -> throw IllegalStateException("Unsupported mode: $mode")
            }
            controlToggle = findViewById(R.id.control_toggle)
            playPauseButton = findViewById(R.id.play)
            progressAndPlay = findViewById(R.id.progress_and_play)
            downloadButton = findViewById(R.id.download)
            circleProgress = findViewById(R.id.circle_progress)
            seekBar = findViewById(R.id.seek)
            duration = findViewById(R.id.duration)
            lottieDirection = REVERSE
            playPauseButton.setOnClickListener(PlayPauseClickedListener())
            playPauseButton.setOnLongClickListener { performLongClick() }
            seekBar.setOnSeekBarChangeListener(SeekBarModifiedListener())
            setTint(typedArray.getColor(R.styleable.AudioView_foregroundTintColor, Color.WHITE))
            waveFormPlayedBarsColor = typedArray.getColor(R.styleable.AudioView_waveformPlayedBarsColor, Color.WHITE)
            waveFormUnplayedBarsColor = typedArray.getColor(R.styleable.AudioView_waveformUnplayedBarsColor, Color.WHITE)
            waveFormThumbTint = typedArray.getColor(R.styleable.AudioView_waveformThumbTint, Color.WHITE)
            setProgressAndPlayBackgroundTint(typedArray.getColor(R.styleable.AudioView_progressAndPlayTint, Color.BLACK))
        } finally {
            typedArray?.recycle()
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
//        if (!EventBus.getDefault().isRegistered(this)) EventBus.getDefault().register(this)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
//        EventBus.getDefault().unregister(this)
    }

    fun setProgressAndPlayBackgroundTint(@ColorInt color: Int) {
        progressAndPlay.background.setColorFilter(color, PorterDuff.Mode.SRC_IN)
    }

    fun setAudio(audio: CwmSignalMsg.MultimediaFileInfo, callbacks: Callbacks?, showControls: Boolean, forceHideDuration: Boolean) {
        this.callbacks = callbacks
        duration?.visibility = VISIBLE

        if (seekBar is WaveFormSeekBarView) {
            audioFile?.let {
                if (!Objects.equals(it.fileUri, audio.fileUri)) {
                    val waveFormView = seekBar as WaveFormSeekBarView
                    waveFormView.setWaveMode(false)
                    seekBar.progress = 0
                    durationMillis = 0
                }
            }

        }
        //check pending download
        if (showControls) {
            controlToggle.displayQuick(downloadButton)
            seekBar.isEnabled = false
            downloadButton.setOnClickListener(DownloadClickedListener(audio))
            circleProgress?.let {
                if (it.isSpinning) it.stopSpinning()
                it.visibility = GONE
            }
            //check transfer state start
        } else if (showControls) {
            controlToggle.displayQuick(progressAndPlay)
            seekBar.isEnabled = false
            circleProgress?.let {
                it.visibility = VISIBLE
                it.spin()
            }
        } else {
            seekBar.isEnabled = true
            circleProgress?.let {
                if (it.isSpinning) it.stopSpinning()
            }
            showPlayButton()
        }

        audioFile = audio

        if (seekBar is WaveFormSeekBarView) {
            val waveFormView = seekBar as WaveFormSeekBarView
            waveFormView.setColors(waveFormPlayedBarsColor, waveFormUnplayedBarsColor, waveFormThumbTint)
            if (Build.VERSION.SDK_INT >= 23) {
                AudioWaveForm(context, audio).getWaveForm(
                    { data ->
                        durationMillis = data.getDuration(TimeUnit.MILLISECONDS)
                        updateProgress(0f, 0)
                        if (!forceHideDuration) {
                            duration?.visibility = VISIBLE
                        }
                        waveFormView.setWaveData(data.waveForm)
                    }
                ) { waveFormView.setWaveMode(false) }
            } else {
                waveFormView.setWaveMode(false)
                duration?.visibility = GONE
            }
        }
        if (forceHideDuration) {
            duration?.visibility = GONE
        }
    }

    fun setDownloadClickListener(listener: MediaClickListener?) {
        downloadListener = listener
    }

    val audioFileUri: Uri?
        get() = if (audioFile != null) Uri.parse(audioFile!!.fileUri) else null

    private fun onPlaybackState(voiceNotePlaybackState: VoiceNotePlaybackState) {
        onDuration(voiceNotePlaybackState.uri, voiceNotePlaybackState.trackDuration)
        onProgress(voiceNotePlaybackState.uri, voiceNotePlaybackState.playheadPositionMillis.toDouble() / voiceNotePlaybackState.trackDuration, voiceNotePlaybackState.playheadPositionMillis)
        onSpeedChanged(voiceNotePlaybackState.uri, voiceNotePlaybackState.speed)
        onStart(voiceNotePlaybackState.uri, voiceNotePlaybackState.isPlaying, voiceNotePlaybackState.isAutoReset)
    }

    private fun onDuration(uri: Uri, durationMillis: Long) {
        if (isTarget(uri)) {
            this.durationMillis = durationMillis
        }
    }

    private fun onStart(uri: Uri, statePlaying: Boolean, autoReset: Boolean) {
        if (!isTarget(uri) || !statePlaying) {
            if (hasAudioUri()) {
                audioFileUri?.let { onStop(it, autoReset) }
            }
            return
        }
        if (isPlaying) {
            return
        }
        isPlaying = true
        togglePlayToPause()
    }

    private fun onStop(uri: Uri, autoReset: Boolean) {
        if (!isTarget(uri)) {
            return
        }
        if (!isPlaying) {
            return
        }
        isPlaying = false
        togglePauseToPlay()
        if (autoReset || autoRewind || seekBar.progress + 5 >= seekBar.max) {
            backwardsCounter = 4
            rewind()
        }
    }

    private fun onProgress(uri: Uri, progress: Double, millis: Long) {
        if (!isTarget(uri)) {
            return
        }
        val seekProgress = floor(progress * seekBar.max).toInt()
        if (seekProgress > seekBar.progress || backwardsCounter > 3) {
            backwardsCounter = 0
            seekBar.progress = seekProgress
            updateProgress(progress.toFloat(), millis)
        } else {
            backwardsCounter++
        }
    }

    private fun onSpeedChanged(uri: Uri, speed: Float) {
        callbacks?.onSpeedChanged(speed, isTarget(uri))
    }

    private fun isTarget(uri: Uri): Boolean {
        return hasAudioUri() && (uri == audioFileUri)
    }

    private fun hasAudioUri(): Boolean {
        return audioFileUri != null
    }

    override fun setFocusable(focusable: Boolean) {
        super.setFocusable(focusable)
        playPauseButton.isFocusable = focusable
        seekBar.isFocusable = focusable
        seekBar.isFocusableInTouchMode = focusable
        downloadButton.isFocusable = focusable
    }

    override fun setClickable(clickable: Boolean) {
        super.setClickable(clickable)
        playPauseButton.isClickable = clickable
        seekBar.isClickable = clickable
        seekBar.setOnTouchListener(if (clickable) null else TouchIgnoringListener())
        downloadButton.isClickable = clickable
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        playPauseButton.isEnabled = enabled
        seekBar.isEnabled = enabled
        downloadButton.isEnabled = enabled
    }

    private fun updateProgress(progress: Float, millis: Long) {
        callbacks?.onProgressUpdated(durationMillis, millis)

        duration?.let {
            if (durationMillis > 0) {
                val remainingSecs = max(0, TimeUnit.MILLISECONDS.toSeconds(durationMillis - millis))
                it.text = resources.getString(R.string.AudioView_duration, remainingSecs / 60, remainingSecs % 60)
            }
        }

        if (smallView) {
            circleProgress?.setInstantProgress((if (seekBar.progress == 0) 1 else progress) as Float)
        }
    }

    fun setTint(foregroundTint: Int) {
        post {
            playPauseButton.addValueCallback(KeyPath("**"), LottieProperty.COLOR_FILTER, LottieValueCallback(SimpleColorFilter(foregroundTint)))
        }
        downloadButton.setColorFilter(foregroundTint, PorterDuff.Mode.SRC_IN)
        circleProgress?.barColor = foregroundTint
        duration?.setTextColor(foregroundTint)
        seekBar.progressDrawable.setColorFilter(foregroundTint, PorterDuff.Mode.SRC_IN)
        seekBar.thumb.setColorFilter(foregroundTint, PorterDuff.Mode.SRC_IN)
    }

    fun getSeekBarGlobalVisibleRect(rect: Rect) {
        seekBar.getGlobalVisibleRect(rect)
    }

    private val progress: Double
        get() = if (seekBar.progress <= 0 || seekBar.max <= 0) {
            0.0
        } else {
            seekBar.progress.toDouble() / seekBar.max.toDouble()
        }

    private fun togglePlayToPause() {
        startLottieAnimation(FORWARDS)
    }

    private fun togglePauseToPlay() {
        startLottieAnimation(REVERSE)
    }

    private fun startLottieAnimation(direction: Int) {
        showPlayButton()
        if (lottieDirection == direction) {
            return
        }
        lottieDirection = direction
        playPauseButton.pauseAnimation()
        playPauseButton.speed = (direction * 2).toFloat()
        playPauseButton.resumeAnimation()
    }

    private fun showPlayButton() {
        if (!smallView) {
            circleProgress?.visibility = GONE
        } else if (seekBar.progress == 0) {
            circleProgress?.setInstantProgress(1f)
        }
        playPauseButton.visibility = VISIBLE
        controlToggle.displayQuick(progressAndPlay)
    }

    fun stopPlaybackAndReset() {
        if (audioFileUri == null) return
        callbacks?.let {
            it.onStopAndReset(audioFileUri!!)
            rewind()
        }
    }

    private inner class PlayPauseClickedListener : OnClickListener {
        override fun onClick(v: View) {
            if (audioFileUri == null) return
            callbacks?.let {
                if (lottieDirection == REVERSE) {
                    it.onPlay(audioFileUri!!, progress)
                } else {
                    it.onPause(audioFileUri!!)
                }
            }
        }
    }

    private fun rewind() {
        seekBar.progress = 0
        updateProgress(0f, 0)
    }

    private inner class DownloadClickedListener constructor(file: CwmSignalMsg.MultimediaFileInfo) : OnClickListener {
        private val file: CwmSignalMsg.MultimediaFileInfo

        init {
            this.file = file
        }
        override fun onClick(v: View) {
            downloadListener?.onClick(v, file)
        }
    }

    private inner class SeekBarModifiedListener : OnSeekBarChangeListener {
        private var wasPlaying = false
        override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {}

        @Synchronized
        override fun onStartTrackingTouch(seekBar: SeekBar) {
            if (audioFile == null || audioFile!!.fileUri == null) return
            wasPlaying = isPlaying
            if (isPlaying) {
                callbacks?.onPause(Uri.parse(audioFile!!.fileUri))
            }
        }

        @Synchronized
        override fun onStopTrackingTouch(seekBar: SeekBar) {
            if (audioFile == null || audioFile!!.fileUri == null) return
            callbacks?.let {
                if (wasPlaying) {
                    it.onSeekTo(Uri.parse(audioFile!!.fileUri), progress)
                } else {
                    it.onProgressUpdated(durationMillis, (durationMillis * progress).roundToLong())
                }
            }
        }
    }

    private class TouchIgnoringListener : OnTouchListener {
        override fun onTouch(v: View, event: MotionEvent): Boolean { return true }
    }

//    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
//    fun onEventAsync(event: PartProgressEvent) {
//        if (audioSlide != null && circleProgress != null && event.attachment.equals(audioSlide.asAttachment())) {
//            circleProgress.setInstantProgress(event.progress as Float / event.total)
//        }
//    }

    interface Callbacks {
        fun onPlay(audioUri: Uri, progress: Double)
        fun onPause(audioUri: Uri)
        fun onSeekTo(audioUri: Uri, progress: Double)
        fun onStopAndReset(audioUri: Uri)
        fun onSpeedChanged(speed: Float, isPlaying: Boolean)
        fun onProgressUpdated(durationMillis: Long, playheadMillis: Long)
    }
}