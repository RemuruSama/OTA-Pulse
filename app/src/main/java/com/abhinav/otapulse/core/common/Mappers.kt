package com.abhinav.otapulse.core.common

import com.abhinav.otapulse.catalog.model.PredefinedDevice
import com.abhinav.otapulse.core.model.Device
import com.abhinav.otapulse.core.model.OtaUpdate
import com.abhinav.otapulse.core.network.NetworkComponent

fun PredefinedDevice.toDomain(): Device {
    return Device(
        name = this.name,
        ruiVersion = this.ruiVersion,
        imei = this.imei,
        beta = this.beta,
        imageResId = this.imageResId,
        firmwareGroups = this.firmwareGroups,
        isFavorite = this.isFavorite,
        isCustom = this.isCustom
    )
}

fun Device.toPredefined(): PredefinedDevice {
    return PredefinedDevice(
        name = this.name,
        ruiVersion = this.ruiVersion,
        imei = this.imei,
        beta = this.beta,
        imageResId = this.imageResId,
        firmwareGroups = this.firmwareGroups,
        isFavorite = this.isFavorite,
        isCustom = this.isCustom
    )
}

fun NetworkComponent.toDomain(): OtaUpdate {
    // Extract just the path segment — strip query params that signed CDN URLs append
    val fileName = try {
        java.net.URL(url).path.substringAfterLast('/')
    } catch (_: Exception) {
        url.substringAfterLast('/').substringBefore('?')
    }
    return OtaUpdate(
        componentId = this.componentId,
        componentName = this.componentName,
        componentVersion = this.componentVersion,
        size = FormatUtils.formatSize(this.size.toLongOrNull() ?: 0L),
        manualUrl = this.manualUrl,
        url = this.url,
        md5 = this.md5,
        versionName = this.versionName ?: this.realVersionName,
        realAndroidVersion = this.realAndroidVersion ?: this.androidVersion,
        realOsVersion = this.realOsVersion ?: this.osVersion ?: this.colorOSVersion ?: this.oplusRomVersion,
        securityPatch = this.securityPatch ?: this.securityPatchVendor,
        panelUrl = this.panelUrl?.takeIf { it.isNotBlank() } ?: this.descriptionUrl,
        fileName = fileName,
        downloadUrl = this.url,
        rawJson = this.rawJson,
        realOtaVersion = this.realOtaVersion,
        otaStreamingProperty = this.otaStreamingProperty,
        vabPackageHash = this.vabPackageHash,
        extraParams = this.extraParams,
        fileHash = this.fileHash,
        fileSize = this.fileSize,
        metadataHash = this.metadataHash,
        metadataSize = this.metadataSize,
        androidVersion = this.androidVersion,
        oplusRomVersion = this.oplusRomVersion,
        securityPatchVendor = this.securityPatchVendor,
        versionTypeId = this.versionTypeId,
        realVersionName = this.realVersionName,
        osVersion = this.osVersion,
        colorOSVersion = this.colorOSVersion,
        publishedTime = this.publishedTime,
        status = this.status,
        nvId16 = this.nvId16,
        packetId = this.packetId,
        packetType = this.packetType,
        forbidOtaLocalUpdate = this.forbidOtaLocalUpdate,
        otaRootOrDebug = this.otaRootOrDebug,
        otaTargetVersion = this.otaTargetVersion,
        oplusSeparateSoft = this.oplusSeparateSoft,
        descriptionUrl = this.descriptionUrl,
        nightUpdateLimit = this.nightUpdateLimit,
        versionTypeH5 = this.versionTypeH5
    )
}

fun String.toFullRegionName(): String {
    return when (this) {
        "GLO" -> "Global"
        "CN" -> "China"
        "VN" -> "Vietnam"
        "IN" -> "India"
        "EU" -> "Europe"
        "TR" -> "Turkey"
        "RU" -> "Russia"
        "MEA" -> "Middle East & Africa"
        "SA" -> "Saudi Arabia"
        "SG" -> "Singapore"
        "TH" -> "Thailand"
        "LATAM" -> "Latin America"
        "BR" -> "Brazil"
        "TW" -> "Taiwan"
        "MY" -> "Malaysia"
        "ID" -> "Indonesia"
        "KZ" -> "Kazakhstan"
        "OCA" -> "Oceania"
        else -> this
    }
}
