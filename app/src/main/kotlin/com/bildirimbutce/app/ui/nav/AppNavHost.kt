package com.bildirimbutce.app.ui.nav

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.bildirimbutce.app.ui.AddExpenseScreen
import com.bildirimbutce.app.ui.HomeScreen
import com.bildirimbutce.app.ui.onboarding.OnboardingScreen
import com.bildirimbutce.app.util.Prefs

/**
 * Uygulamanin rota adresleri. Tek yerde durmalari, yeni ekran eklerken
 * elle yazilmis dizgilerin birbirinden sapmasini engelliyor.
 */
object Route {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val ADD_EXPENSE = "add-expense"
}

/**
 * Ilk acilista onboarding, sonrakilerde dogrudan defter acilir.
 *
 * Ayri bir fonksiyon oldugu icin - Compose'a girmeden - test edilebiliyor.
 * Yanlis tarafa dusmesi ya kullaniciyi her acilista ayni uc sayfaya
 * hapsederdi ya da izni hic anlatmadan bos bir ekranda birakirdi.
 */
fun startDestination(onboardingDone: Boolean): String =
    if (onboardingDone) Route.HOME else Route.ONBOARDING

/**
 * Uygulamanin tek NavHost'u.
 *
 * Uc hedef var: onboarding akisi [OnboardingScreen] (A1-A3), B1-B4 ekranlarini
 * barindiran [HomeScreen] ve elle harcama girisi [AddExpenseScreen]. Sirada
 * bekleyen ekranlar (rapor, ayarlar) birer `composable(...)` satiriyla
 * eklenecek - yol haritasindaki 8 ve 9 numarali maddeler.
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
    val context = LocalContext.current
    val prefs = remember { Prefs(context) }
    // Karar bir kez veriliyor. Bayrak akis biterken degisiyor; baslangic hedefi
    // her bestede yeniden okunsaydi NavHost grafi kendini yeniden kurar ve
    // gecis yigini kullanicinin altindan cekilirdi.
    val start = remember { startDestination(prefs.onboardingDone) }

    NavHost(
        navController = navController,
        startDestination = start,
        modifier = modifier
    ) {
        composable(Route.ONBOARDING) {
            OnboardingScreen(
                onFinish = {
                    // Bayrak burada yaziliyor: akistan cikisin tek kapisi bu -
                    // izin verilmis olsa da "simdilik elle girerim" denmis olsa da.
                    prefs.onboardingDone = true
                    navController.navigate(Route.HOME) {
                        // Yigindan silinsin: geri tusuyla kurulum akisina
                        // donmek anlamsiz olurdu.
                        popUpTo(Route.ONBOARDING) { inclusive = true }
                    }
                }
            )
        }
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
