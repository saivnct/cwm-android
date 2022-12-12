package com.lgt.cwm.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Created by giangtpu on 7/18/22.
 */
@Entity(tableName = "contacts")
data class Contact (
    @PrimaryKey(autoGenerate = false)
    var id: String,     //sha256(standardizedPhoneNumber+username)
    var contactId: String,  //id in phonebook
    var name: String,
    var number: String,
    var standardizedPhoneNumber: String,
    var isOTT: Boolean = false,
    var photoUri: String?,
    var userId: String?,
    var username: String?,
    var avatar: String?,
    var svFirtname: String?,
    var svLastname: String?,
    var createdAt: Long,
){

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Contact

        val photoUri = photoUri ?: ""
        val otherPhotoUri = other.photoUri ?: ""

        return contactId.equals(other.contactId) &&
                name.equals(other.name) &&
                standardizedPhoneNumber.equals(other.standardizedPhoneNumber) &&
                photoUri.equals(otherPhotoUri)
    }

    override fun toString(): String {
        return "Contact(id='$id', contactId='$contactId', name='$name', standardizedPhoneNumber='$standardizedPhoneNumber', photoUri=$photoUri)"
    }

    fun getNameHeader(): String {
        return name.first().toString()
    }


}
