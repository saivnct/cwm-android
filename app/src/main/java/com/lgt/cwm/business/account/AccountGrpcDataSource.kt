package com.lgt.cwm.business.account

import android.content.Context
import com.lgt.cwm.db.entity.Account
import com.lgt.cwm.grpc.GrpcUtils
import com.lgt.cwm.util.Config
import com.lgt.cwm.util.DebugConfig
import com.lgt.cwm.util.Result
import dagger.hilt.android.qualifiers.ApplicationContext
import grpcCWMPb.CWMServiceGrpcKt
import grpcCWMPb.CwmModel
import grpcCWMPb.CwmRqResAccount
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton


/**
 * Created by giangtpu on 6/29/22.
 */
@Singleton
class AccountGrpcDataSource @Inject constructor(
    @ApplicationContext private val applicationContext: Context,
    private val debugConfig: DebugConfig
){
    private val TAG = AccountGrpcDataSource::class.simpleName.toString()

    suspend fun login(phoneFull: String, sessionId: String, nonce: String, nonceResponse: String): Result<CwmRqResAccount.LoginResponse> {
        var result : Result<CwmRqResAccount.LoginResponse>? = null
        val channel = GrpcUtils.getChannel(applicationContext)
            ?: return Result.Error(Exception("Cannot create grpc channel"))

        withTimeoutOrNull(Config.GRPC.TIMEOUT*1000){
            result = try {

                val cwmStub = CWMServiceGrpcKt.CWMServiceCoroutineStub(channel)

                val request = CwmRqResAccount.LoginRequest.newBuilder()
                    .setPhoneFull(phoneFull)
                    .setSessionId(sessionId)
                    .setNonce(nonce)
                    .setResponse(nonceResponse)
                    .build()


//                debugConfig.log(TAG, "call LoginReQuest")
                val response = cwmStub.login(request)
//                debugConfig.log(TAG, "call LoginReQuest done")

                Result.Success(response)

            }catch (ex: Throwable){
            ex.printStackTrace()
                Result.Error(ex)
            }finally {
                channel.shutdown()
            }
        }
        if (result == null) {
            if (!channel.isShutdown){
                channel.shutdown()
            }
            result = Result.Error(Exception("Timeout grpc call"))
        }

        return result!!

    }

    suspend fun createAcc(countryCode: String, phone: String): Result<CwmRqResAccount.CreatAccountResponse>{
        val channel = GrpcUtils.getChannel(applicationContext)
            ?: return Result.Error(Exception("Cannot create grpc channel"))
        return try {
            val cwmStub = CWMServiceGrpcKt.CWMServiceCoroutineStub(channel)
            val request = CwmRqResAccount.CreatAccountRequest.newBuilder()
                .setCountryCode(countryCode)
                .setPhone(phone)
                .build()

//            debugConfig.log(TAG, "call creatUser")
            val response = cwmStub.creatUser(request)
//            debugConfig.log(TAG, "call creatUser done")

            Result.Success(response)

        }catch (ex: Throwable){
            ex.printStackTrace()
            Result.Error(ex)
        }finally {
            channel.shutdown()
        }
    }

    suspend fun verifyAuthencode(countryCode: String, phone: String, deviceInfo: CwmModel.DeviceInfo, authenCode: String): Result<CwmRqResAccount.VerifyAuthencodeResponse>{
        val channel = GrpcUtils.getChannel(applicationContext)
            ?: return Result.Error(Exception("Cannot create grpc channel"))
        return try {
            val cwmStub = CWMServiceGrpcKt.CWMServiceCoroutineStub(channel)
            val request = CwmRqResAccount.VerifyAuthencodeRequest.newBuilder()
                .setCountryCode(countryCode)
                .setPhone(phone)
                .setDeviceInfo(deviceInfo)
                .setAuthencode(authenCode)
                .build()

//            debugConfig.log(TAG, "call verifyAuthencode")
            val response = cwmStub.verifyAuthencode(request)
//            debugConfig.log(TAG, "call verifyAuthencode done")

            Result.Success(response)

        }catch (ex: Throwable){
            ex.printStackTrace()
            Result.Error(ex)
        }finally {
            channel.shutdown()
        }
    }

    suspend fun updateProfile(account: Account, firstName: String, lastName: String) : Result<CwmRqResAccount.UpdateProfileResponse>{
        val channel = GrpcUtils.getChannel(applicationContext, account)
            ?: return Result.Error(Exception("Cannot create grpc channel"))
        return try {
            val cwmStub = CWMServiceGrpcKt.CWMServiceCoroutineStub(channel)
            val request = CwmRqResAccount.UpdateProfileRequest.newBuilder()
                .setFirstName(firstName)
                .setLastName(lastName)
                .build()

//            debugConfig.log(TAG, "call updateProfile")
            val response = cwmStub.updateProfile(request)
//            debugConfig.log(TAG, "call updateProfile done")

            Result.Success(response)

        }catch (ex: Throwable){
            ex.printStackTrace()
            Result.Error(ex)
        }finally {
            channel.shutdown()
        }
    }

    suspend fun updateUsername(account: Account, userName: String) : Result<CwmRqResAccount.UpdateUsernameResponse>{
        val channel = GrpcUtils.getChannel(applicationContext, account)
            ?: return Result.Error(Exception("Cannot create grpc channel"))
        return try {
            val cwmStub = CWMServiceGrpcKt.CWMServiceCoroutineStub(channel)
            val request = CwmRqResAccount.UpdateUsernameRequest.newBuilder()
                .setUserName(userName)
                .build()

//            debugConfig.log(TAG, "call updateUsername")
            val response = cwmStub.updateUsername(request)
//            debugConfig.log(TAG, "call updateUsername done")

            Result.Success(response)

        }catch (ex: Throwable){
            ex.printStackTrace()
            Result.Error(ex)
        }finally {
            channel.shutdown()
        }
    }


    suspend fun updateFCMPushToken(account: Account, pushTokenInfo: CwmModel.PushTokenInfo) : Result<CwmRqResAccount.UpdatePushTokenResponse>{
        val channel = GrpcUtils.getChannel(applicationContext, account)
            ?: return Result.Error(Exception("Cannot create grpc channel"))
        return try {
            val cwmStub = CWMServiceGrpcKt.CWMServiceCoroutineStub(channel)
            val request = CwmRqResAccount.UpdatePushTokenRequest.newBuilder()
                .setPushTokenInfo(pushTokenInfo)
                .build()

//            debugConfig.log(TAG, "call updateFCMPushToken")
            val response = cwmStub.updatePushToken(request)
//            debugConfig.log(TAG, "call updateFCMPushToken done")

            Result.Success(response)

        }catch (ex: Throwable){
            ex.printStackTrace()
            Result.Error(ex)
        }finally {
            channel.shutdown()
        }
    }


}