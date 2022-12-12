package com.lgt.cwm.ui.conversation

import android.content.Context
import androidx.core.content.ContextCompat
import com.lgt.cwm.R

enum class QuoteViewColorTheme(
    private val backgroundColorRes: Int,
    private val barColorRes: Int,
    private val foregroundColorRes: Int
) {

    INCOMING_NORMAL(
        R.color.quote_view_background_incoming_normal,
        R.color.quote_view_bar_incoming_normal,
        R.color.quote_view_foreground_incoming_normal
    ),

    OUTGOING_NORMAL(
        R.color.quote_view_background_outgoing_normal,
        R.color.quote_view_bar_outgoing_normal,
        R.color.quote_view_foreground_outgoing_normal
    );

    fun getBackgroundColor(context: Context) = ContextCompat.getColor(context, backgroundColorRes)
    fun getBarColor(context: Context) = ContextCompat.getColor(context, barColorRes)
    fun getForegroundColor(context: Context) = ContextCompat.getColor(context, foregroundColorRes)

    companion object {
        @JvmStatic
        fun resolveTheme(isOutgoing: Boolean, isPreview: Boolean): QuoteViewColorTheme {
            return when {
                isPreview -> INCOMING_NORMAL
                isOutgoing -> OUTGOING_NORMAL
                else -> INCOMING_NORMAL
            }
        }
    }
}
