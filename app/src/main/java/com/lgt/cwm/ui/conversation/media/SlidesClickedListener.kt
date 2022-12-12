package com.lgt.cwm.ui.conversation.media

import android.view.View
import com.lgt.cwm.models.conversation.Slide

interface SlidesClickedListener {
    fun onClick(v: View, slides: List<Slide>)
}
