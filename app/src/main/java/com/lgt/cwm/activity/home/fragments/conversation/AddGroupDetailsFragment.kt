package com.lgt.cwm.activity.home.fragments.conversation

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.lgt.cwm.R
import com.lgt.cwm.activity.conversation.ConversationActivity
import com.lgt.cwm.databinding.FragmentAddGroupDetailsBinding
import com.lgt.cwm.util.DebugConfig
import com.lgt.cwm.util.Result
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_add_group_details.*
import kotlinx.android.synthetic.main.fragment_add_group_details.spinner
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AddGroupDetailsFragment : Fragment() {
    private val TAG = AddGroupDetailsFragment::class.simpleName.toString()

    @Inject
    lateinit var debugConfig: DebugConfig

    private val addGroupDetailsViewModel: AddGroupDetailsViewModel by viewModels()

    val args: AddGroupDetailsFragmentArgs by navArgs()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val binding: FragmentAddGroupDetailsBinding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_add_group_details, container, false);
        binding.lifecycleOwner = viewLifecycleOwner
        binding.addGroupDetailsViewModel = addGroupDetailsViewModel

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        addGroupDetailsViewModel.listPhoneFull = args.listPhoneFull.toList()

        if (addGroupDetailsViewModel.listPhoneFull.isNullOrEmpty()){
            findNavController().navigateUp();
        }

        requireActivity().actionBar?.let {
            it.title = "Name this group"
        }

        buttonCreateGroup.setOnClickListener {
            createGroupThread(editTextGroupName.text.toString())
        }
    }

    private fun createGroupThread(groupName: String){
        debugConfig.log(TAG, "create new group")

        lifecycleScope.launch {
            try {
                spinner.visibility = View.VISIBLE
                val inputMethodManager =
                    activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                inputMethodManager.hideSoftInputFromWindow(activity?.currentFocus?.windowToken, 0)

                val result = addGroupDetailsViewModel.createNewGroupThread(groupName)
                spinner.visibility = View.GONE
                when (result) {
                    is Result.Success<grpcCWMPb.CwmRqResThread.CreateGroupThreadResponse> -> {
                        val respone = result.data
                        Toast.makeText(activity ,"Create group thread Success!", Toast.LENGTH_SHORT).show()
                        goToChatActivity(respone.groupThreadInfo.threadId)
                    }
                    is Result.Error -> {
                        Toast.makeText(activity ,"Create group thread Failed: ${result.exception.toString()}", Toast.LENGTH_SHORT).show()
                        backToHome()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

        }
    }

    var resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        backToHome()
    }

    private fun goToChatActivity(threadId: String){
        val intent = Intent(activity, ConversationActivity::class.java).apply {
            putExtra(ConversationActivity.EXTRA_THREAD_ID, threadId)
        }

        resultLauncher.launch(intent)
        requireActivity().overridePendingTransition(R.anim.slide_from_end, R.anim.fade_scale_out);
    }

    private fun backToHome(){
        findNavController().navigate(R.id.action_addGroupDetailsFragment_to_conversationListFragment, null,
            NavOptions.Builder().setPopUpTo(R.id.conversationListFragment, true).build())
    }
}