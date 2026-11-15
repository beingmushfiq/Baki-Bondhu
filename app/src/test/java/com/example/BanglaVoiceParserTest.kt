package com.example

import com.example.data.model.TransactionType
import com.example.voice.BanglaVoiceParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BanglaVoiceParserTest {

    private val knownNames = listOf(
        "রহিম চাচা", "করিম ভাই", "হাজী মোহাম্মদ রফিক", "সুমন আহমেদ",
        "জালাল উদ্দিন", "খালেক মিয়া", "আয়নাল হক", "আনোয়ার হোসেন",
        "Rahim", "Karim", "Sumon", "Jalal"
    )

    // --- 1. Bengali Digits Parsing ---

    @Test
    fun `parse Bengali digits for baki sale`() {
        val r1 = BanglaVoiceParser.parse("রহিম ৪৫০ টাকা বাকী", knownNames)
        assertEquals("রহিম", r1.customerName)
        assertEquals(45000L, r1.amountPaisa)
        assertEquals(TransactionType.BAKI_SALE, r1.type)
        assertFalse(r1.isAmountMissing)

        val r2 = BanglaVoiceParser.parse("করিম ভাই ১,২৫০ টাকা বাকি", knownNames)
        assertEquals("করিম ভাই", r2.customerName)
        assertEquals(125000L, r2.amountPaisa)
        assertEquals(TransactionType.BAKI_SALE, r2.type)

        val r3 = BanglaVoiceParser.parse("সুমন ৫০০ বাকি নিলেন", knownNames)
        assertEquals("সুমন", r3.customerName)
        assertEquals(50000L, r3.amountPaisa)
        assertEquals(TransactionType.BAKI_SALE, r3.type)
    }

    @Test
    fun `parse Bengali digits for repayment`() {
        val r1 = BanglaVoiceParser.parse("করিম ভাই ২০০ টাকা জমা", knownNames)
        assertEquals("করিম ভাই", r1.customerName)
        assertEquals(20000L, r1.amountPaisa)
        assertEquals(TransactionType.REPAYMENT, r1.type)

        val r2 = BanglaVoiceParser.parse("আজকে রহিম ৫০০ টাকা পরিশোধ করলেন", knownNames)
        assertEquals("রহিম", r2.customerName)
        assertEquals(50000L, r2.amountPaisa)
        assertEquals(TransactionType.REPAYMENT, r2.type)

        val r3 = BanglaVoiceParser.parse("জালাল উদ্দিন ১০০০ টাকা ফেরত দিল", knownNames)
        assertEquals("জালাল উদ্দিন", r3.customerName)
        assertEquals(100000L, r3.amountPaisa)
        assertEquals(TransactionType.REPAYMENT, r3.type)
    }

    @Test
    fun `parse Bengali digits for cash sale`() {
        val r1 = BanglaVoiceParser.parse("রহিম চাচা ৩০০ টাকা নগদ বিক্রি", knownNames)
        assertEquals("রহিম চাচা", r1.customerName)
        assertEquals(30000L, r1.amountPaisa)
        assertEquals(TransactionType.CASH_SALE, r1.type)

        val r2 = BanglaVoiceParser.parse("ক্যাশ বিক্রি ৫০০ টাকা", knownNames)
        assertEquals(50000L, r2.amountPaisa)
        assertEquals(TransactionType.CASH_SALE, r2.type)
    }

    // --- 2. Spoken Bengali Number Words ---

    @Test
    fun `parse spoken words for hundreds`() {
        val p1 = BanglaVoiceParser.parse("রহিম একশো টাকা বাকি", knownNames)
        assertEquals(10000L, p1.amountPaisa)

        val p2 = BanglaVoiceParser.parse("করিম দুইশো টাকা জমা", knownNames)
        assertEquals(20000L, p2.amountPaisa)

        val p3 = BanglaVoiceParser.parse("সুমন তিনশো টাকা বাকী", knownNames)
        assertEquals(30000L, p3.amountPaisa)

        val p4 = BanglaVoiceParser.parse("জালাল চারশো টাকা পরিশোধ", knownNames)
        assertEquals(40000L, p4.amountPaisa)

        val p5 = BanglaVoiceParser.parse("রহিম পাঁচশো টাকা বাকি", knownNames)
        assertEquals(50000L, p5.amountPaisa)

        val p6 = BanglaVoiceParser.parse("খালেক ছয়শো টাকা জমা", knownNames)
        assertEquals(60000L, p6.amountPaisa)

        val p7 = BanglaVoiceParser.parse("আনোয়ার সাতশো টাকা বাকি", knownNames)
        assertEquals(70000L, p7.amountPaisa)

        val p8 = BanglaVoiceParser.parse("আয়নাল আটশো টাকা জমা", knownNames)
        assertEquals(80000L, p8.amountPaisa)

        val p9 = BanglaVoiceParser.parse("করিম নয়শো টাকা পরিশোধ", knownNames)
        assertEquals(90000L, p9.amountPaisa)
    }

    @Test
    fun `parse spoken words for thousands and compounds`() {
        val p1 = BanglaVoiceParser.parse("হাজী মোহাম্মদ রফিক এক হাজার টাকা বাকি", knownNames)
        assertEquals(100000L, p1.amountPaisa)
        assertEquals("হাজী মোহাম্মদ রফিক", p1.customerName)

        val p2 = BanglaVoiceParser.parse("সুমন আহমেদ দেড় হাজার টাকা পরিশোধ", knownNames)
        assertEquals(150000L, p2.amountPaisa)
        assertEquals("সুমন আহমেদ", p2.customerName)
        assertEquals(TransactionType.REPAYMENT, p2.type)

        val p3 = BanglaVoiceParser.parse("করিম ভাই দুই হাজার টাকা বাকি", knownNames)
        assertEquals(200000L, p3.amountPaisa)

        val p4 = BanglaVoiceParser.parse("রহিম আড়াই হাজার টাকা জমা", knownNames)
        assertEquals(250000L, p4.amountPaisa)

        val p5 = BanglaVoiceParser.parse("জালাল বারোশো টাকা বাকী", knownNames)
        assertEquals(120000L, p5.amountPaisa)

        val p6 = BanglaVoiceParser.parse("সুমন পনেরোশো টাকা জমা", knownNames)
        assertEquals(150000L, p6.amountPaisa)

        val p7 = BanglaVoiceParser.parse("খালেক আড়াইশো টাকা বাকি", knownNames)
        assertEquals(25000L, p7.amountPaisa)

        val p8 = BanglaVoiceParser.parse("করিম দেড়শো টাকা পরিশোধ", knownNames)
        assertEquals(15000L, p8.amountPaisa)
    }

    // --- 3. English and Banglish Formats ---

    @Test
    fun `parse English and Banglish phrases`() {
        val b1 = BanglaVoiceParser.parse("Rahim 450 credit", knownNames)
        assertEquals("Rahim", b1.customerName)
        assertEquals(45000L, b1.amountPaisa)
        assertEquals(TransactionType.BAKI_SALE, b1.type)

        val b2 = BanglaVoiceParser.parse("Karim paid 200", knownNames)
        assertEquals("Karim", b2.customerName)
        assertEquals(20000L, b2.amountPaisa)
        assertEquals(TransactionType.REPAYMENT, b2.type)

        val b3 = BanglaVoiceParser.parse("Sumon cash 1200", knownNames)
        assertEquals("Sumon", b3.customerName)
        assertEquals(120000L, b3.amountPaisa)
        assertEquals(TransactionType.CASH_SALE, b3.type)

        val b4 = BanglaVoiceParser.parse("Jalal 500 baki", knownNames)
        assertEquals("Jalal", b4.customerName)
        assertEquals(50000L, b4.amountPaisa)
        assertEquals(TransactionType.BAKI_SALE, b4.type)
    }

    // --- 4. Critical Safety: Zero Fabrication on Missing Amount ---

    @Test
    fun `missing amount never defaults to 100 or fabricated number`() {
        val result = BanglaVoiceParser.parse("রহিম ভাই বাকি নিলেন", knownNames)
        assertEquals("রহিম ভাই", result.customerName)
        assertNull("Missing amount must be null", result.amountPaisa)
        assertTrue("isAmountMissing must be true", result.isAmountMissing)
        assertFalse("Cannot be confident without amount", result.isConfident)
    }

    @Test
    fun `empty or blank input returns missing amount`() {
        val emptyResult = BanglaVoiceParser.parse("")
        assertNull(emptyResult.amountPaisa)
        assertTrue(emptyResult.isAmountMissing)

        val spaceResult = BanglaVoiceParser.parse("   ")
        assertNull(spaceResult.amountPaisa)
        assertTrue(spaceResult.isAmountMissing)
    }

    // --- 5. Honorific Attachment Stripping ---

    @Test
    fun `strips Bengali case markers from customer name`() {
        val r1 = BanglaVoiceParser.parse("রহিমকে ৫০০ টাকা বাকি", emptyList())
        assertEquals("রহিম", r1.customerName)
        assertEquals(50000L, r1.amountPaisa)

        val r2 = BanglaVoiceParser.parse("করিমের ৩০০ টাকা জমা", emptyList())
        assertEquals("করিম", r2.customerName)
        assertEquals(30000L, r2.amountPaisa)
    }
}
