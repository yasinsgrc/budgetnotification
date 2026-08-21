package com.bildirimbutce.app.util

import android.content.Context

class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("bildirim_butce", Context.MODE_PRIVATE)

    var onboardingDone: Boolean
        get() = sp.getBoolean(KEY_ONBOARDING, false)
        set(v) = sp.edit().putBoolean(KEY_ONBOARDING, v).apply()

    /** Bos = patterns.json icindeki tum kaynaklar dinlenir. */
    var enabledSources: Set<String>
        get() = sp.getStringSet(KEY_SOURCES, emptySet()) ?: emptySet()
        set(v) = sp.edit().putStringSet(KEY_SOURCES, v).apply()

    var patternVersion: Int
        get() = sp.getInt(KEY_PATTERN_VERSION, 0)
        set(v) = sp.edit().putInt(KEY_PATTERN_VERSION, v).apply()

    private companion object {
        const val KEY_ONBOARDING = "onboarding_done"
        const val KEY_SOURCES = "enabled_sources"
        const val KEY_PATTERN_VERSION = "pattern_version"
    }
}
