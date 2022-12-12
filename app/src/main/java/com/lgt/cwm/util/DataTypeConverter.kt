package com.lgt.cwm.util

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Created by giangtpu on 6/29/22.
 */
class DataTypeConverter {

    companion object{
        inline fun <reified T> fromJson(json: String): T {
            return Gson().fromJson(json, object: TypeToken<T>(){}.type)
        }
    }
}