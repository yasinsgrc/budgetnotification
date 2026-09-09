package com.bildirimbutce.app.widget

import com.bildirimbutce.app.data.db.ExpenseEntity
import com.bildirimbutce.app.expenseEntity
import com.bildirimbutce.app.millisAt
import com.bildirimbutce.app.ui.MonthCursor
import com.bildirimbutce.app.ui.toUiState
import com.bildirimbutce.parser.Category
import com.bildirimbutce.parser.TxKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 4x2 widget'in cizecegi satirlar (yol haritasi 12. madde).
 *
 * Saf JVM: `wideSnapshot` Android baglami istemiyor. Test edilen sey bicimleme
 * ve kisma kurallari; `RemoteViews`'in kendisi kapsam disi (4, 5, 6, 8, 9, 10 ve
 * 11. maddedeki gerekce - `compose-ui-test` yok, RemoteViews icin de bir kosum
 * takimi yok).
 */
class WideWidgetSnapshotTest {

    private val cursor = MonthCursor(2026, 7) // Agustos 2026
    private val now = millisAt(2026, 7, 9)

    private fun snapshot(vararg rows: ExpenseEntity) =
        rows.toList().toUiState(cursor, now).wideSnapshot(cursor)

    @Test
    fun `baslik ay adini ve yili tasiyor`() {
        // Turkce yerel ayar: varsayilanda "AGUSTOS" degil "AĞUSTOS" cikmali.
        assertEquals("AĞUSTOS 2026", snapshot().kicker)
    }

    @Test
    fun `bos ay sifir tutar ve hic satir gostermiyor`() {
        val snapshot = snapshot()
        assertEquals("0,00 ₺", snapshot.amount)
        assertTrue(snapshot.rows.isEmpty())
        assertNull(snapshot.change)
    }

    @Test
    fun `tutar bicimlenmis ve para birimi ekli`() {
        val snapshot = snapshot(
            expenseEntity(amountMinor = 182_180, occurredAt = millisAt(2026, 7, 3))
        )
        assertEquals("1.821,80 ₺", snapshot.amount)
    }

    @Test
    fun `satirlar buyukten kucuge diziliyor`() {
        val snapshot = snapshot(
            expenseEntity(amountMinor = 30_200, category = Category.FATURA, occurredAt = millisAt(2026, 7, 2)),
            expenseEntity(amountMinor = 78_000, category = Category.ULASIM, occurredAt = millisAt(2026, 7, 3)),
            expenseEntity(amountMinor = 44_900, category = Category.DIGER, occurredAt = millisAt(2026, 7, 4))
        )
        assertEquals(
            listOf(Category.ULASIM, Category.DIGER, Category.FATURA),
            snapshot.rows.map { it.category }
        )
        assertEquals(listOf("780,00", "449,00", "302,00"), snapshot.rows.map { it.amount })
        assertEquals("Ulaşım", snapshot.rows.first().label)
    }

    @Test
    fun `satir sayisi widget'a sigacak kadar kisiliyor`() {
        // Dort kategori var, widget uc satir tasiyor: en kucugu dusmeli.
        val snapshot = snapshot(
            expenseEntity(amountMinor = 78_000, category = Category.ULASIM, occurredAt = millisAt(2026, 7, 2)),
            expenseEntity(amountMinor = 44_900, category = Category.DIGER, occurredAt = millisAt(2026, 7, 3)),
            expenseEntity(amountMinor = 30_200, category = Category.FATURA, occurredAt = millisAt(2026, 7, 4)),
            expenseEntity(amountMinor = 10_000, category = Category.MARKET, occurredAt = millisAt(2026, 7, 5))
        )
        assertEquals(WIDE_WIDGET_ROW_COUNT, snapshot.rows.size)
        assertTrue(snapshot.rows.none { it.category == Category.MARKET })
    }

    @Test
    fun `iade kategori satirindan dusuluyor`() {
        val snapshot = snapshot(
            expenseEntity(amountMinor = 50_000, category = Category.MARKET, occurredAt = millisAt(2026, 7, 2)),
            expenseEntity(
                amountMinor = 20_000,
                kind = TxKind.REFUND,
                category = Category.MARKET,
                occurredAt = millisAt(2026, 7, 3)
            )
        )
        assertEquals("300,00", snapshot.rows.single().amount)
        assertEquals("300,00 ₺", snapshot.amount)
    }

    @Test
    fun `azalan ay asagi oklu rozet gosteriyor`() {
        val snapshot = snapshot(
            expenseEntity(amountMinor = 88_000, occurredAt = millisAt(2026, 7, 3)),
            // Onceki ay, karsilastirilan gun araliginda (ayin 9'undan once).
            expenseEntity(amountMinor = 100_000, occurredAt = millisAt(2026, 6, 3))
        )
        assertEquals("↓ %12", snapshot.change)
        assertEquals(false, snapshot.changeIncreased)
    }

    @Test
    fun `artan ay yukari oklu rozet gosteriyor`() {
        val snapshot = snapshot(
            expenseEntity(amountMinor = 112_000, occurredAt = millisAt(2026, 7, 3)),
            expenseEntity(amountMinor = 100_000, occurredAt = millisAt(2026, 6, 3))
        )
        assertEquals("↑ %12", snapshot.change)
        assertTrue(snapshot.changeIncreased)
    }

    @Test
    fun `karsilastirilacak ay yoksa rozet susuyor`() {
        val snapshot = snapshot(
            expenseEntity(amountMinor = 88_000, occurredAt = millisAt(2026, 7, 3))
        )
        assertNull(snapshot.change)
    }

    @Test
    fun `onceki ay listeye sizmiyor`() {
        val snapshot = snapshot(
            expenseEntity(amountMinor = 88_000, category = Category.ULASIM, occurredAt = millisAt(2026, 7, 3)),
            expenseEntity(amountMinor = 100_000, category = Category.MARKET, occurredAt = millisAt(2026, 6, 3))
        )
        assertEquals(listOf(Category.ULASIM), snapshot.rows.map { it.category })
        assertEquals("880,00 ₺", snapshot.amount)
    }
}
