package com.lgt.cwm.activity.home.fragments.account.register

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.lgt.cwm.R
import com.lgt.cwm.activity.home.MainActivity
import com.lgt.cwm.databinding.FragmentEditProfileBinding
import com.lgt.cwm.util.DebugConfig
import com.lgt.cwm.util.Result
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_edit_profile.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class EditProfileFragment : Fragment() {
    private val TAG = EditProfileFragment::class.simpleName.toString()

    @Inject
    lateinit var debugConfig: DebugConfig

    private val editProfileViewModel: EditProfileViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val binding: FragmentEditProfileBinding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_edit_profile, container, false);
        binding.lifecycleOwner = viewLifecycleOwner
        binding.editProfileViewModel = editProfileViewModel

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        debugConfig.log(TAG, "onViewCreated")

        initListeners()
    }

    private fun initListeners() {
        buttonSaveProfile.setOnClickListener{
            doUpdateProfile()
        }
    }

    private fun doUpdateProfile(){
        lifecycleScope.launch {
            spinner.visibility = View.VISIBLE
            try {
                val inputMethodManager =
                    activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                inputMethodManager.hideSoftInputFromWindow(activity?.currentFocus?.windowToken, 0)

                val result = editProfileViewModel.updateProfile()

                when (result) {
                    is Result.Success<grpcCWMPb.CwmRqResAccount.UpdateProfileResponse> -> {
                        Toast.makeText(activity ,"Update Profile Success!", Toast.LENGTH_SHORT).show()
                        val mainActivity = activity as? MainActivity
                        mainActivity?.checkActiveAccount()

                        findNavController().navigate(R.id.action_editProfileFragment_to_conversationListFragment)
                    }
                    is Result.Error -> {
                        Toast.makeText(activity ,"Update Profile Failed: ${result.exception.toString()}", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            spinner.visibility = View.GONE
        }
    }

}