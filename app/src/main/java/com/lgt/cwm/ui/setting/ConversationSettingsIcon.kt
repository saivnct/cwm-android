package com.lgt.cwm.ui.setting

import android.content.Context
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.Drawable
import android.graphics.drawable.InsetDrawable
import android.graphics.drawable.LayerDrawable
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.Px
import androidx.core.content.ContextCompat
import com.lgt.cwm.R

const val NO_TINT = -1

sealed class ConversationSettingsIcon {

    private data class FromResource(
        @DrawableRes private val iconId: Int,
        @ColorRes private val iconTintId: Int
    ) : ConversationSettingsIcon() {
        override fun resolve(context: Context) = requireNotNull(ContextCompat.getDrawable(context, iconId)).apply {
            if (iconTintId != NO_TINT) {
                colorFilter = PorterDuffColorFilter(ContextCompat.getColor(context, iconTintId), PorterDuff.Mode.SRC_IN)
            }
        }
    }

    private data class FromResourceWithBackground(
        @DrawableRes private val iconId: Int,
        @ColorRes private val iconTintId: Int,
        @DrawableRes private val backgroundId: Int,
        @ColorRes private val backgroundTint: Int,
        @Px private val insetPx: Int,
    ) : ConversationSettingsIcon() {
        override fun resolve(context: Context): Drawable {
            return LayerDrawable(
                arrayOf(
                    FromResource(backgroundId, backgroundTint).resolve(context),
                    InsetDrawable(FromResource(iconId, iconTintId).resolve(context), insetPx, insetPx, insetPx, insetPx)
                )
            )
        }
    }

    private data class FromDrawable(
        private val drawable: Drawable
    ) : ConversationSettingsIcon() {
        override fun resolve(context: Context): Drawable = drawable
    }

    abstract fun resolve(context: Context): Drawable

    companion object {
        @JvmStatic
        fun from(
            @DrawableRes iconId: Int,
            @ColorRes iconTintId: Int,
            @DrawableRes backgroundId: Int,
            @ColorRes backgroundTint: Int,
            @Px insetPx: Int = 0
        ): ConversationSettingsIcon {
            return FromResourceWithBackground(iconId, iconTintId, backgroundId, backgroundTint, insetPx)
        }

        @JvmStatic
        fun from(@DrawableRes iconId: Int, @ColorRes iconTintId: Int = R.color.cwm_icon_tint_primary): ConversationSettingsIcon = FromResource(iconId, iconTintId)

        @JvmStatic
        fun from(drawable: Drawable): ConversationSettingsIcon = FromDrawable(drawable)
    }
}
