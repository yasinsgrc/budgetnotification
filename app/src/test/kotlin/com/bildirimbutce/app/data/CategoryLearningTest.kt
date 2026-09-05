package com.bildirimbutce.app.data

import com.bildirimbutce.app.data.db.AppDatabase
import com.bildirimbutce.app.data.db.ExpenseEntity
import com.bildirimbutce.app.expenseEntity
import com.bildirimbutce.app.hourBucketStart
import com.bildirimbutce.app.inMemoryDb
import com.bildirimbutce.app.parsedTx
import com.bildirimbutce.parser.Category
import com.bildirimbutce.parser.Ledger
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

/**
 * Kategori duzeltmesi ve magaza -> kategori ogrenmesi.
 *
 * Urunun "dogruluk kullanimla artar" iddiasi tamamen buna dayaniyor:
 * kullanici bir magazayi bir kez duzeltirse bir daha sorulmamali. Ogrenme
 * bozulursa siniflandirici kalici olarak anahtar kelime tablosu kadar kalir.
 */
@RunWith(RobolectricTestRunner::class)
class CategoryLearningTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: ExpenseRepository

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
    fun `bilinmeyen magaza once DIGER olarak kaydediliyor`() = runBlocking {
        repository.record(parsedTx(merchant = UNKNOWN), SOURCE, bucketStart)

        assertEquals(Category.DIGER.name, single().category)
    }

    @Test
    fun `duzeltilen kayit kullanici duzenlemesi olarak isaretleniyor`() = runBlocking {
        repository.record(parsedTx(merchant = UNKNOWN), SOURCE, bucketStart)

        repository.correctCategory(single(), Category.EGLENCE)

        val row = single()
        assertEquals(Category.EGLENCE.name, row.category)
        assertTrue("duzeltilen kayit bir daha otomatik degismemeli", row.userEdited)
    }

    @Test
    fun `duzeltme sonrasi ayni magazadan gelen yeni harcama ogrenilen kategoriyi aliyor`() =
        runBlocking {
            repository.record(parsedTx(merchant = UNKNOWN), SOURCE, bucketStart)
            repository.correctCategory(single(), Category.EGLENCE)

            repository.record(parsedTx(merchant = UNKNOWN), SOURCE, bucketStart + Ledger.HOUR_MILLIS)

            val yeni = rows().maxBy { it.occurredAt }
            assertEquals(
                "ogrenilen kural anahtar kelime tahmininin onune gecmeli",
                Category.EGLENCE.name,
                yeni.category
            )
        }

    @Test
    fun `ogrenilen kural buyuk kucuk harf ve bosluk farkindan etkilenmiyor`() = runBlocking {
        repository.record(parsedTx(merchant = UNKNOWN), SOURCE, bucketStart)
        repository.correctCategory(single(), Category.EGLENCE)

        repository.record(
            parsedTx(merchant = "  zorlu psm "),
            SOURCE,
            bucketStart + Ledger.HOUR_MILLIS
        )

        val yeni = rows().maxBy { it.occurredAt }
        assertEquals(Category.EGLENCE.name, yeni.category)
    }

    @Test
    fun `duzeltme gecmisteki ayni magaza kayitlarini da guncelliyor`() = runBlocking {
        val dao = db.expenseDao()
        dao.insert(expenseEntity(merchant = UNKNOWN, category = Category.DIGER, occurredAt = 1))
        dao.insert(expenseEntity(merchant = UNKNOWN, category = Category.DIGER, occurredAt = 2))

        repository.correctCategory(rows().first { it.occurredAt == 1L }, Category.EGLENCE)

        assertTrue(
            "gecmis kayitlar da duzeltilmeli, yoksa kullanici ayni isi tekrar tekrar yapar",
            rows().all { it.category == Category.EGLENCE.name }
        )
    }

    @Test
    fun `duzeltme kullanicinin elle degistirdigi eski kaydi ezmiyor`() = runBlocking {
        val dao = db.expenseDao()
        dao.insert(
            expenseEntity(
                merchant = UNKNOWN,
                category = Category.SAGLIK,
                occurredAt = 1,
                userEdited = true
            )
        )
        dao.insert(expenseEntity(merchant = UNKNOWN, category = Category.DIGER, occurredAt = 2))

        repository.correctCategory(rows().first { it.occurredAt == 2L }, Category.EGLENCE)

        assertEquals(
            "kullanicinin bilerek verdigi karar korunmali",
            Category.SAGLIK.name,
            rows().first { it.occurredAt == 1L }.category
        )
    }

    @Test
    fun `duzeltme baska magazalara bulasmiyor`() = runBlocking {
        val dao = db.expenseDao()
        dao.insert(expenseEntity(merchant = UNKNOWN, category = Category.DIGER, occurredAt = 1))
        dao.insert(expenseEntity(merchant = "MIGROS", category = Category.MARKET, occurredAt = 2))

        repository.correctCategory(rows().first { it.occurredAt == 1L }, Category.EGLENCE)

        val migros = rows().first { it.merchant == "MIGROS" }
        assertEquals(Category.MARKET.name, migros.category)
        assertFalse(migros.userEdited)
    }

    @Test
    fun `magazasi olmayan kayit duzeltilebiliyor ama kural ogrenilmiyor`() = runBlocking {
        db.expenseDao().insert(
            expenseEntity(merchant = null, category = Category.DIGER, occurredAt = 1)
        )

        repository.correctCategory(single(), Category.FATURA)

        assertEquals(Category.FATURA.name, single().category)
        assertNull(
            "isimsiz kayit icin kural yazilsaydi tum isimsizler ayni kategoriye duserdi",
            db.merchantRuleDao().find("")
        )
    }

    private suspend fun rows(): List<ExpenseEntity> =
        db.expenseDao().getBetween(0L, Long.MAX_VALUE)

    private suspend fun single(): ExpenseEntity = rows().single()

    private companion object {
        const val SOURCE = "com.garanti.cepsubesi"

        /** Anahtar kelime tablosunda yok - kategori yalnizca ogrenmeyle gelebilir. */
        const val UNKNOWN = "ZORLU PSM"
    }
}
