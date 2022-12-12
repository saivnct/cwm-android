package com.lgt.cwm.activity.home.fragments.call

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.viewModels
import com.lgt.cwm.R
import com.lgt.cwm.activity.home.fragments.call.adapter.CallLogAdapter
import com.lgt.cwm.activity.home.fragments.call.models.CallLog
import com.lgt.cwm.databinding.FragmentCallBinding
import com.lgt.cwm.util.DebugConfig
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class CallFragment : Fragment() {
    private val TAG = CallFragment::class.simpleName.toString()

    @Inject lateinit var debugConfig: DebugConfig

    @Inject
    lateinit var callLogAdapter: CallLogAdapter

    private val callViewModel: CallViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val binding: FragmentCallBinding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_call, container, false);
        binding.lifecycleOwner = viewLifecycleOwner
        binding.callViewModel = callViewModel
        binding.callLogAdapter = callLogAdapter

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
//        debugConfig.log(TAG, "onViewCreated")

        val callLogs = arrayListOf<CallLog>().apply {
            add(CallLog("32sa21ea", "as32dff", "Sony Pictures", "+13072205075", 1))
            add(CallLog("32sa21ea", "as32dff", "Sony Game", "+13072205075", 1))
            add(CallLog("32sa21ea", "as32dff", "Bob Bob Nob", "+13072205075", 1))
            add(CallLog("32sa21ea", "as32dff", "Ave Bob Nob", "+13072205075", 1))
            add(CallLog("32sa21ea", "as32dff", "Tamika Hebert", "+13072205075", 1))
            add(CallLog("32sa21ea", "as32dff", "Tamika Hebert Jr", "+13072205075", 1))
            add(CallLog("32sa21ea", "as32dff", "Tayla Harper", "+13072205075", 1))
            add(CallLog("32sa21ea", "as32dff", "Tom Mcfarland Kent", "+13072205075", 1))
            add(CallLog("32sa21ea", "as32dff", "Lillie-May Manning", "+13072205075", 1))
            add(CallLog("32sa21ea", "as32dff", "Radhika Pickett", "+13072205075", 1))
            add(CallLog("32sa21ea", "as32dff", "Roscoe Nicholson", "+13072205075", 1))
            add(CallLog("32sa21ea", "as32dff", "Kya Mccabe", "+13072205075", 1))
        }

        callLogAdapter.setItems(callLogs)
        callLogAdapter.setOnItemClickListener(object :
            CallLogAdapter.OnItemClickListener {
            override fun onItemActiveClick(item: CallLog, position: Int) {
//                debugConfig.log(TAG, "onItemActiveClick!!!!")
            }
        })

    }
}