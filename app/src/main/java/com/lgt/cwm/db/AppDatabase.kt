/*
 * Copyright (C) 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.lgt.cwm.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.lgt.cwm.db.dao.*
import com.lgt.cwm.db.entity.*

/**
 * Created by giangtpu on 6/29/22.
 */
/**
 * SQLite Database.
 */
@Database(entities = arrayOf(
    Account::class,
    Contact::class,
    SignalMsg::class,
    SignalThread::class,
    CWMUser::class,
), version = 1, exportSchema = false)
@TypeConverters(DatabaseTypeConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun contactDao(): ContactDao
    abstract fun signalMsgDao(): SignalMsgDao
    abstract fun signalThreadDao(): SignalThreadDao
    abstract fun cwmUserDao(): CWMUserDao
}
