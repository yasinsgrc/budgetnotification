package com.bildirimbutce.app.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.bildirimbutce.app.data.ExpenseRepository
import com.bildirimbutce.app.data.PatternProvider
import com.bildirimbutce.app.util.Prefs
import com.bildirimbutce.app.widget.BudgetWidget
import com.bildirimbutce.parser.ParseResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Bildirimleri yakalar, ayristirir, kaydeder.
 *
 * Tasarim kurallari:
 *  - Bildirim metni ASLA cihazdan cikmaz (manifest'te INTERNET izni yok).
 *  - onNotificationPosted ana is parcaciginda calisir; disk islemleri IO'ya atilir.
 *  - Yalnizca patterns.json icindeki bilinen paketler islenir; geri kalani
 *    hic okunmaz - hem gizlilik hem pil icin.
 */
class NotificationService : NotificationListenerService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var repository: ExpenseRepository
    private lateinit var prefs: Prefs

    override fun onCreate() {
        super.onCreate()
        repository = ExpenseRepository(applicationContext)
        prefs = Prefs(applicationContext)
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val notification = sbn ?: return
        val pkg = notification.packageName ?: return
        val postedAt = notification.postTime

        val parser = PatternProvider.parser(applicationContext)
        if (!parser.isKnownSource(pkg)) return

        val enabled = prefs.enabledSources
        if (enabled.isNotEmpty() && pkg !in enabled) return

        val text = notification.extractText().ifBlank { return }

        scope.launch {
            when (val result = parser.parse(text)) {
                is ParseResult.Match -> {
                    val added = repository.record(result.transaction, pkg, postedAt)
                    if (added) BudgetWidget.refresh(applicationContext)
                }
                is ParseResult.Ignored -> Log.d(TAG, "Atlandi: ${result.reason}")
                ParseResult.NoMatch -> Log.d(TAG, "Eslesme yok ($pkg)")
            }
        }
    }

    /**
     * Banka SMS'leri uzun oldugu icin sikca kisaltilir; bigText mevcutsa
     * tercih edilir, yoksa baslik + govde birlestirilir.
     */
    private fun StatusBarNotification.extractText(): String {
        val extras = notification.extras
        val big = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
        val body = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
        val title = extras.getString(Notification.EXTRA_TITLE)
        val main = big?.takeIf { it.isNotBlank() } ?: body.orEmpty()
        return listOfNotNull(title?.takeIf { it.isNotBlank() }, main.takeIf { it.isNotBlank() })
            .joinToString(" ")
            .trim()
    }

    private companion object {
        const val TAG = "NotificationService"
    }
}
