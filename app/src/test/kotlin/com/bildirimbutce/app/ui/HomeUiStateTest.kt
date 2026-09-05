package com.bildirimbutce.app.ui

import com.bildirimbutce.app.data.db.ExpenseEntity
import com.bildirimbutce.app.expenseEntity
import com.bildirimbutce.parser.Category
import com.bildirimbutce.parser.TxKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Ana ekranin toplami ve kategori dagilimi.
 *
 * Kritik kural: iade (REFUND) toplamdan dusulur. Yanlis olursa kullaniciya
 * ayin toplami oldugundan yuksek gosterilir - uygulamanin tek isi bu sayiyi
 * dogru vermek.
 *
 * Android baglami gerekmez: toUiState saf bir donusum.
 */
class HomeUiStateTest {

    @Test
    fun `bos liste sifir toplam uretiyor`() {
        val state = emptyList<ExpenseEntity>().toUiState()

        assertEquals(0L, state.totalMinor)
        assertTrue(state.byCategory.isEmpty())
    }

    @Test
    fun `iade toplamdan dusuluyor`() {
        val state = listOf(
            expenseEntity(amountMinor = 24_590, kind = TxKind.EXPENSE),
            expenseEntity(amountMinor = 120_000, kind = TxKind.EXPENSE, occurredAt = 1),
            expenseEntity(amountMinor = 4_500, kind = TxKind.REFUND, occurredAt = 2)
        ).toUiState()

        assertEquals(24_590L + 120_000L - 4_500L, state.totalMinor)
    }

    @Test
    fun `iade kendi kategorisinden dusuluyor`() {
        val state = listOf(
            expenseEntity(merchant = "MIGROS", amountMinor = 24_590, category = Category.MARKET),
            expenseEntity(
                merchant = "MIGROS",
                amountMinor = 4_500,
                kind = TxKind.REFUND,
                category = Category.MARKET,
                occurredAt = 1
            ),
            expenseEntity(
                merchant = "SHELL",
                amountMinor = 120_000,
                category = Category.ULASIM,
                occurredAt = 2
            )
        ).toUiState()

        val market = state.byCategory.first { it.first == Category.MARKET }
        assertEquals(24_590L - 4_500L, market.second)
    }

    @Test
    fun `kategoriler buyukten kucuge siralaniyor`() {
        val state = listOf(
            expenseEntity(amountMinor = 10_000, category = Category.MARKET),
            expenseEntity(amountMinor = 90_000, category = Category.ULASIM, occurredAt = 1),
            expenseEntity(amountMinor = 50_000, category = Category.FATURA, occurredAt = 2)
        ).toUiState()

        assertEquals(
            listOf(Category.ULASIM, Category.FATURA, Category.MARKET),
            state.byCategory.map { it.first }
        )
    }

    /**
     * Iade harcamadan buyukse kategori negatife duser; negatif dilim cizilemez,
     * bu yuzden listeden dusuyor. Toplamda ise gorunmeye devam etmeli.
     */
    @Test
    fun `net negatif kategori dagilimda gosterilmiyor ama toplami etkiliyor`() {
        val state = listOf(
            expenseEntity(amountMinor = 4_500, category = Category.MARKET),
            expenseEntity(
                amountMinor = 24_590,
                kind = TxKind.REFUND,
                category = Category.MARKET,
                occurredAt = 1
            ),
            expenseEntity(amountMinor = 90_000, category = Category.ULASIM, occurredAt = 2)
        ).toUiState()

        assertTrue(state.byCategory.none { it.first == Category.MARKET })
        assertEquals(4_500L - 24_590L + 90_000L, state.totalMinor)
    }

    @Test
    fun `taninmayan kategori adi DIGER olarak gruplaniyor`() {
        val bozuk = expenseEntity(amountMinor = 1_000).copy(category = "ARTIK_YOK")

        val state = listOf(bozuk).toUiState()

        assertEquals(Category.DIGER, state.byCategory.single().first)
    }

    @Test
    fun `kayitlar durumda oldugu gibi tasiniyor`() {
        val rows = listOf(
            expenseEntity(amountMinor = 1_000),
            expenseEntity(amountMinor = 2_000, occurredAt = 1)
        )

        assertEquals(rows, rows.toUiState().expenses)
    }
}
