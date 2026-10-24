package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.CustomerEntity
import com.example.data.model.ShopSettingsEntity
import com.example.data.model.Transaction
import com.example.data.model.TransactionType
import com.example.ui.BakiStrings
import com.example.ui.theme.BakiRed
import com.example.ui.theme.CashBlue
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldTertiary
import com.example.ui.theme.JomaGreen
import com.example.util.DigitalReceiptHelper
import com.example.util.ShareChannel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DigitalReceiptDialog(
    transaction: Transaction,
    customer: CustomerEntity? = null,
    settings: ShopSettingsEntity,
    isBn: Boolean,
    onShare: (ShareChannel) -> Unit,
    onCopy: () -> Unit,
    onDismiss: () -> Unit
) {
    val scrollState = rememberScrollState()
    val dateFormatter = remember {
        SimpleDateFormat(
            if (isBn) "dd MMMM yyyy, hh:mm a" else "dd MMM yyyy, hh:mm a",
            Locale.getDefault()
        )
    }

    val typeColor = when (transaction.type) {
        TransactionType.BAKI_SALE -> BakiRed
        TransactionType.REPAYMENT -> JomaGreen
        TransactionType.CASH_SALE -> CashBlue
    }

    val typeTitle = when (transaction.type) {
        TransactionType.BAKI_SALE -> if (isBn) "বাকি বিক্রি" else "Credit Sale"
        TransactionType.REPAYMENT -> if (isBn) "জমা / পরিশোধ" else "Payment Received"
        TransactionType.CASH_SALE -> if (isBn) "নগদ বিক্রি" else "Cash Sale"
    }

    val shopTitle = settings.shopName.ifBlank {
        if (isBn) "দোকানের বাকি খাতা" else "Shop Credit Ledger"
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 24.dp)
                .testTag("digital_receipt_dialog"),
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(20.dp)
            ) {
                // Top Header: Title & Close
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
                                Icons.Default.Receipt,
                                contentDescription = null,
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (isBn) "ডিজিটাল রসিদ" else "Digital Receipt",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isBn) "খরিদ্দারের সাথে শেয়ার করুন" else "Share with customer",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("receipt_close_btn")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // PHYSICAL PAPER-RECEIPT STYLE CARD
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(18.dp)
                        )
                        .testTag("digital_receipt_preview_card")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        // Shop Header Banner
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(
                                            Color(0xFF00382E),
                                            Color(0xFF005A4B)
                                        )
                                    )
                                )
                                .padding(vertical = 12.dp, horizontal = 14.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = shopTitle,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp
                                )
                                if (settings.ownerName.isNotBlank()) {
                                    Text(
                                        text = "${if (isBn) "স্বত্বাধিকারী" else "Proprietor"}: ${settings.ownerName}",
                                        color = Color(0xFFFFD54F),
                                        fontSize = 11.sp
                                    )
                                }
                                Text(
                                    text = dateFormatter.format(Date(transaction.timestamp)),
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 10.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Customer Details
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Person,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = transaction.customerName,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    val phone = customer?.phone?.ifBlank { null }
                                    if (!phone.isNullOrBlank()) {
                                        Text(
                                            text = phone,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            // Type badge
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(typeColor.copy(alpha = 0.15f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = typeTitle,
                                    color = typeColor,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                        )

                        // Amount Big Typography
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isBn) "লেনদেনের পরিমাণ:" else "Transaction Amount:",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = BakiStrings.formatMoney(transaction.amount, isBn),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black,
                                color = typeColor
                            )
                        }

                        if (transaction.note.isNotBlank() && transaction.note != transaction.originalSpokenText) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "📝 ${if (isBn) "নোট" else "Note"}: ${transaction.note}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (!transaction.originalSpokenText.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "🗣️ \"${transaction.originalSpokenText}\"",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                        }

                        // Current Total Customer Debt Balance
                        if (customer != null && customer.balance > 0) {
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 10.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isBn) "বর্তমান মোট বকেয়া পাওনা:" else "Total Balance Due:",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = BakiStrings.formatMoney(customer.balance, isBn),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BakiRed
                                )
                            }
                        }

                        // bKash / Nagad payment accounts
                        if (settings.bkashNumber.isNotBlank() || settings.nagadNumber.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surface)
                                    .padding(8.dp)
                            ) {
                                Column {
                                    Text(
                                        text = if (isBn) "📲 ডিজিটাল পেমেন্ট নম্বর:" else "📲 Payment Accounts:",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = EmeraldPrimary
                                    )
                                    if (settings.bkashNumber.isNotBlank()) {
                                        Text(
                                            text = "bKash: ${settings.bkashNumber}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    if (settings.nagadNumber.isNotBlank()) {
                                        Text(
                                            text = "Nagad: ${settings.nagadNumber}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "⚡ Powered by DevCenterPoint • devcenterpoint.com",
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // DIRECT SHARING BUTTONS
                Text(
                    text = if (isBn) "রসিদ পাঠানোর মাধ্যম নির্বাচন করুন:" else "Select Share Channel:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(10.dp))

                // 1. WhatsApp Button
                Button(
                    onClick = { onShare(ShareChannel.WHATSAPP) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF25D366),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("receipt_share_whatsapp_btn")
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Message,
                        contentDescription = "WhatsApp",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isBn) "🟢 WhatsApp-এ রসিদ পাঠান" else "Send via WhatsApp",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 2. SMS Button
                OutlinedButton(
                    onClick = { onShare(ShareChannel.SMS) },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("receipt_share_sms_btn")
                ) {
                    Icon(
                        Icons.Default.Phone,
                        contentDescription = "SMS",
                        tint = EmeraldPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isBn) "💬 SMS মাধ্যমে পাঠান" else "Send via SMS",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = EmeraldPrimary
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 3. Android Universal Share & Copy Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { onShare(ShareChannel.CHOOSER) },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1.3f)
                            .height(46.dp)
                            .testTag("receipt_share_all_btn")
                    ) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = "All Apps",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isBn) "অন্যান্য অ্যাপ" else "All Apps",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    OutlinedButton(
                        onClick = onCopy,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .testTag("receipt_copy_btn")
                    ) {
                        Icon(
                            Icons.Default.ContentCopy,
                            contentDescription = "Copy Text",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isBn) "কপি" else "Copy",
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}
