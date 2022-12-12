package com.lgt.cwm.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.lgt.cwm.db.entity.CWMUser

/**
 * Created by giangtpu on 04/10/2022
 */
@Dao
interface CWMUserDao {
    @Query("SELECT * FROM cwmUser ORDER BY phoneFull DESC")
    fun getAll(): List<CWMUser>

    @Query("SELECT * FROM cwmUser WHERE phoneFull = :phoneFull")
    fun getByPhoneFull(phoneFull: String): CWMUser?

    @Query("SELECT * FROM cwmUser WHERE phoneFull in (:phoneFulls) ORDER BY phoneFull DESC")
    fun getAllByListPhoneFull(phoneFulls: List<String>): List<CWMUser>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(cwmUser: CWMUser): Long
}