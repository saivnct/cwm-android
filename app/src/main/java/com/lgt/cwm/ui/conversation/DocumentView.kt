package com.lgt.cwm.ui.conversation

import android.content.Context
import android.graphics.Color
import android.graphics.PorterDuff
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.AttrRes
import com.lgt.cwm.R
import com.lgt.cwm.databinding.DocumentViewBinding
import com.lgt.cwm.models.conversation.Slide
import com.lgt.cwm.ui.components.AnimatingToggle
import com.lgt.cwm.ui.conversation.media.MediaClickListener
import com.lgt.cwm.util.Util
import com.pnikosis.materialishprogress.ProgressWheel
import cwmSignalMsgPb.CwmSignalMsg
import java.util.*

class DocumentView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, @AttrRes defStyleAttr: Int = 0) : FrameLayout(context, attrs, defStyleAttr) {
    private val controlToggle: AnimatingToggle
    private val downloadButton: ImageView
    private val downloadProgress: ProgressWheel
    private val container: View
    private val iconContainer: ViewGroup
    private val fileName: TextView
    private val fileSize: TextView
    private val document: TextView
    private var downloadListener: MediaClickListener? = null
    private var viewListener: MediaClickListener? = null
    private var documentSlide: Slide? = null

    init {
        val binding: DocumentViewBinding =
            DocumentViewBinding.inflate(LayoutInflater.from(context), this, true)
        container = binding.documentContainer
        iconContainer = binding.iconContainer
        controlToggle = binding.controlToggle
        downloadButton = binding.download
        downloadProgress = binding.downloadProgress
        fileName = binding.fileName
        fileSize = binding.fileSize
        document = binding.document

        if (attrs != null) {
            val typedArray = getContext().theme.obtainStyledAttributes(attrs, R.styleable.DocumentView, 0, 0)
            val titleColor = typedArray.getInt(R.styleable.DocumentView_doc_titleColor, Color.BLACK)
            val captionColor = typedArray.getInt(R.styleable.DocumentView_doc_captionColor, Color.BLACK)
            val downloadTint = typedArray.getInt(R.styleable.DocumentView_doc_downloadButtonTint, Color.WHITE)
            typedArray.recycle()

            fileName.setTextColor(titleColor)
            fileSize.setTextColor(captionColor)
            downloadButton.setColorFilter(downloadTint, PorterDuff.Mode.MULTIPLY)
            downloadProgress.barColor = downloadTint
        }
    }

    fun setDownloadClickListener(listener: MediaClickListener?) {
        downloadListener = listener
    }

    fun setDocumentClickListener(listener: MediaClickListener?) {
        viewListener = listener
    }

    fun setDocument(fileInfo: CwmSignalMsg.MultimediaFileInfo, showControls: Boolean?) {
        fileName.text = fileInfo.fileName
        fileSize.text = Util.getPrettyFileSize(fileInfo.fileSize)
        document.text = getFileType(fileInfo.fileName).uppercase(Locale.getDefault())
        controlToggle.displayQuick(iconContainer)

        setOnClickListener(OpenClickedListener(fileInfo))
//        controlToggle.displayQuick(downloadButton)
//        if (showControls && documentSlide.isPendingDownload()) {
//            controlToggle.displayQuick(downloadButton)
//            downloadButton.setOnClickListener(
//                DownloadClickedListener(
//                    documentSlide
//                )
//            )
//            if (downloadProgress.isSpinning) downloadProgress.stopSpinning()
//        } else if (showControls && documentSlide.getTransferState() === AttachmentDatabase.TRANSFER_PROGRESS_STARTED) {
//            controlToggle.displayQuick(downloadProgress)
//            downloadProgress.spin()
//        } else {
//            controlToggle.displayQuick(iconContainer)
//            if (downloadProgress.isSpinning) downloadProgress.stopSpinning()
//        }
//        this.documentSlide = documentSlide
//        fileName.setText(
//            OptionalUtil.or(
//                documentSlide.getFileName(),
//                documentSlide.getCaption()
//            )
//                .orElse(context.getString(R.string.DocumentView_unnamed_file))
//        )
//        fileSize.setText(Util.getPrettyFileSize(documentSlide.getFileSize()))
//        document.setText(documentSlide.getFileType(context).orElse("").toLowerCase())
//        setOnClickListener(
//            org.thoughtcrime.securesms.components.DocumentView.OpenClickedListener(
//                documentSlide
//            )
//        )
    }

    override fun setFocusable(focusable: Boolean) {
        super.setFocusable(focusable)
        downloadButton.isFocusable = focusable
    }

    override fun setClickable(clickable: Boolean) {
        super.setClickable(clickable)
        downloadButton.isClickable = clickable
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        downloadButton.isEnabled = enabled
    }

    private fun getFileType(fileName: String): String {
        if (fileName.isEmpty()) return ""
        val parts = fileName.split(".").toTypedArray()
        if (parts.size < 2) {
            return ""
        }
        val suffix = parts[parts.size - 1]
        return if (suffix.length <= 4) {
            suffix
        } else ""
    }

//    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
//    fun onEventAsync(event: PartProgressEvent) {
//        if (documentSlide != null && event.attachment.equals(documentSlide.asAttachment())) {
//            downloadProgress.setInstantProgress(event.progress as Float / event.total)
//        }
//    }
//
    inner class DownloadClickedListener private constructor(fileInfo: CwmSignalMsg.MultimediaFileInfo) : OnClickListener {
        private val fileInfo: CwmSignalMsg.MultimediaFileInfo
        init {
            this.fileInfo = fileInfo
        }
        override fun onClick(v: View) {
            downloadListener?.onClick(v, fileInfo)
        }
    }

    inner class OpenClickedListener constructor(fileInfo: CwmSignalMsg.MultimediaFileInfo) : OnClickListener {
        private val fileInfo: CwmSignalMsg.MultimediaFileInfo
        init {
            this.fileInfo = fileInfo
        }
        override fun onClick(v: View) {
            viewListener?.onClick(v, fileInfo)
//            if (fileInfo.fileStatus && !slide.isInProgress() && viewListener != null) {
//                viewListener?.onClick(v, fileInfo)
//            }
        }
    }
}