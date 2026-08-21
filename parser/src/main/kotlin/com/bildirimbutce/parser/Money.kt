package com.bildirimbutce.parser

import java.math.BigDecimal
import java.math.RoundingMode

object Money {

    /**
     * Turk bankalarinin yazdigi tutar bicimlerini kurusa cevirir.
     * "1.249,00" -> 124900 | "89,90" -> 8990 | "12.500" -> 1250000 | "45" -> 4500
     *
     * Nokta binlik ayraci, virgul ondalik ayracidir. Sadece nokta iceren ve
     * 3'lu gruplanmis "12.500" gibi degerler binlik kabul edilir.
     */
    fun toMinor(raw: String): Long? {
        var s = raw.trim().replace(" ", "").replace("\u00A0", "")
        s = when {
            Regex("""^\d{1,3}(\.\d{3})+,\d{1,2}$""").matches(s) -> s.replace(".", "").replace(',', '.')
            Regex("""^\d+,\d{1,2}$""").matches(s) -> s.replace(',', '.')
            Regex("""^\d{1,3}(\.\d{3})+$""").matches(s) -> s.replace(".", "")
            Regex("""^\d+$""").matches(s) -> s
            else -> s.replace(".", "").replace(',', '.')
        }
        val value = s.toBigDecimalOrNull() ?: return null
        if (value <= BigDecimal.ZERO) return null
        return value.setScale(2, RoundingMode.HALF_UP).movePointRight(2).toLong()
    }

    /** 24590 -> "245,90" */
    fun format(amountMinor: Long): String {
        val sign = if (amountMinor < 0) "-" else ""
        val abs = kotlin.math.abs(amountMinor)
        val lira = abs / 100
        val kurus = abs % 100
        val grouped = lira.toString()
            .reversed()
            .chunked(3)
            .joinToString(".")
            .reversed()
        return "$sign$grouped,${kurus.toString().padStart(2, '0')}"
    }

    private fun String.toBigDecimalOrNull(): BigDecimal? =
        try { BigDecimal(this) } catch (e: NumberFormatException) { null }
}
