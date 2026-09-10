package com.bildirimbutce.app.ui.theme

import android.text.Spanned
import android.text.style.TypefaceSpan
import androidx.compose.ui.text.font.FontFamily
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * ₺ (U+20BA) icin sistem fontu yedegi (yol haritasi 15. madde).
 *
 * Saf JVM: cizimin kendisi test edilemiyor (`compose-ui-test` bagimliligi yok),
 * ama sessizce bozulabilecek parca cizim degil - **hangi araligin** sistem
 * fontuna dustugu. Aralik bir karakter kayarsa ekranda ya rakamlardan biri
 * Roboto'ya duser (fark edilmesi zor) ya da simge yine Grotesk'te kalir ve
 * yanlis para birimi cizilir (fark edilmesi kolay, ama ancak cihazda).
 */
class LiraFallbackTest {

    @Test
    fun `tek basina simge tumuyle sistem fontuna dusuyor`() {
        val text = withLiraFallback(LIRA)
        assertEquals(LIRA, text.text)
        assertEquals(1, text.spanStyles.size)
        assertEquals(0, text.spanStyles[0].start)
        assertEquals(1, text.spanStyles[0].end)
        assertEquals(FontFamily.Default, text.spanStyles[0].item.fontFamily)
    }

    @Test
    fun `tutarin ardindaki simge yalnizca kendi karakterini kapsiyor`() {
        // "1.821,80 ₺" - rakamlar Grotesk'te kalmali, aksi halde tabular
        // figurler bozulur ve tutar degisirken sayi titrer.
        val text = withLiraFallback("1.821,80 $LIRA")
        assertEquals(1, text.spanStyles.size)
        assertEquals(9, text.spanStyles[0].start)
        assertEquals(10, text.spanStyles[0].end)
    }

    @Test
    fun `bastaki simge de yakalaniyor`() {
        // Rapor ekranindaki "₺ / gun · 10 gun" satiri.
        val text = withLiraFallback("$LIRA / gün · 10 gün")
        assertEquals(1, text.spanStyles.size)
        assertEquals(0, text.spanStyles[0].start)
        assertEquals(1, text.spanStyles[0].end)
    }

    @Test
    fun `birden fazla simge ayri ayri yakalaniyor`() {
        val text = withLiraFallback("$LIRA-$LIRA")
        assertEquals(2, text.spanStyles.size)
        assertEquals(0, text.spanStyles[0].start)
        assertEquals(1, text.spanStyles[0].end)
        assertEquals(2, text.spanStyles[1].start)
        assertEquals(3, text.spanStyles[1].end)
    }

    @Test
    fun `simge yoksa hicbir aralik isaretlenmiyor`() {
        // Aksi halde simgesiz her metin bosuna Roboto'ya dusme riski tasirdi.
        val text = withLiraFallback("MARKET · 21 AĞU 18:42")
        assertTrue(text.spanStyles.isEmpty())
    }

    @Test
    fun `pound isareti yedege dusmuyor`() {
        // Hatanin kendisi ₺'nin £ gibi cizilmesiydi; gercek £ varsa (bugun yok,
        // yarin coklu para birimi gelirse olabilir) ona dokunulmamali.
        val text = withLiraFallback("12,00 £")
        assertTrue(text.spanStyles.isEmpty())
    }
}

/**
 * Ayni kuralin `RemoteViews` tarafi (widget'lar).
 *
 * Robolectric: [SpannableString] ve [TypefaceSpan] Android siniflari, saf JVM'de
 * kurulamiyorlar. Test edilen sey span'in **nereye** kondugu; widget'in cizimi
 * yine kapsam disi.
 */
@RunWith(RobolectricTestRunner::class)
class LiraSpannedTest {

    @Test
    fun `simge araligi sistem fontuna dusuyor`() {
        val spanned = liraSpanned("1.821,80 $LIRA") as Spanned
        val spans = spanned.getSpans(0, spanned.length, TypefaceSpan::class.java)
        assertEquals(1, spans.size)
        assertEquals("sans-serif", spans[0].family)
        assertEquals(9, spanned.getSpanStart(spans[0]))
        assertEquals(10, spanned.getSpanEnd(spans[0]))
    }

    @Test
    fun `simgesiz metin span tasimiyor`() {
        // Dizgeyi oldugu gibi dondurmek `RemoteViews`'a gereksiz bir
        // SpannableString parcel'lamaktan da kurtariyor.
        val result = liraSpanned("EYLÜL HARCAMASI")
        assertFalse(result is Spanned)
    }
}
