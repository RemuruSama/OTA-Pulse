package com.abhinav.otapulse.feature.devices.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.abhinav.otapulse.core.common.DeviceUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

// DeviceInfo remains the same for the initial full load
data class DeviceInfo(
    val deviceName: String,
    val deviceModel: String,
    val isSupported: Boolean,
    val androidVersion: String,
    val osVersion: String,
    val otaVersion: String,
    val incrementalOsVersion: String,
    val kernelVersion: String,
    val securityPatch: String,
    val totalRam: String, // Formatted string "X GB used of Y GB"
    val ramUsagePercent: Int,
    val soc: String,
    val cpuInfo: String,
    val storage: String, // Formatted string "X GB used of Y GB"
    val storageUsagePercent: Int,
    val battery: String, // Formatted string for battery level and status
    val display: String
)

// Data class for RAM specific updates
data class RamInfo(
    val usageString: String,
    val percent: Int
)

@HiltViewModel
class DeviceViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    // Existing deviceInfo for initial, mostly static data
    val deviceInfo: DeviceInfo by lazy {
        // ... (existing lazy initialization code remains the same)
        val (usedRamBytes, totalRamBytes) = DeviceUtils.getRamUsageInfo(context)
        val ramUsagePercent = if (totalRamBytes > 0) ((usedRamBytes * 100) / totalRamBytes).toInt() else 0
        val (usedStorageBytes, totalStorageBytes) = DeviceUtils.getStorageUsageInfo()
        val storageUsagePercent = if (totalStorageBytes > 0) ((usedStorageBytes * 100) / totalStorageBytes).toInt() else 0

        DeviceInfo(
            deviceName = DeviceUtils.getDeviceName(),
            deviceModel = DeviceUtils.getDeviceModel(),
            isSupported = DeviceUtils.getDeviceBrand() in listOf("OPPO", "OnePlus", "realme"),
            androidVersion = DeviceUtils.getAndroidVersion(),
            osVersion = DeviceUtils.getOsVersion(),
            otaVersion = DeviceUtils.getOtaVersion(),
            incrementalOsVersion = DeviceUtils.getIncrementalOsVersion(),
            kernelVersion = DeviceUtils.getKernelVersion(),
            securityPatch = DeviceUtils.getSecurityPatch(),
            totalRam = DeviceUtils.formatRamUsageString(usedRamBytes, totalRamBytes),
            ramUsagePercent = ramUsagePercent,
            soc = DeviceUtils.getSoc(),
            cpuInfo = DeviceUtils.getCpuInfo(),
            storage = DeviceUtils.formatStorageUsageString(usedStorageBytes, totalStorageBytes),
            storageUsagePercent = storageUsagePercent,
            battery = DeviceUtils.getBatteryInfo(context), // Initial battery info
            display = DeviceUtils.getDisplayInfo(context)
        )
    }

    // LiveData for RAM updates
    private val _ramInfoLiveData = MutableLiveData<RamInfo>()
    val ramInfoLiveData: LiveData<RamInfo> = _ramInfoLiveData

    // LiveData for Battery updates
    private val _batteryInfoLiveData = MutableLiveData<String>()
    val batteryInfoLiveData: LiveData<String> = _batteryInfoLiveData

    private val ramUpdateHandler = Handler(Looper.getMainLooper())
    private lateinit var ramUpdateRunnable: Runnable
    private var batteryInfoReceiver: BroadcastReceiver? = null

    companion object {
        private const val RAM_UPDATE_INTERVAL_MS = 2000L // Update RAM every 2 seconds
    }

    init {
        setupRamUpdater()
        setupBatteryUpdater()
        // Post initial values immediately if needed, or rely on first run of runnables/receiver
        // Initial values are already part of `deviceInfo` for the first paint.
        // The LiveData will start pushing updates shortly after.
    }

    private fun setupRamUpdater() {
        ramUpdateRunnable = Runnable {
            val (usedRamBytes, totalRamBytes) = DeviceUtils.getRamUsageInfo(context)
            val ramUsagePercent = if (totalRamBytes > 0) ((usedRamBytes * 100) / totalRamBytes).toInt() else 0
            val ramString = DeviceUtils.formatRamUsageString(usedRamBytes, totalRamBytes)
            _ramInfoLiveData.postValue(RamInfo(ramString, ramUsagePercent))
            ramUpdateHandler.postDelayed(ramUpdateRunnable, RAM_UPDATE_INTERVAL_MS)
        }
    }

    private fun setupBatteryUpdater() {
        batteryInfoReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == Intent.ACTION_BATTERY_CHANGED) {
                    // We can reuse the existing DeviceUtils.getBatteryInfo or re-implement parts here
                    // For simplicity, let's assume getBatteryInfo is efficient enough
                    _batteryInfoLiveData.postValue(DeviceUtils.getBatteryInfo(this@DeviceViewModel.context))
                }
            }
        }
    }

    fun startRealtimeUpdates() {
        // Start RAM updates
        ramUpdateHandler.post(ramUpdateRunnable)

        // Register Battery receiver
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        context.registerReceiver(batteryInfoReceiver, filter)

        // Post current battery state immediately
        _batteryInfoLiveData.postValue(DeviceUtils.getBatteryInfo(this.context))
    }

    fun stopRealtimeUpdates() {
        ramUpdateHandler.removeCallbacks(ramUpdateRunnable)
        try {
            if (batteryInfoReceiver != null) {
                context.unregisterReceiver(batteryInfoReceiver)
                batteryInfoReceiver = null // Avoid unregistering twice
            }
        } catch (e: IllegalArgumentException) {
            // Receiver not registered, ignore
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopRealtimeUpdates()
    }
}
