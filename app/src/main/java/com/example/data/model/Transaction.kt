package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.util.Money
import java.util.UUID

/**
 * Room entity representing a credit ledger Transaction (Baki Sale, Repayment, or Cash Sale).
 * Stored in the 'LedgerEntries' table with a foreign key relation to Customer.
 *
 * Foreign key uses RESTRICT to protect financial audit history from accidental cascade deletion.
 * Monetary amounts are stored in minor currency units (paisa) to prevent rounding errors.
 * 1 BDT = 100 paisa.
 */
@Entity(
    tableName = "LedgerEntries",
    foreignKeys = [
        ForeignKey(
            entity = Customer::class,
            parentColumns = ["id"],
            childColumns = ["customerId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["uuid"], unique = true),
        Index(value = ["customerId"]),
        Index(value = ["timestamp"]),
        Index(value = ["syncStatus"])
    ]
)
data class Transaction(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val uuid: String = UUID.randomUUID().toString(),
    val customerId: Long,
    val customerUuid: String = "",
    val customerName: String,
    val type: TransactionType,
    val amountPaisa: Long,
    val originalSpokenText: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val note: String = "",
    val dueDate: Long? = null,
    val isDeleted: Boolean = false,
    val syncStatus: SyncStatus = SyncStatus.PENDING_SYNC,
    val updatedAt: Long = System.currentTimeMillis()
) {
    // Backward-compatible computed property for Double values
    val amount: Double get() = amountPaisa / 100.0

    // Money helper instance
    val money: Money get() = Money(amountPaisa)

    // Convenient secondary constructor for instantiating with Double during tests / migration
    constructor(
        id: Long = 0,
        uuid: String = UUID.randomUUID().toString(),
        customerId: Long,
        customerUuid: String = "",
        customerName: String,
        type: TransactionType,
        amount: Double,
        originalSpokenText: String? = null,
        timestamp: Long = System.currentTimeMillis(),
        note: String = "",
        dueDate: Long? = null,
        isDeleted: Boolean = false,
        syncStatus: SyncStatus = SyncStatus.PENDING_SYNC,
        updatedAt: Long = System.currentTimeMillis()
    ) : this(
        id = id,
        uuid = uuid,
        customerId = customerId,
        customerUuid = customerUuid,
        customerName = customerName,
        type = type,
        amountPaisa = Math.round(amount * 100.0),
        originalSpokenText = originalSpokenText,
        timestamp = timestamp,
        note = note,
        dueDate = dueDate,
        isDeleted = isDeleted,
        syncStatus = syncStatus,
        updatedAt = updatedAt
    )
}

typealias TransactionEntity = Transaction
typealias LedgerEntry = Transaction
typealias LedgerEntryEntity = Transaction
