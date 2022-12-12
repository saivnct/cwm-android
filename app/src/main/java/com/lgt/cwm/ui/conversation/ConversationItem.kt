package com.lgt.cwm.ui.conversation

import android.animation.ValueAnimator
import android.animation.ValueAnimator.AnimatorUpdateListener
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.net.Uri
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.style.ClickableSpan
import android.text.style.URLSpan
import android.text.util.Linkify
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.annotation.ColorRes
import androidx.annotation.DimenRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.text.util.LinkifyCompat
import androidx.lifecycle.Observer
import com.annimon.stream.Stream
import com.lgt.cwm.BuildConfig
import com.lgt.cwm.R
import com.lgt.cwm.activity.conversation.fragments.adapter.ConversationAdapter
import com.lgt.cwm.activity.media.MediaPreviewActivity
import com.lgt.cwm.activity.media.fragments.MediaPreviewFragment
import com.lgt.cwm.activity.media.models.MediaFileInfo
import com.lgt.cwm.business.media.linkpreview.LinkPreview
import com.lgt.cwm.business.media.mention.MentionAnnotation
import com.lgt.cwm.db.entity.SignalMsgDirection
import com.lgt.cwm.db.entity.SignalMsgStatus
import com.lgt.cwm.models.SignalMsgExt
import com.lgt.cwm.ui.DeliveryStatusView
import com.lgt.cwm.ui.components.Outliner
import com.lgt.cwm.ui.components.voice.VoiceNotePlaybackState
import com.lgt.cwm.ui.conversation.media.ConversationItemThumbnail
import com.lgt.cwm.ui.conversation.media.MediaClickListener
import com.lgt.cwm.ui.glide.GlideRequests
import com.lgt.cwm.util.*
import com.lgt.cwm.util.view.NullableStub
import com.lgt.cwm.util.view.Stub
import com.vanniktech.emoji.EmojiTextView
import cwmSignalMsgPb.CwmSignalMsg
import kotlinx.android.synthetic.main.conversation_item_footer_outgoing.view.*
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.math.abs

open class ConversationItem @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private lateinit var message: SignalMsgExt
    private var nextMessage: SignalMsgExt? = null
    private var previousMessage: SignalMsgExt? = null

    private lateinit var bodyBubble: ConversationItemBodyBubble
    private var deliveryStatusView: DeliveryStatusView? = null
    private var conversationItemBody: EmojiTextView? = null
    private var defaultBubbleColor = 0
    private var quoteView: QuoteView? = null
    private var forwardView: ForwardView? = null
    private var pulseOutlinerAlphaAnimator: ValueAnimator? = null
    private val pulseOutliner = Outliner()
    private var outliners: MutableList<Outliner> = ArrayList(2)

    private lateinit var glideRequests: GlideRequests

    private var eventListener: ItemEventListener? = null

    private var footer: ConversationItemFooter? = null
    private var mediaThumbnailStub: NullableStub<ConversationItemThumbnail>? = null
    private lateinit var documentViewStub: Stub<DocumentView>
    private lateinit var audioViewStub: Stub<AudioView>
    private var linkPreviewStub: Stub<LinkPreviewView>? = null
    private val urlClickListener = UrlClickListener()

    companion object {
        private val MAX_CLUSTERING_TIME_DIFF = TimeUnit.MINUTES.toMillis(3) //3 mins

        private fun isWithinClusteringTime(lhs: SignalMsgExt, rhs: SignalMsgExt): Boolean {
            val timeDiff: Long = abs(lhs.msgDate - rhs.msgDate)
            return timeDiff <= MAX_CLUSTERING_TIME_DIFF
        }
    }

    init {
        defaultBubbleColor = ContextCompat.getColor(context, R.color.conversation_item_bubble_color_normal)
    }

    fun bind(message: SignalMsgExt, nextMessage: SignalMsgExt?, previousMessage: SignalMsgExt?, glideRequests: GlideRequests) {
        this.message = message
        if (message.imType == CwmSignalMsg.SIGNAL_IM_TYPE.FORWARD.number) {
            message.contentSignalForwardMsg?.let { contentSignalForwardMsg ->
                val originMsgExt = SignalMsgExt(
                    contentSignalForwardMsg = contentSignalForwardMsg,
                    msgDate = message.msgDate,
                    serverDate = message.serverDate)
                this.message = originMsgExt
                setForwardedMessage(contentSignalForwardMsg)
            }
        } else {
            forwardView?.setNone()
        }

        this.nextMessage = nextMessage
        this.previousMessage = previousMessage

        this.glideRequests = glideRequests

        setMessageShape(this.message, previousMessage, nextMessage, true)
        setBubbleState()
        setBodyText()
        setMediaAttributes(this.message)

        footer = findViewById(R.id.conversation_item_footer)
        setFooter(this.message)

        if (this.message.replyMsgId.isNotEmpty()) {
            eventListener?.onSetQuote(this, message)
        } else {
            quoteView?.visibility = GONE
        }

        val bigRadius = readDimen(R.dimen.message_corner_radius)
        pulseOutliner.setRadius(bigRadius)
        pulseOutliner.setColor(ContextCompat.getColor(context, R.color.transparent_black))
        pulseOutliner.setStrokeWidth(ViewUtil.dpToPx(4).toFloat())
        outliners.add(pulseOutliner)
        bodyBubble.setOutliners(outliners)
    }

    override fun onFinishInflate() {
        super.onFinishInflate()

        bodyBubble = findViewById(R.id.body_bubble)
        deliveryStatusView = findViewById(R.id.footer_delivery_status)
        mediaThumbnailStub = NullableStub(findViewById(R.id.image_view_stub))
        documentViewStub = Stub(findViewById(R.id.document_view_stub))
        audioViewStub = Stub(findViewById(R.id.audio_view_stub))
        quoteView = findViewById(R.id.quote_view)
        linkPreviewStub = Stub(findViewById(R.id.link_preview_stub))
        conversationItemBody = findViewById(R.id.conversation_item_body)
        forwardView = findViewById(R.id.forward_view)
        bodyBubble.setBackgroundResource(R.drawable.message_bubble_background_sent_alone)
        setBubbleState()
    }

    fun setItemEventListener(eventListener: ItemEventListener?) {
        this.eventListener = eventListener
    }

    private fun setBodyText() {
        val body = if (message.imType == CwmSignalMsg.SIGNAL_IM_TYPE.URL.number) message.dataIMSignalURLMessage else message.contentIMMessage
        if (!body.isNullOrEmpty()) linkifyMessageBody(body, true)
    }

    private fun setMediaAttributes(message: SignalMsgExt) {
        if (message.imType == CwmSignalMsg.SIGNAL_IM_TYPE.MULTIMEDIA.number) {
            audioViewStub.get().visibility = GONE
            documentViewStub.get().visibility = GONE

            mediaThumbnailStub?.let {
                it.require().visibility = VISIBLE
                it.require().setMinimumThumbnailWidth(readDimen(R.dimen.media_bubble_min_width_with_content))
                it.require().setImageResource(
                    glideRequests,
                    message,
                    false,
                    false
                )

                it.require().setThumbnailClickListener(ThumbnailClickListener())
            }

            message.contentSignalMultimediaMessage?.let { contentSignalMultimediaMessage ->
                val multimediaFileInfo = contentSignalMultimediaMessage.multimediaFileInfosList.firstOrNull()
                multimediaFileInfo?.let {
                    if (it.mediaType == CwmSignalMsg.SIGNAL_MEDIA_TYPE.DOC ||
                        it.mediaType == CwmSignalMsg.SIGNAL_MEDIA_TYPE.FILE) {
                        if (hasDocument(it)) {
                            mediaThumbnailStub?.let { if (it.resolved()) it.require().visibility = GONE }
                            documentViewStub.get().visibility = VISIBLE
                            documentViewStub.get().setDocument(it, null)
                            documentViewStub.get().setDocumentClickListener(ThumbnailClickListener())
                        }
                    } else if (it.mediaType == CwmSignalMsg.SIGNAL_MEDIA_TYPE.AUDIO) {
                        if (audioViewStub.resolved()) {
                            eventListener?.onUnregisterVoiceNoteCallbacks(audioViewStub.get().playbackStateObserver)
                        }

                        mediaThumbnailStub?.let { if (it.resolved()) it.require().visibility = GONE }
                        audioViewStub.get().visibility = VISIBLE
                        audioViewStub.get().setAudio(it, AudioViewCallbacks(), false, false)
                        audioViewStub.get().setDownloadClickListener(null)
                        eventListener?.onRegisterVoiceNoteCallbacks(audioViewStub.get().playbackStateObserver)

                    }


                }
            }
        } else if (message.imType == CwmSignalMsg.SIGNAL_IM_TYPE.URL.number) {
            linkPreviewStub?.let {
                it.get().visibility = VISIBLE
                message.contentSignalURLMessage?.let { urlMsg ->
                    val linkPreview = LinkPreview(urlMsg.url, urlMsg.urlTitle, urlMsg.urlDescription, 0,  urlMsg.urlThumbnail)
                    it.get().setLinkPreview(glideRequests, linkPreview, true)
                    ViewUtil.setBottomMargin(it.get(), ViewUtil.dpToPx(8))

                    it.get().setOnClickListener {
                        eventListener?.onLinkPreviewClicked(linkPreview)
                    }

                    var radius = resources.getDimensionPixelSize(R.dimen.message_corner_radius)

                    if (message.replyMsgId.isNotEmpty()) {
                        radius = 0
                    }
                    it.get().setCorners(radius, radius)
                }
            }
        } else {
            linkPreviewStub?.get()?.visibility = GONE
        }

    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    private fun setBubbleState() {
        bodyBubble.background.colorFilter =
            PorterDuffColorFilter(
                ContextCompat.getColor(context, R.color.conversation_item_background_primary),
                PorterDuff.Mode.SRC_IN
            )

//        deliveryStatusView.setRead()

    }

    fun setBubbleBackgroundColor(@ColorRes id: Int) {
        bodyBubble.background.colorFilter =
            PorterDuffColorFilter(
                ContextCompat.getColor(context, id),
                PorterDuff.Mode.SRC_IN
            )
    }

    private fun setMessageShape(current: SignalMsgExt, previous: SignalMsgExt?, next: SignalMsgExt?, isGroupThread: Boolean) {
        val bigRadius: Int = readDimen(R.dimen.message_corner_radius)
        val smallRadius: Int = readDimen(R.dimen.message_corner_collapse_radius)
        var background: Int

        if (current.direction == SignalMsgDirection.OUTGOING.code || current.direction == SignalMsgDirection.OUTGOING_DIFF_SESSION.code) {
            background = R.drawable.message_bubble_background_sent_alone
        } else {
            background = R.drawable.message_bubble_background_received_alone
        }

//        if (isSingularMessage(current, previous, next, isGroupThread)) {
//            if (current.direction == SignalMsgDirection.OUTGOING.code) {
//                background = R.drawable.message_bubble_background_sent_alone
//            } else {
//                background = R.drawable.message_bubble_background_received_alone
//            }
//        } else if (isStartOfMessageCluster(current, previous, isGroupThread)) {
//            if (current.direction == SignalMsgDirection.OUTGOING.code) {
//                background = R.drawable.message_bubble_background_sent_start
//            } else {
//                background = R.drawable.message_bubble_background_received_start
//            }
//        } else if (isEndOfMessageCluster(current, next, isGroupThread)) {
//            if (current.direction == SignalMsgDirection.OUTGOING.code) {
//                background = R.drawable.message_bubble_background_sent_end
//            } else {
//                background = R.drawable.message_bubble_background_received_end
//            }
//            //middle
//        } else  {
//            if (current.direction == SignalMsgDirection.OUTGOING.code) {
//                background = R.drawable.message_bubble_background_sent_middle
//            } else {
//                background = R.drawable.message_bubble_background_received_middle
//            }
//        }

        bodyBubble.setBackgroundResource(background)
    }

    private fun linkifyMessageBody(body: String, shouldLinkifyAllLinks: Boolean) {
        val bodyAndMentions = MentionUtil.extractMentionsFromContent(body)
        val messageBody = SpannableString(bodyAndMentions.body)
        if (bodyAndMentions.mentions.isNotEmpty()) {
            MentionAnnotation.setMentionAnnotations(messageBody, bodyAndMentions.mentions)
            val mentionAnnotations = MentionAnnotation.getMentionAnnotations(messageBody)
            for (annotation in mentionAnnotations) {
                messageBody.setSpan(
                    MentionClickableSpan(annotation.value),
                    messageBody.getSpanStart(annotation),
                    messageBody.getSpanEnd(annotation),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
        }

        val linkPattern = Linkify.WEB_URLS or Linkify.EMAIL_ADDRESSES or Linkify.PHONE_NUMBERS
        val hasLinks = LinkifyCompat.addLinks(messageBody, if (shouldLinkifyAllLinks) linkPattern else 0)
        if (hasLinks) {
            Stream.of(*messageBody.getSpans(0, messageBody.length, URLSpan::class.java))
                .filterNot { url: URLSpan -> LinkUtil.isLegalUrl(url.url) }
                .forEach { o: URLSpan? -> messageBody.removeSpan(o) }

            val urlSpans = messageBody.getSpans(0, messageBody.length, URLSpan::class.java)

            for (urlSpan in urlSpans) {
                val start = messageBody.getSpanStart(urlSpan)
                val end = messageBody.getSpanEnd(urlSpan)
                val span: URLSpan = InterceptableLongClickCopyLinkSpan(urlSpan.url, urlClickListener)

                messageBody.setSpan(span, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

            }
        }
        conversationItemBody?.movementMethod = LongClickMovementMethod.getInstance(context)
        conversationItemBody?.text = messageBody
    }

    private fun setForwardedMessage(forwardMessage: CwmSignalMsg.SignalForwardMessage) {
        var forwardForm = "From ${forwardMessage.from}"
        if (forwardMessage.fromFirstName.isNotEmpty()){
            forwardForm = if (forwardMessage.fromLastName.isNotEmpty()){
                "From ${forwardMessage.fromFirstName} ${forwardMessage.fromLastName}"
            } else "From ${forwardMessage.fromFirstName}"
        }

        if (!forwardMessage.fromUserName.isNullOrEmpty()){
            forwardForm = "From ${forwardMessage.fromUserName}"
        }

        forwardView?.setForwardFrom(forwardForm)
    }

    private fun readDimen(@DimenRes dimenId: Int): Int {
        return context.resources.getDimensionPixelOffset(dimenId)
    }

    private inner class UrlClickListener : UrlClickHandler {
        override fun handleOnClick(urlSpan: URLSpan, widget: View?): Boolean {
            return eventListener != null && eventListener!!.onUrlClicked(urlSpan, widget)
        }
    }

    private inner class MentionClickableSpan constructor(val mentionedId: String) : ClickableSpan() {
        override fun onClick(widget: View) {
            eventListener?.onGroupMemberClicked(mentionedId, "groupid_test")
        }

        override fun updateDrawState(ds: TextPaint) {
            ds.color = Color.parseColor("#0000EE")
        }
    }
//    private fun isSingularMessage(current: SignalMsgExt, previous: SignalMsgExt?, next: SignalMsgExt?, isGroupThread: Boolean): Boolean {
//        return isStartOfMessageCluster(current, previous, isGroupThread) && isEndOfMessageCluster(current, next, isGroupThread)
//    }
//
//    private fun isStartOfMessageCluster(current: SignalMsgExt, previous: SignalMsgExt?, isGroupThread: Boolean): Boolean {
//        return  !previous.isPresent || !DateUtil.isSameDay(current.msgDate, previous.get().msgDate) ||
//                current.to != previous.get().to || !isWithinClusteringTime(current, previous.get())
//    }
//
//    private fun isEndOfMessageCluster(current: SignalMsgExt, next: SignalMsgExt?, isGroupThread: Boolean): Boolean {
//        return !next.isPresent || !DateUtil.isSameDay(current.msgDate, next.get().msgDate) ||
//                current.to != next.get().to || !isWithinClusteringTime(current, next.get())
//    }

    private fun setFooter(message: SignalMsgExt) {
        footer?.let {
            ViewUtil.updateLayoutParams(it, RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT)
            ViewUtil.setTopMargin(it, readDimen(R.dimen.message_bubble_default_footer_bottom_margin))

            it.footer_date.text = DateUtil.convertLongToTimeHourChat(message.msgDate)

            if (message.direction == SignalMsgDirection.INCOMING.code) {
                it.footer_delivery_status.setNone()
            } else {
                when (message.status) {
                    SignalMsgStatus.SENDING.code ->{
                        it.footer_delivery_status.setPending()
                    }
                    SignalMsgStatus.SENT.code ->{
                        it.footer_delivery_status.setSent()
                    }
                    SignalMsgStatus.SENT_SEEN.code, SignalMsgStatus.SENT_SEEN_ALL.code ->{
                        it.footer_delivery_status.setSeen()
                    }
                    SignalMsgStatus.SENT_FAIL.code ->{
                        it.footer_delivery_status.setFail()
                    }

                    else -> {}
                }
            }

        }


    }

    private fun hasDocument(fileInfo: CwmSignalMsg.MultimediaFileInfo?): Boolean {
        var result = false
        fileInfo?.let {
            result = (it.mediaType == CwmSignalMsg.SIGNAL_MEDIA_TYPE.DOC || it.mediaType == CwmSignalMsg.SIGNAL_MEDIA_TYPE.FILE)
        }
        return result
    }

    private fun hasAudio(fileInfo: CwmSignalMsg.MultimediaFileInfo?): Boolean {
        var result = false
        fileInfo?.let {
            result = (it.mediaType == CwmSignalMsg.SIGNAL_MEDIA_TYPE.DOC || it.mediaType == CwmSignalMsg.SIGNAL_MEDIA_TYPE.FILE)
        }
        return result
    }


    private inner class ThumbnailClickListener : MediaClickListener {
        override fun onClick(v: View, mediaFile: CwmSignalMsg.MultimediaFileInfo, mediaFileList: List<CwmSignalMsg.MultimediaFileInfo>?) {
            val mimeType = when (mediaFile.mediaType) {
                CwmSignalMsg.SIGNAL_MEDIA_TYPE.IMAGE -> {
                    "image/*"
                }
                CwmSignalMsg.SIGNAL_MEDIA_TYPE.VIDEO -> {
                    "video/*"
                }
                CwmSignalMsg.SIGNAL_MEDIA_TYPE.AUDIO -> {
                    "audio/*"
                }
                CwmSignalMsg.SIGNAL_MEDIA_TYPE.DOC -> {
                    if (mediaFile.fileUri.endsWith(".pdf")) {
                        "application/pdf"
                    } else if (mediaFile.fileUri.endsWith(".rar") || mediaFile.fileUri.endsWith(".zip")) {
                        "application/zip"
                    } else if (mediaFile.fileUri.endsWith(".doc")) {
                        "application/msword"
                    } else if (mediaFile.fileUri.endsWith(".docx")) {
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                    } else if (mediaFile.fileUri.endsWith(".xls")) {
                        "application/vnd.ms-excel"
                    } else if (mediaFile.fileUri.endsWith(".xlsx")) {
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                    } else if (mediaFile.fileUri.endsWith(".ppt")) {
                        "application/vnd.ms-powerpoint"
                    } else if (mediaFile.fileUri.endsWith(".pptx")) {
                        "application/vnd.openxmlformats-officedocument.presentationml.presentation"
                    } else if (mediaFile.fileUri.endsWith(".txt")) {
                        "text/plain"
                    } else {
                        "application/*"
                    }
                }
                CwmSignalMsg.SIGNAL_MEDIA_TYPE.FILE -> {
                    "application/*"
                }
                else -> {
                    "application/*"
                }
            }

            if (MediaPreviewActivity.isContentTypeSupported(mimeType)) {
                val intent = Intent(context, MediaPreviewActivity::class.java)
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

                val list = ArrayList<MediaFileInfo>()

                var activePosition = 0

                if (mediaFileList != null) {
                    mediaFileList.forEach { file ->
                        var mediaType = ""
                        if (file.mediaType == CwmSignalMsg.SIGNAL_MEDIA_TYPE.VIDEO) {
                            mediaType = "video/*"
                        } else if (file.mediaType == CwmSignalMsg.SIGNAL_MEDIA_TYPE.IMAGE){
                            mediaType = "image/*"
                        }
                        list.add(MediaFileInfo(Uri.parse(file.fileUri), mediaType, file.fileSize))
                    }

                    val index = mediaFileList.indexOf(mediaFile)

                    if (index > 0) {
                        activePosition = index
                    }

                } else {
                    var type = ""
                    if (mediaFile.mediaType == CwmSignalMsg.SIGNAL_MEDIA_TYPE.VIDEO) {
                        type = "video/*"
                    } else if (mediaFile.mediaType == CwmSignalMsg.SIGNAL_MEDIA_TYPE.IMAGE){
                        type = "image/*"
                    }
                    list.add(MediaFileInfo(Uri.parse(mediaFile.fileUri), type, mediaFile.fileSize))
                }


                intent.setDataAndType(list.first().fileUri, list.first().mediaType)

                intent.putParcelableArrayListExtra(MediaPreviewFragment.DATA_MEDIA, list)
                intent.putExtra(MediaPreviewFragment.DATA_POSITION, activePosition)

                context.startActivity(intent)

            } else {
                val filePath = Uri.parse(mediaFile.fileUri).path!!
                val file = File(filePath)
                val uri = FileProvider.getUriForFile(
                    context,
                    BuildConfig.APPLICATION_ID + ".provider",
                    file
                )

                //test scan recent files
//                val rootDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
//                val files = rootDir.listFiles();
//                Log.d("AAA", "------------rootDir ${rootDir} - files size ${files.size}")
//                files.forEachIndexed { index, it ->
//                    val size = Util.getPrettyFileSize(it.length())
//                    val name = it.name
//                    val absolutePath = it.absolutePath
//                    val path = it.path
//                    Log.d("AAA", " ${index} file name ${name} - size ${size} - absolutePath ${absolutePath} - path ${path}")
//                    Log.d("AAA", "Noab path ${path}")
//                    Log.d("AAA", "Yeab path ${absolutePath}")
//                    Log.d("AAA", "Uri ${Uri.fromFile(it)}")
//                }

//                newUri = FileProvider.getUriForFile(
//                    context,
//                    BuildConfig.APPLICATION_ID + ".provider",
//                    files[12]
//                )

                val intent = Intent(Intent.ACTION_VIEW)
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                intent.setDataAndType(uri, Intent.normalizeMimeType(mimeType))
                try {
                    context.startActivity(intent)
                } catch (anfe: ActivityNotFoundException) {
                    Log.w("ConversationItem", "No activity existed to view the media.")
                    Toast.makeText(context, R.string.ConversationItem_unable_to_open_media, Toast.LENGTH_LONG).show()
                }
            }
        }
    }


    private inner class AudioViewCallbacks : AudioView.Callbacks {
        override fun onPlay(audioUri: Uri, progress: Double) {
            eventListener?.onVoiceNotePlay(audioUri,"msg_id_test_123", progress)

//            var mediaPlayer = MediaPlayer.create(context, audioUri)
//            mediaPlayer.start()
//            if (eventListener == null) return
//            eventListener.onVoiceNotePlay(audioUri, messageRecord.getId(), progress)
        }

        override fun onPause(audioUri: Uri) {
            if (eventListener == null) return
            eventListener?.onVoiceNotePause(audioUri)
        }

        override fun onSeekTo(audioUri: Uri, progress: Double) {
            eventListener?.onVoiceNoteSeekTo(audioUri, progress)
        }

        override fun onStopAndReset(audioUri: Uri) {
            throw UnsupportedOperationException()
        }

        override fun onSpeedChanged(speed: Float, isPlaying: Boolean) {
//            footer.setAudioPlaybackSpeed(speed, isPlaying)
        }

        override fun onProgressUpdated(durationMillis: Long, playheadMillis: Long) {
//            footer.setAudioDuration(durationMillis, playheadMillis)
        }
    }

    interface ItemEventListener {
        fun onVoiceNotePlay(uri: Uri, messageId: String, position: Double)
        fun onVoiceNotePause(uri: Uri)
        fun onVoiceNoteSeekTo(uri: Uri, position: Double)
        fun onRegisterVoiceNoteCallbacks(onPlaybackStartObserver: Observer<VoiceNotePlaybackState>)
        fun onUnregisterVoiceNoteCallbacks(onPlaybackStartObserver: Observer<VoiceNotePlaybackState>)
        fun onSetQuote(item: ConversationItem, message: SignalMsgExt)
        fun onQuoteClicked(quoteMsg: SignalMsgExt)
        fun onLinkPreviewClicked(linkPreview: LinkPreview)
        fun onGroupMemberClicked(memberId: String, groupId: String)

        /** @return true if handled, false if you want to let the normal url handling continue*/
        fun onUrlClicked(urlSpan: URLSpan, widget: View?): Boolean
    }

    fun setQuote(current: SignalMsgExt, message: SignalMsgExt, previous: SignalMsgExt?, next: SignalMsgExt?, isGroupThread: Boolean) {
        var quoteMsg = message
        var isForward = false
        if (quoteMsg.imType == CwmSignalMsg.SIGNAL_IM_TYPE.FORWARD.number) {
            message.contentSignalForwardMsg?.let { contentSignalForwardMsg ->
                val originMsgExt = SignalMsgExt(
                    contentSignalForwardMsg = contentSignalForwardMsg,
                    msgDate = message.msgDate,
                    serverDate = message.serverDate)
                quoteMsg = originMsgExt
            }
            isForward = true
        }

        quoteView?.let {
            it.setMessageType(if (current.direction == SignalMsgDirection.OUTGOING.code) QuoteView.MessageType.OUTGOING else QuoteView.MessageType.INCOMING)
            val content = if (quoteMsg.imType == CwmSignalMsg.SIGNAL_IM_TYPE.URL.number) quoteMsg.dataIMSignalURLMessage else quoteMsg.contentIMMessage
            it.setQuote(
                glideRequests,
                quoteMsg.msgId,
                quoteMsg.from,
                false,
                content,
                quoteMsg
            )
            it.visibility = VISIBLE
            it.layoutParams.width = ViewGroup.LayoutParams.WRAP_CONTENT
            it.setOnClickListener {
                if (isForward) eventListener?.onQuoteClicked(message)
                else eventListener?.onQuoteClicked(quoteMsg)
            }

            if (current.direction == SignalMsgDirection.OUTGOING.code) {
                it.setTopCornerSizes(true, true)
            } else if (isGroupThread) {
                it.setTopCornerSizes(false, false)
            } else {
                it.setTopCornerSizes(true, true)
            }

            ViewUtil.setBottomMargin(it, 8)
            if (mediaThumbnailStub!!.resolved()) {
                ViewUtil.setTopMargin(
                    mediaThumbnailStub!!.require(),
                    readDimen(R.dimen.message_bubble_top_padding)
                )
            }

//            if (file.mediaType == CwmSignalMsg.SIGNAL_MEDIA_TYPE.VIDEO) {
//
//            } else {
//                it.dismiss()
//                val topMargin = if (current.direction == SignalMsgDirection.OUTGOING.code) 0 else readDimen(R.dimen.message_bubble_top_image_margin)
//                if (mediaThumbnailStub!!.resolved()) {
//                    ViewUtil.setTopMargin(mediaThumbnailStub!!.require(), topMargin)
//                }
//            }
        }

    }

    fun startPulseOutlinerAnimation() {
        pulseOutlinerAlphaAnimator = ValueAnimator.ofInt(0, 0x66, 0).setDuration(600)
        pulseOutlinerAlphaAnimator?.repeatCount = 1
        pulseOutlinerAlphaAnimator?.addUpdateListener(AnimatorUpdateListener { animator: ValueAnimator ->
            pulseOutliner.setAlpha(animator.animatedValue as Int)
            bodyBubble.invalidate()
            mediaThumbnailStub?.let {
                if (it.resolved()) {
                    it.require().invalidate()
                }
            }

        })
        pulseOutlinerAlphaAnimator?.start()
    }

    fun cancelPulseOutlinerAnimation() {
        pulseOutlinerAlphaAnimator?.cancel()
        pulseOutlinerAlphaAnimator = null
        pulseOutliner.setAlpha(0)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        cancelPulseOutlinerAnimation()
    }
}