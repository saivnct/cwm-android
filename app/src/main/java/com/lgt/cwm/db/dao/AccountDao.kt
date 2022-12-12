package com.lgt.cwm.db.dao

import androidx.room.*
import com.lgt.cwm.db.entity.Account
import com.lgt.cwm.util.DateUtil
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Created by giangtpu on 6/29/22.
 */
@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts WHERE phoneFull = :phoneFull LIMIT 1")
    fun getByPhoneFull(phoneFull: String): Account?

    @Query("SELECT * FROM accounts WHERE id = :id")
    fun getById(id: Long): Account?

    @Query("SELECT * FROM accounts WHERE id = :id")
    fun getByIdFlow(id: Long): Flow<Account>

    //https://medium.com/androiddevelopers/room-flow-273acffe5b57
    fun getByIdDistinctUntilChanged(id: Long) =
        getByIdFlow(id).distinctUntilChanged()

    @Query("SELECT * FROM accounts ORDER BY id DESC")
    fun getAll(): Flow<List<Account>>

    @Query("SELECT * FROM accounts WHERE isActive = 1 LIMIT 1")
    fun getActivedAcc(): Account?

    @Query("SELECT * FROM accounts WHERE isActive = 1 LIMIT 1")
    fun getActivedAccFlow(): Flow<Account?>

    fun getActiveAccFlowDistinctUntilChanged() =
        getActivedAccFlow().distinctUntilChanged()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAccount(account: Account): Long

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun update(account: Account)

    @Query("UPDATE accounts SET isActive = 0 WHERE id <> :id")
    fun setAllInActiveExcept(id: Long): Int

    @Query("UPDATE accounts SET isActive = :isActive, status = :status WHERE id = :id")
    fun changeActiveState(id: Long, isActive: Boolean, status: Int): Int

    @Query("UPDATE accounts SET status = :status, nonce = :nonce, nonceResponse = :nonceRespone, nonceAt = :nonceAt WHERE id = :id")
    fun updateNonce(id: Long, status: Int, nonce: String, nonceRespone: String, nonceAt: Long): Int

    @Query("UPDATE accounts SET status = :status, lastAttempLoginAt = :lastAttempLoginAt, jwt = :jwt, jwtAt = :jwtAt, jwtTTL = :jwtTTL WHERE id = :id")
    fun setLoginStatus(id: Long, status: Int, lastAttempLoginAt: Long, jwt: String = "", jwtAt: Long = 0, jwtTTL: Int = 0): Int

    @Query("UPDATE accounts SET status = :status, lastAttempLoginAt = :lastAttempLoginAt, nonce = :nonce, nonceResponse = :nonceRespone, nonceAt = :nonceAt, jwt = :jwt, jwtAt = :jwtAt, jwtTTL = :jwtTTL WHERE id = :id")
    fun setLoginStatusWithNewNonce(id: Long, status: Int, lastAttempLoginAt: Long, nonce: String, nonceRespone: String, nonceAt: Long, jwt: String = "", jwtAt: Long = 0, jwtTTL: Int = 0): Int

    @Query("UPDATE accounts SET firstName = :firstName, lastName = :lastName WHERE id = :id")
    fun updateProfile(id: Long, firstName: String, lastName: String): Int

    @Query("UPDATE accounts SET username = :userName WHERE id = :id")
    fun updateUserName(id: Long, userName: String): Int
}