package com.lgt.cwm.db

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Created by giangtpu on 7/25/22.
 */
class DatabaseTypeConverter {
    /**
     * @note: type converter between string json and string array.
     * @param value
     * @return
     */
    @TypeConverter
    fun listFromString(value: String): List<String> {
        val listType = object : TypeToken<List<String>>() {}.type
        return Gson().fromJson(value, listType)
    }

    @TypeConverter
    fun stringFromList(list: List<String>): String {
        val gson = Gson()
        return gson.toJson(list)
    }

    @TypeConverter
    fun arrayListFromString(value: String): ArrayList<String> {
        val listType = object : TypeToken<ArrayList<String>>() {}.type
        return Gson().fromJson(value, listType)
    }

    @TypeConverter
    fun stringFromArrayList(list: ArrayList<String>): String {
        val gson = Gson()
        return gson.toJson(list)
    }

}