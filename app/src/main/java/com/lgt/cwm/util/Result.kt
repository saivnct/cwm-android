package com.lgt.cwm.util

/**
 * Created by giangtpu on 6/29/22.
 */
sealed class Result<out R> {
    data class Success<out T>(val data: T) : Result<T>()
    data class Error(val exception: Throwable) : Result<Nothing>()
}