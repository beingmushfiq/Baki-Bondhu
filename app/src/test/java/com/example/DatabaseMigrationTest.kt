package com.example

import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import com.example.data.local.DatabaseMigrations
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DatabaseMigrationTest {

    @Test
    fun `migration from v4 to v5 converts double to paisa and adds uuid columns without data loss`() {
        val context = RuntimeEnvironment.getApplication()
        val dbFile = context.getDatabasePath("test_migration_4_5.db")
        if (dbFile.exists()) dbFile.delete()

        // 1. Create a raw SQLite database matching Version 4 schema
        val openHelper = FrameworkSQLiteOpenHelperFactory().create(
            androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration.builder(context)
                .name("test_migration_4_5.db")
                .callback(object : androidx.sqlite.db.SupportSQLiteOpenHelper.Callback(4) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        // V4 Customers table
                        db.execSQL("""
                            CREATE TABLE IF NOT EXISTS Customers (
                                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                                name TEXT NOT NULL,
                                phone TEXT NOT NULL,
                                balance REAL NOT NULL,
                                trustScore INTEGER NOT NULL,
                                lastTransactionAt INTEGER NOT NULL,
                                createdAt INTEGER NOT NULL,
                                note TEXT NOT NULL,
                                dueDate INTEGER
                            )
                        """.trimIndent())

                        // V4 LedgerEntries table
                        db.execSQL("""
                            CREATE TABLE IF NOT EXISTS LedgerEntries (
                                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                                customerId INTEGER NOT NULL,
                                customerName TEXT NOT NULL,
                                type TEXT NOT NULL,
                                amount REAL NOT NULL,
                                originalSpokenText TEXT,
                                timestamp INTEGER NOT NULL,
                                note TEXT NOT NULL,
                                dueDate INTEGER,
                                FOREIGN KEY(customerId) REFERENCES Customers(id) ON UPDATE NO ACTION ON DELETE CASCADE
                            )
                        """.trimIndent())
                    }

                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
                })
                .build()
        )

        val v4Db = openHelper.writableDatabase

        // 2. Insert test data with Double values in v4
        v4Db.execSQL("INSERT INTO Customers (id, name, phone, balance, trustScore, lastTransactionAt, createdAt, note, dueDate) VALUES (1, 'কামাল', '01711223344', 1250.50, 3, 1700000000000, 1700000000000, 'দোকানের নিয়মিত খরিদ্দার', NULL)")
        v4Db.execSQL("INSERT INTO LedgerEntries (id, customerId, customerName, type, amount, originalSpokenText, timestamp, note, dueDate) VALUES (1, 1, 'কামাল', 'BAKI_SALE', 1250.50, 'কামাল ১২৫০ টাকা বাকি', 1700000000000, 'চাল ও তেল', NULL)")

        // 3. Execute MIGRATION_4_5
        DatabaseMigrations.MIGRATION_4_5.migrate(v4Db)

        // 4. Verify migrated Customers table in v5
        val customerCursor = v4Db.query("SELECT id, uuid, name, phone, balancePaisa, isArchived, syncStatus FROM Customers WHERE id = 1")
        assertTrue("Customer record must survive migration", customerCursor.moveToFirst())
        assertEquals(1L, customerCursor.getLong(0))
        val customerUuid = customerCursor.getString(1)
        assertNotNull(customerUuid)
        assertTrue("UUID must not be blank", customerUuid.isNotBlank())
        assertEquals("কামাল", customerCursor.getString(2))
        assertEquals("01711223344", customerCursor.getString(3))
        assertEquals(125050L, customerCursor.getLong(4)) // 1250.50 * 100
        assertEquals(0, customerCursor.getInt(5)) // isArchived = false
        assertEquals("PENDING_SYNC", customerCursor.getString(6))
        customerCursor.close()

        // 5. Verify migrated LedgerEntries table in v5
        val txCursor = v4Db.query("SELECT id, uuid, customerId, customerUuid, customerName, amountPaisa, isDeleted, syncStatus FROM LedgerEntries WHERE id = 1")
        assertTrue("Transaction record must survive migration", txCursor.moveToFirst())
        assertEquals(1L, txCursor.getLong(0))
        val txUuid = txCursor.getString(1)
        assertNotNull(txUuid)
        assertTrue("Transaction UUID must not be blank", txUuid.isNotBlank())
        assertEquals(1L, txCursor.getLong(2))
        assertEquals(customerUuid, txCursor.getString(3)) // Joined with customer UUID
        assertEquals("কামাল", txCursor.getString(4))
        assertEquals(125050L, txCursor.getLong(5)) // 1250.50 * 100
        assertEquals(0, txCursor.getInt(6)) // isDeleted = false
        assertEquals("PENDING_SYNC", txCursor.getString(7))
        txCursor.close()

        v4Db.close()
        openHelper.close()
        dbFile.delete()
    }
}
