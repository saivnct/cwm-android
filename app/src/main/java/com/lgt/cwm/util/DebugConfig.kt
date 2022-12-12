@file:Suppress("ImplicitThis")

package com.lgt.cwm.util


import android.util.Log
import com.lgt.cwm.BuildConfig
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Created by giangtpu on 7/25/22.
 */
@Singleton
class DebugConfig @Inject constructor() {
    //region Config
    val SHOW_DEBUG_LOG = true
    val SHOW_ALL_DEBUG_LOG = true
    //endregion Config

    //region Log
    /**
     * Log Message With Method TAG
     */
    fun log(message:String){
            if(SHOW_DEBUG_LOG){
                val str = Thread.currentThread().stackTrace
                var tag = if(str.size > 3){
                    str[3].className +"."+str[3].methodName
                }else{
                    BuildConfig.VERSION_NAME
                }
                log(tag,message)
            }
        }

    /**
     * Log Message With Custom TAG
     */
    fun log(TAG:String, message: String){
            if(SHOW_DEBUG_LOG){
                if(SHOW_ALL_DEBUG_LOG){
                    Log.i(TAG,message)
                }else{
                    Log.d(TAG,message)
                }
            }
        }

    /**
     * Log error with method tag
     */
    fun loge(message: String){
        val str = Thread.currentThread().stackTrace
        var tag = if(str.size > 3){
            str[3].className +"."+str[3].methodName
        }else{
            BuildConfig.VERSION_NAME
        }
        loge(tag,message)
    }

    /**
     * Log error with custom tag
     */
    fun loge(tag:String, message: String){
        Log.e(tag,message)
    }

    //endregion Log
}