package com.bildirimbutce.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/**
 * Tipografi olcegi (tasarim v2).
 *
 * v2'de uc yazi ailesi ikiye indi:
 *  - Schibsted Grotesk: basliklar VE rakamlar (tabular figurleri var)
 *  - JetBrains Mono: yalnizca teknik satirlar (kaynak, desen, manifest)
 *
 * FONT DOSYALARI GEREKLI - bunlar olmadan derlenmez:
 *   app/src/main/res/font/schibsted_grotesk_regular.ttf   (400)
 *   app/src/main/res/font/schibsted_grotesk_medium.ttf    (500)
 *   app/src/main/res/font/schibsted_grotesk_semibold.ttf  (600)
 *   app/src/main/res/font/jetbrains_mono_regular.ttf      (400)
 *   app/src/main/res/font/jetbrains_mono_medium.ttf       (500)
 * Ikisi de Google Fonts / OFL lisansli, ticari kullanima acik.
 * Dosya adlari kucuk harf ve alt cizgi olmali; Android kaynak adi kurali.
 */

// GECICI: TTF'ler gelince geri al
private val Grotesk = FontFamily.Default

// GECICI: TTF'ler gelince geri al
private val Mono = FontFamily.Default

/**
 * Para tutarlarinda tabular rakam ZORUNLU. Aksi halde tutar animasyonla
 * degisirken rakam genislikleri oynar ve sayi titrer.
 */
private const val TABULAR = "tnum"

object AppText {

    /** Ana ekrandaki aylik toplam. Tek kullanim. */
    val displayAmount = TextStyle(
        fontFamily = Grotesk,
        fontWeight = FontWeight.SemiBold,
        fontSize = 68.sp,
        lineHeight = 61.sp,
        letterSpacing = (-0.045).em,
        fontFeatureSettings = TABULAR
    )

    /** Duzeltme sheet'indeki tutar, onboarding basligi. */
    val displaySheet = TextStyle(
        fontFamily = Grotesk,
        fontWeight = FontWeight.SemiBold,
        fontSize = 46.sp,
        lineHeight = 42.sp,
        letterSpacing = (-0.04).em,
        fontFeatureSettings = TABULAR
    )

    /** Onboarding basliklari, bos durum basligi. */
    val headline = TextStyle(
        fontFamily = Grotesk,
        fontWeight = FontWeight.Medium,
        fontSize = 38.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.032).em
    )

    /** Izin karti ve kart basliklari. */
    val titleCard = TextStyle(
        fontFamily = Grotesk,
        fontWeight = FontWeight.Medium,
        fontSize = 20.sp,
        lineHeight = 24.sp,
        letterSpacing = (-0.02).em
    )

    /** Islem satirindaki isyeri adi. */
    val bodyLarge = TextStyle(
        fontFamily = Grotesk,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.5.sp,
        lineHeight = 19.sp
    )

    /** Aciklama paragraflari. */
    val body = TextStyle(
        fontFamily = Grotesk,
        fontWeight = FontWeight.Normal,
        fontSize = 14.5.sp,
        lineHeight = 23.sp
    )

    /** Kart ici ikincil metin. */
    val bodySmall = TextStyle(
        fontFamily = Grotesk,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 20.sp
    )

    /** Chip ve buton etiketleri. */
    val labelChip = TextStyle(
        fontFamily = Grotesk,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.5.sp,
        lineHeight = 14.sp
    )

    /** Islem satiri tutari. tabular zorunlu. */
    val amountRow = TextStyle(
        fontFamily = Grotesk,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 17.sp,
        fontFeatureSettings = TABULAR
    )

    /** Tarih, kategori, kaynak satiri. */
    val metaMono = TextStyle(
        fontFamily = Mono,
        fontWeight = FontWeight.Normal,
        fontSize = 10.5.sp,
        lineHeight = 12.sp,
        letterSpacing = 0.06.em
    )

    /** Bolum basliklari: ISLEMLER, KATEGORI. */
    val kicker = TextStyle(
        fontFamily = Mono,
        fontWeight = FontWeight.Medium,
        fontSize = 10.5.sp,
        lineHeight = 12.sp,
        letterSpacing = 0.18.em
    )

    /** Widget tutari. */
    val widgetAmount = TextStyle(
        fontFamily = Grotesk,
        fontWeight = FontWeight.SemiBold,
        fontSize = 32.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.04).em,
        fontFeatureSettings = TABULAR
    )
}

/**
 * M3 Typography eslemesi.
 *
 * Material bilesenleri (Button, TextField, ModalBottomSheet...) kendi
 * stillerini buradan okur; eslemezsek varsayilan Roboto olcegi sizar.
 */
val AppTypography = Typography(
    displayLarge = AppText.displayAmount,
    displayMedium = AppText.displaySheet,
    displaySmall = AppText.headline,
    headlineLarge = AppText.headline,
    headlineMedium = AppText.headline,
    headlineSmall = AppText.displaySheet,
    titleLarge = AppText.titleCard,
    titleMedium = AppText.titleCard,
    titleSmall = AppText.kicker,
    bodyLarge = AppText.bodyLarge,
    bodyMedium = AppText.body,
    bodySmall = AppText.bodySmall,
    labelLarge = AppText.labelChip,
    labelMedium = AppText.labelChip,
    labelSmall = AppText.metaMono
)
