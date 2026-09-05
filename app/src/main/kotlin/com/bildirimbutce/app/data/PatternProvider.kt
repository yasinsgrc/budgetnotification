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
/**
 * Ayarlarda listelenen tek bir bildirim kaynagi.
 *
 * [label] desen setinden geliyor; karsiligi yoksa paket adinin kendisi
 * gosteriliyor. Uydurulmus bir ad, kullanicinin listeyi Android'in uygulama
 * listesiyle karsilastirip dogrulamasini imkansiz kilardi.
 */
data class NotificationSource(val packageName: String, val label: String)

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

    /** Bos durum ekranindaki "hazirlik durumu" listesi icin gercek desen sayisi. */
    fun patternCount(context: Context): Int = readSet(context)?.patterns?.size ?: 0

    /**
     * Onboarding'in "N tanimli uygulama" ve "N banka" satirlari icin dinlenen
     * kaynak sayisi. Ekrana elle yazilsaydi, desen setine banka eklendigi gun
     * sessizce yanlis sayi gosterirdi.
     */
    fun sourceCount(context: Context): Int = readSet(context)?.sources?.size ?: 0

    /** Ayarlardaki "desen seti v1" satiri icin yururlukteki setin surumu. */
    fun patternVersion(context: Context): Int = readSet(context)?.version ?: 0

    /**
     * Ayarlar ekranindaki (F2) kaynak listesi.
     *
     * Liste desen setinden geliyor, koddan degil: hangi paketlerin dinlendigi
     * iki yerde tanimli olsaydi ekran ile [BankNotificationParser.isKnownSource]
     * birbirinden sapar, kullanici ekranda hic gormedigi bir kaynagi kapatamazdi.
     */
    fun sources(context: Context): List<NotificationSource> {
        val set = readSet(context) ?: return emptyList()
        return set.sources.map { NotificationSource(it, set.sourceLabels[it] ?: it) }
    }

    /** Ayni okuma yolu (onbellek > assets) uzerinden desen seti; okunamazsa null. */
    private fun readSet(context: Context): PatternSet? = runCatching {
        PatternSet.fromJson(readCache(context) ?: readAsset(context))
    }.getOrNull()

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
