package com.bildirimbutce.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * Elle girilen kaydin degismez ozellikleri.
 *
 * Kritik olan iki sey var: kayit bildirimden gelenden ayirt edilebilmeli
 * (ekranlar "elle girilenler" listesini buna gore suzuyor) ve tekrar korumasi
 * devrede olmamali - kullanici ayni tutari iki kez girdiginde ikisi de durmali.
 */
class LedgerManualEntryTest {

    private fun entry(
        amountMinor: Long = 8_990,
        merchant: String? = "Kantin",
        category: Category = Category.YEME_ICME,
        occurredAt: Long = 1_757_068_800_000,
        sourceKey: String = "manual:1"
    ) = Ledger.manualEntry(amountMinor, merchant, category, occurredAt, sourceKey)

    @Test
    fun `elle giris manual kaynagiyla damgalaniyor`() {
        val entry = entry()

        assertEquals(Ledger.MANUAL_SOURCE, entry.sourceApp)
        assertEquals(Ledger.MANUAL_PATTERN, entry.patternId)
    }

    /** Tahmin degil, kullanicinin beyani: guven tam olmali. */
    @Test
    fun `elle giriste guven tam`() {
        assertEquals(1f, entry().confidence, 0f)
    }

    /** Ayristirilacak bildirim metni yok; alan bos kalmali, uydurulmamali. */
    @Test
    fun `elle giriste ham metin bos`() {
        assertEquals("", entry().rawText)
    }

    @Test
    fun `elle giris harcama olarak kaydediliyor`() {
        val entry = entry()

        assertEquals(TxKind.EXPENSE, entry.kind)
        assertEquals(8_990L, Ledger.signedMinor(entry.kind, entry.amountMinor))
    }

    @Test
    fun `verilen alanlar oldugu gibi tasiniyor`() {
        val entry = entry(amountMinor = 124_900, sourceKey = "manual:abc")

        assertEquals(124_900L, entry.amountMinor)
        assertEquals("Kantin", entry.merchant)
        assertEquals(Category.YEME_ICME, entry.category)
        assertEquals(1_757_068_800_000L, entry.occurredAt)
        assertEquals("manual:abc", entry.sourceKey)
        assertEquals("TL", entry.currency)
    }

    @Test
    fun `isyeri bos birakilabilir`() {
        assertEquals(null, entry(merchant = null).merchant)
    }

    /**
     * Bildirim kimligi metinden turetildigi icin ayni saat kovasindaki ayni
     * metin tek kayda dusuyor. Elle giriste bu YANLIS olurdu: iki gercek
     * alisveris birbirini yerdi. Bu yuzden anahtar metinden uretilmiyor,
     * disaridan veriliyor - ayni girdi ayni anahtari dogurmuyor.
     */
    @Test
    fun `ayni giris farkli anahtarla iki kayit olabiliyor`() {
        val ilk = entry(sourceKey = "manual:1")
        val ikinci = entry(sourceKey = "manual:2")

        assertNotEquals(ilk.sourceKey, ikinci.sourceKey)
        assertEquals(ilk.amountMinor, ikinci.amountMinor)
        assertEquals(ilk.merchant, ikinci.merchant)
    }
}
