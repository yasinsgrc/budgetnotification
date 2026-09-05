package com.bildirimbutce.app.ui.onboarding

import com.bildirimbutce.app.testContext
import com.bildirimbutce.app.ui.nav.Route
import com.bildirimbutce.app.ui.nav.startDestination
import com.bildirimbutce.app.util.Prefs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * "Goruldu" bayraginin kaliciligi.
 *
 * Bayrak yalnizca bellekte dursaydi akis her acilista bastan gorunurdu -
 * uygulamanin ilk izlenimini her seferinde kurulum ekranina cevirirdi. Test
 * bunu, bayragi yazan ornekten farkli yeni bir [Prefs] ornegiyle okuyarak
 * dogruluyor; ayni ornek uzerinden okumak yalnizca alani test ederdi.
 */
@RunWith(RobolectricTestRunner::class)
class OnboardingSeenFlagTest {

    @Test
    fun `bayrak varsayilan olarak kapali`() {
        assertFalse(Prefs(testContext()).onboardingDone)
    }

    @Test
    fun `yazilan bayrak yeni bir ornekte de okunuyor`() {
        Prefs(testContext()).onboardingDone = true

        assertTrue(Prefs(testContext()).onboardingDone)
    }

    @Test
    fun `akis bittikten sonra uygulama deftere aciliyor`() {
        val prefs = Prefs(testContext())
        assertEquals(Route.ONBOARDING, startDestination(prefs.onboardingDone))

        prefs.onboardingDone = true

        assertEquals(Route.HOME, startDestination(Prefs(testContext()).onboardingDone))
    }
}
