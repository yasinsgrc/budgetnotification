package com.bildirimbutce.app.ui.pro

import com.bildirimbutce.app.ui.MonthCursor

/**
 * Ucretsiz surumun tek siniri: gecmise ne kadar gidilebilecegi.
 *
 * Pro'nun bugun actigi tek sey bu. Tasarimdaki diger uc madde (4x2 widget, CSV
 * disa aktarma, kendi desenini yazma) kodda yok; olmayan bir seyin parasi
 * istenmedigi icin paywall da onlari satmiyor (bkz. [PaywallScreen]).
 *
 * Kural ekranin icinde degil burada duruyor: ay gezinmesini kisitlayan yer
 * (`HomeViewModel.previousMonth`) ile kullaniciya sinirin geldigini soyleyen
 * yer (`HomeScreen`) ayni cumleyi okumak zorunda. Iki kopya olsaydi biri
 * degisip digeri kalabilirdi - 8. maddedeki `signedMinor` ile ayni gerekce.
 */
object ProLimits {

    /**
     * Ucretsiz surumde gorulebilen ay sayisi; **icinde bulunulan ay dahil**.
     *
     * Tasarim E2'deki "3 ay gecmis gorunur" satiri bu sayidan okunuyor. Dahil
     * olmasi sart: uc "ek" ay olsaydi ucretsiz kullanici dort aylik defter
     * gorurdu ve ekranda yazan sayi yalan olurdu.
     */
    const val FREE_MONTH_COUNT = 3

    /**
     * Acilabilen en eski ay; Pro'da sinir olmadigi icin `null`.
     *
     * `null` "sinir yok" demek, "sinir sifir" degil - [canOpen] bu ayrimi
     * erken donusle veriyor.
     */
    fun earliestMonth(now: MonthCursor, isPro: Boolean): MonthCursor? =
        if (isPro) null else now.minus(FREE_MONTH_COUNT - 1)

    /**
     * [cursor] ayinin acilip acilamayacagi.
     *
     * Gelecek aylar kisitlanmiyor: bos bir ayi acmak zaten bir sey gostermiyor
     * ve gecmisi satan bir sinirin gelecege dokunmasi icin sebep yok.
     */
    fun canOpen(cursor: MonthCursor, now: MonthCursor, isPro: Boolean): Boolean {
        val floor = earliestMonth(now, isPro) ?: return true
        return cursor.ordinal >= floor.ordinal
    }
}
