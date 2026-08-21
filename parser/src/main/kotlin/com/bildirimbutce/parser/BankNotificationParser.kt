package com.bildirimbutce.parser

/**
 * Banka bildirim metinlerini yapisal islemlere cevirir.
 *
 * Tamamen saf Kotlin: Android bagimliligi yok, JVM unit testinde emulator
 * olmadan calisir. Desenler [PatternSet] icinden gelir; koda gomulu degildir,
 * boylece Play surumu beklemeden guncellenebilir.
 */
class BankNotificationParser(patternSet: PatternSet) {

    private val ignoreRegex: List<Regex> = patternSet.ignore.map { Regex(it) }

    private val compiled: List<Pair<BankPattern, Regex>> = patternSet.patterns
        .sortedBy { it.priority }
        .mapNotNull { p ->
            val expanded = expand(p.regex, patternSet.macros)
            runCatching { p to Regex(expanded) }
                .onFailure { System.err.println("Gecersiz regex '${p.id}': ${it.message}") }
                .getOrNull()
        }

    /** Bu paketten gelen bildirimler dinlenmeli mi? Bos liste = hepsi. */
    private val sources: Set<String> = patternSet.sources.toSet()

    private val brandTokens: Set<String> = patternSet.brandTokens

    fun isKnownSource(packageName: String): Boolean =
        sources.isEmpty() || packageName in sources

    fun parse(text: String): ParseResult {
        val normalized = normalize(text)
        if (normalized.isBlank()) return ParseResult.NoMatch

        ignoreRegex.forEachIndexed { i, r ->
            if (r.containsMatchIn(normalized)) return ParseResult.Ignored("ignore[$i]")
        }

        for ((pattern, regex) in compiled) {
            val m = regex.find(normalized) ?: continue
            val amountMinor = Money.toMinor(group(m, "amount") ?: continue) ?: continue
            val currency = (group(m, "currency") ?: "TL").normalizeCurrency()
            val merchant = MerchantCleaner.clean(group(m, "merchant"), brandTokens)

            return ParseResult.Match(
                ParsedTransaction(
                    kind = if (pattern.kind.equals("REFUND", true)) TxKind.REFUND else TxKind.EXPENSE,
                    amountMinor = amountMinor,
                    currency = currency,
                    merchant = merchant,
                    patternId = pattern.id,
                    confidence = if (merchant == null) 0.5f else 0.9f,
                    rawText = text
                )
            )
        }
        return ParseResult.NoMatch
    }

    /** Fallback desenlerinde "merchant" grubu hic tanimli olmayabilir. */
    private fun group(m: MatchResult, name: String): String? =
        runCatching { m.groups[name]?.value }.getOrNull()

    /**
     * patterns.json icindeki {amount} {cur} {merchant} makrolarini gercek
     * regex parcalariyla degistirir. Tekrar eden uzun gruplari her desende
     * yazmamak icin - desen dosyasi okunabilir kalir.
     */
    private fun expand(regex: String, macros: Map<String, String>): String {
        var out = regex
        for ((name, replacement) in macros) {
            out = out.replace("{$name}", replacement)
        }
        return out
    }

    private fun normalize(text: String): String = text
        .replace('\u00A0', ' ')
        .replace(Regex("""\s+"""), " ")
        .trim()

    private fun String.normalizeCurrency(): String = when (uppercase()) {
        "₺", "TRL", "TRY" -> "TL"
        "$" -> "USD"
        else -> uppercase()
    }
}
