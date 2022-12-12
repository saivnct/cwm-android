package com.lgt.cwm.util

import android.util.Base64
import com.lgt.cwm.models.NationalPhoneCode
import com.lgt.cwm.util.Constants.COUNTRY_CODE_PREFIX
import com.lgt.cwm.util.Constants.PATTERN_NUMERIC
import java.io.InputStream
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.regex.Pattern

/**
 * Created by giangtpu on 7/7/22.
 */

fun ByteArray.toHex(): String = joinToString(separator = "") { eachByte -> "%02x".format(eachByte) }

fun ByteArray.md5(): String {
    val md5 = MessageDigest.getInstance("md5")
    md5.update(this)
    return md5.digest().toHex()
}

fun ByteArray.encodeBase64(): String {
    return Base64.encodeToString(this, Base64.DEFAULT)
}
fun ByteArray.decodeBase64(): String {
    return Base64.decode(this, Base64.DEFAULT).toString(charset("UTF-8"))
}



fun InputStream.md5(): String {
    val md5 = MessageDigest.getInstance("md5")
    val buffer = ByteArray(1024) //bufferSize = 1024
    var len = 0
    while (this.read(buffer).also { len = it } != -1) {
        md5.update(buffer, 0, len);
    }
    return md5.digest().toHex()
}

fun String.md5(): String {
    val md5 = MessageDigest.getInstance("md5")
    md5.update(this.toByteArray())
    return md5.digest().toHex()
}

fun String.sha256String(): String {
    val sha256 = MessageDigest.getInstance("SHA-256")
    sha256.update(this.toByteArray())
    return sha256.digest().toHex()
}

fun String.ecodeBase64(): String {
    return Base64.encodeToString(this.toByteArray(), Base64.DEFAULT)
}
fun String.decodeBase64(): String {
    return Base64.decode(this, Base64.DEFAULT).toString(charset("UTF-8"))
}


fun String.getStandardizedPhoneNumber(nationalPhoneCode: NationalPhoneCode): String {
    val mNumberPattern = Pattern.compile(PATTERN_NUMERIC)
    val phoneNumber: String = mNumberPattern.matcher(this).replaceAll("")
    if (phoneNumber.startsWith(COUNTRY_CODE_PREFIX)) {
        return phoneNumber
    }
    val internationalPrefix: String = nationalPhoneCode.internationalPrefix
    val nationalPrefix: String = nationalPhoneCode.nationalPrefix
    val countryCode: String = nationalPhoneCode.countryCode

    if (phoneNumber.startsWith(countryCode)) {
        return "+$phoneNumber"
    }else{
        if (phoneNumber.startsWith(internationalPrefix+countryCode)){
            return "+" + phoneNumber.substring(
                internationalPrefix.length,
                phoneNumber.length
            )
        } else if (phoneNumber.startsWith(nationalPrefix)){
            return "+" + countryCode + phoneNumber.substring(
                nationalPrefix.length,
                phoneNumber.length
            )
        }else{
            return "+$countryCode$phoneNumber"
        }
    }
}

fun String.sha256(): ByteArray {
    val sha256 = MessageDigest.getInstance("SHA-256")
    sha256.update(this.toByteArray())
    return sha256.digest()
}

object StringUtil {
    fun getNonceResponse(nonce: String, password: String): String{
        val secret = password + nonce
        return secret.sha256String()
    }

    fun getThreadId(sender: String, receiver: String): String{
        var concat = ""
        if (sender.compareTo(receiver) < 0){
            concat = sender + "_" + receiver
        }else {
            concat = receiver + "_" + sender
        }

        return concat.sha256String()
    }

    private const val AB = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"
    private var rnd: SecureRandom = SecureRandom()

    fun randomString(length: Int): String {
        val sb = StringBuilder(length)
        for (i in 0 until length) sb.append(AB[rnd.nextInt(AB.length)])
        return sb.toString()
    }

    fun emptyIfNull(value: String?): String {
        return value ?: ""
    }


}
