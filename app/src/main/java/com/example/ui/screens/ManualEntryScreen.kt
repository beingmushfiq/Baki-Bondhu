package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CustomerEntity
import com.example.data.model.TransactionType
import com.example.ui.BakiStrings
import com.example.ui.BakiUiState
import com.example.ui.theme.BakiRed
import com.example.ui.theme.CashBlue
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.JomaGreen
import com.example.voice.BanglaVoiceParser

/**
 * Full-screen Jetpack Compose destination for manually recording transactions
 * and adding customers. Serves as the primary tactile fallback when voice recognition
 * encounters errors or ambient noise in a busy shop.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ManualEntryScreen(
    uiState: BakiUiState,
    fallbackVoiceSnippet: String? = null,
    onSaveTransaction: (name: String, phone: String, amount: Double, type: TransactionType, note: String) -> Unit,
    onAddCustomer: (name: String, phone: String, initialDue: Double, note: String) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onNavigateBack() }

    val isBn = uiState.isBangla
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Transaction, 1: Add Customer

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (isBn) "কীবোর্ড ম্যানুয়াল এন্ট্রি" else "Manual Entry",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isBn) "ভয়েস ফলব্যাক ও সরাসরি হিসাব" else "Voice Fallback & Direct Entry",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("manual_entry_back_button")
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier.testTag("manual_entry_screen")
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 600.dp)
            ) {
                // Tab Row: Add Transaction vs Add Customer
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Receipt,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isBn) "হিসাব লিখুন" else "Record Entry",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        },
                        modifier = Modifier.testTag("tab_manual_transaction")
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.PersonAdd,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isBn) "নতুন ক্রেতা" else "Add Customer",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        },
                        modifier = Modifier.testTag("tab_manual_add_customer")
                    )
                }

                // Voice fallback banner if user arrived here from voice recognition
                if (!fallbackVoiceSnippet.isNullOrBlank()) {
                    VoiceFallbackAlertCard(
                        snippet = fallbackVoiceSnippet,
                        isBn = isBn,
                        onUseAsNote = { /* pre-filled */ }
                    )
                }

                // Tab Content
                when (selectedTab) {
                    0 -> ManualTransactionForm(
                        customers = uiState.customers,
                        isBn = isBn,
                        initialNote = fallbackVoiceSnippet ?: "",
                        onSave = { name, phone, amount, type, note ->
                            onSaveTransaction(name, phone, amount, type, note)
                            onNavigateBack()
                        }
                    )
                    1 -> ManualAddCustomerForm(
                        isBn = isBn,
                        onAdd = { name, phone, due, note ->
                            onAddCustomer(name, phone, due, note)
                            onNavigateBack()
                        }
                    )
                }
            }
        }
    }
}

/**
 * Notice card displayed when falling back from a voice recognition attempt.
 */
@Composable
private fun VoiceFallbackAlertCard(
    snippet: String,
    isBn: Boolean,
    onUseAsNote: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("voice_fallback_alert_card"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.ErrorOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isBn) "ভয়েস শনাক্তকরণ ফলব্যাক" else "Voice Recognition Fallback",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Text(
                    text = if (isBn) {
                        "ভয়েস থেকে সঠিক তথ্য পাওয়া যায়নি। কীবোর্ড ব্যবহার করে সহজে তথ্য লিখুন।"
                    } else {
                        "Voice recognition did not capture complete details. Enter manually below."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                if (snippet.isNotBlank()) {
                    Text(
                        text = "\"$snippet\"",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
    }
}

/**
 * Manual form for recording credit sales, collections, and cash sales.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ManualTransactionForm(
    customers: List<CustomerEntity>,
    isBn: Boolean,
    initialNote: String,
    onSave: (name: String, phone: String, amount: Double, type: TransactionType, note: String) -> Unit
) {
    val focusManager = LocalFocusManager.current
    var isNewCustomerMode by remember { mutableStateOf(false) }
    var selectedCustomer by remember { mutableStateOf<CustomerEntity?>(customers.firstOrNull()) }
    var customerSearchQuery by remember { mutableStateOf("") }

    var manualCustomerName by remember { mutableStateOf("") }
    var manualCustomerPhone by remember { mutableStateOf("") }

    var amountText by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(TransactionType.BAKI_SALE) }
    var noteText by remember { mutableStateOf(initialNote) }

    var errorMessage by remember { mutableStateOf<String?>(null) }

    val filteredCustomers = remember(customerSearchQuery, customers) {
        if (customerSearchQuery.isBlank()) {
            customers.take(8)
        } else {
            customers.filter {
                it.name.contains(customerSearchQuery, ignoreCase = true) ||
                        it.phone.contains(customerSearchQuery)
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Transaction Type Selector
        item {
            Text(
                text = if (isBn) "লেনদেনের ধরন নির্বাচন করুন:" else "Select Transaction Type:",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Baki Sale (বাকী)
                FilterChip(
                    selected = selectedType == TransactionType.BAKI_SALE,
                    onClick = { selectedType = TransactionType.BAKI_SALE },
                    label = {
                        Text(
                            text = BakiStrings.bakiSale(isBn),
                            fontWeight = if (selectedType == TransactionType.BAKI_SALE) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = BakiRed.copy(alpha = 0.2f),
                        selectedLabelColor = BakiRed
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_tx_baki")
                )

                // Repayment (জমা)
                FilterChip(
                    selected = selectedType == TransactionType.REPAYMENT,
                    onClick = { selectedType = TransactionType.REPAYMENT },
                    label = {
                        Text(
                            text = BakiStrings.repayment(isBn),
                            fontWeight = if (selectedType == TransactionType.REPAYMENT) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = JomaGreen.copy(alpha = 0.2f),
                        selectedLabelColor = JomaGreen
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_tx_repay")
                )

                // Cash Sale (নগদ)
                FilterChip(
                    selected = selectedType == TransactionType.CASH_SALE,
                    onClick = { selectedType = TransactionType.CASH_SALE },
                    label = {
                        Text(
                            text = BakiStrings.cashSale(isBn),
                            fontWeight = if (selectedType == TransactionType.CASH_SALE) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CashBlue.copy(alpha = 0.2f),
                        selectedLabelColor = CashBlue
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_tx_cash")
                )
            }
        }

        // 2. Customer Section (Toggle between existing customer or new customer)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isBn) "ক্রেতার তথ্য" else "Customer Information",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = if (isNewCustomerMode) {
                                if (isBn) "বিদ্যমান তালিকা থেকে নিন" else "Pick from list"
                            } else {
                                if (isBn) "+ নতুন ক্রেতা লিখুন" else "+ Type new customer"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldPrimary,
                            modifier = Modifier
                                .clickable { isNewCustomerMode = !isNewCustomerMode }
                                .padding(4.dp)
                                .testTag("toggle_customer_mode_button")
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (!isNewCustomerMode) {
                        // Pick existing customer
                        OutlinedTextField(
                            value = customerSearchQuery,
                            onValueChange = { customerSearchQuery = it },
                            label = { Text(if (isBn) "ক্রেতা খুঁজুন..." else "Search customer...") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            trailingIcon = {
                                if (customerSearchQuery.isNotBlank()) {
                                    IconButton(onClick = { customerSearchQuery = "" }) {
                                        Icon(Icons.Default.Close, contentDescription = "Clear")
                                    }
                                }
                            },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("search_customer_field")
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Customer Selection Chips / List
                        Text(
                            text = if (isBn) "তালিকা থেকে নির্বাচন করুন:" else "Select customer:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(filteredCustomers, key = { it.id }) { cust ->
                                val isSelected = selectedCustomer?.id == cust.id
                                Surface(
                                    onClick = { selectedCustomer = cust },
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) EmeraldPrimary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface,
                                    border = androidx.compose.foundation.BorderStroke(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) EmeraldPrimary else MaterialTheme.colorScheme.outlineVariant
                                    ),
                                    modifier = Modifier.testTag("cust_chip_${cust.id}")
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        if (isSelected) {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = null,
                                                tint = EmeraldPrimary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                        }
                                        Column {
                                            Text(
                                                text = cust.name,
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "বাকি: ${BakiStrings.formatMoney(cust.balance, isBn)}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (cust.balance > 0) BakiRed else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Selected Customer banner
                        selectedCustomer?.let { cust ->
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(EmeraldPrimary.copy(alpha = 0.08f))
                                    .padding(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Person, contentDescription = null, tint = EmeraldPrimary)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = cust.name,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                    Text(
                                        text = "${if (isBn) "পূর্বের বাকি:" else "Due:"} ${BakiStrings.formatMoney(cust.balance, isBn)}",
                                        fontWeight = FontWeight.Bold,
                                        color = if (cust.balance > 0) BakiRed else JomaGreen,
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                            }
                        }
                    } else {
                        // New Customer Inline Input
                        OutlinedTextField(
                            value = manualCustomerName,
                            onValueChange = { manualCustomerName = it },
                            label = { Text(if (isBn) "ক্রেতার নাম (যেমন: রহিম চাচা)" else "Customer Name") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_tx_customer_name")
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = manualCustomerPhone,
                            onValueChange = { manualCustomerPhone = it },
                            label = { Text(if (isBn) "মোবাইল নম্বর (ঐচ্ছিক)" else "Phone Number (Optional)") },
                            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_tx_customer_phone")
                        )
                    }
                }
            }
        }

        // 3. Amount Input with Quick Addition Chips
        item {
            Column {
                Text(
                    text = BakiStrings.amountTaka(isBn),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    placeholder = { Text(if (isBn) "০ (টাকার অংক লিখুন)" else "0.00") },
                    leadingIcon = {
                        Text(
                            text = "৳",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldPrimary,
                            modifier = Modifier.padding(start = 12.dp)
                        )
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next
                    ),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldPrimary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_tx_amount")
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Quick Amount Addition Chips
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val increments = listOf(50, 100, 200, 500, 1000, 2000)
                    increments.forEach { inc ->
                        SuggestionChip(
                            onClick = {
                                val curAscii = BanglaVoiceParser.toAsciiDigits(amountText).toDoubleOrNull() ?: 0.0
                                val newTotal = (curAscii + inc).toInt()
                                amountText = if (isBn) {
                                    BanglaVoiceParser.toBanglaDigits(newTotal.toString())
                                } else {
                                    newTotal.toString()
                                }
                            },
                            label = {
                                val labelText = if (isBn) "+${BanglaVoiceParser.toBanglaDigits(inc.toString())} ৳" else "+$inc ৳"
                                Text(labelText, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            },
                            modifier = Modifier.testTag("chip_amount_$inc")
                        )
                    }
                }
            }
        }

        // 4. Details / Items note
        item {
            OutlinedTextField(
                value = noteText,
                onValueChange = { noteText = it },
                label = { Text(if (isBn) "পণ্য বা বিবরণ (ঐচ্ছিক)" else "Items / Note (Optional)") },
                placeholder = { Text(if (isBn) "যেমন: চাল ৫ কেজি, সয়াবিন তেল ১ লিটার" else "e.g. 5kg rice, 1L oil") },
                maxLines = 3,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_tx_note")
            )
        }

        // Error message if any
        if (errorMessage != null) {
            item {
                Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // 5. Submit Transaction Button
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    val cleanAmount = BanglaVoiceParser.toAsciiDigits(amountText).toDoubleOrNull()
                    val targetName = if (isNewCustomerMode) manualCustomerName.trim() else selectedCustomer?.name.orEmpty().trim()
                    val targetPhone = if (isNewCustomerMode) manualCustomerPhone.trim() else selectedCustomer?.phone.orEmpty().trim()

                    if (targetName.isBlank()) {
                        errorMessage = if (isBn) "অনুগ্রহ করে ক্রেতার নাম নির্বাচন বা প্রদান করুন" else "Please select or enter customer name"
                        return@Button
                    }
                    if (cleanAmount == null || cleanAmount <= 0) {
                        errorMessage = if (isBn) "সঠিক টাকার পরিমাণ লিখুন" else "Please enter a valid amount"
                        return@Button
                    }

                    errorMessage = null
                    onSave(targetName, targetPhone, cleanAmount, selectedType, noteText.trim())
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = when (selectedType) {
                        TransactionType.BAKI_SALE -> BakiRed
                        TransactionType.REPAYMENT -> JomaGreen
                        TransactionType.CASH_SALE -> CashBlue
                    }
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("button_save_transaction")
            ) {
                Icon(Icons.Default.Receipt, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                val actionLabel = when (selectedType) {
                    TransactionType.BAKI_SALE -> if (isBn) "বাকি হিসাব সংরক্ষণ করুন" else "Save Credit Sale"
                    TransactionType.REPAYMENT -> if (isBn) "জমা / পরিশোধ সংরক্ষণ করুন" else "Save Repayment"
                    TransactionType.CASH_SALE -> if (isBn) "নগদ বিক্রি সংরক্ষণ করুন" else "Save Cash Sale"
                }
                Text(
                    text = actionLabel,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }
}

/**
 * Dedicated form for creating a new customer in the Room database.
 */
@Composable
private fun ManualAddCustomerForm(
    isBn: Boolean,
    onAdd: (name: String, phone: String, initialDue: Double, note: String) -> Unit
) {
    val focusManager = LocalFocusManager.current
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var initialDueText by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = if (isBn) "খাতায় নতুন ক্রেতার প্রোফাইল যোগ করুন" else "Create New Customer in Ledger",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = if (isBn) "ক্রেতার নাম ও ফোন নম্বর দিয়ে আজীবন বাকি খাতা বজায় রাখুন।" else "Register customer details to track debts securely.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Customer Name
        item {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(BakiStrings.customerName(isBn)) },
                placeholder = { Text(if (isBn) "যেমন: কাশেম ভাই" else "e.g. Kashem") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_new_customer_name")
            )
        }

        // Mobile Phone
        item {
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text(BakiStrings.mobileNumber(isBn)) },
                placeholder = { Text(if (isBn) "যেমন: 017xxxxxxxx" else "e.g. 017xxxxxxxx") },
                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_new_customer_phone")
            )
        }

        // Initial Due
        item {
            OutlinedTextField(
                value = initialDueText,
                onValueChange = { initialDueText = it },
                label = { Text(if (isBn) "পূর্বের বকেয়া (যদি থাকে ৳)" else "Previous Due (Optional ৳)") },
                placeholder = { Text("0") },
                leadingIcon = {
                    Text(
                        "৳",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldPrimary,
                        modifier = Modifier.padding(start = 12.dp)
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_new_customer_due")
            )
        }

        // Address / Note
        item {
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text(if (isBn) "ঠিকানা বা পরিচিতি" else "Address / Reference") },
                placeholder = { Text(if (isBn) "যেমন: উত্তর পাড়া, লাল বাড়ি" else "e.g. North street") },
                maxLines = 3,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_new_customer_note")
            )
        }

        if (errorMessage != null) {
            item {
                Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Save Customer Button
        item {
            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = {
                    val cleanName = name.trim()
                    if (cleanName.isBlank()) {
                        errorMessage = if (isBn) "ক্রেতার নাম প্রদান করা আবশ্যক" else "Customer name is required"
                        return@Button
                    }
                    val due = BanglaVoiceParser.toAsciiDigits(initialDueText).toDoubleOrNull() ?: 0.0
                    errorMessage = null
                    onAdd(cleanName, phone.trim(), due, note.trim())
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("button_save_customer")
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isBn) "ক্রেতা সংরক্ষণ করুন" else "Save Customer",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }
}
