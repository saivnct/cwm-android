package com.lgt.cwm.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Created by giangtpu on 6/29/22.
 */
@Entity(tableName = "accounts")
data class Account(
    var phoneFull: String,
    var phone: String,
    var countryCode: String,
    var username: String,
    var password: String,
    var firstName: String,
    var lastName: String,
    var imei: String,
    var sessionId: String,
    var nonce: String,
    var nonceResponse: String,
    var nonceAt: Long,
    var jwt: String,
    var jwtAt: Long,
    var jwtTTL: Int,    //in minute
    var status: Int,
    var isActive: Boolean,
    var createdAt: Long,
    var lastAttempLoginAt: Long,
    ){
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0

    fun isLogin(): Boolean{
        return this.status == AccountLoginStatus.LOGIN.code
    }

    fun titleName(): String{
        if (!username.isNullOrEmpty()){
            return username
        }else{
            return "${firstName} ${lastName}"
        }
    }
}

enum class AccountLoginStatus(val code: Int){
    DOING_LOGIN(0),
    LOGOUT(1),
    LOGIN(2),
}