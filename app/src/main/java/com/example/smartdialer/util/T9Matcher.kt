package com.example.smartdialer.util

import com.example.smartdialer.data.model.Contact
import com.example.smartdialer.data.model.T9Match

object T9Matcher {

    fun charToT9Digit(ch: Char): Char? {
        return when (ch.uppercaseChar()) {
            'A', 'B', 'C' -> '2'
            'D', 'E', 'F' -> '3'
            'G', 'H', 'I' -> '4'
            'J', 'K', 'L' -> '5'
            'M', 'N', 'O' -> '6'
            'P', 'Q', 'R', 'S' -> '7'
            'T', 'U', 'V' -> '8'
            'W', 'X', 'Y', 'Z' -> '9'
            in '0'..'9' -> ch
            else -> null
        }
    }

    fun toT9Sequence(text: String): String {
        val sb = StringBuilder(text.length)
        for (ch in text) {
            val digit = charToT9Digit(ch)
            if (digit != null) sb.append(digit)
        }
        return sb.toString()
    }

    fun normalizeDigits(number: String): String {
        return buildString {
            for (ch in number) {
                if (ch.isDigit() || ch == '+' || ch == '*' || ch == '#') {
                    append(ch)
                }
            }
        }
    }

    fun formatDialDisplay(raw: String): String {
        if (raw.isEmpty()) return ""
        if (raw.contains('*') || raw.contains('#')) return raw
        val clean = raw.filter { it.isDigit() || it == '+' }
        return when {
            clean.startsWith("+880") && clean.length > 4 -> {
                val rest = clean.removePrefix("+880")
                buildString {
                    append("+880 ")
                    append(rest.take(4))
                    if (rest.length > 4) {
                        append(' ')
                        append(rest.drop(4))
                    }
                }
            }
            clean.startsWith("+") && clean.length > 4 -> {
                val prefix = clean.take(3)
                val rest = clean.drop(3).chunked(3).joinToString(" ")
                "$prefix $rest"
            }
            clean.length in 5..11 -> {
                val first = clean.take(4)
                val second = clean.drop(4).take(3)
                val third = clean.drop(7)
                listOf(first, second, third).filter { it.isNotEmpty() }.joinToString(" ")
            }
            else -> clean
        }
    }

    fun matchContacts(queryDigits: String, contacts: List<Contact>, limit: Int = 6): List<T9Match> {
        val cleanQuery = queryDigits.filter { it.isDigit() }
        if (cleanQuery.isEmpty()) return emptyList()

        val matches = mutableListOf<T9Match>()
        for (contact in contacts) {
            var bestScore = 0
            var bestNumber = contact.primaryNumber

            // 1. T9 match on full name or word initials
            val words = contact.displayName.split(Regex("\\s+")).filter { it.isNotEmpty() }
            val initialsT9 = words.mapNotNull { it.firstOrNull()?.let(::charToT9Digit) }.joinToString("")
            val fullNameT9 = toT9Sequence(contact.displayName)

            if (initialsT9.startsWith(cleanQuery)) {
                bestScore = maxOf(bestScore, 100 - (initialsT9.length - cleanQuery.length))
            }
            for ((wordIdx, word) in words.withIndex()) {
                val wordT9 = toT9Sequence(word)
                if (wordT9.startsWith(cleanQuery)) {
                    bestScore = maxOf(bestScore, if (wordIdx == 0) 95 else 85)
                }
            }
            if (fullNameT9.contains(cleanQuery)) {
                val idx = fullNameT9.indexOf(cleanQuery)
                bestScore = maxOf(bestScore, if (idx == 0) 90 else 65)
            }

            // 2. Phone number digit substring match
            val allNumbers = if (contact.phoneNumbers.isNotEmpty()) {
                contact.phoneNumbers.map { it.number }
            } else {
                listOf(contact.primaryNumber)
            }
            for (num in allNumbers) {
                val digitsOnly = num.filter { it.isDigit() }
                if (digitsOnly.startsWith(cleanQuery)) {
                    bestScore = maxOf(bestScore, 88)
                    bestNumber = num
                } else if (digitsOnly.contains(cleanQuery)) {
                    bestScore = maxOf(bestScore, 70)
                    bestNumber = num
                }
            }

            if (bestScore > 0) {
                matches.add(
                    T9Match(
                        contact = contact,
                        matchedNumber = bestNumber,
                        score = bestScore + if (contact.isFavorite) 12 else 0
                    )
                )
            }
        }
        return matches
            .sortedWith(compareByDescending<T9Match> { it.score }.thenBy { it.contact.displayName })
            .take(limit)
    }
}
