package com.lgt.cwm.activity.home.fragments.setting

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.doOnPreDraw
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.RecyclerView
import com.lgt.cwm.R
import com.lgt.cwm.activity.conversation.fragments.adapter.ConversationSettingsAdapter
import com.lgt.cwm.activity.conversation.fragments.models.ConversationSettingsConfiguration
import com.lgt.cwm.activity.conversation.fragments.models.configure
import com.lgt.cwm.databinding.FragmentSettingBinding
import com.lgt.cwm.ui.setting.ConversationSettingsIcon
import com.lgt.cwm.ui.setting.ConversationSettingsText
import com.lgt.cwm.util.DebugConfig
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SettingFragment : Fragment() {
    private val TAG = SettingFragment::class.simpleName.toString()

    @Inject
    lateinit var debugConfig: DebugConfig

    private val settingViewModel: SettingViewModel by viewModels()

    private lateinit var recyclerView: RecyclerView
    lateinit var settingsAdapter: ConversationSettingsAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val binding: FragmentSettingBinding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_setting, container, false);
        binding.lifecycleOwner = viewLifecycleOwner
        binding.settingViewModel = settingViewModel

        recyclerView = binding.recycler

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        settingsAdapter = ConversationSettingsAdapter()

        recyclerView.adapter = settingsAdapter
        bindAdapter(settingsAdapter)
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
                title = ConversationSettingsText.from(R.string.AppSettingsFragment__account),
                icon = ConversationSettingsIcon.from(R.drawable.ic_account_24),
                onClick = {

                }
            )
            clickPref(
                title = ConversationSettingsText.from(R.string.AppSettingsFragment__linked_devices),
                icon = ConversationSettingsIcon.from(R.drawable.ic_linked_devices_24),
                onClick = {

                }
            )
            clickPref(
                title = ConversationSettingsText.from(R.string.AppSettingsFragment__appearance),
                icon = ConversationSettingsIcon.from(R.drawable.ic_appearance_24),
                onClick = {

                }
            )
            clickPref(
                title = ConversationSettingsText.from(R.string.AppSettingsFragment__chat_settings),
                icon = ConversationSettingsIcon.from(R.drawable.ic_chat_bubble_24),
                onClick = {

                }
            )
            clickPref(
                title = ConversationSettingsText.from(R.string.AppSettingsFragment__notifications),
                icon = ConversationSettingsIcon.from(R.drawable.ic_outline_notifications_24),
                onClick = {

                }
            )
            clickPref(
                title = ConversationSettingsText.from(R.string.AppSettingsFragment__privacy_and_security),
                icon = ConversationSettingsIcon.from(R.drawable.ic_lock_24),
                onClick = {

                }
            )
            clickPref(
                title = ConversationSettingsText.from(R.string.AppSettingsFragment__data_and_storage),
                icon = ConversationSettingsIcon.from(R.drawable.ic_outline_archive_24),
                onClick = {

                }
            )

            dividerPref()

            clickPref(
                title = ConversationSettingsText.from(R.string.AppSettingsFragment__help),
                icon = ConversationSettingsIcon.from(R.drawable.ic_support_24),
                onClick = {

                }
            )
            clickPref(
                title = ConversationSettingsText.from(R.string.AppSettingsFragment__invite_your_friends),
                icon = ConversationSettingsIcon.from(R.drawable.ic_mail_24),
                onClick = {

                }
            )
        }
    }
}