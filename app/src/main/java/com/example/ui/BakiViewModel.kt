package com.example.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.Customer
import com.example.data.model.CustomerEntity
import com.example.data.model.ShopSettingsEntity
import com.example.data.model.SyncState
import com.example.data.model.SyncStatus
import com.example.data.model.Transaction
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.data.repository.BakiRepository
import com.example.data.repository.DefaultBakiRepository
import com.example.ui.components.BakiToastState
import com.example.ui.components.BakiToastType
import com.example.util.DailyCashFlowSummary
import com.example.util.DateRangeFilterType
import com.example.util.DateRangeHelper
import com.example.util.DateRangeSelection
import com.example.util.DigitalReceiptHelper
import com.example.util.OverdueHelper
import com.example.util.ShareChannel
import com.example.voice.BanglaVoiceParser
import com.example.voice.ParsedVoiceResult
import com.example.voice.SpeechState
import com.example.voice.VoiceSpeechManager
import com.example.auth.GoogleAuthManager
import com.example.data.cloud.FirestoreSyncRepository
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Calendar

data class BakiUiState(
    val customers: List<CustomerEntity> = emptyList(),
    val transactions: List<TransactionEntity> = emptyList(),
    val allTransactionsUnfiltered: List<TransactionEntity> = emptyList(),
    val settings: ShopSettingsEntity = ShopSettingsEntity(),
    val syncState: SyncState = SyncState(),
    val isBangla: Boolean = true,
    val totalBaki: Double = 0.0,
    val todayBakiSales: Double = 0.0,
    val todayRepayments: Double = 0.0,
    val todayCashSales: Double = 0.0,
    val cashDrawerBalance: Double = 0.0,
    val isVoiceSheetOpen: Boolean = false,
    val isSelectCustomerSheetOpen: Boolean = false,
    val pendingConfirmation: ParsedVoiceResult? = null,
    val isEditingParsedResult: Boolean = false,
    val selectedCustomerForDetail: CustomerEntity? = null,
    val customerForSms: CustomerEntity? = null,
    val smsTone: SmsTone = SmsTone.GENTLE,
    val isAddCustomerOpen: Boolean = false,
    val isManualEntryOpen: Boolean = false,
    val voiceFallbackSnippet: String? = null,
    val isSettingsOpen: Boolean = false,
    val searchQuery: String = "",
    val messageSnackbar: String? = null,
    val selectedTransactionIds: Set<Long> = emptySet(),
    val isSelectionModeActive: Boolean = false,
    val transactionTypeFilter: TransactionType? = null,
    val transactionToEdit: Transaction? = null,
    val toastState: BakiToastState? = null,
    val transactionForReceipt: Transaction? = null,
    val dateRangeSelection: DateRangeSelection = DateRangeSelection(DateRangeFilterType.ALL),
    val isCustomDateRangeOpen: Boolean = false,
    val endOfDaySummary: DailyCashFlowSummary = DailyCashFlowSummary(),
    val isEndOfDaySummaryOpen: Boolean = false,
    val customerDebtMap: Map<Long, Double> = emptyMap(),
    val currentUser: FirebaseUser? = null,
    val isSyncingCloud: Boolean = false,
    val lastCloudBackupAt: Long? = null,
    val autoBackupEnabled: Boolean = true,
    val isArchiveZoneOpen: Boolean = false,
    val archivedCustomers: List<CustomerEntity> = emptyList(),
    val archivedTransactions: List<TransactionEntity> = emptyList(),
    val updateInfo: com.example.util.AppUpdateInfo? = null,
    val isCheckingUpdate: Boolean = false,
    val updateDownloadProgress: Int? = null
)

class BakiViewModel(
    application: Application,
    private val repository: BakiRepository,
    private val authManager: GoogleAuthManager = GoogleAuthManager(application),
    private val syncRepository: FirestoreSyncRepository = FirestoreSyncRepository(application)
) : AndroidViewModel(application) {

    constructor(application: Application) : this(
        application = application,
        repository = DefaultBakiRepository(
            AppDatabase.getInstance(application).customerDao(),
            AppDatabase.getInstance(application).ledgerEntryDao(),
            AppDatabase.getInstance(application).shopSettingsDao()
        ),
        authManager = GoogleAuthManager(application),
        syncRepository = FirestoreSyncRepository(application)
    )

    private val _isSyncingCloud = MutableStateFlow(false)
    val isSyncingCloud: StateFlow<Boolean> = _isSyncingCloud.asStateFlow()

    private val _lastCloudBackupAt = MutableStateFlow<Long?>(null)
    val lastCloudBackupAt: StateFlow<Long?> = _lastCloudBackupAt.asStateFlow()

    private val _autoBackupEnabled = MutableStateFlow(true)
    val autoBackupEnabled: StateFlow<Boolean> = _autoBackupEnabled.asStateFlow()

    // Cloud Restore Preview & State
    private val _isRestorePreviewOpen = MutableStateFlow(false)
    val isRestorePreviewOpen: StateFlow<Boolean> = _isRestorePreviewOpen.asStateFlow()

    private val _restorePreviewData = MutableStateFlow<com.example.ui.components.CloudRestorePreviewData?>(null)
    val restorePreviewData: StateFlow<com.example.ui.components.CloudRestorePreviewData?> = _restorePreviewData.asStateFlow()

    private val _isFetchingRestorePreview = MutableStateFlow(false)
    val isFetchingRestorePreview: StateFlow<Boolean> = _isFetchingRestorePreview.asStateFlow()

    // Archive Zone & In-App Update
    private val _isArchiveZoneOpen = MutableStateFlow(false)
    val isArchiveZoneOpen: StateFlow<Boolean> = _isArchiveZoneOpen.asStateFlow()

    val inAppUpdateManager by lazy { com.example.util.InAppUpdateManager(getApplication()) }
    private val _updateInfo = MutableStateFlow<com.example.util.AppUpdateInfo?>(null)
    val updateInfo: StateFlow<com.example.util.AppUpdateInfo?> = _updateInfo.asStateFlow()

    private val _isCheckingUpdate = MutableStateFlow(false)
    val isCheckingUpdate: StateFlow<Boolean> = _isCheckingUpdate.asStateFlow()

    private val _updateDownloadProgress = MutableStateFlow<Int?>(null)
    val updateDownloadProgress: StateFlow<Int?> = _updateDownloadProgress.asStateFlow()

    val voiceManager = VoiceSpeechManager(application) {
        uiState.value.customers.map { it.name }
    }

    private val _isVoiceSheetOpen = MutableStateFlow(false)
    val isVoiceSheetOpen: StateFlow<Boolean> = _isVoiceSheetOpen.asStateFlow()

    private val _isSelectCustomerSheetOpen = MutableStateFlow(false)
    val isSelectCustomerSheetOpen: StateFlow<Boolean> = _isSelectCustomerSheetOpen.asStateFlow()

    private val _pendingConfirmation = MutableStateFlow<ParsedVoiceResult?>(null)
    val pendingConfirmation: StateFlow<ParsedVoiceResult?> = _pendingConfirmation.asStateFlow()

    private val _isEditingParsedResult = MutableStateFlow(false)
    val isEditingParsedResult: StateFlow<Boolean> = _isEditingParsedResult.asStateFlow()

    private val _selectedCustomerForDetail = MutableStateFlow<CustomerEntity?>(null)
    val selectedCustomerForDetail: StateFlow<CustomerEntity?> = _selectedCustomerForDetail.asStateFlow()

    private val _customerForSms = MutableStateFlow<CustomerEntity?>(null)
    val customerForSms: StateFlow<CustomerEntity?> = _customerForSms.asStateFlow()

    private val _smsTone = MutableStateFlow(SmsTone.GENTLE)
    val smsTone: StateFlow<SmsTone> = _smsTone.asStateFlow()

    private val _isAddCustomerOpen = MutableStateFlow(false)
    val isAddCustomerOpen: StateFlow<Boolean> = _isAddCustomerOpen.asStateFlow()

    private val _isManualEntryOpen = MutableStateFlow(false)
    val isManualEntryOpen: StateFlow<Boolean> = _isManualEntryOpen.asStateFlow()

    private val _voiceFallbackSnippet = MutableStateFlow<String?>(null)
    val voiceFallbackSnippet: StateFlow<String?> = _voiceFallbackSnippet.asStateFlow()

    private val _isSettingsOpen = MutableStateFlow(false)
    val isSettingsOpen: StateFlow<Boolean> = _isSettingsOpen.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _messageSnackbar = MutableStateFlow<String?>(null)
    val messageSnackbar: StateFlow<String?> = _messageSnackbar.asStateFlow()

    // Multi-Select Batch Management
    private val _selectedTransactionIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedTransactionIds: StateFlow<Set<Long>> = _selectedTransactionIds.asStateFlow()

    private val _isSelectionModeActive = MutableStateFlow(false)
    val isSelectionModeActive: StateFlow<Boolean> = _isSelectionModeActive.asStateFlow()

    // Transaction Filtering
    private val _transactionTypeFilter = MutableStateFlow<TransactionType?>(null)
    val transactionTypeFilter: StateFlow<TransactionType?> = _transactionTypeFilter.asStateFlow()

    // Transaction Editing
    private val _transactionToEdit = MutableStateFlow<Transaction?>(null)
    val transactionToEdit: StateFlow<Transaction?> = _transactionToEdit.asStateFlow()

    // Floating Animated Toast Notification
    private val _toastState = MutableStateFlow<BakiToastState?>(null)
    val toastState: StateFlow<BakiToastState?> = _toastState.asStateFlow()

    // Individual Transaction Digital Receipt
    private val _transactionForReceipt = MutableStateFlow<Transaction?>(null)
    val transactionForReceipt: StateFlow<Transaction?> = _transactionForReceipt.asStateFlow()

    // Date Range Filtering
    private val _dateRangeSelection = MutableStateFlow(DateRangeSelection(DateRangeFilterType.ALL))
    val dateRangeSelection: StateFlow<DateRangeSelection> = _dateRangeSelection.asStateFlow()

    private val _isCustomDateRangeOpen = MutableStateFlow(false)
    val isCustomDateRangeOpen: StateFlow<Boolean> = _isCustomDateRangeOpen.asStateFlow()

    // End-of-Day Summary & Cash Drawer Reconciliation
    private val _isEndOfDaySummaryOpen = MutableStateFlow(false)
    val isEndOfDaySummaryOpen: StateFlow<Boolean> = _isEndOfDaySummaryOpen.asStateFlow()

    // In-Memory Undo buffer for deleted transactions
    private var undoTransactionBuffer: List<Transaction> = emptyList()

    init {
        // Observe voice manager speech state for final result
        viewModelScope.launch {
            voiceManager.speechState.collect { speechState ->
                if (speechState is SpeechState.FinalResult) {
                    processRecognizedSpeech(speechState.text)
                }
            }
        }
    }

    val uiState: StateFlow<BakiUiState> = combine(
        listOf(
            repository.allCustomers,
            repository.allTransactions,
            repository.shopSettings,
            repository.syncState,
            _isVoiceSheetOpen,
            _isSelectCustomerSheetOpen,
            _pendingConfirmation,
            _isEditingParsedResult,
            _selectedCustomerForDetail,
            _customerForSms,
            _smsTone,
            _isAddCustomerOpen,
            _isManualEntryOpen,
            _voiceFallbackSnippet,
            _isSettingsOpen,
            _searchQuery,
            _messageSnackbar,
            _selectedTransactionIds,
            _isSelectionModeActive,
            _transactionTypeFilter,
            _transactionToEdit,
            _toastState,
            _transactionForReceipt,
            _dateRangeSelection,
            _isCustomDateRangeOpen,
            _isEndOfDaySummaryOpen,
            authManager.currentUser,
            _isSyncingCloud,
            _lastCloudBackupAt,
            _autoBackupEnabled,
            _isArchiveZoneOpen,
            repository.archivedCustomers,
            repository.archivedTransactions,
            _updateInfo,
            _isCheckingUpdate,
            _updateDownloadProgress
        )
    ) { args: Array<Any?> ->
        @Suppress("UNCHECKED_CAST")
        val customers = (args[0] as? List<CustomerEntity>) ?: emptyList()
        @Suppress("UNCHECKED_CAST")
        val rawTransactions = (args[1] as? List<TransactionEntity>) ?: emptyList()
        val settings = (args[2] as? ShopSettingsEntity) ?: ShopSettingsEntity()
        val sync = (args[3] as? SyncState) ?: SyncState()
        val voiceOpen = args[4] as Boolean
        val selectCustOpen = args[5] as Boolean
        val pending = args[6] as? ParsedVoiceResult
        val isEditing = args[7] as Boolean
        val selectedCust = args[8] as? CustomerEntity
        val smsCust = args[9] as? CustomerEntity
        val tone = args[10] as SmsTone
        val addCustOpen = args[11] as Boolean
        val manualOpen = args[12] as Boolean
        val fallbackSnippet = args[13] as? String
        val settingsOpen = args[14] as Boolean
        val query = args[15] as String
        val msg = args[16] as? String
        @Suppress("UNCHECKED_CAST")
        val selectedTxIds = (args[17] as? Set<Long>) ?: emptySet()
        val selectionActive = args[18] as Boolean
        val filter = args[19] as? TransactionType
        val editTx = args[20] as? Transaction
        val toast = args[21] as? BakiToastState
        val receiptTx = args[22] as? Transaction
        val dateRange = (args[23] as? DateRangeSelection) ?: DateRangeSelection(DateRangeFilterType.ALL)
        val customDateRangeOpen = args[24] as Boolean
        val endOfDayOpen = args[25] as Boolean
        val user = args[26] as? FirebaseUser
        val syncingCloud = args[27] as Boolean
        val lastBackupAt = args[28] as? Long
        val autoBackup = args[29] as Boolean
        val archiveZoneOpen = args[30] as Boolean
        @Suppress("UNCHECKED_CAST")
        val archCustomers = (args[31] as? List<CustomerEntity>) ?: emptyList()
        @Suppress("UNCHECKED_CAST")
        val archTransactions = (args[32] as? List<TransactionEntity>) ?: emptyList()
        val updateInfo = args[33] as? com.example.util.AppUpdateInfo
        val checkingUpdate = args[34] as Boolean
        val updateProgress = args[35] as? Int

        val totalBaki = customers.filter { it.balance > 0 }.sumOf { it.balance }
        val customerDebtMap = customers.associate { it.id to it.balance }

        val startOfDay = getStartOfDayTimestamp()
        val todayTx = rawTransactions.filter { it.timestamp >= startOfDay }
        val todayBakiSales = todayTx
            .filter { it.type == TransactionType.BAKI_SALE }
            .sumOf { it.amount }
        val todayRepayments = todayTx
            .filter { it.type == TransactionType.REPAYMENT }
            .sumOf { it.amount }
        val todayCashSales = todayTx
            .filter { it.type == TransactionType.CASH_SALE }
            .sumOf { it.amount }
        val cashDrawerBalance = todayCashSales + todayRepayments

        val isBn = settings.language == "bn"

        // Filter transactions by date range and transaction type
        val dateFiltered = DateRangeHelper.filterTransactions(rawTransactions, dateRange)
        val visibleTransactions = if (filter != null) {
            dateFiltered.filter { it.type == filter }
        } else {
            dateFiltered
        }

        // Daily closing cash flow summary
        val endOfDaySummary = DateRangeHelper.calculateCashFlowSummary(
            transactions = rawTransactions,
            dayTimestamp = System.currentTimeMillis(),
            isBn = isBn
        )

        BakiUiState(
            customers = customers,
            transactions = visibleTransactions,
            allTransactionsUnfiltered = rawTransactions,
            settings = settings,
            syncState = sync,
            isBangla = isBn,
            totalBaki = totalBaki,
            todayBakiSales = todayBakiSales,
            todayRepayments = todayRepayments,
            todayCashSales = todayCashSales,
            cashDrawerBalance = cashDrawerBalance,
            isVoiceSheetOpen = voiceOpen,
            isSelectCustomerSheetOpen = selectCustOpen,
            pendingConfirmation = pending,
            isEditingParsedResult = isEditing,
            selectedCustomerForDetail = selectedCust,
            customerForSms = smsCust,
            smsTone = tone,
            isAddCustomerOpen = addCustOpen,
            isManualEntryOpen = manualOpen,
            voiceFallbackSnippet = fallbackSnippet,
            isSettingsOpen = settingsOpen,
            searchQuery = query,
            messageSnackbar = msg,
            selectedTransactionIds = selectedTxIds,
            isSelectionModeActive = selectionActive,
            transactionTypeFilter = filter,
            transactionToEdit = editTx,
            toastState = toast,
            transactionForReceipt = receiptTx,
            dateRangeSelection = dateRange,
            isCustomDateRangeOpen = customDateRangeOpen,
            endOfDaySummary = endOfDaySummary,
            isEndOfDaySummaryOpen = endOfDayOpen,
            customerDebtMap = customerDebtMap,
            currentUser = user,
            isSyncingCloud = syncingCloud,
            lastCloudBackupAt = lastBackupAt,
            autoBackupEnabled = autoBackup,
            isArchiveZoneOpen = archiveZoneOpen,
            archivedCustomers = archCustomers,
            archivedTransactions = archTransactions,
            updateInfo = updateInfo,
            isCheckingUpdate = checkingUpdate,
            updateDownloadProgress = updateProgress
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = BakiUiState()
    )

    private fun getStartOfDayTimestamp(): Long {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis
    }

    // --- Toast Notification Helpers ---
    fun showToast(
        message: String,
        type: BakiToastType = BakiToastType.SUCCESS,
        actionLabel: String? = null,
        onAction: (() -> Unit)? = null
    ) {
        _toastState.value = BakiToastState(
            id = System.currentTimeMillis(),
            message = message,
            type = type,
            actionLabel = actionLabel,
            onAction = onAction
        )
    }

    fun dismissToast() {
        _toastState.value = null
    }

    // --- Multi-Select Batch Operations ---
    fun toggleSelectionMode(active: Boolean? = null) {
        val nextState = active ?: !_isSelectionModeActive.value
        _isSelectionModeActive.value = nextState
        if (!nextState) {
            _selectedTransactionIds.value = emptySet()
        }
    }

    fun toggleSelectTransaction(id: Long) {
        val current = _selectedTransactionIds.value.toMutableSet()
        if (current.contains(id)) {
            current.remove(id)
        } else {
            current.add(id)
        }
        _selectedTransactionIds.value = current
        if (current.isNotEmpty() && !_isSelectionModeActive.value) {
            _isSelectionModeActive.value = true
        } else if (current.isEmpty() && _isSelectionModeActive.value) {
            // Keep selection mode open unless user explicitly cancels
        }
    }

    fun selectAllTransactions(ids: List<Long>) {
        _selectedTransactionIds.value = ids.toSet()
        _isSelectionModeActive.value = true
    }

    fun clearTransactionSelection() {
        _selectedTransactionIds.value = emptySet()
        _isSelectionModeActive.value = false
    }

    fun deleteSelectedTransactions() {
        val idsToDelete = _selectedTransactionIds.value
        if (idsToDelete.isEmpty()) return

        viewModelScope.launch {
            val deletedRecords = repository.deleteTransactionsBatch(idsToDelete)
            undoTransactionBuffer = deletedRecords
            clearTransactionSelection()

            val isBn = uiState.value.isBangla
            val msg = if (isBn) {
                "${deletedRecords.size}টি হিসাব মুছে ফেলা হয়েছে"
            } else {
                "Deleted ${deletedRecords.size} transactions"
            }

            showToast(
                message = msg,
                type = BakiToastType.WARNING,
                actionLabel = if (isBn) "পূর্বাবস্থায় ফিরুন" else "Undo",
                onAction = { restoreDeletedTransactions() }
            )
        }
    }

    fun deleteSingleTransaction(tx: Transaction) {
        viewModelScope.launch {
            val deleted = repository.deleteTransaction(tx.id)
            if (deleted != null) {
                undoTransactionBuffer = listOf(deleted)
                val isBn = uiState.value.isBangla
                val msg = if (isBn) {
                    "${tx.customerName}-এর ৳${tx.amount.toInt()} হিসাব মুছে ফেলা হয়েছে"
                } else {
                    "Deleted transaction for ${tx.customerName}"
                }
                showToast(
                    message = msg,
                    type = BakiToastType.WARNING,
                    actionLabel = if (isBn) "পূর্বাবস্থায় ফিরুন" else "Undo",
                    onAction = { restoreDeletedTransactions() }
                )
            }
        }
    }

    fun restoreDeletedTransactions() {
        val toRestore = undoTransactionBuffer
        if (toRestore.isEmpty()) return

        viewModelScope.launch {
            repository.restoreTransactionsBatch(toRestore)
            undoTransactionBuffer = emptyList()
            val isBn = uiState.value.isBangla
            showToast(
                message = if (isBn) "মুছে ফেলা হিসাব পুনরুদ্ধার করা হয়েছে" else "Restored transactions successfully",
                type = BakiToastType.SUCCESS
            )
        }
    }

    // --- Transaction Filter ---
    fun setTransactionFilter(type: TransactionType?) {
        _transactionTypeFilter.value = type
    }

    // --- Transaction Editing ---
    fun openEditTransactionDialog(tx: Transaction?) {
        _transactionToEdit.value = tx
    }

    fun saveEditedTransaction(oldTx: Transaction, newTx: Transaction) {
        viewModelScope.launch {
            repository.updateTransaction(oldTx, newTx)
            _transactionToEdit.value = null
            val isBn = uiState.value.isBangla
            showToast(
                message = if (isBn) "হিসাব সফলভাবে হালনাগাদ করা হয়েছে" else "Transaction updated successfully",
                type = BakiToastType.SUCCESS
            )
        }
    }

    // --- WhatsApp / SMS Statement Share ---
    fun shareStatement(context: Context, customer: CustomerEntity, transactions: List<Transaction>) {
        val isBn = uiState.value.isBangla
        val settings = uiState.value.settings
        val customerTx = transactions.filter { it.customerId == customer.id }.take(5)

        val statementText = buildString {
            appendLine("📜 *${settings.shopName.ifBlank { "দোকান বাকি খাতা" }}*")
            appendLine("খরিদ্দার: ${customer.name}")
            if (customer.phone.isNotBlank()) appendLine("মোবাইল: ${customer.phone}")
            appendLine("তারিখ: ${java.text.SimpleDateFormat("dd/MM/yyyy hh:mm a", java.util.Locale.getDefault()).format(java.util.Date())}")
            appendLine("─────────────────")
            appendLine("বর্তমান মোট বকেয়া: *৳ ${customer.balance.toInt()}*")
            appendLine("─────────────────")
            if (customerTx.isNotEmpty()) {
                appendLine("সাম্প্রতিক লেনদেন:")
                customerTx.forEach { tx ->
                    val typeLabel = when (tx.type) {
                        TransactionType.BAKI_SALE -> "বাকি বিক্রি"
                        TransactionType.REPAYMENT -> "জমা / পরিশোধ"
                        TransactionType.CASH_SALE -> "নগদ বিক্রি"
                    }
                    appendLine("• $typeLabel: ৳${tx.amount.toInt()} (${java.text.SimpleDateFormat("dd/MM", java.util.Locale.getDefault()).format(java.util.Date(tx.timestamp))})")
                }
                appendLine("─────────────────")
            }
            if (settings.bkashNumber.isNotBlank() || settings.nagadNumber.isNotBlank()) {
                appendLine("বকেয়া পরিশোধের নম্বর:")
                if (settings.bkashNumber.isNotBlank()) appendLine("bKash (Send Money): ${settings.bkashNumber}")
                if (settings.nagadNumber.isNotBlank()) appendLine("Nagad: ${settings.nagadNumber}")
                appendLine("─────────────────")
            }
            appendLine("কারিগরি সহায়তায়: DevCenterPoint (devcenterpoint.com)")
        }

        try {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, statementText)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(shareIntent, if (isBn) "হিসাব বিবরণী শেয়ার করুন" else "Share Statement"))
        } catch (_: Exception) {}
    }

    // --- Individual Transaction Digital Receipt Share ---
    fun openReceiptDialog(tx: Transaction?) {
        _transactionForReceipt.value = tx
    }

    fun shareTransaction(
        context: Context,
        tx: Transaction,
        channel: ShareChannel = ShareChannel.CHOOSER
    ) {
        val customer = uiState.value.customers.find { it.id == tx.customerId }
        val isBn = uiState.value.isBangla
        val receiptText = DigitalReceiptHelper.generateReceiptText(
            transaction = tx,
            customer = customer,
            settings = uiState.value.settings,
            isBn = isBn
        )
        val success = DigitalReceiptHelper.shareReceipt(
            context = context,
            receiptText = receiptText,
            channel = channel,
            customerPhone = customer?.phone?.ifBlank { null },
            title = if (isBn) "রসিদ পাঠান / Share Receipt" else "Share Digital Receipt"
        )
        if (success) {
            val channelName = when (channel) {
                ShareChannel.WHATSAPP -> "WhatsApp"
                ShareChannel.SMS -> "SMS"
                ShareChannel.CHOOSER -> if (isBn) "শেয়ার মেনু" else "Share"
            }
            showToast(
                message = if (isBn) "$channelName এর মাধ্যমে রসিদ প্রস্তুত হচ্ছে..." else "Preparing receipt for $channelName...",
                type = BakiToastType.INFO
            )
        }
    }

    fun copyReceiptText(context: Context, tx: Transaction) {
        val customer = uiState.value.customers.find { it.id == tx.customerId }
        val isBn = uiState.value.isBangla
        val receiptText = DigitalReceiptHelper.generateReceiptText(
            transaction = tx,
            customer = customer,
            settings = uiState.value.settings,
            isBn = isBn
        )
        DigitalReceiptHelper.copyToClipboard(context, receiptText, "Baki Bondhu Receipt")
        showToast(
            message = if (isBn) "ডিজিটাল রসিদ ক্লিপবোর্ডে কপি করা হয়েছে" else "Digital receipt copied to clipboard",
            type = BakiToastType.SUCCESS
        )
    }

    // --- Date-Range Filtering System ---
    fun setDateRangeFilter(type: DateRangeFilterType, startMs: Long? = null, endMs: Long? = null) {
        _dateRangeSelection.value = DateRangeSelection(
            type = type,
            startTimestamp = startMs,
            endTimestamp = endMs
        )
        val isBn = uiState.value.isBangla
        val label = _dateRangeSelection.value.getDisplayLabel(isBn)
        showToast(
            message = if (isBn) "ফিল্টার: $label" else "Filter: $label",
            type = BakiToastType.INFO
        )
    }

    fun openCustomDateRangeDialog(isOpen: Boolean) {
        _isCustomDateRangeOpen.value = isOpen
    }

    // --- End-of-Day Summary & Cash Reconciliation ---
    fun openEndOfDaySummary(isOpen: Boolean) {
        _isEndOfDaySummaryOpen.value = isOpen
    }

    fun reconcileCashDrawer(context: Context) {
        val isBn = uiState.value.isBangla
        val summary = uiState.value.endOfDaySummary
        val amountFormatted = BakiStrings.formatMoney(summary.netCashInDrawer, isBn)
        showToast(
            message = if (isBn) "ক্যাশ ড্রয়ার রিকনসিল করা হয়েছে ($amountFormatted নগদ নিশ্চিত)" else "Cash drawer reconciled ($amountFormatted verified)",
            type = BakiToastType.SUCCESS
        )
        _isEndOfDaySummaryOpen.value = false
    }

    fun shareEndOfDayStatement(context: Context) {
        val isBn = uiState.value.isBangla
        val summary = uiState.value.endOfDaySummary
        val text = DateRangeHelper.formatClosingStatement(
            summary = summary,
            shopName = uiState.value.settings.shopName,
            ownerName = uiState.value.settings.ownerName,
            isBn = isBn
        )
        try {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
            }
            val chooser = Intent.createChooser(shareIntent, if (isBn) "দৈনিক ক্লোজিং হিসাব শেয়ার করুন" else "Share Closing Summary").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (_: Exception) {}
    }

    // --- Overdue & Promised Due Date ---
    fun setCustomerPromisedDueDate(customerId: Long, dueDate: Long?) {
        viewModelScope.launch {
            repository.updateCustomerDueDate(customerId, dueDate)
            val isBn = uiState.value.isBangla
            showToast(
                message = if (isBn) "পরিশোধের প্রতিশ্রুতি তারিখ সংরক্ষিত হয়েছে" else "Promised due date updated",
                type = BakiToastType.SUCCESS
            )
        }
    }

    // --- Core Ledger State & Debt Calculations ---
    fun calculateTotalDebtPerCustomer(): Map<Long, Double> {
        return uiState.value.customerDebtMap
    }

    fun calculateTotalDebtForCustomer(customerId: Long): Double {
        return uiState.value.customerDebtMap[customerId] ?: 0.0
    }

    fun addTransaction(
        customerName: String,
        customerPhone: String?,
        amount: Double,
        type: TransactionType,
        note: String = "",
        dueDate: Long? = null
    ) {
        viewModelScope.launch {
            repository.recordTransaction(
                customerName = customerName,
                customerPhone = customerPhone,
                type = type,
                amount = amount,
                spokenText = null,
                note = note,
                dueDate = dueDate
            )
            val isBn = uiState.value.isBangla
            val money = BakiStrings.formatMoney(amount, isBn)
            showToast(
                message = if (isBn) "$customerName-এর হিসাব যুক্ত হয়েছে ($money)" else "Recorded for $customerName ($money)",
                type = BakiToastType.SUCCESS
            )
            triggerAutoCloudBackup()
        }
    }

    // --- Voice Logic ---
    fun openVoiceSheet() {
        _isVoiceSheetOpen.value = true
        _pendingConfirmation.value = null
        _isEditingParsedResult.value = false
        voiceManager.startListening()
    }

    fun closeVoiceSheet() {
        voiceManager.stopListening()
        _isVoiceSheetOpen.value = false
        _pendingConfirmation.value = null
        _isEditingParsedResult.value = false
    }

    fun cancelVoiceAndClose() {
        voiceManager.cancelCapture()
        _pendingConfirmation.value = null
        _isEditingParsedResult.value = false
        _isVoiceSheetOpen.value = false
    }

    fun resetVoiceConfirmation() {
        _pendingConfirmation.value = null
        _isEditingParsedResult.value = false
        voiceManager.restartListening()
    }

    fun processVoiceInputAndCreateLedgerEntry(spokenText: String) {
        val knownNames = uiState.value.customers.map { it.name }
        val parsed = BanglaVoiceParser.parse(spokenText, knownNames)
        _pendingConfirmation.value = parsed
        _isVoiceSheetOpen.value = false
        _isSelectCustomerSheetOpen.value = false

        viewModelScope.launch {
            repository.recordTransaction(
                customerName = parsed.customerName,
                customerPhone = null,
                type = parsed.type,
                amount = parsed.amount,
                spokenText = spokenText
            )
            val isBn = uiState.value.isBangla
            showToast(
                message = if (isBn) {
                    "${parsed.customerName}-এর হিসাব যুক্ত হয়েছে (৳${parsed.amount.toInt()})"
                } else {
                    "Recorded entry for ${parsed.customerName}"
                },
                type = BakiToastType.SUCCESS
            )
        }
    }

    fun processRecognizedSpeech(text: String) {
        val knownNames = uiState.value.customers.map { it.name }
        val parsed = BanglaVoiceParser.parse(text, knownNames)
        _pendingConfirmation.value = parsed

        val isAutoReadBack = uiState.value.settings.autoReadBack
        if (isAutoReadBack) {
            val phrase = parsed.toReadBackPhrase(uiState.value.isBangla)
            voiceManager.speak(phrase, uiState.value.isBangla)
        }
    }

    fun simulateVoicePhrase(phrase: String) {
        voiceManager.simulatePhrase(phrase)
    }

    fun updatePendingResult(customerName: String, amount: Double, type: TransactionType) {
        val current = _pendingConfirmation.value ?: return
        val amountPaisa = com.example.util.Money.takaToPaisa(amount)
        _pendingConfirmation.value = current.copy(
            customerName = customerName,
            amountPaisa = amountPaisa,
            type = type,
            isAmountMissing = (amountPaisa <= 0)
        )
        _isEditingParsedResult.value = false
    }

    fun updatePendingParsed(customerName: String, amount: Double, type: TransactionType) {
        updatePendingResult(customerName, amount, type)
    }

    fun setEditingParsedResult(editing: Boolean) {
        _isEditingParsedResult.value = editing
    }

    fun toggleEditMode(editing: Boolean) {
        setEditingParsedResult(editing)
    }

    fun openSelectCustomerSheet() {
        _isSelectCustomerSheetOpen.value = true
    }

    fun closeSelectCustomerSheet() {
        _isSelectCustomerSheetOpen.value = false
    }

    fun assignVoiceEntryToCustomer(customer: CustomerEntity) {
        val pending = _pendingConfirmation.value ?: return
        _pendingConfirmation.value = pending.copy(customerName = customer.name)
        confirmPendingTransaction(overrideCustomerPhone = customer.phone.ifBlank { null }, explicitCustomerId = customer.id)
        closeSelectCustomerSheet()
    }

    fun createCustomerAndAssignVoiceEntry(name: String, phone: String) {
        val pending = _pendingConfirmation.value ?: return
        _pendingConfirmation.value = pending.copy(customerName = name.trim())
        confirmPendingTransaction(overrideCustomerPhone = phone.trim().ifBlank { null })
        closeSelectCustomerSheet()
    }

    fun confirmPendingTransaction(overrideCustomerPhone: String? = null, explicitCustomerId: Long? = null) {
        val pending = _pendingConfirmation.value ?: return
        viewModelScope.launch {
            repository.recordTransaction(
                customerName = pending.customerName,
                customerPhone = overrideCustomerPhone,
                type = pending.type,
                amount = pending.amount,
                spokenText = pending.rawText,
                explicitCustomerId = explicitCustomerId
            )
            val isBn = uiState.value.isBangla
            val successMsg = if (isBn) {
                "${pending.customerName}-এর হিসাব সংরক্ষিত হয়েছে (৳${pending.amount.toInt()})"
            } else {
                "Saved for ${pending.customerName} (৳${pending.amount.toInt()})"
            }
            showToast(message = successMsg, type = BakiToastType.SUCCESS)
            closeVoiceSheet()
            triggerAutoCloudBackup()
        }
    }

    fun saveManualTransaction(name: String, phone: String, amount: Double, type: TransactionType, note: String, customerId: Long? = null) {
        viewModelScope.launch {
            repository.recordTransaction(
                customerName = name,
                customerPhone = phone.ifBlank { null },
                type = type,
                amount = amount,
                spokenText = null,
                note = note,
                explicitCustomerId = customerId
            )
            _isManualEntryOpen.value = false
            val isBn = uiState.value.isBangla
            showToast(
                message = if (isBn) "নতুন হিসাব যুক্ত হয়েছে (৳${amount.toInt()})" else "Transaction recorded successfully",
                type = BakiToastType.SUCCESS
            )
            triggerAutoCloudBackup()
        }
    }

    fun renameCustomer(customerId: Long, newName: String, newPhone: String) {
        viewModelScope.launch {
            val existing = uiState.value.customers.find { it.id == customerId } ?: return@launch
            val updated = existing.copy(
                name = newName.trim(),
                phone = newPhone.trim(),
                updatedAt = System.currentTimeMillis()
            )
            repository.updateCustomer(updated)
            if (_selectedCustomerForDetail.value?.id == customerId) {
                _selectedCustomerForDetail.value = updated
            }
            val isBn = uiState.value.isBangla
            showToast(
                message = if (isBn) "খরিদ্দারের তথ্য সফলভাবে পরিবর্তন হয়েছে" else "Customer info updated successfully",
                type = BakiToastType.SUCCESS
            )
            triggerAutoCloudBackup()
        }
    }

    fun deleteCustomer(customer: CustomerEntity) {
        viewModelScope.launch {
            repository.archiveCustomer(customer.id)
            if (_selectedCustomerForDetail.value?.id == customer.id) {
                _selectedCustomerForDetail.value = null
            }
            val isBn = uiState.value.isBangla
            showToast(
                message = if (isBn) "${customer.name}-কে আর্কাইভে পাঠানো হয়েছে" else "${customer.name} archived",
                type = BakiToastType.INFO
            )
            triggerAutoCloudBackup()
        }
    }

    fun addCustomer(name: String, phone: String, initialDue: Double, note: String) {
        viewModelScope.launch {
            val customer = Customer(
                name = name.trim(),
                phone = phone.trim(),
                balance = 0.0,
                trustScore = 2,
                note = note
            )
            repository.addCustomer(customer)
            if (initialDue > 0) {
                repository.recordTransaction(
                    customerName = name.trim(),
                    customerPhone = phone.trim().ifBlank { null },
                    type = TransactionType.BAKI_SALE,
                    amount = initialDue,
                    spokenText = "শুরুর বকেয়া",
                    note = if (note.isNotBlank()) note else "প্রারম্ভিক বকেয়া"
                )
            }
            _isAddCustomerOpen.value = false
            val isBn = uiState.value.isBangla
            showToast(
                message = if (isBn) "$name ক্রেতা যোগ করা হয়েছে" else "Added customer $name",
                type = BakiToastType.SUCCESS
            )
            triggerAutoCloudBackup()
        }
    }

    fun settleCustomerBalance(customer: CustomerEntity) {
        viewModelScope.launch {
            repository.settleCustomerDue(customer.id, customer.balance, customer.name)
            val isBn = uiState.value.isBangla
            val msg = if (isBn) {
                "${customer.name}-এর সম্পূর্ণ বকেয়া পরিশোধ সম্পন্ন"
            } else {
                "Marked full payment for ${customer.name}"
            }
            showToast(message = msg, type = BakiToastType.SUCCESS)
            _selectedCustomerForDetail.value = null
            triggerAutoCloudBackup()
        }
    }

    fun openSmsReminder(customer: CustomerEntity) {
        _customerForSms.value = customer
        _smsTone.value = SmsTone.GENTLE
    }

    fun closeSmsReminder() {
        _customerForSms.value = null
    }

    fun setSmsTone(tone: SmsTone) {
        _smsTone.value = tone
    }

    fun sendSmsIntent(context: Context, customer: CustomerEntity, text: String) {
        try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:${customer.phone}")
                putExtra("sms_body", text)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(shareIntent, "SMS বা মেসেঞ্জার পাঠান"))
        }
        closeSmsReminder()
    }

    fun sendWhatsAppReminder(context: Context, customer: CustomerEntity, customMessage: String? = null) {
        val isBn = uiState.value.isBangla
        val settings = uiState.value.settings
        val tone = _smsTone.value
        val message = customMessage ?: com.example.util.DigitalReceiptHelper.generateWhatsAppReminderMessage(
            customer = customer,
            settings = settings,
            tone = tone,
            isBn = isBn
        )
        val success = com.example.util.DigitalReceiptHelper.openWhatsAppDirect(context, customer.phone, message)
        if (success) {
            showToast(
                message = if (isBn) "WhatsApp এ তাগাদা বার্তা পাঠানো হচ্ছে..." else "Opening WhatsApp reminder...",
                type = com.example.ui.components.BakiToastType.SUCCESS
            )
        } else {
            com.example.util.DigitalReceiptHelper.copyToClipboard(context, message, "WhatsApp Reminder")
            showToast(
                message = if (isBn) "বার্তা ক্লিপবোর্ডে কপি করা হয়েছে" else "Message copied to clipboard",
                type = com.example.ui.components.BakiToastType.INFO
            )
        }
        closeSmsReminder()
    }

    fun updateSettings(shopName: String, ownerName: String, bkashNumber: String, nagadNumber: String, autoReadBack: Boolean) {
        viewModelScope.launch {
            val current = repository.getSettingsDirect()
            val updated = current.copy(
                shopName = shopName.trim(),
                ownerName = ownerName.trim(),
                bkashNumber = bkashNumber.trim(),
                nagadNumber = nagadNumber.trim(),
                autoReadBack = autoReadBack
            )
            repository.updateSettings(updated)
            _isSettingsOpen.value = false
            val isBn = uiState.value.isBangla
            showToast(
                message = if (isBn) "দোকান সেটিংস সংরক্ষিত হয়েছে" else "Shop settings updated",
                type = BakiToastType.SUCCESS
            )
        }
    }

    fun toggleLanguage() {
        viewModelScope.launch {
            val current = repository.getSettingsDirect()
            val newLang = if (current.language == "bn") "en" else "bn"
            repository.updateSettings(current.copy(language = newLang))
            showToast(
                message = if (newLang == "bn") "ভাষা পরিবর্তন: বাংলা" else "Language switched: English",
                type = BakiToastType.INFO
            )
        }
    }

    fun selectCustomerForDetail(customer: CustomerEntity?) {
        _selectedCustomerForDetail.value = customer
    }

    fun triggerSync() {
        if (authManager.currentUser.value != null) {
            backupToCloud(silent = false)
        } else {
            val isBn = uiState.value.isBangla
            showToast(
                message = if (isBn) "সকল হিসাব অফলাইনে স্থানীয়ভাবে সংরক্ষিত আছে" else "All records safely saved locally offline",
                type = BakiToastType.INFO
            )
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            repository.clearDatabase()
            _isSettingsOpen.value = false
            _selectedCustomerForDetail.value = null
            val isBn = uiState.value.isBangla
            showToast(
                message = if (isBn) "সম্পূর্ণ ডাটাবেজ মুছে ফেলা হয়েছে" else "All database records cleared",
                type = BakiToastType.WARNING
            )
        }
    }

    fun openManualScreen(fallbackText: String? = null) {
        _voiceFallbackSnippet.value = fallbackText
        _isManualEntryOpen.value = true
        _isVoiceSheetOpen.value = false
    }

    fun closeManualScreen() {
        _isManualEntryOpen.value = false
        _voiceFallbackSnippet.value = null
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun openAddCustomerDialog(open: Boolean) {
        _isAddCustomerOpen.value = open
    }

    fun openManualEntryDialog(open: Boolean) {
        _isManualEntryOpen.value = open
        if (!open) {
            _voiceFallbackSnippet.value = null
        }
    }

    fun openSettingsDialog(open: Boolean) {
        _isSettingsOpen.value = open
    }

    fun clearSnackbar() {
        _messageSnackbar.value = null
    }

    // --- Google Sign-In & Cloud Backup Integration ---

    fun signInWithGoogle(activityContext: Context? = null) {
        viewModelScope.launch {
            _isSyncingCloud.value = true
            val result = authManager.signInWithGoogle(activityContext)
            _isSyncingCloud.value = false
            result.onSuccess { user ->
                val isBn = uiState.value.isBangla
                val name = user.displayName ?: user.email ?: "Google User"
                showToast(
                    message = if (isBn) "$name অ্যাকাউন্ট সফলভাবে সংযুক্ত হয়েছে" else "Signed in as $name",
                    type = BakiToastType.SUCCESS
                )
                // Initialize user profile in Firestore
                syncRepository.syncUserProfile(user, uiState.value.settings, _autoBackupEnabled.value)
                if (_autoBackupEnabled.value) {
                    backupToCloud(silent = true)
                }
            }.onFailure { e ->
                android.util.Log.e("BakiViewModel", "Google sign in failure", e)
                if (e !is androidx.credentials.exceptions.GetCredentialCancellationException &&
                    !e.message.orEmpty().contains("cancelled", ignoreCase = true)
                ) {
                    val isBn = uiState.value.isBangla
                    val userMsg = when {
                        e.message?.contains("No matching credentials", ignoreCase = true) == true ->
                            if (isBn) "ফোনে কোনো সক্রিয় Google অ্যাকাউন্ট পাওয়া যায়নি" else "No Google account found on device"
                        e.message?.contains("developer error", ignoreCase = true) == true || e.message?.contains("10:", ignoreCase = true) == true ->
                            if (isBn) "Google Play Services কনফিগারেশন ত্রুটি (Developer Error)" else "Google Play Services config issue"
                        else ->
                            if (isBn) "Google সাইন ইন সম্পন্ন হয়নি: ${e.localizedMessage ?: "ত্রুটি"}" else "Google Sign In cancelled or failed: ${e.localizedMessage ?: "Error"}"
                    }
                    showToast(
                        message = userMsg,
                        type = BakiToastType.WARNING
                    )
                }
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authManager.signOut()
            val isBn = uiState.value.isBangla
            showToast(
                message = if (isBn) "Google অ্যাকাউন্ট থেকে সাইন আউট করা হয়েছে" else "Signed out of Google account",
                type = BakiToastType.INFO
            )
        }
    }

    fun setAutoBackup(enabled: Boolean) {
        _autoBackupEnabled.value = enabled
        val user = authManager.currentUser.value
        if (user != null) {
            viewModelScope.launch {
                syncRepository.syncUserProfile(user, uiState.value.settings, enabled)
            }
        }
        val isBn = uiState.value.isBangla
        showToast(
            message = if (enabled) {
                if (isBn) "স্বয়ংক্রিয় ক্লাউড ব্যাকআপ চালু করা হয়েছে" else "Auto Cloud Backup enabled"
            } else {
                if (isBn) "স্বয়ংক্রিয় ক্লাউড ব্যাকআপ বন্ধ করা হয়েছে" else "Auto Cloud Backup disabled"
            },
            type = BakiToastType.INFO
        )
    }

    fun backupToCloud(silent: Boolean = false) {
        val user = authManager.currentUser.value
        if (user == null) {
            if (!silent) {
                val isBn = uiState.value.isBangla
                showToast(
                    message = if (isBn) "ক্লাউড ব্যাকআপের জন্য প্রথমে Google দিয়ে সাইন ইন করুন" else "Please sign in with Google to backup",
                    type = BakiToastType.WARNING
                )
            }
            return
        }

        viewModelScope.launch {
            _isSyncingCloud.value = true
            val isBn = uiState.value.isBangla
            if (!silent) {
                showToast(
                    message = if (isBn) "ক্লাউডে ডেটা ব্যাকআপ নেওয়া হচ্ছে..." else "Backing up to Google Cloud...",
                    type = BakiToastType.INFO
                )
            }
            try {
                val result = withTimeoutOrNull(45_000L) {
                    syncRepository.backupAll(
                        userId = user.uid,
                        user = user,
                        customers = uiState.value.customers,
                        transactions = uiState.value.allTransactionsUnfiltered,
                        settings = uiState.value.settings
                    )
                }

                if (result == null) {
                    // Timed out
                    if (!silent) {
                        showToast(
                            message = if (isBn) "ব্যাকআপ সময়সীমা শেষ (টাইমআউট)। ইন্টারনেট চেক করুন।" else "Backup timed out. Please check internet.",
                            type = BakiToastType.WARNING
                        )
                    }
                } else {
                    result.onSuccess { stats ->
                        _lastCloudBackupAt.value = stats.timestamp
                        val countText = if (isBn) BanglaVoiceParser.toBanglaDigits("${stats.customersBackedUp}") else "${stats.customersBackedUp}"
                        showToast(
                            message = if (isBn) "আলহামদুলিল্লাহ! $countText জন ক্রেতার হিসাব ক্লাউডে সংরক্ষিত" else "Cloud backup successful (${stats.customersBackedUp} accounts secured)",
                            type = BakiToastType.SUCCESS
                        )
                    }.onFailure { error ->
                        showToast(
                            message = if (isBn) "ব্যাকআপ ব্যর্থ হয়েছে: ${error.localizedMessage ?: "নেটওয়ার্ক ত্রুটি"}" else "Cloud backup failed: ${error.localizedMessage}",
                            type = BakiToastType.ERROR
                        )
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("BakiViewModel", "Backup exception", e)
                if (!silent) {
                    showToast(
                        message = if (isBn) "ব্যাকআপ সমস্যা: ${e.localizedMessage ?: "অজ্ঞাত ত্রুটি"}" else "Backup error: ${e.localizedMessage}",
                        type = BakiToastType.ERROR
                    )
                }
            } finally {
                _isSyncingCloud.value = false
            }
        }
    }

    fun restoreFromCloud() {
        val user = authManager.currentUser.value
        if (user == null) {
            val isBn = uiState.value.isBangla
            showToast(
                message = if (isBn) "রিস্টোর করার জন্য প্রথমে Google দিয়ে সাইন ইন করুন" else "Please sign in with Google to restore",
                type = BakiToastType.WARNING
            )
            return
        }

        viewModelScope.launch {
            _isSyncingCloud.value = true
            val isBn = uiState.value.isBangla
            showToast(
                message = if (isBn) "ক্লাউড থেকে ডেটা আনা হচ্ছে..." else "Restoring ledger from Google Cloud...",
                type = BakiToastType.INFO
            )
            try {
                val result = withTimeoutOrNull(45_000L) {
                    syncRepository.restoreAll(user.uid)
                }

                if (result == null) {
                    showToast(
                        message = if (isBn) "রিস্টোর সময়সীমা শেষ (টাইমআউট)। সংযোগ পরীক্ষা করুন।" else "Restore timed out. Check connection.",
                        type = BakiToastType.WARNING
                    )
                } else {
                    result.onSuccess { stats ->
                        repository.restoreCloudData(
                            customers = stats.customersRestored,
                            transactions = stats.transactionsRestored,
                            settings = stats.settingsRestored
                        )
                        val cCount = if (isBn) BanglaVoiceParser.toBanglaDigits("${stats.customersRestored.size}") else "${stats.customersRestored.size}"
                        val tCount = if (isBn) BanglaVoiceParser.toBanglaDigits("${stats.transactionsRestored.size}") else "${stats.transactionsRestored.size}"
                        showToast(
                            message = if (isBn) "সফলভাবে $cCount জন ক্রেতা ও $tCount টি হিসাব রিস্টোর হয়েছে" else "Restored $cCount customers and $tCount entries from Cloud",
                            type = BakiToastType.SUCCESS
                        )
                    }.onFailure { error ->
                        showToast(
                            message = if (isBn) "ক্লাউড রিস্টোর ব্যর্থ হয়েছে: ${error.localizedMessage ?: "ডেটা পাওয়া যায়নি"}" else "Cloud restore failed: ${error.localizedMessage}",
                            type = BakiToastType.ERROR
                        )
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("BakiViewModel", "Restore exception", e)
                showToast(
                    message = if (isBn) "রিস্টোর সমস্যা: ${e.localizedMessage ?: "ত্রুটি"}" else "Restore error: ${e.localizedMessage}",
                    type = BakiToastType.ERROR
                )
            } finally {
                _isSyncingCloud.value = false
            }
        }
    }

    fun openCloudRestorePreview() {
        val user = authManager.currentUser.value
        if (user == null) {
            val isBn = uiState.value.isBangla
            showToast(
                message = if (isBn) "রিস্টোর করার জন্য প্রথমে Google দিয়ে সাইন ইন করুন" else "Please sign in with Google to restore",
                type = BakiToastType.WARNING
            )
            return
        }

        viewModelScope.launch {
            _isFetchingRestorePreview.value = true
            _isRestorePreviewOpen.value = true
            val previewResult = syncRepository.getCloudPreview(user.uid)
            _isFetchingRestorePreview.value = false
            previewResult.onSuccess { stats ->
                _restorePreviewData.value = com.example.ui.components.CloudRestorePreviewData(
                    cloudCustomerCount = stats.cloudCustomerCount,
                    cloudTransactionCount = stats.cloudTransactionCount,
                    localCustomerCount = uiState.value.customers.size,
                    localTransactionCount = uiState.value.allTransactionsUnfiltered.size,
                    lastBackupTimestamp = stats.lastBackupAt
                )
            }.onFailure { error ->
                _isRestorePreviewOpen.value = false
                val isBn = uiState.value.isBangla
                showToast(
                    message = if (isBn) "ক্লাউড প্রিভিউ লোড করা যায়নি: ${error.localizedMessage ?: "নেটওয়ার্ক ত্রুটি"}" else "Failed to load cloud preview: ${error.localizedMessage}",
                    type = BakiToastType.ERROR
                )
            }
        }
    }

    fun dismissRestorePreview() {
        _isRestorePreviewOpen.value = false
        _restorePreviewData.value = null
    }

    fun confirmCloudRestore(mergeWithLocal: Boolean) {
        dismissRestorePreview()
        restoreFromCloud()
    }

    // Archive Zone Operations
    fun openArchiveZone(isOpen: Boolean) {
        _isArchiveZoneOpen.value = isOpen
    }

    fun restoreArchivedCustomer(customerId: Long) {
        viewModelScope.launch {
            repository.restoreCustomer(customerId)
            val isBn = uiState.value.isBangla
            showToast(
                message = if (isBn) "কাস্টমার সফলভাবে পুনরুদ্ধার করা হয়েছে" else "Customer restored",
                type = BakiToastType.SUCCESS
            )
            triggerAutoCloudBackup()
        }
    }

    fun restoreArchivedTransaction(transactionId: Long) {
        viewModelScope.launch {
            repository.restoreTransaction(transactionId)
            val isBn = uiState.value.isBangla
            showToast(
                message = if (isBn) "লেনদেন সফলভাবে পুনরুদ্ধার করা হয়েছে" else "Transaction restored",
                type = BakiToastType.SUCCESS
            )
            triggerAutoCloudBackup()
        }
    }

    fun purgeArchivedCustomer(customerId: Long) {
        viewModelScope.launch {
            repository.purgeCustomer(customerId)
            val isBn = uiState.value.isBangla
            showToast(
                message = if (isBn) "কাস্টমার স্থায়ীভাবে মুছে ফেলা হয়েছে" else "Customer permanently purged",
                type = BakiToastType.INFO
            )
            triggerAutoCloudBackup()
        }
    }

    fun purgeArchivedTransaction(transactionId: Long) {
        viewModelScope.launch {
            repository.purgeTransaction(transactionId)
            val isBn = uiState.value.isBangla
            showToast(
                message = if (isBn) "লেনদেন স্থায়ীভাবে মুছে ফেলা হয়েছে" else "Transaction permanently purged",
                type = BakiToastType.INFO
            )
            triggerAutoCloudBackup()
        }
    }

    fun purgeAllArchive() {
        viewModelScope.launch {
            repository.purgeAllArchived()
            val isBn = uiState.value.isBangla
            showToast(
                message = if (isBn) "আর্কাইভের সকল রেকর্ড স্থায়ীভাবে মুছে ফেলা হয়েছে" else "Archive emptied",
                type = BakiToastType.INFO
            )
            triggerAutoCloudBackup()
        }
    }

    // In-App Update Methods
    fun checkForUpdate(silent: Boolean = false) {
        viewModelScope.launch {
            _isCheckingUpdate.value = true
            val result = inAppUpdateManager.checkForUpdates()
            _isCheckingUpdate.value = false
            result.onSuccess { info ->
                _updateInfo.value = info
                if (!info.isUpdateAvailable && !silent) {
                    val isBn = uiState.value.isBangla
                    showToast(
                        message = if (isBn) "অ্যাপটি ইতিমধ্যে সর্বশেষ সংস্করণে রয়েছে (${info.currentVersion})"
                        else "App is up to date (${info.currentVersion})",
                        type = BakiToastType.INFO
                    )
                }
            }.onFailure { e ->
                if (!silent) {
                    val isBn = uiState.value.isBangla
                    showToast(
                        message = if (isBn) "আপডেট চেক করা যায়নি: ${e.message}" else "Update check failed: ${e.message}",
                        type = BakiToastType.WARNING
                    )
                }
            }
        }
    }

    fun startUpdateDownload(apkUrl: String) {
        viewModelScope.launch {
            _updateDownloadProgress.value = 0
            val result = inAppUpdateManager.downloadAndInstallApk(apkUrl) { progress ->
                _updateDownloadProgress.value = progress
            }
            _updateDownloadProgress.value = null
            result.onFailure { e ->
                val isBn = uiState.value.isBangla
                showToast(
                    message = if (isBn) "ডাউনলোড ব্যর্থ হয়েছে: ${e.message}" else "Download failed: ${e.message}",
                    type = BakiToastType.ERROR
                )
            }
        }
    }

    fun dismissUpdateDialog() {
        _updateInfo.value = null
    }

    private fun triggerAutoCloudBackup() {
        if (authManager.currentUser.value != null && _autoBackupEnabled.value) {
            backupToCloud(silent = true)
        }
    }

    override fun onCleared() {
        super.onCleared()
        voiceManager.destroy()
    }
}
