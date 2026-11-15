package com.example.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Database migrations for AppDatabase.
 *
 * MIGRATION_4_5:
 * - Upgrades schema from v4 to v5.
 * - Converts Customer.balance (REAL) to Customer.balancePaisa (INTEGER).
 * - Converts Transaction.amount (REAL) to Transaction.amountPaisa (INTEGER).
 * - Generates stable UUIDs for all existing customers and transactions.
 * - Adds customerUuid to LedgerEntries for cross-device synchronization.
 * - Adds isArchived, isDeleted, syncStatus, and updatedAt columns.
 * - Replaces unsafe CASCADE deletion with RESTRICT to guarantee ledger audit history preservation.
 */
object DatabaseMigrations {

    val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // 1. Create temporary new Customers table with v5 schema
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS Customers_new (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    uuid TEXT NOT NULL,
                    name TEXT NOT NULL,
                    phone TEXT NOT NULL,
                    balancePaisa INTEGER NOT NULL,
                    trustScore INTEGER NOT NULL,
                    lastTransactionAt INTEGER NOT NULL,
                    createdAt INTEGER NOT NULL,
                    note TEXT NOT NULL,
                    dueDate INTEGER,
                    isArchived INTEGER NOT NULL,
                    syncStatus TEXT NOT NULL,
                    updatedAt INTEGER NOT NULL
                )
            """.trimIndent())

            // 2. Migrate existing rows from Customers to Customers_new
            // Convert balance (Double) to balancePaisa (Long = round(balance * 100))
            db.execSQL("""
                INSERT INTO Customers_new (
                    id, uuid, name, phone, balancePaisa, trustScore,
                    lastTransactionAt, createdAt, note, dueDate,
                    isArchived, syncStatus, updatedAt
                )
                SELECT 
                    id, 
                    lower(hex(randomblob(4)) || '-' || hex(randomblob(2)) || '-4' || substr(hex(randomblob(2)),2) || '-a' || substr(hex(randomblob(2)),2) || '-' || hex(randomblob(6))),
                    name, 
                    phone, 
                    CAST(ROUND(balance * 100) AS INTEGER), 
                    trustScore, 
                    lastTransactionAt, 
                    createdAt, 
                    note, 
                    dueDate, 
                    0, 
                    'PENDING_SYNC', 
                    lastTransactionAt
                FROM Customers
            """.trimIndent())

            // 3. Drop old Customers table and rename new table
            db.execSQL("DROP TABLE Customers")
            db.execSQL("ALTER TABLE Customers_new RENAME TO Customers")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_Customers_uuid ON Customers(uuid)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_Customers_phone ON Customers(phone)")

            // 4. Create temporary new LedgerEntries table with v5 schema and RESTRICT foreign key
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS LedgerEntries_new (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    uuid TEXT NOT NULL,
                    customerId INTEGER NOT NULL,
                    customerUuid TEXT NOT NULL,
                    customerName TEXT NOT NULL,
                    type TEXT NOT NULL,
                    amountPaisa INTEGER NOT NULL,
                    originalSpokenText TEXT,
                    timestamp INTEGER NOT NULL,
                    note TEXT NOT NULL,
                    dueDate INTEGER,
                    isDeleted INTEGER NOT NULL,
                    syncStatus TEXT NOT NULL,
                    updatedAt INTEGER NOT NULL,
                    FOREIGN KEY(customerId) REFERENCES Customers(id) ON UPDATE NO ACTION ON DELETE RESTRICT
                )
            """.trimIndent())

            // 5. Migrate existing rows from LedgerEntries to LedgerEntries_new
            db.execSQL("""
                INSERT INTO LedgerEntries_new (
                    id, uuid, customerId, customerUuid, customerName,
                    type, amountPaisa, originalSpokenText, timestamp,
                    note, dueDate, isDeleted, syncStatus, updatedAt
                )
                SELECT 
                    l.id,
                    lower(hex(randomblob(4)) || '-' || hex(randomblob(2)) || '-4' || substr(hex(randomblob(2)),2) || '-a' || substr(hex(randomblob(2)),2) || '-' || hex(randomblob(6))),
                    l.customerId,
                    COALESCE(c.uuid, ''),
                    l.customerName,
                    l.type,
                    CAST(ROUND(l.amount * 100) AS INTEGER),
                    l.originalSpokenText,
                    l.timestamp,
                    l.note,
                    l.dueDate,
                    0,
                    'PENDING_SYNC',
                    l.timestamp
                FROM LedgerEntries l
                LEFT JOIN Customers c ON l.customerId = c.id
            """.trimIndent())

            // 6. Drop old LedgerEntries table and rename new table
            db.execSQL("DROP TABLE LedgerEntries")
            db.execSQL("ALTER TABLE LedgerEntries_new RENAME TO LedgerEntries")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_LedgerEntries_uuid ON LedgerEntries(uuid)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_LedgerEntries_customerId ON LedgerEntries(customerId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_LedgerEntries_timestamp ON LedgerEntries(timestamp)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_LedgerEntries_syncStatus ON LedgerEntries(syncStatus)")
        }
    }
}
