package com.lgt.cwm.activity.home.fragments.account.register

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.lgt.cwm.business.account.AccountRepository
import com.lgt.cwm.util.DebugConfig
import com.lgt.cwm.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import grpcCWMPb.CwmRqResAccount
import javax.inject.Inject

@HiltViewModel
class EditProfileViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
    private val debugConfig: DebugConfig,
) : ViewModel() {
    private val TAG = EditProfileViewModel::class.simpleName.toString()

    val _edFirstName = MutableLiveData<String?>(null)
    val _edLastName = MutableLiveData<String?>(null)

    suspend fun updateProfile() : Result<CwmRqResAccount.UpdateProfileResponse> {
        return accountRepository.updateProfile(
            firstName = _edFirstName.value?.trim() ?: "",
            lastName = _edLastName.value?.trim() ?: ""
        )
    }
}