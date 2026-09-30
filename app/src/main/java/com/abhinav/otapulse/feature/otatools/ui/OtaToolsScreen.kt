package com.abhinav.otapulse.feature.otatools.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.FolderZip
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.abhinav.otapulse.R
import com.abhinav.otapulse.core.common.HapticType
import com.abhinav.otapulse.core.common.haptic
import com.abhinav.otapulse.core.ui.components.StaggeredItem
import com.abhinav.otapulse.core.ui.components.OtaCard
import com.abhinav.otapulse.core.ui.components.OtaTopAppBar
import com.abhinav.otapulse.core.ui.theme.OtaPulseTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OtaToolsScreen(
    onNavigateToManualQuery: () -> Unit,
    onNavigateToExtraction: () -> Unit,
    onNavigateToLinkResolver: () -> Unit,
    onNavigateToArbChecker: () -> Unit,
    onNavigateToDownloads: () -> Unit = {},
    onNavigateToHistory: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val context = LocalContext.current
    val view = LocalView.current
    var showSections by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { showSections = true }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        topBar = {
            OtaTopAppBar(
                title = stringResource(R.string.title_ota_tools),
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hero Header Card
            item {
                StaggeredItem(visible = showSections, index = 0) {
                    OtaCard(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                        Color.Transparent
                                    )
                                )
                            )
                            .padding(20.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = ImageVector.vectorResource(id = R.drawable.ic_tools),
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = stringResource(R.string.tools_header_badge),
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = MaterialTheme.colorScheme.onPrimary
                                        )
                                    }
                                }
                            }

                            Text(
                                text = stringResource(R.string.tools_header_title).replace("\\n", " "),
                                style = MaterialTheme.typography.headlineLarge.copy(
                                    fontWeight = FontWeight.ExtraBold
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Text(
                                text = stringResource(R.string.tools_header_subtitle),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                }
            }

            // Tools Section Label
            item {
                StaggeredItem(visible = showSections, index = 1) {
                    Text(
                    text = stringResource(R.string.tools_section_label),
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 4.dp, top = 8.dp)
                )
                }
            }

            // Tool 1: Manual Query
            item {
                StaggeredItem(visible = showSections, index = 2) {
                    ToolCard(
                    title = stringResource(R.string.tools_manual_query_title),
                    description = stringResource(R.string.tools_manual_query_desc),
                    icon = Icons.Rounded.Terminal,
                    accentColor = MaterialTheme.colorScheme.primary,
                    onClick = {
                        view.haptic(HapticType.TICK)
                        onNavigateToManualQuery()
                    }
                )
                }
            }

            // Tool 2: Partition Extraction
            item {
                StaggeredItem(visible = showSections, index = 3) {
                    ToolCard(
                    title = stringResource(R.string.tools_partition_extraction_title),
                    description = stringResource(R.string.tools_partition_extraction_desc),
                    icon = ImageVector.vectorResource(id = R.drawable.ic_extract_stroke),
                    accentColor = MaterialTheme.colorScheme.secondary,
                    onClick = {
                        view.haptic(HapticType.TICK)
                        onNavigateToExtraction()
                    }
                )
                }
            }

            // Tool 3: Link Resolver
            item {
                StaggeredItem(visible = showSections, index = 4) {
                    ToolCard(
                    title = stringResource(R.string.tools_link_resolver_title),
                    description = stringResource(R.string.tools_link_resolver_desc),
                    icon = ImageVector.vectorResource(id = R.drawable.ic_open_external_stroke),
                    accentColor = MaterialTheme.colorScheme.tertiary,
                    onClick = {
                        view.haptic(HapticType.TICK)
                        onNavigateToLinkResolver()
                    }
                )
                }
            }

            // Tool 4: ARB Checker
            item {
                StaggeredItem(visible = showSections, index = 5) {
                    ToolCard(
                    title = stringResource(R.string.tools_arb_checker_title),
                    description = stringResource(R.string.tools_arb_checker_desc),
                    icon = Icons.Rounded.Security,
                    accentColor = MaterialTheme.colorScheme.primary,
                    onClick = {
                        view.haptic(HapticType.TICK)
                        onNavigateToArbChecker()
                    }
                )
                }
            }

            // Tool 5: Active Downloads
            item {
                StaggeredItem(visible = showSections, index = 6) {
                    ToolCard(
                    title = stringResource(R.string.tools_active_downloads_title),
                    description = stringResource(R.string.tools_active_downloads_desc),
                    icon = Icons.Rounded.Download,
                    accentColor = MaterialTheme.colorScheme.secondary,
                    onClick = {
                        view.haptic(HapticType.TICK)
                        onNavigateToDownloads()
                    }
                )
                }
            }

            // Tool 6: Search History
            item {
                StaggeredItem(visible = showSections, index = 7) {
                    ToolCard(
                    title = stringResource(R.string.tools_update_history_title),
                    description = stringResource(R.string.tools_update_history_desc),
                    icon = Icons.Rounded.History,
                    accentColor = MaterialTheme.colorScheme.tertiary,
                    onClick = {
                        view.haptic(HapticType.TICK)
                        onNavigateToHistory()
                    }
                )
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(84.dp))
            }
        }
    }
}

@Composable
private fun ToolCard(
    title: String,
    description: String,
    icon: ImageVector,
    accentColor: Color,
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
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accentColor,
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}


