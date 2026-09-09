package com.bildirimbutce.app.ui.pro

import com.bildirimbutce.app.ui.MonthCursor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Ucretsiz surumun uc aylik penceresi (E bolumu).
 *
 * Saf JVM: kural takvimden ve yetkiden baska bir seye bakmiyor, Android
 * baglamina ihtiyaci yok.
 */
class ProLimitsTest {

    /** Agustos 2026; ay indeksi 0 tabanli, [MonthCursor] ile ayni sozlesme. */
    private val now = MonthCursor(2026, 7)

    @Test
    fun `ucretsiz surumde icinde bulunulan ay acik`() {
        assertTrue(ProLimits.canOpen(now, now, isPro = false))
    }

    @Test
    fun `ucretsiz surumde pencere ucuncu ayda bitiyor`() {
        // Pencere: Haziran, Temmuz, Agustos - icinde bulunulan ay dahil uc ay.
        assertTrue(ProLimits.canOpen(MonthCursor(2026, 6), now, isPro = false))
        assertTrue(ProLimits.canOpen(MonthCursor(2026, 5), now, isPro = false))
        assertFalse(ProLimits.canOpen(MonthCursor(2026, 4), now, isPro = false))
    }

    @Test
    fun `en eski ay pencerenin ilk ayi`() {
        assertEquals(MonthCursor(2026, 5), ProLimits.earliestMonth(now, isPro = false))
    }

    @Test
    fun `pencere yil sinirini geciyor`() {
        val ocak = MonthCursor(2026, 0)
        assertEquals(MonthCursor(2025, 10), ProLimits.earliestMonth(ocak, isPro = false))
        assertTrue(ProLimits.canOpen(MonthCursor(2025, 10), ocak, isPro = false))
        assertFalse(ProLimits.canOpen(MonthCursor(2025, 9), ocak, isPro = false))
    }

    @Test
    fun `pro surumde sinir yok`() {
        assertNull(ProLimits.earliestMonth(now, isPro = true))
        assertTrue(ProLimits.canOpen(MonthCursor(2019, 0), now, isPro = true))
    }

    /**
     * Gelecek aylar kisitli degil: sinir gecmisi satiyor, ileri gitmeyi degil.
     * Ucretsiz kullanici bir sonraki aya bakabilmeye devam etmeli.
     */
    @Test
    fun `gelecek aylar ucretsiz surumde de acik`() {
        assertTrue(ProLimits.canOpen(now.next(), now, isPro = false))
    }

    /** Pencere sayidan turuyor; sayi degisirse sinir da degismeli. */
    @Test
    fun `en eski ay sayidan hesaplaniyor`() {
        assertEquals(
            now.minus(ProLimits.FREE_MONTH_COUNT - 1),
            ProLimits.earliestMonth(now, isPro = false)
        )
    }
}
