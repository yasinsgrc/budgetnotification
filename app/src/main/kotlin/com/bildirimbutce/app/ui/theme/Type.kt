package com.bildirimbutce.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.bildirimbutce.app.R

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
 *   app/src/main/res/font/schibsted_grotesk_bold.ttf      (700)
 *   app/src/main/res/font/jetbrains_mono_regular.ttf      (400)
 *   app/src/main/res/font/jetbrains_mono_medium.ttf       (500)
 * Ikisi de Google Fonts / OFL lisansli, ticari kullanima acik.
 * Dosya adlari kucuk harf ve alt cizgi olmali; Android kaynak adi kurali.
 *
 * 700 KESIMI OLMADAN SENTETIK KALINLIK CIKAR. Compose'un varsayilani
 * FontSynthesis.All: istenen agirlik >= 600 ve ailedeki en yakin kesim ondan
 * hafifse, glifleri kendisi sismanlatir. Tasarimin butun display katmani
 * (66/46/34/31 px tutarlar) 700; dosya yokken bunlar 600'e dusuyordu.
 *
 * Mono'nun 700 kesimi BILEREK yok: tasarimda Mono-700 yalnizca on adet TEK
 * KARAKTERLIK rozette geciyor (banka bas harfi, "✓", "↺"). 109 KB'lik bir TTF
 * on glif icin pahali - imzasiz release APK'nin %8'i. Mono en kalin 500'de
 * kaliyor; bu yuzden Mono'ya hicbir yerde Bold ISTENMEMELI, istenirse
 * sentetik kalinlik geri gelir.
 */

object AppText {

    /*
     * AILELER VE `TABULAR` BILEREK BU NESNENIN ICINDE, dosya duzeyinde degil.
     *
     * Disarida durduklarinda ortada bir baslatma dongusu vardi: Kotlin dosya
     * duzeyindeki `val`'leri `TypeKt` sinifina koyuyor, `object AppText` ise
     * ayri bir sinif. `AppText` ailleri okumak icin `TypeKt`'yi baslatiyor,
     * `TypeKt` de `AppTypography`'yi kurarken `AppText`'i okuyordu. JVM ayni
     * is parcaciginda yeniden girisi hata saymaz; yarim kalmis sinifi oldugu
     * gibi verir. Yani hangi sinifa ONCE dokunuldugu sonucu degistiriyordu:
     * once `AppText` okunursa (her ekran oyle yapiyor) `AppTypography`'nin
     * BUTUN alanlari null kuruluyordu - ve Material bilesenleri stillerini
     * oradan okuyor. Aileler iceri alininca `AppText` artik `TypeKt`'ye hic
     * dokunmuyor, dongu kapaniyor. `TypeScaleTest` bunu yakaladi.
     */

    private val Grotesk = FontFamily(
        Font(R.font.schibsted_grotesk_regular, FontWeight.Normal),
        Font(R.font.schibsted_grotesk_medium, FontWeight.Medium),
        Font(R.font.schibsted_grotesk_semibold, FontWeight.SemiBold),
        Font(R.font.schibsted_grotesk_bold, FontWeight.Bold)
    )

    private val Mono = FontFamily(
        Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
        Font(R.font.jetbrains_mono_medium, FontWeight.Medium)
    )

    /**
     * Para tutarlarinda tabular rakam ZORUNLU. Aksi halde tutar animasyonla
     * degisirken rakam genislikleri oynar ve sayi titrer.
     */
    private const val TABULAR = "tnum"

    /** Ana ekrandaki aylik toplam. Tasarim: 700 66px/.9 -.05em. */
    val displayAmount = TextStyle(
        fontFamily = Grotesk,
        fontWeight = FontWeight.Bold,
        fontSize = 66.sp,
        lineHeight = 59.5.sp,
        letterSpacing = (-0.05).em,
        fontFeatureSettings = TABULAR
    )

    /** Duzeltme sheet'indeki tutar, kural sayaci. Tasarim: 700 46px/.9 -.045em. */
    val displaySheet = TextStyle(
        fontFamily = Grotesk,
        fontWeight = FontWeight.Bold,
        fontSize = 46.sp,
        lineHeight = 41.5.sp,
        letterSpacing = (-0.045).em,
        fontFeatureSettings = TABULAR
    )

    /** Onboarding A1 kahraman basligi. Tasarim: 600 40px/1.04 -.035em. */
    val headline = TextStyle(
        fontFamily = Grotesk,
        fontWeight = FontWeight.SemiBold,
        fontSize = 40.sp,
        lineHeight = 41.5.sp,
        letterSpacing = (-0.035).em
    )

    /**
     * Ikinci duzey baslik: A2/A3 sayfa basliklari, paywall basligi.
     * Tasarim: 600 31px/1.08 -.03em.
     *
     * headline'dan ayri durmasi gerekiyordu: tasarim bu iki olcegi (40 ve 31)
     * ayri kullaniyor ve ikisi tek tokena toplandiginda uzun A2 basligi
     * kahraman boyutunda cikip sayfayi tasiriyordu.
     */
    val headlineSmall = TextStyle(
        fontFamily = Grotesk,
        fontWeight = FontWeight.SemiBold,
        fontSize = 31.sp,
        lineHeight = 33.5.sp,
        letterSpacing = (-0.03).em
    )

    /** Kart basliklari: "Bildirim erisimi kapali". Tasarim: 600 20px/1.2 -.02em. */
    val titleCard = TextStyle(
        fontFamily = Grotesk,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 24.sp,
        letterSpacing = (-0.02).em
    )

    /**
     * Ekran/ust bar basligi: "Rapor", "Ayarlar", "Kaynaklar".
     * Tasarim: 600 17px/1 -.02em.
     *
     * Once titleCard kullaniliyordu (20sp): ust bar basligi kart basligiyla
     * ayni agirlikta cikinca ekranin hiyerarsisi duzlesiyordu.
     */
    val titleScreen = TextStyle(
        fontFamily = Grotesk,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        lineHeight = 18.sp,
        letterSpacing = (-0.02).em
    )

    /**
     * Birincil buton etiketi: "Kaydet", "Devam", "Deftere git".
     * Tasarim: 600 16px/1 (ikincil butonlarda 15.5px).
     *
     * Bunlar da titleCard'dan (20sp Medium) geliyordu - tasarladigindan
     * dortte bir buyuk ve bir kesim hafif. Butonlar bu yuzden sisik duruyordu.
     */
    val labelButton = TextStyle(
        fontFamily = Grotesk,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 17.sp
    )

    /**
     * Ana ekrandaki toplamin yanindaki "₺". Tasarim: 500 21px/1, %40 opaklik.
     *
     * titleCard'a bagliydi; o token bu maddede 600'e cikinca simge tutarla
     * ayni agirliga gelirdi. Tasarim simgeyi bilerek bir kesim hafif ve
     * soluk birakiyor - rakamlar okunsun, simge okunmasin diye.
     */
    val currencyMark = TextStyle(
        fontFamily = Grotesk,
        fontWeight = FontWeight.Medium,
        fontSize = 21.sp,
        lineHeight = 22.sp
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

    /**
     * Islem satiri tutari. tabular zorunlu.
     * Tasarim: 600 16px/1 - agirlik Medium'du, isyeri adiyla (600) ayni
     * satirda daha soluk kaliyordu.
     */
    val amountRow = TextStyle(
        fontFamily = Grotesk,
        fontWeight = FontWeight.SemiBold,
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

    /*
     * BURADA BIR `widgetAmount` TOKENI VARDI, SILINDI - hicbir yer okumuyordu.
     * Okuyamazdi da: widget'lar RemoteViews, yani XML (res/layout/widget_*.xml)
     * ve bir Compose TextStyle oraya hicbir sekilde ulasamaz. Widget tipografisi
     * o XML'lerdeki android:textSize / android:fontFamily satirlarinda duruyor;
     * token burada dursaydi "widget bu olcegi kullaniyor" diye yanlis bir
     * izlenim birakirdi. Tasarim degerleri: 2x1 700 31px/.95, 4x2 700 34px/.95.
     */
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
    headlineMedium = AppText.headlineSmall,
    headlineSmall = AppText.headlineSmall,
    titleLarge = AppText.titleCard,
    titleMedium = AppText.titleScreen,
    titleSmall = AppText.kicker,
    bodyLarge = AppText.bodyLarge,
    bodyMedium = AppText.body,
    bodySmall = AppText.bodySmall,
    // labelLarge'i Material'in Button'u okur; labelChip (13.5sp) buraya
    // baglandiginda her Material buton tasarimdan iki punto kucuk cikiyordu.
    labelLarge = AppText.labelButton,
    labelMedium = AppText.labelChip,
    labelSmall = AppText.metaMono
)
