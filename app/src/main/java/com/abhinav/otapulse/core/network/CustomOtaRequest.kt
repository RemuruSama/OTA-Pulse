package com.abhinav.otapulse.core.network

import java.io.Serializable

data class CustomOtaRequest(
    var id: Long = System.currentTimeMillis(),
    var name: String,
    var regionCode: String = "TR",
    var productModel: String = "",
    var otaVersionLetter: String = "C",
    var mode: Int = 0, // 0 for Stable, 1 for Beta
    var imei: String = "",
    var proxy: String = "",
    var reqMode: String = "manual",
    var gray: Int = 0
) : Serializable
