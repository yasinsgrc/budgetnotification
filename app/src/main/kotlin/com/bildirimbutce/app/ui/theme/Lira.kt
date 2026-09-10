package com.bildirimbutce.app.ui.theme

import android.text.SpannableString
import android.text.Spanned
import android.text.style.TypefaceSpan
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily

/**
 * Sistem fontunun aile adi. Compose tarafindaki [FontFamily.Default] de buna
 * cozuluyor; iki taraf ayni yazi tipini kullansin diye tek sabit.
 */
private const val SYSTEM_FAMILY = "sans-serif"

/** Turk lirasi simgesi, U+20BA. Metinlere elle yazilmasin diye burada. */
const val LIRA = "₺"

/**
 * ₺'yi Schibsted Grotesk'e cizdirmeyen tek kural (yol haritasi 15. madde).
 *
 * **Sorun font ureticisinde, bizde degil.** Schibsted Grotesk 1.100 U+20BA icin
 * yanlis bir glif ciziyor: alt ucu kivrilan bir govdeye iki cizgi - yani cift
 * cizgili bir pound (£), Turk lirasi degil. Glif *eksik* degil ve *yanlis
 * eslenmis* de degil; `cmap` U+20BA'yi `uni20BA` adinda kendi glifine
 * gonderiyor, o glifin **sekli** yanlis cizilmis. Bu yuzden "fontu guncelle"
 * bir cozum degildi: Google Fonts'taki guncel surum de ayni 1.100 ve ayni
 * 50 noktali glif.
 *
 * JetBrains Mono 2.211'de ise glif tamamen yok. Compose'da ozel bir
 * `FontFamily` icin sistem yedegi devreye girmiyor, oradan tofu (bos kutu)
 * cikardi - bugun Mono stillerinde ₺ gecmiyor, ama gecerse sessizce kutu olur.
 *
 * **Cozum simgeyi bu iki aileden de almamak.** Sistem fontunun (Roboto) ₺
 * glifi dogru ve o da bir grotesk; yalnizca simge sistem fontuna dusuyor,
 * rakamlar Grotesk'te kaliyor. Rakamlarin da dusmesi kabul edilemezdi: tabular
 * figurler ([AppText] icindeki `tnum`) o zaman kaybolur ve tutar degisirken
 * sayi titrerdi.
 *
 * Widget tarafinin karsiligi [liraSpanned].
 */
fun withLiraFallback(text: String): AnnotatedString = buildAnnotatedString {
    append(text)
    forEachLira(text) { start, end ->
        addStyle(SpanStyle(fontFamily = FontFamily.Default), start, end)
    }
}

/**
 * [withLiraFallback]'in `RemoteViews` karsiligi.
 *
 * Widget'ta Compose yok; `TextView`'a yazilan `CharSequence` span tasiyabiliyor
 * ve [TypefaceSpan] parcelable oldugu icin `RemoteViews` sinirindan sag geciyor.
 * Duzendeki `android:fontFamily` (Grotesk) taban olarak kaliyor, yalnizca
 * simgenin araligi sistem fontuna dusuyor.
 *
 * Simgeyi duzende ayri bir `TextView`'a almak da denendi ve **geri alindi**:
 * 2x1 hucrede tutar tum genisligi kapiyor, `LinearLayout`'un son cocugu olan
 * simge kirpiliyordu. Genisligi agirlikla bolmek de simgeyi widget'in sag
 * kenarina itiyordu; tasarimda tutar ve simge bitisik tek bir oge.
 */
fun liraSpanned(text: String): CharSequence {
    if (!text.contains(LIRA)) return text
    return SpannableString(text).apply {
        forEachLira(text) { start, end ->
            setSpan(TypefaceSpan(SYSTEM_FAMILY), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }
}

/** Metindeki her ₺ icin araligi verir; iki yedegin de tek tarama kurali. */
private inline fun forEachLira(text: String, mark: (start: Int, end: Int) -> Unit) {
    var index = text.indexOf(LIRA)
    while (index >= 0) {
        mark(index, index + LIRA.length)
        index = text.indexOf(LIRA, index + LIRA.length)
    }
}
