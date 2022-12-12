package com.lgt.cwm.ui.conversation.media

import android.content.Context
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.annotation.UiThread
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.FitCenter
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.lgt.cwm.R
import com.lgt.cwm.databinding.ThumbnailViewBinding
import com.lgt.cwm.models.conversation.Slide
import com.lgt.cwm.ui.components.GlideDrawableListeningTarget
import com.lgt.cwm.ui.glide.GlideRequest
import com.lgt.cwm.ui.glide.GlideRequests
import com.lgt.cwm.util.ViewUtil
import com.lgt.cwm.util.concurrent.ListenableFuture
import com.lgt.cwm.util.concurrent.SettableFuture
import cwmSignalMsgPb.CwmSignalMsg
import java.util.*
import java.util.concurrent.ExecutionException
import kotlin.math.max
import kotlin.math.min

open class ThumbnailView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null,
                                                   defStyle: Int = 0) : FrameLayout(context, attrs, defStyle) {

    private val image: ImageView
    private val blurhash: ImageView
    private val playOverlay: View
    private val captionIcon: View
    private var parentClickListener: OnClickListener? = null
    private val dimens = IntArray(2)
    private val bounds = IntArray(4)
    private val measureDimens = IntArray(2)

    private var transferControls: Optional<TransferControlView> =
        Optional.empty<TransferControlView>()

    private var thumbnailClickListener: MediaClickListener? = null
    private var downloadClickListener: MediaListClickListener? = null
    private var slide: Slide? = null
    private var mediaFile: CwmSignalMsg.MultimediaFileInfo? = null
    private var fit: BitmapTransformation = CenterCrop()
    private var radius = 0

    companion object {
        private val TAG: String = ThumbnailView::class.simpleName.toString()
        private const val WIDTH = 0
        private const val HEIGHT = 1
        private const val MIN_WIDTH = 0
        private const val MAX_WIDTH = 1
        private const val MIN_HEIGHT = 2
        private const val MAX_HEIGHT = 3
    }

    init {
        val binding = ThumbnailViewBinding.inflate(LayoutInflater.from(context), this, true);
        image = binding.thumbnailImage
        blurhash = binding.thumbnailBlurhash
        playOverlay = binding.playOverlay
        captionIcon = binding.thumbnailCaptionIcon

        super.setOnClickListener(ThumbnailClickDispatcher())

        if (attrs != null) {
            val typedArray = context.theme.obtainStyledAttributes(attrs, R.styleable.ThumbnailView, 0, 0)
            bounds[MIN_WIDTH] = typedArray.getDimensionPixelSize(R.styleable.ThumbnailView_minWidth, 0)
            bounds[MAX_WIDTH] = typedArray.getDimensionPixelSize(R.styleable.ThumbnailView_maxWidth, 0)
            bounds[MIN_HEIGHT] = typedArray.getDimensionPixelSize(R.styleable.ThumbnailView_minHeight, 0)
            bounds[MAX_HEIGHT] = typedArray.getDimensionPixelSize(R.styleable.ThumbnailView_maxHeight, 0)
            radius = typedArray.getDimensionPixelSize(R.styleable.ThumbnailView_thumbnail_radius, resources.getDimensionPixelSize(R.dimen.thumbnail_default_radius))
            fit = if (typedArray.getInt(R.styleable.ThumbnailView_thumbnail_fit, 0) == 1) FitCenter() else CenterCrop()
            val transparentOverlayColor = typedArray.getColor(R.styleable.ThumbnailView_transparent_overlay_color, -1)
            if (transparentOverlayColor > 0) {
                image.colorFilter = PorterDuffColorFilter(transparentOverlayColor, PorterDuff.Mode.SRC_ATOP)
            } else {
                image.colorFilter = null
            }
            typedArray.recycle()
        } else {
            radius = resources.getDimensionPixelSize(R.dimen.message_corner_collapse_radius)
            image.colorFilter = null
        }
    }

    override fun onMeasure(originalWidthMeasureSpec: Int, originalHeightMeasureSpec: Int) {
        fillTargetDimensions(measureDimens, dimens, bounds)
        if (measureDimens[WIDTH] == 0 && measureDimens[HEIGHT] == 0) {
            super.onMeasure(originalWidthMeasureSpec, originalHeightMeasureSpec)
            return
        }
        val finalWidth = measureDimens[WIDTH] + paddingLeft + paddingRight
        val finalHeight = measureDimens[HEIGHT] + paddingTop + paddingBottom
        super.onMeasure(
            MeasureSpec.makeMeasureSpec(finalWidth, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(finalHeight, MeasureSpec.EXACTLY)
        )
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        var playOverlayScale = 1f
        var captionIconScale = 1f
        val playOverlayWidth = playOverlay.layoutParams.width
        if (playOverlayWidth * 2 > width) {
            playOverlayScale /= 2f
            captionIconScale = 0f
        }
        playOverlay.scaleX = playOverlayScale
        playOverlay.scaleY = playOverlayScale
        captionIcon.scaleX = captionIconScale
        captionIcon.scaleY = captionIconScale
    }

    fun setMinimumThumbnailWidth(width: Int) {
        bounds[MIN_WIDTH] = width
        invalidate()
    }

    private fun fillTargetDimensions(targetDimens: IntArray, dimens: IntArray, bounds: IntArray) {
        val dimensFilledCount = getNonZeroCount(dimens)
        val boundsFilledCount = getNonZeroCount(bounds)
        val dimensAreInvalid = dimensFilledCount > 0 && dimensFilledCount < dimens.size
        if (dimensAreInvalid) {
            Log.w(
                TAG, String.format(
                    Locale.ENGLISH,
                    "Width or height has been specified, but not both. Dimens: %d x %d",
                    dimens[WIDTH],
                    dimens[HEIGHT]
                )
            )
        }
        if (dimensAreInvalid || dimensFilledCount == 0 || boundsFilledCount == 0) {
            targetDimens[WIDTH] = 0
            targetDimens[HEIGHT] = 0
            return
        }
        val naturalWidth = dimens[WIDTH].toDouble()
        val naturalHeight = dimens[HEIGHT].toDouble()
        val minWidth = bounds[MIN_WIDTH]
        val maxWidth = bounds[MAX_WIDTH]
        val minHeight = bounds[MIN_HEIGHT]
        val maxHeight = bounds[MAX_HEIGHT]
        check(!(boundsFilledCount > 0 && boundsFilledCount < bounds.size)) {
            String.format(
                Locale.ENGLISH,
                "One or more min/max dimensions have been specified, but not all. Bounds: [%d, %d, %d, %d]",
                minWidth,
                maxWidth,
                minHeight,
                maxHeight
            )
        }
        var measuredWidth = naturalWidth
        var measuredHeight = naturalHeight
        val widthInBounds = measuredWidth >= minWidth && measuredWidth <= maxWidth
        val heightInBounds = measuredHeight >= minHeight && measuredHeight <= maxHeight
        if (!widthInBounds || !heightInBounds) {
            val minWidthRatio = naturalWidth / minWidth
            val maxWidthRatio = naturalWidth / maxWidth
            val minHeightRatio = naturalHeight / minHeight
            val maxHeightRatio = naturalHeight / maxHeight
            if (maxWidthRatio > 1 || maxHeightRatio > 1) {
                if (maxWidthRatio >= maxHeightRatio) {
                    measuredWidth /= maxWidthRatio
                    measuredHeight /= maxWidthRatio
                } else {
                    measuredWidth /= maxHeightRatio
                    measuredHeight /= maxHeightRatio
                }
                measuredWidth = max(measuredWidth, minWidth.toDouble())
                measuredHeight = max(measuredHeight, minHeight.toDouble())
            } else if (minWidthRatio < 1 || minHeightRatio < 1) {
                if (minWidthRatio <= minHeightRatio) {
                    measuredWidth /= minWidthRatio
                    measuredHeight /= minWidthRatio
                } else {
                    measuredWidth /= minHeightRatio
                    measuredHeight /= minHeightRatio
                }
                measuredWidth = min(measuredWidth, maxWidth.toDouble())
                measuredHeight = min(measuredHeight, maxHeight.toDouble())
            }
        }
        targetDimens[WIDTH] = measuredWidth.toInt()
        targetDimens[HEIGHT] = measuredHeight.toInt()
    }

    private fun getNonZeroCount(vals: IntArray): Int {
        var count = 0
        for (value in vals) {
            if (value > 0) {
                count++
            }
        }
        return count
    }

    override fun setOnClickListener(l: OnClickListener?) {
        parentClickListener = l
    }

    override fun setFocusable(focusable: Boolean) {
        super.setFocusable(focusable)
        if (transferControls.isPresent) transferControls.get().isFocusable = focusable
    }

    override fun setClickable(clickable: Boolean) {
        super.setClickable(clickable)
        if (transferControls.isPresent) transferControls.get().isClickable = clickable
    }

    private fun getTransferControls(): TransferControlView {
        if (!transferControls.isPresent) {
            transferControls = Optional.of(ViewUtil.inflateStub(this, R.id.transfer_controls_stub))
        }
        return transferControls.get()
    }


    fun setBounds(minWidth: Int, maxWidth: Int, minHeight: Int, maxHeight: Int) {
        bounds[MIN_WIDTH] = minWidth
        bounds[MAX_WIDTH] = maxWidth
        bounds[MIN_HEIGHT] = minHeight
        bounds[MAX_HEIGHT] = maxHeight
        forceLayout()
    }

    //TODO: set image resource
    @UiThread
    open fun setImageResource(glideRequests: GlideRequests, fileInfo: CwmSignalMsg.MultimediaFileInfo,
        showControls: Boolean, isPreview: Boolean, naturalWidth: Int, naturalHeight: Int): ListenableFuture<Boolean> {
        this.mediaFile = fileInfo
//        captionIcon.visibility = if (slide.caption.isPresent) VISIBLE else GONE
        dimens[WIDTH] = naturalWidth
        dimens[HEIGHT] = naturalHeight

        if (fileInfo.mediaType == CwmSignalMsg.SIGNAL_MEDIA_TYPE.VIDEO) {
            playOverlay.visibility = VISIBLE
        } else {
            playOverlay.visibility = GONE
        }

        invalidate()
        val result: SettableFuture<Boolean> = SettableFuture()

        buildThumbnailGlideRequest(glideRequests, fileInfo).into(GlideDrawableListeningTarget(image, result))

        result.set(true)

        return result
    }

    @UiThread
    open fun setImageResource(glideRequests: GlideRequests, fileInfo: CwmSignalMsg.MultimediaFileInfo, showControls: Boolean, isPreview: Boolean): ListenableFuture<Boolean> {
        return setImageResource(glideRequests, fileInfo, showControls, isPreview, 0, 0)
    }

    open fun setImageResource(glideRequests: GlideRequests, uri: Uri): ListenableFuture<Boolean> {
        return setImageResource(glideRequests, uri, 0, 0)
    }

    open fun setImageResource(glideRequests: GlideRequests, url: String): ListenableFuture<Boolean> {
        val future: SettableFuture<Boolean> = SettableFuture()
        var request = glideRequests.load(url)
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .transition(DrawableTransitionOptions.withCrossFade())

        request = if (radius > 0) {
            request.transforms(CenterCrop(), RoundedCorners(radius))
        } else {
            request.transforms(CenterCrop())
        }
        request.into(GlideDrawableListeningTarget(image, future))
        blurhash.setImageDrawable(null)
        return future
    }

    open fun setImageResource(glideRequests: GlideRequests, uri: Uri, width: Int, height: Int): ListenableFuture<Boolean> {
        val future: SettableFuture<Boolean> = SettableFuture()
        if (transferControls.isPresent) getTransferControls().visibility = GONE
        var request = glideRequests.load(uri)
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .transition(DrawableTransitionOptions.withCrossFade())

        if (width > 0 && height > 0) {
            request = request.override(width, height)
        }

        request = if (radius > 0) {
            request.transforms(CenterCrop(), RoundedCorners(radius))
        } else {
            request.transforms(CenterCrop())
        }
        request.into(GlideDrawableListeningTarget(image, future))
        blurhash.setImageDrawable(null)

        return future
    }

    fun setThumbnailClickListener(listener: MediaClickListener) {
        thumbnailClickListener = listener
    }

    fun setDownloadClickListener(listener: MediaListClickListener) {
        downloadClickListener = listener
    }

    fun clear(glideRequests: GlideRequests) {
        glideRequests.clear(image)
        image.setImageDrawable(null)
        if (transferControls.isPresent) {
            getTransferControls().clear()
        }
        glideRequests.clear(blurhash)
        blurhash.setImageDrawable(null)
        slide = null
    }

    fun showDownloadText(showDownloadText: Boolean) {
//        getTransferControls().setShowDownloadText(showDownloadText)
    }

    fun showProgressSpinner() {
//        getTransferControls().showProgressSpinner()
    }

    fun setFit(fit: BitmapTransformation) {
        this.fit = fit
    }

    protected fun setRadius(radius: Int) {
        this.radius = radius
    }

    open fun buildThumbnailGlideRequest(glideRequests: GlideRequests, fileInfo: CwmSignalMsg.MultimediaFileInfo?): GlideRequest<Drawable> {
        var uri = ""

        fileInfo?.let { info ->
            uri = info.fileUri
        }

        Log.d("Thumbnail", "load uri ${uri}")
        val request = applySizing(
            glideRequests.load(uri)
                .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                .transition(DrawableTransitionOptions.withCrossFade()), fit)
        return request.apply(RequestOptions.errorOf(R.drawable.ic_missing_thumbnail_picture)) as GlideRequest<Drawable>
    }

    open fun buildPlaceholderGlideRequest(glideRequests: GlideRequests, slide: Slide): RequestBuilder<*> {
        var bitmap = glideRequests.asBitmap()
        val placeholderBlur = slide.getPlaceholderBlur()

        bitmap = if (placeholderBlur != null) {
            bitmap.load(placeholderBlur)
        } else {
            bitmap.load(slide.getPlaceholderRes(context.theme))
        }
        return applySizing(bitmap.diskCacheStrategy(DiskCacheStrategy.NONE), CenterCrop())
    }

    private fun applySizing(request: GlideRequest<*>, fitting: BitmapTransformation): GlideRequest<*> {
        val size = IntArray(2)
        fillTargetDimensions(size, dimens, bounds)
        if (size[WIDTH] == 0 && size[HEIGHT] == 0) {
            size[WIDTH] = defaultWidth
            size[HEIGHT] = defaultHeight
        }

        val newRequest = request.override(
            size[WIDTH],
            size[HEIGHT]
        )
        return if (radius > 0) {
            newRequest.transforms(fitting, RoundedCorners(radius))
        } else {
            newRequest.transforms(fitting)
        }
    }

    private val defaultWidth: Int
        get() {
            val params = layoutParams
            return if (params != null) {
                max(params.width, 0)
            } else 0
        }

    private val defaultHeight: Int
        get() {
            val params = layoutParams
            return if (params != null) {
                max(params.height, 0)
            } else 0
        }

    private inner class ThumbnailClickDispatcher : OnClickListener {
        override fun onClick(view: View) {
            if ( mediaFile != null) {
                thumbnailClickListener?.onClick(view, mediaFile!!)
            } else if (parentClickListener != null) {
                parentClickListener!!.onClick(view)
            }
        }
    }

    private inner class DownloadClickDispatcher : OnClickListener {
        override fun onClick(view: View) {
            Log.i(TAG, "onClick() for download button")
//            if (downloadClickListener != null && slide != null) {
//                downloadClickListener.onClick(view, listOf(slide))
//            } else {
//                Log.w(
//                    TAG,
//                    "Received a download button click, but unable to execute it. slide: " + java.lang.String.valueOf(
//                        slide
//                    ) + "  downloadClickListener: " + java.lang.String.valueOf(downloadClickListener)
//                )
//            }
        }
    }

    private class BlurhashClearListener private constructor(glideRequests: GlideRequests, blurhash: ImageView) : ListenableFuture.Listener<Boolean> {
        private val glideRequests: GlideRequests
        private val blurhash: ImageView

        init {
            this.glideRequests = glideRequests
            this.blurhash = blurhash
        }

        override fun onSuccess(result: Boolean) {
            glideRequests.clear(blurhash)
            blurhash.setImageDrawable(null)
        }

        override fun onFailure(e: ExecutionException) {
            glideRequests.clear(blurhash)
            blurhash.setImageDrawable(null)
        }

    }

}