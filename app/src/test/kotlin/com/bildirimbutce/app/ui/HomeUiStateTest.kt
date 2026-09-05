package com.bildirimbutce.app.ui

import com.bildirimbutce.app.data.db.ExpenseEntity
import com.bildirimbutce.app.expenseEntity
import com.bildirimbutce.parser.Category
import com.bildirimbutce.parser.Ledger
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

    /**
     * Izin kapaliyken (B3) yalnizca elle girilenler gosteriliyor. Suzgec
     * bozulursa kullanici, bildirim erisimi kapaliyken bildirimden gelmis
     * kayitlar gorur ve sistemin hala calistigini saniyor.
     */
    @Test
    fun `elle girilenler ayri suzuluyor`() {
        val elle = expenseEntity(amountMinor = 8_990, sourceApp = Ledger.MANUAL_SOURCE)
        val bildirimden = expenseEntity(amountMinor = 24_590, occurredAt = 1)

        val state = listOf(elle, bildirimden).toUiState()

        assertEquals(listOf(elle), state.manualExpenses)
    }

    @Test
    fun `elle girilenlerin toplami yalnizca kendilerini sayiyor`() {
        val state = listOf(
            expenseEntity(amountMinor = 8_990, sourceApp = Ledger.MANUAL_SOURCE),
            expenseEntity(amountMinor = 1_000, occurredAt = 1, sourceApp = Ledger.MANUAL_SOURCE),
            expenseEntity(amountMinor = 24_590, occurredAt = 2)
        ).toUiState()

        assertEquals(8_990L + 1_000L, state.manualTotalMinor)
        assertEquals(
            "ay toplami her iki kaynagi da saymaya devam etmeli",
            8_990L + 1_000L + 24_590L,
            state.totalMinor
        )
    }

    @Test
    fun `elle girilmis iade kendi toplamindan dusuluyor`() {
        val state = listOf(
            expenseEntity(amountMinor = 8_990, sourceApp = Ledger.MANUAL_SOURCE),
            expenseEntity(
                amountMinor = 1_000,
                kind = TxKind.REFUND,
                occurredAt = 1,
                sourceApp = Ledger.MANUAL_SOURCE
            )
        ).toUiState()

        assertEquals(8_990L - 1_000L, state.manualTotalMinor)
    }

    @Test
    fun `elle giris yoksa liste bos`() {
        val state = listOf(expenseEntity(amountMinor = 24_590)).toUiState()

        assertTrue(state.manualExpenses.isEmpty())
        assertEquals(0L, state.manualTotalMinor)
    }
}
