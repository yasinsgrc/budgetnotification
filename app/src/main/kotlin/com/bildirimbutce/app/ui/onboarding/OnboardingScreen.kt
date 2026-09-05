package com.bildirimbutce.app.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.bildirimbutce.app.data.PatternProvider
import com.bildirimbutce.app.ui.theme.AppRadius
import com.bildirimbutce.app.ui.theme.AppSpace
import com.bildirimbutce.app.ui.theme.AppText
import com.bildirimbutce.app.ui.theme.AppTheme
import com.bildirimbutce.app.util.NotificationAccess
import com.bildirimbutce.parser.Category
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * A - onboarding akisi (tasarim A1-A3).
 *
 * Akisin isi izin istemek degil, izni **anlasilir** kilmak: Android'in "tum
 * bildirimleri okuyabilecek" uyarisi ilk kez gorenin elini titretiyor. A2 o
 * diyalogu onceden gosterip yaninda uc somut sozu siraliyor, boylece kullanici
 * sistem ekranina hazirliksiz dusmuyor.
 *
 * [onFinish] her cikista cagriliyor - izin verildiginde de "simdilik elle
 * girerim" dendiginde de. Bayrak orada yaziliyor; akis ikinci acilista
 * gorunmuyor. Izin verilmediyse hatirlatmayi ana ekrandaki izin karti (B3)
 * surduruyor; onboarding'i her acilista tekrarlamanin anlami yok.
 */
@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    val context = LocalContext.current
    var page by rememberSaveable { mutableStateOf(OnboardingPage.INTRO) }
    var permissionGranted by remember { mutableStateOf(NotificationAccess.isGranted(context)) }

    // Izin sistem ayarindan veriliyor; donuste durumu kendimiz okuyoruz.
    // Alternatif kullaniciya "izni verdim" dedirtmekti - ana ekranda oyle
    // (B3'te izin karti ekranda kaliyor), ama burada akisin bir sonraki adimi
    // izne bagli oldugu icin fazladan bir dokunus istemek gereksiz.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                permissionGranted = NotificationAccess.isGranted(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(permissionGranted, page) {
        if (permissionGranted && page == OnboardingPage.PERMISSION) {
            page = OnboardingPage.READY
        }
    }

    BackHandler(enabled = page != OnboardingPage.INTRO) {
        when (page) {
            OnboardingPage.PERMISSION -> page = OnboardingPage.INTRO
            // Kurulum bitti. Geri tusuyla A2'ye donmek, kullaniciyi verdigi izni
            // bir daha gozden gecirmeye zorlamak olurdu; cikis deftere gecmek.
            OnboardingPage.READY -> onFinish()
            OnboardingPage.INTRO -> Unit
        }
    }

    when (page) {
        OnboardingPage.INTRO -> IntroPage(onNext = { page = OnboardingPage.PERMISSION })
        OnboardingPage.PERMISSION -> PermissionPage(
            onOpenSettings = { context.startActivity(NotificationAccess.settingsIntent()) },
            onSkip = onFinish
        )
        OnboardingPage.READY -> ReadyPage(onFinish = onFinish)
    }
}

/**
 * Uc sayfanin ortak iskeleti: govde kayar, alttaki dugmeler sabit kalir.
 *
 * Kaydirma sus degil - A2'de sistem diyalogu ornegi, uc soz satiri ve iki dugme
 * alt alta duruyor; kucuk ekranda dugmeler disari tasardi.
 */
@Composable
private fun OnboardingPageScaffold(
    content: @Composable ColumnScope.() -> Unit,
    footer: @Composable ColumnScope.() -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = AppSpace.s6)
    ) {
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(AppSpace.s8))
            content()
            Spacer(Modifier.height(AppSpace.s6))
        }
        Column(Modifier.padding(bottom = AppSpace.s8), content = footer)
    }
}

// --- A1 ------------------------------------------------------------------

@Composable
private fun IntroPage(onNext: () -> Unit) {
    OnboardingPageScaffold(
        content = {
            Text("BİLDİRİM BÜTÇE", style = AppText.kicker, color = AppTheme.colors.brandBright)
            Spacer(Modifier.height(AppSpace.s4))
            Text(
                "Harcamaların kendiliğinden listelensin.",
                style = AppText.headline,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(AppSpace.s3))
            Text(
                "Banka bildirimi geldiği anda tutarı ve işyerini okur, deftere yazar. " +
                    "Fiş biriktirmen, hiçbir şey girmen gerekmez.",
                style = AppText.body,
                color = AppTheme.colors.onBackgroundMuted
            )
            Spacer(Modifier.height(AppSpace.s6))
            SampleNotificationCard()
            DownwardConnector()
            SampleExpenseRow()
        },
        footer = {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                PageDots(OnboardingPage.INTRO)
                LightButton(text = "Devam  →", onClick = onNext)
            }
        }
    )
}

/** A1'deki ornek banka bildirimi. Gercek bir kayit degil, ne olacaginin resmi. */
@Composable
private fun SampleNotificationCard() {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AppRadius.lg))
            .background(AppTheme.colors.surfaceMuted.copy(alpha = 0.6f))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(AppRadius.lg))
            .padding(AppSpace.s3),
        horizontalArrangement = Arrangement.spacedBy(AppSpace.s3)
    ) {
        Box(
            Modifier
                .size(26.dp)
                .clip(RoundedCornerShape(AppRadius.xs))
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
        ) {
            Text("G", style = AppText.metaMono, color = MaterialTheme.colorScheme.onPrimary)
        }
        Column(Modifier.weight(1f)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Garanti BBVA", style = AppText.labelChip, color = AppTheme.colors.onBackgroundMuted)
                Text("şimdi", style = AppText.metaMono, color = AppTheme.colors.onBackgroundMuted)
            }
            Spacer(Modifier.height(AppSpace.s1))
            Text(
                "MIGROS TICARET AS işyerinde 245,90 TL harcama yapılmıştır.",
                style = AppText.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.88f)
            )
        }
    }
}

/** Bildirimden kayda giden bag; iki karti "sebep - sonuc" olarak birlestiriyor. */
@Composable
private fun DownwardConnector() {
    Box(
        Modifier
            .fillMaxWidth()
            .height(AppSpace.s4),
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier
                .width(1.dp)
                .height(AppSpace.s4)
                .background(AppTheme.colors.brandBright.copy(alpha = 0.5f))
        )
    }
}

/** A1'de bildirimin karsiligi: deftere dusen satirin ana ekrandaki hali. */
@Composable
private fun SampleExpenseRow() {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AppRadius.lg))
            .background(AppTheme.colors.categoryTint(Category.MARKET))
            .border(
                1.dp,
                AppTheme.colors.categoryTintBorder(Category.MARKET),
                RoundedCornerShape(AppRadius.lg)
            )
            .padding(AppSpace.s3),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpace.s3)
    ) {
        Box(
            Modifier
                .size(AppSpace.s8)
                .clip(RoundedCornerShape(AppRadius.sm))
                .background(AppTheme.colors.categoryTint(Category.MARKET)),
            contentAlignment = Alignment.Center
        ) {
            Text(Category.MARKET.emoji, style = AppText.bodyLarge)
        }
        Column(Modifier.weight(1f)) {
            Text("Migros", style = AppText.bodyLarge, color = MaterialTheme.colorScheme.onBackground)
            Spacer(Modifier.height(3.dp))
            Text("MARKET · 21 AĞU 18:42", style = AppText.metaMono, color = AppTheme.colors.onBackgroundMuted)
        }
        Text("− 245,90", style = AppText.amountRow, color = MaterialTheme.colorScheme.onBackground)
    }
}

// --- A2 ------------------------------------------------------------------

@Composable
private fun PermissionPage(onOpenSettings: () -> Unit, onSkip: () -> Unit) {
    val context = LocalContext.current
    // Sayi elle yazilsaydi patterns.json'a banka eklendigi gun sessizce yalan
    // soylerdi; kaynagindan okunuyor.
    val sourceCount by produceState(initialValue = 0, context) {
        value = withContext(Dispatchers.IO) { PatternProvider.sourceCount(context) }
    }

    OnboardingPageScaffold(
        content = {
            Text("SIRADAKİ EKRAN", style = AppText.kicker, color = AppTheme.colors.warning)
            Spacer(Modifier.height(AppSpace.s4))
            Text(
                "Android şimdi sana ürkütücü bir şey soracak.",
                style = AppText.headline,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(AppSpace.s3))
            Text(
                "“Bildirimlere tam erişim” yazacak. Ne olduğunu önceden bilmen için gösteriyorum.",
                style = AppText.body,
                color = AppTheme.colors.onBackgroundMuted
            )
            Spacer(Modifier.height(AppSpace.s4))
            SystemDialogPreview()
            Spacer(Modifier.height(AppSpace.s4))
            PromiseRow(
                "İnternet iznimiz yok",
                "Manifest'te INTERNET yazmıyor; uygulama ağa teknik olarak çıkamaz."
            )
            PromiseRow(
                "Veri telefondan çıkmıyor",
                "Okunan metin cihazdaki veritabanına yazılır, buluta yedeklenmez."
            )
            PromiseRow(
                "Sadece banka bildirimleri",
                "$sourceCount tanımlı uygulamanın dışındakiler okunmadan atılır."
            )
        },
        footer = {
            BrandButton(text = "Anladım, izin ekranına git", onClick = onOpenSettings)
            Spacer(Modifier.height(AppSpace.s3))
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(AppRadius.md))
                    .clickable(onClick = onSkip)
                    .padding(vertical = AppSpace.s3),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Şimdilik elle girerim",
                    style = AppText.labelChip,
                    color = AppTheme.colors.onBackgroundMuted
                )
            }
        }
    )
}

/**
 * Android'in izin diyalogunun ornegi.
 *
 * Sistem diyalogundan birebir kopya degil, ozeti - Android surumden surume
 * ifadeyi degistiriyor ve birebir taklit, degistigi gun yaniltici olurdu.
 */
@Composable
private fun SystemDialogPreview() {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AppRadius.lg))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(AppRadius.lg))
            .padding(AppSpace.s4)
    ) {
        Text(
            "Bildirim Bütçe için bildirimlere tam erişim verilsin mi?",
            style = AppText.titleCard,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(AppSpace.s2))
        Text(
            "Bildirim Bütçe tüm bildirimleri okuyabilecek. Kişiler ve mesajların " +
                "içeriği gibi kişisel bilgileri içerebilir.",
            style = AppText.bodySmall,
            color = AppTheme.colors.onBackgroundMuted
        )
        Spacer(Modifier.height(AppSpace.s3))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppSpace.s4, Alignment.End)
        ) {
            Text("İzin verme", style = AppText.labelChip, color = MaterialTheme.colorScheme.primary)
            Text("İzin ver", style = AppText.labelChip, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun PromiseRow(title: String, body: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = AppSpace.s2),
        horizontalArrangement = Arrangement.spacedBy(AppSpace.s3)
    ) {
        Box(
            Modifier
                .size(20.dp)
                .clip(RoundedCornerShape(AppRadius.xs))
                .background(AppTheme.colors.refund.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Text("✓", style = AppText.metaMono, color = AppTheme.colors.refund)
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = AppText.labelChip, color = MaterialTheme.colorScheme.onBackground)
            Spacer(Modifier.height(AppSpace.s1))
            Text(body, style = AppText.bodySmall, color = AppTheme.colors.onBackgroundMuted)
        }
    }
}

// --- A3 ------------------------------------------------------------------

@Composable
private fun ReadyPage(onFinish: () -> Unit) {
    val context = LocalContext.current
    val counts by produceState(initialValue = 0 to 0, context) {
        value = withContext(Dispatchers.IO) {
            PatternProvider.sourceCount(context) to PatternProvider.patternCount(context)
        }
    }
    val (sourceCount, patternCount) = counts

    OnboardingPageScaffold(
        content = {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(Modifier.height(AppSpace.s8))
                Box(
                    Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(AppTheme.colors.refund.copy(alpha = 0.12f))
                        .border(1.dp, AppTheme.colors.refund.copy(alpha = 0.35f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("✓", style = AppText.headline, color = AppTheme.colors.refund)
                }
                Spacer(Modifier.height(AppSpace.s6))
                Text(
                    "Dinlemeye başladım",
                    style = AppText.headline,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(Modifier.height(AppSpace.s3))
                Text(
                    "Bundan sonra hiçbir şey yapmana gerek yok. Kartını kullandığında " +
                        "harcama saniyeler içinde defterde olacak.",
                    style = AppText.body,
                    color = AppTheme.colors.onBackgroundMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.widthIn(max = 280.dp)
                )
                Spacer(Modifier.height(AppSpace.s6))
                Row(horizontalArrangement = Arrangement.spacedBy(AppSpace.s2)) {
                    ReadyStat("$sourceCount", "BANKA")
                    ReadyStat("$patternCount", "DESEN")
                    // Sabit sifir: manifest'te INTERNET izni yok, uygulamanin
                    // konusabilecegi bir sunucu bulunmuyor.
                    ReadyStat("0", "SUNUCU")
                }
            }
        },
        footer = {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                PageDots(OnboardingPage.READY)
                LightButton(text = "Deftere git  →", onClick = onFinish)
            }
        }
    )
}

@Composable
private fun ReadyStat(value: String, label: String) {
    Column(
        Modifier
            .clip(RoundedCornerShape(AppRadius.md))
            .background(AppTheme.colors.surfaceMuted.copy(alpha = 0.6f))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(AppRadius.md))
            .padding(AppSpace.s3)
    ) {
        Text(value, style = AppText.titleCard, color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.height(AppSpace.s1))
        Text(label, style = AppText.kicker, color = AppTheme.colors.onBackgroundMuted)
    }
}

// --- Ortak parcalar -------------------------------------------------------

/** Kacinci sayfada olundugunu gosteren serit. A2'nin yerinde iki dugme var. */
@Composable
private fun PageDots(current: OnboardingPage) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(AppSpace.s2),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OnboardingPage.entries.forEach { page ->
            val active = page == current
            Box(
                Modifier
                    .width(if (active) 22.dp else 5.dp)
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(
                        if (active) AppTheme.colors.brandBright
                        else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.22f)
                    )
            )
        }
    }
}

/** A1/A3'un "devam" dugmesi - ana ekrandaki kayan dugmeyle ayni dil. */
@Composable
private fun LightButton(text: String, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(AppRadius.md))
            .background(MaterialTheme.colorScheme.onBackground)
            .clickable(onClick = onClick)
            .padding(horizontal = AppSpace.s5, vertical = AppSpace.s4),
        contentAlignment = Alignment.Center
    ) {
        Text(text, style = AppText.labelChip, color = MaterialTheme.colorScheme.background)
    }
}

/** A2'nin asil eylemi. Marka rengi, cunku sayfadaki tek "ileri" yol bu. */
@Composable
private fun BrandButton(text: String, onClick: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AppRadius.md))
            .background(AppTheme.colors.brandBright)
            .clickable(onClick = onClick)
            .padding(vertical = AppSpace.s4),
        contentAlignment = Alignment.Center
    ) {
        Text(text, style = AppText.titleCard, color = MaterialTheme.colorScheme.background)
    }
}
