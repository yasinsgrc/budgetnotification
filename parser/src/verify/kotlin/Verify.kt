package com.bildirimbutce.parser.verify

import com.bildirimbutce.parser.*
import java.io.File
import java.util.Calendar

/**
 * Bagimsiz dogrulama kosucusu.
 *
 * JUnit'siz calisir: `kotlinc` ile derlenip `java` ile kosulabilir. Amaci
 * ayristirici dogrulugunu ve UCTAN UCA akisi (ayristir -> tekrar koru ->
 * kategorile -> topla) Android olmadan dogrulamaktir.
 *
 * Kullanim: java -jar verify.jar <patterns.json> <fixtures.tsv>
 */
object Verify {

    /** Dogruluk esigi. Altina duserse dogrulama basarisiz sayilir. */
    private const val THRESHOLD = 95.0

    /**
     * Yayin karari icin gereken en az GERCEK ornek sayisi (roadmap madde 2).
     * Bu sayiya ulasilmadan gercek alt kumenin orani anlamli degildir; az
     * sayida ornekte %100 gormek guven vermez, yalnizca ornek azligini gosterir.
     */
    private const val REAL_SAMPLE_TARGET = 150

    @JvmStatic
    fun main(args: Array<String>) {
        val patternsPath = args.getOrElse(0) { "patterns/patterns.json" }
        val fixturesPath = args.getOrElse(1) { "parser/src/test/resources/fixtures.tsv" }

        val patternSet = PatternSet.fromJson(File(patternsPath).readText())
        val parser = BankNotificationParser(patternSet)

        println("Desen seti v${patternSet.version}: ${patternSet.patterns.size} desen, " +
            "${patternSet.ignore.size} ignore kurali, ${patternSet.sources.size} kaynak\n")

        val accuracyOk = runAccuracy(parser, File(fixturesPath))
        println()
        val e2eOk = runEndToEnd(parser)

        println()
        if (accuracyOk && e2eOk) {
            println("SONUC: TUM DOGRULAMALAR GECTI")
        } else {
            println("SONUC: BASARISIZ")
            kotlin.system.exitProcess(1)
        }
    }

    // ---------- 1) Ayristirma dogrulugu ----------

    private fun runAccuracy(parser: BankNotificationParser, file: File): Boolean {
        data class Row(
            val line: Int, val text: String, val kind: String,
            val amount: String, val merchant: String, val origin: String
        )

        val rows = file.readLines()
            .mapIndexed { i, l -> i + 1 to l }
            .filter { (_, l) -> l.isNotBlank() && !l.startsWith("#") }
            .map { (n, l) ->
                val c = l.split("\t")
                require(c.size >= 4) { "fixtures satir $n: en az 4 kolon bekleniyor, ${c.size} var" }
                // 5. kolon opsiyonel. Eksikse SYNTHETIC sayilir: guvenli yon bu.
                // Tersi (eksigi REAL saymak) sentetik satirlarin yayin kararini
                // sisirmesine yol acardi.
                val origin = c.getOrNull(4)?.trim()?.uppercase()?.ifBlank { null } ?: "SYNTHETIC"
                require(origin == "REAL" || origin == "SYNTHETIC") {
                    "fixtures satir $n: koken 'REAL' ya da 'SYNTHETIC' olmali, '$origin' bulundu"
                }
                Row(n, c[0].trim(), c[1].trim(), c[2].trim(), c[3].trim(), origin)
            }

        val failures = mutableListOf<String>()
        val byKind = mutableMapOf<String, IntArray>()   // [dogru, toplam]
        val byOrigin = mutableMapOf<String, IntArray>() // [dogru, toplam]

        for (r in rows) {
            val (kind, amount, merchant) = outcome(parser, r.text)
            val ok = kind == r.kind && amount == r.amount && merchant == r.merchant
            byKind.getOrPut(r.kind) { IntArray(2) }.also { it[1]++; if (ok) it[0]++ }
            byOrigin.getOrPut(r.origin) { IntArray(2) }.also { it[1]++; if (ok) it[0]++ }
            if (!ok) {
                failures += "  satir ${r.line}: [${r.origin}] beklenen [${r.kind}|${r.amount}|${r.merchant}] " +
                    "gelen [$kind|$amount|$merchant]\n     ${r.text}"
            }
        }

        val acc = (rows.size - failures.size) * 100.0 / rows.size
        println("--- 1) Ayristirma dogrulugu ---")
        byKind.toSortedMap().forEach { (k, v) ->
            println("  %-8s %3d/%-3d  (%.1f%%)".format(k, v[0], v[1], v[0] * 100.0 / v[1]))
        }
        println("  TOPLAM   ${rows.size - failures.size}/${rows.size}  (%.1f%%)".format(acc))
        if (failures.isNotEmpty()) {
            println("\n  Basarisiz ornekler (ilk 25):")
            failures.take(25).forEach { println(it) }
            if (failures.size > 25) println("  ... ve ${failures.size - 25} tane daha")
        }

        val realOk = reportByOrigin(byOrigin)
        return acc >= THRESHOLD && realOk
    }

    /**
     * Kokene gore dogruluk ve yayin karari (roadmap madde 2).
     *
     * Neden ayri: sentetik ornekler kendi ureteclerinden geldikleri icin kolay
     * orneklerdir ve toplam orani yukari ceker. Yayin karari yalnizca gercek
     * bildirimlerin oranina bakmalidir.
     *
     * @return gercek alt kume kapisi gecti mi (yeterli ornek yoksa true)
     */
    private fun reportByOrigin(byOrigin: Map<String, IntArray>): Boolean {
        val real = byOrigin["REAL"] ?: IntArray(2)
        val synthetic = byOrigin["SYNTHETIC"] ?: IntArray(2)

        println("\n--- 1b) Kokene gore dogruluk ---")
        listOf("SYNTHETIC" to synthetic, "REAL" to real).forEach { (label, v) ->
            if (v[1] == 0) {
                println("  %-9s   0/0    (ornek yok)".format(label))
            } else {
                println("  %-9s %3d/%-3d  (%.1f%%)".format(label, v[0], v[1], v[0] * 100.0 / v[1]))
            }
        }

        println("\n  Yayin karari (roadmap madde 2):")
        if (real[1] < REAL_SAMPLE_TARGET) {
            println("    KARAR VERILEMEZ - ${real[1]}/$REAL_SAMPLE_TARGET gercek ornek.")
            println("    ${REAL_SAMPLE_TARGET - real[1]} gercek bildirim daha gerekiyor.")
            println("    Ekleme:  ./scripts/add-fixture.sh \"<metin>\" EXPENSE <kurus> \"<isyeri>\"")
            return true // ornek yoklugu basarisizlik degil, henuz olculmemis demek
        }

        val realAcc = real[0] * 100.0 / real[1]
        return if (realAcc >= THRESHOLD) {
            println("    YAYINLANABILIR - gercek ornek dogrulugu %.1f%% (esik %%%.0f)".format(realAcc, THRESHOLD))
            true
        } else {
            println("    YAYINLAMA - gercek ornek dogrulugu %.1f%%, esik %%%.0f".format(realAcc, THRESHOLD))
            println("    patterns/patterns.json'a desen ekleyip tekrar olcun.")
            false
        }
    }

    private fun outcome(parser: BankNotificationParser, text: String): Triple<String, String, String> =
        when (val r = parser.parse(text)) {
            is ParseResult.Ignored -> Triple("IGNORE", "-", "-")
            ParseResult.NoMatch -> Triple("NONE", "-", "-")
            is ParseResult.Match -> Triple(
                r.transaction.kind.name,
                r.transaction.amountMinor.toString(),
                r.transaction.merchant ?: "-"
            )
        }

    // ---------- 2) Uctan uca akis ----------

    /**
     * Gercek bir gunu simule eder: bildirimler gelir, bazilari tekrarlanir,
     * bazilari OTP'dir, biri iadedir. Sonucta ay toplaminin ve kategori
     * dagiliminin dogru olmasi beklenir.
     */
    private fun runEndToEnd(parser: BankNotificationParser): Boolean {
        println("--- 2) Uctan uca akis (ayristir -> tekrar koru -> kategorile -> topla) ---")

        val bank = "com.garanti.cepsubesi"
        val t0 = timestamp(2026, Calendar.AUGUST, 21, 9, 0)

        // (metin, gonderim zamani)
        val stream = listOf(
            "Garanti BBVA: 1234 kartiniz ile MIGROS isyerinde 245,90 TL tutarinda harcama yapilmistir." to t0,
            // ayni bildirim 4 saniye sonra tekrar teslim edildi -> sayilmamali
            "Garanti BBVA: 1234 kartiniz ile MIGROS isyerinde 245,90 TL tutarinda harcama yapilmistir." to (t0 + 4_000),
            "Tek kullanımlık şifreniz: 445566. Kimseyle paylaşmayın." to (t0 + 60_000),
            "Garanti BBVA: 1234 kartiniz ile SHELL PETROL isyerinde 1.200,00 TL tutarinda harcama yapilmistir." to (t0 + 3_600_000),
            "Garanti BBVA: 1234 kartiniz ile STARBUCKS isyerinde 175,00 TL tutarinda harcama yapilmistir." to (t0 + 7_200_000),
            // ayni magazadan 3 saat sonra gercek ikinci alisveris -> sayilmali
            "Garanti BBVA: 1234 kartiniz ile MIGROS isyerinde 245,90 TL tutarinda harcama yapilmistir." to (t0 + 10_800_000),
            "MIGROS isyerinden 45,00 TL iade yapılmıştır." to (t0 + 14_400_000),
            "Kargonuz yola çıktı, takip numarası 1234567890." to (t0 + 15_000_000),
        )

        val ledger = LinkedHashMap<String, LedgerEntry>() // sourceKey -> entry (unique index taklidi)
        var ignored = 0
        var unmatched = 0

        for ((text, postedAt) in stream) {
            if (!parser.isKnownSource(bank)) continue
            when (val result = parser.parse(text)) {
                is ParseResult.Ignored -> ignored++
                ParseResult.NoMatch -> unmatched++
                is ParseResult.Match -> {
                    val entry = Ledger.entryFor(result.transaction, bank, postedAt)
                    ledger.putIfAbsent(entry.sourceKey, entry)
                }
            }
        }

        val entries = ledger.values.toList()
        val total = Ledger.total(entries)
        val categories = Ledger.byCategory(entries)

        println("  gelen bildirim      : ${stream.size}")
        println("  yok sayilan (OTP vb): $ignored")
        println("  eslesmeyen          : $unmatched")
        println("  kaydedilen islem    : ${entries.size}")
        println("  ay toplami          : ${Money.format(total)} TL")
        categories.forEach { (c, amount) ->
            println("    ${c.label.padEnd(12)} ${Money.format(amount)} TL")
        }

        // Beklenen: 245,90 + 1.200,00 + 175,00 + 245,90 - 45,00 = 1.821,80
        val checks = listOf(
            check("tekrarlanan bildirim yok sayildi", entries.size == 5),
            check("OTP islem sayilmadi", ignored == 1),
            check("kargo bildirimi eslesmedi", unmatched == 1),
            check("ay toplami dogru", total == 182180L),
            check("Market kategorisi dogru", categories.firstOrNull { it.first == Category.MARKET }?.second == 44680L),
            check("Ulasim kategorisi dogru", categories.firstOrNull { it.first == Category.ULASIM }?.second == 120000L),
            check("ayni magazadan 3 saat sonraki alisveris korundu",
                entries.count { it.merchant == "Migros" && it.kind == TxKind.EXPENSE } == 2),
            check("ay araligi hesabi tutarli", Ledger.monthRange(2026, Calendar.AUGUST).let { (f, t) -> t0 in f..t })
        )
        return checks.all { it }
    }

    private fun check(label: String, ok: Boolean): Boolean {
        println("  [${if (ok) "GECTI" else "KALDI"}] $label")
        return ok
    }

    private fun timestamp(y: Int, m: Int, d: Int, h: Int, min: Int): Long =
        Calendar.getInstance().apply { clear(); set(y, m, d, h, min, 0) }.timeInMillis
}
