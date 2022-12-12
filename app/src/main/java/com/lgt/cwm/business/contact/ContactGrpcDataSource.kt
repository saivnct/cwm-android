package com.lgt.cwm.business.contact

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
class ContactGrpcDataSource @Inject constructor(
    @ApplicationContext private val applicationContext: Context,
    private val debugConfig: DebugConfig
){
    private val TAG = ContactGrpcDataSource::class.simpleName.toString()

    private fun generateSyncContactRequestFlow(syncContactData: SyncContactData): Flow<CwmRqResAccount.SyncContactRequest> = flow{
        for (contact in syncContactData.listContactAdd){
            val contactInfo = CwmModel.ContactInfo.newBuilder()
                .setContactId(contact.id)
                .setName(contact.name)
                .setPhoneFull(contact.standardizedPhoneNumber)
                .setSyncType(CwmModel.CONTACT_SYNC_TYPE.ADD)
                .build()

            val request = CwmRqResAccount.SyncContactRequest.newBuilder()
                .setContactInfo(contactInfo)
                .build()

            emit(request)
        }

        for (contact in syncContactData.listContactUpdate){
            val contactInfo = CwmModel.ContactInfo.newBuilder()
                .setContactId(contact.id)
                .setName(contact.name)
                .setPhoneFull(contact.standardizedPhoneNumber)
                .setSyncType(CwmModel.CONTACT_SYNC_TYPE.UPDATE)
                .build()

            val request = CwmRqResAccount.SyncContactRequest.newBuilder()
                .setContactInfo(contactInfo)
                .build()

            emit(request)
        }

        for (contact in syncContactData.listContactRemove){
            val contactInfo = CwmModel.ContactInfo.newBuilder()
                .setContactId(contact.id)
                .setName(contact.name)
                .setPhoneFull(contact.standardizedPhoneNumber)
                .setSyncType(CwmModel.CONTACT_SYNC_TYPE.REMOVE)
                .build()

            val request = CwmRqResAccount.SyncContactRequest.newBuilder()
                .setContactInfo(contactInfo)
                .build()

            emit(request)
        }
    }

    suspend fun syncContactStream(account: Account, syncContactData: SyncContactData, onSyncContactStreamItem: OnSyncContactStreamItem) : Result<Boolean>{
        val channel = GrpcUtils.getChannel(applicationContext, account)
            ?: return Result.Error(Exception("Cannot create grpc channel"))

        try {
            val cwmStub = CWMServiceGrpcKt.CWMServiceCoroutineStub(channel)

            cwmStub.syncContact(generateSyncContactRequestFlow(syncContactData))
                .catch { exception ->
                    debugConfig.log(TAG, "syncContactStream error at flow!!!!!!!!")
                    exception.printStackTrace()
                }
                .collect{ syncContactResponse ->
                    val contactInfo = syncContactResponse.contactInfo
                    contactInfo?.let { contactInfo ->
                        val contact = syncContactData.listContactAll.find { it -> it.id.equals(contactInfo.contactId) }
                        contact?.let { contact ->
                            contact.userId = contactInfo.userId
                            contact.username = contactInfo.username
                            contact.avatar = contactInfo.userAvatar
                            contact.svFirtname = contactInfo.firstName
                            contact.svLastname = contactInfo.lastName
                            contact.isOTT = !contact.userId.isNullOrEmpty()

                            onSyncContactStreamItem.handleSyncContactInfo(contact, contactInfo)
                        }
                    }
                }

            return  Result.Success(true)
        }catch (ex: Throwable){
            return Result.Error(ex)
        }finally {
            channel.shutdown()
        }
    }

    interface OnSyncContactStreamItem {
        fun handleSyncContactInfo(contact: Contact, contactSyncInfo: CwmModel.ContactInfo)
    }



}