package com.lgt.cwm.db.dao

import androidx.room.*
import com.lgt.cwm.db.entity.SignalThread
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Created by giangtpu on 7/25/22.
 */
@Dao
interface SignalThreadDao {

    @Query("SELECT * FROM signalThreads WHERE verified = 1 ORDER BY lastModified DESC")
    fun getAllVerifiedThreadFlow(): Flow<List<SignalThread>>

    @Query("SELECT * FROM signalThreads ORDER BY lastModified DESC")
    fun getAll(): List<SignalThread>

    @Query("SELECT * FROM signalThreads WHERE threadId = :threadId")
    fun getByThreadId(threadId: String): SignalThread?

    @Query("SELECT * FROM signalThreads WHERE threadId = :threadId")
    fun getByThreadIdFlow(threadId: String): Flow<SignalThread?>
    fun getByThreadIdDistinctUntilChanged(threadId: String) =
        getByThreadIdFlow(threadId).distinctUntilChanged()


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(signalThread: SignalThread): Long

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun update(signalThread: SignalThread)

    @Query("DELETE FROM signalThreads WHERE threadId = :threadId")
    fun deleteSignalThread(threadId: String)

    @Query("UPDATE signalThreads SET unreadMsgs = :unreadMsgs WHERE threadId = :threadId")
    fun updateUnreadMsgs(threadId: String, unreadMsgs: Long): Int

    @Query("UPDATE signalThreads SET lastViewPos = :lastViewPos WHERE threadId = :threadId")
    fun updateLastViewPos(threadId: String, lastViewPos: Int): Int

    @Query("UPDATE signalThreads SET lastViewPos = 0, unreadMsgs = 0 WHERE threadId = :threadId")
    fun resetLastViewPos(threadId: String): Int


    @Query("UPDATE signalThreads SET lastMsgId = :lastMsgId, lastMsg = :lastMsg, lastMsgImType = :lastMsgImType, lastMsgStatus = :lastMsgStatus, lastMsgDate = :lastMsgDate, lastMsgServerDate = :lastMsgServerDate, unreadMsgs = :unreadMsgs, lastModified = :lastModified WHERE threadId = :threadId")
    fun updateLastMsgInfo(threadId: String, lastMsgId: String?, lastMsg: ByteArray?,
                          lastMsgImType: Int?,
                          lastMsgStatus: Int?, lastMsgDate: Long?, lastMsgServerDate: Long?, unreadMsgs: Long,
                          lastModified: Long): Int

    @Query("UPDATE signalThreads SET lastMsgId = :lastMsgId, lastMsg = :lastMsg, lastMsgImType = :lastMsgImType, lastMsgStatus = :lastMsgStatus, lastMsgDate = :lastMsgDate, lastModified = :lastModified WHERE threadId = :threadId")
    fun updateLastMsgInfo(threadId: String, lastMsgId: String?, lastMsg: ByteArray?,
                          lastMsgImType: Int?,
                          lastMsgStatus: Int?, lastMsgDate: Long?,
                          lastModified: Long): Int

    @Query("UPDATE signalThreads SET lastMsgStatus = :lastMsgStatus, lastMsgServerDate = :lastMsgServerDate, lastMsgDate = :lastMsgServerDate WHERE threadId = :threadId")
    fun updateLastMsgInfo(threadId: String, lastMsgStatus: Int?, lastMsgServerDate: Long?): Int

    @Query("UPDATE signalThreads SET lastMsgStatus = :lastMsgStatus WHERE threadId = :threadId")
    fun updateLastMsgInfo(threadId: String, lastMsgStatus: Int?): Int

    @Query("UPDATE signalThreads SET lastMsg = :lastMsg,lastMsgStatus = :lastMsgStatus WHERE threadId = :threadId")
    fun updateLastMsgInfo(threadId: String, lastMsg: ByteArray?, lastMsgStatus: Int?): Int

    @Query("UPDATE signalThreads SET lastMsg = :lastMsg WHERE threadId = :threadId")
    fun updateLastMsgInfo(threadId: String, lastMsg: ByteArray?): Int
}