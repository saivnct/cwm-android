package com.lgt.cwm.activity.test

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.database.ContentObserver
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.ContactsContract
import android.provider.Settings
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import cn.pedant.SweetAlert.SweetAlertDialog
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.ktx.messaging
import com.lgt.cwm.R
import com.lgt.cwm.activity.test.fragments.dashboard.TestDashboardFragment
import com.lgt.cwm.activity.test.navigation.ClickListener
import com.lgt.cwm.activity.test.navigation.TestNavigationItemModel
import com.lgt.cwm.activity.test.navigation.TestNavigationRVAdapter
import com.lgt.cwm.activity.test.navigation.TestRecyclerTouchListener
import com.lgt.cwm.activity.testchat.TestChatActivity
import com.lgt.cwm.databinding.ActivityTestMainBinding
import com.lgt.cwm.business.notification.NotificationHandler
import com.lgt.cwm.util.DebugConfig
import com.tbruyelle.rxpermissions3.RxPermissions
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.activity_test_main.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Created by giangtpu on 6/29/22.
 */
@AndroidEntryPoint
class TestMainActivity : AppCompatActivity() {
    val TAG = TestMainActivity::class.java.simpleName.toString()

    val TestDashboardFragmentTAG= "TestDashboardFragment"

    private val testMainViewModel: TestMainViewModel by viewModels()

    @Inject lateinit var testNavigationRVAdapter: TestNavigationRVAdapter
    @Inject lateinit var debugConfig: DebugConfig
    @Inject lateinit var notificationHandler: NotificationHandler


    lateinit private var logoutDialog: SweetAlertDialog
    lateinit private var contentObserver: ContentObserver
    private val rxPermissions: RxPermissions by lazy {
        RxPermissions(this)
    }

    private var items = arrayListOf(
        TestNavigationItemModel(
            R.drawable.ic_dashboard,
            R.string.navtitle_dashboard
        ),
        TestNavigationItemModel(
            R.drawable.ic_menu_conversation,
            R.string.navtitle_log
        ),
        TestNavigationItemModel(
            R.drawable.ic_exit,
            R.string.log_out
        ),
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding: ActivityTestMainBinding = DataBindingUtil.setContentView(
            this,
            R.layout.activity_test_main
        );
        binding.lifecycleOwner = this  // use Fragment.viewLifecycleOwner for fragments


        setSupportActionBar(toolbar)
        toolbar.setTitleTextAppearance(this, R.style.ToolbarTextAppearance)

        // clear title action bar
        supportActionBar?.let {
            it.setDisplayShowTitleEnabled(false)
        }

        // Setup Recyclerview's Layout
        rvNav.layoutManager = LinearLayoutManager(this)
        rvNav.setHasFixedSize(true)

        // Add Item Touch Listener
        rvNav.addOnItemTouchListener(TestRecyclerTouchListener(this, object : ClickListener {
            override fun onClick(view: View, position: Int) {
                // highlight selected item in navigation menu (!= log out)
                if (position != items.count() - 1) {
                    testNavigationRVAdapter.setSelectItem(position)
                }

                when (position) {
                    0 -> {
                        // Dashboard fragment
                        val currentFragment = supportFragmentManager.findFragmentByTag(
                            TestDashboardFragmentTAG
                        );
                        if ((currentFragment == null) || !(currentFragment is TestDashboardFragment)) {
                            supportFragmentManager.beginTransaction().replace(
                                R.id.fragment_container,
                                TestDashboardFragment(), TestDashboardFragmentTAG
                            ).commit()
                        }
                    }
                    1 -> {
                        // Log fragment

                    }
                    2 -> {
                        // Log out
                        performLogout()
                    }
                }

                Handler().postDelayed({
                    drawerLayout.closeDrawer(GravityCompat.START)
                }, 100)
            }
        }))

        // Init Adapter with item data and highlight the default menu item ('Dashboard' Fragment)
        initAdapter()

        // Close the soft keyboard when you open or close the Drawer
        val toggle: ActionBarDrawerToggle = object : ActionBarDrawerToggle(
            this,
            drawerLayout,
            toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        ) {
            override fun onDrawerClosed(drawerView: View) {
                // Triggered once the drawer closes
                super.onDrawerClosed(drawerView)
                try {
                    val inputMethodManager =
                        getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    inputMethodManager.hideSoftInputFromWindow(currentFocus?.windowToken, 0)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            override fun onDrawerOpened(drawerView: View) {
                // Triggered once the drawer opens
                super.onDrawerOpened(drawerView)
                try {
                    val inputMethodManager =
                        getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    inputMethodManager.hideSoftInputFromWindow(currentFocus!!.windowToken, 0)
                } catch (e: Exception) {
                    e.stackTrace
                }
            }
        }
        drawerLayout.addDrawerListener(toggle)

//        toggle.isDrawerIndicatorEnabled = false
//        toggle.setHomeAsUpIndicator(R.drawable.ic_hamburger)
//        toggle.toolbarNavigationClickListener = View.OnClickListener {
//            drawerLayout.openDrawer(
//                GravityCompat.START
//            )
//        }
        toggle.syncState()

        val currentFragment = supportFragmentManager.findFragmentByTag(TestDashboardFragmentTAG);
        if ( (currentFragment == null) || !(currentFragment is TestDashboardFragment)){
            supportFragmentManager.beginTransaction().replace(
                R.id.fragment_container,
                TestDashboardFragment(), TestDashboardFragmentTAG
            ).commit()
        }

        consumeObservableData()
        debugConfig.log(TAG, "mainViewModel:"+testMainViewModel.hashCode())


        notificationHandler.registerNotificationChannels()

        checkFirebaseToken()

//        registerContactObserver()

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU){
            checkPostNotificationPermission()
        }
        checkContactPermission()
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterContactObserver()
    }

    private fun consumeObservableData() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                testMainViewModel.toolbarTitleState.collect {
                    toolbar.title = it
                }
            }
        }
    }

    private fun initAdapter(highlightItemPos: Int? = null) {
        testNavigationRVAdapter.setNavItem(items)
        rvNav.adapter = testNavigationRVAdapter
        testNavigationRVAdapter.setSelectItem(highlightItemPos ?: 0)
    }

    private fun performLogout() {
        logoutDialog = SweetAlertDialog(this, SweetAlertDialog.WARNING_TYPE)
        with(logoutDialog) {
            titleText = getString(R.string.log_out)
            contentText = getString(R.string.log_out_confirm)
            confirmText = getString(R.string.btn_confirm)
            cancelText = getString(R.string.btn_cancel)
            setConfirmClickListener {
                it.dismiss()    //dismiss dialog before finish activity
                finish()
//                doLogoutAccount()
            }
            setCancelClickListener {
                it.dismiss()
            }
            show()
        }

    }

    fun checkFirebaseToken() {
        // [START log_reg_token]
        Firebase.messaging.getToken().addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w(TAG, "Fetching FCM registration token failed", task.exception)
                return@addOnCompleteListener
            }

            // Get new FCM registration token
            val token = task.result

            // Log and toast
            val msg = "FCM Registration token: $token"
            Log.d(TAG, msg)
            Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
        }
        // [END log_reg_token]
    }

    fun gotoChat(){
        val intent = Intent(this, TestChatActivity::class.java)
        startActivity(intent)
    }



    @SuppressLint("CheckResult")
    @RequiresApi(33)
    fun checkPostNotificationPermission(){
        rxPermissions.request(Manifest.permission.POST_NOTIFICATIONS)
            .subscribe { granted ->
                if(granted){
                    debugConfig.log(TAG, "checkPermission POST_NOTIFICATIONS got permission!")
                } else if (!shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)){
                    //TODO SHOW POPUP NOTIFY ABOUT PERMISSIONS
                    debugConfig.log(TAG, "gotoApplicationPermissionSetting")
                    gotoApplicationPermissionSetting(this)
                }
            }
    }

    fun checkContactPermission(){
        rxPermissions.request(Manifest.permission.READ_CONTACTS)
            .subscribe { granted ->
                if(granted){
                    debugConfig.log(TAG, "checkPermission READ_CONTACTS got permission!")
                    registerContactObserver()
                } else if (!shouldShowRequestPermissionRationale(Manifest.permission.READ_CONTACTS)){
                    //TODO SHOW POPUP NOTIFY ABOUT PERMISSIONS
                    debugConfig.log(TAG, "gotoApplicationPermissionSetting")
                    gotoApplicationPermissionSetting(this)
                }
            }
    }

    private fun gotoApplicationPermissionSetting(context: Context) {
        val intent = Intent()
        intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val uri =
            Uri.fromParts("package", context.getPackageName(), null)
        intent.data = uri
        startActivity(intent)
    }

    fun registerContactObserver(){
        //TODO - check permission
        debugConfig.log(TAG,"registerContactObserver")

        testMainViewModel.syncContact()

        val handler = Handler(Looper.getMainLooper())

        contentObserver = object : ContentObserver(handler) {
            override fun onChange(self: Boolean) {
                debugConfig.log(TAG, "contentObserver - onChange - boolean value is $self")
                testMainViewModel.syncContact()
            }
        }

        getContentResolver().registerContentObserver(ContactsContract.Contacts.CONTENT_URI, true, contentObserver);
    };

    fun unregisterContactObserver(){
        debugConfig.log(TAG,"unregisterContactObserver")
        getContentResolver().unregisterContentObserver(contentObserver);
    }


}