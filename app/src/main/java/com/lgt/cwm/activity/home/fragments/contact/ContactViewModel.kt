package com.lgt.cwm.activity.home.fragments.contact

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import com.lgt.cwm.business.contact.ContactRepository
import com.lgt.cwm.db.entity.Contact
import com.lgt.cwm.util.DebugConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ContactViewModel @Inject constructor(
    private val contactRepository: ContactRepository,
    private val debugConfig: DebugConfig,
) : ViewModel() {
    private val TAG = ContactViewModel::class.simpleName.toString()

    val allOTTContact: LiveData<List<Contact>> = contactRepository.allContactOTTFlow.asLiveData();
}