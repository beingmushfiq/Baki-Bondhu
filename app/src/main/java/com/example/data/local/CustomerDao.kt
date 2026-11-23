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
    @Query("SELECT * FROM Customers WHERE isArchived = 0 ORDER BY balancePaisa DESC, lastTransactionAt DESC")
    fun getAllCustomers(): Flow<List<Customer>>

    @Query("SELECT * FROM Customers ORDER BY balancePaisa DESC, lastTransactionAt DESC")
    fun getAllCustomersIncludingArchived(): Flow<List<Customer>>

    @Query("SELECT * FROM Customers WHERE id = :id LIMIT 1")
    fun getCustomerById(id: Long): Flow<Customer?>

    @Query("SELECT * FROM Customers WHERE id = :id LIMIT 1")
    suspend fun getCustomerByIdDirect(id: Long): Customer?

    @Query("SELECT * FROM Customers WHERE uuid = :uuid LIMIT 1")
    suspend fun getCustomerByUuid(uuid: String): Customer?

    @Query("SELECT * FROM Customers WHERE LOWER(TRIM(name)) = LOWER(TRIM(:name)) AND isArchived = 0 LIMIT 1")
    suspend fun findCustomerByName(name: String): Customer?

    @Query("SELECT * FROM Customers WHERE LOWER(TRIM(name)) = LOWER(TRIM(:name)) LIMIT 1")
    suspend fun findCustomerByNameIncludingArchived(name: String): Customer?

    @Query("SELECT * FROM Customers WHERE phone != '' AND phone = :phone AND isArchived = 0 LIMIT 1")
    suspend fun findCustomerByPhone(phone: String): Customer?

    @Query("SELECT * FROM Customers WHERE balancePaisa > 0 AND isArchived = 0 ORDER BY balancePaisa DESC")
    fun getCustomersWithDue(): Flow<List<Customer>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: Customer): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomers(customers: List<Customer>): List<Long>

    @Update
    suspend fun updateCustomer(customer: Customer)

    @Query("UPDATE Customers SET balancePaisa = balancePaisa + :amountPaisaDelta, lastTransactionAt = :timestamp, updatedAt = :timestamp, syncStatus = 'PENDING_SYNC' WHERE id = :customerId")
    suspend fun adjustBalancePaisa(customerId: Long, amountPaisaDelta: Long, timestamp: Long)

    suspend fun adjustBalance(customerId: Long, amountDelta: Double, timestamp: Long) {
        adjustBalancePaisa(customerId, Math.round(amountDelta * 100.0), timestamp)
    }

    @Query("UPDATE Customers SET dueDate = :dueDate, updatedAt = :timestamp, syncStatus = 'PENDING_SYNC' WHERE id = :customerId")
    suspend fun updateCustomerDueDate(customerId: Long, dueDate: Long?, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE Customers SET isArchived = 1, updatedAt = :timestamp, syncStatus = 'PENDING_SYNC' WHERE id = :customerId")
    suspend fun archiveCustomer(customerId: Long, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE Customers SET isArchived = 0, updatedAt = :timestamp, syncStatus = 'PENDING_SYNC' WHERE id = :customerId")
    suspend fun unarchiveCustomer(customerId: Long, timestamp: Long = System.currentTimeMillis())

    @Query("SELECT * FROM Customers WHERE isArchived = 1 ORDER BY updatedAt DESC")
    fun getArchivedCustomers(): Flow<List<Customer>>

    @Query("DELETE FROM Customers WHERE id = :customerId")
    suspend fun purgeCustomerById(customerId: Long)

    @Query("DELETE FROM Customers WHERE isArchived = 1")
    suspend fun purgeAllArchivedCustomers()

    @Query("SELECT * FROM Customers WHERE syncStatus = 'PENDING_SYNC'")
    suspend fun getPendingSyncCustomers(): List<Customer>

    @Query("UPDATE Customers SET syncStatus = 'SYNCED' WHERE uuid IN (:uuids)")
    suspend fun markCustomersSynced(uuids: List<String>)

    @Query("UPDATE Customers SET balancePaisa = :reconciledPaisa, updatedAt = :timestamp WHERE id = :customerId")
    suspend fun setCustomerBalancePaisa(customerId: Long, reconciledPaisa: Long, timestamp: Long)

    @Delete
    suspend fun deleteCustomer(customer: Customer)

    @Query("DELETE FROM Customers")
    suspend fun clearAllCustomers()
}
