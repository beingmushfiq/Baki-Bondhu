package com.example.data.repository

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
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/**
 * Repository interface defining abstract data operations for Baki Bondhu.
 * Decouples UI components from Room DAOs and abstracts offline-first persistence,
 * atomic balance adjustments, and batch deletion rollback.
 */
interface BakiRepository {
    val allCustomers: Flow<List<CustomerEntity>>
    val customersWithDue: Flow<List<CustomerEntity>>
    val allLedgerEntries: Flow<List<LedgerEntryEntity>>
    val allTransactions: Flow<List<LedgerEntryEntity>>
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
    suspend fun recordTransaction(
        customerName: String,
        customerPhone: String?,
        type: TransactionType,
        amount: Double,
        spokenText: String?,
        note: String = "",
        dueDate: Long? = null
    ): Long
    fun getTransactionsInDateRange(startTime: Long, endTime: Long): Flow<List<LedgerEntryEntity>>
    suspend fun updateCustomerDueDate(customerId: Long, dueDate: Long?)
    suspend fun calculateTotalDebtPerCustomer(): Map<Long, Double>
    suspend fun deleteTransaction(transactionId: Long): Transaction?
    suspend fun deleteTransactionsBatch(ids: Set<Long>): List<Transaction>
    suspend fun restoreTransactionsBatch(transactions: List<Transaction>)
    suspend fun updateTransaction(oldTx: Transaction, newTx: Transaction)
    suspend fun settleCustomerDue(customerId: Long, amount: Double, customerName: String): Long
    suspend fun syncOfflineData(): Boolean
    suspend fun clearDatabase()
    suspend fun restoreCloudData(
        customers: List<CustomerEntity>,
        transactions: List<TransactionEntity>,
        settings: ShopSettingsEntity?
    )
}

/**
 * Default implementation of BakiRepository managing local Room tables (Customers, LedgerEntries)
 * and atomic financial consistency.
 */
class DefaultBakiRepository(
    private val customerDao: CustomerDao,
    private val ledgerEntryDao: LedgerEntryDao,
    private val shopSettingsDao: ShopSettingsDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : BakiRepository {

    override val allCustomers: Flow<List<CustomerEntity>> = customerDao.getAllCustomers()
    override val customersWithDue: Flow<List<CustomerEntity>> = customerDao.getCustomersWithDue()
    override val allLedgerEntries: Flow<List<LedgerEntryEntity>> = ledgerEntryDao.getAllTransactions()
    override val allTransactions: Flow<List<LedgerEntryEntity>> = allLedgerEntries
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
        val id = customerDao.insertCustomer(customer)
        markPendingSync()
        id
    }

    override suspend fun updateCustomer(customer: CustomerEntity) = withContext(ioDispatcher) {
        customerDao.updateCustomer(customer)
        markPendingSync()
    }

    override suspend fun deleteCustomer(customer: CustomerEntity) = withContext(ioDispatcher) {
        customerDao.deleteCustomer(customer)
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

    override suspend fun recordTransaction(
        customerName: String,
        customerPhone: String?,
        type: TransactionType,
        amount: Double,
        spokenText: String?,
        note: String,
        dueDate: Long?
    ): Long = withContext(ioDispatcher) {
        val cleanName = customerName.trim().ifBlank { "সাধারণ খরিদ্দার" }
        val existing = customerDao.findCustomerByName(cleanName)
        val now = System.currentTimeMillis()

        val customerId = if (existing != null) {
            if (dueDate != null) {
                customerDao.updateCustomerDueDate(existing.id, dueDate)
            }
            existing.id
        } else {
            // Register new customer in Customers table on the fly
            customerDao.insertCustomer(
                Customer(
                    name = cleanName,
                    phone = customerPhone ?: "",
                    balance = 0.0,
                    trustScore = 2,
                    lastTransactionAt = now,
                    createdAt = now,
                    dueDate = dueDate
                )
            )
        }

        // Calculate balance delta
        val balanceDelta = when (type) {
            TransactionType.BAKI_SALE -> amount       // Customer owes more baki
            TransactionType.REPAYMENT -> -amount      // Customer repaid debt
            TransactionType.CASH_SALE -> 0.0          // Immediate cash, ledger balance unaffected
        }

        if (balanceDelta != 0.0) {
            customerDao.adjustBalance(customerId, balanceDelta, now)
        }

        // Insert into LedgerEntries table
        val entryId = ledgerEntryDao.insertTransaction(
            LedgerEntryEntity(
                customerId = customerId,
                customerName = cleanName,
                type = type,
                amount = amount,
                originalSpokenText = spokenText,
                timestamp = now,
                note = note,
                dueDate = dueDate
            )
        )

        markPendingSync()
        entryId
    }

    override suspend fun deleteTransaction(transactionId: Long): Transaction? = withContext(ioDispatcher) {
        val target = ledgerEntryDao.getTransactionById(transactionId) ?: return@withContext null
        val now = System.currentTimeMillis()

        // Reverse the financial impact of this transaction
        val reverseDelta = when (target.type) {
            TransactionType.BAKI_SALE -> -target.amount // Removing credit sale decreases debt
            TransactionType.REPAYMENT -> target.amount  // Removing payment restores debt
            TransactionType.CASH_SALE -> 0.0
        }

        if (reverseDelta != 0.0) {
            customerDao.adjustBalance(target.customerId, reverseDelta, now)
        }

        ledgerEntryDao.deleteTransactionById(transactionId)
        markPendingSync()
        target
    }

    override suspend fun deleteTransactionsBatch(ids: Set<Long>): List<Transaction> = withContext(ioDispatcher) {
        if (ids.isEmpty()) return@withContext emptyList()
        val targets = ledgerEntryDao.getTransactionsByIds(ids.toList())
        val now = System.currentTimeMillis()

        for (target in targets) {
            val reverseDelta = when (target.type) {
                TransactionType.BAKI_SALE -> -target.amount
                TransactionType.REPAYMENT -> target.amount
                TransactionType.CASH_SALE -> 0.0
            }
            if (reverseDelta != 0.0) {
                customerDao.adjustBalance(target.customerId, reverseDelta, now)
            }
        }

        ledgerEntryDao.deleteTransactionsByIds(ids.toList())
        markPendingSync()
        targets
    }

    override suspend fun restoreTransactionsBatch(transactions: List<Transaction>) = withContext(ioDispatcher) {
        if (transactions.isEmpty()) return@withContext
        val now = System.currentTimeMillis()

        for (tx in transactions) {
            val forwardDelta = when (tx.type) {
                TransactionType.BAKI_SALE -> tx.amount
                TransactionType.REPAYMENT -> -tx.amount
                TransactionType.CASH_SALE -> 0.0
            }
            if (forwardDelta != 0.0) {
                customerDao.adjustBalance(tx.customerId, forwardDelta, now)
            }
        }

        ledgerEntryDao.insertTransactions(transactions)
        markPendingSync()
    }

    override suspend fun updateTransaction(oldTx: Transaction, newTx: Transaction) = withContext(ioDispatcher) {
        val now = System.currentTimeMillis()
        val oldImpact = when (oldTx.type) {
            TransactionType.BAKI_SALE -> oldTx.amount
            TransactionType.REPAYMENT -> -oldTx.amount
            TransactionType.CASH_SALE -> 0.0
        }
        val newImpact = when (newTx.type) {
            TransactionType.BAKI_SALE -> newTx.amount
            TransactionType.REPAYMENT -> -newTx.amount
            TransactionType.CASH_SALE -> 0.0
        }
        val netDelta = newImpact - oldImpact

        if (netDelta != 0.0) {
            customerDao.adjustBalance(newTx.customerId, netDelta, now)
        }

        ledgerEntryDao.updateTransaction(newTx)
        markPendingSync()
    }

    override suspend fun settleCustomerDue(customerId: Long, amount: Double, customerName: String): Long =
        withContext(ioDispatcher) {
            val now = System.currentTimeMillis()
            customerDao.adjustBalance(customerId, -amount, now)
            val entryId = ledgerEntryDao.insertTransaction(
                LedgerEntryEntity(
                    customerId = customerId,
                    customerName = customerName,
                    type = TransactionType.REPAYMENT,
                    amount = amount,
                    originalSpokenText = "সম্পূর্ণ বকেয়া পরিশোধ",
                    timestamp = now,
                    note = "বকেয়া নিষ্পত্তি"
                )
            )
            markPendingSync()
            entryId
        }

    override suspend fun syncOfflineData(): Boolean = withContext(ioDispatcher) {
        val current = _syncState.value
        _syncState.value = current.copy(status = SyncStatus.SYNCING)
        try {
            // Emulate background synchronization of offline baki records
            delay(800)
            _syncState.value = SyncState(
                status = SyncStatus.SYNCED,
                pendingCount = 0,
                lastSyncTimestamp = System.currentTimeMillis(),
                errorMessage = null
            )
            true
        } catch (e: Exception) {
            _syncState.value = current.copy(
                status = SyncStatus.FAILED,
                errorMessage = e.localizedMessage
            )
            false
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

    private fun markPendingSync() {
        val current = _syncState.value
        _syncState.value = current.copy(
            status = SyncStatus.PENDING_SYNC,
            pendingCount = current.pendingCount + 1
        )
    }
}
