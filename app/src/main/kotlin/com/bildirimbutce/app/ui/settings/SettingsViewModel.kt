package com.bildirimbutce.app.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bildirimbutce.app.data.ExpenseRepository
import com.bildirimbutce.app.data.NotificationSource
import com.bildirimbutce.app.data.PatternProvider
import com.bildirimbutce.app.data.SourceSelection
import com.bildirimbutce.app.data.db.MerchantRuleEntity
import com.bildirimbutce.app.util.Prefs
import com.bildirimbutce.app.widget.BudgetWidget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** F2'deki tek satir: kaynak ve anahtarinin durumu. */
data class SourceRow(val source: NotificationSource, val enabled: Boolean)

/**
 * Ayarlar bolumunun (F1-F4) tek ViewModel'i.
 *
 * Dort ekran ayni ornegi paylasiyor (bkz. `AppNavHost`): sayaclar F1'de,
 * anahtarlar F2'de duruyor. Ekran basina ayri ornek olsaydi F2'de kapatilan
 * banka F1'e donuldugunde hala acik gorunurdu - tercih diske yazilmis olsa
 * bile eski ornegin akisi bunu duymazdi.
 */
class SettingsViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = ExpenseRepository(app)
    private val prefs = Prefs(app)

    private val sources = MutableStateFlow<List<NotificationSource>>(emptyList())

    /** Kullanicinin tercihi; `null` = hic secim yapilmadi, hepsi dinleniyor. */
    private val stored = MutableStateFlow(prefs.enabledSources)

    private val _patternVersion = MutableStateFlow(0)

    /** F1 alt bilgisindeki "desen seti v1"; elle yazilsa guncellemede yalan olurdu. */
    val patternVersion: StateFlow<Int> = _patternVersion.asStateFlow()

    val sourceRows: StateFlow<List<SourceRow>> = combine(sources, stored) { all, selection ->
        val enabled = SourceSelection.enabled(all, selection)
        all.map { SourceRow(it, it.packageName in enabled) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val rules: StateFlow<List<MerchantRuleEntity>> = repository.observeMerchantRules()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val expenseCount: StateFlow<Int> = repository.observeExpenseCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    init {
        // Desen dosyasi diskten okunuyor; ana is parcaciginda acilmasi ayarlar
        // ekranini ilk karede takilmis gosterirdi.
        viewModelScope.launch {
            val context = getApplication<Application>()
            sources.value = withContext(Dispatchers.IO) { PatternProvider.sources(context) }
            _patternVersion.value = withContext(Dispatchers.IO) { PatternProvider.patternVersion(context) }
        }
    }

    fun setSourceEnabled(packageName: String, enabled: Boolean) {
        val next = SourceSelection.toggled(sources.value, stored.value, packageName, enabled)
        // Once diske: servis tercihi her bildirimde Prefs'ten okuyor, ekran ise
        // asagidaki akistan. Sira ters olsaydi anahtar ekranda kapanmis
        // gorunurken servis eski listeyle calismaya devam edebilirdi.
        prefs.enabledSources = next
        stored.value = next
    }

    fun forgetRule(merchantKey: String) = viewModelScope.launch {
        repository.forgetRule(merchantKey)
    }

    fun eraseAll() = viewModelScope.launch {
        repository.eraseAll()
        // Widget kendi sorgusunu ayri yapiyor; yenilenmezse silinmis bir ayin
        // toplamini ana ekranda gostermeye devam ederdi.
        BudgetWidget.refresh(getApplication())
    }
}
