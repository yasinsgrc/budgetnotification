package com.bildirimbutce.parser

/** Bildirimden cikarilan islem tipi. */
enum class TxKind { EXPENSE, REFUND }

/**
 * Ayristirilmis tek bir islem.
 *
 * @param amountMinor Tutar kurus cinsinden (245,90 TL -> 24590). Para birimlerinde
 *   Double kullanmiyoruz; yuvarlama hatasi birikir.
 * @param merchant Temizlenmis magaza adi. Ayristirilamazsa null.
 * @param patternId Hangi desenin esleştigi - hata ayiklama ve dogruluk olcumu icin.
 * @param confidence 0..1. Magaza adi bulunamayan fallback esleşmeleri dusuk skor alir.
 */
data class ParsedTransaction(
    val kind: TxKind,
    val amountMinor: Long,
    val currency: String,
    val merchant: String?,
    val patternId: String,
    val confidence: Float,
    val rawText: String
)

/** Ayristirma sonucu. Ignore, "eslesmedi"den farklidir: bilerek atlandi demektir. */
sealed interface ParseResult {
    data class Match(val transaction: ParsedTransaction) : ParseResult
    data class Ignored(val reason: String) : ParseResult
    data object NoMatch : ParseResult
}
