package com.lgt.cwm.activity.home

import android.Manifest
import android.content.Context
import android.content.Intent
import android.database.ContentObserver
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.ContactsContract
import android.provider.Settings
import android.view.Menu
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.badge.BadgeDrawable
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.ktx.messaging
import com.lgt.cwm.R
import com.lgt.cwm.activity.conversation.ConversationActivity
import com.lgt.cwm.activity.conversation.ConversationActivity.Companion.EXTRA_THREAD_ID
import com.lgt.cwm.business.notification.NotificationHandler
import com.lgt.cwm.databinding.ActivityMainBinding
import com.lgt.cwm.databinding.NavHeaderMainBinding
import com.lgt.cwm.db.MyPreference
import com.lgt.cwm.ui.components.voice.VoiceNoteMediaController
import com.lgt.cwm.ui.components.voice.VoiceNoteMediaControllerOwner
import com.lgt.cwm.util.Config
import com.lgt.cwm.util.DebugConfig
import com.lyft.kronos.KronosClock
import com.tbruyelle.rxpermissions3.RxPermissions
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity() : AppCompatActivity(), VoiceNoteMediaControllerOwner {
    private val TAG = MainActivity::class.simpleName.toString()

    @Inject lateinit var debugConfig: DebugConfig
    @Inject lateinit var notificationHandler: NotificationHandler
    @Inject lateinit var myPreference: MyPreference
    @Inject lateinit var kronosClock: KronosClock

    lateinit var bottomNavView: BottomNavigationView
    lateinit var navController: NavController
    lateinit var unreadMessageBadge: BadgeDrawable

    private val mainViewModel: MainViewModel by viewModels()

    private var mediaController: VoiceNoteMediaController? = null

    private val rxPermissions: RxPermissions by lazy {
        RxPermissions(this)
    }
    private var contentObserver: ContentObserver? = null

    private lateinit var appBarConfiguration: AppBarConfiguration



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        debugConfig.log(TAG, "onCreate")

        val binding: ActivityMainBinding = DataBindingUtil.setContentView(
            this,
            R.layout.activity_main
        );
        binding.lifecycleOwner = this
        setSupportActionBar(binding.appBarMain.toolbar)
        binding.appBarMain.fab.setOnClickListener { view ->
            Snackbar.make(view, "Make new conversation", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()
        }


        mediaController = VoiceNoteMediaController(this)

        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView

        val navHeaderMainBinding: NavHeaderMainBinding = DataBindingUtil.inflate(getLayoutInflater(), R.layout.nav_header_main, binding
            .navView, false);
        navHeaderMainBinding.lifecycleOwner = this
        navHeaderMainBinding.mainViewModel = mainViewModel


        navView.addHeaderView(navHeaderMainBinding.root)




        navController = findNavController(R.id.nav_host_fragment_content_main)
        bottomNavView = binding.appBarMain.contentMain.bottomNavView


        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
//        appBarConfiguration = AppBarConfiguration(
//            setOf(
//                R.id.conversationListFragment, R.id.callFragment, R.id.contactFragment, R.id.settingFragment
//            ), drawerLayout
//        )
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.conversationListFragment, R.id.contactFragment, R.id.settingFragment
            ), drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
        bottomNavView.setupWithNavController(navController)
        unreadMessageBadge = bottomNavView.getOrCreateBadge(R.id.conversationListFragment)
        hideBadgeNumberUnreadMessage()

        initListeners()

//        addMenuProvider(object : MenuProvider {
//            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
//                // Add menu items here
//                menuInflater.inflate(R.menu.main, menu)
//            }
//
//            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
//                // Handle the menu selection
//                return true
//            }
//        })

        initObserver()

        checkActiveAccount()

        val now1 = kronosClock.getCurrentTimeMs()
        val now2 = System.currentTimeMillis()

        debugConfig.log(TAG,"now1: ${now1} - now2: ${now2} - diff: ${(now1-now2)/1000}s  - ${(now1-now2)/60000}min")
    }

    override fun onResume() {
        super.onResume()
//        debugConfig.log(TAG, "onResume!!!!!!!!")
        mainViewModel.currentActiveAcc?.let {
            if (it.isLogin()){
                fetchAllUnreceivedMsg()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterContactObserver()
    }

    fun visibilityToolbar(visibility: Boolean) {
        if (visibility) {
            toolbar.visibility = View.VISIBLE
        } else {
            toolbar.visibility = View.GONE
        }
    }

    private fun visibilityBottomNavigationBar(visibility: Boolean) {
        if (visibility) {
            bottomNavView.visibility = View.VISIBLE
        } else {
            bottomNavView.visibility = View.GONE
        }
    }

    private fun initListeners() {
        fab.setOnClickListener{
//            navController.navigate(R.id.action_conversationListFragment_to_newConversationFragment)
            navController.navigate(R.id.action_conversationListFragment_to_newConversationFragment)
        }

        navController.addOnDestinationChangedListener { _, destination, _ ->
//            debugConfig.log(TAG, "addOnDestinationChangedListener label ${destination.label} - id ${destination.id}")
            fab.visibility = View.GONE

            visibilityToolbar(true)
            visibilityBottomNavigationBar(false)

            when (destination.id) {
                R.id.splashFragment -> {
                    visibilityToolbar(false)
                }
                R.id.registerFragment -> {
                    visibilityToolbar(false)
                }
                R.id.editProfileFragment -> {
                    visibilityToolbar(false)
                }
                R.id.enterSmsCodeFragment -> {
                    visibilityToolbar(false)
                }
                R.id.countryPickerFragment -> {
                    visibilityToolbar(false)
                }

                R.id.conversationListFragment -> {
                    fab.visibility = View.VISIBLE
                    visibilityBottomNavigationBar(true)
                }
                R.id.callFragment -> {
                    visibilityBottomNavigationBar(true)
                }
                R.id.contactFragment -> {
                    visibilityBottomNavigationBar(true)
                }
                R.id.settingFragment -> {
                    visibilityBottomNavigationBar(true)
                }
                R.id.newConversationFragment -> {
                    supportActionBar?.let {
                        it.setDisplayHomeAsUpEnabled(true)
                        it.setTitle(R.string.NewConversationFragment_title)
                    }
                }
                R.id.action_newConversationFragment_to_createGroupFragment -> {
                    supportActionBar?.let {
                        it.title = "Create group"
                    }
                }
                R.id.addGroupDetailsFragment -> {
                    supportActionBar?.let {
                        it.title = "Name this group"
                    }
                }
                else -> {

                }
            }
        }
    }

    private fun initObserver(){
        mainViewModel.activeAcc.observe(this) { account ->
            mainViewModel.currentActiveAcc = account
            account?.let { it ->
                if (it.isLogin()){
                    checkFirebaseToken()
                }
            }
        }

        mainViewModel.countAllUnhanldedEventMsg.observe(this){  numberUnhanldedEventMsg ->
            if (numberUnhanldedEventMsg > 0 &&
                mainViewModel.currentActiveAcc != null){
//                debugConfig.log(TAG,"numberUnhanldedEventMsg  ${numberUnhanldedEventMsg}")
                mainViewModel.startWorkerMessageEventHanlde()
            }
        }

        mainViewModel.countAllSendingMsg.observe(this){  numberSendingMsg ->
            if (numberSendingMsg > 0 &&
                mainViewModel.currentActiveAcc != null){
//                debugConfig.log(TAG,"numberSendingMsg  ${numberSendingMsg}")
                mainViewModel.startWorkerMessageTrySend()
            }
        }

        mainViewModel.countAllUnreadMsg.observe(this){ unreadMsg ->
            if (unreadMsg > 0){
                showBadgeNumberUnreadMessage(unreadMsg)
            }else{
                hideBadgeNumberUnreadMessage()
            }

        }



    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    fun checkActiveAccount(){
        lifecycleScope.launch {
            val account = mainViewModel.getActiveAccount()

            //check and go to Chat
            intent.action?.let { action ->
                if (action.startsWith(Config.IntentAction.OPEN_CHAT)){
                    val threadId = intent.getStringExtra(EXTRA_THREAD_ID)
                    if (!threadId.isNullOrEmpty() && account != null){
                        gotoChat(threadId)
                        return@launch
                    }
                }
            }


            if (account != null){
                checkPermissions()
            }
        }
    }

     fun fetchAllUnreceivedMsg(){
         mainViewModel.fetchAllUnreceivedMsg()
    }


    //region Badge
    fun showBadgeNumberUnreadMessage(count: Long) {
        unreadMessageBadge.isVisible = true
        unreadMessageBadge.number = count.toInt()
    }

    fun hideBadgeNumberUnreadMessage() {
        unreadMessageBadge.isVisible = false
        unreadMessageBadge.clearNumber()
    }
    //endregion


    fun gotoChat(threadId: String){
        val intent = Intent(this, ConversationActivity::class.java).apply {
            putExtra(EXTRA_THREAD_ID, threadId)
        }
        this.startActivity(intent)
        this.overridePendingTransition(R.anim.slide_from_end, R.anim.fade_scale_out);
    }


    //region premission
    fun checkPermissions(){
        val permissions = arrayListOf<String>()
        permissions.add(Manifest.permission.READ_CONTACTS)
//        permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)   //TODO - will ask for WRITE_EXTERNAL_STORAGE when user want to save file to external storage
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU){
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        rxPermissions.requestEach(*(permissions.toTypedArray()))    // * is spread operator     https://kotlinlang.org/docs/functions.html#variable-number-of-arguments-varargs
            .subscribe { permission ->
                if (permission.granted) {
                    debugConfig.log(TAG, "permission ${permission.name} is granted !")
                    if (permission.name == Manifest.permission.READ_CONTACTS){
                        registerContactObserver()
                    }
                } else if (permission.shouldShowRequestPermissionRationale) {
                    //TODO SHOW POPUP NOTIFY ABOUT PERMISSIONS
                    debugConfig.log(TAG, "gotoApplicationPermissionSetting")
                    gotoApplicationPermissionSetting(this)
                } else {
                    // Denied permission with ask never again
                    // Need to go to the settings
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
    //endregion


    //region contact
    fun registerContactObserver(){
//        debugConfig.log(TAG,"registerContactObserver")

        mainViewModel.syncContact()

        val handler = Handler(Looper.getMainLooper())

        contentObserver = object : ContentObserver(handler) {
            override fun onChange(self: Boolean) {
//                debugConfig.log(TAG, "contentObserver - onChange - boolean value is $self")
                mainViewModel.syncContact()
            }
        }

        getContentResolver().registerContentObserver(ContactsContract.Contacts.CONTENT_URI, true, contentObserver!!);
    };

    fun unregisterContactObserver(){
//        debugConfig.log(TAG,"unregisterContactObserver")
        contentObserver?.let { contentObserver ->
            getContentResolver().unregisterContentObserver(contentObserver);
        }
    }
    //endregion

    //region firebase
    fun checkFirebaseToken() {
        Firebase.messaging.getToken().addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                debugConfig.log(TAG, "Fetching FCM registration token failed ${task.exception}")
                return@addOnCompleteListener
            }

            // Get new FCM registration token
            val token = task.result
            debugConfig.log(TAG, "FCM Registration token: $token")

            val dbToken = myPreference.getFCMToken()
//            debugConfig.log(TAG, "dbToken: $dbToken")

            if (dbToken.isNullOrEmpty() || !token.equals(dbToken)){
                debugConfig.log(TAG, "submitNewFCMToken: $token")
                mainViewModel.startWorkerSendFCMToken(token)
            }
        }
    }
    //endregion

    override val voiceNoteMediaController: VoiceNoteMediaController
        get() {
            if (mediaController == null) mediaController = VoiceNoteMediaController(this)
            return mediaController!!
    }

}