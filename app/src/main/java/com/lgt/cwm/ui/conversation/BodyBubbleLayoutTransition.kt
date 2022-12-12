package com.lgt.cwm.ui.conversation

import android.animation.LayoutTransition

class BodyBubbleLayoutTransition : LayoutTransition() {
    init {
        disableTransitionType(APPEARING)
        disableTransitionType(DISAPPEARING)
        disableTransitionType(CHANGE_APPEARING)
        disableTransitionType(CHANGING)

        setDuration(100L)
    }
}