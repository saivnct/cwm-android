package com.lgt.cwm.ui.recycleview

import android.content.Context
import android.util.DisplayMetrics
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView


class SmoothScrollingLinearLayoutManager(context: Context, reverseLayout: Boolean) : LinearLayoutManager(context, RecyclerView.VERTICAL, reverseLayout) {

    override fun supportsPredictiveItemAnimations(): Boolean {
        return false
    }

    fun smoothScrollToPosition(context: Context, position: Int, millisecondsPerInch: Float) {
        val scroller: LinearSmoothScroller = object : LinearSmoothScroller(context) {
            override fun getVerticalSnapPreference(): Int {
                return SNAP_TO_END
            }

            override fun calculateSpeedPerPixel(displayMetrics: DisplayMetrics): Float {
                return millisecondsPerInch / displayMetrics.densityDpi
            }
        }
        scroller.targetPosition = position
        startSmoothScroll(scroller)
    }
}