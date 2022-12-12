package com.lgt.cwm.http.request

import android.graphics.Bitmap

data class APIUploadProfile (
        val name: String,
        val phone: String,
        val idCard: String?,
        val idFront: Bitmap?,
        val idBack: Bitmap?,
        val selfie: Bitmap?,
)