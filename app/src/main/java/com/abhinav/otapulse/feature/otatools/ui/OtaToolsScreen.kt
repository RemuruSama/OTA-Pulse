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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.abhinav.otapulse.R
import com.abhinav.otapulse.core.common.performHapticFeedback
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

            // Tools Section Label
            item {
                Text(
                    text = stringResource(R.string.tools_section_label),
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 4.dp, top = 8.dp)
                )
            }

            // Tool 1: Manual Query
            item {
                ToolCard(
                    title = stringResource(R.string.tools_manual_query_title),
                    description = stringResource(R.string.tools_manual_query_desc),
                    icon = Icons.Rounded.Terminal,
                    accentColor = MaterialTheme.colorScheme.primary,
                    onClick = {
                        context.performHapticFeedback()
                        onNavigateToManualQuery()
                    }
                )
            }

            // Tool 2: Partition Extraction
            item {
                ToolCard(
                    title = stringResource(R.string.tools_partition_extraction_title),
                    description = stringResource(R.string.tools_partition_extraction_desc),
                    icon = ImageVector.vectorResource(id = R.drawable.ic_extract_stroke),
                    accentColor = MaterialTheme.colorScheme.secondary,
                    onClick = {
                        context.performHapticFeedback()
                        onNavigateToExtraction()
                    }
                )
            }

            // Tool 3: Link Resolver
            item {
                ToolCard(
                    title = stringResource(R.string.tools_link_resolver_title),
                    description = stringResource(R.string.tools_link_resolver_desc),
                    icon = ImageVector.vectorResource(id = R.drawable.ic_open_external_stroke),
                    accentColor = MaterialTheme.colorScheme.tertiary,
                    onClick = {
                        context.performHapticFeedback()
                        onNavigateToLinkResolver()
                    }
                )
            }

            // Tool 4: ARB Checker
            item {
                ToolCard(
                    title = stringResource(R.string.tools_arb_checker_title),
                    description = stringResource(R.string.tools_arb_checker_desc),
                    icon = Icons.Rounded.Security,
                    accentColor = OtaPulseTheme.extendedColors.arbSafe,
                    onClick = {
                        context.performHapticFeedback()
                        onNavigateToArbChecker()
                    }
                )
            }

            // Tool 5: Active Downloads
            item {
                ToolCard(
                    title = "Active Downloads",
                    description = "Monitor live OTA package downloads, speed charts, pause/resume queues, and verify MD5 checksums.",
                    icon = Icons.Rounded.Download,
                    accentColor = MaterialTheme.colorScheme.primary,
                    onClick = {
                        context.performHapticFeedback()
                        onNavigateToDownloads()
                    }
                )
            }

            // Tool 6: Update History
            item {
                ToolCard(
                    title = "Update History",
                    description = "Browse chronological logs of queried and downloaded firmware updates, search across models, or export JSON.",
                    icon = Icons.Rounded.History,
                    accentColor = MaterialTheme.colorScheme.secondary,
                    onClick = {
                        context.performHapticFeedback()
                        onNavigateToHistory()
                    }
                )
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
