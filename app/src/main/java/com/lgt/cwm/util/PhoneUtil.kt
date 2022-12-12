package com.lgt.cwm.util

import android.content.Context
import com.lgt.cwm.R
import com.lgt.cwm.models.NationalPhoneCode

/**
 * Created by giangtpu on 7/18/22.
 */
object PhoneUtil {
    fun getPhoneFull(countryCode: String, phone: String,): String{
        return "+$countryCode$phone"
    }

    fun getNationalPhoneCodeFromCountryCode(countryCode: String, context: Context): NationalPhoneCode {
        val rl = context.resources.getStringArray(R.array.CountryCodes)

        var internationalPrefix = "00"
        var nationalPrefix = ""
        for (i in rl.indices) {
            val g = rl[i].split(",")
            if (g[0].trim() == countryCode) {
                internationalPrefix = g[2]
                if (g.size > 3) {
                    nationalPrefix = g[3]
                }
                break
            }
        }

        return NationalPhoneCode(
            countryCode = countryCode,
            internationalPrefix = internationalPrefix,
            nationalPrefix = nationalPrefix)
    }
}