package com.kal.utils

import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal

class NumberFormatterTest {

    @Test
    fun testIndianCommaGroupingResult() {
        // Under 1,000 (no commas)
        assertEquals("0", NumberFormatter.formatResult(BigDecimal.ZERO, 6, 10, ",", "."))
        assertEquals("89", NumberFormatter.formatResult(BigDecimal(89), 6, 10, ",", "."))
        assertEquals("999", NumberFormatter.formatResult(BigDecimal(999), 6, 10, ",", "."))

        // Thousands (1,000 to 99,999)
        assertEquals("1,000", NumberFormatter.formatResult(BigDecimal(1000), 6, 10, ",", "."))
        assertEquals("7,921", NumberFormatter.formatResult(BigDecimal(7921), 6, 10, ",", "."))
        assertEquals("8,989", NumberFormatter.formatResult(BigDecimal(8989), 6, 10, ",", "."))
        assertEquals("10,000", NumberFormatter.formatResult(BigDecimal(10000), 6, 10, ",", "."))
        assertEquals("99,999", NumberFormatter.formatResult(BigDecimal(99999), 6, 10, ",", "."))

        // Lakhs (1,00,000 to 99,99,999)
        assertEquals("1,00,000", NumberFormatter.formatResult(BigDecimal(100000), 6, 10, ",", "."))
        assertEquals("8,00,021", NumberFormatter.formatResult(BigDecimal(800021), 6, 10, ",", "."))
        assertEquals("10,00,000", NumberFormatter.formatResult(BigDecimal(1000000), 6, 10, ",", "."))
        assertEquals("12,34,567", NumberFormatter.formatResult(BigDecimal(1234567), 6, 10, ",", "."))

        // Crores (1,00,00,000+)
        assertEquals("1,00,00,000", NumberFormatter.formatResult(BigDecimal(10000000), 6, 10, ",", "."))
        assertEquals("12,34,56,789", NumberFormatter.formatResult(BigDecimal(123456789), 6, 10, ",", "."))
    }

    @Test
    fun testIndianCommaGroupingExpression() {
        assertEquals("8,989 × 89", NumberFormatter.formatExpression("8989 * 89", ",", "."))
        assertEquals("1,00,000 + 50,000", NumberFormatter.formatExpression("100000 + 50000", ",", "."))
    }
}
