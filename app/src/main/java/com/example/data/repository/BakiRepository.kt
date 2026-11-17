package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.local.AppDatabase
import com.example.data.local.CustomerDao
import com.example.data.local.LedgerEntryDao
import com.example.data.local.ShopSettingsDao
import com.example.data.model.Customer
import com.example.data.model.CustomerEntity
import com.example.data.model.LedgerEntryEntity
import com.example.data.model.ShopSettingsEntity
import com.example.data.model.SyncState
import com.example.data.model.SyncStatus
import com.example.data.model.Transaction
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.util.Money
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * Repository interface defining abstract data operations for Baki Bondhu.
 * Decouples UI components from Room DAOs and abstracts offline-first persistence,
 * atomic balance adjustments, and batch deletion rollback.
 */
interface BakiRepository {
    val allCustomers: Flow<List<CustomerEntity>>
    val customersWithDue: Flow<List<CustomerEntity>>
    val archivedCustomers: Flow<List<CustomerEntity>>
    val allLedgerEntries: Flow<List<LedgerEntryEntity>>
    val allTransactions: Flow<List<LedgerEntryEntity>>
    val archivedTransactions: Flow<List<LedgerEntryEntity>>
    val shopSettings: Flow<ShopSettingsEntity?>
    val syncState: Flow<SyncState>

    fun getCustomerById(id: Long): Flow<CustomerEntity?>
    fun getTransactionsForCustomer(customerId: Long): Flow<List<LedgerEntryEntity>>
    fun getRecentTransactions(limit: Int = 10): Flow<List<LedgerEntryEntity>>

    suspend fun getSettingsDirect(): ShopSettingsEntity
    suspend fun updateSettings(settings: ShopSettingsEntity)
    suspend fun addCustomer(customer: CustomerEntity): Long
    suspend fun updateCustomer(customer: CustomerEntity)
    suspend fun deleteCustomer(customer: CustomerEntity)
    suspend fun archiveCustomer(customerId: Long)
    suspend fun restoreCustomer(customerId: Long)
    suspend fun restoreTransaction(transactionId: Long)
    suspend fun purgeCustomer(customerId: Long)
    suspend fun purgeTransaction(transactionId: Long)
    suspend fun purgeAllArchived()

    suspend fun recordTransaction(
        customerName: String,
        customerPhone: String?,
        type: TransactionType,
        amount: Double,
        spokenText: String?,
        note: String = "",
        dueDate: Long? = null
    ): Long

    suspend fun recordTransactionPaisa(
        customerName: String,
        customerPhone: String?,
        type: TransactionType,
        amountPaisa: Long,
        spokenText: String?,
        note: String = "",
        dueDate: Long? = null
    ): Long

    fun getTransactionsInDateRange(startTime: Long, endTime: Long): Flow<List<LedgerEntryEntity>>
    suspend fun updateCustomerDueDate(customerId: Long, dueDate: Long?)
    suspend fun calculateTotalDebtPerCustomer(): Map<Long, Double>
    suspend fun reconcileCustomerBalance(customerId: Long): Long
    suspend fun deleteTransaction(transactionId: Long): Transaction?
    suspend fun deleteTransactionsBatch(ids: Set<Long>): List<Transaction>
    suspend fun restoreTransactionsBatch(transactions: List<Transaction>)
    suspend fun updateTransaction(oldTx: Transaction, newTx: Transaction)
    suspend fun settleCustomerDue(customerId: Long, amount: Double, customerName: String): Long
    suspend fun clearDatabase()
    suspend fun restoreCloudData(
        customers: List<CustomerEntity>,
        transactions: List<TransactionEntity>,
        settings: ShopSettingsEntity?
    )
    suspend fun syncOfflineData(): Boolean
}

/**
 * Default implementation of BakiRepository managing local Room tables (Customers, LedgerEntries)
 * and atomic financial consistency with Long Paisa precision.
 */
class DefaultBakiRepository(
    private val customerDao: CustomerDao,
    private val ledgerEntryDao: LedgerEntryDao,
    private val shopSettingsDao: ShopSettingsDao,
    private val database: AppDatabase? = null,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : BakiRepository {

    override val allCustomers: Flow<List<CustomerEntity>> = customerDao.getAllCustomers()
    override val customersWithDue: Flow<List<CustomerEntity>> = customerDao.getCustomersWithDue()
    override val archivedCustomers: Flow<List<CustomerEntity>> = customerDao.getArchivedCustomers()
    override val allLedgerEntries: Flow<List<LedgerEntryEntity>> = ledgerEntryDao.getAllTransactions()
    override val allTransactions: Flow<List<LedgerEntryEntity>> = allLedgerEntries
    override val archivedTransactions: Flow<List<LedgerEntryEntity>> = ledgerEntryDao.getArchivedTransactions()
    override val shopSettings: Flow<ShopSettingsEntity?> = shopSettingsDao.getSettings()

    private val _syncState = MutableStateFlow(
        SyncState(
            status = SyncStatus.SYNCED,
            pendingCount = 0,
            lastSyncTimestamp = System.currentTimeMillis()
        )
    )
    override val syncState: Flow<SyncState> = _syncState.asStateFlow()

    override fun getCustomerById(id: Long): Flow<CustomerEntity?> = customerDao.getCustomerById(id)

    override fun getTransactionsForCustomer(customerId: Long): Flow<List<LedgerEntryEntity>> =
        ledgerEntryDao.getTransactionsForCustomer(customerId)

    override fun getRecentTransactions(limit: Int): Flow<List<LedgerEntryEntity>> =
        ledgerEntryDao.getRecentTransactions(limit)

    override suspend fun getSettingsDirect(): ShopSettingsEntity = withContext(ioDispatcher) {
        shopSettingsDao.getSettingsDirect() ?: ShopSettingsEntity()
    }

    override suspend fun updateSettings(settings: ShopSettingsEntity) = withContext(ioDispatcher) {
        shopSettingsDao.saveSettings(settings)
    }

    override suspend fun addCustomer(customer: CustomerEntity): Long = withContext(ioDispatcher) {
        val now = System.currentTimeMillis()
        val customerToInsert = if (customer.uuid.isBlank()) {
            customer.copy(uuid = UUID.randomUUID().toString(), createdAt = now, updatedAt = now)
        } else {
            customer.copy(updatedAt = now)
        }
        val id = customerDao.insertCustomer(customerToInsert)
        markPendingSync()
        id
    }

    override suspend fun updateCustomer(customer: CustomerEntity) = withContext(ioDispatcher) {
        customerDao.updateCustomer(customer.copy(updatedAt = System.currentTimeMillis()))
        markPendingSync()
    }

    override suspend fun deleteCustomer(customer: CustomerEntity) = withContext(ioDispatcher) {
        // Soft-archive customer rather than hard deleting to preserve financial transaction integrity
        customerDao.archiveCustomer(customer.id, System.currentTimeMillis())
        markPendingSync()
    }

    override suspend fun archiveCustomer(customerId: Long) = withContext(ioDispatcher) {
        customerDao.archiveCustomer(customerId, System.currentTimeMillis())
        markPendingSync()
    }

    override fun getTransactionsInDateRange(startTime: Long, endTime: Long): Flow<List<LedgerEntryEntity>> =
        ledgerEntryDao.getTransactionsBetween(startTime, endTime)

    override suspend fun updateCustomerDueDate(customerId: Long, dueDate: Long?) = withContext(ioDispatcher) {
        customerDao.updateCustomerDueDate(customerId, dueDate)
        markPendingSync()
    }

    override suspend fun calculateTotalDebtPerCustomer(): Map<Long, Double> = withContext(ioDispatcher) {
        customerDao.getAllCustomers().first().associate { it.id to it.balance }
    }

    override suspend fun reconcileCustomerBalance(customerId: Long): Long = withContext(ioDispatcher) {
        val netPaisa = ledgerEntryDao.calculateNetLedgerPaisaForCustomer(customerId) ?: 0L
        val now = System.currentTimeMillis()
        customerDao.setCustomerBalancePaisa(customerId, netPaisa, now)
        netPaisa
    }

    override suspend fun recordTransaction(
        customerName: String,
        customerPhone: String?,
        type: TransactionType,
        amount: Double,
        spokenText: String?,
        note: String,
        dueDate: Long?
    ): Long {
        return recordTransactionPaisa(
            customerName = customerName,
            customerPhone = customerPhone,
            type = type,
            amountPaisa = Math.round(amount * 100.0),
            spokenText = spokenText,
            note = note,
            dueDate = dueDate
        )
    }

    override suspend fun recordTransactionPaisa(
        customerName: String,
        customerPhone: String?,
        type: TransactionType,
        amountPaisa: Long,
        spokenText: String?,
        note: String,
        dueDate: Long?
    ): Long = withContext(ioDispatcher) {
        val cleanName = customerName.trim().ifBlank { "সাধারণ খরিদ্দার" }
        val cleanPhone = customerPhone?.trim() ?: ""
        val now = System.currentTimeMillis()

        suspend fun executeAtomic(): Long {
            // Find existing customer by phone first if provided, otherwise by exact name
            val existingByPhone = if (cleanPhone.isNotBlank()) customerDao.findCustomerByPhone(cleanPhone) else null
            val existing = existingByPhone ?: customerDao.findCustomerByName(cleanName)

            val (customerId, customerUuid) = if (existing != null) {
                if (dueDate != null) {
                    customerDao.updateCustomerDueDate(existing.id, dueDate, now)
                }
                Pair(existing.id, existing.uuid)
            } else {
                val newUuid = UUID.randomUUID().toString()
                val newId = customerDao.insertCustomer(
                    Customer(
                        uuid = newUuid,
                        name = cleanName,
                        phone = cleanPhone,
                        balancePaisa = 0L,
                        trustScore = 2,
                        lastTransactionAt = now,
                        createdAt = now,
                        dueDate = dueDate,
                        isArchived = false,
                        syncStatus = SyncStatus.PENDING_SYNC,
                        updatedAt = now
                    )
                )
                Pair(newId, newUuid)
            }

            // Calculate balance delta in paisa
            val balancePaisaDelta = when (type) {
                TransactionType.BAKI_SALE -> amountPaisa   // Customer owes more baki
                TransactionType.REPAYMENT -> -amountPaisa  // Customer repaid debt
                TransactionType.CASH_SALE -> 0L            // Immediate cash sale
            }

            if (balancePaisaDelta != 0L) {
                customerDao.adjustBalancePaisa(customerId, balancePaisaDelta, now)
            }

            // Insert into LedgerEntries table
            val entryId = ledgerEntryDao.insertTransaction(
                Transaction(
                    uuid = UUID.randomUUID().toString(),
                    customerId = customerId,
                    customerUuid = customerUuid,
                    customerName = cleanName,
                    type = type,
                    amountPaisa = amountPaisa,
                    originalSpokenText = spokenText,
                    timestamp = now,
                    note = note,
                    dueDate = dueDate,
                    isDeleted = false,
                    syncStatus = SyncStatus.PENDING_SYNC,
                    updatedAt = now
                )
            )

            markPendingSync()
            return entryId
        }

        if (database != null) {
            database.withTransaction { executeAtomic() }
        } else {
            executeAtomic()
        }
    }

    override suspend fun deleteTransaction(transactionId: Long): Transaction? = withContext(ioDispatcher) {
        suspend fun execute(): Transaction? {
            val target = ledgerEntryDao.getTransactionById(transactionId) ?: return null
            val now = System.currentTimeMillis()

            val reversePaisaDelta = when (target.type) {
                TransactionType.BAKI_SALE -> -target.amountPaisa
                TransactionType.REPAYMENT -> target.amountPaisa
                TransactionType.CASH_SALE -> 0L
            }

            if (reversePaisaDelta != 0L) {
                customerDao.adjustBalancePaisa(target.customerId, reversePaisaDelta, now)
            }

            // Soft delete with tombstone for cloud sync
            ledgerEntryDao.softDeleteTransactionById(transactionId, now)
            markPendingSync()
            return target
        }

        if (database != null) {
            database.withTransaction { execute() }
        } else {
            execute()
        }
    }

    override suspend fun deleteTransactionsBatch(ids: Set<Long>): List<Transaction> = withContext(ioDispatcher) {
        if (ids.isEmpty()) return@withContext emptyList()

        suspend fun execute(): List<Transaction> {
            val targets = ledgerEntryDao.getTransactionsByIds(ids.toList())
            val now = System.currentTimeMillis()

            for (target in targets) {
                val reversePaisaDelta = when (target.type) {
                    TransactionType.BAKI_SALE -> -target.amountPaisa
                    TransactionType.REPAYMENT -> target.amountPaisa
                    TransactionType.CASH_SALE -> 0L
                }
                if (reversePaisaDelta != 0L) {
                    customerDao.adjustBalancePaisa(target.customerId, reversePaisaDelta, now)
                }
            }

            ledgerEntryDao.softDeleteTransactionsByIds(ids.toList(), now)
            markPendingSync()
            return targets
        }

        if (database != null) {
            database.withTransaction { execute() }
        } else {
            execute()
        }
    }

    override suspend fun restoreTransactionsBatch(transactions: List<Transaction>) = withContext(ioDispatcher) {
        if (transactions.isEmpty()) return@withContext

        suspend fun execute() {
            val now = System.currentTimeMillis()
            val idsToRestore = transactions.map { it.id }

            for (tx in transactions) {
                val forwardPaisaDelta = when (tx.type) {
                    TransactionType.BAKI_SALE -> tx.amountPaisa
                    TransactionType.REPAYMENT -> -tx.amountPaisa
                    TransactionType.CASH_SALE -> 0L
                }
                if (forwardPaisaDelta != 0L) {
                    customerDao.adjustBalancePaisa(tx.customerId, forwardPaisaDelta, now)
                }
            }

            ledgerEntryDao.restoreSoftDeletedTransactions(idsToRestore, now)
            markPendingSync()
        }

        if (database != null) {
            database.withTransaction { execute() }
        } else {
            execute()
        }
    }

    override suspend fun updateTransaction(oldTx: Transaction, newTx: Transaction) = withContext(ioDispatcher) {
        suspend fun execute() {
            val now = System.currentTimeMillis()
            val oldPaisaImpact = when (oldTx.type) {
                TransactionType.BAKI_SALE -> oldTx.amountPaisa
                TransactionType.REPAYMENT -> -oldTx.amountPaisa
                TransactionType.CASH_SALE -> 0L
            }
            val newPaisaImpact = when (newTx.type) {
                TransactionType.BAKI_SALE -> newTx.amountPaisa
                TransactionType.REPAYMENT -> -newTx.amountPaisa
                TransactionType.CASH_SALE -> 0L
            }

            val netDelta = newPaisaImpact - oldPaisaImpact

            if (oldTx.customerId == newTx.customerId) {
                if (netDelta != 0L) {
                    customerDao.adjustBalancePaisa(newTx.customerId, netDelta, now)
                }
            } else {
                // Reassigned to another customer
                if (oldPaisaImpact != 0L) {
                    customerDao.adjustBalancePaisa(oldTx.customerId, -oldPaisaImpact, now)
                }
                if (newPaisaImpact != 0L) {
                    customerDao.adjustBalancePaisa(newTx.customerId, newPaisaImpact, now)
                }
            }

            ledgerEntryDao.updateTransaction(newTx.copy(updatedAt = now, syncStatus = SyncStatus.PENDING_SYNC))
            markPendingSync()
        }

        if (database != null) {
            database.withTransaction { execute() }
        } else {
            execute()
        }
    }

    override suspend fun settleCustomerDue(customerId: Long, amount: Double, customerName: String): Long =
        withContext(ioDispatcher) {
            val now = System.currentTimeMillis()
            val amountPaisa = Math.round(amount * 100.0)

            suspend fun execute(): Long {
                customerDao.adjustBalancePaisa(customerId, -amountPaisa, now)
                val customer = customerDao.getCustomerByIdDirect(customerId)
                val entryId = ledgerEntryDao.insertTransaction(
                    Transaction(
                        uuid = UUID.randomUUID().toString(),
                        customerId = customerId,
                        customerUuid = customer?.uuid ?: "",
                        customerName = customerName,
                        type = TransactionType.REPAYMENT,
                        amountPaisa = amountPaisa,
                        originalSpokenText = "সম্পূর্ণ বকেয়া পরিশোধ",
                        timestamp = now,
                        note = "বকেয়া নিষ্পত্তি",
                        isDeleted = false,
                        syncStatus = SyncStatus.PENDING_SYNC,
                        updatedAt = now
                    )
                )
                markPendingSync()
                return entryId
            }

            if (database != null) {
                database.withTransaction { execute() }
            } else {
                execute()
            }
        }

    override suspend fun clearDatabase() = withContext(ioDispatcher) {
        ledgerEntryDao.clearAllLedgerEntries()
        customerDao.clearAllCustomers()
        _syncState.value = SyncState(
            status = SyncStatus.SYNCED,
            pendingCount = 0,
            lastSyncTimestamp = System.currentTimeMillis()
        )
    }

    override suspend fun restoreCloudData(
        customers: List<CustomerEntity>,
        transactions: List<TransactionEntity>,
        settings: ShopSettingsEntity?
    ) = withContext(ioDispatcher) {
        suspend fun execute() {
            if (customers.isNotEmpty()) {
                customerDao.insertCustomers(customers)
            }
            if (transactions.isNotEmpty()) {
                ledgerEntryDao.insertTransactions(transactions)
            }
            if (settings != null) {
                shopSettingsDao.saveSettings(settings)
            }
            _syncState.value = SyncState(
                status = SyncStatus.SYNCED,
                pendingCount = 0,
                lastSyncTimestamp = System.currentTimeMillis()
            )
        }

        if (database != null) {
            database.withTransaction { execute() }
        } else {
            execute()
        }
    }

    override suspend fun syncOfflineData(): Boolean = withContext(ioDispatcher) {
        _syncState.value = SyncState(
            status = SyncStatus.SYNCED,
            pendingCount = 0,
            lastSyncTimestamp = System.currentTimeMillis()
        )
        true
    }

    override suspend fun restoreCustomer(customerId: Long) = withContext(ioDispatcher) {
        customerDao.unarchiveCustomer(customerId, System.currentTimeMillis())
        markPendingSync()
    }

    override suspend fun restoreTransaction(transactionId: Long) = withContext(ioDispatcher) {
        val tx = ledgerEntryDao.getTransactionById(transactionId) ?: return@withContext
        val now = System.currentTimeMillis()
        val delta = when (tx.type) {
            TransactionType.BAKI_SALE -> tx.amountPaisa
            TransactionType.REPAYMENT -> -tx.amountPaisa
            TransactionType.CASH_SALE -> 0L
        }
        if (delta != 0L) {
            customerDao.adjustBalancePaisa(tx.customerId, delta, now)
        }
        ledgerEntryDao.restoreSoftDeletedTransaction(transactionId, now)
        markPendingSync()
    }

    override suspend fun purgeCustomer(customerId: Long) = withContext(ioDispatcher) {
        customerDao.purgeCustomerById(customerId)
        markPendingSync()
    }

    override suspend fun purgeTransaction(transactionId: Long) = withContext(ioDispatcher) {
        ledgerEntryDao.deleteTransactionById(transactionId)
        markPendingSync()
    }

    override suspend fun purgeAllArchived() = withContext(ioDispatcher) {
        customerDao.purgeAllArchivedCustomers()
        ledgerEntryDao.purgeAllArchivedTransactions()
        markPendingSync()
    }

    private fun markPendingSync() {
        val current = _syncState.value
        _syncState.value = current.copy(
            status = SyncStatus.PENDING_SYNC,
            pendingCount = current.pendingCount + 1
        )
    }
}
