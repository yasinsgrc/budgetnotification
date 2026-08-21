package com.bildirimbutce.parser

import java.util.Locale

/**
 * Regex'in yakaladigi ham magaza metnini insana gosterilebilir hale getirir.
 *
 * Kritik nokta: on ekler ZINCIRLENIR ("1234 kartiniz ile MIGROS...").
 * Tek gecislik temizlik "ile MIGROS" birakir; bu yuzden sabit noktaya
 * ulasana kadar donguye alinir.
 */
object MerchantCleaner {

    private val TR: Locale = Locale.forLanguageTag("tr-TR")

    private const val MAX_PREFIX_PASSES = 6

    private val PREFIXES = listOf(
        Regex("""(?iu)^(?:sayın|sayin|sn\.?|değerli)\s+(?:müşterimiz|musterimiz)?,?\s*"""),
        Regex("""(?iu)^\d{4}\s+"""),
        Regex("""(?iu)^(?:nolu|no'?lu)\s+"""),
        Regex("""(?iu)^(?:kredi|banka|world|bonus|maximum|axess|paraf|advantage|troy|ticari|sanal)\s+"""),
        Regex("""(?iu)^(?:kartınız(?:la|dan|ın)?|kartiniz(?:la|dan)?|kartınıza)\s+"""),
        Regex("""(?iu)^(?:ile|üzerinden|tarihinde|adlı|olan|nolu)\s+""")
    )

    /** Fiil / dolgu kelimeleri: bunlar magaza adi degil, regex kaymasidir. */
    private val STOPWORDS = setOf(
        "yapilmistir", "yapılmıştır", "yapildi", "yapıldı", "gerceklesti", "gerçekleşti",
        "gerceklestirilmistir", "gerçekleştirilmiştir", "olusmustur", "oluşmuştur",
        "tutarinda", "tutarında", "harcama", "islem", "işlem", "odeme", "ödeme",
        "kartiniz", "kartınız", "bilgilerinize", "musterimiz", "müşterimiz", "tarafindan", "tarafından"
    )

    fun clean(raw: String?, brandTokens: Set<String> = emptySet()): String? {
        if (raw == null) return null
        var s = raw.trim()

        // Sabit noktaya kadar: on ekler zincirlenebilir.
        // (repeat + return@repeat yalnizca iterasyonu atlar, donguyu kirmaz.)
        for (i in 0 until MAX_PREFIX_PASSES) {
            val before = s
            for (p in PREFIXES) s = p.replace(s, "")
            s = s.trim()
            if (s == before) break
        }

        s = s.replace(Regex("""\b\d{2}[./]\d{2}[./]\d{4}\b"""), "")
        s = s.replace(Regex("""(?iu)\s*(?:tarihinde|saat \d{2}[:.]\d{2})\s*"""), " ")
        s = s.replace(Regex("""[*]+"""), " ")
        s = s.replace(Regex("""\s{2,}"""), " ").trim()
        s = s.replace(Regex("""(?iu)[\s,.;:'’\-]+$"""), "")

        if (s.isBlank() || s.length < 2) return null
        if (s.lowercase(TR) in STOPWORDS) return null
        if (s.replace(Regex("""[^\p{L}]"""), "").length < 2) return null

        return titleCase(s, brandTokens)
    }

    /**
     * "MIGROS TICARET" -> "Migros Ticaret", "A101"/"BIM" bozulmadan kalir.
     *
     * DIKKAT - Turkce locale tuzagi: "MIGROS".lowercase(tr) = "mıgros" (noktasiz i).
     * Bankalar marka adlarini ASCII buyuk harfle yazar, dolayisiyla GORUNTULEME icin
     * ROOT locale dogrudur. Stopword karsilastirmasinda ise TR locale kullaniyoruz,
     * cunku orada gercek Turkce kelimeler ("YAPILMISTIR") esleştirilir.
     */
    /**
     * Bir kelime oldugu gibi mi kalmali?
     *
     * Onceki kural "4 harften kisa ve tamami buyuk harf" idi; bu cok genisti:
     * "SOK MARKETLER" -> "SOK Marketler", "BURGER KING" -> "Burger KING".
     * Artik yalnizca rakam iceren (A101, N11) veya patterns.json'daki
     * brandTokens listesinde bulunan (BIM, IKEA, LC, A.S) kelimeler korunur.
     * Liste desen dosyasindan guncellenebilir.
     */
    private fun isBrandToken(word: String, brandTokens: Set<String>): Boolean {
        if (word.any { it.isDigit() }) return true
        return word.uppercase(Locale.ROOT) in brandTokens
    }

    private fun titleCase(s: String, brandTokens: Set<String>): String = s.split(" ")
        .filter { it.isNotEmpty() }
        .joinToString(" ") { word ->
            if (isBrandToken(word, brandTokens)) {
                word
            } else {
                val head = word.first().uppercaseChar()
                val tail = word.drop(1)
                    .lowercase(Locale.ROOT)
                    .replace("i\u0307", "i") // ROOT locale 'İ' -> "i" + birlesik nokta
                head + tail
            }
        }
}
