package com.bildirimbutce.app.data

import android.content.Context
import android.util.Log
import com.bildirimbutce.parser.BankNotificationParser
import com.bildirimbutce.parser.PatternSet
import java.io.File

/**
 * Desenleri saglar. Oncelik sirasi: onbellek (indirilmis) > assets (paketle gelen).
 *
 * Neden onemli: bir banka bildirim sablonunu degistirdiginde Play surum
 * incelemesini (1-3 gun) beklemeden ayni gun duzeltme yayinlayabilirsiniz.
 * Uzaktan guncelleme varsayilan olarak KAPALI - manifest'te INTERNET izni yok.
 */
object PatternProvider {

    private const val TAG = "PatternProvider"
    private const val ASSET = "patterns.json"
    private const val CACHE = "patterns_cache.json"

    @Volatile private var cached: BankNotificationParser? = null

    fun parser(context: Context): BankNotificationParser = cached ?: synchronized(this) {
        cached ?: build(context).also { cached = it }
    }

    fun invalidate() {
        cached = null
    }

    private fun build(context: Context): BankNotificationParser {
        val raw = readCache(context) ?: readAsset(context)
        return BankNotificationParser(PatternSet.fromJson(raw))
    }

    private fun readAsset(context: Context): String =
        context.assets.open(ASSET).bufferedReader().use { it.readText() }

    private fun readCache(context: Context): String? {
        val f = File(context.filesDir, CACHE)
        if (!f.exists()) return null
        return runCatching { f.readText() }
            .onFailure { Log.w(TAG, "Onbellek okunamadi, assets'e donuluyor", it) }
            .getOrNull()
    }

    /**
     * Indirilen desen setini dogrulayip onbellege alir.
     * Cagiran taraf indirmeyi yapar (INTERNET izni gerekir).
     * Bozuk JSON yazilmaz - kullanici calisan bir surumle kalir.
     */
    fun installUpdate(context: Context, json: String, currentVersion: Int): Boolean {
        val parsed = runCatching { PatternSet.fromJson(json) }.getOrElse {
            Log.w(TAG, "Gecersiz desen paketi reddedildi", it); return false
        }
        if (parsed.version <= currentVersion) return false
        if (parsed.patterns.isEmpty()) return false
        runCatching { BankNotificationParser(parsed).parse("test 10,00 TL harcama") }
            .onFailure { Log.w(TAG, "Desen seti calistirilamadi, reddedildi", it); return false }

        File(context.filesDir, CACHE).writeText(json)
        invalidate()
        return true
    }
}
