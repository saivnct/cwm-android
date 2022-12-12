package com.lgt.cwm.activity.testchat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Created by giangtpu on 09/07/2022.
 */
@HiltViewModel
class TestChatViewModel @Inject constructor(): ViewModel(){
    private val TAG = TestChatViewModel::class.simpleName.toString()

    private val _toolbarTitleState = MutableStateFlow("")
    val toolbarTitleState: StateFlow<String> = _toolbarTitleState.asStateFlow()

    fun setToolBarTitle(title: String) {
        viewModelScope.launch {
            _toolbarTitleState.emit(title)
        }
    }
}