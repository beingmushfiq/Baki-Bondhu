package com.example

import com.example.util.Money
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MoneyTest {

    @Test
    fun `conversion from BDT double to Paisa is exact without floating drift`() {
        val money = Money.fromBdt(1250.50)
        assertEquals(125050L, money.paisa)
        assertEquals(1250.50, money.toBdtDouble(), 0.001)

        val zero = Money.fromBdt(0.0)
        assertEquals(0L, zero.paisa)
        assertTrue(zero.isZero)
    }

    @Test
    fun `arithmetic operations are deterministic and integer based`() {
        val m1 = Money.fromBdt(100.10) // 10010 paisa
        val m2 = Money.fromBdt(200.20) // 20020 paisa
        val sum = m1 + m2
        assertEquals(30030L, sum.paisa)
        assertEquals(300.30, sum.toBdtDouble(), 0.001)

        val diff = m2 - m1
        assertEquals(10010L, diff.paisa)
        assertEquals(100.10, diff.toBdtDouble(), 0.001)

        val product = m1 * 3
        assertEquals(30030L, product.paisa)
    }

    @Test
    fun `bilingual formatting uses non-breaking space and correct numerals`() {
        val wholeMoney = Money.fromBdt(1500.0)
        assertEquals("৳\u00A01,500", wholeMoney.format(isBn = false))
        assertEquals("৳\u00A0১,৫০০", wholeMoney.format(isBn = true))
        assertTrue(wholeMoney.format(isBn = true).contains('\u00A0'))

        val fractionalMoney = Money.fromBdt(450.75)
        assertEquals("৳\u00A0450.75", fractionalMoney.format(isBn = false))
        assertEquals("৳\u00A0৪৫০.৭৫", fractionalMoney.format(isBn = true))

        val negativeMoney = Money.fromBdt(-500.0)
        assertEquals("-৳\u00A0500", negativeMoney.format(isBn = false))
        assertEquals("-৳\u00A0৫০০", negativeMoney.format(isBn = true))
        assertTrue(negativeMoney.isNegative)
        assertFalse(negativeMoney.isPositive)
    }
}
