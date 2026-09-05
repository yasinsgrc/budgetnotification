package com.bildirimbutce.app.data

import android.content.Context
import com.bildirimbutce.app.data.db.AppDatabase
import com.bildirimbutce.app.data.db.ExpenseEntity
import com.bildirimbutce.app.data.db.MerchantRuleEntity
import com.bildirimbutce.parser.Categorizer
import com.bildirimbutce.parser.Category
import com.bildirimbutce.parser.Ledger
import com.bildirimbutce.parser.ParsedTransaction
import com.bildirimbutce.parser.TxKind
import kotlinx.coroutines.flow.Flow
import java.util.Locale
import java.util.UUID

class ExpenseRepository internal constructor(db: AppDatabase) {

    /** Uygulama icindeki normal kullanim: tek surumlu veritabanini kullanir. */
    constructor(context: Context) : this(AppDatabase.get(context))

    private val expenses = db.expenseDao()
    private val rules = db.merchantRuleDao()

    fun observeMonth(year: Int, month: Int): Flow<List<ExpenseEntity>> {
        val (from, to) = Ledger.monthRange(year, month)
        return expenses.observeBetween(from, to)
    }

    /**
     * [year]/[month] ayinda biten [monthCount] aylik pencere - rapor ekrani icin.
     *
     * Tek akis donuyor, ay basina bir tane degil: rapor hem aylik cubuklari hem
     * secili ayin ayrintisini ayni listeden turetiyor. Ayri sorgular olsaydi
     * aylar birbirinden farkli anlik goruntulere dusebilir, cubuklarin toplami
     * ile ekrandaki ay toplami tutmayabilirdi.
     */
    fun observeMonths(year: Int, month: Int, monthCount: Int): Flow<List<ExpenseEntity>> {
        val (from, to) = Ledger.rangeEndingAt(year, month, monthCount)
        return expenses.observeBetween(from, to)
    }

    suspend fun monthTotalMinor(year: Int, month: Int): Long {
        val (from, to) = Ledger.monthRange(year, month)
        return expenses.getBetween(from, to).sumOf { it.signedAmount() }
    }

    /**
     * Ayristirilmis bildirimi kaydeder. Ayni bildirim tekrar gelirse
     * sourceKey catismasi nedeniyle sessizce yok sayilir.
     * @return eklendiyse true
     */
    suspend fun record(
        tx: ParsedTransaction,
        sourceApp: String,
        postedAt: Long
    ): Boolean {
        // Is mantigi saf cekirdekte (:parser) - burada yalnizca Room'a cevriliyor.
        val entry = Ledger.entryFor(tx, sourceApp, postedAt, resolveCategory(tx.merchant))
        val entity = ExpenseEntity(
            amountMinor = entry.amountMinor,
            currency = entry.currency,
            merchant = entry.merchant,
            category = entry.category.name,
            kind = entry.kind.name,
            occurredAt = entry.occurredAt,
            sourceApp = entry.sourceApp,
            patternId = entry.patternId,
            confidence = entry.confidence,
            rawText = entry.rawText,
            sourceKey = entry.sourceKey
        )
        return expenses.insert(entity) != -1L
    }

    /**
     * Elle girilen harcamayi kaydeder.
     *
     * Bildirim erisimi reddedilirse elde calisan bir butce defteri kalmasi
     * gerekiyor; bu yol o yuzden var. Tekrar korumasi bilerek devre disi:
     * [ExpenseEntity.sourceKey] rastgele uretiliyor, cunku kullanici ayni
     * tutari gercekten iki kez girebilir ve ikisini de gormeli.
     *
     * [category] null ise magazadan cozuluyor (once ogrenilmis kural, sonra
     * anahtar kelime) ve kayit `userEdited = false` kaliyor - boylece kullanici
     * ileride bu magazayi duzeltirse gecmise donuk duzeltme bunu da yakalar.
     * Kullanici kategoriyi ekranda kendi sectiyse karar onundur, ezilmemeli.
     *
     * @return eklenen kaydin id'si
     */
    suspend fun addManual(
        amountMinor: Long,
        merchant: String?,
        category: Category?,
        occurredAt: Long
    ): Long {
        val cleaned = merchant?.trim()?.ifBlank { null }
        // Is mantigi saf cekirdekte (:parser) - burada yalnizca Room'a cevriliyor.
        val entry = Ledger.manualEntry(
            amountMinor = amountMinor,
            merchant = cleaned,
            category = category ?: resolveCategory(cleaned),
            occurredAt = occurredAt,
            sourceKey = "${Ledger.MANUAL_SOURCE}:${UUID.randomUUID()}"
        )
        return expenses.insert(
            ExpenseEntity(
                amountMinor = entry.amountMinor,
                currency = entry.currency,
                merchant = entry.merchant,
                category = entry.category.name,
                kind = entry.kind.name,
                occurredAt = entry.occurredAt,
                sourceApp = entry.sourceApp,
                patternId = entry.patternId,
                confidence = entry.confidence,
                rawText = entry.rawText,
                sourceKey = entry.sourceKey,
                userEdited = category != null
            )
        )
    }

    suspend fun update(expense: ExpenseEntity) = expenses.update(expense)

    suspend fun delete(expense: ExpenseEntity) = expenses.delete(expense)

    /**
     * Kullanici kategoriyi duzeltti: bu magaza icin kural ogrenilir ve
     * gecmisteki elle duzeltilmemis kayitlar da guncellenir.
     */
    suspend fun correctCategory(expense: ExpenseEntity, category: Category) {
        expenses.update(expense.copy(category = category.name, userEdited = true))
        val merchant = expense.merchant ?: return
        rules.upsert(MerchantRuleEntity(merchant.merchantKey(), category.name))
        expenses.recategorizeMerchant(merchant, category.name)
    }

    /** Ogrenilmis "isyeri -> kategori" kurallari (ayarlar > F3). */
    fun observeMerchantRules(): Flow<List<MerchantRuleEntity>> = rules.observeAll()

    /** Ayarlardaki kayit sayaci - secili ay degil, defterin tamami. */
    fun observeExpenseCount(): Flow<Int> = expenses.observeCount()

    /**
     * Bir kurali unutur.
     *
     * Gecmis kayitlara bilerek dokunulmuyor: kural ogrenilirken duzeltilen
     * satirlar `userEdited = true` oldu, yani kullanicinin kendi karari.
     * "Kurali sil" demek "bundan sonrasini yeniden tahmin et" demektir,
     * "gecmiste verdigim kararlari geri al" demek degil.
     */
    suspend fun forgetRule(merchantKey: String) = rules.deleteByKey(merchantKey)

    /**
     * Tum yerel veriyi siler (ayarlar > F4).
     *
     * Tercihlere (dinlenen kaynaklar, onboarding bayragi) dokunulmuyor: onlar
     * veri degil ayar. Kurallar siliniyor, cunku her kural kullanicinin harcama
     * gecmisinden turedi - "verimi sil" dedikten sonra isyeri->kategori
     * eslesmelerinin durmasi, verinin gercekten silinmedigi anlamina gelirdi.
     */
    suspend fun eraseAll() {
        expenses.deleteAll()
        rules.deleteAll()
    }

    private suspend fun resolveCategory(merchant: String?): Category {
        val key = merchant?.merchantKey()
        if (key != null) {
            rules.find(key)?.let { return Category.from(it.category) }
        }
        return Categorizer.guess(merchant)
    }

    private fun String.merchantKey(): String = trim().lowercase(Locale.ROOT)

    private fun ExpenseEntity.signedAmount(): Long =
        Ledger.signedMinor(TxKind.valueOf(kind), amountMinor)
}
