package com.kal.cashcounter

object IndianCurrencyWords {

    private val ones = arrayOf(
        "", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine",
        "ten", "eleven", "twelve", "thirteen", "fourteen", "fifteen", "sixteen", "seventeen",
        "eighteen", "nineteen"
    )

    private val tens = arrayOf(
        "", "", "twenty", "thirty", "forty", "fifty", "sixty", "seventy", "eighty", "ninety"
    )

    private fun below100(n: Long): String {
        return if (n < 20) {
            ones[n.toInt()]
        } else {
            val t = tens[(n / 10).toInt()]
            val rem = (n % 10).toInt()
            if (rem > 0) "$t-${ones[rem]}" else t
        }
    }

    private fun below1000(n: Long): String {
        val h = n / 100
        val r = n % 100
        val parts = mutableListOf<String>()
        if (h > 0) {
            parts.add("${ones[h.toInt()]} hundred")
        }
        if (r > 0) {
            parts.add(below100(r))
        }
        return parts.joinToString(" ")
    }

    fun format(n: Long): String {
        if (n <= 0L) return "Add notes or coins to see the total."

        val cr = n / 10_000_000L
        val lk = (n % 10_000_000L) / 100_000L
        val th = (n % 100_000L) / 1_000L
        val rest = n % 1_000L

        val parts = mutableListOf<String>()
        if (cr > 0) parts.add("${below100(cr)} crore")
        if (lk > 0) parts.add("${below100(lk)} lakh")
        if (th > 0) parts.add("${below100(th)} thousand")
        if (rest > 0) parts.add(below1000(rest))

        val unit = if (n == 1L) "rupee" else "rupees"
        val words = (parts.joinToString(" ") + " $unit").trim()
        return words.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }
}
