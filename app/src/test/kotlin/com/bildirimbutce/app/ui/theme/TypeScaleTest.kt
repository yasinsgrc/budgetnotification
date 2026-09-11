package com.bildirimbutce.app.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontListFontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.ResourceFont
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.bildirimbutce.app.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tipografi olceginin tasarim v2 ile ayni oldugunu sabitler (yol haritasi 17).
 *
 * NEDEN TEST EDILIYOR: bu maddeden once olcek tasarimdan sessizce kaymisti -
 * display katmani 600'de kalmisti (tasarim 700), butonlar kart basligi
 * tokenini kullandigi icin 20sp cikiyordu (tasarim 16). Hicbiri derlemeyi
 * bozmadi, hicbir test kirmizi yanmadi; kayma ancak tasarim dosyasi acilip
 * ekranla yan yana konunca goruldu. Sayilar burada duruyorsa bir daha
 * sessizce kayamaz.
 *
 * Test edilen sey CIZIM DEGIL (`compose-ui-test` bagimliligi hala yok, 4-12.
 * maddelerdeki gerekce), olcegin kendisi: hangi token hangi punto, hangi
 * agirlik, hangi tracking. Cizim emulatorde gozle dogrulandi.
 *
 * Kaynak: design/design-v2-tum-ekranlar.dc.html. Oradaki degerler CSS `font:`
 * kisayolu, yani `agirlik boyut/satir-yuksekligi`; satir yuksekligi carpan
 * oldugu icin sp karsiligi boyutla carpilarak yazildi (66 * .9 = 59.4 -> 59.5).
 */
class TypeScaleTest {

    private fun resourceFonts(family: FontFamily): List<ResourceFont> =
        (family as FontListFontFamily).fonts.filterIsInstance<ResourceFont>()

    // --- Tasarimin display katmani 700; ailede gercek bir 700 kesimi olmali ---

    @Test
    fun `grotesk ailesinde gercek bir 700 kesimi var`() {
        // Bu testin asil isi sentetik kalinligi yakalamak. Dosya ailede yoksa
        // Compose (FontSynthesis.All) Bold istegini 600'u sismanlatarak
        // karsilar: derleme gecer, ekran calisir, tutar bulanik cikar.
        val fonts = resourceFonts(AppText.displayAmount.fontFamily!!)
        val bold = fonts.singleOrNull { it.weight == FontWeight.Bold }
        assertTrue(
            "Grotesk ailesinde 700 kesimi yok; Bold istekleri sentetik kalinliga duser",
            bold != null
        )
        assertEquals(R.font.schibsted_grotesk_bold, bold!!.resId)
    }

    @Test
    fun `mono ailesinin en kalin kesimi Medium`() {
        // JetBrains Mono 700 bilerek gonderilmiyor (gerekce Type.kt'de).
        // Bu yuzden Mono tabanli hicbir token Medium'u asmamali - asarsa
        // sentetik kalinlik geri gelir.
        listOf("metaMono" to AppText.metaMono, "kicker" to AppText.kicker).forEach { (ad, stil) ->
            val enKalin = resourceFonts(stil.fontFamily!!).maxOf { it.weight.weight }
            assertEquals("$ad Mono ailesinde olmayan bir agirlik istiyor", 500, enKalin)
            assertTrue(
                "$ad istenen agirlik ailede yok",
                stil.fontWeight!!.weight <= enKalin
            )
        }
    }

    // --- Token bazinda tasarim degerleri ---

    private fun assertScale(
        ad: String,
        stil: TextStyle,
        agirlik: FontWeight,
        boyut: Float,
        satir: Float,
        tracking: Float? = null
    ) {
        assertEquals("$ad agirligi", agirlik, stil.fontWeight)
        assertEquals("$ad boyutu", boyut.sp, stil.fontSize)
        assertEquals("$ad satir yuksekligi", satir.sp, stil.lineHeight)
        if (tracking != null) assertEquals("$ad tracking", tracking.em, stil.letterSpacing)
    }

    @Test
    fun `display katmani tasarimla ayni`() {
        // tasarim: 700 66px/.9 -.05em
        assertScale("displayAmount", AppText.displayAmount, FontWeight.Bold, 66f, 59.5f, -0.05f)
        // tasarim: 700 46px/.9 -.045em
        assertScale("displaySheet", AppText.displaySheet, FontWeight.Bold, 46f, 41.5f, -0.045f)
    }

    @Test
    fun `baslik katmani iki basamakli`() {
        // Tasarim iki ayri olcek kullaniyor; tek tokena toplanirlarsa uzun A2
        // basligi kahraman boyutunda cikip sayfayi tasiriyor.
        // tasarim: 600 40px/1.04 -.035em
        assertScale("headline", AppText.headline, FontWeight.SemiBold, 40f, 41.5f, -0.035f)
        // tasarim: 600 31px/1.08 -.03em
        assertScale("headlineSmall", AppText.headlineSmall, FontWeight.SemiBold, 31f, 33.5f, -0.03f)
        assertTrue(
            "headlineSmall, headline'dan kucuk olmali",
            AppText.headlineSmall.fontSize.value < AppText.headline.fontSize.value
        )
    }

    @Test
    fun `baslik ve buton katmanlari birbirinden ayri`() {
        // Ucu de titleCard'a bagliydi: ust bar basligi da buton etiketi de
        // 20sp Medium cikiyordu. Tasarim ucunu ayri olculerde kullaniyor.
        // tasarim: 600 20px/1.2 -.02em
        assertScale("titleCard", AppText.titleCard, FontWeight.SemiBold, 20f, 24f, -0.02f)
        // tasarim: 600 17px/1 -.02em
        assertScale("titleScreen", AppText.titleScreen, FontWeight.SemiBold, 17f, 18f, -0.02f)
        // tasarim: 600 16px/1
        assertScale("labelButton", AppText.labelButton, FontWeight.SemiBold, 16f, 17f)

        val boyutlar = listOf(
            AppText.titleCard.fontSize.value,
            AppText.titleScreen.fontSize.value,
            AppText.labelButton.fontSize.value
        )
        assertEquals("uc katman ayri punto olmali", boyutlar.size, boyutlar.distinct().size)
    }

    @Test
    fun `para simgesi tutardan bir kesim hafif`() {
        // tasarim: 500 21px/1, %40 opaklik. Simge tutarla ayni agirliga
        // cikarsa rakamlarla yarisir; tasarim onu bilerek geri cekiyor.
        assertScale("currencyMark", AppText.currencyMark, FontWeight.Medium, 21f, 22f)
        assertTrue(
            "simge, tutardan hafif olmali",
            AppText.currencyMark.fontWeight!!.weight < AppText.displayAmount.fontWeight!!.weight
        )
    }

    @Test
    fun `islem satirinda tutar ile isyeri adi ayni agirlikta`() {
        // tasarim: isyeri 600 15.5px, tutar 600 16px. Tutar Medium kalinca
        // ayni satirda adin yaninda soluk duruyordu.
        assertEquals(FontWeight.SemiBold, AppText.amountRow.fontWeight)
        assertEquals(AppText.bodyLarge.fontWeight, AppText.amountRow.fontWeight)
    }

    @Test
    fun `para tasiyan her token tabular figur kullaniyor`() {
        // Aksi halde tutar degisirken rakam genislikleri oynar ve sayi titrer.
        listOf(
            "displayAmount" to AppText.displayAmount,
            "displaySheet" to AppText.displaySheet,
            "amountRow" to AppText.amountRow
        ).forEach { (ad, stil) ->
            assertEquals("$ad tabular figur tasimali", "tnum", stil.fontFeatureSettings)
        }
    }

    @Test
    fun `Material buton etiketi olcekten okunuyor`() {
        // labelLarge'i Material'in Button'u okur. labelChip bagliyken her
        // Material buton tasarimdan iki punto kucuk ciziliyordu.
        assertEquals(AppText.labelButton, AppTypography.labelLarge)
    }

    @Test
    fun `M3 eslemesinin hicbir alani bos degil`() {
        // Bu test bir olcek sorusu degil, BASLATMA SIRASI sorusu.
        //
        // Aileler dosya duzeyinde dururken `AppText` ile `AppTypography`
        // birbirini baslatiyordu; once `AppText` okunan her koşuda (yani
        // uygulamanin normal akisinda) `AppTypography` BASTAN ASAGI null
        // kuruluyordu. Testin ilk yazildigi gun kirmizi yanan sey buydu.
        //
        // `AppText`e ONCE dokunulmasi sart: donguyu ancak bu sira aciga
        // cikariyor, ters sirada iki nesne de dogru kuruluyor.
        val ilkDokunus = AppText.labelChip
        assertEquals(FontWeight.SemiBold, ilkDokunus.fontWeight)

        mapOf(
            "displayLarge" to AppTypography.displayLarge,
            "displayMedium" to AppTypography.displayMedium,
            "displaySmall" to AppTypography.displaySmall,
            "headlineLarge" to AppTypography.headlineLarge,
            "headlineMedium" to AppTypography.headlineMedium,
            "headlineSmall" to AppTypography.headlineSmall,
            "titleLarge" to AppTypography.titleLarge,
            "titleMedium" to AppTypography.titleMedium,
            "titleSmall" to AppTypography.titleSmall,
            "bodyLarge" to AppTypography.bodyLarge,
            "bodyMedium" to AppTypography.bodyMedium,
            "bodySmall" to AppTypography.bodySmall,
            "labelLarge" to AppTypography.labelLarge,
            "labelMedium" to AppTypography.labelMedium,
            "labelSmall" to AppTypography.labelSmall
        ).forEach { (ad, stil) ->
            @Suppress("SENSELESS_COMPARISON")
            assertTrue("AppTypography.$ad null kuruldu", stil != null)
            assertTrue("AppTypography.$ad bir aile tasimali", stil.fontFamily != null)
        }
    }
}
