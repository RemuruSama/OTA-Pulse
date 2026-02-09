package com.abhinav.otapulse.util

/**
 * Data class to hold the specific information for each region.
 */
data class RegionInfo(
    val displayName: String, // e.g., "GLO", "IN"
    val nvid: String,        // e.g., "NVA7", "NV1B"
    val serverCode: String   // e.g., "GL", "IN", "EU"
)

/**
 * A centralized object that holds the predefined list of all supported regions.
 */
object RegionData {
    val regions = listOf(
        RegionInfo("GLO", "NVA7", "GL"),
        RegionInfo("CN", "NV97", "CN"),
        RegionInfo("VN", "NV3C", "EU"),
        RegionInfo("IN", "NV1B", "IN"),
        RegionInfo("EU", "NV44", "EU"),
        RegionInfo("TR", "NV51", "GL"),
        RegionInfo("RU", "NV37", "EU"),
        RegionInfo("MEA", "NVA6", "EU"),
        RegionInfo("SA", "NV83", "EU"),
        RegionInfo("SG", "NV2C", "EU"),
        RegionInfo("TH", "NV39", "EU"),
        RegionInfo("LATAM", "NV9A", "EU"),
        RegionInfo("BR", "NV9E", "GL"),
        RegionInfo("TW", "NV1A", "EU"),
        RegionInfo("MY", "NV38", "EU"),
        RegionInfo("ID", "NV33", "EU"),
        RegionInfo("OCA", "NVA5", "EU")
    )
}
