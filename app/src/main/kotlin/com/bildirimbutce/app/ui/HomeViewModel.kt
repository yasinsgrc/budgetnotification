package com.bildirimbutce.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bildirimbutce.app.data.ExpenseRepository
import com.bildirimbutce.app.data.db.ExpenseEntity
import com.bildirimbutce.parser.Category
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

    val label: String
        get() = "${MONTHS[month]} $year"

    companion object {
        val MONTHS = listOf(
            "Ocak", "Şubat", "Mart", "Nisan", "Mayıs", "Haziran",
            "Temmuz", "Ağustos", "Eylül", "Ekim", "Kasım", "Aralık"
        )

        fun now(): MonthCursor {
            val c = Calendar.getInstance()
            return MonthCursor(c.get(Calendar.YEAR), c.get(Calendar.MONTH))
        }
    }
}

data class HomeUiState(
    val expenses: List<ExpenseEntity> = emptyList(),
    val totalMinor: Long = 0,
    val byCategory: List<Pair<Category, Long>> = emptyList()
)

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

    private fun List<ExpenseEntity>.toUiState(): HomeUiState {
        val total = sumOf { if (it.kind == TxKind.REFUND.name) -it.amountMinor else it.amountMinor }
        val grouped = groupBy { Category.from(it.category) }
            .map { (category, items) ->
                category to items.sumOf {
                    if (it.kind == TxKind.REFUND.name) -it.amountMinor else it.amountMinor
                }
            }
            .filter { it.second > 0 }
            .sortedByDescending { it.second }
        return HomeUiState(expenses = this, totalMinor = total, byCategory = grouped)
    }
}
