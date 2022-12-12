package com.lgt.cwm.activity.home.fragments.splash

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.lgt.cwm.R
import com.lgt.cwm.databinding.FragmentSplashBinding
import com.lgt.cwm.util.DebugConfig
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SplashFragment : Fragment() {
    private val TAG = SplashFragment::class.simpleName.toString()

    @Inject
    lateinit var debugConfig: DebugConfig

    private val splashViewModel: SplashViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val binding: FragmentSplashBinding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_splash, container, false);
        binding.lifecycleOwner = viewLifecycleOwner
        binding.splashViewModel = splashViewModel

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
//        debugConfig.log(TAG, "onViewCreated")
        checkActiveAccount()
    }

    fun checkActiveAccount(){
        lifecycleScope.launch {
            val account = splashViewModel.getActiveAccount()
            account?.also {
                if (account.firstName.isNullOrEmpty()){
                    //go to editProfile
                    findNavController().navigate(R.id.editProfileFragment)
                }else{
                    //go to home
                    findNavController().navigate(R.id.action_splashFragment_to_conversationListFragment)
                }
            }?: run{
                //go to register (test)
                findNavController().navigate(R.id.registerFragment)
            }
        }
    }

}