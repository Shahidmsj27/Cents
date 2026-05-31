package com.shahid.cents

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY smsTimestamp DESC")
    fun observeTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE smsHash = :smsHash LIMIT 1")
    suspend fun findByHash(smsHash: String): TransactionEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(transaction: TransactionEntity): Long

    @Query("DELETE FROM transactions WHERE smsTimestamp >= :startMillis AND smsTimestamp < :endMillis")
    suspend fun deleteInRange(startMillis: Long, endMillis: Long): Int

    @Query("""
        DELETE FROM transactions
        WHERE lower(sender) LIKE '%epfo%'
           OR lower(sender) LIKE '%epf%'
           OR lower(merchant) LIKE '%epfo%'
           OR lower(merchant) LIKE '%epf%'
    """)
    suspend fun deleteProvidentFundNotices(): Int

    @Query("UPDATE transactions SET category = :category WHERE id = :id")
    suspend fun updateCategory(id: Long, category: String)

    @Query("UPDATE transactions SET category = :category WHERE merchant = :merchant")
    suspend fun updateCategoryByMerchant(merchant: String, category: String)

    @Query("UPDATE transactions SET category = :category WHERE sender = :sender")
    suspend fun updateCategoryBySender(sender: String, category: String)
}
