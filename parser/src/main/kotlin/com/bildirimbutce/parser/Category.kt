package com.bildirimbutce.parser

/** Sabit kategori seti. */
enum class Category(val label: String, val emoji: String) {
    MARKET("Market", "\uD83D\uDED2"),
    YEME_ICME("Yeme & İçme", "\uD83C\uDF7D"),
    ULASIM("Ulaşım", "\uD83D\uDE97"),
    FATURA("Fatura", "\uD83D\uDCC4"),
    ALISVERIS("Alışveriş", "\uD83D\uDECD"),
    SAGLIK("Sağlık", "\uD83D\uDC8A"),
    EGLENCE("Eğlence", "\uD83C\uDFAC"),
    DIGER("Diğer", "\u2753");

    companion object {
        fun from(name: String?): Category = entries.firstOrNull { it.name == name } ?: DIGER
    }
}

/**
 * Anahtar kelime siniflandirici.
 *
 * Kasitli olarak aptal: makine ogrenmesi yok, tablo var. Kullanici bir magazayi
 * duzeltince kural ogrenilir ve bir daha sorulmaz - dogruluk kullanimla artar.
 */
object Categorizer {

    private val KEYWORDS: List<Pair<Category, List<String>>> = listOf(
        Category.MARKET to listOf("migros", "bim", "a101", "sok ", "şok", "carrefour", "macrocenter", "tarim kredi", "getir", "market", "bakkal"),
        Category.YEME_ICME to listOf("starbucks", "kahve", "coffee", "restoran", "burger", "pizza", "yemek", "cafe", "kafe", "dominos", "yemeksepeti", "simit", "tavuk"),
        Category.ULASIM to listOf("shell", "opet", "petrol", "petrol ofisi", "total", "istanbulkart", "uber", "bitaksi", "otopark", "hgs", "ogs", "thy", "pegasus", "akaryakit", "benzin"),
        Category.FATURA to listOf("turkcell", "vodafone", "turk telekom", "türk telekom", "superonline", "elektrik", "dogalgaz", "doğalgaz", "su idaresi", "aski", "iski", "netflix", "spotify", "youtube premium", "icloud", "google one", "enerjisa", "igdas"),
        Category.SAGLIK to listOf("eczane", "hastane", "medical", "tip merkezi", "tıp merkezi", "diş", "optik", "laboratuvar"),
        Category.EGLENCE to listOf("sinema", "cinemaximum", "biletix", "steam", "playstation", "xbox", "spor salon", "macfit", "fitness"),
        Category.ALISVERIS to listOf("trendyol", "hepsiburada", "amazon", "n11", "lcw", "lc waikiki", "defacto", "zara", "mediamarkt", "teknosa", "vatan", "apple", "ikea", "koton", "boyner")
    )

    fun guess(merchant: String?): Category {
        val m = merchant?.lowercase() ?: return Category.DIGER
        for ((category, words) in KEYWORDS) {
            if (words.any { m.contains(it) }) return category
        }
        return Category.DIGER
    }
}
