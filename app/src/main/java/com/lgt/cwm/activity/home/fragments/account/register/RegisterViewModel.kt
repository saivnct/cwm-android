package com.lgt.cwm.activity.home.fragments.account.register

import androidx.lifecycle.*
import com.lgt.cwm.business.account.AccountRepository
import com.lgt.cwm.util.DebugConfig
import com.lgt.cwm.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
    private val debugConfig: DebugConfig,
) : ViewModel() {
    private val TAG = RegisterViewModel::class.simpleName.toString()


    val _edCountryCode = MutableLiveData<String?>(null)
    val edCountryCodeErr: LiveData<Boolean> = Transformations.map(_edCountryCode) {
        return@map (!it.isNullOrEmpty() && it.length < 1)
    }

    val _edPhone = MutableLiveData<String?>(null)
    val edPhoneErr: LiveData<Boolean> = Transformations.map(_edPhone) {
        return@map (!it.isNullOrEmpty() && it.length < 8)
    }

    var enableRegisterLiveData: MediatorLiveData<Boolean> = MediatorLiveData()

    init {
        enableRegisterLiveData.addSource(_edCountryCode) { edCountryCode ->
            val edPhone = _edPhone.value
            enableRegisterLiveData.value =
                (!edCountryCode.isNullOrEmpty() && edCountryCode.length >= 1) && (!edPhone.isNullOrEmpty() && edPhone.length >= 8)
        }

        enableRegisterLiveData.addSource(_edPhone) { edPhone ->
            val edCountryCode = _edCountryCode.value
            enableRegisterLiveData.value =
                (!edCountryCode.isNullOrEmpty() && edCountryCode.length >= 1) && (!edPhone.isNullOrEmpty() && edPhone.length >= 8)
        }
    }



    suspend fun registerAcc() : Result<grpcCWMPb.CwmRqResAccount.CreatAccountResponse>{
//        debugConfig.log("registerAcc")
        val countryCode = _edCountryCode.value
        if (countryCode.isNullOrEmpty()){
            return Result.Error(Exception("Invalid Country Code!"))
        }

        val phone = _edPhone.value
        if (phone.isNullOrEmpty()){
            return Result.Error(Exception("Invalid Phone!"))
        }

//        debugConfig.log("start registerAcc")

        val result = accountRepository.createAcc(countryCode, phone)

        return result
    }

    fun setCountryCode(countryCode: String){
        _edCountryCode.postValue(countryCode)
    }

}