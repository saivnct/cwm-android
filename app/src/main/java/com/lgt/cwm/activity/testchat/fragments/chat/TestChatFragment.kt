package com.lgt.cwm.activity.testchat.fragments.chat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import com.lgt.cwm.R
import com.lgt.cwm.activity.testchat.fragments.chat.adapter.TestChatViewAdapter
import com.lgt.cwm.activity.test.fragments.dashboard.TestDashboardFragment
import com.lgt.cwm.activity.testchat.TestChatViewModel
import com.lgt.cwm.databinding.FragmentTestChatBinding
import com.lgt.cwm.util.DebugConfig
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_test_chat.*
import javax.inject.Inject

/**
 * Created by giangtpu on 7/6/22.
 */
@AndroidEntryPoint
class TestChatFragment : Fragment(){
    private val TAG = TestDashboardFragment::class.simpleName.toString()

    @Inject
    lateinit var debugConfig: DebugConfig

    @Inject
    lateinit var testChatViewAdapter: TestChatViewAdapter

    //view model will clear when destroy fragment
    // Get a reference to the ViewModel scoped to this Fragment
    private val testChatFragmentViewModel: TestChatFragmentViewModel by viewModels()

    //view model will clear when destroy activity
    // Get a reference to the ViewModel scoped to its Activity
    private val testChatViewModel: TestChatViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding: FragmentTestChatBinding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_test_chat, container, false);
        binding.lifecycleOwner = viewLifecycleOwner
        binding.testChatFragmentViewModel = testChatFragmentViewModel
        binding.testChatViewAdapter = testChatViewAdapter
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        debugConfig.log(TAG, "testChatViewModel:"+testChatFragmentViewModel.hashCode())
        debugConfig.log(TAG, "mainViewModel:"+testChatViewModel.hashCode())

        testChatViewModel.setToolBarTitle(getString(R.string.navtitle_chat))

        initListener()

        initObserve()
    }

    fun initListener(){
        btnSend.setOnClickListener {
            testChatFragmentViewModel.sendMsg()
        }
    }

    fun initObserve(){
//        testChatFragmentViewModel.wsChatMsgLiveData.observe(viewLifecycleOwner) { wsChatMsg ->
//            testChatViewAdapter.addItem(wsChatMsg)
//        }

    }
}