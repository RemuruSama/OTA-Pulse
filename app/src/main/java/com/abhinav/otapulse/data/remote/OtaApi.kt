package com.abhinav.otapulse.data.remote

import android.util.Log
import com.abhinav.otapulse.domain.model.Device
import com.abhinav.otapulse.domain.model.OtaUpdate
import com.abhinav.otapulse.domain.model.RegionVariant
import com.abhinav.otapulse.util.RegionData
import com.abhinav.otapulse.util.Request
import com.abhinav.otapulse.util.toDomain
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OtaApi @Inject constructor() {

    companion object {
        private const val TAG = "OtaApi"
    }

    suspend fun fetchOtaDetails(device: Device, variant: RegionVariant): Result<OtaUpdate> = withContext(Dispatchers.IO) {
        try {
            // 1. Resolve Region Info
            // variant.region contains the Display Name (e.g., "IN", "GLO", "RU")
            val regionInfo = RegionData.regions.find {
                it.displayName.equals(variant.region, ignoreCase = true)
            }

            // 2. Determine Server Index
            // Map the serverCode (GL, CN, IN, EU) to the API's integer index
            val regionIndex = when (regionInfo?.serverCode?.uppercase()) {
                "CN" -> 1
                "IN" -> 2
                "EU" -> 3
                else -> 0 // Defaults to "GL" (Global) index
            }


            // 3. Determine NV Identifier
            // Critical: Use the specific NV ID from RegionData (e.g., "NV1B", "NV37")
            // If explicit NV ID is provided in variant (e.g. from Custom Mode), use it.
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
                nvIdentifier = nvIdentifier, // Explicitly pass the NV ID
                language = variant.language // Pass language if available
            )

            request.prepare()

            val payload = request.getPayload()
            val url = URL(request.url)

            Log.d(TAG, "Request URL: $url")

            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.connectTimeout = 10000
            connection.readTimeout = 10000

            payload.headers.forEach { (key, value) -> connection.setRequestProperty(key, value) }

            OutputStreamWriter(connection.outputStream).use { it.write(payload.body) }

            val responseCode = connection.responseCode
            val responseBody = connection.inputStream.bufferedReader().use { it.readText() }

            // Validate HTTP and JSON structure
            Request.validateResponse(responseCode, responseBody)

            val jsonResponse = JSONObject(responseBody)

            if (jsonResponse.has(request.responseContentKey)) {
                // Decrypt logic
                val decryptedContent = request.decrypt(jsonResponse.getString(request.responseContentKey))
                val content = JSONObject(decryptedContent)

                // Check for server-side errors (e.g., "update check failed")
                Request.validateContent(content)

                val components = request.parseComponents(content)
                if (components.isNotEmpty()) {
                    Result.success(components.first().toDomain())
                } else {
                    Result.failure(Exception("Server returned empty component list."))
                }
            } else {
                Result.failure(Exception("Response missing content key '${request.responseContentKey}'."))
            }
        } catch (e: Exception) {
            Log.e(TAG, "OTA Fetch Failed", e)
            Result.failure(e)
        }
    }
}