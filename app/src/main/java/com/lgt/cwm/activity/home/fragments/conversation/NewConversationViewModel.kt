package com.lgt.cwm.activity.home.fragments.conversation

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import com.lgt.cwm.activity.home.fragments.contact.ContactViewModel
import com.lgt.cwm.business.contact.ContactRepository
import com.lgt.cwm.db.entity.Contact
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class NewConversationViewModel @Inject constructor(
    private val contactRepository: ContactRepository
    ) : ViewModel() {
    private val TAG = NewConversationViewModel::class.simpleName.toString()

    val allOTTContact: LiveData<List<Contact>> = contactRepository.allContactOTTFlow.asLiveData();
}