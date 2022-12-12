package com.lgt.cwm.ui.components

import android.graphics.*
import android.view.View

class CornerMask(view: View) {
    val radii = FloatArray(8)
    private val clearPaint = Paint()
    private val outline = Path()
    private val corners = Path()
    private val bounds = RectF()

    init {
        view.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        clearPaint.color = Color.BLACK
        clearPaint.style = Paint.Style.FILL
        clearPaint.isAntiAlias = true
        clearPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    }

    fun mask(canvas: Canvas) {
        bounds.set(canvas.clipBounds)
        corners.reset()
        corners.addRoundRect(bounds, radii, Path.Direction.CW)

        // Note: There's a bug in the P beta where most PorterDuff modes aren't working. But CLEAR does.
        //       So we find and inverse path and use Mode.CLEAR.
        //       See issue https://issuetracker.google.com/issues/111394085.
        outline.reset()
        outline.addRect(bounds, Path.Direction.CW)
        outline.op(corners, Path.Op.DIFFERENCE)
        canvas.drawPath(outline, clearPaint)
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

    fun setRadii(topLeft: Float, topRight: Float, bottomRight: Float, bottomLeft: Float) {
        radii[1] = topLeft
        radii[0] = radii[1]
        radii[3] = topRight
        radii[2] = radii[3]
        radii[5] = bottomRight
        radii[4] = radii[5]
        radii[7] = bottomLeft
        radii[6] = radii[7]
    }

    fun setTopLeftRadius(radius: Int) {
        radii[1] = radius.toFloat()
        radii[0] = radii[1]
    }

    fun setTopRightRadius(radius: Int) {
        radii[3] = radius.toFloat()
        radii[2] = radii[3]
    }

    fun setBottomRightRadius(radius: Int) {
        radii[5] = radius.toFloat()
        radii[4] = radii[5]
    }

    fun setBottomLeftRadius(radius: Int) {
        radii[7] = radius.toFloat()
        radii[6] = radii[7]
    }


}