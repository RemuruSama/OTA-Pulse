package com.abhinav.otapulse.data.repository

import android.content.Context
import com.abhinav.otapulse.domain.model.OtaRequest
import com.abhinav.otapulse.domain.repository.OtaRepository
import com.abhinav.otapulse.util.NetworkComponent
import com.abhinav.otapulse.util.Request
import com.abhinav.otapulse.util.toDomain
import com.abhinav.otapulse.util.PredefinedDevice
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

import com.abhinav.otapulse.data.local.CustomDeviceManager
import com.abhinav.otapulse.di.FavoritesPrefs
import com.abhinav.otapulse.util.DeviceCatalog
import android.content.SharedPreferences

@Singleton
class OtaRepositoryImpl @Inject constructor(
    private val client: OkHttpClient,
    @FavoritesPrefs private val favoritesPrefs: SharedPreferences,
    private val customDeviceManager: CustomDeviceManager,
    @ApplicationContext private val context: Context
) : OtaRepository {
    
    // In-memory cache for devices to support Flow
    private val _devicesFlow = MutableStateFlow<List<com.abhinav.otapulse.domain.model.Device>>(emptyList())

    init {
        updateDevicesCache()
    }

    private fun updateDevicesCache() {
        val favorites = getFavorites()
        val fixedDevices = DeviceCatalog.predefinedDevices.map { it.toDomain() }
        val customDevices = customDeviceManager.getCustomDevices().map { it.toDomain() }
        
        val allDevices = (fixedDevices + customDevices).distinctBy { it.name }
        
        allDevices.forEach { device ->
            device.isFavorite = favorites[device.name] ?: false
        }
        _devicesFlow.value = allDevices.sortedByDescending { it.isFavorite }
    }

    override suspend fun fetchOtaUpdate(otaRequest: OtaRequest): Result<List<NetworkComponent>> = withContext(Dispatchers.IO) {
        try {
            // 1. Prepare Request (Using existing Request logic)
            // Ideally, the logic inside `Request` class should be extracted here, but it's complex encryption.
            // I will use `Request` as a helper.
            val requestHelper = Request(
                reqVersion = otaRequest.version,
                model = otaRequest.model,
                firmwareVersion = otaRequest.firmwareVersion,
                region = otaRequest.region,
                ruiVersion = otaRequest.ruiVersion,
                imei0 = otaRequest.imei0,
                beta = otaRequest.beta,
                deviceId = otaRequest.deviceId,
                nvIdentifier = otaRequest.nvIdentifier,
                imei1 = otaRequest.imei1,
                language = otaRequest.language
            )

            requestHelper.prepare()
            val payload = requestHelper.getPayload()
            
            // 2. Resolve URL
            val targetUrl = requestHelper.url // Request.kt resolves it internally now? No, it has `resolveUrl`.
            // Wait, `Request.prepare()` sets `url`.
            
            // 3. Execute Network Call
            val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
            val body = payload.body.toRequestBody(mediaType)
            
            val requestBuilder = okhttp3.Request.Builder()
                .url(targetUrl)
                .post(body)
                
            payload.headers.forEach { (k, v) -> requestBuilder.addHeader(k, v) }
            
            val response = client.newCall(requestBuilder.build()).execute()
            val responseBodyString = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("HTTP ${response.code}"))
            }

            // 4. Decrypt and Parse
            // Validation
            Request.validateResponse(response.code, responseBodyString)
            
            // Decrypt
            val jsonResponse = JSONObject(responseBodyString)
            if (!jsonResponse.has(requestHelper.responseContentKey)) {
                return@withContext Result.failure(Exception("Response missing content key '${requestHelper.responseContentKey}'"))
            }

            val encryptedContent = jsonResponse.getString(requestHelper.responseContentKey)
            val decrypted = requestHelper.decrypt(encryptedContent)
            val jsonContent = JSONObject(decrypted)
            
            // Validate Content
            Request.validateContent(jsonContent)
            
            // Parse
            val components = requestHelper.parseComponents(jsonContent)
            
            return@withContext Result.success(components)

        } catch (e: Exception) {
            return@withContext Result.failure(e)
        }
    }

    override fun getDevices(): Flow<List<com.abhinav.otapulse.domain.model.Device>> = _devicesFlow.asStateFlow()

    override fun getFavorites(): Map<String, Boolean> {
        val favoritesJson = favoritesPrefs.getString("favorites", "{}") ?: "{}"
        val json = JSONObject(favoritesJson)
        val map = mutableMapOf<String, Boolean>()
        json.keys().forEach { key ->
            map[key] = json.getBoolean(key)
        }
        return map
    }

    override fun toggleFavoriteStatus(deviceName: String, isFavorite: Boolean) {
        val favorites = getFavorites().toMutableMap()
        if (isFavorite) {
            favorites[deviceName] = true
        } else {
            favorites.remove(deviceName)
        }
        
        val json = JSONObject()
        favorites.forEach { (k, v) -> json.put(k, v) }
        favoritesPrefs.edit().putString("favorites", json.toString()).apply()
        
        updateDevicesCache()
    }

    override fun addCustomDevice(device: PredefinedDevice) {
        customDeviceManager.addDevice(device)
        updateDevicesCache()
    }

    override fun updateCustomDevice(oldName: String, newDevice: PredefinedDevice) {
        customDeviceManager.updateDevice(oldName, newDevice)
        updateDevicesCache()
    }

    override fun deleteCustomDevice(deviceName: String) {
        customDeviceManager.deleteDevice(deviceName)
        updateDevicesCache()
    }
}
