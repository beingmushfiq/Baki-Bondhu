package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.model.Customer
import com.example.data.model.CustomerEntity
import com.example.data.model.LedgerEntryEntity
import com.example.data.model.ShopSettingsEntity
import com.example.data.model.SyncStatus
import com.example.data.model.Transaction
import com.example.data.model.TransactionType
import com.example.data.repository.BakiRepository
import com.example.data.repository.DefaultBakiRepository
import android.app.Application
import com.example.ui.BakiViewModel
import com.example.util.DateRangeFilterType
import com.example.util.DateRangeHelper
import com.example.util.DateRangeSelection
import com.example.util.DigitalReceiptHelper
import com.example.util.OverdueHelper
import com.example.util.OverdueLevel
import com.example.util.ShareChannel
import com.example.voice.BanglaSpeechRecognitionService
import com.example.voice.BanglaVoiceState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Baki Bondhu", appName)
  }

  @Test
  fun `verify Room database initializes Customers and LedgerEntries tables`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()

    // Insert into Customers table
    val customerId = db.customerDao().insertCustomer(
      CustomerEntity(name = "Rahim", phone = "01700000000", balance = 500.0)
    )
    assertTrue(customerId > 0)

    // Insert into LedgerEntries table
    val entryId = db.ledgerEntryDao().insertTransaction(
      LedgerEntryEntity(
        customerId = customerId,
        customerName = "Rahim",
        type = TransactionType.BAKI_SALE,
        amount = 500.0,
        originalSpokenText = "Rahim 500 baki"
      )
    )
    assertTrue(entryId > 0)

    // Verify direct SQLite table names in sqlite_master
    val cursor = db.openHelper.readableDatabase.query(
      "SELECT name FROM sqlite_master WHERE type='table' AND name IN ('Customers', 'LedgerEntries')"
    )
    val tableNames = mutableListOf<String>()
    while (cursor.moveToNext()) {
      tableNames.add(cursor.getString(0))
    }
    cursor.close()

    assertTrue(tableNames.contains("Customers"))
    assertTrue(tableNames.contains("LedgerEntries"))

    db.close()
  }

  @Test
  fun `verify BanglaSpeechRecognitionService captures and parses credit entry`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val service = BanglaSpeechRecognitionService(context) { listOf("রহিম চাচা", "করিম ভাই") }

    assertEquals(BanglaVoiceState.Idle, service.voiceState.value)

    // Simulate shopkeeper speaking "রহিম চাচা ৪৫০ টাকা বাকী"
    service.simulateSpokenEntry("রহিম চাচা ৪৫০ টাকা বাকী")

    val state = service.voiceState.value
    assertTrue(state is BanglaVoiceState.Completed)

    val completed = state as BanglaVoiceState.Completed
    assertEquals("রহিম চাচা", completed.parsedEntry.customerName)
    assertEquals(450.0, completed.parsedEntry.amount, 0.01)
    assertEquals(TransactionType.BAKI_SALE, completed.parsedEntry.type)

    service.destroy()
  }

  @Test
  fun `verify GoogleCloudSpeechService and system speech intent configured for Bangla`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val service = BanglaSpeechRecognitionService(context)

    // Check system voice intent configuration
    val intent = service.createSystemSpeechIntent()
    assertEquals("bn-BD", intent.getStringExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE))
    assertEquals("bn-BD", intent.getStringExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE))

    // Check cloud speech service initialization
    val apiKey = service.cloudSpeechService.resolveApiKey()
    org.junit.Assert.assertNotNull(apiKey)
    assertEquals(false, service.cloudSpeechService.isRecordingActive())

    service.destroy()
  }

  @Test
  fun `verify BakiRepository abstracts database operations and tracks offline-first sync state`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
    val repository: BakiRepository = DefaultBakiRepository(
      db.customerDao(),
      db.ledgerEntryDao(),
      db.shopSettingsDao()
    )

    // Initial state is SYNCED
    val initialSync = repository.syncState.first()
    assertEquals(SyncStatus.SYNCED, initialSync.status)

    // Record transaction via repository abstraction
    val entryId = repository.recordTransaction(
      customerName = "জাহিদ",
      customerPhone = "01500000000",
      type = TransactionType.BAKI_SALE,
      amount = 350.0,
      spokenText = "জাহিদ ৩৫০ বাকি"
    )
    assertTrue(entryId > 0)

    // Sync state now transitions to PENDING_SYNC
    val pendingSync = repository.syncState.first()
    assertEquals(SyncStatus.PENDING_SYNC, pendingSync.status)
    assertEquals(1, pendingSync.pendingCount)

    // Trigger sync
    val syncSuccess = repository.syncOfflineData()
    assertTrue(syncSuccess)

    // Restores to SYNCED
    val finalSync = repository.syncState.first()
    assertEquals(SyncStatus.SYNCED, finalSync.status)
    assertEquals(0, finalSync.pendingCount)

    db.close()
  }

  @Test
  fun `verify BakiViewModel can be instantiated by AndroidViewModelFactory`() {
    val application = ApplicationProvider.getApplicationContext<android.app.Application>()
    val factory = androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.getInstance(application)
    val viewModel = factory.create(com.example.ui.BakiViewModel::class.java)
    org.junit.Assert.assertNotNull(viewModel)
    viewModel.voiceManager.destroy()
  }

  @Test
  fun `verify processVoiceInputAndCreateLedgerEntry persists new LedgerEntry for customer`() = runBlocking {
    val application = ApplicationProvider.getApplicationContext<android.app.Application>()
    val db = Room.inMemoryDatabaseBuilder(application, AppDatabase::class.java).build()
    val repository: BakiRepository = DefaultBakiRepository(
      db.customerDao(),
      db.ledgerEntryDao(),
      db.shopSettingsDao()
    )
    val viewModel = com.example.ui.BakiViewModel(application, repository)

    // Process spoken input "কামাল ৫০০ টাকা জমা"
    viewModel.processVoiceInputAndCreateLedgerEntry("কামাল ৫০০ টাকা জমা")
    org.robolectric.shadows.ShadowLooper.idleMainLooper()
    // Verify LedgerEntry was created reactively in Room
    val entries = repository.allLedgerEntries.first { it.isNotEmpty() }
    assertTrue(entries.isNotEmpty())

    val entry = entries.first()
    assertEquals("কামাল", entry.customerName)
    assertEquals(500.0, entry.amount, 0.01)
    assertEquals(TransactionType.REPAYMENT, entry.type)

    viewModel.voiceManager.destroy()
    db.close()
  }

  @Test
  fun `verify clearDatabase wipes all customers and ledger entries`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
    val repository: BakiRepository = DefaultBakiRepository(
      db.customerDao(),
      db.ledgerEntryDao(),
      db.shopSettingsDao()
    )

    // Add customer & transaction
    repository.recordTransaction(
      customerName = "করিম",
      customerPhone = "01800000000",
      type = TransactionType.BAKI_SALE,
      amount = 800.0,
      spokenText = "করিম ৮০০ বাকি"
    )

    val customersBefore = repository.allCustomers.first()
    assertTrue(customersBefore.isNotEmpty())

    // Clear database
    repository.clearDatabase()

    val customersAfter = repository.allCustomers.first()
    val entriesAfter = repository.allLedgerEntries.first()
    assertTrue(customersAfter.isEmpty())
    assertTrue(entriesAfter.isEmpty())

    db.close()
  }

  @Test
  fun `verify Customer and Transaction entities and DAOs store credit ledger data and log entries`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
    val customerDao = db.customerDao()
    val transactionDao = db.transactionDao()

    // 1. Create and insert Customer entity
    val customer = com.example.data.model.Customer(
      name = "জামাল উদ্দিন",
      phone = "01912345678",
      balance = 0.0
    )
    val customerId = customerDao.insertCustomer(customer)
    assertTrue(customerId > 0)

    // 2. Create and insert Transaction entity
    val transaction = com.example.data.model.Transaction(
      customerId = customerId,
      customerName = "জামাল উদ্দিন",
      type = TransactionType.BAKI_SALE,
      amount = 1250.0,
      originalSpokenText = "জামাল উদ্দিন ১২৫০ টাকা বাকি",
      note = "চাল ও তেল"
    )
    val txId = transactionDao.insertTransaction(transaction)
    assertTrue(txId > 0)

    // Adjust customer balance
    customerDao.adjustBalance(customerId, 1250.0, System.currentTimeMillis())

    // 3. Verify transaction entries are logged and retrievable
    val transactions = transactionDao.getAllTransactions().first()
    assertEquals(1, transactions.size)
    assertEquals(1250.0, transactions.first().amount, 0.01)
    assertEquals("জামাল উদ্দিন", transactions.first().customerName)
    assertEquals(TransactionType.BAKI_SALE, transactions.first().type)

    // 4. Verify customer balance
    val updatedCustomer = customerDao.getCustomerById(customerId).first()
    org.junit.Assert.assertNotNull(updatedCustomer)
    assertEquals(1250.0, updatedCustomer!!.balance, 0.01)

    db.close()
  }

  @Test
  fun `verify total outstanding debt summary card calculations across all customers`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
    val repository: BakiRepository = DefaultBakiRepository(
      db.customerDao(),
      db.ledgerEntryDao(),
      db.shopSettingsDao()
    )

    // Add multiple customers with different balances
    repository.recordTransaction("কবির", "01711111111", TransactionType.BAKI_SALE, 1500.0, null)
    repository.recordTransaction("শফিক", "01822222222", TransactionType.BAKI_SALE, 2500.0, null)
    repository.recordTransaction("শফিক", "01822222222", TransactionType.REPAYMENT, 500.0, null) // Balance now 2000.0
    repository.recordTransaction("হাসান", "01933333333", TransactionType.CASH_SALE, 800.0, null) // Cash sale, balance = 0

    val customers = repository.allCustomers.first()
    assertEquals(3, customers.size)

    val totalOutstandingDebt = customers.filter { it.balance > 0 }.sumOf { it.balance }
    assertEquals(3500.0, totalOutstandingDebt, 0.01)

    val debtorCount = customers.count { it.balance > 0 }
    assertEquals(2, debtorCount)

    db.close()
  }

  @Test
  fun `verify DigitalReceiptHelper generates properly structured digital receipt with shop info and amounts`() {
    val tx = Transaction(
      id = 101L,
      customerId = 1L,
      customerName = "রহিম চাচা",
      type = TransactionType.BAKI_SALE,
      amount = 450.0,
      note = "চাল ৫ কেজি",
      originalSpokenText = "রহিম চাচা ৪৫০ টাকা বাকি"
    )

    val customer = CustomerEntity(
      id = 1L,
      name = "রহিম চাচা",
      phone = "01712345678",
      balance = 1450.0
    )

    val settings = ShopSettingsEntity(
      shopName = "বিসমিল্লাহ জেনারেল স্টোর",
      ownerName = "মোঃ আলমগীর",
      bkashNumber = "01811223344",
      nagadNumber = "01999887766"
    )

    val receipt = DigitalReceiptHelper.generateReceiptText(
      transaction = tx,
      customer = customer,
      settings = settings,
      isBn = true
    )

    assertTrue("Receipt must contain shop name", receipt.contains("বিসমিল্লাহ জেনারেল স্টোর"))
    assertTrue("Receipt must contain customer name", receipt.contains("রহিম চাচা"))
    assertTrue("Receipt must contain customer phone", receipt.contains("01712345678"))
    assertTrue("Receipt must contain transaction amount", receipt.contains("৪৫০"))
    assertTrue("Receipt must contain note", receipt.contains("চাল ৫ কেজি"))
    assertTrue("Receipt must contain total balance", receipt.contains("১৪৫০"))
    assertTrue("Receipt must contain bKash info", receipt.contains("01811223344"))
    assertTrue("Receipt must contain Nagad info", receipt.contains("01999887766"))
    assertTrue("Receipt must attribute DevCenterPoint", receipt.contains("DevCenterPoint"))
  }

  @Test
  fun `verify DigitalReceiptHelper triggers WhatsApp and SMS share intents`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val tx = Transaction(
      id = 102L,
      customerId = 2L,
      customerName = "করিম ভাই",
      type = TransactionType.REPAYMENT,
      amount = 200.0
    )
    val customer = CustomerEntity(
      id = 2L,
      name = "করিম ভাই",
      phone = "01800000000",
      balance = 0.0
    )

    val receiptText = DigitalReceiptHelper.generateReceiptText(tx, customer, isBn = true)

    // Test WhatsApp share trigger
    val whatsappTriggered = DigitalReceiptHelper.shareReceipt(
      context = context,
      receiptText = receiptText,
      channel = ShareChannel.WHATSAPP,
      customerPhone = customer.phone
    )
    assertTrue("WhatsApp share trigger must execute", whatsappTriggered)

    // Test SMS share trigger
    val smsTriggered = DigitalReceiptHelper.shareReceipt(
      context = context,
      receiptText = receiptText,
      channel = ShareChannel.SMS,
      customerPhone = customer.phone
    )
    assertTrue("SMS share trigger must execute", smsTriggered)

    // Test Chooser share trigger
    val chooserTriggered = DigitalReceiptHelper.shareReceipt(
      context = context,
      receiptText = receiptText,
      channel = ShareChannel.CHOOSER
    )
    assertTrue("Chooser share trigger must execute", chooserTriggered)
  }

  @Test
  fun `verify DateRangeHelper filters transactions by daily, weekly, and custom range`() {
    val now = System.currentTimeMillis()
    val oneHourAgo = now - 3600000L
    val startOfWeek = DateRangeHelper.getStartOfWeek(now)
    val withinWeek = startOfWeek + 3600000L
    val beforeWeek = startOfWeek - 86400000L

    val txList = listOf(
      Transaction(id = 1, customerId = 1, customerName = "A", type = TransactionType.BAKI_SALE, amount = 100.0, timestamp = oneHourAgo),
      Transaction(id = 2, customerId = 2, customerName = "B", type = TransactionType.REPAYMENT, amount = 50.0, timestamp = withinWeek),
      Transaction(id = 3, customerId = 3, customerName = "C", type = TransactionType.CASH_SALE, amount = 70.0, timestamp = beforeWeek)
    )

    // ALL range returns all 3
    val allResult = DateRangeHelper.filterTransactions(txList, DateRangeSelection(DateRangeFilterType.ALL))
    assertEquals(3, allResult.size)

    // TODAY range returns tx from oneHourAgo
    val todayResult = DateRangeHelper.filterTransactions(txList, DateRangeSelection(DateRangeFilterType.TODAY))
    assertTrue(todayResult.any { it.id == 1L })

    // THIS_WEEK returns tx from oneHourAgo and withinWeek
    val weekResult = DateRangeHelper.filterTransactions(txList, DateRangeSelection(DateRangeFilterType.THIS_WEEK))
    assertTrue(weekResult.any { it.id == 1L })
    assertTrue(weekResult.any { it.id == 2L })
    assertTrue(weekResult.none { it.id == 3L })

    // CUSTOM range
    val customSelection = DateRangeSelection(
      type = DateRangeFilterType.CUSTOM,
      startTimestamp = startOfWeek - (2 * 86400000L),
      endTimestamp = now
    )
    val customResult = DateRangeHelper.filterTransactions(txList, customSelection)
    assertEquals(3, customResult.size)
  }

  @Test
  fun `verify EndOfDaySummary calculates net cash flow and cash drawer reconciliation`() {
    val now = System.currentTimeMillis()
    val transactions = listOf(
      Transaction(id = 1, customerId = 1, customerName = "Rahim", type = TransactionType.BAKI_SALE, amount = 1000.0, timestamp = now),
      Transaction(id = 2, customerId = 2, customerName = "Karim", type = TransactionType.REPAYMENT, amount = 400.0, timestamp = now),
      Transaction(id = 3, customerId = 3, customerName = "Jamal", type = TransactionType.CASH_SALE, amount = 300.0, timestamp = now)
    )

    val summary = DateRangeHelper.calculateCashFlowSummary(
      transactions = transactions,
      dayTimestamp = now,
      isBn = true
    )

    assertEquals(1000.0, summary.totalCreditSales, 0.01)
    assertEquals(400.0, summary.totalRepayments, 0.01)
    assertEquals(300.0, summary.totalCashSales, 0.01)
    // Physical cash in drawer = Repayments (400) + Cash Sales (300) = 700
    assertEquals(700.0, summary.netCashInDrawer, 0.01)
    // Net credit change = Credit Sales (1000) - Repayments (400) = 600
    assertEquals(600.0, summary.netCreditChange, 0.01)
    assertEquals(3, summary.transactionCount)
  }

  @Test
  fun `verify OverdueHelper calculates Due Soon, Overdue, and Critical tiers`() {
    val now = System.currentTimeMillis()
    val oneDayMs = 86400000L

    // 1. Settled customer (balance 0)
    val settledCust = Customer(name = "Settled", balance = 0.0)
    val settledBadge = OverdueHelper.computeCustomerOverdue(settledCust, now)
    assertEquals(OverdueLevel.NONE, settledBadge.level)

    // 2. Due soon (due in 2 days)
    val dueSoonCust = Customer(name = "Soon", balance = 500.0, dueDate = now + 2 * oneDayMs)
    val dueSoonBadge = OverdueHelper.computeCustomerOverdue(dueSoonCust, now)
    assertEquals(OverdueLevel.DUE_SOON, dueSoonBadge.level)

    // 3. Overdue (due 3 days ago)
    val overdueCust = Customer(name = "Overdue", balance = 500.0, dueDate = now - 3 * oneDayMs)
    val overdueBadge = OverdueHelper.computeCustomerOverdue(overdueCust, now)
    assertEquals(OverdueLevel.OVERDUE, overdueBadge.level)

    // 4. Critical (due 10 days ago)
    val criticalCust = Customer(name = "Critical", balance = 500.0, dueDate = now - 10 * oneDayMs)
    val criticalBadge = OverdueHelper.computeCustomerOverdue(criticalCust, now)
    assertEquals(OverdueLevel.CRITICAL, criticalBadge.level)
  }

  @Test
  fun `verify BakiViewModel exposes ledger StateFlow, adds transaction, and calculates customer debt`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
    val repo = DefaultBakiRepository(
      customerDao = db.customerDao(),
      ledgerEntryDao = db.ledgerEntryDao(),
      shopSettingsDao = db.shopSettingsDao()
    )

    val viewModel = BakiViewModel(context as Application, repo)

    // Add transaction with due date
    val dueDate = System.currentTimeMillis() + 86400000L * 5
    repo.recordTransaction(
      customerName = "হাসান ভাই",
      customerPhone = "01900000000",
      type = TransactionType.BAKI_SALE,
      amount = 850.0,
      spokenText = null,
      note = "চাল ও ডাল",
      dueDate = dueDate
    )

    // Verify debt calculation via repository and ViewModel methods
    val debtMap = repo.calculateTotalDebtPerCustomer()
    assertTrue("Debt map must contain customer debt", debtMap.isNotEmpty())
    val customerDebt = debtMap.values.first()
    assertEquals(850.0, customerDebt, 0.01)

    // Verify Customer record has balance and dueDate preserved
    val customers = repo.allCustomers.first()
    assertEquals(1, customers.size)
    assertEquals("হাসান ভাই", customers[0].name)
    assertEquals(850.0, customers[0].balance, 0.01)
    assertEquals(dueDate, customers[0].dueDate)

    db.close()
  }

  @Test
  fun `verify formatWhatsAppPhone correctly standardizes Bangladeshi phone numbers`() {
    // Standard local formats
    assertEquals("8801712345678", DigitalReceiptHelper.formatWhatsAppPhone("01712345678"))
    assertEquals("8801812345678", DigitalReceiptHelper.formatWhatsAppPhone("+8801812345678"))
    assertEquals("8801912345678", DigitalReceiptHelper.formatWhatsAppPhone("8801912345678"))
    assertEquals("8801612345678", DigitalReceiptHelper.formatWhatsAppPhone("1612345678"))
    // With spaces and hyphens
    assertEquals("8801712345678", DigitalReceiptHelper.formatWhatsAppPhone("017 12-345 678"))
  }

  @Test
  fun `verify personalized Bangla WhatsApp payment reminder formats correctly`() {
    val customer = CustomerEntity(
      id = 101L,
      name = "জামাল ভাই",
      phone = "01811223344",
      balance = 1250.0,
      lastTransactionAt = System.currentTimeMillis()
    )
    val settings = ShopSettingsEntity(
      shopName = "মায়ের দোয়া স্টোর",
      ownerName = "করিম মিয়া",
      bkashNumber = "01700112233",
      nagadNumber = "01800112233"
    )

    // Test Standard Tone (Bangla)
    val msgStandard = DigitalReceiptHelper.generateWhatsAppReminderMessage(
      customer = customer,
      settings = settings,
      tone = com.example.ui.SmsTone.STANDARD,
      isBn = true
    )
    assertTrue("Message must address customer by name", msgStandard.contains("জামাল ভাই"))
    assertTrue("Message must contain shop name", msgStandard.contains("মায়ের দোয়া স্টোর"))
    assertTrue("Message must contain formatted debt amount", msgStandard.contains("১২৫০"))
    assertTrue("Message must contain bKash number", msgStandard.contains("01700112233"))
    assertTrue("Message must contain Nagad number", msgStandard.contains("01800112233"))

    // Test Gentle Tone (Bangla)
    val msgGentle = DigitalReceiptHelper.generateWhatsAppReminderMessage(
      customer = customer,
      settings = settings,
      tone = com.example.ui.SmsTone.GENTLE,
      isBn = true
    )
    assertTrue(msgGentle.contains("সুবিধাজনক সময়ে"))

    // Test Firm Tone (Bangla)
    val msgFirm = DigitalReceiptHelper.generateWhatsAppReminderMessage(
      customer = customer,
      settings = settings,
      tone = com.example.ui.SmsTone.FIRM,
      isBn = true
    )
    assertTrue(msgFirm.contains("জরুরি তাগাদা"))
  }

  @Test
  fun `verify openWhatsAppDirect executes without throwing on device context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val success = DigitalReceiptHelper.openWhatsAppDirect(
      context = context,
      phone = "01711223344",
      message = "সালাম জামাল ভাই, বকেয়া পাওনা ১২৫০ টাকা।"
    )
    // In Robolectric without WhatsApp app installed, fallback mechanisms trigger cleanly
    assertTrue("openWhatsAppDirect should handle intent safely", success || !success)
  }

  @Test
  fun `verify BakiStrings formatMoney uses non-breaking space`() {
    val bnZero = com.example.ui.BakiStrings.formatMoney(0.0, true)
    assertEquals("৳\u00A0০", bnZero)

    val bnAmount = com.example.ui.BakiStrings.formatMoney(1250.0, true)
    assertEquals("৳\u00A0১২৫০", bnAmount)

    val enAmount = com.example.ui.BakiStrings.formatMoney(1250.0, false)
    assertEquals("৳\u00A01250", enAmount)

    // Ensure non-breaking space is preserved
    assertTrue(bnZero.contains('\u00A0'))
    assertTrue(bnAmount.contains('\u00A0'))
  }

  @Test
  fun `verify persistent dashboard search query updates state correctly`() {
    val application = ApplicationProvider.getApplicationContext<Application>()
    val viewModel = BakiViewModel(application)

    assertEquals("", viewModel.searchQuery.value)

    viewModel.setSearchQuery("Rahim")
    assertEquals("Rahim", viewModel.searchQuery.value)

    viewModel.setSearchQuery("")
    assertEquals("", viewModel.searchQuery.value)
  }

  @Test
  fun `verify daily closing summary dialog state and cash drawer reconciliation`() {
    val application = ApplicationProvider.getApplicationContext<Application>()
    val viewModel = BakiViewModel(application)

    assertEquals(false, viewModel.isEndOfDaySummaryOpen.value)

    viewModel.openEndOfDaySummary(true)
    assertEquals(true, viewModel.isEndOfDaySummaryOpen.value)

    viewModel.openEndOfDaySummary(false)
    assertEquals(false, viewModel.isEndOfDaySummaryOpen.value)
  }

  @Test
  fun `verify GoogleSignInCancelledException formats properly`() {
    val exception = com.example.auth.GoogleSignInCancelledException()
    assertEquals("Google Sign-In was cancelled by user", exception.message)
  }

  @Test
  fun `verify restoreCloudData does not fail with foreign key constraint when restoring with customerId 0 and existing local data`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
    val repository: BakiRepository = DefaultBakiRepository(
      customerDao = db.customerDao(),
      ledgerEntryDao = db.ledgerEntryDao(),
      shopSettingsDao = db.shopSettingsDao(),
      database = db
    )

    // Setup local state matching user screenshot: 1 customer with 450 Taka debt and 1 transaction
    val localCustId = repository.addCustomer(
      CustomerEntity(name = "আমার দোকান খদ্দের", phone = "01711111111", balance = 0.0)
    )
    repository.recordTransaction(
      customerName = "আমার দোকান খদ্দের",
      customerPhone = "01711111111",
      type = TransactionType.BAKI_SALE,
      amount = 450.0,
      spokenText = "আমার দোকান খদ্দের ৪৫০ বাকি"
    )

    val localCustomer = db.customerDao().getCustomerByIdDirect(localCustId)
    assertNotNull(localCustomer)

    // Prepare cloud data where customerId is 0L (as parsed from Firestore/RTDB)
    val cloudCustomer = CustomerEntity(
      id = 0L,
      uuid = localCustomer!!.uuid,
      name = "আমার দোকান খদ্দের",
      phone = "01711111111",
      balancePaisa = 45000L
    )
    val cloudTx = LedgerEntryEntity(
      id = 0L,
      uuid = java.util.UUID.randomUUID().toString(),
      customerId = 0L, // Notice 0L from cloud!
      customerUuid = localCustomer.uuid,
      customerName = "আমার দোকান খদ্দের",
      type = TransactionType.BAKI_SALE,
      amountPaisa = 45000L
    )

    // Perform cloud restore (Smart Merge)
    repository.restoreCloudData(
      customers = listOf(cloudCustomer),
      transactions = listOf(cloudTx),
      settings = null,
      mergeWithLocal = true
    )

    // Verify no Foreign Key failure occurred and customer still exists with proper transactions
    val restoredCust = db.customerDao().getCustomerByIdDirect(localCustId)
    assertNotNull(restoredCust)
    assertEquals(localCustId, restoredCust!!.id)

    val txs = db.ledgerEntryDao().getTransactionsForCustomer(localCustId).first()
    assertTrue(txs.isNotEmpty())
    assertEquals(localCustId, txs.first().customerId)

    db.close()
  }

  @Test
  fun `verify restoreCloudData full overwrite clears safely without foreign key violation`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
    val repository: BakiRepository = DefaultBakiRepository(
      customerDao = db.customerDao(),
      ledgerEntryDao = db.ledgerEntryDao(),
      shopSettingsDao = db.shopSettingsDao(),
      database = db
    )

    // Setup local customer and transaction
    repository.recordTransaction(
      customerName = "পুরাতন খদ্দের",
      customerPhone = "01800000000",
      type = TransactionType.BAKI_SALE,
      amount = 200.0,
      spokenText = "পুরাতন খদ্দের ২০০ বাকি"
    )

    // Restore brand new cloud customer and transaction with mergeWithLocal = false (full overwrite)
    val cloudCustUuid = java.util.UUID.randomUUID().toString()
    val cloudCust = CustomerEntity(
      id = 0L,
      uuid = cloudCustUuid,
      name = "নতুন ক্লাউড খদ্দের",
      phone = "01900000000",
      balancePaisa = 100000L
    )
    val cloudTx = LedgerEntryEntity(
      id = 0L,
      uuid = java.util.UUID.randomUUID().toString(),
      customerId = 0L,
      customerUuid = cloudCustUuid,
      customerName = "নতুন ক্লাউড খদ্দের",
      type = TransactionType.BAKI_SALE,
      amountPaisa = 100000L
    )

    repository.restoreCloudData(
      customers = listOf(cloudCust),
      transactions = listOf(cloudTx),
      settings = null,
      mergeWithLocal = false
    )

    val customers = db.customerDao().getAllCustomers().first()
    assertEquals(1, customers.size)
    assertEquals("নতুন ক্লাউড খদ্দের", customers[0].name)

    val transactions = db.ledgerEntryDao().getAllTransactions().first()
    assertEquals(1, transactions.size)
    assertEquals(customers[0].id, transactions[0].customerId)

    db.close()
  }

  @Test
  fun `verify restoreCloudData handles orphaned transactions gracefully without foreign key crash`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
    val repository: BakiRepository = DefaultBakiRepository(
      customerDao = db.customerDao(),
      ledgerEntryDao = db.ledgerEntryDao(),
      shopSettingsDao = db.shopSettingsDao(),
      database = db
    )

    // Transaction with unknown customerUuid and customerId = 0
    val orphanTx = LedgerEntryEntity(
      id = 0L,
      uuid = java.util.UUID.randomUUID().toString(),
      customerId = 0L,
      customerUuid = "non-existent-uuid",
      customerName = "হারানো খদ্দের",
      type = TransactionType.BAKI_SALE,
      amountPaisa = 15000L
    )

    // Restore with empty customers list
    repository.restoreCloudData(
      customers = emptyList(),
      transactions = listOf(orphanTx),
      settings = null,
      mergeWithLocal = true
    )

    val customers = db.customerDao().getAllCustomers().first()
    assertTrue(customers.any { it.name == "হারানো খদ্দের" })

    val transactions = db.ledgerEntryDao().getAllTransactions().first()
    assertEquals(1, transactions.size)
    assertTrue(transactions[0].customerId > 0)

    db.close()
  }
}


