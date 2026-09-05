package com.bildirimbutce.app.ui.report

import com.bildirimbutce.app.data.db.ExpenseEntity
import com.bildirimbutce.app.ui.MonthCursor
import com.bildirimbutce.app.ui.signedMinor
import com.bildirimbutce.parser.Category
import com.bildirimbutce.parser.TxKind
import java.util.Calendar
import kotlin.math.roundToInt

/**
 * C1 - aylik rapor ekraninin durumu ve hesaplari.
 *
 * ViewModel'in ve Compose'un disinda duruyor: ekranin tum aritmetigi (ay
 * cubuklari, gun ortalamasi, haftanin ritmi, en cok gidilen yerler) burada ve
 * Android baglami olmadan test edilebilmesi gerekiyor. Rapor kullaniciya
 * "paran su gune gidiyor" diyor; yanlis bir bolme sessizce yanlis bir
 * aliskanlik ogretir.
 *
 * Butun toplamlar isaretli: iade (REFUND) dusulur. Kural tek yerde -
 * [signedMinor] ana ekranla paylasiliyor.
 */

/** Rapor penceresi: secili ay dahil geriye dogru kac ay cizilir. */
const val REPORT_MONTH_COUNT = 6

/** "EN COK GIDEN YERLER" listesinin uzunlugu. */
private const val TOP_MERCHANT_COUNT = 5

/**
 * Haftanin ritmi cumlesi ancak tepe gun ortalamayi bu kadar asarsa yazilir.
 * Daha dusugunde cumle gurultuye isaret parmagi uzatmis olurdu: yedi kovaya
 * bolunmus tek bir ayda %5-10 sapma tesadufun kendisidir.
 */
private const val WEEKDAY_NOTE_THRESHOLD_PERCENT = 15

/** Pazartesi ilk. `Calendar.DAY_OF_WEEK` pazari 1 saydigi icin kaydiriliyor. */
private val WEEKDAY_SHORT = listOf("Pt", "Sa", "Ça", "Pe", "Cu", "Ct", "Pz")
private val WEEKDAY_LONG = listOf(
    "Pazartesi", "Salı", "Çarşamba", "Perşembe", "Cuma", "Cumartesi", "Pazar"
)

/** Aylik cubuk. [isSelected] raporun acildigi ay. */
data class MonthBar(
    val cursor: MonthCursor,
    val totalMinor: Long,
    val isSelected: Boolean
)

/** Haftanin bir gunu. [isPeak] ayin en pahali gunu. */
data class WeekdayBar(
    val label: String,
    val totalMinor: Long,
    val isPeak: Boolean
)

/** "Cuma gunleri ortalamanin %64 ustunde" cumlesinin verisi. */
data class WeekdayPeak(val label: String, val percentAboveAverage: Int)

/** Ayin en pahali gunu. Ay adi [ReportUiState.cursor]'dan geliyor. */
data class PeakDay(val dayOfMonth: Int, val totalMinor: Long)

/** "En cok giden yerler" satiri. */
data class MerchantRow(
    val name: String,
    val category: Category,
    val count: Int,
    val totalMinor: Long
)

data class ReportUiState(
    val cursor: MonthCursor = MonthCursor(1970, 0),
    val months: List<MonthBar> = emptyList(),
    val totalMinor: Long = 0,
    val dailyAverageMinor: Long = 0,
    /** Gun ortalamasinin bolundugu gun sayisi - ekranda aciklamasi gosteriliyor. */
    val daysCounted: Int = 0,
    val peakDay: PeakDay? = null,
    val expenseCount: Int = 0,
    val refundCount: Int = 0,
    val weekdays: List<WeekdayBar> = emptyList(),
    val weekdayPeak: WeekdayPeak? = null,
    val topMerchants: List<MerchantRow> = emptyList()
) {
    /** Secili ayda hic kayit yok. Aylik cubuklar yine de cizilir. */
    val isEmpty: Boolean get() = expenseCount == 0 && refundCount == 0
}

/**
 * [REPORT_MONTH_COUNT] aylik pencereyi rapor durumuna cevirir.
 *
 * Liste pencerenin tamamini tasiyor; cubuklar tum pencereden, geri kalan her
 * sey yalnizca [cursor] ayindan hesaplaniyor.
 *
 * [now] disaridan geliyor cunku gun ortalamasi icinde bulunulan ayda bugune,
 * gecmis aylarda ayin tamamina bolunuyor - sabit bir saate baglamadan bu
 * ayrimi test etmenin yolu yok.
 */
internal fun List<ExpenseEntity>.toReportState(cursor: MonthCursor, now: Long): ReportUiState {
    val byMonth = groupBy { MonthCursor.of(it.occurredAt) }
    val months = (REPORT_MONTH_COUNT - 1 downTo 0).map { back ->
        val month = cursor.minus(back)
        MonthBar(
            cursor = month,
            totalMinor = byMonth[month].orEmpty().sumOf { it.signedMinor() },
            isSelected = month == cursor
        )
    }

    val rows = byMonth[cursor].orEmpty()
    val totalMinor = rows.sumOf { it.signedMinor() }
    val daysCounted = daysCounted(cursor, now)
    val weekdaySums = LongArray(WEEKDAY_SHORT.size)
    rows.forEach { weekdaySums[weekdayIndex(it.occurredAt)] += it.signedMinor() }
    val peakIndex = weekdaySums.indices
        .maxByOrNull { weekdaySums[it] }
        ?.takeIf { weekdaySums[it] > 0 }

    return ReportUiState(
        cursor = cursor,
        months = months,
        totalMinor = totalMinor,
        // Toplam negatifse (ay iadeyle kapandiysa) ortalama gostermek anlamsiz.
        dailyAverageMinor = if (totalMinor > 0 && daysCounted > 0) totalMinor / daysCounted else 0,
        daysCounted = daysCounted,
        peakDay = rows.peakDay(),
        expenseCount = rows.count { it.kind != TxKind.REFUND.name },
        refundCount = rows.count { it.kind == TxKind.REFUND.name },
        weekdays = WEEKDAY_SHORT.mapIndexed { index, label ->
            WeekdayBar(label, weekdaySums[index], index == peakIndex)
        },
        weekdayPeak = weekdayPeak(weekdaySums, peakIndex),
        topMerchants = rows.topMerchants()
    )
}

/** Ayin en pahali gunu. Net sifir ya da negatif gunler aday degil. */
private fun List<ExpenseEntity>.peakDay(): PeakDay? =
    groupBy { dayOfMonth(it.occurredAt) }
        .map { (day, items) -> PeakDay(day, items.sumOf { it.signedMinor() }) }
        .filter { it.totalMinor > 0 }
        .maxByOrNull { it.totalMinor }

/**
 * En cok gidilen yerler.
 *
 * Isyeri adi olmayan kayitlar disarida: "Bilinmeyen isyeri" bir yer degil,
 * ayristiricinin okuyamadigi bir satir - listenin basina cikmasi kullaniciya
 * var olmayan bir aliskanlik gosterirdi.
 *
 * Kategori en yeni kayittan aliniyor: kullanici bir duzeltme yaptiysa
 * ogrenilen kural en son satirda gorunur.
 */
private fun List<ExpenseEntity>.topMerchants(): List<MerchantRow> =
    mapNotNull { row -> row.merchant?.trim()?.ifBlank { null }?.let { it to row } }
        .groupBy({ it.first }, { it.second })
        .map { (name, items) ->
            MerchantRow(
                name = name,
                category = Category.from(items.maxByOrNull { it.occurredAt }?.category),
                count = items.size,
                totalMinor = items.sumOf { it.signedMinor() }
            )
        }
        .filter { it.totalMinor > 0 }
        .sortedByDescending { it.totalMinor }
        .take(TOP_MERCHANT_COUNT)

/**
 * Tepe gunun yedi gunluk ortalamayi asma orani.
 *
 * Bolen yedi kovanin ortalamasi (ayin toplami / 7), gecen gun sayisi degil:
 * cumle "hangi gun digerlerinden ayrisiyor" diyor, "gunde ne kadar
 * harciyorsun" demiyor - o sayi zaten gun ortalamasi kutusunda.
 */
private fun weekdayPeak(sums: LongArray, peakIndex: Int?): WeekdayPeak? {
    if (peakIndex == null) return null
    val average = sums.sum().toDouble() / sums.size
    if (average <= 0) return null
    val percent = ((sums[peakIndex] / average - 1) * 100).roundToInt()
    if (percent < WEEKDAY_NOTE_THRESHOLD_PERCENT) return null
    return WeekdayPeak(WEEKDAY_LONG[peakIndex], percent)
}

/**
 * Gun ortalamasinin bolundugu gun sayisi.
 *
 * Icinde bulunulan ayda ayin tamamina bolmek yanlis olurdu: ayin 3'unde
 * kullanici ortalamasini onda biri kadar gorur ve "iyi gidiyorum" sanirdi.
 * Gecmis aylarda ay kapandigi icin bolen ayin gun sayisi.
 */
private fun daysCounted(cursor: MonthCursor, now: Long): Int {
    val cal = Calendar.getInstance().apply { clear(); set(cursor.year, cursor.month, 1) }
    val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    if (MonthCursor.of(now) != cursor) return daysInMonth
    return dayOfMonth(now).coerceIn(1, daysInMonth)
}

private fun dayOfMonth(millis: Long): Int =
    Calendar.getInstance().apply { timeInMillis = millis }.get(Calendar.DAY_OF_MONTH)

/** Pazartesi = 0. `Calendar` pazari 1, cumartesiyi 7 sayar. */
private fun weekdayIndex(millis: Long): Int {
    val dow = Calendar.getInstance().apply { timeInMillis = millis }.get(Calendar.DAY_OF_WEEK)
    return (dow + 5) % 7
}
