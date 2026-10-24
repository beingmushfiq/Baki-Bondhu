package com.example.ui.screens

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.IconButton
import androidx.compose.ui.res.painterResource
import com.example.R
import com.example.data.model.CustomerEntity
import com.example.ui.BakiStrings
import com.example.ui.BakiUiState
import com.example.ui.theme.BakiRed
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldTertiary
import com.example.ui.theme.JomaGreen

@Composable
fun ReportsScreen(
    uiState: BakiUiState,
    onCustomerClick: (CustomerEntity) -> Unit,
    onSendSmsClick: (CustomerEntity) -> Unit,
    onSendWhatsAppClick: ((CustomerEntity) -> Unit)? = null
) {
    val isBn = uiState.isBangla
    val now = System.currentTimeMillis()
    val dayMs = 24 * 60 * 60 * 1000L

    // Categorize customers by due age
    val customersWithDue = remember(uiState.customers) {
        uiState.customers.filter { it.balance > 0 }
    }

    // Sort by oldest lastTransactionAt
    val oldestDues = remember(customersWithDue) {
        customersWithDue.sortedBy { it.lastTransactionAt }
    }

    val latePayers = remember(customersWithDue) {
        customersWithDue.filter { it.trustScore == 1 || (now - it.lastTransactionAt) > (14 * dayMs) }
    }

    val regularPayers = remember(uiState.customers) {
        uiState.customers.filter { it.trustScore >= 2 }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
        }

        // Summary Card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Text(
                        text = if (isBn) "বাকি ও তাগাদা পর্যালোচনা" else "Credit & Dues Review",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = if (isBn) "মোট বাকি হিসাব" else "Total Due Accounts",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            val count = customersWithDue.size
                            Text(
                                text = if (isBn) com.example.voice.BanglaVoiceParser.toBanglaDigits(count.toString()) else count.toString(),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Column {
                            Text(
                                text = if (isBn) "জরুরি তাগাদা বাকি" else "Urgent Dues (>14d)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            val lateCount = latePayers.size
                            Text(
                                text = if (isBn) com.example.voice.BanglaVoiceParser.toBanglaDigits(lateCount.toString()) else lateCount.toString(),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = BakiRed
                            )
                        }

                        Column {
                            Text(
                                text = if (isBn) "মোট টাকা বাকি" else "Total Amount Due",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = BakiStrings.formatMoney(uiState.totalBaki, isBn),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = BakiRed
                            )
                        }
                    }
                }
            }
        }

        // PRIORITY OLDEST DUES (Section 3 of PRD: "Review. A simple daily and weekly screen shows total baki, the oldest dues, and who usually pays late.")
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.HourglassBottom,
                    contentDescription = null,
                    tint = BakiRed,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isBn) "সবচেয়ে পুরোনো বকেয়া (জরুরি তাগাদা তালিকা)" else "Oldest Dues (Action Priority)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (oldestDues.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (isBn) "আলহামদুলিল্লাহ! কোনো পুরোনো বকেয়া নেই।" else "Great! No overdue accounts pending.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(20.dp),
                        color = JomaGreen,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        } else {
            items(oldestDues.take(8), key = { it.id }) { customer ->
                val daysAgo = ((now - customer.lastTransactionAt) / dayMs).coerceAtLeast(0)
                val daysAgoText = if (isBn) "${com.example.voice.BanglaVoiceParser.toBanglaDigits(daysAgo.toString())} দিন আগে" else "${daysAgo}d ago"

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onCustomerClick(customer) },
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
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
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.errorContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = customer.name.take(1),
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = customer.name,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = if (isBn) "বাকি বয়স: $daysAgoText" else "Due age: $daysAgoText",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = BakiRed,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = BakiStrings.formatMoney(customer.balance, isBn),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp,
                                color = BakiRed
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(
                                onClick = {
                                    if (onSendWhatsAppClick != null) {
                                        onSendWhatsAppClick(customer)
                                    } else {
                                        onSendSmsClick(customer)
                                    }
                                },
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF25D366).copy(alpha = 0.14f))
                                    .testTag("report_whatsapp_btn_${customer.id}")
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_whatsapp),
                                    contentDescription = "WhatsApp",
                                    tint = Color(0xFF25D366),
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Button(
                                onClick = { onSendSmsClick(customer) },
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                modifier = Modifier
                                    .height(38.dp)
                                    .testTag("report_sms_btn_${customer.id}")
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.Message,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = Color.White
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isBn) "তাগাদা" else "SMS",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }

        // CUSTOMER RELIABILITY BREAKDOWN
        item {
            Spacer(modifier = Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Speed,
                    contentDescription = null,
                    tint = EmeraldPrimary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isBn) "ক্রেতাদের পরিশোধের স্কোর (Reliability)" else "Customer Payment Reliability",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(JomaGreen))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(BakiStrings.trustGood(isBn), style = MaterialTheme.typography.bodyMedium)
                        }
                        val goodCount = uiState.customers.count { it.trustScore == 3 }
                        Text(
                            text = if (isBn) "${com.example.voice.BanglaVoiceParser.toBanglaDigits(goodCount.toString())} জন" else "$goodCount customers",
                            fontWeight = FontWeight.Bold,
                            color = JomaGreen
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(GoldTertiary))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(BakiStrings.trustMedium(isBn), style = MaterialTheme.typography.bodyMedium)
                        }
                        val medCount = uiState.customers.count { it.trustScore == 2 }
                        Text(
                            text = if (isBn) "${com.example.voice.BanglaVoiceParser.toBanglaDigits(medCount.toString())} জন" else "$medCount customers",
                            fontWeight = FontWeight.Bold,
                            color = GoldTertiary
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(BakiRed))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(BakiStrings.trustLow(isBn), style = MaterialTheme.typography.bodyMedium)
                        }
                        val lowCount = uiState.customers.count { it.trustScore == 1 }
                        Text(
                            text = if (isBn) "${com.example.voice.BanglaVoiceParser.toBanglaDigits(lowCount.toString())} জন" else "$lowCount customers",
                            fontWeight = FontWeight.Bold,
                            color = BakiRed
                        )
                    }
                }
            }
        }

        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
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
