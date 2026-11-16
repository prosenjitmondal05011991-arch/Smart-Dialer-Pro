package com.example.smartdialer.util

object BlocklistMatcher {

    fun normalizeNumber(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return ""
        val hasLeadingPlus = trimmed.startsWith("+")
        val digitsOnly = trimmed.filter { it.isDigit() }
        if (digitsOnly.isEmpty()) return ""
        return if (hasLeadingPlus) "+$digitsOnly" else digitsOnly
    }

    fun digitsOnly(raw: String): String = raw.filter { it.isDigit() }

    /**
     * Matches an incoming caller number against a stored blocked number.
     * Supports exact normalized matching and trailing national subscriber
     * matching (last 10 digits when both numbers contain at least 10 digits)
     * so +8801712345678 and 01712345678 match consistently.
     */
    fun isMatch(incomingRaw: String, blockedRaw: String): Boolean {
        val incomingDigits = digitsOnly(incomingRaw)
        val blockedDigits = digitsOnly(blockedRaw)
        if (incomingDigits.isEmpty() || blockedDigits.isEmpty()) return false

        if (incomingDigits == blockedDigits) return true

        val incomingNorm = normalizeNumber(incomingRaw)
        val blockedNorm = normalizeNumber(blockedRaw)
        if (incomingNorm.isNotEmpty() && incomingNorm == blockedNorm) return true

        if (incomingDigits.length >= 10 && blockedDigits.length >= 10) {
            return incomingDigits.takeLast(10) == blockedDigits.takeLast(10)
        }
        return false
    }
}
