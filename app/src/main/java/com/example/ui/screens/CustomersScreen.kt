package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.painterResource
import com.example.R
import com.example.data.model.CustomerEntity
import com.example.ui.BakiStrings
import com.example.ui.BakiUiState
import com.example.ui.theme.BakiRed
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.JomaGreen
import com.example.util.OverdueHelper
import com.example.util.OverdueLevel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CustomersScreen(
    uiState: BakiUiState,
    onCustomerClick: (CustomerEntity) -> Unit,
    onSendSmsClick: (CustomerEntity) -> Unit,
    onSendWhatsAppClick: ((CustomerEntity) -> Unit)? = null,
    onAddCustomerClick: () -> Unit
) {
    val isBn = uiState.isBangla
    var searchQuery by remember { mutableStateOf("") }
    var filterTab by remember { mutableStateOf(0) } // 0: All, 1: With Due, 2: Settled
    val dateFormatter = SimpleDateFormat("dd MMM", Locale.getDefault())

    val filteredCustomers = remember(uiState.customers, searchQuery, filterTab) {
        uiState.customers.filter { cust ->
            val matchesQuery = searchQuery.isBlank() ||
                    cust.name.contains(searchQuery, ignoreCase = true) ||
                    cust.phone.contains(searchQuery)

            val matchesFilter = when (filterTab) {
                1 -> cust.balance > 0 // With Due
                2 -> cust.balance <= 0 // Settled
                else -> true
            }

            matchesQuery && matchesFilter
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
            Spacer(modifier = Modifier.height(8.dp))

            // 1. TOTAL OUTSTANDING AMOUNT SUMMARY CARD
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("customer_list_total_due_summary_card"),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    Color(0xFF004D40),
                                    Color(0xFF00695C),
                                    Color(0xFF0F766E)
                                )
                            )
                        )
                        .padding(16.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isBn) "মোট বাকি পাওনা (সর্বমোট)" else "Total Outstanding Debt",
                                color = Color.White.copy(alpha = 0.9f),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Medium
                            )

                            val dueCount = uiState.customers.count { it.balance > 0 }
                            val dueCountStr = if (isBn) com.example.voice.BanglaVoiceParser.toBanglaDigits("$dueCount") else "$dueCount"
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.White.copy(alpha = 0.2f))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = if (isBn) "$dueCountStr জনের বাকি" else "$dueCount Customers Due",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = BakiStrings.formatMoney(uiState.totalBaki, isBn),
                            color = Color(0xFFFFD54F),
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Black
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val settledCount = uiState.customers.count { it.balance <= 0 }
                            val totalCustomersStr = if (isBn) com.example.voice.BanglaVoiceParser.toBanglaDigits("${uiState.customers.size}") else "${uiState.customers.size}"
                            val settledCountStr = if (isBn) com.example.voice.BanglaVoiceParser.toBanglaDigits("$settledCount") else "$settledCount"
                            Text(
                                text = if (isBn) "মোট খরিদ্দার: $totalCustomersStr জন" else "Total Customers: ${uiState.customers.size}",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 12.sp
                            )
                            Text(
                                text = if (isBn) "পরিশোধিত: $settledCountStr জন" else "Cleared: $settledCount",
                                color = Color(0xFFA7F3D0),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Search Bar with Add Customer Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(BakiStrings.searchCustomer(isBn)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("search_customer_input")
                )

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = onAddCustomerClick,
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .height(54.dp)
                        .testTag("fab_add_customer")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Customer")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isBn) "নতুন" else "Add")
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Filter Tabs
            val allCount = if (isBn) com.example.voice.BanglaVoiceParser.toBanglaDigits("${uiState.customers.size}") else "${uiState.customers.size}"
            val dueCount = uiState.customers.count { it.balance > 0 }
            val dueCountStr = if (isBn) com.example.voice.BanglaVoiceParser.toBanglaDigits("$dueCount") else "$dueCount"
            val settledCount = uiState.customers.count { it.balance <= 0 }
            val settledCountStr = if (isBn) com.example.voice.BanglaVoiceParser.toBanglaDigits("$settledCount") else "$settledCount"

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = filterTab == 0,
                    onClick = { filterTab = 0 },
                    label = {
                        Text(
                            text = if (isBn) "সবাই ($allCount)" else "All ($allCount)",
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                            fontSize = 12.sp,
                            maxLines = 2
                        )
                    },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = filterTab == 1,
                    onClick = { filterTab = 1 },
                    label = {
                        Text(
                            text = if (isBn) "বাকি আছে ($dueCountStr)" else "Due ($dueCountStr)",
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                            fontSize = 12.sp,
                            maxLines = 2
                        )
                    },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = filterTab == 2,
                    onClick = { filterTab = 2 },
                    label = {
                        Text(
                            text = if (isBn) "পরিশোধিত ($settledCountStr)" else "Clear ($settledCountStr)",
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                            fontSize = 12.sp,
                            maxLines = 2
                        )
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Customer List
            if (filteredCustomers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = if (isBn) "কোনো ক্রেতা পাওয়া যায়নি" else "No customers found",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    itemsIndexed(filteredCustomers, key = { _, customer -> customer.id }) { index, customer ->
                        val serialNum = String.format("%02d", index + 1)
                        val serialText = if (isBn) "${com.example.voice.BanglaVoiceParser.toBanglaDigits(serialNum)}." else "$serialNum."
                        CustomerCardItem(
                            customer = customer,
                            serialText = serialText,
                            isBn = isBn,
                            dateFormatter = dateFormatter,
                            onClick = { onCustomerClick(customer) },
                            onSendSms = { onSendSmsClick(customer) },
                            onSendWhatsApp = { onSendWhatsAppClick?.invoke(customer) }
                        )
                    }

                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            com.example.ui.components.DevCenterPointSubtleFooter()
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(90.dp))
                    }
                }
        }
    }
}

@Composable
fun CustomerCardItem(
    customer: CustomerEntity,
    serialText: String? = null,
    isBn: Boolean,
    dateFormatter: SimpleDateFormat,
    onClick: () -> Unit,
    onSendSms: () -> Unit,
    onSendWhatsApp: (() -> Unit)? = null
) {
    val hasDue = customer.balance > 0
    val amountColor = if (hasDue) BakiRed else JomaGreen

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("customer_card_${customer.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Serial + Avatar + Details
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                if (!serialText.isNullOrBlank()) {
                    Text(
                        text = serialText,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = (if (hasDue) BakiRed else EmeraldPrimary).copy(alpha = 0.85f),
                        modifier = Modifier.padding(end = 6.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            if (hasDue) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f)
                            else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = customer.name.take(1),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = if (hasDue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f, fill = false)) {
                    Text(
                        text = customer.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (customer.phone.isNotBlank()) {
                            Icon(
                                Icons.Default.Phone,
                                contentDescription = null,
                                modifier = Modifier.size(11.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = customer.phone,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = " • ",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = dateFormatter.format(Date(customer.lastTransactionAt)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }

                    val overdueInfo = OverdueHelper.computeCustomerOverdue(customer)
                    if (hasDue && overdueInfo.level != OverdueLevel.NONE) {
                        Spacer(modifier = Modifier.height(3.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = overdueInfo.containerColor,
                            contentColor = overdueInfo.contentColor,
                            border = BorderStroke(1.dp, overdueInfo.borderColor),
                            modifier = Modifier.testTag("customer_overdue_badge_${customer.id}")
                        ) {
                            Text(
                                text = if (isBn) overdueInfo.labelBn else overdueInfo.labelEn,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Right: Balance & Quick WhatsApp/SMS reminder buttons (fixed compact bounds)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = BakiStrings.formatMoney(customer.balance, isBn),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = amountColor
                    )
                    Text(
                        text = if (hasDue) (if (isBn) "বাকি" else "Due") else (if (isBn) "পরিশোধ" else "Clear"),
                        style = MaterialTheme.typography.labelSmall,
                        color = amountColor,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                if (hasDue) {
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(
                        onClick = {
                            if (onSendWhatsApp != null) {
                                onSendWhatsApp()
                            } else {
                                onSendSms()
                            }
                        },
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF25D366).copy(alpha = 0.14f))
                            .testTag("whatsapp_btn_${customer.id}")
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_whatsapp),
                            contentDescription = "Send WhatsApp Reminder",
                            tint = Color(0xFF25D366),
                            modifier = Modifier.size(17.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = onSendSms,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(EmeraldPrimary.copy(alpha = 0.12f))
                            .testTag("sms_btn_${customer.id}")
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Message,
                            contentDescription = "Send SMS",
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
            }
        }
    }
}
