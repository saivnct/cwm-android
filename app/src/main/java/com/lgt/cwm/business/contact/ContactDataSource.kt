package com.lgt.cwm.business.contact

import android.content.Context
import android.database.Cursor
import android.provider.ContactsContract
import com.google.common.base.Predicate
import com.google.common.collect.Iterables
import com.lgt.cwm.db.entity.Contact
import com.lgt.cwm.models.NationalPhoneCode
import com.lgt.cwm.models.SyncContactData
import com.lgt.cwm.util.Constants.MIN_LENGTH_PHONE_NUMBER
import com.lgt.cwm.util.DateUtil
import com.lgt.cwm.util.DebugConfig
import com.lgt.cwm.util.getStandardizedPhoneNumber
import com.lgt.cwm.util.sha256String
import com.lyft.kronos.KronosClock
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Created by giangtpu on 7/18/22.
 */
@Singleton
class ContactDataSource @Inject constructor(
    @ApplicationContext private val applicationContext: Context,
    private val kronosClock: KronosClock,
    private val debugConfig: DebugConfig
){
    private val TAG = ContactDataSource::class.simpleName.toString()



    suspend fun syncContactsWithDevice(nationalPhoneCode: NationalPhoneCode, listContactDBAll: List<Contact>): SyncContactData {

        val listContactAdd: MutableList<Contact> = ArrayList<Contact>()
        val listContactUpdate: MutableList<Contact> = ArrayList<Contact>()
        val listContactRemove: MutableList<Contact> = ArrayList<Contact>()


        val listContactDevice = fetchPhoneNumberFromDevice(nationalPhoneCode)


        for (contactDevice in listContactDevice){
            val contactDB = listContactDBAll.find { it -> it.id.equals(contactDevice.id) }


            contactDB?.also { contactDB ->
                if (!contactDB.equals(contactDevice)) {
                    listContactUpdate.add(contactDevice)
                }
            }?: run{
                listContactAdd.add(contactDevice)
            }
        }

        for (contactDB in listContactDBAll){
            val contactDevice = listContactDevice.find { it -> it.id.equals(contactDB.id) }
            if (contactDevice == null) {
                listContactRemove.add(contactDB)
            }
        }
        val listContactAll: MutableList<Contact> = ArrayList<Contact>()
        listContactAll.addAll(listContactDevice)
        listContactAll.addAll(listContactRemove)


//        for (contact in listContactDBAll){
//            debugConfig.log(TAG,contact.toString())
//        }
//
//        debugConfig.log(TAG,"-----------------------------------------------------------------")
//
//        for (contact in listContactAdd){
//            debugConfig.log(TAG,contact.toString())
//        }



//        debugConfig.log(TAG,"listContactDevice: ${listContactDevice.size}")
//        debugConfig.log(TAG,"listContactDBAll: ${listContactDBAll.size}")
//        debugConfig.log(TAG,"listContactAll: ${listContactAll.size}")

        return SyncContactData(
            listContactAll = listContactAll,
            listContactAdd = listContactAdd,
            listContactUpdate = listContactUpdate,
            listContactRemove = listContactRemove,
        )
    }

    suspend fun fetchPhoneNumberFromDevice(nationalPhoneCode: NationalPhoneCode): List<Contact>{
        val listContacts: MutableList<Contact> = ArrayList<Contact>()
        var contactcursor: Cursor? = null
        try {
            contactcursor = applicationContext.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone.NUMBER,
                    ContactsContract.CommonDataKinds.Phone.RAW_CONTACT_ID,  //http://android-contact-id-vs-raw-contact-id.blogspot.com/2011/04/android-contacts-contactid-vs.html
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.PHOTO_URI
                ),
                null,
                null,
                null
            )

            contactcursor?.let { contactcursor ->
//                debugConfig.log(TAG,"phonebook has ${contactcursor.count} contacts")

                while (contactcursor.moveToNext()) {
                    // get number
                    val contactInfo = parseContactFromCursor(contactcursor, nationalPhoneCode) ?: continue

                    //HANDLE - 1 contact multi numbers
                    var phonecursor: Cursor? = null
                    try {
                        phonecursor = applicationContext.contentResolver.query(
                            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                            arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
                            ContactsContract.CommonDataKinds.Phone.RAW_CONTACT_ID +" = " + contactInfo.contactId,
                            null,
                            null
                        )

                        phonecursor?.let { phonecursor ->


                            var numbers = ""
                            while (phonecursor.moveToNext()){
                                val number: String =
                                    phonecursor.getString(contactcursor.getColumnIndex(
                                        ContactsContract.CommonDataKinds.Phone.NUMBER))

                                val standardizedPhoneNumber = number.getStandardizedPhoneNumber(nationalPhoneCode)
                                if (standardizedPhoneNumber.length > MIN_LENGTH_PHONE_NUMBER){
                                    val id = (standardizedPhoneNumber+contactInfo.name).sha256String()
                                    val contact = Contact(
                                        id = id,
                                        contactId = contactInfo.contactId,
                                        name = contactInfo.name,
                                        number = number,
                                        standardizedPhoneNumber = standardizedPhoneNumber,
                                        photoUri = contactInfo.photoUri,
                                        userId = null,
                                        username = null,
                                        avatar = null,
                                        svFirtname = null,
                                        svLastname = null,
                                        createdAt = kronosClock.getCurrentTimeMs()
                                    )
                                    listContacts.add(contact)

                                    numbers += standardizedPhoneNumber + "-"
                                }
                            }

//                            debugConfig.log(TAG,"${contactInfo.contactId}, ${contactInfo.name} has ${phonecursor.count} numbers: ${numbers}")

                        }


                    }catch (e: Exception) {
                        e.printStackTrace()
                    } finally {
                        phonecursor?.close()
                    }

                }

            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            contactcursor?.close()
        }

        //remove Duplicate Contact Read From Device
        val listValidContacts: MutableList<Contact> = ArrayList<Contact>()
        for (contact in listContacts) {
            val matchingContact = listValidContacts.find { it -> it.id.equals(contact.id) }
            if (matchingContact == null) {
                listValidContacts.add(contact)
            }
        }

        return listValidContacts
    }

    suspend fun parseContactFromCursor(cursor: Cursor, nationalPhoneCode: NationalPhoneCode): Contact? {

        val contactId: String =
            cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.RAW_CONTACT_ID))

        val name: String =
            cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME))

        val photoUri: String? =
            cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_URI))


        return Contact(
            id = "",
            contactId = contactId,
            name = name,
            number = "",
            standardizedPhoneNumber = "",
            photoUri = photoUri,
            userId = null,
            username = null,
            avatar = null,
            svFirtname = null,
            svLastname = null,
            createdAt = kronosClock.getCurrentTimeMs()
        )
    }

}