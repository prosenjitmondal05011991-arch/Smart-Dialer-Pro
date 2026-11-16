package com.example.smartdialer.permissions

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.core.content.ContextCompat

object PermissionHelper {

    val ALL_RUNTIME_PERMISSIONS: List<String> = listOf(
        Manifest.permission.READ_CONTACTS,
        Manifest.permission.WRITE_CONTACTS,
        Manifest.permission.READ_CALL_LOG,
        Manifest.permission.CALL_PHONE,
        Manifest.permission.READ_PHONE_STATE
    )

    fun hasPermission(context: Context, permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun hasContactsReadPermission(context: Context): Boolean =
        hasPermission(context, Manifest.permission.READ_CONTACTS)

    fun hasContactsWritePermission(context: Context): Boolean =
        hasPermission(context, Manifest.permission.WRITE_CONTACTS)

    fun hasCallLogPermission(context: Context): Boolean =
        hasPermission(context, Manifest.permission.READ_CALL_LOG)

    fun hasCallPhonePermission(context: Context): Boolean =
        hasPermission(context, Manifest.permission.CALL_PHONE)

    fun hasPhoneStatePermission(context: Context): Boolean =
        hasPermission(context, Manifest.permission.READ_PHONE_STATE)

    fun missingRuntimePermissions(context: Context): List<String> =
        ALL_RUNTIME_PERMISSIONS.filterNot { hasPermission(context, it) }

    fun grantedRuntimePermissionCount(context: Context): Int =
        ALL_RUNTIME_PERMISSIONS.count { hasPermission(context, it) }

    fun areAllRuntimePermissionsGranted(context: Context): Boolean =
        missingRuntimePermissions(context).isEmpty()

    fun openAppSettings(context: Context) {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", context.packageName, null)
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
