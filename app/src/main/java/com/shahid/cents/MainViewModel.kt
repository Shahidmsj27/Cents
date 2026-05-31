package com.shahid.cents

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class TransactionFilter { Spent, Received }
enum class SortOrder(val label: String) {
    DateDesc("Newest first"),
    DateAsc("Oldest first"),
    AmountDesc("Highest amount"),
    AmountAsc("Lowest amount")
}

data class MainUiState(
    val transactions: List<TransactionEntity> = emptyList(),
    val visibleTransactions: List<TransactionEntity> = emptyList(),
    val categorySummaries: List<CategorySummary> = emptyList(),
    val allCategories: List<String> = emptyList(),
    val debitTotalPaise: Long = 0,
    val creditTotalPaise: Long = 0,
    val dateWindow: DateWindow = DateWindow(),
    val selectedCategory: String? = null,
    val selectedTypeFilter: TransactionFilter? = null,
    val sortOrder: SortOrder = SortOrder.DateDesc,
    val isScanning: Boolean = false,
    val lastScanMessage: String? = null
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as CentsApp
    private val scanner = SmsScanner(application, app.repository)
    private val scanState = MutableStateFlow(ScanState())
    private val dateWindow = MutableStateFlow(DateWindow())
    private val selectedCategory = MutableStateFlow<String?>(null)
    private val selectedTypeFilter = MutableStateFlow<TransactionFilter?>(null)
    private val sortOrder = MutableStateFlow(SortOrder.DateDesc)

    val uiState: StateFlow<MainUiState> = combine(
            combine(app.repository.transactions, scanState, dateWindow) { t, s, w -> Triple(t, s, w) },
            combine(selectedCategory, selectedTypeFilter, sortOrder) { c, tf, s -> Triple(c, tf, s) }
        ) { (transactions, scan, window), (category, typeFilter, sort) ->
            val filteredTransactions = transactions.filter { it.smsTimestamp >= window.startMillis && it.smsTimestamp < window.endMillis }
            val byCategory = if (category == null) filteredTransactions else filteredTransactions.filter { it.category.equals(category, ignoreCase = true) }
            val typeFiltered = when (typeFilter) {
                TransactionFilter.Spent -> byCategory.filter { it.type == TransactionType.Debit || it.type == TransactionType.Bill }
                TransactionFilter.Received -> byCategory.filter { it.type == TransactionType.Credit || it.type == TransactionType.Reversal }
                null -> byCategory
            }
            val visibleTransactions = when (sort) {
                SortOrder.DateDesc -> typeFiltered.sortedByDescending { it.smsTimestamp }
                SortOrder.DateAsc -> typeFiltered.sortedBy { it.smsTimestamp }
                SortOrder.AmountDesc -> typeFiltered.sortedByDescending { it.amountPaise }
                SortOrder.AmountAsc -> typeFiltered.sortedBy { it.amountPaise }
            }
            val periodOutflows = filteredTransactions.filter { it.type != TransactionType.Credit }
            val periodDebitTotal = periodOutflows.sumOf { it.amountPaise }
            val outflows = visibleTransactions.filter { it.type != TransactionType.Credit }
            val inflows = visibleTransactions.filter { it.type == TransactionType.Credit }
            MainUiState(
                transactions = filteredTransactions,
                visibleTransactions = visibleTransactions,
                allCategories = filteredTransactions.map { it.category }.distinct().sorted(),
                categorySummaries = periodOutflows
                    .groupBy { it.category }
                    .map { (cat, catTransactions) ->
                        val amount = catTransactions.sumOf { it.amountPaise }
                        CategorySummary(
                            category = cat,
                            amountPaise = amount,
                            transactionCount = catTransactions.size,
                            share = if (periodDebitTotal == 0L) 0f else amount.toFloat() / periodDebitTotal.toFloat()
                        )
                    }
                    .sortedByDescending { it.amountPaise },
                debitTotalPaise = outflows.sumOf { it.amountPaise },
                creditTotalPaise = inflows.sumOf { it.amountPaise },
                dateWindow = window,
                selectedCategory = category,
                selectedTypeFilter = typeFilter,
                sortOrder = sort,
                isScanning = scan.isScanning,
                lastScanMessage = scan.lastScanMessage
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MainUiState())

    init {
        viewModelScope.launch {
            app.repository.cleanupNonTransactionalNotices()
        }
    }

    fun scanSelectedPeriod(onDone: (String) -> Unit) {
        viewModelScope.launch {
            scanState.update { it.copy(isScanning = true) }
            val window = dateWindow.value
            val deleted = app.repository.deleteInRange(window.startMillis, window.endMillis)
            val result = scanner.scanRange(window.startMillis, window.endMillis)
            val message = "${window.label()}: refreshed $deleted old rows. ${result.message()}"
            scanState.update { it.copy(isScanning = false, lastScanMessage = message) }
            onDone(message)
        }
    }

    fun selectFilter(filter: DateFilter) {
        dateWindow.update { it.withFilter(filter) }
        selectedCategory.value = null
    }

    fun previousPeriod() {
        dateWindow.update { it.previous() }
        selectedCategory.value = null
    }

    fun nextPeriod() {
        dateWindow.update { it.next() }
        selectedCategory.value = null
    }

    fun setCustomRange(startMillis: Long, endMillis: Long) {
        dateWindow.update { it.withCustomRange(startMillis, endMillis) }
        selectedCategory.value = null
    }

    fun selectCategory(category: String) {
        selectedCategory.value = category
        selectedTypeFilter.value = null
    }

    fun clearCategory() {
        selectedCategory.value = null
    }

    fun toggleTypeFilter(filter: TransactionFilter) {
        selectedTypeFilter.update { if (it == filter) null else filter }
        selectedCategory.value = null
    }

    fun setSortOrder(order: SortOrder) {
        sortOrder.value = order
    }

    fun recategorizeTransaction(transaction: TransactionEntity, newCategory: String, applyToAll: Boolean) {
        viewModelScope.launch {
            app.repository.updateCategory(transaction.id, newCategory)
            if (applyToAll) {
                val merchant = transaction.merchant
                if (merchant != null) {
                    val rule = CategoryRuleEntity(
                        pattern = merchant.lowercase(),
                        category = newCategory
                    )
                    app.repository.insertRule(rule)
                    app.repository.updateCategoryByMerchant(merchant, newCategory)
                } else {
                    val rule = CategoryRuleEntity(
                        pattern = transaction.sender.lowercase(),
                        category = newCategory,
                        isSenderRule = true
                    )
                    app.repository.insertRule(rule)
                    app.repository.updateCategoryBySender(transaction.sender, newCategory)
                }
            }
        }
    }
}

private data class ScanState(
    val isScanning: Boolean = false,
    val lastScanMessage: String? = null
)
