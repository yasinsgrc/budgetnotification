package com.bildirimbutce.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bildirimbutce.app.BuildConfig
import com.bildirimbutce.app.ui.theme.AppRadius
import com.bildirimbutce.app.ui.theme.AppSpace
import com.bildirimbutce.app.ui.theme.AppText
import com.bildirimbutce.app.ui.theme.AppTheme
import com.bildirimbutce.app.util.NotificationAccess

/**
 * F1 - ayarlar.
 *
 * Bolumun tamami seffaflik uzerine kurulu: bildirimleri okuyan bir uygulamanin
 * neyi okudugunu ve neyi okumadigini gosterebilmesi gerekiyor. Bu yuzden dort
 * ekranin ucu (kaynaklar, kurallar, izinler) sayilarla konusuyor ve sayilarin
 * hepsi calisan koddan okunuyor - hicbiri ekrana elle yazilmadi.
 */
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    onSources: () -> Unit,
    onRules: () -> Unit,
    onPrivacy: () -> Unit
) {
    val context = LocalContext.current
    val sourceRows by viewModel.sourceRows.collectAsStateWithLifecycle()
    val rules by viewModel.rules.collectAsStateWithLifecycle()
    val expenseCount by viewModel.expenseCount.collectAsStateWithLifecycle()
    val patternVersion by viewModel.patternVersion.collectAsStateWithLifecycle()

    // Izin sistem ayarindan veriliyor; kullanici oradan dondugunde satirin hala
    // "kapali" demesi, uygulamanin kendi durumunu bilmedigi izlenimini verirdi.
    var permissionGranted by remember { mutableStateOf(NotificationAccess.isGranted(context)) }
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

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
    ) {
        SettingsTopBar("Ayarlar", onBack)

        SettingsGroup("BİLDİRİM") {
            SettingsRow(
                icon = "🔔",
                label = "Bildirim erişimi",
                sub = "sistem ayarından verilir",
                value = if (permissionGranted) "AÇIK" else "KAPALI",
                valueColor = if (permissionGranted) AppTheme.colors.refund else AppTheme.colors.warning,
                onClick = { context.startActivity(NotificationAccess.settingsIntent()) }
            )
            SettingsRow(
                icon = "🏦",
                label = "Kaynaklar",
                sub = "dinlenen uygulamalar",
                value = "${sourceRows.count { it.enabled }} / ${sourceRows.size}",
                onClick = onSources
            )
        }

        SettingsGroup("VERİ") {
            SettingsRow(
                icon = "🧠",
                label = "Öğrenilen kurallar",
                sub = "işyeri → kategori",
                value = "${rules.size}",
                onClick = onRules
            )
            SettingsRow(
                icon = "🔒",
                label = "Verilerin nereye gidiyor",
                sub = "izin listesi ve sayılar",
                value = "→",
                onClick = onPrivacy
            )
        }

        Spacer(Modifier.height(AppSpace.s5))
        Text(
            "sürüm ${BuildConfig.VERSION_NAME} · desen seti v$patternVersion · $expenseCount kayıt\n" +
                "MIT lisansı · internet izni yok",
            style = AppText.metaMono,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.32f),
            modifier = Modifier.padding(horizontal = AppSpace.s6)
        )
        Spacer(Modifier.height(AppSpace.s8))
    }
}

/** Ayarlar bolumundeki dort ekranin ortak baslik cubugu. */
@Composable
internal fun SettingsTopBar(title: String, onBack: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = AppSpace.s6, vertical = AppSpace.s4),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpace.s3)
    ) {
        Box(
            Modifier
                .size(AppSpace.s6)
                .clip(RoundedCornerShape(AppRadius.sm))
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(AppRadius.sm))
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center
        ) {
            Text("‹", style = AppText.bodyLarge, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.66f))
        }
        Text(title, style = AppText.titleCard, color = MaterialTheme.colorScheme.onBackground)
    }
}

/** Ayarlar bolumundeki bolum basligi. */
@Composable
internal fun SettingsKicker(text: String) {
    Text(
        text,
        style = AppText.kicker,
        color = AppTheme.colors.onBackgroundMuted,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppSpace.s6, vertical = AppSpace.s3)
    )
}

/**
 * Baslik + satirlarin tek bir kart icinde toplanmasi.
 *
 * Satirlari ayiran 1dp cizgi kartin zemininden geliyor: her satira ayri
 * kenarlik cizmek 26dp'lik ekran boslugunda cizgileri kalinlastirip listeyi
 * tabloya cevirmisti.
 */
@Composable
private fun SettingsGroup(title: String, content: @Composable () -> Unit) {
    Column {
        SettingsKicker(title)
        Column(
            Modifier
                .padding(horizontal = AppSpace.s6)
                .fillMaxWidth()
                .clip(RoundedCornerShape(AppRadius.md))
                .background(MaterialTheme.colorScheme.outlineVariant),
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            content()
        }
    }
}

@Composable
private fun SettingsRow(
    icon: String,
    label: String,
    sub: String,
    value: String,
    valueColor: Color = Color.Unspecified,
    onClick: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .clickable(onClick = onClick)
            .padding(horizontal = AppSpace.s4, vertical = AppSpace.s3),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpace.s3)
    ) {
        Box(
            Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(AppRadius.sm))
                .background(AppTheme.colors.surfaceMuted),
            contentAlignment = Alignment.Center
        ) {
            Text(icon, style = AppText.labelChip)
        }
        Column(Modifier.weight(1f)) {
            Text(label, style = AppText.bodyLarge, color = MaterialTheme.colorScheme.onBackground)
            Spacer(Modifier.height(2.dp))
            Text(sub, style = AppText.bodySmall, color = AppTheme.colors.onBackgroundMuted)
        }
        Text(
            value,
            style = AppText.metaMono,
            color = if (valueColor == Color.Unspecified) AppTheme.colors.onBackgroundMuted else valueColor
        )
    }
}
