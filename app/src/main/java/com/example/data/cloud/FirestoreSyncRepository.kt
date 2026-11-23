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
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
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

class FirestoreSyncRepository(
    private val dbProvider: () -> FirebaseFirestore?,
    private val rtdbProvider: () -> FirebaseDatabase? = { null }
) {

    constructor(db: FirebaseFirestore) : this({ db }, { null })

    constructor(context: Context) : this(
        dbProvider = {
            try {
                val appContext = context.applicationContext
                if (com.google.firebase.FirebaseApp.getApps(appContext).isEmpty()) {
                    com.google.firebase.FirebaseApp.initializeApp(appContext)
                }
                val dbInstance = try {
                    val dbId = try { appContext.getString(R.string.firestore_database_id).trim() } catch (_: Exception) { "" }
                    if (dbId.isNotBlank() && dbId != "(default)" && !dbId.contains("ai-studio-android")) {
                        try {
                            FirebaseFirestore.getInstance(dbId)
                        } catch (_: Exception) {
                            FirebaseFirestore.getInstance()
                        }
                    } else {
                        FirebaseFirestore.getInstance()
                    }
                } catch (inner: Exception) {
                    FirebaseFirestore.getInstance()
                }
                dbInstance
            } catch (e: Exception) {
                Log.w("FirestoreSyncRepo", "Firestore not available: ${e.message}")
                null
            }
        },
        rtdbProvider = {
            try {
                val appContext = context.applicationContext
                if (com.google.firebase.FirebaseApp.getApps(appContext).isEmpty()) {
                    com.google.firebase.FirebaseApp.initializeApp(appContext)
                }
                val rtdbUrl = try {
                    appContext.getString(R.string.firebase_database_url).trim()
                } catch (_: Exception) { "" }.ifBlank {
                    "https://gen-lang-client-0530887953-default-rtdb.asia-southeast1.firebasedatabase.app/"
                }
                FirebaseDatabase.getInstance(rtdbUrl)
            } catch (e: Exception) {
                Log.w("FirestoreSyncRepo", "Realtime Database not available: ${e.message}")
                null
            }
        }
    )

    private val db: FirebaseFirestore? get() = dbProvider()
    private val rtdb: FirebaseDatabase? get() = rtdbProvider()
    private val tag = "FirestoreSyncRepo"

    /**
     * Initializes or updates user profile metadata in both Realtime Database and Firestore.
     */
    suspend fun syncUserProfile(user: FirebaseUser, settings: ShopSettingsEntity? = null, autoBackup: Boolean = true) {
        val now = System.currentTimeMillis()
        val data = mutableMapOf<String, Any>(
            "userId" to user.uid,
            "email" to (user.email ?: ""),
            "displayName" to (user.displayName ?: ""),
            "autoBackupEnabled" to autoBackup,
            "updatedAt" to now
        )
        if (settings != null) {
            data["shopName"] = settings.shopName
            data["ownerName"] = settings.ownerName
            data["bkashNumber"] = settings.bkashNumber
            data["nagadNumber"] = settings.nagadNumber
            data["language"] = settings.language
        }

        // 1. Sync to Realtime Database if available
        try {
            rtdb?.reference?.child("users")?.child(user.uid)?.child("profile")?.updateChildren(data)?.await()
        } catch (e: Exception) {
            Log.w(tag, "Failed to update RTDB profile: ${e.message}")
        }

        // 2. Sync to Firestore if available
        try {
            db?.collection("users")?.document(user.uid)?.set(data, SetOptions.merge())?.await()
        } catch (e: Exception) {
            Log.w(tag, "Failed to update Firestore profile: ${e.message}")
        }
    }

    /**
     * Performs a complete backup of local Room ledger data to the user's cloud database.
     * Prioritizes the user's provisioned Firebase Realtime Database with graceful Firestore fallback.
     */
    suspend fun backupAll(
        userId: String,
        user: FirebaseUser,
        customers: List<CustomerEntity>,
        transactions: List<TransactionEntity>,
        settings: ShopSettingsEntity
    ): Result<BackupStats> {
        val now = System.currentTimeMillis()

        val profileData = mapOf(
            "userId" to userId,
            "email" to (user.email ?: ""),
            "displayName" to (user.displayName ?: ""),
            "shopName" to settings.shopName,
            "ownerName" to settings.ownerName,
            "bkashNumber" to settings.bkashNumber,
            "nagadNumber" to settings.nagadNumber,
            "language" to settings.language,
            "lastBackupAt" to now,
            "customerCount" to customers.size,
            "transactionCount" to transactions.size,
            "totalBakiPaisa" to customers.filter { it.balancePaisa > 0 }.sumOf { it.balancePaisa },
            "updatedAt" to now
        )

        // ==========================================
        // STRATEGY 1: FIREBASE REALTIME DATABASE (RTDB)
        // ==========================================
        val rtdbInstance = rtdb
        if (rtdbInstance != null) {
            try {
                val userRef = rtdbInstance.reference.child("users").child(userId)

                // 1. Prepare atomic update tree
                val custMap = mutableMapOf<String, Any?>()
                for (cust in customers) {
                    val key = cust.uuid.ifBlank { cust.id.toString() }
                    custMap[key] = mapOf(
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
                        "dueDate" to cust.dueDate,
                        "isArchived" to cust.isArchived,
                        "updatedAt" to cust.updatedAt
                    )
                }

                val txMap = mutableMapOf<String, Any?>()
                for (tx in transactions) {
                    val key = tx.uuid.ifBlank { tx.id.toString() }
                    txMap[key] = mapOf(
                        "uuid" to tx.uuid,
                        "userId" to userId,
                        "customerId" to tx.customerId,
                        "customerUuid" to tx.customerUuid,
                        "customerName" to tx.customerName,
                        "type" to tx.type.name,
                        "amountPaisa" to tx.amountPaisa,
                        "amount" to tx.amount,
                        "originalSpokenText" to tx.originalSpokenText,
                        "note" to tx.note,
                        "timestamp" to tx.timestamp,
                        "dueDate" to tx.dueDate,
                        "isDeleted" to tx.isDeleted,
                        "updatedAt" to tx.updatedAt
                    )
                }

                val fullPayload = mapOf<String, Any?>(
                    "profile" to profileData,
                    "customers" to custMap,
                    "transactions" to txMap
                )

                // 2. Perform single atomic payload update with 25-second timeout
                withTimeout(25_000L) {
                    userRef.updateChildren(fullPayload).await()
                }

                Log.i(tag, "Successfully backed up ${customers.size} customers and ${transactions.size} transactions to Realtime Database")
                return Result.success(
                    BackupStats(
                        customersBackedUp = customers.size,
                        transactionsBackedUp = transactions.size,
                        timestamp = now
                    )
                )
            } catch (rtdbError: Exception) {
                Log.w(tag, "Realtime Database backup failed: ${rtdbError.message}", rtdbError)
                if (isPermissionDenied(rtdbError)) {
                    return Result.failure(formatRtdbPermissionError(rtdbError))
                }
                // If not permission denied, continue to Firestore fallback below
            }
        }

        // ==========================================
        // STRATEGY 2: CLOUD FIRESTORE FALLBACK
        // ==========================================
        val database = db ?: return Result.failure(
            IllegalStateException("ক্লাউড ডাটাবেজ সংযোগ পাওয়া যায়নি। ইন্টারনেট সংযোগ ও ফায়ারবেস কনফিগারেশন যাচাই করুন।")
        )

        return try {
            val userDoc = database.collection("users").document(userId)

            // Verify server connection & database existence using fast Source.SERVER probe
            try {
                withTimeout(8000L) {
                    userDoc.get(com.google.firebase.firestore.Source.SERVER).await()
                }
            } catch (probeError: Exception) {
                if (probeError is com.google.firebase.firestore.FirebaseFirestoreException) {
                    return Result.failure(formatFirestoreError(probeError))
                }
                if (probeError is kotlinx.coroutines.TimeoutCancellationException) {
                    return Result.failure(IllegalStateException("ক্লাউড ডাটাবেজ রেসপন্স করছে না। Firebase কনসোলে Database রুলস ও সংযোগ যাচাই করুন।"))
                }
                Log.w(tag, "Server probe warning: ${probeError.message}")
            }

            userDoc.set(profileData, SetOptions.merge()).await()

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
            Log.e(tag, "Firestore backup failed: ${e.message}", e)
            Result.failure(formatFirestoreError(e))
        }
    }

    /**
     * Pulls the user's complete cloud ledger and settings from the cloud database.
     * Prioritizes Realtime Database, then falls back to Firestore.
     */
    suspend fun restoreAll(userId: String): Result<RestoreStats> {
        // ==========================================
        // STRATEGY 1: FIREBASE REALTIME DATABASE (RTDB)
        // ==========================================
        val rtdbInstance = rtdb
        if (rtdbInstance != null) {
            try {
                val userRef = rtdbInstance.reference.child("users").child(userId)
                val snapshot: DataSnapshot = withTimeout(15_000L) {
                    userRef.get().await()
                }

                if (snapshot.exists() && (snapshot.hasChild("customers") || snapshot.hasChild("profile"))) {
                    val profileSnap = snapshot.child("profile")
                    val restoredSettings = if (profileSnap.exists()) {
                        ShopSettingsEntity(
                            shopName = profileSnap.child("shopName").getValue(String::class.java) ?: "",
                            ownerName = profileSnap.child("ownerName").getValue(String::class.java) ?: "",
                            bkashNumber = profileSnap.child("bkashNumber").getValue(String::class.java) ?: "",
                            nagadNumber = profileSnap.child("nagadNumber").getValue(String::class.java) ?: "",
                            language = profileSnap.child("language").getValue(String::class.java) ?: "bn"
                        )
                    } else null

                    val restoredCustomers = snapshot.child("customers").children.mapNotNull { child ->
                        try {
                            val uuid = child.child("uuid").getValue(String::class.java) ?: child.key ?: UUID.randomUUID().toString()
                            val name = child.child("name").getValue(String::class.java) ?: return@mapNotNull null
                            val phone = child.child("phone").getValue(String::class.java) ?: ""
                            val balancePaisa = child.child("balancePaisa").getValue(Long::class.java)
                                ?: Math.round((child.child("balance").getValue(Double::class.java) ?: 0.0) * 100.0)
                            val trust = (child.child("trustScore").getValue(Long::class.java) ?: 2L).toInt()
                            val note = child.child("note").getValue(String::class.java) ?: ""
                            val lastTx = child.child("lastTransactionAt").getValue(Long::class.java) ?: System.currentTimeMillis()
                            val createdAt = child.child("createdAt").getValue(Long::class.java) ?: System.currentTimeMillis()
                            val dueDate = child.child("dueDate").getValue(Long::class.java)
                            val isArchived = child.child("isArchived").getValue(Boolean::class.java) ?: false
                            val updatedAt = child.child("updatedAt").getValue(Long::class.java) ?: System.currentTimeMillis()

                            CustomerEntity(
                                id = 0L,
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

                    val restoredTransactions = snapshot.child("transactions").children.mapNotNull { child ->
                        try {
                            val uuid = child.child("uuid").getValue(String::class.java) ?: child.key ?: UUID.randomUUID().toString()
                            val custUuid = child.child("customerUuid").getValue(String::class.java) ?: ""
                            val custName = child.child("customerName").getValue(String::class.java) ?: ""
                            val rawCustId = child.child("customerId").getValue(Long::class.java) ?: 0L
                            val typeStr = child.child("type").getValue(String::class.java) ?: "BAKI_SALE"
                            val type = try { TransactionType.valueOf(typeStr) } catch (_: Exception) { TransactionType.BAKI_SALE }
                            val amountPaisa = child.child("amountPaisa").getValue(Long::class.java)
                                ?: Math.round((child.child("amount").getValue(Double::class.java) ?: 0.0) * 100.0)
                            val originalText = child.child("originalSpokenText").getValue(String::class.java)
                            val note = child.child("note").getValue(String::class.java) ?: ""
                            val timestamp = child.child("timestamp").getValue(Long::class.java) ?: System.currentTimeMillis()
                            val dueDate = child.child("dueDate").getValue(Long::class.java)
                            val isDeleted = child.child("isDeleted").getValue(Boolean::class.java) ?: false
                            val updatedAt = child.child("updatedAt").getValue(Long::class.java) ?: System.currentTimeMillis()

                            TransactionEntity(
                                id = 0L,
                                uuid = uuid,
                                customerId = rawCustId,
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

                    return Result.success(
                        RestoreStats(
                            customersRestored = restoredCustomers,
                            transactionsRestored = restoredTransactions,
                            settingsRestored = restoredSettings,
                            timestamp = System.currentTimeMillis()
                        )
                    )
                }
            } catch (rtdbError: Exception) {
                Log.w(tag, "Realtime Database restore failed: ${rtdbError.message}", rtdbError)
                if (isPermissionDenied(rtdbError)) {
                    return Result.failure(formatRtdbPermissionError(rtdbError))
                }
            }
        }

        // ==========================================
        // STRATEGY 2: CLOUD FIRESTORE FALLBACK
        // ==========================================
        val database = db ?: return Result.failure(IllegalStateException("Firestore is not initialized"))
        return try {
            val userDoc = try {
                withTimeout(8000L) {
                    database.collection("users").document(userId).get(com.google.firebase.firestore.Source.SERVER).await()
                }
            } catch (probeError: Exception) {
                if (probeError is com.google.firebase.firestore.FirebaseFirestoreException) {
                    return Result.failure(formatFirestoreError(probeError))
                }
                database.collection("users").document(userId).get().await()
            }

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
                        id = 0L,
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
                    val rawCustId = doc.getLong("customerId") ?: 0L
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
                        id = 0L,
                        uuid = uuid,
                        customerId = rawCustId,
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
            Result.failure(formatFirestoreError(e))
        }
    }

    /**
     * Retrieves cloud summary counts and last backup timestamp for pre-restore preview.
     */
    suspend fun getCloudPreview(userId: String): Result<CloudPreviewStats> {
        // Try Realtime Database first
        val rtdbInstance = rtdb
        if (rtdbInstance != null) {
            try {
                val userRef = rtdbInstance.reference.child("users").child(userId)
                val snapshot: DataSnapshot = withTimeout(10_000L) {
                    userRef.get().await()
                }
                if (snapshot.exists()) {
                    val lastBackup = snapshot.child("profile").child("lastBackupAt").getValue(Long::class.java)
                    val custCount = snapshot.child("customers").childrenCount.toInt()
                    val txCount = snapshot.child("transactions").childrenCount.toInt()
                    return Result.success(
                        CloudPreviewStats(
                            cloudCustomerCount = custCount,
                            cloudTransactionCount = txCount,
                            lastBackupAt = lastBackup
                        )
                    )
                }
            } catch (rtdbError: Exception) {
                Log.w(tag, "RTDB cloud preview warning: ${rtdbError.message}")
                if (isPermissionDenied(rtdbError)) {
                    return Result.failure(formatRtdbPermissionError(rtdbError))
                }
            }
        }

        // Firestore fallback
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
            Result.failure(formatFirestoreError(e))
        }
    }

    private fun isPermissionDenied(e: Exception): Boolean {
        val msg = e.message?.lowercase().orEmpty()
        return msg.contains("permission_denied") || msg.contains("permission denied") || msg.contains("permission")
    }

    private fun formatRtdbPermissionError(e: Exception): Exception {
        return Exception(
            "ডাটাবেজে লেখার অনুমতি নেই (Permission Denied)! Firebase Console > Realtime Database > Rules ট্যাবে গিয়ে লিখুন: \".read\": \"auth != null\", \".write\": \"auth != null\" এবং 'Publish' বাটনে চাপুন।",
            e
        )
    }

    private fun formatFirestoreError(e: Exception): Exception {
        if (e is com.google.firebase.firestore.FirebaseFirestoreException) {
            val msg = when (e.code) {
                com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                    "অনুমতি নেই (Permission Denied)! Firebase Console > Database > Rules এ write/read পারমিশন সক্রিয় করুন।"
                com.google.firebase.firestore.FirebaseFirestoreException.Code.NOT_FOUND ->
                    "ক্লাউড ডাটাবেজ পাওয়া যায়নি (NOT_FOUND)! Firebase Console-এ গিয়ে Cloud Firestore বা Realtime Database তৈরি করুন।"
                com.google.firebase.firestore.FirebaseFirestoreException.Code.UNAVAILABLE ->
                    "গুগল সার্ভার অনুপলব্ধ বা সংযোগ বন্ধ। ইন্টারনেট সংযোগ পরীক্ষা করুন।"
                com.google.firebase.firestore.FirebaseFirestoreException.Code.UNAUTHENTICATED ->
                    "অথেনটিকেশন ব্যর্থ হয়েছে। পুনরায় Google সাইন ইন করুন।"
                else -> e.localizedMessage ?: e.message ?: "Database error: ${e.code}"
            }
            return Exception(msg, e)
        }
        return e
    }
}
