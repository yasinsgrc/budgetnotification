package com.bildirimbutce.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.bildirimbutce.app.ui.nav.AppNavHost
import com.bildirimbutce.app.ui.theme.BildirimButceTheme

class MainActivity : ComponentActivity() {

    /**
     * Widget'in istedigi hedef; tuketilince temizleniyor.
     *
     * Durum olarak tutulmasinin sebebi [onNewIntent]: uygulama zaten acikken
     * widget'a dokunuldugunda yeni niyet buraya duser, `setContent` bir daha
     * calismaz. `intent`'i dogrudan okusaydik ikinci dokunus hicbir sey yapmazdi.
     */
    private var pendingRoute by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        pendingRoute = intent?.getStringExtra(EXTRA_ROUTE)
        setContent {
            BildirimButceTheme {
                AppNavHost(
                    deepLinkRoute = pendingRoute,
                    onDeepLinkHandled = { pendingRoute = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingRoute = intent.getStringExtra(EXTRA_ROUTE)
    }

    companion object {
        /**
         * Acilista gidilecek rota. Bugun tek yazani kilitli 4x2 widget
         * (`BudgetWidgets.openApp`), tek okuyani [AppNavHost].
         */
        const val EXTRA_ROUTE = "com.bildirimbutce.app.extra.ROUTE"
    }
}
