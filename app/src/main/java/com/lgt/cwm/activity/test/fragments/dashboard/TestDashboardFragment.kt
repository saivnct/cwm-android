package com.lgt.cwm.activity.test.fragments.dashboard

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat.getSystemService
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.lgt.cwm.R
import com.lgt.cwm.activity.test.TestMainActivity
import com.lgt.cwm.activity.test.TestMainViewModel
import com.lgt.cwm.activity.test.fragments.dashboard.adapter.TestAccsViewAdapter
import com.lgt.cwm.databinding.FragmentTestDashboardBinding
import com.lgt.cwm.db.entity.Account
import com.lgt.cwm.util.DebugConfig
import com.tbruyelle.rxpermissions3.RxPermissions
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_test_dashboard.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Created by giangtpu on 6/29/22.
 */
@AndroidEntryPoint
class TestDashboardFragment : Fragment(){
    private val TAG = TestDashboardFragment::class.simpleName.toString()

    @Inject
    lateinit var debugConfig: DebugConfig

    @Inject
    lateinit var testAccsViewAdapter: TestAccsViewAdapter

    //view model will clear when destroy fragment
    // Get a reference to the ViewModel scoped to this Fragment
    private val testDashboardViewModel: TestDashboardViewModel by viewModels()

    //view model will clear when destroy activity
    // Get a reference to the ViewModel scoped to its Activity
    private val testMainViewModel: TestMainViewModel by activityViewModels()



    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding: FragmentTestDashboardBinding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_test_dashboard, container, false);
        binding.lifecycleOwner = viewLifecycleOwner
        binding.dashboardViewModel = testDashboardViewModel
        binding.testAccsViewAdapter = testAccsViewAdapter;


        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        debugConfig.log(TAG, "testDashboardViewModel:"+testDashboardViewModel.hashCode())
        debugConfig.log(TAG, "testMainViewModel:"+testMainViewModel.hashCode())

        testMainViewModel.setToolBarTitle(getString(R.string.navtitle_dashboard))

        initListener()

        initObserve()

    }

    fun initListener(){

        btnTestHttpAPI.setOnClickListener{
            testDashboardViewModel.testHTTPAPI()
        }

        btnTestGrpcAPI.setOnClickListener {
            testDashboardViewModel.testGrpcAPI()
        }

        btnCreateUser.setOnClickListener {
            testDashboardViewModel.registerAcc()
        }


        btnVerifyAuthenCode.setOnClickListener {
            testDashboardViewModel.verifyAuthenCode()
        }


        testAccsViewAdapter.setOnItemClickListener(object : TestAccsViewAdapter.OnItemClickListener{
            override fun onItemActiveClick(item: Account, position: Int) {
                debugConfig.log(TAG, "onItemActiveClick!!!!")
                testDashboardViewModel.toggleAccountActive(item)
            }

            override fun onItemClick(item: Account, position: Int) {
                debugConfig.log(TAG, "onItemClick!!!!")
                (activity as TestMainActivity).gotoChat()
            }

            override fun onItemTestClick(item: Account, position: Int) {
                testDashboardViewModel.login(item)
//                testDashboardViewModel.testNotification(item)
            }

        })
    }

    fun initObserve(){
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                testDashboardViewModel.spinnerState.collect { show ->
                    debugConfig.log(TAG, "spinnerState change!!!!")
                    spinner.visibility = if (show) View.VISIBLE else View.GONE
                    try {
                        val inputMethodManager =
                            activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                        inputMethodManager.hideSoftInputFromWindow(activity?.currentFocus?.windowToken, 0)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                testDashboardViewModel.toastState.collect { msg ->
                    if (!msg.isNullOrEmpty()){
                        Toast.makeText(activity ,msg, Toast.LENGTH_SHORT).show()
                        testDashboardViewModel.doneToastState()
                    }
                }
            }
        }


        testDashboardViewModel.allAcc.observe(viewLifecycleOwner) { accs ->
            testAccsViewAdapter.setItems(accs)
        }
    }







}