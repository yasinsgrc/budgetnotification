package com.bildirimbutce.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MerchantCleanerTest {

    /** patterns.json'daki brandTokens listesinin test karsiligi. */
    private val brands = setOf("BIM", "IKEA", "LC", "AB", "AS", "A.S", "COM")

    @Test
    fun `zincirlenmis on ekler temizlenir`() {
        // Tek gecislik temizlik burada "Ile Migros" birakirdi - regresyon testi
        assertEquals("Migros Ticaret A.S", MerchantCleaner.clean("1234 kartiniz ile MIGROS TICARET A.S."))
        assertEquals("Trendyol", MerchantCleaner.clean("Sayın müşterimiz, kartınızla TRENDYOL"))
    }

    @Test
    fun `turkce locale noktasiz i uretmez`() {
        // "MIGROS".lowercase(tr) = "mıgros" olurdu - marka adi bozulur
        assertEquals("Migros Ticaret", MerchantCleaner.clean("MIGROS TICARET"))
        assertEquals("Magazacilik", MerchantCleaner.clean("MAGAZACILIK"))
    }

    @Test
    fun `brandTokens disindaki kisa kelimeler kucultulur`() {
        // Onceki "4 harf ve buyuk harf" kurali bunlari bozuyordu
        assertEquals("Sok Marketler", MerchantCleaner.clean("SOK MARKETLER", brands))
        assertEquals("Burger King", MerchantCleaner.clean("BURGER KING", brands))
        assertEquals("A101 Yeni Magazacilik", MerchantCleaner.clean("A101 YENI MAGAZACILIK", brands))
    }

    @Test
    fun `kisa buyuk harfli markalar bozulmaz`() {
        assertEquals("BIM", MerchantCleaner.clean("BIM", brands))
        assertEquals("A101", MerchantCleaner.clean("A101", brands))
        assertEquals("Migros", MerchantCleaner.clean("MIGROS"))
    }

    @Test
    fun `fiil kaymalari magaza sayilmaz`() {
        assertNull(MerchantCleaner.clean("yapılmıştır"))
        assertNull(MerchantCleaner.clean("gerçekleşti"))
        assertNull(MerchantCleaner.clean("--"))
        assertNull(MerchantCleaner.clean(""))
    }
}
