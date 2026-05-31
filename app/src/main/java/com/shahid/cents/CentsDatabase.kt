package com.shahid.cents

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [TransactionEntity::class, CategoryRuleEntity::class], version = 2, exportSchema = false)
abstract class CentsDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryRuleDao(): CategoryRuleDao

    companion object {
        fun create(context: Context): CentsDatabase = Room.databaseBuilder(
            context.applicationContext,
            CentsDatabase::class.java,
            "cents.db"
        ).fallbackToDestructiveMigration().build()
    }
}
