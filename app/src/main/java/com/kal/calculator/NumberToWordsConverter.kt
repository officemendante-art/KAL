package com.kal.calculator

import java.math.BigDecimal
import java.math.BigInteger
import java.math.MathContext

/**
 * Converts numbers into the Indian Numbering System (INR) words using Lakhs and Crores.
 */
object NumberToWordsConverter {

    private val ONES = arrayOf(
        "Zero", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine",
        "Ten", "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen", "Seventeen", "Eighteen", "Nineteen"
    )

    private val TENS = arrayOf(
        "", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Eighty", "Ninety"
    )

    private val DIGIT_WORDS = arrayOf(
        "Zero", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine"
    )

    private val ONE_THOUSAND = BigInteger.valueOf(1000)
    private val ONE_LAKH = BigInteger.valueOf(100_000)
    private val ONE_CRORE = BigInteger.valueOf(10_000_000)

    /**
     * Converts a BigDecimal number to formal capitalized Indian English words (INR standard).
     * Returns null if number is invalid or exceeds bounds.
     */
    fun convert(number: BigDecimal): String {
        val sign = number.signum()
        if (sign == 0 && number.scale() <= 0) {
            return "Zero"
        }

        val prefix = if (sign < 0) "Negative " else ""
        val absNumber = number.abs()

        val integerPart = absNumber.toBigInteger()
        val intWords = if (integerPart == BigInteger.ZERO) {
            "Zero"
        } else {
            convertBigInteger(integerPart)
        }

        // Process decimal part if present
        val plainStr = absNumber.stripTrailingZeros().toPlainString()
        val decimalIndex = plainStr.indexOf('.')
        val decimalWords = if (decimalIndex != -1 && decimalIndex < plainStr.length - 1) {
            val decimals = plainStr.substring(decimalIndex + 1)
            val decWords = decimals.map { ch ->
                if (ch.isDigit()) DIGIT_WORDS[ch - '0'] else ""
            }.filter { it.isNotEmpty() }.joinToString(" ")
            if (decWords.isNotEmpty()) " Point $decWords" else ""
        } else {
            ""
        }

        return prefix + intWords + decimalWords
    }

    /**
     * Converts a clean string representation of a number to Indian words.
     * Returns null if unparseable, infinite, or NaN.
     */
    fun convertString(numStr: String): String? {
        val clean = numStr.trim().replace(",", "").replace(" ", "")
        if (clean.isEmpty() || clean.equals("error", ignoreCase = true) ||
            clean.equals("nan", ignoreCase = true) || clean.contains("infinity", ignoreCase = true) ||
            clean.contains("too high", ignoreCase = true)
        ) {
            return null
        }
        return try {
            val bd = BigDecimal(clean)
            convert(bd)
        } catch (e: Exception) {
            null
        }
    }

    private fun convertBigInteger(n: BigInteger): String {
        if (n == BigInteger.ZERO) return "Zero"

        val parts = mutableListOf<String>()

        val crore = n.divide(ONE_CRORE)
        val remCrore = n.mod(ONE_CRORE)

        if (crore > BigInteger.ZERO) {
            parts.add(convertBigInteger(crore) + " Crore")
        }

        val lakh = remCrore.divide(ONE_LAKH).toInt()
        val remLakh = remCrore.mod(ONE_LAKH)

        if (lakh > 0) {
            parts.add(convertUnderThousand(lakh) + " Lakh")
        }

        val thousand = remLakh.divide(ONE_THOUSAND).toInt()
        val remThousand = remLakh.mod(ONE_THOUSAND).toInt()

        if (thousand > 0) {
            parts.add(convertUnderThousand(thousand) + " Thousand")
        }

        if (remThousand > 0) {
            parts.add(convertUnderThousand(remThousand))
        }

        return parts.joinToString(" ")
    }

    private fun convertUnderThousand(n: Int): String {
        val hundreds = n / 100
        val rem = n % 100

        val result = StringBuilder()
        if (hundreds > 0) {
            result.append(ONES[hundreds]).append(" Hundred")
            if (rem > 0) {
                result.append(" ")
            }
        }

        if (rem in 1..19) {
            result.append(ONES[rem])
        } else if (rem >= 20) {
            val tens = rem / 10
            val units = rem % 10
            if (units > 0) {
                result.append(TENS[tens]).append("-").append(ONES[units])
            } else {
                result.append(TENS[tens])
            }
        }

        return result.toString()
    }

    data class WordsResult(val words: String, val isPending: Boolean)

    /**
     * Resolves the clean evaluated words for the expression, safely handling trailing operators
     * (e.g. "9,86,55,555 +") by retaining the evaluated subtotal so the words strip never vanishes.
     */
    fun resolveResultWords(expression: String, resultStr: String?): WordsResult? {
        val cleanExpr = expression.trim()
        if (cleanExpr.isEmpty()) return null

        // 1. If resultStr is non-empty and valid from calculation engine (e.g. "69,52,560")
        if (!resultStr.isNullOrBlank()) {
            val words = convertString(resultStr)
            if (words != null) {
                return WordsResult(words, isPending = false)
            }
        }

        // 2. Direct single number entry without operators (e.g. "9,86,55,555")
        val directWords = convertString(cleanExpr)
        if (directWords != null) {
            return WordsResult(directWords, isPending = false)
        }

        // 3. Expression with trailing operator(s) (e.g. "9,86,55,555 +") or compound expression
        val strippedExpr = stripTrailingOperators(cleanExpr)
        if (strippedExpr.isNotEmpty()) {
            val isPending = strippedExpr != cleanExpr

            // Single number before trailing operator (e.g. "9,86,55,555 +")
            val prefixSingle = convertString(strippedExpr)
            if (prefixSingle != null) {
                return WordsResult(prefixSingle, isPending = isPending)
            }

            // Compound expression before trailing operator (e.g. "10 + 20 +")
            val evalPrefix = evaluateSimpleMath(strippedExpr)
            if (evalPrefix != null) {
                val words = convert(evalPrefix)
                return WordsResult(words, isPending = isPending)
            }
        }

        return null
    }

    private fun stripTrailingOperators(expr: String): String {
        var s = expr.trim()
        val ops = arrayOf("+", "-", "–", "×", "÷", "*", "/", "^", "%")
        var changed = true
        while (changed && s.isNotEmpty()) {
            changed = false
            for (op in ops) {
                if (s.endsWith(op)) {
                    s = s.substring(0, s.length - op.length).trim()
                    changed = true
                    break
                }
            }
        }
        return s
    }

    private fun evaluateSimpleMath(expr: String): BigDecimal? {
        return try {
            val clean = expr.replace(",", "").replace(" ", "").replace("–", "-")
            val tokens = mutableListOf<String>()
            val numBuf = StringBuilder()
            var i = 0
            while (i < clean.length) {
                val c = clean[i]
                if (c.isDigit() || c == '.') {
                    numBuf.append(c)
                } else if (c == '-' && (numBuf.isEmpty() && (tokens.isEmpty() || tokens.last() in listOf("+", "-", "×", "*", "÷", "/")))) {
                    numBuf.append(c)
                } else if (c in listOf('+', '-', '×', '*', '÷', '/')) {
                    if (numBuf.isNotEmpty()) {
                        tokens.add(numBuf.toString())
                        numBuf.clear()
                    }
                    tokens.add(c.toString())
                }
                i++
            }
            if (numBuf.isNotEmpty()) {
                tokens.add(numBuf.toString())
            }
            if (tokens.isEmpty() || tokens.size % 2 == 0) return null

            // First pass: multiplication and division
            val pass1 = mutableListOf<String>()
            var j = 0
            while (j < tokens.size) {
                val token = tokens[j]
                if (token == "×" || token == "*" || token == "÷" || token == "/") {
                    val prev = BigDecimal(pass1.removeAt(pass1.size - 1))
                    val next = BigDecimal(tokens[j + 1])
                    val res = if (token == "÷" || token == "/") {
                        if (next.compareTo(BigDecimal.ZERO) == 0) return null
                        prev.divide(next, MathContext.DECIMAL64)
                    } else {
                        prev.multiply(next, MathContext.DECIMAL64)
                    }
                    pass1.add(res.stripTrailingZeros().toPlainString())
                    j += 2
                } else {
                    pass1.add(token)
                    j++
                }
            }

            // Second pass: addition and subtraction
            var total = BigDecimal(pass1[0])
            var k = 1
            while (k < pass1.size) {
                val op = pass1[k]
                val next = BigDecimal(pass1[k + 1])
                total = if (op == "+") total.add(next, MathContext.DECIMAL64) else total.subtract(next, MathContext.DECIMAL64)
                k += 2
            }
            total.stripTrailingZeros()
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Formats an equation and evaluated result into a single sentence preview.
     * Handles formatted numbers with commas (e.g. "8,989 × 89" with result "8,00,021"
     * -> "Eight Thousand Nine Hundred Eighty-Nine × Eighty-Nine = Eight Lakh Twenty-One").
     */
    fun formatPreviewEquation(expression: String, resultStr: String?): String? {
        val resultWords = if (!resultStr.isNullOrBlank()) {
            convertString(resultStr)
        } else null

        val cleanExpr = expression.trim()
        if (cleanExpr.isEmpty()) {
            return null
        }

        // Tokenize expression: match number tokens (with optional commas within) and decimals
        val regex = Regex("""\d[\d,]*(\.\d+)?""")
        val hasOperators = cleanExpr.any { it == '+' || it == '-' || it == '–' || it == '×' || it == '÷' || it == '*' || it == '/' || it == '^' }

        if (!hasOperators) {
            // Just a single number entry
            return resultWords ?: convertString(cleanExpr)
        }

        if (resultWords == null) {
            return null
        }

        // Format expression tokens into words while preserving operators
        val formattedExpr = StringBuilder()
        var lastIdx = 0

        for (match in regex.findAll(cleanExpr)) {
            val start = match.range.first
            val end = match.range.last + 1

            // Append operator/spacing preceding the number
            if (start > lastIdx) {
                val op = cleanExpr.substring(lastIdx, start).trim()
                if (op.isNotEmpty()) {
                    if (formattedExpr.isNotEmpty() && !formattedExpr.endsWith(" ")) {
                        formattedExpr.append(" ")
                    }
                    formattedExpr.append(op).append(" ")
                }
            }

            // Convert number token to words (stripping any formatting commas)
            val numToken = match.value
            val numWords = convertString(numToken) ?: numToken
            if (formattedExpr.isNotEmpty() && !formattedExpr.endsWith(" ")) {
                formattedExpr.append(" ")
            }
            formattedExpr.append(numWords)
            lastIdx = end
        }

        // Append trailing operators if any
        if (lastIdx < cleanExpr.length) {
            val trailing = cleanExpr.substring(lastIdx).trim()
            if (trailing.isNotEmpty()) {
                formattedExpr.append(" ").append(trailing)
            }
        }

        return "$formattedExpr = $resultWords"
    }
}
