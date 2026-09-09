package com.bildirimbutce.app.ui.nav

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Kilitli 4x2 widget'in actigi hedef (yol haritasi 12. madde).
 *
 * Saf JVM: karar `deepLinkTarget`'a ayrildigi icin Compose'a girmeden
 * dogrulanabiliyor - `startDestination` ile ayni gerekce (6. madde).
 */
class DeepLinkTest {

    @Test
    fun `widget paywall'i aciyor`() {
        assertEquals(Route.PAYWALL, deepLinkTarget(Route.PAYWALL, Route.HOME))
    }

    @Test
    fun `niyet bos gelirse hicbir yere gidilmiyor`() {
        assertNull(deepLinkTarget(null, Route.HOME))
    }

    @Test
    fun `beyaz listede olmayan adres yok sayiliyor`() {
        // Grafta karsiligi olmayan bir adres `navigate`'e verilseydi calisma
        // aninda patlardi; niyet uygulamanin disindan geliyor.
        assertNull(deepLinkTarget("bir-yerde-olmayan-rota", Route.HOME))
        assertNull(deepLinkTarget(Route.SETTINGS, Route.HOME))
    }

    @Test
    fun `kurulum bitmediyse paywall acilmiyor`() {
        // Onboarding'in ustune satin alma ekrani itmek, kullaniciyi izni hic
        // anlatmadan odeme sayfasinda birakirdi.
        assertNull(deepLinkTarget(Route.PAYWALL, Route.ONBOARDING))
    }
}
