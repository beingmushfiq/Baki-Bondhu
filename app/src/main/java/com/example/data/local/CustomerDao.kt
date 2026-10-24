package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Customer
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) for Customers in the credit ledger.
 */
@Dao
interface CustomerDao {
    @Query("SELECT * FROM Customers ORDER BY balance DESC, lastTransactionAt DESC")
    fun getAllCustomers(): Flow<List<Customer>>

    @Query("SELECT * FROM Customers WHERE id = :id LIMIT 1")
    fun getCustomerById(id: Long): Flow<Customer?>

    @Query("SELECT * FROM Customers WHERE LOWER(TRIM(name)) = LOWER(TRIM(:name)) LIMIT 1")
    suspend fun findCustomerByName(name: String): Customer?

    @Query("SELECT * FROM Customers WHERE balance > 0 ORDER BY balance DESC")
    fun getCustomersWithDue(): Flow<List<Customer>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: Customer): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomers(customers: List<Customer>): List<Long>

    @Update
    suspend fun updateCustomer(customer: Customer)

    @Query("UPDATE Customers SET balance = balance + :amountDelta, lastTransactionAt = :timestamp WHERE id = :customerId")
    suspend fun adjustBalance(customerId: Long, amountDelta: Double, timestamp: Long)

    @Query("UPDATE Customers SET dueDate = :dueDate WHERE id = :customerId")
    suspend fun updateCustomerDueDate(customerId: Long, dueDate: Long?)

    @Delete
    suspend fun deleteCustomer(customer: Customer)

    @Query("DELETE FROM Customers")
    suspend fun clearAllCustomers()
}
