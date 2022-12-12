package com.lgt.cwm.db.dao

import androidx.room.*
import com.lgt.cwm.db.entity.Contact
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Created by giangtpu on 7/19/22.
 */
@Dao
interface ContactDao {
    @Query("SELECT * FROM contacts WHERE isOTT = 1 ORDER BY name DESC")
    fun getAllOTTFlow(): Flow<List<Contact>>
    fun getAllOTTFlowDistinctUntilChanged() =
        getAllOTTFlow().distinctUntilChanged()



    @Query("SELECT * FROM contacts ORDER BY name DESC")
    fun getAllFlow(): Flow<List<Contact>>

    @Query("SELECT * FROM contacts ORDER BY name DESC")
    fun getAll(): List<Contact>

    @Query("SELECT COUNT(id) FROM contacts")
    fun getAllCount(): Long

    @Query("SELECT * FROM contacts WHERE id = :id")
    fun getById(id: String): Contact?

    @Query("SELECT * FROM contacts WHERE id = :id")
    fun getByIdFlow(id: String): Flow<Contact?>
    fun getByIdFlowDistinctUntilChanged(id: String) =
        getByIdFlow(id).distinctUntilChanged()


    @Query("SELECT * FROM contacts WHERE standardizedPhoneNumber = :phoneFull LIMIT 1")
    fun getOneByPhoneFull(phoneFull: String): Contact?

    @Query("SELECT * FROM contacts WHERE standardizedPhoneNumber = :phoneFull")
    fun getAllByPhoneFull(phoneFull: String): List<Contact>



    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertContact(contact: Contact) : Long

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun updateContact(contact: Contact)

    @Query("DELETE FROM contacts WHERE id = :id")
    fun deleteContact(id: String)


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertListContact(contacts: List<Contact>) : List<Long>

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun updateListContact(contacts: List<Contact>)

    @Query("DELETE FROM contacts WHERE id in (:idList)")
    fun deleteListContact(idList: List<String>)
}