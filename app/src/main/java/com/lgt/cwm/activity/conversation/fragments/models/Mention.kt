package com.lgt.cwm.activity.conversation.fragments.models

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import java.util.*

data class Mention(val recipientId: String, val displayName: String, val start: Int, val length: Int)
//@Parcelize
//class Mention(val recipientId: String, val start: Int, val length: Int) : Comparable<Mention>, Parcelable {
//
//    override operator fun compareTo(other: Mention): Int {
//        return start.compareTo(other.start)
//    }
//
//    override fun hashCode(): Int {
//        return Objects.hash(recipientId, start, length)
//    }
//
//    override fun equals(other: Any?): Boolean {
//        if (this === other) return true
//        if (javaClass != other?.javaClass) return false
//
//        val that = other as Mention
//        return recipientId == that.recipientId && start == that.start && length == that.length
//    }
//}