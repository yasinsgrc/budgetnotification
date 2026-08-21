package com.bildirimbutce.parser

import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Veri odakli dogruluk testi.
 *
 * Neden boyle: bu projede riskin tamami regex kalitesinde. Gercek bildirim
 * topladikca fixtures.tsv'ye satir eklersiniz - Kotlin koduna dokunmadan
 * dogruluk orani olculur. Esik %95'in altina duserse build kirmizi olur.
 */
class ParserAccuracyTest {

    private val parser: BankNotificationParser by lazy {
        val json = requireNotNull(javaClass.getResourceAsStream("/patterns.json")) {
            "patterns.json test classpath'inde yok - parser/build.gradle.kts icindeki processTestResources ayarina bakin"
        }.bufferedReader().readText()
        BankNotificationParser(PatternSet.fromJson(json))
    }

    private data class Fixture(
        val line: Int, val text: String, val kind: String,
        val amountMinor: String, val merchant: String
    )

    private fun fixtures(): List<Fixture> {
        val stream = requireNotNull(javaClass.getResourceAsStream("/fixtures.tsv")) {
            "fixtures.tsv bulunamadi"
        }
        return stream.bufferedReader().readLines()
            .mapIndexed { i, l -> i + 1 to l }
            .filter { (_, l) -> l.isNotBlank() && !l.startsWith("#") }
            .map { (n, l) ->
                val c = l.split("\t")
                require(c.size >= 4) { "fixtures.tsv satir $n: 4 sutun bekleniyor, ${c.size} bulundu (TAB kullanin)" }
                Fixture(n, c[0].trim(), c[1].trim(), c[2].trim(), c[3].trim())
            }
    }

    private fun outcome(text: String): Triple<String, String, String> =
        when (val r = parser.parse(text)) {
            is ParseResult.Ignored -> Triple("IGNORE", "-", "-")
            is ParseResult.NoMatch -> Triple("NONE", "-", "-")
            is ParseResult.Match -> Triple(
                r.transaction.kind.name,
                r.transaction.amountMinor.toString(),
                r.transaction.merchant ?: "-"
            )
        }

    @Test
    fun `fixture dosyasindaki tum ornekler dogru ayristirilir`() {
        val all = fixtures()
        assertTrue("fixtures.tsv bos", all.isNotEmpty())

        val failures = mutableListOf<String>()
        for (f in all) {
            val (kind, amount, merchant) = outcome(f.text)
            // Tam esitlik: equalsIgnoreCase, 'ı' ile 'i' farkini gizler ve
            // Turkce locale kaynakli goruntuleme hatalarini gecirir.
            val ok = kind == f.kind && amount == f.amountMinor && merchant == f.merchant
            if (!ok) {
                failures += "satir ${f.line}: beklenen [${f.kind}|${f.amountMinor}|${f.merchant}] " +
                    "gelen [$kind|$amount|$merchant]\n    metin: ${f.text}"
            }
        }

        val accuracy = (all.size - failures.size) * 100.0 / all.size
        println("Ayristirma dogrulugu: %.1f%% (%d/%d)".format(accuracy, all.size - failures.size, all.size))
        if (failures.isNotEmpty()) {
            println("Basarisiz ornekler:\n" + failures.joinToString("\n"))
        }
        assertTrue(
            "Dogruluk %95 esiginin altinda: %.1f%%".format(accuracy) + "\n" + failures.joinToString("\n"),
            accuracy >= 95.0
        )
    }

    @Test
    fun `dogrulama kodlari asla islem olarak kaydedilmez`() {
        val otp = listOf(
            "Tek kullanımlık şifreniz: 456789",
            "Doğrulama kodunuz 112233. 3 dakika geçerlidir.",
            "Onay kodu: 998877 - 250,00 TL tutarındaki işleminiz için"
        )
        for (t in otp) {
            assertTrue("OTP islem sayildi: $t", parser.parse(t) is ParseResult.Ignored)
        }
    }

    @Test
    fun `bilinmeyen paketler dinlenmez`() {
        assertTrue(parser.isKnownSource("com.garanti.cepsubesi"))
        assertTrue(!parser.isKnownSource("com.instagram.android"))
    }
}
