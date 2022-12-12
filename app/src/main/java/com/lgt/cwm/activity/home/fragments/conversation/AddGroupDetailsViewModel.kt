package com.lgt.cwm.activity.home.fragments.conversation

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import com.lgt.cwm.business.message.MessageRepository
import com.lgt.cwm.util.Constants.GROUPNAME_MIN_LENGTH
import com.lgt.cwm.util.DebugConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AddGroupDetailsViewModel @Inject constructor(
    private val debugConfig: DebugConfig,
    private val messageRepository: MessageRepository
) : ViewModel() {
    private val TAG = AddGroupDetailsViewModel::class.simpleName.toString()

    val _edGroupName = MutableLiveData<String?>(null)
    var listPhoneFull : List<String>? = null

    val enableSubmitLiveData: LiveData<Boolean> = Transformations.map(_edGroupName) {
        return@map (!it.isNullOrEmpty() && it.length >= GROUPNAME_MIN_LENGTH)
    }

    suspend fun createNewGroupThread(groupName: String) = messageRepository.createNewGroupThread(groupName, listPhoneFull)


}

