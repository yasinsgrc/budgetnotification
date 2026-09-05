package com.bildirimbutce.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Rapor penceresinin ("son N ay") sinirlari.
 *
 * Rapor tek sorguyla N aylik veri cekiyor; pencere bir ay kaysa cubuklardan
 * biri eksik ya da fazla dolar ve kullanici olmayan bir egilim gorur.
 */
class LedgerRangeTest {

    @Test
    fun `tek aylik pencere ayin kendisiyle ayni`() {
        assertEquals(Ledger.monthRange(2026, 7), Ledger.rangeEndingAt(2026, 7, 1))
    }

    @Test
    fun `pencere secili ayin sonunda bitiyor`() {
        val (_, to) = Ledger.rangeEndingAt(2026, 7, 6)

        assertEquals(Ledger.monthRange(2026, 7).second, to)
    }

    @Test
    fun `alti aylik pencere bes ay geriden basliyor`() {
        val (from, _) = Ledger.rangeEndingAt(2026, 7, 6)

        // Agustos dahil geriye dogru alti ay: mart.
        assertEquals(Ledger.monthRange(2026, 2).first, from)
    }

    @Test
    fun `pencere yil sinirini asiyor`() {
        val (from, to) = Ledger.rangeEndingAt(2026, 0, 6)

        assertEquals(Ledger.monthRange(2025, 7).first, from)
        assertEquals(Ledger.monthRange(2026, 0).second, to)
        assertTrue(from < to)
    }

    /** Subat 29 cekerken pencerenin sonu kaymamali. */
    @Test
    fun `artik yil subatiyla biten pencere subatin sonunda bitiyor`() {
        val (_, to) = Ledger.rangeEndingAt(2028, 1, 6)

        assertEquals(Ledger.monthRange(2028, 1).second, to)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `sifir aylik pencere reddediliyor`() {
        Ledger.rangeEndingAt(2026, 7, 0)
    }
}
