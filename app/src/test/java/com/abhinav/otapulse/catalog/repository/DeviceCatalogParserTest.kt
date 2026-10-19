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

    @Test
    fun `parseDevices parses compact schema without ruiVersion and derives it correctly`() {
        val json = """
            [
              {
                "name": "OnePlus 15",
                "variants": [
                  {
                    "displayName": "CN",
                    "model": "PLK110"
                  },
                  {
                    "displayName": "IN",
                    "model": "CPH2745",
                    "productName": "CPH2745IN"
                  }
                ],
                "firmwareGroups": {
                  "Android 16": "A",
                  "Android 17": {
                    "versionLetter": "C",
                    "reqMode": "taste"
                  }
                }
              }
            ]
        """.trimIndent()

        val result = DeviceCatalogParser.parseDevices(json)
        assertEquals(1, result.devices.size)
        val device = result.devices.first()
        assertEquals("OnePlus 15", device.name)
        // Highest Android version is 17 -> ruiVersion derived as 7
        assertEquals(7, device.ruiVersion)

        // Verify Android 16 group
        val a16Variants = device.firmwareGroups["Android 16"]
        assertNotNull(a16Variants)
        assertEquals(2, a16Variants!!.size)
        assertEquals("PLK110", a16Variants[0].productModel)
        assertEquals("PLK110", a16Variants[0].productName) // fallback to model
        assertEquals("manual", a16Variants[0].reqMode)
        assertTrue(a16Variants[0].firmwareVersion.contains("11.A.01"))

        // Verify Android 17 group with group overrides
        val a17Variants = device.firmwareGroups["Android 17"]
        assertNotNull(a17Variants)
        assertEquals(2, a17Variants!!.size)
        assertEquals("CPH2745", a17Variants[1].productModel)
        assertEquals("CPH2745IN", a17Variants[1].productName)
        assertEquals("taste", a17Variants[1].reqMode)
        assertTrue(a17Variants[1].firmwareVersion.contains("11.C.01"))
    }

    @Test
    fun `parseDevices supports array shorthand for firmwareGroups`() {
        val json = """
            [
              {
                "name": "OnePlus 15T",
                "variants": [
                  {
                    "code": "CN",
                    "model": "PLZ110"
                  }
                ],
                "firmwareGroups": ["Android 16"]
              }
            ]
        """.trimIndent()

        val result = DeviceCatalogParser.parseDevices(json)
        assertEquals(1, result.devices.size)
        val device = result.devices.first()
        assertEquals(6, device.ruiVersion)
        val variants = device.firmwareGroups["Android 16"]
        assertNotNull(variants)
        assertEquals(1, variants!!.size)
        assertEquals("CN", variants[0].displayName)
        assertEquals("PLZ110", variants[0].productModel)
    }

    @Test
    fun `parseDevices successfully parses all production catalog files`() {
        val deviceFiles = listOf("oneplus.json", "oppo.json", "realme.json")
        for (fileName in deviceFiles) {
            val file = java.io.File("../devices/$fileName").takeIf { it.exists() }
                ?: java.io.File("devices/$fileName")
            assertTrue("File $fileName should exist", file.exists())
            val result = DeviceCatalogParser.parseDevices(file.readText())
            assertTrue("Devices should not be empty for $fileName", result.devices.isNotEmpty())
            assertEquals("Should skip 0 devices for $fileName", 0, result.skippedCount)
        }
    }
}

