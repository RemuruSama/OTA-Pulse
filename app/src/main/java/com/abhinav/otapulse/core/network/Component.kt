package com.abhinav.otapulse.core.network

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

object Component {
    const val OTA_UPDATES_DIR = "OTAPulseDownloader"
}

@Parcelize
data class NetworkComponent(
    val componentId: String,
    val componentName: String,
    val componentVersion: String,
    val size: String,
    val manualUrl: String?,
    val url: String,
    val md5: String,
    val otaStreamingProperty: String?,
    val vabPackageHash: String?,
    val extraParams: String?,
    val fileHash: String?,
    val fileSize: String?,
    val metadataHash: String?,
    val metadataSize: String?,
    val androidVersion: String?,
    val oplusRomVersion: String?,
    val securityPatch: String?,
    val securityPatchVendor: String?,
    val versionTypeId: String?,
    val versionName: String?,
    val realVersionName: String?,
    val realAndroidVersion: String?,
    val realOsVersion: String?,
    val osVersion: String?,
    val colorOSVersion: String?,
    val panelUrl: String?,
    val realOtaVersion: String?,
    val rawJson: String? = null,
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
) : Parcelable
