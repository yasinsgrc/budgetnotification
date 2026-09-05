package com.bildirimbutce.app.ui.onboarding

/**
 * Onboarding'in uc sayfasi (tasarim A1-A3).
 *
 * Sayfalar birer rota **degil**, tek rotanin ic durumu: ucu birlikte tek bir
 * kurulum sihirbazi olusturuyor. Ayri rota olsalardi her cikis kendi `popUpTo`
 * zincirini tasirdi ve grafta "izin verilmeden ulasilabilen A3" gibi anlamsiz
 * adresler dogardi - A3'un tek anlami izin verilmis olmasi.
 */
enum class OnboardingPage {
    /** A1 - uygulama ne ise yarar. */
    INTRO,

    /** A2 - Android'in soracagi izin ekranini onceden gosterir. */
    PERMISSION,

    /** A3 - izin verildi, dinleme basladi. */
    READY;

    /** Ileri yon. Son sayfada `null`: akis biter, bayrak yazilir, deftere gecilir. */
    fun next(): OnboardingPage? = entries.getOrNull(ordinal + 1)
}
