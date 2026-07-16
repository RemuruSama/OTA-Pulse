package com.abhinav.otapulse.core.network

import com.abhinav.otapulse.core.common.Crypto
import com.abhinav.otapulse.core.model.OtaError
import org.json.JSONException
import org.json.JSONObject

data class RequestPayload(val body: String, val headers: Map<String, String>)

/**
 * Refactored Request class to handle OTA protocol logic.
 * Adapted for ColorOS 16 / Android 16 JSON structure.
 */
class Request(
    private val reqVersion: Int,
    private val model: String,
    private val firmwareVersion: String,
    private val region: Int,
    private val ruiVersion: Int,
    private val imei0: String?,
    private val beta: Boolean,
    private val deviceId: String? = null,
    private val nvIdentifier: String? = null,
    private val imei1: String? = null,
    private val language: String? = "en-EN",
    private val reqMode: String? = "manual",
    private val gray: Int = 0
) {
    private val properties: MutableMap<String, Any> = mutableMapOf()
    private var v2SymmetricKey: String? = null
    lateinit var responseContentKey: String private set
    lateinit var url: String private set

    init {
        initializeProperties()
    }

    private fun initializeProperties() {
        properties["model"] = model
        properties["productName"] = model
        properties["otaVersion"] = firmwareVersion
        properties["rui_version"] = ruiVersion
        properties["region"] = region
        properties["imei"] = imei0 ?: Data.defaultBody["imei"]!!
        properties["imei1"] = imei1 ?: Data.defaultBody["imei1"]!!
        properties["otaPrefix"] = firmwareVersion.split("_").take(2).joinToString("_")
        properties["romVersion"] = firmwareVersion.split("_").take(2).joinToString("_")
        properties["nvId"] = nvIdentifier ?: "0"
        properties["language"] = language ?: "en-EN"
        properties["gray"] = gray.toString()
        properties["reqMode"] = reqMode ?: "manual"

        val idToHash = deviceId ?: imei0 ?: Data.defaultHeaders["deviceId"]!!
        properties["deviceId"] = Crypto.sha256(idToHash)

        if (ruiVersion == 1) properties["version"] = "2"
    }

    fun prepare() {
        val region = properties["region"] as Int
        val ruiVersion = properties["rui_version"] as Int
        val nvId = properties["nvId"] as String
        val regionStr = when (region) { 1 -> "CN" 2 -> "IN" 3 -> "EU" else -> "GL" }

        properties["trackRegion"] = regionStr
        properties["uRegion"] = regionStr

        if (region == 1 && properties["language"] == "en-EN") {
            properties["language"] = "zh-CN"
        }

        properties["nvCarrier"] = resolveNvCarrier(nvId, region)

        // Android Version Calculation
        // For RUI 1-5: 10 + RUI - 1 (e.g. RUI 5 = Android 14)
        // For RUI 6+: Direct offset of 10 (e.g. RUI 6 = Android 16)
        val androidVer = if (ruiVersion >= 6) 10 + ruiVersion else 10 + ruiVersion - 1
        properties["androidVersion"] = "Android${androidVer}.0"

        // ColorOS Version Calculation
        // For RUI 1: ColorOS 7
        // For RUI 2-5: 11 + RUI - 2 (e.g. RUI 5 = ColorOS 14)
        // For RUI 6+: Direct offset of 10 (e.g. RUI 6 = ColorOS 16)
        val cosVer = if (ruiVersion >= 6) 10 + ruiVersion else if (ruiVersion == 1) 7 else 11 + ruiVersion - 2
        properties["colorOSVersion"] = "ColorOS$cosVer"
        properties["isRealme"] = if (model.contains("RMX", ignoreCase = true)) "1" else "0"
        properties["time"] = System.currentTimeMillis().toString()

        url = resolveUrl(ruiVersion, region)
        responseContentKey = if (reqVersion == 2 && ruiVersion >= 2) "body" else "params"
        try {
            android.util.Log.d("RequestDebug", "Prepared Request -> url: $url, model: $model, firmwareVersion: $firmwareVersion, region: $region, ruiVersion: $ruiVersion, nvId: $nvId, nvCarrier: ${properties["nvCarrier"]}, reqMode: ${properties["reqMode"]}, otaPrefix: ${properties["otaPrefix"]}")
        } catch (_: Throwable) {}
    }

    private fun resolveNvCarrier(nvId: String, region: Int): String {
        return if (nvId == "0" || nvId.isBlank()) {
            when (region) {
                1 -> "10010111"
                3 -> "01000100"
                else -> "00011011"
            }
        } else {
            nvId
        }
    }

    private fun resolveUrl(ruiVersion: Int, region: Int): String {
        return when {
            ruiVersion >= 2 && reqVersion == 2 -> Data.serverParams.getValue(region).serverURL
            else -> Data.urls.getValue(minOf(ruiVersion, 2)).getValue(region)
        }
    }

    fun getPayload(): RequestPayload {
        val rawBody = Data.defaultBody.toMutableMap()
        properties.forEach { (key, value) ->
            if (rawBody.containsKey(key)) rawBody[key] = value.toString()
        }
        if (beta) rawBody["mode"] = "1"

        val bodyJson = JSONObject(rawBody as Map<*, *>)
        val headers = buildHeaders()
        val bodyString: String

        if (reqVersion == 2 && properties["rui_version"] as Int >= 2) {
            val (cipher, key, iv) = Crypto.encryptCtrV2(bodyJson.toString())
            this.v2SymmetricKey = key

            val paramsObject = JSONObject(mapOf("cipher" to cipher, "iv" to iv))
            bodyString = JSONObject(mapOf("params" to paramsObject.toString())).toString()

            val region = properties["region"] as Int
            val serverConfig = Data.serverParams.getValue(region)
            val protectedKey = Crypto.generateProtectedKey(key, serverConfig.pubKey)

            val time = (properties["time"] as String).toLong()
            val version = (time + 86400 * 1000).toString()

            val protectedKeyHeader = JSONObject(mapOf(
                "SCENE_1" to mapOf(
                    "protectedKey" to protectedKey,
                    "version" to version,
                    "negotiationVersion" to serverConfig.negotiationVersion
                )
            ))
            headers["protectedKey"] = protectedKeyHeader.toString()
        } else {
            val cipher = if (properties["rui_version"] == 1) {
                Crypto.encryptEcb(bodyJson.toString())
            } else {
                Crypto.encryptCtrV1(bodyJson.toString())
            }
            bodyString = JSONObject(mapOf("params" to cipher)).toString()
        }
        return RequestPayload(body = bodyString, headers = headers)
    }

    fun decrypt(responseBody: String): String {
        return try {
            when {
                reqVersion == 2 && properties["rui_version"] as Int >= 2 -> {
                    val key = v2SymmetricKey
                        ?: error("decrypt() called before getPayload(); v2SymmetricKey is unset")
                    val bodyObject = JSONObject(responseBody)
                    Crypto.decryptCtrV2(
                        bodyObject.getString("cipher"),
                        key,
                        bodyObject.getString("iv")
                    )
                }
                properties["rui_version"] == 1 -> Crypto.decryptEcb(responseBody)
                else -> Crypto.decryptCtrV1(responseBody)
            }
        } catch (e: Exception) {
            throw RuntimeException("Failed to decrypt response: $responseBody", e)
        }
    }

    fun parseComponents(content: JSONObject): List<NetworkComponent> {
        val componentsList = mutableListOf<NetworkComponent>()
        val rawJson = content.toString(2)

        // Adaptive Parsing: Check if the useful content is wrapped inside a "body" object
        val dataRoot = if (content.has("body")) content.getJSONObject("body") else content

        val componentsArray = dataRoot.optJSONArray("components") ?: return emptyList()

        for (i in 0 until componentsArray.length()) {
            val componentObject = componentsArray.getJSONObject(i)
            val componentPackets = componentObject.getJSONObject("componentPackets")
            val vabInfo = componentPackets.optJSONObject("vabInfo")?.optJSONObject("data")

            val headerMap = extractHeaderMap(vabInfo)

            // Description parsing logic
            val descriptionObj = dataRoot.optJSONObject("description")
            val changelogUrl = descriptionObj?.optString("panelUrl") ?: ""
            val h5Url = descriptionObj?.optString("url") ?: ""

            // Clean URLs by aggressively filtering out ANY whitespace character (spaces, tabs, newlines, unicode spaces)
            val cleanManualUrl = componentPackets.optString("manualUrl").filter { !it.isWhitespace() }
            val cleanUrl = componentPackets.getString("url").filter { !it.isWhitespace() }
            val cleanPanelUrl = changelogUrl.filter { !it.isWhitespace() }
            val cleanH5Url = h5Url.filter { !it.isWhitespace() }

            componentsList.add(
                NetworkComponent(
                    componentId = componentObject.getString("componentId"),
                    componentName = componentObject.getString("componentName"),
                    componentVersion = componentObject.getString("componentVersion"),
                    size = componentPackets.getString("size"),
                    manualUrl = cleanManualUrl,
                    url = cleanUrl,
                    md5 = componentPackets.getString("md5"),
                    otaStreamingProperty = vabInfo?.optString("otaStreamingProperty"),
                    vabPackageHash = vabInfo?.optString("vab_package_hash"),
                    extraParams = vabInfo?.optString("extra_params"),
                    fileHash = headerMap["FILE_HASH"],
                    fileSize = headerMap["FILE_SIZE"],
                    metadataHash = headerMap["METADATA_HASH"],
                    metadataSize = headerMap["METADATA_SIZE"],
                    androidVersion = headerMap["android_version"],
                    oplusRomVersion = headerMap["oplus_rom_version"],
                    securityPatch = headerMap["security_patch"] ?: dataRoot.optString("securityPatch").takeIf { it.isNotEmpty() },
                    securityPatchVendor = headerMap["security_patch_vendor"] ?: dataRoot.optString("securityPatchVendor").takeIf { it.isNotEmpty() } ?: headerMap["security_patch"] ?: dataRoot.optString("securityPatch").takeIf { it.isNotEmpty() },
                    versionTypeId = dataRoot.optString("versionTypeId"),
                    versionName = dataRoot.optString("versionName"),
                    // New fields from latest JSON
                    realVersionName = dataRoot.optString("realVersionName"),
                    publishedTime = dataRoot.optLong("publishedTime", 0),
                    status = dataRoot.optString("status"),
                    // Existing fields
                    realAndroidVersion = dataRoot.optString("realAndroidVersion"),
                    realOsVersion = dataRoot.optString("realOsVersion"),
                    osVersion = dataRoot.optString("osVersion"),
                    colorOSVersion = dataRoot.optString("colorOSVersion"),
                    panelUrl = cleanPanelUrl,
                    realOtaVersion = dataRoot.optString("realOtaVersion").takeIf { it.isNotEmpty() } ?: dataRoot.optString("otaVersion").takeIf { it.isNotEmpty() },
                    rawJson = rawJson,
                    nvId16 = dataRoot.optString("nvId16").takeIf { it.isNotEmpty() },
                    packetId = componentPackets.optString("id").takeIf { it.isNotEmpty() },
                    packetType = componentPackets.optString("type").takeIf { it.isNotEmpty() },
                    forbidOtaLocalUpdate = headerMap["forbid_ota_local_update"],
                    otaRootOrDebug = headerMap["ota_root_or_debug"],
                    otaTargetVersion = headerMap["ota_target_version"],
                    oplusSeparateSoft = headerMap["oplus_separate_soft"],
                    descriptionUrl = cleanH5Url.takeIf { it.isNotEmpty() },
                    nightUpdateLimit = dataRoot.optString("nightUpdateLimit").takeIf { it.isNotEmpty() },
                    versionTypeH5 = dataRoot.optString("versionTypeH5").takeIf { it.isNotEmpty() }
                )
            )
        }
        return componentsList
    }

    private fun extractHeaderMap(vabInfo: JSONObject?): Map<String, String> {
        val headerMap = mutableMapOf<String, String>()
        vabInfo?.optJSONArray("header")?.let { array ->
            for (j in 0 until array.length()) {
                val entry = array.getString(j)
                // Use indexOf instead of split so values containing '=' (e.g. Base64
                // padding or date strings like "2025-04=01") are never truncated.
                val eqIdx = entry.indexOf('=')
                if (eqIdx > 0) {
                    val key = entry.substring(0, eqIdx)
                    val value = entry.substring(eqIdx + 1)
                    headerMap[key] = value
                }
            }
        }
        return headerMap
    }

    private fun buildHeaders(): MutableMap<String, String> {
        val headers = Data.defaultHeaders.toMutableMap()
        properties.forEach { (key, value) ->
            if (headers.containsKey(key)) headers[key] = value.toString()
        }
        if (reqVersion == 2) headers["version"] = "2"
        // Override header 'mode' with reqMode (e.g. "manual", "client_auto", "server_auto", "taste")
        headers["mode"] = (reqMode ?: "manual")
        return headers
    }

    companion object {
        fun validateResponse(responseCode: Int, responseBody: String) {
            if (responseCode != 200) throw OtaError.NetworkError(responseCode, "HTTP Error: $responseCode. Body: $responseBody")
            try {
                val json = JSONObject(responseBody)
                // Check outer body if it exists, or root
                val toCheck = if (json.has("body") && json.optJSONObject("body") != null) {
                    json.getJSONObject("body")
                } else {
                    json
                }

                if (toCheck.optInt("responseCode", 200) != 200) {
                    throw OtaError.ServerError(toCheck.getInt("responseCode"), toCheck.optString("errMsg", "Unknown server error"))
                }
            } catch (e: OtaError) {
                throw e
            } catch (e: JSONException) {
                throw OtaError.ServerError(responseCode, "Server returned non-JSON response: $responseBody")
            }
        }

        fun validateContent(content: JSONObject) {
            val toCheck = if (content.has("body")) content.getJSONObject("body") else content
            if (toCheck.has("checkFailReason") && toCheck.get("checkFailReason") != JSONObject.NULL) {
                throw OtaError.UpdateCheckFailed(toCheck.getString("checkFailReason"))
            }
        }
    }
}
