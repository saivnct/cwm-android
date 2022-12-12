package com.lgt.cwm.http.response

import com.google.gson.JsonElement

/**
 * Created by giangtpu on 10/26/20.
 */
class DataPagingRespone {
    var totalItem: Long? = null
    var pageSized: Int? = null
    var page: Int? = null
    var items: List<JsonElement>? = null
}