package com.abhinav.otapulse.core.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class AppUpdateInfo(
    val version: String,
    val downloadUrl: String,
    val changelog: String
) : Parcelable
