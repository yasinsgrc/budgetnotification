@file:OptIn(ExperimentalMaterial3Api::class)

package com.bildirimbutce.app.ui

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import android.widget.Toast
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bildirimbutce.app.BuildConfig
import com.bildirimbutce.app.data.db.ExpenseEntity
import com.bildirimbutce.app.debug.TestNotificationSeeder
import com.bildirimbutce.parser.Category
import com.bildirimbutce.app.util.NotificationAccess
import com.bildirimbutce.parser.Money
import com.bildirimbutce.parser.TxKind
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(viewModel: HomeViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val cursor by viewModel.cursor.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var permissionGranted by remember { mutableStateOf(NotificationAccess.isGranted(context)) }
    var editing by remember { mutableStateOf<ExpenseEntity?>(null) }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Bildirim Bütçe", fontWeight = FontWeight.SemiBold) })
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (!permissionGranted) {
                item {
                    PermissionCard(
                        onGrant = {
                            context.startActivity(NotificationAccess.settingsIntent())
                        },
                        onRecheck = { permissionGranted = NotificationAccess.isGranted(context) }
                    )
                }
            }

            item {
                MonthHeader(
                    label = cursor.label,
                    totalMinor = state.totalMinor,
                    onPrevious = viewModel::previousMonth,
                    onNext = viewModel::nextMonth
                )
            }

            if (BuildConfig.DEBUG) {
                item {
                    TextButton(onClick = {
                        scope.launch {
                            val added = TestNotificationSeeder.seed(context)
                            Toast.makeText(context, "$added test kaydı eklendi", Toast.LENGTH_SHORT).show()
                        }
                    }) { Text("Test bildirimi") }
                }
            }

            if (state.byCategory.isNotEmpty()) {
                item { CategoryBreakdown(state.byCategory, state.totalMinor) }
            }

            if (state.expenses.isEmpty()) {
                item { EmptyState(permissionGranted) }
            } else {
                items(state.expenses, key = { it.id }) { expense ->
                    ExpenseRow(expense) { editing = expense }
                }
            }
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
private fun PermissionCard(onGrant: () -> Unit, onRecheck: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(Modifier.padding(16.dp)) {
            Text("Bildirim erişimi kapalı", fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            Text(
                "Harcamaların otomatik yakalanması için bildirim erişimi gerekiyor. " +
                    "Uygulamanın internet izni yoktur; okunan metinler telefondan çıkmaz.",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onGrant) { Text("Ayarları aç") }
                TextButton(onClick = onRecheck) { Text("İzni verdim") }
            }
        }
    }
}

@Composable
private fun MonthHeader(
    label: String,
    totalMinor: Long,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    Card {
        Column(Modifier.padding(20.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onPrevious) { Text("‹") }
                Text(label, style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = onNext) { Text("›") }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "${Money.format(totalMinor)} ₺",
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold
            )
            Text("bu ayki toplam harcama", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun CategoryBreakdown(rows: List<Pair<Category, Long>>, totalMinor: Long) {
    Card {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Kategoriler", style = MaterialTheme.typography.titleSmall)
            rows.forEach { (category, amount) ->
                val ratio = if (totalMinor > 0) amount.toFloat() / totalMinor else 0f
                Column {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${category.emoji}  ${category.label}", style = MaterialTheme.typography.bodyMedium)
                        Text("${Money.format(amount)} ₺", style = MaterialTheme.typography.bodyMedium)
                    }
                    Spacer(Modifier.height(4.dp))
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth(ratio.coerceIn(0f, 1f))
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(MaterialTheme.colorScheme.primary)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ExpenseRow(expense: ExpenseEntity, onClick: () -> Unit) {
    val isRefund = expense.kind == TxKind.REFUND.name
    Card(onClick = onClick) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(Category.from(expense.category).emoji, fontSize = 22.sp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    expense.merchant ?: "Bilinmeyen işyeri",
                    fontWeight = FontWeight.Medium,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    dateFormat.format(Date(expense.occurredAt)) +
                        if (expense.merchant == null) "  •  dokunup düzeltin" else "",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Text(
                (if (isRefund) "+" else "−") + " ${Money.format(expense.amountMinor)} ₺",
                fontWeight = FontWeight.SemiBold,
                color = if (isRefund) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun EmptyState(permissionGranted: Boolean) {
    Column(Modifier.fillMaxWidth().padding(vertical = 48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(if (permissionGranted) "Henüz harcama yok" else "İzin bekleniyor", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(6.dp))
        Text(
            if (permissionGranted) "Bir banka bildirimi geldiğinde burada görünecek."
            else "Bildirim erişimi verildiğinde harcamalar otomatik listelenir.",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

private val dateFormat = SimpleDateFormat("d MMM • HH:mm", Locale("tr", "TR"))
