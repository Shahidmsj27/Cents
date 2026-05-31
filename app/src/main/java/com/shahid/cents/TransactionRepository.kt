package com.shahid.cents

import kotlinx.coroutines.flow.Flow

class TransactionRepository(
    private val dao: TransactionDao,
    private val ruleDao: CategoryRuleDao
) {
    val transactions: Flow<List<TransactionEntity>> = dao.observeTransactions()

    suspend fun cleanupNonTransactionalNotices(): Int = dao.deleteProvidentFundNotices()

    suspend fun deleteInRange(startMillis: Long, endMillis: Long): Int = dao.deleteInRange(startMillis, endMillis)

    suspend fun processSms(sender: String, body: String, timestamp: Long, keepRawSms: Boolean = false): ProcessResult {
        val parsed = TransactionParser.parse(sender, body) ?: return ProcessResult.NotTransaction
        val hash = SmsHash.create(sender, timestamp, body)
        val rules = ruleDao.getAllRules()
        val category = matchRule(rules, sender, parsed.merchant) ?: parsed.category
        val transaction = TransactionEntity(
            amountPaise = parsed.amountPaise,
            type = parsed.type,
            category = category,
            merchant = parsed.merchant,
            accountHint = parsed.accountHint,
            balancePaise = parsed.balancePaise,
            sender = sender,
            smsTimestamp = timestamp,
            smsHash = hash,
            rawBody = if (keepRawSms) body else null
        )
        return if (dao.insert(transaction) == -1L) ProcessResult.Duplicate else ProcessResult.Imported
    }

    private fun matchRule(rules: List<CategoryRuleEntity>, sender: String, merchant: String?): String? {
        for (rule in rules) {
            val target = if (rule.isSenderRule) sender.lowercase() else (merchant?.lowercase() ?: continue)
            if (target.contains(rule.pattern)) return rule.category
        }
        return null
    }

    suspend fun updateCategory(id: Long, category: String) = dao.updateCategory(id, category)

    suspend fun updateCategoryByMerchant(merchant: String, category: String) = dao.updateCategoryByMerchant(merchant, category)

    suspend fun updateCategoryBySender(sender: String, category: String) = dao.updateCategoryBySender(sender, category)

    suspend fun insertRule(rule: CategoryRuleEntity) = ruleDao.insertRule(rule)
}

enum class ProcessResult {
    NotTransaction,
    Duplicate,
    Imported
}
