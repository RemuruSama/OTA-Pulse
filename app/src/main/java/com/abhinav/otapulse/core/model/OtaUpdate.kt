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
    val rawJson: String? = null,
    val realOtaVersion: String? = null,
    val otaStreamingProperty: String? = null,
    val vabPackageHash: String? = null,
    val extraParams: String? = null,
    val fileHash: String? = null,
    val fileSize: String? = null,
    val metadataHash: String? = null,
    val metadataSize: String? = null,
    val androidVersion: String? = null,
    val oplusRomVersion: String? = null,
    val securityPatchVendor: String? = null,
    val versionTypeId: String? = null,
    val realVersionName: String? = null,
    val osVersion: String? = null,
    val colorOSVersion: String? = null,
    val publishedTime: Long = 0,
    val status: String? = null,
    val nvId16: String? = null,
    val packetId: String? = null,
    val packetType: String? = null,
    val forbidOtaLocalUpdate: String? = null,
    val otaRootOrDebug: String? = null,
    val otaTargetVersion: String? = null,
    val oplusSeparateSoft: String? = null,
    val descriptionUrl: String? = null,
    val nightUpdateLimit: String? = null,
    val versionTypeH5: String? = null
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
