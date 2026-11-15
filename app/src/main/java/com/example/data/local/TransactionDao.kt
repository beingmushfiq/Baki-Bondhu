package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Transaction
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) for credit ledger transactions.
 */
@Dao
interface TransactionDao {
    @Query("SELECT * FROM LedgerEntries WHERE isDeleted = 0 ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<Transaction>>

    @Query("SELECT * FROM LedgerEntries WHERE customerId = :customerId AND isDeleted = 0 ORDER BY timestamp DESC")
    fun getTransactionsForCustomer(customerId: Long): Flow<List<Transaction>>

    @Query("SELECT * FROM LedgerEntries WHERE isDeleted = 0 ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentTransactions(limit: Int): Flow<List<Transaction>>

    @Query("SELECT * FROM LedgerEntries WHERE timestamp >= :startOfDayTimestamp AND isDeleted = 0 ORDER BY timestamp DESC")
    fun getTodayTransactions(startOfDayTimestamp: Long): Flow<List<Transaction>>

    @Query("SELECT * FROM LedgerEntries WHERE timestamp >= :startTime AND timestamp < :endTime AND isDeleted = 0 ORDER BY timestamp DESC")
    fun getTransactionsBetween(startTime: Long, endTime: Long): Flow<List<Transaction>>

    @Query("SELECT * FROM LedgerEntries WHERE id = :id LIMIT 1")
    suspend fun getTransactionById(id: Long): Transaction?

    @Query("SELECT * FROM LedgerEntries WHERE uuid = :uuid LIMIT 1")
    suspend fun getTransactionByUuid(uuid: String): Transaction?

    @Query("SELECT * FROM LedgerEntries WHERE id IN (:ids)")
    suspend fun getTransactionsByIds(ids: List<Long>): List<Transaction>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: Transaction): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<Transaction>): List<Long>

    @Update
    suspend fun updateTransaction(transaction: Transaction)

    @Query("UPDATE LedgerEntries SET isDeleted = 1, syncStatus = 'PENDING_SYNC', updatedAt = :timestamp WHERE id = :id")
    suspend fun softDeleteTransactionById(id: Long, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE LedgerEntries SET isDeleted = 1, syncStatus = 'PENDING_SYNC', updatedAt = :timestamp WHERE id IN (:ids)")
    suspend fun softDeleteTransactionsByIds(ids: List<Long>, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE LedgerEntries SET isDeleted = 0, syncStatus = 'PENDING_SYNC', updatedAt = :timestamp WHERE id IN (:ids)")
    suspend fun restoreSoftDeletedTransactions(ids: List<Long>, timestamp: Long = System.currentTimeMillis())

    @Query("SELECT SUM(CASE WHEN type = 'BAKI_SALE' THEN amountPaisa WHEN type = 'REPAYMENT' THEN -amountPaisa ELSE 0 END) FROM LedgerEntries WHERE customerId = :customerId AND isDeleted = 0")
    suspend fun calculateNetLedgerPaisaForCustomer(customerId: Long): Long?

    @Query("SELECT * FROM LedgerEntries WHERE syncStatus = 'PENDING_SYNC'")
    suspend fun getPendingSyncTransactions(): List<Transaction>

    @Query("UPDATE LedgerEntries SET syncStatus = 'SYNCED' WHERE uuid IN (:uuids)")
    suspend fun markTransactionsSynced(uuids: List<String>)

    @Delete
    suspend fun deleteTransaction(transaction: Transaction)

    @Query("DELETE FROM LedgerEntries WHERE id = :id")
    suspend fun deleteTransactionById(id: Long)

    @Query("DELETE FROM LedgerEntries WHERE id IN (:ids)")
    suspend fun deleteTransactionsByIds(ids: List<Long>)

    @Query("DELETE FROM LedgerEntries")
    suspend fun clearAllLedgerEntries()
}

typealias LedgerEntryDao = TransactionDao
