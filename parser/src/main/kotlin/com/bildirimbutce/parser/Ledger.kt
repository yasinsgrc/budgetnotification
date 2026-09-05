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

    /** Tekrar korumasinin kova boyu; testler de bunu kullanir ki degisirse birlikte degissin. */
    const val HOUR_MILLIS = 3_600_000L

    /**
     * Elle girilen kayitlarin kaynak damgasi.
     *
     * Bildirimden gelenler paket adiyla (`com.garanti.cepsubesi` gibi)
     * damgalanir; elle girilenin paketi yok. Ekranlar "elle girilenler"
     * listesini bu damgayla ayirir - yeni bir sutun ve sema surumu gerekmiyor.
     */
    const val MANUAL_SOURCE = "manual"

    /** Elle girilenlerin desen kimligi: ayristirma yapilmadi, kullanici yazdi. */
    const val MANUAL_PATTERN = "manual"

    /**
     * Bildirim kimligi.
     *
     * Saat kovasi kullaniliyor: Android ayni bildirimi guncellendiginde
     * saniyeler icinde tekrar teslim eder (tekrar kayit olur), ama kullanici
     * ayni magazadan ertesi saat gercekten tekrar alisveris yapabilir
     * (kaybolmamali).
     */
    fun sourceKey(sourceApp: String, rawText: String, postedAt: Long): String {
        val hourBucket = postedAt / HOUR_MILLIS
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

    /**
     * Elle girilen kayit.
     *
     * Bildirimden gelenin aksine tekrar korumasi YOK: kullanici ayni saat
     * icinde ayni tutari gercekten iki kez harcamis olabilir ve ikisini de
     * gormeli. Bu yuzden [sourceKey] metinden turetilmiyor, disaridan
     * benzersiz veriliyor - unique index iki gercek girisi birbirine yemesin.
     *
     * [rawText] bos: ayristirilacak bir bildirim metni yok. Duzeltme sayfasi
     * "kaynak/desen/guven" satirlarini bu alanlardan okuyor, bu yuzden hepsi
     * doldurulmali; guven 1.0 cunku tahmin degil, kullanicinin beyani.
     */
    fun manualEntry(
        amountMinor: Long,
        merchant: String?,
        category: Category,
        occurredAt: Long,
        sourceKey: String,
        currency: String = "TL"
    ): LedgerEntry = LedgerEntry(
        amountMinor = amountMinor,
        currency = currency,
        merchant = merchant,
        category = category,
        kind = TxKind.EXPENSE,
        occurredAt = occurredAt,
        sourceApp = MANUAL_SOURCE,
        patternId = MANUAL_PATTERN,
        confidence = 1f,
        rawText = "",
        sourceKey = sourceKey
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

    /**
     * [year]/[month] ayinda biten, [monthCount] ay uzunlugunda aralik.
     *
     * Rapor ekrani "son 6 ay" grafigini tek sorguyla okuyor: ay basina ayri
     * sorgu acmak yerine pencerenin tamami bir kez cekilip bellekte aylara
     * bolunuyor. Hesabin burada durmasi, artik yil ve farkli ay uzunluklarinin
     * emulator olmadan test edilebilmesini sagliyor.
     */
    fun rangeEndingAt(year: Int, month: Int, monthCount: Int): Pair<Long, Long> {
        require(monthCount >= 1) { "monthCount en az 1 olmali: $monthCount" }
        val cal = Calendar.getInstance().apply { clear(); set(year, month, 1, 0, 0, 0) }
        cal.add(Calendar.MONTH, -(monthCount - 1))
        return cal.timeInMillis to monthRange(year, month).second
    }
}
