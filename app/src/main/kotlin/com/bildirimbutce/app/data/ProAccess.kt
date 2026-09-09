package com.bildirimbutce.app.data

import com.bildirimbutce.app.util.Prefs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Bir satin alma denemesinin sonucu.
 *
 * [Unavailable] bir hata degil, bir **durum**: satin alma yolu baglanana kadar
 * dondurulen tek cevap bu ve ekran sebebini kullaniciya oldugu gibi gosteriyor.
 * Sessizce hicbir sey yapmayan bir dugme, 9. maddede kaldirilan olu tiklamanin
 * aynisi olurdu.
 */
sealed interface PurchaseOutcome {
    data object Granted : PurchaseOutcome
    data object Cancelled : PurchaseOutcome
    data class Unavailable(val reason: String) : PurchaseOutcome
}

/**
 * Pro yetkisinin tek kapisi.
 *
 * Arayuz, [StoredProAccess] ile Play Billing arasindaki dikis: bugun yetki
 * yalnizca cihazda tutulan bir bayraktan okunuyor, yarin ayni arayuzu uygulayan
 * bir `PlayBillingProAccess` gelecek. Ekranlar ve ViewModel'ler bayragi degil bu
 * arayuzu gordugu icin o gun degisecek tek dosya uygulamanin kendisi.
 *
 * [refresh] gecici bir test kancasi degil, gercek API yuzeyi: Play'de yapilan
 * bir satin alma (ya da iade) uygulamanin disinda gerceklesir, bu yuzden yetki
 * one cikildiginda yeniden sorulmak zorunda.
 */
interface ProAccess {
    val isPro: StateFlow<Boolean>

    /** Kalici yetkiyi kaynagindan tekrar okur. */
    fun refresh()

    suspend fun purchase(): PurchaseOutcome

    suspend fun restore(): PurchaseOutcome
}

/**
 * Yetkiyi [Prefs] uzerinden okuyan uygulama - satin alma yolu **yok**.
 *
 * Billing bagimliligi bilerek eklenmedi: Play Console'da ne uygulama ne de urun
 * tanimli, imzali bir APK hic uretilmedi (yol haritasi madde 7). Bu kosullarda
 * yazilacak `BillingClient` kodu derlenir ama bir kez bile calistirilamazdi -
 * 7. maddenin R8 kurallari icin elestirdigi "yazildi ama denenmedi" durumunun
 * aynisi. Gerekce [purchase] cevabinda kullaniciya da ayni cumleyle yaziliyor.
 */
class StoredProAccess(private val prefs: Prefs) : ProAccess {

    private val _isPro = MutableStateFlow(prefs.isPro)
    override val isPro: StateFlow<Boolean> = _isPro.asStateFlow()

    override fun refresh() {
        _isPro.value = prefs.isPro
    }

    override suspend fun purchase(): PurchaseOutcome = PurchaseOutcome.Unavailable(UNAVAILABLE)

    override suspend fun restore(): PurchaseOutcome = PurchaseOutcome.Unavailable(UNAVAILABLE)

    /**
     * Yetkiyi elle yazar. Uretim akisinda **cagirilmiyor**; satin alma yolu
     * baglandiginda `PlayBillingProAccess` kendi yetkisini kendi yazacak. Bugun
     * iki yerde kullaniliyor: testler ve hata ayiklama derlemesindeki anahtar -
     * ucretsiz surumun sinirinin cihazda gorulebilmesi icin.
     */
    fun setEntitlement(value: Boolean) {
        prefs.isPro = value
        refresh()
    }

    private companion object {
        const val UNAVAILABLE =
            "Satın alma henüz açılmadı: Play Console kurulumu tamamlanmadı ve " +
                "uygulama hiç imzalanıp yayınlanmadı."
    }
}
