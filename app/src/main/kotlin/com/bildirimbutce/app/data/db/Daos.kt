package com.bildirimbutce.app.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {

    /** Tekrarlanan bildirimler sessizce yok sayilir (sourceKey unique). */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(expense: ExpenseEntity): Long

    @Update
    suspend fun update(expense: ExpenseEntity)

    @Delete
    suspend fun delete(expense: ExpenseEntity)

    @Query("SELECT * FROM expenses WHERE occurredAt BETWEEN :from AND :to ORDER BY occurredAt DESC")
    fun observeBetween(from: Long, to: Long): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE occurredAt BETWEEN :from AND :to")
    suspend fun getBetween(from: Long, to: Long): List<ExpenseEntity>

    @Query("SELECT * FROM expenses WHERE id = :id")
    suspend fun byId(id: Long): ExpenseEntity?

    @Query("UPDATE expenses SET category = :category, userEdited = 1 WHERE merchant = :merchant AND userEdited = 0")
    suspend fun recategorizeMerchant(merchant: String, category: String)

    /** Ayarlardaki kayit sayaci; ay penceresine bagli degil, tablonun tamami. */
    @Query("SELECT COUNT(*) FROM expenses")
    fun observeCount(): Flow<Int>

    @Query("DELETE FROM expenses")
    suspend fun deleteAll()
}

@Dao
interface MerchantRuleDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(rule: MerchantRuleEntity)

    @Query("SELECT * FROM merchant_rules WHERE merchantKey = :key")
    suspend fun find(key: String): MerchantRuleEntity?

    /** F3 listesi. Siralama sabit olmali, yoksa liste her yenilemede karisir. */
    @Query("SELECT * FROM merchant_rules ORDER BY merchantKey")
    fun observeAll(): Flow<List<MerchantRuleEntity>>

    @Query("DELETE FROM merchant_rules WHERE merchantKey = :key")
    suspend fun deleteByKey(key: String)

    @Query("DELETE FROM merchant_rules")
    suspend fun deleteAll()
}
