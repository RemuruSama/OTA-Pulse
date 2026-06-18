package com.abhinav.otapulse.core.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class RegionVariant(
    val displayName: String,
    val productModel: String,
    val productName: String,
    val firmwareVersion: String,
    val region: String,
    val nvId: String? = null,
    val language: String? = null,
    val reqMode: String? = "manual",
    val gray: Int = 0
) : Parcelable
