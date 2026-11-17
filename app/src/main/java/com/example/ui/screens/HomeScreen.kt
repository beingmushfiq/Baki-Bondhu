package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.draw.rotate
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import com.example.data.model.CustomerEntity
import com.google.firebase.auth.FirebaseUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.ui.BakiStrings
import com.example.ui.BakiUiState
import com.example.ui.components.DevCenterPointSubtleFooter
import com.example.ui.theme.BakiRed
import com.example.ui.theme.CashBlue
import com.example.ui.theme.CoralCredit
import com.example.ui.theme.CyanNet
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldTertiary
import com.example.ui.theme.JomaGreen
import com.example.ui.theme.MintCash
import com.example.ui.theme.RoyalJadeAlpine
import com.example.ui.theme.RoyalJadeForest
import com.example.ui.theme.RoyalJadeGlassBorder
import com.example.ui.theme.RoyalJadeObsidian
import com.example.ui.theme.SolarChampagne
import com.example.ui.theme.SolarChampagneGlow
import com.example.util.DateRangeFilterType
import com.example.util.OverdueHelper
import com.example.util.OverdueLevel
import com.example.voice.BanglaVoiceParser
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    uiState: BakiUiState,
    onVoiceClick: () -> Unit,
    onAddCustomerClick: () -> Unit,
    onManualEntryClick: () -> Unit,
    onTransactionClick: (TransactionEntity) -> Unit,
    onToggleLanguage: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onSignInWithGoogle: () -> Unit = {},
    onSyncClick: () -> Unit = {},
    onSearchQueryChange: (String) -> Unit = {},
    onCustomerClick: (CustomerEntity) -> Unit = {},
    onToggleSelect: (Long) -> Unit = {},
    onSelectAll: () -> Unit = {},
    onDeleteSelected: () -> Unit = {},
    onClearSelection: () -> Unit = {},
    onToggleSelectionMode: () -> Unit = {},
    onFilterChange: (TransactionType?) -> Unit = {},
    onDateRangeChange: (DateRangeFilterType) -> Unit = {},
    onOpenCustomDateRange: () -> Unit = {},
    onOpenEndOfDaySummary: () -> Unit = {},
    onEditTransaction: (TransactionEntity) -> Unit = {},
    onDeleteTransaction: (TransactionEntity) -> Unit = {},
    onShareTransaction: (TransactionEntity) -> Unit = {}
) {
    val isBn = uiState.isBangla
    val dateFormatter = remember(isBn) { SimpleDateFormat("dd MMM, hh:mm a", if (isBn) Locale.forLanguageTag("bn-BD") else Locale.ENGLISH) }

    val query = uiState.searchQuery.trim()
    val matchingCustomers = remember(uiState.customers, query) {
        if (query.isBlank()) {
            emptyList()
        } else {
            uiState.customers.filter {
                it.name.contains(query, ignoreCase = true) || it.phone.contains(query)
            }
        }
    }

    val filteredTransactions = remember(uiState.transactions, uiState.transactionTypeFilter, query) {
        var list = uiState.transactions
        val filter = uiState.transactionTypeFilter
        if (filter != null) {
            list = list.filter { it.type == filter }
        }
        if (query.isNotBlank()) {
            list = list.filter {
                it.customerName.contains(query, ignoreCase = true) ||
                it.note.contains(query, ignoreCase = true) ||
                (it.originalSpokenText?.contains(query, ignoreCase = true) == true)
            }
        }
        list
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 0. INTEGRATED ADAPTIVE DASHBOARD HEADER (Replaces top navbar, zero jitter on language change)
        item {
            IntegratedDashboardHeader(
                uiState = uiState,
                isBn = isBn,
                onToggleLanguage = onToggleLanguage,
                onOpenSettings = onOpenSettings,
                onSignInWithGoogle = onSignInWithGoogle,
                onSyncClick = onSyncClick
            )
        }

        // 1. TOTAL OUTSTANDING DEBT SUMMARY CARD (SHOP FINANCES AT A GLANCE)
        item {
            ShopDebtSummaryCard(
                uiState = uiState,
                isBn = isBn,
                onOpenEndOfDaySummary = onOpenEndOfDaySummary
            )
        }

        // 2. PERSISTENT SEARCH BAR (INSTANT CUSTOMER & TRANSACTION FINDER)
        item {
            DashboardSearchBar(
                query = uiState.searchQuery,
                onQueryChange = onSearchQueryChange,
                isBn = isBn,
                matchingCustomers = matchingCustomers,
                onCustomerClick = onCustomerClick
            )
        }

        // 2. GIANT VOICE ACTION BANNER
        item {
            Card(
                onClick = onVoiceClick,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                ),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("home_voice_banner")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(EmeraldPrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Mic,
                            contentDescription = "Speak Mic",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isBn) "মুখে বলে হিসাব লিখুন" else "Speak to Record Entry",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = if (isBn) "যেমন: 'রহিম ৪৫০ বাকি' বা 'করিম ২০০ জমা'" else "e.g. 'Rahim 450 credit' or 'Karim 200 paid'",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }

        // 3. QUICK SHORTCUT BUTTONS
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onAddCustomerClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("home_add_customer_btn")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isBn) "নতুন ক্রেতা" else "Add Customer", fontSize = 13.sp)
                }

                OutlinedButton(
                    onClick = onManualEntryClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("home_manual_entry_btn")
                ) {
                    Icon(Icons.Default.EditNote, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isBn) "কীবোর্ড এন্ট্রি" else "Type Entry", fontSize = 13.sp)
                }
            }
        }

        // 4. MULTI-SELECT CONTEXTUAL ACTION BAR (Active during selection)
        item {
            AnimatedVisibility(
                visible = uiState.isSelectionModeActive,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("batch_selection_bar")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = onClearSelection) {
                                Icon(Icons.Default.Close, contentDescription = "Close Selection")
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            val count = uiState.selectedTransactionIds.size
                            Text(
                                text = if (isBn) {
                                    "${BanglaVoiceParser.toBanglaDigits(count.toString())}টি নির্বাচিত"
                                } else {
                                    "$count selected"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButtonWithTag(
                                onClick = onSelectAll,
                                text = if (isBn) "সব বাছুন" else "Select All",
                                tag = "select_all_btn"
                            )

                            Spacer(modifier = Modifier.width(4.dp))

                            Button(
                                onClick = onDeleteSelected,
                                enabled = uiState.selectedTransactionIds.isNotEmpty(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("batch_delete_button")
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete Selected",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isBn) "মুছুন" else "Delete",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // 5. RECENT TRANSACTIONS HEADER & FILTER CHIPS
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (isBn) "সাম্প্রতিক লেনদেন" else "Recent Transactions",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isBn) {
                                "(${BanglaVoiceParser.toBanglaDigits(filteredTransactions.size.toString())}টি)"
                            } else {
                                "(${filteredTransactions.size})"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (uiState.transactions.isNotEmpty()) {
                        Surface(
                            onClick = onToggleSelectionMode,
                            shape = RoundedCornerShape(12.dp),
                            color = if (uiState.isSelectionModeActive) {
                                EmeraldPrimary.copy(alpha = 0.15f)
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                            },
                            modifier = Modifier.testTag("toggle_selection_mode_btn")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (uiState.isSelectionModeActive) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                                    contentDescription = "Batch Select",
                                    modifier = Modifier.size(16.dp),
                                    tint = if (uiState.isSelectionModeActive) EmeraldPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (uiState.isSelectionModeActive) {
                                        if (isBn) "সম্পন্ন" else "Done"
                                    } else {
                                        if (isBn) "বাছাই" else "Select"
                                    },
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (uiState.isSelectionModeActive) EmeraldPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Date Range Filter Quick Chips (5 responsive scrollable tabs)
                val dateScrollState = rememberScrollState()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(dateScrollState),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = uiState.dateRangeSelection.type == DateRangeFilterType.ALL,
                        onClick = { onDateRangeChange(DateRangeFilterType.ALL) },
                        label = { Text(if (isBn) "সব দিন" else "All Time") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = EmeraldPrimary.copy(alpha = 0.2f),
                            selectedLabelColor = EmeraldPrimary
                        ),
                        modifier = Modifier.testTag("date_filter_all")
                    )

                    FilterChip(
                        selected = uiState.dateRangeSelection.type == DateRangeFilterType.TODAY,
                        onClick = { onDateRangeChange(DateRangeFilterType.TODAY) },
                        label = { Text(if (isBn) "আজকে" else "Today") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = EmeraldPrimary.copy(alpha = 0.2f),
                            selectedLabelColor = EmeraldPrimary
                        ),
                        modifier = Modifier.testTag("date_filter_today")
                    )

                    FilterChip(
                        selected = uiState.dateRangeSelection.type == DateRangeFilterType.THIS_WEEK,
                        onClick = { onDateRangeChange(DateRangeFilterType.THIS_WEEK) },
                        label = { Text(if (isBn) "এই সপ্তাহে" else "This Week") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = EmeraldPrimary.copy(alpha = 0.2f),
                            selectedLabelColor = EmeraldPrimary
                        ),
                        modifier = Modifier.testTag("date_filter_this_week")
                    )

                    FilterChip(
                        selected = uiState.dateRangeSelection.type == DateRangeFilterType.THIS_MONTH,
                        onClick = { onDateRangeChange(DateRangeFilterType.THIS_MONTH) },
                        label = { Text(if (isBn) "এই মাসে" else "This Month") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = EmeraldPrimary.copy(alpha = 0.2f),
                            selectedLabelColor = EmeraldPrimary
                        ),
                        modifier = Modifier.testTag("date_filter_this_month")
                    )

                    // 5th Tab: Calendar Icon-Only button for Custom Date Range
                    FilterChip(
                        selected = uiState.dateRangeSelection.type == DateRangeFilterType.CUSTOM,
                        onClick = onOpenCustomDateRange,
                        label = {
                            if (uiState.dateRangeSelection.type == DateRangeFilterType.CUSTOM) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.DateRange,
                                        contentDescription = if (isBn) "তারিখ নির্বাচন" else "Select Date Range",
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(uiState.dateRangeSelection.getDisplayLabel(isBn))
                                }
                            } else {
                                Icon(
                                    Icons.Default.DateRange,
                                    contentDescription = if (isBn) "তারিখ নির্বাচন" else "Select Date Range",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = GoldTertiary.copy(alpha = 0.25f),
                            selectedLabelColor = GoldTertiary,
                            selectedLeadingIconColor = GoldTertiary
                        ),
                        modifier = Modifier.testTag("date_filter_custom")
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Transaction Type Filter Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val totalCountStr = if (isBn) BanglaVoiceParser.toBanglaDigits("${uiState.transactions.size}") else "${uiState.transactions.size}"
                    FilterChip(
                        selected = uiState.transactionTypeFilter == null,
                        onClick = { onFilterChange(null) },
                        label = { Text(if (isBn) "সব ($totalCountStr)" else "All ($totalCountStr)") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = EmeraldPrimary.copy(alpha = 0.2f),
                            selectedLabelColor = EmeraldPrimary
                        ),
                        modifier = Modifier.testTag("filter_all")
                    )

                    FilterChip(
                        selected = uiState.transactionTypeFilter == TransactionType.BAKI_SALE,
                        onClick = { onFilterChange(TransactionType.BAKI_SALE) },
                        label = { Text(if (isBn) "বাকি" else "Credit") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = BakiRed.copy(alpha = 0.15f),
                            selectedLabelColor = BakiRed
                        ),
                        modifier = Modifier.testTag("filter_baki")
                    )

                    FilterChip(
                        selected = uiState.transactionTypeFilter == TransactionType.REPAYMENT,
                        onClick = { onFilterChange(TransactionType.REPAYMENT) },
                        label = { Text(if (isBn) "জমা" else "Paid") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = JomaGreen.copy(alpha = 0.15f),
                            selectedLabelColor = JomaGreen
                        ),
                        modifier = Modifier.testTag("filter_repayment")
                    )

                    FilterChip(
                        selected = uiState.transactionTypeFilter == TransactionType.CASH_SALE,
                        onClick = { onFilterChange(TransactionType.CASH_SALE) },
                        label = { Text(if (isBn) "নগদ" else "Cash") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CashBlue.copy(alpha = 0.15f),
                            selectedLabelColor = CashBlue
                        ),
                        modifier = Modifier.testTag("filter_cash")
                    )
                }
            }
        }

        // 6. TRANSACTIONS LIST
        if (filteredTransactions.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ReceiptLong,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = if (uiState.transactionTypeFilter != null) {
                                if (isBn) "এই ফিল্টারে কোনো লেনদেন নেই" else "No transactions match this filter"
                            } else {
                                if (isBn) "এখনও কোনো লেনদেন লিপিবদ্ধ হয়নি" else "No transactions yet"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = if (isBn) "উপরের মাইক বাটনে চেপে কথা বলে শুরু করুন" else "Tap the microphone above to record your first sale",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(filteredTransactions.take(15), key = { it.id }) { tx ->
                val typeColor = when (tx.type) {
                    TransactionType.BAKI_SALE -> BakiRed
                    TransactionType.REPAYMENT -> JomaGreen
                    TransactionType.CASH_SALE -> CashBlue
                }
                val typeLabel = when (tx.type) {
                    TransactionType.BAKI_SALE -> BakiStrings.bakiSale(isBn)
                    TransactionType.REPAYMENT -> BakiStrings.repayment(isBn)
                    TransactionType.CASH_SALE -> BakiStrings.cashSale(isBn)
                }
                val isSelected = uiState.selectedTransactionIds.contains(tx.id)

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) {
                            EmeraldPrimary.copy(alpha = 0.12f)
                        } else {
                            MaterialTheme.colorScheme.surface
                        }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (uiState.isSelectionModeActive) {
                                onToggleSelect(tx.id)
                            } else {
                                onTransactionClick(tx)
                            }
                        }
                        .testTag("transaction_item_${tx.id}"),
                    elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 3.dp else 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            if (uiState.isSelectionModeActive) {
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = { onToggleSelect(tx.id) },
                                    colors = CheckboxDefaults.colors(checkedColor = EmeraldPrimary),
                                    modifier = Modifier.testTag("checkbox_${tx.id}")
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(typeColor)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = tx.customerName,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = typeLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = typeColor
                                    )
                                    val dateStr = dateFormatter.format(Date(tx.timestamp))
                                    Text(
                                        text = " • ${if (isBn) BanglaVoiceParser.toBanglaDigits(dateStr) else dateStr}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (!tx.originalSpokenText.isNullOrBlank()) {
                                    Text(
                                        text = "🗣️ \"${tx.originalSpokenText}\"",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                    )
                                }
                                if (tx.note.isNotBlank() && tx.note != tx.originalSpokenText) {
                                    Text(
                                        text = "📝 ${tx.note}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                                    )
                                }

                                val overdueInfo = OverdueHelper.computeTransactionOverdue(tx)
                                if (overdueInfo != null) {
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = overdueInfo.containerColor,
                                        contentColor = overdueInfo.contentColor,
                                        border = BorderStroke(1.dp, overdueInfo.borderColor),
                                        modifier = Modifier.testTag("tx_overdue_badge_${tx.id}")
                                    ) {
                                        Text(
                                            text = if (isBn) overdueInfo.labelBn else overdueInfo.labelEn,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Right: Amount and Action Buttons (Edit / Delete)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = BakiStrings.formatMoney(tx.amount, isBn),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 18.sp,
                                    color = typeColor
                                )
                            }

                            if (!uiState.isSelectionModeActive) {
                                Spacer(modifier = Modifier.width(4.dp))
                                IconButton(
                                    onClick = { onShareTransaction(tx) },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .testTag("share_tx_${tx.id}")
                                ) {
                                    Icon(
                                        Icons.Default.Share,
                                        contentDescription = "Share Receipt",
                                        tint = EmeraldPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { onEditTransaction(tx) },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .testTag("edit_tx_${tx.id}")
                                ) {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = "Edit Transaction",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { onDeleteTransaction(tx) },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .testTag("delete_tx_${tx.id}")
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Delete Transaction",
                                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 7. DEV CENTER POINT SUBTLE BRANDING FOOTER
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 80.dp),
                contentAlignment = Alignment.Center
            ) {
                DevCenterPointSubtleFooter()
            }
        }
    }
}

@Composable
private fun TextButtonWithTag(
    onClick: () -> Unit,
    text: String,
    tag: String
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = Color.Transparent,
        modifier = Modifier.testTag(tag)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
        )
    }
}

/**
 * Self-contained adaptive header directly embedded in the dashboard.
 * Designed with fixed control dimensions so toggling between বাংলা and English
 * preserves 100% of the layout geometry without text collision or horizontal jitter.
 */
@Composable
fun IntegratedDashboardHeader(
    uiState: BakiUiState,
    isBn: Boolean,
    onToggleLanguage: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onSignInWithGoogle: () -> Unit = {},
    onSyncClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 10.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left Branding: Logo + Shop Name & Tagline (Takes all available space, never wraps vertically)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .border(1.dp, EmeraldPrimary.copy(alpha = 0.25f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_baki_logo),
                    contentDescription = "Baki Bondhu",
                    tint = Color.Unspecified,
                    modifier = Modifier
                        .size(28.dp)
                        .testTag("dashboard_header_logo")
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                // Primary: Shop Name (or App Name if blank)
                val primaryTitle = uiState.settings.shopName.ifBlank {
                    if (isBn) "বিসমিল্লাহ জেনারেল স্টোর" else "Baki Bondhu"
                }
                Text(
                    text = primaryTitle,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )

                // Subtitle: App Name & Tagline
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isBn) "বাকী বন্ধু • সহজ খাতা" else "Baki Bondhu • Ledger",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(6.dp))

        // Right Controls: Compact Language Switcher + Cloud Sync + Google Account + Settings
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            // Fixed-width Language Switcher (Compact 60dp pill, immune to wrapping)
            Surface(
                onClick = onToggleLanguage,
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier
                    .width(62.dp)
                    .height(32.dp)
                    .testTag("language_toggle_button")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        Icons.Default.Language,
                        contentDescription = "Language",
                        modifier = Modifier.size(13.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = if (isBn) "বাংলা" else "ENG",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            // Cloud Sync Status Indicator
            CloudSyncIndicator(
                isSyncing = uiState.isSyncingCloud,
                currentUser = uiState.currentUser,
                onClick = onSyncClick,
                isBn = isBn
            )

            // Google Cloud Account Avatar / Sign-In
            val currentUser = uiState.currentUser
            IconButton(
                onClick = {
                    if (currentUser == null) {
                        onSignInWithGoogle()
                    } else {
                        onOpenSettings()
                    }
                },
                modifier = Modifier
                    .size(32.dp)
                    .testTag("google_account_button")
            ) {
                if (currentUser != null) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(RoyalJadeForest),
                        contentAlignment = Alignment.Center
                    ) {
                        val initial = (currentUser.displayName?.firstOrNull()
                            ?: currentUser.email?.firstOrNull()
                            ?: 'G').uppercaseChar()
                        Text(
                            text = "$initial",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = SolarChampagne
                        )
                    }
                } else {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_google),
                        contentDescription = "Google Sign-In",
                        tint = Color.Unspecified,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Settings Gear Icon
            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier
                    .size(32.dp)
                    .testTag("settings_button")
            ) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/**
 * Summary card positioned at the very top of the main screen displaying the total outstanding
 * debt across all customers, debtor count, daily credit/repayment metrics, and cash drawer
 * reconciliation for an immediate, high-fidelity view of shop finances.
 */
@Composable
fun ShopDebtSummaryCard(
    uiState: BakiUiState,
    isBn: Boolean,
    onOpenEndOfDaySummary: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val debtorCustomers = remember(uiState.customers) {
        uiState.customers.filter { it.balance > 0 }
    }
    val debtorCount = debtorCustomers.size

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("summary_debt_card")
            .testTag("hero_total_baki_card")
            .testTag("total_outstanding_debt_card"),
        shape = RoundedCornerShape(26.dp),
        border = BorderStroke(1.dp, RoyalJadeGlassBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(
                            RoyalJadeObsidian,
                            RoyalJadeForest,
                            RoyalJadeAlpine
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Header Row: Title & Sync / Debtor Status Badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.AccountBalanceWallet,
                                contentDescription = "Shop Wallet",
                                tint = SolarChampagne,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (isBn) "মোট বকেয়া পাওনা" else "Total Outstanding Debt",
                                color = Color.White.copy(alpha = 0.95f),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isBn) "সকল খরিদ্দারের সম্মিলিত বাকি" else "Combined credit across all customers",
                                color = Color.White.copy(alpha = 0.75f),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }

                    // Debtor count pill badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.18f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                            .testTag("text_debtor_count")
                    ) {
                        val debtorLabel = if (isBn) {
                            if (debtorCount > 0) "${BanglaVoiceParser.toBanglaDigits(debtorCount.toString())} জন বাকিদার" else "বকেয়া নেই"
                        } else {
                            if (debtorCount > 0) "$debtorCount debtors" else "No dues"
                        }
                        Text(
                            text = debtorLabel,
                            color = if (debtorCount > 0) SolarChampagneGlow else Color(0xFFA7F3D0),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Giant Total Debt Amount Display (Solar Champagne) - Guaranteed complete visibility
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.testTag("text_total_debt_amount")
                ) {
                    Text(
                        text = "৳",
                        color = SolarChampagne,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    val amountStr = remember(uiState.totalBaki, isBn) {
                        val intVal = uiState.totalBaki.toInt()
                        val text = intVal.toString()
                        if (isBn) BanglaVoiceParser.toBanglaDigits(text) else text
                    }
                    Text(
                        text = amountStr,
                        color = SolarChampagne,
                        fontSize = if (uiState.totalBaki >= 1000000) 28.sp else if (uiState.totalBaki >= 100000) 32.sp else 36.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.5).sp,
                        maxLines = 1,
                        softWrap = false
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Bottom Financial Stats Grid (Compact Modern Cards)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 1. Today's Credit Sales
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF2E0A0D).copy(alpha = 0.5f))
                            .border(1.dp, CoralCredit.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
                            .padding(10.dp)
                            .testTag("today_credit_stat")
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.ArrowUpward,
                                    contentDescription = null,
                                    tint = CoralCredit,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isBn) "আজকের বাকি" else "Today's Credit",
                                    color = Color.White.copy(alpha = 0.8f),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = BakiStrings.formatMoney(uiState.todayBakiSales, isBn),
                                color = CoralCredit,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    // 2. Today's Repayments / Collection
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF063328).copy(alpha = 0.5f))
                            .border(1.dp, MintCash.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
                            .padding(10.dp)
                            .testTag("today_repayment_stat")
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.ArrowDownward,
                                    contentDescription = null,
                                    tint = MintCash,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isBn) "আজকের জমা" else "Today's Paid",
                                    color = Color.White.copy(alpha = 0.8f),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = BakiStrings.formatMoney(uiState.todayRepayments, isBn),
                                color = MintCash,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    // 3. Cash Drawer Reconciliation (Physical Cash In Hand)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF062235).copy(alpha = 0.5f))
                            .border(1.dp, CyanNet.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
                            .padding(10.dp)
                            .testTag("cash_drawer_stat")
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Payments,
                                    contentDescription = null,
                                    tint = CyanNet,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isBn) "ক্যাশ ড্রয়ার" else "Cash Till",
                                    color = Color.White.copy(alpha = 0.8f),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = BakiStrings.formatMoney(uiState.cashDrawerBalance, isBn),
                                color = CyanNet,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // End-of-Day Reconciliation Action Banner
                Surface(
                    onClick = onOpenEndOfDaySummary,
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White.copy(alpha = 0.15f),
                    contentColor = Color.White,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("home_closing_reconciliation_btn")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ReceiptLong,
                                contentDescription = null,
                                tint = SolarChampagne,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isBn) "দিনের ক্লোজিং হিসাব ও ক্যাশ ড্রয়ার মিলান" else "Daily Closing & Cash Till Reconciliation",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Small Cloud Sync status indicator icon in the dashboard header that shows:
 * - A spinning cloud animation when data is actively uploading/backing up to Firebase
 * - A crisp green tick for 'Synced' when all transactions are safely stored in Firebase
 * - A subtle outline cloud when working locally / offline
 */
@Composable
fun CloudSyncIndicator(
    isSyncing: Boolean,
    currentUser: FirebaseUser?,
    onClick: () -> Unit,
    isBn: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "sync_rotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sync_degrees"
    )

    IconButton(
        onClick = onClick,
        modifier = modifier
            .size(32.dp)
            .testTag("cloud_sync_indicator")
    ) {
        when {
            isSyncing -> {
                Icon(
                    imageVector = Icons.Default.CloudSync,
                    contentDescription = BakiStrings.cloudSyncing(isBn),
                    tint = SolarChampagne,
                    modifier = Modifier
                        .size(20.dp)
                        .rotate(rotation)
                )
            }
            currentUser != null -> {
                // Verified Synced State: Green tick
                Icon(
                    imageVector = Icons.Default.CloudDone,
                    contentDescription = BakiStrings.cloudSynced(isBn),
                    tint = MintCash,
                    modifier = Modifier.size(20.dp)
                )
            }
            else -> {
                // Local / Guest state
                Icon(
                    imageVector = Icons.Default.CloudQueue,
                    contentDescription = BakiStrings.tapToSync(isBn),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(19.dp)
                )
            }
        }
    }
}

/**
 * Persistent search bar positioned on the main dashboard directly below the summary card.
 * Enables shopkeepers to instantly find customers by name or phone number, displaying
 * matching customer chips with current dues and filtering the transactions list dynamically.
 */
@Composable
fun DashboardSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    isBn: Boolean,
    matchingCustomers: List<CustomerEntity> = emptyList(),
    onCustomerClick: (CustomerEntity) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("dashboard_search_bar")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = EmeraldPrimary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                androidx.compose.foundation.text.BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 10.dp)
                        .testTag("dashboard_search_input"),
                    decorationBox = { innerTextField ->
                        if (query.isEmpty()) {
                            Text(
                                text = BakiStrings.searchHint(isBn),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                            )
                        }
                        innerTextField()
                    }
                )
                if (query.isNotEmpty()) {
                    IconButton(
                        onClick = { onQueryChange("") },
                        modifier = Modifier
                            .size(28.dp)
                            .testTag("dashboard_search_clear_btn")
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Clear",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // Live matching customer chips when searching
        if (query.isNotBlank() && matchingCustomers.isNotEmpty()) {
            Text(
                text = if (isBn) "খুঁজে পাওয়া খরিদ্দার (${BanglaVoiceParser.toBanglaDigits(matchingCustomers.size.toString())}):" else "Matching Customers (${matchingCustomers.size}):",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
            androidx.compose.foundation.lazy.LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(matchingCustomers, key = { it.id }) { cust ->
                    Surface(
                        onClick = { onCustomerClick(cust) },
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(
                            1.dp,
                            if (cust.balance > 0) CoralCredit.copy(alpha = 0.4f) else EmeraldPrimary.copy(alpha = 0.3f)
                        ),
                        shadowElevation = 2.dp,
                        modifier = Modifier.testTag("search_cust_chip_${cust.id}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(EmeraldPrimary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${cust.name.firstOrNull() ?: '?'}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = EmeraldPrimary
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = cust.name,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (cust.balance > 0) "${if (isBn) "বাকি: " else "Due: "}${BakiStrings.formatMoney(cust.balance, isBn)}" else (if (isBn) "বকেয়া নেই" else "No Due"),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (cust.balance > 0) CoralCredit else MintCash,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        } else if (query.isNotBlank() && matchingCustomers.isEmpty()) {
            Text(
                text = if (isBn) "কোনো খরিদ্দার পাওয়া যায়নি" else "No matching customer found",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.padding(horizontal = 6.dp)
            )
        }
    }
}

