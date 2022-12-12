package com.lgt.cwm.ui.conversation

import android.content.Context
import android.graphics.Canvas
import android.net.Uri
import android.text.TextUtils
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.shape.CornerFamily
import com.google.android.material.shape.ShapeAppearanceModel
import com.lgt.cwm.R
import com.lgt.cwm.databinding.QuoteViewBinding
import com.lgt.cwm.models.SignalMsgExt
import com.lgt.cwm.ui.components.CornerMask
import com.lgt.cwm.ui.components.Projection
import com.lgt.cwm.ui.glide.GlideRequests
import com.lgt.cwm.util.ViewUtil
import cwmSignalMsgPb.CwmSignalMsg


class QuoteView : FrameLayout {
    private val TAG = QuoteView::class.simpleName.toString()

    enum class MessageType(code: Int) {
        // These codes must match the values for the QuoteView_message_type XML attribute.
        PREVIEW(0), OUTGOING(1), INCOMING(2), STORY_REPLY_OUTGOING(3);

        companion object {
            fun fromCode(code: Int): MessageType {
                for (value in values()) {
                    if (value.ordinal == code) {
                        return value
                    }
                }
                throw IllegalArgumentException("Unsupported code $code")
            }
        }
    }

    private lateinit var background: View
    private lateinit var mainView: ViewGroup
    private lateinit var authorView: TextView
    private lateinit var bodyView: TextView
    private lateinit var quoteBarView: View
    private lateinit var thumbnailView: ShapeableImageView
    private lateinit var attachmentVideoOverlayView: View
    private lateinit var attachmentContainerView: ViewGroup
    private lateinit var attachmentNameView: TextView
    private lateinit var dismissView: ImageView

    private lateinit var mediaDescriptionText: TextView

    var quoteId: String = ""
    var author: String = ""
    var isSelf = true
    var body: CharSequence? = null
    var attachments: List<CwmSignalMsg.MultimediaFileInfo> = emptyList()

    private var message: SignalMsgExt? = null
    private var largeCornerRadius: Int = 0
    private var smallCornerRadius: Int = 0
    private lateinit var cornerMask: CornerMask
    private var thumbHeight: Int = 0
    private var thumbWidth: Int = 0

    private var messageType: MessageType = MessageType.PREVIEW

    constructor(context: Context) : super((context)) {
        initialize(null)
    }

    constructor(context: Context, attrs: AttributeSet?) : super((context), attrs) {
        initialize(attrs)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super((context), attrs, defStyleAttr) {
        initialize(attrs)
    }

    private fun initialize(attrs: AttributeSet?) {
        val binding = QuoteViewBinding.inflate(LayoutInflater.from(context), this, true)

        background = binding.quoteBackground
        mainView = binding.quoteMain
        authorView = binding.quoteAuthor
        bodyView = binding.quoteText
        quoteBarView = binding.quoteBar
        thumbnailView = binding.quoteThumbnail
        attachmentVideoOverlayView = binding.quoteVideoOverlay
        attachmentContainerView = binding.quoteAttachmentContainer
        attachmentNameView = binding.quoteAttachmentName
        dismissView = binding.quoteDismiss
        mediaDescriptionText = binding.mediaType
        largeCornerRadius = resources.getDimensionPixelSize(R.dimen.quote_corner_radius_large)
        smallCornerRadius = resources.getDimensionPixelSize(R.dimen.quote_corner_radius_bottom)
        cornerMask = CornerMask(this)

        if (attrs != null) {
            val typedArray = context.theme.obtainStyledAttributes(attrs, R.styleable.QuoteView, 0, 0)
            messageType = MessageType.fromCode(typedArray.getInt(R.styleable.QuoteView_message_type, 0))
            typedArray.recycle()
            dismissView.visibility = if (messageType == MessageType.PREVIEW) VISIBLE else GONE
        }
        setMessageType(messageType)
        dismissView.setOnClickListener { visibility = GONE }
    }

    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)
        cornerMask.mask(canvas)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()

    }

    fun setMessageType(messageType: MessageType) {
        this.messageType = messageType
        cornerMask.setRadii(largeCornerRadius, largeCornerRadius, smallCornerRadius, smallCornerRadius)
        thumbHeight = resources.getDimensionPixelSize(R.dimen.quote_thumb_size)
        thumbWidth = thumbHeight

        if (messageType == MessageType.PREVIEW) {
            val radius: Int = resources.getDimensionPixelOffset(R.dimen.quote_corner_radius_preview)
            cornerMask.setTopLeftRadius(radius)
            cornerMask.setTopRightRadius(radius)
        }

        val params: ViewGroup.LayoutParams = thumbnailView.layoutParams
        params.height = thumbHeight
        params.width = thumbWidth
        thumbnailView.layoutParams = params
    }

    fun setQuote(glideRequests: GlideRequests, id: String, author: String, isSelf: Boolean, body: CharSequence?, message: SignalMsgExt) {
        quoteId = id
        this.body = body
        this.author = author
        this.isSelf = isSelf
        this.message = message
        this.attachments = emptyList()

        message.contentSignalMultimediaMessage?.multimediaFileInfosList?.let {
            this.attachments = it
        }

        setQuoteAuthor(author, isSelf)
        setQuoteText(body, attachments)
        setQuoteAttachment(glideRequests, attachments)

        applyColorTheme()
    }

    fun setTopCornerSizes(topLeftLarge: Boolean, topRightLarge: Boolean) {
        cornerMask.setTopLeftRadius(if (topLeftLarge) largeCornerRadius else smallCornerRadius)
        cornerMask.setTopRightRadius(if (topRightLarge) largeCornerRadius else smallCornerRadius)
    }

    fun dismiss() {
        quoteId = ""
        author = ""
        body = null
        visibility = GONE
    }


    fun getProjection(parent: ViewGroup): Projection {
        return Projection.relativeToParent(parent, this, corners)
    }

    val corners: Projection.Corners
        get() = Projection.Corners(cornerMask.radii)

    private fun setQuoteAuthor(author: String, isSelf: Boolean) {
        Log.d("AAA", "setQuoteAuthor author ${author} isSelf ${isSelf} ")
        authorView.text = if (isSelf) context.getString(R.string.QuoteView_you) else author
    }

    private fun setQuoteText(body: CharSequence?, attachments: List<CwmSignalMsg.MultimediaFileInfo> ) {

        if (!body.isNullOrEmpty() || attachments.isEmpty()) {

            bodyView.text = body ?: ""
            bodyView.visibility = VISIBLE
            mediaDescriptionText.visibility = GONE
            return
        }

        //Text present for media
        bodyView.visibility = GONE
        mediaDescriptionText.visibility = VISIBLE
        val fileInfo = attachments.firstOrNull()

        if (fileInfo != null) {
            when (fileInfo.mediaType) {
                CwmSignalMsg.SIGNAL_MEDIA_TYPE.IMAGE -> {
                    mediaDescriptionText.setText(R.string.QuoteView_photo)
                }
                CwmSignalMsg.SIGNAL_MEDIA_TYPE.VIDEO -> {
                    mediaDescriptionText.setText(R.string.QuoteView_video)
                }
                CwmSignalMsg.SIGNAL_MEDIA_TYPE.AUDIO -> {
                    mediaDescriptionText.setText(R.string.QuoteView_audio)
                }
                CwmSignalMsg.SIGNAL_MEDIA_TYPE.DOC -> {
                    mediaDescriptionText.visibility = GONE
                }
                CwmSignalMsg.SIGNAL_MEDIA_TYPE.FILE -> {
                    mediaDescriptionText.visibility = GONE
                }
                else -> { mediaDescriptionText.visibility = GONE }
            }
        }
    }

    private fun setQuoteAttachment(glideRequests: GlideRequests,
                                   attachments: List<CwmSignalMsg.MultimediaFileInfo>) {
        mainView.minimumHeight = thumbHeight
        thumbnailView.setPadding(0, 0, 0, 0)

        val fileInfo = attachments.firstOrNull()
        attachmentVideoOverlayView.visibility = GONE

        if (fileInfo != null) {
            when (fileInfo.mediaType) {
                CwmSignalMsg.SIGNAL_MEDIA_TYPE.IMAGE, CwmSignalMsg.SIGNAL_MEDIA_TYPE.VIDEO -> {
                    thumbnailView.visibility = VISIBLE
                    attachmentContainerView.visibility = GONE
                    dismissView.setBackgroundResource(R.drawable.dismiss_background)
                    if (fileInfo.mediaType == CwmSignalMsg.SIGNAL_MEDIA_TYPE.VIDEO) {
                        attachmentVideoOverlayView.visibility = VISIBLE
                    }

                    glideRequests.load(Uri.parse(fileInfo.fileUri))
                        .centerCrop()
                        .override(thumbWidth, thumbHeight)
                        .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                        .into(thumbnailView)
                }
                CwmSignalMsg.SIGNAL_MEDIA_TYPE.DOC, CwmSignalMsg.SIGNAL_MEDIA_TYPE.FILE -> {
                    thumbnailView.visibility = GONE
                    attachmentContainerView.visibility = VISIBLE
                    attachmentNameView.text = fileInfo.fileName
                }
                else -> {
                    thumbnailView.visibility = GONE
                    attachmentContainerView.visibility = GONE
                    dismissView.background = null
                }
            }

        } else {
            thumbnailView.visibility = GONE
            attachmentContainerView.visibility = GONE
            dismissView.background = null
        }
    }


    fun setTextSize(unit: Int, size: Float) {
        bodyView.setTextSize(unit, size)
    }

//    val mentions: List<Any>
//        get() {
//            return MentionAnnotation.getMentionsFromAnnotations(body)
//        }

    private fun buildShapeAppearanceForLayoutDirection(): ShapeAppearanceModel {
        val fourDp: Int = ViewUtil.dpToPx(4)
        return if (layoutDirection == LAYOUT_DIRECTION_LTR) {
            ShapeAppearanceModel.builder()
                .setTopRightCorner(CornerFamily.ROUNDED, fourDp.toFloat())
                .setBottomRightCorner(CornerFamily.ROUNDED, fourDp.toFloat())
                .build()
        } else {
            ShapeAppearanceModel.builder()
                .setTopLeftCorner(CornerFamily.ROUNDED, fourDp.toFloat())
                .setBottomLeftCorner(CornerFamily.ROUNDED, fourDp.toFloat())
                .build()
        }
    }

    private fun applyColorTheme() {
        val isOutgoing: Boolean = messageType != MessageType.INCOMING
        val isPreview: Boolean = messageType == MessageType.PREVIEW
        val quoteViewColorTheme: QuoteViewColorTheme =
            QuoteViewColorTheme.resolveTheme(isOutgoing, isPreview)
        quoteBarView.setBackgroundColor(quoteViewColorTheme.getBarColor(context))
        background.setBackgroundColor(quoteViewColorTheme.getBackgroundColor(context))
        authorView.setTextColor(quoteViewColorTheme.getForegroundColor(context))
        bodyView.setTextColor(quoteViewColorTheme.getForegroundColor(context))
        attachmentNameView.setTextColor(quoteViewColorTheme.getForegroundColor(context))
        mediaDescriptionText.setTextColor(quoteViewColorTheme.getForegroundColor(context))
    }

}