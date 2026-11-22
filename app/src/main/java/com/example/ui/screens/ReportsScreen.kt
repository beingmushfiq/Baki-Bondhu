package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PriceCheck
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.CustomerEntity
import com.example.data.model.TransactionType
import com.example.ui.BakiStrings
import com.example.ui.BakiUiState
import com.example.ui.components.DevCenterPointSubtleFooter
import com.example.ui.theme.BakiRed
import com.example.ui.theme.CashBlue
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldTertiary
import com.example.ui.theme.JomaGreen
import com.example.util.DateRangeHelper
import com.example.util.Money
import com.example.voice.BanglaVoiceParser
import java.util.Calendar

@Composable
fun ReportsScreen(
    uiState: BakiUiState,
    onCustomerClick: (CustomerEntity) -> Unit,
    onSendSmsClick: (CustomerEntity) -> Unit,
    onSendWhatsAppClick: ((CustomerEntity) -> Unit)? = null,
    onOpenDailyClosing: (() -> Unit)? = null
) {
    val isBn = uiState.isBangla
    val now = System.currentTimeMillis()
    val dayMs = 24 * 60 * 60 * 1000L

    // Date Boundaries (Asia/Dhaka timezone)
    val startOfToday = remember(now) { DateRangeHelper.getStartOfDay(now) }
    val startOfWeekSat = remember(now) { DateRangeHelper.getStartOfWeek(now) }
    val startOfMonth = remember(now) {
        Calendar.getInstance(DateRangeHelper.DHAKA_TIMEZONE).apply {
            timeInMillis = now
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    // 11 Core Financial Metrics computed directly from Room ledger
    val allTx = uiState.allTransactionsUnfiltered

    // 1. Today's metrics
    val todayTx = remember(allTx, startOfToday) { allTx.filter { it.timestamp >= startOfToday } }
    val todayCreditSalesPaisa = remember(todayTx) {
        todayTx.filter { it.type == TransactionType.BAKI_SALE }.sumOf { it.amountPaisa }
    }
    val todayRepaymentsPaisa = remember(todayTx) {
        todayTx.filter { it.type == TransactionType.REPAYMENT }.sumOf { it.amountPaisa }
    }
    val todayCashSalesPaisa = remember(todayTx) {
        todayTx.filter { it.type == TransactionType.CASH_SALE }.sumOf { it.amountPaisa }
    }
    val todayNetCashDrawerPaisa = todayCashSalesPaisa + todayRepaymentsPaisa

    // 2. Saturday-aligned Weekly metrics (Traditional Bangladeshi Commercial Cycle)
    val weekTx = remember(allTx, startOfWeekSat) { allTx.filter { it.timestamp >= startOfWeekSat } }
    val weekCreditSalesPaisa = remember(weekTx) {
        weekTx.filter { it.type == TransactionType.BAKI_SALE }.sumOf { it.amountPaisa }
    }
    val weekRepaymentsPaisa = remember(weekTx) {
        weekTx.filter { it.type == TransactionType.REPAYMENT }.sumOf { it.amountPaisa }
    }

    // 3. Monthly metrics
    val monthTx = remember(allTx, startOfMonth) { allTx.filter { it.timestamp >= startOfMonth } }
    val monthCreditSalesPaisa = remember(monthTx) {
        monthTx.filter { it.type == TransactionType.BAKI_SALE }.sumOf { it.amountPaisa }
    }
    val monthRepaymentsPaisa = remember(monthTx) {
        monthTx.filter { it.type == TransactionType.REPAYMENT }.sumOf { it.amountPaisa }
    }

    // 4. Receivables & Aging metrics
    val customersWithDue = remember(uiState.customers) {
        uiState.customers.filter { it.balancePaisa > 0 }
    }
    val oldestDues = remember(customersWithDue) {
        customersWithDue.sortedBy { it.lastTransactionAt }
    }
    val oldestDueAgeDays = remember(oldestDues, now) {
        if (oldestDues.isNotEmpty()) {
            ((now - oldestDues.first().lastTransactionAt) / dayMs).coerceAtLeast(0)
        } else 0L
    }
    val latePayers = remember(customersWithDue, now) {
        customersWithDue.filter { it.trustScore == 1 || (now - it.lastTransactionAt) > (14 * dayMs) }
    }
    val settledCount = remember(uiState.customers) {
        uiState.customers.count { it.balancePaisa <= 0 }
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

        // 1. CASH DRAWER & DAILY CLOSING RECONCILIATION HERO CARD
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = EmeraldDark),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("reports_cash_drawer_hero_card"),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
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
                        .padding(18.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f, fill = false)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.AccountBalanceWallet,
                                        contentDescription = null,
                                        tint = GoldTertiary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f, fill = false)) {
                                    Text(
                                        text = if (isBn) "আজকের ক্যাশ ড্রয়ার স্থিতি" else "Today's Till Cash Flow",
                                        color = Color.White,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = if (isBn) "নগদ বিক্রি + আদায়কৃত টাকা" else "Cash Sales + Repayments Collected",
                                        color = Color.White.copy(alpha = 0.75f),
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            if (onOpenDailyClosing != null) {
                                OutlinedButton(
                                    onClick = onOpenDailyClosing,
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = Color.White.copy(alpha = 0.12f),
                                        contentColor = GoldTertiary
                                    ),
                                    border = BorderStroke(1.dp, GoldTertiary.copy(alpha = 0.7f)),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                    modifier = Modifier
                                        .height(34.dp)
                                        .wrapContentWidth()
                                        .testTag("reports_open_closing_btn")
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            Icons.AutoMirrored.Filled.ReceiptLong,
                                            contentDescription = null,
                                            tint = GoldTertiary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text(
                                            text = if (isBn) "ক্লোজিং" else "Closing",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            softWrap = false,
                                            color = GoldTertiary
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Column {
                                Text(
                                    text = if (isBn) "মোট ড্রয়ার নগদ" else "Net Cash In Drawer",
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = Money(todayNetCashDrawerPaisa).format(isBn),
                                    color = Color.White,
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = if (isBn) "মোট বকেয়া পাওনা" else "Total Debt Due",
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = Money.fromBdt(uiState.totalBaki).format(isBn),
                                    color = Color(0xFFFF8A80),
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. TODAY'S FINANCIAL BREAKDOWN (METRICS 1-4)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Today,
                        contentDescription = null,
                        tint = EmeraldPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isBn) "আজকের লেনদেন সারসংক্ষেপ" else "Today's Transaction Summary",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Metric 1: Today's Repayments
                    FinancialMetricCard(
                        title = if (isBn) "আজকের আদায়" else "Repayments",
                        amountText = Money(todayRepaymentsPaisa).format(isBn),
                        badgeText = if (isBn) "উসুল" else "Collected",
                        accentColor = JomaGreen,
                        icon = Icons.AutoMirrored.Filled.TrendingDown,
                        modifier = Modifier.weight(1f)
                    )

                    // Metric 2: Today's Credit Sales
                    FinancialMetricCard(
                        title = if (isBn) "আজকের বাকি বিক্রি" else "Credit Sales",
                        amountText = Money(todayCreditSalesPaisa).format(isBn),
                        badgeText = if (isBn) "বাকি" else "Due",
                        accentColor = BakiRed,
                        icon = Icons.AutoMirrored.Filled.TrendingUp,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Metric 3: Today's Cash Sales
                    FinancialMetricCard(
                        title = if (isBn) "আজকের নগদ বিক্রি" else "Cash Sales",
                        amountText = Money(todayCashSalesPaisa).format(isBn),
                        badgeText = if (isBn) "ক্যাশ" else "Cash",
                        accentColor = CashBlue,
                        icon = Icons.Default.Payments,
                        modifier = Modifier.weight(1f)
                    )

                    // Metric 4: Net Daily Balance Change
                    val netChangePaisa = todayCreditSalesPaisa - todayRepaymentsPaisa
                    val isExpansion = netChangePaisa >= 0L
                    FinancialMetricCard(
                        title = if (isBn) {
                            if (isExpansion) "বাকি বৃদ্ধি" else "বাকি হ্রাস"
                        } else {
                            if (isExpansion) "Debt Growth" else "Debt Reduced"
                        },
                        amountText = Money(Math.abs(netChangePaisa)).format(isBn),
                        badgeText = if (isExpansion) "+ বাকি" else "✓ আদায় বেশি",
                        accentColor = if (isExpansion) BakiRed else JomaGreen,
                        icon = if (isExpansion) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 3. BANGLADESHI COMMERCIAL CYCLE: SATURDAY-ALIGNED WEEKLY & MONTHLY METRICS (METRICS 5-8)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.DateRange,
                        contentDescription = null,
                        tint = EmeraldPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isBn) "সাপ্তাহিক ও মাসিক পরিক্রমা (শনিবার - শুক্রবার)" else "Weekly & Monthly Performance (Sat-Fri)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Weekly Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = if (isBn) "চলতি সপ্তাহ (শনিবার থেকে)" else "This Week (from Saturday)",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (isBn) "বাকি: ${Money(weekCreditSalesPaisa).format(isBn)} | আদায়: ${Money(weekRepaymentsPaisa).format(isBn)}"
                                    else "Credit: ${Money(weekCreditSalesPaisa).format(isBn)} | Repaid: ${Money(weekRepaymentsPaisa).format(isBn)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            val recoveryRate = if (weekCreditSalesPaisa > 0) {
                                ((weekRepaymentsPaisa.toDouble() / weekCreditSalesPaisa) * 100).toInt().coerceAtMost(999)
                            } else 100
                            val rateStr = if (isBn) "${BanglaVoiceParser.toBanglaDigits("$recoveryRate")}%" else "$recoveryRate%"

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (recoveryRate >= 70) JomaGreen.copy(alpha = 0.15f) else BakiRed.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = if (isBn) "আদায় হার $rateStr" else "$rateStr Recov.",
                                    color = if (recoveryRate >= 70) JomaGreen else BakiRed,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Surface(
                            modifier = Modifier.fillMaxWidth().height(1.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                        ) {}

                        // Monthly Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = if (isBn) "চলতি মাস (১ তারিখ থেকে)" else "This Month (Month-to-Date)",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (isBn) "বাকি: ${Money(monthCreditSalesPaisa).format(isBn)} | আদায়: ${Money(monthRepaymentsPaisa).format(isBn)}"
                                    else "Credit: ${Money(monthCreditSalesPaisa).format(isBn)} | Repaid: ${Money(monthRepaymentsPaisa).format(isBn)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            val netMonthPaisa = monthRepaymentsPaisa - monthCreditSalesPaisa
                            val isNetSurplus = netMonthPaisa >= 0
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isNetSurplus) JomaGreen.copy(alpha = 0.15f) else GoldTertiary.copy(alpha = 0.20f)
                            ) {
                                Text(
                                    text = if (isBn) {
                                        if (isNetSurplus) "উদ্বৃত্ত আদায়" else "বকেয়া বৃদ্ধি"
                                    } else {
                                        if (isNetSurplus) "Net Inflow" else "Net Credit"
                                    },
                                    color = if (isNetSurplus) JomaGreen else Color(0xFFB45309),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 4. OVERDUE & RISK REVIEW (METRICS 9-11)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.HourglassBottom,
                        contentDescription = null,
                        tint = BakiRed,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isBn) "জরুরি তাগাদা তালিকা (${latePayers.size} জন)" else "Urgent Action Dues (${latePayers.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (oldestDueAgeDays > 0) {
                    val ageStr = if (isBn) "${BanglaVoiceParser.toBanglaDigits("$oldestDueAgeDays")} দিন" else "${oldestDueAgeDays}d"
                    Text(
                        text = if (isBn) "সর্বোচ্চ বয়স: $ageStr" else "Max age: $ageStr",
                        style = MaterialTheme.typography.labelSmall,
                        color = BakiRed,
                        fontWeight = FontWeight.SemiBold
                    )
                }
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
            itemsIndexed(oldestDues.take(8), key = { _, customer -> customer.id }) { index, customer ->
                val daysAgo = ((now - customer.lastTransactionAt) / dayMs).coerceAtLeast(0)
                val daysAgoText = if (isBn) "${BanglaVoiceParser.toBanglaDigits(daysAgo.toString())} দিন আগে" else "${daysAgo}d ago"
                val serialNum = String.format("%02d", index + 1)
                val serialText = if (isBn) "${BanglaVoiceParser.toBanglaDigits(serialNum)}." else "$serialNum."

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
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left: Serial + Avatar + Name + Age
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = serialText,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = BakiRed.copy(alpha = 0.85f),
                                modifier = Modifier.padding(end = 6.dp)
                            )
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
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
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f, fill = false)) {
                                Text(
                                    text = customer.name,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = if (isBn) "বাকি বয়স: $daysAgoText" else "Due age: $daysAgoText",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = BakiRed,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Right: Amount + WhatsApp + Tagada
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = BakiStrings.formatMoney(customer.balance, isBn),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp,
                                color = BakiRed
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            IconButton(
                                onClick = {
                                    if (onSendWhatsAppClick != null) {
                                        onSendWhatsAppClick(customer)
                                    } else {
                                        onSendSmsClick(customer)
                                    }
                                },
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF25D366).copy(alpha = 0.14f))
                                    .testTag("report_whatsapp_btn_${customer.id}")
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_whatsapp),
                                    contentDescription = "WhatsApp",
                                    tint = Color(0xFF25D366),
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Button(
                                onClick = { onSendSmsClick(customer) },
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                modifier = Modifier
                                    .height(34.dp)
                                    .testTag("report_sms_btn_${customer.id}")
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.Message,
                                    contentDescription = null,
                                    modifier = Modifier.size(13.dp),
                                    tint = Color.White
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = if (isBn) "তাগাদা" else "SMS",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }

        // 5. CUSTOMER RELIABILITY BREAKDOWN
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Speed,
                    contentDescription = null,
                    tint = EmeraldPrimary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isBn) "ক্রেতাদের পরিশোধের আস্থা স্কোর (Reliability)" else "Customer Payment Reliability",
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
                            text = if (isBn) "${BanglaVoiceParser.toBanglaDigits(goodCount.toString())} জন" else "$goodCount customers",
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
                            text = if (isBn) "${BanglaVoiceParser.toBanglaDigits(medCount.toString())} জন" else "$medCount customers",
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
                            text = if (isBn) "${BanglaVoiceParser.toBanglaDigits(lowCount.toString())} জন" else "$lowCount customers",
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
                DevCenterPointSubtleFooter()
            }
        }

        item {
            Spacer(modifier = Modifier.height(90.dp))
        }
    }
}

@Composable
private fun FinancialMetricCard(
    title: String,
    amountText: String,
    badgeText: String,
    accentColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.25f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
                )
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = accentColor.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = badgeText,
                        color = accentColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = title,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = amountText,
                fontWeight = FontWeight.Black,
                fontSize = 16.sp,
                color = accentColor,
                maxLines = 1
            )
        }
    }
}
