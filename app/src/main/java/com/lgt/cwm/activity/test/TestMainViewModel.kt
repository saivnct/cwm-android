package com.lgt.cwm.activity.test

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lgt.cwm.business.contact.ContactRepository
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
class TestMainViewModel @Inject constructor(
    private val contactRepository: ContactRepository,
    private val debugConfig: DebugConfig,
): ViewModel(){

    private val TAG = TestMainViewModel::class.simpleName.toString()

    private val _toolbarTitleState = MutableStateFlow("")
    val toolbarTitleState: StateFlow<String> = _toolbarTitleState.asStateFlow()



    fun setToolBarTitle(title: String) {
        viewModelScope.launch {
            _toolbarTitleState.emit(title)
        }
    }

    fun syncContact(){
        debugConfig.log(TAG, "onContactChange")

        viewModelScope.launch {
            debugConfig.log(TAG, "call syncContact")

//            val result = contactRepository.syncContact()
//            when (result) {
//                is Result.Success<String> -> debugConfig.log(TAG, "Success: ${result.data.toString()}")
//                is Result.Error -> debugConfig.log(TAG, "Failed: ${result.exception.toString()}")
//            }
        }
    }
}