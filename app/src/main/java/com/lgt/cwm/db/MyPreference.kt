package com.lgt.cwm.db

import android.content.Context
import android.content.SharedPreferences
import com.lgt.cwm.util.Config.SharePreference.SET_LANGUAGE
import com.lgt.cwm.util.Config.SharePreference.SHARE_PREF_COOKIES
import com.lgt.cwm.util.Config.SharePreference.SHARE_PREF_FCM_TOKEN
import com.lgt.cwm.util.Config.SharePreference.SHARE_PREF_INITIAL_SYNC_MSG
import com.lgt.cwm.util.Config.SharePreference.SHARE_PREF_LAST_FETCH_MSG_SVDATE
import com.lgt.cwm.util.Config.SharePreference.SHARE_PREF_NAME
import com.lgt.cwm.util.Config.SharePreference.SHARE_PREF_TOKEN
import com.lgt.cwm.util.DebugConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Share preference for app
 * @author giangtpu
 * April 1st 2020
 */

@Singleton
class MyPreference @Inject constructor(
    @ApplicationContext private val context: Context,
    private val  debugConfig: DebugConfig) {

    private val TAG  = MyPreference::class.simpleName.toString()

    private val customPrefs: SharedPreferences by lazy{
        context.getSharedPreferences(SHARE_PREF_NAME, Context.MODE_PRIVATE)
    }

    //region last fetch msg
    fun setLastFetchMsgServerDate(serverDate :Long){
        val editor =  customPrefs.edit()
        editor.putLong(SHARE_PREF_LAST_FETCH_MSG_SVDATE,serverDate)
        editor.apply()
    }

    fun getLastFetchMsgServerDate() :Long{
        return customPrefs.getLong(SHARE_PREF_LAST_FETCH_MSG_SVDATE,0)
    }
    //endregion

    //region InitialSyncMs
    fun setInitialSyncMsg(isSynced :Boolean){
        val editor =  customPrefs.edit()
        editor.putBoolean(SHARE_PREF_INITIAL_SYNC_MSG,isSynced)
        editor.apply()
    }

    fun getInitialSyncMsg() :Boolean{
        return customPrefs.getBoolean(SHARE_PREF_INITIAL_SYNC_MSG,false)
    }
    //endregion


    //region FCM
    fun setFCMToken(token :String){
        val editor =  customPrefs.edit()
        editor.putString(SHARE_PREF_FCM_TOKEN,token)
        editor.apply()
    }

    fun getFCMToken() :String?{
        return customPrefs.getString(SHARE_PREF_FCM_TOKEN,null)
    }
    //endregion

    //region Access token
    fun setToken(token :String){
        val editor =  customPrefs.edit()
        editor.putString(SHARE_PREF_TOKEN,token)
        editor.apply()
    }

    fun getToken() :String?{
        return customPrefs.getString(SHARE_PREF_TOKEN,null)
    }
    //endregion Access token

    //region LANGUAGE
    fun saveLanguage(lang: String?) {
        val editor =  customPrefs.edit()
        editor.putString(SET_LANGUAGE, lang)
        editor.apply()
    }

    fun loadLanguage(): Locale? {
        val lang = customPrefs.getString(SET_LANGUAGE,"")
        return if (lang == "vi") {
            Locale("vi", "VN")
        } else {
            Locale.ENGLISH
        }
    }
    //endregion LANGUAGE

    //region COOKIES
    fun getCookies(): HashSet<String> {
        return customPrefs.getStringSet(SHARE_PREF_COOKIES, HashSet<String>()) as HashSet<String>
    }

    fun setCookies(cookies: HashSet<String>){
        debugConfig.log(TAG, "set cookie $cookies")
        customPrefs.edit().apply {
            putStringSet(SHARE_PREF_COOKIES, cookies)
            commit()
        }
    }

    fun clearCookies(){
        debugConfig.loge(TAG, "clear cookie!!!")
        setCookies(HashSet<String>())
    }
    //endregion

}