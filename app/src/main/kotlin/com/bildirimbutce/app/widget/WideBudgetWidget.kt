package com.bildirimbutce.app.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.view.View
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import com.bildirimbutce.app.R
import com.bildirimbutce.app.data.StoredProAccess
import com.bildirimbutce.app.ui.nav.Route
import com.bildirimbutce.app.ui.theme.liraSpanned
import com.bildirimbutce.app.util.Prefs
import kotlinx.coroutines.launch

/**
 * 4x2 Pro widget: ay toplami, degisim rozeti ve kategori kirilimi.
 *
 * 2x1 ([BudgetWidget]) "ne kadar harcadim" sorusunu cevapliyor; bu widget
 * "nereye gitti"yi de ekliyor. Ucretsiz surumde kilitli - yol haritasi 12. madde
 * ve tasarimin E1 listesi ikisi de bunu Pro ozelligi sayiyor.
 *
 * **Kilit sessiz degil.** Yetki yoksa widget ana ekrandan kaybolmuyor ya da bos
 * durmuyor; ne oldugunu yaziyor ve dokunus paywall'a gidiyor. Provider'i
 * `PackageManager` ile kapatmak da dusunuldu: o zaman kullanici widget'i
 * seciciden hic goremez, dolayisiyla Pro'nun ne actigini da ogrenemezdi.
 */
class WideBudgetWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        render(context, appWidgetManager, appWidgetIds)
    }

    companion object {

        /** Ana ekranda duran ornekleri yeniden cizer; bkz. [BudgetWidgets.refreshAll]. */
        internal fun renderAll(context: Context, manager: AppWidgetManager) {
            val ids = BudgetWidgets.idsOf(context, manager, WideBudgetWidget::class.java)
            if (ids.isNotEmpty()) render(context, manager, ids)
        }

        private fun render(context: Context, manager: AppWidgetManager, ids: IntArray) {
            // Yetki bayrak olarak degil arayuz olarak okunuyor: satin alma yolu
            // baglandiginda degisecek olan `StoredProAccess`, burasi degil.
            if (!StoredProAccess(Prefs(context)).isPro.value) {
                val locked = lockedViews(context)
                ids.forEach { manager.updateAppWidget(it, locked) }
                return
            }
            BudgetWidgets.scope.launch {
                val (cursor, state) = BudgetWidgets.currentMonth(context)
                val views = dataViews(context, state.wideSnapshot(cursor))
                ids.forEach { manager.updateAppWidget(it, views) }
            }
        }

        /**
         * Kilitli hal. Uygulama acilmiyor, dogrudan paywall aciliyor: "Pro'da"
         * yazip kullaniciyi ana ekrana birakmak, 9. maddede kaldirilan olu
         * tiklamalarin uzun yoldan yapilmis hali olurdu.
         */
        private fun lockedViews(context: Context) =
            RemoteViews(context.packageName, R.layout.widget_budget_wide_locked).apply {
                setOnClickPendingIntent(
                    R.id.widget_wide_locked_root,
                    BudgetWidgets.openApp(context, Route.PAYWALL)
                )
            }

        private fun dataViews(context: Context, snapshot: WideWidgetSnapshot) =
            RemoteViews(context.packageName, R.layout.widget_budget_wide).apply {
                setTextViewText(R.id.widget_wide_kicker, snapshot.kicker)
                // ₺ Grotesk'te yanlis ciziliyor; simge sistem fontuna dusuyor
                // (bkz. ui/theme/Lira.kt).
                setTextViewText(R.id.widget_wide_amount, liraSpanned(snapshot.amount))
                bindChange(context, snapshot)
                bindRows(snapshot)
                setOnClickPendingIntent(R.id.widget_wide_root, BudgetWidgets.openApp(context))
            }

        /**
         * Degisim rozeti. Rengi XML'de sabitlenemiyor cunku yone gore degisiyor;
         * bu yuzden uygulama baglamindan cozuluyor. Gece/gunduz ayrimi cihaz
         * genelinde bir yapilandirma oldugu icin host ile ayni degeri veriyor.
         */
        private fun RemoteViews.bindChange(context: Context, snapshot: WideWidgetSnapshot) {
            val text = snapshot.change
            if (text == null) {
                setViewVisibility(R.id.widget_wide_change, View.GONE)
                return
            }
            setViewVisibility(R.id.widget_wide_change, View.VISIBLE)
            setTextViewText(R.id.widget_wide_change, text)
            val color = if (snapshot.changeIncreased) R.color.widget_warning else R.color.widget_positive
            setTextColor(R.id.widget_wide_change, ContextCompat.getColor(context, color))
        }

        /**
         * Kategori satirlari. Satir sayisi sabit ([WIDE_WIDGET_ROW_COUNT]) cunku
         * `RemoteViews` calisma aninda gorunum uretemez; kullanilmayanlar `GONE`
         * yapiliyor. Bos ay uc satiri da gizler, geriye "0,00 ₺" kalir.
         */
        private fun RemoteViews.bindRows(snapshot: WideWidgetSnapshot) {
            ROW_IDS.forEachIndexed { index, row ->
                val data = snapshot.rows.getOrNull(index)
                if (data == null) {
                    setViewVisibility(row.container, View.GONE)
                    return@forEachIndexed
                }
                setViewVisibility(row.container, View.VISIBLE)
                setTextViewText(row.label, data.label)
                setTextViewText(row.amount, data.amount)
                // ImageView.setColorFilter @RemotableViewMethod; API 31'deki
                // RemoteViews.setColorFilter minSdk 26'da yok, bu yuzden setInt.
                setInt(row.dot, "setColorFilter", BudgetWidgets.categoryColor(data.category))
            }
        }

        private data class RowIds(val container: Int, val dot: Int, val label: Int, val amount: Int)

        private val ROW_IDS = listOf(
            RowIds(R.id.widget_wide_row_1, R.id.widget_wide_dot_1, R.id.widget_wide_label_1, R.id.widget_wide_value_1),
            RowIds(R.id.widget_wide_row_2, R.id.widget_wide_dot_2, R.id.widget_wide_label_2, R.id.widget_wide_value_2),
            RowIds(R.id.widget_wide_row_3, R.id.widget_wide_dot_3, R.id.widget_wide_label_3, R.id.widget_wide_value_3)
        )
    }
}
