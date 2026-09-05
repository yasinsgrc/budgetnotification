package com.bildirimbutce.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Kaynak secimi kurali (ayarlar > F2).
 *
 * Saf JVM: kural Android'e dokunmuyor, cunku ayni cevabi hem ekranin hem
 * servisin vermesi gerekiyor. Uc durum var ve ucu de birbirinden farkli:
 * `null` (hic secim yok), bos kume (hepsi kapali), dolu kume (secilenler).
 */
class SourceSelectionTest {

    private val sources = listOf(
        NotificationSource("com.bank.a", "A Bank"),
        NotificationSource("com.bank.b", "B Bank"),
        NotificationSource("com.bank.c", "C Bank")
    )

    @Test
    fun `secim yapilmamissa hepsi dinleniyor`() {
        assertTrue(SourceSelection.listensTo(null, "com.bank.a"))
        assertEquals(
            sources.map { it.packageName }.toSet(),
            SourceSelection.enabled(sources, null)
        )
    }

    /**
     * "Bos kume = hepsi" kisayoluna dusulmesin diye ayri test: o kural gecerli
     * olsaydi son anahtari kapatan kullanici, farkinda olmadan tum bankalari
     * geri acmis olurdu.
     */
    @Test
    fun `bos kume hicbirini dinlemiyor`() {
        assertFalse(SourceSelection.listensTo(emptySet(), "com.bank.a"))
        assertTrue(SourceSelection.enabled(sources, emptySet()).isEmpty())
    }

    @Test
    fun `yalnizca secili paket dinleniyor`() {
        val stored = setOf("com.bank.b")

        assertTrue(SourceSelection.listensTo(stored, "com.bank.b"))
        assertFalse(SourceSelection.listensTo(stored, "com.bank.a"))
    }

    @Test
    fun `hepsi acikken bir tanesi kapatilinca kalanlar saklaniyor`() {
        val next = SourceSelection.toggled(sources, null, "com.bank.b", enable = false)

        assertEquals(setOf("com.bank.a", "com.bank.c"), next)
    }

    /**
     * Hepsi tekrar acildiginda tercih siliniyor ("hepsi" = null).
     *
     * Tam liste saklansaydi desen setine yarin eklenecek banka kapali dogar,
     * kullanici hic kapatmadigi bir kaynagin harcamalarini kacirirdi.
     */
    @Test
    fun `son kapali kaynak da acilinca tercih siliniyor`() {
        val partial = setOf("com.bank.a", "com.bank.c")

        assertNull(SourceSelection.toggled(sources, partial, "com.bank.b", enable = true))
    }

    @Test
    fun `hepsi kapatilinca bos kume saklaniyor`() {
        var stored = SourceSelection.toggled(sources, null, "com.bank.a", enable = false)
        stored = SourceSelection.toggled(sources, stored, "com.bank.b", enable = false)
        stored = SourceSelection.toggled(sources, stored, "com.bank.c", enable = false)

        assertEquals(emptySet<String>(), stored)
        assertFalse(SourceSelection.listensTo(stored, "com.bank.a"))
    }

    /**
     * Desen setine sonradan eklenen banka: kullanici hic secim yapmadiysa
     * kendiliginden dinlenmeli. Bunun tasiyicisi `null`.
     */
    @Test
    fun `secim yapilmamis kullanici icin yeni kaynak da dinleniyor`() {
        assertTrue(SourceSelection.listensTo(null, "com.bank.yeni"))
    }
}
