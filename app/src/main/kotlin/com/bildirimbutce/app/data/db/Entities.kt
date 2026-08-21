package com.bildirimbutce.app.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Kaydedilmis tek bir islem.
 *
 * [sourceKey] uzerindeki unique index kritik: Android ayni bildirimi
 * guncellendiginde tekrar tekrar teslim eder. Bu index olmadan kullanici
 * tek harcamayi listede 3-4 kez gorur ve uygulamaya guvenmez.
 */
@Entity(
    tableName = "expenses",
    indices = [
        Index(value = ["sourceKey"], unique = true),
        Index(value = ["occurredAt"])
    ]
)
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amountMinor: Long,
    val currency: String,
    val merchant: String?,
    val category: String,
    val kind: String,
    val occurredAt: Long,
    val sourceApp: String,
    val patternId: String,
    val confidence: Float,
    val rawText: String,
    val sourceKey: String,
    val userEdited: Boolean = false
)

/** Kullanicinin "bu magaza aslinda su kategori" duzeltmesi - bir kez ogrenilir. */
@Entity(tableName = "merchant_rules")
data class MerchantRuleEntity(
    @PrimaryKey val merchantKey: String,
    val category: String
)
