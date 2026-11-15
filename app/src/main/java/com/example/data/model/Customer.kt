package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.util.Money
import java.util.UUID

/**
 * Room entity representing a Customer in the credit ledger.
 * Stored in the 'Customers' table.
 *
 * Balances are stored in minor currency units (paisa) to guarantee financial precision.
 * 1 BDT = 100 paisa.
 */
@Entity(
    tableName = "Customers",
    indices = [
        Index(value = ["uuid"], unique = true),
        Index(value = ["phone"])
    ]
)
data class Customer(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val uuid: String = UUID.randomUUID().toString(),
    val name: String,
    val phone: String = "",
    val balancePaisa: Long = 0L, // positive: owes shop (বাকি); negative: advance (জমা)
    val trustScore: Int = 2,    // 1: Delayed, 2: Regular, 3: Good payer
    val lastTransactionAt: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),
    val note: String = "",
    val dueDate: Long? = null,
    val isArchived: Boolean = false,
    val syncStatus: SyncStatus = SyncStatus.PENDING_SYNC,
    val updatedAt: Long = System.currentTimeMillis()
) {
    // Backward-compatible computed property for Double values
    val balance: Double get() = balancePaisa / 100.0

    // Money helper instance
    val money: Money get() = Money(balancePaisa)

    // Convenient secondary constructor for instantiating with Double during tests / migration
    constructor(
        id: Long = 0,
        uuid: String = UUID.randomUUID().toString(),
        name: String,
        phone: String = "",
        balance: Double,
        trustScore: Int = 2,
        lastTransactionAt: Long = System.currentTimeMillis(),
        createdAt: Long = System.currentTimeMillis(),
        note: String = "",
        dueDate: Long? = null,
        isArchived: Boolean = false,
        syncStatus: SyncStatus = SyncStatus.PENDING_SYNC,
        updatedAt: Long = System.currentTimeMillis()
    ) : this(
        id = id,
        uuid = uuid,
        name = name,
        phone = phone,
        balancePaisa = Math.round(balance * 100.0),
        trustScore = trustScore,
        lastTransactionAt = lastTransactionAt,
        createdAt = createdAt,
        note = note,
        dueDate = dueDate,
        isArchived = isArchived,
        syncStatus = syncStatus,
        updatedAt = updatedAt
    )
}

typealias CustomerEntity = Customer
