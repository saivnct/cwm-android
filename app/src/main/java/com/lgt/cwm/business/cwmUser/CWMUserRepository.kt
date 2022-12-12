package com.lgt.cwm.business.cwmUser

import android.content.Context
import com.lgt.cwm.business.account.AccountRepository
import com.lgt.cwm.db.dao.CWMUserDao
import com.lgt.cwm.db.dao.ContactDao
import com.lgt.cwm.db.entity.Account
import com.lgt.cwm.db.entity.CWMUser
import com.lgt.cwm.di.AppCoroutineScope
import com.lgt.cwm.di.IODispatcher
import com.lgt.cwm.util.DebugConfig
import com.lgt.cwm.util.Result
import dagger.hilt.android.qualifiers.ApplicationContext
import grpcCWMPb.CwmRqResAccount
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Created by giangtpu on 04/10/2022
 */
@Singleton
class CWMUserRepository @Inject constructor(
    @ApplicationContext private val applicationContext: Context,
    @AppCoroutineScope private val appCoroutineScope: CoroutineScope,
    @IODispatcher private val ioDispatcher: CoroutineDispatcher,
    private val accountRepository: AccountRepository,
    private val cwmUserGrpcDataSource: CWMUserGrpcDataSource,
    private val cwmUserDao: CWMUserDao,
    private val contactDao: ContactDao,
    private val debugConfig: DebugConfig,
){
    private val TAG = CWMUserRepository::class.simpleName.toString()

    suspend fun handleParticipantInfosList(participantInfosList: List<grpcCWMPb.CwmModel.ThreadParticipantInfo>, currentAccount: Account){
        participantInfosList.forEach { participantInfo ->
            val cwmUser = CWMUser(
                phoneFull = participantInfo.phoneFull,
                isMyAcc = participantInfo.phoneFull.equals(currentAccount.phoneFull),
                userId = participantInfo.userId,
                username = participantInfo.username,
                avatar = participantInfo.userAvatar,
                firstName = participantInfo.firstName,
                lastName = participantInfo.lastName,
            )

            saveCWMUser(cwmUser)
        }
    }

    suspend fun handleSearchUserInfosList(searchUserInfosList: List<grpcCWMPb.CwmModel.SearchUserInfo>, currentAccount: Account){
        searchUserInfosList.forEach { searchUserInfo ->
            val cwmUser = CWMUser(
                phoneFull = searchUserInfo.phoneFull,
                isMyAcc = searchUserInfo.phoneFull.equals(currentAccount.phoneFull),
                userId = searchUserInfo.userId,
                username = searchUserInfo.username,
                avatar = searchUserInfo.userAvatar,
                firstName = searchUserInfo.firstName,
                lastName = searchUserInfo.lastName,
            )

            saveCWMUser(cwmUser)
        }
    }

    suspend fun saveCWMUser(cwmUser: CWMUser) {
        withContext(ioDispatcher) {
            appCoroutineScope.launch {
                try {
                    cwmUserDao.insert(cwmUser)
                }catch (e: Throwable){
                    e.printStackTrace()
                }
            }.join()
        }
    }

    suspend fun getByPhoneFull(phoneFull: String) : CWMUser?{
        return withContext(ioDispatcher) {
            return@withContext cwmUserDao.getByPhoneFull(phoneFull)
        }
    }

    suspend fun getAllByListPhoneFull(phoneFulls: List<String>) : List<CWMUser>{
        return withContext(ioDispatcher) {
            return@withContext cwmUserDao.getAllByListPhoneFull(phoneFulls)
        }
    }


    //api
    suspend fun searchByUsername(userName: String) : Result<CwmRqResAccount.SearchByUsernameResponse> {
        return withContext(ioDispatcher){

            accountRepository.getActiveAccount()?.let{ account ->
                if (!account.isLogin()){
                    return@withContext Result.Error(Exception("Not Login Acount!"))
                }

                try{
                    val result = cwmUserGrpcDataSource.searchByUsername(account, userName)
                    accountRepository.checkAndHandleSessionExpired(result, account)
                    return@withContext result

                }catch (e: Throwable){
                    return@withContext Result.Error(e)
                }
            }

            return@withContext Result.Error(Exception("Invalid Active Acount!"))
        }
    }
    //api
    suspend fun searchByPhoneFull(phoneFull: String) : Result<CwmRqResAccount.SearchByPhoneFullResponse> {
        return withContext(ioDispatcher){

            accountRepository.getActiveAccount()?.let{ account ->
                if (!account.isLogin()){
                    return@withContext Result.Error(Exception("Not Login Acount!"))
                }

                try{
                    val result = cwmUserGrpcDataSource.searchByPhoneFull(account, phoneFull)
                    accountRepository.checkAndHandleSessionExpired(result, account)
                    return@withContext result

                }catch (e: Throwable){
                    return@withContext Result.Error(e)
                }
            }

            return@withContext Result.Error(Exception("Invalid Active Acount!"))
        }
    }

    //api
    suspend fun findByListPhoneFull(listPhoneFull: List<String>) : Result<CwmRqResAccount.FindByListPhoneFullResponse> {
        return withContext(ioDispatcher){

            accountRepository.getActiveAccount()?.let{ account ->
                if (!account.isLogin()){
                    return@withContext Result.Error(Exception("Not Login Acount!"))
                }

                try{
                    val result = cwmUserGrpcDataSource.findByListPhoneFull(account, listPhoneFull)

                    when (result){
                        is Result.Success<CwmRqResAccount.FindByListPhoneFullResponse> -> {
                            handleSearchUserInfosList(result.data.searchUserInfosList,account)
                        }
                        is Result.Error -> {
                            debugConfig.log(TAG, "findByListPhoneFull Failed: ${result.exception.toString()}")
                        }
                    }

                    accountRepository.checkAndHandleSessionExpired(result, account)
                    return@withContext result

                }catch (e: Throwable){
                    return@withContext Result.Error(e)
                }
            }

            return@withContext Result.Error(Exception("Invalid Active Acount!"))
        }
    }
}