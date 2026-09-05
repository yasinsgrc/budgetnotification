package com.bildirimbutce.app.ui.nav

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.bildirimbutce.app.ui.HomeScreen

/**
 * Uygulamanin rota adresleri. Tek yerde durmalari, yeni ekran eklerken
 * elle yazilmis dizgilerin birbirinden sapmasini engelliyor.
 */
object Route {
    const val HOME = "home"
}

/**
 * Uygulamanin tek NavHost'u.
 *
 * Su an tek hedef var (B1-B4 ekranlarini barindiran [HomeScreen]); NavHost'un
 * kendisi bu asamada gorunur bir sey degistirmiyor. Amaci sirada bekleyen
 * ekranlarin (elle harcama girisi, onboarding, rapor, ayarlar) bir
 * `composable(...)` satiriyla baglanabilmesi - yol haritasindaki 5, 6, 8, 9
 * numarali maddelerin hepsi buna bagliydi.
 *
 * `EditExpenseSheet` bilerek rota degil: modal alt sayfa olarak kendi geri
 * tusunu zaten yonetiyor ve yalnizca listedeki bir kayittan aciliyor. Rotaya
 * cevirmek, kaydin id'sini adres uzerinden tasiyip veritabanindan yeniden
 * okumayi gerektirirdi - gorunur bir kazanci olmadan.
 */
@Composable
fun AppNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Route.HOME,
        modifier = modifier
    ) {
        composable(Route.HOME) {
            HomeScreen()
        }
    }
}
