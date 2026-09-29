package com.kal.cashcounter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CashCounterTest {

    @Test
    fun testIndianNumberFormatting_zero() {
        assertEquals("0", CashCounterFormatter.formatIndianNumber(0L))
        assertEquals("₹0", CashCounterFormatter.formatCurrency(0L))
        assertEquals("0 notes", CashCounterFormatter.formatNotes(0L))
        assertEquals("0 notes", CashCounterFormatter.formatTotalPill(0L, 0L))
    }

    @Test
    fun testIndianNumberFormatting_underThousand() {
        assertEquals("1", CashCounterFormatter.formatIndianNumber(1L))
        assertEquals("500", CashCounterFormatter.formatIndianNumber(500L))
        assertEquals("₹500", CashCounterFormatter.formatCurrency(500L))
        assertEquals("1 note", CashCounterFormatter.formatNotes(1L))
        assertEquals("500 notes", CashCounterFormatter.formatNotes(500L))
    }

    @Test
    fun testIndianNumberFormatting_thousands() {
        assertEquals("1,000", CashCounterFormatter.formatIndianNumber(1000L))
        assertEquals("10,000", CashCounterFormatter.formatIndianNumber(10000L))
        assertEquals("99,999", CashCounterFormatter.formatIndianNumber(99999L))
    }

    @Test
    fun testTotalPillFormatting() {
        assertEquals("0 notes", CashCounterFormatter.formatTotalPill(0L, 0L))
        assertEquals("94 notes + 45 coins", CashCounterFormatter.formatTotalPill(94L, 45L))
        assertEquals("1 note + 1 coin", CashCounterFormatter.formatTotalPill(1L, 1L))
        assertEquals("5 notes", CashCounterFormatter.formatTotalPill(5L, 0L))
        assertEquals("10 coins", CashCounterFormatter.formatTotalPill(0L, 10L))
        assertEquals("1 note", CashCounterFormatter.formatTotalPill(1L, 0L))
        assertEquals("1 coin", CashCounterFormatter.formatTotalPill(0L, 1L))
    }

    @Test
    fun testIndianNumberFormatting_lakhs() {
        assertEquals("1,00,000", CashCounterFormatter.formatIndianNumber(100000L))
        assertEquals("19,99,980", CashCounterFormatter.formatIndianNumber(1999980L))
        assertEquals("₹19,99,980", CashCounterFormatter.formatCurrency(1999980L))
        assertEquals("4,99,99,500", CashCounterFormatter.formatIndianNumber(49999500L))
        assertEquals("₹4,99,99,500", CashCounterFormatter.formatCurrency(49999500L))
    }

    @Test
    fun testIndianNumberFormatting_crores() {
        assertEquals("8,79,99,120", CashCounterFormatter.formatIndianNumber(87999120L))
        assertEquals("₹8,79,99,120", CashCounterFormatter.formatCurrency(87999120L))
        assertEquals("5,99,994 notes", CashCounterFormatter.formatNotes(599994L))
    }

    @Test
    fun testCashDenomination_subtotalAndCoins() {
        val d500 = CashDenomination(500, 10L)
        assertEquals(5000L, d500.subtotal)
        assertFalse(d500.isCoin)

        val d5 = CashDenomination(5, 20L)
        assertEquals(100L, d5.subtotal)
        assertTrue(d5.isCoin)

        val d2 = CashDenomination(2, 50L)
        assertEquals(100L, d2.subtotal)
        assertTrue(d2.isCoin)

        val d1 = CashDenomination(1, 100L)
        assertEquals(100L, d1.subtotal)
        assertTrue(d1.isCoin)
    }

    @Test
    fun testInitialState_emptyAtZero() {
        val vm = CashCounterViewModel()
        assertEquals(0L, vm.totalNotes)
        assertEquals(0L, vm.totalCoins)
        assertEquals(0L, vm.totalPieces)
        assertEquals(0L, vm.grandTotal)
        assertNull(vm.activeDenomination.value)
        val denoms = vm.denominations.value
        assertEquals(9, denoms.size)
        denoms.forEach { assertEquals(0L, it.count) }
    }

    @Test
    fun testNextDenominationNavigation() {
        val vm = CashCounterViewModel()
        assertEquals(200, vm.getNextDenomination(500))
        assertEquals(100, vm.getNextDenomination(200))
        assertEquals(50, vm.getNextDenomination(100))
        assertEquals(20, vm.getNextDenomination(50))
        assertEquals(10, vm.getNextDenomination(20))
        assertEquals(5, vm.getNextDenomination(10))
        assertEquals(2, vm.getNextDenomination(5))
        assertEquals(1, vm.getNextDenomination(2))
        assertNull(vm.getNextDenomination(1))
    }

    @Test
    fun testViewModel_digitEntryAndArithmetic() {
        val vm = CashCounterViewModel()
        // Type 5, then 0 -> 50
        val hit1 = vm.appendDigit(500, 5, isFresh = true)
        assertFalse(hit1)
        assertEquals(5L, vm.denominations.value.first { it.value == 500 }.count)
        val hit2 = vm.appendDigit(500, 0, isFresh = false)
        assertFalse(hit2)
        assertEquals(50L, vm.denominations.value.first { it.value == 500 }.count)

        // Increment
        vm.increment(500)
        assertEquals(51L, vm.denominations.value.first { it.value == 500 }.count)

        // Decrement
        vm.decrement(500)
        assertEquals(50L, vm.denominations.value.first { it.value == 500 }.count)

        // Backspace
        vm.backspace(500, isFresh = false)
        assertEquals(5L, vm.denominations.value.first { it.value == 500 }.count)

        // Clear active
        vm.clearActive(500)
        assertEquals(0L, vm.denominations.value.first { it.value == 500 }.count)

        // Decrement at 0 should not go below 0
        vm.decrement(500)
        assertEquals(0L, vm.denominations.value.first { it.value == 500 }.count)
    }

    @Test
    fun testViewModel_coinsAndNotesGrandTotal() {
        val vm = CashCounterViewModel()
        // 10 notes of 500 = 5,000
        vm.appendDigit(500, 1)
        vm.appendDigit(500, 0)
        // 5 notes of 200 = 1,000
        vm.appendDigit(200, 5)
        // 20 coins of 5 = 100
        vm.appendDigit(5, 2)
        vm.appendDigit(5, 0)

        assertEquals(15L, vm.totalNotes)
        assertEquals(20L, vm.totalCoins)
        assertEquals(35L, vm.totalPieces)
        assertEquals(6100L, vm.grandTotal)
        assertEquals("15 notes + 20 coins", CashCounterFormatter.formatTotalPill(vm.totalNotes, vm.totalCoins))

        // Reset all
        vm.resetAll()
        assertEquals(0L, vm.totalNotes)
        assertEquals(0L, vm.totalCoins)
        assertEquals(0L, vm.grandTotal)
        assertNull(vm.activeDenomination.value)
    }

    @Test
    fun testViewModel_max99999Cap() {
        val vm = CashCounterViewModel()
        // Append 5 nines
        repeat(5) {
            val hit = vm.appendDigit(500, 9)
            assertFalse(hit)
        }
        assertEquals(99999L, vm.denominations.value.first { it.value == 500 }.count)

        // 6th nine should hit limit and NOT exceed 99999
        val hitLimit = vm.appendDigit(500, 9)
        assertTrue(hitLimit)
        assertEquals(99999L, vm.denominations.value.first { it.value == 500 }.count)

        // Increment should also stay at 99999
        vm.increment(500)
        assertEquals(99999L, vm.denominations.value.first { it.value == 500 }.count)
    }

    @Test
    fun testIndianCurrencyWords() {
        assertEquals("Add notes or coins to see the total.", IndianCurrencyWords.format(0L))
        assertEquals("One rupee", IndianCurrencyWords.format(1L))
        assertEquals("Eleven thousand five hundred forty-five rupees", IndianCurrencyWords.format(11545L))
        assertEquals("Eight crore eighty-seven lakh ninety-nine thousand one hundred twelve rupees", IndianCurrencyWords.format(88799112L))
        assertEquals("Four crore ninety-nine lakh ninety-nine thousand five hundred rupees", IndianCurrencyWords.format(49999500L))
    }
}
