package com.lgt.cwm.activity.test.fragments.dashboard

import androidx.lifecycle.*
import com.lgt.cwm.business.account.AccountRepository
import com.lgt.cwm.business.contact.ContactRepository
import com.lgt.cwm.business.message.MessageRepository
import com.lgt.cwm.business.ws.WSRepository
import com.lgt.cwm.db.entity.Account
import com.lgt.cwm.http.response.TestPlan
import com.lgt.cwm.business.notification.NotificationHandler
import com.lgt.cwm.util.DebugConfig
import com.lgt.cwm.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Created by giangtpu on 6/29/22.
 */
@HiltViewModel
class TestDashboardViewModel @Inject constructor(
    private val contactRepository: ContactRepository,
    private val accountRepository: AccountRepository,
    private val wsRepository: WSRepository,
    private val messageRepository: MessageRepository,
    private val notificationHandler: NotificationHandler,
    private val debugConfig: DebugConfig,
    ): ViewModel(){

    private val TAG = TestDashboardViewModel::class.simpleName.toString()

    private val _spinnerState = MutableStateFlow(false)
    val spinnerState: StateFlow<Boolean> = _spinnerState.asStateFlow()

    private val _toastState = MutableStateFlow("")
    val toastState: StateFlow<String> = _toastState.asStateFlow()

    val allAcc: LiveData<List<Account>> = accountRepository.allAccFlow.asLiveData();

    val _edCountryCode = MutableLiveData<String?>("84")
    val edCountryCodeErr: LiveData<Boolean> = Transformations.map(_edCountryCode) {
        return@map (!it.isNullOrEmpty() && it.length < 1)
    }

    val _edPhone = MutableLiveData<String?>("966229268")
    val edPhoneErr: LiveData<Boolean> = Transformations.map(_edPhone) {
        return@map (!it.isNullOrEmpty() && it.length < 8)
    }

    val _edAuthenCode = MutableLiveData<String?>(null)

    fun doneToastState(){
        viewModelScope.launch {
            _toastState.emit("")
        }
    }


    fun registerAcc(){
        viewModelScope.launch {
            debugConfig.log("registerAcc")
            val countryCode = _edCountryCode.value
            if (countryCode.isNullOrEmpty()){
                return@launch
            }

            val phone = _edPhone.value
            if (phone.isNullOrEmpty()){
                return@launch
            }

            debugConfig.log("start registerAcc")

            _spinnerState.emit(true)
            val result = accountRepository.createAcc(countryCode, phone)
            when (result) {
                is Result.Success<grpcCWMPb.CwmRqResAccount.CreatAccountResponse> -> debugConfig.log(TAG, "Register Acc Success!!!")
                is Result.Error -> debugConfig.log(TAG, "Failed: ${result.exception.toString()}")
            }
            _toastState.emit("Register Acc Success!!!")
            _spinnerState.emit(false)
        }
    }

    fun verifyAuthenCode(){
        viewModelScope.launch {
            val countryCode = _edCountryCode.value
            if (countryCode.isNullOrEmpty()){
                return@launch
            }

            val phone = _edPhone.value
            if (phone.isNullOrEmpty()){
                return@launch
            }

            val authenCode = _edAuthenCode.value
            if (authenCode.isNullOrEmpty()){
                return@launch
            }

            _spinnerState.emit(true)
            val result = accountRepository.verifyAuthencode(countryCode, phone, authenCode)
            when (result) {
                is Result.Success<grpcCWMPb.CwmRqResAccount.VerifyAuthencodeResponse> -> debugConfig.log(TAG, "Verify AuthenCode Success!!!")
                is Result.Error -> debugConfig.log(TAG, "Failed: ${result.exception.toString()}")
            }

            _edCountryCode.postValue("")
            _edPhone.postValue("")
            _edAuthenCode.postValue("")


            _toastState.emit("New Acc Added!!!")
            _spinnerState.emit(false)
        }
    }

    fun toggleAccountActive(account: Account){
        viewModelScope.launch {
            _spinnerState.emit(true)
            account.isActive = !account.isActive
            accountRepository.updateAccount(account)
            accountRepository.setAllInActiveExcept(account)

            _edCountryCode.postValue("")
            _spinnerState.emit(false)
        }
    }

    fun login(account: Account){
        viewModelScope.launch {
            debugConfig.log(TAG, "call loginAPI")
            _spinnerState.emit(true)
            val result = accountRepository.grpcLogin(account)
            when (result) {
                is Result.Success<grpcCWMPb.CwmRqResAccount.LoginResponse> -> debugConfig.log(TAG, "Success!!! jwt: ${result.data.toString()}")
                is Result.Error -> debugConfig.log(TAG, "Failed: ${result.exception.toString()}")
            }
            _spinnerState.emit(false)
        }
    }



//    fun testNotification(account: Account){
//        val message = "message at ${DateUtil.now().time}"
//        notificationHandler.sendChatNotification(account.username, message)
//    }


    fun testHTTPAPI(){
        viewModelScope.launch {
            debugConfig.log(TAG, "call testAPI")
            _spinnerState.emit(true)

            val result = accountRepository.testHTTPApi()
            when (result) {
                is Result.Success<List<TestPlan>> -> debugConfig.log(TAG, "Success: ${result.data.toString()}")
                is Result.Error -> debugConfig.log(TAG, "Failed: ${result.exception.toString()}")
            }

            _spinnerState.emit(false)
        }
    }

    fun testGrpcAPI(){
        viewModelScope.launch {
            debugConfig.log(TAG, "call testGrpcAPI")
            _spinnerState.emit(true)

//            val result = contactRepository.syncContact()
//            when (result) {
//                is Result.Success<String> -> debugConfig.log(TAG, "Success: ${result.data.toString()}")
//                is Result.Error -> debugConfig.log(TAG, "Failed: ${result.exception.toString()}")
//            }

            _spinnerState.emit(false)
        }
    }
}