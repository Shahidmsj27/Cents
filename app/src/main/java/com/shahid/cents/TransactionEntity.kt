package com.shahid.cents

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transactions",
    indices = [Index(value = ["smsHash"], unique = true)]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amountPaise: Long,
    val type: TransactionType,
    val category: String,
    val merchant: String?,
    val accountHint: String?,
    val balancePaise: Long?,
    val sender: String,
    val smsTimestamp: Long,
    val smsHash: String,
    val rawBody: String?,
    val createdAt: Long = System.currentTimeMillis()
)

enum class TransactionType {
    Debit,
    Credit,
    Bill,
    Reversal
}
