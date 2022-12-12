package com.lgt.cwm.ui.conversation

import androidx.core.view.doOnNextLayout
import androidx.recyclerview.widget.RecyclerView
import com.lgt.cwm.util.DimensionUnit

/**
 * Adds necessary padding to each side of the given RecyclerView in order to ensure that
 * if all buttons can fit in the visible real-estate on screen, they are centered.
 */
class AttachmentButtonCenterHelper(private val recyclerView: RecyclerView) : RecyclerView.AdapterDataObserver() {

    //style name="Cwm.Widget.ImageView.ActionButton" width = 42, margin start = 16, margin end = 16 => total = 42 + 16 + 16 = 74
    private val itemWidth: Float = DimensionUnit.DP.toPixels(74f)
    private val defaultPadding: Float = DimensionUnit.DP.toPixels(16f)

    override fun onChanged() {
        val itemCount = recyclerView.adapter?.itemCount ?: return
        val requiredSpace = itemWidth * itemCount

        recyclerView.doOnNextLayout {
            if (it.measuredWidth >= requiredSpace) {
                val extraSpace = it.measuredWidth - requiredSpace
                val availablePadding = extraSpace / 2f
                it.post {
                    it.setPadding(availablePadding.toInt(), it.paddingTop, availablePadding.toInt(), it.paddingBottom)
                }
            } else {
                it.setPadding(defaultPadding.toInt(), it.paddingTop, defaultPadding.toInt(), it.paddingBottom)
            }
        }
    }
}
