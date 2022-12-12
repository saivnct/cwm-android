package com.lgt.cwm.models

import android.content.Context
import com.lgt.cwm.R

/**
 * Created by giangtpu on 04/10/2022
 */
data class ThreadParticipantInfo (
    val phoneFull: String,     //sha256(standardizedPhoneNumber+username)
    val contactName: String,  //name in Contact table
    val userId: String,
    val username: String,
    val avatar: String,
    val firstName: String,
    val lastName: String,
    val isMyAcc: Boolean,
){
    fun getName(context: Context): String{
        if (isMyAcc){
            return  context.getString(R.string.you)
        }

        if (contactName.isNotEmpty()){
            return contactName
        }

        if (firstName.isNotEmpty()){
            if (lastName.isNotEmpty()){
                return "$firstName $lastName"
            }
            return firstName
        }

        if (!username.isNullOrEmpty()){
            return username
        }

        return phoneFull
    }

    override fun equals(other: Any?): Boolean {
        when (other) {
            is ThreadParticipantInfo -> {
                return this.phoneFull.equals(other.phoneFull) &&
                        this.contactName.equals(other.contactName) &&
                        this.userId.equals(other.userId) &&
                        this.username.equals(other.username) &&
                        this.avatar.equals(other.avatar) &&
                        this.firstName.equals(other.firstName) &&
                        this.lastName.equals(other.lastName)
            }
            else -> {
                return false
            }
        }
    }

}