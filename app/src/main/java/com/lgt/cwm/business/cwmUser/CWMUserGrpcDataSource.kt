package com.lgt.cwm.business.cwmUser

import android.content.Context
import com.lgt.cwm.db.entity.Account
import com.lgt.cwm.db.entity.Contact
import com.lgt.cwm.grpc.GrpcUtils
import com.lgt.cwm.models.SyncContactData
import com.lgt.cwm.util.DebugConfig
import com.lgt.cwm.util.Result
import dagger.hilt.android.qualifiers.ApplicationContext
import grpcCWMPb.CWMServiceGrpcKt
import grpcCWMPb.CwmModel
import grpcCWMPb.CwmRqResAccount
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Created by giangtpu on 7/18/22.
 */
@Singleton
class CWMUserGrpcDataSource @Inject constructor(
    @ApplicationContext private val applicationContext: Context,
    private val debugConfig: DebugConfig
){
    private val TAG = CWMUserGrpcDataSource::class.simpleName.toString()


    suspend fun searchByUsername(account: Account, userName: String) : Result<CwmRqResAccount.SearchByUsernameResponse>{
        val channel = GrpcUtils.getChannel(applicationContext, account)
            ?: return Result.Error(Exception("Cannot create grpc channel"))
        return try {
            val cwmStub = CWMServiceGrpcKt.CWMServiceCoroutineStub(channel)
            val request = CwmRqResAccount.SearchByUsernameRequest.newBuilder()
                .setUserName(userName)
                .build()

            debugConfig.log(TAG, "call searchByUsername")
            val response = cwmStub.searchByUsername(request)
            debugConfig.log(TAG, "call searchByUsername done")

            Result.Success(response)

        }catch (ex: Throwable){
            ex.printStackTrace()
            Result.Error(ex)
        }finally {
            channel.shutdown()
        }
    }

    suspend fun searchByPhoneFull(account: Account, phoneFull: String) : Result<CwmRqResAccount.SearchByPhoneFullResponse>{
        val channel = GrpcUtils.getChannel(applicationContext, account)
            ?: return Result.Error(Exception("Cannot create grpc channel"))
        return try {
            val cwmStub = CWMServiceGrpcKt.CWMServiceCoroutineStub(channel)
            val request = CwmRqResAccount.SearchByPhoneFullRequest.newBuilder()
                .setPhoneFull(phoneFull)
                .build()

            debugConfig.log(TAG, "call searchByPhoneFull")
            val response = cwmStub.searchByPhoneFull(request)
            debugConfig.log(TAG, "call searchByPhoneFull done")

            Result.Success(response)
        }catch (ex: Throwable){
            ex.printStackTrace()
            Result.Error(ex)
        }finally {
            channel.shutdown()
        }
    }

    suspend fun findByListPhoneFull(account: Account, listPhoneFull: List<String>) : Result<CwmRqResAccount.FindByListPhoneFullResponse>{
        val channel = GrpcUtils.getChannel(applicationContext, account)
            ?: return Result.Error(Exception("Cannot create grpc channel"))
        return try {
            val cwmStub = CWMServiceGrpcKt.CWMServiceCoroutineStub(channel)
            val request = CwmRqResAccount.FindByListPhoneFullRequest.newBuilder()
                .addAllPhoneFulls(listPhoneFull)
                .build()

            debugConfig.log(TAG, "call findByListPhoneFull")
            val response = cwmStub.findByListPhoneFull(request)
            debugConfig.log(TAG, "call findByListPhoneFull done")

            Result.Success(response)
        }catch (ex: Throwable){
            ex.printStackTrace()
            Result.Error(ex)
        }finally {
            channel.shutdown()
        }
    }



}