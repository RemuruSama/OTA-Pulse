package com.abhinav.otapulse.core.model

data class OtaHistoryEntry(
    val timestamp: Long,
    val deviceName: String,
    val region: String,
    val otaUpdate: OtaUpdate
)
