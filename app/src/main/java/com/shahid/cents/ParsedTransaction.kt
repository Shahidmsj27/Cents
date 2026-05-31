package com.shahid.cents

data class ParsedTransaction(
    val amountPaise: Long,
    val type: TransactionType,
    val category: String,
    val merchant: String?,
    val accountHint: String?,
    val balancePaise: Long?
)
