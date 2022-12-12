package com.lgt.cwm.ui.conversation.media

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnLongClickListener
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.IdRes
import com.lgt.cwm.R
import com.lgt.cwm.activity.media.models.MediaFileInfo
import com.lgt.cwm.databinding.AlbumThumbnailViewBinding
import com.lgt.cwm.models.conversation.Slide
import com.lgt.cwm.ui.glide.GlideRequests
import com.lgt.cwm.util.view.Stub
import cwmSignalMsgPb.CwmSignalMsg
import kotlin.math.min

class AlbumThumbnailView : FrameLayout {
    private var thumbnailClickListener: MediaClickListener? = null
    private var downloadClickListener: MediaListClickListener? = null
    private var currentSizeClass = 0
    private lateinit var albumCellContainer: ViewGroup
    private lateinit var transferControls: Stub<TransferControlView>

    private var fileList: List<CwmSignalMsg.MultimediaFileInfo> = listOf()

    private val defaultThumbnailClickListener = object : MediaClickListener {
        override fun onClick(
            v: View,
            mediaFile: CwmSignalMsg.MultimediaFileInfo,
            mediaFileList: List<CwmSignalMsg.MultimediaFileInfo>?
        ) {
            thumbnailClickListener?.onClick(v, mediaFile, fileList)
        }
    }

    private val defaultLongClickListener = OnLongClickListener { this.performLongClick() }

    constructor(context: Context) : super(context) {
        initialize()
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        initialize()
    }

    private fun initialize() {
        val binding: AlbumThumbnailViewBinding =
            AlbumThumbnailViewBinding.inflate(LayoutInflater.from(context), this, true);
        albumCellContainer = binding.albumCellContainer
        transferControls = binding.albumTransferControlsStub.viewStub?.let { Stub(it) }!!
    }

    fun setThumbnails(glideRequests: GlideRequests, listFileInfo: List<CwmSignalMsg.MultimediaFileInfo>, showControls: Boolean) {
        check(listFileInfo.size >= 2) { "Provided less than two files." }

        //TODO: show control
//        if (showControls) {
//            transferControls.get().setShowDownloadText(true)
//            transferControls.get().setSlides(slides)
//            transferControls.get().setDownloadClickListener { v ->
//                if (downloadClickListener != null) {
//                    downloadClickListener!!.onClick(v, slides)
//                }
//            }
//        } else {
//            if (transferControls.resolved()) {
//                transferControls.get().setVisibility(GONE)
//            }
//        }

        val sizeClass = sizeClass(listFileInfo.size)
        if (sizeClass != currentSizeClass) {
            inflateLayout(sizeClass)
            currentSizeClass = sizeClass
        }

        showThumbnails(glideRequests, listFileInfo)
//        showSlidesTest(glideRequests, sizeClass)
    }

    fun setCellBackgroundColor(@ColorInt color: Int) {
        val cellRoot = findViewById<ViewGroup>(R.id.album_thumbnail_root)
        if (cellRoot != null) {
            for (i in 0 until cellRoot.childCount) {
                cellRoot.getChildAt(i).setBackgroundColor(color)
            }
        }
    }

    fun setThumbnailClickListener(listener: MediaClickListener?) {
        thumbnailClickListener = listener
    }

    fun setDownloadClickListener(listener: MediaListClickListener?) {
        downloadClickListener = listener
    }

    private fun inflateLayout(sizeClass: Int) {
        albumCellContainer.removeAllViews()
        when (sizeClass) {
            2 -> inflate(context, R.layout.album_thumbnail_2, albumCellContainer)
            3 -> inflate(context, R.layout.album_thumbnail_3, albumCellContainer)
            4 -> inflate(context, R.layout.album_thumbnail_4, albumCellContainer)
            5 -> inflate(context, R.layout.album_thumbnail_5, albumCellContainer)
            else -> inflate(context, R.layout.album_thumbnail_many, albumCellContainer)
        }
    }

    private fun showThumbnails(glideRequests: GlideRequests, listFileInfo: List<CwmSignalMsg.MultimediaFileInfo>) {
        this.fileList = listFileInfo

        setThumbnail(glideRequests, listFileInfo[0], R.id.album_cell_1)
        setThumbnail(glideRequests, listFileInfo[1], R.id.album_cell_2)
        if (listFileInfo.size >= 3) {
            setThumbnail(glideRequests, listFileInfo[2], R.id.album_cell_3)
        }
        if (listFileInfo.size >= 4) {
            setThumbnail(glideRequests, listFileInfo[3], R.id.album_cell_4)
        }
        if (listFileInfo.size >= 5) {
            setThumbnail(glideRequests, listFileInfo[4], R.id.album_cell_5)
        }
        if (listFileInfo.size > 5) {
            val text = findViewById<TextView>(R.id.album_cell_overflow_text)
            text.text = context.getString(R.string.AlbumThumbnailView_plus, listFileInfo.size - 5)
        }
    }

    private fun showSlidesTest(glideRequests: GlideRequests, num: Int) {
//        setSlide(glideRequests, null, R.id.album_cell_1)
//        setSlide(glideRequests, null, R.id.album_cell_2)
//        if (num >= 3) {
//            setSlide(glideRequests, null, R.id.album_cell_3)
//        }
//        if (num >= 4) {
//            setSlide(glideRequests, null, R.id.album_cell_4)
//        }
//        if (num >= 5) {
//            setSlide(glideRequests, null, R.id.album_cell_5)
//        }
//        if (num > 5) {
//            val text = findViewById<TextView>(R.id.album_cell_overflow_text)
//            text.text = context.getString(R.string.AlbumThumbnailView_plus, num - 5)
//        }
    }

    private fun setThumbnail(glideRequests: GlideRequests, fileInfo: CwmSignalMsg.MultimediaFileInfo, @IdRes id: Int) {
        val cell: ThumbnailView = findViewById(id)
        cell.setImageResource(glideRequests, fileInfo, false, false)
        cell.setThumbnailClickListener(defaultThumbnailClickListener)
        cell.setOnLongClickListener(defaultLongClickListener)
    }

    private fun sizeClass(size: Int): Int {
        return min(size, 6)
    }
}