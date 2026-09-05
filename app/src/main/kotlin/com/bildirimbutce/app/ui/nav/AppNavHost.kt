package com.bildirimbutce.app.ui.nav

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.bildirimbutce.app.ui.AddExpenseScreen
import com.bildirimbutce.app.ui.HomeScreen
import com.bildirimbutce.app.ui.MonthCursor
import com.bildirimbutce.app.ui.onboarding.OnboardingScreen
import com.bildirimbutce.app.ui.report.ReportScreen
import com.bildirimbutce.app.ui.settings.PrivacyScreen
import com.bildirimbutce.app.ui.settings.RulesScreen
import com.bildirimbutce.app.ui.settings.SettingsScreen
import com.bildirimbutce.app.ui.settings.SettingsViewModel
import com.bildirimbutce.app.ui.settings.SourcesScreen
import com.bildirimbutce.app.util.Prefs

/**
 * Uygulamanin rota adresleri. Tek yerde durmalari, yeni ekran eklerken
 * elle yazilmis dizgilerin birbirinden sapmasini engelliyor.
 */
object Route {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val ADD_EXPENSE = "add-expense"

    /**
     * Ayarlar bolumu (F1-F4). Dort hedef ic ice bir grafta duruyor; sebebi
     * [AppNavHost] icindeki `settingsViewModel` yorumunda.
     */
    const val SETTINGS_GRAPH = "settings-graph"
    const val SETTINGS = "settings"
    const val SETTINGS_SOURCES = "settings/sources"
    const val SETTINGS_RULES = "settings/rules"
    const val SETTINGS_PRIVACY = "settings/privacy"

    /** Rapor rotasinin ay argumanlari; 0-tabanli ay, [MonthCursor] ile ayni sozlesme. */
    const val ARG_YEAR = "year"
    const val ARG_MONTH = "month"

    /**
     * Rapor, hangi ayin raporu oldugunu adresinde tasiyor.
     *
     * Ekranin kendi basina "bu ay"i varsaymasi, kullanici gecmis bir aya
     * bakarken raporu sessizce baska bir aya kaydirirdi. Ay adreste durdugu
     * icin surec olduruldugunde geri donuste de ayni rapor aciliyor.
     */
    const val REPORT = "report/{$ARG_YEAR}/{$ARG_MONTH}"

    fun report(cursor: MonthCursor): String = "report/${cursor.year}/${cursor.month}"
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
 * Hedefler: onboarding akisi [OnboardingScreen] (A1-A3), B1-B4 ekranlarini
 * barindiran [HomeScreen], elle harcama girisi [AddExpenseScreen], aylik rapor
 * [ReportScreen] (C1) ve ayarlar bolumu (F1-F4) - sonuncusu ic ice bir graf.
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
            HomeScreen(
                onAddExpense = { navController.navigate(Route.ADD_EXPENSE) },
                onReport = { navController.navigate(Route.report(it)) },
                onSettings = { navController.navigate(Route.SETTINGS_GRAPH) }
            )
        }
        composable(Route.ADD_EXPENSE) {
            // Kaydettikten sonra da vazgectikten sonra da ayni sey olur: geri
            // don. Ana ekran listeyi Room'dan akisla okudugu icin yeni kayit
            // kendiliginden gorunur, elle yenileme gerekmiyor.
            AddExpenseScreen(onDone = { navController.popBackStack() })
        }
        composable(
            Route.REPORT,
            arguments = listOf(
                navArgument(Route.ARG_YEAR) { type = NavType.IntType },
                navArgument(Route.ARG_MONTH) { type = NavType.IntType }
            )
        ) { entry ->
            // Argumanlar rotada zorunlu; yine de bir varsayilan veriliyor ki
            // adres elle bozulursa ekran cokmek yerine icinde bulunulan ayi acsin.
            val fallback = MonthCursor.now()
            ReportScreen(
                year = entry.arguments?.getInt(Route.ARG_YEAR) ?: fallback.year,
                month = entry.arguments?.getInt(Route.ARG_MONTH) ?: fallback.month,
                onBack = { navController.popBackStack() }
            )
        }

        // Ayarlar bolumu (F1-F4). Ic ice graf, ekranlarin tek bir ViewModel
        // paylasabilmesi icin - gerekcesi asagidaki `settingsViewModel`de.
        navigation(startDestination = Route.SETTINGS, route = Route.SETTINGS_GRAPH) {
            composable(Route.SETTINGS) { entry ->
                SettingsScreen(
                    viewModel = settingsViewModel(navController, entry),
                    onBack = { navController.popBackStack() },
                    onSources = { navController.navigate(Route.SETTINGS_SOURCES) },
                    onRules = { navController.navigate(Route.SETTINGS_RULES) },
                    onPrivacy = { navController.navigate(Route.SETTINGS_PRIVACY) }
                )
            }
            composable(Route.SETTINGS_SOURCES) { entry ->
                SourcesScreen(
                    viewModel = settingsViewModel(navController, entry),
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Route.SETTINGS_RULES) { entry ->
                RulesScreen(
                    viewModel = settingsViewModel(navController, entry),
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Route.SETTINGS_PRIVACY) { entry ->
                PrivacyScreen(
                    viewModel = settingsViewModel(navController, entry),
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}

/**
 * Ayarlar bolumunun dort ekrani icin ortak ViewModel.
 *
 * Ornek, ekranin kendi hedefine degil ic graf girisine baglaniyor: sayaclar
 * F1'de, anahtarlar F2'de duruyor ve ikisi ayni durumu gosteriyor. Ekran basina
 * ayri ornek olsaydi F2'de kapatilan banka F1'e donuldugunde hala acik
 * gorunurdu - tercih diske yazilmis olsa bile eski ornegin akisi bunu duymazdi.
 */
@Composable
private fun settingsViewModel(
    navController: NavHostController,
    entry: NavBackStackEntry
): SettingsViewModel {
    val parent = remember(entry) { navController.getBackStackEntry(Route.SETTINGS_GRAPH) }
    return viewModel(parent)
}
