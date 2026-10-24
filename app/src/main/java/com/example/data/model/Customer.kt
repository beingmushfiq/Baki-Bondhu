package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity representing a Customer in the credit ledger.
 */
@Entity(tableName = "Customers")
data class Customer(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val phone: String = "",
    val balance: Double = 0.0, // positive: owes shop (বাকি); negative: advance (জমা)
    val trustScore: Int = 2,    // 1: Delayed, 2: Regular, 3: Good payer
    val lastTransactionAt: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),
    val note: String = "",
    val dueDate: Long? = null
)

typealias CustomerEntity = Customer
