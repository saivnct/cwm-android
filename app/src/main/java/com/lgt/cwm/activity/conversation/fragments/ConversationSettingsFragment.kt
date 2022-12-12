package com.lgt.cwm.activity.conversation.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.doOnPreDraw
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.lgt.cwm.R
import com.lgt.cwm.activity.conversation.fragments.adapter.ConversationSettingsAdapter
import com.lgt.cwm.activity.conversation.fragments.models.ConversationSettingsConfiguration
import com.lgt.cwm.activity.conversation.fragments.models.configure
import com.lgt.cwm.databinding.FragmentConversationSettingsBinding
import com.lgt.cwm.ui.setting.ConversationSettingsIcon
import com.lgt.cwm.ui.setting.ConversationSettingsText
import com.lgt.cwm.util.DebugConfig
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject


@AndroidEntryPoint
class ConversationSettingsFragment : Fragment() {
    private val TAG = ConversationSettingsFragment::class.simpleName.toString()

    @Inject
    lateinit var debugConfig: DebugConfig

    private val conversationSettingsViewModel: ConversationSettingsViewModel by viewModels()

    lateinit var conversationSettingsAdapter: ConversationSettingsAdapter

    private lateinit var recyclerView: RecyclerView
//    private lateinit var callback: Callback

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val binding: FragmentConversationSettingsBinding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_conversation_settings, container, false);
        binding.lifecycleOwner = viewLifecycleOwner
        binding.conversationSettingsViewModel = conversationSettingsViewModel

        binding.toolbar.setNavigationOnClickListener { v ->
            findNavController().popBackStack()
        }

        recyclerView = binding.recycler

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        debugConfig.log(TAG, "onViewCreated")

        conversationSettingsAdapter = ConversationSettingsAdapter()

        recyclerView.adapter = conversationSettingsAdapter

        bindAdapter(conversationSettingsAdapter)
    }

    private fun bindAdapter(adapter: ConversationSettingsAdapter) {
        adapter.submitList(getConfiguration().toMappingModelList()) {
            (view?.parent as? ViewGroup)?.doOnPreDraw {
//                callback.onContentWillRender()
            }
        }
    }

    private fun getConfiguration(): ConversationSettingsConfiguration {
        return configure {
            clickPref(
                title = ConversationSettingsText.from(R.string.CommunicationActions_no_browser_found),
                icon = ConversationSettingsIcon.from(R.drawable.ic_attach_file),
                onClick = {

                }
            )
            dividerPref()
            clickPref(
                title = ConversationSettingsText.from(R.string.conversation_item_sent__message_read),
                icon = ConversationSettingsIcon.from(R.drawable.ic_drafts_24),
                onClick = {

                }
            )
            sectionHeaderPref(R.string.CommunicationActions_send_email)
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
//        callback = context as Callback
//        val callback = object : OnBackPressedCallback(
//            true // default to enabled
//        ) {
//            override fun handleOnBackPressed() {
//                debugConfig.log(TAG, "handleOnBackPressed")
//            }
//        }
//
//        requireActivity().onBackPressedDispatcher.addCallback(
//            this, // LifecycleOwner
//            callback);

    }

    interface Callback {
        fun onContentWillRender()
    }
}