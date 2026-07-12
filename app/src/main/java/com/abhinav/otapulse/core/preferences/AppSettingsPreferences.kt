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

package com.abhinav.otapulse.core.preferences

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

data class AppSettings(
    val advancedMode: Boolean = true,
    val autoUpdateCheck: Boolean = true,
    val autoSoftwareUpdateCheck: Boolean = true,
    val checkIntervalHours: Long = 6L,
    val arbDetection: Boolean = true,
    val browserDesktopMode: Boolean = false,
    val browserShowControls: Boolean = true
)

@Singleton
class AppSettingsPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        const val PREFS_NAME = "app_settings_prefs"
        const val PREF_ADVANCED_MODE_ENABLED = "advanced_mode_enabled"
        const val PREF_AUTO_UPDATE_CHECK = "auto_update_check_enabled"
        const val PREF_AUTO_SOFTWARE_UPDATE_CHECK = "auto_software_update_check_enabled"
        const val PREF_CHECK_INTERVAL_HOURS = "check_interval_hours"
        const val PREF_ARB_DETECTION_ENABLED = "arb_detection_enabled"
        const val PREF_BROWSER_DESKTOP_MODE = "browser_desktop_mode"
        const val PREF_BROWSER_SHOW_CONTROLS = "browser_show_controls"
        const val DEFAULT_CHECK_INTERVAL_HOURS = 6L
    }

    val appSettingsFlow: Flow<AppSettings> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
            trySend(getAppSettings())
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(getAppSettings())
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    fun getAppSettings(): AppSettings {
        return AppSettings(
            advancedMode = prefs.getBoolean(PREF_ADVANCED_MODE_ENABLED, true),
            autoUpdateCheck = prefs.getBoolean(PREF_AUTO_UPDATE_CHECK, true),
            autoSoftwareUpdateCheck = prefs.getBoolean(PREF_AUTO_SOFTWARE_UPDATE_CHECK, true),
            checkIntervalHours = prefs.getLong(PREF_CHECK_INTERVAL_HOURS, DEFAULT_CHECK_INTERVAL_HOURS),
            arbDetection = prefs.getBoolean(PREF_ARB_DETECTION_ENABLED, true),
            browserDesktopMode = prefs.getBoolean(PREF_BROWSER_DESKTOP_MODE, false),
            browserShowControls = prefs.getBoolean(PREF_BROWSER_SHOW_CONTROLS, true)
        )
    }

    fun setAdvancedMode(enabled: Boolean) {
        prefs.edit().putBoolean(PREF_ADVANCED_MODE_ENABLED, enabled).apply()
    }

    fun setAutoUpdateCheck(enabled: Boolean) {
        prefs.edit().putBoolean(PREF_AUTO_UPDATE_CHECK, enabled).apply()
    }

    fun setAutoSoftwareUpdateCheck(enabled: Boolean) {
        prefs.edit().putBoolean(PREF_AUTO_SOFTWARE_UPDATE_CHECK, enabled).apply()
    }

    fun setCheckIntervalHours(hours: Long) {
        prefs.edit().putLong(PREF_CHECK_INTERVAL_HOURS, hours).apply()
    }

    fun setArbDetection(enabled: Boolean) {
        prefs.edit().putBoolean(PREF_ARB_DETECTION_ENABLED, enabled).apply()
    }

    fun setBrowserDesktopMode(enabled: Boolean) {
        prefs.edit().putBoolean(PREF_BROWSER_DESKTOP_MODE, enabled).apply()
    }

    fun setBrowserShowControls(enabled: Boolean) {
        prefs.edit().putBoolean(PREF_BROWSER_SHOW_CONTROLS, enabled).apply()
    }
}
