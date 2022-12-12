package com.lgt.cwm.activity.conversation

import android.os.Bundle
import android.view.MotionEvent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.findNavController
import com.lgt.cwm.R
import com.lgt.cwm.activity.conversation.fragments.ConversationFragment
import com.lgt.cwm.business.notification.NotificationHandler
import com.lgt.cwm.databinding.ActivityConversationBinding
import com.lgt.cwm.db.entity.Contact
import com.lgt.cwm.models.SignalThreadExt
import com.lgt.cwm.models.ThreadParticipantInfo
import com.lgt.cwm.util.DebugConfig
import cwmSignalMsgPb.CwmSignalMsg
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ConversationActivity : AppCompatActivity() {
    private val TAG = ConversationActivity::class.simpleName.toString()

    private val conversationActivityViewModel: ConversationActivityViewModel by viewModels()

    private var fragment: ConversationFragment? = null

    companion object {
        const val EXTRA_THREAD_ID = "ThreadId"
        const val EXTRA_CONTACT_ID = "ContactId"
    }

    @Inject lateinit var debugConfig: DebugConfig
    @Inject lateinit var notificationHandler: NotificationHandler

    lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding: ActivityConversationBinding = DataBindingUtil.setContentView(this, R.layout.activity_conversation)
        binding.lifecycleOwner = this

        navController = findNavController(R.id.nav_host_fragment_conversation)

        navController.addOnDestinationChangedListener { _, destination, _ ->
//            debugConfig.log(TAG, "addOnDestinationChangedListener ${destination.label}")
            when (destination.id) {
                R.id.conversationFragment -> {

                }

                else -> {

                }
            }
        }

        val navFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_conversation)
        fragment = navFragment!!.childFragmentManager.primaryNavigationFragment as ConversationFragment?
        debugConfig.log(TAG, "fragment ${fragment}")

        val threadId = intent.getStringExtra(EXTRA_THREAD_ID)
        val contactId = intent.getStringExtra(EXTRA_CONTACT_ID)
        threadId?.also { threadId ->
            findThread(threadId)
        }?: run{
            contactId?.also { contactId ->
                findOrCreateSoloThread(contactId)
            }?: run{
                //No Init Info
                finish()
            }
        }

        initObserver()
    }

    override fun onResume() {
        super.onResume()
//        debugConfig.log(TAG, "onResume")
        lifecycleScope.launch{
            notificationHandler.updateCurrentActiveThread(conversationActivityViewModel.signalThreadExt?.threadId)
        }

        conversationActivityViewModel.currentActiveAcc?.let {
            if (it.isLogin()){
                fetchAllUnreceivedMsg()
            }
        }
    }

    override fun onPause() {
        super.onPause()
//        debugConfig.log(TAG, "onPause")
        lifecycleScope.launch{
            notificationHandler.updateCurrentActiveThread(null)
        }
    }



    private fun initObserver(){
        conversationActivityViewModel.activeAcc.observe(this) { account ->
            conversationActivityViewModel.currentActiveAcc = account
        }
    }

    fun fetchAllUnreceivedMsg(){
        conversationActivityViewModel.fetchAllUnreceivedMsg()
    }

    fun findThread(threadId: String){
        lifecycleScope.launch {
            val signalThread = conversationActivityViewModel.findThreadByThreadId(threadId)
            signalThread?.also { signalThread ->
                notificationHandler.updateCurrentActiveThread(signalThread.threadId)

                var threadName = signalThread.threadName

                if (signalThread.threadType == CwmSignalMsg.SIGNAL_THREAD_TYPE.SOLO.number){
                    val contact = conversationActivityViewModel.findOneContactByPhoneFull(signalThread.phoneFull)
                    contact?.let{ contact ->
                        conversationActivityViewModel.contact = contact
                        threadName = contact.name
                    }
                }

                val cwmUsers = conversationActivityViewModel.getAllCWMUserByListPhoneFull(signalThread.participants)
                val threadParticipantInfos = cwmUsers.map { cwmUser ->
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
                val signalThreadExt = SignalThreadExt(signalThread, threadName, threadParticipantInfos)
                conversationActivityViewModel.signalThreadExt = signalThreadExt

                conversationActivityViewModel.threadReady()
            }?: run{
                //No found Thread
                debugConfig.log(TAG, "Not found thread ${threadId}")
                finish()
            }
        }
    }

    fun findOrCreateSoloThread(contactId: String){
        lifecycleScope.launch {
            val contact = conversationActivityViewModel.findContactdById(contactId)
            contact?.also { contact ->
                val signalThread = conversationActivityViewModel.findOrCreateSoloThread(contact)
                signalThread?.also { signalThread ->
                    notificationHandler.updateCurrentActiveThread(signalThread.threadId)

                    val threadName = contact.name
                    val cwmUsers = conversationActivityViewModel.getAllCWMUserByListPhoneFull(signalThread.participants)
                    val threadParticipantInfos = cwmUsers.map { cwmUser ->
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
                    val signalThreadExt = SignalThreadExt(signalThread, threadName, threadParticipantInfos)
                    conversationActivityViewModel.signalThreadExt = signalThreadExt

                    conversationActivityViewModel.contact = contact

                    conversationActivityViewModel.threadReady()
                }?: run{
                    //No found Thread
                    debugConfig.log(TAG, "Cannot create new solo thread with contact ${contact.standardizedPhoneNumber}")
                    finish()
                }
            }?: run{
                //No found Contact
                debugConfig.log(TAG, "Not found contact ${contactId}")
                finish()
            }
        }
    }



}