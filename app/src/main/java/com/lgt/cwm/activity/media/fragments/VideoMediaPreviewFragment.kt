package com.lgt.cwm.activity.media.fragments

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import com.lgt.cwm.R
import com.lgt.cwm.databinding.FragmentVideoMediaPreviewBinding
import com.lgt.cwm.ui.video.VideoPlayer

class VideoMediaPreviewFragment : MediaPreviewFragment() {

    private lateinit var videoView: VideoPlayer


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val binding: FragmentVideoMediaPreviewBinding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_video_media_preview, container, false);
        binding.lifecycleOwner = viewLifecycleOwner

        videoView = binding.videoPlayer

        val arguments = requireArguments()
        arguments.let {
            val uri = it.getParcelable<Uri>(DATA_URI)!!
            var contentType = ""
            it.getString(DATA_CONTENT_TYPE)?.let { contentType = it }
            if (!isVideo(contentType)) {
                throw AssertionError("This fragment can only display video")
            }

            videoView.setWindow(requireActivity().window)
            videoView.setVideoSource(uri, true, context)

        }

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        videoView.play()
    }

    override fun onPause() {
        super.onPause()
        videoView.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        cleanUp()
    }

    override fun cleanUp() {
        videoView.cleanup()
    }


    override fun getPlaybackControls(): View? {
        return videoView.controlView
    }

    private fun getUri(): Uri {
        return requireArguments().getParcelable(DATA_URI)!!
    }
}