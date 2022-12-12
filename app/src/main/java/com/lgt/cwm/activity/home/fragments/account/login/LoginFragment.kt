package com.lgt.cwm.activity.home.fragments.account.login

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.lgt.cwm.R
import com.lgt.cwm.databinding.FragmentLoginBinding
import com.lgt.cwm.util.DebugConfig
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class LoginFragment : Fragment() {
    private val TAG = LoginFragment::class.simpleName.toString()

    @Inject
    lateinit var debugConfig: DebugConfig

    private val loginViewModel: LoginViewModel by viewModels()
    private lateinit var binding: FragmentLoginBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_login, container, false);
        binding.lifecycleOwner = viewLifecycleOwner
        binding.loginViewModel = loginViewModel

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        debugConfig.log(TAG, "onViewCreated")

        initListeners()
    }

    private fun initListeners() {
        binding.buttonLogin.setOnClickListener(buttonLoginClick)
        binding.buttonRegister.setOnClickListener(buttonRegisterClick)
    }

    //region Listeners
    val buttonLoginClick = View.OnClickListener {
        debugConfig.log(TAG, "Login Click")
        debugConfig.log(TAG, "phone ${binding.editTextUsername.text} - pass ${binding.editTextPassword.text}")
        findNavController().navigate(R.id.action_loginFragment_to_conversationListFragment)
    }

    val buttonRegisterClick = View.OnClickListener {
        debugConfig.log(TAG, "Register Click")
        findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
    }

    //endregion Listeners

}