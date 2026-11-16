package com.example

import com.example.smartdialer.data.model.Contact
import com.example.smartdialer.data.model.PhoneNumberEntry
import com.example.smartdialer.util.T9Matcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class T9MatcherUnitTest {

    private val sampleContacts = listOf(
        Contact(
            id = 1L,
            lookupKey = "k1",
            displayName = "Sadia Akter",
            primaryNumber = "+880 1712 345678",
            phoneNumbers = listOf(
                PhoneNumberEntry("+880 1712 345678", "+8801712345678", "Mobile", true)
            ),
            isStarred = true
        ),
        Contact(
            id = 2L,
            lookupKey = "k2",
            displayName = "Rakib Hasan",
            primaryNumber = "01711000001",
            phoneNumbers = listOf(
                PhoneNumberEntry("01711000001", "01711000001", "Mobile", true)
            )
        )
    )

    @Test
    fun `t9 matches name by keypad digits`() {
        // SADIA -> 72342
        val matches = T9Matcher.matchContacts("7234", sampleContacts)
        assertTrue(matches.isNotEmpty())
        assertEquals("Sadia Akter", matches.first().contact.displayName)
    }

    @Test
    fun `t9 matches phone number digits`() {
        val matches = T9Matcher.matchContacts("01711", sampleContacts)
        assertEquals(1, matches.size)
        assertEquals("Rakib Hasan", matches.first().contact.displayName)
    }

    @Test
    fun `format dial display groups digits cleanly`() {
        assertEquals("0171 234 5678", T9Matcher.formatDialDisplay("01712345678"))
        assertEquals("*121#", T9Matcher.formatDialDisplay("*121#"))
    }
}
