package com.example.smartdialer.data.repository

import android.content.Context
import android.provider.CallLog
import com.example.smartdialer.data.model.CallDirection
import com.example.smartdialer.data.model.CallLogEntry
import com.example.smartdialer.permissions.PermissionHelper
import com.example.smartdialer.util.T9Matcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CallLogRepository(private val context: Context) {

    suspend fun loadDeviceCallLog(limit: Int = 200): Result<List<CallLogEntry>> =
        withContext(Dispatchers.IO) {
            if (!PermissionHelper.hasCallLogPermission(context)) {
                return@withContext Result.failure(SecurityException("READ_CALL_LOG not granted"))
            }

            runCatching {
                val entries = mutableListOf<CallLogEntry>()
                val projection = arrayOf(
                    CallLog.Calls._ID,
                    CallLog.Calls.CACHED_NAME,
                    CallLog.Calls.NUMBER,
                    CallLog.Calls.TYPE,
                    CallLog.Calls.DATE,
                    CallLog.Calls.DURATION,
                    CallLog.Calls.CACHED_PHOTO_URI
                )

                context.contentResolver.query(
                    CallLog.Calls.CONTENT_URI,
                    projection,
                    null,
                    null,
                    "${CallLog.Calls.DATE} DESC"
                )?.use { cursor ->
                    val idIdx = cursor.getColumnIndexOrThrow(CallLog.Calls._ID)
                    val nameIdx = cursor.getColumnIndexOrThrow(CallLog.Calls.CACHED_NAME)
                    val numberIdx = cursor.getColumnIndexOrThrow(CallLog.Calls.NUMBER)
                    val typeIdx = cursor.getColumnIndexOrThrow(CallLog.Calls.TYPE)
                    val dateIdx = cursor.getColumnIndexOrThrow(CallLog.Calls.DATE)
                    val durationIdx = cursor.getColumnIndexOrThrow(CallLog.Calls.DURATION)
                    val photoIdx = cursor.getColumnIndexOrThrow(CallLog.Calls.CACHED_PHOTO_URI)

                    while (cursor.moveToNext() && entries.size < limit) {
                        val id = cursor.getLong(idIdx)
                        val cachedName = cursor.getString(nameIdx)?.trim()?.takeIf { it.isNotEmpty() }
                        val rawNumber = cursor.getString(numberIdx)?.trim().orEmpty()
                        val type = cursor.getInt(typeIdx)
                        val timestamp = cursor.getLong(dateIdx)
                        val duration = cursor.getLong(durationIdx).coerceAtLeast(0L)
                        val photoUri = cursor.getString(photoIdx)

                        entries.add(
                            CallLogEntry(
                                id = id,
                                cachedName = cachedName,
                                number = rawNumber,
                                formattedNumber = T9Matcher.formatDialDisplay(rawNumber),
                                direction = mapCallType(type),
                                timestampMillis = timestamp,
                                durationSeconds = duration,
                                photoUri = photoUri
                            )
                        )
                    }
                }
                entries
            }
        }

    private fun mapCallType(type: Int): CallDirection {
        return when (type) {
            CallLog.Calls.INCOMING_TYPE -> CallDirection.INCOMING
            CallLog.Calls.OUTGOING_TYPE -> CallDirection.OUTGOING
            CallLog.Calls.MISSED_TYPE -> CallDirection.MISSED
            CallLog.Calls.REJECTED_TYPE -> CallDirection.REJECTED
            CallLog.Calls.VOICEMAIL_TYPE -> CallDirection.VOICEMAIL
            else -> CallDirection.UNKNOWN
        }
    }
}
