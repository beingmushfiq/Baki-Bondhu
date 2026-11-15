package com.example.voice

import com.example.data.model.TransactionType
import com.example.util.Money
import java.util.regex.Pattern

data class ParsedVoiceResult(
    val customerName: String,
    val amountPaisa: Long?,
    val type: TransactionType,
    val rawText: String,
    val isConfident: Boolean = true,
    val isAmountMissing: Boolean = (amountPaisa == null || amountPaisa <= 0L),
    val isCustomerAmbiguous: Boolean = false
) {
    // Backward-compatible Double accessor
    val amount: Double get() = (amountPaisa ?: 0L) / 100.0

    val money: Money get() = Money(amountPaisa ?: 0L)

    // Secondary constructor for Double during legacy transitions
    constructor(
        customerName: String,
        amount: Double,
        type: TransactionType,
        rawText: String,
        isConfident: Boolean = true
    ) : this(
        customerName = customerName,
        amountPaisa = if (amount > 0.0) Math.round(amount * 100.0) else null,
        type = type,
        rawText = rawText,
        isConfident = isConfident,
        isAmountMissing = amount <= 0.0
    )

    fun toReadBackPhrase(isBangla: Boolean = true): String {
        val moneyText = if (amountPaisa != null && amountPaisa > 0L) {
            Money(amountPaisa).format(isBn = isBangla)
        } else {
            if (isBangla) "টাকার পরিমাণ উল্লেখ নেই" else "Amount not specified"
        }

        return if (isBangla) {
            when (type) {
                TransactionType.BAKI_SALE -> "$customerName, $moneyText বাকী, নিশ্চিত করবেন?"
                TransactionType.REPAYMENT -> "$customerName, $moneyText জমা পাওয়া গেছে, নিশ্চিত করবেন?"
                TransactionType.CASH_SALE -> "$customerName, $moneyText নগদ বিক্রি, নিশ্চিত করবেন?"
            }
        } else {
            when (type) {
                TransactionType.BAKI_SALE -> "$customerName, $moneyText credit sale, please confirm?"
                TransactionType.REPAYMENT -> "$customerName, $moneyText payment received, please confirm?"
                TransactionType.CASH_SALE -> "$customerName, $moneyText cash sale, please confirm?"
            }
        }
    }
}

object BanglaVoiceParser {

    private val banglaToAsciiDigits = mapOf(
        '০' to '0', '১' to '1', '২' to '2', '৩' to '3', '৪' to '4',
        '৫' to '5', '৬' to '6', '৭' to '7', '৮' to '8', '৯' to '9'
    )

    private val asciiToBanglaDigits = mapOf(
        '0' to '০', '1' to '১', '2' to '২', '3' to '৩', '4' to '৪',
        '5' to '৫', '6' to '৬', '7' to '৭', '8' to '৮', '9' to '৯'
    )

    private val compoundWordValues = listOf(
        "আড়াই হাজার টাকা" to 250000L,
        "আড়াই হাজার টাকা" to 250000L,
        "দেড় হাজার টাকা" to 150000L,
        "দেড় হাজার টাকা" to 150000L,
        "দুই হাজার টাকা" to 200000L,
        "এক হাজার টাকা" to 100000L,
        "তিন হাজার টাকা" to 300000L,
        "চার হাজার টাকা" to 400000L,
        "পাঁচ হাজার টাকা" to 500000L,
        "ছয় হাজার টাকা" to 600000L,
        "সাত হাজার টাকা" to 700000L,
        "আট হাজার টাকা" to 800000L,
        "নয় হাজার টাকা" to 900000L,
        "দশ হাজার টাকা" to 1000000L,
        "বিশ হাজার টাকা" to 2000000L,
        "পঞ্চাশ হাজার টাকা" to 5000000L,
        "আড়াই হাজার" to 250000L,
        "আড়াই হাজার" to 250000L,
        "দেড় হাজার" to 150000L,
        "দেড় হাজার" to 150000L,
        "দুই হাজার" to 200000L,
        "এক হাজার" to 100000L,
        "তিন হাজার" to 300000L,
        "চার হাজার" to 400000L,
        "পাঁচ হাজার" to 500000L,
        "ছয় হাজার" to 600000L,
        "সাত হাজার" to 700000L,
        "আট হাজার" to 800000L,
        "নয় হাজার" to 900000L,
        "দশ হাজার" to 1000000L,
        "বিশ হাজার" to 2000000L,
        "পঞ্চাশ হাজার" to 5000000L,
        "এক লাখ" to 10000000L,
        "দেড়শো" to 15000L,
        "আড়াইশো" to 25000L,
        "আড়াইশো" to 25000L,
        "সাড়ে তিনশো" to 35000L,
        "সাড়ে চারশো" to 45000L,
        "পনেরোশো" to 150000L,
        "বারোশো" to 120000L,
        "এগারোশো" to 110000L,
        "নয়শো" to 90000L,
        "নয়শো" to 90000L,
        "নশো" to 90000L,
        "নয়শত" to 90000L,
        "নয়শত" to 90000L,
        "আটশো" to 80000L,
        "আটশত" to 80000L,
        "সাতশো" to 70000L,
        "সাতশত" to 70000L,
        "ছয়শো" to 60000L,
        "ছয়শো" to 60000L,
        "ছশো" to 60000L,
        "ছশত" to 60000L,
        "পাঁচশো" to 50000L,
        "পাঁচশত" to 50000L,
        "চারশো" to 40000L,
        "চারশত" to 40000L,
        "তিনশো" to 30000L,
        "তিনশত" to 30000L,
        "দুইশো" to 20000L,
        "দুশো" to 20000L,
        "দুইশত" to 20000L,
        "একশো" to 10000L,
        "একশত" to 10000L,
        "একশ" to 10000L,
        "পঞ্চাশ" to 5000L,
        "একশত টাকা" to 10000L,
        "হাজার" to 100000L
    ).sortedByDescending { it.first.length }

    fun toBanglaDigits(asciiStr: String): String {
        val sb = StringBuilder()
        for (ch in asciiStr) {
            sb.append(asciiToBanglaDigits[ch] ?: ch)
        }
        return sb.toString()
    }

    fun toAsciiDigits(str: String): String {
        val sb = StringBuilder()
        for (ch in str) {
            sb.append(banglaToAsciiDigits[ch] ?: ch)
        }
        return sb.toString()
    }

    /**
     * Parses spoken Bengali, Banglish, or English credit ledger phrase into structured transaction data.
     * NEVER fabricates an amount if none was spoken.
     */
    fun parse(input: String, knownCustomerNames: List<String> = emptyList()): ParsedVoiceResult {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) {
            return ParsedVoiceResult(
                customerName = "নতুন ক্রেতা",
                amountPaisa = null,
                type = TransactionType.BAKI_SALE,
                rawText = input,
                isConfident = false,
                isAmountMissing = true
            )
        }

        val lower = trimmed.lowercase()

        // 1. Determine Intent (BAKI_SALE vs REPAYMENT vs CASH_SALE)
        val type = when {
            containsAny(lower,
                "জমা", "পরিশোধ", "দিল", "দিলো", "শোধ", "ফেরত", "পেল", "পেলেন", "দিয়েছেন", "দিয়েছে",
                "joma", "paid", "repay", "repayment", "received", "cleared") ->
                TransactionType.REPAYMENT

            containsAny(lower,
                "নগদ", "ক্যাশ", "নগদে", "নগদ বিক্রি", "cash", "cash sale") ->
                TransactionType.CASH_SALE

            containsAny(lower,
                "বাকি", "বাকী", "ধার", "করজ", "নিলো", "নিল", "নিলেন", "নেওয়া", "লিখেন", "রাখুন",
                "baki", "due", "credit", "borrow") ->
                TransactionType.BAKI_SALE

            else -> TransactionType.BAKI_SALE // Default ledger assumption
        }

        // 2. Extract Amount in Paisa (1 BDT = 100 Paisa)
        // Check for compound spoken amounts first (e.g. "দেড় হাজার পাঁচশো" = 2000, "দুই হাজার তিনশো" = 2300)
        var extractedPaisa = extractWordAmountPaisa(trimmed)
        if (extractedPaisa == null || extractedPaisa == 0L) {
            extractedPaisa = extractNumericAmountPaisa(trimmed)
        }

        val isAmountMissing = (extractedPaisa == null || extractedPaisa <= 0L)

        // 3. Extract Customer Name
        var extractedName = ""
        var isAmbiguous = false

        // A. Match against known customer names (Exact or sub-phrase)
        val sortedKnown = knownCustomerNames.filter { it.isNotBlank() }.sortedByDescending { it.length }
        for (known in sortedKnown) {
            if (trimmed.contains(known, ignoreCase = true)) {
                extractedName = known
                break
            }
        }

        // B. If no known customer matched, extract name candidates using grammatical isolation
        if (extractedName.isEmpty()) {
            val tokens = trimmed.split(Regex("[\\s,।!?-]+")).filter { it.isNotBlank() }
            val nonNameWords = setOf(
                "টাকা", "টাকার", "টাকায়", "টাকায়", "বাকি", "বাকী", "ধার", "জমা", "পরিশোধ", "শোধ",
                "দিল", "দিলো", "দিয়েছেন", "দিয়েছে", "দিবেন", "নিল", "নিলো", "নিলেন", "নেবেন", "নগদ", "ক্যাশ", "বিক্রি",
                "হিসাব", "লিখুন", "রাখুন", "লিখেন", "আজকে", "আজ", "গতকাল", "করলেন", "করল", "করলো", "করেছেন", "করবেন",
                "taka", "tk", "baki", "joma", "paid", "due", "credit", "cash", "er", "এর", "কে", "রে",
                "আড়াই", "দেড়", "হাজার", "পনেরোশো", "বারোশো", "নয়শো", "নয়শো", "আটশো", "সাতশো", "ছয়শো", "ছয়শো", "ছশো",
                "পাঁচশো", "পাঁচশত", "চারশো", "তিনশো", "আড়াইশো", "দুইশো", "দুশো", "দেড়শো", "একশো",
                "একশত", "একশ", "পঞ্চাশ", "লাখ", "কোটি", "shoo", "hajar", "hazar", "lakh"
            )

            val nameCandidates = mutableListOf<String>()
            for (token in tokens) {
                val cleanDigitCheck = toAsciiDigits(token).replace(Regex("[^0-9]"), "")
                if (cleanDigitCheck.isNotEmpty()) {
                    if (nameCandidates.isNotEmpty()) break
                    continue
                }
                val isNumberWord = compoundWordValues.any { it.first == token.lowercase() }
                if (isNumberWord) {
                    if (nameCandidates.isNotEmpty()) break
                    continue
                }
                if (nonNameWords.contains(token.lowercase())) continue

                // Clean honorific prefixes/suffixes if attached
                var cleanedToken = token
                for (suffix in listOf("ের", "কে", "রে", "এর")) {
                    if (cleanedToken.length > 2 && cleanedToken.endsWith(suffix)) {
                        cleanedToken = cleanedToken.dropLast(suffix.length)
                        break
                    }
                }

                if (cleanedToken.isNotBlank()) {
                    nameCandidates.add(cleanedToken)
                }
            }

            extractedName = when {
                nameCandidates.isNotEmpty() -> nameCandidates.take(2).joinToString(" ")
                else -> {
                    isAmbiguous = true
                    "ক্রেতা"
                }
            }
        }

        return ParsedVoiceResult(
            customerName = extractedName.trim(),
            amountPaisa = if (isAmountMissing) null else extractedPaisa,
            type = type,
            rawText = trimmed,
            isConfident = !isAmountMissing && !isAmbiguous,
            isAmountMissing = isAmountMissing,
            isCustomerAmbiguous = isAmbiguous
        )
    }

    private fun containsAny(text: String, vararg keywords: String): Boolean {
        for (kw in keywords) {
            if (text.contains(kw, ignoreCase = true)) return true
        }
        return false
    }

    private fun extractNumericAmountPaisa(text: String): Long? {
        val asciiText = toAsciiDigits(text)
        val regex = Regex("\\b\\d+(?:,\\d+)*(?:\\.\\d+)?\\b")
        val matches = regex.findAll(asciiText).toList()

        for (m in matches) {
            val cleaned = m.value.replace(",", "")
            val num = cleaned.toDoubleOrNull()
            if (num != null && num > 0.0) {
                // Check if preceded by thousand/lakh multiplier in words
                val matchIndex = m.range.first
                val beforeMatch = asciiText.substring(0, matchIndex).trim()
                var multiplier = 1L
                if (beforeMatch.endsWith("হাজার") || text.contains("${m.value} হাজার")) {
                    multiplier = 1000L
                }
                return Math.round(num * multiplier * 100.0)
            }
        }
        return null
    }

    private fun extractWordAmountPaisa(text: String): Long? {
        val lower = text.lowercase()

        for ((phrase, paisaValue) in compoundWordValues) {
            if (lower.contains(phrase)) {
                return paisaValue
            }
        }

        return null
    }
}
