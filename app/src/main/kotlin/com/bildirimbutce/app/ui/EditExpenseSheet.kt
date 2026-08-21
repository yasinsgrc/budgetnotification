@file:OptIn(ExperimentalMaterial3Api::class)

package com.bildirimbutce.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bildirimbutce.app.data.db.ExpenseEntity
import com.bildirimbutce.parser.Category
import com.bildirimbutce.parser.Money

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

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 32.dp)) {
            Text("${Money.format(expense.amountMinor)} ${expense.currency}",
                style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = merchant,
                onValueChange = { merchant = it },
                label = { Text("İşyeri") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))
            Text("Kategori", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(Category.entries.toList()) { category ->
                    FilterChip(
                        selected = category.name == expense.category,
                        onClick = { onCategory(category) },
                        label = { Text("${category.emoji} ${category.label}") }
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
            Text(
                "Kaynak: ${expense.sourceApp}  •  desen: ${expense.patternId}",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(Modifier.height(16.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { onMerchant(merchant) }, modifier = Modifier.weight(1f)) {
                    Text("Kaydet")
                }
                TextButton(onClick = onDelete) { Text("Sil") }
            }
        }
    }
}
