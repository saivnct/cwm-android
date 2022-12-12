package com.lgt.cwm.ui.conversation.media

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import androidx.core.content.ContextCompat
import com.lgt.cwm.R
import com.lgt.cwm.ui.components.CornerMask
import com.lgt.cwm.ui.components.Outliner

class OutlinedThumbnailView : ThumbnailView {
    private lateinit var cornerMask: CornerMask
    private lateinit var outliner: Outliner

    constructor(context: Context) : super(context) {
        init(null)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(attrs)
    }

    private fun init(attrs: AttributeSet?) {
        cornerMask = CornerMask(this)
        outliner = Outliner()
        val defaultOutlinerColor = ContextCompat.getColor(context, R.color.transparent_black_20)
        outliner.setColor(defaultOutlinerColor)

        var radius = 0
        if (attrs != null) {
            val typedArray = context.theme.obtainStyledAttributes(attrs, R.styleable.OutlinedThumbnailView, 0, 0)
            radius = typedArray.getDimensionPixelOffset(R.styleable.OutlinedThumbnailView_otv_cornerRadius, 0)
            val stroke = typedArray.getDimensionPixelSize(R.styleable.OutlinedThumbnailView_otv_strokeWidth, 1)
            outliner.setStrokeWidth(stroke.toFloat())
            outliner.setColor(typedArray.getColor(R.styleable.OutlinedThumbnailView_otv_strokeColor, defaultOutlinerColor))
        }
        setRadius(radius)
        setCorners(radius, radius, radius, radius)
        setWillNotDraw(false)
    }

    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)
        cornerMask.mask(canvas)
        outliner.draw(canvas)
    }

    fun setCorners(topLeft: Int, topRight: Int, bottomRight: Int, bottomLeft: Int) {
        cornerMask.setRadii(topLeft, topRight, bottomRight, bottomLeft)
        outliner.setRadii(topLeft, topRight, bottomRight, bottomLeft)
        postInvalidate()
    }
}