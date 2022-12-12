package com.lgt.cwm.business.ws

import com.lgt.cwm.db.entity.Account
import com.lgt.cwm.di.AppCoroutineScope
import com.lgt.cwm.di.IODispatcher
import com.lgt.cwm.util.DebugConfig
import com.lgt.cwm.util.Result
import cwmSIPPb.CwmSIP
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Created by giangtpu on 7/6/22.
 */
@Singleton
class WSRepository @Inject constructor(
    @AppCoroutineScope private val appCoroutineScope: CoroutineScope,
    @IODispatcher private val ioDispatcher: CoroutineDispatcher,
    private val accountWsDataSource: WsDataSource,
    private val debugConfig: DebugConfig,
){
    private val TAG = WSRepository::class.simpleName.toString()

    val wsStateFlow = accountWsDataSource.wsStateFlow
    //old logic handle chat msg
//    val wsChatMsgFlow = accountWsDataSource.wsChatMsgFlow

    fun isConnected(): Boolean{
        return accountWsDataSource.isConnected()
    }

    fun startNewSocket(account: Account){
        accountWsDataSource.connect(account)
    }
    fun disconnectCurrentSocket(){
        accountWsDataSource.disconnect()
    }

    fun reconnectAttemp(){
        accountWsDataSource.reconnectAttemp()
    }

    fun startWorkerHandleChatMessage(cwmMsgDataBase64: String, shouldFetchAll: Boolean = false) = accountWsDataSource.startWorkerHandleChatMessage(cwmMsgDataBase64, shouldFetchAll)

    suspend fun sendMsg(cwmRequest: CwmSIP.CWMRequest) : Result<Long> {
        return withContext(ioDispatcher){
            return@withContext accountWsDataSource.sendWSChatMsg(cwmRequest)
        }
    }

    suspend fun sendConfirmRecieved(msgIds: List<String>) : Result<Boolean> {
        return withContext(ioDispatcher){
            return@withContext accountWsDataSource.sendWSConfirmRecieved(msgIds)
        }
    }


}