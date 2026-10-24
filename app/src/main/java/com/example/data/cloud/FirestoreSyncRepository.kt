package com.example.data.cloud

import android.content.Context
import android.util.Log
import com.example.R
import com.example.data.model.CustomerEntity
import com.example.data.model.ShopSettingsEntity
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

data class BackupStats(
    val customersBackedUp: Int,
    val transactionsBackedUp: Int,
    val timestamp: Long
)

data class RestoreStats(
    val customersRestored: List<CustomerEntity>,
    val transactionsRestored: List<TransactionEntity>,
    val settingsRestored: ShopSettingsEntity?,
    val timestamp: Long
)

class FirestoreSyncRepository(private val dbProvider: () -> FirebaseFirestore?) {

    constructor(db: FirebaseFirestore) : this({ db })

    constructor(context: Context) : this({
        try {
            val appContext = context.applicationContext
            if (com.google.firebase.FirebaseApp.getApps(appContext).isEmpty()) {
                com.google.firebase.FirebaseApp.initializeApp(appContext)
            }
            FirebaseFirestore.getInstance(
                appContext.getString(R.string.firestore_database_id)
            )
        } catch (e: Exception) {
            Log.w("FirestoreSyncRepo", "Firestore not available: ${e.message}")
            null
        }
    })

    private val db: FirebaseFirestore? get() = dbProvider()

    private val tag = "FirestoreSyncRepo"

    /**
     * Initializes or updates the user profile metadata document in Firestore.
     */
    suspend fun syncUserProfile(user: FirebaseUser, settings: ShopSettingsEntity? = null, autoBackup: Boolean = true) {
        val database = db ?: return
        val userDoc = database.collection("users").document(user.uid)
        val data = mutableMapOf<String, Any>(
            "userId" to user.uid,
            "email" to (user.email ?: ""),
            "displayName" to (user.displayName ?: ""),
            "autoBackupEnabled" to autoBackup,
            "updatedAt" to System.currentTimeMillis()
        )
        if (settings != null) {
            data["shopName"] = settings.shopName
            data["ownerName"] = settings.ownerName
            data["bkashNumber"] = settings.bkashNumber
            data["nagadNumber"] = settings.nagadNumber
        }
        userDoc.set(data, SetOptions.merge()).await()
    }

    /**
     * Performs a complete backup of local Room ledger data to the user's Firestore cloud database.
     */
    suspend fun backupAll(
        userId: String,
        user: FirebaseUser,
        customers: List<CustomerEntity>,
        transactions: List<TransactionEntity>,
        settings: ShopSettingsEntity
    ): Result<BackupStats> {
        val database = db ?: return Result.failure(IllegalStateException("Firestore is not initialized"))
        return try {
            val userDoc = database.collection("users").document(userId)

            // Update user profile and backup metadata
            val profileData = mapOf(
                "userId" to userId,
                "email" to (user.email ?: ""),
                "displayName" to (user.displayName ?: ""),
                "shopName" to settings.shopName,
                "ownerName" to settings.ownerName,
                "bkashNumber" to settings.bkashNumber,
                "nagadNumber" to settings.nagadNumber,
                "lastBackupAt" to System.currentTimeMillis(),
                "customerCount" to customers.size,
                "transactionCount" to transactions.size,
                "totalBaki" to customers.filter { it.balance > 0 }.sumOf { it.balance },
                "updatedAt" to System.currentTimeMillis()
            )
            userDoc.set(profileData, SetOptions.merge()).await()

            // Back up customers in subcollection
            val customersCol = userDoc.collection("customers")
            for (cust in customers) {
                val custMap = mutableMapOf<String, Any>(
                    "id" to cust.id,
                    "userId" to userId,
                    "name" to cust.name,
                    "phone" to cust.phone,
                    "balance" to cust.balance,
                    "trustScore" to cust.trustScore,
                    "note" to cust.note,
                    "lastTransactionAt" to cust.lastTransactionAt,
                    "createdAt" to cust.createdAt
                )
                if (cust.dueDate != null) {
                    custMap["dueDate"] = cust.dueDate
                }
                customersCol.document(cust.id.toString()).set(custMap, SetOptions.merge()).await()
            }

            // Back up transactions in subcollection
            val txCol = userDoc.collection("transactions")
            for (tx in transactions) {
                val txMap = mutableMapOf<String, Any>(
                    "id" to tx.id,
                    "userId" to userId,
                    "customerId" to tx.customerId,
                    "customerName" to tx.customerName,
                    "type" to tx.type.name,
                    "amount" to tx.amount,
                    "note" to tx.note,
                    "timestamp" to tx.timestamp
                )
                if (tx.originalSpokenText != null) {
                    txMap["originalSpokenText"] = tx.originalSpokenText
                }
                if (tx.dueDate != null) {
                    txMap["dueDate"] = tx.dueDate
                }
                txCol.document(tx.id.toString()).set(txMap, SetOptions.merge()).await()
            }

            val stats = BackupStats(
                customersBackedUp = customers.size,
                transactionsBackedUp = transactions.size,
                timestamp = System.currentTimeMillis()
            )
            Result.success(stats)
        } catch (e: Exception) {
            Log.e(tag, "Backup failed", e)
            Result.failure(e)
        }
    }

    /**
     * Backs up a single customer directly to the user's cloud account.
     */
    suspend fun backupCustomer(userId: String, customer: CustomerEntity) {
        val database = db ?: return
        try {
            val doc = database.collection("users").document(userId)
                .collection("customers").document(customer.id.toString())
            val custMap = mutableMapOf<String, Any>(
                "id" to customer.id,
                "userId" to userId,
                "name" to customer.name,
                "phone" to customer.phone,
                "balance" to customer.balance,
                "trustScore" to customer.trustScore,
                "note" to customer.note,
                "lastTransactionAt" to customer.lastTransactionAt,
                "createdAt" to customer.createdAt
            )
            if (customer.dueDate != null) {
                custMap["dueDate"] = customer.dueDate
            }
            doc.set(custMap, SetOptions.merge()).await()
        } catch (e: Exception) {
            Log.w(tag, "Failed to auto-backup customer ${customer.id}", e)
        }
    }

    /**
     * Backs up a single transaction directly to the user's cloud account.
     */
    suspend fun backupTransaction(userId: String, tx: TransactionEntity) {
        val database = db ?: return
        try {
            val doc = database.collection("users").document(userId)
                .collection("transactions").document(tx.id.toString())
            val txMap = mutableMapOf<String, Any>(
                "id" to tx.id,
                "userId" to userId,
                "customerId" to tx.customerId,
                "customerName" to tx.customerName,
                "type" to tx.type.name,
                "amount" to tx.amount,
                "note" to tx.note,
                "timestamp" to tx.timestamp
            )
            if (tx.originalSpokenText != null) {
                txMap["originalSpokenText"] = tx.originalSpokenText
            }
            if (tx.dueDate != null) {
                txMap["dueDate"] = tx.dueDate
            }
            doc.set(txMap, SetOptions.merge()).await()
        } catch (e: Exception) {
            Log.w(tag, "Failed to auto-backup transaction ${tx.id}", e)
        }
    }

    /**
     * Pulls the user's complete cloud ledger and settings from Firestore.
     */
    suspend fun restoreAll(userId: String): Result<RestoreStats> {
        val database = db ?: return Result.failure(IllegalStateException("Firestore is not initialized"))
        return try {
            val userDoc = database.collection("users").document(userId).get().await()

            var restoredSettings: ShopSettingsEntity? = null
            if (userDoc.exists()) {
                val sName = userDoc.getString("shopName") ?: ""
                val oName = userDoc.getString("ownerName") ?: ""
                val bkash = userDoc.getString("bkashNumber") ?: ""
                val nagad = userDoc.getString("nagadNumber") ?: ""
                restoredSettings = ShopSettingsEntity(
                    shopName = sName,
                    ownerName = oName,
                    bkashNumber = bkash,
                    nagadNumber = nagad
                )
            }

            val custSnap = database.collection("users").document(userId)
                .collection("customers").get().await()

            val restoredCustomers = custSnap.documents.mapNotNull { doc ->
                try {
                    val id = doc.getLong("id") ?: 0L
                    val name = doc.getString("name") ?: return@mapNotNull null
                    val phone = doc.getString("phone") ?: ""
                    val balance = doc.getDouble("balance") ?: 0.0
                    val trust = (doc.getLong("trustScore") ?: 2L).toInt()
                    val note = doc.getString("note") ?: ""
                    val lastTx = doc.getLong("lastTransactionAt") ?: System.currentTimeMillis()
                    val createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                    val dueDate = doc.getLong("dueDate")

                    CustomerEntity(
                        id = id,
                        name = name,
                        phone = phone,
                        balance = balance,
                        trustScore = trust,
                        note = note,
                        lastTransactionAt = lastTx,
                        createdAt = createdAt,
                        dueDate = dueDate
                    )
                } catch (_: Exception) {
                    null
                }
            }

            val txSnap = database.collection("users").document(userId)
                .collection("transactions").get().await()

            val restoredTransactions = txSnap.documents.mapNotNull { doc ->
                try {
                    val id = doc.getLong("id") ?: 0L
                    val custId = doc.getLong("customerId") ?: 0L
                    val custName = doc.getString("customerName") ?: ""
                    val typeStr = doc.getString("type") ?: "BAKI_SALE"
                    val type = try { TransactionType.valueOf(typeStr) } catch (_: Exception) { TransactionType.BAKI_SALE }
                    val amount = doc.getDouble("amount") ?: 0.0
                    val originalText = doc.getString("originalSpokenText")
                    val note = doc.getString("note") ?: ""
                    val timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                    val dueDate = doc.getLong("dueDate")

                    TransactionEntity(
                        id = id,
                        customerId = custId,
                        customerName = custName,
                        type = type,
                        amount = amount,
                        originalSpokenText = originalText,
                        note = note,
                        timestamp = timestamp,
                        dueDate = dueDate
                    )
                } catch (_: Exception) {
                    null
                }
            }

            val stats = RestoreStats(
                customersRestored = restoredCustomers,
                transactionsRestored = restoredTransactions,
                settingsRestored = restoredSettings,
                timestamp = System.currentTimeMillis()
            )
            Result.success(stats)
        } catch (e: Exception) {
            Log.e(tag, "Restore failed", e)
            Result.failure(e)
        }
    }
}
