package com.example.util

import com.example.voice.BanglaVoiceParser
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.abs

/**
 * Immutable monetary representation stored in minor currency units (Paisa).
 * 1 BDT = 100 Paisa.
 *
 * Prevents floating-point representation drift and rounding discrepancies
 * in financial ledger calculations.
 */
data class Money(val paisa: Long = 0L) : Comparable<Money> {

    operator fun plus(other: Money): Money = Money(this.paisa + other.paisa)

    operator fun minus(other: Money): Money = Money(this.paisa - other.paisa)

    operator fun times(multiplier: Long): Money = Money(this.paisa * multiplier)

    operator fun times(multiplier: Int): Money = Money(this.paisa * multiplier)

    operator fun div(divisor: Long): Money = Money(this.paisa / divisor)

    operator fun unaryMinus(): Money = Money(-this.paisa)

    override fun compareTo(other: Money): Int = this.paisa.compareTo(other.paisa)

    val isPositive: Boolean get() = paisa > 0L
    val isNegative: Boolean get() = paisa < 0L
    val isZero: Boolean get() = paisa == 0L

    fun toBdtDouble(): Double = paisa / 100.0

    /**
     * Formats monetary amount with BDT symbol (৳) and non-breaking space (\u00A0).
     *
     * @param isBn If true, converts digits to Bengali numerals (০-৯).
     * @param showDecimalsAlways If false, drops .00 when paisa fraction is zero.
     */
    fun format(isBn: Boolean = true, showDecimalsAlways: Boolean = false): String {
        val absPaisa = abs(paisa)
        val hasFraction = (absPaisa % 100L) != 0L

        val symbols = DecimalFormatSymbols(Locale.US).apply {
            groupingSeparator = ','
            decimalSeparator = '.'
        }

        val pattern = if (hasFraction || showDecimalsAlways) "#,##0.00" else "#,##0"
        val formatter = DecimalFormat(pattern, symbols)
        val formattedNumber = formatter.format(absPaisa / 100.0)

        val prefix = if (paisa < 0L) "-৳\u00A0" else "৳\u00A0"
        val resultEn = "$prefix$formattedNumber"

        return if (isBn) {
            val bnDigits = BanglaVoiceParser.toBanglaDigits(formattedNumber)
            "$prefix$bnDigits"
        } else {
            resultEn
        }
    }

    override fun toString(): String = format(isBn = false, showDecimalsAlways = true)

    companion object {
        val ZERO = Money(0L)

        fun fromPaisa(paisa: Long): Money = Money(paisa)

        fun fromBdt(bdt: Double): Money = Money(Math.round(bdt * 100.0))

        fun fromBdt(bdt: Long): Money = Money(bdt * 100L)

        fun fromBdt(bdt: Int): Money = Money(bdt.toLong() * 100L)

        fun takaToPaisa(taka: Double): Long = Math.round(taka * 100.0)

        fun paisaToTaka(paisa: Long): Double = paisa / 100.0
    }
}
