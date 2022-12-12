package com.lgt.cwm.activity.home

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import com.lgt.cwm.business.account.AccountRepository
import com.lgt.cwm.business.contact.ContactRepository
import com.lgt.cwm.business.message.MessageRepository
import com.lgt.cwm.business.message.WorkerMessageEventHanlde
import com.lgt.cwm.db.entity.Account
import com.lgt.cwm.db.entity.SignalMsgStatus
import com.lgt.cwm.db.entity.SignalThread
import com.lgt.cwm.util.DebugConfig
import com.lgt.cwm.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
    private val contactRepository: ContactRepository,
    private val messageRepository: MessageRepository,
    private val debugConfig: DebugConfig,
) : ViewModel() {

    private val TAG = MainViewModel::class.simpleName.toString()

    var activeAcc: LiveData<Account?> = accountRepository.activeAccFlow.asLiveData()
    var countAllUnhanldedEventMsg: LiveData<Long> = messageRepository.countAllUnhanldedEventMsgFlow.asLiveData()
    var countAllSendingMsg: LiveData<Long> = messageRepository.countAllSendingMsgFlow.asLiveData()
    var countAllUnreadMsg: LiveData<Long> = messageRepository.countAllUnreadMsgFlow.asLiveData()

    var currentActiveAcc: Account? = null

    suspend fun getActiveAccount(): Account? {
        return accountRepository.getActiveAccount()
    }




    fun startWorkerMessageEventHanlde() = messageRepository.startWorkerMessageEventHanlde()
    fun startWorkerMessageTrySend() = messageRepository.startWorkerMessageTrySend()
    fun startWorkerSendFCMToken(fcmToken: String) = accountRepository.startWorkerSendFCMToken(fcmToken)

    fun syncContact(){
        contactRepository.startWorkerSyncContact()
    }

    fun fetchAllUnreceivedMsg(){
        messageRepository.startWorkerFetchMessage()
    }
}