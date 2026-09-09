package com.bildirimbutce.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.bildirimbutce.app.MainActivity
import com.bildirimbutce.app.data.ExpenseRepository
import com.bildirimbutce.app.ui.HOME_MONTH_COUNT
import com.bildirimbutce.app.ui.HomeUiState
import com.bildirimbutce.app.ui.MonthCursor
import com.bildirimbutce.app.ui.toUiState
import com.bildirimbutce.parser.Category
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first

/**
 * Iki widget'in ortak yolu: 2x1 [BudgetWidget] ve 4x2 [WideBudgetWidget].
 *
 * Tek giris noktasi olmasinin sebebi cagri yerleri: yeni harcama kaydedildiginde
 * (`NotificationService`), veri silindiginde (`SettingsViewModel`) ve Pro yetkisi
 * degistiginde (`ProViewModel`) **her iki** widget da bayatlar. Cagri yerleri tek
 * tek providerlari bilseydi, ucuncu bir widget eklendigi gun biri unutulur ve
 * kullanici ana ekraninda artik dogru olmayan bir rakam gormeye devam ederdi.
 */
object BudgetWidgets {

    /**
     * Widget cizimi bir Room sorgusu istiyor; `onUpdate` ana is parcaciginda
     * calisir. Kapsam tek providerin companion'inda degil burada duruyor - iki
     * provider ayni kapsami paylasiyor.
     */
    internal val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Ana ekrandaki tum widget'lari yeniden cizer. */
    fun refreshAll(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        BudgetWidget.renderAll(context, manager)
        WideBudgetWidget.renderAll(context, manager)
    }

    /** Bir provider'in ana ekranda duran ornekleri; hicbiri yoksa bos dizi. */
    internal fun idsOf(context: Context, manager: AppWidgetManager, provider: Class<*>): IntArray =
        manager.getAppWidgetIds(ComponentName(context, provider))

    /**
     * Widget'larin okudugu tek pencere: acik ay + karsilastirilacak onceki ay.
     *
     * Ekranin durumu yeniden hesaplanmiyor, **ayni fonksiyondan** okunuyor
     * ([toUiState]). Widget kendi toplamini ayri bir kuralla hesaplasaydi iade
     * (REFUND) isareti ya da kategori suzgeci iki yerde ayri ayri bakim
     * gerektirirdi; kullanici ana ekranda bir rakam, ev ekraninda baskasini
     * gorurdu.
     */
    internal suspend fun currentMonth(context: Context): Pair<MonthCursor, HomeUiState> {
        val now = System.currentTimeMillis()
        val cursor = MonthCursor.of(now)
        val rows = ExpenseRepository(context)
            .observeMonths(cursor.year, cursor.month, HOME_MONTH_COUNT)
            .first()
        return cursor to rows.toUiState(cursor, now)
    }

    /**
     * Widget'a dokununca uygulamayi acan niyet.
     *
     * [route] verilirse uygulama o hedefe gider (kilitli 4x2 widget paywall'a
     * gonderiyor). Istek kodunun rotaya gore ayrilmasi sart: `FLAG_UPDATE_CURRENT`
     * ayni istek kodundaki niyeti gunceller, iki widget ayni kodu paylassaydi
     * ikisi de en son yazilan hedefe giderdi.
     */
    internal fun openApp(context: Context, route: String? = null): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (route != null) putExtra(MainActivity.EXTRA_ROUTE, route)
        }
        return PendingIntent.getActivity(
            context,
            route?.hashCode() ?: 0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    /**
     * Kategori rengi. `AppColors.categoryColor` ile **birebir ayni** olmali:
     * RemoteViews Compose token'larina erisemedigi icin tablo burada ikinci kez
     * yaziliyor, sapmasi kullanicinin ayni kategoriyi iki ekranda iki renkte
     * gormesi demek.
     */
    internal fun categoryColor(category: Category): Int = when (category) {
        Category.MARKET -> 0xFF2AE3AE
        Category.YEME_ICME -> 0xFFFFB020
        Category.ULASIM -> 0xFFC6F24E
        Category.FATURA -> 0xFF4FC3F7
        Category.ALISVERIS -> 0xFFFF6B5A
        Category.SAGLIK -> 0xFFFF8FB1
        Category.EGLENCE -> 0xFF8B7BFF
        Category.DIGER -> 0xFF7A8794
    }.toInt()
}
