package com.lgt.cwm.ui.animation

import android.view.View
import androidx.viewpager2.widget.ViewPager2
import kotlin.math.abs

/**
 * Based on https://developer.android.com/training/animation/screen-slide#depth-page
 */

class DepthPageTransformer : ViewPager2.PageTransformer {

    companion object {
        private const val MIN_SCALE = 0.75f
    }

    override fun transformPage(view: View, position: Float) {
        val pageWidth = view.width
        if (position < -1f) {
            view.alpha = 0f
        } else if (position <= 0f) {
            view.alpha = 1f
            view.translationX = 0f
            view.scaleX = 1f
            view.scaleY = 1f
        } else if (position <= 1f) {
            view.alpha = 1f - position
            view.translationX = pageWidth * -position
            val scaleFactor = MIN_SCALE + (1f - MIN_SCALE) * (1f - abs(position))
            view.scaleX = scaleFactor
            view.scaleY = scaleFactor
        } else {
            view.alpha = 0f
        }
    }

}