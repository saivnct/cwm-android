package com.lgt.cwm.activity.media.models

import android.net.Uri
import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class MediaFileInfo(
    val fileUri: Uri,
    val mediaType: String = "",
    val mediaSize: Long = 0
) : Parcelable