package com.bildirimbutce.app

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.bildirimbutce.app.data.db.AppDatabase
import com.bildirimbutce.app.data.db.ExpenseEntity
import com.bildirimbutce.parser.Category
import com.bildirimbutce.parser.Ledger
import com.bildirimbutce.parser.ParsedTransaction
import com.bildirimbutce.parser.TxKind

/**
 * Testler icin ortak yardimcilar.
 *
 * Veritabani bellekte kuruluyor: her test temiz bir sema ile basliyor ve
 * emulator gerekmiyor - Robolectric gercek SQLite'i JVM uzerinde calistirir.
 */
internal fun testContext(): Context = ApplicationProvider.getApplicationContext()

internal fun inMemoryDb(): AppDatabase =
    Room.inMemoryDatabaseBuilder(testContext(), AppDatabase::class.java)
        .allowMainThreadQueries()
        .build()

/** Saat kovasinin basi. Tekrar korumasi kova bazli oldugu icin testler buna gore kurulur. */
internal fun hourBucketStart(millis: Long): Long =
    millis / Ledger.HOUR_MILLIS * Ledger.HOUR_MILLIS

internal fun parsedTx(
    merchant: String? = "MIGROS",
    amountMinor: Long = 24_590,
    kind: TxKind = TxKind.EXPENSE,
    rawText: String = "1234 kartiniz ile $merchant isyerinde harcama yapilmistir.",
    patternId: String = "test",
    confidence: Float = 0.9f,
    currency: String = "TL"
) = ParsedTransaction(
    kind = kind,
    amountMinor = amountMinor,
    currency = currency,
    merchant = merchant,
    patternId = patternId,
    confidence = confidence,
    rawText = rawText
)

internal fun expenseEntity(
    merchant: String? = "MIGROS",
    amountMinor: Long = 24_590,
    kind: TxKind = TxKind.EXPENSE,
    category: Category = Category.MARKET,
    occurredAt: Long = 0L,
    sourceKey: String = "key-$merchant-$occurredAt-$amountMinor-${kind.name}",
    userEdited: Boolean = false,
    /** Elle girilenler [Ledger.MANUAL_SOURCE] tasir; ekranlar listeyi buna gore ayirir. */
    sourceApp: String = "test.app"
) = ExpenseEntity(
    amountMinor = amountMinor,
    currency = "TL",
    merchant = merchant,
    category = category.name,
    kind = kind.name,
    occurredAt = occurredAt,
    sourceApp = sourceApp,
    patternId = "test",
    confidence = 0.9f,
    rawText = "test",
    sourceKey = sourceKey,
    userEdited = userEdited
)
