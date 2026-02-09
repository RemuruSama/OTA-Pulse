package com.abhinav.otapulse.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Device(
    val name: String,
    val ruiVersion: Int,
    val imei: String,
    val beta: Boolean,
    val imageUrl: String?,
    val imageResId: Int?,
    val firmwareGroups: Map<String, List<RegionVariant>>,
    var isFavorite: Boolean,
    var isLoadingDetails: Boolean = false,
    var isError: Boolean = false,
    val isCustom: Boolean // New property to identify custom devices
) : Parcelable

@Parcelize
data class RegionVariant(
    val displayName: String,
    val productModel: String,
    val firmwareVersion: String,
    val region: String,
    val nvId: String? = null,
    val language: String? = null
) : Parcelable
