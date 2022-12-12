package com.lgt.cwm.models

import android.content.Context
import android.text.SpannableString
import com.lgt.cwm.R
import com.lgt.cwm.business.media.mention.MentionAnnotation
import com.lgt.cwm.db.entity.SignalThread
import com.lgt.cwm.util.DateUtil
import com.lgt.cwm.util.MentionUtil
import com.lgt.cwm.util.SignalMsgHelper
import cwmSignalMsgPb.CwmSignalMsg

//Wrapper data class of signalThread
data class SignalThreadExt (
    val threadId: String,
    val threadName: String,         //GROUP: group's name - SOLO: "firstname lastname"
    val phoneFull: String,         //GROUP: empty - SOLO: phone number
    val active: Boolean,
    val verified: Boolean,
    val hidden: Boolean,        //hidden thread for ex: event thread from server to  client
    val threadType: Int,
    val participants: List<String>,
    val participantInfos: List<ThreadParticipantInfo>,
    val admin: Boolean,
    val admins: List<String> = arrayListOf(),
    val creator: String = "",
    val createdAt: Long,
    var lastSignalMsgExt: SignalMsgExt? = null,
    val unreadMsgs: Long = 0,
    val lastModified: Long = 0,
    val lastServerModified: Long = 0,
    val lastViewPos: Int? = null
){

    constructor(signalThread: SignalThread, threadName: String, participantInfos: List<ThreadParticipantInfo>): this(
        threadId = signalThread.threadId,
        threadName = threadName,
        phoneFull = signalThread.phoneFull,
        active = signalThread.active,
        verified = signalThread.verified,
        hidden = signalThread.hidden,
        threadType = signalThread.threadType,
        participants = signalThread.participants,
        participantInfos = participantInfos,
        admin = signalThread.admin,
        admins = signalThread.admins,
        creator = signalThread.creator,
        createdAt = signalThread.createdAt,
        unreadMsgs = signalThread.unreadMsgs,
        lastModified = signalThread.lastModified,
        lastServerModified = signalThread.lastServerModified,
        lastViewPos = signalThread.lastViewPos,
    ) {
        if (signalThread.hasLastMsg()){
            this.lastSignalMsgExt = SignalMsgExt(signalThread)
        }
    }

    fun unreadMsgsStr(): String {
        return "${this.unreadMsgs}"
    }

    fun getLastMsgTimeStr(): String {
        this.lastSignalMsgExt?.let {
            return DateUtil.convertLongToTimeHourChat(it.msgDate)
        }
        return ""
    }

    fun getLastMsgContentStr(context: Context): String{
        this.lastSignalMsgExt?.let {  lastSignalMsgExt ->
            return  getMsgContentStr(context, lastSignalMsgExt)
        }

        return ""
    }

    fun getMsgContentStr(context: Context, signalMsgExt: SignalMsgExt): String {
        signalMsgExt.content?.let { lastMsg ->
            when (signalMsgExt.imType) {
                CwmSignalMsg.SIGNAL_IM_TYPE.IM.number -> {
                    val bodyAndMentions = MentionUtil.extractMentionsFromContent(signalMsgExt.getIMMessageContent())
                    if (bodyAndMentions.mentions.isNotEmpty()) {
                        val messageBody = SpannableString(bodyAndMentions.body)
                        MentionAnnotation.setMentionAnnotations(messageBody, bodyAndMentions.mentions)
                        return messageBody.toString()
                    } else return signalMsgExt.getIMMessageContent()
                }
                CwmSignalMsg.SIGNAL_IM_TYPE.CONTACT.number -> {
                    return context.getString(R.string.im_type_content_contact)
                }
                CwmSignalMsg.SIGNAL_IM_TYPE.EMOTICON.number -> {
                    return context.getString(R.string.im_type_content_emoticon)
                }
                CwmSignalMsg.SIGNAL_IM_TYPE.URL.number -> {
                    val signalURLMessageProto = try{
                        CwmSignalMsg.SignalURLMessage.parseFrom(lastMsg)
                    }catch (e: Throwable){null}

                    return signalURLMessageProto?.data?.toString(Charsets.UTF_8) ?: context.getString(R.string.im_type_content_url)
                }
                CwmSignalMsg.SIGNAL_IM_TYPE.MULTIMEDIA.number -> {
                    return  signalMsgExt.getLastSignalMultimediaMessageContent(context)
                }
                CwmSignalMsg.SIGNAL_IM_TYPE.GROUP_THREAD_NOTIFICATION.number -> {
                    return signalMsgExt.getSignalGroupThreadNotificationMessageContent(context, this.participantInfos)
                }
                CwmSignalMsg.SIGNAL_IM_TYPE.FORWARD.number -> {
                    signalMsgExt.contentSignalForwardMsg?.let { contentSignalForwardMsg ->
                        val originMsgExt = SignalMsgExt(
                            contentSignalForwardMsg = contentSignalForwardMsg,
                            msgDate = signalMsgExt.msgDate,
                            serverDate = signalMsgExt.serverDate,)
                        return getMsgContentStr(context, originMsgExt)
                    }
                }
                else -> {
                    return ""
                }
            }
        }


        return ""
    }

    fun getLastMsgChecksum(): String{
        this.lastSignalMsgExt?.let {
            return it.checksum
        }

        return ""
    }

    fun getLastMsgStatus(): Int{
        this.lastSignalMsgExt?.let {
            return it.status
        }

        return 0
    }

    fun isContentTheSameWith(other: SignalThreadExt): Boolean{
        if (!this.threadId.equals(other.threadId)){
            return false
        }

        if (!this.threadName.equals(other.threadName)){
            return false
        }

        if (!this.participantInfos.equals(other.participantInfos)){
            return false
        }

        if (!this.participants.equals(other.participants)){
            return false
        }

        if (!this.getLastMsgChecksum().equals(other.getLastMsgChecksum())){
            return false
        }

        if (this.getLastMsgStatus() != other.getLastMsgStatus()){
            return false
        }

        if (this.lastModified != other.lastModified){
            return false
        }

        if (this.unreadMsgs != other.unreadMsgs){
            return false
        }

        return true
    }

}