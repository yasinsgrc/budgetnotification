package com.bildirimbutce.app.data

import com.bildirimbutce.app.data.db.AppDatabase
import com.bildirimbutce.app.data.db.ExpenseEntity
import com.bildirimbutce.app.inMemoryDb
import com.bildirimbutce.parser.Category
import com.bildirimbutce.parser.Ledger
import com.bildirimbutce.parser.TxKind
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Calendar

/**
 * Elle harcama girisi (D2).
 *
 * Bu yol, bildirim erisimi reddedilirse geriye kalan tek kayit yolu; bozulursa
 * uygulamanin yayinlanabilir bir yedegi kalmaz. Iki kural ozellikle kirilgan:
 * tekrar korumasi burada calismamali (bildirimde tam tersi isteniyor) ve
 * kullanicinin kendi sectigi kategori sonradan ezilmemeli.
 */
@RunWith(RobolectricTestRunner::class)
class ManualEntryTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: ExpenseRepository

    private val occurredAt = 1_700_000_000_000L

    @Before
    fun setUp() {
        db = inMemoryDb()
        repository = ExpenseRepository(db)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `elle giris kaydediliyor`() = runBlocking {
        repository.addManual(8_990, "Kantin", Category.YEME_ICME, occurredAt)

        val row = single()
        assertEquals(8_990L, row.amountMinor)
        assertEquals("Kantin", row.merchant)
        assertEquals(Category.YEME_ICME.name, row.category)
        assertEquals(TxKind.EXPENSE.name, row.kind)
        assertEquals(occurredAt, row.occurredAt)
    }

    /** Ekranlar "elle girilenler" listesini bu damgayla suzuyor. */
    @Test
    fun `elle giris manual kaynagiyla damgalaniyor`() = runBlocking {
        repository.addManual(8_990, "Kantin", Category.YEME_ICME, occurredAt)

        assertEquals(Ledger.MANUAL_SOURCE, single().sourceApp)
    }

    /**
     * Bildirimde tekrar teslim yok sayilir; elle giriste sayilmamali. Kullanici
     * ayni gun ayni kantinde iki kez 89,90 harcamis olabilir ve ikisini de
     * gormeli - biri sessizce yutulursa ayin toplami eksik cikar.
     */
    @Test
    fun `ayni giris iki kez yapilabiliyor`() = runBlocking {
        repository.addManual(8_990, "Kantin", Category.YEME_ICME, occurredAt)
        repository.addManual(8_990, "Kantin", Category.YEME_ICME, occurredAt)

        val rows = rows()
        assertEquals("elle giriste tekrar korumasi calismamali", 2, rows.size)
        assertEquals(2, rows.map { it.sourceKey }.distinct().size)
    }

    @Test
    fun `kullanicinin sectigi kategori kullanici karari sayiliyor`() = runBlocking {
        repository.addManual(8_990, "Kantin", Category.EGLENCE, occurredAt)

        assertTrue(
            "kullanici bilerek sectiyse ogrenilen kural bunu ezmemeli",
            single().userEdited
        )
    }

    @Test
    fun `kategori secilmediginde isyeri adindan tahmin ediliyor`() = runBlocking {
        repository.addManual(24_590, "Migros Akatlar", null, occurredAt)

        val row = single()
        assertEquals(Category.MARKET.name, row.category)
        assertFalse(
            "tahmin kullanici karari degil; sonradan duzeltme bunu yakalayabilmeli",
            row.userEdited
        )
    }

    /** Elle giris de ogrenilen kuraldan yararlanmali, anahtar kelimeye dusmeden. */
    @Test
    fun `kategori secilmediginde once ogrenilen kural uygulaniyor`() = runBlocking {
        repository.addManual(8_990, UNKNOWN, null, occurredAt)
        repository.correctCategory(single(), Category.EGLENCE)

        repository.addManual(8_990, UNKNOWN, null, occurredAt + 1)

        val yeni = rows().maxBy { it.occurredAt }
        assertEquals(Category.EGLENCE.name, yeni.category)
    }

    @Test
    fun `bos isyeri null olarak kaydediliyor`() = runBlocking {
        repository.addManual(8_990, "   ", null, occurredAt)

        assertNull(
            "bos dizgi kaydedilirse liste bos bir isim gosterir; null 'Bilinmeyen isyeri' demek",
            single().merchant
        )
    }

    @Test
    fun `isyeri adindaki bosluklar kirpiliyor`() = runBlocking {
        repository.addManual(8_990, "  Kantin  ", null, occurredAt)

        assertEquals("Kantin", single().merchant)
    }

    @Test
    fun `elle giris aylik toplama giriyor`() = runBlocking {
        repository.addManual(8_990, "Kantin", Category.YEME_ICME, occurredAt)
        repository.addManual(24_590, "Migros", Category.MARKET, occurredAt)

        val cal = Calendar.getInstance().apply { timeInMillis = occurredAt }
        val total = repository.monthTotalMinor(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH))

        assertEquals(8_990L + 24_590L, total)
    }

    private suspend fun rows(): List<ExpenseEntity> =
        db.expenseDao().getBetween(0L, Long.MAX_VALUE)

    private suspend fun single(): ExpenseEntity = rows().single()

    private companion object {
        /** Anahtar kelime tablosunda yok - kategori yalnizca ogrenmeyle gelebilir. */
        const val UNKNOWN = "ZORLU PSM"
    }
}
