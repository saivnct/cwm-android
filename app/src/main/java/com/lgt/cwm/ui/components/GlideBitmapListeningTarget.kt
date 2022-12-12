package com.lgt.cwm.ui.components

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.widget.ImageView
import com.bumptech.glide.request.target.BitmapImageViewTarget
import com.lgt.cwm.util.concurrent.SettableFuture

class GlideBitmapListeningTarget(view: ImageView, loaded: SettableFuture<Boolean>) : BitmapImageViewTarget(view) {

    private val loaded: SettableFuture<Boolean>

    init {
        this.loaded = loaded
    }

    override fun setResource(resource: Bitmap?) {
        super.setResource(resource)
        loaded.set(true)
    }

    override fun onLoadFailed(errorDrawable: Drawable?) {
        super.onLoadFailed(errorDrawable)
        loaded.set(true)
    }
}