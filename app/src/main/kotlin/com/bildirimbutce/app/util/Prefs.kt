package com.bildirimbutce.app.util

import android.content.Context

class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("bildirim_butce", Context.MODE_PRIVATE)

    var onboardingDone: Boolean
        get() = sp.getBoolean(KEY_ONBOARDING, false)
        set(v) = sp.edit().putBoolean(KEY_ONBOARDING, v).apply()

    /**
     * Kullanicinin dinlenmesini istedigi kaynaklar (ayarlar > F2).
     *
     * `null` = hic secim yapilmadi, desen setindeki tum kaynaklar dinlenir.
     * Bos kume = kullanici hepsini kapatti, hicbiri dinlenmez.
     *
     * Ayrim sart: "bos kume = hepsi" olsaydi son anahtari da kapatan kullanici
     * sessizce **tum** bankalari geri acmis olurdu. `null` ayrica desen setine
     * sonradan eklenen bankanin kendiliginden dinlenmesini saglar - hicbir
     * secim yapmamis kullanici icin yeni banka da listenin parcasidir.
     */
    var enabledSources: Set<String>?
        get() = sp.getStringSet(KEY_SOURCES, null)
        set(v) {
            val editor = sp.edit()
            if (v == null) editor.remove(KEY_SOURCES) else editor.putStringSet(KEY_SOURCES, v)
            editor.apply()
        }

    var patternVersion: Int
        get() = sp.getInt(KEY_PATTERN_VERSION, 0)
        set(v) = sp.edit().putInt(KEY_PATTERN_VERSION, v).apply()

    private companion object {
        const val KEY_ONBOARDING = "onboarding_done"
        const val KEY_SOURCES = "enabled_sources"
        const val KEY_PATTERN_VERSION = "pattern_version"
    }
}
