package com.lgt.cwm.activity.conversation.fragments.forward

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.lgt.cwm.R
import com.lgt.cwm.activity.conversation.fragments.adapter.ContactShareSelectionAdapter
import com.lgt.cwm.activity.conversation.fragments.adapter.ShareSelectionAdapter
import com.lgt.cwm.databinding.MultiselectForwardFragmentBinding
import com.lgt.cwm.db.entity.Contact
import com.lgt.cwm.models.SignalThreadExt
import com.lgt.cwm.models.ThreadParticipantInfo
import com.lgt.cwm.ui.contact.LetterHeaderDecoration
import com.lgt.cwm.util.DebugConfig
import com.lgt.cwm.util.FullscreenHelper
import com.lgt.cwm.util.visible
import cwmSignalMsgPb.CwmSignalMsg
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.multiselect_forward_fragment.*
import kotlinx.android.synthetic.main.multiselect_forward_fragment_bottom_bar_and_spacer.view.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MultiselectForwardFragment : Fragment() {
    private val TAG = MultiselectForwardFragment::class.simpleName.toString()

    @Inject
    lateinit var debugConfig: DebugConfig

    private var callback: Callback? = null
//    private var dismissibleDialog: SimpleProgressDialog.DismissibleDialog? = null

    @Inject
    lateinit var contactShareSelectionAdapter: ContactShareSelectionAdapter
    private val multiselectForwardViewModel: MultiselectForwardViewModel by viewModels()
    private var messageIds: ArrayList<String> = arrayListOf()
    @Inject
    lateinit var shareSelectionAdapter: ShareSelectionAdapter

    fun setCallBack(callback: Callback?){
        this.callback = callback
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding: MultiselectForwardFragmentBinding = DataBindingUtil.inflate(inflater, R.layout.multiselect_forward_fragment, container, false);
        binding.lifecycleOwner = viewLifecycleOwner

        arguments?.let {
            messageIds = requireNotNull(it.getStringArrayList(MultiselectForwardBottomSheet.ARG_MESSAGES))
//            debugConfig.log(TAG, "message id selected to forward ${messageIds}")
        }


        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.minimumHeight = resources.displayMetrics.heightPixels

        bottom_bar_layout.selected_list.adapter = shareSelectionAdapter

        FullscreenHelper.configureBottomBarLayout(requireActivity(), bottom_bar_layout.bottom_bar_spacer, bottom_bar_layout.bottom_bar)
//        bottom_bar_layout.background_helper.setBackgroundColor(callback.getDialogBackgroundColor())
//        bottom_bar_layout.bottom_bar_spacer.setBackgroundColor(callback.getDialogBackgroundColor())
        bottom_bar_layout.bottom_bar.visible = false


        contact_selection_list.layoutManager = LinearLayoutManager(context)
        contact_selection_list.adapter = contactShareSelectionAdapter
        contact_selection_list.addItemDecoration(LetterHeaderDecoration(requireContext()) { false })



        initListener()
        initObserver()
    }

    fun initListener(){
        contactShareSelectionAdapter.setOnItemClickListener(object : ContactShareSelectionAdapter.OnItemClickListener {
            override fun onItemActiveClick(item: SignalThreadExt, position: Int, selected: Boolean) {

                if (selected) {
                    multiselectForwardViewModel.selectedThreads.add(item)
                }else{
                    multiselectForwardViewModel.selectedThreads.remove(item)
                }

                if (multiselectForwardViewModel.selectedThreads.size > 0){
                    toggleShowBottomBar(true)
                }else{
                    toggleShowBottomBar(false)
                }

                val threadNames = multiselectForwardViewModel.selectedThreads.map { signalThreadExt ->
                    signalThreadExt.threadName
                }

                shareSelectionAdapter.submitList(threadNames)

            }
        })

        bottom_bar_layout.bottom_bar.share_confirm.setOnClickListener {
            //show loading start send progress
//            dismissibleDialog?.dismiss()
//            dismissibleDialog = SimpleProgressDialog.showDelayed(requireContext())

            //dismiss loading if send progress complete
//            dismissibleDialog?.dismiss()

            //send all success or some fail
            dismissWithSuccess()
        }
    }

    fun initObserver(){
        multiselectForwardViewModel.allVerifiedSignalThreadLiveData.observe(viewLifecycleOwner) { threads ->
            multiselectForwardViewModel.allVerifiedSignalThread = threads
            reloadListAdapter()
        }
    }

    fun reloadListAdapter() {
        lifecycleScope.launch{
            val threads = multiselectForwardViewModel.allVerifiedSignalThread ?: return@launch

            val conversations = threads.filter { thread ->
                if (thread.threadType == CwmSignalMsg.SIGNAL_THREAD_TYPE.SOLO.number && thread.lastModified == 0L){
                    return@filter false
                }

                return@filter true
            }.map { thread ->

                var threadName = thread.threadName

                if (thread.threadType == CwmSignalMsg.SIGNAL_THREAD_TYPE.SOLO.number){
                    val contact = multiselectForwardViewModel.findOneContactByPhoneFull(thread.phoneFull)
                    contact?.let {
                        threadName = it.name
                    }
                }
                val currentSignalThreadExt = contactShareSelectionAdapter.currentList.firstOrNull { signalThreadExt -> signalThreadExt.threadId.equals(thread.threadId) }
                var threadParticipantInfos = currentSignalThreadExt?.participantInfos ?: arrayListOf()
                if (
                    currentSignalThreadExt == null ||
                    currentSignalThreadExt.participantInfos.isEmpty() ||
                    !thread.participants.equals(currentSignalThreadExt.participants)
                ){
                    val cwmUsers = multiselectForwardViewModel.getAllCWMUserByListPhoneFull(thread.participants)
                    threadParticipantInfos = cwmUsers.map { cwmUser ->
                        val contact: Contact? = multiselectForwardViewModel.findOneContactByPhoneFull(cwmUser.phoneFull)
                        ThreadParticipantInfo(
                            phoneFull = cwmUser.phoneFull,
                            contactName = contact?.name ?: "",
                            userId = cwmUser.userId ?: "",
                            username = cwmUser.username ?: "",
                            avatar = cwmUser.avatar ?: "",
                            firstName = cwmUser.firstName ?: "",
                            lastName = cwmUser.lastName ?: "",
                            isMyAcc = cwmUser.isMyAcc
                        )
                    }
                }

                return@map SignalThreadExt(thread, threadName, threadParticipantInfos)
            }

            contactShareSelectionAdapter.submitList(conversations)
        }
    }

    fun toggleShowBottomBar(isShow: Boolean) {
        if (isShow && !bottom_bar_layout.bottom_bar.isVisible) {
            bottom_bar_layout.bottom_bar.animation = AnimationUtils.loadAnimation(requireContext(), R.anim.slide_fade_from_bottom)
            bottom_bar_layout.bottom_bar.visible = true
        } else if (!isShow && bottom_bar_layout.bottom_bar.isVisible) {
            bottom_bar_layout.bottom_bar.animation = AnimationUtils.loadAnimation(requireContext(), R.anim.slide_fade_to_bottom)
            bottom_bar_layout.bottom_bar.visible = false
        }
    }

//    override fun onDestroyView() {
//        dismissibleDialog?.dismissNow()
//        super.onDestroyView()
//    }

    private fun dismissWithSuccess() {
        dismissAndShowToast()
    }

    private fun dismissAndShowToast() {
        callback?.onFinishForwardAction()
//        dismissibleDialog?.dismiss()

        val threadIds = multiselectForwardViewModel.selectedThreads.map { signalThreadExt -> signalThreadExt.threadId  }

        val resultsBundle = Bundle().apply {
            putStringArrayList(MultiselectForwardBottomSheet.RESULT_THREADS_SELECTION, ArrayList(threadIds))
            putStringArrayList(MultiselectForwardBottomSheet.RESULT_MSGS_SELECTION, messageIds)
        }
        callback?.setResult(resultsBundle)
        callback?.exitFlow()
    }


    interface Callback {
        fun onFinishForwardAction()
        fun exitFlow()
        fun onSearchInputFocused()
        fun setResult(bundle: Bundle)
        fun getContainer(): ViewGroup
        fun getDialogBackgroundColor(): Int
    }


}
