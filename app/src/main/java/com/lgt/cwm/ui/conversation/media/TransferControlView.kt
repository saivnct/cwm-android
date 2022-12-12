package com.lgt.cwm.ui.conversation.media

import android.animation.LayoutTransition
import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.lgt.cwm.R
import com.lgt.cwm.business.events.PartProgressEvent
import com.lgt.cwm.databinding.TransferControlsViewBinding
import com.lgt.cwm.db.entity.SignalMsg
import com.pnikosis.materialishprogress.ProgressWheel
import cwmSignalMsgPb.CwmSignalMsg
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class TransferControlView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : FrameLayout(context, attrs, defStyleAttr) {
    private var files: List<CwmSignalMsg.MultimediaFileInfo>? = null
    private var current: View? = null
    private var progressWheel: ProgressWheel
    private var downloadDetails: View
    private var downloadDetailsText: TextView

    companion object {

    }

    init {
        val binding: TransferControlsViewBinding =
            TransferControlsViewBinding.inflate(LayoutInflater.from(context), this, true);
        isLongClickable = false
        background = ContextCompat.getDrawable(context, R.drawable.transfer_controls_background)
        visibility = GONE
        layoutTransition = LayoutTransition()

        progressWheel = binding.progressWheel
        downloadDetails = binding.downloadDetails
        downloadDetailsText = binding.downloadDetailsText

    }

    override fun setFocusable(focusable: Boolean) {
        super.setFocusable(focusable)
        downloadDetails.isFocusable = focusable
    }

    override fun setClickable(clickable: Boolean) {
        super.setClickable(clickable)
        downloadDetails.isClickable = clickable
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (!EventBus.getDefault().isRegistered(this)) EventBus.getDefault().register(this)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        EventBus.getDefault().unregister(this)
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun onEventAsync(event: PartProgressEvent) {
        setMultimediaFiles(listOf(event.fileInfo))
    }

    fun setMultimediaFile(file: CwmSignalMsg.MultimediaFileInfo) {
        setMultimediaFiles(listOf(file))
    }

    fun setMultimediaFiles(files: List<CwmSignalMsg.MultimediaFileInfo>) {
        require(files.isNotEmpty()) { "Must provide at least one file." }
        this.files = files

        when (files[0].fileStatus) {
            CwmSignalMsg.SIGNAL_MEDIA_FILE_STATUS.SENDING,
            CwmSignalMsg.SIGNAL_MEDIA_FILE_STATUS.DOWNLOADING -> showProgressSpinner()

            CwmSignalMsg.SIGNAL_MEDIA_FILE_STATUS.SEND_FAILED,
            CwmSignalMsg.SIGNAL_MEDIA_FILE_STATUS.DOWNLOAD_FAILED -> {
                downloadDetailsText.text = "Failed. Tap retry"
                display(downloadDetails)
            }

            else -> {
                display(null)
            }
        }
    }

    fun showProgressSpinner() {
        progressWheel.spin()
        display(progressWheel)
    }

    fun setDownloadClickListener(listener: OnClickListener?) {
        downloadDetails.setOnClickListener(listener)
    }

    fun clear() {
        clearAnimation()
        visibility = GONE
        current?.clearAnimation()
        current?.visibility = GONE
        current = null
        files = null
    }

    fun setShowDownloadText(showDownloadText: Boolean) {
        downloadDetailsText.visibility = if (showDownloadText) VISIBLE else GONE
        forceLayout()
    }

    private fun display(view: View?) {
        if (current == view) {
            return
        }
        current?.visibility = GONE

        if (view != null) {
            view.visibility = VISIBLE
            visibility = VISIBLE
        } else {
            visibility = GONE
        }

        current = view
    }

}