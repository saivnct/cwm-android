package com.lgt.cwm.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Created by giangtpu on 04/10/2022
 */

@Entity(tableName = "cwmUser")
data class CWMUser(
    @PrimaryKey(autoGenerate = false)
    var phoneFull: String,     //sha256(standardizedPhoneNumber+username)
    var isMyAcc: Boolean,     //sha256(standardizedPhoneNumber+username)
    var userId: String?,
    var username: String?,
    var avatar: String?,
    var firstName: String?,
    var lastName: String?,
){
}