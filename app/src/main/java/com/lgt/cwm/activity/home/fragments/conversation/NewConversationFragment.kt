package com.lgt.cwm.activity.home.fragments.conversation

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.lgt.cwm.R
import com.lgt.cwm.activity.conversation.ConversationActivity
import com.lgt.cwm.activity.home.fragments.conversation.adapter.NewConversationContactSelectionAdapter
import com.lgt.cwm.databinding.FragmentNewConversationBinding
import com.lgt.cwm.db.entity.Contact
import com.lgt.cwm.ui.contact.LetterHeaderDecoration
import com.lgt.cwm.util.DebugConfig
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_new_conversation.*
import javax.inject.Inject

@AndroidEntryPoint
class NewConversationFragment : Fragment() {
    private val TAG = NewConversationFragment::class.simpleName.toString()

    @Inject
    lateinit var debugConfig: DebugConfig

    private val newConversationViewModel: NewConversationViewModel by viewModels()

    @Inject
    lateinit var newConversationContactSelectionAdapter: NewConversationContactSelectionAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val binding: FragmentNewConversationBinding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_new_conversation, container, false);
        binding.lifecycleOwner = viewLifecycleOwner
        binding.newConversationViewModel = newConversationViewModel
        binding.newConversationContactSelectionAdapter = newConversationContactSelectionAdapter

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
//        debugConfig.log(TAG, "onViewCreated")


        //pull to refresh
        swipe_refresh.setOnRefreshListener {
//            debugConfig.log(TAG, "onRefresh called from SwipeRefreshLayout")
            // This method performs the actual data-refresh operation.
            //TODO 1: Refresh list contact
            updateListContact()

            swipe_refresh.isRefreshing = false
        }

        initializeRecyclerView()

        new_group_item.setOnClickListener {
//            debugConfig.log(TAG, "Click create new group")
            findNavController().navigate(R.id.action_newConversationFragment_to_createGroupFragment)
        }

        initObserver()
    }

    var resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        findNavController().navigate(R.id.action_newConversationFragment_to_conversationListFragment)

        //Back button not send result code
//        if (result.resultCode == Activity.RESULT_OK) {
//
//        }
    }

    private fun initializeRecyclerView() {
        recycler_view.addItemDecoration(LetterHeaderDecoration(requireContext()) { false })

        newConversationContactSelectionAdapter.setOnItemClickListener(object : NewConversationContactSelectionAdapter.OnItemClickListener {
            override fun onItemActiveClick(item: Contact, position: Int) {
//                debugConfig.log(TAG, "Go to single chat")

                val intent = Intent(activity, ConversationActivity::class.java).apply {
                    putExtra(ConversationActivity.EXTRA_CONTACT_ID, item.id)
                }
                resultLauncher.launch(intent)
                requireActivity().overridePendingTransition(R.anim.slide_from_end, R.anim.fade_scale_out);
            }
        })

    }

    fun initObserver(){
        newConversationViewModel.allOTTContact.observe(viewLifecycleOwner) { contacts ->
            newConversationContactSelectionAdapter.setItems(contacts)
        }
    }


    private fun updateListContact() {

    }

}