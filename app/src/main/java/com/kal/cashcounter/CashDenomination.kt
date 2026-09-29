package com.kal.cashcounter

data class CashDenomination(
    val value: Int,
    val count: Long = 0L
) {
    val subtotal: Long
        get() = value.toLong() * count

    val isCoin: Boolean
        get() = value in intArrayOf(5, 2, 1)
}
