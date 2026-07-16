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

package com.abhinav.otapulse.feature.settings.libraries.ui

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.abhinav.otapulse.R
import com.abhinav.otapulse.core.common.openInAppBrowser
import com.abhinav.otapulse.core.common.performHapticFeedback
import com.abhinav.otapulse.core.ui.components.OtaCard
import com.abhinav.otapulse.core.ui.components.OtaTopAppBar

data class Library(val name: String, val license: String, val url: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibrariesScreen(
    onNavigateBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val context = LocalContext.current

    val libraries = remember {
        listOf(
            Library("Activity", "Apache License 2.0", "https://developer.android.com/jetpack/androidx/releases/activity"),
            Library("Activity KTX", "Apache License 2.0", "https://developer.android.com/kotlin/ktx"),
            Library("Apache Commons Compress", "Apache License 2.0", "https://commons.apache.org/proper/commons-compress/"),
            Library("AppCompat", "Apache License 2.0", "https://developer.android.com/jetpack/androidx/releases/appcompat"),
            Library("ConstraintLayout", "Apache License 2.0", "https://developer.android.com/jetpack/androidx/releases/constraintlayout"),
            Library("FlexboxLayout", "Apache License 2.0", "https://github.com/google/flexbox-layout"),
            Library("Fragment", "Apache License 2.0", "https://developer.android.com/jetpack/androidx/releases/fragment"),
            Library("Fragment KTX", "Apache License 2.0", "https://developer.android.com/kotlin/ktx"),
            Library("Glide", "BSD, MIT, and Apache License 2.0", "https://github.com/bumptech/glide"),
            Library("Gson", "Apache License 2.0", "https://github.com/google/gson"),
            Library("Hilt Android", "Apache License 2.0", "https://dagger.dev/hilt/"),
            Library("Hilt Work", "Apache License 2.0", "https://developer.android.com/training/dependency-injection/hilt-jetpack"),
            Library("JSON in Java", "The JSON License", "https://github.com/stleary/JSON-java"),
            Library("Core KTX", "Apache License 2.0", "https://developer.android.com/kotlin/ktx"),
            Library("Lifecycle LiveData KTX", "Apache License 2.0", "https://developer.android.com/kotlin/ktx"),
            Library("Lifecycle ViewModel KTX", "Apache License 2.0", "https://developer.android.com/kotlin/ktx"),
            Library("Markwon", "Apache License 2.0", "https://github.com/noties/Markwon"),
            Library("Material Components for Android", "Apache License 2.0", "https://github.com/material-components/material-components-android"),
            Library("Navigation Fragment KTX", "Apache License 2.0", "https://developer.android.com/kotlin/ktx"),
            Library("Navigation UI KTX", "Apache License 2.0", "https://developer.android.com/kotlin/ktx"),
            Library("OkHttp", "Apache License 2.0", "https://square.github.io/okhttp/"),
            Library("Protocol Buffers Java Lite", "BSD 3-Clause License", "https://github.com/protocolbuffers/protobuf"),
            Library("Protocol Buffers Kotlin Lite", "BSD 3-Clause License", "https://github.com/protocolbuffers/protobuf"),
            Library("RecyclerView", "Apache License 2.0", "https://developer.android.com/jetpack/androidx/releases/recyclerview"),
            Library("realme-ota", "Open Source", "https://github.com/R0rt1z2/realme-ota"),
            Library("WorkManager KTX", "Apache License 2.0", "https://developer.android.com/kotlin/ktx"),
            Library("XZ for Java", "Public Domain", "https://tukaani.org/xz/java.html"),
            Library("kotlinx-coroutines-android", "Apache License 2.0", "https://github.com/Kotlin/kotlinx.coroutines")
        ).sortedBy { it.name }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        topBar = {
            OtaTopAppBar(
                title = stringResource(R.string.libraries_title),
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    IconButton(onClick = {
                        context.performHapticFeedback()
                        onNavigateBack()
                    }) {
                        Icon(
                            imageVector = Icons.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.libs_back_cd),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 84.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = "${libraries.size} OPEN SOURCE LIBRARIES",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                )
            }

            items(libraries, key = { it.name }) { library ->
                LibraryCard(
                    library = library,
                    onClick = {
                        context.performHapticFeedback()
                        try {
                            context.openInAppBrowser(library.url, library.name)
                        } catch (e: Exception) {
                            Toast.makeText(context, context.getString(R.string.libs_err_open_link), Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun LibraryCard(
    library: Library,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OtaCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = Icons.Rounded.Code,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = library.name,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = library.license,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Icon(
                imageVector = Icons.Rounded.OpenInNew,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
