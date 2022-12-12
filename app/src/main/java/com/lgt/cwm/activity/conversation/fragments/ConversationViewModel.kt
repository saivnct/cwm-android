package com.lgt.cwm.activity.conversation.fragments

import android.os.CountDownTimer
import androidx.lifecycle.*
import com.lgt.cwm.activity.conversation.fragments.models.LinkPreviewState
import com.lgt.cwm.business.account.AccountRepository
import com.lgt.cwm.business.contact.ContactRepository
import com.lgt.cwm.business.media.linkpreview.LinkPreview
import com.lgt.cwm.business.media.linkpreview.LinkPreviewRepository
import com.lgt.cwm.business.media.linkpreview.LinkPreviewUtil
import com.lgt.cwm.business.message.MessageRepository
import com.lgt.cwm.business.ws.WSRepository
import com.lgt.cwm.db.entity.Contact
import com.lgt.cwm.db.entity.SignalMsg
import com.lgt.cwm.db.entity.SignalMsgStatus
import com.lgt.cwm.db.entity.SignalThread
import com.lgt.cwm.http.connection.RequestController
import com.lgt.cwm.models.*
import com.lgt.cwm.util.Debouncer
import com.lgt.cwm.util.DebugConfig
import com.lgt.cwm.util.SIPHelper
import com.lgt.cwm.util.SignalMsgHelper
import com.lgt.cwm.util.livedata.SingleLiveEvent
import com.lyft.kronos.KronosClock
import cwmSignalMsgPb.CwmSignalMsg
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ConversationViewModel @Inject constructor(
    private val debugConfig: DebugConfig,
    private val accountRepository: AccountRepository,
    private val messageRepository: MessageRepository,
    private val wsRepository: WSRepository,
    private val contactRepository: ContactRepository,
    private val kronosClock: KronosClock,
) : ViewModel() {
    private val TAG = ConversationViewModel::class.simpleName.toString()

    var signalThreadExt : SignalThreadExt? = null
    var signalMsgExtList : List<SignalMsgExt>? = null
    var lastViewPos : Int? = null

    var unreadMsgCount: Int = 0
    var startUnreadPosition: Int? = null
    var unsendSeenStateMsgIds: List<String> = arrayListOf()

    var selectedMessagesMutableLiveData: MutableLiveData<MutableSet<String>> = MutableLiveData(mutableSetOf())

    var fetchingOldMsg : Boolean = false

    var isTyping : Boolean = false
    var userTyping : String? = null

    var countAllUnhanldedEventMsg: LiveData<Long> = messageRepository.countAllUnhanldedEventMsgFlow.asLiveData()
    var countAllSendingMsg: LiveData<Long> = messageRepository.countAllSendingMsgFlow.asLiveData()

    var signalTypingMsgLiveData: LiveData<SignalTypingMsgExt> = messageRepository.typingStateFlow.asLiveData()
    val typingTimeout = MutableLiveData<Boolean>(false)
    private var typingCountDownTimer : CountDownTimer? = null

    //link preview
    private val debouncer: Debouncer = Debouncer(250)
    private var userCanceledLinkPreview = false
    private var activeUrl: String? = null
    private var activeRequestLinkPreview: RequestController? = null
    val linkPreviewState: MutableLiveData<LinkPreviewState> = MutableLiveData()

    //region mention
    val mentionListMutableLiveData: MutableLiveData<List<ThreadParticipantInfo>> = MutableLiveData(mutableListOf())

    val liveQuery: MutableLiveData<String?> = MutableLiveData(null)

    val selectedMention: SingleLiveEvent<ThreadParticipantInfo> = SingleLiveEvent()
    //endregion



//    init {
//        debugConfig.log(TAG,"ConversationViewModel - init ${this.hashCode()}")
//    }


    fun startWorkerMessageEventHanlde() = messageRepository.startWorkerMessageEventHanlde()
    fun startWorkerMessageTrySend() = messageRepository.startWorkerMessageTrySend()

    fun getThreadByThreadIdLiveData(threadId: String) : LiveData<SignalThread?> =  messageRepository.getThreadByThreadIdFlow(threadId).asLiveData()


    fun getContactByIdLiveData(id: String) : LiveData<Contact?> = contactRepository.getByIdFlow(id).asLiveData()

    fun allMsgExtHaveContentByThreadIdFlow(threadId: String) : LiveData<List<SignalMsgExt>> = Transformations.map(messageRepository.allMsgHaveContentByThreadIdFlow(threadId).asLiveData()) {
        it.map { signalMsg ->
            SignalMsgExt(signalMsg)
        }
    }

    fun countAllMsgIdByThreadIdAndStatus(threadId: String, status: Int) : LiveData<Long> = messageRepository.countAllMsgIdByThreadIdAndStatusFlow(threadId, status).asLiveData()
    fun allMsgIdByThreadIdAndNotSendSeenState(threadId: String) : LiveData<List<String>> = messageRepository.allMsgIdByThreadIdAndNotSendSeenStateFlow(threadId).asLiveData()

    suspend fun deleteMsgsOfThread(threadId: String, msgIds: List<String>, deleteForAllMembers: Boolean) = messageRepository.deleteMsgsOfThread(threadId, msgIds, deleteForAllMembers)



    suspend fun resendMsg(msgId: String): Boolean{
        accountRepository.getActiveAccount()?.let { account ->
            val signalMsg = messageRepository.findSignalMsgByMsgId(msgId)
            if (signalMsg != null){
                signalThreadExt?.let { thread ->
                    messageRepository.updateMsgStatusAndMsgDate(
                        msgId = msgId,
                        status = SignalMsgStatus.SENDING.code,
                        msgDate = kronosClock.getCurrentTimeMs()
                    )

                    messageRepository.saveUpdateSignalThreadLastMsgInfo(
                        threadId = thread.threadId,
                        lastMsgId = signalMsg.msgId,
                        lastMsg = signalMsg.content,
                        lastMsgImType = signalMsg.imType,
                        lastMsgStatus = signalMsg.status,
                        lastMsgDate = signalMsg.msgDate,
                        lastModified = kronosClock.getCurrentTimeMs()
                    )

                    messageRepository.startWorkerMessageTrySend()
                    return true
                }
            }
        }

        return false
    }

    suspend fun sendTypingMsg(isTyping: Boolean) {
        this.isTyping = isTyping
        accountRepository.getActiveAccount()?.let { account ->
            signalThreadExt?.let { thread ->
//                debugConfig.log(TAG, "sendTypingMsg")
                val signalMsg = SignalMsgHelper.createTypingMessage(
                    from = account,
                    to = thread.phoneFull,
                    threadId = thread.threadId,
                    threadType = thread.threadType,
                    isTyping = isTyping,
                    msgDate = kronosClock.getCurrentTimeMs()
                )

                val cwmRequest = SIPHelper.getCwmSignalMsg(signalMsg, account.sessionId)

                if (wsRepository.isConnected()){
                    val result = wsRepository.sendMsg(cwmRequest)
                    if (result is com.lgt.cwm.util.Result.Error){
                        debugConfig.log(TAG,result.exception.toString())
                    }
                }

            }
        }
    }

    suspend fun sendForwardMsg(threadIds: List<String>, msgIds: List<String>): List<String>{   //return list success thread
        val listThreadSendSuccess = arrayListOf<String>()

        accountRepository.getActiveAccount()?.let { account ->
            signalThreadExt?.let { thread ->
                if (threadIds.isNullOrEmpty() || msgIds.isNullOrEmpty()){
                    return listThreadSendSuccess
                }

                for (msgId in msgIds){
                    val signalMsgToForward = messageRepository.findSignalMsgByMsgId(msgId)
                    if (signalMsgToForward == null){
                        debugConfig.log(TAG, "sendForwardMsg - Invalid msgId - not found signalMsgToForward ${msgId}")
                        continue
                    }

                    for (threadId in threadIds){
                        val signalThreadToForward = messageRepository.findSignalThreadByThreadId(threadId)
                        if (signalThreadToForward == null){
                            debugConfig.log(TAG, "sendForwardMsg - Invalid threadId - not found signalThreadToForward ${thread}")
                            continue
                        }

                        var signalMsg : SignalMsg? = null

                        val signalMsgToForwardExt = SignalMsgExt(signalMsgToForward)
                        if (signalMsgToForward.imType == CwmSignalMsg.SIGNAL_IM_TYPE.FORWARD.number){

                            signalMsgToForwardExt.contentSignalForwardMsg?.let { contentSignalForwardMsg ->
                                val originalSignalMsgToForwardExt = SignalMsgExt(
                                    contentSignalForwardMsg = contentSignalForwardMsg,
                                    msgDate = signalMsgToForwardExt.msgDate,
                                    serverDate = signalMsgToForwardExt.serverDate,)

                                signalMsg = SignalMsgHelper.createForwardMessage(
                                    from = account,
                                    to = signalThreadToForward.phoneFull,
                                    threadId = signalThreadToForward.threadId,
                                    threadType = signalThreadToForward.threadType,
                                    signalMsgToForward = originalSignalMsgToForwardExt,
                                    msgDate = kronosClock.getCurrentTimeMs()
                                )
                            }
                        }else{
                            signalMsg = SignalMsgHelper.createForwardMessage(
                                from = account,
                                to = signalThreadToForward.phoneFull,
                                threadId = signalThreadToForward.threadId,
                                threadType = signalThreadToForward.threadType,
                                signalMsgToForward = signalMsgToForwardExt,
                                msgDate = kronosClock.getCurrentTimeMs()
                            )
                        }

                        if (signalMsg == null){
                            debugConfig.log(TAG, "sendMsg - cannot create signalMsg")
                            continue
                        }



                        messageRepository.saveSignalMsg(signalMsg!!)

                        messageRepository.saveUpdateSignalThreadLastMsgInfo(
                            threadId = signalThreadToForward.threadId,
                            lastMsgId = signalMsg!!.msgId,
                            lastMsg = signalMsg!!.content,
                            lastMsgImType = signalMsg!!.imType,
                            lastMsgStatus = signalMsg!!.status,
                            lastMsgDate = signalMsg!!.msgDate,
                            lastModified = kronosClock.getCurrentTimeMs()
                        )

                        messageRepository.startWorkerMessageTrySend()

                        listThreadSendSuccess.add(threadId)
                    }
                }
            }
        }

        return listThreadSendSuccess
    }

    suspend fun sendMediaMsg(fileMetaDatas: List<FileMetaData>, replyMsgId: String? = null): Boolean{
        accountRepository.getActiveAccount()?.let { account ->
            signalThreadExt?.let { thread ->
                var rMsgId = replyMsgId
                if (!rMsgId.isNullOrEmpty()){
                    val replySignalMsg = messageRepository.findSignalMsgByMsgId(rMsgId)
                    if (replySignalMsg == null){
                        rMsgId = null
                    }
                }

                val signalMsg = SignalMsgHelper.createMultiMediaMessage(
                    from = account,
                    to = thread.phoneFull,
                    threadId = thread.threadId,
                    threadType = thread.threadType,
                    fileMetaDatas = fileMetaDatas,
                    msgDate = kronosClock.getCurrentTimeMs(),
                    replyMsgId = rMsgId
                )
                messageRepository.saveSignalMsg(signalMsg)

                messageRepository.saveUpdateSignalThreadLastMsgInfo(
                    threadId = thread.threadId,
                    lastMsgId = signalMsg.msgId,
                    lastMsg = signalMsg.content,
                    lastMsgImType = signalMsg.imType,
                    lastMsgStatus = signalMsg.status,
                    lastMsgDate = signalMsg.msgDate,
                    lastModified = kronosClock.getCurrentTimeMs()
                )

                messageRepository.startWorkerMessageTrySend()

                return true
            }
        }

        return false
    }

    suspend fun sendMsg(content: String, replyMsgId: String? = null): Boolean{
        accountRepository.getActiveAccount()?.let { account ->
            signalThreadExt?.let { thread ->
                var rMsgId = replyMsgId
                if (!rMsgId.isNullOrEmpty()){
                    val replySignalMsg = messageRepository.findSignalMsgByMsgId(rMsgId)
                    if (replySignalMsg == null){
                        rMsgId = null
                    }
                }


                val signalMsg = SignalMsgHelper.createIMMessage(
                    from = account,
                    to = thread.phoneFull,
                    threadId = thread.threadId,
                    threadType = thread.threadType,
                    content = content,
                    msgDate = kronosClock.getCurrentTimeMs(),
                    replyMsgId = rMsgId
                )
                messageRepository.saveSignalMsg(signalMsg)

                messageRepository.saveUpdateSignalThreadLastMsgInfo(
                    threadId = thread.threadId,
                    lastMsgId = signalMsg.msgId,
                    lastMsg = signalMsg.content,
                    lastMsgImType = signalMsg.imType,
                    lastMsgStatus = signalMsg.status,
                    lastMsgDate = signalMsg.msgDate,
                    lastModified = kronosClock.getCurrentTimeMs()
                )

                messageRepository.startWorkerMessageTrySend()

                return true
            }
        }

        return false
    }

    suspend fun sendURLMsg(content: String,
                           url: String, urlTitle: String,
                           urlDescription: String, urlThumbnail: String?,
                           replyMsgId: String? = null): Boolean{
        resetLinkPreview()

        accountRepository.getActiveAccount()?.let { account ->
            signalThreadExt?.let { thread ->
                var rMsgId = replyMsgId
                if (!rMsgId.isNullOrEmpty()){
                    val replySignalMsg = messageRepository.findSignalMsgByMsgId(rMsgId)
                    if (replySignalMsg == null){
                        rMsgId = null
                    }
                }


                val signalMsg = SignalMsgHelper.createURLMessage(
                    from = account,
                    to = thread.phoneFull,
                    threadId = thread.threadId,
                    threadType = thread.threadType,
                    content = content,
                    url = url,
                    urlTitle = urlTitle,
                    urlDescription = urlDescription,
                    urlThumbnail = urlThumbnail,
                    msgDate = kronosClock.getCurrentTimeMs(),
                    replyMsgId = rMsgId
                )
                messageRepository.saveSignalMsg(signalMsg)

                messageRepository.saveUpdateSignalThreadLastMsgInfo(
                    threadId = thread.threadId,
                    lastMsgId = signalMsg.msgId,
                    lastMsg = signalMsg.content,
                    lastMsgImType = signalMsg.imType,
                    lastMsgStatus = signalMsg.status,
                    lastMsgDate = signalMsg.msgDate,
                    lastModified = kronosClock.getCurrentTimeMs()
                )

                messageRepository.startWorkerMessageTrySend()

                return true
            }
        }

        return false
    }

    suspend fun sendSeenStateMsg() {
        accountRepository.getActiveAccount()?.let { account ->
            signalThreadExt?.let { thread ->
                if (unsendSeenStateMsgIds.isNotEmpty()){
//                    debugConfig.log(TAG, "sendSeenStateMsg")
                    val signalMsg = SignalMsgHelper.createSeenStateMessage(
                        from = account,
                        to = thread.phoneFull,
                        threadId = thread.threadId,
                        threadType = thread.threadType,
                        msgIdList = unsendSeenStateMsgIds,
                        msgDate = kronosClock.getCurrentTimeMs()
                    )

                    messageRepository.saveSignalMsg(signalMsg)
                    messageRepository.updateListMsgSendSeenState(msgIds = unsendSeenStateMsgIds, sendSeenState = 1)

                    messageRepository.startWorkerMessageTrySend()
                }
            }
        }
    }



    suspend fun updateMsgRecieveSeen(serverDate: Long){
        signalThreadExt?.let { thread ->
            messageRepository.updateMsgRecieveSeen(thread.threadId, serverDate)
            messageRepository.updateThreadUnreadMsgs(thread.threadId)
        }
    }




    suspend fun fetchOldMsgOfThread() {
        if (fetchingOldMsg) {
            return
        }

        fetchingOldMsg = true

        try {
            signalThreadExt?.let { thread ->
                messageRepository.fetchOldMsgOfThread(thread.threadId)
            }
        }catch (e : Throwable){
            e.printStackTrace()
        }finally {
            fetchingOldMsg = false
        }
    }

    suspend fun updateThreadLastViewPos(pos: Int){
        signalThreadExt?.let { thread ->
            var actualPos = pos - thread.unreadMsgs.toInt()
            if (actualPos < 0){
                actualPos = 0
            }


            messageRepository.updateThreadLastViewPos(thread.threadId, actualPos)
        }
    }

    suspend fun resetThreadLastViewPosByThreadId(threadId: String?){
        threadId?.let {
            messageRepository.resetThreadLastViewPosByThreadId(it)
        }
    }

    fun startTypingCounter(){
        typingCountDownTimer?.cancel()
        typingTimeout.postValue(false)

        typingCountDownTimer =  object : CountDownTimer(10 * 1000, 1000) {  //30sec
            // Callback function, fired on regular interval
            override fun onTick(millisUntilFinished: Long) {
            }

            // Callback function, fired
            // when the time is up
            override fun onFinish() {
                typingTimeout.postValue(true)
            }
        }.start()

    }



    fun endSelection() {
        selectedMessagesMutableLiveData.postValue(mutableSetOf<String>())
    }

    fun toggleMessageSelected(msgId: String) {
        var selectedMsgs = selectedMessagesMutableLiveData.value
        if (selectedMsgs == null){
            selectedMsgs = mutableSetOf<String>()
        }


        if (selectedMsgs.contains(msgId)) {
            selectedMsgs.remove(msgId)
        } else {
            selectedMsgs.add(msgId)
        }
        selectedMessagesMutableLiveData.postValue(selectedMsgs)
    }


    suspend fun findQuoteMsgByMsgId(replyMsgId: String): SignalMsgExt? {
        val replySignalMsg = messageRepository.findSignalMsgByMsgId(replyMsgId)
        if (replySignalMsg == null){
            return null
        } else {
            return SignalMsgExt(replySignalMsg)
        }
    }

    //region link preview

    override fun onCleared() {
        super.onCleared()
        activeRequestLinkPreview?.cancel()
        debouncer.clear()

//        debugConfig.log(TAG,"ConversationViewModel - onCleared ${this.hashCode()}")
    }

    fun hasLinkPreview(): Boolean {
        linkPreviewState.value?.let {
            return it.linkPreview != null
        }
        return false
    }

    fun onTextChanged(text: String) {
        debouncer.publish {
            if (text.isEmpty()) {
                userCanceledLinkPreview = false
            }
            if (userCanceledLinkPreview) {
                return@publish
            }
            val link = LinkPreviewUtil.findValidPreviewUrls(text).findFirst()

            link?.let {
                if (it.url == activeUrl) return@publish
            }

            if (activeRequestLinkPreview != null) {
                activeRequestLinkPreview?.cancel()
                activeRequestLinkPreview = null
            }

            if (link == null) {
                activeUrl = null
                linkPreviewState.postValue(LinkPreviewState.forNoLinks())
                return@publish
            }
            linkPreviewState.postValue(LinkPreviewState.forLoading())
            activeUrl = link.url
            activeRequestLinkPreview = LinkPreviewRepository().getLinkPreview(link.url,
                object : LinkPreviewRepository.Callback {
                    override fun onSuccess(linkPreview: LinkPreview) {
                        debugConfig.log(TAG,"getLinkPreview: $linkPreview")
                        if (!userCanceledLinkPreview) {
                            if (activeUrl != null && activeUrl == linkPreview.url) {
                                linkPreviewState.postValue(LinkPreviewState.forPreview(linkPreview))
                            } else {
                                linkPreviewState.postValue(LinkPreviewState.forNoLinks())
                            }
                        }
                        activeRequestLinkPreview = null
                    }

                    override fun onError(error: LinkPreviewRepository.Error) {
                        debugConfig.log(TAG,"getLinkPreview error ${error}")
                        if (!userCanceledLinkPreview) {
                            if (activeUrl != null) {
                                linkPreviewState.postValue(LinkPreviewState.forLinksWithNoPreview(activeUrl, error))
                            } else {
                                linkPreviewState.postValue(LinkPreviewState.forNoLinks())
                            }
                        }
                        activeRequestLinkPreview = null
                    }

                })
        }

    }

    fun onUserCancelLinkPreview() {
        userCanceledLinkPreview = true
        if (activeRequestLinkPreview != null) {
            activeRequestLinkPreview?.cancel()
            activeRequestLinkPreview = null
        }
        activeUrl = null

        debouncer.clear()
        linkPreviewState.postValue(LinkPreviewState.forNoLinks())
    }

    fun resetLinkPreview() {
        if (activeRequestLinkPreview != null) {
            activeRequestLinkPreview?.cancel()
            activeRequestLinkPreview = null
        }

        userCanceledLinkPreview = false
        activeUrl = null
        debouncer.clear()
        linkPreviewState.postValue(LinkPreviewState.forNoLinks())
    }
    //endregion link preview


}