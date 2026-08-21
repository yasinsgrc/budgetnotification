package com.bildirimbutce.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MoneyTest {

    @Test
    fun `turk formatindaki tutarlar kurusa cevrilir`() {
        assertEquals(24590L, Money.toMinor("245,90"))
        assertEquals(124900L, Money.toMinor("1.249,00"))
        assertEquals(1250000L, Money.toMinor("12.500"))
        assertEquals(4500L, Money.toMinor("45"))
        assertEquals(999L, Money.toMinor("9,99"))
        assertEquals(123456789L, Money.toMinor("1.234.567,89"))
    }

    @Test
    fun `gecersiz ve sifir tutarlar reddedilir`() {
        assertNull(Money.toMinor("abc"))
        assertNull(Money.toMinor("0"))
        assertNull(Money.toMinor("0,00"))
        assertNull(Money.toMinor(""))
    }

    @Test
    fun `bicimlendirme binlik ayraci kullanir`() {
        assertEquals("245,90", Money.format(24590))
        assertEquals("1.249,00", Money.format(124900))
        assertEquals("0,05", Money.format(5))
        assertEquals("1.234.567,89", Money.format(123456789))
    }
}
