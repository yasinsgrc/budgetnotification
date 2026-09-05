package com.bildirimbutce.app.ui.onboarding

import com.bildirimbutce.app.ui.nav.Route
import com.bildirimbutce.app.ui.nav.startDestination
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Onboarding'in sayfa sirasi ve baslangic hedefi.
 *
 * Ekranin cizimi degil, akisin kurallari test ediliyor: hangi sayfadan hangi
 * sayfaya gecildigi ve uygulamanin hangi hedefle acildigi. Ikisi de Android
 * baglami olmadan kararlastirilabildigi icin emulator gerekmiyor.
 */
class OnboardingFlowTest {

    @Test
    fun `ilk acilista onboarding aciliyor`() {
        assertEquals(Route.ONBOARDING, startDestination(onboardingDone = false))
    }

    @Test
    fun `akis bir kez gorulduyse dogrudan deftere giriliyor`() {
        assertEquals(Route.HOME, startDestination(onboardingDone = true))
    }

    @Test
    fun `sayfalar A1 A2 A3 sirasiyla ilerliyor`() {
        assertEquals(OnboardingPage.PERMISSION, OnboardingPage.INTRO.next())
        assertEquals(OnboardingPage.READY, OnboardingPage.PERMISSION.next())
    }

    /**
     * Son sayfada "ileri" yok. `null` donmesi, cikisin sayfa degistirmek degil
     * akisi bitirmek (bayragi yazip deftere gecmek) oldugunu soyluyor.
     */
    @Test
    fun `son sayfadan sonrasi yok`() {
        assertNull(OnboardingPage.READY.next())
    }
}
