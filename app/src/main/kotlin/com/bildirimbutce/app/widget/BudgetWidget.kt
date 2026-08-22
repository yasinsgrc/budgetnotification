package com.bildirimbutce.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.widget.RemoteViews
import com.bildirimbutce.app.MainActivity
import com.bildirimbutce.app.R
import com.bildirimbutce.app.data.ExpenseRepository
import com.bildirimbutce.app.data.db.ExpenseEntity
import com.bildirimbutce.parser.Category
import com.bildirimbutce.parser.Ledger
import com.bildirimbutce.parser.Money
import com.bildirimbutce.parser.TxKind
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * Ana ekran widget'i: bu ayki toplam harcama.
 *
 * Uygulamanin indirilme sebebinin yarisi bu - web sitesi ana ekrana
 * canli rakam koyamaz.
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
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        /** Yeni bir harcama kaydedildiginde servis tarafindan cagrilir. */
        fun refresh(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, BudgetWidget::class.java))
            if (ids.isNotEmpty()) render(context, manager, ids)
        }

        private fun render(context: Context, manager: AppWidgetManager, ids: IntArray) {
            scope.launch {
                val cal = Calendar.getInstance()
                val year = cal.get(Calendar.YEAR)
                val month = cal.get(Calendar.MONTH)
                val expenses = ExpenseRepository(context).observeMonth(year, month).first()
                val total = expenses.sumOf { it.signedAmount() }
                val byCategory = expenses.byCategory()

                val open = PendingIntent.getActivity(
                    context, 0,
                    Intent(context, MainActivity::class.java),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )

                val views = RemoteViews(context.packageName, R.layout.widget_budget).apply {
                    setTextViewText(R.id.widget_kicker, MONTHS[month].uppercase() + " HARCAMASI")
                    setTextViewText(R.id.widget_amount, "${Money.format(total)} ₺")
                    if (byCategory.isNotEmpty()) {
                        setImageViewBitmap(R.id.widget_ribbon, ribbonBitmap(byCategory, total))
                    }
                    setOnClickPendingIntent(R.id.widget_root, open)
                }
                ids.forEach { manager.updateAppWidget(it, views) }
            }
        }

        private fun List<ExpenseEntity>.signedTotal(): Long = sumOf { it.signedAmount() }

        private fun ExpenseEntity.signedAmount(): Long = Ledger.signedMinor(TxKind.valueOf(kind), amountMinor)

        private fun List<ExpenseEntity>.byCategory(): List<Pair<Category, Long>> =
            groupBy { Category.from(it.category) }
                .map { (category, items) -> category to items.signedTotal() }
                .filter { it.second > 0 }
                .sortedByDescending { it.second }

        /**
         * Kategori seridi RemoteViews'ta oransal genislikte cizilemez (weight
         * API'leri API 31+ gerektirir, minSdk 26). Bitmap olarak ciziliyor.
         * Renkler Theme.kt -> AppColors.categoryColor ile birebir ayni olmali.
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

        private fun paintFor(category: Category) = android.graphics.Paint().apply {
            color = when (category) {
                Category.MARKET -> Color.parseColor("#2AE3AE")
                Category.YEME_ICME -> Color.parseColor("#FFB020")
                Category.ULASIM -> Color.parseColor("#C6F24E")
                Category.FATURA -> Color.parseColor("#4FC3F7")
                Category.ALISVERIS -> Color.parseColor("#FF6B5A")
                Category.SAGLIK -> Color.parseColor("#FF8FB1")
                Category.EGLENCE -> Color.parseColor("#8B7BFF")
                Category.DIGER -> Color.parseColor("#7A8794")
            }
        }

        private val MONTHS = listOf(
            "Ocak", "Şubat", "Mart", "Nisan", "Mayıs", "Haziran",
            "Temmuz", "Ağustos", "Eylül", "Ekim", "Kasım", "Aralık"
        )
    }
}
