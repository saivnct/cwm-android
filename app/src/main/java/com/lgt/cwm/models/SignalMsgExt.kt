package com.lgt.cwm.models

import android.content.Context
import com.lgt.cwm.R
import com.lgt.cwm.db.entity.SignalMsg
import com.lgt.cwm.db.entity.SignalMsgDirection
import com.lgt.cwm.db.entity.SignalMsgStatus
import com.lgt.cwm.db.entity.SignalThread
import com.lgt.cwm.util.md5
import cwmSignalMsgPb.CwmSignalMsg

/**
 * Created by giangtpu on 25/08/2022.
 */
data class SignalMsgExt(
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
    var serverDate: Long = 0L,
    var checksum: String,
    var content: ByteArray?,

    var contentIMMessage: String? = null,
    var contentSignalMultimediaMessage: CwmSignalMsg.SignalMultimediaMessage? = null,

    var contentSignalGroupThreadNotificationMessage: CwmSignalMsg.SignalGroupThreadNotificationMessage? = null,
    var groupExecutorInfo: ThreadParticipantInfo? = null,
    var groupCreatorInfo: ThreadParticipantInfo? = null,
    var groupTargetMemberInfoStr: String? = null,

    var contentSignalURLMessage: CwmSignalMsg.SignalURLMessage? = null,
    var dataIMSignalURLMessage: String? = null,


    var contentSignalForwardMsg: CwmSignalMsg.SignalForwardMessage? = null,

    var status: Int = SignalMsgStatus.UNKNOWN.code,
    var seenBy: ArrayList<String> = arrayListOf(),
    var direction: Int = SignalMsgDirection.INCOMING.code,
) {

    constructor(signalThread: SignalThread): this(
        msgId = signalThread.lastMsgId ?: "",
        threadId = signalThread.threadId,
        from = "",
        fromFirstName = "",
        fromLastName = "",
        fromUserName = "",
        to = "",
        threadType = signalThread.threadType,
        replyMsgId = "",
        imType = signalThread.lastMsgImType ?: 0,
        msgDate = signalThread.lastMsgDate ?: 0,
        serverDate = signalThread.lastMsgServerDate ?: 0,
        checksum = signalThread.lastMsg?.md5() ?: "",
        content = signalThread.lastMsg,
        status = signalThread.lastMsgStatus ?: 0
    ){
        initialParseMsg()
    }

    constructor(contentSignalForwardMsg: CwmSignalMsg.SignalForwardMessage, msgDate: Long, serverDate: Long): this(
        msgId = contentSignalForwardMsg.msgId,
        threadId = contentSignalForwardMsg.threadId,
        from = contentSignalForwardMsg.from,
        fromFirstName = contentSignalForwardMsg.fromFirstName,
        fromLastName = contentSignalForwardMsg.fromLastName,
        fromUserName = contentSignalForwardMsg.fromUserName,
        to = "",
        threadType = contentSignalForwardMsg.threadType.number,
        replyMsgId = "",
        imType = contentSignalForwardMsg.imType.number,
        msgDate = msgDate,
        serverDate = serverDate,
        checksum = contentSignalForwardMsg.checksum,
        content = contentSignalForwardMsg.data.toByteArray(),
    ){
        initialParseMsg()
    }

    constructor(signalMsg: SignalMsg): this(
        msgId = signalMsg.msgId,
        threadId = signalMsg.threadId,
        from = signalMsg.from,
        fromFirstName = signalMsg.fromFirstName,
        fromLastName = signalMsg.fromLastName,
        fromUserName = signalMsg.fromUserName,
        to = signalMsg.to,
        threadType = signalMsg.threadType,
        replyMsgId = signalMsg.replyMsgId,
        imType = signalMsg.imType,
        msgDate = signalMsg.msgDate,
        serverDate = signalMsg.serverDate,
        checksum = signalMsg.checksum,
        content = signalMsg.content,
        status = signalMsg.status,
        seenBy = signalMsg.seenBy,
        direction = signalMsg.direction,
    ) {
        initialParseMsg()
    }

    private fun initialParseMsg(){
        when(this.imType) {
            CwmSignalMsg.SIGNAL_IM_TYPE.IM.number -> {
                this.contentIMMessage = this.content?.toString(Charsets.UTF_8) ?: ""
            }

            CwmSignalMsg.SIGNAL_IM_TYPE.MULTIMEDIA.number -> {
                this.contentSignalMultimediaMessage = try{
                    CwmSignalMsg.SignalMultimediaMessage.parseFrom(this.content)
                }catch (e: Throwable){null}
            }

            CwmSignalMsg.SIGNAL_IM_TYPE.GROUP_THREAD_NOTIFICATION.number -> {
                this.contentSignalGroupThreadNotificationMessage = try{
                    CwmSignalMsg.SignalGroupThreadNotificationMessage.parseFrom(this.content)
                }catch (e: Throwable){null}
            }

            CwmSignalMsg.SIGNAL_IM_TYPE.URL.number -> {
                this.contentSignalURLMessage = try{
                    CwmSignalMsg.SignalURLMessage.parseFrom(this.content)
                }catch (e: Throwable){null}

                this.dataIMSignalURLMessage = this.contentSignalURLMessage?.data?.toString(Charsets.UTF_8) ?: ""
            }

            CwmSignalMsg.SIGNAL_IM_TYPE.FORWARD.number -> {
                this.contentSignalForwardMsg = try {
                    CwmSignalMsg.SignalForwardMessage.parseFrom(this.content)
                }catch (e: Throwable){null}
            }
        }
    }

    fun getIMMessageContent(): String{
       return this.contentIMMessage ?: ""
    }

    fun getSignalGroupThreadNotificationMessageContent(context: Context): String{
        val signalGroupThreadNotificationMessageProto = this.contentSignalGroupThreadNotificationMessage ?: return ""

        val groupName = signalGroupThreadNotificationMessageProto.groupName

        val executor = signalGroupThreadNotificationMessageProto.executor
        val creator = signalGroupThreadNotificationMessageProto.creator
        val targetMembers = signalGroupThreadNotificationMessageProto.targetMembersList
        val targetMemberInfoStr = groupTargetMemberInfoStr ?: targetMembers.joinToString(separator = ", ")

        val content = when (signalGroupThreadNotificationMessageProto.notificationType) {
            CwmSignalMsg.SIGNAL_GROUP_THREAD_NOTIFICATION_MSG_TYPE.GROUP_THREAD_CREATED
            -> context.getString(R.string.group_thread_notification_created, groupCreatorInfo?.getName(context) ?: creator)

            CwmSignalMsg.SIGNAL_GROUP_THREAD_NOTIFICATION_MSG_TYPE.GROUP_THREAD_LEAVE
            -> context.getString(R.string.group_thread_notification_left, groupExecutorInfo?.getName(context) ?: executor)

            CwmSignalMsg.SIGNAL_GROUP_THREAD_NOTIFICATION_MSG_TYPE.GROUP_THREAD_ADD_USERS
            -> context.getString(R.string.group_thread_notification_add_new_users, groupExecutorInfo?.getName(context) ?: executor, targetMemberInfoStr)

            CwmSignalMsg.SIGNAL_GROUP_THREAD_NOTIFICATION_MSG_TYPE.GROUP_THREAD_REMOVE_USERS
            -> context.getString(R.string.group_thread_notification_remove_user, groupExecutorInfo?.getName(context) ?: executor, targetMemberInfoStr)

            CwmSignalMsg.SIGNAL_GROUP_THREAD_NOTIFICATION_MSG_TYPE.GROUP_THREAD_PROMOTE_ADMIN
            -> context.getString(R.string.group_thread_notification_promote_admin, groupExecutorInfo?.getName(context) ?: executor, targetMemberInfoStr)

            CwmSignalMsg.SIGNAL_GROUP_THREAD_NOTIFICATION_MSG_TYPE.GROUP_THREAD_REVOKE_ADMIN
            -> context.getString(R.string.group_thread_notification_revoke_admin, groupExecutorInfo?.getName(context) ?: executor, targetMemberInfoStr)
            CwmSignalMsg.SIGNAL_GROUP_THREAD_NOTIFICATION_MSG_TYPE.GROUP_THREAD_CHANGE_NAME
            -> context.getString(R.string.group_thread_notification_change_groupname, groupExecutorInfo?.getName(context) ?: executor, groupName)

            else -> context.getString(R.string.im_type_content_group_thread_notification)
        }
        return content
    }

    fun getSignalGroupThreadNotificationMessageContent(context: Context, participantInfos: List<ThreadParticipantInfo>): String{
        val signalGroupThreadNotificationMessageProto = this.contentSignalGroupThreadNotificationMessage ?: return ""

        val groupName = signalGroupThreadNotificationMessageProto.groupName

        val executor = signalGroupThreadNotificationMessageProto.executor
        val executorInfo = participantInfos.firstOrNull { participantInfo -> participantInfo.phoneFull.equals(executor) }

        val creator = signalGroupThreadNotificationMessageProto.creator
        val creatorInfo = participantInfos.firstOrNull { participantInfo -> participantInfo.phoneFull.equals(creator) }

        val targetMembers = signalGroupThreadNotificationMessageProto.targetMembersList
        val targetMemberNames = targetMembers.map { targetMember ->
            val targetMemberInfo = participantInfos.firstOrNull { participantInfo -> participantInfo.phoneFull.equals(targetMember) }
            return targetMemberInfo?.getName(context) ?: targetMember
        }

        val targetMemberInfoStr = targetMemberNames.joinToString(separator = ", ")

        when (signalGroupThreadNotificationMessageProto?.notificationType) {
            CwmSignalMsg.SIGNAL_GROUP_THREAD_NOTIFICATION_MSG_TYPE.GROUP_THREAD_CREATED -> {
                return context.getString(R.string.group_thread_notification_created, creatorInfo?.getName(context) ?: creator)
            }
            CwmSignalMsg.SIGNAL_GROUP_THREAD_NOTIFICATION_MSG_TYPE.GROUP_THREAD_LEAVE -> {
                return context.getString(R.string.group_thread_notification_left, executorInfo?.getName(context) ?: executor)
            }
            CwmSignalMsg.SIGNAL_GROUP_THREAD_NOTIFICATION_MSG_TYPE.GROUP_THREAD_ADD_USERS -> {
                return context.getString(R.string.group_thread_notification_add_new_users, executorInfo?.getName(context) ?: executor, targetMemberInfoStr)
            }
            CwmSignalMsg.SIGNAL_GROUP_THREAD_NOTIFICATION_MSG_TYPE.GROUP_THREAD_REMOVE_USERS -> {
                return context.getString(R.string.group_thread_notification_remove_user, executorInfo?.getName(context) ?: executor, targetMemberInfoStr)
            }
            CwmSignalMsg.SIGNAL_GROUP_THREAD_NOTIFICATION_MSG_TYPE.GROUP_THREAD_PROMOTE_ADMIN -> {
                return context.getString(R.string.group_thread_notification_promote_admin, executorInfo?.getName(context) ?: executor, targetMemberInfoStr)
            }
            CwmSignalMsg.SIGNAL_GROUP_THREAD_NOTIFICATION_MSG_TYPE.GROUP_THREAD_REVOKE_ADMIN -> {
                return context.getString(R.string.group_thread_notification_revoke_admin, executorInfo?.getName(context) ?: executor, targetMemberInfoStr)
            }
            CwmSignalMsg.SIGNAL_GROUP_THREAD_NOTIFICATION_MSG_TYPE.GROUP_THREAD_CHANGE_NAME -> {
                return context.getString(R.string.group_thread_notification_change_groupname, executorInfo?.getName(context) ?: executor, groupName)
            }
            else -> {
                return context.getString(R.string.im_type_content_group_thread_notification)
            }
        }
    }

    fun getLastSignalMultimediaMessageContent(context: Context): String{
        val content = when (this.contentSignalMultimediaMessage?.multimediaFileInfosList?.lastOrNull()?.mediaType) {
            CwmSignalMsg.SIGNAL_MEDIA_TYPE.IMAGE -> context.getString(R.string.im_type_content_img)
            CwmSignalMsg.SIGNAL_MEDIA_TYPE.VIDEO -> context.getString(R.string.im_type_content_video)
            CwmSignalMsg.SIGNAL_MEDIA_TYPE.AUDIO -> context.getString(R.string.im_type_content_audio)
            CwmSignalMsg.SIGNAL_MEDIA_TYPE.DOC -> context.getString(R.string.im_type_content_doc)
            CwmSignalMsg.SIGNAL_MEDIA_TYPE.FILE -> context.getString(R.string.im_type_content_file)
            else -> context.getString(R.string.im_type_content_multi_media)
        }

        return content
    }

    fun isContentTheSameWith(other: SignalMsgExt): Boolean{
        if (!this.checksum.equals(other.checksum)) return false

        if (!this.msgId.equals(other.msgId)) return false

        if (this.status != other.status) return false

        if (this.imType != other.imType) return false

        if (this.direction != other.direction) return false

        this.groupExecutorInfo?.let {
            if (!it.equals(other.groupExecutorInfo)) return false
        }

        other.groupExecutorInfo?.let {
            if (!it.equals(this.groupExecutorInfo)) return false
        }

        this.groupCreatorInfo?.let {
            if (!it.equals(other.groupCreatorInfo)) return false
        }

        other.groupCreatorInfo?.let {
            if (!it.equals(this.groupCreatorInfo)) return false
        }

        this.groupTargetMemberInfoStr?.let {
            if (!it.equals(other.groupTargetMemberInfoStr)) return false
        }

        other.groupTargetMemberInfoStr?.let {
            if (!it.equals(this.groupTargetMemberInfoStr)) return false
        }

        return true
    }



}