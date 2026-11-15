package com.example.util

import com.example.data.model.Transaction
import com.example.data.model.TransactionType
import com.example.ui.BakiStrings
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs

enum class DateRangeFilterType {
    ALL,
    TODAY,
    THIS_WEEK,
    CUSTOM
}

data class DateRangeSelection(
    val type: DateRangeFilterType = DateRangeFilterType.ALL,
    val startTimestamp: Long? = null,
    val endTimestamp: Long? = null
) {
    fun getDisplayLabel(isBn: Boolean): String {
        return when (type) {
            DateRangeFilterType.ALL -> if (isBn) "সব লেনদেন" else "All Transactions"
            DateRangeFilterType.TODAY -> if (isBn) "আজকের লেনদেন" else "Today's History"
            DateRangeFilterType.THIS_WEEK -> if (isBn) "এই সপ্তাহের" else "This Week"
            DateRangeFilterType.CUSTOM -> {
                if (startTimestamp != null && endTimestamp != null) {
                    val sdf = SimpleDateFormat("dd MMM", Locale.getDefault()).apply {
                        timeZone = TimeZone.getTimeZone("Asia/Dhaka")
                    }
                    val startStr = sdf.format(Date(startTimestamp))
                    val endStr = sdf.format(Date(endTimestamp))
                    "$startStr - $endStr"
                } else {
                    if (isBn) "নির্দিষ্ট তারিখ" else "Custom Date"
                }
            }
        }
    }
}

data class DailyCashFlowSummary(
    val totalCreditSalesPaisa: Long = 0L,
    val totalRepaymentsPaisa: Long = 0L,
    val totalCashSalesPaisa: Long = 0L,
    val transactionCount: Int = 0,
    val dateFormatted: String = ""
) {
    // Backward-compatible Double accessors
    val totalCreditSales: Double get() = totalCreditSalesPaisa / 100.0
    val totalRepayments: Double get() = totalRepaymentsPaisa / 100.0
    val totalCashSales: Double get() = totalCashSalesPaisa / 100.0
    val netCashInDrawer: Double get() = (totalCashSalesPaisa + totalRepaymentsPaisa) / 100.0
    val netCreditChange: Double get() = (totalCreditSalesPaisa - totalRepaymentsPaisa) / 100.0

    val netCashInDrawerMoney: Money get() = Money(totalCashSalesPaisa + totalRepaymentsPaisa)
    val netCreditChangeMoney: Money get() = Money(totalCreditSalesPaisa - totalRepaymentsPaisa)
}

object DateRangeHelper {

    val DHAKA_TIMEZONE: TimeZone = TimeZone.getTimeZone("Asia/Dhaka")

    fun getStartOfDay(timestamp: Long = System.currentTimeMillis()): Long {
        return Calendar.getInstance(DHAKA_TIMEZONE).apply {
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    fun getEndOfDay(timestamp: Long = System.currentTimeMillis()): Long {
        return Calendar.getInstance(DHAKA_TIMEZONE).apply {
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis
    }

    /**
     * Calculates the start of week based on Saturday for Bangladeshi business practices.
     */
    fun getStartOfWeek(timestamp: Long = System.currentTimeMillis()): Long {
        return Calendar.getInstance(DHAKA_TIMEZONE).apply {
            timeInMillis = timestamp
            firstDayOfWeek = Calendar.SATURDAY
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            // Adjust to the most recent Saturday
            val currentDay = get(Calendar.DAY_OF_WEEK)
            val daysBack = if (currentDay == Calendar.SATURDAY) {
                0
            } else {
                (currentDay + 1) % 7
            }
            add(Calendar.DAY_OF_MONTH, -daysBack)
        }.timeInMillis
    }

    fun getEndOfWeek(timestamp: Long = System.currentTimeMillis()): Long {
        return Calendar.getInstance(DHAKA_TIMEZONE).apply {
            timeInMillis = getStartOfWeek(timestamp)
            add(Calendar.DAY_OF_MONTH, 6)
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis
    }

    /**
     * Filters a list of transactions based on the selected DateRangeSelection using half-open intervals.
     */
    fun filterTransactions(
        transactions: List<Transaction>,
        selection: DateRangeSelection
    ): List<Transaction> {
        val now = System.currentTimeMillis()
        return when (selection.type) {
            DateRangeFilterType.ALL -> transactions
            DateRangeFilterType.TODAY -> {
                val start = getStartOfDay(now)
                val end = getEndOfDay(now)
                transactions.filter { it.timestamp in start..end }
            }
            DateRangeFilterType.THIS_WEEK -> {
                val start = getStartOfWeek(now)
                val end = getEndOfWeek(now)
                transactions.filter { it.timestamp in start..end }
            }
            DateRangeFilterType.CUSTOM -> {
                val start = selection.startTimestamp ?: getStartOfDay(now)
                val end = selection.endTimestamp ?: getEndOfDay(now)
                transactions.filter { it.timestamp in start..end }
            }
        }
    }

    /**
     * Computes Net Daily Cash Flow summary across the given transactions using exact Paisa arithmetic.
     */
    fun calculateCashFlowSummary(
        transactions: List<Transaction>,
        dayTimestamp: Long = System.currentTimeMillis(),
        isBn: Boolean = true
    ): DailyCashFlowSummary {
        val startOfDay = getStartOfDay(dayTimestamp)
        val endOfDay = getEndOfDay(dayTimestamp)
        val todayTxs = transactions.filter { it.timestamp in startOfDay..endOfDay }

        var creditSalesPaisa = 0L
        var repaymentsPaisa = 0L
        var cashSalesPaisa = 0L

        for (tx in todayTxs) {
            when (tx.type) {
                TransactionType.BAKI_SALE -> creditSalesPaisa += tx.amountPaisa
                TransactionType.REPAYMENT -> repaymentsPaisa += tx.amountPaisa
                TransactionType.CASH_SALE -> cashSalesPaisa += tx.amountPaisa
            }
        }

        val sdf = SimpleDateFormat(if (isBn) "dd MMMM yyyy" else "dd MMM yyyy", Locale.getDefault()).apply {
            timeZone = DHAKA_TIMEZONE
        }
        val dateStr = sdf.format(Date(dayTimestamp))

        return DailyCashFlowSummary(
            totalCreditSalesPaisa = creditSalesPaisa,
            totalRepaymentsPaisa = repaymentsPaisa,
            totalCashSalesPaisa = cashSalesPaisa,
            transactionCount = todayTxs.size,
            dateFormatted = dateStr
        )
    }

    /**
     * Formats an end-of-day closing summary for digital receipt sharing.
     */
    fun formatClosingStatement(
        summary: DailyCashFlowSummary,
        shopName: String,
        ownerName: String,
        isBn: Boolean = true
    ): String {
        return buildString {
            appendLine("📊 *${if (isBn) "দিনের ক্লোজিং হিসাব ও ক্যাশ রিকনসিলিয়েশন" else "DAILY CLOSING & CASH RECONCILIATION"}*")
            appendLine("🏪 *${shopName.ifBlank { if (isBn) "দোকানের বাকি খাতা" else "Shop Credit Ledger" }}*")
            if (ownerName.isNotBlank()) {
                appendLine("${if (isBn) "স্বত্বাধিকারী" else "Proprietor"}: $ownerName")
            }
            appendLine("📅 ${if (isBn) "তারিখ" else "Date"}: ${summary.dateFormatted}")
            appendLine("──────────────────")
            appendLine("💵 ${if (isBn) "আজকের নগদ বিক্রি (Cash Sales)" else "Today's Cash Sales"}: *${Money(summary.totalCashSalesPaisa).format(isBn)}*")
            appendLine("📥 ${if (isBn) "আজকের বাকি আদায় (Repayments)" else "Repayments Collected"}: *${Money(summary.totalRepaymentsPaisa).format(isBn)}*")
            appendLine("📤 ${if (isBn) "আজকের বাকি দেওয়া (Credit Given)" else "Credit Sales Given"}: *${Money(summary.totalCreditSalesPaisa).format(isBn)}*")
            appendLine("──────────────────")
            appendLine("🏦 *${if (isBn) "ক্যাশ ড্রয়ারে মোট নগদ স্থিতি" else "Net Cash In Drawer"}: ${summary.netCashInDrawerMoney.format(isBn)}*")
            val netChangePaisa = summary.totalCreditSalesPaisa - summary.totalRepaymentsPaisa
            val creditChangeLabel = if (netChangePaisa >= 0L) {
                if (isBn) "বাকি বৃদ্ধি" else "Net Credit Increase"
            } else {
                if (isBn) "বাকি হ্রাস (আদায় বেশি)" else "Net Credit Reduction"
            }
            appendLine("📈 *$creditChangeLabel: ${Money(abs(netChangePaisa)).format(isBn)}*")
            appendLine("📝 ${if (isBn) "মোট লেনদেন সংখ্যা" else "Total Transactions"}: ${summary.transactionCount}")
            appendLine("──────────────────")
            appendLine(if (isBn) "⚡ কারিগরি সহায়তায়: DevCenterPoint (devcenterpoint.com)" else "⚡ Powered by DevCenterPoint (devcenterpoint.com)")
        }
    }
}
