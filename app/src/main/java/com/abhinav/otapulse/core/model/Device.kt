package com.abhinav.otapulse.core.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Device(
    val name: String,
    val ruiVersion: Int,
    val imei: String,
    val beta: Boolean,
    val imageResId: Int?,
    val firmwareGroups: Map<String, List<RegionVariant>>,
    val isFavorite: Boolean,
    val isLoadingDetails: Boolean = false,
    val isError: Boolean = false,
    val isCustom: Boolean // New property to identify custom devices
) : Parcelable
