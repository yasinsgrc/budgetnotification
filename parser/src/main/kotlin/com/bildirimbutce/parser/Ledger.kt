package com.bildirimbutce.parser

import java.security.MessageDigest
import java.util.Calendar

/**
 * Kaydedilmeye hazir islem. Room'a bagimli degildir; Android katmani bunu
 * kendi entity'sine cevirir. Boylece tum is mantigi emulator olmadan
 * test edilebilir.
 */
data class LedgerEntry(
    val amountMinor: Long,
    val currency: String,
    val merchant: String?,
    val category: Category,
    val kind: TxKind,
    val occurredAt: Long,
    val sourceApp: String,
    val patternId: String,
    val confidence: Float,
    val rawText: String,
    val sourceKey: String
)

object Ledger {

    /**
     * Bildirim kimligi.
     *
     * Saat kovasi kullaniliyor: Android ayni bildirimi guncellendiginde
     * saniyeler icinde tekrar teslim eder (tekrar kayit olur), ama kullanici
     * ayni magazadan ertesi saat gercekten tekrar alisveris yapabilir
     * (kaybolmamali).
     */
    fun sourceKey(sourceApp: String, rawText: String, postedAt: Long): String {
        val hourBucket = postedAt / 3_600_000L
        val seed = "$sourceApp|${rawText.trim()}|$hourBucket"
        val digest = MessageDigest.getInstance("SHA-256").digest(seed.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }.take(32)
    }

    fun entryFor(
        tx: ParsedTransaction,
        sourceApp: String,
        postedAt: Long,
        category: Category = Categorizer.guess(tx.merchant)
    ): LedgerEntry = LedgerEntry(
        amountMinor = tx.amountMinor,
        currency = tx.currency,
        merchant = tx.merchant,
        category = category,
        kind = tx.kind,
        occurredAt = postedAt,
        sourceApp = sourceApp,
        patternId = tx.patternId,
        confidence = tx.confidence,
        rawText = tx.rawText,
        sourceKey = sourceKey(sourceApp, tx.rawText, postedAt)
    )

    /** Iade negatif sayilir. */
    fun signedMinor(kind: TxKind, amountMinor: Long): Long =
        if (kind == TxKind.REFUND) -amountMinor else amountMinor

    fun total(entries: List<LedgerEntry>): Long =
        entries.sumOf { signedMinor(it.kind, it.amountMinor) }

    fun byCategory(entries: List<LedgerEntry>): List<Pair<Category, Long>> =
        entries.groupBy { it.category }
            .map { (category, items) -> category to total(items) }
            .filter { it.second > 0 }
            .sortedByDescending { it.second }

    /** [year] ve 0-tabanli [month] icin [from, to] zaman araligi. */
    fun monthRange(year: Int, month: Int): Pair<Long, Long> {
        val cal = Calendar.getInstance().apply { clear(); set(year, month, 1, 0, 0, 0) }
        val from = cal.timeInMillis
        cal.add(Calendar.MONTH, 1)
        return from to (cal.timeInMillis - 1)
    }
}
