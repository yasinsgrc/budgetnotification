package com.bildirimbutce.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.bildirimbutce.app.ui.nav.AppNavHost
import com.bildirimbutce.app.ui.theme.BildirimButceTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            BildirimButceTheme {
                AppNavHost()
            }
        }
    }
}
