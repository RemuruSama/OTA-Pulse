package com.abhinav.otapulse.core.common

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.abhinav.otapulse.R
import java.util.Locale

object LocaleHelper {

    fun applyLocale(context: Context, localeTag: String) {
        val appLocale: LocaleListCompat = if (localeTag == "system") {
            LocaleListCompat.getEmptyLocaleList()
        } else {
            LocaleListCompat.forLanguageTags(localeTag)
        }
        AppCompatDelegate.setApplicationLocales(appLocale)
    }

    fun getSelectedLocale(context: Context): String {
        val locales = AppCompatDelegate.getApplicationLocales()
        return if (locales.isEmpty) {
            "system"
        } else {
            val tag = locales.toLanguageTags()
            // Normalize common discrepancies
            when {
                tag.contains("zh-TW", ignoreCase = true) || tag.contains("zh-rTW", ignoreCase = true) || tag.contains("zh-Hant", ignoreCase = true) -> "zh-TW"
                tag.contains("zh-CN", ignoreCase = true) || tag.contains("zh-rCN", ignoreCase = true) || tag.contains("zh-Hans", ignoreCase = true) -> "zh"
                tag.startsWith("zh") -> "zh"
                tag.contains("pt-BR", ignoreCase = true) || tag.contains("pt-rBR", ignoreCase = true) -> "pt-BR"
                tag.contains("pt-PT", ignoreCase = true) || tag.contains("pt-rPT", ignoreCase = true) -> "pt-PT"
                tag.startsWith("pt") -> "pt-BR"
                tag.startsWith("tl") || tag.startsWith("fil") -> "fil"
                tag.startsWith("in") || tag.startsWith("id") -> "id"
                else -> tag
            }
        }
    }

    fun getDisplayName(context: Context, localeTag: String): String {
        if (localeTag == "system" || localeTag.isEmpty()) return context.getString(R.string.language_option_system)
        val locale = Locale.forLanguageTag(localeTag)
        return locale.getDisplayName(locale).replaceFirstChar { it.uppercase() }
    }
}
