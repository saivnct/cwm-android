package com.lgt.cwm.activity.home.fragments.contact

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.lgt.cwm.R
import com.lgt.cwm.activity.conversation.ConversationActivity
import com.lgt.cwm.activity.home.fragments.contact.adapter.ContactListAdapter
import com.lgt.cwm.databinding.FragmentContactBinding
import com.lgt.cwm.db.entity.Contact
import com.lgt.cwm.ui.contact.LetterHeaderDecoration
import com.lgt.cwm.util.DebugConfig
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_contact.*
import javax.inject.Inject

@AndroidEntryPoint
class ContactFragment : Fragment() {
    private val TAG = ContactFragment::class.simpleName.toString()

    @Inject
    lateinit var debugConfig: DebugConfig

    @Inject
    lateinit var contactListAdapter: ContactListAdapter

    private val contactViewModel: ContactViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val binding: FragmentContactBinding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_contact, container, false);
        binding.lifecycleOwner = viewLifecycleOwner
        binding.contactViewModel = contactViewModel
        binding.contactListAdapter = contactListAdapter

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
//        debugConfig.log(TAG, "onViewCreated")

        rv_contact.addItemDecoration(LetterHeaderDecoration(requireContext()) { false })

        contactListAdapter.setOnItemClickListener(object : ContactListAdapter.OnItemClickListener {
            override fun onItemActiveClick(item: Contact, position: Int) {
//                debugConfig.log(TAG, "Go to single chat")

                val intent = Intent(activity, ConversationActivity::class.java).apply {
                    putExtra(ConversationActivity.EXTRA_CONTACT_ID, item.id)
                }
                requireActivity().startActivity(intent)
                requireActivity().overridePendingTransition(R.anim.slide_from_end, R.anim.fade_scale_out);
            }
        })

        initObserver()
    }

    fun initObserver(){
        contactViewModel.allOTTContact.observe(viewLifecycleOwner) { contacts ->
            contactListAdapter.setItems(contacts)
        }
    }



}