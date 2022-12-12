package com.lgt.cwm.models.conversation

import android.content.Context
import android.content.res.Resources.Theme
import androidx.annotation.DrawableRes
import com.lgt.cwm.R

class ImageSlide : Slide {

    companion object {
        private val TAG = ImageSlide::class.simpleName.toString()
    }

    private val borderless: Boolean

    constructor(context: Context, attachment: Attachment) : super(context, attachment) {
        borderless = attachment.isBorderless
    }

    @DrawableRes
    override fun getPlaceholderRes(theme: Theme): Int {
        return 0
    }

    override fun hasImage(): Boolean {
        return true
    }

    override fun hasPlaceholder(): Boolean {
        return getPlaceholderBlur() != null
    }

    override fun isBorderless(): Boolean {
        return borderless
    }

    override val contentDescription: String = context.getString(R.string.Slide_image)

}