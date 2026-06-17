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
        versionName = this.versionName,
        realAndroidVersion = this.realAndroidVersion,
        realOsVersion = this.realOsVersion,
        securityPatch = this.securityPatch,
        panelUrl = this.panelUrl,
        fileName = fileName,
        downloadUrl = this.url,
        rawJson = this.rawJson,
        realOtaVersion = this.realOtaVersion
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
