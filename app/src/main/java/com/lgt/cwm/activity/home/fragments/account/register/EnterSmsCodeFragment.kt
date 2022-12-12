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
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.lgt.cwm.R
import com.lgt.cwm.activity.home.MainActivity
import com.lgt.cwm.databinding.FragmentEnterSmsCodeBinding
import com.lgt.cwm.util.DebugConfig
import com.lgt.cwm.util.Result
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_enter_sms_code.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class EnterSmsCodeFragment : Fragment() {
    private val TAG = EnterSmsCodeFragment::class.simpleName.toString()

    @Inject
    lateinit var debugConfig: DebugConfig

    private val enterSmsCodeViewModel: EnterSmsCodeViewModel by viewModels()

    val args: EnterSmsCodeFragmentArgs by navArgs()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val binding: FragmentEnterSmsCodeBinding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_enter_sms_code, container, false);
        binding.lifecycleOwner = viewLifecycleOwner
        binding.enterSmsCodeViewModel = enterSmsCodeViewModel

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
//        debugConfig.log(TAG, "onViewCreated")

        initListeners()

        initObserver()

        enterSmsCodeViewModel.initData(
            phoneFull = args.phoneFull,
            countryCode = args.countryCode,
            phone = args.phone,
            codeTTL = args.codeTTL
        )
        enterSmsCodeViewModel.startCounter(args.codeTTL)
    }

    private fun initListeners() {
        buttonResendCode.setOnClickListener{
            doVerifyAuthenCode()

        }
    }

    private fun initObserver(){
        enterSmsCodeViewModel._authencodeTimeout.observe(viewLifecycleOwner, Observer { isTimeOut ->
            if (isTimeOut){
                findNavController().navigateUp();
            }
        })

        enterSmsCodeViewModel.activeAccLiveData.observe(viewLifecycleOwner, Observer { account ->
            account?.let {
                if (it.isLogin()){
//                    debugConfig.log(TAG,"Login Success!")
                    spinner.visibility = View.GONE
                    if (it.firstName.isNullOrEmpty()){
                        findNavController().navigate(R.id.action_enterSmsCodeFragment_to_editProfileFragment)
                    }else{
                        //already has first name -> go to main
                        val mainActivity = activity as? MainActivity
                        mainActivity?.checkActiveAccount()
                        findNavController().navigate(R.id.action_enterSmsCodeFragment_to_conversationListFragment)
                    }
                }else{
                    spinner.visibility = View.VISIBLE
                }
            }
        })
    }

    private fun doVerifyAuthenCode(){
        lifecycleScope.launch {
            try {
                spinner.visibility = View.VISIBLE
                val inputMethodManager =
                    activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                inputMethodManager.hideSoftInputFromWindow(activity?.currentFocus?.windowToken, 0)

                val result = enterSmsCodeViewModel.verifyAuthenCode()
                spinner.visibility = View.GONE
                when (result) {
                    is Result.Success<grpcCWMPb.CwmRqResAccount.VerifyAuthencodeResponse> -> {
                        Toast.makeText(activity ,"Verify AuthenCode Success!", Toast.LENGTH_SHORT).show()
                    }
                    is Result.Error -> {
                        Toast.makeText(activity ,"Verify AuthenCode Failed: ${result.exception.toString()}", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

        }

    }
}