package com.example.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.data.model.CustomerEntity
import com.example.data.model.ShopSettingsEntity
import com.example.data.model.Transaction
import com.example.data.model.TransactionType
import com.example.ui.BakiStrings
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class ShareChannel {
    CHOOSER,
    WHATSAPP,
    SMS
}

object DigitalReceiptHelper {

    fun generateReceiptText(
        transaction: Transaction,
        customer: CustomerEntity? = null,
        settings: ShopSettingsEntity = ShopSettingsEntity(),
        isBn: Boolean = true
    ): String {
        val dateFormat = SimpleDateFormat(
            if (isBn) "dd MMMM yyyy, hh:mm a" else "dd MMM yyyy, hh:mm a",
            Locale.getDefault()
        )
        val formattedDate = dateFormat.format(Date(transaction.timestamp))
        val shopName = settings.shopName.ifBlank { if (isBn) "দোকানের বাকি খাতা" else "Shop Credit Ledger" }
        val customerPhone = customer?.phone?.ifBlank { null }

        val typeTitle = when (transaction.type) {
            TransactionType.BAKI_SALE -> if (isBn) "বাকি বিক্রি (Credit Sale)" else "Credit Sale"
            TransactionType.REPAYMENT -> if (isBn) "জমা / পরিশোধ (Payment Received)" else "Payment Received"
            TransactionType.CASH_SALE -> if (isBn) "নগদ বিক্রি (Cash Sale)" else "Cash Sale"
        }

        val amountFormatted = BakiStrings.formatMoney(transaction.amount, isBn)

        return buildString {
            appendLine("🧾 *${if (isBn) "ডিজিটাল রসিদ" else "DIGITAL RECEIPT"}*")
            appendLine("🏪 *${shopName}*")
            if (settings.ownerName.isNotBlank()) {
                appendLine("${if (isBn) "দোকানদার" else "Proprietor"}: ${settings.ownerName}")
            }
            appendLine("📅 ${if (isBn) "তারিখ ও সময়" else "Date & Time"}: $formattedDate")
            appendLine("──────────────────")
            appendLine("👤 ${if (isBn) "খরিদ্দার" else "Customer"}: ${transaction.customerName}")
            if (!customerPhone.isNullOrBlank()) {
                appendLine("📱 ${if (isBn) "মোবাইল" else "Mobile"}: $customerPhone")
            }
            appendLine("──────────────────")
            appendLine("📌 ${if (isBn) "লেনদেনের ধরন" else "Type"}: $typeTitle")
            appendLine("💵 ${if (isBn) "টাকার পরিমাণ" else "Amount"}: *$amountFormatted*")

            if (transaction.note.isNotBlank() && transaction.note != transaction.originalSpokenText) {
                appendLine("📝 ${if (isBn) "নোট / বিবরণ" else "Note"}: ${transaction.note}")
            }

            if (!transaction.originalSpokenText.isNullOrBlank()) {
                appendLine("🗣️ ${if (isBn) "ভয়েস রেকর্ড" else "Voice Note"}: \"${transaction.originalSpokenText}\"")
            }

            if (customer != null && customer.balance > 0) {
                appendLine("──────────────────")
                val totalDue = BakiStrings.formatMoney(customer.balance, isBn)
                appendLine("💰 *${if (isBn) "বর্তমান মোট বকেয়া" else "Total Current Balance"}: $totalDue*")
            }

            if (settings.bkashNumber.isNotBlank() || settings.nagadNumber.isNotBlank()) {
                appendLine("──────────────────")
                appendLine(if (isBn) "📲 পরিশোধের ডিজিটাল মাধ্যম:" else "📲 Payment Accounts:")
                if (settings.bkashNumber.isNotBlank()) {
                    appendLine("• bKash (Send Money): ${settings.bkashNumber}")
                }
                if (settings.nagadNumber.isNotBlank()) {
                    appendLine("• Nagad: ${settings.nagadNumber}")
                }
            }

            appendLine("──────────────────")
            appendLine(if (isBn) "⚡ কারিগরি সহায়তায়: DevCenterPoint (devcenterpoint.com)" else "⚡ Powered by DevCenterPoint (devcenterpoint.com)")
        }
    }

    fun shareReceipt(
        context: Context,
        receiptText: String,
        channel: ShareChannel = ShareChannel.CHOOSER,
        customerPhone: String? = null,
        title: String = "Share Receipt"
    ): Boolean {
        return try {
            when (channel) {
                ShareChannel.WHATSAPP -> {
                    val cleanPhone = customerPhone?.filter { it.isDigit() }
                    val intent = if (!cleanPhone.isNullOrBlank()) {
                        val formattedNumber = if (cleanPhone.startsWith("0")) "88$cleanPhone" else cleanPhone
                        Intent(Intent.ACTION_VIEW).apply {
                            data = Uri.parse("https://api.whatsapp.com/send?phone=$formattedNumber&text=${Uri.encode(receiptText)}")
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                    } else {
                        Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            setPackage("com.whatsapp")
                            putExtra(Intent.EXTRA_TEXT, receiptText)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                    }
                    context.startActivity(intent)
                    true
                }
                ShareChannel.SMS -> {
                    val cleanPhone = customerPhone?.filter { it.isDigit() }
                    val smsUri = if (!cleanPhone.isNullOrBlank()) {
                        Uri.parse("smsto:$cleanPhone")
                    } else {
                        Uri.parse("smsto:")
                    }
                    val intent = Intent(Intent.ACTION_SENDTO, smsUri).apply {
                        putExtra("sms_body", receiptText)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                    true
                }
                ShareChannel.CHOOSER -> {
                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, receiptText)
                    }
                    val chooserIntent = Intent.createChooser(sendIntent, title).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(chooserIntent)
                    true
                }
            }
        } catch (_: Exception) {
            // Fallback gracefully to generic chooser
            try {
                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, receiptText)
                }
                val chooserIntent = Intent.createChooser(sendIntent, title).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(chooserIntent)
                true
            } catch (_: Exception) {
                false
            }
        }
    }

    fun formatWhatsAppPhone(phone: String): String {
        val clean = phone.filter { it.isDigit() }
        return when {
            clean.startsWith("880") -> clean
            clean.startsWith("0") -> "88$clean"
            clean.length == 10 && clean.startsWith("1") -> "880$clean"
            else -> clean
        }
    }

    fun generateWhatsAppReminderMessage(
        customer: CustomerEntity,
        settings: ShopSettingsEntity = ShopSettingsEntity(),
        tone: com.example.ui.SmsTone = com.example.ui.SmsTone.STANDARD,
        isBn: Boolean = true
    ): String {
        return BakiStrings.buildWhatsAppReminderText(
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

    /**
     * Opens WhatsApp directly with prefilled personalized message and customer's phone deep-link.
     */
    fun openWhatsAppDirect(
        context: Context,
        phone: String,
        message: String
    ): Boolean {
        val formattedNumber = formatWhatsAppPhone(phone)
        val encodedText = Uri.encode(message)
        val uri = if (formattedNumber.isNotBlank()) {
            Uri.parse("https://api.whatsapp.com/send?phone=$formattedNumber&text=$encodedText")
        } else {
            Uri.parse("https://api.whatsapp.com/send?text=$encodedText")
        }

        // Try launching WhatsApp package specifically
        val intentWithPackage = Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage("com.whatsapp")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        return try {
            context.startActivity(intentWithPackage)
            true
        } catch (_: Exception) {
            // Fallback 1: Try without com.whatsapp package (handles WhatsApp Business, browsers, or other WA apps)
            try {
                val genericIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(genericIntent)
                true
            } catch (_: Exception) {
                // Fallback 2: Share sheet or copy text
                try {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, message)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    val chooser = Intent.createChooser(shareIntent, "WhatsApp").apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(chooser)
                    true
                } catch (_: Exception) {
                    false
                }
            }
        }
    }

    fun copyToClipboard(context: Context, text: String, label: String = "Digital Receipt") {
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText(label, text)
            clipboard.setPrimaryClip(clip)
        } catch (_: Exception) {}
    }
}
