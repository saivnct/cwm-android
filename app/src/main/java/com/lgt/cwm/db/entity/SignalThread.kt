package com.lgt.cwm.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Created by giangtpu on 7/25/22.
 */
@Entity(tableName = "signalThreads")
class SignalThread (
    @PrimaryKey(autoGenerate = false)
    var threadId: String,
    var threadName: String,         //GROUP: group's name - SOLO: "firstname lastname"
    var phoneFull: String,         //GROUP: empty - SOLO: phone number
    var active: Boolean,
    var verified: Boolean,
    var hidden: Boolean,        //hidden thread for ex: event thread from server to  client
    var threadType: Int,
    var participants: List<String>,
    var admin: Boolean,
    var admins: List<String> = arrayListOf(),
    var creator: String = "",
    var createdAt: Long,
    var lastMsgId: String? = null,
    @ColumnInfo(typeAffinity = ColumnInfo.BLOB)
    var lastMsg: ByteArray? = null,
    var lastMsgImType: Int? = null,
    var lastMsgStatus: Int? = null,
    var lastMsgDate: Long? = null,
    var lastMsgServerDate: Long? = null,
    var unreadMsgs: Long = 0,
    var lastModified: Long = 0,
    var lastServerModified: Long = 0,
    var lastViewPos: Int? = null
    ){

    fun hasLastMsg(): Boolean{
        return !lastMsgId.isNullOrEmpty() &&
                lastMsgImType != null
    }

}