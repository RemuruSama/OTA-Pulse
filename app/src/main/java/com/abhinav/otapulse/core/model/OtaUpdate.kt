package com.abhinav.otapulse.core.model

import android.os.Parcelable
import com.google.gson.Gson
import kotlinx.parcelize.Parcelize

@Parcelize
data class OtaUpdate(
    val componentId: String,
    val componentName: String,
    val componentVersion: String,
    val size: String,
    val manualUrl: String?,
    val url: String,
    val md5: String,
    val versionName: String?,
    val realAndroidVersion: String?,
    val realOsVersion: String?,
    val securityPatch: String?,
    val panelUrl: String?,
    val arbStatus: String? = null,
    val fileName: String,
    val downloadUrl: String,
    val rawJson: String? = null
) : Parcelable {
    companion object {
        fun fromString(otaUpdateString: String): OtaUpdate? {
            return try {
                Gson().fromJson(otaUpdateString, OtaUpdate::class.java)
            } catch (e: Exception) {
                null
            }
        }
    }
}

/**
 * Converts a [Map] of String key-value pairs into a plain [Map] ready to be
 * stored as download extras. Previously returned `Fetch2's Extras`; now simply
 * returns the map itself since [OkHttpDownloadEngine] uses `Map<String, String>`
 * directly.
 */
fun Map<String, String>.toExtras(): Map<String, String> = this
