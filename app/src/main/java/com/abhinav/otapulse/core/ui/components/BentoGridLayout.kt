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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

import com.abhinav.otapulse.core.ui.theme.OtaPulseMotion

/**
 * Staggered Bento Grid Layout for device catalogs and dashboards.
 *
 * Uses LazyVerticalStaggeredGrid with automatic item placement animations,
 * staggered entrance animations, and configurable column counts.
 */
@Composable
fun <T> BentoGrid(
    items: List<T>,
    modifier: Modifier = Modifier,
    columns: Int = 2,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    itemSpacing: Dp = 12.dp,
    sessionKey: String = "",
    key: ((T) -> Any)? = null,
    itemContent: @Composable (T) -> Unit
) {
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(columns),
        modifier = modifier,
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(itemSpacing),
        verticalItemSpacing = itemSpacing
    ) {
        itemsIndexed(
            items = items,
            key = if (key != null) { index, item -> key(item) } else null
        ) { index, item ->
            Box(
                modifier = Modifier
                    .stackItemAppearance(index, sessionKey)
                    .animateItem(placementSpec = OtaPulseMotion.StackReorderSpec)
            ) {
                itemContent(item)
            }
        }
    }
}

