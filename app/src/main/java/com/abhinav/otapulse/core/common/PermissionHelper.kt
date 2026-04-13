package com.abhinav.otapulse.core.common

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Centralized helper for checking permissions across the app.
 * Removes code duplication in Activities and Fragments.
 */
@Singleton
class PermissionHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val permissionPrefs by lazy {
        context.getSharedPreferences(PERMISSION_PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun hasNotificationPermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        }
        return true
    }

    fun wasNotificationPermissionRequested(): Boolean {
        return permissionPrefs.getBoolean(KEY_NOTIFICATION_PERMISSION_REQUESTED, false)
    }

    fun markNotificationPermissionRequested() {
        permissionPrefs.edit().putBoolean(KEY_NOTIFICATION_PERMISSION_REQUESTED, true).apply()
    }

    /**
     * Checks if the app has the necessary storage permissions for downloads.
     * Note: On Android 10+ (Q), standard DownloadManager/Fetch writes to public dir don't always need WRITE_EXTERNAL_STORAGE.
     */
    fun hasStoragePermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager()
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+ uses Scoped Storage. Writing to Downloads collection usually doesn't need explicit permission
            // if using MediaStore or DownloadManager. However, some libraries might still request it.
            // For this app, we assume we don't need MANAGE_EXTERNAL_STORAGE anymore.
            return true
        }
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun getRequiredStoragePermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else {
            emptyArray()
        }
    }

    private companion object {
        const val PERMISSION_PREFS_NAME = "ota_pulse_permission_prefs"
        const val KEY_NOTIFICATION_PERMISSION_REQUESTED = "notification_permission_requested"
    }
}
