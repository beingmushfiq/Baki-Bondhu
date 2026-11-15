package com.example.data.cloud

import android.content.Context
import android.util.Log
import com.example.R
import com.example.data.model.CustomerEntity
import com.example.data.model.ShopSettingsEntity
import com.example.data.model.SyncStatus
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import java.util.UUID

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

data class CloudPreviewStats(
    val cloudCustomerCount: Int,
    val cloudTransactionCount: Int,
    val lastBackupAt: Long?
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
     * Initializes or updates user profile metadata in Firestore.
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
            data["language"] = settings.language
        }
        userDoc.set(data, SetOptions.merge()).await()
    }

    /**
     * Performs an atomic chunked batch backup of local Room ledger data to the user's Firestore cloud database.
     * Uses stable UUIDs to guarantee cross-device deduplication.
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
            val now = System.currentTimeMillis()

            // 1. Update user profile document
            val profileData = mapOf(
                "userId" to userId,
                "email" to (user.email ?: ""),
                "displayName" to (user.displayName ?: ""),
                "shopName" to settings.shopName,
                "ownerName" to settings.ownerName,
                "bkashNumber" to settings.bkashNumber,
                "nagadNumber" to settings.nagadNumber,
                "lastBackupAt" to now,
                "customerCount" to customers.size,
                "transactionCount" to transactions.size,
                "totalBakiPaisa" to customers.filter { it.balancePaisa > 0 }.sumOf { it.balancePaisa },
                "updatedAt" to now
            )
            userDoc.set(profileData, SetOptions.merge()).await()

            // 2. Batch commit customers using UUIDs in chunks of 400 (well within Firestore 500 limit)
            val customersCol = userDoc.collection("customers")
            customers.chunked(400).forEach { chunk ->
                val batch = database.batch()
                for (cust in chunk) {
                    val docRef = customersCol.document(cust.uuid.ifBlank { cust.id.toString() })
                    val custMap = mutableMapOf<String, Any>(
                        "uuid" to cust.uuid,
                        "userId" to userId,
                        "name" to cust.name,
                        "phone" to cust.phone,
                        "balancePaisa" to cust.balancePaisa,
                        "balance" to cust.balance,
                        "trustScore" to cust.trustScore,
                        "note" to cust.note,
                        "lastTransactionAt" to cust.lastTransactionAt,
                        "createdAt" to cust.createdAt,
                        "isArchived" to cust.isArchived,
                        "updatedAt" to cust.updatedAt
                    )
                    if (cust.dueDate != null) {
                        custMap["dueDate"] = cust.dueDate
                    }
                    batch.set(docRef, custMap, SetOptions.merge())
                }
                batch.commit().await()
            }

            // 3. Batch commit transactions using UUIDs in chunks of 400
            val txCol = userDoc.collection("transactions")
            transactions.chunked(400).forEach { chunk ->
                val batch = database.batch()
                for (tx in chunk) {
                    val docRef = txCol.document(tx.uuid.ifBlank { tx.id.toString() })
                    val txMap = mutableMapOf<String, Any>(
                        "uuid" to tx.uuid,
                        "userId" to userId,
                        "customerId" to tx.customerId,
                        "customerUuid" to tx.customerUuid,
                        "customerName" to tx.customerName,
                        "type" to tx.type.name,
                        "amountPaisa" to tx.amountPaisa,
                        "amount" to tx.amount,
                        "note" to tx.note,
                        "timestamp" to tx.timestamp,
                        "isDeleted" to tx.isDeleted,
                        "updatedAt" to tx.updatedAt
                    )
                    if (tx.originalSpokenText != null) {
                        txMap["originalSpokenText"] = tx.originalSpokenText
                    }
                    if (tx.dueDate != null) {
                        txMap["dueDate"] = tx.dueDate
                    }
                    batch.set(docRef, txMap, SetOptions.merge())
                }
                batch.commit().await()
            }

            val stats = BackupStats(
                customersBackedUp = customers.size,
                transactionsBackedUp = transactions.size,
                timestamp = now
            )
            Result.success(stats)
        } catch (e: Exception) {
            Log.e(tag, "Backup failed", e)
            Result.failure(e)
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
                val lang = userDoc.getString("language") ?: "bn"
                restoredSettings = ShopSettingsEntity(
                    shopName = sName,
                    ownerName = oName,
                    bkashNumber = bkash,
                    nagadNumber = nagad,
                    language = lang
                )
            }

            val custSnap = database.collection("users").document(userId)
                .collection("customers").get().await()

            val restoredCustomers = custSnap.documents.mapNotNull { doc ->
                try {
                    val uuid = doc.getString("uuid") ?: doc.id.ifBlank { UUID.randomUUID().toString() }
                    val name = doc.getString("name") ?: return@mapNotNull null
                    val phone = doc.getString("phone") ?: ""
                    // Handle balancePaisa with fallback to legacy balance Double
                    val balancePaisa = doc.getLong("balancePaisa")
                        ?: Math.round((doc.getDouble("balance") ?: 0.0) * 100.0)
                    val trust = (doc.getLong("trustScore") ?: 2L).toInt()
                    val note = doc.getString("note") ?: ""
                    val lastTx = doc.getLong("lastTransactionAt") ?: System.currentTimeMillis()
                    val createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                    val dueDate = doc.getLong("dueDate")
                    val isArchived = doc.getBoolean("isArchived") ?: false
                    val updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis()

                    CustomerEntity(
                        id = 0L, // Re-indexed locally by Room
                        uuid = uuid,
                        name = name,
                        phone = phone,
                        balancePaisa = balancePaisa,
                        trustScore = trust,
                        note = note,
                        lastTransactionAt = lastTx,
                        createdAt = createdAt,
                        dueDate = dueDate,
                        isArchived = isArchived,
                        syncStatus = SyncStatus.SYNCED,
                        updatedAt = updatedAt
                    )
                } catch (_: Exception) {
                    null
                }
            }

            val txSnap = database.collection("users").document(userId)
                .collection("transactions").get().await()

            val restoredTransactions = txSnap.documents.mapNotNull { doc ->
                try {
                    val uuid = doc.getString("uuid") ?: doc.id.ifBlank { UUID.randomUUID().toString() }
                    val custUuid = doc.getString("customerUuid") ?: ""
                    val custName = doc.getString("customerName") ?: ""
                    val typeStr = doc.getString("type") ?: "BAKI_SALE"
                    val type = try { TransactionType.valueOf(typeStr) } catch (_: Exception) { TransactionType.BAKI_SALE }
                    val amountPaisa = doc.getLong("amountPaisa")
                        ?: Math.round((doc.getDouble("amount") ?: 0.0) * 100.0)
                    val originalText = doc.getString("originalSpokenText")
                    val note = doc.getString("note") ?: ""
                    val timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                    val dueDate = doc.getLong("dueDate")
                    val isDeleted = doc.getBoolean("isDeleted") ?: false
                    val updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis()

                    TransactionEntity(
                        id = 0L, // Re-indexed locally by Room
                        uuid = uuid,
                        customerId = 0L, // Resolved to local ID during insertion
                        customerUuid = custUuid,
                        customerName = custName,
                        type = type,
                        amountPaisa = amountPaisa,
                        originalSpokenText = originalText,
                        note = note,
                        timestamp = timestamp,
                        dueDate = dueDate,
                        isDeleted = isDeleted,
                        syncStatus = SyncStatus.SYNCED,
                        updatedAt = updatedAt
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

    /**
     * Retrieves cloud summary counts and last backup timestamp for pre-restore preview.
     */
    suspend fun getCloudPreview(userId: String): Result<CloudPreviewStats> {
        val database = db ?: return Result.failure(IllegalStateException("Firestore is not initialized"))
        return try {
            val userDoc = database.collection("users").document(userId).get().await()
            val lastBackup = if (userDoc.exists()) userDoc.getLong("lastBackupAt") else null

            val custSnap = database.collection("users").document(userId)
                .collection("customers").get().await()
            val txSnap = database.collection("users").document(userId)
                .collection("transactions").get().await()

            Result.success(
                CloudPreviewStats(
                    cloudCustomerCount = custSnap.size(),
                    cloudTransactionCount = txSnap.size(),
                    lastBackupAt = lastBackup
                )
            )
        } catch (e: Exception) {
            Log.e(tag, "Get cloud preview failed", e)
            Result.failure(e)
        }
    }
}
