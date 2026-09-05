package com.bildirimbutce.app.ui

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bildirimbutce.app.ui.theme.AppRadius
import com.bildirimbutce.app.ui.theme.AppSpace
import com.bildirimbutce.app.ui.theme.AppText
import com.bildirimbutce.app.ui.theme.AppTheme
import com.bildirimbutce.parser.Category
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * D2 - elle harcama girisi.
 *
 * Bu ekran estetik bir ek degil, yedek yol: Play Console bildirim erisimini
 * reddederse geriye yayinlanabilir bir butce defteri kalmasi gerekiyor.
 *
 * Tek zorunlu alan tutar. Isyeri bos birakilabilir (liste "Bilinmeyen isyeri"
 * gosterir, bildirimden gelen kayitlarda da oyle), kategori bos birakilirsa
 * isyeri adindan tahmin edilir.
 */
@Composable
fun AddExpenseScreen(
    onDone: () -> Unit,
    viewModel: AddExpenseViewModel = viewModel()
) {
    val draft by viewModel.draft.collectAsStateWithLifecycle()
    val amountFocus = remember { FocusRequester() }

    // Ekran tek is icin aciliyor; klavye hazir gelsin, kullanici once tutari yazar.
    LaunchedEffect(Unit) { amountFocus.requestFocus() }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = AppSpace.s6)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(vertical = AppSpace.s4),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("ELLE HARCAMA EKLE", style = AppText.kicker, color = AppTheme.colors.onBackgroundMuted)
            Box(
                Modifier
                    .size(AppSpace.s6)
                    .clip(RoundedCornerShape(AppRadius.sm))
                    .background(AppTheme.colors.surfaceMuted)
                    .clickable(onClick = onDone),
                contentAlignment = Alignment.Center
            ) {
                Text("✕", style = AppText.metaMono, color = AppTheme.colors.onBackgroundMuted)
            }
        }

        Text("TUTAR", style = AppText.kicker, color = AppTheme.colors.onBackgroundMuted)
        Spacer(Modifier.height(AppSpace.s2))
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextField(
                value = draft.amountText,
                onValueChange = viewModel::setAmount,
                placeholder = {
                    Text(
                        "0,00",
                        style = AppText.displaySheet,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f)
                    )
                },
                singleLine = true,
                textStyle = AppText.displaySheet,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Next
                ),
                shape = RoundedCornerShape(AppRadius.md),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = AppTheme.colors.surfaceMuted,
                    unfocusedContainerColor = AppTheme.colors.surfaceMuted,
                    focusedIndicatorColor = AppTheme.colors.brandBright,
                    unfocusedIndicatorColor = MaterialTheme.colorScheme.outline
                ),
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(amountFocus)
            )
            Text(
                "₺",
                style = AppText.titleCard,
                color = AppTheme.colors.onBackgroundMuted,
                modifier = Modifier.padding(start = AppSpace.s3)
            )
        }

        Spacer(Modifier.height(AppSpace.s5))
        Text("İŞYERİ", style = AppText.kicker, color = AppTheme.colors.onBackgroundMuted)
        Spacer(Modifier.height(AppSpace.s2))
        TextField(
            value = draft.merchant,
            onValueChange = viewModel::setMerchant,
            placeholder = { Text("İsteğe bağlı", style = AppText.bodyLarge) },
            singleLine = true,
            textStyle = AppText.bodyLarge,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            shape = RoundedCornerShape(AppRadius.md),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = AppTheme.colors.surfaceMuted,
                unfocusedContainerColor = AppTheme.colors.surfaceMuted,
                focusedIndicatorColor = AppTheme.colors.brandBright,
                unfocusedIndicatorColor = MaterialTheme.colorScheme.outline
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(AppSpace.s5))
        Text("KATEGORİ", style = AppText.kicker, color = AppTheme.colors.onBackgroundMuted)
        Spacer(Modifier.height(AppSpace.s2))
        val selected = draft.effectiveCategory
        LazyRow(horizontalArrangement = Arrangement.spacedBy(AppSpace.s2)) {
            items(Category.entries.toList()) { category ->
                CategoryChip(
                    category = category,
                    selected = category == selected,
                    onClick = { viewModel.setCategory(category) }
                )
            }
        }
        if (draft.category == null) {
            Spacer(Modifier.height(AppSpace.s2))
            Text(
                "İşyeri adından tahmin edildi — yanlışsa dokun, düzelt.",
                style = AppText.bodySmall,
                color = AppTheme.colors.onBackgroundMuted
            )
        }

        Spacer(Modifier.height(AppSpace.s5))
        Text(
            "TARİH · ${dateTimeFormat.format(Date()).uppercase(Locale("tr", "TR"))}",
            style = AppText.metaMono,
            color = AppTheme.colors.onBackgroundMuted
        )

        Spacer(Modifier.height(AppSpace.s6))
        SaveButton(enabled = draft.canSave) { viewModel.save(onDone) }
    }
}

@Composable
private fun CategoryChip(category: Category, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .clip(RoundedCornerShape(AppRadius.md))
            .background(if (selected) AppTheme.colors.categoryTint(category) else AppTheme.colors.surfaceMuted)
            .border(
                1.dp,
                if (selected) AppTheme.colors.categoryTintBorder(category) else MaterialTheme.colorScheme.outline,
                RoundedCornerShape(AppRadius.md)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = AppSpace.s3, vertical = AppSpace.s2),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpace.s2)
    ) {
        Text(category.emoji, style = AppText.labelChip)
        Text(
            category.label,
            style = AppText.labelChip,
            color = if (selected) AppTheme.colors.categoryColor(category) else MaterialTheme.colorScheme.onBackground
        )
    }
}

/**
 * Tutar cozulemedigi surece buton soluk ve tiklanamaz. Alternatif, dokununca
 * sessizce hicbir sey yapmayan bir butondu: kullanici kaydettigini sanardi.
 */
@Composable
private fun SaveButton(enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AppRadius.md))
            .background(if (enabled) AppTheme.colors.brandBright else AppTheme.colors.surfaceMuted)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = AppSpace.s3),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "Kaydet",
            style = AppText.titleCard,
            color = if (enabled) MaterialTheme.colorScheme.background else AppTheme.colors.onBackgroundMuted
        )
    }
}

private val dateTimeFormat = SimpleDateFormat("d MMM HH:mm", Locale("tr", "TR"))
