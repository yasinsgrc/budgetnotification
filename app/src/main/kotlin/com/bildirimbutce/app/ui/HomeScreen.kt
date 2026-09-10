@file:OptIn(ExperimentalMaterial3Api::class)

package com.bildirimbutce.app.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bildirimbutce.app.BuildConfig
import com.bildirimbutce.app.data.PatternProvider
import com.bildirimbutce.app.data.db.ExpenseEntity
import com.bildirimbutce.app.debug.TestNotificationSeeder
import com.bildirimbutce.app.ui.theme.AppRadius
import com.bildirimbutce.app.ui.theme.AppSpace
import com.bildirimbutce.app.ui.theme.AppText
import com.bildirimbutce.app.ui.theme.AppTheme
import com.bildirimbutce.app.ui.theme.dashedBorder
import com.bildirimbutce.app.util.NotificationAccess
import com.bildirimbutce.parser.Category
import com.bildirimbutce.parser.Money
import com.bildirimbutce.parser.TxKind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    onAddExpense: () -> Unit,
    onReport: (MonthCursor) -> Unit,
    onSettings: () -> Unit,
    onPro: () -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val cursor by viewModel.cursor.collectAsStateWithLifecycle()
    val isPro by viewModel.isPro.collectAsStateWithLifecycle()
    val atHistoryLimit by viewModel.atHistoryLimit.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var permissionGranted by remember { mutableStateOf(NotificationAccess.isGranted(context)) }
    var editing by remember { mutableStateOf<ExpenseEntity?>(null) }
    val scope = rememberCoroutineScope()

    // Yetki uygulamanin disinda (Play Store'da) degisebilir; paywall'dan
    // donuste ekranin eski cevabi gostermemesi icin one cikista yeniden
    // okunuyor - izin durumunun onboarding'de yeniden okunmasiyla ayni gerekce.
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refreshPro()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val showEmptyState = permissionGranted && state.expenses.isEmpty()
    val showTransactions = permissionGranted && state.expenses.isNotEmpty()
    // Izin kapaliyken (B3) yalnizca elle girilenler gosterilir: bildirimden
    // gelenler zaten yakalanamiyor, eski kayitlarin listede durmasi kullaniciyi
    // "demek ki hala calisiyor" yanilgisina dusururdu.
    val manualEntries = state.manualExpenses

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = AppSpace.s8)
        ) {
            item {
                MonthTopBar(
                    label = cursor.label,
                    isPro = isPro,
                    // Ucretsiz surumde rozet bir odul degil bir teklif; defter
                    // bosken teklif edilecek bir gecmis de yok. Pro'da rozet her
                    // zaman duruyor: satin alinmis bir seyin gorunurlugu listenin
                    // doluluguna bagli olmamali.
                    showPro = showTransactions || isPro,
                    atHistoryLimit = atHistoryLimit,
                    onPrevious = viewModel::previousMonth,
                    onNext = viewModel::nextMonth,
                    onSettings = onSettings,
                    onPro = onPro
                )
            }

            if (BuildConfig.DEBUG) {
                item {
                    DebugSeedButton {
                        scope.launch {
                            val added = TestNotificationSeeder.seed(context)
                            Toast.makeText(context, "$added test kaydı eklendi", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }

            when {
                !permissionGranted -> {
                    item {
                        PermissionWarningCard(
                            onGrant = { context.startActivity(NotificationAccess.settingsIntent()) },
                            onRecheck = { permissionGranted = NotificationAccess.isGranted(context) }
                        )
                    }
                    if (manualEntries.isEmpty()) {
                        item { EmptyState(permissionGranted = false) }
                        item {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = AppSpace.s6),
                                contentAlignment = Alignment.Center
                            ) {
                                AddManuallyButton(onAddExpense)
                            }
                        }
                    } else {
                        // Toplam soluk: bu, ayin toplami degil - yalnizca elle
                        // girilenlerin toplami. Solukluk farki tasiyor.
                        item {
                            TotalHeader(
                                totalMinor = state.manualTotalMinor,
                                subtitle = "elle girdiğin harcamalar",
                                muted = true
                            )
                        }
                        item { SectionHeader("ELLE GİRİLENLER · ${manualEntries.size}") }
                        items(manualEntries, key = { it.id }) { expense ->
                            ExpenseRow(expense) { editing = expense }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        }
                    }
                }

                showEmptyState -> {
                    item { TotalHeader(totalMinor = 0, subtitle = "bu ay henüz harcama yok", muted = true) }
                    item { ReadinessCard(context = context, permissionGranted = true, onAdd = onAddExpense) }
                }

                // Rozet yalnizca burada ciziliyor: bos ayda karsilastirilacak
                // bir harcama yok, izin kapaliyken (B3) gosterilen sayi ayin
                // toplami degil - ikisinde de yuzde yaniltici olurdu.
                else -> {
                    item {
                        TotalHeader(
                            totalMinor = state.totalMinor,
                            subtitle = "bu ay harcadın",
                            change = state.change
                        )
                    }
                    if (state.byCategory.isNotEmpty()) {
                        item { CategoryRibbon(state.byCategory, state.totalMinor) }
                    }
                    item {
                        TransactionsHeader(
                            count = state.expenses.size,
                            onReport = { onReport(cursor) }
                        )
                    }
                    items(state.expenses, key = { it.id }) { expense ->
                        ExpenseRow(expense) { editing = expense }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    }
                }
            }
        }

        // Liste doluyken gorunur - izin acik olsun olmasin. Izin kapaliyken elle
        // giris tek yol oldugu icin dugmenin orada da durmasi gerekiyor. Liste
        // bosken bos durum kartinin kendi dugmesi isi goruyor, ikisi cakismasin.
        if (showTransactions || manualEntries.isNotEmpty()) {
            FloatingAddButton(
                onClick = onAddExpense,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = AppSpace.s6)
            )
        }
    }

    editing?.let { expense ->
        EditExpenseSheet(
            expense = expense,
            onDismiss = { editing = null },
            onCategory = { viewModel.setCategory(expense, it); editing = null },
            onMerchant = { viewModel.setMerchant(expense, it); editing = null },
            onDelete = { viewModel.delete(expense); editing = null }
        )
    }
}

@Composable
private fun DebugSeedButton(onSeed: () -> Unit) {
    Text(
        "TEST BİLDİRİMİ EKLE",
        style = AppText.kicker,
        color = AppTheme.colors.brandBright,
        modifier = Modifier
            .padding(horizontal = AppSpace.s6, vertical = AppSpace.s2)
            .clickable(onClick = onSeed)
    )
}

@Composable
private fun MonthTopBar(
    label: String,
    isPro: Boolean,
    showPro: Boolean,
    atHistoryLimit: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSettings: () -> Unit,
    onPro: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = AppSpace.s6, vertical = AppSpace.s4),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpace.s3)) {
            // Ucretsiz surumun siniri: ok solar ve dokunus paywall'a gider.
            // Sessizce hicbir sey yapan bir ok, 9. maddede kaldirilan olu
            // tiklamalardan biri olurdu.
            NavChevron(
                symbol = "‹",
                muted = atHistoryLimit,
                description = if (atHistoryLimit) {
                    "Daha eski aylar Pro sürümde; Pro ekranını açar"
                } else {
                    "Önceki ay"
                },
                onClick = if (atHistoryLimit) onPro else onPrevious
            )
            Text(label.uppercase(Locale("tr", "TR")), style = AppText.kicker, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.78f))
            NavChevron(symbol = "›", description = "Sonraki ay", onClick = onNext)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpace.s2)) {
            if (showPro) ProChip(isPro = isPro, onClick = onPro)
            SettingsGearButton(onSettings)
        }
    }
}

@Composable
private fun NavChevron(
    symbol: String,
    description: String,
    muted: Boolean = false,
    onClick: () -> Unit
) {
    Box(
        Modifier
            .size(AppSpace.s6)
            .clip(RoundedCornerShape(AppRadius.sm))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(AppRadius.sm))
            .clickable(onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center
    ) {
        Text(
            symbol,
            style = AppText.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = if (muted) 0.24f else 0.66f)
        )
    }
}

/**
 * Pro rozeti (E bolumu).
 *
 * Iki isi var ve ikisi ayni gorunemez: yetki acikken **rozet** (satin alinmis
 * bir sey), kapaliyken **teklif**. Ayrimi solukluk tasiyor; ikisi de ayni
 * parlaklikta cizilseydi ucretsiz kullanici Pro'ya sahip oldugunu sanirdi.
 * Rozet halinde tiklanabilir olmasinin sebebi ekranin Pro durumunu da
 * gosterebilmesi (`PaywallScreen`'deki "Pro etkin" karti).
 */
@Composable
private fun ProChip(isPro: Boolean, onClick: () -> Unit) {
    val accent = AppTheme.colors.proAccent
    Box(
        Modifier
            .clip(RoundedCornerShape(AppRadius.sm))
            .background(accent.copy(alpha = if (isPro) 0.16f else 0.08f))
            .border(1.dp, accent.copy(alpha = if (isPro) 0.4f else 0.2f), RoundedCornerShape(AppRadius.sm))
            .clickable(onClick = onClick)
            .padding(horizontal = AppSpace.s3, vertical = AppSpace.s2)
            .semantics {
                contentDescription = if (isPro) "Pro sürüm etkin" else "Pro sürüme geç"
            }
    ) {
        Text("PRO", style = AppText.kicker, color = accent.copy(alpha = if (isPro) 1f else 0.6f))
    }
}

/** Ayarlar bolumunu (F1-F4) aciyor. */
@Composable
private fun SettingsGearButton(onClick: () -> Unit) {
    Box(
        Modifier
            .size(AppSpace.s6)
            .clip(RoundedCornerShape(AppRadius.sm))
            .background(AppTheme.colors.surfaceMuted)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text("⚙", style = AppText.bodyLarge, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.66f))
    }
}

@Composable
private fun TotalHeader(
    totalMinor: Long,
    subtitle: String,
    muted: Boolean = false,
    change: MonthChange? = null
) {
    Column(Modifier.padding(horizontal = AppSpace.s6, vertical = AppSpace.s4)) {
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(AppSpace.s1)) {
            Text(
                Money.format(totalMinor),
                style = AppText.displayAmount,
                color = if (muted) MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f) else MaterialTheme.colorScheme.onBackground
            )
            Text(
                "₺",
                style = AppText.titleCard,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                modifier = Modifier.padding(top = AppSpace.s2)
            )
        }
        Spacer(Modifier.height(AppSpace.s2))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppSpace.s3)
        ) {
            Text(subtitle, style = AppText.body, color = AppTheme.colors.onBackgroundMuted)
            change?.let { MonthChangeBadge(it) }
        }
    }
}

/**
 * "↓ %12 TEMMUZ" - onceki aya gore degisim.
 *
 * Rozette yalnizca ay adi var; karsilastirmanin, ay bitmemisken onceki ayin
 * ayni gunune kadar kisildigi bilgisi oraya sigmiyor - bu yuzden tam cumleyi
 * `contentDescription` soyluyor. Ekran okuyucu kullanan biri yuzdenin neye
 * gore hesaplandigini bilmeden kalmamali.
 */
@Composable
private fun MonthChangeBadge(change: MonthChange) {
    val color = if (change.increased) AppTheme.colors.danger else AppTheme.colors.refund
    val direction = if (change.increased) "daha fazla" else "daha az"
    val reference = change.comparedDays
        ?.let { "geçen ayın ilk $it gününe" }
        ?: "geçen aya"

    Row(
        Modifier
            .clip(RoundedCornerShape(AppRadius.xs))
            .background(color.copy(alpha = 0.13f))
            .padding(horizontal = AppSpace.s2, vertical = 5.dp)
            .semantics {
                contentDescription = "$reference göre %${change.percent} $direction harcadın"
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpace.s1)
    ) {
        Text(
            (if (change.increased) "↑" else "↓") + " %${change.percent}",
            style = AppText.metaMono,
            color = color
        )
        Text(change.previousLabel, style = AppText.metaMono, color = color.copy(alpha = 0.55f))
    }
}

@Composable
private fun CategoryRibbon(rows: List<Pair<Category, Long>>, totalMinor: Long) {
    Column(Modifier.padding(horizontal = AppSpace.s6, vertical = AppSpace.s2)) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(AppSpace.s2)
                .clip(RoundedCornerShape(AppRadius.xs)),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            rows.forEach { (category, amount) ->
                val ratio = if (totalMinor > 0) amount.toFloat() / totalMinor else 0f
                Box(
                    Modifier
                        .weight(ratio.coerceAtLeast(0.01f))
                        .fillMaxSize()
                        .background(AppTheme.colors.categoryColor(category))
                )
            }
        }
        Spacer(Modifier.height(AppSpace.s3))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(AppSpace.s2)) {
            items(rows, key = { it.first }) { (category, amount) ->
                Row(
                    Modifier
                        .clip(RoundedCornerShape(AppRadius.sm))
                        .background(AppTheme.colors.surfaceMuted)
                        .padding(horizontal = AppSpace.s3, vertical = AppSpace.s2),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AppSpace.s2)
                ) {
                    Box(
                        Modifier
                            .size(6.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(AppTheme.colors.categoryColor(category))
                    )
                    Text(category.label, style = AppText.labelChip, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f))
                    Text("${Money.format(amount)} ₺", style = AppText.labelChip, color = AppTheme.colors.onBackgroundMuted)
                }
            }
        }
    }
}

/**
 * "RAPOR →" acik olan ayin raporunu aciyor, "bu ay"in degil: kullanici
 * temmuza bakarken agustos raporunu gormek istemez.
 */
@Composable
private fun TransactionsHeader(count: Int, onReport: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = AppSpace.s6, vertical = AppSpace.s3),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        Text("İŞLEMLER · $count", style = AppText.kicker, color = AppTheme.colors.onBackgroundMuted)
        Text(
            "RAPOR →",
            style = AppText.kicker,
            color = AppTheme.colors.brandBright,
            modifier = Modifier.clickable(onClick = onReport)
        )
    }
}

/** Yan eylemi olmayan bolum basligi (B3'teki "ELLE GIRILENLER" gibi). */
@Composable
private fun SectionHeader(text: String) {
    Text(
        text,
        style = AppText.kicker,
        color = AppTheme.colors.onBackgroundMuted,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppSpace.s6, vertical = AppSpace.s3)
    )
}

@Composable
private fun ExpenseRow(expense: ExpenseEntity, onClick: () -> Unit) {
    val category = Category.from(expense.category)
    val isRefund = expense.kind == TxKind.REFUND.name
    val isUnknown = expense.merchant == null

    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = AppSpace.s6, vertical = AppSpace.s3),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(AppSpace.s8)
                .clip(RoundedCornerShape(AppRadius.sm))
                .background(if (isUnknown) AppTheme.colors.surfaceMuted else AppTheme.colors.categoryTint(category))
                .border(
                    1.dp,
                    if (isUnknown) AppTheme.colors.warning.copy(alpha = 0.5f) else AppTheme.colors.categoryTintBorder(category),
                    RoundedCornerShape(AppRadius.sm)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(category.emoji, style = AppText.bodyLarge)
        }
        Spacer(Modifier.width(AppSpace.s3))
        Column(Modifier.weight(1f)) {
            Text(
                expense.merchant ?: "Bilinmeyen işyeri",
                style = AppText.bodyLarge,
                color = if (isUnknown) AppTheme.colors.onBackgroundMuted else MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(3.dp))
            Text(
                metaLine(expense, category, isUnknown),
                style = AppText.metaMono,
                color = AppTheme.colors.onBackgroundMuted
            )
        }
        Text(
            (if (isRefund) "+" else "−") + " ${Money.format(expense.amountMinor)} ₺",
            style = AppText.amountRow,
            color = if (isRefund) AppTheme.colors.refund else MaterialTheme.colorScheme.onBackground
        )
    }
}

private fun metaLine(expense: ExpenseEntity, category: Category, isUnknown: Boolean): String {
    val date = dateFormat.format(Date(expense.occurredAt)).uppercase(Locale("tr", "TR"))
    val prefix = if (isUnknown) "DOKUN VE DÜZELT" else category.label.uppercase(Locale("tr", "TR"))
    return "$prefix · $date"
}

@Composable
private fun FloatingAddButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .clip(RoundedCornerShape(AppRadius.md))
            .background(MaterialTheme.colorScheme.onBackground)
            .clickable(onClick = onClick)
            .padding(horizontal = AppSpace.s4, vertical = AppSpace.s3),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpace.s2)
    ) {
        Text("+", style = AppText.titleCard, color = MaterialTheme.colorScheme.background)
        Text("Elle ekle", style = AppText.bodyLarge, color = MaterialTheme.colorScheme.background)
    }
}

@Composable
private fun PermissionWarningCard(onGrant: () -> Unit, onRecheck: () -> Unit) {
    Column(
        Modifier
            .padding(horizontal = AppSpace.s6, vertical = AppSpace.s3)
            .fillMaxWidth()
            .clip(RoundedCornerShape(AppRadius.lg))
            .background(AppTheme.colors.warning.copy(alpha = 0.07f))
            .border(1.dp, AppTheme.colors.warning.copy(alpha = 0.26f), RoundedCornerShape(AppRadius.lg))
            .padding(AppSpace.s4)
    ) {
        Text("İZİN GEREKLİ", style = AppText.kicker, color = AppTheme.colors.warning)
        Spacer(Modifier.height(AppSpace.s2))
        Text("Bildirim erişimi kapalı", style = AppText.titleCard, color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.height(AppSpace.s1))
        Text(
            "Harcamalar otomatik yakalanamıyor. İnternet iznimiz yok — okunan metinler telefondan çıkmaz.",
            style = AppText.body,
            color = AppTheme.colors.onBackgroundMuted
        )
        Spacer(Modifier.height(AppSpace.s3))
        Row(horizontalArrangement = Arrangement.spacedBy(AppSpace.s2), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .clip(RoundedCornerShape(AppRadius.md))
                    .background(AppTheme.colors.warningBright)
                    .clickable(onClick = onGrant)
                    .padding(horizontal = AppSpace.s4, vertical = AppSpace.s3)
            ) {
                Text("Ayarları aç", style = AppText.labelChip, color = AppTheme.colors.onWarningBright)
            }
            Box(
                Modifier
                    .clip(RoundedCornerShape(AppRadius.md))
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(AppRadius.md))
                    .clickable(onClick = onRecheck)
                    .padding(horizontal = AppSpace.s4, vertical = AppSpace.s3)
            ) {
                Text("İzni verdim", style = AppText.labelChip, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.78f))
            }
        }
    }
}

@Composable
private fun ReadinessCard(context: android.content.Context, permissionGranted: Boolean, onAdd: () -> Unit) {
    val patternCount by produceState(initialValue = 0, context) {
        value = withContext(Dispatchers.IO) { PatternProvider.patternCount(context) }
    }

    Column(Modifier.padding(horizontal = AppSpace.s6, vertical = AppSpace.s4)) {
        EmptyStateCard(onAdd = onAdd)
        Spacer(Modifier.height(AppSpace.s5))
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(AppRadius.lg))
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(AppRadius.lg))
                .padding(AppSpace.s4)
        ) {
            Text("HAZIRLIK DURUMU", style = AppText.kicker, color = AppTheme.colors.onBackgroundMuted)
            Spacer(Modifier.height(AppSpace.s3))
            ReadinessRow("Bildirim erişimi", if (permissionGranted) "açık" else "kapalı", ok = permissionGranted)
            ReadinessRow("Desen seti v1", "$patternCount desen", ok = true)
            ReadinessRow("İlk harcama", "bekliyor", ok = false)
        }
    }
}

@Composable
private fun ReadinessRow(label: String, value: String, ok: Boolean) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = AppSpace.s2),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpace.s2)
    ) {
        Box(
            Modifier
                .size(18.dp)
                .clip(RoundedCornerShape(AppRadius.xs))
                .background(if (ok) AppTheme.colors.refund.copy(alpha = 0.14f) else AppTheme.colors.surfaceMuted),
            contentAlignment = Alignment.Center
        ) {
            Text(if (ok) "✓" else "—", style = AppText.metaMono, color = if (ok) AppTheme.colors.refund else AppTheme.colors.onBackgroundMuted)
        }
        Text(label, style = AppText.body, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f))
        Text(value, style = AppText.metaMono, color = AppTheme.colors.onBackgroundMuted)
    }
}

/**
 * B4 bos durum karti. Tasarimdaki TEK kesikli kenarlikli oge bu; kart "burada
 * bir sey olacak ama henuz yok" diyor, kesikli cizgi de tam olarak bunu
 * anlatiyor - duz cizgi onu dolu bir kartla ayni agirliga sokuyordu.
 *
 * Olculer tasarimin en yakin token'ina yuvarlanmis durumda (piksel-birebir
 * degil, token-birebir). Yuvarlanmayan tek deger yaricap: tasarim 20px diyor,
 * `AppRadius.lg` zaten 20dp.
 */
@Composable
private fun EmptyStateCard(onAdd: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AppRadius.lg))
            .background(AppTheme.colors.surfaceMuted.copy(alpha = 0.5f))
            .dashedBorder(MaterialTheme.colorScheme.outline, AppRadius.lg)
            .padding(horizontal = AppSpace.s5, vertical = AppSpace.s6),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(AppRadius.md))
                .background(AppTheme.colors.refund.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Text("📥", style = AppText.headline)
        }
        Spacer(Modifier.height(AppSpace.s4))
        Text("İlk bildirim bekleniyor", style = AppText.titleCard, color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.height(AppSpace.s2))
        Text(
            "Kartını bir yerde kullandığında harcama saniyeler içinde burada olacak. Beklemek istemezsen elle de ekleyebilirsin.",
            style = AppText.bodySmall,
            color = AppTheme.colors.onBackgroundMuted,
            modifier = Modifier.widthIn(max = 260.dp)
        )
        Spacer(Modifier.height(AppSpace.s4))
        AddManuallyButton(onAdd)
    }
}

/** "+ Elle harcama ekle" - hem bos durum kartinda hem B3 ekraninda ayni dugme. */
@Composable
private fun AddManuallyButton(onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(AppRadius.md))
            .background(MaterialTheme.colorScheme.onBackground)
            .clickable(onClick = onClick)
            .padding(horizontal = AppSpace.s4, vertical = AppSpace.s3)
    ) {
        Text("+ Elle harcama ekle", style = AppText.labelChip, color = MaterialTheme.colorScheme.background)
    }
}

@Composable
private fun EmptyState(permissionGranted: Boolean) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = AppSpace.s6, vertical = AppSpace.s8),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            if (permissionGranted) "Henüz harcama yok" else "İzin bekleniyor",
            style = AppText.titleCard,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(AppSpace.s2))
        Text(
            if (permissionGranted) "Bir banka bildirimi geldiğinde burada görünecek."
            else "Bildirim erişimi verildiğinde harcamalar otomatik listelenir.",
            style = AppText.body,
            color = AppTheme.colors.onBackgroundMuted
        )
    }
}

private val dateFormat = SimpleDateFormat("d MMM HH:mm", Locale("tr", "TR"))
