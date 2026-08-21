package com.bildirimbutce.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.bildirimbutce.app.MainActivity
import com.bildirimbutce.app.R
import com.bildirimbutce.app.data.ExpenseRepository
import com.bildirimbutce.parser.Money
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
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
                val total = ExpenseRepository(context)
                    .monthTotalMinor(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH))

                val open = PendingIntent.getActivity(
                    context, 0,
                    Intent(context, MainActivity::class.java),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )

                val views = RemoteViews(context.packageName, R.layout.widget_budget).apply {
                    setTextViewText(R.id.widget_amount, "${Money.format(total)} ₺")
                    setTextViewText(R.id.widget_label, MONTHS[cal.get(Calendar.MONTH)] + " harcaması")
                    setOnClickPendingIntent(R.id.widget_root, open)
                }
                ids.forEach { manager.updateAppWidget(it, views) }
            }
        }

        private val MONTHS = listOf(
            "Ocak", "Şubat", "Mart", "Nisan", "Mayıs", "Haziran",
            "Temmuz", "Ağustos", "Eylül", "Ekim", "Kasım", "Aralık"
        )
    }
}
