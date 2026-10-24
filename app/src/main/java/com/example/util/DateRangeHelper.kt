package com.example.util

import com.example.data.model.Transaction
import com.example.data.model.TransactionType
import com.example.ui.BakiStrings
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

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
                    val sdf = SimpleDateFormat("dd MMM", Locale.getDefault())
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
    val totalCreditSales: Double = 0.0,
    val totalRepayments: Double = 0.0,
    val totalCashSales: Double = 0.0,
    val netCashInDrawer: Double = 0.0, // totalCashSales + totalRepayments
    val netCreditChange: Double = 0.0, // totalCreditSales - totalRepayments
    val transactionCount: Int = 0,
    val dateFormatted: String = ""
)

object DateRangeHelper {

    fun getStartOfDay(timestamp: Long = System.currentTimeMillis()): Long {
        return Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    fun getEndOfDay(timestamp: Long = System.currentTimeMillis()): Long {
        return Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis
    }

    fun getStartOfWeek(timestamp: Long = System.currentTimeMillis()): Long {
        return Calendar.getInstance().apply {
            timeInMillis = timestamp
            // Set to beginning of current week (Saturday or Sunday based on locale, standard firstDayOfWeek)
            set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    fun getEndOfWeek(timestamp: Long = System.currentTimeMillis()): Long {
        return Calendar.getInstance().apply {
            timeInMillis = getStartOfWeek(timestamp)
            add(Calendar.DAY_OF_WEEK, 6)
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis
    }

    /**
     * Filters a list of transactions based on the selected DateRangeSelection.
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
     * Computes Net Daily Cash Flow summary across the given transactions.
     */
    fun calculateCashFlowSummary(
        transactions: List<Transaction>,
        dayTimestamp: Long = System.currentTimeMillis(),
        isBn: Boolean = true
    ): DailyCashFlowSummary {
        val startOfDay = getStartOfDay(dayTimestamp)
        val endOfDay = getEndOfDay(dayTimestamp)
        val todayTxs = transactions.filter { it.timestamp in startOfDay..endOfDay }

        var creditSales = 0.0
        var repayments = 0.0
        var cashSales = 0.0

        for (tx in todayTxs) {
            when (tx.type) {
                TransactionType.BAKI_SALE -> creditSales += tx.amount
                TransactionType.REPAYMENT -> repayments += tx.amount
                TransactionType.CASH_SALE -> cashSales += tx.amount
            }
        }

        val netCash = cashSales + repayments
        val netCredit = creditSales - repayments

        val sdf = SimpleDateFormat(if (isBn) "dd MMMM yyyy" else "dd MMM yyyy", Locale.getDefault())
        val dateStr = sdf.format(Date(dayTimestamp))

        return DailyCashFlowSummary(
            totalCreditSales = creditSales,
            totalRepayments = repayments,
            totalCashSales = cashSales,
            netCashInDrawer = netCash,
            netCreditChange = netCredit,
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
            appendLine("💵 ${if (isBn) "আজকের নগদ বিক্রি (Cash Sales)" else "Today's Cash Sales"}: *${BakiStrings.formatMoney(summary.totalCashSales, isBn)}*")
            appendLine("📥 ${if (isBn) "আজকের বাকি আদায় (Repayments)" else "Repayments Collected"}: *${BakiStrings.formatMoney(summary.totalRepayments, isBn)}*")
            appendLine("📤 ${if (isBn) "আজকের বাকি দেওয়া (Credit Given)" else "Credit Sales Given"}: *${BakiStrings.formatMoney(summary.totalCreditSales, isBn)}*")
            appendLine("──────────────────")
            appendLine("🏦 *${if (isBn) "ক্যাশ ড্রয়ারে মোট নগদ স্থিতি" else "Net Cash In Drawer"}: ${BakiStrings.formatMoney(summary.netCashInDrawer, isBn)}*")
            val creditChangeLabel = if (summary.netCreditChange >= 0) {
                if (isBn) "বাকি বৃদ্ধি" else "Net Credit Increase"
            } else {
                if (isBn) "বাকি হ্রাস (আদায় বেশি)" else "Net Credit Reduction"
            }
            appendLine("📈 *$creditChangeLabel: ${BakiStrings.formatMoney(Math.abs(summary.netCreditChange), isBn)}*")
            appendLine("📝 ${if (isBn) "মোট লেনদেন সংখ্যা" else "Total Transactions"}: ${summary.transactionCount}")
            appendLine("──────────────────")
            appendLine(if (isBn) "⚡ কারিগরি সহায়তায়: DevCenterPoint (devcenterpoint.com)" else "⚡ Powered by DevCenterPoint (devcenterpoint.com)")
        }
    }
}
