package com.abhinav.otapulse.domain.model

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
    val language: String? = "en-EN"
)
