package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.CustomerEntity
import com.example.data.model.TransactionType
import com.example.ui.BakiStrings
import com.example.ui.theme.BakiRed
import com.example.ui.theme.CashBlue
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.JomaGreen
import com.example.voice.ParsedVoiceResult

/**
 * Bottom Sheet / Dialog that immediately prompts the user after recording voice:
 * - Shows what was heard & detected amount/type
 * - Lets user pick an existing customer with 1 tap (highlighting best match)
 * - OR create a new customer immediately with 1 tap.
 */
@Composable
fun CustomerSelectOrNewSheet(
    parsedResult: ParsedVoiceResult,
    customers: List<CustomerEntity>,
    isBn: Boolean,
    onSelectCustomer: (customer: CustomerEntity) -> Unit,
    onCreateNewCustomerAndAssign: (name: String, phone: String) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var isCreatingNew by remember { mutableStateOf(false) }

    // Pre-fill with detected name if available and not a generic word
    val initialName = remember(parsedResult.customerName) {
        if (parsedResult.customerName != "ক্রেতা" && parsedResult.customerName.isNotBlank()) {
            parsedResult.customerName
        } else {
            ""
        }
    }
    var newCustomerName by remember { mutableStateOf(initialName) }
    var newCustomerPhone by remember { mutableStateOf("") }

    val filteredCustomers = remember(customers, searchQuery) {
        if (searchQuery.isBlank()) customers
        else customers.filter {
            it.name.contains(searchQuery, ignoreCase = true) || it.phone.contains(searchQuery)
        }
    }

    // Find suggested match
    val suggestedCustomer = remember(customers, parsedResult.customerName) {
        customers.find { it.name.equals(parsedResult.customerName, ignoreCase = true) }
    }

    val typeColor = when (parsedResult.type) {
        TransactionType.BAKI_SALE -> BakiRed
        TransactionType.REPAYMENT -> JomaGreen
        TransactionType.CASH_SALE -> CashBlue
    }

    val typeLabel = when (parsedResult.type) {
        TransactionType.BAKI_SALE -> BakiStrings.bakiSale(isBn)
        TransactionType.REPAYMENT -> BakiStrings.repayment(isBn)
        TransactionType.CASH_SALE -> BakiStrings.cashSale(isBn)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.90f)
                .testTag("customer_select_or_new_sheet")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (isBn) "ভয়েস এন্ট্রি নিশ্চিত করুন" else "Confirm Voice Entry",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isBn) "কার নামে হিসাবটি যোগ করবেন?" else "Assign this entry to which customer?",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Recorded Entry Summary Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = typeColor.copy(alpha = 0.12f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(typeColor)
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = typeLabel,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "🗣️ \"${parsedResult.rawText}\"",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Text(
                            text = BakiStrings.formatMoney(parsedResult.amount, isBn),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = typeColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Toggle: Create New Customer vs Select Existing
                if (!isCreatingNew) {
                    // Option to Switch to Create New Customer
                    OutlinedButton(
                        onClick = { isCreatingNew = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("switch_to_create_customer_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = EmeraldPrimary)
                    ) {
                        Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isBn) "+ নতুন ক্রেতা তৈরি ও সংরক্ষণ করুন" else "+ Create New Customer & Assign",
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Search Customer
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text(if (isBn) "তালিকা থেকে খুঁজুন..." else "Search customer...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("voice_customer_search_input")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Suggested Customer Card if matched
                    if (suggestedCustomer != null && searchQuery.isBlank()) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectCustomer(suggestedCustomer) }
                                .border(1.5.dp, EmeraldPrimary, RoundedCornerShape(12.dp))
                                .testTag("suggested_customer_card"),
                            colors = CardDefaults.cardColors(containerColor = EmeraldPrimary.copy(alpha = 0.08f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = EmeraldPrimary)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = suggestedCustomer.name,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                        Text(
                                            text = if (isBn) "ভয়েস থেকে প্রস্তাবিত" else "Matched from speech",
                                            fontSize = 11.sp,
                                            color = EmeraldPrimary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                                Button(
                                    onClick = { onSelectCustomer(suggestedCustomer) },
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(if (isBn) "যুক্ত করুন" else "Select")
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // Existing Customer List
                    Text(
                        text = if (isBn) "সকল খরিদ্দার তালিকা (${filteredCustomers.size}):" else "Existing Customers (${filteredCustomers.size}):",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    if (filteredCustomers.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = if (isBn) "কোনো খরিদ্দার পাওয়া যায়নি" else "No matching customers found",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = { isCreatingNew = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                                ) {
                                    Text(if (isBn) "নতুন ক্রেতা হিসেবে যোগ করুন" else "Add As New Customer")
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(filteredCustomers, key = { it.id }) { cust ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onSelectCustomer(cust) }
                                        .testTag("select_cust_${cust.id}"),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .clip(CircleShape)
                                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                                            }
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(cust.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                                if (cust.phone.isNotBlank()) {
                                                    Text(cust.phone, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                            }
                                        }

                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = BakiStrings.formatMoney(cust.balance, isBn),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = if (cust.balance > 0) BakiRed else JomaGreen
                                            )
                                            Text(
                                                text = if (isBn) "ট্যাপ করে সিলেক্ট করুন" else "Tap to assign",
                                                fontSize = 10.sp,
                                                color = EmeraldPrimary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Inline Create New Customer Form
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = if (isBn) "নতুন ক্রেতার তথ্য দিন" else "New Customer Information",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldPrimary
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = newCustomerName,
                                onValueChange = { newCustomerName = it },
                                label = { Text(if (isBn) "ক্রেতার নাম *" else "Customer Name *") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("new_customer_name_input")
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = newCustomerPhone,
                                onValueChange = { newCustomerPhone = it },
                                label = { Text(if (isBn) "মোবাইল নম্বর (ঐচ্ছিক)" else "Phone Number (Optional)") },
                                placeholder = { Text("01XXXXXXXXX") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("new_customer_phone_input")
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = {
                                    if (newCustomerName.isNotBlank()) {
                                        onCreateNewCustomerAndAssign(newCustomerName.trim(), newCustomerPhone.trim())
                                    }
                                },
                                enabled = newCustomerName.isNotBlank(),
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("confirm_create_and_assign_btn")
                            ) {
                                Text(
                                    text = if (isBn) "ক্রেতা তৈরি ও হিসাব সংরক্ষণ করুন" else "Create Customer & Save Entry",
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedButton(
                                onClick = { isCreatingNew = false },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(if (isBn) "তালিকায় ফিরে যান" else "Back to Customer List")
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Cancel Button
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (isBn) "বাতিল" else "Cancel")
                }
            }
        }
    }
}
