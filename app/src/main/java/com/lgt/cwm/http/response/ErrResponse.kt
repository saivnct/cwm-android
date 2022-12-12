package com.lgt.cwm.http.response

import com.google.gson.annotations.SerializedName

/**
 * Created by giangtpu on 6/29/22.
 */
class ErrResponse {
    @SerializedName("status")
    var status: String? = null

    @SerializedName("error")
    var error: String? = null

    @SerializedName("msg")
    var msg: String? = null
}