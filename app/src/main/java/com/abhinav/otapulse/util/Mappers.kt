package com.abhinav.otapulse.util

import com.abhinav.otapulse.domain.model.Device
import com.abhinav.otapulse.domain.model.OtaUpdate
import com.abhinav.otapulse.domain.model.RegionVariant as DomainRegionVariant
import com.abhinav.otapulse.util.RegionVariant as DataRegionVariant

fun PredefinedDevice.toDomain(): Device {
    return Device(
        name = this.name,
        ruiVersion = this.ruiVersion,
        imei = this.imei,
        beta = this.beta,
        imageUrl = this.imageUrl,
        imageResId = this.imageResId,
        firmwareGroups = this.firmwareGroups.mapValues { entry ->
            entry.value.map { it.toDomain() }
        },
        isFavorite = this.isFavorite,
        isCustom = this.isCustom // Map the new property
    )
}

fun DataRegionVariant.toDomain(): DomainRegionVariant {
    return DomainRegionVariant(
        displayName = this.displayName,
        productModel = this.productModel,
        firmwareVersion = this.firmwareVersion,
        region = this.region,
        nvId = this.nvId,
        language = this.language
    )
}

fun DomainRegionVariant.toData(): DataRegionVariant {
    return DataRegionVariant(
        displayName = this.displayName,
        productModel = this.productModel,
        firmwareVersion = this.firmwareVersion,
        region = this.region,
        nvId = this.nvId,
        language = this.language
    )
}

fun Device.toPredefined(): PredefinedDevice {
    return PredefinedDevice(
        name = this.name,
        ruiVersion = this.ruiVersion,
        imei = this.imei,
        beta = this.beta,
        imageUrl = this.imageUrl,
        imageResId = this.imageResId,
        firmwareGroups = this.firmwareGroups.mapValues { entry ->
            entry.value.map { it.toData() }
        },
        isFavorite = this.isFavorite,
        isCustom = this.isCustom
    )
}

fun NetworkComponent.toDomain(): OtaUpdate {
    val fileName = url.substringAfterLast('/')
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
        downloadUrl = this.url
    )
}
