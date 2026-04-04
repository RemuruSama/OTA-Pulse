package com.abhinav.otapulse.core.common

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import java.io.File
import java.text.DecimalFormat

object DeviceUtils {
    private const val TAG = "DeviceUtils"

    // Changed to Public so Fragments can use it for Auto-Fill
    fun getSystemProperty(key: String): String {
        return try {
            val prop = Class.forName("android.os.SystemProperties")
                .getMethod("get", String::class.java)
                .invoke(null, key) as String
            if (prop.isBlank()) "" else prop
        } catch (e: Exception) {
            ""
        }
    }

    fun getDeviceName(): String {
        val marketName = getSystemProperty("ro.vendor.oplus.market.name")
        return if (marketName.isNotBlank()) marketName else Build.MODEL
    }

    fun getDeviceModel(): String = "${Build.DEVICE} (${getSystemProperty("ro.product.vendor.name")})"
    fun getAndroidVersion(): String = Build.VERSION.RELEASE
    fun getOsVersion(): String = getSystemProperty("ro.build.display.id")
    fun getOtaVersion(): String = getSystemProperty("ro.build.version.ota")
    fun getIncrementalOsVersion(): String = Build.VERSION.INCREMENTAL
    fun getSecurityPatch(): String = Build.VERSION.SECURITY_PATCH
    fun getDeviceBrand(): String = getSystemProperty("ro.product.brand")

    fun getRamUsageInfo(context: Context): Pair<Long, Long> {
        return try {
            val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val memInfo = ActivityManager.MemoryInfo()
            actManager.getMemoryInfo(memInfo)
            val totalRamBytes = memInfo.totalMem
            val usedRamBytes = totalRamBytes - memInfo.availMem
            Pair(usedRamBytes, totalRamBytes)
        } catch (e: Exception) {
            Log.e(TAG, "Error reading RAM info", e)
            Pair(0L, 0L)
        }
    }

    fun formatRamUsageString(usedRamBytes: Long, totalRamBytes: Long): String {
        if (totalRamBytes <= 0) return "Unknown"
        return try {
            val df = DecimalFormat("#.##")
            val usedRamGb = usedRamBytes / (1024.0 * 1024.0 * 1024.0)
            val totalRamGb = totalRamBytes / (1024.0 * 1024.0 * 1024.0)
            "${df.format(usedRamGb)} GB used of ${df.format(totalRamGb)} GB"
        } catch (e: Exception) {
            "Unknown"
        }
    }

    fun getSoc(): String {
        val manufacturer = getSystemProperty("ro.soc.manufacturer")
        val model = getSystemProperty("ro.soc.model")
        val board = getSystemProperty("ro.product.board")
        if ((manufacturer == "Unknown" || manufacturer.isBlank()) && (model == "Unknown" || model.isBlank())) return Build.HARDWARE
        return "$manufacturer $model ($board)".trim()
    }

    fun getStorageUsageInfo(): Pair<Long, Long> {
        return try {
            val internalStatFs = StatFs(Environment.getDataDirectory().path)
            val totalInternalBytes = internalStatFs.blockCountLong * internalStatFs.blockSizeLong
            val availableInternalBytes = internalStatFs.availableBlocksLong * internalStatFs.blockSizeLong
            val usedInternalBytes = totalInternalBytes - availableInternalBytes
            Pair(usedInternalBytes, totalInternalBytes)
        } catch (e: Exception) {
            Log.e(TAG, "Error reading storage info", e)
            Pair(0L, 0L)
        }
    }

    fun formatStorageUsageString(usedStorageBytes: Long, totalStorageBytes: Long): String {
        if (totalStorageBytes <= 0) return "Unknown"
        return try {
            val df = DecimalFormat("#.##")
            val usedGb = usedStorageBytes / (1024.0 * 1024.0 * 1024.0)
            val totalGb = totalStorageBytes / (1024.0 * 1024.0 * 1024.0)
            "${df.format(usedGb)} GB used of ${df.format(totalGb)} GB"
        } catch (e: Exception) {
            "Unknown"
        }
    }

    private fun getBatteryCapacityMah(context: Context): Long? {
        try {
            val psRoot = File("/sys/class/power_supply")
            if (psRoot.exists() && psRoot.isDirectory) {
                // Simplified safe check
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "SELinux blocked access to /sys/class/power_supply: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Generic error scanning /sys/class/power_supply", e)
        }

        try {
            val ppClass = Class.forName("com.android.internal.os.PowerProfile")
            val ctor = ppClass.getConstructor(Context::class.java)
            val powerProfile = ctor.newInstance(context)
            val method = ppClass.getMethod("getBatteryCapacity")
            val cap = method.invoke(powerProfile) as? Double
            if (cap != null && cap > 0) return cap.toLong()
        } catch (e: Exception) {
            // Ignore
        }
        return null
    }

    fun getBatteryInfo(context: Context): String {
        return try {
            val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            val batteryLevel = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)

            val intentFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val batteryStatusIntent = context.registerReceiver(null, intentFilter)
            val status = batteryStatusIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL

            val chargingStatus = if (isCharging) "Charging" else "Not Charging"
            val baseInfo = "$batteryLevel% ($chargingStatus)"
            baseInfo
        } catch (e: Exception) {
            Log.e(TAG, "Error getting battery info", e)
            "Unknown"
        }
    }

    fun getDisplayInfo(context: Context): String {
        return try {
            val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val displayMetrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            val display = windowManager.defaultDisplay
            @Suppress("DEPRECATION")
            display.getRealMetrics(displayMetrics)
            "${displayMetrics.widthPixels} x ${displayMetrics.heightPixels} @ ${displayMetrics.densityDpi}dpi"
        } catch (e: Exception) {
            "Unknown"
        }
    }

    fun getCpuInfo(): String {
        return try {
            val cpuClusters = mutableMapOf<Long, Int>()
            val cpuDir = File("/sys/devices/system/cpu/")
            val cpuFiles = cpuDir.listFiles { _, name -> name.matches(Regex("cpu[0-9]+")) } ?: return "Unknown"
            cpuFiles.forEach { cpuFile ->
                val maxFreqFile = File(cpuFile, "cpufreq/cpuinfo_max_freq")
                if (maxFreqFile.exists() && maxFreqFile.canRead()) {
                    val maxFreq = maxFreqFile.readText().trim().toLong()
                    cpuClusters[maxFreq] = (cpuClusters[maxFreq] ?: 0) + 1
                }
            }
            if (cpuClusters.isEmpty()) {
                Log.w(TAG, "CPU cluster info is empty.")
                return "Unknown"
            }
            val result = StringBuilder()
            cpuClusters.toSortedMap(compareByDescending { it }).forEach { (freq, count) ->
                val freqGhz = freq / 1000000.0
                val formattedFreq = if (freqGhz % 1 == 0.0) "%.0f".format(freqGhz) else "%.3f".format(freqGhz)
                if (result.isNotEmpty()) result.append("\n")
                result.append("• $formattedFreq GHz ($count)")
            }
            result.toString()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting CPU info", e)
            "Unknown"
        }
    }
    fun getKernelVersion(): String = System.getProperty("os.version") ?: "Unknown"
}
