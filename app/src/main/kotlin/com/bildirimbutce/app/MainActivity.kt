package com.bildirimbutce.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
                // [enableEdgeToEdge] icerigi sistem cubuklarinin ALTINA cizdiriyor.
                // Hicbir ekran kendi basina inset okumadigi icin ana ekranin ust
                // bari saat/sarj gostergesiyle ust uste biniyordu. Dolgu tek yerde
                // duruyor - NavHost'un disinda - ki kural ekran basina
                // kopyalanmasin: yeni bir hedef eklendiginde unutulacak bir adim
                // olsaydi hata sessizce geri gelirdi.
                //
                // Zemin dolgunun DISINDA: iceride olsaydi cubuklarin arkasi
                // boyanmadan kalir ve XML'deki windowBackground gorunurdu - o renk
                // yalnizca acik tema icin tanimli (`values/colors.xml`), koyu temada
                // ekranin tepesinde acik bir serit birakirdi.
                //
                // Klavye (ime) bilerek disarida: buradaki is cubuk ortusmesi.
                // `safeDrawing` secilseydi klavye acilinca tum graf yeniden
                // olculurdu - elle giris ekranini bugunku davranisindan koparan,
                // istenmemis bir degisiklik.
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    AppNavHost(
                        modifier = Modifier.windowInsetsPadding(
                            WindowInsets.systemBars.union(WindowInsets.displayCutout)
                        ),
                        deepLinkRoute = pendingRoute,
                        onDeepLinkHandled = { pendingRoute = null }
                    )
                }
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
