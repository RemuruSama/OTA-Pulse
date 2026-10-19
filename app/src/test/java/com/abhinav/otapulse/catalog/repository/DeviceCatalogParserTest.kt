package com.abhinav.otapulse.catalog.repository

import org.junit.Assert.*
import org.junit.Test

class DeviceCatalogParserTest {

    @Test
    fun `parseDevices parses valid device catalog JSON successfully`() {
        val json = """
            [
              {
                "name": "OnePlus 12",
                "ruiVersion": 6,
                "firmwareGroups": {
                  "Android 15": [
                    {
                      "displayName": "India",
                      "productModel": "CPH2581",
                      "productName": "CPH2581",
                      "firmwareVersion": "CPH2581_15.0.0.200(EX01)",
                      "region": "IN"
                    }
                  ]
                }
              }
            ]
        """.trimIndent()

        val result = DeviceCatalogParser.parseDevices(json)
        assertEquals(1, result.devices.size)
        assertEquals(0, result.skippedCount)
        val device = result.devices.first()
        assertEquals("OnePlus 12", device.name)
        assertEquals(6, device.ruiVersion)
        assertTrue(device.firmwareGroups.containsKey("Android 15"))
        val variant = device.firmwareGroups["Android 15"]!!.first()
        assertEquals("India", variant.displayName)
        assertEquals("CPH2581", variant.productModel)
    }

    @Test
    fun `parseDevices skips invalid device missing name or firmwareGroups`() {
        val json = """
            [
              {
                "ruiVersion": 6
              },
              {
                "name": "Realme GT 6",
                "ruiVersion": 6,
                "firmwareGroups": {
                  "Android 14": [
                    {
                      "displayName": "Global",
                      "productModel": "RMX3851",
                      "productName": "RMX3851",
                      "firmwareVersion": "RMX3851_14.0.0.100(EX01)",
                      "region": "GL"
                    }
                  ]
                }
              }
            ]
        """.trimIndent()

        val result = DeviceCatalogParser.parseDevices(json)
        assertEquals(1, result.devices.size)
        assertEquals(1, result.skippedCount)
        assertEquals("Realme GT 6", result.devices.first().name)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `parseDevices throws IllegalArgumentException when JSON is not an array`() {
        DeviceCatalogParser.parseDevices("""{"name": "Not an array"}""")
    }
}
