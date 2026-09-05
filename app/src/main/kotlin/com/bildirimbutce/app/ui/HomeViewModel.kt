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

        fun now(): MonthCursor = of(System.currentTimeMillis())
    }
}

data class HomeUiState(
    val expenses: List<ExpenseEntity> = emptyList(),
    val totalMinor: Long = 0,
    val byCategory: List<Pair<Category, Long>> = emptyList(),
    /** Yalnizca elle girilenler: izin kapaliyken (B3) gosterilen liste. */
    val manualExpenses: List<ExpenseEntity> = emptyList(),
    val manualTotalMinor: Long = 0
)

/**
 * Kayit listesini ekran durumuna cevirir. Iade (REFUND) toplamdan dusulur;
 * bu kural bozulursa kullanici ayin toplamina guvenemez.
 *
 * ViewModel'in disinda duruyor ki Android baglami olmadan test edilebilsin.
 */
internal fun List<ExpenseEntity>.toUiState(): HomeUiState {
    val grouped = groupBy { Category.from(it.category) }
        .map { (category, items) -> category to items.sumOf { it.signedMinor() } }
        .filter { it.second > 0 }
        .sortedByDescending { it.second }
    val manual = filter { it.sourceApp == Ledger.MANUAL_SOURCE }
    return HomeUiState(
        expenses = this,
        totalMinor = sumOf { it.signedMinor() },
        byCategory = grouped,
        manualExpenses = manual,
        manualTotalMinor = manual.sumOf { it.signedMinor() }
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
        .flatMapLatest { repository.observeMonth(it.year, it.month) }
        .map { list -> list.toUiState() }
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
