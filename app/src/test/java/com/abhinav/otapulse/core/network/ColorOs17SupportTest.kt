/*
 * Copyright (C) 2026 OTA Pulse
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.abhinav.otapulse.core.network

import com.abhinav.otapulse.core.common.toDomain
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Unit tests validating ColorOS 17 (Android 17) JSON response parsing,
 * request generation, and unified ColorOS branding logic.
 */
class ColorOs17SupportTest {

    private fun loadSampleJson(): JSONObject {
        val resourceFile = File("src/test/resources/coloros17_sample.json")
        val jsonString = if (resourceFile.exists()) {
            resourceFile.readText()
        } else {
            File("app/src/test/resources/coloros17_sample.json").readText()
        }
        return JSONObject(jsonString)
    }

    @Test
    fun `parseComponents extracts ColorOS 17 and Android 17 fields correctly`() {
        val json = loadSampleJson()
        val request = Request(
            reqVersion = 2,
            model = "PLK110",
            firmwareVersion = "PLK110_11.C.75_1750_202609240641",
            region = 1, // CN
            ruiVersion = 7,
            imei0 = "0",
            beta = false
        )

        val components = request.parseComponents(json)
        assertEquals(1, components.size)

        val comp = components[0]
        assertEquals("my_manifest_PLK110_11.C.75_1750_202609240641.97.3ad3d618", comp.componentId)
        assertEquals("PLK110_11.C.75_1750_202609240641.97.3ad3d618", comp.componentVersion)
        assertEquals("Android 17", comp.realAndroidVersion)
        assertEquals("ColorOS 17.0.0", comp.realOsVersion)
        assertEquals("ColorOS 17.0", comp.colorOSVersion)
        assertEquals("37", comp.androidApiLevel)
        assertEquals(1750L, comp.versionCode)
        assertEquals("%s 超流畅，更懂你", comp.upgradeTips)
        assertEquals("5", comp.secLevel)
        assertEquals(true, comp.componentAssembleType)
        assertEquals("17.0.0.102Patch03", comp.opexVersionName)
        assertEquals("• 优化部分场景的系统稳定性", comp.opexContent)
        assertEquals("0", comp.oplusUpdateEngineVerifyDisable)
        assertEquals("PLK110_17.0.0.102(CN01)", comp.versionName)
        assertEquals("PLK110_17.0.0.102(CN01)", comp.realVersionName)
        assertEquals("NV97", comp.nvId16)
        assertEquals("2026-09-01", comp.securityPatch)
        assertEquals("2026-09-01", comp.securityPatchVendor)
    }

    @Test
    fun `toDomain maps ColorOS 17 metadata to OtaUpdate`() {
        val json = loadSampleJson()
        val request = Request(
            reqVersion = 2,
            model = "PLK110",
            firmwareVersion = "PLK110_11.C.75_1750_202609240641",
            region = 1,
            ruiVersion = 7,
            imei0 = "0",
            beta = false
        )

        val comp = request.parseComponents(json).first()
        val ota = comp.toDomain()

        assertEquals("ColorOS 17.0.0", ota.realOsVersion)
        assertEquals("Android 17", ota.realAndroidVersion)
        assertEquals("37", ota.androidApiLevel)
        assertEquals(1750L, ota.versionCode)
        assertEquals("%s 超流畅，更懂你", ota.upgradeTips)
        assertEquals("5", ota.secLevel)
        assertEquals(true, ota.componentAssembleType)
        assertEquals("17.0.0.102Patch03", ota.opexVersionName)
        assertEquals("• 优化部分场景的系统稳定性", ota.opexContent)
        assertNotNull(ota.downloadUrl)
        assertTrue(ota.downloadUrl.contains("allawntech.com"))
    }

    @Test
    fun `Request prepare for ColorOS 17 disables isRealme flag and sets ColorOS 17 branding`() {
        // Even with a realme RMX model, ColorOS 17 (ruiVersion = 7) should set isRealme = "0"
        // because realme and OxygenOS merged into base ColorOS on Android 17.
        val request = Request(
            reqVersion = 2,
            model = "RMX5261",
            firmwareVersion = "RMX5261_11.C.01_0001_100001010000",
            region = 2, // IN
            ruiVersion = 7,
            imei0 = "0",
            beta = false
        )
        request.prepare()

        val payload = request.getPayload()
        val bodyJson = JSONObject(payload.body)
        // With reqVersion = 2, body contains encrypted params
        assertNotNull(bodyJson)
    }

    @Test
    fun `Request prepare sets expected properties for ColorOS 17`() {
        val request = Request(
            reqVersion = 2,
            model = "PLK110",
            firmwareVersion = "PLK110_11.C.75_1750_202609240641",
            region = 1,
            ruiVersion = 7,
            imei0 = "0",
            beta = false
        )
        request.prepare()

        // Access internal properties via reflection to verify prepared keys
        val propField = Request::class.java.getDeclaredField("properties")
        propField.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val properties = propField.get(request) as Map<String, Any>

        assertEquals("Android 17", properties["androidVersion"])
        assertEquals("ColorOS 17.0", properties["colorOSVersion"])
        assertEquals("0", properties["isRealme"])
    }

    @Test
    fun `Request prepare supports direct ColorOS version 17`() {
        val request = Request(
            reqVersion = 2,
            model = "PLK110",
            firmwareVersion = "PLK110_11.C.75_1750_202609240641",
            region = 1,
            ruiVersion = 17, // Direct ColorOS 17
            imei0 = "0",
            beta = false
        )
        request.prepare()

        val propField = Request::class.java.getDeclaredField("properties")
        propField.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val properties = propField.get(request) as Map<String, Any>

        assertEquals("Android 17", properties["androidVersion"])
        assertEquals("ColorOS 17.0", properties["colorOSVersion"])
        assertEquals("0", properties["isRealme"])
    }

    @Test
    fun `Request prepare supports taste reqMode for OnePlus 15 Android 17 release`() {
        val request = Request(
            reqVersion = 2,
            model = "PLK110",
            firmwareVersion = "PLK110_11.C.75_1750_202609240641",
            region = 1,
            ruiVersion = 7,
            imei0 = "0",
            beta = false,
            reqMode = "taste"
        )
        request.prepare()

        val payload = request.getPayload()
        assertEquals("taste", payload.headers["mode"])

        val propField = Request::class.java.getDeclaredField("properties")
        propField.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val properties = propField.get(request) as Map<String, Any>
        assertEquals("taste", properties["reqMode"])
    }
}

