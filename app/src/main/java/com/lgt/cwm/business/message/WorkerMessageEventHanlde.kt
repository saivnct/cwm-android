package com.lgt.cwm.business.message

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.lgt.cwm.business.account.AccountRepository
import com.lgt.cwm.business.contact.ContactRepository
import com.lgt.cwm.business.notification.NotificationHandler
import com.lgt.cwm.util.DebugConfig
import cwmSignalMsgPb.CwmSignalMsg
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Created by giangtpu on 11/09/2022.
 */
@HiltWorker
class WorkerMessageEventHanlde @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val notificationHandler: NotificationHandler,
    private val accountRepository: AccountRepository,
    private val messageRepository: MessageRepository,
    private val contactRepository: ContactRepository,
    private val debugConfig: DebugConfig,
    ) : CoroutineWorker(context, params){

    private val TAG = WorkerMessageEventHanlde::class.simpleName.toString()
//    companion object {
//        const val Progress = "Progress"
//    }

    override suspend fun doWork(): Result {
//        debugConfig.log(TAG, "doWork !!!!!!!!!")
        val account = accountRepository.getActiveAccount() ?: return Result.success()

        var continuteLoop = true
        while (continuteLoop) {
            val signalMsg = messageRepository.getFirstUnhanldedEventMsg()
            if (signalMsg != null){
//                debugConfig.log(TAG, "checkToHandlePendingEventMsg - try handle even msg ${signalMsg.msgId}")
                val fromMe =  signalMsg.from.equals(account.phoneFull)
                val signalEventMessageProto = try{
                    CwmSignalMsg.SignalEventMessage.parseFrom(signalMsg.content)
                }catch (e: Throwable) {
                    null
                }

                try {
                    when (signalEventMessageProto?.eventType){
                        CwmSignalMsg.SIGNAL_EVENT_MSG_TYPE.UPDATE_CONTACT_OTT -> {
                            handleSignalEventMessageUpdateContactOTT(signalEventMessageProto)
                        }
                        CwmSignalMsg.SIGNAL_EVENT_MSG_TYPE.MSG_DELETE -> {
                            handleSignalEventMessageMsgDelete(signalEventMessageProto, fromMe)
                        }
                        CwmSignalMsg.SIGNAL_EVENT_MSG_TYPE.THREAD_CLEAR_MSG -> {
                            handleSignalEventMessageThreadClearMsg(signalMsg.serverDate, signalEventMessageProto, fromMe)
                        }
                        CwmSignalMsg.SIGNAL_EVENT_MSG_TYPE.THREAD_DELETED -> {
                            handleSignalEventMessageThreadDeleted(signalMsg.serverDate, signalEventMessageProto, fromMe)
                        }
                        else -> {}
                    }

                    messageRepository.setHandledEventMsg(signalMsg.msgId)
                }catch (e: Throwable){
                    e.printStackTrace()
                }

            }
            else{
//                debugConfig.log(TAG, "checkToHandlePendingEventMsg - Not found pending msg")
                continuteLoop = false
            }

        }



//        debugConfig.log(TAG, "doWork DONE !!!!!!!!!")

        return Result.success()
    }

    //Expedited worker need this - To maintain backwards compatibility for expedited jobs, WorkManager might run a foreground service on platform versions older than Android 12. Foreground services can display a notification to the user
    override suspend fun getForegroundInfo(): ForegroundInfo {
        return ForegroundInfo(
            this.hashCode(), notificationHandler.getDefaultWorkerNotification()
        )
    }


    suspend fun handleSignalEventMessageUpdateContactOTT(signalEventMessageProto : CwmSignalMsg.SignalEventMessage) {
        debugConfig.log(TAG, "handleSignalEventMessageUpdateContactOTT")
        val signalEventMessageNewContactOTTProto = try{
            CwmSignalMsg.SignalEventMessageUpdateContactOTT.parseFrom(signalEventMessageProto.data)
        }catch (e: Throwable){null} ?: return

        val listContact = contactRepository.getAllByPhoneFull(signalEventMessageNewContactOTTProto.phoneFull)
        if (listContact.isNotEmpty()){
            for (contact in listContact){
                contact.userId = signalEventMessageNewContactOTTProto.userId
                contact.isOTT = !contact.userId.isNullOrEmpty()
                contact.username = signalEventMessageNewContactOTTProto.username
                contact.avatar = signalEventMessageNewContactOTTProto.userAvatar
                contact.svFirtname = signalEventMessageNewContactOTTProto.firstName
                contact.svLastname = signalEventMessageNewContactOTTProto.lastName
            }
            contactRepository.updateListContact(listContact)
        }
    }


    suspend fun handleSignalEventMessageMsgDelete(signalEventMessageProto : CwmSignalMsg.SignalEventMessage, fromMe: Boolean){
        val signalEventMessageMsgDeleteProto = try {
            CwmSignalMsg.SignalEventMessageMsgDelete.parseFrom(signalEventMessageProto.data)
        }catch (e: Throwable) {null} ?: return

        val threadId = signalEventMessageMsgDeleteProto.threadId
        val msgIds = signalEventMessageMsgDeleteProto.msgIdsList
        val deleteForAllMembers = signalEventMessageMsgDeleteProto.deleteForAllMembers

        debugConfig.log(TAG, "handleSignalEventMessageMsgDelete ${threadId} - ${msgIds}")

        if (msgIds.isNotEmpty() && (fromMe || deleteForAllMembers)){
            messageRepository.deleteSignalMsgByListMsgId(msgIds)
            val signalThread = messageRepository.findSignalThreadByThreadId(threadId)

            signalThread?.lastMsgId?.let {
                if (msgIds.contains(it)){
                    debugConfig.log(TAG, "handleSignalEventMessageMsgDelete - update last msg of thread")
                    messageRepository.updateThreadLastMsg(threadId)
                }
            }
        }
    }

    suspend fun handleSignalEventMessageThreadClearMsg(toServerDate: Long, signalEventMessageProto : CwmSignalMsg.SignalEventMessage, fromMe: Boolean){
//        debugConfig.log(TAG, "handleSignalEventMessageThreadClearMsg")
        val signalEventMessageThreadClearMsgProto = try{
            CwmSignalMsg.SignalEventMessageThreadClearMsg.parseFrom(signalEventMessageProto.data)
        }catch (e: Throwable) {null} ?: return

        val threadId = signalEventMessageThreadClearMsgProto.threadId
        val deleteForAllMembers = signalEventMessageThreadClearMsgProto.deleteForAllMembers

        debugConfig.log(TAG, "handleSignalEventMessageThreadClearMsg - threadId ${threadId} - deleteForAllMembers ${deleteForAllMembers} - toServerDate ${toServerDate}")

        if (!threadId.isEmpty() && (fromMe || deleteForAllMembers)){
            messageRepository.clearSignalMsgByThreadId(threadId, toServerDate)
            messageRepository.updateThreadLastMsg(threadId)
        }
    }

    suspend fun handleSignalEventMessageThreadDeleted(serverDate: Long, signalEventMessageProto : CwmSignalMsg.SignalEventMessage, fromMe: Boolean){
        debugConfig.log(TAG, "handleSignalEventMessageThreadDeleted")
        val signalEventMessageThreadDeletedProto = try{
            CwmSignalMsg.SignalEventMessageThreadDeleted.parseFrom(signalEventMessageProto.data)
        }catch (e: Throwable){null} ?: return

        val threadId = signalEventMessageThreadDeletedProto.threadId
        val deleteForAllMembers = signalEventMessageThreadDeletedProto.deleteForAllMembers

        if (!threadId.isEmpty() && (fromMe || deleteForAllMembers)){
            messageRepository.delSignalThread(threadId, serverDate)
        }
    }










}