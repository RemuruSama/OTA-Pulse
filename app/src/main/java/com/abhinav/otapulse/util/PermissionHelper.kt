package com.abhinav.otapulse.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
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

    fun hasNotificationPermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        }
        return true
    }

    /**
     * Checks if the app has the necessary storage permissions for downloads.
     * Note: On Android 10+ (Q), standard DownloadManager/Fetch writes to public dir don't always need WRITE_EXTERNAL_STORAGE.
     */
    fun hasStoragePermission(): Boolean {
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
}