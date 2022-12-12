package com.lgt.cwm.activity.media.fragments

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.target.Target
import com.lgt.cwm.R
import com.lgt.cwm.databinding.FragmentImageMediaPreviewBinding
import com.lgt.cwm.ui.glide.GlideApp
import com.lgt.cwm.ui.glide.GlideRequests

class ImageMediaPreviewFragment : MediaPreviewFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val binding: FragmentImageMediaPreviewBinding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_image_media_preview, container, false);
        binding.lifecycleOwner = viewLifecycleOwner

        arguments?.let {
            val uri = it.getParcelable<Uri>(DATA_URI)!!
            var contentType = ""
            it.getString(DATA_CONTENT_TYPE)?.let { contentType = it }
            if (!isImageType(contentType)) {
                throw AssertionError("This fragment can only display image")
            }

            val glideRequests: GlideRequests = GlideApp.with(requireActivity())
//            glideRequests.load("https://icatcare.org/app/uploads/2018/07/Thinking-of-getting-a-cat.png")

            glideRequests.load(uri)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .dontTransform()
                .override(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)
                .into(binding.photoView)
        }

        return binding.root
    }

}