package com.bildirimbutce.app.ui.settings

import com.bildirimbutce.app.data.ExpenseRepository
import com.bildirimbutce.app.data.PatternProvider
import com.bildirimbutce.app.data.SourceSelection
import com.bildirimbutce.app.data.db.AppDatabase
import com.bildirimbutce.app.expenseEntity
import com.bildirimbutce.app.inMemoryDb
import com.bildirimbutce.app.testContext
import com.bildirimbutce.app.util.Prefs
import com.bildirimbutce.parser.Category
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Ayarlar ekranlarinin dayandigi veri yollari (F2-F4).
 *
 * Ekranlarin kendisi (Compose) test edilmiyor - `compose-ui-test` bagimliligi
 * projede yok. Burada test edilen, ekranlarin ustune kuruldugu davranis:
 * kaynak listesinin desen setinden gelmesi, tercihin diskte kalici olmasi,
 * kural silmenin gecmise dokunmamasi ve "tum veriyi sil"in gercekten silmesi.
 */
@RunWith(RobolectricTestRunner::class)
class SettingsDataTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: ExpenseRepository

    @Before
    fun setUp() {
        PatternProvider.invalidate()
        db = inMemoryDb()
        repository = ExpenseRepository(db)
    }

    @After
    fun tearDown() {
        db.close()
        PatternProvider.invalidate()
    }

    /**
     * Kaynak listesi ile ayristiricinin dinledigi kume ayni dosyadan gelmeli;
     * ayrilsalardi kullanici ekranda hic gormedigi bir kaynagi kapatamazdi.
     */
    @Test
    fun `kaynak listesi desen setinden geliyor ve adlandirilmis`() {
        val sources = PatternProvider.sources(testContext())

        assertEquals(PatternProvider.sourceCount(testContext()), sources.size)
        assertTrue("liste bos gelirse ayarlar ekrani hicbir sey gostermez", sources.isNotEmpty())
        assertTrue(
            "paket adiyla ayni kalan etiket, sourceLabels'ta karsiligi olmadigini gosterir",
            sources.all { it.label.isNotBlank() && it.label != it.packageName }
        )
    }

    /** Tercih diske yazilmali: uygulama kapaninca secim unutulursa anahtar yalan soyler. */
    @Test
    fun `kapatilan kaynak yeni bir Prefs orneginde de kapali`() {
        val sources = PatternProvider.sources(testContext())
        val target = sources.first().packageName

        val next = SourceSelection.toggled(sources, Prefs(testContext()).enabledSources, target, false)
        Prefs(testContext()).enabledSources = next

        val stored = Prefs(testContext()).enabledSources
        assertFalse(SourceSelection.listensTo(stored, target))
        assertTrue(SourceSelection.listensTo(stored, sources[1].packageName))
    }

    @Test
    fun `varsayilan tercih bos kume degil null - hic secim yapilmadi demek`() {
        assertNull(Prefs(testContext()).enabledSources)
    }

    @Test
    fun `duzeltme kural uretiyor ve liste bunu gosteriyor`() = runBlocking {
        db.expenseDao().insert(expenseEntity(merchant = "MIGROS"))
        val row = db.expenseDao().getBetween(0L, Long.MAX_VALUE).single()

        repository.correctCategory(row, Category.YEME_ICME)

        val rules = repository.observeMerchantRules().first()
        assertEquals(1, rules.size)
        assertEquals("migros", rules.single().merchantKey)
        assertEquals(Category.YEME_ICME.name, rules.single().category)
    }

    /**
     * Kural silmek "bundan sonra yeniden tahmin et" demek; kullanicinin daha
     * once elle verdigi kararlari geri almak degil.
     */
    @Test
    fun `kural silinince gecmis kayit yerinde kaliyor`() = runBlocking {
        db.expenseDao().insert(expenseEntity(merchant = "MIGROS"))
        val row = db.expenseDao().getBetween(0L, Long.MAX_VALUE).single()
        repository.correctCategory(row, Category.YEME_ICME)

        repository.forgetRule("migros")

        assertTrue(repository.observeMerchantRules().first().isEmpty())
        val after = db.expenseDao().getBetween(0L, Long.MAX_VALUE).single()
        assertEquals(Category.YEME_ICME.name, after.category)
        assertTrue("kullanicinin karari korunmali", after.userEdited)
    }

    @Test
    fun `tum veriyi sil hem kayitlari hem kurallari siliyor`() = runBlocking {
        db.expenseDao().insert(expenseEntity(merchant = "MIGROS", occurredAt = 1_000L))
        db.expenseDao().insert(expenseEntity(merchant = "SHELL", occurredAt = 2_000L))
        val row = db.expenseDao().getBetween(0L, Long.MAX_VALUE).first()
        repository.correctCategory(row, Category.YEME_ICME)
        assertEquals(2, repository.observeExpenseCount().first())

        repository.eraseAll()

        assertEquals(0, repository.observeExpenseCount().first())
        assertTrue(repository.observeMerchantRules().first().isEmpty())
    }

    /** Silme veriye dokunuyor, ayara degil: kaynak tercihi ve onboarding bayragi kalmali. */
    @Test
    fun `tum veriyi sil tercihlere dokunmuyor`() = runBlocking {
        val prefs = Prefs(testContext())
        prefs.onboardingDone = true
        prefs.enabledSources = setOf("com.bank.a")

        repository.eraseAll()

        assertTrue(Prefs(testContext()).onboardingDone)
        assertEquals(setOf("com.bank.a"), Prefs(testContext()).enabledSources)
    }
}
