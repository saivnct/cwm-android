package com.lgt.cwm.ui.conversation.media

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.annotation.ColorInt
import androidx.annotation.UiThread
import com.lgt.cwm.R
import com.lgt.cwm.models.SignalMsgExt
import com.lgt.cwm.ui.components.CornerMask
import com.lgt.cwm.ui.components.Outliner
import com.lgt.cwm.ui.components.Projection
import com.lgt.cwm.ui.glide.GlideRequests
import com.lgt.cwm.util.ViewUtil
import cwmSignalMsgPb.CwmSignalMsg

class ConversationItemThumbnail : FrameLayout {
    private lateinit var thumbnail: ThumbnailView
    private lateinit var album: AlbumThumbnailView
    private lateinit var shade: ImageView
    private var cornerMask: CornerMask = CornerMask(this)
    private var pulseOutliner: Outliner? = null
    private var borderless = false
    private lateinit var normalBounds: IntArray
    private lateinit var gifBounds: IntArray
    private var minimumThumbnailWidth = 0

    constructor(context: Context) : super(context) {
        init(null)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(attrs)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle) {
        init(attrs)
    }

    private fun init(attrs: AttributeSet?) {
        inflate(context, R.layout.conversation_item_thumbnail, this)

        thumbnail = findViewById(R.id.conversation_thumbnail_image)
        album = findViewById(R.id.conversation_thumbnail_album)
        shade = findViewById(R.id.conversation_thumbnail_shade)

        var gifWidth: Int = ViewUtil.dpToPx(260)
        if (attrs != null) {
            val typedArray = context.theme.obtainStyledAttributes(attrs, R.styleable.ConversationItemThumbnail, 0, 0)
            normalBounds = intArrayOf(
                typedArray.getDimensionPixelSize(R.styleable.ConversationItemThumbnail_conversationThumbnail_minWidth, 0),
                typedArray.getDimensionPixelSize(R.styleable.ConversationItemThumbnail_conversationThumbnail_maxWidth, 0),
                typedArray.getDimensionPixelSize(R.styleable.ConversationItemThumbnail_conversationThumbnail_minHeight, 0),
                typedArray.getDimensionPixelSize(R.styleable.ConversationItemThumbnail_conversationThumbnail_maxHeight, 0)
            )
            gifWidth = typedArray.getDimensionPixelSize(R.styleable.ConversationItemThumbnail_conversationThumbnail_gifWidth, gifWidth)
            typedArray.recycle()
        } else {
            normalBounds = intArrayOf(0, 0, 0, 0)
        }
        gifBounds = intArrayOf(gifWidth, gifWidth, 1, Int.MAX_VALUE)
        minimumThumbnailWidth = -1
    }

    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)
        if (!borderless) {
            cornerMask.mask(canvas)
        }
        pulseOutliner?.draw(canvas)
    }

    fun hideThumbnailView() {
        thumbnail.alpha = 0f
    }

    fun showThumbnailView() {
        thumbnail.alpha = 1f
    }

    val corners = Projection.Corners(cornerMask.radii)

    fun setPulseOutliner(outliner: Outliner) {
        pulseOutliner = outliner
    }

    override fun setFocusable(focusable: Boolean) {
        thumbnail.isFocusable = focusable
        album.isFocusable = focusable
    }

    override fun setClickable(clickable: Boolean) {
        thumbnail.isClickable = clickable
        album.isClickable = clickable
    }

    override fun setOnLongClickListener(l: OnLongClickListener?) {
        thumbnail.setOnLongClickListener(l)
        album.setOnLongClickListener(l)
    }

    fun showShade(show: Boolean) {
        shade.visibility = if (show) VISIBLE else GONE
        forceLayout()
    }

    fun setCorners(topLeft: Int, topRight: Int, bottomRight: Int, bottomLeft: Int) {
        cornerMask.setRadii(topLeft, topRight, bottomRight, bottomLeft)
    }

    fun setMinimumThumbnailWidth(width: Int) {
        minimumThumbnailWidth = width
        thumbnail.setMinimumThumbnailWidth(width)
    }

    fun setBorderless(borderless: Boolean) {
        this.borderless = borderless
    }

    @UiThread
    fun setImageResource(glideRequests: GlideRequests, message: SignalMsgExt, showControls: Boolean, isPreview: Boolean) {
        message.contentSignalMultimediaMessage?.multimediaFileInfosList?.let { it ->
            if (it.size == 1) {
                thumbnail.visibility = VISIBLE
                album.visibility = GONE
                thumbnail.setImageResource(glideRequests, it.first(), showControls, isPreview)
                touchDelegate = thumbnail.touchDelegate
            } else {
                thumbnail.visibility = GONE
                album.visibility = VISIBLE

                album.setThumbnails(glideRequests, it, showControls)
                touchDelegate = album.touchDelegate
            }

        }
    }

    fun setConversationColor(@ColorInt color: Int) {
        if (album.visibility == VISIBLE) {
            album.setCellBackgroundColor(color)
        }
    }

    fun setThumbnailClickListener(listener: MediaClickListener) {
        thumbnail.setThumbnailClickListener(listener)
        album.setThumbnailClickListener(listener)
    }

    fun setDownloadClickListener(listener: MediaListClickListener) {
        thumbnail.setDownloadClickListener(listener)
        album.setDownloadClickListener(listener)
    }

    private fun setThumbnailBounds(bounds: IntArray) {
        thumbnail.setBounds(bounds[0], bounds[1], bounds[2], bounds[3])
    }
}