package com.kal.calculator

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.math.BigDecimal

class NumberToWordsConverterTest {

    @Test
    fun testBasicNumbers() {
        assertEquals("Zero", NumberToWordsConverter.convert(BigDecimal.ZERO))
        assertEquals("One", NumberToWordsConverter.convert(BigDecimal(1)))
        assertEquals("Fifteen", NumberToWordsConverter.convert(BigDecimal(15)))
        assertEquals("Twenty", NumberToWordsConverter.convert(BigDecimal(20)))
        assertEquals("Twenty-One", NumberToWordsConverter.convert(BigDecimal(21)))
        assertEquals("Eighty-Nine", NumberToWordsConverter.convert(BigDecimal(89)))
        assertEquals("One Hundred", NumberToWordsConverter.convert(BigDecimal(100)))
        assertEquals("One Hundred Five", NumberToWordsConverter.convert(BigDecimal(105)))
        assertEquals("One Hundred Twenty-Three", NumberToWordsConverter.convert(BigDecimal(123)))
    }

    @Test
    fun testIndianNumberingScales() {
        // Thousands
        assertEquals("Seven Thousand Nine Hundred Twenty-One", NumberToWordsConverter.convert(BigDecimal(7921)))
        assertEquals("Ten Thousand", NumberToWordsConverter.convert(BigDecimal(10000)))
        assertEquals("Eighty-Nine Thousand Seven Hundred Twenty-One", NumberToWordsConverter.convert(BigDecimal(89721)))

        // Lakhs
        assertEquals("One Lakh", NumberToWordsConverter.convert(BigDecimal(100000)))
        assertEquals("Eight Lakh Twenty-One", NumberToWordsConverter.convert(BigDecimal(800021)))
        assertEquals("Ten Lakh", NumberToWordsConverter.convert(BigDecimal(1000000)))
        assertEquals("Twelve Lakh Thirty-Four Thousand Five Hundred Sixty-Seven", NumberToWordsConverter.convert(BigDecimal(1234567)))

        // Crores
        assertEquals("One Crore", NumberToWordsConverter.convert(BigDecimal(10000000)))
        assertEquals("Eighty-Nine Crore", NumberToWordsConverter.convert(BigDecimal(890000000)))
        assertEquals("One Hundred Crore", NumberToWordsConverter.convert(BigDecimal("1000000000")))
        assertEquals("One Hundred Twenty-Three Crore Forty-Five Lakh Sixty-Seven Thousand Eight Hundred Ninety",
            NumberToWordsConverter.convert(BigDecimal("1234567890")))
    }

    @Test
    fun testDecimalsAndNegatives() {
        assertEquals("Eighty-Nine Point Five", NumberToWordsConverter.convert(BigDecimal("89.5")))
        assertEquals("Zero Point Five", NumberToWordsConverter.convert(BigDecimal("0.5")))
        assertEquals("Negative Forty-Two", NumberToWordsConverter.convert(BigDecimal(-42)))
        assertEquals("Negative Eight Lakh Twenty-One", NumberToWordsConverter.convert(BigDecimal(-800021)))
        assertEquals("Negative One Hundred Twenty-Three Point Four Five", NumberToWordsConverter.convert(BigDecimal("-123.45")))
    }

    @Test
    fun testConvertString() {
        assertEquals("Seven Thousand Nine Hundred Twenty-One", NumberToWordsConverter.convertString("7,921"))
        assertEquals("Eight Lakh Twenty-One", NumberToWordsConverter.convertString("8,00,021"))
        assertEquals("Eight Lakh Twenty-One", NumberToWordsConverter.convertString("800,021"))
        assertEquals("One Lakh", NumberToWordsConverter.convertString("1,00,000"))
        assertNull(NumberToWordsConverter.convertString(""))
        assertNull(NumberToWordsConverter.convertString("Error"))
        assertNull(NumberToWordsConverter.convertString("Value is too high"))
        assertNull(NumberToWordsConverter.convertString("NaN"))
        assertNull(NumberToWordsConverter.convertString("Infinity"))
    }

    @Test
    fun testFormatPreviewEquation() {
        val preview = NumberToWordsConverter.formatPreviewEquation("89 × 89", "7,921")
        assertEquals("Eighty-Nine × Eighty-Nine = Seven Thousand Nine Hundred Twenty-One", preview)

        // Equation with comma-formatted inputs and Indian Lakh result
        val previewWithCommas = NumberToWordsConverter.formatPreviewEquation("8,989 × 89", "8,00,021")
        assertEquals("Eight Thousand Nine Hundred Eighty-Nine × Eighty-Nine = Eight Lakh Twenty-One", previewWithCommas)

        val previewSingle = NumberToWordsConverter.formatPreviewEquation("1,00,000", "1,00,000")
        assertEquals("One Lakh", previewSingle)

        val previewPlus = NumberToWordsConverter.formatPreviewEquation("150 + 25", "175")
        assertEquals("One Hundred Fifty + Twenty-Five = One Hundred Seventy-Five", previewPlus)

        assertNull(NumberToWordsConverter.formatPreviewEquation("89 / 0", "Error"))
        assertNull(NumberToWordsConverter.formatPreviewEquation("", "0"))
    }

    @Test
    fun testResolveResultWords() {
        // 1. Single number entry (e.g. while typing initial figure)
        val single = NumberToWordsConverter.resolveResultWords("9,86,55,555", "")
        org.junit.Assert.assertNotNull(single)
        assertEquals("Nine Crore Eighty-Six Lakh Fifty-Five Thousand Five Hundred Fifty-Five", single?.words)
        assertEquals(false, single?.isPending)

        // 2. Trailing operator ("+" entered) - must NOT vanish, evaluates subtotal with isPending = true
        val pendingPlus = NumberToWordsConverter.resolveResultWords("9,86,55,555 +", "")
        org.junit.Assert.assertNotNull(pendingPlus)
        assertEquals("Nine Crore Eighty-Six Lakh Fifty-Five Thousand Five Hundred Fifty-Five", pendingPlus?.words)
        assertEquals(true, pendingPlus?.isPending)

        // 3. Completed expression with evaluated result
        val completed = NumberToWordsConverter.resolveResultWords("9,86,55,555 + 98", "9,86,55,653")
        org.junit.Assert.assertNotNull(completed)
        assertEquals("Nine Crore Eighty-Six Lakh Fifty-Five Thousand Six Hundred Fifty-Three", completed?.words)
        assertEquals(false, completed?.isPending)

        // 4. Compound expression with trailing operator (e.g. "100 + 200 +")
        val compoundPending = NumberToWordsConverter.resolveResultWords("100 + 200 +", "")
        org.junit.Assert.assertNotNull(compoundPending)
        assertEquals("Three Hundred", compoundPending?.words)
        assertEquals(true, compoundPending?.isPending)

        // 5. Empty expression returns null
        assertNull(NumberToWordsConverter.resolveResultWords("", ""))
        assertNull(NumberToWordsConverter.resolveResultWords("   ", null))
    }

    @Test
    fun testFinancialDenominationWordsPattern() {
        val pattern = java.util.regex.Pattern.compile("\\b(Crore|Lakh|Thousand|Hundred|Point)\\b")
        val text = "Six Crore Five Lakh Four Thousand Three Hundred Twenty-One Point Five"
        val matcher = pattern.matcher(text)
        val matchedTokens = mutableListOf<String>()
        while (matcher.find()) {
            matchedTokens.add(matcher.group())
        }
        assertEquals(listOf("Crore", "Lakh", "Thousand", "Hundred", "Point"), matchedTokens)
    }
}
