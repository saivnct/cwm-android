package com.lgt.cwm.activity.media.fragments

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.provider.MediaStore
import android.view.View
import androidx.fragment.app.Fragment
import cwmSignalMsgPb.CwmSignalMsg

abstract class MediaPreviewFragment : Fragment() {
    companion object {
        const val DATA_URI = "DATA_URI"
        const val DATA_MEDIA = "DATA_MEDIA"
        const val DATA_POSITION = "DATA_POSITION"
        const val DATA_SIZE = "DATA_SIZE"
        const val DATA_CONTENT_TYPE = "DATA_CONTENT_TYPE"
        const val AUTO_PLAY = "AUTO_PLAY"
        const val VIDEO_GIF = "VIDEO_GIF"

//        fun newInstance(fileInfo: CwmSignalMsg.MultimediaFileInfo, autoPlay: Boolean): MediaPreviewFragment {
//            return newInstance(
//                fileInfo.fileUri,
//                fileInfo.mediaType.number,
//                fileInfo.fileSize,
//                autoPlay,
//                false
//            )
//        }

        fun newInstance(uri: Uri, mediaType: String, fileSize: Long, autoPlay: Boolean, isVideoGif: Boolean): MediaPreviewFragment {
            val args = Bundle()
            args.putParcelable(DATA_URI, uri)
            args.putString(DATA_CONTENT_TYPE, mediaType)
            args.putLong(DATA_SIZE, fileSize)
            args.putBoolean(AUTO_PLAY, autoPlay)
            args.putBoolean(VIDEO_GIF, isVideoGif)
            val fragment = createCorrectFragmentType(mediaType)
            fragment.arguments = args
            return fragment
        }

        private fun createCorrectFragmentType(contentType: String): MediaPreviewFragment {
            return if (isVideo(contentType)) {
                VideoMediaPreviewFragment()
            } else if (isImageType(contentType)) {
                ImageMediaPreviewFragment()
            } else {
                throw AssertionError("Unexpected media type: $contentType")
            }
        }

        fun isVideo(contentType: String): Boolean {
            return contentType.isNotEmpty() && contentType.trim().startsWith("video/")
        }

        fun isImageType(contentType: String?): Boolean {
            return if (contentType == null) {
                false
            } else (contentType.startsWith("image/") && contentType != "image/svg+xml") || contentType == MediaStore.Images.Media.CONTENT_TYPE
        }
    }


    override fun onAttach(context: Context) {
        super.onAttach(context)
//        events =
//            if (context is org.thoughtcrime.securesms.mediapreview.MediaPreviewFragment.Events) {
//                context as org.thoughtcrime.securesms.mediapreview.MediaPreviewFragment.Events
//            } else if (parentFragment is org.thoughtcrime.securesms.mediapreview.MediaPreviewFragment.Events) {
//                parentFragment as org.thoughtcrime.securesms.mediapreview.MediaPreviewFragment.Events?
//            } else {
//                throw AssertionError("Parent component must support " + org.thoughtcrime.securesms.mediapreview.MediaPreviewFragment.Events::class.java)
//            }
    }

    override fun onResume() {
        super.onResume()
//        checkMediaStillAvailable()
    }

    open fun cleanUp() {}
    open fun pause() {}
    open fun getPlaybackControls(): View? {
        return null
    }

//    private fun checkMediaStillAvailable() {
//        if (attachmentId == null) {
//            attachmentId = PartUriParser(
//                Objects.requireNonNull(
//                    requireArguments().getParcelable<Parcelable>(
//                        DATA_URI
//                    )
//                )
//            ).getPartId()
//        }
//        SimpleTask.run(
//            viewLifecycleOwner.lifecycle,
//            { SignalDatabase.attachments().hasAttachment(attachmentId) }
//        ) { hasAttachment -> if (!hasAttachment) events.mediaNotAvailable() }
//    }

    interface Events {
        fun singleTapOnMedia(): Boolean
        fun mediaNotAvailable()
        fun onMediaReady()
//        val videoControlsDelegate: VideoControlsDelegate? = null
    }


}