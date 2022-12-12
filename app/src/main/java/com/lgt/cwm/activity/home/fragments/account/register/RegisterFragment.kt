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
import com.lgt.cwm.databinding.FragmentRegisterBinding
import com.lgt.cwm.util.DebugConfig
import com.lgt.cwm.util.Result
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_register.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class RegisterFragment : Fragment() {
    private val TAG = RegisterFragment::class.simpleName.toString()

    companion object {
        const val KEY_PHONE_FULL = "phone_full"
        const val KEY_CODE_TTL = "code_ttl"
    }

    @Inject
    lateinit var debugConfig: DebugConfig

    private val registerViewModel: RegisterViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val binding : FragmentRegisterBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_register, container, false);
        binding.lifecycleOwner = viewLifecycleOwner
        binding.registerViewModel = registerViewModel

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
//        debugConfig.log(TAG, "onViewCreated")
        initListeners()

        parentFragmentManager.setFragmentResultListener(CountryPickerFragment.REQUEST_COUNTRY_SELECT, this) { _, bundle ->
            val country = bundle.getString(CountryPickerFragment.KEY_COUNTRY)
            val countryCode = bundle.getString(CountryPickerFragment.KEY_COUNTRY_CODE)
            textViewDropdown.setText(country)
            registerViewModel.setCountryCode(countryCode ?: "")
        }

    }

    private fun initListeners() {
        buttonDoRegister.setOnClickListener {
//            debugConfig.log(TAG, "Do Register Click with phone ${editTextPhoneNumber.text}")
            doRegisterAcc()
        }

        textViewDropdown.setOnClickListener{
//            debugConfig.log(TAG, "Pick country code")
            findNavController().navigate(R.id.action_registerFragment_to_countryPickerFragment)
        }
    }

    private fun doRegisterAcc(){
        lifecycleScope.launch {
            spinner.visibility = View.VISIBLE

            try {
                val inputMethodManager =
                    activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                inputMethodManager.hideSoftInputFromWindow(activity?.currentFocus?.windowToken, 0)

                val result = registerViewModel.registerAcc()

                when (result) {
                    is Result.Success<grpcCWMPb.CwmRqResAccount.CreatAccountResponse> -> {
//                        debugConfig.log(TAG, "Register Acc Success!!!")

                        val response = result.data

                        val action = RegisterFragmentDirections.actionRegisterFragmentToEnterSmsCodeFragment(
                            phone = registerViewModel._edPhone.value ?: "",
                            countryCode = registerViewModel._edCountryCode.value ?: "",
                            phoneFull = response.phoneFull,
                            codeTTL = response.authenCodeTimeOut
                        )
                        findNavController().navigate(action)


                    }
                    is Result.Error -> {
                        Toast.makeText(activity ,"Register Acc Failed: ${result.exception.toString()}", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            spinner.visibility = View.GONE
        }
    }

    //endregion Listeners
}