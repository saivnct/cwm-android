package com.lgt.cwm.ui.conversation.media

import android.view.View
import com.lgt.cwm.models.conversation.Slide

interface SlideClickListener {
    fun onClick(v: View, slide: Slide)
}
