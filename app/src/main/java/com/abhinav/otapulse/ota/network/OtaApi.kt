package com.abhinav.otapulse.ota.network

import android.util.Log
import com.abhinav.otapulse.core.model.Device
import com.abhinav.otapulse.core.model.OtaError
import com.abhinav.otapulse.core.model.OtaUpdate
import com.abhinav.otapulse.core.model.RegionVariant
import com.abhinav.otapulse.core.network.NetworkComponent
import com.abhinav.otapulse.catalog.model.RegionData
import com.abhinav.otapulse.core.network.Request
import com.abhinav.otapulse.core.common.toDomain
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OtaApi @Inject constructor(private val httpClient: OkHttpClient) {

    companion object {
        private const val TAG = "OtaApi"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }

    /**
     * Shared method that executes an OTA request and returns the parsed components.
     * This is the single source of truth for the HTTP → decrypt → parse pipeline.
     */
    suspend fun executeOtaRequest(request: Request): Result<List<NetworkComponent>> = withContext(Dispatchers.IO) {
        try {
            val payload = request.getPayload()

            Log.d(TAG, "Request URL: ${request.url}")

            val requestBody = payload.body.toRequestBody(JSON_MEDIA_TYPE)
            val okRequest = okhttp3.Request.Builder()
                .url(request.url)
                .post(requestBody)
                .apply { payload.headers.forEach { (key, value) -> addHeader(key, value) } }
                .build()

            val response = httpClient.newCall(okRequest).execute()
            val responseCode = response.code
            val responseBody = response.body?.string() ?: ""

            // Validate HTTP and JSON structure
            Request.validateResponse(responseCode, responseBody)

            val jsonResponse = JSONObject(responseBody)

            if (jsonResponse.has(request.responseContentKey)) {
                val decryptedContent = try {
                    request.decrypt(jsonResponse.getString(request.responseContentKey))
                } catch (e: OtaError) {
                    throw e
                } catch (e: Exception) {
                    throw OtaError.DecryptionError("Failed to decrypt response: ${e.message}")
                }
                val content = JSONObject(decryptedContent)

                // Check for server-side errors (e.g., "update check failed")
                Request.validateContent(content)

                val components = request.parseComponents(content)
                if (components.isNotEmpty()) {
                    Result.success(components)
                } else {
                    Result.failure(OtaError.EmptyResponse())
                }
            } else {
                Result.failure(OtaError.EmptyResponse("Response missing content key '${request.responseContentKey}'."))
            }
        } catch (e: OtaError) {
            Log.e(TAG, "OTA Fetch Failed: ${e.message}")
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "OTA Fetch Failed", e)
            Result.failure(OtaError.Unknown(e))
        }
    }

    suspend fun fetchOtaDetails(device: Device, variant: RegionVariant): Result<OtaUpdate> = withContext(Dispatchers.IO) {
        val regionInfo = RegionData.regions.find {
            it.displayName.equals(variant.region, ignoreCase = true)
        }

        val regionIndex = when (regionInfo?.serverCode?.uppercase()) {
            "CN" -> 1
            "IN" -> 2
            "EU" -> 3
            else -> 0
        }

        val nvIdentifier = variant.nvId ?: regionInfo?.nvid

        Log.d(TAG, "Fetching OTA: Model=${variant.productModel}, Region=${variant.region} (Index=$regionIndex, NV=$nvIdentifier)")

        val request = Request(
            reqVersion = if (device.ruiVersion == 1) 1 else 2,
            model = variant.productModel,
            firmwareVersion = variant.firmwareVersion,
            region = regionIndex,
            ruiVersion = device.ruiVersion,
            imei0 = device.imei,
            beta = device.beta,
            nvIdentifier = nvIdentifier,
            language = variant.language
        )

        request.prepare()

        // Delegate to shared method
        val componentsResult = executeOtaRequest(request)
        componentsResult.map { components ->
            components.first().toDomain()
        }
    }
}
