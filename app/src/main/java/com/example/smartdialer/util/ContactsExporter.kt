package com.example.smartdialer.util

import android.content.ContentResolver
import android.net.Uri
import com.example.smartdialer.data.model.Contact
import com.example.smartdialer.data.model.PhoneNumberEntry
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class ContactExportFormat(
    val extension: String,
    val mimeType: String
) {
    VCF("vcf", "text/x-vcard"),
    CSV("csv", "text/csv")
}

object ContactsExporter {

    fun defaultExportFileName(
        format: ContactExportFormat,
        timestampMillis: Long = System.currentTimeMillis()
    ): String {
        val formatter = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US)
        val stamp = formatter.format(Date(timestampMillis))
        return "smart_dialer_contacts_$stamp.${format.extension}"
    }

    fun formatContacts(contacts: List<Contact>, format: ContactExportFormat): String {
        return when (format) {
            ContactExportFormat.CSV -> formatAsCsv(contacts)
            ContactExportFormat.VCF -> formatAsVcf(contacts)
        }
    }

    fun formatAsCsv(contacts: List<Contact>): String {
        val sb = StringBuilder()
        sb.append("Name,Phone Numbers,Emails,Organization,Favorite\r\n")
        contacts.forEach { contact ->
            val numbers = resolvedPhoneEntries(contact)
                .map { it.number.trim() }
                .filter { it.isNotEmpty() }
                .distinct()
                .joinToString("; ")
            val emails = contact.emails
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .distinct()
                .joinToString("; ")
            val org = contact.organization?.trim().orEmpty()
            val fav = if (contact.isFavorite) "Yes" else "No"

            sb.append(escapeCsvField(contact.displayName.trim()))
                .append(',')
                .append(escapeCsvField(numbers))
                .append(',')
                .append(escapeCsvField(emails))
                .append(',')
                .append(escapeCsvField(org))
                .append(',')
                .append(escapeCsvField(fav))
                .append("\r\n")
        }
        return sb.toString()
    }

    fun formatAsVcf(contacts: List<Contact>): String {
        val sb = StringBuilder()
        contacts.forEachIndexed { index, contact ->
            val name = contact.displayName.trim().ifEmpty { contact.primaryNumber.trim() }
            if (name.isEmpty()) return@forEachIndexed

            sb.append("BEGIN:VCARD\r\n")
            sb.append("VERSION:3.0\r\n")
            sb.append("FN:").append(escapeVCardValue(name)).append("\r\n")
            sb.append("N:").append(buildStructuredName(name)).append("\r\n")

            resolvedPhoneEntries(contact).forEach { entry ->
                val raw = entry.number.trim()
                if (raw.isNotEmpty()) {
                    val vcardType = mapPhoneLabelToVCardType(entry.label)
                    sb.append("TEL;TYPE=").append(vcardType).append(':').append(raw).append("\r\n")
                }
            }

            contact.emails.map { it.trim() }.filter { it.isNotEmpty() }.distinct().forEach { email ->
                sb.append("EMAIL;TYPE=INTERNET:").append(escapeVCardValue(email)).append("\r\n")
            }

            contact.organization?.trim()?.takeIf { it.isNotEmpty() }?.let { org ->
                sb.append("ORG:").append(escapeVCardValue(org)).append("\r\n")
            }

            sb.append("END:VCARD")
            if (index < contacts.lastIndex) {
                sb.append("\r\n")
            }
        }
        return sb.toString()
    }

    fun writeExportToUri(
        contentResolver: ContentResolver,
        uri: Uri,
        content: String
    ): Result<Unit> {
        return runCatching {
            val stream = contentResolver.openOutputStream(uri, "wt")
                ?: throw IOException("Unable to open destination file")
            stream.use { output ->
                output.write(content.toByteArray(Charsets.UTF_8))
                output.flush()
            }
        }
    }

    private fun resolvedPhoneEntries(contact: Contact): List<PhoneNumberEntry> {
        return contact.phoneNumbers.ifEmpty {
            val primary = contact.primaryNumber.trim()
            if (primary.isEmpty()) {
                emptyList()
            } else {
                listOf(
                    PhoneNumberEntry(
                        number = primary,
                        normalizedNumber = primary.filter { it.isDigit() || it == '+' },
                        label = "Mobile",
                        isPrimary = true
                    )
                )
            }
        }
    }

    private fun escapeCsvField(value: String): String {
        val needsQuotes = value.contains(',') ||
            value.contains('"') ||
            value.contains('\n') ||
            value.contains('\r')
        return if (needsQuotes) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }

    private fun escapeVCardValue(value: String): String {
        return value
            .replace("\\", "\\\\")
            .replace("\r\n", "\\n")
            .replace("\n", "\\n")
            .replace("\r", "\\n")
            .replace(";", "\\;")
            .replace(",", "\\,")
    }

    private fun buildStructuredName(displayName: String): String {
        val parts = displayName.split(Regex("\\s+")).filter { it.isNotEmpty() }
        return if (parts.size >= 2) {
            val family = escapeVCardValue(parts.last())
            val given = escapeVCardValue(parts.dropLast(1).joinToString(" "))
            "$family;$given;;;"
        } else {
            ";${escapeVCardValue(displayName)};;;"
        }
    }

    private fun mapPhoneLabelToVCardType(label: String): String {
        val lower = label.lowercase(Locale.US)
        return when {
            lower.contains("mobile") || lower.contains("cell") -> "CELL"
            lower.contains("home") -> "HOME"
            lower.contains("work") || lower.contains("office") -> "WORK"
            lower.contains("fax") -> "FAX"
            else -> "VOICE"
        }
    }
}
