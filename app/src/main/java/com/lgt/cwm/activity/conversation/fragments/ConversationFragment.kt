package com.lgt.cwm.activity.conversation.fragments

import android.Manifest
import android.animation.Animator
import android.animation.ValueAnimator
import android.app.Activity
import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.graphics.PorterDuff
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.provider.MediaStore
import android.text.Annotation
import android.text.Editable
import android.text.TextWatcher
import android.text.format.DateUtils
import android.text.style.URLSpan
import android.view.*
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.animation.*
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.MainThread
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemDecoration
import com.lgt.cwm.BuildConfig
import com.lgt.cwm.R
import com.lgt.cwm.activity.conversation.ConversationActivityViewModel
import com.lgt.cwm.activity.conversation.fragments.adapter.ConversationAdapter
import com.lgt.cwm.activity.conversation.fragments.forward.MultiselectForwardBottomSheet
import com.lgt.cwm.activity.conversation.fragments.models.QuoteModel
import com.lgt.cwm.business.media.audio.AudioRecorder
import com.lgt.cwm.business.media.linkpreview.LinkPreview
import com.lgt.cwm.business.media.linkpreview.LinkPreviewRepository
import com.lgt.cwm.business.media.mention.MentionValidatorWatcher
import com.lgt.cwm.databinding.FragmentConversationBinding
import com.lgt.cwm.db.entity.Contact
import com.lgt.cwm.db.entity.SignalMsgDirection
import com.lgt.cwm.db.entity.SignalMsgStatus
import com.lgt.cwm.db.entity.SignalThread
import com.lgt.cwm.models.*
import com.lgt.cwm.ui.animation.AnimationCompleteListener
import com.lgt.cwm.ui.avatar.AvatarConstants
import com.lgt.cwm.ui.avatar.AvatarGenerator
import com.lgt.cwm.ui.avatar.RandomColors
import com.lgt.cwm.ui.components.MicrophoneRecorderView
import com.lgt.cwm.ui.components.permissions.Permissions
import com.lgt.cwm.ui.components.voice.VoiceNoteDraft
import com.lgt.cwm.ui.components.voice.VoiceNoteMediaController
import com.lgt.cwm.ui.components.voice.VoiceNotePlaybackState
import com.lgt.cwm.ui.components.voice.VoiceNotePlayerView
import com.lgt.cwm.ui.conversation.*
import com.lgt.cwm.ui.glide.GlideApp
import com.lgt.cwm.ui.glide.GlideRequests
import com.lgt.cwm.ui.menu.ActionItem
import com.lgt.cwm.ui.menu.SignalContextMenu
import com.lgt.cwm.ui.popup.ModalBottomSheetDialogFragment
import com.lgt.cwm.ui.recycleview.SmoothScrollingLinearLayoutManager
import com.lgt.cwm.util.*
import com.lgt.cwm.util.concurrent.AssertedSuccessListener
import com.lgt.cwm.util.concurrent.ListenableFuture
import com.lgt.cwm.util.concurrent.SettableFuture
import com.lgt.cwm.util.view.Stub
import com.vanniktech.emoji.EmojiPopup
import cwmSignalMsgPb.CwmSignalMsg
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.chat_input_panel.*
import kotlinx.android.synthetic.main.chat_input_panel.view.*
import kotlinx.android.synthetic.main.conversation_toolbar.view.*
import kotlinx.android.synthetic.main.fragment_conversation.*
import kotlinx.android.synthetic.main.recording_layout.view.*
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.min


@AndroidEntryPoint
class ConversationFragment : Fragment(),
    ActionMode.Callback,
    AttachmentKeyboard.Callback,
    MicrophoneRecorderView.Listener {
    private val TAG = ConversationFragment::class.simpleName.toString()

//    val args: ConversationFragment by navArgs()

    companion object {
        const val SCROLL_ANIMATION_THRESHOLD = 50
        const val FADE_TIME = 150
        const val QUOTE_REVEAL_DURATION_MILLIS: Long = 150

        const val GO_TO_THREADID = "GO_TO_THREADID"
    }

    @Inject
    lateinit var debugConfig: DebugConfig

    @Inject
    lateinit var conversationAdapter: ConversationAdapter

    private val conversationActivityViewModel: ConversationActivityViewModel by activityViewModels()
    private val conversationViewModel: ConversationViewModel by viewModels()

    private lateinit var glideRequests: GlideRequests
    private lateinit var conversationScrollListener: RecyclerView.OnScrollListener
    private var inlineDateDecoration: ItemDecoration? = null

    private lateinit var audioRecorder: AudioRecorder
    private lateinit var voiceNoteMediaController: VoiceNoteMediaController
    private var voiceNotePlayerView: VoiceNotePlayerView? = null
    private lateinit var voiceNotePlayerViewStub: Stub<FrameLayout>
    private lateinit var mentionsSuggestions: Stub<View>

    private lateinit var slideToCancel: SlideToCancel
    private lateinit var recordTime: RecordTime
    private lateinit var voiceRecorderWakeLock: VoiceRecorderWakeLock

    private var activeContextMenu: SignalContextMenu? = null
    private var actionMode: ActionMode? = null
    private lateinit var scrollButtonInAnimation: Animation
    private lateinit var scrollButtonOutAnimation: Animation

    private lateinit var emojiPopup: EmojiPopup

    private var currentPhotoCameraPath = ""
    private lateinit var currentPhotoCameraUri: Uri

    private lateinit var quoteView: QuoteView
    private var quoteAnimator: ValueAnimator? = null

    // define 'afterMeasured' layout listener:
//    inline fun <T: View> T.afterMeasured(crossinline f: T.() -> Unit) {
//        viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
//            override fun onGlobalLayout() {
//                //OnGlobalLayoutListener
//                //This listener is available for any view’s ViewTreeObserver and it’s quite often used to get a callback when the view is inflated and measured, and we already have a width and height available to do any kind of calculations, animations, etc.
//                if (measuredWidth > 0 && measuredHeight > 0) {
//                    viewTreeObserver.removeOnGlobalLayoutListener(this)
//                    f()
//                }
//            }
//        })
//    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding: FragmentConversationBinding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_conversation, container, false);
        binding.lifecycleOwner = viewLifecycleOwner
        binding.conversationViewModel = conversationViewModel
        binding.conversationAdapter = conversationAdapter

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeView()

        glideRequests = GlideApp.with(this)
        emojiPopup = EmojiPopup(
            rootView = viewInputPanel.layoutCompose, editText = editTextCompose,
            onSoftKeyboardCloseListener = { },
            onEmojiClickListener = { },
            onSoftKeyboardOpenListener = { },
            onEmojiPopupShownListener = { viewCompose.imageButtonEmoji.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.ic_keyboard)) },
            onEmojiPopupDismissListener = { viewCompose.imageButtonEmoji.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.ic_emoji)) },
            onEmojiBackspaceClickListener = { viewCompose.imageButtonEmoji.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.ic_emoji)) },
        )

        voiceRecorderWakeLock = VoiceRecorderWakeLock(requireActivity())
        audioRecorder = AudioRecorder(requireContext())
        voiceNoteMediaController = VoiceNoteMediaController(requireActivity())

        voiceNoteMediaController.voiceNotePlayerViewState.observe(viewLifecycleOwner) { state ->
            debugConfig.log("observe voiceNotePlayerViewState ${state}")
            if (state.isPresent) {
                requireVoiceNotePlayerView().show()
                requireVoiceNotePlayerView().setState(state.get())
            } else if (voiceNotePlayerViewStub.resolved()) {
                requireVoiceNotePlayerView().hide()
            }
        }

        initListeners()

        initActivityObserve()

    }

    override fun onPause() {
        super.onPause()
        updateThreadLastPosMsgId()
    }

    private fun initializeView() {
        //initialize
        initializeActionBar()

        rvChat.setHasFixedSize(false)
        val layoutManager: LinearLayoutManager = SmoothScrollingLinearLayoutManager(requireActivity(), true)
        rvChat.layoutManager = layoutManager

        conversationScrollListener = ConversationScrollListener(requireContext())
        rvChat.addOnScrollListener(conversationScrollListener)
        setInlineDateDecoration(conversationAdapter)

        scroll_to_bottom.setUnreadCount(0)
        initializeScrollButtonAnimations()

        slideToCancel = SlideToCancel(viewInputPanel.slide_to_cancel)
        viewInputPanel.recorder_view.setListener(this)

        mentionsSuggestions = Stub(conversation_mention_suggestions_stub)

        voiceNotePlayerViewStub = Stub(voice_note_player_stub)
        recordTime = RecordTime(viewInputPanel.record_time, viewInputPanel.microphone,
            TimeUnit.HOURS.toSeconds(1)
        ) { viewInputPanel.recorder_view.cancelAction() }
        viewInputPanel.record_cancel.setOnClickListener { v: View? -> viewInputPanel.recorder_view.cancelAction() }

        viewCompose.button_toggle.background.setColorFilter(ContextCompat.getColor(requireContext(), R.color.teal_400), PorterDuff.Mode.MULTIPLY)
        quoteView = viewInputPanel.quote_view
    }

    private fun initActivityObserve(){
        conversationActivityViewModel.threadReadyLiveData.observe(viewLifecycleOwner) { threadReady ->
            if (threadReady) {
                initObserve()
            }
        }

        conversationViewModel.selectedMessagesMutableLiveData.observe(viewLifecycleOwner) { messageIds ->
//            debugConfig.log(TAG, "${messageIds}")
            conversationAdapter.setSelectedMessages(messageIds)
            updateMultiSelectState()
        }
    }

    private fun initObserve(){
        conversationActivityViewModel.signalThreadExt?.let { signalThreadExt ->
            conversationViewModel.signalThreadExt = signalThreadExt
            if (signalThreadExt.threadType == CwmSignalMsg.SIGNAL_THREAD_TYPE.GROUP.number){
                initializeMentions()
                conversationViewModel.mentionListMutableLiveData.postValue(
                    signalThreadExt.participantInfos.filter { p -> !p.isMyAcc }
                )
            }


            updateToolbarTitle()

            val size = ViewUtil.dpToPx(48)
            val name = toolbar.textViewTitle.text.toString()
            val avatar = AvatarGenerator.AvatarBuilder(requireContext())
                .setLabel(name)
                .setAvatarSize(size)
                .setTextSize(24)
                .setBackgroundColor(RandomColors(AvatarConstants.COLOR700).getColor(name))
                .toCircle()
                .build()
            toolbar.imageViewAvatar.setImageDrawable(avatar)

            signalThreadExt.lastViewPos?.let { lastViewPos ->
                if (signalThreadExt.unreadMsgs > 0){
                    if (lastViewPos > 0){
                        conversationViewModel.lastViewPos = lastViewPos + signalThreadExt.unreadMsgs.toInt()
                    }else{
                        conversationViewModel.lastViewPos = lastViewPos + signalThreadExt.unreadMsgs.toInt() - 1
                    }
                }else{
                    conversationViewModel.lastViewPos = lastViewPos
                }

            }

            conversationViewModel.getThreadByThreadIdLiveData(signalThreadExt.threadId).observe(viewLifecycleOwner) { signalThread ->
                onSignalThreadUpdate(signalThread)
            }

            conversationViewModel.allMsgExtHaveContentByThreadIdFlow(signalThreadExt.threadId).observe(viewLifecycleOwner) { signalMsgExtList ->
                conversationViewModel.signalMsgExtList = signalMsgExtList
                reloadListMsgAdapter()


                val startUnreadPosition = signalMsgExtList.indexOfLast { signalMsgExt -> signalMsgExt.status == SignalMsgStatus.RECEIVED_UNREAD.code }
                if (startUnreadPosition >= 0){
                    conversationViewModel.startUnreadPosition = startUnreadPosition
                }else{
                    conversationViewModel.startUnreadPosition = null
                }
            }

            conversationViewModel.countAllMsgIdByThreadIdAndStatus(signalThreadExt.threadId, SignalMsgStatus.RECEIVED_UNREAD.code).observe(viewLifecycleOwner) { unreadMsgCount ->
//                debugConfig.log(TAG, "on count unread Msg - ${count}")
                conversationViewModel.unreadMsgCount = unreadMsgCount.toInt()
                scroll_to_bottom.setUnreadCount(conversationViewModel.unreadMsgCount)
            }

            conversationViewModel.allMsgIdByThreadIdAndNotSendSeenState(signalThreadExt.threadId).observe(viewLifecycleOwner) { msgIdList ->
//                debugConfig.log(TAG, "on unread Msg - ${msgIdList.size}")
                conversationViewModel.unsendSeenStateMsgIds = msgIdList
                if (conversationViewModel.unsendSeenStateMsgIds.isNotEmpty()){
                    sendSeenStateMsg()
                }
            }

        }

        conversationActivityViewModel.contact?.let { c ->
            conversationViewModel.getContactByIdLiveData(c.id).observe(viewLifecycleOwner) { contact ->
                contact?.let {
                    conversationActivityViewModel.contact = it
                    updateToolbarTitle()
                }
            }
        }

        conversationViewModel.countAllUnhanldedEventMsg.observe(viewLifecycleOwner){  numberUnhanldedEventMsg ->
            if (numberUnhanldedEventMsg > 0){
//                debugConfig.log(TAG,"numberUnhanldedEventMsg  ${numberUnhanldedEventMsg}")
                conversationViewModel.startWorkerMessageEventHanlde()
            }
        }

        conversationViewModel.countAllSendingMsg.observe(viewLifecycleOwner){  numberSendingMsg ->
            if (numberSendingMsg > 0){
//                debugConfig.log(TAG,"numberSendingMsg  ${numberSendingMsg}")
                conversationViewModel.startWorkerMessageTrySend()
            }
        }

        conversationViewModel.signalTypingMsgLiveData.observe(viewLifecycleOwner){ signalTypingMsgExt ->
            updateToolbarTyping(signalTypingMsgExt)
        }

        conversationViewModel.typingTimeout.observe(viewLifecycleOwner){ isTimeout ->
            if (isTimeout) {
                updateToolbarTitle()
            }
        }


    }

    private fun onSignalThreadUpdate(signalThread: SignalThread?){
        lifecycleScope.launch{
            if (signalThread == null){
                conversationViewModel.signalThreadExt = null
                return@launch
            }

            var threadName = signalThread.threadName

            if (signalThread.threadType == CwmSignalMsg.SIGNAL_THREAD_TYPE.SOLO.number){
                conversationActivityViewModel.contact?.let{
                    threadName = it.name
                }
            }

            val currentSignalThreadExt = conversationViewModel.signalThreadExt
            var threadParticipantInfos = currentSignalThreadExt?.participantInfos ?: arrayListOf()
            if (
                currentSignalThreadExt == null ||
                threadParticipantInfos.isEmpty() ||
                !signalThread.participants.equals(currentSignalThreadExt.participants)
            ){
                val cwmUsers = conversationActivityViewModel.getAllCWMUserByListPhoneFull(signalThread.participants)
                threadParticipantInfos = cwmUsers.map { cwmUser ->
                    val threadContact: Contact? = conversationActivityViewModel.findOneContactByPhoneFull(cwmUser.phoneFull)
                    ThreadParticipantInfo(
                        phoneFull = cwmUser.phoneFull,
                        contactName = threadContact?.name ?: "",
                        userId = cwmUser.userId ?: "",
                        username = cwmUser.username ?: "",
                        avatar = cwmUser.avatar ?: "",
                        firstName = cwmUser.firstName ?: "",
                        lastName = cwmUser.lastName ?: "",
                        isMyAcc = cwmUser.isMyAcc
                    )
                }


                if (signalThread.threadType == CwmSignalMsg.SIGNAL_THREAD_TYPE.GROUP.number){
                    conversationViewModel.mentionListMutableLiveData.postValue(
                        threadParticipantInfos.filter { p -> !p.isMyAcc }
                    )
                }

            }

            val signalThreadExt = SignalThreadExt(signalThread, threadName, threadParticipantInfos)

            conversationViewModel.signalThreadExt = signalThreadExt

            updateToolbarTitle()

            reloadListMsgAdapter()
        }
    }



    private fun updateToolbarTitle(){
        val signalThreadExt = conversationViewModel.signalThreadExt ?: return

        if (signalThreadExt.threadType == CwmSignalMsg.SIGNAL_THREAD_TYPE.GROUP.number){
            toolbar.textViewTitle.text = signalThreadExt.threadName
            toolbar.textViewSubTitle.text = getString(R.string.thread_title_participants,signalThreadExt.participants.size)
        }else{
            val contact = conversationActivityViewModel.contact
            if (contact != null){
                toolbar.textViewTitle.text = contact.name
                toolbar.textViewSubTitle.text = contact.standardizedPhoneNumber
            } else if (signalThreadExt.threadName.isNotEmpty()){
                toolbar.textViewTitle.text = signalThreadExt.threadName
                toolbar.textViewSubTitle.text = signalThreadExt.phoneFull
            }else{
                toolbar.textViewTitle.text = signalThreadExt.phoneFull
                toolbar.textViewSubTitle.text = ""
            }
        }
    }

    private fun updateToolbarTyping(signalTypingMsgExt: SignalTypingMsgExt){
        val signalThreadExt = conversationViewModel.signalThreadExt ?: return

        if (!signalTypingMsgExt.threadId.equals(signalThreadExt.threadId)){
            return
        }

        if (signalTypingMsgExt.typingType ==  CwmSignalMsg.SIGNAL_TYPING_MSG_TYPE.M_UNTYPING) {
            if (signalTypingMsgExt.from.equals(conversationViewModel.userTyping)){
                updateToolbarTitle()
            }

            return
        }

        var typingName = signalTypingMsgExt.fromFirstName
        if (!signalTypingMsgExt.fromLastName.isNullOrEmpty()){
            typingName += " ${signalTypingMsgExt.fromLastName}"
        }

        val threadParticipantInfo = signalThreadExt.participantInfos.firstOrNull { participantInfo ->
            participantInfo.phoneFull.equals(signalTypingMsgExt.from)
        }

        threadParticipantInfo?.let {
            typingName = it.getName(requireContext())
        }

        toolbar.textViewSubTitle.text = getString(R.string.thread_title_user_typing,typingName)
        conversationViewModel.userTyping = signalTypingMsgExt.from
        conversationViewModel.startTypingCounter()
    }

    private fun reloadListMsgAdapter(){
//        debugConfig.log(TAG,"reloadListMsgAdapter")

        val currentSignalMsgExtList = conversationViewModel.signalMsgExtList ?: return

//        debugConfig.log(TAG,"reloadListMsgAdapter - pass")

        val signalMsgExtList = currentSignalMsgExtList.map{ signalMsgExt ->
            if (signalMsgExt.imType == CwmSignalMsg.SIGNAL_IM_TYPE.GROUP_THREAD_NOTIFICATION.number){
                val signalGroupThreadNotificationMessageProto = signalMsgExt.contentSignalGroupThreadNotificationMessage
                signalGroupThreadNotificationMessageProto?.let {
                    val executor = signalGroupThreadNotificationMessageProto.executor
                    val executorInfo = conversationViewModel.signalThreadExt?.participantInfos?.firstOrNull { participantInfo -> participantInfo.phoneFull.equals(executor) }

                    val creator = signalGroupThreadNotificationMessageProto.creator
                    val creatorInfo = conversationViewModel.signalThreadExt?.participantInfos?.firstOrNull { participantInfo -> participantInfo.phoneFull.equals(creator) }


                    val targetMembers = signalGroupThreadNotificationMessageProto.targetMembersList
                    val targetMemberNames = targetMembers.map { targetMember ->
                        val targetMemberInfo = conversationViewModel.signalThreadExt?.participantInfos?.firstOrNull { participantInfo -> participantInfo.phoneFull.equals(targetMember) }
                        targetMemberInfo?.getName(requireContext()) ?: targetMember
                    }
                    val targetMemberInfoStr = targetMemberNames.joinToString(separator = ", ")

                    signalMsgExt.groupExecutorInfo = executorInfo
                    signalMsgExt.groupCreatorInfo = creatorInfo
                    signalMsgExt.groupTargetMemberInfoStr = targetMemberInfoStr
                }
            }

            signalMsgExt
        }

        conversationViewModel.signalMsgExtList = signalMsgExtList
        conversationAdapter.submitList(signalMsgExtList)
    }



    private fun initListeners() {
        conversationAdapter.setOnItemClickListener(object :
            ConversationAdapter.OnItemClickListener {
            override fun onItemClick(item: SignalMsgExt, position: Int) {
//                debugConfig.log(TAG, "click  ${position}")
                if (actionMode != null) {
                    handleActionMode(item)
                }
            }

            override fun onItemLongClick(item: SignalMsgExt, position: Int, view: View) {
//                debugConfig.log(TAG, "long click  ${position}")
                if (actionMode != null) {
                    handleActionMode(item)
                    return
                }

                if (activeContextMenu != null) {
//                    debugConfig.log(TAG, "Already showing a context menu.")
                    return
                }

                view.isSelected = true

                val actionItems: MutableList<ActionItem> = mutableListOf()

                if (item.status == SignalMsgStatus.SENT_FAIL.code){
                    actionItems.add(ActionItem(
                        R.drawable.ic_reply_24,
                        resources.getString(R.string.conversation_selection__menu_resend_message)
                    ) { resendMsg(item) })
                }

                actionItems.add(ActionItem(
                    R.drawable.ic_reply_24,
                    resources.getString(R.string.conversation_selection__menu_reply)
                ) { handleReply(item) })
                actionItems.add(ActionItem(
                    R.drawable.ic_forward_24,
                    resources.getString(R.string.conversation_selection__menu_forward)
                ) { handleForward(listOf(item.msgId)) })
                actionItems.add(
                    ActionItem(
                        R.drawable.ic_copy_24,
                        resources.getString(R.string.conversation_selection__menu_copy)
                    ) { handleCopy(item) })
                actionItems.add(
                    ActionItem(
                        R.drawable.ic_select_24,
                        resources.getString(R.string.conversation_selection__menu_multi_select)
                    ) { handleSelected(item) })
                actionItems.add(
                    ActionItem(
                        R.drawable.ic_info_24,
                        resources.getString(R.string.conversation_selection__menu_message_details)
                    ) { handleInfo(item) })
                actionItems.add(
                    ActionItem(
                        R.drawable.ic_delete_24,
                        resources.getString(R.string.conversation_selection__menu_delete)
                    ) { handleDelete(item) })

                activeContextMenu = SignalContextMenu.Builder(view, rvChat)
                    .preferredHorizontalPosition(SignalContextMenu.HorizontalPosition.END)
                    .offsetY(ViewUtil.dpToPx(8))
                    .offsetX(ViewUtil.dpToPx(16))
                    .onDismiss {
                        activeContextMenu = null
                        view.isSelected = false
                        rvChat.suppressLayout(false)
                    }
                    .show(actionItems)

                rvChat.suppressLayout(true)
            }

        })

        conversationAdapter.setItemEventListener(object :
            ConversationItem.ItemEventListener {
            override fun onVoiceNotePlay(uri: Uri, messageId: String, position: Double) {
                voiceNoteMediaController.startConsecutivePlayback(uri, 0, position)
            }

            override fun onVoiceNotePause(uri: Uri) {
                voiceNoteMediaController.pausePlayback(uri)
            }

            override fun onVoiceNoteSeekTo(uri: Uri, position: Double) {
                voiceNoteMediaController.seekToPosition(uri, position)
            }

            override fun onRegisterVoiceNoteCallbacks(onPlaybackStartObserver: Observer<VoiceNotePlaybackState>) {
                voiceNoteMediaController.voiceNotePlaybackState.observe(viewLifecycleOwner, onPlaybackStartObserver)
            }

            override fun onUnregisterVoiceNoteCallbacks(onPlaybackStartObserver: Observer<VoiceNotePlaybackState>) {
                voiceNoteMediaController.voiceNotePlaybackState.removeObserver(onPlaybackStartObserver)
            }

            override fun onSetQuote(item: ConversationItem, message: SignalMsgExt) {
                lifecycleScope.launch{
                    val quoteMsg = conversationViewModel.findQuoteMsgByMsgId(message.replyMsgId)
                    if (quoteMsg != null) {
                        item.setQuote(message, quoteMsg, null, null, false)
                    }
                }
            }

            override fun onQuoteClicked(quoteMsg: SignalMsgExt) {
                debugConfig.log(TAG, "onQuoteClicked ${quoteMsg.msgId}")
                val position = conversationAdapter.getPositionByItem(quoteMsg)
                if (position >= 0) {
                    getListLayoutManager().scrollToPositionWithOffset(position,rvChat.height/6)
                    //highlight item
                    conversationAdapter.pulseAtPosition(position)
                } else {
                    Toast.makeText(context, R.string.ConversationFragment_quoted_message_not_found, Toast.LENGTH_SHORT).show()
                    return
                }
            }

            override fun onLinkPreviewClicked(linkPreview: LinkPreview) {
                CommunicationActions.openBrowserLink(requireContext(), linkPreview.url)
            }

            override fun onGroupMemberClicked(memberId: String, groupId: String) {
                debugConfig.log(TAG, "onGroupMemberClicked memberId ${memberId} - groupId ${groupId}")
            }

            override fun onUrlClicked(urlSpan: URLSpan, widget: View?): Boolean {
                debugConfig.log(TAG, "onUrlClicked ${urlSpan.url}")
                CommunicationActions.openActionView(urlSpan, requireContext())
                return true
            }
        })

        conversationAdapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver(){
            override fun onItemRangeInserted(
                positionStart: Int,
                itemCount: Int
            ) {
                super.onItemRangeInserted(positionStart, itemCount)
//                debugConfig.log(TAG, "registerAdapterDataObserver onItemRangeInserted - positionStart ${positionStart} - itemCount ${itemCount}")
                if (conversationViewModel.lastViewPos != null){
                    var lastViewPos = conversationViewModel.lastViewPos ?: 0
                    if (lastViewPos >= conversationAdapter.currentList.size){
                        lastViewPos = conversationAdapter.currentList.size - 1
                    }

                    scrollToPos(lastViewPos)

                    conversationViewModel.lastViewPos = null

                }
                else{
                    if (currentViewPosition() == 0){
                        scrollToPos(0)

                    }else{
                        val lastSignalMsgExt = conversationAdapter.currentList.firstOrNull()
                        if (lastSignalMsgExt != null && lastSignalMsgExt.direction == SignalMsgDirection.OUTGOING.code){
                            scrollToPos(0)
                        }
                    }
                }
            }

            override fun onItemRangeMoved(fromPosition: Int, toPosition: Int, itemCount: Int) {
                super.onItemRangeMoved(fromPosition, toPosition, itemCount)

                debugConfig.log(TAG, "registerAdapterDataObserver onItemRangeMoved - fromPosition ${fromPosition} - toPosition ${toPosition} - itemCount ${itemCount} - currentList ${conversationAdapter.currentList.size}")
                val lastSignalMsgExt = conversationAdapter.currentList.firstOrNull()
                if (lastSignalMsgExt != null && lastSignalMsgExt.direction == SignalMsgDirection.OUTGOING.code){
                    scrollToPos(0)
                }
            }
        })

        imageButtonSend.setOnClickListener{
            sendIMMsg()
        }

        scroll_to_bottom.setOnClickListener {
            scrollToBottom()
        }

        viewCompose.imageButtonAttachFile.setOnClickListener {
            ModalBottomSheetDialogFragment.newInstance(this).show(childFragmentManager, ModalBottomSheetDialogFragment::class.java.canonicalName)
        }

        viewCompose.imageButtonEmoji.setOnClickListener {
            emojiPopup.toggle()

//            val emojiPopup = EmojiPopup(layoutCompose, editTextCompose)
//            emojiPopup.toggle() // Toggles visibility of the Popup.
//            emojiPopup.dismiss() // Dismisses the Popup.
//            emojiPopup.isShowing // Returns true when Popup is showing.
        }

        editTextCompose.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val text = s?.toString()
                val typing = !text.isNullOrEmpty()
                if (typing != conversationViewModel.isTyping){
                    sendTypingMsg(typing)
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
//                debugConfig.log(TAG, "onTextChanged ${s} - ${start} - ${before} - ${count}")
                val txt = s ?: ""
                conversationViewModel.onTextChanged(txt.trim().toString())
            }
        })


        conversationViewModel.linkPreviewState.observe(viewLifecycleOwner){ previewState ->
            if (previewState.isLoading) {
                setLinkPreviewLoading()
            } else if (previewState.hasLinks && previewState.linkPreview == null) {
                setLinkPreviewNoPreview(previewState.error)
            } else {
                setLinkPreview(glideRequests, previewState.linkPreview)
            }

        }

        viewCompose.link_preview.setCloseClickedListener(object : LinkPreviewView.CloseClickedListener {
            override fun onCloseClicked() {
                conversationViewModel.onUserCancelLinkPreview()
            }
        })

        childFragmentManager.setFragmentResultListener(MultiselectForwardBottomSheet.RESULT_KEY, this) { _, bundle ->

            val threadIds = bundle.getStringArrayList(MultiselectForwardBottomSheet.RESULT_THREADS_SELECTION)
            val msgIds = bundle.getStringArrayList(MultiselectForwardBottomSheet.RESULT_MSGS_SELECTION)
            debugConfig.log(TAG, "Thread selected to forward ${threadIds}")
            debugConfig.log(TAG, "msg selected to forward ${msgIds}")


            if (!threadIds.isNullOrEmpty() && !msgIds.isNullOrEmpty()){
                sendForwardMsg(
                    threadIds = threadIds,
                    msgIds = msgIds
                )
            }
        }
    }

    fun initializeMentions() {
        editTextCompose.setMentionQueryChangedListener(object : ComposeText.MentionQueryChangedListener {
            override fun onQueryChanged(query: String?) {
                if (!mentionsSuggestions.resolved()) {
                    mentionsSuggestions.get()
                }
                conversationViewModel.liveQuery.postValue(query)
            }
        })

        conversationViewModel.selectedMention.observe(viewLifecycleOwner) {
            debugConfig.log(TAG, "Mention replace ${it.getName(requireContext())}")
            editTextCompose.replaceTextWithMention(
                it.getName(requireContext()),
                it.phoneFull
            )
        }

        editTextCompose.setMentionValidator(object : MentionValidatorWatcher.MentionValidator {
            override fun getInvalidMentionAnnotations(mentionAnnotations: List<Annotation>): List<Annotation> {
                debugConfig.log(TAG, "Mention getInvalidMentionAnnotations ${mentionAnnotations.size}")
                return emptyList()
            }
        })
    }

    private fun sendTypingMsg(isTyping: Boolean){
        lifecycleScope.launch{
            debugConfig.log(TAG, "do sendTyping message - ${isTyping}")
            conversationViewModel.sendTypingMsg(isTyping)
        }
    }


    private fun resendMsg(message: SignalMsgExt){
        lifecycleScope.launch{
//            debugConfig.log(TAG, "do resendMsg message")
            conversationViewModel.resendMsg(message.msgId)
            editTextCompose.setText("")
        }
    }

    private fun sendMediaMsg(fileMetaDatas: List<FileMetaData>){
        lifecycleScope.launch{
//            debugConfig.log(TAG, "do send media message")

            val quote = getQuote()
            if (quote != null) {
                val replyMsgId = quote.replyId
                if (replyMsgId.isEmpty()){
                    debugConfig.log(TAG, "sendReplyMediaMsg - invalid replyMsgId")
                    return@launch
                }
                conversationViewModel.sendMediaMsg(
                    fileMetaDatas = fileMetaDatas,
                    replyMsgId = replyMsgId
                )
                clearQuote()

            } else {
                conversationViewModel.sendMediaMsg(fileMetaDatas)
            }

            editTextCompose.setText("")
        }
    }

    private fun sendIMMsg(){
        lifecycleScope.launch{
//            debugConfig.log(TAG, "do send message")
            var content = editTextCompose.text.toString().trim()

            if (editTextCompose.getMentions().isNotEmpty()) {
                content = MentionUtil.addMentionsToBody(content, editTextCompose.getMentions())
            }


            if (content.isNullOrEmpty()){
                return@launch
            }

            val quote = getQuote()
            if (quote != null) {
                val replyMsgId = quote.replyId
                if (replyMsgId.isEmpty()){
                    debugConfig.log(TAG, "sendReplyMediaMsg - invalid replyMsgId")
                    return@launch
                }

                if (conversationViewModel.hasLinkPreview()) {
                    conversationViewModel.linkPreviewState.value?.linkPreview?.let {
                        conversationViewModel.sendURLMsg(
                            content = content,
                            url = it.url,
                            urlTitle = it.title,
                            urlDescription = it.description,
                            urlThumbnail = it.thumbnail,
                            replyMsgId = replyMsgId
                        )
                    }
                } else {
                    conversationViewModel.sendMsg(
                        content = content,
                        replyMsgId = replyMsgId
                    )
                }

                clearQuote()
            } else {
                if (conversationViewModel.hasLinkPreview()) {
                    conversationViewModel.linkPreviewState.value?.linkPreview?.let {
                        conversationViewModel.sendURLMsg(
                            content = content,
                            url = it.url,
                            urlTitle = it.title,
                            urlDescription = it.description,
                            urlThumbnail = it.thumbnail,
                            replyMsgId = null
                        )
                    }
                } else {
                    conversationViewModel.sendMsg(content)
                }
            }

            editTextCompose.setText("")
        }
    }

    private fun sendSeenStateMsg(){
        lifecycleScope.launch{
//            debugConfig.log(TAG, "do send seenstate message")
            conversationViewModel.sendSeenStateMsg()
        }
    }


    private fun sendForwardMsg(threadIds: List<String>, msgIds: List<String>){
        lifecycleScope.launch{
//            debugConfig.log(TAG, "do send Forward message - ${threadIds} - ${msgIds}")


            val listThreadSendSuccess = conversationViewModel.sendForwardMsg(
                threadIds = threadIds,
                msgIds = msgIds,
            )

            if (listThreadSendSuccess.isNotEmpty()){
                for (threadId in listThreadSendSuccess){
                    conversationViewModel.resetThreadLastViewPosByThreadId(threadId)
                }

                if (listThreadSendSuccess.size == 1){
//                    debugConfig.log(TAG, "do send Forward message - finishAndSendBackData")
                    val gotoThreadId = listThreadSendSuccess.firstOrNull()
                    finishAndSendBackData(gotoThreadId)
                }
            }


        }
    }


    private fun fetchOldMsgOfThread(){
        lifecycleScope.launch{
//            debugConfig.log(TAG, "do fetchOldMsgOfThread")
            conversationViewModel.fetchOldMsgOfThread()
        }
    }

    private fun updateThreadLastPosMsgId(){
        lifecycleScope.launch{
            val pos = currentViewPosition()
//            debugConfig.log(TAG, "do updateThreadLastPosMsgId - pos: ${pos}")
            if (pos < conversationAdapter.currentList.size && pos >= 0){
                conversationViewModel.updateThreadLastViewPos(pos)
            }

        }
    }

    private fun updateMsgRecieveSeen(startUnreadSignalMsgExt: SignalMsgExt){
        lifecycleScope.launch{
            conversationViewModel.updateMsgRecieveSeen(startUnreadSignalMsgExt.serverDate)
        }
    }

    private fun checkAndUpdateMsgRecieveSeen(){
//        debugConfig.log(TAG,"checkAndUpdateMsgRecieveSeen - adapter ${conversationAdapter.currentList.size}")

        if (conversationViewModel.unreadMsgCount > 0){
            val startUnreadPosition = conversationViewModel.startUnreadPosition
            startUnreadPosition?.let {
                val currentViewPosition = currentViewPosition()
//                debugConfig.log(TAG,"checkAndUpdateMsgRecieveSeen - currentViewPosition ${currentViewPosition}")
//                debugConfig.log(TAG,"startUnreadPosition ${startUnreadPosition}")
                if (currentViewPosition <= startUnreadPosition &&
                    currentViewPosition >= 0  &&
                    currentViewPosition < conversationAdapter.currentList.size){
                    val signalMsgExt = conversationAdapter.currentList[currentViewPosition]
//                    debugConfig.log(TAG,"serverdate ${signalMsgExt.serverDate}")
                    updateMsgRecieveSeen(signalMsgExt)
                }
            }
        }
    }

    fun finishAndSendBackData(gotoThreadId: String?) {
        gotoThreadId?.let {
            val resultsData = Intent().apply {
                putExtra(GO_TO_THREADID, gotoThreadId)
            }
            requireActivity().setResult(RESULT_OK, resultsData)
        }


        requireActivity().finish()
    }

    private fun initializeActionBar() {
        toolbar.setNavigationOnClickListener { v ->
            //on back
            finishAndSendBackData(null)
        }
        toolbar.overflowIcon?.colorFilter = BlendModeColorFilterCompat.createBlendModeColorFilterCompat(
            Color.WHITE, BlendModeCompat.SRC_ATOP)

        toolbar.conversationToolbar.setOnClickListener {
            findNavController().navigate(R.id.action_conversationFragment_to_conversationSettingsFragment)
        }

        toolbar.inflateMenu(R.menu.conversation_menu)
        toolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.action_mute -> {
                    // do mute
//                    val vibrator = ServiceUtil.getVibrator(requireContext())
//                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                        vibrator.vibrate(VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE))
//                    } else {
//                        vibrator.vibrate(20)
//                    }
//
//                    requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
//                    requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LOCKED
//
//                    voiceNoteMediaController.pausePlayback()
//                    audioRecorder.startRecording()
                    true
                }
                R.id.action_media -> {
                    // navigate media list
//                    val vibrator = ServiceUtil.getVibrator(requireContext())
//                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                        vibrator.vibrate(VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE))
//                    } else {
//                        vibrator.vibrate(20)
//                    }
//
//                    requireActivity().window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
//                    requireActivity().requestedOrientation =
//                        ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
//
//                    val future: ListenableFuture<VoiceNoteDraft> = audioRecorder.stopRecording()
//                    future.addListener(object : ListenableFuture.Listener<VoiceNoteDraft?> {
//                        override fun onSuccess(result: VoiceNoteDraft?) {
//                            debugConfig.log(TAG, "record success uri ${result?.uri} - ${result?.size}")
//                            result?.uri?.let {
//                                val mp = MediaPlayer()
//                                mp.setDataSource(it.path)
//                                mp.prepare()
//                                mp.start()
//                            }
//
//                        }
//
//                        override fun onFailure(e: ExecutionException?) {
//                            Toast.makeText(requireContext(), "Unable to record", Toast.LENGTH_LONG).show()
//                        }
//                    })
                    true
                }
                R.id.action_settings -> {
                    true
                }
                else -> true
            }}
    }

    private fun showSpinner(visibility: Boolean) {
        if (visibility) {
            load_more_progress.visibility = VISIBLE
            load_more_progress.spin()
        } else {
            load_more_progress.visibility = GONE
            load_more_progress.stopSpinning()
        }
    }

    private fun getListLayoutManager(): SmoothScrollingLinearLayoutManager {
        return rvChat.layoutManager as SmoothScrollingLinearLayoutManager
    }

    fun currentViewPosition(): Int{
        return getListLayoutManager().findFirstCompletelyVisibleItemPosition()
    }

    fun scrollToBottom() {
        debugConfig.log(TAG, "scrollToBottom")
        var position = 0
        if (conversationViewModel.unreadMsgCount > 0){
            position = conversationViewModel.startUnreadPosition ?: 0
        }


        if (getListLayoutManager().findFirstVisibleItemPosition() < SCROLL_ANIMATION_THRESHOLD) {
//            debugConfig.log(TAG, "scrollToBottom: Smooth scrolling to bottom of screen.")
            getListLayoutManager().smoothScrollToPosition(requireActivity(), position,50f)
        } else {
//            debugConfig.log(TAG, "scrollToBottom: Scrolling to bottom of screen.")
            getListLayoutManager().smoothScrollToPosition(requireActivity(), position,1f)
        }
    }

    fun scrollToPos(pos: Int){
//        debugConfig.log(TAG, "scrollToPos ${pos}")
        getListLayoutManager().scrollToPositionWithOffset(pos,0)
        checkAndUpdateMsgRecieveSeen()
    }

    private fun setInlineDateDecoration(adapter: ConversationAdapter) {
        inlineDateDecoration?.let {
            rvChat.removeItemDecoration(it)
        }

        inlineDateDecoration = StickyHeaderDecoration(
            adapter,
            false,
            false,
            ConversationAdapter.HEADER_TYPE_INLINE_DATE
        )
        rvChat.addItemDecoration(inlineDateDecoration as StickyHeaderDecoration, 0)
    }

    //region On Scroll
    inner class ConversationScrollListener constructor(context: Context) : RecyclerView.OnScrollListener() {
        private var conversationDateHeader: ConversationDateHeader
        private var wasAtBottom = true
        private var lastPositionId: Long = -1

        init {
            conversationDateHeader = ConversationDateHeader(context, scroll_date_header)
        }

        override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
//            debugConfig.log(TAG, "onScrolled ${dx} - ${dy}")
            val currentlyAtBottom = !rv.canScrollVertically(1)
            val currentlyAtTop = !rv.canScrollVertically(-1)

            val currentlyAtZoomScrollHeight = isAtZoomScrollHeight()
            val positionId = getHeaderPositionId()
            if (currentlyAtBottom && !wasAtBottom) {
//                ViewUtil.fadeOut(composeDivider, 50, View.INVISIBLE)
            } else if (!currentlyAtBottom && wasAtBottom) {
//                ViewUtil.fadeIn(composeDivider, 500)
            }

            if (currentlyAtBottom) {
                ViewUtil.animateOut(scroll_to_bottom, scrollButtonOutAnimation, View.INVISIBLE)
            } else if (currentlyAtZoomScrollHeight) {
                ViewUtil.animateIn(scroll_to_bottom, scrollButtonInAnimation)
            }

            if (currentlyAtTop) {
                fetchOldMsgOfThread()
            }

            checkAndUpdateMsgRecieveSeen()



            if (positionId.toLong() != lastPositionId) {
                bindScrollHeader(conversationDateHeader, positionId)
            }
            wasAtBottom = currentlyAtBottom
            lastPositionId = positionId.toLong()
//            postMarkAsReadRequest()
        }

        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
            if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                conversationDateHeader.show()
            } else if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                conversationDateHeader.hide()
            }
        }

        private fun isAtZoomScrollHeight(): Boolean {
            return currentViewPosition() > 0
        }

        private fun getHeaderPositionId(): Int {
            return getListLayoutManager().findLastVisibleItemPosition()
        }

        private fun bindScrollHeader(headerViewHolder: ConversationAdapter.StickyHeaderViewHolder, positionId: Int) {
            if ((rvChat.adapter as ConversationAdapter).getHeaderId(positionId) != -1L) {
                (rvChat.adapter as ConversationAdapter).onBindHeaderViewHolder(
                    headerViewHolder,
                    positionId,
                    ConversationAdapter.HEADER_TYPE_POPOVER_DATE
                )
            }
        }
    }

    private fun initializeScrollButtonAnimations() {
        scrollButtonInAnimation = AnimationUtils.loadAnimation(requireContext(), R.anim.fade_scale_in)
        scrollButtonOutAnimation = AnimationUtils.loadAnimation(requireContext(), R.anim.fade_scale_out)
        scrollButtonInAnimation.duration = 100
        scrollButtonOutAnimation.duration = 50
    }

    //endregion On Scroll

    //region Date Header
    inner class ConversationDateHeader constructor(context: Context, textView: TextView) : ConversationAdapter.StickyHeaderViewHolder(textView) {
        private val animateIn: Animation
        private val animateOut: Animation
        private var pendingHide = false

        init {
            animateIn = AnimationUtils.loadAnimation(context, R.anim.slide_from_top)
            animateOut = AnimationUtils.loadAnimation(context, R.anim.slide_to_top)
            animateIn.duration = 100
            animateOut.duration = 100
        }

        fun show() {
            if (textView.text == null || textView.text.isEmpty()) { return }
            if (pendingHide) { pendingHide = false } else {
                ViewUtil.animateIn(textView, animateIn)
            }
        }

        fun hide() {
            pendingHide = true
            if (pendingHide) {
                pendingHide = false
                textView.visibility = View.GONE
            }
        }
    }
    //endregion Date Header

    //region Action Mode
    //TODO: handle action menu
    private fun handleReply(message: SignalMsgExt) {
        debugConfig.log(TAG, "handleReply")
        var content = if (message.imType == CwmSignalMsg.SIGNAL_IM_TYPE.URL.number) message.dataIMSignalURLMessage else message.contentIMMessage

        var messageReply = message
        if (message.imType == CwmSignalMsg.SIGNAL_IM_TYPE.FORWARD.number) {

            message.contentSignalForwardMsg?.let { contentSignalForwardMsg ->
                val originMsgExt = SignalMsgExt(
                    contentSignalForwardMsg = contentSignalForwardMsg,
                    msgDate = message.msgDate,
                    serverDate = message.serverDate,)

                content = if (originMsgExt.imType == CwmSignalMsg.SIGNAL_IM_TYPE.URL.number) originMsgExt.dataIMSignalURLMessage else originMsgExt.contentIMMessage
                messageReply = originMsgExt
            }
        }

        setQuote(
            GlideApp.with(this), message.msgId,
                message.from, content, messageReply)
    }
    private fun handleForward(messageIds: List<String>) {
        debugConfig.log(TAG, "handleForward")

        val fragment = MultiselectForwardBottomSheet()
        val argumentsBundle = Bundle().apply {
            putStringArrayList(MultiselectForwardBottomSheet.ARG_MESSAGES, ArrayList(messageIds))
        }
        fragment.arguments = argumentsBundle
        fragment.show(childFragmentManager, "BOTTOM")
    }
    private fun handleCopy(message: SignalMsgExt) {
        debugConfig.log(TAG, "handleCopy")
    }
    private fun handleSelected(message: SignalMsgExt) {
        debugConfig.log(TAG, "handleSelected")
        startActionMode()
        handleActionMode(message)
    }
    private fun handleInfo(message: SignalMsgExt) {
        debugConfig.log(TAG, "handleInfo")
    }
    private fun handleDelete(message: SignalMsgExt) {
        debugConfig.log(TAG, "handleDelete")
        showDeleteConfirmPopup(mutableListOf(message.msgId))
    }

    private fun updateMultiSelectState() {
        val count = conversationViewModel.selectedMessagesMutableLiveData.value?.size ?: 0
        actionMode?.let {
            it.title = requireContext().resources.getQuantityString(
                R.plurals.ConversationListFragment_s_selected,
                count,
                count
            )
        }

    }

    private fun startActionMode() {
        actionMode = (activity as AppCompatActivity?)!!.startSupportActionMode(this@ConversationFragment)
    }

    private fun endActionMode() {
        actionMode?.let {
            it.finish()
            actionMode = null
        }
    }

    fun handleActionMode(item: SignalMsgExt) {
        //action mode
        conversationViewModel.toggleMessageSelected(item.msgId)
        val currentSelectedMsgs = conversationViewModel.selectedMessagesMutableLiveData.value

        if (currentSelectedMsgs == null || currentSelectedMsgs.isEmpty()) {
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
        inflater.inflate(R.menu.conversation_action_menu, menu)
        return true
    }

    override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
        return false
    }

    override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_forward -> {
                debugConfig.log(TAG, "Action Forward")
                val currentSelectedMsgs = conversationViewModel.selectedMessagesMutableLiveData.value
                currentSelectedMsgs?.let {
                    handleForward(it.toList())
                }
                mode.finish() // Action picked, so close the CAB
                true
            }
            R.id.action_delete -> {
//                debugConfig.log(TAG, "Action Delete")
//                mode.finish() // Action picked, so close the CAB
                val currentSelectedMsgIds = conversationViewModel.selectedMessagesMutableLiveData.value
                if (!currentSelectedMsgIds.isNullOrEmpty()){
                    showDeleteConfirmPopup(currentSelectedMsgIds.toList())
                }

                true
            }
            else -> false
        }
    }

    override fun onDestroyActionMode(mode: ActionMode) {
        conversationViewModel.endSelection()
        endActionMode()
    }
    //endregion Action Mode


    //region POPUP
    fun showDeleteConfirmPopup(msgIds: List<String>){
        var tilte = getString(R.string.popup_delete_msg_tilte)
        var message = getString(R.string.popup_delete_msg_body)
        if (msgIds.size > 1){
            tilte = getString(R.string.popup_delete_multiple_msgss_tilte, msgIds.size)
            message = getString(R.string.popup_delete_multiple_msgss_body)
        }

        AlertDialog.Builder(requireContext())
            .setTitle(tilte)
            .setMessage(message)
            .setPositiveButton(getString(R.string.btn_delete)) { _, _ ->
//                debugConfig.log("DeleteConfirmPopup Delete")
                endActionMode()
                //TODO - IMPLEMENT QUERY CHECK deleteForAllMembers
                deleteMsgs(msgIds, true)
            }
            .setNegativeButton(getString(R.string.btn_cancel)) { _, _ ->
//                debugConfig.log("DeleteConfirmPopup Cancel")
            }
            .show()
    }
    //endregion

    //region API
    fun deleteMsgs(msgIds: List<String>, deleteForAllMembers: Boolean){
        lifecycleScope.launch {
            try {
                val signalThread = conversationViewModel.signalThreadExt ?: return@launch

                spinner.visibility = View.VISIBLE
                val inputMethodManager =
                    activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                inputMethodManager.hideSoftInputFromWindow(activity?.currentFocus?.windowToken, 0)


                val isGroup = signalThread.threadType == CwmSignalMsg.SIGNAL_THREAD_TYPE.GROUP.number
                val isAdmin = signalThread.admin

                var deleteForAll = deleteForAllMembers
                if (deleteForAll && isGroup && !isAdmin){
                    deleteForAll = false
                }
                val result = conversationViewModel.deleteMsgsOfThread(signalThread.threadId, msgIds, deleteForAll)
                when (result) {
                    is Result.Success<Boolean> -> { }
                    is Result.Error -> {
                        Toast.makeText(activity ,"Failed To delete messages of thread ${signalThread.threadName} - ${signalThread.phoneFull}: ${result.exception.toString()}", Toast.LENGTH_SHORT).show()
                    }
                }

                spinner.visibility = View.GONE

            } catch (e: Exception) {
                e.printStackTrace()
            }

        }
    }
    //endregion


    //region attachment
    private var resultCaptureImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            debugConfig.log(TAG, "resultCaptureImage ${result.data?.data}")
            val fileMetaData = FileUtil.getFileMetaData(requireActivity(), currentPhotoCameraUri, false)
            fileMetaData?.let {
                sendMediaMsg(listOf(fileMetaData))
            }
        }
    }
    private var resultPickMediaLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.clipData?.let {
                val fileMetaDatas = mutableListOf<FileMetaData>()
                val count = it.itemCount
                for (i in 0 until count) {
//                    debugConfig.log(TAG, "multiple select, uri: ${it.getItemAt(i).uri}")
                    val fileMetaData = FileUtil.getFileMetaData(requireActivity(), it.getItemAt(i).uri, true)
                    fileMetaData?.let{
                        fileMetaDatas.add(it)
                    }
                }
                sendMediaMsg(fileMetaDatas)
            }
        }
    }
    private var resultPickFileLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.data?.let {
//                debugConfig.log(TAG, "result uri ${it}")
                val fileMetaData = FileUtil.getFileMetaData(requireActivity(), it, false)
                fileMetaData?.let {
//                    debugConfig.log(TAG,"fileMetaData ${fileMetaData.toString()}")
                    sendMediaMsg(listOf(fileMetaData))
                }
            }
        }
    }

    override fun onAttachmentSelectorClicked(button: AttachmentKeyboardButton) {
        when (button) {
            AttachmentKeyboardButton.CAMERA -> {
                val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)

                val fileName = "camera_" + System.currentTimeMillis()
                val file = File.createTempFile(
                    fileName,
                    ".jpg",
                    requireContext().filesDir
                ).apply {
                    // Save a file: path for use with ACTION_VIEW intents
                    currentPhotoCameraPath = absolutePath
                }
                val captureUri = FileProvider.getUriForFile(requireContext(),
                    BuildConfig.APPLICATION_ID + ".provider", file)
                currentPhotoCameraUri = captureUri
                intent.putExtra(MediaStore.EXTRA_OUTPUT, captureUri)
                resultCaptureImageLauncher.launch(intent)

//                Permissions.with(requireActivity())
//                    .request(Manifest.permission.CAMERA)
//                    .ifNecessary()
//                    .withRationaleDialog(
//                        getString(R.string.ConversationActivity_to_capture_photos_and_video_allow_application_access_to_the_camera),
//                        R.drawable.ic_camera_24
//                    )
//                    .withPermanentDenialDialog(getString(R.string.ConversationActivity_application_needs_the_camera_permission_to_take_photos_or_video))
//                    .onAllGranted {
//                        debugConfig.log("Camera granted")
//                    }
//                    .onAnyDenied {
//                        Toast.makeText(requireContext(), R.string.ConversationActivity_application_needs_camera_permissions_to_take_photos_or_video, Toast.LENGTH_LONG).show()
//                    }
//                    .execute()
            }
            AttachmentKeyboardButton.GALLERY -> {
//                Permissions.with(this)
//                    .request(Manifest.permission.READ_EXTERNAL_STORAGE)
//                    .onAllGranted {
//                        debugConfig.log(TAG, "READ_EXTERNAL_STORAGE Granted!")
//                    }
//                    .withPermanentDenialDialog(getString(R.string.AttachmentManager_application_requires_the_external_storage_permission_in_order_to_attach_photos_videos_or_audio))
//                    .execute()

//                debugConfig.log(TAG, "Pick photos and videos")
                val intent = Intent(Intent.ACTION_PICK)
                intent.type = "image/* video/*"
                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                resultPickMediaLauncher.launch(Intent.createChooser(intent, "Choose Pictures"))

            }
            AttachmentKeyboardButton.FILE -> {
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
                intent.addCategory(Intent.CATEGORY_OPENABLE)
                intent.type = "*/*"
                intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                resultPickFileLauncher.launch(intent)
            }
            AttachmentKeyboardButton.CONTACT -> {
                debugConfig.log(TAG, "Pick contact")
            }
        }
    }

    override fun onAttachmentPermissionsRequested() {
        Permissions.with(this)
            .request(Manifest.permission.READ_EXTERNAL_STORAGE)
            .onAllGranted {
                debugConfig.log(TAG, "READ_EXTERNAL_STORAGE Granted!")
            }
            .withPermanentDenialDialog(getString(R.string.AttachmentManager_application_requires_the_external_storage_permission_in_order_to_attach_photos_videos_or_audio))
            .execute()
    }

    //endregion attachment

    //region media player
    private fun requireVoiceNotePlayerView(): VoiceNotePlayerView {
        var voiceNotePlayerView = voiceNotePlayerView
        if (voiceNotePlayerView == null) {
            voiceNotePlayerView = voiceNotePlayerViewStub.get().findViewById(R.id.voice_note_player_view)
            voiceNotePlayerView.listener = VoiceNotePlayerViewListener()
        }
        return voiceNotePlayerView!!
    }

    private inner class VoiceNotePlayerViewListener : VoiceNotePlayerView.Listener {
        override fun onCloseRequested(uri: Uri) {
            voiceNoteMediaController.stopPlaybackAndReset(uri)
        }

        override fun onSpeedChangeRequested(uri: Uri, speed: Float) {
            voiceNoteMediaController.setPlaybackSpeed(uri, speed)
        }

        override fun onPlay(uri: Uri, messageId: Long, position: Double) {
            voiceNoteMediaController.startSinglePlayback(uri, messageId, position)
        }

        override fun onPause(uri: Uri) {
            voiceNoteMediaController.pausePlayback(uri)
        }

        override fun onNavigateToMessage(threadId: Long, threadRecipientId: String, senderId: String, messageTimestamp: Long, messagePositionInThread: Long) {
            //TODO: jump to message position
        }
    }

    //endregion media player

    //region record
    private class SlideToCancel(val slideToCancelView: View) {

        fun display() {
            ViewUtil.fadeIn(slideToCancelView, FADE_TIME)
        }

        fun hide(): ListenableFuture<Void> {
            val future: SettableFuture<Void> = SettableFuture()
            val animation = AnimationSet(true)
            animation.addAnimation(TranslateAnimation(
                    Animation.ABSOLUTE, slideToCancelView.translationX,
                    Animation.ABSOLUTE, 0f,
                    Animation.RELATIVE_TO_SELF, 0f,
                    Animation.RELATIVE_TO_SELF, 0f)
            )
            animation.addAnimation(AlphaAnimation(1f, 0f))
            animation.duration = MicrophoneRecorderView.ANIMATION_DURATION.toLong()
            animation.fillBefore = true
            animation.fillAfter = false
            slideToCancelView.postDelayed({ future.set(null) }, MicrophoneRecorderView.ANIMATION_DURATION.toLong())
            slideToCancelView.visibility = GONE
            slideToCancelView.startAnimation(animation)
            return future
        }

        fun moveTo(offset: Float) {
            val animation: Animation = TranslateAnimation(
                Animation.ABSOLUTE, offset,
                Animation.ABSOLUTE, offset,
                Animation.RELATIVE_TO_SELF, 0f,
                Animation.RELATIVE_TO_SELF, 0f
            )
            animation.duration = 0
            animation.fillAfter = true
            animation.fillBefore = true
            slideToCancelView.startAnimation(animation)
        }

    }

    private class RecordTime constructor(private val recordTimeView: TextView, private val microphone: View,
        private val limitSeconds: Long, private val onLimitHit: Runnable) : Runnable {

        private var startTime: Long = 0

        companion object {
            private fun pulseAnimation(): Animation {
                val animation = AlphaAnimation(0f, 1f)
                animation.interpolator = pulseInterpolator()
                animation.repeatCount = Animation.INFINITE
                animation.duration = 1000
                return animation
            }

            private fun pulseInterpolator(): Interpolator {
                return Interpolator { inp: Float ->
                    var input = inp*5f
                    if (input > 1) {
                        input = 4 - input
                    }
                    max(0f, min(1f, input))
                }
            }
        }

        @MainThread
        fun display() {
            startTime = System.currentTimeMillis()
            recordTimeView.text = DateUtils.formatElapsedTime(0)
            ViewUtil.fadeIn(recordTimeView, FADE_TIME)
            ThreadUtil.runOnMainDelayed(this, TimeUnit.SECONDS.toMillis(1))
            microphone.visibility = VISIBLE
            microphone.startAnimation(pulseAnimation())
        }

        @MainThread
        fun hide(): Long {
            val elapsedTime = System.currentTimeMillis() - startTime
            startTime = 0
            ViewUtil.fadeOut(recordTimeView, FADE_TIME, View.INVISIBLE)
            microphone.clearAnimation()
            ViewUtil.fadeOut(microphone, FADE_TIME, View.INVISIBLE)
            return elapsedTime
        }

        @MainThread
        override fun run() {
            val localStartTime = startTime
            if (localStartTime > 0) {
                val elapsedTime = System.currentTimeMillis() - localStartTime
                val elapsedSeconds = TimeUnit.MILLISECONDS.toSeconds(elapsedTime)
                if (elapsedSeconds >= limitSeconds) {
                    onLimitHit.run()
                } else {
                    recordTimeView.text = DateUtils.formatElapsedTime(elapsedSeconds)
                    ThreadUtil.runOnMainDelayed(this, TimeUnit.SECONDS.toMillis(1))
                }
            }
        }
    }

    private fun fadeInNormalComposeViews() {
        fadeIn(viewInputPanel.layoutCompose)
    }

    private fun fadeIn(v: View) {
        v.animate().alpha(1f).setDuration(FADE_TIME.toLong()).start()
    }

    private fun fadeOut(v: View) {
        v.animate().alpha(0f).setDuration(FADE_TIME.toLong()).start()
    }
    override fun onRecordPressed() {
        recordTime.display()
        slideToCancel.display()

        fadeOut(viewInputPanel.layoutCompose)

        onRecorderStarted()
    }

    override fun onRecordReleased() {
        val elapsedTime: Long = onRecordHideEvent()
        debugConfig.log(TAG, "Elapsed time: $elapsedTime")
        if (elapsedTime > 1000) {
            onRecorderFinished()
        } else {
            Toast.makeText(context, R.string.InputPanel_tap_and_hold_to_record_a_voice_message_release_to_send, Toast.LENGTH_LONG).show()
            onRecorderCanceled()
        }
    }

    override fun onRecordCanceled() {
        onRecordHideEvent()

        onRecorderCanceled()
    }

    override fun onRecordLocked() {
        slideToCancel.hide()
        viewInputPanel.record_cancel.visibility = VISIBLE

        onRecorderLocked()
    }

    override fun onRecordMoved(offsetX: Float, absoluteX: Float) {
        slideToCancel.moveTo(offsetX)

        val position: Float = absoluteX / viewInputPanel.recording_container.width

        if (ViewUtil.isLtr(viewCompose) && position <= 0.5 || ViewUtil.isRtl(viewCompose) && position >= 0.6) {
            viewInputPanel.recorder_view.cancelAction()
        }
    }

    override fun onRecordPermissionRequired() {
        debugConfig.log(TAG, "onRecordPermissionRequired")
        Permissions.with(this)
            .request(Manifest.permission.RECORD_AUDIO)
            .ifNecessary()
            .withRationaleDialog(getString(R.string.ConversationActivity_to_send_audio_messages_allow_app_access_to_your_microphone),
                R.drawable.ic_mic_solid_24)
            .withPermanentDenialDialog(getString(R.string.ConversationActivity_app_requires_the_microphone_permission_in_order_to_send_audio_messages))
            .execute()
    }

    private fun onRecordHideEvent(): Long {
        viewInputPanel.record_cancel.visibility = GONE
        val future = slideToCancel.hide()
        val elapsedTime = recordTime.hide()

        future.addListener(object : AssertedSuccessListener<Void?>() {
            override fun onSuccess(result: Void?) {
                fadeInNormalComposeViews()
            }
        })
        return elapsedTime
    }

    private fun onRecorderStarted() {
        val vibrator = ServiceUtil.getVibrator(requireContext())
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            vibrator.vibrate(20)
        }

        requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LOCKED

        voiceNoteMediaController.pausePlayback()
        audioRecorder.startRecording()
    }

    private fun onRecorderLocked() {
        voiceRecorderWakeLock.acquire()
//        updateToggleButtonState()
        requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }

    private fun onRecorderFinished() {
        voiceRecorderWakeLock.release()
//        updateToggleButtonState()
        val vibrator = ServiceUtil.getVibrator(requireContext())
        vibrator.vibrate(20)
        requireActivity().window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        val future = audioRecorder.stopRecording()

        future.addListener(object : ListenableFuture.Listener<VoiceNoteDraft?> {
            override fun onSuccess(result: VoiceNoteDraft?) {
                debugConfig.log(TAG, "record success uri ${result?.uri} - ${result?.size}")
                result?.uri?.let {
                    debugConfig.log(TAG, "do send uri ${result?.uri} - ${result?.size}")

                    val fileMetaData = FileUtil.getFileMetaData(requireActivity(), it, false)
                    debugConfig.log(TAG, "get file meta ${fileMetaData}")

                    fileMetaData?.let {
                    debugConfig.log(TAG,"fileMetaData ${fileMetaData.toString()}")
                        sendMediaMsg(listOf(fileMetaData))
                    }
//                    val mp = MediaPlayer()
//                    mp.setDataSource(it.path)
//                    mp.prepare()
//                    mp.start()
                }

            }

            override fun onFailure(e: ExecutionException?) {
                Toast.makeText(requireContext(), R.string.ConversationActivity_unable_to_record_audio, Toast.LENGTH_LONG).show()
            }
        })

//        future.addListener(object : ListenableFuture.Listener<VoiceNoteDraft> {
//            override fun onSuccess(result: VoiceNoteDraft) {
////                sendVoiceNote(result.getUri(), result.getSize())
//            }
//
//            override fun onFailure(e: ExecutionException?) {
//                Toast.makeText(requireContext(), R.string.ConversationActivity_unable_to_record_audio, Toast.LENGTH_LONG).show()
//            }
//        })
    }

    private fun onRecorderCanceled() {
        voiceRecorderWakeLock.release()
//        updateToggleButtonState()
        val vibrator = ServiceUtil.getVibrator(requireContext())
        vibrator.vibrate(50)
        requireActivity().window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        val future = audioRecorder.stopRecording()
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
//            future.addListener(DeleteCanceledVoiceNoteListener())
        } else {
//            draftViewModel.setVoiceNoteDraftFuture(future)
        }
    }
    //endregion record

    //region quote
    fun setQuote(glideRequests: GlideRequests, id: String, author: String,
                 body: CharSequence?, message: SignalMsgExt) {
        quoteView.setQuote(glideRequests, id, author, false, body, message)
        val originalHeight = if (quoteView.visibility == VISIBLE) quoteView.measuredHeight else 0
        quoteView.visibility = VISIBLE

        var maxWidth = viewCompose.width

        if (quoteView.layoutParams is ViewGroup.MarginLayoutParams) {
            val layoutParams = quoteView.layoutParams as ViewGroup.MarginLayoutParams
            maxWidth -= layoutParams.leftMargin + layoutParams.rightMargin
        }
        quoteView.measure(View.MeasureSpec.makeMeasureSpec(maxWidth, View.MeasureSpec.AT_MOST), 0)
        quoteAnimator?.cancel()
        quoteAnimator = createHeightAnimator(quoteView, originalHeight, quoteView.measuredHeight, null)
        quoteAnimator?.start()

//        if (this.linkPreview.getVisibility() === VISIBLE) {
//            val cornerRadius: Int = readDimen(R.dimen.message_corner_collapse_radius)
//            this.linkPreview.setCorners(cornerRadius, cornerRadius)
//        }
    }

    fun clearQuote() {
        quoteAnimator?.cancel()
        quoteAnimator = createHeightAnimator(quoteView, quoteView.measuredHeight, 0,
            object : AnimationCompleteListener() {
                override fun onAnimationEnd(animation: Animator) {
                    quoteView.dismiss()
                }
            })
        quoteAnimator?.start()
    }

    private fun createHeightAnimator(view: View, originalHeight: Int, finalHeight: Int, onAnimationComplete: AnimationCompleteListener?): ValueAnimator {
        val animator = ValueAnimator.ofInt(originalHeight, finalHeight)
            .setDuration(QUOTE_REVEAL_DURATION_MILLIS)
        animator.addUpdateListener { animation: ValueAnimator ->
            val params = view.layoutParams
            params.height = animation.animatedValue as Int
            view.layoutParams = params
        }
        if (onAnimationComplete != null) {
            animator.addListener(onAnimationComplete)
        }
        return animator
    }

    fun getQuote(): QuoteModel? {
        return if (quoteView.quoteId.isNotEmpty() && quoteView.visibility == VISIBLE) {
            QuoteModel(
                quoteView.quoteId,
                quoteView.author,
                quoteView.body,
                false,
                quoteView.attachments
            )
        } else {
            null
        }
    }
    //endregion quote

    //region link preview
    private fun setLinkPreviewLoading() {
        viewCompose.link_preview.visibility = VISIBLE
        viewCompose.link_preview.setLoading()
    }

    private fun setLinkPreviewNoPreview(customError: LinkPreviewRepository.Error?) {
        viewCompose.link_preview.visibility = VISIBLE
        viewCompose.link_preview.setNoPreview(customError)
    }

    private fun setLinkPreview(glideRequests: GlideRequests, preview: LinkPreview?) {
        if (preview != null) {
            viewCompose.link_preview.visibility = VISIBLE
            viewCompose.link_preview.setLinkPreview(glideRequests, preview, true)
        } else {
            viewCompose.link_preview.visibility = GONE
        }
        val cornerRadius: Int =
            if (quoteView.visibility == VISIBLE) resources.getDimensionPixelSize(R.dimen.message_corner_collapse_radius)
            else resources.getDimensionPixelSize(R.dimen.message_corner_radius)
        viewCompose.link_preview.setCorners(cornerRadius, cornerRadius)
    }
    //endregion link preview
}