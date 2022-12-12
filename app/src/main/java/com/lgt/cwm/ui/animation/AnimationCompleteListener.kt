package com.lgt.cwm.ui.animation

import android.animation.Animator

abstract class AnimationCompleteListener: Animator.AnimatorListener {
    override fun onAnimationStart(animation: Animator) {}
    abstract override fun onAnimationEnd(animation: Animator)
    override fun onAnimationCancel(animation: Animator) {}
    override fun onAnimationRepeat(animation: Animator) {}
}