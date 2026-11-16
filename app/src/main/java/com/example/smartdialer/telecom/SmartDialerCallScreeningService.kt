package com.example.smartdialer.telecom

import android.os.Build
import android.telecom.Call
import android.telecom.CallScreeningService
import com.example.smartdialer.data.local.SmartDialerDatabase
import com.example.smartdialer.util.BlocklistMatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Local-first CallScreeningService that checks incoming caller numbers
 * against the Room blocklist database and automatically rejects matched calls.
 * Never logs caller phone numbers or PII.
 */
class SmartDialerCallScreeningService : CallScreeningService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onScreenCall(callDetails: Call.Details) {
        val isIncoming = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            callDetails.callDirection == Call.Details.DIRECTION_INCOMING
        } else {
            true
        }

        val incomingNumber = callDetails.handle?.schemeSpecificPart.orEmpty()
        if (!isIncoming || incomingNumber.isBlank()) {
            respondToCall(callDetails, buildAllowResponse())
            return
        }

        serviceScope.launch {
            val shouldBlock = runCatching {
                val dao = SmartDialerDatabase.getInstance(applicationContext).blockedNumberDao()
                val blockedEntries = dao.getAllBlockedSnapshot()
                blockedEntries.any { entry ->
                    BlocklistMatcher.isMatch(incomingNumber, entry.normalizedNumber)
                }
            }.getOrDefault(false)

            val response = if (shouldBlock) {
                buildBlockResponse()
            } else {
                buildAllowResponse()
            }
            respondToCall(callDetails, response)
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun buildAllowResponse(): CallResponse {
        return CallResponse.Builder()
            .setDisallowCall(false)
            .setRejectCall(false)
            .setSilenceCall(false)
            .setSkipCallLog(false)
            .setSkipNotification(false)
            .build()
    }

    private fun buildBlockResponse(): CallResponse {
        return CallResponse.Builder()
            .setDisallowCall(true)
            .setRejectCall(true)
            .setSilenceCall(true)
            .setSkipCallLog(false)
            .setSkipNotification(true)
            .build()
    }
}
