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
    @Query("SELECT * FROM LedgerEntries ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<Transaction>>

    @Query("SELECT * FROM LedgerEntries WHERE customerId = :customerId ORDER BY timestamp DESC")
    fun getTransactionsForCustomer(customerId: Long): Flow<List<Transaction>>

    @Query("SELECT * FROM LedgerEntries ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentTransactions(limit: Int): Flow<List<Transaction>>

    @Query("SELECT * FROM LedgerEntries WHERE timestamp >= :startOfDayTimestamp ORDER BY timestamp DESC")
    fun getTodayTransactions(startOfDayTimestamp: Long): Flow<List<Transaction>>

    @Query("SELECT * FROM LedgerEntries WHERE timestamp >= :startTime AND timestamp <= :endTime ORDER BY timestamp DESC")
    fun getTransactionsBetween(startTime: Long, endTime: Long): Flow<List<Transaction>>

    @Query("SELECT * FROM LedgerEntries WHERE id = :id LIMIT 1")
    suspend fun getTransactionById(id: Long): Transaction?

    @Query("SELECT * FROM LedgerEntries WHERE id IN (:ids)")
    suspend fun getTransactionsByIds(ids: List<Long>): List<Transaction>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: Transaction): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<Transaction>): List<Long>

    @Update
    suspend fun updateTransaction(transaction: Transaction)

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
