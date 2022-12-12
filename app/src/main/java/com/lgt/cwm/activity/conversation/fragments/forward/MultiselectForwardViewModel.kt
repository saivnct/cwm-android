package com.lgt.cwm.activity.conversation.fragments.forward

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import com.lgt.cwm.business.contact.ContactRepository
import com.lgt.cwm.business.cwmUser.CWMUserRepository
import com.lgt.cwm.business.message.MessageRepository
import com.lgt.cwm.db.entity.SignalThread
import com.lgt.cwm.models.SignalThreadExt
import com.lgt.cwm.util.DebugConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * Created by giangtpu on 12/10/2022
 */
@HiltViewModel
class MultiselectForwardViewModel @Inject constructor(
    private val contactRepository: ContactRepository,
    private val messageRepository: MessageRepository,
    private val cwmUserRepository: CWMUserRepository,
    private val debugConfig: DebugConfig,
): ViewModel(){
    private val TAG = MultiselectForwardViewModel::class.simpleName.toString()

    val allVerifiedSignalThreadLiveData: LiveData<List<SignalThread>> = messageRepository.allVerifiedSignalThreadFlow.asLiveData();
    var allVerifiedSignalThread: List<SignalThread>? = null

    val selectedThreads = arrayListOf<SignalThreadExt>()

    suspend fun findOneContactByPhoneFull(phoneFull: String) = contactRepository.findOneContactByPhoneFull(phoneFull)

    suspend fun getAllCWMUserByListPhoneFull(phoneFulls: List<String>) = cwmUserRepository.getAllByListPhoneFull(phoneFulls)
}