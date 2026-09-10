package com.bildirimbutce.app.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.widget.RemoteViews
import com.bildirimbutce.app.R
import com.bildirimbutce.app.ui.theme.LIRA
import com.bildirimbutce.app.ui.theme.liraSpanned
import com.bildirimbutce.parser.Category
import com.bildirimbutce.parser.Money
import kotlinx.coroutines.launch

/**
 * Ana ekran widget'i: bu ayki toplam harcama.
 *
 * Uygulamanin indirilme sebebinin yarisi bu - web sitesi ana ekrana
 * canli rakam koyamaz.
 *
 * Kategori kirilimini gosteren 4x2 surumu icin bkz. [WideBudgetWidget].
 */
class BudgetWidget : AppWidgetProvider() {

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
            val ids = BudgetWidgets.idsOf(context, manager, BudgetWidget::class.java)
            if (ids.isNotEmpty()) render(context, manager, ids)
        }

        private fun render(context: Context, manager: AppWidgetManager, ids: IntArray) {
            BudgetWidgets.scope.launch {
                val (cursor, state) = BudgetWidgets.currentMonth(context)

                val views = RemoteViews(context.packageName, R.layout.widget_budget).apply {
                    setTextViewText(R.id.widget_kicker, "${cursor.upperLabel} HARCAMASI")
                    setTextViewText(
                        R.id.widget_amount,
                        liraSpanned("${Money.format(state.totalMinor)} $LIRA")
                    )
                    if (state.byCategory.isNotEmpty()) {
                        setImageViewBitmap(
                            R.id.widget_ribbon,
                            ribbonBitmap(state.byCategory, state.totalMinor)
                        )
                    }
                    setOnClickPendingIntent(R.id.widget_root, BudgetWidgets.openApp(context))
                }
                ids.forEach { manager.updateAppWidget(it, views) }
            }
        }

        /**
         * Kategori seridi RemoteViews'ta oransal genislikte cizilemez (weight
         * API'leri API 31+ gerektirir, minSdk 26). Bitmap olarak ciziliyor.
         */
        private fun ribbonBitmap(rows: List<Pair<Category, Long>>, total: Long): Bitmap {
            val width = 600
            val height = 20
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            var x = 0f
            rows.forEach { (category, amount) ->
                val segmentWidth = if (total > 0) (amount.toFloat() / total) * width else 0f
                canvas.drawRect(x, 0f, x + segmentWidth, height.toFloat(), paintFor(category))
                x += segmentWidth
            }
            return bitmap
        }

        private fun paintFor(category: Category) =
            Paint().apply { color = BudgetWidgets.categoryColor(category) }
    }
}
