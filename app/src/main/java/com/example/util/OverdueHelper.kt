package com.example.util

import androidx.compose.ui.graphics.Color
import com.example.data.model.Customer
import com.example.data.model.Transaction
import com.example.data.model.TransactionType
import com.example.voice.BanglaVoiceParser
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

enum class OverdueLevel {
    NONE,
    DUE_SOON,
    OVERDUE,
    CRITICAL
}

data class OverdueBadgeInfo(
    val level: OverdueLevel,
    val labelBn: String,
    val labelEn: String,
    val containerColor: Color,
    val contentColor: Color,
    val borderColor: Color,
    val daysDifference: Int? = null,
    val formattedDueDate: String? = null
)

object OverdueHelper {

    private val ONE_DAY_MS = TimeUnit.DAYS.toMillis(1)

    /**
     * Calculates overdue level for a customer based on their balance, promised due date,
     * or aging of debt since last transaction.
     */
    fun computeCustomerOverdue(
        customer: Customer,
        currentTime: Long = System.currentTimeMillis()
    ): OverdueBadgeInfo {
        if (customer.balance <= 0.0) {
            return OverdueBadgeInfo(
                level = OverdueLevel.NONE,
                labelBn = "পরিশোধিত",
                labelEn = "Settled",
                containerColor = Color(0xFFE8F5E9),
                contentColor = Color(0xFF2E7D32),
                borderColor = Color(0xFFA5D6A7)
            )
        }

        // Case A: Customer has explicit promised due date
        if (customer.dueDate != null && customer.dueDate > 0L) {
            val diffMs = customer.dueDate - currentTime
            val diffDays = (diffMs / ONE_DAY_MS).toInt()
            val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            val formattedDate = dateFormat.format(Date(customer.dueDate))
            val formattedDateBn = BanglaVoiceParser.toBanglaDigits(formattedDate)

            return when {
                diffMs < -7 * ONE_DAY_MS -> {
                    // Past due by more than 7 days
                    val overdueDays = -diffDays
                    val overdueDaysBn = BanglaVoiceParser.toBanglaDigits(overdueDays.toString())
                    OverdueBadgeInfo(
                        level = OverdueLevel.CRITICAL,
                        labelBn = "জরুরি তাগাদা (${overdueDaysBn} দিন অতিবাহিত)",
                        labelEn = "Critical (${overdueDays}d overdue)",
                        containerColor = Color(0xFFFFEBEE),
                        contentColor = Color(0xFFC62828),
                        borderColor = Color(0xFFEF9A9A),
                        daysDifference = overdueDays,
                        formattedDueDate = formattedDate
                    )
                }
                diffMs < 0 -> {
                    // Past due by 1 to 7 days
                    val overdueDays = -diffDays.coerceAtLeast(1)
                    val overdueDaysBn = BanglaVoiceParser.toBanglaDigits(overdueDays.toString())
                    OverdueBadgeInfo(
                        level = OverdueLevel.OVERDUE,
                        labelBn = "মেয়াদোত্তীর্ণ (${overdueDaysBn} দিন)",
                        labelEn = "Overdue (${overdueDays}d)",
                        containerColor = Color(0xFFFFF3E0),
                        contentColor = Color(0xFFE65100),
                        borderColor = Color(0xFFFFCC80),
                        daysDifference = overdueDays,
                        formattedDueDate = formattedDate
                    )
                }
                diffMs <= 3 * ONE_DAY_MS -> {
                    // Due within 3 days
                    val remainingDays = diffDays.coerceAtLeast(0)
                    val remainingDaysBn = BanglaVoiceParser.toBanglaDigits(remainingDays.toString())
                    OverdueBadgeInfo(
                        level = OverdueLevel.DUE_SOON,
                        labelBn = if (remainingDays == 0) "আজই পরিশোধের দিন" else "${remainingDaysBn} দিনে পরিশোধ",
                        labelEn = if (remainingDays == 0) "Due Today" else "Due in ${remainingDays}d",
                        containerColor = Color(0xFFFFFDE7),
                        contentColor = Color(0xFFF57F17),
                        borderColor = Color(0xFFFFF59D),
                        daysDifference = remainingDays,
                        formattedDueDate = formattedDate
                    )
                }
                else -> {
                    // Due in the future (> 3 days)
                    OverdueBadgeInfo(
                        level = OverdueLevel.NONE,
                        labelBn = "মেয়াদ: $formattedDateBn",
                        labelEn = "Due: $formattedDate",
                        containerColor = Color(0xFFE0F2F1),
                        contentColor = Color(0xFF00695C),
                        borderColor = Color(0xFF80CBC4),
                        daysDifference = diffDays,
                        formattedDueDate = formattedDate
                    )
                }
            }
        }

        // Case B: No explicit due date, compute debt aging from last transaction
        val debtAgeMs = currentTime - customer.lastTransactionAt
        val debtAgeDays = (debtAgeMs / ONE_DAY_MS).toInt().coerceAtLeast(0)
        val debtAgeDaysBn = BanglaVoiceParser.toBanglaDigits(debtAgeDays.toString())

        return when {
            debtAgeDays >= 30 -> {
                OverdueBadgeInfo(
                    level = OverdueLevel.CRITICAL,
                    labelBn = "জরুরি বকেয়া (${debtAgeDaysBn} দিন)",
                    labelEn = "Critical Debt (${debtAgeDays}d)",
                    containerColor = Color(0xFFFFEBEE),
                    contentColor = Color(0xFFC62828),
                    borderColor = Color(0xFFEF9A9A),
                    daysDifference = debtAgeDays
                )
            }
            debtAgeDays >= 14 -> {
                OverdueBadgeInfo(
                    level = OverdueLevel.OVERDUE,
                    labelBn = "বকেয়া তাগাদা (${debtAgeDaysBn} দিন)",
                    labelEn = "Overdue Action (${debtAgeDays}d)",
                    containerColor = Color(0xFFFFF3E0),
                    contentColor = Color(0xFFE65100),
                    borderColor = Color(0xFFFFCC80),
                    daysDifference = debtAgeDays
                )
            }
            else -> {
                OverdueBadgeInfo(
                    level = OverdueLevel.NONE,
                    labelBn = "নিয়মিত",
                    labelEn = "Active",
                    containerColor = Color(0xFFE8F5E9),
                    contentColor = Color(0xFF2E7D32),
                    borderColor = Color(0xFFA5D6A7),
                    daysDifference = debtAgeDays
                )
            }
        }
    }

    /**
     * Calculates overdue level for an individual transaction if it represents a credit sale.
     */
    fun computeTransactionOverdue(
        transaction: Transaction,
        currentTime: Long = System.currentTimeMillis()
    ): OverdueBadgeInfo? {
        if (transaction.type != TransactionType.BAKI_SALE) return null
        val dueDate = transaction.dueDate ?: return null
        val diffMs = dueDate - currentTime
        val diffDays = (diffMs / ONE_DAY_MS).toInt()

        return when {
            diffMs < -7 * ONE_DAY_MS -> OverdueBadgeInfo(
                level = OverdueLevel.CRITICAL,
                labelBn = "জরুরি তাগাদা",
                labelEn = "Critical Due",
                containerColor = Color(0xFFFFEBEE),
                contentColor = Color(0xFFC62828),
                borderColor = Color(0xFFEF9A9A)
            )
            diffMs < 0 -> OverdueBadgeInfo(
                level = OverdueLevel.OVERDUE,
                labelBn = "মেয়াদোত্তীর্ণ",
                labelEn = "Overdue",
                containerColor = Color(0xFFFFF3E0),
                contentColor = Color(0xFFE65100),
                borderColor = Color(0xFFFFCC80)
            )
            diffMs <= 3 * ONE_DAY_MS -> OverdueBadgeInfo(
                level = OverdueLevel.DUE_SOON,
                labelBn = "শীঘ্রই বকেয়া",
                labelEn = "Due Soon",
                containerColor = Color(0xFFFFFDE7),
                contentColor = Color(0xFFF57F17),
                borderColor = Color(0xFFFFF59D)
            )
            else -> null
        }
    }
}
