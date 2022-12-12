package com.lgt.cwm.activity.conversation.fragments

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.lgt.cwm.R
import com.lgt.cwm.activity.conversation.fragments.adapter.MentionsPickerAdapter
import com.lgt.cwm.databinding.MentionsPickerFragmentBinding
import com.lgt.cwm.models.ThreadParticipantInfo
import com.lgt.cwm.util.DebugConfig
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.mentions_picker_fragment.*
import javax.inject.Inject

@AndroidEntryPoint
class MentionsPickerFragment : Fragment() {
    private val TAG = MentionsPickerFragment::class.simpleName.toString()

    @Inject
    lateinit var debugConfig: DebugConfig

    @Inject
    lateinit var mentionsPickerAdapter: MentionsPickerAdapter
    private lateinit var behavior: BottomSheetBehavior<View>

    //child fragment share with parent fragment
    private val conversationViewModel: ConversationViewModel by viewModels(ownerProducer = { requireParentFragment() })

    private val handler = Handler(Looper.getMainLooper())


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val binding: MentionsPickerFragmentBinding = DataBindingUtil.inflate(inflater, R.layout.mentions_picker_fragment, container, false);
        binding.lifecycleOwner = viewLifecycleOwner
        behavior = BottomSheetBehavior.from(binding.mentionsPickerBottomSheet)
        initializeBehavior()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        mentions_picker_list.layoutManager = LinearLayoutManager(requireContext())
        mentions_picker_list.adapter = mentionsPickerAdapter
        mentions_picker_list.itemAnimator = null

        initListener()
        initObserver()
    }

    fun initListener(){
        mentionsPickerAdapter.setOnItemClickListener(object : MentionsPickerAdapter.OnItemClickListener {
            override fun onItemActiveClick(item: ThreadParticipantInfo, position: Int) {
                conversationViewModel.selectedMention.postValue(item)
                updateBottomSheetBehavior(mentionsPickerAdapter.itemCount)
            }
        })
    }

    fun initObserver(){
        conversationViewModel.mentionListMutableLiveData.observe(viewLifecycleOwner) { mentionList ->
            debugConfig.log(TAG, "Mention !!! size Mention ${mentionList.size}")
            mentionsPickerAdapter.submitList(mentionList)
        }

        conversationViewModel.liveQuery.observe(viewLifecycleOwner) { query ->
            debugConfig.log(TAG, "Mention liveQuery ${query} ")
            if (query != null) {
                val members = conversationViewModel.mentionListMutableLiveData.value ?: arrayListOf()
                if (query.isNotEmpty()) {
                    val membersFilter = members.filter { member ->
                        member.contactName.contains(query, ignoreCase = true) ||
                                member.firstName.contains(query, ignoreCase = true) ||
                                member.lastName.contains(query, ignoreCase = true) ||
                                member.username.contains(query, ignoreCase = true)
                    }
                    if (membersFilter.isNotEmpty()) {
                        mentionsPickerAdapter.submitList(membersFilter)
                        updateBottomSheetBehavior(membersFilter.size)
                    } else {
                        updateBottomSheetBehavior(0)
                    }

                } else {
                    mentionsPickerAdapter.submitList(members)
                    updateBottomSheetBehavior(members.size)
                }
            } else {
                updateBottomSheetBehavior(0)
            }
        }
    }

    private fun updateBottomSheetBehavior(count: Int) {
        val isShowing = count > 0
        if (isShowing) {
            mentions_picker_list.scrollToPosition(0)
            behavior.state = BottomSheetBehavior.STATE_COLLAPSED
            handler.post(lockSheetAfterListUpdate)
            showDividers(true)
        } else {
            handler.removeCallbacks(lockSheetAfterListUpdate)
            behavior.isHideable = true
            behavior.setState(BottomSheetBehavior.STATE_HIDDEN)
        }
    }

    private val lockSheetAfterListUpdate = Runnable {
        behavior.setHideable(false)
    }

    private fun initializeBehavior() {
        behavior.isHideable = true
        behavior.state = BottomSheetBehavior.STATE_HIDDEN
        behavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                    mentionsPickerAdapter.submitList(emptyList())
                    showDividers(false)
                } else {
                    showDividers(true)
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                showDividers(slideOffset.isNaN() || slideOffset > -0.8f)
            }
        })
    }

    private fun showDividers(showDividers: Boolean) {
        mentions_picker_top_divider.visibility = if (showDividers) View.VISIBLE else View.GONE
        mentions_picker_bottom_divider.visibility = if (showDividers) View.VISIBLE else View.GONE
    }
}