package com.bildirimbutce.app.ui.pro

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bildirimbutce.app.data.ProAccess
import com.bildirimbutce.app.data.PurchaseOutcome
import com.bildirimbutce.app.data.StoredProAccess
import com.bildirimbutce.app.util.Prefs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Paywall ekraninin (E1) ViewModel'i.
 *
 * Satin alma yolu baglandiginda degisecek tek satir [access]'in kuruldugu yer;
 * ekran [ProAccess] davranisini goruyor, kalicilik ayrintisini degil.
 */
class ProViewModel(app: Application) : AndroidViewModel(app) {

    private val access: StoredProAccess = StoredProAccess(Prefs(app))

    val isPro: StateFlow<Boolean> = access.isPro

    private val _message = MutableStateFlow<String?>(null)

    /**
     * Son denemenin kullaniciya donen cevabi.
     *
     * Satin alma bugun her zaman [PurchaseOutcome.Unavailable] donuyor ve
     * sebebi ekranda yaziyor. Bos birakilsaydi dokunulup hicbir sey olmayan
     * bir dugme kalirdi.
     */
    val message: StateFlow<String?> = _message.asStateFlow()

    fun purchase() = viewModelScope.launch {
        _message.value = access.purchase().describe()
    }

    fun restore() = viewModelScope.launch {
        _message.value = access.restore().describe()
    }

    /**
     * Yetkiyi elle yazar - **yalnizca hata ayiklama derlemesindeki anahtar**
     * cagiriyor (bkz. [PaywallScreen]). Ucretsiz surumun uc aylik siniri
     * cihazda baska turlu gorulemezdi: satin alma yolu henuz yok.
     */
    fun setEntitlementForDebug(value: Boolean) {
        access.setEntitlement(value)
        _message.value = null
    }

    private fun PurchaseOutcome.describe(): String? = when (this) {
        is PurchaseOutcome.Granted -> null
        is PurchaseOutcome.Cancelled -> "Satın alma yarıda kesildi."
        is PurchaseOutcome.Unavailable -> reason
    }
}
