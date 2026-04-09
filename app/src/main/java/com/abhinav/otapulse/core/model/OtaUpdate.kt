package com.abhinav.otapulse.core.model

import android.os.Parcelable
import com.google.gson.Gson
import com.tonyodev.fetch2core.Extras
import kotlinx.parcelize.Parcelize

@Parcelize
data class OtaUpdate(
    val componentId: String,
    val componentName: String,
    val componentVersion: String,
    val size: String,
    val manualUrl: String?,
    val url: String,
    val md5: String,
    val versionName: String?,
    val realAndroidVersion: String?,
    val realOsVersion: String?,
    val securityPatch: String?,
    val panelUrl: String?,
    val arbStatus: String? = null,
    val fileName: String,
    val downloadUrl: String,
    val rawJson: String? = null
) : Parcelable {
    companion object {
        fun fromString(otaUpdateString: String): OtaUpdate? {
            return try {
                Gson().fromJson(otaUpdateString, OtaUpdate::class.java)
            } catch (e: Exception) {
                null
            }
        }
    }
}

fun Map<String, String>.toData(): Extras {
    val map = mutableMapOf<String, String>()
    this.forEach { (key, value) ->
        map[key] = value
    }
    return Extras(map)
}
