//package com.lgt.cwm.business.ws
//
//import android.app.Service
//import android.content.Intent
//import android.os.IBinder
//import android.widget.Toast
//import com.lgt.cwm.business.account.AccountRepository
//import com.lgt.cwm.db.entity.Account
//import com.lgt.cwm.util.DebugConfig
//import com.lgt.cwm.ws.WSState
//import dagger.hilt.android.AndroidEntryPoint
//import kotlinx.coroutines.CoroutineScope
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.SupervisorJob
//import kotlinx.coroutines.launch
//import javax.inject.Inject
//
///**
// * Created by giangtpu on 10/07/2022.
// */
//@AndroidEntryPoint
//class WSService : Service(){
//    private val TAG = WSService::class.simpleName.toString()
//
//    @Inject
//    lateinit var wsRepository: WSRepository
//    @Inject
//    lateinit var accountRepository: AccountRepository
//    @Inject
//    lateinit var debugConfig: DebugConfig
//
//    private val job = SupervisorJob()
//    private val scope = CoroutineScope(Dispatchers.IO + job)
//    private var currentActiveAccount: Account? = null
//
//    override fun onCreate() {
//        super.onCreate()
//        observeWS()
//    }
//
//    private fun observeWS(){
//        scope.launch {
//            accountRepository.activeAccFlow
//                .collect { account ->
////                    account?.also { acc ->
////                        debugConfig.log(TAG,"On New Active Account ${acc.username} !!!!")
////                    } ?: run {
////                        debugConfig.log(TAG,"There's no active account!!!!")
////                    }
//                    currentActiveAccount = account
//                    wsRepository.disconnectCurrentSocket()
//                }
//        }
//
//        scope.launch {
//            wsRepository.wsStateFlow.collect() { wsState ->
//                debugConfig.log(TAG,"On New wsState ${wsState} !!!!")
//                if (wsState == WSState.DISCONNECTED){
//                    currentActiveAccount?.let{ acc ->
//                        debugConfig.log(TAG,"wsState DISCONECTED -> RETRY CONNECT !!!!")
//                        wsRepository.startNewSocket(acc)
//                    }
//                }
//            }
//        }
//    }
//
//
//    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
//        debugConfig.log(TAG,"WSService starting !!!! - ${this.hashCode()}")
//        Toast.makeText(this, "WSService starting", Toast.LENGTH_SHORT).show()
//
//        // If we get killed, after returning from here, restart
//        return START_STICKY
//
////        return START_NOT_STICKY
//    }
//
//    //destroy service when app is killed, must register android:stopWithTask="false" in Android Manifest
//    override fun onTaskRemoved(rootIntent: Intent?) {
//        super.onTaskRemoved(rootIntent)
//
//        //unregister listeners
//        //do any other cleanup if required
//
//        //stop service
//        debugConfig.log(TAG,"WSService onTaskRemoved !!!! - ${this.hashCode()}")
//        stopSelf();
//    }
//
//
//    override fun onBind(p0: Intent?): IBinder? {
//        // We don't provide binding, so return null
//        return null
//    }
//
//    override fun onDestroy() {
//        super.onDestroy()
//        job.cancel()
//        debugConfig.log(TAG,"WSService destroy !!!! - ${this.hashCode()}")
//        Toast.makeText(this, "WSService destroy!!!", Toast.LENGTH_SHORT).show()
//    }
//}