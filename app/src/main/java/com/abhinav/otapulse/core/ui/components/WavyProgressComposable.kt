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

package com.abhinav.otapulse.core.ui.components

import android.view.View
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.abhinav.otapulse.core.ui.WavyCircularProgressIndicator

/**
 * Compose wrapper around [WavyCircularProgressIndicator].
 *
 * Renders an Android 14+ Google Play Protect style squiggly circular spinner
 * using an underlying canvas View inside an AndroidView wrapper.
 */
@Composable
fun WavyProgressIndicator(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    strokeWidth: Dp = 2.dp,
    size: Dp = 48.dp
) {
    val density = LocalDensity.current
    val colorArgb = color.toArgb()
    val strokePx = with(density) { strokeWidth.toPx() }

    AndroidView(
        factory = { context ->
            WavyCircularProgressIndicator(context).apply {
                this.indicatorColor = colorArgb
                this.strokeWidth = strokePx
                this.visibility = View.VISIBLE
            }
        },
        update = { view ->
            view.indicatorColor = colorArgb
            view.strokeWidth = strokePx
        },
        modifier = modifier.size(size)
    )
}
