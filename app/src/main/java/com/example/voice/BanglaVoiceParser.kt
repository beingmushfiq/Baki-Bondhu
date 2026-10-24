package com.example.voice

import com.example.data.model.TransactionType

data class ParsedVoiceResult(
    val customerName: String,
    val amount: Double,
    val type: TransactionType,
    val rawText: String,
    val isConfident: Boolean = true
) {
    fun toReadBackPhrase(isBangla: Boolean = true): String {
        val bnDigits = BanglaVoiceParser.toBanglaDigits(amount.toInt().toString())
        return if (isBangla) {
            when (type) {
                TransactionType.BAKI_SALE -> "$customerName, $bnDigits টাকা বাকী, নিশ্চিত করবেন?"
                TransactionType.REPAYMENT -> "$customerName, $bnDigits টাকা জমা পাওয়া গেছে, নিশ্চিত করবেন?"
                TransactionType.CASH_SALE -> "$customerName, $bnDigits টাকা নগদ বিক্রি, নিশ্চিত করবেন?"
            }
        } else {
            when (type) {
                TransactionType.BAKI_SALE -> "$customerName, ${amount.toInt()} Taka credit sale, please confirm?"
                TransactionType.REPAYMENT -> "$customerName, ${amount.toInt()} Taka payment received, please confirm?"
                TransactionType.CASH_SALE -> "$customerName, ${amount.toInt()} Taka cash sale, please confirm?"
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

    fun parse(input: String, knownCustomerNames: List<String> = emptyList()): ParsedVoiceResult {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) {
            return ParsedVoiceResult(
                customerName = "নতুন ক্রেতা",
                amount = 100.0,
                type = TransactionType.BAKI_SALE,
                rawText = input,
                isConfident = false
            )
        }

        val lower = trimmed.lowercase()

        // 1. Determine Intent
        val type = when {
            containsAny(lower, "জমা", "পরিশোধ", "দিল", "শোধ", "ফেরত", "পেল", "পেলেন", "joma", "paid", "repay", "received") ->
                TransactionType.REPAYMENT
            containsAny(lower, "নগদ", "ক্যাশ", "নগদে", "cash") ->
                TransactionType.CASH_SALE
            containsAny(lower, "বাকি", "বাকী", "ধার", "করজ", "নিলো", "নিল", "baki", "due", "credit") ->
                TransactionType.BAKI_SALE
            else -> TransactionType.BAKI_SALE // Default for shop credit ledger
        }

        // 2. Extract Amount
        var extractedAmount = extractNumericAmount(trimmed)
        if (extractedAmount == 0.0) {
            extractedAmount = extractWordAmount(trimmed)
        }
        if (extractedAmount == 0.0) {
            extractedAmount = 100.0 // Reasonable fallback default
        }

        // 3. Extract Customer Name
        var extractedName = ""

        // Check if a known customer name is mentioned
        for (known in knownCustomerNames) {
            if (known.isNotBlank() && trimmed.contains(known, ignoreCase = true)) {
                extractedName = known
                break
            }
        }

        if (extractedName.isEmpty()) {
            // Remove known stop words and numbers to isolate the name
            val tokens = trimmed.split(Regex("[\\s,।]+")).filter { it.isNotBlank() }
            val nonNameWords = setOf(
                "টাকা", "টাকার", "টাকায়", "বাকি", "বাকী", "ধার", "জমা", "পরিশোধ", "শোধ",
                "দিল", "দিলো", "নিল", "নিলো", "নগদ", "ক্যাশ", "বিক্রি", "taka", "tk", "baki",
                "joma", "paid", "due", "credit", "cash", "er", "এর",
                "আড়াই", "দেড়", "হাজার", "পনেরোশো", "বারোশো", "নয়শো", "আটশো", "সাতশো", "ছয়শো",
                "পাঁচশো", "পাঁচশত", "চারশো", "তিনশো", "আড়াইশো", "দুইশো", "দুশো", "দেড়শো", "একশো", "একশত", "একশ", "পঞ্চাশ"
            )

            val nameCandidates = mutableListOf<String>()
            for (token in tokens) {
                // If it's a number, skip
                val cleanToken = toAsciiDigits(token).replace(Regex("[^0-9]"), "")
                if (cleanToken.isNotEmpty()) continue
                if (nonNameWords.contains(token.lowercase())) continue

                nameCandidates.add(token)
            }

            extractedName = if (nameCandidates.isNotEmpty()) {
                nameCandidates.take(2).joinToString(" ")
            } else {
                "ক্রেতা"
            }
        }

        return ParsedVoiceResult(
            customerName = extractedName.trim(),
            amount = extractedAmount,
            type = type,
            rawText = trimmed,
            isConfident = true
        )
    }

    private fun containsAny(text: String, vararg keywords: String): Boolean {
        for (kw in keywords) {
            if (text.contains(kw, ignoreCase = true)) return true
        }
        return false
    }

    private fun extractNumericAmount(text: String): Double {
        val asciiText = toAsciiDigits(text)
        // Look for digit patterns like 450, 1,200, 50.00
        val regex = Regex("\\b\\d+(?:,\\d+)*(?:\\.\\d+)?\\b")
        val matches = regex.findAll(asciiText).toList()
        if (matches.isNotEmpty()) {
            // Usually the amount is the first or last number mentioned
            for (m in matches) {
                val cleaned = m.value.replace(",", "")
                val num = cleaned.toDoubleOrNull()
                if (num != null && num > 0) {
                    return num
                }
            }
        }
        return 0.0
    }

    private fun extractWordAmount(text: String): Double {
        val lower = text.lowercase()
        val wordValues = mapOf(
            "আড়াই হাজার" to 2500.0,
            "দেড় হাজার" to 1500.0,
            "দুই হাজার" to 2000.0,
            "এক হাজার" to 1000.0,
            "হাজার" to 1000.0,
            "পনেরোশো" to 1500.0,
            "বারোশো" to 1200.0,
            "এক হাজার" to 1000.0,
            "নয়শো" to 900.0,
            "আটশো" to 800.0,
            "সাতশো" to 700.0,
            "ছয়শো" to 600.0,
            "পাঁচশো" to 500.0,
            "পাঁচশত" to 500.0,
            "চারশো" to 400.0,
            "সাড়ে তিনশো" to 350.0,
            "তিনশো" to 300.0,
            "আড়াইশো" to 250.0,
            "দুইশো" to 200.0,
            "দেড়শো" to 150.0,
            "একশো" to 100.0,
            "একশত" to 100.0,
            "পঞ্চাশ" to 50.0,
            "একশ" to 100.0
        )

        for ((word, value) in wordValues) {
            if (lower.contains(word)) {
                return value
            }
        }
        return 0.0
    }
}
