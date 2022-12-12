package com.lgt.cwm.ui

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.animation.Animation
import android.view.animation.LinearInterpolator
import android.view.animation.RotateAnimation
import android.widget.FrameLayout
import com.lgt.cwm.databinding.DeliveryStatusViewBinding
import kotlinx.android.synthetic.main.delivery_status_view.view.*

class DeliveryStatusView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : FrameLayout(context, attrs, defStyle) {

    private val rotationAnimation: RotateAnimation

    init {
        val binding: DeliveryStatusViewBinding =
            DeliveryStatusViewBinding.inflate(LayoutInflater.from(context), this, true);

        rotationAnimation = RotateAnimation(
            0F, 360f,
            Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f
        )
        rotationAnimation.interpolator = LinearInterpolator()
        rotationAnimation.duration = 1500
        rotationAnimation.repeatCount = Animation.INFINITE
        if (attrs != null) {
           //apply style
        }
    }

    fun setNone() {
        this.visibility = GONE
    }

    val isPending: Boolean get() = pending_indicator.visibility == VISIBLE

    fun setPending() {
        this.visibility = VISIBLE
        pending_indicator.visibility = VISIBLE
        pending_indicator.startAnimation(rotationAnimation)
        sent_indicator.visibility = GONE
        delivered_indicator.visibility = GONE
        read_indicator.visibility = GONE
        fail_indicator.visibility = GONE
    }

    fun setSent() {
        this.visibility = VISIBLE
        pending_indicator.visibility = GONE
        pending_indicator.clearAnimation()
        sent_indicator.visibility = VISIBLE
        delivered_indicator.visibility = GONE
        read_indicator.visibility = GONE
        fail_indicator.visibility = GONE
    }

    fun setDelivered() {
        this.visibility = VISIBLE
        pending_indicator.visibility = GONE
        pending_indicator.clearAnimation()
        sent_indicator.visibility = GONE
        delivered_indicator.visibility = VISIBLE
        read_indicator.visibility = GONE
        fail_indicator.visibility = GONE
    }

    fun setSeen() {
        this.visibility = VISIBLE
        pending_indicator.visibility = GONE
        pending_indicator.clearAnimation()
        sent_indicator.visibility = GONE
        delivered_indicator.visibility = GONE
        read_indicator.visibility = VISIBLE
        fail_indicator.visibility = GONE
    }

    fun setFail() {
        this.visibility = VISIBLE
        pending_indicator.visibility = GONE
        pending_indicator.clearAnimation()
        sent_indicator.visibility = GONE
        delivered_indicator.visibility = GONE
        read_indicator.visibility = GONE
        fail_indicator.visibility = VISIBLE
    }

    fun setTint(color: Int) {
        pending_indicator.setColorFilter(color)
        delivered_indicator.setColorFilter(color)
        sent_indicator.setColorFilter(color)
        read_indicator.setColorFilter(color)
    }

}