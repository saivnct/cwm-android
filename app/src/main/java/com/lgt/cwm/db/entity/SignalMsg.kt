package com.lgt.cwm.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Created by giangtpu on 7/25/22.
 */
@Entity(tableName = "signalMsgs")
class SignalMsg (
    @PrimaryKey(autoGenerate = false)
    var msgId: String,
    var threadId: String,
    var from: String,
    var fromFirstName: String,
    var fromLastName: String,
    var fromUserName: String,
    var to: String,
    var threadType: Int,
    var replyMsgId: String,
    var imType: Int,
    var msgDate: Long,
    var confirmReceive: Boolean,
    var sendSeenState: Boolean,
    var eventHandled: Boolean,
    var multiMediaDownloadHandled: Boolean,
    var serverDate: Long = 0L,
    var checksum: String,
    @ColumnInfo(typeAffinity = ColumnInfo.BLOB)
    var content: ByteArray? = null,

    var status: Int = SignalMsgStatus.UNKNOWN.code,
    var seenBy: ArrayList<String> = arrayListOf(),
    var direction: Int = SignalMsgDirection.INCOMING.code,
    var threadVerified: Boolean,
){

}

enum class SignalMsgStatus(val code: Int){
    UNKNOWN(0),
    SENDING(1),
    SENT_FAIL(2),
    SENT(3),
    SENT_SEEN(4),
    SENT_SEEN_ALL(5),
    RECEIVED_UNREAD(6),
    RECEIVED_SEEN(7)
}

enum class SignalMsgDirection(val code: Int){
    INCOMING(0),
    OUTGOING(1),
    OUTGOING_DIFF_SESSION(2),
}