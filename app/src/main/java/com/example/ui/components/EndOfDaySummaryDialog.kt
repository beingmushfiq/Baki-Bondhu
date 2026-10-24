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
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
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
import com.example.data.model.ShopSettingsEntity
import com.example.ui.BakiStrings
import com.example.ui.theme.BakiRed
import com.example.ui.theme.CashBlue
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldTertiary
import com.example.ui.theme.JomaGreen
import com.example.util.DailyCashFlowSummary

@Composable
fun EndOfDaySummaryDialog(
    summary: DailyCashFlowSummary,
    settings: ShopSettingsEntity,
    isBn: Boolean,
    onReconcile: () -> Unit,
    onShareReport: () -> Unit,
    onDismiss: () -> Unit
) {
    val scrollState = rememberScrollState()

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
                .testTag("end_of_day_summary_dialog"),
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(20.dp)
            ) {
                // Header: Title & Close Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(EmeraldPrimary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ReceiptLong,
                                contentDescription = null,
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = if (isBn) "দিনের ক্লোজিং হিসাব" else "End-of-Day Summary",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${summary.dateFormatted} • ${settings.shopName.ifBlank { if (isBn) "দোকানের হিসাব" else "Shop Ledger" }}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("closing_dialog_close_btn")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Hero Card: Net Cash In Drawer (Physical Till Verification)
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = EmeraldDark),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("cash_drawer_hero_card")
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        EmeraldDark,
                                        Color(0xFF064E3B)
                                    )
                                )
                            )
                            .padding(20.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isBn) "🏦 ক্যাশ ড্রয়ারের মোট নগদ" else "🏦 Net Cash in Drawer",
                                    color = Color.White.copy(alpha = 0.85f),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(GoldTertiary.copy(alpha = 0.25f))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = if (isBn) "আদায় + নগদ বিক্রি" else "Sales + Repayments",
                                        color = GoldTertiary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = BakiStrings.formatMoney(summary.netCashInDrawer, isBn),
                                color = Color.White,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.testTag("closing_net_cash_amount")
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = if (isBn) "দোকান বন্ধের সময় ক্যাশ বাক্সে এই পরিমাণ নগদ টাকা থাকা উচিত।" else "Physical till cash should match this amount upon closing.",
                                color = Color.White.copy(alpha = 0.75f),
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Breakdown Grid
                Text(
                    text = if (isBn) "দিনের আর্থিক বিবরণী (Breakdown):" else "Daily Breakdown:",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Metric Rows
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Repayments
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = JomaGreen.copy(alpha = 0.10f)),
                        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(JomaGreen.copy(alpha = 0.3f), JomaGreen.copy(alpha = 0.3f))))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = if (isBn) "আজকের আদায়" else "Repayments",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = BakiStrings.formatMoney(summary.totalRepayments, isBn),
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = JomaGreen
                            )
                        }
                    }

                    // Credit Sales Given
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = BakiRed.copy(alpha = 0.10f)),
                        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(BakiRed.copy(alpha = 0.3f), BakiRed.copy(alpha = 0.3f))))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = if (isBn) "আজকের বাকি দেওয়া" else "Credit Sales",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = BakiStrings.formatMoney(summary.totalCreditSales, isBn),
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = BakiRed
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Cash Sales
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = CashBlue.copy(alpha = 0.10f)),
                        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(CashBlue.copy(alpha = 0.3f), CashBlue.copy(alpha = 0.3f))))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = if (isBn) "নগদ বিক্রি" else "Cash Sales",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = BakiStrings.formatMoney(summary.totalCashSales, isBn),
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = CashBlue
                            )
                        }
                    }

                    // Net Credit Change
                    val isPositiveExpansion = summary.netCreditChange >= 0
                    val changeColor = if (isPositiveExpansion) BakiRed else JomaGreen
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = changeColor.copy(alpha = 0.10f)),
                        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(changeColor.copy(alpha = 0.3f), changeColor.copy(alpha = 0.3f))))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = if (isBn) {
                                    if (isPositiveExpansion) "বাকি বৃদ্ধি" else "বাকি হ্রাস"
                                } else {
                                    if (isPositiveExpansion) "Credit Incurred" else "Debt Reduced"
                                },
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = BakiStrings.formatMoney(Math.abs(summary.netCreditChange), isBn),
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = changeColor
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Stats footer: total transaction count
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isBn) "মোট সম্পন্ন লেনদেন সংখ্যা" else "Total Transactions Processed",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "${summary.transactionCount} টি",
                            fontWeight = FontWeight.Bold,
                            color = EmeraldPrimary,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Actions: Reconcile Cash Drawer & Share Report
                Button(
                    onClick = onReconcile,
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("closing_reconcile_btn")
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isBn) "ক্যাশ ড্রয়ার হিসাব নিশ্চিত করুন" else "Confirm & Reconcile Drawer",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = onShareReport,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("closing_share_btn")
                ) {
                    Icon(
                        Icons.Default.Share,
                        contentDescription = null,
                        tint = EmeraldPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isBn) "দৈনিক হিসাবের বিবরণী শেয়ার করুন" else "Share Daily Closing Statement",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = EmeraldPrimary
                    )
                }
            }
        }
    }
}
