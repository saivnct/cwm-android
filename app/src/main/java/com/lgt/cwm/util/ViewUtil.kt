package com.lgt.cwm.util

import android.app.Activity
import android.content.Context
import android.content.res.Resources
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import android.view.ViewStub
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import androidx.annotation.IdRes
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import com.lgt.cwm.util.concurrent.ListenableFuture
import com.lgt.cwm.util.concurrent.SettableFuture
import com.lgt.cwm.util.view.Stub

object ViewUtil {
    @JvmStatic
    fun pxToDp(px: Float): Float {
        return px / Resources.getSystem().displayMetrics.density
    }

    @JvmStatic
    fun dpToPx(context: Context, dp: Int): Int {
        return (dp * context.resources.displayMetrics.density + 0.5).toInt()
    }

    @JvmStatic
    fun dpToPx(dp: Int): Int {
        return Math.round(dp * Resources.getSystem().displayMetrics.density)
    }

    @JvmStatic
    fun dpToSp(dp: Int): Int {
        return (dpToPx(dp) / Resources.getSystem().displayMetrics.scaledDensity).toInt()
    }

    @JvmStatic
    fun spToPx(sp: Float): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP,
            sp,
            Resources.getSystem().displayMetrics
        ).toInt()
    }

    @JvmStatic
    fun animateOut(view: View, animation: Animation): ListenableFuture<Boolean> {
        return animateOut(view, animation, View.GONE)
    }

    @JvmStatic
    fun animateOut(view: View, animation: Animation, visibility: Int): ListenableFuture<Boolean> {
        val future = SettableFuture(false)
        if (view.visibility == visibility) {
            future.set(true)
        } else {
            view.clearAnimation()
            animation.reset()
            animation.startTime = 0
            animation.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation) {}
                override fun onAnimationRepeat(animation: Animation) {}
                override fun onAnimationEnd(animation: Animation) {
                    view.visibility = visibility
                    future.set(true)
                }
            })
            view.startAnimation(animation)
        }
        return future
    }

    @JvmStatic
    fun animateIn(view: View, animation: Animation) {
        if (view.visibility == View.VISIBLE) return
        view.clearAnimation()
        animation.reset()
        animation.startTime = 0
        view.visibility = View.VISIBLE
        view.startAnimation(animation)
    }

    @JvmStatic
    fun isLtr(view: View): Boolean {
        return isLtr(view.context)
    }

    @JvmStatic
    fun isLtr(context: Context): Boolean {
        return context.resources.configuration.layoutDirection == View.LAYOUT_DIRECTION_LTR
    }

    @JvmStatic
    fun isRtl(view: View): Boolean {
        return isRtl(view.context)
    }

    @JvmStatic
    fun isRtl(context: Context): Boolean {
        return context.resources.configuration.layoutDirection == View.LAYOUT_DIRECTION_RTL
    }

    @JvmStatic
    fun updateLayoutParams(view: View, width: Int, height: Int) {
        view.layoutParams.width = width
        view.layoutParams.height = height
        view.requestLayout()
    }

    @JvmStatic
    fun updateLayoutParamsIfNonNull(view: View?, width: Int, height: Int) {
        view?.let { updateLayoutParams(it, width, height) }
    }

    @JvmStatic
    fun setVisibilityIfNonNull(view: View?, visibility: Int) {
        view?.visibility = visibility
    }

    @JvmStatic
    fun getLeftMargin(view: View): Int {
        return if (isLtr(view)) {
            (view.layoutParams as MarginLayoutParams).leftMargin
        } else (view.layoutParams as MarginLayoutParams).rightMargin
    }

    @JvmStatic
    fun getRightMargin(view: View): Int {
        return if (isLtr(view)) {
            (view.layoutParams as MarginLayoutParams).rightMargin
        } else (view.layoutParams as MarginLayoutParams).leftMargin
    }

    @JvmStatic
    fun getTopMargin(view: View): Int {
        return (view.layoutParams as MarginLayoutParams).topMargin
    }

    @JvmStatic
    fun setLeftMargin(view: View, margin: Int) {
        if (isLtr(view)) {
            (view.layoutParams as MarginLayoutParams).leftMargin = margin
        } else {
            (view.layoutParams as MarginLayoutParams).rightMargin = margin
        }
        view.forceLayout()
        view.requestLayout()
    }

    @JvmStatic
    fun setRightMargin(view: View, margin: Int) {
        if (isLtr(view)) {
            (view.layoutParams as MarginLayoutParams).rightMargin = margin
        } else {
            (view.layoutParams as MarginLayoutParams).leftMargin = margin
        }
        view.forceLayout()
        view.requestLayout()
    }

    @JvmStatic
    fun setTopMargin(view: View, margin: Int) {
        (view.layoutParams as MarginLayoutParams).topMargin = margin
        view.requestLayout()
    }

    @JvmStatic
    fun setBottomMargin(view: View, margin: Int) {
        (view.layoutParams as MarginLayoutParams).bottomMargin = margin
        view.requestLayout()
    }

    @JvmStatic
    fun <T : View> inflateStub(parent: View, @IdRes stubId: Int): T {
        return (parent.findViewById<View>(stubId) as ViewStub).inflate() as T
    }

    @JvmStatic
    fun getStatusBarHeight(view: View): Int {
        var result = 0
        val resourceId = view.resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resourceId > 0) {
            result = view.resources.getDimensionPixelSize(resourceId)
        }
        return result
    }

    @JvmStatic
    fun getNavigationBarHeight(view: View): Int {
        var result = 0
        val resourceId = view.resources.getIdentifier("navigation_bar_height", "dimen", "android")
        if (resourceId > 0) {
            result = view.resources.getDimensionPixelSize(resourceId)
        }
        return result
    }

    @JvmStatic
    fun <T : View> findStubById(parent: Activity, @IdRes resId: Int): Stub<T> {
        return Stub(parent.findViewById(resId))
    }

    @JvmStatic
    fun <T : View> findStubById(parent: View, @IdRes resId: Int): Stub<T> {
        return Stub(parent.findViewById(resId))
    }

    @JvmStatic
    fun getAlphaAnimation(from: Float, to: Float, duration: Int): Animation {
        val anim: Animation = AlphaAnimation(from, to)
        anim.interpolator = FastOutSlowInInterpolator()
        anim.duration = duration.toLong()
        return anim
    }

    @JvmStatic
    fun fadeIn(view: View, duration: Int) {
        animateIn(view, getAlphaAnimation(0f, 1f, duration))
    }

    @JvmStatic
    fun fadeOut(view: View, duration: Int): ListenableFuture<Boolean> {
        return fadeOut(view, duration, View.GONE)
    }

    @JvmStatic
    fun fadeOut(view: View, duration: Int, visibility: Int): ListenableFuture<Boolean> {
        return animateOut(view, getAlphaAnimation(1f, 0f, duration), visibility)
    }

}