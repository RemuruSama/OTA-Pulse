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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import com.abhinav.otapulse.core.ui.theme.OtaPulseTheme
import com.abhinav.otapulse.core.ui.theme.holographicEdgeBrush

/**
 * Scrollable Top App Bar for OTA Pulse.
 *
 * Uses a tight, perfectly padded custom container with statusBarsPadding to remove excessive default TopAppBar vertical spacing.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OtaTopAppBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    scrollBehavior: TopAppBarScrollBehavior? = null,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    bottomContent: @Composable () -> Unit = {}
) {
    val isHolo = OtaPulseTheme.holographicConfig.isEnabled

    // Forced transparency to remove any "layer" or "dim" effect while scrolling.
    // This ensures a perfectly seamless look where the top bar matches the screen background.
    val containerColor = Color.Transparent

    val statusBarsTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val topPadding = statusBarsTop + 8.dp
    val bottomPadding = 6.dp

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(containerColor)
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                val heightOffset = scrollBehavior?.state?.heightOffset?.roundToInt() ?: 0
                val fullHeight = placeable.height.toFloat()

                if (scrollBehavior != null && scrollBehavior.state.heightOffsetLimit != -fullHeight) {
                    scrollBehavior.state.heightOffsetLimit = -fullHeight
                }

                val layoutHeight = placeable.height + heightOffset
                layout(placeable.width, layoutHeight.coerceAtLeast(0)) {
                    placeable.placeWithLayer(0, heightOffset)
                }
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    top = topPadding,
                    bottom = bottomPadding
                )
                .defaultMinSize(minHeight = 48.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            navigationIcon()

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 4.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 1.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            actions()
        }

        bottomContent()

        if (isHolo) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(holographicEdgeBrush())
            )
        } else {
            // Completely removed the divider for non-holo to ensure a seamless look
        }
    }
}
