package com.lgt.cwm.activity.home.fragments.conversation

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.lgt.cwm.R
import com.lgt.cwm.activity.conversation.ConversationActivity
import com.lgt.cwm.activity.conversation.ConversationActivity.Companion.EXTRA_THREAD_ID
import com.lgt.cwm.activity.conversation.fragments.ConversationFragment
import com.lgt.cwm.activity.home.fragments.conversation.adapter.ConversationListAdapter
import com.lgt.cwm.databinding.FragmentConversationListBinding
import com.lgt.cwm.db.entity.Contact
import com.lgt.cwm.models.SignalThreadExt
import com.lgt.cwm.models.ThreadParticipantInfo
import com.lgt.cwm.ui.components.voice.VoiceNoteMediaControllerOwner
import com.lgt.cwm.ui.components.voice.VoiceNotePlayerView
import com.lgt.cwm.util.DebugConfig
import com.lgt.cwm.util.Result
import com.lgt.cwm.util.view.Stub
import cwmSignalMsgPb.CwmSignalMsg
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_conversation_list.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ConversationListFragment : Fragment(), ActionMode.Callback {
    private val TAG = ConversationListFragment::class.simpleName.toString()

    @Inject
    lateinit var debugConfig: DebugConfig

    @Inject
    lateinit var conversationListAdapter: ConversationListAdapter

    private val conversationListViewModel: ConversationListViewModel by viewModels()

    private var actionMode: ActionMode? = null

    private var voiceNotePlayerViewStub: Stub<FrameLayout>? = null
    private val voiceNotePlayerView: VoiceNotePlayerView? = null

    private lateinit var mediaControllerOwner: VoiceNoteMediaControllerOwner

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val binding: FragmentConversationListBinding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_conversation_list, container, false);
        binding.lifecycleOwner = viewLifecycleOwner
        binding.conversationListViewModel = conversationListViewModel

        binding.conversationListAdapter = conversationListAdapter

        voiceNotePlayerViewStub = Stub(binding.voiceNotePlayer.viewStub!!)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        conversationListAdapter.setOnItemClickListener(object :
            ConversationListAdapter.OnItemClickListener {
            override fun onItemActiveClick(item: SignalThreadExt, position: Int) {
//                debugConfig.log(TAG, "onItemActiveClick!!!!")

                if (actionMode == null) {
                    gotoChat(item.threadId)
                } else {
                    conversationListViewModel.toggleThreadSelected(item)
                    checkActionMode()
                }
            }

            override fun onItemLongClick(item: SignalThreadExt, position: Int) {
                if (actionMode == null) {
                    startActionMode()
                }
                conversationListViewModel.toggleThreadSelected(item)
                checkActionMode()
            }
        })

        initObserver()

        initializeVoiceNotePlayer()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        mediaControllerOwner = if (context is VoiceNoteMediaControllerOwner) {
            context
        } else {
            throw ClassCastException("Expected context to be a Listener")
        }
    }

    private var startConversationActivityLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == AppCompatActivity.RESULT_OK) {

            result.data?.apply {
                val gotoThreadId = this.getStringExtra(ConversationFragment.GO_TO_THREADID)
                if (!gotoThreadId.isNullOrEmpty()){
                    debugConfig.log(TAG, "GO_TO_THREADID ${gotoThreadId}")
                    gotoChat(gotoThreadId)
                }
            }


        }
    }

    fun initObserver(){
//        debugConfig.log(TAG, "initObserver")
        conversationListViewModel.allVerifiedSignalThreadLiveData.observe(viewLifecycleOwner) { threads ->
            conversationListViewModel.allVerifiedSignalThread = threads
            reloadListAdapter()
        }

        conversationListViewModel.allOTTContactLiveData.observe(viewLifecycleOwner) { contacts ->
//            debugConfig.log(TAG, "onContactsUpdate!!!!")
            reloadListAdapter()
        }

        conversationListViewModel.selectedThreadsMutableLiveData.observe(viewLifecycleOwner) { conversations ->

            val threadIds = mutableSetOf<String>()
            threadIds.addAll(conversations.map { c -> c.threadId })
//            debugConfig.log(TAG, "selectedThreadsMutableLiveData update: ${conversations.size}")
//            debugConfig.log(TAG, "selectedThreadsMutableLiveData update: ${threadIds}")

            conversationListAdapter.setSelectedThreadIds(threadIds)
            updateMultiSelectState()
        }
    }

    private fun reloadListAdapter(){
//        debugConfig.log(TAG, "onThreadsUpdate!!!!")

        lifecycleScope.launch {
            val threads = conversationListViewModel.allVerifiedSignalThread ?: return@launch

            val conversations = threads.filter { thread ->
                if (thread.threadType == CwmSignalMsg.SIGNAL_THREAD_TYPE.SOLO.number && thread.lastModified == 0L){
                    return@filter false
                }

                return@filter true
            }.map { thread ->

                var threadName = thread.threadName

                if (thread.threadType == CwmSignalMsg.SIGNAL_THREAD_TYPE.SOLO.number){
                    val contact = conversationListViewModel.findOneContactByPhoneFull(thread.phoneFull)
                    contact?.let {
                        threadName = it.name
                    }
                }
                val currentSignalThreadExt = conversationListAdapter.currentList.firstOrNull { signalThreadExt -> signalThreadExt.threadId.equals(thread.threadId) }
                var threadParticipantInfos = currentSignalThreadExt?.participantInfos ?: arrayListOf()
                if (
                    currentSignalThreadExt == null ||
                    currentSignalThreadExt.participantInfos.isEmpty() ||
                    !thread.participants.equals(currentSignalThreadExt.participants)
                ){
                    val cwmUsers = conversationListViewModel.getAllCWMUserByListPhoneFull(thread.participants)
                    threadParticipantInfos = cwmUsers.map { cwmUser ->
                        val contact: Contact? = conversationListViewModel.findOneContactByPhoneFull(cwmUser.phoneFull)
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

            conversationListAdapter.submitList(conversations)
        }
    }


    fun gotoChat(threadId: String){
        val intent = Intent(activity, ConversationActivity::class.java).apply {
            putExtra(EXTRA_THREAD_ID, threadId)
        }
        startConversationActivityLauncher.launch(intent)
        requireActivity().overridePendingTransition(R.anim.slide_from_end, R.anim.fade_scale_out);
    }


    //region action mode
    private fun updateMultiSelectState() {
        val count = conversationListViewModel.selectedThreadsMutableLiveData.value?.size ?: 0
        actionMode?.let {
            it.title = requireContext().resources.getQuantityString(
                R.plurals.ConversationListFragment_s_selected,
                count,
                count
            )
        }

    }

    private fun startActionMode() {
//        (activity as MainActivity).visibilityToolbar(false)
        actionMode = (activity as AppCompatActivity?)!!.startSupportActionMode(this@ConversationListFragment)
    //        ViewUtil.animateIn(bottomActionBar, bottomActionBar.getEnterAnimation())
//        ViewUtil.fadeOut(fab, 250)
//        ViewUtil.fadeOut(cameraFab, 250)
//        if (megaphoneContainer.resolved()) {
//            ViewUtil.fadeOut(megaphoneContainer.get(), 250)
//        }
//        requireCallback().onMultiSelectStarted()
    }
    private fun endActionMode() {
//        (activity as MainActivity).visibilityToolbar(true)
        actionMode?.let {
            it.finish()
            actionMode = null
//            requireCallback().onMultiSelectFinished()
        }
    }

    fun checkActionMode() {
        val currentSelectedThreads = conversationListViewModel.selectedThreadsMutableLiveData.value

        if (currentSelectedThreads == null || currentSelectedThreads.isEmpty()) {
            endActionMode()
        } else {
            updateMultiSelectState()
        }
    }

    override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
        mode.title = requireContext().resources.getQuantityString(
            R.plurals.ConversationListFragment_s_selected,
            1,
            1
        )

        val inflater: MenuInflater = mode.menuInflater
        inflater.inflate(R.menu.conversation_list_action_menu, menu)
        return true
    }

    override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
        updateMultiSelectState()
        return false
    }

    override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_delete -> {
                debugConfig.log(TAG, "Action Delete")
//                mode.finish() // Action picked, so close the CAB
                showDeleteConfirmPopup()
                true
            }
            R.id.action_select_all -> {
                debugConfig.log(TAG, "Action select all")
                conversationListViewModel.selectAllThreads(conversationListAdapter.currentList)
                checkActionMode()
                true
            }
            R.id.action_clear_history -> {
                debugConfig.log(TAG, "Action mark as unread")
//                mode.finish() // Action picked, so close the CAB
                showClearHistoryConfirmPopup()
                true
            }
//            R.id.action_archive -> {
//                debugConfig.log(TAG, "Action archive")
////                mode.finish() // Action picked, so close the CAB
//                true
//            }
            else -> false
        }
    }

    override fun onDestroyActionMode(mode: ActionMode) {
        conversationListViewModel.endSelection()
        endActionMode()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        voiceNotePlayerViewStub = null
    }

    //endregion action mode

    //region POPUP
    fun showDeleteConfirmPopup(){
        val currentSelectedThreads = conversationListViewModel.selectedThreadsMutableLiveData.value
        if (currentSelectedThreads == null || currentSelectedThreads.isEmpty()){
            return
        }

        var tilte = getString(R.string.popup_delete_chat_tilte)
        var message = getString(R.string.popup_delete_chat_msg)
        if (currentSelectedThreads.size == 1){
            val conversation = currentSelectedThreads.iterator().next()
            if (conversation.threadType == CwmSignalMsg.SIGNAL_THREAD_TYPE.GROUP.number){
                tilte = getString(R.string.popup_delete_chat_group_tilte)
                message = getString(R.string.popup_delete_chat_group_msg, conversation.threadName)
            }
        } else {
            tilte = getString(R.string.popup_delete_multiple_chats_tilte,currentSelectedThreads.size)
            message = getString(R.string.popup_delete_multiple_chats_msg)
        }


        AlertDialog.Builder(requireContext())
            .setTitle(tilte)
            .setMessage(message)
            .setPositiveButton(getString(R.string.btn_delete)) { _, _ ->
                debugConfig.log("DeleteConfirmPopup Delete")
                endActionMode()
                //TODO - IMPLEMENT QUERY CHECK deleteForAllMembers
                deleteThreads(true)
            }
            .setNegativeButton(getString(R.string.btn_cancel)) { _, _ ->
                debugConfig.log("DeleteConfirmPopup Cancel")
            }
            .show()
    }

    fun showClearHistoryConfirmPopup(){
        val currentSelectedThreads = conversationListViewModel.selectedThreadsMutableLiveData.value
        if (currentSelectedThreads == null || currentSelectedThreads.isEmpty()){
            return
        }

        var tilte = getString(R.string.popup_clear_chat_tilte)
        var message = getString(R.string.popup_clear_chat_msg)
        if (currentSelectedThreads.size == 1){
            val conversation = currentSelectedThreads.iterator().next()
            if (conversation.threadType == CwmSignalMsg.SIGNAL_THREAD_TYPE.GROUP.number){
                message = getString(R.string.popup_clear_chat_group_msg, conversation.threadName)
            }
        } else {
            tilte = getString(R.string.popup_clear_multiple_chats_tilte,currentSelectedThreads.size)
            message = getString(R.string.popup_clear_multiple_chats_msg)
        }


        AlertDialog.Builder(requireContext())
            .setTitle(tilte)
            .setMessage(message)
            .setPositiveButton(getString(R.string.btn_clear_history)) { _, _ ->
                debugConfig.log("ClearHistoryConfirmPopup Clear")
                endActionMode()
                //TODO - IMPLEMENT QUERY CHECK deleteForAllMembers
                clearAllMsgOfThreads(true)
            }
            .setNegativeButton(getString(R.string.btn_cancel)) { _, _ ->
                debugConfig.log("ClearHistoryConfirmPopup Cancel")
            }
            .show()
    }
    //endregion

    //region API
    fun deleteThreads(deleteForAllMembers: Boolean){
        lifecycleScope.launch {
            try {
                val currentSelectedThreads = conversationListViewModel.selectedThreadsMutableLiveData.value
                if (currentSelectedThreads == null || currentSelectedThreads.isEmpty()){
                    return@launch
                }

                spinner.visibility = View.VISIBLE
                val inputMethodManager =
                    activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                inputMethodManager.hideSoftInputFromWindow(activity?.currentFocus?.windowToken, 0)


                for (conversation in currentSelectedThreads.sortedBy { t -> t.lastModified }){
                    val isGroup = conversation.threadType == CwmSignalMsg.SIGNAL_THREAD_TYPE.GROUP.number
                    var deleteForAll = deleteForAllMembers
                    if (deleteForAll && isGroup){
                        //GROUP THREAD CAN ONLY BE LEFT AND DELETE FOR SELF
                        deleteForAll = false
                    }
                    val result = conversationListViewModel.deleteThread(conversation.threadId, isGroup, deleteForAll)
                    when (result) {
                        is Result.Success<Boolean> -> { }
                        is Result.Error -> {
                            Toast.makeText(activity ,"Failed To delete thread ${conversation.threadName} - ${conversation.phoneFull}: ${result.exception.toString()}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                spinner.visibility = View.GONE

            } catch (e: Exception) {
                e.printStackTrace()
            }

        }
    }

    fun clearAllMsgOfThreads(deleteForAllMembers: Boolean){
        lifecycleScope.launch {
            try {
                val currentSelectedThreads = conversationListViewModel.selectedThreadsMutableLiveData.value
                if (currentSelectedThreads == null || currentSelectedThreads.isEmpty()){
                    return@launch
                }

                spinner.visibility = View.VISIBLE
                val inputMethodManager =
                    activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                inputMethodManager.hideSoftInputFromWindow(activity?.currentFocus?.windowToken, 0)


                for (conversation in currentSelectedThreads.sortedBy { t -> t.lastModified }){
                    var deleteForAll = deleteForAllMembers
                    if (deleteForAll &&
                        conversation.threadType == CwmSignalMsg.SIGNAL_THREAD_TYPE.GROUP.number &&
                        !conversation.admin){
                        deleteForAll = false
                    }
                    val result = conversationListViewModel.clearAllMsgOfThread(conversation.threadId, deleteForAll)
                    when (result) {
                        is Result.Success<Boolean> -> { }
                        is Result.Error -> {
                            Toast.makeText(activity ,"Failed To clear thread ${conversation.threadName} - ${conversation.phoneFull}: ${result.exception.toString()}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                spinner.visibility = View.GONE

            } catch (e: Exception) {
                e.printStackTrace()
            }

        }
    }
    //endregion

    //region voice note
    private fun initializeVoiceNotePlayer() {
        mediaControllerOwner.voiceNoteMediaController.voiceNotePlayerViewState.observe(viewLifecycleOwner
        ) { state ->
            if (state.isPresent()) {
                requireVoiceNotePlayerView().setState(state.get())
                requireVoiceNotePlayerView().show()
            } else if (voiceNotePlayerViewStub!!.resolved()) {
                requireVoiceNotePlayerView().hide()
            }
        }
    }

    private fun requireVoiceNotePlayerView(): VoiceNotePlayerView {
        var voiceNotePlayerView = voiceNotePlayerView
        if (voiceNotePlayerView == null) {
            voiceNotePlayerView = voiceNotePlayerViewStub!!.get().findViewById(R.id.voice_note_player_view)
            voiceNotePlayerView.listener = VoiceNotePlayerViewListener()
        }
        return voiceNotePlayerView!!
    }

    private inner class VoiceNotePlayerViewListener : VoiceNotePlayerView.Listener {
        override fun onCloseRequested(uri: Uri) {
            if (voiceNotePlayerViewStub!!.resolved()) {
                mediaControllerOwner.voiceNoteMediaController.stopPlaybackAndReset(uri)
            }
        }

        override fun onSpeedChangeRequested(uri: Uri, speed: Float) {
            mediaControllerOwner.voiceNoteMediaController.setPlaybackSpeed(uri, speed)
        }

        override fun onPlay(uri: Uri, messageId: Long, position: Double) {
            mediaControllerOwner.voiceNoteMediaController.startSinglePlayback(uri, messageId, position)
        }

        override fun onPause(uri: Uri) {
            mediaControllerOwner.voiceNoteMediaController.pausePlayback(uri)
        }

        override fun onNavigateToMessage(threadId: Long, threadRecipientId: String, senderId: String, messageSentAt: Long, messagePositionInThread: Long) {

        }
    }

    //endregion voice note

}