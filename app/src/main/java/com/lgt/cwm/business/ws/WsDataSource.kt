package com.lgt.cwm.business.ws

import android.content.Context
import android.util.Base64
import androidx.work.*
import com.lgt.cwm.business.message.WorkerHandleChatMessage
import com.lgt.cwm.db.entity.Account
import com.lgt.cwm.di.AppCoroutineScope
import com.lgt.cwm.util.*
import com.lgt.cwm.ws.WSQuery
import com.lgt.cwm.ws.WSState
import cwmSIPPb.CwmSIP
import dagger.hilt.android.qualifiers.ApplicationContext
import io.socket.client.Ack
import io.socket.client.IO
import io.socket.client.Manager
import io.socket.client.Socket
import io.socket.engineio.client.transports.WebSocket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.net.URI
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException


/**
 * Created by giangtpu on 04/07/2022.
 */
@Singleton
class WsDataSource @Inject constructor(
    @ApplicationContext private val applicationContext: Context,
    @AppCoroutineScope private val appCoroutineScope: CoroutineScope,
    val debugConfig: DebugConfig
    ){

    private val TAG = WsDataSource::class.simpleName.toString()

    var socket: Socket? = null

    private val _wsStateFlow = MutableSharedFlow<WSState>(replay = 1)
    val wsStateFlow = _wsStateFlow.asSharedFlow()



    init {
        emitWSState(WSState.DISCONNECTED)
    }

    private fun emitWSState(wsState: WSState){
        appCoroutineScope.launch {
            _wsStateFlow.emit(wsState)
        }
    }

    //old logic handle chat msg
//    private val _wsChatMsgFlow = MutableSharedFlow<String>()
//    val wsChatMsgFlow = _wsChatMsgFlow.asSharedFlow()
//    private fun emitWSChatMsg(cwmMsgDataBase64: String){
//        appCoroutineScope.launch {
//            _wsChatMsgFlow.emit(cwmMsgDataBase64)
//        }
//    }

    fun startWorkerHandleChatMessage(cwmMsgDataBase64: String, shouldFetchAll: Boolean = false){
//        debugConfig.log(TAG,"startWorkerHandleChatMessage")

        val request = OneTimeWorkRequestBuilder<WorkerHandleChatMessage>()
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .setInputData(
                workDataOf(
                    WorkerHandleChatMessage.INPUT_CWM_MSG to cwmMsgDataBase64,
                    WorkerHandleChatMessage.INPUT_SHOULD_FETCH_ALL to shouldFetchAll,
                )
            )
            .addTag("WorkerHandleChatMessage")
            .build()

        WorkManager.getInstance(applicationContext)
            .enqueueUniqueWork(
                "WorkerHandleChatMessage",
                ExistingWorkPolicy.APPEND_OR_REPLACE,
                request
            )
    }



    fun createNewSocket(account: Account){
        //+ on URL will be become %20 => wrong phoneFull as expected => handle this on server side
        val wsQuery = WSQuery(authorization = account.jwt.toByteArray().toHex())
        val uri: URI = URI.create("http://${Config.WS.HOST}:${Config.WS.PORT}")

        //socket io v1
        val options = IO.Options()
        options.transports = arrayOf(WebSocket.NAME)
        options.query = wsQuery.toQueryString()
        options.path = Config.WS.PATH


//        debugConfig.log(TAG, "wsQuery ${wsQuery.toQueryString()}")

        //socket io v4
//        val options = IO.Options.builder()
//            .setPath(Config.WS.PATH)
//            .setTransports(arrayOf(WebSocket.NAME)) // Set the transfer to 'websocket' instead of 'polling')
//            .setQuery(wsQuery.toQueryString())
//            .build()

        socket = IO.socket(uri, options)
    }



    // using suspendCoroutine to convert a callback-based API to a suspending function
    //Use suspendCancellableCoroutine instead of suspendCoroutine with proper exception handling
    suspend fun sendWSChatMsg(cwmRequest: CwmSIP.CWMRequest) : Result<Long> =
        suspendCancellableCoroutine { cont ->
            socket?.also { socket ->
                if (socket.connected()){
                    try {
                        val args = arrayOf(Base64.encodeToString(cwmRequest.toByteArray(), Base64.DEFAULT))

                        socket.emit("sendSignalMsg", args,
                            Ack { args: Array<Any> ->

                                if (args != null && args[0] != null){
                                    try{
                                        //                                val response = args[0] as ByteArray
                                        val response = args[0] as String

                                        val cwmResponse = CwmSIP.CWMResponse.parseFrom(Base64.decode(response, Base64.DEFAULT))
                                        val status = cwmResponse.code
                                        val message = cwmResponse.message
                                        val serverDate = cwmResponse.content.toByteArray().toLong()
//                                    debugConfig.log(TAG, "sendWSChatMsg - response code ${status} - ${message} - serverDate ${serverDate}")

                                        if (status == SIPCode.OK.code) {
                                            cont.resume(Result.Success(serverDate))
                                        }else{
                                            cont.resume(Result.Error(Exception(message)))
                                        }
                                    }catch (ex: Throwable){
                                        cont.resumeWithException(ex)
                                    }
                                }

                            }
                        )
                    }catch (ex: Throwable){
                        cont.resumeWithException(ex)
                    }
                }else{
                    cont.resume(Result.Error(Exception("Socket is not connected")))
                }
            }?: run {
                cont.resume(Result.Error(Exception("Socket is not connected")))
            }
        }

    suspend fun sendWSConfirmRecieved(msgIds: List<String>) : Result<Boolean> =
        suspendCancellableCoroutine { cont ->
            socket?.also { socket ->
                if (socket.connected()){
                    try {
                        val msgIdsStr = msgIds.joinToString(",")
                        val args = arrayOf(msgIdsStr)

                        socket.emit("confirmRecieved", args,
                            Ack { args: Array<Any> ->
                                try{
                                    //                                val response = args[0] as ByteArray
                                    val response = args[0] as String
                                    if (response.equals(msgIdsStr)) {
                                        cont.resume(Result.Success(true))
                                    }else{
                                        cont.resume(Result.Error(Exception("received invalid response msgId")))
                                    }
                                }catch (ex: Throwable){
                                    cont.resumeWithException(ex)
                                }
                            }
                        )
                    }catch (ex: Throwable){
                        cont.resumeWithException(ex)
                    }
                }else{
                    cont.resume(Result.Error(Exception("Socket is not connected")))
                }
            }?: run {
                cont.resume(Result.Error(Exception("Socket is not connected")))
            }
        }

    fun isConnected(): Boolean{
        return socket?.connected() ?: false
    }


    fun connect(account: Account){

        socket?.let { socket ->
            socket.disconnect()
            socket.off()
        }

        createNewSocket(account)
        socket?.let{ socket ->
            setupEvents()
            emitWSState(WSState.CONNECTING)
            socket.connect()
        }
    }

    fun disconnect(){
        socket?.let { socket ->
            socket.disconnect()
            socket.off()
        }

        emitWSState(WSState.DISCONNECTED)
    }

    fun reconnectAttemp(){
        socket?.let { socket ->
            socket.disconnect()
            socket.off()
        }
    }

    //CONNECT -> ERROR -> RECONNECT_ATTEMPT -> DISCONNECT -> new socket
    fun setupEvents(){
        socket?.let { socket ->
            socket.io().on(Manager.EVENT_RECONNECT_ATTEMPT){
//                debugConfig.log(TAG, "Socket.EVENT_RECONNECT_ATTEMPT")
                emitWSState(WSState.RECONNECT_ATTEMPT)
            }

//            socket.on(Socket.EVENT_PING){
//                debugConfig.log(TAG, "Socket.EVENT_PING")
//            }
//
//            socket.on(Socket.EVENT_PONG){
//                debugConfig.log(TAG, "Socket.EVENT_PONG")
//            }

            socket.on(Socket.EVENT_CONNECT){
//                debugConfig.log(TAG, "Socket.EVENT_CONNECT")
                emitWSState(WSState.CONNECTED)
            }

            socket.on(Socket.EVENT_DISCONNECT){
//                debugConfig.log(TAG, "Socket.EVENT_DISCONNECT")
                emitWSState(WSState.DISCONNECTED)
            }

            socket.on(Socket.EVENT_CONNECT_ERROR){ args ->
                for (o in args) {
                    debugConfig.log(TAG, "EVENT_CONNECT_ERROR:"+o.toString())
                }
//                val err = args[0]
//
//                if (err is EngineIOException) {
//                    debugConfig.log(TAG, "Socket.EVENT_CONNECT_ERROR - code: ${err.code} - msg: ${err.message}")
//                }else{
//                    debugConfig.log(TAG, "Socket.EVENT_CONNECT_ERROR - "+err)
//                }
                emitWSState(WSState.ERROR)
            }

            socket.on("onChatMsg"){ args ->
                //json
//                val data: JSONObject? = args[0] as? JSONObject
//                data?.let {
//                    debugConfig.log(TAG, "Socket onChatMsg ${data.toString()}")
//
//                    val from: String
//                    val msg: String
//                    try {
//                        from = data.getString("from")
//                        msg = data.getString("msg")
//
//                        emitWSChatMsg(WSChatMsg(from = from, msg = msg))
//                    } catch (e: JSONException) {
//                        e.printStackTrace()
//                    }
//                }

                //proto
                try {
                    val cwmMsgDataBase64 = args[0] as String
//                    debugConfig.log(TAG, "Socket onChatMsg ${data}")
                    startWorkerHandleChatMessage(cwmMsgDataBase64)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}