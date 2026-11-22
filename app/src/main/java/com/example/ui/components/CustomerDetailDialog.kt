package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.res.painterResource
import com.example.R
import com.example.data.model.CustomerEntity
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.ui.BakiStrings
import com.example.ui.theme.BakiRed
import com.example.ui.theme.CashBlue
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldTertiary
import com.example.ui.theme.JomaGreen
import com.example.util.OverdueHelper
import com.example.util.OverdueLevel
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Add
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

@Composable
fun CustomerDetailDialog(
    customer: CustomerEntity,
    transactions: List<TransactionEntity>,
    isBn: Boolean,
    onSendSms: () -> Unit,
    onSendWhatsApp: (() -> Unit)? = null,
    onSettleBalance: () -> Unit,
    onAddTransaction: (() -> Unit)? = null,
    onShareStatement: (() -> Unit)? = null,
    onEditTransaction: ((TransactionEntity) -> Unit)? = null,
    onDeleteTransaction: ((TransactionEntity) -> Unit)? = null,
    onShareTransaction: ((TransactionEntity) -> Unit)? = null,
    onUpdateDueDate: ((Long?) -> Unit)? = null,
    onRenameCustomer: ((newName: String, newPhone: String) -> Unit)? = null,
    onDeleteCustomer: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val dateFormatter = SimpleDateFormat("dd MMMM yyyy, hh:mm a", Locale.getDefault())
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    // Chronologically sorted list (newest first)
    val chronologicalTx = remember(transactions, customer.id) {
        transactions
            .filter { it.customerId == customer.id }
            .sortedByDescending { it.timestamp }
    }

    // Cumulative stats
    val totalBakiTaken = remember(chronologicalTx) {
        chronologicalTx
            .filter { it.type == TransactionType.BAKI_SALE }
            .sumOf { it.amount }
    }

    val totalRepaid = remember(chronologicalTx) {
        chronologicalTx
            .filter { it.type == TransactionType.REPAYMENT }
            .sumOf { it.amount }
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
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.88f)
                .testTag("customer_detail_view_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // 1. HEADER ROW: Customer Info & Close Button
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
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = customer.name.take(1),
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = customer.name,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )

                            if (customer.phone.isNotBlank()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Phone,
                                        contentDescription = null,
                                        modifier = Modifier.size(13.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = customer.phone,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // Direct Call Button (if phone exists)
                    if (customer.phone.isNotBlank()) {
                        IconButton(
                            onClick = {
                                try {
                                    val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                                        data = Uri.parse("tel:${customer.phone}")
                                    }
                                    context.startActivity(dialIntent)
                                } catch (_: Exception) {}
                            },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Icon(
                                Icons.Default.Call,
                                contentDescription = "Call Customer",
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                    }

                    // Direct WhatsApp Reminder Quick Button (if phone exists and due > 0)
                    if (customer.phone.isNotBlank() && customer.balance > 0) {
                        IconButton(
                            onClick = {
                                if (onSendWhatsApp != null) {
                                    onSendWhatsApp()
                                } else {
                                    onSendSms()
                                }
                            },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF25D366).copy(alpha = 0.15f))
                                .testTag("detail_quick_whatsapp_button")
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_whatsapp),
                                contentDescription = "Send WhatsApp Reminder",
                                tint = Color(0xFF25D366),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                    }

                    // Edit customer name/phone button
                    if (onRenameCustomer != null) {
                        IconButton(
                            onClick = { showRenameDialog = true },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                                .testTag("detail_edit_customer_button")
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = if (isBn) "খরিদ্দার সম্পাদনা" else "Edit Customer",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                    }

                    // Delete (archive) customer button
                    if (onDeleteCustomer != null) {
                        IconButton(
                            onClick = { showDeleteConfirmDialog = true },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f))
                                .testTag("detail_delete_customer_button")
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = if (isBn) "খরিদ্দার মুছুন" else "Delete Customer",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 2. FINANCIAL SUMMARY & OUTSTANDING BALANCE HERO
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (customer.balance > 0) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)
                        else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = if (customer.balance > 0) {
                                        if (isBn) "বর্তমান মোট বকেয়া পাওনা" else "Current Outstanding Due"
                                    } else {
                                        if (isBn) "সম্পূর্ণ হিসাব পরিশোধিত" else "Account Fully Settled"
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                val trustText = when (customer.trustScore) {
                                    3 -> BakiStrings.trustGood(isBn)
                                    1 -> BakiStrings.trustLow(isBn)
                                    else -> BakiStrings.trustMedium(isBn)
                                }
                                val trustColor = when (customer.trustScore) {
                                    3 -> JomaGreen
                                    1 -> BakiRed
                                    else -> GoldTertiary
                                }
                                Text(
                                    text = "• $trustText",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = trustColor,
                                    fontWeight = FontWeight.Bold
                                )

                                val overdueInfo = OverdueHelper.computeCustomerOverdue(customer)
                                if (customer.balance > 0 && overdueInfo.level != OverdueLevel.NONE) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = overdueInfo.containerColor,
                                        contentColor = overdueInfo.contentColor,
                                        border = BorderStroke(1.dp, overdueInfo.borderColor),
                                        modifier = Modifier.testTag("detail_overdue_badge")
                                    ) {
                                        Text(
                                            text = if (isBn) overdueInfo.labelBn else overdueInfo.labelEn,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            Text(
                                text = BakiStrings.formatMoney(customer.balance, isBn),
                                fontSize = 28.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (customer.balance > 0) BakiRed else JomaGreen
                            )
                        }

                        // Promised Due Date Quick Selector
                        if (customer.balance > 0 && onUpdateDueDate != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = if (isBn) "পরিশোধের মেয়াদ:" else "Promised Due:",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                val now = System.currentTimeMillis()
                                val oneDay = TimeUnit.DAYS.toMillis(1)

                                Surface(
                                    onClick = { onUpdateDueDate(now + 7 * oneDay) },
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.testTag("due_date_7d")
                                ) {
                                    Text("+৭ দিন", fontSize = 10.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp))
                                }
                                Surface(
                                    onClick = { onUpdateDueDate(now + 15 * oneDay) },
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.testTag("due_date_15d")
                                ) {
                                    Text("+১৫ দিন", fontSize = 10.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp))
                                }
                                Surface(
                                    onClick = { onUpdateDueDate(now + 30 * oneDay) },
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.testTag("due_date_30d")
                                ) {
                                    Text("+৩০ দিন", fontSize = 10.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp))
                                }
                                if (customer.dueDate != null) {
                                    Surface(
                                        onClick = { onUpdateDueDate(null) },
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f),
                                        modifier = Modifier.testTag("due_date_clear")
                                    ) {
                                        Text("মুছুন", fontSize = 10.sp, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp))
                                    }
                                }
                            }
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 10.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )

                        // Cumulative stats row: Total Taken vs Total Paid
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = if (isBn) "মোট বাকি নেওয়া হয়েছে:" else "Total Credit Taken:",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = BakiStrings.formatMoney(totalBakiTaken, isBn),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = BakiRed
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = if (isBn) "মোট পরিশোধ করা হয়েছে:" else "Total Amount Repaid:",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = BakiStrings.formatMoney(totalRepaid, isBn),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = JomaGreen
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 3. ACTION BUTTONS (Add Transaction, Settle, WhatsApp & SMS Reminders)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    onAddTransaction?.let { onAdd ->
                        Button(
                            onClick = onAdd,
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("detail_add_transaction_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isBn) "+ হিসাব লিখুন" else "+ Add Entry",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (customer.balance > 0) {
                        Button(
                            onClick = onSettleBalance,
                            colors = ButtonDefaults.buttonColors(containerColor = JomaGreen),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("detail_settle_button")
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isBn) "পরিশোধ" else "Settle",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                if (customer.balance > 0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                if (onSendWhatsApp != null) {
                                    onSendWhatsApp()
                                } else {
                                    onSendSms()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF25D366),
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .weight(1.3f)
                                .height(46.dp)
                                .testTag("detail_send_whatsapp_button")
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_whatsapp),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isBn) "হোয়াটসঅ্যাপ তাগাদা" else "WhatsApp Reminder",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        OutlinedButton(
                            onClick = onSendSms,
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("detail_send_sms_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Message, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isBn) "তাগাদা SMS" else "SMS / Options",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // One-tap Digital WhatsApp / SMS Statement Sharing
                OutlinedButton(
                    onClick = { onShareStatement?.invoke() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .testTag("detail_share_statement_button")
                ) {
                    Icon(
                        Icons.Default.Share,
                        contentDescription = "Share Statement",
                        tint = EmeraldPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isBn) "রসিদ ও হিসাব শেয়ার (WhatsApp / SMS)" else "Share Statement (WhatsApp / SMS)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = EmeraldPrimary
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 4. CHRONOLOGICAL TRANSACTION LEDGER HISTORY
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.History,
                            contentDescription = null,
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isBn) "লেনদেনের পূর্ণাঙ্গ ইতিহাস (${chronologicalTx.size}টি)" else "Full Transaction Ledger (${chronologicalTx.size})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = if (isBn) "সর্বশেষ আগে" else "Newest First",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (chronologicalTx.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Receipt,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (isBn) "এখনও কোনো লেনদেন রেকর্ড নেই" else "No transactions on record yet",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(chronologicalTx, key = { it.id }) { tx ->
                            val color = when (tx.type) {
                                TransactionType.BAKI_SALE -> BakiRed
                                TransactionType.REPAYMENT -> JomaGreen
                                TransactionType.CASH_SALE -> CashBlue
                            }
                            val label = when (tx.type) {
                                TransactionType.BAKI_SALE -> BakiStrings.bakiSale(isBn)
                                TransactionType.REPAYMENT -> BakiStrings.repayment(isBn)
                                TransactionType.CASH_SALE -> BakiStrings.cashSale(isBn)
                            }
                            val sign = when (tx.type) {
                                TransactionType.BAKI_SALE -> "+ "
                                TransactionType.REPAYMENT -> "- "
                                TransactionType.CASH_SALE -> ""
                            }

                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .clip(CircleShape)
                                                    .background(color)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = label,
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = color
                                            )
                                        }

                                        Text(
                                            text = dateFormatter.format(Date(tx.timestamp)),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )

                                        if (!tx.originalSpokenText.isNullOrBlank()) {
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "🗣️ \"${tx.originalSpokenText}\"",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
                                            )
                                        }

                                        if (tx.note.isNotBlank() && tx.note != tx.originalSpokenText) {
                                            Text(
                                                text = "📝 ${tx.note}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                                            )
                                        }
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = "$sign${BakiStrings.formatMoney(tx.amount, isBn)}",
                                                fontWeight = FontWeight.ExtraBold,
                                                style = MaterialTheme.typography.titleMedium,
                                                color = color
                                            )
                                        }

                                        if (onShareTransaction != null) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            IconButton(
                                                onClick = { onShareTransaction(tx) },
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .testTag("customer_share_tx_${tx.id}")
                                            ) {
                                                Icon(
                                                    Icons.Default.Share,
                                                    contentDescription = "Share",
                                                    tint = EmeraldPrimary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }

                                        if (onEditTransaction != null) {
                                            IconButton(
                                                onClick = { onEditTransaction(tx) },
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .testTag("customer_edit_tx_${tx.id}")
                                            ) {
                                                Icon(
                                                    Icons.Default.Edit,
                                                    contentDescription = "Edit",
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }

                                        if (onDeleteTransaction != null) {
                                            IconButton(
                                                onClick = { onDeleteTransaction(tx) },
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .testTag("customer_delete_tx_${tx.id}")
                                            ) {
                                                Icon(
                                                    Icons.Default.Delete,
                                                    contentDescription = "Delete",
                                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Bottom Close Button
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                ) {
                    Text(if (isBn) "বন্ধ করুন" else "Close")
                }
            }
        }
    }

    // Rename customer dialog
    if (showRenameDialog && onRenameCustomer != null) {
        RenameCustomerDialog(
            currentName = customer.name,
            currentPhone = customer.phone,
            isBn = isBn,
            onConfirm = { newName, newPhone ->
                onRenameCustomer(newName, newPhone)
                showRenameDialog = false
            },
            onDismiss = { showRenameDialog = false }
        )
    }

    // Delete (archive) confirmation dialog
    if (showDeleteConfirmDialog && onDeleteCustomer != null) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = {
                Text(
                    text = if (isBn) "খরিদ্দার মুছবেন?" else "Delete Customer?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    if (customer.balance > 0) {
                        androidx.compose.material3.Surface(
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
                        ) {
                            Text(
                                text = if (isBn) "⚠️ এই খরিদ্দারের ৳ ${customer.balance.toLong()} বাকি রয়েছে।"
                                else "⚠️ This customer has ৳ ${customer.balance.toLong()} outstanding.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                modifier = androidx.compose.ui.Modifier.padding(8.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    Text(
                        text = if (isBn) "মুছলে '${customer.name}' আর্কাইভে যাবে। সেখান থেকে পুনরুদ্ধার করা যাবে।"
                        else "Deleting '${customer.name}' moves them to Archive. You can restore anytime.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            },
            confirmButton = {
                androidx.compose.material3.Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        onDeleteCustomer()
                        onDismiss()
                    },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(
                        text = if (isBn) "আর্কাইভে পাঠান" else "Archive & Delete",
                        color = androidx.compose.ui.graphics.Color.White
                    )
                }
            },
            dismissButton = {
                androidx.compose.material3.OutlinedButton(
                    onClick = { showDeleteConfirmDialog = false }
                ) {
                    Text(if (isBn) "বাতিল" else "Cancel")
                }
            }
        )
    }
}
