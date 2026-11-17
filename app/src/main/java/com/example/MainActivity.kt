package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import com.example.data.model.SyncStatus
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.res.painterResource
import com.example.ui.BakiStrings
import com.example.ui.BakiViewModel
import com.example.ui.components.AddCustomerDialog
import com.example.ui.components.BakiFloatingToastHost
import com.example.ui.components.CustomDateRangeDialog
import com.example.ui.components.CustomerDetailDialog
import com.example.ui.components.CustomerSelectOrNewSheet
import com.example.ui.components.DigitalReceiptDialog
import com.example.ui.components.EditTransactionDialog
import com.example.ui.components.EndOfDaySummaryDialog
import com.example.ui.components.ManualEntryDialog
import com.example.ui.components.OpeningBrandLoader
import com.example.ui.components.SettingsSheet
import com.example.ui.components.SmsReminderDialog
import com.example.ui.components.SpeedDialFab
import com.example.ui.components.VoiceRecordSheet
import com.example.ui.screens.CustomersScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ManualEntryScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.MyApplicationTheme
import com.example.util.DateRangeFilterType

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                BakiBondhuApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BakiBondhuApp(viewModel: BakiViewModel = viewModel()) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val isRestorePreviewOpen by viewModel.isRestorePreviewOpen.collectAsState()
    val restorePreviewData by viewModel.restorePreviewData.collectAsState()
    val isFetchingRestorePreview by viewModel.isFetchingRestorePreview.collectAsState()
    val isBn = uiState.isBangla
    val coroutineScope = rememberCoroutineScope()
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 3 })
    val snackbarHostState = remember { SnackbarHostState() }

    // Google Speech Recognition Intent Launcher (100% reliable system dialog on real devices)
    val systemVoiceLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val spokenMatches = result.data?.getStringArrayListExtra(android.speech.RecognizerIntent.EXTRA_RESULTS)
            val recognizedText = spokenMatches?.firstOrNull()
            if (!recognizedText.isNullOrBlank()) {
                viewModel.processRecognizedSpeech(recognizedText)
            }
        }
    }

    // Audio permission request launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        viewModel.openVoiceSheet()
    }

    fun requestVoiceEntry() {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            viewModel.openVoiceSheet()
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    // Snackbar notifications
    LaunchedEffect(uiState.messageSnackbar) {
        uiState.messageSnackbar?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    // Handle back button
    BackHandler(enabled = pagerState.currentPage != 0 || uiState.selectedCustomerForDetail != null || uiState.isManualEntryOpen || uiState.transactionForReceipt != null) {
        if (uiState.transactionForReceipt != null) {
            viewModel.openReceiptDialog(null)
        } else if (uiState.isManualEntryOpen) {
            viewModel.openManualEntryDialog(false)
        } else if (uiState.selectedCustomerForDetail != null) {
            viewModel.selectCustomerForDetail(null)
        } else {
            coroutineScope.launch { pagerState.animateScrollToPage(0) }
        }
    }

    var showOpeningLoader by remember { mutableStateOf(true) }
    var dismissedOnboarding by remember { mutableStateOf(false) }
    val showOnboarding = !dismissedOnboarding &&
            !showOpeningLoader &&
            uiState.settings.ownerName.isBlank() &&
            (uiState.settings.shopName == "আমার দোকান" || uiState.settings.shopName.isBlank()) &&
            uiState.customers.isEmpty()

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                windowInsets = WindowInsets.navigationBars
            ) {
                // Tab 0: Home
                NavigationBarItem(
                    selected = pagerState.currentPage == 0,
                    onClick = { coroutineScope.launch { pagerState.animateScrollToPage(0) } },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text(BakiStrings.homeTab(isBn), fontSize = 12.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = EmeraldPrimary.copy(alpha = 0.2f)
                    ),
                    modifier = Modifier.testTag("tab_home")
                )

                // Tab 1: Customers / Ledger
                NavigationBarItem(
                    selected = pagerState.currentPage == 1,
                    onClick = { coroutineScope.launch { pagerState.animateScrollToPage(1) } },
                    icon = {
                        val dueCount = uiState.customers.count { it.balance > 0 }
                        if (dueCount > 0) {
                            BadgedBox(badge = { Badge { Text(if (isBn) com.example.voice.BanglaVoiceParser.toBanglaDigits("$dueCount") else "$dueCount") } }) {
                                Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = "Customers")
                            }
                        } else {
                            Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = "Customers")
                        }
                    },
                    label = { Text(BakiStrings.customersTab(isBn), fontSize = 12.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = EmeraldPrimary.copy(alpha = 0.2f)
                    ),
                    modifier = Modifier.testTag("tab_customers")
                )

                // Tab 2: Reports & Dues
                NavigationBarItem(
                    selected = pagerState.currentPage == 2,
                    onClick = { coroutineScope.launch { pagerState.animateScrollToPage(2) } },
                    icon = { Icon(Icons.Default.Assessment, contentDescription = "Reports") },
                    label = { Text(BakiStrings.reportsTab(isBn), fontSize = 12.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = EmeraldPrimary.copy(alpha = 0.2f)
                    ),
                    modifier = Modifier.testTag("tab_reports")
                )
            }
        },
        floatingActionButtonPosition = FabPosition.End,
        floatingActionButton = {
            if (pagerState.currentPage != 1) {
                SpeedDialFab(
                    isBn = isBn,
                    onVoiceClick = { requestVoiceEntry() },
                    onManualDebtClick = { viewModel.openManualEntryDialog(true) },
                    onAddCustomerClick = { viewModel.openAddCustomerDialog(true) },
                    modifier = Modifier.padding(bottom = 12.dp, end = 4.dp)
                )
            }
        },
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.padding(bottom = if (pagerState.currentPage != 1) 96.dp else 16.dp)
            )
        },
        contentWindowInsets = WindowInsets.statusBars
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            HorizontalPager(
                state = pagerState,
                userScrollEnabled = true,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                    when (page) {
                        0 -> HomeScreen(
                            uiState = uiState,
                            onVoiceClick = { requestVoiceEntry() },
                            onAddCustomerClick = { viewModel.openAddCustomerDialog(true) },
                            onManualEntryClick = { viewModel.openManualEntryDialog(true) },
                            onToggleLanguage = { viewModel.toggleLanguage() },
                            onOpenSettings = { viewModel.openSettingsDialog(true) },
                            onSignInWithGoogle = { viewModel.signInWithGoogle(context) },
                            onSyncClick = { viewModel.triggerSync() },
                            onSearchQueryChange = { query -> viewModel.setSearchQuery(query) },
                            onCustomerClick = { customer -> viewModel.selectCustomerForDetail(customer) },
                            onTransactionClick = { tx ->
                                val matchedCust = uiState.customers.find { it.id == tx.customerId }
                                if (matchedCust != null) {
                                    viewModel.selectCustomerForDetail(matchedCust)
                                }
                            },
                            onToggleSelect = { id -> viewModel.toggleSelectTransaction(id) },
                            onSelectAll = {
                                val allIds = uiState.transactions.map { it.id }
                                viewModel.selectAllTransactions(allIds)
                            },
                            onDeleteSelected = { viewModel.deleteSelectedTransactions() },
                            onClearSelection = { viewModel.clearTransactionSelection() },
                            onToggleSelectionMode = { viewModel.toggleSelectionMode() },
                            onFilterChange = { filter -> viewModel.setTransactionFilter(filter) },
                            onDateRangeChange = { range -> viewModel.setDateRangeFilter(range) },
                            onOpenCustomDateRange = { viewModel.openCustomDateRangeDialog(true) },
                            onOpenEndOfDaySummary = { viewModel.openEndOfDaySummary(true) },
                            onEditTransaction = { tx -> viewModel.openEditTransactionDialog(tx) },
                            onDeleteTransaction = { tx -> viewModel.deleteSingleTransaction(tx) },
                            onShareTransaction = { tx -> viewModel.openReceiptDialog(tx) }
                        )

                        1 -> CustomersScreen(
                            uiState = uiState,
                            onCustomerClick = { customer -> viewModel.selectCustomerForDetail(customer) },
                            onSendSmsClick = { customer -> viewModel.openSmsReminder(customer) },
                            onSendWhatsAppClick = { customer -> viewModel.sendWhatsAppReminder(context, customer) },
                            onAddCustomerClick = { viewModel.openAddCustomerDialog(true) }
                        )

                        2 -> ReportsScreen(
                            uiState = uiState,
                            onCustomerClick = { customer -> viewModel.selectCustomerForDetail(customer) },
                            onSendSmsClick = { customer -> viewModel.openSmsReminder(customer) },
                            onSendWhatsAppClick = { customer -> viewModel.sendWhatsAppReminder(context, customer) },
                            onOpenDailyClosing = { viewModel.openEndOfDaySummary(true) }
                        )
                    }
                }

            // MODALS & OVERLAYS

            // 0.5 First-Run Merchant Onboarding
            if (showOnboarding) {
                com.example.ui.components.MerchantOnboardingDialog(
                    initialShopName = uiState.settings.shopName,
                    initialOwnerName = uiState.settings.ownerName,
                    initialBkash = uiState.settings.bkashNumber,
                    initialNagad = uiState.settings.nagadNumber,
                    isBn = isBn,
                    onSaveAndStart = { sName, oName, bkash, nagad, isBangla ->
                        viewModel.updateSettings(
                            shopName = sName,
                            ownerName = oName,
                            bkashNumber = bkash,
                            nagadNumber = nagad,
                            autoReadBack = uiState.settings.autoReadBack
                        )
                        if (isBangla != isBn) {
                            viewModel.toggleLanguage()
                        }
                        dismissedOnboarding = true
                    },
                    onDismiss = {
                        dismissedOnboarding = true
                    }
                )
            }

            // 1. Voice Recording & Read-Back Sheet
            if (uiState.isVoiceSheetOpen) {
                VoiceRecordSheet(
                    viewModel = viewModel,
                    onLaunchSystemVoice = {
                        try {
                            systemVoiceLauncher.launch(viewModel.voiceManager.createSystemSpeechIntent())
                        } catch (_: Exception) {}
                    },
                    onDismiss = { viewModel.closeVoiceSheet() }
                )
            }

            // 1.5 Quick Voice Entry Customer Selection & Creation Dialog
            if (uiState.isSelectCustomerSheetOpen && uiState.pendingConfirmation != null) {
                CustomerSelectOrNewSheet(
                    parsedResult = uiState.pendingConfirmation!!,
                    customers = uiState.customers,
                    isBn = isBn,
                    onSelectCustomer = { customer ->
                        viewModel.assignVoiceEntryToCustomer(customer)
                    },
                    onCreateNewCustomerAndAssign = { name, phone ->
                        viewModel.createCustomerAndAssignVoiceEntry(name, phone)
                    },
                    onDismiss = {
                        viewModel.closeSelectCustomerSheet()
                    }
                )
            }

            // 2. Customer Detail & Ledger Dialog
            uiState.selectedCustomerForDetail?.let { customer ->
                CustomerDetailDialog(
                    customer = customer,
                    transactions = uiState.transactions,
                    isBn = isBn,
                    onSendSms = {
                        viewModel.openSmsReminder(customer)
                    },
                    onSendWhatsApp = {
                        viewModel.sendWhatsAppReminder(context, customer)
                    },
                    onSettleBalance = {
                        viewModel.settleCustomerBalance(customer)
                    },
                    onAddTransaction = {
                        viewModel.openManualEntryDialog(true)
                    },
                    onShareStatement = {
                        viewModel.shareStatement(context, customer, uiState.transactions)
                    },
                    onEditTransaction = { tx ->
                        viewModel.openEditTransactionDialog(tx)
                    },
                    onDeleteTransaction = { tx ->
                        viewModel.deleteSingleTransaction(tx)
                    },
                    onShareTransaction = { tx ->
                        viewModel.openReceiptDialog(tx)
                    },
                    onDismiss = {
                        viewModel.selectCustomerForDetail(null)
                    }
                )
            }

            // 3. SMS Reminder Dialog (Tier A Payment Flow)
            uiState.customerForSms?.let { customer ->
                SmsReminderDialog(
                    customer = customer,
                    settings = uiState.settings,
                    tone = uiState.smsTone,
                    isBn = isBn,
                    onToneChanged = { viewModel.setSmsTone(it) },
                    onSendWhatsApp = { text ->
                        viewModel.sendWhatsAppReminder(context, customer, text)
                    },
                    onSend = { smsText ->
                        viewModel.sendSmsIntent(context, customer, smsText)
                    },
                    onDismiss = { viewModel.closeSmsReminder() }
                )
            }

            // 4. Add Customer Dialog
            if (uiState.isAddCustomerOpen) {
                AddCustomerDialog(
                    isBn = isBn,
                    onAdd = { name, phone, due, note ->
                        viewModel.addCustomer(name, phone, due, note)
                    },
                    onDismiss = { viewModel.openAddCustomerDialog(false) }
                )
            }

            // 5. Manual Entry Screen (Tactile Fallback for Voice & Direct Entry)
            if (uiState.isManualEntryOpen) {
                ManualEntryScreen(
                    uiState = uiState,
                    fallbackVoiceSnippet = uiState.voiceFallbackSnippet,
                    onSaveTransaction = { name, phone, amount, type, note ->
                        viewModel.saveManualTransaction(name, phone, amount, type, note)
                    },
                    onAddCustomer = { name, phone, due, note ->
                        viewModel.addCustomer(name, phone, due, note)
                    },
                    onNavigateBack = { viewModel.openManualEntryDialog(false) }
                )
            }

            // 6. Settings Sheet (Shop Details & Google Cloud Backup)
            if (uiState.isSettingsOpen) {
                SettingsSheet(
                    settings = uiState.settings,
                    isBn = isBn,
                    currentUser = uiState.currentUser,
                    isSyncingCloud = uiState.isSyncingCloud,
                    lastCloudBackupAt = uiState.lastCloudBackupAt,
                    autoBackupEnabled = uiState.autoBackupEnabled,
                    isRestorePreviewOpen = isRestorePreviewOpen,
                    restorePreviewData = restorePreviewData,
                    isFetchingRestorePreview = isFetchingRestorePreview,
                    isCheckingUpdate = uiState.isCheckingUpdate,
                    onSignInWithGoogle = { viewModel.signInWithGoogle(context) },
                    onSignOut = { viewModel.signOut() },
                    onSetAutoBackup = { viewModel.setAutoBackup(it) },
                    onBackupToCloud = { viewModel.backupToCloud() },
                    onRestoreFromCloud = { viewModel.openCloudRestorePreview() },
                    onDismissRestorePreview = { viewModel.dismissRestorePreview() },
                    onConfirmCloudRestore = { mergeWithLocal -> viewModel.confirmCloudRestore(mergeWithLocal) },
                    onOpenArchiveZone = { viewModel.openArchiveZone(true) },
                    onCheckForUpdate = { viewModel.checkForUpdate(silent = false) },
                    onSave = { shopName, ownerName, bkash, nagad, autoReadBack ->
                        viewModel.updateSettings(shopName, ownerName, bkash, nagad, autoReadBack)
                    },
                    onClearDatabase = {
                        viewModel.clearAllData()
                    },
                    onDismiss = { viewModel.openSettingsDialog(false) }
                )
            }

            // 7. Edit Transaction Dialog
            uiState.transactionToEdit?.let { txToEdit ->
                EditTransactionDialog(
                    transaction = txToEdit,
                    isBn = isBn,
                    onSave = { oldTx, newTx ->
                        viewModel.saveEditedTransaction(oldTx, newTx)
                    },
                    onDismiss = { viewModel.openEditTransactionDialog(null) }
                )
            }

            // Archive Zone Dialog
            if (uiState.isArchiveZoneOpen) {
                com.example.ui.components.ArchiveZoneDialog(
                    archivedCustomers = uiState.archivedCustomers,
                    archivedTransactions = uiState.archivedTransactions,
                    isBn = isBn,
                    onRestoreCustomer = { viewModel.restoreArchivedCustomer(it) },
                    onRestoreTransaction = { viewModel.restoreArchivedTransaction(it) },
                    onPurgeCustomer = { viewModel.purgeArchivedCustomer(it) },
                    onPurgeTransaction = { viewModel.purgeArchivedTransaction(it) },
                    onPurgeAll = { viewModel.purgeAllArchive() },
                    onDismiss = { viewModel.openArchiveZone(false) }
                )
            }

            // In-App Update Dialog
            uiState.updateInfo?.let { update ->
                if (update.isUpdateAvailable) {
                    androidx.compose.material3.AlertDialog(
                        onDismissRequest = { viewModel.dismissUpdateDialog() },
                        title = {
                            Text(
                                if (isBn) "নতুন আপডেট উপলব্ধ: v${update.latestVersion}"
                                else "New Update Available: v${update.latestVersion}",
                                fontWeight = FontWeight.Bold
                            )
                        },
                        text = {
                            Column {
                                Text(
                                    if (isBn) "বর্তমান সংস্করণ: v${update.currentVersion}\n${update.releaseTitle ?: ""}\n\n${update.releaseNotes?.take(250) ?: ""}"
                                    else "Current version: v${update.currentVersion}\n${update.releaseTitle ?: ""}\n\n${update.releaseNotes?.take(250) ?: ""}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                if (uiState.updateDownloadProgress != null) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    androidx.compose.material3.LinearProgressIndicator(
                                        progress = { (uiState.updateDownloadProgress ?: 0) / 100f },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "${uiState.updateDownloadProgress}%",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    update.downloadUrl?.let { url ->
                                        viewModel.startUpdateDownload(url)
                                    }
                                },
                                enabled = uiState.updateDownloadProgress == null,
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                            ) {
                                Text(if (isBn) "এখনই ডাউনলোড ও আপডেট" else "Download & Install", color = Color.White)
                            }
                        },
                        dismissButton = {
                            OutlinedButton(onClick = { viewModel.dismissUpdateDialog() }) {
                                Text(if (isBn) "পরে" else "Later")
                            }
                        }
                    )
                }
            }

            // 8. Animated Floating Toast Notification Host (Floats safely above bottom bar and FAB)
            val toastBottomPadding = if (pagerState.currentPage != 1) 112.dp else 24.dp
            BakiFloatingToastHost(
                toast = uiState.toastState,
                onDismiss = { viewModel.dismissToast() },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = toastBottomPadding)
                    .zIndex(100f)
            )

            // 9. Individual Transaction Digital Receipt Dialog
            uiState.transactionForReceipt?.let { txForReceipt ->
                val customer = uiState.customers.find { it.id == txForReceipt.customerId }
                DigitalReceiptDialog(
                    transaction = txForReceipt,
                    customer = customer,
                    settings = uiState.settings,
                    isBn = isBn,
                    onShare = { channel ->
                        viewModel.shareTransaction(context, txForReceipt, channel)
                    },
                    onCopy = {
                        viewModel.copyReceiptText(context, txForReceipt)
                    },
                    onDismiss = { viewModel.openReceiptDialog(null) }
                )
            }

            // 9.5 Daily Closing & Cash Till Reconciliation Dialog
            if (uiState.isEndOfDaySummaryOpen) {
                EndOfDaySummaryDialog(
                    summary = uiState.endOfDaySummary,
                    settings = uiState.settings,
                    isBn = isBn,
                    onReconcile = { viewModel.reconcileCashDrawer(context) },
                    onShareReport = { viewModel.shareEndOfDayStatement(context) },
                    onDismiss = { viewModel.openEndOfDaySummary(false) }
                )
            }

            // 10. Opening Brand Splash Entrance Loader
            OpeningBrandLoader(
                visible = showOpeningLoader,
                isBn = isBn,
                onComplete = { showOpeningLoader = false }
            )
        }
    }
}

/**
 * Material 3 Expressive Pill Tab item with active pill indicator and responsive touch feedback.
 */
@Composable
fun ExpressivePillTab(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    badge: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = if (selected) MaterialTheme.colorScheme.surface else Color.Transparent,
        tonalElevation = if (selected) 4.dp else 0.dp,
        shadowElevation = if (selected) 2.dp else 0.dp,
        modifier = modifier.height(40.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (badge != null) {
                BadgedBox(badge = { Badge { Text(badge, fontSize = 10.sp) } }) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        modifier = Modifier.size(16.dp),
                        tint = if (selected) EmeraldPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    modifier = Modifier.size(16.dp),
                    tint = if (selected) EmeraldPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) EmeraldPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}
