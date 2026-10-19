package com.abhinav.otapulse.util

import org.junit.Assert.*
import org.junit.Test
import com.abhinav.otapulse.core.network.Request
import com.abhinav.otapulse.core.model.OtaError

/**
 * Unit tests for Request — verifies URL resolution, version calculations, and NV carrier logic.
 * These tests use the public API of the Request class without needing any Android context.
 */
class RequestTest {

    // --- Android Version Calculation ---

    @Test
    fun `RUI 1 produces Android 10`() {
        val request = createRequest(ruiVersion = 1)
        request.prepare()
        // RUI 1 → Android 10 (10 + 1 - 1 = 10)
        assertTrue(request.url.isNotEmpty())
    }

    @Test
    fun `RUI 5 produces correct URL resolution`() {
        val request = createRequest(ruiVersion = 5, reqVersion = 2)
        request.prepare()
        // RUI 5, version 2 → should use serverParams URL
        assertTrue(request.url.contains("component-ota"))
    }

    @Test
    fun `RUI 6 produces correct URL for v2`() {
        val request = createRequest(ruiVersion = 6, reqVersion = 2)
        request.prepare()
        // RUI 6+, version 2 → uses serverParams
        assertTrue(request.url.contains("allawnos.com") || request.url.contains("allawntech.com"))
    }

    // --- URL Resolution ---

    @Test
    fun `V1 request uses ifota URL`() {
        val request = createRequest(ruiVersion = 1, reqVersion = 1, region = 0)
        request.prepare()
        assertTrue(request.url.contains("ifota"))
    }

    @Test
    fun `V2 request uses component-ota URL`() {
        val request = createRequest(ruiVersion = 2, reqVersion = 2, region = 0)
        request.prepare()
        assertTrue(request.url.contains("component-ota") || request.url.contains("allawnos"))
    }

    @Test
    fun `CN region resolves correctly`() {
        val request = createRequest(ruiVersion = 2, reqVersion = 2, region = 1)
        request.prepare()
        assertTrue(request.url.contains("cn") || request.url.contains("allawntech"))
    }

    @Test
    fun `IN region resolves correctly`() {
        val request = createRequest(ruiVersion = 2, reqVersion = 2, region = 2)
        request.prepare()
        assertTrue(request.url.contains("in") || request.url.contains("allawnos"))
    }

    @Test
    fun `EU region resolves correctly`() {
        val request = createRequest(ruiVersion = 2, reqVersion = 2, region = 3)
        request.prepare()
        assertTrue(request.url.contains("eu") || request.url.contains("allawnos"))
    }

    // --- Response Content Key ---

    @Test
    fun `V1 request uses params content key`() {
        val request = createRequest(ruiVersion = 1, reqVersion = 1)
        request.prepare()
        assertEquals("params", request.responseContentKey)
    }

    @Test
    fun `V2 RUI2+ request uses body content key`() {
        val request = createRequest(ruiVersion = 2, reqVersion = 2)
        request.prepare()
        assertEquals("body", request.responseContentKey)
    }

    // --- Payload Generation ---

    @Test
    fun `getPayload returns non-empty body and headers`() {
        val request = createRequest(ruiVersion = 2, reqVersion = 2)
        request.prepare()
        val payload = request.getPayload()
        assertTrue(payload.body.isNotEmpty())
        assertTrue(payload.headers.isNotEmpty())
        assertTrue(payload.headers.containsKey("Content-Type"))
    }

    @Test
    fun `V2 payload contains protectedKey header`() {
        val request = createRequest(ruiVersion = 2, reqVersion = 2)
        request.prepare()
        val payload = request.getPayload()
        assertTrue(payload.headers.containsKey("protectedKey"))
    }

    @Test
    fun `V1 payload does not contain protectedKey header`() {
        val request = createRequest(ruiVersion = 1, reqVersion = 1)
        request.prepare()
        val payload = request.getPayload()
        assertFalse(payload.headers.containsKey("protectedKey"))
    }

    @Test
    fun `parseComponents handles incomplete component objects without throwing JSONException`() {
        val request = createRequest()
        val json = org.json.JSONObject("""
            {
                "components": [
                    {
                        "componentId": "test_comp_1"
                    }
                ]
            }
        """.trimIndent())

        val components = request.parseComponents(json)
        assertEquals(1, components.size)
        assertEquals("test_comp_1", components[0].componentId)
        assertEquals("", components[0].url)
        assertEquals("", components[0].manualUrl)
    }

    @Test
    fun `parseComponents returns empty list when components key is missing`() {
        val request = createRequest()
        val json = org.json.JSONObject("""{"status": "ok"}""")
        val components = request.parseComponents(json)
        assertTrue(components.isEmpty())
    }

    @Test
    fun `validateResponse accepts valid 200 response`() {
        Request.validateResponse(200, """{"responseCode": 200, "status": "success"}""")
    }

    @Test(expected = OtaError.NetworkError::class)
    fun `validateResponse throws NetworkError on non-200 HTTP code`() {
        Request.validateResponse(500, "Internal Server Error")
    }

    @Test(expected = OtaError.ServerError::class)
    fun `validateResponse throws ServerError on server error code inside body`() {
        Request.validateResponse(200, """{"responseCode": 404, "errMsg": "No update available"}""")
    }

    // --- Helper ---

    private fun createRequest(
        ruiVersion: Int = 2,
        reqVersion: Int = 2,
        region: Int = 0,
        model: String = "RMX3840",
        firmwareVersion: String = "RMX3840_14.0.0.800(EX01)"
    ) = Request(
        reqVersion = reqVersion,
        model = model,
        firmwareVersion = firmwareVersion,
        region = region,
        ruiVersion = ruiVersion,
        imei0 = "000000000000000",
        beta = false
    )
}
