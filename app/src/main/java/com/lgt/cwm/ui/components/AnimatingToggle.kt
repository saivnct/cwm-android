package com.lgt.cwm.ui.components

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.FrameLayout
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import com.lgt.cwm.R
import com.lgt.cwm.util.ViewUtil

class AnimatingToggle @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : FrameLayout(context, attrs, defStyleAttr) {
    private var current: View? = null
    private val inAnimation: Animation = AnimationUtils.loadAnimation(getContext(), R.anim.animation_toggle_in)
    private val outAnimation: Animation = AnimationUtils.loadAnimation(getContext(), R.anim.animation_toggle_out)

    init {
        outAnimation.interpolator = FastOutSlowInInterpolator()
        inAnimation.interpolator = FastOutSlowInInterpolator()
    }

    override fun addView(child: View, index: Int, params: ViewGroup.LayoutParams) {
        super.addView(child, index, params)
        if (!isInEditMode) {
            if (childCount == 1) {
                current = child
                child.visibility = VISIBLE
            } else {
                child.visibility = GONE
            }
            child.isClickable = false
        }
    }

    fun display(view: View?) {
        if (view == current) return
        current?.let { ViewUtil.animateOut(it, outAnimation, GONE) }
        view?.let { ViewUtil.animateIn(it, inAnimation) }
        current = view
    }

    fun displayQuick(view: View?) {
        if (view == current) return
        current?.visibility = GONE
        view?.visibility = VISIBLE
        current = view
    }
}