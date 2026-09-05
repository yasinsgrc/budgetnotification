package com.bildirimbutce.app.ui.settings

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bildirimbutce.app.ui.theme.AppRadius
import com.bildirimbutce.app.ui.theme.AppSpace
import com.bildirimbutce.app.ui.theme.AppText
import com.bildirimbutce.app.ui.theme.AppTheme

/**
 * F4 - "verilerin nereye gidiyor".
 *
 * Ekranin tamami tek bir iddiaya dayaniyor: veri cihazdan cikmiyor. Iddia
 * ekrana cumle olarak yazilsaydi dogrulanamazdi; bu yuzden izin listesi elle
 * yazilmiyor, uygulamanin kendi manifest'inden [PackageManager] ile okunuyor.
 * Koda bir gun INTERNET izni eklenirse ekran onu kendiliginden gosterir -
 * metni guncellemeyi unutmak mumkun degil.
 */
@Composable
fun PrivacyScreen(viewModel: SettingsViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val expenseCount by viewModel.expenseCount.collectAsStateWithLifecycle()
    val rows by viewModel.sourceRows.collectAsStateWithLifecycle()
    val rules by viewModel.rules.collectAsStateWithLifecycle()

    val permissions = remember(context) { requestedPermissions(context) }
    val internetRequested = remember(permissions) { permissions.any { it.endsWith(".INTERNET") } }
    val backupAllowed = remember(context) { isBackupAllowed(context) }

    var confirming by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = AppSpace.s8)
    ) {
        item { SettingsTopBar("Verilerin nereye gidiyor", onBack) }
        item {
            Column(Modifier.padding(horizontal = AppSpace.s6, vertical = AppSpace.s3)) {
                Text(
                    if (internetRequested) "Bir yere gidebilir." else "Hiçbir yere.",
                    style = AppText.headline,
                    color = if (internetRequested) AppTheme.colors.danger
                    else MaterialTheme.colorScheme.onBackground
                )
                Spacer(Modifier.height(AppSpace.s3))
                Text(
                    "Bu bir pazarlama cümlesi değil, izin listesinin sonucu. Aşağıdaki liste " +
                        "uygulamanın kendi manifest'inden okunuyor; aynısını Android'in " +
                        "uygulama bilgisi ekranından da doğrulayabilirsin.",
                    style = AppText.body,
                    color = AppTheme.colors.onBackgroundMuted
                )
            }
        }

        item { SettingsKicker("İSTENEN İZİNLER") }
        item {
            Column(Modifier.padding(horizontal = AppSpace.s6)) {
                // INTERNET her zaman listeleniyor; ekranin asil iddiasi onun
                // **yoklugu** oldugu icin yoklugun da bir satiri olmasi gerekiyor.
                ProofRow(
                    text = "android.permission.INTERNET",
                    state = if (internetRequested) "VAR" else "YOK",
                    ok = !internetRequested
                )
                permissions.forEach { ProofRow(text = it, state = "VAR", ok = true) }
                if (permissions.isEmpty()) {
                    ProofRow(text = "— başka izin istenmiyor —", state = "", ok = true)
                }
            }
        }

        item { SettingsKicker("SAYILARLA") }
        item {
            Column(Modifier.padding(horizontal = AppSpace.s6)) {
                ProofRow(
                    text = "buluta yedekleme",
                    state = if (backupAllowed) "AÇIK" else "KAPALI",
                    ok = !backupAllowed
                )
                ProofRow(
                    text = "dinlenen kaynak",
                    state = "${rows.count { it.enabled }} / ${rows.size}",
                    ok = true
                )
                ProofRow(text = "cihazdaki kayıt", state = "$expenseCount", ok = true)
                ProofRow(text = "öğrenilmiş kural", state = "${rules.size}", ok = true)
            }
        }

        item {
            Spacer(Modifier.height(AppSpace.s5))
            EraseButton(enabled = expenseCount > 0 || rules.isNotEmpty()) { confirming = true }
        }
    }

    // Silme geri alinamiyor ve tek dokunusla ulasilabilir bir yerde duruyor;
    // onaysiz olsaydi yanlislikla dokunmak butun defteri goturebilirdi.
    if (confirming) {
        AlertDialog(
            onDismissRequest = { confirming = false },
            title = { Text("Tüm veri silinsin mi?", style = AppText.titleCard) },
            text = {
                Text(
                    "$expenseCount kayıt ve ${rules.size} öğrenilmiş kural silinecek. " +
                        "İşlem geri alınamaz: veriler yalnızca bu cihazda tutulduğu için " +
                        "geri getirilebilecek bir kopya yok.",
                    style = AppText.body
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.eraseAll(); confirming = false }) {
                    Text("Sil", color = AppTheme.colors.danger)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirming = false }) { Text("Vazgeç") }
            }
        )
    }
}

@Composable
private fun ProofRow(text: String, state: String, ok: Boolean) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = AppSpace.s2),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpace.s3)
    ) {
        Box(
            Modifier
                .size(18.dp)
                .clip(RoundedCornerShape(AppRadius.xs))
                .background(
                    if (ok) AppTheme.colors.refund.copy(alpha = 0.14f)
                    else AppTheme.colors.warning.copy(alpha = 0.16f)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                if (ok) "✓" else "!",
                style = AppText.metaMono,
                color = if (ok) AppTheme.colors.refund else AppTheme.colors.warning
            )
        }
        Text(
            text,
            style = AppText.metaMono,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            modifier = Modifier.weight(1f)
        )
        Text(
            state,
            style = AppText.metaMono,
            color = if (ok) AppTheme.colors.onBackgroundMuted else AppTheme.colors.warning
        )
    }
}

/** Silinecek bir sey yokken dugme soluk: bos bir defter icin onay sormanin anlami yok. */
@Composable
private fun EraseButton(enabled: Boolean, onClick: () -> Unit) {
    val color = if (enabled) AppTheme.colors.danger else AppTheme.colors.onBackgroundMuted
    Box(
        Modifier
            .padding(horizontal = AppSpace.s6)
            .fillMaxWidth()
            .clip(RoundedCornerShape(AppRadius.md))
            .border(1.dp, color.copy(alpha = 0.34f), RoundedCornerShape(AppRadius.md))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = AppSpace.s3),
        contentAlignment = Alignment.Center
    ) {
        Text("Tüm veriyi sil", style = AppText.labelChip, color = color)
    }
}

/**
 * Manifest'te beyan edilen izinler; kurulu paketten okunuyor. Ekranda yazan
 * liste ile yayinlanan APK'nin izinleri boylece ayni kaynaktan geliyor.
 */
private fun requestedPermissions(context: Context): List<String> = runCatching {
    context.packageManager
        .getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS)
        .requestedPermissions
        ?.toList()
        .orEmpty()
        // AGP'nin hedef SDK icin ekledigi ic izin; kullaniciya bir sey anlatmiyor.
        .filterNot { it.endsWith("DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION") }
        .sorted()
}.getOrDefault(emptyList())

private fun isBackupAllowed(context: Context): Boolean =
    (context.applicationInfo.flags and ApplicationInfo.FLAG_ALLOW_BACKUP) != 0
