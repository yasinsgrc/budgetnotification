package com.bildirimbutce.app.ui.settings

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bildirimbutce.app.ui.theme.AppRadius
import com.bildirimbutce.app.ui.theme.AppSpace
import com.bildirimbutce.app.ui.theme.AppText
import com.bildirimbutce.app.ui.theme.AppTheme
import java.util.Locale

/**
 * F2 - dinlenen bildirim kaynaklari.
 *
 * Liste `patterns.json`'dan geliyor, koddan degil: kullanicinin kapatabildigi
 * kume ile servisin dinledigi kume ayni dosyadan turemezse ekran, gercekte
 * okunan bir uygulamayi hic gostermeyebilirdi.
 *
 * Bu ekran ayni zamanda Play Console beyanindaki "kullanici dinlenen listeyi
 * daraltabilir" cumlesinin karsiligi; ekran olmadan o cumle yazilamiyordu.
 */
@Composable
fun SourcesScreen(viewModel: SettingsViewModel, onBack: () -> Unit) {
    val rows by viewModel.sourceRows.collectAsStateWithLifecycle()
    val enabledCount = rows.count { it.enabled }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = AppSpace.s8)
    ) {
        item { SettingsTopBar("Kaynaklar", onBack) }
        item {
            Text(
                "Yalnızca açık olan uygulamaların bildirimleri okunur. Kapalı olanlar " +
                    "servise gelse bile metni ayrıştırılmadan atılır.",
                style = AppText.body,
                color = AppTheme.colors.onBackgroundMuted,
                modifier = Modifier.padding(horizontal = AppSpace.s6, vertical = AppSpace.s2)
            )
        }

        // Hepsi kapaliyken otomatik yakalama tamamen durur. Sessiz kalsaydik
        // kullanici harcamalarinin neden gelmedigini uygulamanin bozuklugu
        // sanardi: izin acik oldugu icin ana ekranda hicbir uyari cikmaz.
        if (rows.isNotEmpty() && enabledCount == 0) {
            item { AllSourcesOffWarning() }
        }

        items(rows, key = { it.source.packageName }) { row ->
            SourceRowItem(row) { viewModel.setSourceEnabled(row.source.packageName, it) }
        }

        item {
            Spacer(Modifier.height(AppSpace.s4))
            Text(
                "$enabledCount / ${rows.size} kaynak dinleniyor · patterns.json",
                style = AppText.metaMono,
                color = AppTheme.colors.onBackgroundMuted,
                modifier = Modifier.padding(horizontal = AppSpace.s6)
            )
        }
    }
}

@Composable
private fun AllSourcesOffWarning() {
    Column(
        Modifier
            .padding(horizontal = AppSpace.s6, vertical = AppSpace.s3)
            .fillMaxWidth()
            .clip(RoundedCornerShape(AppRadius.md))
            .background(AppTheme.colors.warning.copy(alpha = 0.09f))
            .padding(AppSpace.s4)
    ) {
        Text("HİÇBİRİ AÇIK DEĞİL", style = AppText.kicker, color = AppTheme.colors.warning)
        Spacer(Modifier.height(AppSpace.s2))
        Text(
            "Otomatik yakalama durdu. Harcamalar yalnızca elle eklediğinde deftere girer.",
            style = AppText.bodySmall,
            color = AppTheme.colors.onBackgroundMuted
        )
    }
}

/**
 * Bas harf kutusu da acik/kapali bilgisini tasiyor: on yedi satirlik listede
 * her anahtarin konumunu tek tek okumak yerine renk taranabiliyor.
 */
@Composable
private fun SourceRowItem(row: SourceRow, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = AppSpace.s6, vertical = AppSpace.s2),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpace.s3)
    ) {
        Box(
            Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(AppRadius.sm))
                .background(
                    if (row.enabled) AppTheme.colors.brandBright.copy(alpha = 0.16f)
                    else AppTheme.colors.surfaceMuted
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                row.source.label.take(1).uppercase(Locale("tr", "TR")),
                style = AppText.metaMono,
                color = if (row.enabled) AppTheme.colors.brandBright else AppTheme.colors.onBackgroundMuted
            )
        }
        Column(Modifier.weight(1f)) {
            Text(
                row.source.label,
                style = AppText.bodyLarge,
                color = if (row.enabled) MaterialTheme.colorScheme.onBackground
                else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f)
            )
            Spacer(Modifier.height(2.dp))
            // Paket adi da yaziliyor: kullanici listeyi Android'in uygulama
            // listesiyle karsilastirip dogrulayabilsin.
            Text(row.source.packageName, style = AppText.metaMono, color = AppTheme.colors.onBackgroundMuted)
        }
        Switch(
            checked = row.enabled,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.background,
                checkedTrackColor = AppTheme.colors.brandBright,
                uncheckedThumbColor = AppTheme.colors.onBackgroundMuted,
                uncheckedTrackColor = AppTheme.colors.surfaceMuted
            )
        )
    }
}
