package com.lgt.cwm.ui.components

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import androidx.annotation.ColorInt
import com.lgt.cwm.util.ViewUtil

class Outliner {
    private val radii = FloatArray(8)
    private val corners = Path()
    private val bounds = RectF()
    private val outlinePaint = Paint()

    init {
        outlinePaint.style = Paint.Style.STROKE
        outlinePaint.strokeWidth = ViewUtil.dpToPx(1).toFloat()
        outlinePaint.isAntiAlias = true
    }

    fun setColor(@ColorInt color: Int) {
        outlinePaint.color = color
    }

    fun setStrokeWidth(pixels: Float) {
        outlinePaint.strokeWidth = pixels
    }

    fun setAlpha(alpha: Int) {
        outlinePaint.alpha = alpha
    }

    @JvmOverloads
    fun draw(canvas: Canvas, top: Int = 0, right: Int = canvas.width,
             bottom: Int = canvas.height, left: Int = 0) {
        val halfStrokeWidth = outlinePaint.strokeWidth / 2
        bounds.left = left + halfStrokeWidth
        bounds.top = top + halfStrokeWidth
        bounds.right = right - halfStrokeWidth
        bounds.bottom = bottom - halfStrokeWidth
        corners.reset()
        corners.addRoundRect(bounds, radii, Path.Direction.CW)
        canvas.drawPath(corners, outlinePaint)
    }

    fun setRadius(radius: Int) {
        setRadii(radius, radius, radius, radius)
    }

    fun setRadii(topLeft: Int, topRight: Int, bottomRight: Int, bottomLeft: Int) {
        radii[1] = topLeft.toFloat()
        radii[0] = radii[1]
        radii[3] = topRight.toFloat()
        radii[2] = radii[3]
        radii[5] = bottomRight.toFloat()
        radii[4] = radii[5]
        radii[7] = bottomLeft.toFloat()
        radii[6] = radii[7]
    }
}