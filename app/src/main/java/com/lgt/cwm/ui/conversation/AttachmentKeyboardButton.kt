package com.lgt.cwm.ui.conversation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.lgt.cwm.R

enum class AttachmentKeyboardButton(@StringRes val titleRes: Int, @DrawableRes val iconRes: Int) {
    CAMERA(R.string.AttachmentKeyboard_camera, R.drawable.ic_photo_camera_white_24),
    GALLERY(R.string.AttachmentKeyboard_gallery, R.drawable.ic_gallery_white_24),
    FILE(R.string.AttachmentKeyboard_file, R.drawable.ic_file_white_24),
    CONTACT(R.string.AttachmentKeyboard_contact, R.drawable.ic_contact_white_24),
}