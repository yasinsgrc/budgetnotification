package com.bildirimbutce.app.ui

import com.bildirimbutce.app.expenseEntity
import com.bildirimbutce.app.millisAt
import com.bildirimbutce.parser.TxKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Aylik degisim rozeti ("↓ %12 TEMMUZ").
 *
 * Rozet tek bir sayi gosteriyor ama iki karar tasiyor: neye bolundugu ve ne
 * zaman susmasi gerektigi. Ikisi de yanlis olursa rozet kullaniciya yalan
 * soyler - "harcamalarin ucte bire dustu" diyen bir ekran, ayin ucte birinde
 * durdugunu soylemedigi surece yanlistir.
 *
 * Android baglami gerekmez: [toUiState] saf bir donusum, "simdi" disaridan
 * veriliyor.
 */
class MonthChangeTest {

    private val agustos = MonthCursor(2026, 7)

    /** Agustos kapandi: eylulden bakiliyor, karsilastirma ayin tamamini kapsar. */
    private val agustosKapandi = millisAt(2026, 8, 5)

    /** Agustos hala acik: ayin dokuzuncu gunu. */
    private val agustosunDokuzu = millisAt(2026, 7, 9)

    @Test
    fun `onceki aydan az harcandiysa rozet asagi bakiyor`() {
        val state = listOf(
            expenseEntity(amountMinor = 100_000, occurredAt = millisAt(2026, 6, 10)),
            expenseEntity(amountMinor = 88_000, occurredAt = millisAt(2026, 7, 10))
        ).toUiState(agustos, agustosKapandi)

        val change = requireNotNull(state.change)
        assertEquals(12, change.percent)
        assertEquals(false, change.increased)
        assertEquals("TEMMUZ", change.previousLabel)
        assertNull("kapanmis ayda karsilastirma kisilmaz", change.comparedDays)
    }

    @Test
    fun `onceki aydan cok harcandiysa rozet yukari bakiyor`() {
        val state = listOf(
            expenseEntity(amountMinor = 100_000, occurredAt = millisAt(2026, 6, 10)),
            expenseEntity(amountMinor = 131_000, occurredAt = millisAt(2026, 7, 10))
        ).toUiState(agustos, agustosKapandi)

        val change = requireNotNull(state.change)
        assertEquals(31, change.percent)
        assertTrue(change.increased)
    }

    /**
     * Rozetin en kritik kurali. Ayin dokuzunda, kapanmis bir ayin tamamiyla
     * karsilastirma yapilsaydi kullanici her ayin basinda "harcamalarim
     * dustu" diye sevinir, ay sonunda sayinin tersine dondugunu gorurdu.
     */
    @Test
    fun `ay bitmemisken onceki ayin yalnizca ayni gunune kadari sayiliyor`() {
        val state = listOf(
            expenseEntity(amountMinor = 50_000, occurredAt = millisAt(2026, 6, 5)),
            expenseEntity(amountMinor = 150_000, occurredAt = millisAt(2026, 6, 20)),
            expenseEntity(amountMinor = 40_000, occurredAt = millisAt(2026, 7, 3))
        ).toUiState(agustos, agustosunDokuzu)

        assertEquals("yalnizca temmuzun ilk dokuz gunu", 50_000L, state.previousTotalMinor)
        val change = requireNotNull(state.change)
        assertEquals(20, change.percent)
        assertEquals(false, change.increased)
        assertEquals(9, change.comparedDays)
    }

    @Test
    fun `ay kapandiysa onceki ayin tamami sayiliyor`() {
        val state = listOf(
            expenseEntity(amountMinor = 50_000, occurredAt = millisAt(2026, 6, 5)),
            expenseEntity(amountMinor = 150_000, occurredAt = millisAt(2026, 6, 20)),
            expenseEntity(amountMinor = 40_000, occurredAt = millisAt(2026, 7, 3))
        ).toUiState(agustos, agustosKapandi)

        assertEquals(200_000L, state.previousTotalMinor)
        assertEquals(80, requireNotNull(state.change).percent)
    }

    @Test
    fun `onceki ayda kayit yoksa rozet cizilmiyor`() {
        val state = listOf(
            expenseEntity(amountMinor = 88_000, occurredAt = millisAt(2026, 7, 10))
        ).toUiState(agustos, agustosKapandi)

        assertEquals(0L, state.previousTotalMinor)
        assertNull(state.change)
    }

    /** Bolen sifir ya da negatifse yuzde diye bir sey yok. */
    @Test
    fun `onceki ay iadeyle negatif kapandiysa rozet cizilmiyor`() {
        val state = listOf(
            expenseEntity(amountMinor = 10_000, occurredAt = millisAt(2026, 6, 10)),
            expenseEntity(
                amountMinor = 30_000,
                kind = TxKind.REFUND,
                occurredAt = millisAt(2026, 6, 12)
            ),
            expenseEntity(amountMinor = 88_000, occurredAt = millisAt(2026, 7, 10))
        ).toUiState(agustos, agustosKapandi)

        assertTrue(state.previousTotalMinor < 0)
        assertNull(state.change)
    }

    /** Acik ay net negatifse yuzde artik harcamayi anlatmiyor. */
    @Test
    fun `secili ay iadeyle negatife dustuyse rozet cizilmiyor`() {
        val state = listOf(
            expenseEntity(amountMinor = 100_000, occurredAt = millisAt(2026, 6, 10)),
            expenseEntity(amountMinor = 10_000, occurredAt = millisAt(2026, 7, 3)),
            expenseEntity(
                amountMinor = 30_000,
                kind = TxKind.REFUND,
                occurredAt = millisAt(2026, 7, 4)
            )
        ).toUiState(agustos, agustosKapandi)

        assertTrue(state.totalMinor < 0)
        assertNull(state.change)
    }

    /** "↓ %0" diye bir rozet yok; fark yuzde yarimin altindaysa rozet susuyor. */
    @Test
    fun `fark yuzde sifira yuvarlaniyorsa rozet cizilmiyor`() {
        val state = listOf(
            expenseEntity(amountMinor = 100_000, occurredAt = millisAt(2026, 6, 10)),
            expenseEntity(amountMinor = 100_200, occurredAt = millisAt(2026, 7, 10))
        ).toUiState(agustos, agustosKapandi)

        assertEquals(
            "karsilastirma yapildi, yalnizca rozet susuyor",
            100_000L,
            state.previousTotalMinor
        )
        assertNull(state.change)
    }

    /**
     * Ay adi Turkce buyuk harf kuraliyla yaziliyor: yerel ayar verilmeseydi
     * "NİSAN" yerine "NISAN" cikardi.
     */
    @Test
    fun `rozetteki ay adi Turkce buyuk harfle yaziliyor`() {
        val state = listOf(
            expenseEntity(amountMinor = 100_000, occurredAt = millisAt(2026, 3, 10)),
            expenseEntity(amountMinor = 50_000, occurredAt = millisAt(2026, 4, 10))
        ).toUiState(MonthCursor(2026, 4), millisAt(2026, 5, 2))

        assertEquals("NİSAN", requireNotNull(state.change).previousLabel)
    }

    /**
     * Pencere iki ay tasiyor ama ekran tek ay gosteriyor. Suzgec bozulursa
     * kullanici gecen ayin harcamalarini bu ayin listesinde ve toplaminda
     * gorur - rozetin anlatmaya calistigi seyin tam tersi.
     */
    @Test
    fun `onceki ayin kayitlari listeye ve toplama girmiyor`() {
        val gecenAy = expenseEntity(amountMinor = 100_000, occurredAt = millisAt(2026, 6, 10))
        val buAy = expenseEntity(amountMinor = 88_000, occurredAt = millisAt(2026, 7, 10))

        val state = listOf(gecenAy, buAy).toUiState(agustos, agustosKapandi)

        assertEquals(listOf(buAy), state.expenses)
        assertEquals(88_000L, state.totalMinor)
        assertEquals(88_000L, state.byCategory.single().second)
    }
}
