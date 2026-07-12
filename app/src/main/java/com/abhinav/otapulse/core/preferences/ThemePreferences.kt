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
import androidx.appcompat.app.AppCompatDelegate
import com.abhinav.otapulse.core.ui.theme.ThemeMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

data class ThemeSettings(
    val themeMode: ThemeMode = ThemeMode.MATERIAL_YOU,
    val nightMode: Int = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM,
    val amoledDark: Boolean = false,
    val dynamicColor: Boolean = true,
    val gradientBackground: Boolean = true
)

@Singleton
class ThemePreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)

    companion object {
        const val PREFS_NAME = "theme_prefs"
        const val PREF_THEME_MODE = "theme_mode"
        const val PREF_NIGHT_MODE = "night_mode"
        const val PREF_AMOLED_MODE = "amoled_mode"
        const val PREF_DYNAMIC_COLOR = "dynamic_color_enabled"
        const val PREF_GRADIENT_BACKGROUND = "gradient_background_enabled"
    }

    val themeSettingsFlow: Flow<ThemeSettings> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
            trySend(getThemeSettings())
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(getThemeSettings())
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    fun getThemeSettings(): ThemeSettings {
        val modeStr = prefs.getString(PREF_THEME_MODE, ThemeMode.MATERIAL_YOU.name) ?: ThemeMode.MATERIAL_YOU.name
        val themeMode = try {
            ThemeMode.valueOf(modeStr)
        } catch (e: Exception) {
            ThemeMode.MATERIAL_YOU
        }
        val nightMode = prefs.getInt(PREF_NIGHT_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        val amoledDark = prefs.getBoolean(PREF_AMOLED_MODE, false)
        val dynamicColor = prefs.getBoolean(PREF_DYNAMIC_COLOR, true)
        val gradientBackground = prefs.getBoolean(PREF_GRADIENT_BACKGROUND, true)

        return ThemeSettings(
            themeMode = themeMode,
            nightMode = nightMode,
            amoledDark = amoledDark,
            dynamicColor = dynamicColor,
            gradientBackground = gradientBackground
        )
    }

    fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putString(PREF_THEME_MODE, mode.name).apply()
        if (mode == ThemeMode.HOLOGRAPHIC) {
            setNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        }
    }

    fun setNightMode(mode: Int) {
        prefs.edit().putInt(PREF_NIGHT_MODE, mode).apply()
        AppCompatDelegate.setDefaultNightMode(mode)
    }

    fun setAmoledDark(enabled: Boolean) {
        prefs.edit().putBoolean(PREF_AMOLED_MODE, enabled).apply()
        if (enabled && getThemeSettings().nightMode == AppCompatDelegate.MODE_NIGHT_NO) {
            setNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        }
    }

    fun setDynamicColor(enabled: Boolean) {
        prefs.edit().putBoolean(PREF_DYNAMIC_COLOR, enabled).apply()
    }

    fun setGradientBackground(enabled: Boolean) {
        prefs.edit().putBoolean(PREF_GRADIENT_BACKGROUND, enabled).apply()
    }
}
