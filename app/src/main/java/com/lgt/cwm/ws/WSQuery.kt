package com.lgt.cwm.ws

import com.google.gson.annotations.SerializedName

/**
 * Created by giangtpu on 03/07/2022.
 */
data class WSQuery(
    @SerializedName("authorization") val authorization: String,
){
    fun toQueryString(): String{
        return "authorization=$authorization"
    }
}
