package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity representing a credit ledger Transaction (Baki Sale, Repayment, or Cash Sale).
 * Stored in the 'LedgerEntries' table with a foreign key relation to Customer.
 */
@Entity(
    tableName = "LedgerEntries",
    foreignKeys = [
        ForeignKey(
            entity = Customer::class,
            parentColumns = ["id"],
            childColumns = ["customerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["customerId"])]
)
data class Transaction(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val customerId: Long,
    val customerName: String,
    val type: TransactionType,
    val amount: Double,
    val originalSpokenText: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val note: String = "",
    val dueDate: Long? = null
)

typealias TransactionEntity = Transaction
typealias LedgerEntry = Transaction
typealias LedgerEntryEntity = Transaction
