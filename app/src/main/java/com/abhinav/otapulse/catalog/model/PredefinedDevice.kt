package com.abhinav.otapulse.catalog.model

import com.abhinav.otapulse.core.model.RegionVariant

import android.os.Parcelable
import androidx.annotation.DrawableRes
import kotlinx.parcelize.Parcelize

@Parcelize
data class PredefinedDevice(
    val name: String,
    val ruiVersion: Int,
    val imei: String = "",
    val beta: Boolean = false,
    @DrawableRes val imageResId: Int? = null,
    val firmwareGroups: Map<String, List<RegionVariant>>,
    val isFavorite: Boolean = false,
    var isLoadingDetails: Boolean = false,
    val isCustom: Boolean = false // New property with a default value
) : Parcelable
