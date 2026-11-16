package com.example.smartdialer.telecom

import android.annotation.SuppressLint
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.ContactsContract
import android.telecom.TelecomManager
import android.telephony.SubscriptionManager
import com.example.smartdialer.data.model.DefaultDialerStatus
import com.example.smartdialer.data.model.SimAccount
import com.example.smartdialer.permissions.PermissionHelper

object DialerTelecomManager {

    fun checkDefaultDialerStatus(context: Context): DefaultDialerStatus {
        val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
        val defaultPkg = runCatching { telecomManager?.defaultDialerPackage }.getOrNull()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(RoleManager::class.java)
            val isHeld = runCatching {
                roleManager?.isRoleHeld(RoleManager.ROLE_DIALER) == true
            }.getOrDefault(false)
            DefaultDialerStatus(
                isDefaultDialer = isHeld,
                currentDefaultPackage = defaultPkg,
                supportsRoleManager = true
            )
        } else {
            val isDefault = !defaultPkg.isNullOrBlank() && defaultPkg == context.packageName
            DefaultDialerStatus(
                isDefaultDialer = isDefault,
                currentDefaultPackage = defaultPkg,
                supportsRoleManager = false
            )
        }
    }

    fun createRequestDefaultDialerIntent(context: Context): Intent? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(RoleManager::class.java)
            if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_DIALER)) {
                roleManager.createRequestRoleIntent(RoleManager.ROLE_DIALER)
            } else {
                null
            }
        } else {
            @Suppress("DEPRECATION")
            Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER).apply {
                putExtra(TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME, context.packageName)
            }
        }
    }

    fun isCallScreeningRoleSupported(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

    fun isCallScreeningRoleHeld(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return checkDefaultDialerStatus(context).isDefaultDialer
        }
        val roleManager = context.getSystemService(RoleManager::class.java) ?: return false
        return runCatching {
            roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING) ||
                roleManager.isRoleHeld(RoleManager.ROLE_DIALER)
        }.getOrDefault(false)
    }

    fun createRequestCallScreeningRoleIntent(context: Context): Intent? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return createRequestDefaultDialerIntent(context)
        }
        val roleManager = context.getSystemService(RoleManager::class.java) ?: return null
        return if (roleManager.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING)) {
            roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)
        } else {
            null
        }
    }

    @SuppressLint("MissingPermission")
    fun getAvailableSims(context: Context): List<SimAccount> {
        if (!PermissionHelper.hasPhoneStatePermission(context)) {
            return emptyList()
        }

        val sims = mutableListOf<SimAccount>()
        runCatching {
            val subManager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as? SubscriptionManager
            val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
            val phoneAccounts = runCatching { telecomManager?.callCapablePhoneAccounts.orEmpty() }.getOrDefault(emptyList())
            val subs = subManager?.activeSubscriptionInfoList.orEmpty()

            subs.forEachIndexed { index, info ->
                val matchingHandle = phoneAccounts.getOrNull(index)
                sims.add(
                    SimAccount(
                        slotIndex = info.simSlotIndex + 1,
                        subscriptionId = info.subscriptionId,
                        displayName = info.displayName?.toString()?.takeIf { it.isNotBlank() }
                            ?: "SIM ${info.simSlotIndex + 1}",
                        carrierName = info.carrierName?.toString().orEmpty(),
                        phoneAccountHandle = matchingHandle
                    )
                )
            }
        }
        return sims
    }

    /**
     * Places a call via TelecomManager when CALL_PHONE is granted, or opens ACTION_DIAL
     * safely without claiming fake calling functionality.
     * Returns true if direct TelecomManager placement was used, false if system ACTION_DIAL was opened.
     */
    @SuppressLint("MissingPermission")
    fun placeCallOrDial(
        context: Context,
        rawNumber: String,
        selectedSim: SimAccount? = null
    ): Result<Boolean> {
        val cleaned = rawNumber.trim()
        if (cleaned.isEmpty()) {
            return Result.failure(IllegalArgumentException("Empty phone number"))
        }
        val uri = Uri.fromParts("tel", cleaned, null)

        if (PermissionHelper.hasCallPhonePermission(context)) {
            val telecomPlaced = runCatching {
                val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
                if (telecomManager != null) {
                    val extras = Bundle().apply {
                        selectedSim?.phoneAccountHandle?.let { handle ->
                            putParcelable(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, handle)
                        }
                    }
                    telecomManager.placeCall(uri, extras)
                    true
                } else {
                    false
                }
            }.getOrDefault(false)

            if (telecomPlaced) {
                return Result.success(true)
            }
        }

        return runCatching {
            val dialIntent = Intent(Intent.ACTION_DIAL, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(dialIntent)
            false
        }
    }

    fun openSmsApp(context: Context, number: String) {
        val intent = Intent(Intent.ACTION_SENDTO, Uri.fromParts("smsto", number.trim(), null)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
    }

    fun openSystemInsertContact(context: Context, phoneNumber: String? = null) {
        val intent = Intent(ContactsContract.Intents.Insert.ACTION).apply {
            type = ContactsContract.RawContacts.CONTENT_TYPE
            if (!phoneNumber.isNullOrBlank()) {
                putExtra(ContactsContract.Intents.Insert.PHONE, phoneNumber.trim())
            }
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
    }

    fun vibrateKeyTap(context: Context) {
        runCatching {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.getSystemService(VibratorManager::class.java)?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            if (vibrator?.hasVibrator() == true) {
                vibrator.vibrate(
                    VibrationEffect.createOneShot(16L, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            }
        }
    }
}
