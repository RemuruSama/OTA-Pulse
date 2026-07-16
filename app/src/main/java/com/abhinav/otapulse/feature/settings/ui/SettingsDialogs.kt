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

package com.abhinav.otapulse.feature.settings.ui

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.abhinav.otapulse.R
import com.abhinav.otapulse.core.ui.ApplyDialogBlurEffect
import com.abhinav.otapulse.core.common.LocaleHelper
import com.abhinav.otapulse.core.common.openExternalBrowser
import com.abhinav.otapulse.core.ui.components.OtaOutlinedButton
import com.abhinav.otapulse.core.ui.components.OtaPrimaryButton
import com.abhinav.otapulse.core.ui.components.OtaTonalButton
import com.abhinav.otapulse.core.ui.theme.OtaPulseTheme

@Composable
fun LanguageSelectionDialog(
    onDismiss: () -> Unit,
    onLanguageSelected: (String) -> Unit
) {
    val context = LocalContext.current
    val currentLocaleTag = LocaleHelper.getSelectedLocale(context)

    val languages = linkedMapOf(
        "system" to context.getString(R.string.language_option_system),
        "ar"    to context.getString(R.string.lang_ar),
        "bn"    to context.getString(R.string.lang_bn),
        "zh"    to context.getString(R.string.lang_zh),
        "zh-TW" to context.getString(R.string.lang_zh_TW),
        "fr"    to context.getString(R.string.lang_fr),
        "de"    to context.getString(R.string.lang_de),
        "hi"    to context.getString(R.string.lang_hi),
        "id"    to context.getString(R.string.lang_id),
        "it"    to context.getString(R.string.lang_it),
        "ja"    to context.getString(R.string.lang_ja),
        "ms"    to context.getString(R.string.lang_ms),
        "pt-BR" to context.getString(R.string.lang_pt_BR),
        "pt-PT" to context.getString(R.string.lang_pt_PT),
        "ru"    to context.getString(R.string.lang_ru),
        "es"    to context.getString(R.string.lang_es),
        "fil"   to context.getString(R.string.lang_fil),
        "th"    to context.getString(R.string.lang_th),
        "tr"    to context.getString(R.string.lang_tr),
        "ur"    to context.getString(R.string.lang_ur),
        "vi"    to context.getString(R.string.lang_vi)
    )

    val isHolo = OtaPulseTheme.holographicConfig.isEnabled
    val containerColor = if (isHolo) OtaPulseTheme.extendedColors.glassPanel else AlertDialogDefaults.containerColor

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = containerColor,
        icon = {
            Icon(
                imageVector = Icons.Rounded.Language,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
        },
        title = {
            Text(
                text = context.getString(R.string.settings_app_language),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 380.dp)
            ) {
                items(languages.entries.toList()) { entry ->
                    val isSelected = entry.key == currentLocaleTag
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                onLanguageSelected(entry.key)
                                onDismiss()
                            }
                            .padding(vertical = 10.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = {
                                onLanguageSelected(entry.key)
                                onDismiss()
                            }
                        )
                        Text(
                            text = entry.value,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            ),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(start = 12.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            ApplyDialogBlurEffect()
            TextButton(onClick = onDismiss) {
                Text(
                    text = context.getString(R.string.cancel),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    )
}

@Composable
fun CheckIntervalDialog(
    currentHours: Long,
    onDismiss: () -> Unit,
    onIntervalSelected: (Long) -> Unit
) {
    val context = LocalContext.current
    val hourValues = listOf(1L, 3L, 6L, 12L, 24L)
    val labels = mapOf(
        1L to context.getString(R.string.settings_check_interval_1h),
        3L to context.getString(R.string.settings_check_interval_3h),
        6L to context.getString(R.string.settings_check_interval_6h),
        12L to context.getString(R.string.settings_check_interval_12h),
        24L to context.getString(R.string.settings_check_interval_24h)
    )

    val isHolo = OtaPulseTheme.holographicConfig.isEnabled
    val containerColor = if (isHolo) OtaPulseTheme.extendedColors.glassPanel else AlertDialogDefaults.containerColor

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = containerColor,
        icon = {
            Icon(
                imageVector = Icons.Rounded.Schedule,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
        },
        title = {
            Text(
                text = context.getString(R.string.settings_check_interval_title),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                hourValues.forEach { hours ->
                    val isSelected = hours == currentHours
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                onIntervalSelected(hours)
                                onDismiss()
                            }
                            .padding(vertical = 10.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = {
                                onIntervalSelected(hours)
                                onDismiss()
                            }
                        )
                        Text(
                            text = labels[hours] ?: "${hours}h",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            ),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(start = 12.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            ApplyDialogBlurEffect()
            TextButton(onClick = onDismiss) {
                Text(
                    text = context.getString(R.string.cancel),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    )
}

@Composable
fun ImportConfirmationDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
        title = { Text(text = context.getString(R.string.import_overwrite_title)) },
        text = { Text(text = context.getString(R.string.import_overwrite_message)) },
        confirmButton = {
            ApplyDialogBlurEffect()
            OtaPrimaryButton(
                text = context.getString(R.string.import_action),
                onClick = {
                    onConfirm()
                    onDismiss()
                }
            )
        },
        dismissButton = {
            OtaOutlinedButton(
                text = context.getString(R.string.cancel),
                onClick = onDismiss
            )
        }
    )
}

@Composable
fun DeveloperDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    Dialog(onDismissRequest = onDismiss) {
        ApplyDialogBlurEffect()
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                // Avatar image (120dp x 120dp) cleanly clipped to CircleShape without internal padding
                Image(
                    painter = painterResource(id = R.drawable.ic_avatar_placeholder),
                    contentDescription = context.getString(R.string.creator_avatar_desc),
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Developer Name
                Text(
                    text = context.getString(R.string.creator_name),
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Developer Role
                Text(
                    text = context.getString(R.string.developer_role),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Developer Message
                Text(
                    text = context.getString(R.string.developer_message),
                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(28.dp))

                // Button Container
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    OtaTonalButton(
                        text = context.getString(R.string.github_button_text),
                        icon = ImageVector.vectorResource(id = R.drawable.ic_github),
                        onClick = {
                            context.openExternalBrowser("https://github.com/RemuruSama")
                            onDismiss()
                        },
                        modifier = Modifier.widthIn(min = 140.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ContributorsDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.Groups, contentDescription = null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.secondary) },
        title = { Text(text = "Contributors") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "Open Source Community", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Special thanks to all contributors who helped build, translate, and improve OTA Pulse on GitHub.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        },
        confirmButton = {
            ApplyDialogBlurEffect()
            OtaPrimaryButton(
                text = "View on GitHub",
                onClick = {
                    context.openExternalBrowser("https://github.com/RemuruSama/OTA-Pulse")
                    onDismiss()
                }
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = context.getString(R.string.close_action))
            }
        }
    )
}
