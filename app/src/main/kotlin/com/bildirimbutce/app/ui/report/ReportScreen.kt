package com.bildirimbutce.app.ui.report

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bildirimbutce.app.ui.MonthCursor
import com.bildirimbutce.app.ui.theme.AppRadius
import com.bildirimbutce.app.ui.theme.AppSpace
import com.bildirimbutce.app.ui.theme.AppText
import com.bildirimbutce.app.ui.theme.AppTheme
import com.bildirimbutce.parser.Money
import java.util.Locale

/** En yuksek cubugun boyu; kalanlar buna oranlaniyor. */
private val MONTH_BAR_MAX = 96.dp
private val WEEKDAY_BAR_MAX = 44.dp

/**
 * Sifir da bir cevaptir: cubuk tamamen kaybolursa ay grafikten silinmis gibi
 * gorunur, ince bir cizgi "bu ay bos" der.
 */
private val BAR_MIN = 3.dp

/**
 * C1 - aylik rapor.
 *
 * Ana ekrandaki "RAPOR →" buradan aciliyor ve hangi ayin raporu oldugu adres
 * uzerinden ([year]/[month]) geliyor: ekran kendi basina "bu ay"i varsaysaydi
 * kullanici gecmis bir aya bakarken rapor sessizce baska bir aya atlardi.
 */
@Composable
fun ReportScreen(
    year: Int,
    month: Int,
    onBack: () -> Unit,
    viewModel: ReportViewModel = viewModel()
) {
    LaunchedEffect(year, month) { viewModel.setMonth(year, month) }
    val state by viewModel.state.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = AppSpace.s8)
    ) {
        item { ReportTopBar(MonthCursor(year, month).label, onBack) }
        item { MonthChart(state.months) }

        if (state.isEmpty) {
            item { EmptyReport() }
        } else {
            item { StatTiles(state) }
            item { WeekdayRhythm(state.weekdays, state.weekdayPeak) }
            if (state.topMerchants.isNotEmpty()) {
                item { SectionKicker("EN ÇOK GİDEN YERLER") }
                itemsIndexed(state.topMerchants) { index, row ->
                    MerchantRowItem(rank = index + 1, row = row)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                }
            }
        }
    }
}

@Composable
private fun ReportTopBar(monthLabel: String, onBack: () -> Unit) {
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
        Text("Rapor", style = AppText.titleCard, color = MaterialTheme.colorScheme.onBackground)
        Text(
            monthLabel.uppercase(Locale("tr", "TR")),
            style = AppText.kicker,
            color = AppTheme.colors.onBackgroundMuted
        )
    }
}

/**
 * Son [REPORT_MONTH_COUNT] ayin cubuk grafigi.
 *
 * Cubuklar birbirine gore oranli, mutlak bir eksene degil: rapor "gecen aya
 * gore ne oldu" sorusuna bakiyor, uygulamada mutlak bir butce cizgisi yok.
 */
@Composable
private fun MonthChart(months: List<MonthBar>) {
    val max = months.maxOfOrNull { it.totalMinor }?.coerceAtLeast(1L) ?: 1L

    Column(Modifier.padding(horizontal = AppSpace.s6, vertical = AppSpace.s3)) {
        Text("SON $REPORT_MONTH_COUNT AY", style = AppText.kicker, color = AppTheme.colors.onBackgroundMuted)
        Spacer(Modifier.height(AppSpace.s4))
        Row(
            Modifier
                .fillMaxWidth()
                .height(MONTH_BAR_MAX + 46.dp),
            horizontalArrangement = Arrangement.spacedBy(AppSpace.s2),
            verticalAlignment = Alignment.Bottom
        ) {
            months.forEach { bar ->
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(Modifier.weight(1f))
                    Text(
                        Money.format(bar.totalMinor).substringBefore(','),
                        style = AppText.metaMono,
                        color = if (bar.isSelected) AppTheme.colors.brandBright
                        else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
                    )
                    Spacer(Modifier.height(AppSpace.s2))
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(barHeight(bar.totalMinor, max, MONTH_BAR_MAX))
                            .clip(RoundedCornerShape(AppRadius.xs))
                            .background(
                                if (bar.isSelected) AppTheme.colors.brandBright
                                else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.13f)
                            )
                    )
                    Spacer(Modifier.height(AppSpace.s2))
                    Text(
                        bar.cursor.shortLabel,
                        style = AppText.kicker,
                        color = if (bar.isSelected) AppTheme.colors.brandBright else AppTheme.colors.onBackgroundMuted
                    )
                }
            }
        }
    }
}

@Composable
private fun StatTiles(state: ReportUiState) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = AppSpace.s6, vertical = AppSpace.s3),
        horizontalArrangement = Arrangement.spacedBy(AppSpace.s2)
    ) {
        StatTile(
            kicker = "GÜN ORTALAMASI",
            value = Money.format(state.dailyAverageMinor),
            sub = "₺ / gün · ${state.daysCounted} gün"
        )
        StatTile(
            kicker = "EN YÜKSEK GÜN",
            value = state.peakDay?.let { Money.format(it.totalMinor) } ?: "—",
            sub = state.peakDay
                ?.let { "${it.dayOfMonth} ${MonthCursor.MONTHS[state.cursor.month]}" }
                ?: "harcama yok",
            valueColor = AppTheme.colors.danger
        )
        StatTile(
            kicker = "İŞLEM SAYISI",
            value = "${state.expenseCount + state.refundCount}",
            sub = "${state.expenseCount} harcama, ${state.refundCount} iade"
        )
    }
}

@Composable
private fun RowScope.StatTile(
    kicker: String,
    value: String,
    sub: String,
    valueColor: Color = Color.Unspecified
) {
    Column(
        Modifier
            .weight(1f)
            .clip(RoundedCornerShape(AppRadius.md))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(AppRadius.md))
            .padding(AppSpace.s3)
    ) {
        Text(kicker, style = AppText.metaMono, color = AppTheme.colors.onBackgroundMuted)
        Spacer(Modifier.height(AppSpace.s2))
        Text(
            value,
            style = AppText.amountRow,
            color = if (valueColor == Color.Unspecified) MaterialTheme.colorScheme.onBackground else valueColor
        )
        Spacer(Modifier.height(AppSpace.s1))
        Text(sub, style = AppText.bodySmall, color = AppTheme.colors.onBackgroundMuted)
    }
}

/**
 * Haftanin ritmi.
 *
 * Cumle yalnizca tepe gun gercekten ayrisiyorsa yaziliyor ([WeekdayPeak] null
 * gelebilir): "her gun aynisin" demenin degeri yok, uydurmanin zarari var.
 */
@Composable
private fun WeekdayRhythm(days: List<WeekdayBar>, peak: WeekdayPeak?) {
    val max = days.maxOfOrNull { it.totalMinor }?.coerceAtLeast(1L) ?: 1L

    Column(Modifier.padding(horizontal = AppSpace.s6, vertical = AppSpace.s3)) {
        Text("HAFTANIN RİTMİ", style = AppText.kicker, color = AppTheme.colors.onBackgroundMuted)
        Spacer(Modifier.height(AppSpace.s4))
        Row(
            Modifier
                .fillMaxWidth()
                .height(WEEKDAY_BAR_MAX + 20.dp),
            horizontalArrangement = Arrangement.spacedBy(AppSpace.s2),
            verticalAlignment = Alignment.Bottom
        ) {
            days.forEach { day ->
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(Modifier.weight(1f))
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(barHeight(day.totalMinor, max, WEEKDAY_BAR_MAX))
                            .clip(RoundedCornerShape(AppRadius.xs))
                            .background(
                                if (day.isPeak) AppTheme.colors.warningBright
                                else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.13f)
                            )
                    )
                    Spacer(Modifier.height(AppSpace.s2))
                    Text(day.label, style = AppText.metaMono, color = AppTheme.colors.onBackgroundMuted)
                }
            }
        }
        if (peak != null) {
            Spacer(Modifier.height(AppSpace.s3))
            Text(
                buildAnnotatedString {
                    append("${peak.label} günleri ortalamanın ")
                    withStyle(SpanStyle(color = AppTheme.colors.warningBright)) {
                        append("%${peak.percentAboveAverage} üstünde")
                    }
                    append(" harcıyorsun.")
                },
                style = AppText.bodySmall,
                color = AppTheme.colors.onBackgroundMuted
            )
        }
    }
}

@Composable
private fun SectionKicker(text: String) {
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
private fun MerchantRowItem(rank: Int, row: MerchantRow) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = AppSpace.s6, vertical = AppSpace.s3),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            rank.toString().padStart(2, '0'),
            style = AppText.metaMono,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.34f),
            modifier = Modifier.width(22.dp)
        )
        Column(Modifier.weight(1f)) {
            Text(row.name, style = AppText.bodyLarge, color = MaterialTheme.colorScheme.onBackground)
            Spacer(Modifier.height(3.dp))
            Text(
                "${row.category.label.uppercase(Locale("tr", "TR"))} · ${row.count} İŞLEM",
                style = AppText.metaMono,
                color = AppTheme.colors.onBackgroundMuted
            )
        }
        Text(
            "${Money.format(row.totalMinor)} ₺",
            style = AppText.amountRow,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
private fun EmptyReport() {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = AppSpace.s6, vertical = AppSpace.s5)
    ) {
        Text("Bu ayda kayıt yok", style = AppText.titleCard, color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.height(AppSpace.s2))
        Text(
            "Rapor tek bir aya bakıyor. Yukarıdaki çubuklar diğer ayları gösteriyor; " +
                "dolu bir aya geçmek için ana ekrandan ayı değiştir.",
            style = AppText.body,
            color = AppTheme.colors.onBackgroundMuted
        )
    }
}

/** Oranli cubuk boyu. Net negatif (iade agirlikli) aylar sifir kabul edilir. */
private fun barHeight(valueMinor: Long, maxMinor: Long, maxHeight: Dp): Dp {
    val ratio = (valueMinor.toFloat() / maxMinor).coerceIn(0f, 1f)
    return (maxHeight * ratio).coerceAtLeast(BAR_MIN)
}
