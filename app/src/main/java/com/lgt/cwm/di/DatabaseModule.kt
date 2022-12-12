package com.lgt.cwm.di

import android.content.Context
import androidx.room.Room
import com.lgt.cwm.db.AppDatabase
import com.lgt.cwm.db.dao.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Created by giangtpu on 6/29/22.
 */
@InstallIn(SingletonComponent::class)
@Module
object DatabaseModule {

    @Provides
    fun provideAccountDAO(database: AppDatabase): AccountDao{
        return database.accountDao()
    }

    @Provides
    fun provideContactDao(database: AppDatabase): ContactDao {
        return database.contactDao()
    }

    @Provides
    fun provideSignalMsgDao(database: AppDatabase): SignalMsgDao {
        return database.signalMsgDao()
    }

    @Provides
    fun provideSignalThreadDao(database: AppDatabase): SignalThreadDao {
        return database.signalThreadDao()
    }

    @Provides
    fun provideCWMUserDao(database: AppDatabase): CWMUserDao {
        return database.cwmUserDao()
    }

    @Singleton
    @Provides
    fun provideDatabase(@ApplicationContext applicationContext: Context): AppDatabase {
        return Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "logging.db"
        ).build()
    }
}