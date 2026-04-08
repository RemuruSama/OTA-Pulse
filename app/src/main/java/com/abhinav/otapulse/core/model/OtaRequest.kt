package com.abhinav.otapulse.core.model

data class OtaRequest(
    val version: Int,
    val model: String,
    val firmwareVersion: String,
    val region: Int,
    val ruiVersion: Int,
    val imei0: String?,
    val beta: Boolean,
    val deviceId: String? = null,
    val nvIdentifier: String? = null,
    val imei1: String? = null,
    val language: String? = "en-EN",
    val reqMode: String? = "manual",
    val gray: Int = 0
)
