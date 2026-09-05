package com.bildirimbutce.app.ui

import com.bildirimbutce.parser.Category
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Elle giris formunun kurallari.
 *
 * Form saf tutuluyor (Android baglami yok) ki asil riskli kisim - hangi tutarin
 * kaydedilebilir sayildigi ve nasil okundugu - emulatorsuz test edilebilsin.
 * Yanlis okunan bir tutar, kullanicinin ayin toplamina bir daha guvenmemesi
 * demek.
 */
class ManualEntryDraftTest {

    @Test
    fun `bos form kaydedilemez`() {
        val draft = ManualEntryDraft()

        assertFalse(draft.canSave)
        assertNull(draft.amountMinor)
    }

    @Test
    fun `sifir tutar kaydedilemez`() {
        assertFalse("sifir harcama diye bir sey yok", ManualEntryDraft(amountText = "0").canSave)
        assertFalse(ManualEntryDraft(amountText = "0,00").canSave)
    }

    @Test
    fun `turkce bicimli tutar kurusa cevriliyor`() {
        assertEquals(8_990L, ManualEntryDraft(amountText = "89,90").amountMinor)
        assertEquals(124_900L, ManualEntryDraft(amountText = "1.249,00").amountMinor)
        assertEquals(4_500L, ManualEntryDraft(amountText = "45").amountMinor)
    }

    @Test
    fun `gecerli tutar kaydedilebilir`() {
        assertTrue(ManualEntryDraft(amountText = "89,90").canSave)
    }

    @Test
    fun `isyeri zorunlu degil`() {
        assertTrue(ManualEntryDraft(amountText = "89,90", merchant = "").canSave)
    }

    @Test
    fun `kategori secilmediginde isyeri adindan tahmin ediliyor`() {
        val draft = ManualEntryDraft(merchant = "Migros Akatlar")

        assertEquals(Category.MARKET, draft.effectiveCategory)
        assertNull("tahmin, kullanicinin secimi sayilmamali", draft.category)
    }

    @Test
    fun `kullanicinin secimi tahmini eziyor`() {
        val draft = ManualEntryDraft(merchant = "Migros Akatlar", category = Category.EGLENCE)

        assertEquals(Category.EGLENCE, draft.effectiveCategory)
    }

    @Test
    fun `isyeri bosken tahmin DIGER`() {
        assertEquals(Category.DIGER, ManualEntryDraft(merchant = "   ").effectiveCategory)
    }

    @Test
    fun `tutar alani harf kabul etmiyor`() {
        assertEquals("8990", "8a9b9c0".asAmountInput())
    }

    /**
     * Klavyeye gore nokta da gelebiliyor. Nokta virgule cevrilmezse
     * [com.bildirimbutce.parser.Money.toMinor] "89.90"i binlik ayraci sanip
     * 8.990,00 TL yazardi - 100 kat hata.
     */
    @Test
    fun `nokta ondalik ayracina cevriliyor`() {
        val text = "89.90".asAmountInput()

        assertEquals("89,90", text)
        assertEquals(8_990L, ManualEntryDraft(amountText = text).amountMinor)
    }

    @Test
    fun `ikinci ondalik ayraci yok sayiliyor`() {
        assertEquals("89,90", "89,9,0".asAmountInput())
    }

    @Test
    fun `ikiden fazla kurus hanesi yok sayiliyor`() {
        assertEquals("89,90", "89,9012".asAmountInput())
    }

    @Test
    fun `bastaki ondalik ayraci yok sayiliyor`() {
        assertEquals("", ",".asAmountInput())
        assertEquals("90", ",90".asAmountInput())
    }
}
