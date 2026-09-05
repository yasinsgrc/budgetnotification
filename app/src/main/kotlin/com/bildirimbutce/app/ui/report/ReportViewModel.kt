package com.bildirimbutce.app.ui.report

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bildirimbutce.app.data.ExpenseRepository
import com.bildirimbutce.app.ui.MonthCursor
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * C1 raporunun ViewModel'i.
 *
 * Ay disaridan geliyor ([setMonth]) cunku rapor, ana ekranda hangi ay aciksa
 * onun raporudur; ay adres uzerinde tasinip ekran acilirken veriliyor. Hedef
 * belirlenene kadar akis baslamiyor (`filterNotNull`) - aksi halde ekran once
 * "bu ay"in verisini cekip hemen istenen aya atlardi ve kullanici bir anlik
 * baska bir ayin sayilarini gorurdu.
 *
 * Hesaplarin tamami saf [toReportState] icinde; burasi yalnizca Room akisini
 * o fonksiyona bagliyor.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ReportViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = ExpenseRepository(app)

    private val target = MutableStateFlow<MonthCursor?>(null)

    val state: StateFlow<ReportUiState> = target
        .filterNotNull()
        .flatMapLatest { cursor ->
            repository.observeMonths(cursor.year, cursor.month, REPORT_MONTH_COUNT)
                .map { rows -> rows.toReportState(cursor, System.currentTimeMillis()) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReportUiState())

    fun setMonth(year: Int, month: Int) {
        target.value = MonthCursor(year, month)
    }
}
