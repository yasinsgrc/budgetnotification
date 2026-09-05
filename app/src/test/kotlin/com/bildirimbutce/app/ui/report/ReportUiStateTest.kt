package com.bildirimbutce.app.ui.report

import com.bildirimbutce.app.data.db.ExpenseEntity
import com.bildirimbutce.app.expenseEntity
import com.bildirimbutce.app.millisAt
import com.bildirimbutce.app.ui.MonthCursor
import com.bildirimbutce.parser.Category
import com.bildirimbutce.parser.TxKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * C1 raporunun aritmetigi.
 *
 * Rapor kullaniciya yalnizca liste degil, bir yorum satiyor ("cuma gunleri
 * fazla harciyorsun"). Yanlis bir bolme ya da kacan bir iade, kullaniciya
 * olmayan bir aliskanlik ogretir - bu yuzden her sayi ayri ayri sabitlendi.
 *
 * Android baglami gerekmiyor: [toReportState] saf bir donusum.
 *
 * Takvim sabitleri: 2026 Agustos 31 gun ceker, 1 Agustos 2026 cumartesidir,
 * dolayisiyla 7 Agustos 2026 cumadir.
 */
class ReportUiStateTest {

    private val agustos = MonthCursor(2026, 7)

    /** Ay kapandiktan sonraki bir an: gun ortalamasi ayin tamamina bolunmeli. */
    private val ayKapandiktanSonra = millisAt(2026, 8, 15)

    private fun harcama(
        gun: Int,
        tutar: Long,
        merchant: String? = "MIGROS",
        kind: TxKind = TxKind.EXPENSE,
        category: Category = Category.MARKET,
        ay: Int = 7,
        yil: Int = 2026
    ): ExpenseEntity = expenseEntity(
        merchant = merchant,
        amountMinor = tutar,
        kind = kind,
        category = category,
        occurredAt = millisAt(yil, ay, gun)
    )

    // --- Ay cubuklari ---

    @Test
    fun `bos ay penceresi alti sifir cubukla doluyor`() {
        val state = emptyList<ExpenseEntity>().toReportState(agustos, ayKapandiktanSonra)

        assertEquals(REPORT_MONTH_COUNT, state.months.size)
        assertTrue(state.months.all { it.totalMinor == 0L })
        assertTrue(state.isEmpty)
    }

    @Test
    fun `pencere secili ayda bitiyor ve geriye dogru siralaniyor`() {
        val state = emptyList<ExpenseEntity>().toReportState(agustos, ayKapandiktanSonra)

        assertEquals(
            listOf(
                MonthCursor(2026, 2), MonthCursor(2026, 3), MonthCursor(2026, 4),
                MonthCursor(2026, 5), MonthCursor(2026, 6), agustos
            ),
            state.months.map { it.cursor }
        )
        assertEquals(listOf(agustos), state.months.filter { it.isSelected }.map { it.cursor })
    }

    /**
     * Pencere yil sinirini asabilir. `previous()` zinciri yerine 12 tabanli
     * hesap kondu; ocakta bir onceki yila dusmezse rapor bos cikardi.
     */
    @Test
    fun `pencere yil sinirini asiyor`() {
        val ocak = MonthCursor(2026, 0)

        val state = emptyList<ExpenseEntity>().toReportState(ocak, millisAt(2026, 0, 20))

        assertEquals(MonthCursor(2025, 7), state.months.first().cursor)
        assertEquals(ocak, state.months.last().cursor)
    }

    @Test
    fun `cubuklar kendi aylarina yaziliyor`() {
        val state = listOf(
            harcama(gun = 3, tutar = 10_000, ay = 5),
            harcama(gun = 4, tutar = 25_000, ay = 7)
        ).toReportState(agustos, ayKapandiktanSonra)

        assertEquals(10_000L, state.months.first { it.cursor == MonthCursor(2026, 5) }.totalMinor)
        assertEquals(25_000L, state.months.first { it.cursor == agustos }.totalMinor)
    }

    /**
     * Sorgu pencere kadar veri getiriyor ama donusum de kendini korumali:
     * pencere disindan bir kayit sizarsa hicbir cubuga yazilmamali.
     */
    @Test
    fun `pencere disindaki kayit hicbir cubuga yazilmiyor`() {
        val state = listOf(harcama(gun = 3, tutar = 10_000, ay = 1))
            .toReportState(agustos, ayKapandiktanSonra)

        assertTrue(state.months.all { it.totalMinor == 0L })
        assertTrue(state.isEmpty)
    }

    @Test
    fun `iade ay toplamindan dusuluyor`() {
        val state = listOf(
            harcama(gun = 3, tutar = 30_000),
            harcama(gun = 4, tutar = 5_000, kind = TxKind.REFUND)
        ).toReportState(agustos, ayKapandiktanSonra)

        assertEquals(25_000L, state.totalMinor)
        assertEquals(25_000L, state.months.last().totalMinor)
    }

    // --- Gun ortalamasi ---

    @Test
    fun `kapanmis ayda gun ortalamasi ayin tamamina bolunuyor`() {
        val state = listOf(harcama(gun = 3, tutar = 310_000))
            .toReportState(agustos, ayKapandiktanSonra)

        assertEquals(31, state.daysCounted)
        assertEquals(10_000L, state.dailyAverageMinor)
    }

    /**
     * Ayin 10'unda ayin tamamina bolseydik kullanici ortalamasini ucte biri
     * kadar gorur ve "iyi gidiyorum" sanirdi.
     */
    @Test
    fun `icinde bulunulan ayda gun ortalamasi gecen gune bolunuyor`() {
        val state = listOf(harcama(gun = 3, tutar = 310_000))
            .toReportState(agustos, millisAt(2026, 7, 10))

        assertEquals(10, state.daysCounted)
        assertEquals(31_000L, state.dailyAverageMinor)
    }

    @Test
    fun `ay iadeyle negatife duserse ortalama gosterilmiyor`() {
        val state = listOf(
            harcama(gun = 3, tutar = 5_000),
            harcama(gun = 4, tutar = 30_000, kind = TxKind.REFUND)
        ).toReportState(agustos, ayKapandiktanSonra)

        assertEquals(-25_000L, state.totalMinor)
        assertEquals(0L, state.dailyAverageMinor)
    }

    // --- En yuksek gun ve islem sayilari ---

    @Test
    fun `en yuksek gun ayni gunun kayitlarini topluyor`() {
        val state = listOf(
            harcama(gun = 3, tutar = 40_000),
            harcama(gun = 18, tutar = 30_000),
            harcama(gun = 18, tutar = 25_000)
        ).toReportState(agustos, ayKapandiktanSonra)

        assertEquals(18, state.peakDay?.dayOfMonth)
        assertEquals(55_000L, state.peakDay?.totalMinor)
    }

    @Test
    fun `neti negatif olan gun en yuksek gun olamaz`() {
        val state = listOf(
            harcama(gun = 18, tutar = 10_000),
            harcama(gun = 18, tutar = 90_000, kind = TxKind.REFUND)
        ).toReportState(agustos, ayKapandiktanSonra)

        assertNull(state.peakDay)
    }

    @Test
    fun `islem sayisi harcama ve iadeyi ayiriyor`() {
        val state = listOf(
            harcama(gun = 3, tutar = 10_000),
            harcama(gun = 4, tutar = 20_000),
            harcama(gun = 5, tutar = 5_000, kind = TxKind.REFUND)
        ).toReportState(agustos, ayKapandiktanSonra)

        assertEquals(2, state.expenseCount)
        assertEquals(1, state.refundCount)
        assertFalse(state.isEmpty)
    }

    // --- Haftanin ritmi ---

    /** 7 Agustos 2026 cuma; pazartesi ilk sayildigi icin indeks 4 olmali. */
    @Test
    fun `kayit haftanin dogru gunune yaziliyor`() {
        val state = listOf(harcama(gun = 7, tutar = 50_000))
            .toReportState(agustos, ayKapandiktanSonra)

        assertEquals(listOf("Pt", "Sa", "Ça", "Pe", "Cu", "Ct", "Pz"), state.weekdays.map { it.label })
        assertEquals(50_000L, state.weekdays[4].totalMinor)
        assertEquals(listOf("Cu"), state.weekdays.filter { it.isPeak }.map { it.label })
    }

    @Test
    fun `tepe gun cumlesi gunu adiyla soyluyor`() {
        val state = listOf(
            harcama(gun = 7, tutar = 50_000),
            harcama(gun = 14, tutar = 50_000)
        ).toReportState(agustos, ayKapandiktanSonra)

        assertEquals("Cuma", state.weekdayPeak?.label)
        // Tek gune yiginca ortalama toplamin yedide biri olur: %600 ustunde.
        assertEquals(600, state.weekdayPeak?.percentAboveAverage)
    }

    /**
     * Harcama gunlere esit dagilirsa "su gun ayrisiyor" demek uydurma olurdu;
     * cumle hic yazilmamali.
     */
    @Test
    fun `esik altinda tepe gun cumlesi yazilmiyor`() {
        val state = (3..9).map { harcama(gun = it, tutar = 10_000) }
            .toReportState(agustos, ayKapandiktanSonra)

        assertNull(state.weekdayPeak)
    }

    @Test
    fun `bos ayda tepe gun cumlesi yok`() {
        val state = emptyList<ExpenseEntity>().toReportState(agustos, ayKapandiktanSonra)

        assertNull(state.weekdayPeak)
        assertTrue(state.weekdays.none { it.isPeak })
    }

    // --- En cok giden yerler ---

    @Test
    fun `en cok giden yerler buyukten kucuge siralaniyor`() {
        val state = listOf(
            harcama(gun = 3, tutar = 24_590, merchant = "Migros"),
            harcama(gun = 4, tutar = 78_000, merchant = "Shell", category = Category.ULASIM),
            harcama(gun = 5, tutar = 30_200, merchant = "Turkcell", category = Category.FATURA)
        ).toReportState(agustos, ayKapandiktanSonra)

        assertEquals(listOf("Shell", "Turkcell", "Migros"), state.topMerchants.map { it.name })
    }

    @Test
    fun `ayni isyerinin kayitlari tek satirda toplaniyor`() {
        val state = listOf(
            harcama(gun = 3, tutar = 20_000, merchant = "Migros"),
            harcama(gun = 9, tutar = 15_000, merchant = "Migros")
        ).toReportState(agustos, ayKapandiktanSonra)

        val migros = state.topMerchants.single()
        assertEquals(2, migros.count)
        assertEquals(35_000L, migros.totalMinor)
    }

    /**
     * "Bilinmeyen isyeri" bir yer degil, ayristiricinin okuyamadigi bir satir.
     * Listenin basina cikarsa kullaniciya gitmedigi bir yeri gosterirdi.
     */
    @Test
    fun `isyeri adi olmayan kayit listeye girmiyor`() {
        val state = listOf(
            harcama(gun = 3, tutar = 90_000, merchant = null),
            harcama(gun = 4, tutar = 10_000, merchant = "Migros")
        ).toReportState(agustos, ayKapandiktanSonra)

        assertEquals(listOf("Migros"), state.topMerchants.map { it.name })
    }

    @Test
    fun `iade isyeri toplamindan dusuluyor ve neti negatif yer listeye girmiyor`() {
        val state = listOf(
            harcama(gun = 3, tutar = 10_000, merchant = "Zara", category = Category.ALISVERIS),
            harcama(
                gun = 9,
                tutar = 40_000,
                merchant = "Zara",
                kind = TxKind.REFUND,
                category = Category.ALISVERIS
            ),
            harcama(gun = 4, tutar = 20_000, merchant = "Migros")
        ).toReportState(agustos, ayKapandiktanSonra)

        assertEquals(listOf("Migros"), state.topMerchants.map { it.name })
    }

    /**
     * Kullanici bir isyerini duzeltince kural ogreniliyor ve sonraki kayitlar
     * yeni kategoriye dusuyor. Listede eski kategori yazsaydi rapor,
     * kullanicinin kendi duzeltmesini yok saymis gorunurdu.
     */
    @Test
    fun `isyeri kategorisi en yeni kayittan aliniyor`() {
        val state = listOf(
            harcama(gun = 3, tutar = 10_000, merchant = "Shell", category = Category.DIGER),
            harcama(gun = 20, tutar = 10_000, merchant = "Shell", category = Category.ULASIM)
        ).toReportState(agustos, ayKapandiktanSonra)

        assertEquals(Category.ULASIM, state.topMerchants.single().category)
    }

    @Test
    fun `liste bes satirla sinirli`() {
        val state = (1..8).map { harcama(gun = it, tutar = it * 10_000L, merchant = "Yer $it") }
            .toReportState(agustos, ayKapandiktanSonra)

        assertEquals(5, state.topMerchants.size)
        assertEquals("Yer 8", state.topMerchants.first().name)
    }

    @Test
    fun `en cok giden yerler yalnizca secili aydan hesaplaniyor`() {
        val state = listOf(
            harcama(gun = 3, tutar = 90_000, merchant = "Önceki Ay", ay = 6),
            harcama(gun = 4, tutar = 10_000, merchant = "Migros", ay = 7)
        ).toReportState(agustos, ayKapandiktanSonra)

        assertEquals(listOf("Migros"), state.topMerchants.map { it.name })
    }
}
