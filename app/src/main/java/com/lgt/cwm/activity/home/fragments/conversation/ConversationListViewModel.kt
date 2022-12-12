package com.lgt.cwm.activity.home.fragments.conversation

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import com.lgt.cwm.activity.conversation.fragments.ConversationViewModel
import com.lgt.cwm.business.contact.ContactRepository
import com.lgt.cwm.business.cwmUser.CWMUserRepository
import com.lgt.cwm.business.message.MessageRepository
import com.lgt.cwm.db.entity.Contact
import com.lgt.cwm.db.entity.SignalThread
import com.lgt.cwm.models.SignalThreadExt
import com.lgt.cwm.util.DebugConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ConversationListViewModel @Inject constructor(
    private val debugConfig: DebugConfig,
    private val messageRepository: MessageRepository,
    private val contactRepository: ContactRepository,
    private val cwmUserRepository: CWMUserRepository,
) : ViewModel() {
    private val TAG = ConversationViewModel::class.simpleName.toString()

    val allVerifiedSignalThreadLiveData: LiveData<List<SignalThread>> = messageRepository.allVerifiedSignalThreadFlow.asLiveData();
    val allOTTContactLiveData: LiveData<List<Contact>> = contactRepository.allContactOTTFlow.asLiveData();

    var allVerifiedSignalThread: List<SignalThread>? = null

    var selectedThreadsMutableLiveData: MutableLiveData<MutableSet<SignalThreadExt>> = MutableLiveData(mutableSetOf())

    suspend fun findOneContactByPhoneFull(phoneFull: String) = contactRepository.findOneContactByPhoneFull(phoneFull)
    suspend fun findContactById(id: String) = contactRepository.findContactById(id)

    suspend fun getAllCWMUserByListPhoneFull(phoneFulls: List<String>) = cwmUserRepository.getAllByListPhoneFull(phoneFulls)

    suspend fun clearAllMsgOfThread(threadId: String, deleteForAllMembers: Boolean) = messageRepository.clearAllMsgOfThread(threadId, deleteForAllMembers)
    suspend fun deleteThread(threadId: String, isGroup: Boolean, deleteForAllMembers: Boolean) = messageRepository.deleteThread(threadId, isGroup, deleteForAllMembers)



    fun endSelection() {
        selectedThreadsMutableLiveData.postValue(mutableSetOf())
    }

    fun toggleThreadSelected(conversation: SignalThreadExt) {
        var selectedThreads = selectedThreadsMutableLiveData.value
        if (selectedThreads == null){
            selectedThreads = mutableSetOf()
        }

        if (selectedThreads.contains(conversation)) {
            selectedThreads.remove(conversation)
        } else {
            selectedThreads.add(conversation)
        }
        selectedThreadsMutableLiveData.postValue(selectedThreads)
    }



    fun selectAllThreads(items: Collection<SignalThreadExt>) {
        var selectedThreads = selectedThreadsMutableLiveData.value
        if (selectedThreads == null){
            selectedThreads = mutableSetOf()
        }

        if (selectedThreads.size == items.size){
            selectedThreads.clear()
        }else{
            selectedThreads.addAll(items)
        }
        selectedThreadsMutableLiveData.postValue(selectedThreads)
    }

}