package com.lgt.cwm.activity.conversation

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import com.lgt.cwm.business.account.AccountRepository
import com.lgt.cwm.business.contact.ContactRepository
import com.lgt.cwm.business.cwmUser.CWMUserRepository
import com.lgt.cwm.business.message.MessageRepository
import com.lgt.cwm.db.entity.Account
import com.lgt.cwm.db.entity.Contact
import com.lgt.cwm.db.entity.SignalThread
import com.lgt.cwm.models.SignalThreadExt
import com.lgt.cwm.util.DebugConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * Created by giangtpu on 7/25/22.
 */
@HiltViewModel
class ConversationActivityViewModel @Inject constructor(
    private val debugConfig: DebugConfig,
    private val accountRepository: AccountRepository,
    private val messageRepository: MessageRepository,
    private val contactRepository: ContactRepository,
    private val cwmUserRepository: CWMUserRepository,
) : ViewModel(){
    private val TAG = ConversationActivityViewModel::class.simpleName.toString()
    var activeAcc: LiveData<Account?> = accountRepository.activeAccFlow.asLiveData()

    val threadReadyLiveData = MutableLiveData<Boolean>(false)

    var signalThreadExt: SignalThreadExt? = null
    var contact: Contact? = null
    var currentActiveAcc: Account? = null

    suspend fun findThreadByThreadId(threadId: String) = messageRepository.findSignalThreadByThreadId(threadId)

    suspend fun findOrCreateSoloThread(contact: Contact) = messageRepository.findOrCreateSoloSignalThread(contact)

    suspend fun findContactdById(id: String) = contactRepository.findContactById(id)
    suspend fun findOneContactByPhoneFull(phoneFull: String) = contactRepository.findOneContactByPhoneFull(phoneFull)

    suspend fun getAllCWMUserByListPhoneFull(phoneFulls: List<String>) = cwmUserRepository.getAllByListPhoneFull(phoneFulls)


    suspend fun threadReady(){
        threadReadyLiveData.postValue(true)
    }

    fun fetchAllUnreceivedMsg(){
        messageRepository.startWorkerFetchMessage()
    }

}