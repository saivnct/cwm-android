package com.lgt.cwm.util

/**
 * Config for app
 * @author giangtpu
 * April 1st 2020
 */

object Config {
    object SharePreference{
        const val SHARE_PREF_NAME = "CWM_SHARE_PREF"
        const val SHARE_PREF_COOKIES = "SHARE_PREF_COOKIES"
        const val SET_LANGUAGE = "SET_LANGUAGE"
        const val SHARE_PREF_TOKEN = "SHARE_PREF_TOKEN"
        const val SHARE_PREF_FCM_TOKEN = "SHARE_PREF_FCM_TOKEN"
        const val SHARE_PREF_INITIAL_SYNC_MSG = "SHARE_PREF_INITIAL_SYNC_MSG"
        const val SHARE_PREF_LAST_FETCH_MSG_SVDATE = "SHARE_PREF_LAST_FETCH_MSG_SVDATE"
    }

    object GRPC{
        //cannot use localhost -> When running on the Android emulator, localhost is the Android device
//        const val HOST = "cwmlocalhost"
        const val HOST = "192.168.1.179"
        const val PORT = 50050
        const val TLS = false
        const val CHANNEL_CREDENTIAL_AUTHENTICATION = true
        const val TLS_CERT_FILE_NAME = "server.crt"
        const val TIMEOUT : Long = 30 //30 second
    }

    object WS{
        const val HOST = "192.168.1.179"
        const val PORT = 9000
        const val PATH = "/ws"
    }

    object IntentAction{
        const val OPEN_CHAT = "com.lgt.cwm.openchat"
    }
}