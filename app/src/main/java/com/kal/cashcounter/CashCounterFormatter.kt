package com.kal.cashcounter

object CashCounterFormatter {

    /**
     * Formats a number using the Indian Numbering System (Lakhs and Crores).
     * Example:
     * - 0 -> "0"
     * - 500 -> "500"
     * - 1000 -> "1,000"
     * - 100000 -> "1,00,000"
     * - 1999980 -> "19,99,980"
     * - 49999500 -> "4,99,99,500"
     * - 87999120 -> "8,79,99,120"
     */
    fun formatIndianNumber(value: Long): String {
        if (value == 0L) return "0"
        val isNegative = value < 0
        var n = kotlin.math.abs(value)
        val last3 = (n % 1000).toString().padStart(3, '0')
        n /= 1000
        if (n == 0L) {
            val trimmed = (kotlin.math.abs(value) % 1000).toString()
            return if (isNegative) "-$trimmed" else trimmed
        }
        val groups = ArrayList<String>()
        while (n > 0) {
            val group = (n % 100).toString()
            n /= 100
            if (n > 0) {
                groups.add(group.padStart(2, '0'))
            } else {
                groups.add(group)
            }
        }
        groups.reverse()
        val formatted = groups.joinToString(",") + "," + last3
        return if (isNegative) "-$formatted" else formatted
    }

    fun formatCurrency(amount: Long): String {
        return "₹" + formatIndianNumber(amount)
    }

    fun formatNotes(count: Long): String {
        return when (count) {
            0L -> "0 notes"
            1L -> "1 note"
            else -> "${formatIndianNumber(count)} notes"
        }
    }

    /**
     * Formats the combined notes and coins count pill.
     * Example: "94 notes + 45 coins", "1 note + 1 coin", "0 notes"
     */
    fun formatTotalPill(notes: Long, coins: Long): String {
        return when {
            notes == 0L && coins == 0L -> "0 notes"
            coins == 0L -> "${formatIndianNumber(notes)} ${if (notes == 1L) "note" else "notes"}"
            notes == 0L -> "${formatIndianNumber(coins)} ${if (coins == 1L) "coin" else "coins"}"
            else -> "${formatIndianNumber(notes)} ${if (notes == 1L) "note" else "notes"} + ${formatIndianNumber(coins)} ${if (coins == 1L) "coin" else "coins"}"
        }
    }
}
