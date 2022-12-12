package com.lgt.cwm.activity.testchat

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NavUtils
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.lgt.cwm.R
import com.lgt.cwm.activity.testchat.fragments.chat.TestChatFragment
import com.lgt.cwm.databinding.ActivityTestChatBinding
import com.lgt.cwm.util.DebugConfig
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.activity_test_main.*
import kotlinx.coroutines.launch
import javax.inject.Inject


/**
 * Created by giangtpu on 09/07/2022.
 */
@AndroidEntryPoint
class TestChatActivity : AppCompatActivity() {
    val TAG = TestChatActivity::class.java.simpleName.toString()

    val TestChatFragmentTAG= "TestChatFragment"

    @Inject
    lateinit var debugConfig: DebugConfig

    private val testChatViewModel: TestChatViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding: ActivityTestChatBinding = DataBindingUtil.setContentView(
            this,
            R.layout.activity_test_chat
        );
        binding.lifecycleOwner = this  // use Fragment.viewLifecycleOwner for fragments

        setSupportActionBar(toolbar)
        toolbar.setTitleTextAppearance(this, R.style.ToolbarTextAppearance)

        // clear title action bar
        supportActionBar?.let {
            it.setDisplayShowTitleEnabled(false)
        }

        val currentFragment = supportFragmentManager.findFragmentByTag(TestChatFragmentTAG);
        if ( (currentFragment == null) || !(currentFragment is TestChatFragment)){
            supportFragmentManager.beginTransaction().replace(
                R.id.fragment_container,
                TestChatFragment(), TestChatFragmentTAG
            ).commit()
        }

        consumeObservableData()
    }


    private fun consumeObservableData() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                testChatViewModel.toolbarTitleState.collect {
                    toolbar.title = it
                }
            }
        }
    }

    override fun onBackPressed() {
        debugConfig.log(TAG,"onBackPressed!!!!!!")
        val intent = NavUtils.getParentActivityIntent(this)
        startActivity(intent)
        finish()
    }
}