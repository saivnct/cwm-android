package com.lgt.cwm.activity.conversation.fragments.models

import com.lgt.cwm.business.media.linkpreview.LinkPreview
import com.lgt.cwm.business.media.linkpreview.LinkPreviewRepository

class LinkPreviewState constructor(
    val activeUrlForError: String?,
    val isLoading: Boolean,
    val hasLinks: Boolean,
    val linkPreview: LinkPreview?,
    val error: LinkPreviewRepository.Error?
) {


    fun hasContent(): Boolean {
        return isLoading || hasLinks
    }

    companion object {
        fun forLoading(): LinkPreviewState {
            return LinkPreviewState(null, true, false, null, null)
        }

        fun forPreview(linkPreview: LinkPreview): LinkPreviewState {
            return LinkPreviewState(null, false, true, linkPreview, null)
        }

        fun forLinksWithNoPreview(activeUrlForError: String?, error: LinkPreviewRepository.Error): LinkPreviewState {
            return LinkPreviewState(
                activeUrlForError,
                false,
                true,
                null,
                error
            )
        }

        fun forNoLinks(): LinkPreviewState {
            return LinkPreviewState(null, false, false, null, null)
        }
    }

}