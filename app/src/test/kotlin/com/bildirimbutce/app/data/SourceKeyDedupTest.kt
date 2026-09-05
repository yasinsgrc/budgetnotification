package com.bildirimbutce.app.data

import com.bildirimbutce.app.data.db.AppDatabase
import com.bildirimbutce.app.expenseEntity
import com.bildirimbutce.app.hourBucketStart
import com.bildirimbutce.app.inMemoryDb
import com.bildirimbutce.app.parsedTx
import com.bildirimbutce.parser.Ledger
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Tekrar korumasi.
 *
 * Android ayni bildirimi guncellendiginde saniyeler icinde tekrar teslim eder.
 * Koruma calismazsa kullanici tek harcamayi listede 3-4 kez gorur; asiri
 * korursa ayni magazadan yapilan gercek ikinci alisveris kaybolur. Iki taraf
 * da burada olculuyor.
 */
@RunWith(RobolectricTestRunner::class)
class SourceKeyDedupTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: ExpenseRepository

    /** Kova sinirina hizali sabit bir an - testler kosma saatine gore kaymasin. */
    private val bucketStart = hourBucketStart(1_700_000_000_000L)

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
    fun `ayni bildirim ayni saat icinde tekrar gelirse tek kayit kalir`() = runBlocking {
        val tx = parsedTx()

        assertTrue("ilk teslim kaydedilmeli", repository.record(tx, SOURCE, bucketStart + 60_000))
        assertFalse(
            "ayni saat kovasindaki tekrar teslim yok sayilmali",
            repository.record(tx, SOURCE, bucketStart + 120_000)
        )
        assertFalse(
            "kovanin sonuna kadar hala ayni bildirim",
            repository.record(tx, SOURCE, bucketStart + Ledger.HOUR_MILLIS - 1)
        )

        assertEquals(1, allRows().size)
    }

    @Test
    fun `bir sonraki saatteki gercek alisveris kaydediliyor`() = runBlocking {
        val tx = parsedTx()

        assertTrue(repository.record(tx, SOURCE, bucketStart + 60_000))
        assertTrue(
            "sonraki kova ayri bir alisveris sayilmali, yoksa gercek harcama kaybolur",
            repository.record(tx, SOURCE, bucketStart + Ledger.HOUR_MILLIS)
        )

        assertEquals(2, allRows().size)
    }

    @Test
    fun `ayni saatteki farkli harcamalar ayri kayit olur`() = runBlocking {
        assertTrue(repository.record(parsedTx(merchant = "MIGROS"), SOURCE, bucketStart + 60_000))
        assertTrue(repository.record(parsedTx(merchant = "SHELL"), SOURCE, bucketStart + 60_000))

        assertEquals(2, allRows().size)
    }

    @Test
    fun `ayni metin farkli uygulamadan gelirse ayri kayit olur`() = runBlocking {
        val tx = parsedTx()

        assertTrue(repository.record(tx, SOURCE, bucketStart + 60_000))
        assertTrue(
            "banka uygulamasi ve SMS uygulamasi ayri kaynak sayilir",
            repository.record(tx, "com.android.mms", bucketStart + 60_000)
        )

        assertEquals(2, allRows().size)
    }

    @Test
    fun `sourceKey ayni kovada sabit farkli kovada degisik`() {
        val text = "1234 kartiniz ile MIGROS isyerinde 245,90 TL harcama"

        assertEquals(
            Ledger.sourceKey(SOURCE, text, bucketStart + 1),
            Ledger.sourceKey(SOURCE, text, bucketStart + Ledger.HOUR_MILLIS - 1)
        )
        assertNotEquals(
            Ledger.sourceKey(SOURCE, text, bucketStart),
            Ledger.sourceKey(SOURCE, text, bucketStart + Ledger.HOUR_MILLIS)
        )
    }

    /**
     * Korumanin gercek dayanagi Room'daki unique index; repository katmani
     * atlansa bile veritabani ikinci kaydi reddetmeli.
     */
    @Test
    fun `unique index dogrudan DAO seviyesinde de tekrari reddediyor`() = runBlocking {
        val dao = db.expenseDao()
        val row = expenseEntity(sourceKey = "ayni-anahtar")

        assertNotEquals(-1L, dao.insert(row))
        assertEquals(
            "IGNORE stratejisi ile ikinci insert -1 donmeli",
            -1L,
            dao.insert(row.copy(amountMinor = 999))
        )
        assertEquals(1, allRows().size)
    }

    private suspend fun allRows() = db.expenseDao().getBetween(0L, Long.MAX_VALUE)

    private companion object {
        const val SOURCE = "com.garanti.cepsubesi"
    }
}
