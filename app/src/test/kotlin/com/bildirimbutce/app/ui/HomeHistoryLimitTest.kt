package com.bildirimbutce.app.ui

import com.bildirimbutce.app.ui.pro.ProLimits
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Ay imlecinin ucretsiz surumde durdugu yer.
 *
 * `ProLimitsTest` kurali sayi olarak dogruluyor; burada dogrulanan sey kuralin
 * gezinmeye **uygulanmasi**: imlecin sinirin otesine gecmemesi, yetki acikken
 * gecmesi ve okun ne zaman solacagi.
 *
 * "Simdi" disaridan veriliyor - `HomeViewModel` uretimde `MonthCursor.now()`
 * gecirir, test sabit bir ay verir. Sabitlenmeseydi bu testler ay basinda
 * kirmizi, ortasinda yesil yanardi.
 */
class HomeHistoryLimitTest {

    /** Agustos 2026; ay indeksi 0 tabanli. */
    private val now = MonthCursor(2026, 7)

    /** Ucretsiz pencerenin en eski ayi: Haziran 2026. */
    private val floor = now.minus(ProLimits.FREE_MONTH_COUNT - 1)

    @Test
    fun `ucretsiz surumde pencere icinde geri gidiliyor`() {
        assertEquals(MonthCursor(2026, 6), now.steppedBack(now, isPro = false))
        assertEquals(floor, MonthCursor(2026, 6).steppedBack(now, isPro = false))
    }

    @Test
    fun `ucretsiz surumde imlec sinirda duruyor`() {
        assertEquals("sinirda imlec yerinde kalmali", floor, floor.steppedBack(now, isPro = false))
    }

    @Test
    fun `pro surumde sinir yok`() {
        assertEquals(floor.previous(), floor.steppedBack(now, isPro = true))
    }

    @Test
    fun `sinir bayragi yalnizca son ayda yaniyor`() {
        assertFalse(now.atHistoryLimit(now, isPro = false))
        assertFalse(MonthCursor(2026, 6).atHistoryLimit(now, isPro = false))
        assertTrue(floor.atHistoryLimit(now, isPro = false))
    }

    @Test
    fun `pro surumde sinir bayragi hic yanmiyor`() {
        assertFalse(floor.atHistoryLimit(now, isPro = true))
        assertFalse(MonthCursor(2019, 0).atHistoryLimit(now, isPro = true))
    }

    /** Sinir yalnizca gecmise bakiyor; ileri gitmek ucretsiz surumde de serbest. */
    @Test
    fun `gelecek aydan geri donmek serbest`() {
        val future = now.next()

        assertFalse(future.atHistoryLimit(now, isPro = false))
        assertEquals(now, future.steppedBack(now, isPro = false))
    }

    /** Pencere yil sinirini gecerken de dogru yerde durmali. */
    @Test
    fun `sinir yil sinirinda da tutuyor`() {
        val ocak = MonthCursor(2026, 0)
        val kasim = MonthCursor(2025, 10)

        assertEquals(kasim, MonthCursor(2025, 11).steppedBack(ocak, isPro = false))
        assertEquals(kasim, kasim.steppedBack(ocak, isPro = false))
        assertTrue(kasim.atHistoryLimit(ocak, isPro = false))
    }
}
