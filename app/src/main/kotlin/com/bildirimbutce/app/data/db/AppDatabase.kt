package com.bildirimbutce.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration

@Database(
    entities = [ExpenseEntity::class, MerchantRuleEntity::class],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun expenseDao(): ExpenseDao
    abstract fun merchantRuleDao(): MerchantRuleDao

    companion object {
        internal const val NAME = "bildirim_butce.db"

        /**
         * Surum yukseltmeleri. Uretim ve migration testi ayni diziyi kullanir;
         * yeni bir migration eklenince test de otomatik olarak onu dener.
         */
        internal val MIGRATIONS: Array<Migration> = emptyArray()

        @Volatile private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                NAME
            ).addMigrations(*MIGRATIONS).build().also { instance = it }
        }
    }
}
