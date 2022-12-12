package com.lgt.cwm.http.response

import com.google.gson.annotations.SerializedName

/**
 * Created by giangtpu on 6/29/22.
 */
class APILoginResponse {
    @SerializedName("access_token")
    var access_token: String? = null
    @SerializedName("token_type")
    var token_type: String? = null
    @SerializedName("expires_at")
    var expires_at: String? = null

}