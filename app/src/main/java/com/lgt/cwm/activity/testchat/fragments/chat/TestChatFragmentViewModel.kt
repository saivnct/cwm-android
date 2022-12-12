package com.lgt.cwm.activity.testchat.fragments.chat

import androidx.lifecycle.*
import com.lgt.cwm.business.account.AccountRepository
import com.lgt.cwm.business.ws.WSRepository
import com.lgt.cwm.db.entity.Account
import com.lgt.cwm.util.DebugConfig
import com.lgt.cwm.ws.WSState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Created by giangtpu on 7/6/22.
 */
@HiltViewModel
class TestChatFragmentViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
    private val wsRepository: WSRepository,
    private val debugConfig: DebugConfig
): ViewModel(){
    private val TAG = TestChatFragmentViewModel::class.simpleName.toString()

    val activeAccLiveData: LiveData<Account?> = accountRepository.activeAccFlow.asLiveData();
    val wsStateLiveData: LiveData<WSState?> = wsRepository.wsStateFlow.asLiveData();

    var enableChatLiveData: MediatorLiveData<Boolean> = MediatorLiveData()


//    val wsChatMsgLiveData: LiveData<WSChatMsg> = wsRepository.wsChatMsgFlow.asLiveData();

    val _edMsg = MutableLiveData<String?>(null)
    val enableBtnSend: LiveData<Boolean> = Transformations.map(_edMsg) {
        return@map !it.isNullOrEmpty()
    }

    init {
        enableChatLiveData.addSource(activeAccLiveData) {
            enableChatLiveData.value =
                (it?.isActive ?: false) && ((wsStateLiveData.value ?: WSState.DISCONNECTED) == WSState.CONNECTED)
        }

        enableChatLiveData.addSource(wsStateLiveData) {
            enableChatLiveData.value =
                (activeAccLiveData.value?.isActive ?: false) && ((it ?: WSState.DISCONNECTED) == WSState.CONNECTED)
        }
    }

    fun sendMsg() {
        viewModelScope.launch{
            val msg = _edMsg.value
            if (!msg.isNullOrEmpty()){
//                val account = activeAccLiveData.value
//                account?.let {
//                    val result = wsRepository.sendMsg(it, msg)
//                    when (result) {
//                        is Result.Success<Any> -> debugConfig.log(TAG, "Send chat msg Success!!!")
//                        is Result.Error -> debugConfig.log(TAG, "Failed: ${result.exception.toString()}")
//                    }
//                }


            }
            _edMsg.postValue("")
        }
    }

}