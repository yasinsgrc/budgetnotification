package com.bildirimbutce.app.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Kesikli kenarligin kose geometrisi (yol haritasi 14. madde).
 *
 * Saf JVM: cizimin kendisi test edilemiyor (`compose-ui-test` bagimliligi yok),
 * ama cizginin iceri yaslanmasindan dogan yaricap kurali sayidan ibaret ve
 * sessizce bozulabilecek tek parca o. Kural bozulursa kenarlik ya kosede
 * kalinlasir ya da `clip` tarafindan kesilir - ikisi de ekranda fark edilmesi
 * zor, testte kolay.
 */
class DashedBorderTest {

    @Test
    fun `yaricap cizginin yarisi kadar kuculuyor`() {
        // Stroke yolun uzerine ortalanir: 1px cizgi yarim piksel iceri kayar,
        // kose de ayni miktarda kuculmezse es merkezli olmaz.
        assertEquals(19.5f, insetCornerRadius(20f, 1f), 0.001f)
    }

    @Test
    fun `kalin cizgi yaricapi daha cok kucultuyor`() {
        assertEquals(17f, insetCornerRadius(20f, 6f), 0.001f)
    }

    @Test
    fun `cizgisiz durumda yaricap aynen kaliyor`() {
        assertEquals(20f, insetCornerRadius(20f, 0f), 0.001f)
    }

    @Test
    fun `yaricap negatife dusmuyor`() {
        // Kalin cizgi + kucuk yaricap: coerce olmasaydi `drawRoundRect`
        // negatif yaricapla cizerdi.
        assertEquals(0f, insetCornerRadius(1f, 8f), 0.001f)
        assertEquals(0f, insetCornerRadius(0f, 1f), 0.001f)
    }
}
