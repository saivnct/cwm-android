package com.lgt.cwm.http.response

import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName

/**
 * Created by giangtpu on 6/29/22.
 */
class BasePaginationAPIResponse {
    @SerializedName("from")
    var from: Int? = null
    @SerializedName("to")
    var to: Int? = null
    @SerializedName("per_page")
    var per_page: Int? = null
    @SerializedName("total")
    var total: Int? = null
    @SerializedName("current_page")
    var current_page: Int? = null
    @SerializedName("last_page")
    var last_page: Int? = null
    @SerializedName("first_page_url")
    var first_page_url: String? = null
    @SerializedName("last_page_url")
    var last_page_url: String? = null
    @SerializedName("next_page_url")
    var next_page_url: String? = null
    @SerializedName("prev_page_url")
    var prev_page_url: String? = null
    @SerializedName("path")
    var path: String? = null
    @SerializedName("data")
    var data: JsonElement? = null
}