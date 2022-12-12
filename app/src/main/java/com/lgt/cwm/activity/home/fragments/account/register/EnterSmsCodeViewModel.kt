package com.lgt.cwm.activity.home.fragments.account.register

import android.os.CountDownTimer
import androidx.lifecycle.*
import com.lgt.cwm.business.account.AccountRepository
import com.lgt.cwm.db.entity.Account
import com.lgt.cwm.util.Constants.AUTHENCODE_LENGTH
import com.lgt.cwm.util.DebugConfig
import com.lgt.cwm.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class EnterSmsCodeViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
    private val debugConfig: DebugConfig,
) : ViewModel() {
    private val TAG = EnterSmsCodeViewModel::class.simpleName.toString()
    private var countryCode: String? = null
    private var phone: String? = null

    val _edPhoneFull = MutableLiveData<String>("your phone number")

    val _edVerificationCode = MutableLiveData<String?>(null)

    val _edCounter = MutableLiveData<String?>("00:00")

    val _authencodeTimeout = MutableLiveData<Boolean>(false)

    val enableSubmitLiveData: LiveData<Boolean> = Transformations.map(_edVerificationCode) {
        return@map (!it.isNullOrEmpty() && it.length == AUTHENCODE_LENGTH)
    }

    val activeAccLiveData: LiveData<Account?> = accountRepository.activeAccFlow.asLiveData();

    fun initData(phoneFull: String, countryCode: String, phone: String, codeTTL: Int ){
        this.countryCode = countryCode
        this.phone = phone

        _edPhoneFull.postValue(phoneFull)
        startCounter(codeTTL)
    }


    fun startCounter(countFrom: Int){
        object : CountDownTimer(countFrom.toLong() * 1000, 1000) {
            // Callback function, fired on regular interval
            override fun onTick(millisUntilFinished: Long) {
                val sec = millisUntilFinished / 1000

                val minutes = sec / 60;
                val seconds = sec % 60;

                val counterStr = String.format("%02d:%02d", minutes, seconds);
                _edCounter.postValue(counterStr)

//                debugConfig.log(TAG, "on tick ${minutes}:${seconds}")
            }

            // Callback function, fired
            // when the time is up
            override fun onFinish() {
                _authencodeTimeout.postValue(true)
            }
        }.start()
    }

    suspend fun verifyAuthenCode() : Result<grpcCWMPb.CwmRqResAccount.VerifyAuthencodeResponse>{
        val authenCode = _edVerificationCode.value
        if (authenCode.isNullOrEmpty() || authenCode.length != AUTHENCODE_LENGTH){
            return Result.Error(Exception("Invalid verification Code!"))
        }

        if (countryCode.isNullOrEmpty()){
            return Result.Error(Exception("Invalid country Code!"))
        }

        if (phone.isNullOrEmpty()){
            return Result.Error(Exception("Invalid phone!"))
        }

        return accountRepository.verifyAuthencode(countryCode!!, phone!!, authenCode)
    }

}