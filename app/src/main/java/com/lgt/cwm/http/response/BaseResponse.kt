package com.lgt.cwm.http.response

import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName

/**
 * Created by giangtpu on 6/29/22.
 */
class BaseResponse {
    @SerializedName("status")
    var status: Int = 0 //1:ok 0:fail

    @SerializedName("errorCode")
    var errorCode: Int = 0

    @SerializedName("msg")
    var msg: String? = null

    @SerializedName("data")
    var data: JsonElement? = null

    fun isSuccess() : Boolean{
        return status == 1
    }
}