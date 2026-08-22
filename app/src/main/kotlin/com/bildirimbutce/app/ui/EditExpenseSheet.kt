@file:OptIn(ExperimentalMaterial3Api::class)

package com.bildirimbutce.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bildirimbutce.app.data.db.ExpenseEntity
import com.bildirimbutce.app.ui.theme.AppRadius
import com.bildirimbutce.app.ui.theme.AppSpace
import com.bildirimbutce.app.ui.theme.AppText
import com.bildirimbutce.app.ui.theme.AppTheme
import com.bildirimbutce.parser.Category
import com.bildirimbutce.parser.Money
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Duzeltme sayfasi. Her kayit duzeltilebilir olmali: ayristirma asla %100
 * dogru olmayacak ve kullanicinin duzeltebildigi bir hata, guveni yikmaz.
 */
@Composable
fun EditExpenseSheet(
    expense: ExpenseEntity,
    onDismiss: () -> Unit,
    onCategory: (Category) -> Unit,
    onMerchant: (String) -> Unit,
    onDelete: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    var merchant by remember { mutableStateOf(expense.merchant.orEmpty()) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(Modifier.padding(horizontal = AppSpace.s5).padding(bottom = AppSpace.s8)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("İŞLEMİ DÜZELT", style = AppText.kicker, color = AppTheme.colors.onBackgroundMuted)
                Box(
                    Modifier
                        .size(AppSpace.s6)
                        .clip(RoundedCornerShape(AppRadius.sm))
                        .background(AppTheme.colors.surfaceMuted)
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    Text("✕", style = AppText.metaMono, color = AppTheme.colors.onBackgroundMuted)
                }
            }

            Spacer(Modifier.height(AppSpace.s4))
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(AppSpace.s1)) {
                Text(Money.format(expense.amountMinor), style = AppText.displaySheet, color = MaterialTheme.colorScheme.onBackground)
                Text(
                    expense.currency,
                    style = AppText.titleCard,
                    color = AppTheme.colors.onBackgroundMuted,
                    modifier = Modifier.padding(top = AppSpace.s1)
                )
            }
            Spacer(Modifier.height(AppSpace.s1))
            Text(
                "${dateTimeFormat.format(Date(expense.occurredAt)).uppercase(Locale("tr", "TR"))} · ${expense.kindLabel()}",
                style = AppText.metaMono,
                color = AppTheme.colors.onBackgroundMuted
            )

            Spacer(Modifier.height(AppSpace.s5))
            Text("İŞYERİ", style = AppText.kicker, color = AppTheme.colors.onBackgroundMuted)
            Spacer(Modifier.height(AppSpace.s2))
            TextField(
                value = merchant,
                onValueChange = { merchant = it },
                placeholder = { Text("Bilinmeyen işyeri", style = AppText.bodyLarge) },
                singleLine = true,
                textStyle = AppText.bodyLarge,
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
            LazyRow(horizontalArrangement = Arrangement.spacedBy(AppSpace.s2)) {
                items(Category.entries.toList()) { category ->
                    val selected = category.name == expense.category
                    Row(
                        Modifier
                            .clip(RoundedCornerShape(AppRadius.md))
                            .background(if (selected) AppTheme.colors.categoryTint(category) else AppTheme.colors.surfaceMuted)
                            .border(
                                1.dp,
                                if (selected) AppTheme.colors.categoryTintBorder(category) else MaterialTheme.colorScheme.outline,
                                RoundedCornerShape(AppRadius.md)
                            )
                            .clickable { onCategory(category) }
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
            }

            Spacer(Modifier.height(AppSpace.s3))
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(AppRadius.md))
                    .background(AppTheme.colors.brandBright.copy(alpha = 0.07f))
                    .padding(AppSpace.s3),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppSpace.s2)
            ) {
                Text("↺", style = AppText.metaMono, color = AppTheme.colors.brandBright, fontWeight = FontWeight.Bold)
                Text(
                    "Bu işyerini bir daha sormam — kuralı öğreniyorum.",
                    style = AppText.body,
                    color = AppTheme.colors.onBackgroundMuted
                )
            }

            Spacer(Modifier.height(AppSpace.s4))
            Column(
                Modifier
                    .border(width = 1.dp, color = MaterialTheme.colorScheme.outline)
                    .padding(start = AppSpace.s3)
            ) {
                Text("kaynak  ${expense.sourceApp}", style = AppText.metaMono, color = AppTheme.colors.onBackgroundMuted)
                Text("desen   ${expense.patternId}", style = AppText.metaMono, color = AppTheme.colors.onBackgroundMuted)
                Text("güven   ${"%.2f".format(expense.confidence)}", style = AppText.metaMono, color = AppTheme.colors.onBackgroundMuted)
            }

            Spacer(Modifier.height(AppSpace.s4))
            Row(horizontalArrangement = Arrangement.spacedBy(AppSpace.s3), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(AppRadius.md))
                        .background(AppTheme.colors.brandBright)
                        .clickable { onMerchant(merchant) }
                        .padding(vertical = AppSpace.s3),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Kaydet", style = AppText.titleCard, color = MaterialTheme.colorScheme.background)
                }
                Box(
                    Modifier
                        .size(AppSpace.s8 + AppSpace.s1)
                        .clip(RoundedCornerShape(AppRadius.md))
                        .border(1.dp, AppTheme.colors.danger.copy(alpha = 0.38f), RoundedCornerShape(AppRadius.md))
                        .clickable(onClick = onDelete),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🗑", style = AppText.bodyLarge, color = AppTheme.colors.danger)
                }
            }
        }
    }
}

private fun ExpenseEntity.kindLabel(): String =
    if (kind == "REFUND") "İADE" else "HARCAMA"

private val dateTimeFormat = SimpleDateFormat("d MMM HH:mm", Locale("tr", "TR"))
