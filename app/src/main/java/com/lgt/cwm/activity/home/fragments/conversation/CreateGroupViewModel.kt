package com.lgt.cwm.activity.home.fragments.conversation

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import com.lgt.cwm.business.contact.ContactRepository
import com.lgt.cwm.db.entity.Contact
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class CreateGroupViewModel @Inject constructor(
    private val contactRepository: ContactRepository
) : ViewModel() {
    private val TAG = NewConversationViewModel::class.simpleName.toString()

    val allOTTContact: LiveData<List<Contact>> = contactRepository.allContactOTTFlow.asLiveData();

    val selectedItems = arrayListOf<String>()
    val selectedItemsLiveData = MutableLiveData<List<String>>(arrayListOf())

    fun addSelectedPhoneFull(phoneFull: String){
        selectedItems.add(phoneFull)
        selectedItemsLiveData.postValue(selectedItems)
    }

    fun removeSelectedPhoneFull(phoneFull: String){
        selectedItems.remove(phoneFull)
        selectedItemsLiveData.postValue(selectedItems)
    }
}