package com.example.ui

import com.example.voice.BanglaVoiceParser
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object BakiStrings {

    fun appName(isBn: Boolean) = if (isBn) "বাকী বন্ধু" else "Baki Bondhu"
    fun totalBaki(isBn: Boolean) = if (isBn) "মোট বাকি পাওনা" else "Total Credit Due"
    fun todaySales(isBn: Boolean) = if (isBn) "আজকের বাকি বিক্রি" else "Today's Credit Sales"
    fun todayCollected(isBn: Boolean) = if (isBn) "আজকের আদায়" else "Today's Collection"
    fun speakNow(isBn: Boolean) = if (isBn) "বলুন: নাম, টাকা ও বাকি/জমা" else "Speak: Name, Amount & Due/Paid"
    fun holdToSpeak(isBn: Boolean) = if (isBn) "কথা বলতে চাপুন" else "Tap to Speak"
    fun listening(isBn: Boolean) = if (isBn) "শুনছি... বলুন" else "Listening... please speak"
    fun customersTab(isBn: Boolean) = if (isBn) "খাতা / ক্রেতা" else "Ledger / Customers"
    fun homeTab(isBn: Boolean) = if (isBn) "হোম" else "Home"
    fun reportsTab(isBn: Boolean) = if (isBn) "রিপোর্ট ও তাগাদা" else "Reports & Dues"
    fun settingsTab(isBn: Boolean) = if (isBn) "দোকান সেটিংস" else "Shop Settings"
    fun confirmEntry(isBn: Boolean) = if (isBn) "হিসাব নিশ্চিত করুন" else "Confirm Transaction"
    fun confirmAndSave(isBn: Boolean) = if (isBn) "নিশ্চিত ও সংরক্ষণ করুন" else "Confirm & Save"
    fun correctEntry(isBn: Boolean) = if (isBn) "সংশোধন করুন" else "Edit / Correct"
    fun cancel(isBn: Boolean) = if (isBn) "বাতিল" else "Cancel"
    fun save(isBn: Boolean) = if (isBn) "সংরক্ষণ" else "Save"
    fun sendSms(isBn: Boolean) = if (isBn) "SMS তাগাদা পাঠান" else "Send SMS Reminder"
    fun settleDue(isBn: Boolean) = if (isBn) "বাকি পরিশোধ মার্ক করুন" else "Mark Due as Settled"
    fun manualEntry(isBn: Boolean) = if (isBn) "কীবোর্ডে হিসাব লিখুন" else "Type Manually"
    fun addCustomer(isBn: Boolean) = if (isBn) "নতুন ক্রেতা যোগ করুন" else "Add New Customer"
    fun customerName(isBn: Boolean) = if (isBn) "ক্রেতার নাম" else "Customer Name"
    fun mobileNumber(isBn: Boolean) = if (isBn) "মোবাইল নম্বর" else "Mobile Number"
    fun amountTaka(isBn: Boolean) = if (isBn) "টাকার পরিমাণ (৳)" else "Amount (৳)"
    fun bakiSale(isBn: Boolean) = if (isBn) "বাকি বিক্রি" else "Credit Sale"
    fun repayment(isBn: Boolean) = if (isBn) "জমা / পরিশোধ" else "Repayment"
    fun cashSale(isBn: Boolean) = if (isBn) "নগদ বিক্রি" else "Cash Sale"
    fun searchCustomer(isBn: Boolean) = if (isBn) "ক্রেতা বা ফোন নম্বর খুঁজুন..." else "Search customer or phone..."
    fun oldestDues(isBn: Boolean) = if (isBn) "পুরোনো বকেয়া (জরুরি তাগাদা)" else "Oldest Dues (Urgent Action)"
    fun trustGood(isBn: Boolean) = if (isBn) "নিয়মিত পরিশোধকারী" else "Reliable Payer"
    fun trustMedium(isBn: Boolean) = if (isBn) "মাঝারি ক্রেতা" else "Moderate Payer"
    fun trustLow(isBn: Boolean) = if (isBn) "দেরি করে দেন" else "Late Payer"
    fun bkashNagadInfo(isBn: Boolean) = if (isBn) "বিকাশ ও নগদ নম্বর" else "bKash & Nagad Number"
    fun allTime(isBn: Boolean) = if (isBn) "সব দিন" else "All Time"
    fun today(isBn: Boolean) = if (isBn) "আজকে" else "Today"
    fun thisWeek(isBn: Boolean) = if (isBn) "এই সপ্তাহে" else "This Week"
    fun customDate(isBn: Boolean) = if (isBn) "তারিখ 📅" else "Custom 📅"
    fun cashDrawer(isBn: Boolean) = if (isBn) "ক্যাশ ড্রয়ার" else "Cash Till"
    fun dailyClosing(isBn: Boolean) = if (isBn) "দিনের ক্লোজিং ও ক্যাশ ড্রয়ার মিলান" else "Daily Closing & Till Reconciliation"
    fun recentTransactions(isBn: Boolean) = if (isBn) "সাম্প্রতিক লেনদেন" else "Recent Transactions"
    fun selectMode(isBn: Boolean) = if (isBn) "বাছাই" else "Select"
    fun done(isBn: Boolean) = if (isBn) "সম্পন্ন" else "Done"
    fun voiceBannerTitle(isBn: Boolean) = if (isBn) "মুখে বলে হিসাব লিখুন" else "Speak to Record Entry"
    fun voiceBannerSub(isBn: Boolean) = if (isBn) "যেমন: 'রহিম ৪৫০ বাকি' বা 'করিম ২০০ জমা'" else "e.g. 'Rahim 450 credit' or 'Karim 200 paid'"
    fun freeVoiceTitle(isBn: Boolean) = if (isBn) "গুগল ভয়েস ইনপুট (ফ্রি • কোনো কী দরকার নেই)" else "Google Voice Input (Free • No Key Needed)"

    fun searchHint(isBn: Boolean) = if (isBn) "ক্রেতার নাম বা ফোন নম্বর খুঁজুন..." else "Search customer or phone..."
    fun cloudSynced(isBn: Boolean) = if (isBn) "সব তথ্য ক্লাউডে সুরক্ষিত" else "All records backed up to cloud"
    fun cloudSyncing(isBn: Boolean) = if (isBn) "ক্লাউডে ব্যাকআপ নেওয়া হচ্ছে..." else "Backing up to cloud..."
    fun tapToSync(isBn: Boolean) = if (isBn) "ক্লাউড ব্যাকআপ নিতে চাপুন" else "Tap to backup to cloud"

    fun formatMoney(amount: Double, isBn: Boolean): String {
        val intVal = amount.toInt()
        val text = "৳\u00A0$intVal"
        return if (isBn) BanglaVoiceParser.toBanglaDigits(text) else text
    }

    fun buildSmsText(
        customerName: String,
        amount: Double,
        shopName: String,
        bkashNumber: String,
        nagadNumber: String,
        tone: SmsTone,
        isBn: Boolean
    ): String {
        val amountStr = if (isBn) BanglaVoiceParser.toBanglaDigits(amount.toInt().toString()) else "${amount.toInt()}"
        val paymentInfo = buildString {
            if (bkashNumber.isNotBlank()) append("bKash (Send Money): $bkashNumber")
            if (nagadNumber.isNotBlank()) {
                if (isNotEmpty()) append(" / ")
                append("Nagad: $nagadNumber")
            }
        }

        return if (isBn) {
            when (tone) {
                SmsTone.GENTLE -> "আসসালামু আলাইকুম $customerName ভাই, আপনার $shopName এ $amountStr টাকা বাকি আছে। সময়মত পরিশোধের অনুরোধ রইলো। টাকা পাঠানোর নম্বর: $paymentInfo। ধন্যবাদ।"
                SmsTone.STANDARD -> "প্রিয় $customerName, আপনার $shopName এ বাকি বিল $amountStr টাকা। অনুগ্রহ করে পরিশোধ করুন। $paymentInfo। ধন্যবাদ।"
                SmsTone.FIRM -> "জরুরি তাগাদা: $customerName, আপনার $amountStr টাকা বাকি বকেয়া দীর্ঘদিনের। দয়া করে আজই $paymentInfo নম্বরে পাঠিয়ে পরিশোধ করুন।"
            }
        } else {
            when (tone) {
                SmsTone.GENTLE -> "Dear $customerName, friendly reminder that you have a due balance of ৳$amountStr at $shopName. You can send payment to: $paymentInfo. Thank you!"
                SmsTone.STANDARD -> "Dear $customerName, your pending credit bill at $shopName is ৳$amountStr. Please pay via $paymentInfo. Thank you."
                SmsTone.FIRM -> "Urgent Reminder: $customerName, your overdue payment of ৳$amountStr at $shopName is pending. Please send to $paymentInfo today."
            }
        }
    }

    fun buildWhatsAppReminderText(
        customerName: String,
        amount: Double,
        shopName: String,
        ownerName: String = "",
        bkashNumber: String = "",
        nagadNumber: String = "",
        lastTransactionAt: Long = 0L,
        tone: SmsTone = SmsTone.STANDARD,
        isBn: Boolean = true
    ): String {
        val amountFormatted = formatMoney(amount, isBn)
        val sName = shopName.ifBlank { if (isBn) "দোকানের হিসাব" else "Shop Ledger" }
        val lastDateText = if (lastTransactionAt > 0L) {
            val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            val d = sdf.format(Date(lastTransactionAt))
            if (isBn) BanglaVoiceParser.toBanglaDigits(d) else d
        } else null

        val paymentAccounts = buildList {
            if (bkashNumber.isNotBlank()) add("• *bKash (Send Money)*: `$bkashNumber`")
            if (nagadNumber.isNotBlank()) add("• *Nagad (Send Money)*: `$nagadNumber`")
        }

        return if (isBn) {
            buildString {
                when (tone) {
                    SmsTone.GENTLE -> {
                        appendLine("আসসালামু আলাইকুম *$customerName* ভাই/আপা,")
                        appendLine("আশা করি আল্লাহর রহমতে ভালো আছেন। *$sName* এ আপনার বাকী হিসাবের একটি বন্ধুত্বপূর্ণ বিবরণ নিচে দেওয়া হলো:")
                    }
                    SmsTone.STANDARD -> {
                        appendLine("প্রিয় *$customerName*,")
                        appendLine("*$sName* থেকে আপনার বর্তমান বাকী বিলের তাগাদা বিবরণ:")
                    }
                    SmsTone.FIRM -> {
                        appendLine("⚠️ *জরুরি তাগাদা নোটিশ*")
                        appendLine("জনাব *$customerName*,")
                        appendLine("*$sName* এ আপনার দীর্ঘদিনের বকেয়া পাওনা দ্রুততম সময়ে পরিশোধ করার অনুরোধ করা হচ্ছে:")
                    }
                }
                appendLine()
                appendLine("💰 *মোট বকেয়া পাওনা: $amountFormatted*")
                if (lastDateText != null) {
                    appendLine("📅 শেষ লেনদেন: $lastDateText")
                }
                appendLine("──────────────────")
                if (paymentAccounts.isNotEmpty()) {
                    appendLine("📲 *সহজে ঘরে বসেই পরিশোধ করুন:*")
                    paymentAccounts.forEach { appendLine(it) }
                    appendLine()
                }
                when (tone) {
                    SmsTone.GENTLE -> appendLine("সুবিধাজনক সময়ে বকেয়া পরিশোধের অনুরোধ রইলো। অনেক ধন্যবাদ।")
                    SmsTone.STANDARD -> appendLine("অনুগ্রহ করে দ্রুততম সময়ে পরিশোধ করে হিসাব সচল রাখুন। ধন্যবাদ।")
                    SmsTone.FIRM -> appendLine("দয়া করে আজই বকেয়া পরিশোধ করে আপনার ক্রেডিট হিসাব সচল রাখুন।")
                }
                if (ownerName.isNotBlank()) {
                    appendLine("— *$ownerName* ($sName)")
                } else {
                    appendLine("— *$sName*")
                }
                appendLine("⚡ *বাকী বন্ধু (DevCenterPoint)*")
            }
        } else {
            buildString {
                when (tone) {
                    SmsTone.GENTLE -> {
                        appendLine("Hello *$customerName*,")
                        appendLine("Hope you are doing well. Here is a friendly statement from *$sName*:")
                    }
                    SmsTone.STANDARD -> {
                        appendLine("Dear *$customerName*,")
                        appendLine("Payment reminder for your outstanding credit balance at *$sName*:")
                    }
                    SmsTone.FIRM -> {
                        appendLine("⚠️ *Urgent Payment Notice*")
                        appendLine("Attention *$customerName*,")
                        appendLine("Your overdue balance at *$sName* requires immediate settlement:")
                    }
                }
                appendLine()
                appendLine("💰 *Total Amount Due: $amountFormatted*")
                if (lastDateText != null) {
                    appendLine("📅 Last Transaction: $lastDateText")
                }
                appendLine("──────────────────")
                if (paymentAccounts.isNotEmpty()) {
                    appendLine("📲 *Pay directly via Mobile Banking:*")
                    paymentAccounts.forEach { appendLine(it) }
                    appendLine()
                }
                when (tone) {
                    SmsTone.GENTLE -> appendLine("Please clear the dues when convenient. Thank you!")
                    SmsTone.STANDARD -> appendLine("Please settle this invoice at your earliest convenience. Thank you.")
                    SmsTone.FIRM -> appendLine("Please settle this pending bill today to avoid account suspension.")
                }
                if (ownerName.isNotBlank()) {
                    appendLine("— *$ownerName* ($sName)")
                } else {
                    appendLine("— *$sName*")
                }
                appendLine("⚡ *Baki Bondhu*")
            }
        }
    }
}

enum class SmsTone {
    GENTLE,
    STANDARD,
    FIRM
}
