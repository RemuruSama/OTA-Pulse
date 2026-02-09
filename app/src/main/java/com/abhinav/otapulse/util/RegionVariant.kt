package com.abhinav.otapulse.util

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class RegionVariant(
    val displayName: String,
    val productModel: String,
    val firmwareVersion: String,
    val region: String,
    val nvId: String? = null,
    val language: String? = null
) : Parcelable