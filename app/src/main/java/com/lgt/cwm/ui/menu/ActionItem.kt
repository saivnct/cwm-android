package com.lgt.cwm.ui.menu

import androidx.annotation.DrawableRes

/**
 * Represents an action to be rendered via [SignalContextMenu] or [BottomActionBar]
 */
data class ActionItem(
    @DrawableRes val iconRes: Int,
    val title: CharSequence,
    val action: Runnable
)
