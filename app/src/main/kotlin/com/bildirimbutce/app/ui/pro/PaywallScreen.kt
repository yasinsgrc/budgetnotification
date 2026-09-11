package com.bildirimbutce.app.ui.pro

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bildirimbutce.app.BuildConfig
import com.bildirimbutce.app.ui.theme.AppRadius
import com.bildirimbutce.app.ui.theme.AppSpace
import com.bildirimbutce.app.ui.theme.AppText
import com.bildirimbutce.app.ui.theme.AppTheme
import com.bildirimbutce.app.widget.WIDE_WIDGET_ROW_COUNT

/**
 * E1 - Pro / paywall.
 *
 * Ekran tasarimdan iki yerde bilerek ayriliyor; ikisi de ayni sebeple: burada
 * yazan her seyin bugun dogru olmasi gerekiyor.
 *
 *  - **Dort ozellik yerine iki tane.** Tasarim sinirsiz gecmis, 4x2 widget, CSV
 *    disa aktarma ve kendi desenini yazma sayiyor; son ikisi kodda yok. 4x2
 *    widget yol haritasinin 12. maddesiyle yazildi ve listeye o gun eklendi.
 *    Olmayan bir seyin parasini istemek, 9. maddede kaldirilan "hicbir yere
 *    gitmeyen dugme"nin para karsiligi olurdu.
 *  - **Fiyat yazmiyor.** Tasarimdaki 149,00 ₺ bir yer tutucu. Gercek fiyat
 *    Play Console'daki urunden okunur; urun tanimli olmadigi icin ekrana
 *    yazilacak dogru bir sayi da yok.
 *
 * Satin alma dugmesi bu yuzden soluk ve sebebini kendisi soyluyor
 * ([ProViewModel.message]).
 */
@Composable
fun PaywallScreen(
    onClose: () -> Unit,
    viewModel: ProViewModel = viewModel()
) {
    val isPro by viewModel.isPro.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = AppSpace.s8)
    ) {
        item { CloseBar(onClose) }

        item {
            Column(Modifier.padding(horizontal = AppSpace.s6)) {
                ProBadge()
                Spacer(Modifier.height(AppSpace.s5))
                Text(
                    "Bir kere öde, ömür boyu kullan.",
                    style = AppText.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(Modifier.height(AppSpace.s3))
                Text(
                    "Abonelik yok, yenileme yok. Ödeme Play Store uygulamasına " +
                        "devredilir; uygulamanın kendisi yine internete çıkmaz.",
                    style = AppText.body,
                    color = AppTheme.colors.onBackgroundMuted
                )
            }
        }

        item {
            Spacer(Modifier.height(AppSpace.s5))
            // Iddia burada cumle olarak duruyor; dogrulanabilecegi yer ayarlardaki
            // F4 - orada izin listesi uygulamanin kendi manifest'inden okunuyor.
            NoteCard(
                accent = AppTheme.colors.refund,
                mark = "i",
                text = "Manifest'e INTERNET izni eklenmedi. Ayarlar > \"Verilerin nereye " +
                    "gidiyor\" ekranındaki izin listesi uygulamanın kendi manifest'inden " +
                    "okunuyor; bu cümleyi oradan doğrulayabilirsin."
            )
        }

        item {
            Spacer(Modifier.height(AppSpace.s4))
            FeatureRow(
                mark = "∞",
                title = "Sınırsız geçmiş",
                body = "Ücretsiz sürümde son ${ProLimits.FREE_MONTH_COUNT} ay görünür; Pro'da tamamı."
            )
        }

        item {
            Spacer(Modifier.height(AppSpace.s4))
            FeatureRow(
                mark = "▦",
                title = "4×2 widget",
                body = "Ana ekranda ay toplamı, geçen aya göre değişim ve en çok " +
                    "harcanan ${WIDE_WIDGET_ROW_COUNT} kategori. Ücretsiz sürümdeki " +
                    "2×1 widget yalnızca toplamı gösterir."
            )
        }

        item {
            Spacer(Modifier.height(AppSpace.s4))
            Text(
                "Pro'nun açtığı şeyler şu an bunlar. Tasarımda duran CSV dışa aktarma " +
                    "ve kendi desenini yazma henüz yazılmadı — olmayan bir şeyin " +
                    "parası istenmiyor.",
                style = AppText.bodySmall,
                color = AppTheme.colors.onBackgroundMuted,
                modifier = Modifier.padding(horizontal = AppSpace.s6)
            )
        }

        item {
            Spacer(Modifier.height(AppSpace.s6))
            if (isPro) {
                ActiveCard()
            } else {
                PurchaseBlock(onPurchase = viewModel::purchase, onRestore = viewModel::restore)
            }
        }

        message?.let { text ->
            item {
                Spacer(Modifier.height(AppSpace.s4))
                NoteCard(accent = AppTheme.colors.warning, mark = "!", text = text)
            }
        }

        // Satin alma yolu olmadigi surece ucretsiz surumun siniri cihazda baska
        // turlu gorulemez. Yalnizca hata ayiklama derlemesinde - ana ekrandaki
        // `TestNotificationSeeder` dugmesiyle ayni gerekce.
        if (BuildConfig.DEBUG) {
            item {
                Spacer(Modifier.height(AppSpace.s5))
                DebugEntitlementToggle(isPro) { viewModel.setEntitlementForDebug(!isPro) }
            }
        }
    }
}

@Composable
private fun CloseBar(onClose: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = AppSpace.s6, vertical = AppSpace.s4),
        horizontalArrangement = Arrangement.End
    ) {
        Box(
            Modifier
                .size(AppSpace.s6)
                .clip(RoundedCornerShape(AppRadius.sm))
                .background(AppTheme.colors.surfaceMuted)
                .clickable(onClick = onClose)
                .semantics { contentDescription = "Kapat" },
            contentAlignment = Alignment.Center
        ) {
            Text(
                "✕",
                style = AppText.metaMono,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun ProBadge() {
    Box(
        Modifier
            .clip(RoundedCornerShape(AppRadius.sm))
            .background(AppTheme.colors.proAccent.copy(alpha = 0.14f))
            .border(1.dp, AppTheme.colors.proAccent.copy(alpha = 0.32f), RoundedCornerShape(AppRadius.sm))
            .padding(horizontal = AppSpace.s3, vertical = AppSpace.s2)
    ) {
        Text("BİLDİRİM BÜTÇE PRO", style = AppText.kicker, color = AppTheme.colors.proAccent)
    }
}

@Composable
private fun FeatureRow(mark: String, title: String, body: String) {
    Row(
        Modifier
            .padding(horizontal = AppSpace.s6)
            .fillMaxWidth()
            .clip(RoundedCornerShape(AppRadius.lg))
            .background(AppTheme.colors.surfaceMuted.copy(alpha = 0.5f))
            .padding(AppSpace.s4),
        horizontalArrangement = Arrangement.spacedBy(AppSpace.s3)
    ) {
        Box(
            Modifier
                .size(20.dp)
                .clip(RoundedCornerShape(AppRadius.xs))
                .background(AppTheme.colors.proAccent.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Text(mark, style = AppText.metaMono, color = AppTheme.colors.proAccent)
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = AppText.bodyLarge, color = MaterialTheme.colorScheme.onBackground)
            Spacer(Modifier.height(3.dp))
            Text(body, style = AppText.body, color = AppTheme.colors.onBackgroundMuted)
        }
    }
}

/**
 * Satin alma dugmesi ve altindaki gerekce.
 *
 * Dugme tiklanabilir ama soluk: tasarimdaki altin degrade "bu is calisiyor"
 * derdi. Tiklanamaz olsaydi da kullanici **neden** olmadigini hic ogrenemezdi -
 * dokunus, [ProViewModel.message] uzerinden sebebi ekrana getiriyor.
 */
@Composable
private fun PurchaseBlock(onPurchase: () -> Unit, onRestore: () -> Unit) {
    Column(Modifier.padding(horizontal = AppSpace.s6)) {
        Text("HENÜZ AÇILMADI", style = AppText.kicker, color = AppTheme.colors.onBackgroundMuted)
        Spacer(Modifier.height(AppSpace.s2))
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(AppRadius.md))
                .background(AppTheme.colors.proAccent.copy(alpha = 0.14f))
                .border(1.dp, AppTheme.colors.proAccent.copy(alpha = 0.32f), RoundedCornerShape(AppRadius.md))
                .clickable(onClick = onPurchase)
                .padding(horizontal = AppSpace.s4, vertical = AppSpace.s4),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Pro'ya geç", style = AppText.bodyLarge, color = AppTheme.colors.proAccent)
            // Fiyat Play Console'daki urunden okunur; urun yokken ekrana
            // yazilacak dogru bir sayi yok.
            Text(
                "fiyat Play'den gelecek",
                style = AppText.metaMono,
                color = AppTheme.colors.onBackgroundMuted
            )
        }
        Spacer(Modifier.height(AppSpace.s3))
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text(
                "Satın almayı geri yükle",
                style = AppText.labelChip,
                color = AppTheme.colors.onBackgroundMuted,
                modifier = Modifier.clickable(onClick = onRestore)
            )
        }
    }
}

/**
 * Yetki acikken gosterilen durum. Tasarimda karsiligi yok: satin almis bir
 * kullaniciya satin alma ekrani gostermenin anlami yok, ama ekran ProChip'ten
 * aciliyor ve Pro kullanicisi da oraya dokunabiliyor.
 */
@Composable
private fun ActiveCard() {
    Row(
        Modifier
            .padding(horizontal = AppSpace.s6)
            .fillMaxWidth()
            .clip(RoundedCornerShape(AppRadius.lg))
            .background(AppTheme.colors.proAccent.copy(alpha = 0.1f))
            .border(1.dp, AppTheme.colors.proAccent.copy(alpha = 0.3f), RoundedCornerShape(AppRadius.lg))
            .padding(AppSpace.s4),
        horizontalArrangement = Arrangement.spacedBy(AppSpace.s3),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("✓", style = AppText.bodyLarge, color = AppTheme.colors.proAccent)
        Text(
            "Pro etkin — tüm geçmiş açık.",
            style = AppText.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
private fun NoteCard(accent: Color, mark: String, text: String) {
    Row(
        Modifier
            .padding(horizontal = AppSpace.s6)
            .fillMaxWidth()
            .clip(RoundedCornerShape(AppRadius.md))
            .background(accent.copy(alpha = 0.07f))
            .border(1.dp, accent.copy(alpha = 0.2f), RoundedCornerShape(AppRadius.md))
            .padding(AppSpace.s3),
        horizontalArrangement = Arrangement.spacedBy(AppSpace.s3)
    ) {
        Text(mark, style = AppText.metaMono, color = accent)
        Text(text, style = AppText.bodySmall, color = AppTheme.colors.onBackgroundMuted)
    }
}

@Composable
private fun DebugEntitlementToggle(isPro: Boolean, onToggle: () -> Unit) {
    Text(
        if (isPro) "GELİŞTİRME: PRO'YU KAPAT" else "GELİŞTİRME: PRO'YU AÇ",
        style = AppText.kicker,
        color = AppTheme.colors.brandBright,
        modifier = Modifier
            .padding(horizontal = AppSpace.s6)
            .clickable(onClick = onToggle)
    )
}
