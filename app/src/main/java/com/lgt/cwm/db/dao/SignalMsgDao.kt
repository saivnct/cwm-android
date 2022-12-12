package com.lgt.cwm.db.dao

import androidx.room.*
import com.lgt.cwm.db.entity.SignalMsg
import com.lgt.cwm.db.entity.SignalMsgDirection
import com.lgt.cwm.db.entity.SignalMsgStatus
import cwmSignalMsgPb.CwmSignalMsg
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Created by giangtpu on 7/25/22.
 */
@Dao
interface SignalMsgDao {
    @Query("SELECT * FROM signalMsgs ORDER BY msgDate DESC")
    fun getAll(): List<SignalMsg>

    //TODO: Filter Not IM_TYPE: SEENSTATE & EVENT
    @Query("SELECT * FROM signalMsgs WHERE status = :status ORDER BY msgDate ASC LIMIT 1")
    fun getFirstByStatus(status: Int): SignalMsg?

    @Query("SELECT COUNT(msgId) FROM signalMsgs WHERE status = :status")
    fun countAllByStatusFlow(status: Int): Flow<Long>
    fun countAllByStatusFlowDistinctUntilChanged(status: Int) =
        countAllByStatusFlow(status).distinctUntilChanged()

    //CwmSignalMsg.SIGNAL_IM_TYPE.TYPING.number = 0
    //CwmSignalMsg.SIGNAL_IM_TYPE.SEENSTATE.number = 1
    //CwmSignalMsg.SIGNAL_IM_TYPE.EVENT.number = 2
    @Query("SELECT * FROM signalMsgs WHERE threadId = :threadId AND imType > :imTypeEvent ORDER BY msgDate DESC")
    fun getAllMsgHaveContentByThreadIdFlow(threadId: String, imTypeEvent: Int = CwmSignalMsg.SIGNAL_IM_TYPE.EVENT.number): Flow<List<SignalMsg>>
    @Query("SELECT COUNT(msgId) FROM signalMsgs WHERE threadId = :threadId AND imType > :imTypeEvent ")
    fun countAllMsgHaveContentByThreadIdFlow(threadId: String, imTypeEvent: Int = CwmSignalMsg.SIGNAL_IM_TYPE.EVENT.number): Long

    @Query("SELECT * FROM signalMsgs WHERE threadId = :threadId AND imType > :imTypeEvent ORDER BY msgDate DESC LIMIT 1")
    fun getLastMsgHaveContentByThreadId(threadId: String, imTypeEvent: Int = CwmSignalMsg.SIGNAL_IM_TYPE.EVENT.number): SignalMsg?
    @Query("SELECT COUNT(msgId) FROM signalMsgs WHERE threadId = :threadId AND status = :status AND imType > :imTypeEvent")
    fun countAllUnreadMsgOfThreadId(threadId: String, status: Int = SignalMsgStatus.RECEIVED_UNREAD.code, imTypeEvent: Int = CwmSignalMsg.SIGNAL_IM_TYPE.EVENT.number): Long

    @Query("SELECT COUNT(msgId) FROM signalMsgs WHERE status = :status AND imType > :imTypeEvent")
    fun countAllUnreadMsg(status: Int = SignalMsgStatus.RECEIVED_UNREAD.code, imTypeEvent: Int = CwmSignalMsg.SIGNAL_IM_TYPE.EVENT.number): Long
    @Query("SELECT COUNT(msgId) FROM signalMsgs WHERE status = :status AND imType > :imTypeEvent")
    fun countAllUnreadMsgFlow(status: Int = SignalMsgStatus.RECEIVED_UNREAD.code, imTypeEvent: Int = CwmSignalMsg.SIGNAL_IM_TYPE.EVENT.number): Flow<Long>
    @Query("SELECT COUNT(DISTINCT threadId) FROM signalMsgs WHERE status = :status AND imType > :imTypeEvent")
    fun countAllUnreadThread(status: Int = SignalMsgStatus.RECEIVED_UNREAD.code, imTypeEvent: Int = CwmSignalMsg.SIGNAL_IM_TYPE.EVENT.number): Long

    @Query("SELECT COUNT(msgId) FROM signalMsgs WHERE status = :status AND imType > :imTypeEvent AND threadId <> :exceptThreadId")
    fun countAllUnreadMsgExceptThread(exceptThreadId: String, status: Int = SignalMsgStatus.RECEIVED_UNREAD.code, imTypeEvent: Int = CwmSignalMsg.SIGNAL_IM_TYPE.EVENT.number): Long
    @Query("SELECT COUNT(DISTINCT threadId) FROM signalMsgs WHERE status = :status AND imType > :imTypeEvent AND threadId <> :exceptThreadId")
    fun countAllUnreadThreadExceptThread(exceptThreadId: String, status: Int = SignalMsgStatus.RECEIVED_UNREAD.code, imTypeEvent: Int = CwmSignalMsg.SIGNAL_IM_TYPE.EVENT.number): Long



    @Query("UPDATE signalMsgs SET eventHandled = 1 WHERE msgId = :msgId")
    fun setHandledEventMsg(msgId: String): Int

    //CwmSignalMsg.SIGNAL_IM_TYPE.EVENT.number = 2
    @Query("SELECT * FROM signalMsgs WHERE imType = :imTypeEvent AND eventHandled = 0  ORDER BY msgDate ASC LIMIT 1")
    fun getFirstUnhanldedEventMsg(imTypeEvent: Int = CwmSignalMsg.SIGNAL_IM_TYPE.EVENT.number): SignalMsg?

    @Query("SELECT COUNT(msgId) FROM signalMsgs WHERE imType = :imTypeEvent AND eventHandled = 0")
    fun countAllUnhanldedEventMsgFlow(imTypeEvent: Int = CwmSignalMsg.SIGNAL_IM_TYPE.EVENT.number): Flow<Long>
    fun countAllUnhanldedEventMsgFlowDistinctUntilChanged() =
        countAllUnhanldedEventMsgFlow().distinctUntilChanged()



    @Query("UPDATE signalMsgs SET confirmReceive = 1 WHERE msgId in (:msgIds)")
    fun setConfirmReceiveMsgs(msgIds: List<String>): Int

//    @Query("SELECT msgId FROM signalMsgs WHERE confirmReceive = 0  ORDER BY msgDate ASC")
//    fun getAllUnconfirmReceiveMsg(): List<String>
//    @Query("SELECT COUNT(msgId) FROM signalMsgs WHERE confirmReceive = 0")
//    fun countAllUnconfirmReceiveFlow(): Flow<Long>
//    fun countAllUnconfirmReceiveFlowDistinctUntilChanged() =
//        countAllUnconfirmReceiveFlow().distinctUntilChanged()



    @Query("UPDATE signalMsgs SET multiMediaDownloadHandled = 1 WHERE msgId = :msgId")
    fun setHandledMultiMediaDownloadMsg(msgId: String): Int
    @Query("UPDATE signalMsgs SET multiMediaDownloadHandled = 1, content = :content, checksum = :checksum WHERE msgId = :msgId")
    fun setHandledMultiMediaDownloadMsg(msgId: String, content: ByteArray?, checksum: String): Int

    @Query("SELECT * FROM signalMsgs WHERE imType = :imTypeMultiMedia AND multiMediaDownloadHandled = 0  ORDER BY msgDate ASC LIMIT 1")
    fun getFirstUnHandledMultiMediaDownloadMsg(imTypeMultiMedia: Int = CwmSignalMsg.SIGNAL_IM_TYPE.MULTIMEDIA.number): SignalMsg?
    @Query("SELECT COUNT(msgId) FROM signalMsgs WHERE imType = :imTypeMultiMedia AND multiMediaDownloadHandled = 0")
    fun countAllUnHandledMultiMediaDownloadFlow(imTypeMultiMedia: Int = CwmSignalMsg.SIGNAL_IM_TYPE.MULTIMEDIA.number): Flow<Long>
    fun countAllUnHandledMultiMediaDownloadFlowDistinctUntilChanged() =
        countAllUnHandledMultiMediaDownloadFlow().distinctUntilChanged()






    @Query("SELECT msgId FROM signalMsgs WHERE threadId = :threadId AND status = :status")
    fun getAllMsgIdByThreadIdAndStatus(threadId: String, status: Int): List<String>
    @Query("SELECT msgId FROM signalMsgs WHERE threadId = :threadId AND status = :status")
    fun getAllMsgIdByThreadIdAndStatusFlow(threadId: String, status: Int): Flow<List<String>>
    fun getAllMsgIdByThreadIdAndStatusFlowDistinctUntilChanged(threadId: String, status: Int) =
        getAllMsgIdByThreadIdAndStatusFlow(threadId, status).distinctUntilChanged()

    @Query("SELECT COUNT(msgId) FROM signalMsgs WHERE threadId = :threadId AND status = :status")
    fun countAllMsgIdByThreadIdAndStatusFlow(threadId: String, status: Int): Flow<Long>
    fun countAllMsgIdByThreadIdAndStatusFlowDistinctUntilChanged(threadId: String, status: Int) =
        countAllMsgIdByThreadIdAndStatusFlow(threadId, status).distinctUntilChanged()


    @Query("SELECT msgId FROM signalMsgs WHERE threadId = :threadId AND sendSeenState = 0")
    fun getAllMsgIdByThreadIdAndNotSendSeenStateFlow(threadId: String): Flow<List<String>>
    fun getAllMsgIdByThreadIdAndNotSendSeenStateFlowUntilChanged(threadId: String) =
        getAllMsgIdByThreadIdAndNotSendSeenStateFlow(threadId).distinctUntilChanged()


    @Query("SELECT * FROM signalMsgs ORDER BY msgDate DESC LIMIT 1")
    fun getLastMsg(): SignalMsg?
    @Query("SELECT * FROM signalMsgs WHERE threadId = :threadId ORDER BY msgDate ASC LIMIT 1")
    fun getOldestMsgByThreadId(threadId: String): SignalMsg?


    @Query("SELECT * FROM signalMsgs WHERE msgId = :msgId")
    fun getByMsgId(msgId: String): SignalMsg?

    @Query("SELECT * FROM signalMsgs WHERE msgId IN (:msgIds) ORDER BY msgDate ASC")
    fun getByListMsgId(msgIds: List<String>): List<SignalMsg>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertSignalMsg(signalMsg: SignalMsg): Long

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun update(signalMsg: SignalMsg)

    @Query("DELETE FROM signalMsgs WHERE threadId = :threadId")
    fun deleteByThreadId(threadId: String)

    @Query("DELETE FROM signalMsgs WHERE msgId IN (:msgIds)")
    fun deleteByMsgIds(msgIds: List<String>)

    @Query("DELETE FROM signalMsgs WHERE threadId = :threadId AND serverDate < :toServerDate")
    fun clearByThreadId(threadId: String, toServerDate: Long)


    @Query("UPDATE signalMsgs SET status = :status WHERE msgId IN (:msgIds)")
    fun updateListMsgStatus(msgIds: List<String>, status: Int): Int

    @Query("UPDATE signalMsgs SET status = :status WHERE msgId = :msgId")
    fun updateMsgStatus(msgId: String, status: Int): Int

    @Query("UPDATE signalMsgs SET status = :status WHERE threadId = :threadId AND serverDate <= :serverDate AND direction = :direction")
    fun updateMsgRecieveSeen(threadId: String, serverDate: Long, status: Int = SignalMsgStatus.RECEIVED_SEEN.code, direction: Int = SignalMsgDirection.INCOMING.code): Int



    @Query("UPDATE signalMsgs SET status = :status, serverDate = :serverDate, msgDate = :serverDate WHERE msgId = :msgId")
    fun updateMsgStatusAndServerDate(msgId: String, status: Int, serverDate: Long): Int

    @Query("UPDATE signalMsgs SET status = :status, msgDate = :msgDate WHERE msgId = :msgId")
    fun updateMsgStatusAndMsgDate(msgId: String, status: Int, msgDate: Long): Int



    @Query("UPDATE signalMsgs SET content = :content, checksum = :checksum WHERE msgId = :msgId")
    fun updateMsgContent(msgId: String, content: ByteArray?, checksum: String): Int

    @Query("UPDATE signalMsgs SET status = :status, content = :content, checksum = :checksum WHERE msgId = :msgId")
    fun updateMsgStatusAndContent(msgId: String, status: Int, content: ByteArray?, checksum: String): Int


    @Query("UPDATE signalMsgs SET status = :status, seenBy = :seenBy WHERE msgId = :msgId")
    fun updateMsgSeenState(msgId: String, status: Int, seenBy: List<String>): Int

    @Query("UPDATE signalMsgs SET status = :status, sendSeenState = :sendSeenState WHERE msgId = :msgId")
    fun updateMsgSeenState(msgId: String, status: Int, sendSeenState: Int ): Int

    @Query("UPDATE signalMsgs SET status = :status, sendSeenState = :sendSeenState WHERE msgId IN (:msgIds)")
    fun updateListMsgSeenState(msgIds: List<String>, status: Int, sendSeenState: Int): Int

    @Query("UPDATE signalMsgs SET sendSeenState = :sendSeenState WHERE msgId IN (:msgIds)")
    fun updateListMsgSendSeenState(msgIds: List<String>, sendSeenState: Int): Int
}