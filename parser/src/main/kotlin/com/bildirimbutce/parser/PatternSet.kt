package com.bildirimbutce.parser

data class BankPattern(
    val id: String,
    val kind: String = "EXPENSE",
    val priority: Int = 50,
    val regex: String
)

data class PatternSet(
    val version: Int = 1,
    val updatedAt: String = "",
    val macros: Map<String, String> = emptyMap(),
    val sources: List<String> = emptyList(),
    /**
     * Paket adi -> ekranda gorunecek ad ("com.ykb.android" -> "Yapi Kredi").
     *
     * Adlar kaynak listesiyle ayni dosyada duruyor: koda gomulu bir tablo
     * olsaydi desen setine banka eklendigi gun eksik kalirdi. Karsiligi
     * olmayan paket icin ekran paket adinin kendisini gosterir - uydurma bir
     * ad, listeyi dogrulanamaz hale getirirdi.
     */
    val sourceLabels: Map<String, String> = emptyMap(),
    val brandTokens: Set<String> = emptySet(),
    val ignore: List<String> = emptyList(),
    val patterns: List<BankPattern> = emptyList()
) {
    companion object {

        fun fromJson(raw: String): PatternSet {
            val root = Json.parse(raw).obj()
            return PatternSet(
                version = root["version"].int(1),
                updatedAt = root["updatedAt"].string(),
                macros = root["macros"].stringMap(),
                sources = root["sources"].stringList(),
                sourceLabels = root["sourceLabels"].stringMap(),
                brandTokens = root["brandTokens"].stringList().toSet(),
                ignore = root["ignore"].stringList(),
                patterns = root["patterns"].list().map { item ->
                    val f = item.obj()
                    BankPattern(
                        id = f["id"].string(),
                        kind = f["kind"].string("EXPENSE"),
                        priority = f["priority"].int(50),
                        regex = f["regex"].string()
                    )
                }.filter { it.id.isNotBlank() && it.regex.isNotBlank() }
            )
        }
    }
}
