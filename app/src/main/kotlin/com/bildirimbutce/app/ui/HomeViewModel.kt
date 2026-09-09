package com.bildirimbutce.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bildirimbutce.app.data.ExpenseRepository
import com.bildirimbutce.app.data.db.ExpenseEntity
import com.bildirimbutce.parser.Category
import com.bildirimbutce.parser.Ledger
import com.bildirimbutce.parser.TxKind
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/** Ay adlarinin buyuk harfe cevrildigi yerel ayar; varsayilana birakilamaz. */
private val TURKISH = Locale("tr", "TR")

/**
 * Ana ekranin cektigi pencere: acik ay + karsilastirilacak onceki ay.
 *
 * Iki ay tek sorguyla okunuyor. Ay basina ayri sorgu acilsaydi aylar
 * birbirinden farkli anlik goruntulere dusebilir, rozet ekrandaki toplamla
 * tutmayan bir yuzde gosterebilirdi.
 */
const val HOME_MONTH_COUNT = 2

data class MonthCursor(val year: Int, val month: Int) {
    fun previous(): MonthCursor =
        if (month == 0) MonthCursor(year - 1, 11) else MonthCursor(year, month - 1)

    fun next(): MonthCursor =
        if (month == 11) MonthCursor(year + 1, 0) else MonthCursor(year, month + 1)

    /**
     * [months] ay geri. Rapor penceresi ("son 6 ay") bunu kullaniyor.
     *
     * `previous()`'i tekrar tekrar cagirmak yerine 12 tabaninda hesaplaniyor:
     * negatif ay indeksi `floorMod` ile duzeldigi icin yil siniri ayri bir
     * durum olmaktan cikiyor.
     */
    fun minus(months: Int): MonthCursor {
        val total = year * 12 + month - months
        return MonthCursor(Math.floorDiv(total, 12), Math.floorMod(total, 12))
    }

    val label: String
        get() = "${MONTHS[month]} $year"

    /** Grafik ekseni icin kisa ad: "AĞU". */
    val shortLabel: String
        get() = SHORT_MONTHS[month]

    /**
     * Degisim rozetindeki ay adi: "TEMMUZ".
     *
     * Yerel ayar acikca veriliyor: Turkce buyuk harf kurali "i"yi "İ" yapar,
     * varsayilan yerel ayarda "NISAN"/"EKIM" cikardi. Ust satirdaki ay basligi
     * da (`MonthTopBar`) ayni kurali kullaniyor.
     */
    val upperLabel: String
        get() = MONTHS[month].uppercase(TURKISH)

    companion object {
        val MONTHS = listOf(
            "Ocak", "Şubat", "Mart", "Nisan", "Mayıs", "Haziran",
            "Temmuz", "Ağustos", "Eylül", "Ekim", "Kasım", "Aralık"
        )

        /**
         * Kisa adlar elle yazildi, `MONTHS`'tan kesilmedi: Turkce buyuk harf
         * kurali "İ" ve "I"yi ayirir, `uppercase()` yanlis yerel ayarda
         * "NIS"/"EKI" uretirdi.
         */
        val SHORT_MONTHS = listOf(
            "OCA", "ŞUB", "MAR", "NİS", "MAY", "HAZ",
            "TEM", "AĞU", "EYL", "EKİ", "KAS", "ARA"
        )

        /** Bir zaman damgasinin dustugu ay; rapor kayitlari aylara bununla boluyor. */
        fun of(millis: Long): MonthCursor {
            val c = Calendar.getInstance().apply { timeInMillis = millis }
            return MonthCursor(c.get(Calendar.YEAR), c.get(Calendar.MONTH))
        }

        /**
         * Zaman damgasinin ayin kacinci gunune dustugu.
         *
         * [of] ile ayni yerde duruyor: ikisi de ayni takvim yorumundan cikiyor
         * ve iki ekran birden kullaniyor (ana ekrandaki degisim rozeti, rapor
         * ekranindaki gun ortalamasi). Iki kopya olsaydi biri degisip digeri
         * kalabilirdi.
         */
        fun dayOf(millis: Long): Int =
            Calendar.getInstance().apply { timeInMillis = millis }.get(Calendar.DAY_OF_MONTH)

        fun now(): MonthCursor = of(System.currentTimeMillis())
    }
}

/**
 * Aylik degisim rozeti: "↓ %12 TEMMUZ".
 *
 * [percent] her zaman pozitif; yonu [increased] tasiyor. Ikisi tek bir isaretli
 * sayida birlesseydi ekran her yerde `abs()` cagirmak zorunda kalirdi.
 */
data class MonthChange(
    val percent: Int,
    val increased: Boolean,
    /** Karsilastirilan ayin adi: "TEMMUZ". */
    val previousLabel: String,
    /**
     * Karsilastirma onceki ayin ilk kac gunuyle sinirli; ay kapandiysa null.
     *
     * Icinde bulunulan ayin dokuzuncu gununde, kapanmis bir ayin tamamiyla
     * karsilastirma yapmak "↓ %70" gibi bir sayi uretirdi: kullanici ayin
     * ucte birinde durup "harcamalarim ucte bire dustu" derdi.
     */
    val comparedDays: Int?
)

data class HomeUiState(
    val expenses: List<ExpenseEntity> = emptyList(),
    val totalMinor: Long = 0,
    val byCategory: List<Pair<Category, Long>> = emptyList(),
    /** Yalnizca elle girilenler: izin kapaliyken (B3) gosterilen liste. */
    val manualExpenses: List<ExpenseEntity> = emptyList(),
    val manualTotalMinor: Long = 0,
    /**
     * Onceki ayin karsilastirilabilir toplami - rozetin boleni.
     *
     * [change] null oldugunda da dolu olabilir (yuzde sifira yuvarlandiysa ya
     * da bu ay iadeyle negatif kapandiysa rozet cizilmiyor); rozetin ciziliyor
     * olmasi ile karsilastirmanin yapilabilmis olmasi ayri seyler.
     */
    val previousTotalMinor: Long = 0,
    val change: MonthChange? = null
)

/**
 * Iki aylik pencereyi ekran durumuna cevirir.
 *
 * Liste [cursor] ayini ve ondan onceki ayi tasiyor (bkz. [HOME_MONTH_COUNT]);
 * ekranda gosterilen her sey yalnizca [cursor] ayindan, degisim rozetinin
 * boleni onceki aydan hesaplaniyor.
 *
 * Iade (REFUND) toplamdan dusulur; bu kural bozulursa kullanici ayin toplamina
 * guvenemez.
 *
 * [now] disaridan geliyor cunku acik olan ay henuz bitmemisse karsilastirma
 * onceki ayin ayni gunune kadar kisiliyor - sabit bir saate baglamadan bu
 * ayrimi test etmenin yolu yok.
 *
 * ViewModel'in disinda duruyor ki Android baglami olmadan test edilebilsin.
 */
internal fun List<ExpenseEntity>.toUiState(cursor: MonthCursor, now: Long): HomeUiState {
    val rows = filter { MonthCursor.of(it.occurredAt) == cursor }
    val grouped = rows.groupBy { Category.from(it.category) }
        .map { (category, items) -> category to items.sumOf { it.signedMinor() } }
        .filter { it.second > 0 }
        .sortedByDescending { it.second }
    val manual = rows.filter { it.sourceApp == Ledger.MANUAL_SOURCE }

    val totalMinor = rows.sumOf { it.signedMinor() }
    val previous = cursor.previous()
    // Acik ay bitmediyse onceki aydan da yalnizca ayni gune kadari sayiliyor.
    val comparedDays = if (MonthCursor.of(now) == cursor) MonthCursor.dayOf(now) else null
    val previousTotalMinor = filter { row ->
        MonthCursor.of(row.occurredAt) == previous &&
            (comparedDays == null || MonthCursor.dayOf(row.occurredAt) <= comparedDays)
    }.sumOf { it.signedMinor() }

    return HomeUiState(
        expenses = rows,
        totalMinor = totalMinor,
        byCategory = grouped,
        manualExpenses = manual,
        manualTotalMinor = manual.sumOf { it.signedMinor() },
        previousTotalMinor = previousTotalMinor,
        change = monthChange(totalMinor, previousTotalMinor, previous, comparedDays)
    )
}

/**
 * Rozetin kendisi. Susmasi gereken uc durum var; ucu de sayinin anlamsiz
 * oldugu yerler:
 *
 *  - onceki ay net sifir ya da negatifse bolen yok ("%sonsuz artti" denemez),
 *  - acik ay iadeyle negatife dustuyse yuzde artik harcamayi anlatmiyor,
 *  - fark yuzde yarimin altindaysa rozet "↓ %0" yazardi.
 */
private fun monthChange(
    totalMinor: Long,
    previousTotalMinor: Long,
    previous: MonthCursor,
    comparedDays: Int?
): MonthChange? {
    if (previousTotalMinor <= 0 || totalMinor < 0) return null
    val percent = ((totalMinor - previousTotalMinor) * 100.0 / previousTotalMinor).roundToInt()
    if (percent == 0) return null
    return MonthChange(
        percent = abs(percent),
        increased = percent > 0,
        previousLabel = previous.upperLabel,
        comparedDays = comparedDays
    )
}

/**
 * Iade negatif sayilir. `internal`: rapor ekrani da ayni kurali kullaniyor ve
 * kuralin iki kopyasi olsaydi biri degisip digeri kalabilirdi.
 */
internal fun ExpenseEntity.signedMinor(): Long =
    if (kind == TxKind.REFUND.name) -amountMinor else amountMinor

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = ExpenseRepository(app)

    private val _cursor = MutableStateFlow(MonthCursor.now())
    val cursor: StateFlow<MonthCursor> = _cursor.asStateFlow()

    val state: StateFlow<HomeUiState> = _cursor
        .flatMapLatest { cursor ->
            repository.observeMonths(cursor.year, cursor.month, HOME_MONTH_COUNT)
                .map { rows -> rows.toUiState(cursor, System.currentTimeMillis()) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun previousMonth() { _cursor.value = _cursor.value.previous() }

    fun nextMonth() { _cursor.value = _cursor.value.next() }

    fun setCategory(expense: ExpenseEntity, category: Category) = viewModelScope.launch {
        repository.correctCategory(expense, category)
    }

    fun setMerchant(expense: ExpenseEntity, merchant: String) = viewModelScope.launch {
        repository.update(expense.copy(merchant = merchant.trim().ifBlank { null }, userEdited = true))
    }

    fun delete(expense: ExpenseEntity) = viewModelScope.launch {
        repository.delete(expense)
    }
}
