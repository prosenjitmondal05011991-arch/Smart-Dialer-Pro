package com.example.smartdialer.data.repository

import android.content.ContentProviderOperation
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import com.example.smartdialer.data.local.ContactNoteDao
import com.example.smartdialer.data.local.ContactNoteEntity
import com.example.smartdialer.data.local.FavoriteContactDao
import com.example.smartdialer.data.local.FavoriteContactEntity
import com.example.smartdialer.data.model.Contact
import com.example.smartdialer.data.model.ContactDraft
import com.example.smartdialer.data.model.PhoneNumberEntry
import com.example.smartdialer.permissions.PermissionHelper
import com.example.smartdialer.util.T9Matcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class ContactsRepository(
    private val context: Context,
    private val favoriteDao: FavoriteContactDao,
    private val noteDao: ContactNoteDao
) {

    fun observeLocalFavorites(): Flow<List<FavoriteContactEntity>> =
        favoriteDao.observeFavorites()

    fun observeContactNote(lookupKey: String): Flow<String> =
        noteDao.observeNote(lookupKey).map { it?.noteText.orEmpty() }

    suspend fun saveContactNote(lookupKey: String, noteText: String) {
        val trimmed = noteText.trim()
        if (trimmed.isEmpty()) {
            noteDao.deleteNote(lookupKey)
        } else {
            noteDao.upsertNote(
                ContactNoteEntity(
                    lookupKey = lookupKey,
                    noteText = trimmed,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    suspend fun loadDeviceContacts(): Result<List<Contact>> = withContext(Dispatchers.IO) {
        if (!PermissionHelper.hasContactsReadPermission(context)) {
            return@withContext Result.failure(SecurityException("READ_CONTACTS not granted"))
        }

        runCatching {
            val contactsMap = LinkedHashMap<Long, MutableContactBuilder>()
            val projection = arrayOf(
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                ContactsContract.CommonDataKinds.Phone.LOOKUP_KEY,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.TYPE,
                ContactsContract.CommonDataKinds.Phone.LABEL,
                ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI,
                ContactsContract.CommonDataKinds.Phone.STARRED,
                ContactsContract.CommonDataKinds.Phone.IS_PRIMARY
            )

            context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection,
                null,
                null,
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY} COLLATE NOCASE ASC"
            )?.use { cursor ->
                val idIdx = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                val lookupIdx = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.LOOKUP_KEY)
                val nameIdx = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY)
                val numIdx = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
                val typeIdx = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.TYPE)
                val labelIdx = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.LABEL)
                val photoIdx = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI)
                val starredIdx = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.STARRED)
                val primaryIdx = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.IS_PRIMARY)

                while (cursor.moveToNext()) {
                    val contactId = cursor.getLong(idIdx)
                    val lookupKey = cursor.getString(lookupIdx) ?: contactId.toString()
                    val displayName = cursor.getString(nameIdx)?.trim().orEmpty()
                    val rawNumber = cursor.getString(numIdx)?.trim().orEmpty()
                    if (rawNumber.isEmpty()) continue

                    val phoneType = cursor.getInt(typeIdx)
                    val customLabel = cursor.getString(labelIdx)
                    val typeLabel = ContactsContract.CommonDataKinds.Phone.getTypeLabel(
                        context.resources,
                        phoneType,
                        customLabel
                    ).toString()
                    val photoUri = cursor.getString(photoIdx)
                    val starred = cursor.getInt(starredIdx) == 1
                    val isPrimary = cursor.getInt(primaryIdx) == 1
                    val normalized = T9Matcher.normalizeDigits(rawNumber)

                    val builder = contactsMap.getOrPut(contactId) {
                        MutableContactBuilder(
                            id = contactId,
                            lookupKey = lookupKey,
                            displayName = displayName.ifEmpty { rawNumber },
                            photoUri = photoUri,
                            isStarred = starred
                        )
                    }
                    if (builder.numbers.none { it.normalizedNumber == normalized }) {
                        builder.numbers.add(
                            PhoneNumberEntry(
                                number = rawNumber,
                                normalizedNumber = normalized,
                                label = typeLabel,
                                isPrimary = isPrimary
                            )
                        )
                    }
                }
            }

            contactsMap.values.map { it.build() }
        }
    }

    suspend fun loadContactDetail(contactId: Long): Contact? = withContext(Dispatchers.IO) {
        if (!PermissionHelper.hasContactsReadPermission(context)) return@withContext null
        val base = loadDeviceContacts().getOrNull()?.firstOrNull { it.id == contactId } ?: return@withContext null

        val emails = mutableListOf<String>()
        context.contentResolver.query(
            ContactsContract.CommonDataKinds.Email.CONTENT_URI,
            arrayOf(ContactsContract.CommonDataKinds.Email.ADDRESS),
            "${ContactsContract.CommonDataKinds.Email.CONTACT_ID} = ?",
            arrayOf(contactId.toString()),
            null
        )?.use { cursor ->
            val addrIdx = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Email.ADDRESS)
            while (cursor.moveToNext()) {
                val email = cursor.getString(addrIdx)?.trim()
                if (!email.isNullOrEmpty() && email !in emails) {
                    emails.add(email)
                }
            }
        }

        var orgName: String? = null
        context.contentResolver.query(
            ContactsContract.Data.CONTENT_URI,
            arrayOf(ContactsContract.CommonDataKinds.Organization.COMPANY),
            "${ContactsContract.Data.CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?",
            arrayOf(contactId.toString(), ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE),
            null
        )?.use { cursor ->
            val companyIdx = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Organization.COMPANY)
            if (cursor.moveToFirst()) {
                orgName = cursor.getString(companyIdx)?.trim()?.takeIf { it.isNotEmpty() }
            }
        }

        val isLocalFav = favoriteDao.isFavorite(base.lookupKey)
        base.copy(
            emails = emails,
            organization = orgName,
            isLocalFavorite = isLocalFav
        )
    }

    suspend fun loadContactsForExport(): Result<List<Contact>> = withContext(Dispatchers.IO) {
        if (!PermissionHelper.hasContactsReadPermission(context)) {
            return@withContext Result.failure(SecurityException("READ_CONTACTS not granted"))
        }

        val baseContacts = loadDeviceContacts().getOrElse { return@withContext Result.failure(it) }
        if (baseContacts.isEmpty()) {
            return@withContext Result.success(emptyList())
        }

        runCatching {
            val emailsByContact = HashMap<Long, MutableList<String>>()
            context.contentResolver.query(
                ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                arrayOf(
                    ContactsContract.CommonDataKinds.Email.CONTACT_ID,
                    ContactsContract.CommonDataKinds.Email.ADDRESS
                ),
                null,
                null,
                null
            )?.use { cursor ->
                val idIdx = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Email.CONTACT_ID)
                val addrIdx = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Email.ADDRESS)
                while (cursor.moveToNext()) {
                    val contactId = cursor.getLong(idIdx)
                    val email = cursor.getString(addrIdx)?.trim()
                    if (!email.isNullOrEmpty()) {
                        val list = emailsByContact.getOrPut(contactId) { mutableListOf() }
                        if (email !in list) list.add(email)
                    }
                }
            }

            val orgByContact = HashMap<Long, String>()
            context.contentResolver.query(
                ContactsContract.Data.CONTENT_URI,
                arrayOf(
                    ContactsContract.Data.CONTACT_ID,
                    ContactsContract.CommonDataKinds.Organization.COMPANY
                ),
                "${ContactsContract.Data.MIMETYPE} = ?",
                arrayOf(ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE),
                null
            )?.use { cursor ->
                val idIdx = cursor.getColumnIndexOrThrow(ContactsContract.Data.CONTACT_ID)
                val companyIdx = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Organization.COMPANY)
                while (cursor.moveToNext()) {
                    val contactId = cursor.getLong(idIdx)
                    val org = cursor.getString(companyIdx)?.trim()
                    if (!org.isNullOrEmpty() && !orgByContact.containsKey(contactId)) {
                        orgByContact[contactId] = org
                    }
                }
            }

            baseContacts.map { contact ->
                val isLocalFav = favoriteDao.isFavorite(contact.lookupKey)
                contact.copy(
                    emails = emailsByContact[contact.id].orEmpty(),
                    organization = orgByContact[contact.id],
                    isLocalFavorite = isLocalFav
                )
            }
        }
    }

    suspend fun setFavorite(contact: Contact, favorite: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            if (favorite) {
                favoriteDao.upsertFavorite(
                    FavoriteContactEntity(
                        lookupKey = contact.lookupKey,
                        contactId = contact.id,
                        displayName = contact.displayName,
                        phoneNumber = contact.primaryNumber,
                        photoUri = contact.photoUri
                    )
                )
            } else {
                favoriteDao.deleteByLookupKey(contact.lookupKey)
            }

            if (PermissionHelper.hasContactsWritePermission(context) && contact.id > 0L) {
                val values = ContentValues().apply {
                    put(ContactsContract.Contacts.STARRED, if (favorite) 1 else 0)
                }
                context.contentResolver.update(
                    ContactsContract.Contacts.CONTENT_URI,
                    values,
                    "${ContactsContract.Contacts._ID} = ?",
                    arrayOf(contact.id.toString())
                )
            }
            Unit
        }
    }

    suspend fun removeLocalFavorite(lookupKey: String) {
        favoriteDao.deleteByLookupKey(lookupKey)
    }

    suspend fun insertContact(draft: ContactDraft): Result<Unit> = withContext(Dispatchers.IO) {
        if (!PermissionHelper.hasContactsWritePermission(context)) {
            return@withContext Result.failure(SecurityException("WRITE_CONTACTS not granted"))
        }

        runCatching {
            val ops = ArrayList<ContentProviderOperation>()
            ops.add(
                ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                    .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                    .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
                    .build()
            )
            ops.add(
                ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                    .withValue(
                        ContactsContract.Data.MIMETYPE,
                        ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE
                    )
                    .withValue(
                        ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME,
                        draft.displayName.trim()
                    )
                    .build()
            )
            ops.add(
                ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                    .withValue(
                        ContactsContract.Data.MIMETYPE,
                        ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE
                    )
                    .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, draft.phoneNumber.trim())
                    .withValue(
                        ContactsContract.CommonDataKinds.Phone.TYPE,
                        ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE
                    )
                    .build()
            )
            if (draft.email.isNotBlank()) {
                ops.add(
                    ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                        .withValue(
                            ContactsContract.Data.MIMETYPE,
                            ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE
                        )
                        .withValue(ContactsContract.CommonDataKinds.Email.ADDRESS, draft.email.trim())
                        .withValue(
                            ContactsContract.CommonDataKinds.Email.TYPE,
                            ContactsContract.CommonDataKinds.Email.TYPE_HOME
                        )
                        .build()
                )
            }
            if (draft.organization.isNotBlank()) {
                ops.add(
                    ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                        .withValue(
                            ContactsContract.Data.MIMETYPE,
                            ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE
                        )
                        .withValue(
                            ContactsContract.CommonDataKinds.Organization.COMPANY,
                            draft.organization.trim()
                        )
                        .withValue(
                            ContactsContract.CommonDataKinds.Organization.TYPE,
                            ContactsContract.CommonDataKinds.Organization.TYPE_WORK
                        )
                        .build()
                )
            }
            if (!draft.photoUri.isNullOrBlank()) {
                readCompressedPhotoBytes(Uri.parse(draft.photoUri))?.let { bytes ->
                    ops.add(
                        ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                            .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                            .withValue(
                                ContactsContract.Data.MIMETYPE,
                                ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE
                            )
                            .withValue(ContactsContract.CommonDataKinds.Photo.PHOTO, bytes)
                            .build()
                    )
                }
            }

            context.contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
            Unit
        }
    }

    suspend fun updateContact(draft: ContactDraft): Result<Unit> = withContext(Dispatchers.IO) {
        if (!PermissionHelper.hasContactsWritePermission(context)) {
            return@withContext Result.failure(SecurityException("WRITE_CONTACTS not granted"))
        }
        val contactId = draft.contactId ?: return@withContext insertContact(draft)

        runCatching {
            val rawContactId = findRawContactId(contactId)
                ?: return@withContext insertContact(draft)

            val ops = ArrayList<ContentProviderOperation>()
            ops.add(
                ContentProviderOperation.newUpdate(ContactsContract.Data.CONTENT_URI)
                    .withSelection(
                        "${ContactsContract.Data.CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?",
                        arrayOf(
                            contactId.toString(),
                            ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE
                        )
                    )
                    .withValue(
                        ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME,
                        draft.displayName.trim()
                    )
                    .build()
            )

            val phoneDataId = findFirstDataId(
                contactId,
                ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE
            )
            if (phoneDataId != null) {
                ops.add(
                    ContentProviderOperation.newUpdate(ContactsContract.Data.CONTENT_URI)
                        .withSelection("${ContactsContract.Data._ID} = ?", arrayOf(phoneDataId.toString()))
                        .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, draft.phoneNumber.trim())
                        .build()
                )
            } else {
                ops.add(
                    ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValue(ContactsContract.Data.RAW_CONTACT_ID, rawContactId)
                        .withValue(
                            ContactsContract.Data.MIMETYPE,
                            ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE
                        )
                        .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, draft.phoneNumber.trim())
                        .withValue(
                            ContactsContract.CommonDataKinds.Phone.TYPE,
                            ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE
                        )
                        .build()
                )
            }

            if (draft.email.isNotBlank()) {
                val emailDataId = findFirstDataId(
                    contactId,
                    ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE
                )
                if (emailDataId != null) {
                    ops.add(
                        ContentProviderOperation.newUpdate(ContactsContract.Data.CONTENT_URI)
                            .withSelection("${ContactsContract.Data._ID} = ?", arrayOf(emailDataId.toString()))
                            .withValue(ContactsContract.CommonDataKinds.Email.ADDRESS, draft.email.trim())
                            .build()
                    )
                } else {
                    ops.add(
                        ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                            .withValue(ContactsContract.Data.RAW_CONTACT_ID, rawContactId)
                            .withValue(
                                ContactsContract.Data.MIMETYPE,
                                ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE
                            )
                            .withValue(ContactsContract.CommonDataKinds.Email.ADDRESS, draft.email.trim())
                            .withValue(
                                ContactsContract.CommonDataKinds.Email.TYPE,
                                ContactsContract.CommonDataKinds.Email.TYPE_HOME
                            )
                            .build()
                    )
                }
            }

            if (draft.organization.isNotBlank()) {
                val orgDataId = findFirstDataId(
                    contactId,
                    ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE
                )
                if (orgDataId != null) {
                    ops.add(
                        ContentProviderOperation.newUpdate(ContactsContract.Data.CONTENT_URI)
                            .withSelection("${ContactsContract.Data._ID} = ?", arrayOf(orgDataId.toString()))
                            .withValue(
                                ContactsContract.CommonDataKinds.Organization.COMPANY,
                                draft.organization.trim()
                            )
                            .build()
                    )
                } else {
                    ops.add(
                        ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                            .withValue(ContactsContract.Data.RAW_CONTACT_ID, rawContactId)
                            .withValue(
                                ContactsContract.Data.MIMETYPE,
                                ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE
                            )
                            .withValue(
                                ContactsContract.CommonDataKinds.Organization.COMPANY,
                                draft.organization.trim()
                            )
                            .withValue(
                                ContactsContract.CommonDataKinds.Organization.TYPE,
                                ContactsContract.CommonDataKinds.Organization.TYPE_WORK
                            )
                            .build()
                    )
                }
            }

            context.contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
            Unit
        }
    }

    suspend fun deleteContact(contact: Contact): Result<Unit> = withContext(Dispatchers.IO) {
        if (!PermissionHelper.hasContactsWritePermission(context)) {
            return@withContext Result.failure(SecurityException("WRITE_CONTACTS not granted"))
        }

        runCatching {
            val lookupUri = ContactsContract.Contacts.getLookupUri(contact.id, contact.lookupKey)
                ?: Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, contact.id.toString())
            val deleted = context.contentResolver.delete(lookupUri, null, null)
            if (deleted <= 0) {
                throw IllegalStateException("Contact could not be deleted")
            }
            favoriteDao.deleteByLookupKey(contact.lookupKey)
            noteDao.deleteNote(contact.lookupKey)
            Unit
        }
    }

    private fun findRawContactId(contactId: Long): Long? {
        context.contentResolver.query(
            ContactsContract.RawContacts.CONTENT_URI,
            arrayOf(ContactsContract.RawContacts._ID),
            "${ContactsContract.RawContacts.CONTACT_ID} = ?",
            arrayOf(contactId.toString()),
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                return cursor.getLong(0)
            }
        }
        return null
    }

    private fun findFirstDataId(contactId: Long, mimeType: String): Long? {
        context.contentResolver.query(
            ContactsContract.Data.CONTENT_URI,
            arrayOf(ContactsContract.Data._ID),
            "${ContactsContract.Data.CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?",
            arrayOf(contactId.toString(), mimeType),
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                return cursor.getLong(0)
            }
        }
        return null
    }

    private fun readCompressedPhotoBytes(uri: Uri): ByteArray? {
        return runCatching {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val raw = stream.readBytes()
                if (raw.size <= 512_000) raw else null
            }
        }.getOrNull()
    }

    private class MutableContactBuilder(
        val id: Long,
        val lookupKey: String,
        val displayName: String,
        val photoUri: String?,
        val isStarred: Boolean
    ) {
        val numbers = mutableListOf<PhoneNumberEntry>()

        fun build(): Contact {
            val primary = numbers.firstOrNull { it.isPrimary }?.number
                ?: numbers.firstOrNull()?.number.orEmpty()
            return Contact(
                id = id,
                lookupKey = lookupKey,
                displayName = displayName,
                primaryNumber = primary,
                phoneNumbers = numbers.toList(),
                photoUri = photoUri,
                isStarred = isStarred
            )
        }
    }
}
