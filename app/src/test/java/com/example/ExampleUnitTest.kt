package com.example

import com.example.data.model.TransactionType
import com.example.ui.BakiStrings
import com.example.ui.SmsTone
import com.example.voice.BanglaVoiceParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun banglaDigitsConversion_isCorrect() {
        val ascii = "450"
        val bangla = BanglaVoiceParser.toBanglaDigits(ascii)
        assertEquals("৪৫০", bangla)

        val convertedBack = BanglaVoiceParser.toAsciiDigits(bangla)
        assertEquals("450", convertedBack)
    }

    @Test
    fun voiceParser_extractsBakiSaleCorrectly() {
        val phrase = "রহিম ৪৫০ বাকী"
        val parsed = BanglaVoiceParser.parse(phrase)

        assertEquals("রহিম", parsed.customerName)
        assertEquals(450.0, parsed.amount, 0.01)
        assertEquals(TransactionType.BAKI_SALE, parsed.type)
    }

    @Test
    fun voiceParser_extractsRepaymentCorrectly() {
        val phrase = "করিম ভাই ২০০ টাকা জমা"
        val parsed = BanglaVoiceParser.parse(phrase)

        assertEquals("করিম ভাই", parsed.customerName)
        assertEquals(200.0, parsed.amount, 0.01)
        assertEquals(TransactionType.REPAYMENT, parsed.type)
    }

    @Test
    fun voiceParser_extractsWordNumbersCorrectly() {
        val phrase = "সুমন পাঁচশো পরিশোধ"
        val parsed = BanglaVoiceParser.parse(phrase)

        assertEquals("সুমন", parsed.customerName)
        assertEquals(500.0, parsed.amount, 0.01)
        assertEquals(TransactionType.REPAYMENT, parsed.type)
    }

    @Test
    fun smsTemplate_includesShopkeeperBkashAndAmount() {
        val sms = BakiStrings.buildSmsText(
            customerName = "রহিম",
            amount = 450.0,
            shopName = "বিসমিল্লাহ স্টোর",
            bkashNumber = "01812345678",
            nagadNumber = "01712345678",
            tone = SmsTone.GENTLE,
            isBn = true
        )

        assertTrue(sms.contains("রহিম"))
        assertTrue(sms.contains("৪৫০"))
        assertTrue(sms.contains("01812345678"))
        assertTrue(sms.contains("বিসমিল্লাহ স্টোর"))
    }

    @Test
    fun voiceParser_handlesSpokenHazarAndDues() {
        val phrase = "সালাম ভাই এক হাজার টাকা বাকি"
        val parsed = BanglaVoiceParser.parse(phrase)

        assertEquals("সালাম ভাই", parsed.customerName)
        assertEquals(1000.0, parsed.amount, 0.01)
        assertEquals(TransactionType.BAKI_SALE, parsed.type)
    }
}
