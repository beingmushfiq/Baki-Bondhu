package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.CustomerEntity
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.ui.BakiStrings
import com.example.ui.theme.BakiRed
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.JomaGreen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ArchiveZoneDialog(
    archivedCustomers: List<CustomerEntity>,
    archivedTransactions: List<TransactionEntity>,
    isBn: Boolean,
    onRestoreCustomer: (Long) -> Unit,
    onRestoreTransaction: (Long) -> Unit,
    onPurgeCustomer: (Long) -> Unit,
    onPurgeTransaction: (Long) -> Unit,
    onPurgeAll: () -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Customers, 1: Transactions
    var showConfirmPurgeAll by remember { mutableStateOf(false) }
    var itemToPurge by remember { mutableStateOf<Pair<Boolean, Long>?>(null) } // (isCustomer, id)

    val dateFormat = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(EmeraldPrimary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Archive,
                                contentDescription = null,
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (isBn) "আর্কাইভ জোন" else "Archive Zone",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (isBn) "মুছে ফেলা হিসাবের নিরাপত্তা সংরক্ষণাগার" else "Safely restore or permanently purge",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Tabs: Customers vs Transactions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        label = {
                            Text(
                                text = if (isBn) "কাস্টমার (${archivedCustomers.size})" else "Customers (${archivedCustomers.size})"
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        label = {
                            Text(
                                text = if (isBn) "লেনদেন (${archivedTransactions.size})" else "Ledger Entries (${archivedTransactions.size})"
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Content List
                Box(modifier = Modifier.weight(1f, fill = false).height(300.dp)) {
                    if (selectedTab == 0) {
                        if (archivedCustomers.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxWidth().height(200.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (isBn) "কোনো আর্কাইভ কাস্টমার নেই" else "No archived customers",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 14.sp
                                )
                            }
                        } else {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(archivedCustomers, key = { it.id }) { customer ->
                                    Card(
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = customer.name,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 15.sp
                                                )
                                                Text(
                                                    text = "${if (isBn) "বাকি:" else "Due:"} ${BakiStrings.formatMoney(customer.balance, isBn)}",
                                                    fontSize = 13.sp,
                                                    color = if (customer.balance > 0) BakiRed else JomaGreen
                                                )
                                            }

                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                // Restore
                                                IconButton(
                                                    onClick = { onRestoreCustomer(customer.id) },
                                                    modifier = Modifier.size(34.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Default.Restore,
                                                        contentDescription = "Restore",
                                                        tint = EmeraldPrimary
                                                    )
                                                }
                                                // Purge
                                                IconButton(
                                                    onClick = { itemToPurge = Pair(true, customer.id) },
                                                    modifier = Modifier.size(34.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Default.DeleteForever,
                                                        contentDescription = "Purge",
                                                        tint = MaterialTheme.colorScheme.error
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        if (archivedTransactions.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxWidth().height(200.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (isBn) "কোনো আর্কাইভ লেনদেন নেই" else "No archived transactions",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 14.sp
                                )
                            }
                        } else {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(archivedTransactions, key = { it.id }) { tx ->
                                    Card(
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = tx.customerName,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp
                                                )
                                                val typeLabel = when (tx.type) {
                                                    TransactionType.BAKI_SALE -> if (isBn) "বাকি" else "Credit"
                                                    TransactionType.REPAYMENT -> if (isBn) "জমা" else "Repayment"
                                                    TransactionType.CASH_SALE -> if (isBn) "নগদ" else "Cash"
                                                }
                                                Text(
                                                    text = "$typeLabel: ${BakiStrings.formatMoney(tx.amount, isBn)} • ${dateFormat.format(Date(tx.timestamp))}",
                                                    fontSize = 12.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }

                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                IconButton(
                                                    onClick = { onRestoreTransaction(tx.id) },
                                                    modifier = Modifier.size(34.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Default.Restore,
                                                        contentDescription = "Restore",
                                                        tint = EmeraldPrimary
                                                    )
                                                }
                                                IconButton(
                                                    onClick = { itemToPurge = Pair(false, tx.id) },
                                                    modifier = Modifier.size(34.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Default.DeleteForever,
                                                        contentDescription = "Purge",
                                                        tint = MaterialTheme.colorScheme.error
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Purge All / Close Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (archivedCustomers.isNotEmpty() || archivedTransactions.isNotEmpty()) {
                        OutlinedButton(
                            onClick = { showConfirmPurgeAll = true },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                            modifier = Modifier.weight(1f).height(44.dp)
                        ) {
                            Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isBn) "সব খালি করুন" else "Empty All", fontSize = 12.sp)
                        }
                    }

                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        modifier = Modifier.weight(1f).height(44.dp)
                    ) {
                        Text(if (isBn) "ঠিক আছে" else "Close", color = Color.White)
                    }
                }
            }
        }
    }

    // Confirmation Dialog for Single Purge
    itemToPurge?.let { (isCust, id) ->
        AlertDialog(
            onDismissRequest = { itemToPurge = null },
            title = { Text(if (isBn) "স্থায়ী মুছে ফেলার সতর্কতা" else "Permanent Purge Warning") },
            text = {
                Text(
                    if (isBn) "এটি স্থায়ীভাবে মুছে ফেলা হবে এবং আর ফিরিয়ে আনা সম্ভব হবে না। আপনি কি নিশ্চিত?"
                    else "This item will be permanently purged from the database and cannot be recovered. Are you sure?"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (isCust) onPurgeCustomer(id) else onPurgeTransaction(id)
                        itemToPurge = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(if (isBn) "স্থায়ীভাবে মুছুন" else "Purge Permanently", color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { itemToPurge = null }) {
                    Text(if (isBn) "বাতিল" else "Cancel")
                }
            }
        )
    }

    // Confirmation Dialog for Purge All
    if (showConfirmPurgeAll) {
        AlertDialog(
            onDismissRequest = { showConfirmPurgeAll = false },
            title = { Text(if (isBn) "আর্কাইভ খালি করার সতর্কতা" else "Empty Archive Confirmation") },
            text = {
                Text(
                    if (isBn) "আর্কাইভের সকল কাস্টমার ও লেনদেনের রেকর্ড স্থায়ীভাবে মুছে ফেলা হবে। এই কাজটির পর ডাটা ফেরত আনা যাবে না।"
                    else "All archived customer and transaction records will be permanently removed. This action cannot be undone."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmPurgeAll = false
                        onPurgeAll()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(if (isBn) "হ্যাঁ, সব খালি করুন" else "Yes, Empty Archive", color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showConfirmPurgeAll = false }) {
                    Text(if (isBn) "বাতিল" else "Cancel")
                }
            }
        )
    }
}
