# Database & Financial Precision Architecture — Baki Bondhu (বাকী বন্ধু)

> **Owner / Engineering**: DevCenterPoint ([devcenterpoint.com](https://devcenterpoint.com))  
> **Target Database Engine**: Room 2.6.1 / SQLite 3  
> **Current Database Version**: `5`  
> **Schema Migrations**: Non-destructive (`MIGRATION_4_5`)

---

## 1. The Zero-Drift Financial Principle

In financial and accounting software, floating-point numbers (`Float`, `Double`) conform to the **IEEE 754 standard**, which represents fractional numbers in base-2 binary. In base-2, common decimal values like $0.1$ or $0.2$ are recurring fractions. Over hundreds of ledger additions and subtractions, accumulation of floating-point inaccuracies leads to off-by-one errors (e.g., $৳\ 0.01$ or $৳\ 0.02$ discrepancies).

In Bangladeshi retail businesses, even a 1-paisa discrepancy erodes merchant confidence.

### The Minor Unit Solution: `Long` Paisa
Baki Bondhu represents **all monetary amounts as integer 64-bit numbers (`Long`)** denominated in minor currency units (**Paisa**):
$$\text{Amount (Paisa)} = \text{Amount (BDT)} \times 100$$

- $৳\ 1.00 = 100\text{ Paisa}$
- $৳\ 500.00 = 50,000\text{ Paisa}$
- $৳\ 1250.50 = 125,050\text{ Paisa}$

All additions, subtractions, debt totals, and daily closing cash flow summaries are computed purely with integer arithmetic.

---

## 2. Room Schema Entities (Version 5)

### 2.1 `CustomerEntity` (`Customers` table)

```kotlin
@Entity(
    tableName = "Customers",
    indices = [
        Index(value = ["name"]),
        Index(value = ["phone"]),
        Index(value = ["uuid"], unique = true)
    ]
)
data class CustomerEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val uuid: String = UUID.randomUUID().toString(),
    val name: String,
    val phone: String = "",
    val balancePaisa: Long = 0L,
    val isArchived: Boolean = false,
    val syncStatus: SyncStatus = SyncStatus.SYNCED,
    val updatedAt: Long = System.currentTimeMillis(),
    val dueDate: Long? = null,
    val lastTransactionAt: Long? = null
)
```

### 2.2 `LedgerEntryEntity` (`LedgerEntries` table)

```kotlin
@Entity(
    tableName = "LedgerEntries",
    foreignKeys = [
        ForeignKey(
            entity = CustomerEntity::class,
            parentColumns = ["id"],
            childColumns = ["customerId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["customerId"]),
        Index(value = ["customerUuid"]),
        Index(value = ["uuid"], unique = true),
        Index(value = ["timestamp"]),
        Index(value = ["isDeleted"])
    ]
)
data class LedgerEntryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val uuid: String = UUID.randomUUID().toString(),
    val customerId: Long,
    val customerUuid: String = "",
    val customerName: String,
    val type: TransactionType, // BAKI_SALE, REPAYMENT, CASH_SALE
    val amountPaisa: Long,
    val note: String? = null,
    val originalSpokenText: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val dueDate: Long? = null,
    val isDeleted: Boolean = false,
    val syncStatus: SyncStatus = SyncStatus.SYNCED,
    val updatedAt: Long = System.currentTimeMillis()
)
```

### 2.3 `ShopSettingsEntity` (`ShopSettings` table)

```kotlin
@Entity(tableName = "ShopSettings")
data class ShopSettingsEntity(
    @PrimaryKey val id: Int = 1,
    val shopName: String = "",
    val ownerName: String = "",
    val bkashNumber: String = "",
    val nagadNumber: String = "",
    val isBangla: Boolean = true,
    val isAudioEnabled: Boolean = true,
    val lastBackupTimestamp: Long = 0L
)
```

---

## 3. Schema Evolution & Migration Strategy

| Database Version | Key Changes Introduced |
| :--- | :--- |
| **v1** | Basic Customer and Transaction tables with ID, Name, Amount (`Double`). |
| **v2** | Added `note` and `originalSpokenText` fields to capture raw voice commands. |
| **v3** | Added database indexes on `Customer.name` and `Customer.phone` for rapid search. |
| **v4** | Added `dueDate` column to track customer repayment promises; added `ShopSettings` table. |
| **v5 (Current)** | **Complete Financial & Cloud Overhaul**: Converted all currency fields to `balancePaisa` and `amountPaisa` (`Long`), generated stable UUIDs, added soft-delete tombstone (`isDeleted`), added `syncStatus` and `updatedAt`, replaced `CASCADE` delete with `RESTRICT` to prevent accidental financial loss. |

### The `MIGRATION_4_5` Implementation

```sql
-- 1. Upgrade Customers table to support Paisa and UUIDs
ALTER TABLE Customers ADD COLUMN balancePaisa INTEGER NOT NULL DEFAULT 0;
ALTER TABLE Customers ADD COLUMN uuid TEXT NOT NULL DEFAULT '';
ALTER TABLE Customers ADD COLUMN isArchived INTEGER NOT NULL DEFAULT 0;
ALTER TABLE Customers ADD COLUMN syncStatus TEXT NOT NULL DEFAULT 'SYNCED';
ALTER TABLE Customers ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0;

-- 2. Convert existing Double balance to Long Paisa
UPDATE Customers SET balancePaisa = CAST(ROUND(balance * 100) AS INTEGER);
UPDATE Customers SET updatedAt = CAST(strftime('%s', 'now') AS INTEGER) * 1000;
UPDATE Customers SET uuid = lower(hex(randomblob(16))) WHERE uuid = '';
CREATE UNIQUE INDEX IF NOT EXISTS index_Customers_uuid ON Customers(uuid);

-- 3. Recreate LedgerEntries with RESTRICT foreign key and Paisa
CREATE TABLE LedgerEntries_new (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    uuid TEXT NOT NULL,
    customerId INTEGER NOT NULL,
    customerUuid TEXT NOT NULL DEFAULT '',
    customerName TEXT NOT NULL,
    type TEXT NOT NULL,
    amountPaisa INTEGER NOT NULL DEFAULT 0,
    note TEXT,
    originalSpokenText TEXT,
    timestamp INTEGER NOT NULL,
    dueDate INTEGER,
    isDeleted INTEGER NOT NULL DEFAULT 0,
    syncStatus TEXT NOT NULL DEFAULT 'SYNCED',
    updatedAt INTEGER NOT NULL DEFAULT 0,
    FOREIGN KEY (customerId) REFERENCES Customers(id) ON DELETE RESTRICT
);

-- 4. Copy and convert existing transactions
INSERT INTO LedgerEntries_new (
    id, uuid, customerId, customerUuid, customerName, type,
    amountPaisa, note, originalSpokenText, timestamp, dueDate,
    isDeleted, syncStatus, updatedAt
)
SELECT 
    id,
    lower(hex(randomblob(16))),
    customerId,
    (SELECT uuid FROM Customers WHERE Customers.id = LedgerEntries.customerId),
    customerName,
    type,
    CAST(ROUND(amount * 100) AS INTEGER),
    note,
    originalSpokenText,
    timestamp,
    dueDate,
    0,
    'SYNCED',
    timestamp
FROM LedgerEntries;

-- 5. Swap tables and recreate indexes
DROP TABLE LedgerEntries;
ALTER TABLE LedgerEntries_new RENAME TO LedgerEntries;
CREATE INDEX IF NOT EXISTS index_LedgerEntries_customerId ON LedgerEntries(customerId);
CREATE INDEX IF NOT EXISTS index_LedgerEntries_customerUuid ON LedgerEntries(customerUuid);
CREATE UNIQUE INDEX IF NOT EXISTS index_LedgerEntries_uuid ON LedgerEntries(uuid);
CREATE INDEX IF NOT EXISTS index_LedgerEntries_timestamp ON LedgerEntries(timestamp);
CREATE INDEX IF NOT EXISTS index_LedgerEntries_isDeleted ON LedgerEntries(isDeleted);
```

---

## 4. Query Optimization & Transaction Safety

1. **Half-Open Intervals for Date Queries**:
   All daily, weekly, and monthly calculations use half-open intervals `[start, end)`:
   ```sql
   SELECT * FROM LedgerEntries 
   WHERE timestamp >= :startTimestamp AND timestamp < :endTimestamp 
     AND isDeleted = 0 
   ORDER BY timestamp DESC
   ```
   This prevents edge-case transaction dropping or double-counting on midnight boundaries.

2. **Atomic Customer Balance Updates**:
   Balances are never calculated in-memory and blindly written back. Instead, `CustomerDao` executes an atomic SQLite delta query:
   ```sql
   UPDATE Customers 
   SET balancePaisa = balancePaisa + :deltaPaisa, 
       updatedAt = :now, 
       lastTransactionAt = :now 
   WHERE id = :customerId
   ```
   Combined with Room's `@Transaction` annotation in `BakiRepository`, ledger entry creation and balance updates either succeed together or roll back completely.
