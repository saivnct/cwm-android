package com.lgt.cwm.business.account

import android.content.Context
import android.os.Build
import android.provider.Settings
import androidx.work.*
import com.lgt.cwm.BuildConfig
import com.lgt.cwm.db.MyPreference
import com.lgt.cwm.db.dao.AccountDao
import com.lgt.cwm.db.entity.Account
import com.lgt.cwm.db.entity.AccountLoginStatus
import com.lgt.cwm.di.AppCoroutineScope
import com.lgt.cwm.di.IODispatcher
import com.lgt.cwm.http.response.TestPlan
import com.lgt.cwm.util.*
import com.lyft.kronos.KronosClock
import dagger.hilt.android.qualifiers.ApplicationContext
import grpcCWMPb.CwmModel
import grpcCWMPb.CwmRqResAccount
import io.grpc.Status
import io.grpc.StatusException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton


/**
 * Created by giangtpu on 6/29/22.
 */
@Singleton
class AccountRepository @Inject constructor(
    @ApplicationContext private val applicationContext: Context,
    @AppCoroutineScope private val appCoroutineScope: CoroutineScope,
    @IODispatcher private val ioDispatcher: CoroutineDispatcher,
    private val accountHttpDataSource: AccountHttpDataSource,
    private val accountGrpcDataSource: AccountGrpcDataSource,
    private val accountDAO: AccountDao,
    private val myPreference: MyPreference,
    private val kronosClock: KronosClock,
    private val debugConfig: DebugConfig,
){
    private val TAG = AccountRepository::class.simpleName.toString()
    private val loginSemaphore = Semaphore(1)

    val allAccFlow: Flow<List<Account>>
        get() = accountDAO.getAll()

    val activeAccFlow: Flow<Account?>
        get() = accountDAO.getActiveAccFlowDistinctUntilChanged()

    var currentActiveAccount: Account? = null


    suspend fun getActiveAccount() : Account?{
        return withContext(ioDispatcher) {
            return@withContext accountDAO.getActivedAcc()
        }
    }

    suspend fun insertAccount(account: Account) {
        withContext(ioDispatcher) {
            appCoroutineScope.launch {
                // if this can throw an exception, wrap inside try/catch
                // or rely on a CoroutineExceptionHandler installed
                // in the externalScope's CoroutineScope
                try {
                    accountDAO.insertAccount(account)
                }catch (e: Throwable){
                    e.printStackTrace()
                }
            }.join()
        }
    }

    suspend fun updateAccount(account: Account) {
        withContext(ioDispatcher) {
            appCoroutineScope.launch {
                try {
                    accountDAO.update(account)
                }catch (e: Throwable){
                    e.printStackTrace()
                }
            }.join()
        }
    }

    suspend fun setAllInActiveExcept(account: Account) {
        withContext(ioDispatcher) {
            appCoroutineScope.launch {
                try {
                    accountDAO.setAllInActiveExcept(account.id)
                }catch (e: Throwable){
                    e.printStackTrace()
                }
            }.join()
        }
    }


    suspend fun checkAndHandleSessionExpired(result: Result<Any>, account: Account){
//        debugConfig.log(TAG,"checkAndHandleSessionExpired")
        if (result is Result.Error){
            val ex = result.exception
            if (ex is StatusException){
//                debugConfig.log(TAG,"checkAndHandleSessionExpired - StatusException")
                if (ex.status.code == Status.UNAUTHENTICATED.code){
                    val nonce = ex.status.description
                    if (!nonce.isNullOrEmpty() && !nonce.equals(account.nonce)){
                        debugConfig.log(TAG, "checkAndHandleSessionExpired - onLoginSessionExpired - nonce changed := ${nonce}")
                        grpcLogin(account, nonce)
                    }else{
                        grpcLogin(account, null)
                    }
                }
            }
        }
    }

    suspend fun grpcLogin(account: Account, newNonce: String? = null): Result<grpcCWMPb.CwmRqResAccount.LoginResponse> {

        return withContext(ioDispatcher){
            if (!loginSemaphore.tryAcquire()){
                return@withContext Result.Error(Exception("Cannot acquire loginSemaphore"))
            }

            try{
                val lastAttempLoginAt = kronosClock.getCurrentTimeMs()
                var nonce = account.nonce
                var nonceResponse = account.nonceResponse

                if (!newNonce.isNullOrEmpty() ){
                    debugConfig.log(TAG, "Call grpcLogin with new Nonce ${newNonce}")
                    nonce = newNonce
                    nonceResponse = StringUtil.getNonceResponse(newNonce, account.password)

                    accountDAO.setLoginStatusWithNewNonce(
                        id = account.id,
                        status = AccountLoginStatus.DOING_LOGIN.code,
                        lastAttempLoginAt = lastAttempLoginAt,
                        nonce = newNonce,
                        nonceRespone = nonceResponse,
                        nonceAt = lastAttempLoginAt
                    )
                }else{
                    debugConfig.log(TAG, "Call grpcLogin")

                    accountDAO.setLoginStatus(
                        id = account.id,
                        status = AccountLoginStatus.DOING_LOGIN.code,
                        lastAttempLoginAt = lastAttempLoginAt
                    )
                }

                val result = accountGrpcDataSource.login(
                    phoneFull = account.phoneFull,
                    sessionId = account.sessionId,
                    nonce = nonce,
                    nonceResponse = nonceResponse
                )

                when (result) {
                    is Result.Success -> {
                        val responseData = result.data
                        accountDAO.setLoginStatus(
                            id = account.id,
                            status = AccountLoginStatus.LOGIN.code,
                            lastAttempLoginAt = lastAttempLoginAt,
                            jwt = responseData.jwt,
                            jwtAt = kronosClock.getCurrentTimeMs(),
                            jwtTTL = responseData.jwtTTL
                        )
                        debugConfig.log(TAG, "grpcLogin success")
                        return@withContext result
                    }
                    is Result.Error -> {
                        val ex = result.exception
                        if (ex is StatusException){
                            if (ex.status.code == Status.UNAUTHENTICATED.code){
                                val nonce = ex.status.description
                                debugConfig.log(TAG, "Grpc UNAUTHENTICATED - nonce := ${nonce}")
                                nonce?.let {
                                    if (!it.equals(account.nonce)) {
                                        debugConfig.log(TAG, "Update new nonce!!! nonce := ${nonce}")

                                        val nonceRespone = StringUtil.getNonceResponse(it, account.password)

                                        accountDAO.updateNonce(
                                            id = account.id,
                                            status = AccountLoginStatus.LOGOUT.code,
                                            nonce = it,
                                            nonceRespone = nonceRespone,
                                            nonceAt = kronosClock.getCurrentTimeMs()
                                        )

                                        return@withContext result
                                    }
                                }
                            }
                            else if (ex.status.code == Status.NOT_FOUND.code){
                                debugConfig.log(TAG, "Not Found User!!!")
                                accountDAO.changeActiveState(
                                    id = account.id,
                                    isActive = false,
                                    status = AccountLoginStatus.LOGOUT.code
                                )
                                return@withContext result
                            }
                        }
                        debugConfig.log(TAG, "grpcLogin Err:"+ex.toString())
                        accountDAO.setLoginStatus(
                            id = account.id,
                            status = AccountLoginStatus.LOGOUT.code,
                            lastAttempLoginAt = lastAttempLoginAt
                        )
                    }
                }

                return@withContext result
            }catch (e: Throwable){
                e.printStackTrace()
                return@withContext Result.Error(e)
            }finally {
                debugConfig.log(TAG, "release loginSemaphore")
                loginSemaphore.release()
            }

        }


    }


    suspend fun createAcc(countryCode: String, phone: String): Result<grpcCWMPb.CwmRqResAccount.CreatAccountResponse>{
        return withContext(ioDispatcher){
            debugConfig.log(TAG, "Call createAcc")

            val phoneFull = PhoneUtil.getPhoneFull(countryCode, phone)
            val account = accountDAO.getByPhoneFull(phoneFull)
            account?.let {
                accountDAO.changeActiveState(
                    id = it.id,
                    isActive = false,
                    status = AccountLoginStatus.LOGOUT.code
                )
            }

            val result = accountGrpcDataSource.createAcc(countryCode, phone)
            return@withContext result
        }
    }

    suspend fun verifyAuthencode(countryCode: String, phone: String, authenCode: String): Result<grpcCWMPb.CwmRqResAccount.VerifyAuthencodeResponse>{
        return withContext(ioDispatcher){
            debugConfig.log(TAG, "Call verifyAuthencode")

            val imei = Settings.Secure.getString(applicationContext.getContentResolver(), Settings.Secure.ANDROID_ID)

            val deviceInfo = CwmModel.DeviceInfo.newBuilder()
                .setDeviceName(Build.MODEL)
                .setImei(imei)
                .setManufacturer(Build.MANUFACTURER)
                .setOs(CwmModel.OS_TYPE.ANDROID)
                .setOsVersion(Build.VERSION.SDK_INT.toString())
                .build()

            val result = accountGrpcDataSource.verifyAuthencode(countryCode, phone, deviceInfo, authenCode)

            if (result is Result.Success){
                val response = result.data

                val authencodeSHA256 = authenCode.sha256()
                val iv = response.iv
                val passEnc = response.passEnc
                val encryptionAESByte = EncryptionAESByte()
                val pass = encryptionAESByte.decrypt(authencodeSHA256, passEnc.toByteArray(), iv.toByteArray())

                if (pass == null){
                    debugConfig.log("Cannot decrypt pass!!!!!!!!!")
                    return@withContext Result.Error(Exception("Cannot decrypt pass"))
                }
                val password = String(pass)
//                debugConfig.log("password: ${password} !!!!!!!!!")

                val nonce = response.nonce
                val nonceRespone = StringUtil.getNonceResponse(nonce, password)

                val now = kronosClock.getCurrentTimeMs()
                val phoneFull = PhoneUtil.getPhoneFull(countryCode, phone)

                var account = accountDAO.getByPhoneFull(phoneFull)
                account?.also { acc ->
                    debugConfig.log("SAVE EXISTED ACC!!!!!!!!!")

                    acc.password = password
                    acc.imei = imei
                    acc.sessionId = response.deviceInfo.sessionId
                    acc.nonce = response.nonce
                    acc.nonceResponse = nonceRespone
                    acc.nonceAt = now
                    acc.jwt = ""
                    acc.jwtAt = 0
                    acc.jwtTTL = 0
                    acc.status = AccountLoginStatus.LOGOUT.code
                    acc.lastAttempLoginAt = 0
                    acc.isActive = true
                    acc.username = response.userName
                    acc.firstName = response.firstName
                    acc.lastName = response.lastName

                    accountDAO.update(acc)
                    accountDAO.setAllInActiveExcept(acc.id)
                } ?: run {
                    debugConfig.log("INSERT NEW ACC!!!!!!!!!")

                    account = Account(
                        phoneFull = phoneFull,
                        phone = phone,
                        countryCode = countryCode,
                        username = response.userName,
                        firstName = response.firstName,
                        lastName = response.lastName,
                        password = password,
                        imei = imei,
                        sessionId = response.deviceInfo.sessionId,
                        nonce = nonce,
                        nonceResponse = nonceRespone,
                        nonceAt = now,
                        jwt = "",
                        jwtAt = 0,
                        jwtTTL = 0,
                        status = AccountLoginStatus.LOGOUT.code,
                        isActive = true,
                        createdAt = now,
                        lastAttempLoginAt = 0
                    )
                    val id = accountDAO.insertAccount(account!!)
                    accountDAO.setAllInActiveExcept(id)
                }
            }

            // => Login Activity wait for Active Account -> go to Home
            return@withContext result
        }
    }

    suspend fun updateProfile(firstName: String, lastName: String) : Result<grpcCWMPb.CwmRqResAccount.UpdateProfileResponse>{
        return withContext(ioDispatcher){
            debugConfig.log(TAG, "Call updateProfile")
            getActiveAccount()?.let{ account ->
                if (!account.isLogin()){
                    return@withContext Result.Error(Exception("Not Login Acount!"))
                }
                try{
                    val result = accountGrpcDataSource.updateProfile(account, firstName, lastName)
                    when (result) {
                        is Result.Success<grpcCWMPb.CwmRqResAccount.UpdateProfileResponse> -> {
                            val updateProfileResponse = result.data
                            accountDAO.updateProfile(account.id, updateProfileResponse.firstName, updateProfileResponse.lastName)
                        }
                        is Result.Error -> {
                            debugConfig.log(TAG, "Failed: ${result.exception.toString()}")
                        }
                    }
                    checkAndHandleSessionExpired(result, account)


                    return@withContext result
                }catch (e: Throwable){
                    return@withContext Result.Error(e)
                }
            }

            return@withContext Result.Error(Exception("Invalid Active Acount!"))
        }
    }


    suspend fun updateUserName(userName: String) : Result<grpcCWMPb.CwmRqResAccount.UpdateUsernameResponse>{
        return withContext(ioDispatcher){
            debugConfig.log(TAG, "Call updateUserName")
            getActiveAccount()?.let{ account ->
                if (!account.isLogin()){
                    return@withContext Result.Error(Exception("Not Login Acount!"))
                }
                try{
                    val result = accountGrpcDataSource.updateUsername(account, userName)
                    when (result) {
                        is Result.Success<grpcCWMPb.CwmRqResAccount.UpdateUsernameResponse> -> {
                            val updateProfileResponse = result.data
                            accountDAO.updateUserName(account.id, updateProfileResponse.userName)
                        }
                        is Result.Error -> {
                            debugConfig.log(TAG, "Failed: ${result.exception.toString()}")
                        }
                    }
                    checkAndHandleSessionExpired(result, account)

                    return@withContext result
                }catch (e: Throwable){
                    return@withContext Result.Error(e)
                }
            }

            return@withContext Result.Error(Exception("Invalid Active Acount!"))
        }
    }

    suspend fun updateFCMPushToken(fcmToken: String) : Result<CwmRqResAccount.UpdatePushTokenResponse>{
        return withContext(ioDispatcher){
//            debugConfig.log(TAG, "Call updateFCMPushToken")
            getActiveAccount()?.let{ account ->
                if (!account.isLogin()){
                    return@withContext Result.Error(Exception("Not Login Acount!"))
                }
                try{
                    val pushTokenInfo = CwmModel.PushTokenInfo.newBuilder()
                                                .setPushTokenServiceType(CwmModel.PUSH_TOKEN_SERVICE_TYPE.FCM)
                                                .setPushtokenID(fcmToken)
                                                .setAppid(BuildConfig.APPLICATION_ID)
                                                .build()

                    val result = accountGrpcDataSource.updateFCMPushToken(account, pushTokenInfo)


                    when (result) {
                        is Result.Success<CwmRqResAccount.UpdatePushTokenResponse> -> {
                            myPreference.setFCMToken(fcmToken)
                        }
                        is Result.Error -> {
//                            debugConfig.log(TAG, "Failed: ${result.exception.toString()}")
                        }
                    }


                    checkAndHandleSessionExpired(result, account)

                    return@withContext result
                }catch (e: Throwable){
                    return@withContext Result.Error(e)
                }
            }

            return@withContext Result.Error(Exception("Invalid Active Acount!"))
        }
    }

    //region worker
    fun startWorkerSendFCMToken(fcmToken: String){
        debugConfig.log(TAG, "call startWorkerSendFCMToken")

        val request = OneTimeWorkRequestBuilder<WorkerSendFCMToken>()
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .setInputData(
                workDataOf(
                    WorkerSendFCMToken.INPUT_FCM_TOKEN to fcmToken,
                )
            )
            .addTag("WorkerSendFCMToken")
            .build()

        WorkManager.getInstance(applicationContext)
            .enqueueUniqueWork(
                "WorkerSendFCMToken",
                ExistingWorkPolicy.APPEND_OR_REPLACE,
                request
            )
    }

//    fun startWorkerLogin(newNonce: String?){
//        debugConfig.log(TAG, "call startWorkerLogin")
//
//        val request = OneTimeWorkRequestBuilder<WorkerLogin>()
//            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
//            .setInputData(
//                workDataOf(
//                    WorkerLogin.INPUT_NEW_NONCE to newNonce,
//                )
//            )
//            .addTag("WorkerLogin")
//            .build()
//
//        WorkManager.getInstance(applicationContext)
//            .enqueueUniqueWork(
//                "WorkerLogin",
//                ExistingWorkPolicy.KEEP,
//                request
//            )
//
//
//    }

    //endregion


    //region testing
    suspend fun testHTTPApi(): Result<List<TestPlan>> {
        return withContext(ioDispatcher){
            accountHttpDataSource.testApi()
        }
    }

//    fun getAppCoroutineScope(): Int{
//        return appCoroutineScope.hashCode()
//    }
//
//    fun getIODispatcher(): Int{
//        return ioDispatcher.hashCode()
//    }
    //endregion
}