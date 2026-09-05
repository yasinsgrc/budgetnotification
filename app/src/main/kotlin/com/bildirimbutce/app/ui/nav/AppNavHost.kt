package com.bildirimbutce.app.ui.nav

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.bildirimbutce.app.ui.AddExpenseScreen
import com.bildirimbutce.app.ui.HomeScreen

/**
 * Uygulamanin rota adresleri. Tek yerde durmalari, yeni ekran eklerken
 * elle yazilmis dizgilerin birbirinden sapmasini engelliyor.
 */
object Route {
    const val HOME = "home"
    const val ADD_EXPENSE = "add-expense"
}

/**
 * Uygulamanin tek NavHost'u.
 *
 * Iki hedef var: B1-B4 ekranlarini barindiran [HomeScreen] ve elle harcama
 * girisi [AddExpenseScreen]. Sirada bekleyen ekranlar (onboarding, rapor,
 * ayarlar) birer `composable(...)` satiriyla eklenecek - yol haritasindaki
 * 6, 8, 9 numarali maddeler.
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
            HomeScreen(onAddExpense = { navController.navigate(Route.ADD_EXPENSE) })
        }
        composable(Route.ADD_EXPENSE) {
            // Kaydettikten sonra da vazgectikten sonra da ayni sey olur: geri
            // don. Ana ekran listeyi Room'dan akisla okudugu icin yeni kayit
            // kendiliginden gorunur, elle yenileme gerekmiyor.
            AddExpenseScreen(onDone = { navController.popBackStack() })
        }
    }
}
