package com.abhinav.otapulse.feature.history

import com.abhinav.otapulse.core.model.OtaHistoryEntry
import com.abhinav.otapulse.core.model.OtaUpdate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HistorySearchFilterTest {

    private fun filterHistory(historyList: List<OtaHistoryEntry>, searchQuery: String): List<OtaHistoryEntry> {
        if (searchQuery.isBlank()) return historyList
        val q = searchQuery.trim().lowercase()
        return historyList.filter { entry ->
            val ota = entry.otaUpdate
            entry.deviceName.lowercase().contains(q) ||
            entry.deviceName.removePrefix("Custom|").lowercase().contains(q) ||
            entry.region.lowercase().contains(q) ||
            ota.versionName?.lowercase()?.contains(q) == true ||
            ota.realVersionName?.lowercase()?.contains(q) == true ||
            ota.realOtaVersion?.lowercase()?.contains(q) == true ||
            ota.componentVersion.lowercase().contains(q) ||
            ota.otaTargetVersion?.lowercase()?.contains(q) == true ||
            ota.realOsVersion?.lowercase()?.contains(q) == true ||
            ota.oplusRomVersion?.lowercase()?.contains(q) == true
        }
    }

    private fun createSampleEntry(
        deviceName: String,
        region: String,
        versionName: String? = null,
        realOtaVersion: String? = null,
        componentVersion: String = "1.0.0",
        realOsVersion: String? = null
    ): OtaHistoryEntry {
        return OtaHistoryEntry(
            id = 1L,
            timestamp = System.currentTimeMillis(),
            deviceName = deviceName,
            region = region,
            otaUpdate = OtaUpdate(
                componentId = "test",
                componentName = "test",
                componentVersion = componentVersion,
                size = "1024",
                manualUrl = null,
                url = "https://example.com/ota.zip",
                md5 = "d41d8cd98f00b204e9800998ecf8427e",
                versionName = versionName,
                realAndroidVersion = "14",
                realOsVersion = realOsVersion,
                securityPatch = null,
                panelUrl = null,
                fileName = "ota.zip",
                downloadUrl = "https://example.com/ota.zip",
                realOtaVersion = realOtaVersion
            )
        )
    }

    @Test
    fun `search matches by realOtaVersion when versionName is null`() {
        val entry = createSampleEntry(
            deviceName = "Custom|RMX3840",
            region = "EU",
            versionName = null,
            realOtaVersion = "RMX3840_11.C.12_3540_202602261724"
        )
        val list = listOf(entry)

        val results = filterHistory(list, "C.12")
        assertEquals(1, results.size)
    }

    @Test
    fun `search matches by device model without Custom prefix`() {
        val entry = createSampleEntry(
            deviceName = "Custom|Realme GT Neo 3",
            region = "GLO",
            componentVersion = "RMX3561_11.F.08"
        )
        val list = listOf(entry)

        val results = filterHistory(list, "Realme GT")
        assertEquals(1, results.size)
    }

    @Test
    fun `search matches by componentVersion code`() {
        val entry = createSampleEntry(
            deviceName = "Custom Device",
            region = "IN",
            versionName = "Android 14",
            componentVersion = "CPH2487_11.H.54_0540"
        )
        val list = listOf(entry)

        val results = filterHistory(list, "CPH2487")
        assertEquals(1, results.size)
    }
}
