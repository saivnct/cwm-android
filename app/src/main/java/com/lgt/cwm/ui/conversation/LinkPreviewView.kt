package com.lgt.cwm.ui.conversation

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.annotation.StringRes
import com.lgt.cwm.R
import com.lgt.cwm.business.media.linkpreview.LinkPreview
import com.lgt.cwm.business.media.linkpreview.LinkPreviewRepository
import com.lgt.cwm.databinding.LinkPreviewBinding
import com.lgt.cwm.ui.components.CornerMask
import com.lgt.cwm.ui.conversation.media.MediaListClickListener
import com.lgt.cwm.ui.conversation.media.OutlinedThumbnailView
import com.lgt.cwm.ui.glide.GlideRequests
import com.lgt.cwm.util.ViewUtil
import okhttp3.HttpUrl
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

class LinkPreviewView : FrameLayout {
    private lateinit var container: ViewGroup
    private lateinit var thumbnail: OutlinedThumbnailView
    private lateinit var title: TextView
    private lateinit var description: TextView
    private lateinit var site: TextView
    private lateinit var divider: View
    private lateinit var closeButton: View
    private lateinit var spinner: View
    private lateinit var noPreview: TextView
    private var type = 0
    private var defaultRadius = 0
    private lateinit var cornerMask: CornerMask
    private var closeClickedListener: CloseClickedListener? = null

    companion object {
        private const val TYPE_CONVERSATION = 0
        private const val TYPE_COMPOSE = 1

        private fun formatDate(date: Long): String {
            val dateFormat: DateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            return dateFormat.format(date)
        }
    }

    constructor(context: Context) : super(context) {
        init(null)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(attrs)
    }

    private fun init(attrs: AttributeSet?) {
        val binding = LinkPreviewBinding.inflate(LayoutInflater.from(context), this, true)

        container = binding.linkpreviewContainer
        thumbnail = binding.linkpreviewThumbnail
        title = binding.linkpreviewTitle
        description = binding.linkpreviewDescription
        site = binding.linkpreviewSite
        divider = binding.linkpreviewDivider
        spinner = binding.linkpreviewProgressWheel
        closeButton = binding.linkpreviewClose
        noPreview = binding.linkpreviewNoPreview
        defaultRadius = resources.getDimensionPixelSize(R.dimen.thumbnail_default_radius)
        cornerMask = CornerMask(this)

        if (attrs != null) {
            val typedArray = context.theme.obtainStyledAttributes(attrs, R.styleable.LinkPreviewView, 0, 0)
            type = typedArray.getInt(R.styleable.LinkPreviewView_linkpreview_type, 0)
            typedArray.recycle()
        }

        if (type == TYPE_COMPOSE) {
            container.setBackgroundColor(Color.TRANSPARENT)
            container.setPadding(0, 0, 0, 0)
            divider.visibility = VISIBLE
            closeButton.visibility = VISIBLE
            title.maxLines = 2
            description.maxLines = 2
            closeButton.setOnClickListener {
                closeClickedListener?.onCloseClicked()
            }
        }

        setWillNotDraw(false)
    }

    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)
        if (type == TYPE_COMPOSE) return

        cornerMask.mask(canvas)
    }

    fun setLoading() {
        title.visibility = GONE
        site.visibility = GONE
        description.visibility = GONE
        thumbnail.visibility = GONE
        spinner.visibility = VISIBLE
        noPreview.visibility = INVISIBLE
    }

    fun setNoPreview(customError: LinkPreviewRepository.Error?) {
        title.visibility = GONE
        site.visibility = GONE
        thumbnail.visibility = GONE
        spinner.visibility = GONE
        noPreview.visibility = VISIBLE
        noPreview.setText(getLinkPreviewErrorString(customError))
    }

    fun setLinkPreview(glideRequests: GlideRequests, linkPreview: LinkPreview, showThumbnail: Boolean) {
        spinner.visibility = GONE
        noPreview.visibility = GONE
        if (linkPreview.title.isNotEmpty()) {
            title.text = linkPreview.title
            title.visibility = VISIBLE
        } else {
            title.visibility = GONE
        }
        if (linkPreview.description.isNotEmpty()) {
            description.text = linkPreview.description
            description.visibility = VISIBLE
        } else {
            description.visibility = GONE
        }
        var domain: String? = null
        if (linkPreview.url.isNotEmpty()) {
            val url = HttpUrl.parse(linkPreview.url)
            if (url != null) {
                domain = url.topPrivateDomain()
            }
        }
        if (domain != null && linkPreview.date > 0) {
            site.text = context.getString(R.string.LinkPreviewView_domain_date, domain, formatDate(linkPreview.date.toLong()))
            site.visibility = VISIBLE
        } else if (domain != null) {
            site.text = domain
            site.visibility = VISIBLE
        } else if (linkPreview.date > 0) {
            site.text = formatDate(linkPreview.date.toLong())
            site.visibility = VISIBLE
        } else {
            site.visibility = GONE
        }
        if (showThumbnail && !linkPreview.thumbnail.isNullOrEmpty()) {
            thumbnail.visibility = VISIBLE
            thumbnail.setImageResource(glideRequests, linkPreview.thumbnail!!)
            thumbnail.showDownloadText(false)
        } else {
            thumbnail.visibility = GONE
        }
    }

    fun setCorners(topStart: Int, topEnd: Int) {
        if (ViewUtil.isRtl(this)) {
            cornerMask.setRadii(topEnd, topStart, 0, 0)
            thumbnail.setCorners(defaultRadius, topEnd, defaultRadius, defaultRadius)
        } else {
            cornerMask.setRadii(topStart, topEnd, 0, 0)
            thumbnail.setCorners(topStart, defaultRadius, defaultRadius, defaultRadius)
        }
        postInvalidate()
    }

    @StringRes
    private fun getLinkPreviewErrorString(customError: LinkPreviewRepository.Error?): Int {
        return R.string.LinkPreviewView_no_link_preview_available
    }

    fun setCloseClickedListener(closeClickedListener: CloseClickedListener?) {
        this.closeClickedListener = closeClickedListener
    }

    fun setDownloadClickedListener(listener: MediaListClickListener) {
        thumbnail.setDownloadClickListener(listener)
    }

    interface CloseClickedListener {
        fun onCloseClicked()
    }

}