package com.lgt.cwm.ui.conversation

import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.widget.LinearLayout
import com.annimon.stream.Collectors
import com.annimon.stream.Objects
import com.annimon.stream.Stream
import com.lgt.cwm.ui.components.ClipProjectionDrawable
import com.lgt.cwm.ui.components.Outliner
import com.lgt.cwm.ui.components.Projection


class ConversationItemBodyBubble : LinearLayout {
    private var outliners: List<Outliner>? = emptyList()
    private var sizeChangedListener: OnSizeChangedListener? = null
    private var clipProjectionDrawable: ClipProjectionDrawable? = null
    private var quoteViewProjection: Projection? = null
    private var videoPlayerProjection: Projection? = null

    constructor(context: Context) : super(context) {
        layoutTransition = BodyBubbleLayoutTransition()
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        layoutTransition = BodyBubbleLayoutTransition()
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        layoutTransition = BodyBubbleLayoutTransition()
    }

    fun setOutliners(outliners: List<Outliner>) {
        this.outliners = outliners
    }

    fun setOnSizeChangedListener(listener: OnSizeChangedListener?) {
        sizeChangedListener = listener
    }

    override fun setBackground(background: Drawable) {
        clipProjectionDrawable = ClipProjectionDrawable(background)
        clipProjectionDrawable?.setProjections(projections)
        super.setBackground(clipProjectionDrawable)
    }

    fun setQuoteViewProjection(quoteViewProjection: Projection?) {
        this.quoteViewProjection?.release()
        this.quoteViewProjection = quoteViewProjection
        clipProjectionDrawable?.setProjections(projections)
    }

    fun setVideoPlayerProjection(videoPlayerProjection: Projection?) {
        this.videoPlayerProjection?.release()
        this.videoPlayerProjection = videoPlayerProjection
        clipProjectionDrawable?.setProjections(projections)
    }

    fun getVideoPlayerProjection(): Projection? {
        return videoPlayerProjection
    }

    private val projections: MutableSet<Projection>
        get() = Stream.of(quoteViewProjection, videoPlayerProjection)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet())!!

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (outliners == null || outliners?.isEmpty() == true) return
        outliners?.let {
            if (outliners!!.isEmpty()) return

            for (outliner: Outliner in outliners!!) {
                outliner.draw(canvas, 0, measuredWidth, measuredHeight, 0)
            }
        }

    }

    override fun onSizeChanged(width: Int, height: Int, oldWidth: Int, oldHeight: Int) {
        if (sizeChangedListener != null) {
            post {
                sizeChangedListener?.onSizeChanged(width, height)
            }
        }
    }

    interface OnSizeChangedListener {
        fun onSizeChanged(width: Int, height: Int)
    }
}