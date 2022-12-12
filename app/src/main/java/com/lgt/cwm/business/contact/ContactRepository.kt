package com.lgt.cwm.business.contact

import android.content.Context
import androidx.work.*
import com.lgt.cwm.business.account.AccountRepository
import com.lgt.cwm.db.dao.ContactDao
import com.lgt.cwm.db.entity.Contact
import com.lgt.cwm.di.AppCoroutineScope
import com.lgt.cwm.di.IODispatcher
import com.lgt.cwm.util.DebugConfig
import com.lgt.cwm.util.PhoneUtil
import com.lgt.cwm.util.Result
import dagger.hilt.android.qualifiers.ApplicationContext
import grpcCWMPb.CwmModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Created by giangtpu on 7/18/22.
 */
@Singleton
class ContactRepository @Inject constructor(
    @ApplicationContext private val applicationContext: Context,
    @AppCoroutineScope private val appCoroutineScope: CoroutineScope,
    @IODispatcher private val ioDispatcher: CoroutineDispatcher,
    private val accountRepository: AccountRepository,
    private val contactDataSource: ContactDataSource,
    private val contactGrpcDataSource: ContactGrpcDataSource,
    private val contactDao: ContactDao,
    private val debugConfig: DebugConfig,
){
    private val TAG = ContactRepository::class.simpleName.toString()

    val allContactOTTFlow: Flow<List<Contact>> = contactDao.getAllOTTFlowDistinctUntilChanged()

    fun getByIdFlow(id: String) = contactDao.getByIdFlowDistinctUntilChanged(id)
    fun getAllByPhoneFull(phoneFull: String) = contactDao.getAllByPhoneFull(phoneFull)
    fun updateListContact(contacts: List<Contact>) = contactDao.updateListContact(contacts)

    suspend fun findContactById(id: String) : Contact?{
        return withContext(ioDispatcher) {
            return@withContext contactDao.getById(id)
        }
    }

    suspend fun findOneContactByPhoneFull(phoneFull: String) : Contact?{
        return withContext(ioDispatcher) {
            return@withContext contactDao.getOneByPhoneFull(phoneFull)
        }
    }

    //region worker
    fun startWorkerSyncContact(){
        debugConfig.log(TAG, "call startWorkerSyncContact")

        val request = OneTimeWorkRequestBuilder<WorkerSyncContact>()
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .addTag("WorkerSyncContact")
            .build()

        WorkManager.getInstance(applicationContext)
            .enqueueUniqueWork(
                "WorkerSyncContact",
                ExistingWorkPolicy.KEEP,
                request
            )
    }
    //endregion

    //api
    suspend fun syncContact(): Result<Boolean> {
//        debugConfig.log(TAG,"start syncContact")

        return withContext(ioDispatcher){
            val acc = accountRepository.getActiveAccount()
            if (acc == null){
                return@withContext Result.Error(Exception("Invalid Active Acount!"))
            }

            if (!acc.isLogin()){
                return@withContext Result.Error(Exception("Not Login Acount!"))
            }

            try {
                val nationalPhoneCode = PhoneUtil.getNationalPhoneCodeFromCountryCode(acc.countryCode, applicationContext)
                val listContactAll = contactDao.getAll()

                val syncContactData = contactDataSource.syncContactsWithDevice(nationalPhoneCode, listContactAll)

                debugConfig.log(TAG, "syncContact device - all ${syncContactData.listContactAll.size} - add ${syncContactData.listContactAdd.size} - remove ${syncContactData.listContactRemove.size} - update ${syncContactData.listContactUpdate.size}")

                if (syncContactData.listContactAdd.size > 0 ||
                    syncContactData.listContactUpdate.size > 0 ||
                    syncContactData.listContactRemove.size > 0)
                {

                    val syncContactResult = contactGrpcDataSource.syncContactStream(acc, syncContactData, object : ContactGrpcDataSource.OnSyncContactStreamItem{
                        override fun handleSyncContactInfo(contact: Contact, contactSyncInfo: CwmModel.ContactInfo) {
//                                debugConfig.log(TAG, "OnSyncContactStreamItem - handleSyncContactInfo")
                            when (contactSyncInfo.syncType) {
                                CwmModel.CONTACT_SYNC_TYPE.ADD -> {
                                    contactDao.insertContact(contact)
                                }
                                CwmModel.CONTACT_SYNC_TYPE.UPDATE -> {
                                    contactDao.updateContact(contact)
                                }
                                CwmModel.CONTACT_SYNC_TYPE.REMOVE -> {
                                    contactDao.deleteContact(contact.id)
                                }
                                else -> {}
                            }
                        }
                    })
                    accountRepository.checkAndHandleSessionExpired(syncContactResult, acc)

                    return@withContext syncContactResult
                }
                //TODO - HANDLE SESSION EXPIRED

                return@withContext Result.Success(true)

            }catch (e: Throwable){
                return@withContext Result.Error(e)
            }


        }
    }


}