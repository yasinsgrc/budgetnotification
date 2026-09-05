package com.bildirimbutce.app.service

import com.bildirimbutce.app.data.ExpenseRepository
import com.bildirimbutce.app.data.PatternProvider
import com.bildirimbutce.app.data.db.AppDatabase
import com.bildirimbutce.app.hourBucketStart
import com.bildirimbutce.app.inMemoryDb
import com.bildirimbutce.app.testContext
import com.bildirimbutce.parser.Category
import com.bildirimbutce.parser.Ledger
import com.bildirimbutce.parser.ParseResult
import com.bildirimbutce.parser.TxKind
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Bildirim -> ayristirma -> kayit akisi.
 *
 * NotificationService'in kendisi burada kurulamiyor: StatusBarNotification'in
 * kurucusu @hide, derleme SDK'sinda yok. Bu yuzden servisin yaptigi is birebir
 * ayni sirada test ediliyor: paket suzgeci, gercek patterns.json ile
 * ayristirma, ExpenseRepository.record, tekrar teslim. Servis sinifinda geriye
 * yalnizca Android baglantisi kaliyor.
 */
@RunWith(RobolectricTestRunner::class)
class NotificationFlowTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: ExpenseRepository

    private val bucketStart = hourBucketStart(1_700_000_000_000L)

    @Before
    fun setUp() {
        // PatternProvider surec omurlu onbellek tutuyor; testler birbirini etkilemesin.
        PatternProvider.invalidate()
        db = inMemoryDb()
        repository = ExpenseRepository(db)
    }

    @After
    fun tearDown() {
        db.close()
        PatternProvider.invalidate()
    }

    private fun parser() = PatternProvider.parser(testContext())

    @Test
    fun `bilinen banka paketleri dinleniyor bilinmeyenler atlaniyor`() {
        val parser = parser()

        assertTrue(parser.isKnownSource(GARANTI))
        assertTrue(
            "SMS uygulamalari da banka mesaji tasiyor",
            parser.isKnownSource("com.android.mms")
        )
        assertFalse(
            "listede olmayan uygulamanin bildirimi hic okunmamali - gizlilik ve pil",
            parser.isKnownSource("com.instagram.android")
        )
    }

    @Test
    fun `harcama bildirimi ayristirilip kaydediliyor`() = runBlocking {
        val result = parser().parse(HARCAMA)
        assertTrue("paketle gelen patterns.json bu metni tanimali", result is ParseResult.Match)

        assertTrue(repository.record((result as ParseResult.Match).transaction, GARANTI, bucketStart))

        val row = rows().single()
        assertEquals(24_590L, row.amountMinor)
        assertEquals("TL", row.currency)
        assertEquals(TxKind.EXPENSE.name, row.kind)
        assertEquals(Category.MARKET.name, row.category)
        assertEquals(GARANTI, row.sourceApp)
        assertEquals(bucketStart, row.occurredAt)
        assertTrue("magaza adi cikarilmali", row.merchant?.contains("migros", true) == true)
    }

    @Test
    fun `iade bildirimi REFUND olarak kaydediliyor`() = runBlocking {
        assertTrue(handle(IADE, bucketStart))

        val row = rows().single()
        assertEquals(TxKind.REFUND.name, row.kind)
        assertEquals(4_500L, row.amountMinor)
    }

    /**
     * Android bildirimi guncelledikce ayni metni tekrar teslim eder; akis
     * bastan sona kosuldugunda ikinci teslim tabloya yeni satir eklememeli.
     */
    @Test
    fun `ayni bildirimin tekrar teslimi yeni kayit acmiyor`() = runBlocking {
        val eklendi = listOf(bucketStart, bucketStart + 5_000, bucketStart + 30_000)
            .count { postedAt -> handle(HARCAMA, postedAt) }

        assertEquals(1, eklendi)
        assertEquals(1, rows().size)
    }

    @Test
    fun `eslesmeyen bildirim kayit acmiyor`() = runBlocking {
        assertFalse(handle("Yarin hava parcali bulutlu, 18 derece.", bucketStart))

        assertTrue(rows().isEmpty())
    }

    @Test
    fun `farkli bildirimler ayri kayit oluyor ve toplam iadeyi dusuyor`() = runBlocking {
        assertTrue(handle(HARCAMA, bucketStart))
        assertTrue(handle(IADE, bucketStart))

        assertEquals(2, rows().size)
        val total = rows().sumOf { Ledger.signedMinor(TxKind.valueOf(it.kind), it.amountMinor) }
        assertEquals(24_590L - 4_500L, total)
    }

    /** Servisteki sirayi tekrarlar: ayristir, esleserse kaydet. */
    private suspend fun handle(text: String, postedAt: Long): Boolean =
        when (val result = parser().parse(text)) {
            is ParseResult.Match -> repository.record(result.transaction, GARANTI, postedAt)
            is ParseResult.Ignored, ParseResult.NoMatch -> false
        }

    private suspend fun rows() = db.expenseDao().getBetween(0L, Long.MAX_VALUE)

    private companion object {
        const val GARANTI = "com.garanti.cepsubesi"

        // TestNotificationSeeder.SAMPLE_TEXTS ile ayni metinler - uctan uca
        // dogrulamada gercek cihazda islendikleri gorulmustu.
        const val HARCAMA =
            "Garanti BBVA: 1234 kartiniz ile MIGROS isyerinde 245,90 TL tutarinda harcama yapilmistir."
        const val IADE = "MIGROS isyerinden 45,00 TL iade yapılmıştır."
    }
}

/**
 * Bildirim metninin kurulusu.
 *
 * Kisaltilmis govde kullanilirsa tutar "245,90 TL" yerine "245,9..." olarak
 * gelir ve ayristirma sessizce basarisiz olur - kullanici harcamanin
 * kaydedilmedigini fark etmez. Bu yuzden bigText onceligi test altinda.
 */
class NotificationTextTest {

    @Test
    fun `bigText varsa kisaltilmis govde yerine o kullaniliyor`() {
        val text = NotificationText.compose(
            title = "Garanti BBVA",
            body = "1234 kartiniz ile MIGROS isyerinde 245,9...",
            bigText = "1234 kartiniz ile MIGROS isyerinde 245,90 TL tutarinda harcama yapilmistir."
        )

        assertTrue(text.contains("245,90 TL"))
        assertFalse(text.contains("245,9..."))
    }

    @Test
    fun `bigText yoksa govde kullaniliyor`() {
        val text = NotificationText.compose(
            title = "Garanti BBVA",
            body = "MIGROS 245,90 TL",
            bigText = null
        )

        assertEquals("Garanti BBVA MIGROS 245,90 TL", text)
    }

    @Test
    fun `bos bigText govdenin onune gecmiyor`() {
        val text = NotificationText.compose(
            title = null,
            body = "MIGROS 245,90 TL",
            bigText = "   "
        )

        assertEquals("MIGROS 245,90 TL", text)
    }

    @Test
    fun `baslik govdenin onune ekleniyor`() {
        assertEquals(
            "Garanti BBVA MIGROS 245,90 TL",
            NotificationText.compose("Garanti BBVA", "MIGROS 245,90 TL", null)
        )
    }

    @Test
    fun `baslik yoksa bosluk birakilmiyor`() {
        assertEquals(
            "MIGROS 245,90 TL",
            NotificationText.compose(null, "MIGROS 245,90 TL", null)
        )
    }

    @Test
    fun `tum alanlar bossa bos metin doner`() {
        assertEquals("", NotificationText.compose(null, null, null))
        assertEquals("", NotificationText.compose("  ", "", "   "))
    }
}
