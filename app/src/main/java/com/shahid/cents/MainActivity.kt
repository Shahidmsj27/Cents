package com.shahid.cents

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val LocalAppColors = staticCompositionLocalOf { AppColorPalettes.green }

@Composable
private fun CentsTheme(content: @Composable () -> Unit) {
    val theme by ThemeManager.currentTheme.collectAsState()
    val isDark = when (theme) {
        AppTheme.System -> isSystemInDarkTheme()
        AppTheme.Light -> false
        AppTheme.Dark -> true
        else -> isSystemInDarkTheme()
    }
    val scheme = ThemePalettes.scheme(theme, isDark)
    val appColors = AppColorPalettes.colors(theme, isDark)
    CompositionLocalProvider(LocalAppColors provides appColors) {
        MaterialTheme(colorScheme = scheme) {
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                content()
            }
        }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { CentsTheme { CentsScreen() } }
    }
}

@Composable
private fun CentsScreen(viewModel: MainViewModel = viewModel()) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var hasSmsPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED)
    }
    var scanMessage by remember { mutableStateOf<String?>(null) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        hasSmsPermission = result[Manifest.permission.READ_SMS] == true && result[Manifest.permission.RECEIVE_SMS] == true
    }
    var tagTransaction by remember { mutableStateOf<TransactionEntity?>(null) }
    var showDateRangeDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Cents", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black)
                    Text(
                        "Private SMS-based expense tracking for your device",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TextButton(onClick = { showSettingsDialog = true }) {
                    Text("\u2699\uFE0F", style = MaterialTheme.typography.titleLarge)
                }
            }
        }

        item {
            DateFilterCard(
                uiState = uiState,
                onSelectFilter = { filter ->
                    if (filter == DateFilter.Custom) showDateRangeDialog = true
                    else viewModel.selectFilter(filter)
                },
                onPrevious = viewModel::previousPeriod,
                onNext = viewModel::nextPeriod
            )
        }

        item {
            PermissionCard(
                hasSmsPermission = hasSmsPermission,
                isScanning = uiState.isScanning,
                periodLabel = uiState.dateWindow.label(),
                scanMessage = uiState.lastScanMessage ?: scanMessage,
                onRequestPermission = {
                    permissionLauncher.launch(arrayOf(Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS))
                },
                onScan = { viewModel.scanSelectedPeriod { message -> scanMessage = message } }
            )
        }

        item {
            SummaryCards(
                uiState = uiState,
                onSpentClick = { viewModel.toggleTypeFilter(TransactionFilter.Spent) },
                onReceivedClick = { viewModel.toggleTypeFilter(TransactionFilter.Received) }
            )
        }

        item {
            CategoryBreakdown(
                summaries = uiState.categorySummaries,
                selectedCategory = uiState.selectedCategory,
                onCategorySelected = viewModel::selectCategory,
                onClearCategory = viewModel::clearCategory
            )
        }

        item {
            val title = when {
                uiState.selectedTypeFilter == TransactionFilter.Received -> "Received"
                uiState.selectedTypeFilter == TransactionFilter.Spent -> "Spent"
                uiState.selectedCategory != null -> uiState.selectedCategory!!
                else -> "Transactions"
            }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "$title \u00B7 ${uiState.visibleTransactions.size} entries",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                if (uiState.visibleTransactions.isNotEmpty()) {
                    Box {
                        Text(
                            "Sort: ${uiState.sortOrder.label} \u25BC",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clickable(onClick = { showSortMenu = true })
                        )
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            SortOrder.entries.forEach { order ->
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            RadioButton(
                                                selected = uiState.sortOrder == order,
                                                onClick = null
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Text(order.label)
                                        }
                                    },
                                    onClick = {
                                        viewModel.setSortOrder(order)
                                        showSortMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        if (uiState.visibleTransactions.isEmpty()) {
            item { EmptyState("No transactions in this period yet.") }
        } else {
            items(uiState.visibleTransactions, key = { it.id }) { transaction ->
                TransactionRow(transaction = transaction, onClick = { tagTransaction = transaction })
            }
        }
    }

    if (showDateRangeDialog) {
        CustomDateRangeDialog(
            onApply = { start, end ->
                viewModel.setCustomRange(start, end)
                showDateRangeDialog = false
            },
            onDismiss = { showDateRangeDialog = false }
        )
    }

    if (showSettingsDialog) {
        SettingsDialog(onDismiss = { showSettingsDialog = false })
    }

    tagTransaction?.let { transaction ->
        TagDialog(
            transaction = transaction,
            allCategories = uiState.allCategories,
            onSave = { txn, newCategory, applyToAll ->
                viewModel.recategorizeTransaction(txn, newCategory, applyToAll)
                tagTransaction = null
            },
            onDismiss = { tagTransaction = null }
        )
    }
}

@Composable
private fun SettingsDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val currentTheme by ThemeManager.currentTheme.collectAsState()

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
        title = { Text("Theme") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                AppTheme.entries.forEach { theme ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { ThemeManager.set(context, theme) }
                            .padding(vertical = 8.dp)
                    ) {
                        RadioButton(
                            selected = currentTheme == theme,
                            onClick = { ThemeManager.set(context, theme) }
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(theme.label, fontWeight = FontWeight.Medium)
                            Text(
                                when (theme) {
                                    AppTheme.System -> "Follows device setting"
                                    AppTheme.Light -> "Always light mode"
                                    AppTheme.Dark -> "Always dark mode"
                                    AppTheme.Monochrome -> "Grayscale palette"
                                    AppTheme.Forest -> "Earthy green tones"
                                    AppTheme.Ocean -> "Blue tones"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomDateRangeDialog(
    onApply: (Long, Long) -> Unit,
    onDismiss: () -> Unit
) {
    var startMs by remember { mutableStateOf<Long?>(null) }
    var step by remember { mutableStateOf<Int?>(1) }

    if (step == 1) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = startMs ?: System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(onClick = {
                    startMs = state.selectedDateMillis
                    step = 2
                }) { Text("Next") }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
        ) {
            Text("Select start date", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(start = 24.dp, top = 8.dp))
            DatePicker(state = state)
        }
    }

    if (step == 2) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(onClick = {
                    val end = state.selectedDateMillis ?: return@TextButton
                    step = null
                    onApply(startMs!!, end + 86400000L)
                }) { Text("Apply") }
            },
            dismissButton = { TextButton(onClick = { step = 1 }) { Text("Back") } }
        ) {
            Text("Select end date", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(start = 24.dp, top = 8.dp))
            DatePicker(state = state)
        }
    }
}

@Composable
private fun DateFilterCard(
    uiState: MainUiState,
    onSelectFilter: (DateFilter) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("View", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                DateFilter.entries.forEach { filter ->
                    val selected = uiState.dateWindow.filter == filter
                    FilterChip(
                        label = filter.label,
                        selected = selected,
                        onClick = { onSelectFilter(filter) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            if (uiState.dateWindow.filter != DateFilter.Custom) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(onClick = onPrevious) { Text("Prev") }
                    Text(uiState.dateWindow.label(), fontWeight = FontWeight.Bold)
                    OutlinedButton(onClick = onNext) { Text("Next") }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(uiState.dateWindow.label(), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun FilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isCustom = label == "Custom"
    Surface(
        modifier = modifier.height(38.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(99.dp),
        color = when {
            selected -> MaterialTheme.colorScheme.primary
            isCustom -> MaterialTheme.colorScheme.surface
            else -> MaterialTheme.colorScheme.surfaceVariant
        },
        border = BorderStroke(
            1.dp,
            when {
                selected -> MaterialTheme.colorScheme.primary
                isCustom -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.outline
            }
        )
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 8.dp)) {
            Text(
                label,
                color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun PermissionCard(
    hasSmsPermission: Boolean,
    isScanning: Boolean,
    periodLabel: String,
    scanMessage: String?,
    onRequestPermission: () -> Unit,
    onScan: () -> Unit
) {
    Card(colors = CardDefaults.cardColors(containerColor = LocalAppColors.current.darkCardBg), shape = RoundedCornerShape(24.dp)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("SMS Sync", color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                "Cents scans only the selected period locally, extracts expenses, and stores parsed transactions on-device.",
                color = Color(0xFFD7DDD5)
            )
            Button(enabled = !isScanning, onClick = if (hasSmsPermission) onScan else onRequestPermission) {
                Text(if (isScanning) "Scanning..." else if (hasSmsPermission) "Sync $periodLabel" else "Allow SMS access")
            }
            if (isScanning) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = Color(0xFF2B363C)
                    )
                    Text("Reading SMS for $periodLabel locally.", color = Color(0xFFD7DDD5))
                }
            }
            if (scanMessage != null) Text(scanMessage, color = Color(0xFFB8E986))
        }
    }
}

@Composable
private fun CategoryBreakdown(
    summaries: List<CategorySummary>,
    selectedCategory: String?,
    onCategorySelected: (String) -> Unit,
    onClearCategory: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Expense Categories", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                if (selectedCategory != null) {
                    Text(
                        "Show all",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable(onClick = onClearCategory)
                    )
                }
            }
            if (summaries.isEmpty()) {
                Text("No categorized expenses in this period yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                summaries.forEach { summary ->
                    CategoryRow(
                        summary = summary,
                        selected = selectedCategory == summary.category,
                        onClick = { onCategorySelected(summary.category) }
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryRow(summary: CategorySummary, selected: Boolean, onClick: () -> Unit) {
    val ac = LocalAppColors.current
    Column(
        modifier = Modifier.fillMaxWidth()
            .background(if (selected) ac.selectedCategoryBg else Color.Transparent, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text(summary.category, fontWeight = FontWeight.Bold)
                Text("${summary.transactionCount} transactions", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
            Text(formatMoney(summary.amountPaise), fontWeight = FontWeight.Black)
        }
        Box(
            modifier = Modifier.fillMaxWidth().height(8.dp).background(ac.progressBarBg, RoundedCornerShape(99.dp))
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(summary.share.coerceIn(0.03f, 1f))
                    .height(8.dp)
                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(99.dp))
            )
        }
    }
}

@Composable
private fun SummaryCards(
    uiState: MainUiState,
    onSpentClick: () -> Unit,
    onReceivedClick: () -> Unit
) {
    val ac = LocalAppColors.current
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        MetricCard(
            title = "Spent",
            value = formatMoney(uiState.debitTotalPaise),
            color = ac.spentCardBg,
            selected = uiState.selectedTypeFilter == TransactionFilter.Spent,
            onClick = onSpentClick,
            modifier = Modifier.weight(1f)
        )
        MetricCard(
            title = "Received",
            value = formatMoney(uiState.creditTotalPaise),
            color = ac.incomeCardBg,
            selected = uiState.selectedTypeFilter == TransactionFilter.Received,
            onClick = onReceivedClick,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun MetricCard(
    title: String,
    value: String,
    color: Color,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ac = LocalAppColors.current
    Card(
        modifier = modifier.clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = color),
        shape = RoundedCornerShape(20.dp),
        border = if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        elevation = CardDefaults.cardElevation(defaultElevation = if (selected) 3.dp else 0.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun EmptyState(message: String) {
    Box(
        modifier = Modifier.fillMaxWidth()
            .background(LocalAppColors.current.emptyStateBg, RoundedCornerShape(20.dp))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun TransactionRow(transaction: TransactionEntity, onClick: () -> Unit) {
    val ac = LocalAppColors.current
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text(transaction.merchant ?: transaction.sender, fontWeight = FontWeight.Bold)
                    Text(
                        "${transaction.category} - ${transaction.accountHint ?: "Account unknown"}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = when (transaction.type) {
                        TransactionType.Credit, TransactionType.Reversal -> "+${formatMoney(transaction.amountPaise)}"
                        else -> "-${formatMoney(transaction.amountPaise)}"
                    },
                    color = when (transaction.type) {
                        TransactionType.Credit, TransactionType.Reversal -> ac.positiveAmount
                        else -> ac.negativeAmount
                    },
                    fontWeight = FontWeight.Black
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                formatDate(transaction.smsTimestamp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun TagDialog(
    transaction: TransactionEntity,
    allCategories: List<String>,
    onSave: (TransactionEntity, String, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var category by remember(transaction.id) { mutableStateOf(transaction.category) }
    var applyToAll by remember(transaction.id) { mutableStateOf(false) }
    val topCategories = remember(allCategories) {
        val preferred = listOf("Food", "Transport", "Bills", "Shopping", "Investment", "Groceries", "Income", "Salary", "Unknown", "EMI", "Rent", "Healthcare", "Entertainment", "Fuel", "ATM")
        preferred.filter { it in allCategories } + (allCategories - preferred.toSet())
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { Button(onClick = { onSave(transaction, category, applyToAll) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        title = { Text("Tag Transaction") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(transaction.merchant ?: transaction.sender, fontWeight = FontWeight.Bold)
                Text("Amount: ${formatMoney(transaction.amountPaise)}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Current: ${transaction.category}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                HorizontalDivider()

                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Quick select:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    topCategories.take(12).chunked(3).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { cat ->
                                FilterChip(
                                    label = cat,
                                    selected = category == cat,
                                    onClick = { category = cat },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                HorizontalDivider()
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = applyToAll, onCheckedChange = { applyToAll = it })
                    Text(
                        if (transaction.merchant != null) "Apply to all past & future from ${transaction.merchant}"
                        else "Apply to all past & future from ${transaction.sender}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    )
}

private fun formatMoney(paise: Long): String {
    val formatter = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
    return formatter.format(paise / 100.0)
}

private fun formatDate(timestamp: Long): String = SimpleDateFormat("dd MMM yyyy, h:mm a", Locale.ENGLISH).format(Date(timestamp))
