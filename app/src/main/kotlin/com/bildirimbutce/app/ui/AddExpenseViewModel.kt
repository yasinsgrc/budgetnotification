package com.bildirimbutce.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bildirimbutce.app.data.ExpenseRepository
import com.bildirimbutce.parser.Categorizer
import com.bildirimbutce.parser.Category
import com.bildirimbutce.parser.Money
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Elle harcama giris formunun durumu.
 *
 * [category] null demek "kullanici henuz secmedi": kategori isyeri adindan
 * tahmin edilir. Kullanici bir cipe dokundugu anda karar onun olur ve tahmin
 * devreye girmez; bu ayrim kayittaki `userEdited` bayragini belirliyor.
 */
data class ManualEntryDraft(
    val amountText: String = "",
    val merchant: String = "",
    val category: Category? = null
) {
    /** Ekranda secili gorunecek kategori: kullanicinin secimi, yoksa tahmin. */
    val effectiveCategory: Category
        get() = category ?: Categorizer.guess(merchant.trim().ifBlank { null })

    /** Tutar tek zorunlu alan; cozulemiyorsa kaydedilemez. */
    val amountMinor: Long?
        get() = Money.toMinor(amountText)

    val canSave: Boolean
        get() = amountMinor != null
}

/**
 * Tutar alanina yalnizca rakam ve tek bir ondalik ayraci girilebilsin.
 *
 * Klavye filtresine guvenilmiyor: cihaza gore nokta da virgul de gelebiliyor,
 * ikisini birden yazan kullanici [Money.toMinor]'un binlik ayraci kuralina
 * takilirdi. Nokta virgule cevriliyor, ikinci ayrac ve ikiden fazla kurus
 * hanesi yok sayiliyor.
 */
internal fun String.asAmountInput(): String {
    val builder = StringBuilder()
    var separatorSeen = false
    var decimals = 0
    for (raw in this) {
        val ch = if (raw == '.') ',' else raw
        when {
            ch.isDigit() && separatorSeen && decimals == 2 -> Unit
            ch.isDigit() -> {
                builder.append(ch)
                if (separatorSeen) decimals++
            }
            ch == ',' && !separatorSeen && builder.isNotEmpty() -> {
                builder.append(ch)
                separatorSeen = true
            }
        }
    }
    return builder.toString()
}

class AddExpenseViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = ExpenseRepository(app)

    private val _draft = MutableStateFlow(ManualEntryDraft())
    val draft: StateFlow<ManualEntryDraft> = _draft.asStateFlow()

    fun setAmount(text: String) {
        _draft.value = _draft.value.copy(amountText = text.asAmountInput())
    }

    fun setMerchant(text: String) {
        _draft.value = _draft.value.copy(merchant = text)
    }

    fun setCategory(category: Category) {
        _draft.value = _draft.value.copy(category = category)
    }

    /**
     * Kaydeder, sonra [onSaved] ile ekrani kapatir.
     *
     * Tarih "su an": elle giris, olan biteni ayni gun not etmek icin. Gecmise
     * tarih secimi bilerek yok - yol haritasindaki madde bunu istemiyor ve
     * tarih secici formu iki katina cikarirdi. Ekran hangi tarihe yazildigini
     * acikca gosteriyor, kullanici karanlikta kalmiyor.
     */
    fun save(onSaved: () -> Unit) {
        val draft = _draft.value
        val amountMinor = draft.amountMinor ?: return
        viewModelScope.launch {
            repository.addManual(
                amountMinor = amountMinor,
                merchant = draft.merchant,
                category = draft.category,
                occurredAt = System.currentTimeMillis()
            )
            onSaved()
        }
    }
}
