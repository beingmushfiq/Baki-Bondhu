package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.Customer
import com.example.data.model.ShopSettingsEntity
import com.example.data.model.Transaction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Main Room database for Baki Bondhu application.
 * Manages Customer, Transaction (LedgerEntries), and ShopSettings entities.
 *
 * Configured with explicit migrations (v4 to v5) and no destructive production wipes.
 */
@Database(
    entities = [
        Customer::class,
        Transaction::class,
        ShopSettingsEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun customerDao(): CustomerDao
    abstract fun transactionDao(): TransactionDao
    fun ledgerEntryDao(): LedgerEntryDao = transactionDao()
    abstract fun shopSettingsDao(): ShopSettingsDao

    suspend fun clearAllData() {
        transactionDao().clearAllLedgerEntries()
        customerDao().clearAllCustomers()
    }

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "baki_bondhu.db"
                )
                    .addMigrations(DatabaseMigrations.MIGRATION_4_5)
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // Initialize default empty shop settings on first installation
                            CoroutineScope(Dispatchers.IO).launch {
                                val database = getInstance(context)
                                val existing = database.shopSettingsDao().getSettingsDirect()
                                if (existing == null) {
                                    database.shopSettingsDao().saveSettings(
                                        ShopSettingsEntity(
                                            id = 1,
                                            shopName = "আমার দোকান",
                                            ownerName = "",
                                            bkashNumber = "",
                                            nagadNumber = "",
                                            language = "bn",
                                            autoReadBack = true
                                        )
                                    )
                                }
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
