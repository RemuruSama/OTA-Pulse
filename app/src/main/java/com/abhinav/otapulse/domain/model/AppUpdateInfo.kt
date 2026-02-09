package com.abhinav.otapulse.domain.model

data class AppUpdateInfo(
    val version: String,
    val downloadUrl: String,
    val changelog: String
)
