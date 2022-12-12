package com.lgt.cwm.activity.home.fragments.splash

import androidx.lifecycle.ViewModel
import com.lgt.cwm.business.account.AccountRepository
import com.lgt.cwm.db.entity.Account
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
) : ViewModel() {
    suspend fun getActiveAccount(): Account? {
        return accountRepository.getActiveAccount()
    }
}