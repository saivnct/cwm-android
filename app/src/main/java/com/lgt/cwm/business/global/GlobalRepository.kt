package com.lgt.cwm.business.global

import android.content.Context
import com.lgt.cwm.business.account.AccountRepository
import com.lgt.cwm.business.message.MessageRepository
import com.lgt.cwm.business.ws.WSRepository
import com.lgt.cwm.db.entity.AccountLoginStatus
import com.lgt.cwm.di.AppCoroutineScope
import com.lgt.cwm.di.IODispatcher
import com.lgt.cwm.util.DateUtil
import com.lgt.cwm.util.DebugConfig
import com.lgt.cwm.ws.WSState
import com.lyft.kronos.KronosClock
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Created by giangtpu on 29/07/2022.
 */
@Singleton
class GlobalRepository @Inject constructor(
    @ApplicationContext private val applicationContext: Context,
    @AppCoroutineScope private val appCoroutineScope: CoroutineScope,
    @IODispatcher private val ioDispatcher: CoroutineDispatcher,
    private val accountRepository: AccountRepository,
    private val messageRepository: MessageRepository,
//    private val messageRetryService: MessageRetryService,
    private val wsRepository: WSRepository,
    private val kronosClock: KronosClock,
    private val debugConfig: DebugConfig,
){
    private val TAG = GlobalRepository::class.simpleName.toString()

    fun observeGlobal(){
        messageRepository.startPeriodicWorkerFetchMessage()

        appCoroutineScope.launch(){
            accountRepository.activeAccFlow
                .collect { account ->
                    accountRepository.currentActiveAccount = account
                    account?.also { acc ->
//                        debugConfig.log(TAG,"On New Active Account ${acc.phoneFull} !!!!")
                        if (acc.status != AccountLoginStatus.DOING_LOGIN.code){
                            wsRepository.disconnectCurrentSocket()
                        }else{
                            val now = kronosClock.getCurrentTimeMs()
                            val diffSecond = (now - acc.lastAttempLoginAt) / 1000
                            if (diffSecond > 5){
                                wsRepository.disconnectCurrentSocket()
                            }
                        }

                        if (acc.isLogin()){
                            messageRepository.startWorkerFetchMessage()
                        }

                    } ?: run {
                        debugConfig.log(TAG,"There's no active account!!!!")
                        wsRepository.disconnectCurrentSocket()
                    }
//                    wsRepository.disconnectCurrentSocket()
//                    accountRepository.currentActiveAccount = account
                }
        }

        appCoroutineScope.launch {
            wsRepository.wsStateFlow.collect() { wsState ->
//                debugConfig.log(TAG,"On New wsState ${wsState} !!!!")
                accountRepository.currentActiveAccount?.let{ acc ->
                    if (wsState == WSState.DISCONNECTED){
//                        debugConfig.log(TAG,"wsState DISCONECTED -> RETRY CONNECT !!!!")
                        wsRepository.startNewSocket(acc)
                    }else if (wsState == WSState.RECONNECT_ATTEMPT){
                        wsRepository.reconnectAttemp()
                        val now = kronosClock.getCurrentTimeMs()
                        val jwtTTLAt = acc.jwtAt + acc.jwtTTL*60*1000
                        if (now > jwtTTLAt || acc.jwt.isNullOrEmpty()){
                            debugConfig.log(TAG,"wsState JWT TIMEOUT at ${jwtTTLAt} -> RELOGIN !!!!")
                            accountRepository.grpcLogin(acc, null)
                        }else{
                            wsRepository.startNewSocket(acc)
                        }
                    }
                }
            }
        }

        //region old logic handle chat msg
//        appCoroutineScope.launch {
//            wsRepository.wsChatMsgFlow.collect() { cwmRequest ->
//                messageRepository.onWSChatMsg(cwmRequest)
//            }
//        }
//        appCoroutineScope.launch {
//            messageRepository.handleChannelChatMsg()
//        }
        //test
//        appCoroutineScope.launch {
//            messageRepository.testProduceChatMsg()
//        }
        //endregion


        //        appCoroutineScope.launch {
//            messageRepository.countAllByStatusFlow(SignalMsgStatus.SENDING.code).collect() { numberPendingMsg ->
////                debugConfig.log(TAG, "countAllByStatusFlow ${numberPendingMsg}")
//                messageRetryService.checkTimerTask(numberPendingMsg > 0)
//            }
//        }




    }

}