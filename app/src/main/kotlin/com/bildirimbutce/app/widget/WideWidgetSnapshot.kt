package com.bildirimbutce.app.widget

import com.bildirimbutce.app.ui.HomeUiState
import com.bildirimbutce.app.ui.MonthCursor
import com.bildirimbutce.app.ui.theme.LIRA
import com.bildirimbutce.parser.Category
import com.bildirimbutce.parser.Money

/**
 * 4x2 widget'in kac kategori satiri cizdigi.
 *
 * Uc, tasarimdan geliyor ama keyfi degil: 4x2 hucrenin yuksekligi baslik, tutar
 * ve uc satirdan sonra bitiyor. Dorduncu satir eklenseydi bazi launcher'larda
 * sessizce kirpilir, kullanici listenin bittigini sanirdi.
 */
internal const val WIDE_WIDGET_ROW_COUNT = 3

/** Kategori satiri: nokta rengi kategoriden, iki metin de hazir bicimlenmis. */
internal data class WidgetCategoryRow(
    val category: Category,
    val label: String,
    val amount: String
)

/**
 * 4x2 widget'in cizecegi her sey - hazir dizgiler halinde.
 *
 * `RemoteViews` kurulmadan once hesaplanmasinin sebebi test edilebilirlik:
 * bicimleme ve susma kurallari Android baglami olmadan dogrulanabiliyor, geriye
 * kalan yalnizca cizim.
 */
internal data class WideWidgetSnapshot(
    /** "AĞUSTOS 2026" */
    val kicker: String,
    /**
     * "1.821,80 ₺"
     *
     * Simge dizgede duruyor ama Grotesk'e cizdirilmiyor: `RemoteViews`'a
     * yazilirken sistem fontu span'i ekleniyor (bkz. ui/theme/Lira.kt).
     */
    val amount: String,
    /** "↓ %12" - rozet susuyorsa null. */
    val change: String?,
    /** Rozetin yonu; [change] null iken anlamsiz. */
    val changeIncreased: Boolean,
    /** En cok harcanan [WIDE_WIDGET_ROW_COUNT] kategori; bos ay icin bos liste. */
    val rows: List<WidgetCategoryRow>
)

/**
 * Ekran durumunu 4x2 widget'in satirlarina cevirir.
 *
 * Yeni bir sayi uretmiyor: toplam, kategori kirilimi ve degisim rozeti ana
 * ekranin kullandigi `toUiState`'ten geliyor. Buradaki tek is bicimlemek ve
 * listeyi widget'a sigacak kadar kismak.
 *
 * Rozetin **ne zaman susacagina** burada karar verilmiyor - [HomeUiState.change]
 * zaten null geliyorsa (onceki ay net sifir/negatif, acik ay iadeyle negatife
 * dustu, ya da fark yuzde yarimin altinda) rozet cizilmiyor. Kural iki yerde
 * olsaydi widget ile ana ekran farkli aylarda farkli seyler soylerdi.
 */
internal fun HomeUiState.wideSnapshot(cursor: MonthCursor): WideWidgetSnapshot =
    WideWidgetSnapshot(
        kicker = "${cursor.upperLabel} ${cursor.year}",
        amount = "${Money.format(totalMinor)} $LIRA",
        change = change?.let { "${if (it.increased) "↑" else "↓"} %${it.percent}" },
        changeIncreased = change?.increased ?: false,
        rows = byCategory.take(WIDE_WIDGET_ROW_COUNT).map { (category, amountMinor) ->
            WidgetCategoryRow(
                category = category,
                label = category.label,
                amount = Money.format(amountMinor)
            )
        }
    )
