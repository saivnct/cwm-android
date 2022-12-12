package com.lgt.cwm.util

/**
 * Created by giangtpu on 7/25/22.
 */
object Constants {

    object ServerName {
        const val ServerName = "cwmServer"
        const val ServerEventName = "cwmServerEvent"
    }


    const val PATTERN_NUMERIC = "[^+0-9]";
    const val COUNTRY_CODE_PREFIX = "+";
    const val MIN_LENGTH_PHONE_NUMBER = 8;

    const val MAXIMUM_GROUP_MEMBER = 50
    const val MAXIMUM_FORWARD = 20

    const val AUTHENCODE_LENGTH = 6
    const val GROUPNAME_MIN_LENGTH = 3

    const val S3NamePrefxix = "cwm_ttl_"
}