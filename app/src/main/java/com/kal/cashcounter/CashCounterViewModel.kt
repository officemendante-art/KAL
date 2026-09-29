package com.kal.cashcounter

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class CashCounterViewModel : ViewModel() {

    // 6 Notes + 3 Coins (Indian Rupee system per spec)
    val supportedDenominations = intArrayOf(500, 200, 100, 50, 20, 10, 5, 2, 1)

    private val _denominations = MutableStateFlow<List<CashDenomination>>(
        supportedDenominations.map { CashDenomination(it, 0L) }
    )
    val denominations: StateFlow<List<CashDenomination>> = _denominations.asStateFlow()

    private val _activeDenomination = MutableStateFlow<Int?>(null)
    val activeDenomination: StateFlow<Int?> = _activeDenomination.asStateFlow()

    val totalNotes: Long
        get() = _denominations.value.filter { !it.isCoin }.sumOf { it.count }

    val totalCoins: Long
        get() = _denominations.value.filter { it.isCoin }.sumOf { it.count }

    val totalPieces: Long
        get() = totalNotes + totalCoins

    val grandTotal: Long
        get() = _denominations.value.sumOf { it.subtotal }

    fun setActiveDenomination(value: Int?) {
        _activeDenomination.value = value
    }

    fun getNextDenomination(current: Int): Int? {
        val index = supportedDenominations.indexOf(current)
        if (index in 0 until supportedDenominations.size - 1) {
            return supportedDenominations[index + 1]
        }
        return null
    }

    fun increment(value: Int) {
        updateItem(value) { count ->
            if (count < MAX_COUNT) count + 1 else count
        }
    }

    fun decrement(value: Int) {
        updateItem(value) { count ->
            if (count > 0L) count - 1 else 0L
        }
    }

    fun appendDigit(value: Int, digit: Int, isFresh: Boolean = false): Boolean {
        var limitExceeded = false
        updateItem(value) { count ->
            val base = if (isFresh || count == 0L) "" else count.toString()
            val nextStr = base + digit
            if (nextStr.length > 5 || nextStr.toLong() > MAX_COUNT) {
                limitExceeded = true
                count
            } else {
                nextStr.toLong()
            }
        }
        return limitExceeded
    }

    fun backspace(value: Int, isFresh: Boolean = false) {
        updateItem(value) { count ->
            if (isFresh) 0L else count / 10L
        }
    }

    fun clearActive(value: Int) {
        updateItem(value) { 0L }
    }

    fun resetAll() {
        _denominations.value = supportedDenominations.map { CashDenomination(it, 0L) }
        _activeDenomination.value = null
    }

    private fun updateItem(value: Int, transform: (Long) -> Long) {
        val currentList = _denominations.value
        val updated = currentList.map { item ->
            if (item.value == value) {
                item.copy(count = transform(item.count))
            } else {
                item
            }
        }
        _denominations.value = updated
    }

    companion object {
        const val MAX_COUNT = 99999L
    }
}
