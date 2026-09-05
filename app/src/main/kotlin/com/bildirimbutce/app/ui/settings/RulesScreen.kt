package com.bildirimbutce.app.ui.settings

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bildirimbutce.app.data.db.MerchantRuleEntity
import com.bildirimbutce.app.ui.theme.AppRadius
import com.bildirimbutce.app.ui.theme.AppSpace
import com.bildirimbutce.app.ui.theme.AppText
import com.bildirimbutce.app.ui.theme.AppTheme
import com.bildirimbutce.parser.Category

/**
 * F3 - ogrenilmis "isyeri -> kategori" kurallari.
 *
 * Ekranin isi kural yazmak degil, ogrenilenleri **gorunur** kilmak: kural,
 * kullanici bir kategoriyi duzeltince sessizce olusuyor. Gorulemezse uygulama
 * bir sure sonra "nedense hep boyle siniflandiriyor" haline gelir ve
 * kullanicinin elinde her kaydi tek tek duzeltmekten baska yol kalmazdi.
 */
@Composable
fun RulesScreen(viewModel: SettingsViewModel, onBack: () -> Unit) {
    val rules by viewModel.rules.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = AppSpace.s8)
    ) {
        item { SettingsTopBar("Kurallar", onBack) }
        item { RulesHeadline(rules.size) }

        if (rules.isEmpty()) {
            item { EmptyRules() }
        } else {
            item { SettingsKicker("İŞYERİ → KATEGORİ") }
            items(rules, key = { it.merchantKey }) { rule ->
                RuleRow(rule) { viewModel.forgetRule(rule.merchantKey) }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            }
        }
    }
}

@Composable
private fun RulesHeadline(count: Int) {
    Column(Modifier.padding(horizontal = AppSpace.s6, vertical = AppSpace.s3)) {
        Text("$count", style = AppText.displaySheet, color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.height(AppSpace.s2))
        Text(
            "işyeri kuralı öğrenildi. Her düzeltmen bir sonrakini otomatik doğru yapıyor.",
            style = AppText.body,
            color = AppTheme.colors.onBackgroundMuted
        )
    }
}

@Composable
private fun EmptyRules() {
    Text(
        "Henüz kural yok. Bir harcamanın kategorisini düzelttiğinde o işyeri için " +
            "kural burada belirir ve sonraki harcamalar doğrudan doğru kategoriye düşer.",
        style = AppText.body,
        color = AppTheme.colors.onBackgroundMuted,
        modifier = Modifier.padding(horizontal = AppSpace.s6, vertical = AppSpace.s3)
    )
}

/**
 * "✕" kurali unutturuyor, gecmisi geri almiyor.
 *
 * Kural ogrenilirken duzeltilen kayitlar `userEdited = true` oldu, yani
 * kullanicinin kendi karari. Silme onlara da dokunsaydi, kullanicinin elle
 * verdigi kararlari da silmis olurduk.
 */
@Composable
private fun RuleRow(rule: MerchantRuleEntity, onForget: () -> Unit) {
    val category = Category.from(rule.category)

    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = AppSpace.s6, vertical = AppSpace.s3),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpace.s3)
    ) {
        Text(
            rule.merchantKey,
            style = AppText.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f)
        )
        Text("→", style = AppText.metaMono, color = AppTheme.colors.onBackgroundMuted)
        Row(
            Modifier
                .clip(RoundedCornerShape(AppRadius.sm))
                .background(AppTheme.colors.categoryTint(category))
                .padding(horizontal = AppSpace.s3, vertical = AppSpace.s2),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppSpace.s1)
        ) {
            Text(category.emoji, style = AppText.labelChip)
            Text(category.label, style = AppText.labelChip, color = AppTheme.colors.categoryColor(category))
        }
        Box(
            Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(AppRadius.sm))
                .clickable(onClick = onForget),
            contentAlignment = Alignment.Center
        ) {
            Text("✕", style = AppText.metaMono, color = AppTheme.colors.onBackgroundMuted)
        }
    }
}
