package com.bildirimbutce.app.data

import com.bildirimbutce.app.testContext
import com.bildirimbutce.app.util.Prefs
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Pro yetkisinin kaliciligi ve satin alma yolunun bugunku cevabi (E bolumu).
 *
 * Kalicilik, bayragi yazan ornekten **farkli** yeni bir [StoredProAccess]
 * uzerinden okunuyor; ayni ornekten okumak yalnizca bellekteki alani test
 * ederdi ve uygulama yeniden acildiginda Pro'nun kaybolup kaybolmadigi
 * hakkinda hicbir sey soylemezdi.
 */
@RunWith(RobolectricTestRunner::class)
class ProAccessTest {

    private fun access() = StoredProAccess(Prefs(testContext()))

    @Test
    fun `yetki varsayilan olarak kapali`() {
        assertFalse(access().isPro.value)
    }

    @Test
    fun `yazilan yetki yeni bir ornekte de okunuyor`() {
        access().setEntitlement(true)

        assertTrue(access().isPro.value)
    }

    @Test
    fun `yetki geri alinabiliyor`() {
        access().setEntitlement(true)
        access().setEntitlement(false)

        assertFalse(access().isPro.value)
    }

    /**
     * Yetki uygulamanin disinda degisebildigi icin ekranlar [ProAccess.refresh]
     * cagiriyor. Akis yalnizca kurulusta okusaydi, paywall'dan donen ana ekran
     * eski cevabi gostermeye devam ederdi.
     */
    @Test
    fun `refresh diskteki degisikligi akisa tasiyor`() {
        val subject = access()
        assertFalse(subject.isPro.value)

        Prefs(testContext()).isPro = true
        assertFalse("refresh cagrilmadan akis degismemeli", subject.isPro.value)

        subject.refresh()
        assertTrue(subject.isPro.value)
    }

    /**
     * Satin alma yolu yok ve bunu sessizce yapmiyor: cevap sebebini tasiyor,
     * ekran da onu oldugu gibi basiyor.
     */
    @Test
    fun `satin alma ve geri yukleme sebebiyle birlikte reddediyor`() = runTest {
        val subject = access()

        val purchase = subject.purchase()
        val restore = subject.restore()

        assertTrue(purchase is PurchaseOutcome.Unavailable)
        assertTrue(restore is PurchaseOutcome.Unavailable)
        assertTrue((purchase as PurchaseOutcome.Unavailable).reason.isNotBlank())
        assertEquals(purchase.reason, (restore as PurchaseOutcome.Unavailable).reason)
    }

    /** Reddedilen bir satin alma yetkiyi acmamali. */
    @Test
    fun `basarisiz satin alma yetkiyi degistirmiyor`() = runTest {
        val subject = access()

        subject.purchase()

        assertFalse(subject.isPro.value)
        assertFalse(access().isPro.value)
    }
}
