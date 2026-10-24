package com.example.ui.components

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.data.model.CustomerEntity
import com.example.data.model.ShopSettingsEntity
import com.example.ui.BakiStrings
import com.example.ui.SmsTone
import com.example.ui.theme.BakiRed
import com.example.ui.theme.EmeraldPrimary
import com.example.util.ShareChannel

@Composable
fun SmsReminderDialog(
    customer: CustomerEntity,
    settings: ShopSettingsEntity,
    tone: SmsTone,
    isBn: Boolean,
    onToneChanged: (SmsTone) -> Unit,
    onSendWhatsApp: (String) -> Unit = {},
    onSend: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var selectedChannel by remember { mutableStateOf(ShareChannel.WHATSAPP) }

    val whatsAppText = remember(customer, settings, tone, isBn) {
        BakiStrings.buildWhatsAppReminderText(
            customerName = customer.name,
            amount = customer.balance,
            shopName = settings.shopName,
            ownerName = settings.ownerName,
            bkashNumber = settings.bkashNumber,
            nagadNumber = settings.nagadNumber,
            lastTransactionAt = customer.lastTransactionAt,
            tone = tone,
            isBn = isBn
        )
    }

    val smsText = remember(customer, settings, tone, isBn) {
        BakiStrings.buildSmsText(
            customerName = customer.name,
            amount = customer.balance,
            shopName = settings.shopName,
            bkashNumber = settings.bkashNumber,
            nagadNumber = settings.nagadNumber,
            tone = tone,
            isBn = isBn
        )
    }

    val activeText = if (selectedChannel == ShareChannel.WHATSAPP) whatsAppText else smsText

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
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
                            text = if (isBn) "বাকি পরিশোধের তাগাদা" else "Payment Reminder",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "${customer.name} • ${customer.phone.ifBlank { if (isBn) "ফোন নম্বর নেই" else "No phone" }}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Due amount badge
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isBn) "মোট বকেয়া বিল:" else "Total Balance Due:",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = BakiStrings.formatMoney(customer.balance, isBn),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = BakiRed
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Channel Selector: WhatsApp vs SMS
                Text(
                    text = if (isBn) "তাগাদার মাধ্যম নির্বাচন করুন:" else "Select Channel:",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedChannel == ShareChannel.WHATSAPP,
                        onClick = { selectedChannel = ShareChannel.WHATSAPP },
                        label = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_whatsapp),
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = if (selectedChannel == ShareChannel.WHATSAPP) Color(0xFF25D366) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (isBn) "WhatsApp (দ্রুত তাগাদা)" else "WhatsApp (Fast)")
                            }
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF25D366).copy(alpha = 0.15f),
                            selectedLabelColor = Color(0xFF1B8A44)
                        ),
                        modifier = Modifier.weight(1.3f).testTag("channel_whatsapp_chip")
                    )

                    FilterChip(
                        selected = selectedChannel == ShareChannel.SMS,
                        onClick = { selectedChannel = ShareChannel.SMS },
                        label = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.AutoMirrored.Filled.Send,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = if (selectedChannel == ShareChannel.SMS) EmeraldPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (isBn) "সাধারণ SMS" else "SMS")
                            }
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = EmeraldPrimary.copy(alpha = 0.15f),
                            selectedLabelColor = EmeraldPrimary
                        ),
                        modifier = Modifier.weight(1f).testTag("channel_sms_chip")
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Tone selection
                Text(
                    text = if (isBn) "বার্তার ভাষা ও সুর নির্বাচন করুন:" else "Select Message Tone:",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = tone == SmsTone.GENTLE,
                        onClick = { onToneChanged(SmsTone.GENTLE) },
                        label = { Text(if (isBn) "নম্র / বন্ধুসুলভ" else "Gentle") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = tone == SmsTone.STANDARD,
                        onClick = { onToneChanged(SmsTone.STANDARD) },
                        label = { Text(if (isBn) "সাধারণ" else "Standard") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = tone == SmsTone.FIRM,
                        onClick = { onToneChanged(SmsTone.FIRM) },
                        label = { Text(if (isBn) "জরুরি" else "Urgent") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Live Message Preview Card
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (selectedChannel == ShareChannel.WHATSAPP) {
                            if (isBn) "WhatsApp বার্তার প্রিভিউ:" else "WhatsApp Preview:"
                        } else {
                            if (isBn) "SMS বার্তার প্রিভিউ:" else "SMS Preview:"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )

                    if (selectedChannel == ShareChannel.WHATSAPP) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF25D366).copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "WhatsApp Deep Link",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1B8A44),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reminder_preview_card"),
                    colors = CardDefaults.cardColors(
                        containerColor = if (selectedChannel == ShareChannel.WHATSAPP) {
                            Color(0xFFE7F9EE)
                        } else {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                        }
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = activeText,
                            style = MaterialTheme.typography.bodyMedium,
                            lineHeight = 20.sp,
                            color = Color(0xFF1E293B)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            OutlinedButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(activeText))
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Icon(
                                    Icons.Default.ContentCopy,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (isBn) "কপি করুন" else "Copy Text", fontSize = 11.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Actions: Cancel, Send WhatsApp, Send SMS
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(0.8f)
                            .height(48.dp)
                    ) {
                        Text(BakiStrings.cancel(isBn), fontSize = 12.sp)
                    }

                    // Direct WhatsApp Button (Instant Deep Link)
                    Button(
                        onClick = { onSendWhatsApp(whatsAppText) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1.5f)
                            .height(48.dp)
                            .testTag("send_whatsapp_button")
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_whatsapp),
                            contentDescription = "WhatsApp",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isBn) "WhatsApp এ পাঠান" else "WhatsApp",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    // Traditional SMS Button
                    OutlinedButton(
                        onClick = { onSend(smsText) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1.1f)
                            .height(48.dp)
                            .testTag("send_sms_submit_button")
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = null,
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isBn) "SMS" else "SMS",
                            color = EmeraldPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}
