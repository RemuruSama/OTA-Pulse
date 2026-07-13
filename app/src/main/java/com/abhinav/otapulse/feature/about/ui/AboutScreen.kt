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

package com.abhinav.otapulse.feature.about.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import com.abhinav.otapulse.R
import com.abhinav.otapulse.core.common.openExternalBrowser
import com.abhinav.otapulse.core.common.openInAppBrowser
import com.abhinav.otapulse.core.common.performHapticFeedback
import com.abhinav.otapulse.core.network.GitHubUpdater
import com.abhinav.otapulse.core.network.UpdateInfo
import com.abhinav.otapulse.core.ui.theme.OtaPulseMotion
import com.abhinav.otapulse.core.ui.components.OtaCard
import com.abhinav.otapulse.core.ui.components.StaggeredItem
import com.abhinav.otapulse.core.ui.components.OtaPrimaryButton
import com.abhinav.otapulse.core.ui.components.OtaTonalButton
import com.abhinav.otapulse.core.ui.components.OtaTopAppBar
import com.abhinav.otapulse.core.ui.theme.OtaPulseTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onNavigateToAppUpdate: (UpdateInfo?) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val context = LocalContext.current

    val currentVersion = remember {
        try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "16.0.5"
        } catch (e: Exception) {
            "16.0.5"
        }
    }

    val appIconBitmap = remember(context) {
        try {
            val drawable = context.packageManager.getApplicationIcon(context.applicationInfo)
            val bitmap = if (drawable is BitmapDrawable && drawable.bitmap != null) {
                drawable.bitmap
            } else {
                val width = if (drawable.intrinsicWidth <= 0) 192 else drawable.intrinsicWidth
                val height = if (drawable.intrinsicHeight <= 0) 192 else drawable.intrinsicHeight
                val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bmp)
                drawable.setBounds(0, 0, canvas.width, canvas.height)
                drawable.draw(canvas)
                bmp
            }
            bitmap.asImageBitmap()
        } catch (e: Exception) {
            null
        }
    }

    var pendingUpdateInfo by remember { mutableStateOf<UpdateInfo?>(null) }
    var isCheckingUpdate by remember { mutableStateOf(false) }
    var remainingSeconds by remember { mutableIntStateOf(0) }
    var lastUpdateResult by remember { mutableStateOf<UpdateInfo?>(null) }

    // Logic for 5-second simulated loading/cooldown
    LaunchedEffect(remainingSeconds) {
        if (remainingSeconds > 0) {
            delay(500L)
            remainingSeconds--
            if (remainingSeconds == 0) {
                isCheckingUpdate = false
                pendingUpdateInfo = lastUpdateResult
                if (lastUpdateResult != null) {
                    onNavigateToAppUpdate(lastUpdateResult)
                } else {
                    Toast.makeText(context, "OTA Pulse is up to date!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Silent check on screen open
    LaunchedEffect(currentVersion) {
        GitHubUpdater.checkForUpdate(currentVersion) { info ->
            if (info != null) {
                pendingUpdateInfo = info
            }
        }
    }

    // State for staggered entry animation
    var showSections by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        showSections = true
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        topBar = {
            OtaTopAppBar(
                title = stringResource(R.string.title_about),
                scrollBehavior = scrollBehavior
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Minimalist Hero Centerpiece
            StaggeredItem(visible = showSections, index = 0) {
                MinimalHeroSection(
                    currentVersion = currentVersion,
                    appIconBitmap = appIconBitmap,
                    isCheckingUpdate = isCheckingUpdate,
                    remainingSeconds = remainingSeconds,
                    pendingUpdateInfo = pendingUpdateInfo,
                    onCheckOrInstallUpdate = {
                        context.performHapticFeedback()
                        if (pendingUpdateInfo != null) {
                            onNavigateToAppUpdate(pendingUpdateInfo)
                        } else if (!isCheckingUpdate && remainingSeconds == 0) {
                            isCheckingUpdate = true
                            remainingSeconds = 5
                            GitHubUpdater.checkForUpdate(currentVersion) { info ->
                                lastUpdateResult = info
                            }
                        }
                    },
                    onCopyVersion = {
                        context.performHapticFeedback()
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Version", currentVersion))
                        Toast.makeText(context, "Copied version: v$currentVersion", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 2. Community & Links Bento Grid (Clean 3-item Row)
            StaggeredItem(visible = showSections, index = 1) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MinimalBentoCard(
                        title = "Website",
                        subtitle = "Official",
                        icon = Icons.Rounded.Public,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            context.performHapticFeedback()
                            try {
                                context.openExternalBrowser("https://remurusama.github.io/OTA-Pulse/")
                            } catch (e: Exception) {
                                Toast.makeText(context, "Could not open Website", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )

                    MinimalBentoCard(
                        title = "GitHub",
                        subtitle = "Source",
                        icon = ImageVector.vectorResource(id = R.drawable.ic_github),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            context.performHapticFeedback()
                            try {
                                context.openExternalBrowser("https://github.com/RemuruSama/OTA-Pulse")
                            } catch (e: Exception) {
                                Toast.makeText(context, "Could not open GitHub", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )

                    MinimalBentoCard(
                        title = "Telegram",
                        subtitle = "Community",
                        icon = Icons.AutoMirrored.Rounded.Send,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            context.performHapticFeedback()
                            try {
                                context.openInAppBrowser("https://t.me/abhinav_v1")
                            } catch (e: Exception) {
                                Toast.makeText(context, "Could not open Telegram", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            }

            // 3. Lead Architect Spotlight Card (Clean Single Row)
            StaggeredItem(visible = showSections, index = 2) {
                MinimalCreatorCard(
                    onClick = {
                        context.performHapticFeedback()
                        try {
                            context.openInAppBrowser("https://t.me/CodeSenseiX")
                        } catch (e: Exception) {
                            Toast.makeText(context, "Could not open Telegram", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }

            // 4. Support & Funding Card (Minimalist Rows)
            val upiId = stringResource(R.string.upi_id_value)
            val upiCopiedToast = stringResource(R.string.upi_id_copied_toast)
            StaggeredItem(visible = showSections, index = 3) {
                MinimalSupportCard(
                    upiId = upiId,
                    onOpenPayPal = {
                        context.performHapticFeedback()
                        try {
                            context.openExternalBrowser("https://paypal.me/Abhinavftp?country.x=IN&locale.x=en_GB")
                        } catch (e: Exception) {
                            Toast.makeText(context, "Could not open PayPal", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onCopyUpi = {
                        context.performHapticFeedback()
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("UPI ID", upiId))
                        Toast.makeText(context, upiCopiedToast, Toast.LENGTH_SHORT).show()
                    }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 5. Minimal Signature Footer
            StaggeredItem(visible = showSections, index = 4) {
                Text(
                    text = "OTA Pulse • Made with ❤️ for Android",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Medium
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            context.performHapticFeedback()
                            Toast.makeText(context, "OTA Pulse Engine Online 🚀", Toast.LENGTH_SHORT).show()
                        }
                )
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
private fun MinimalHeroSection(
    currentVersion: String,
    appIconBitmap: androidx.compose.ui.graphics.ImageBitmap?,
    isCheckingUpdate: Boolean,
    remainingSeconds: Int,
    pendingUpdateInfo: UpdateInfo?,
    onCheckOrInstallUpdate: () -> Unit,
    onCopyVersion: () -> Unit
) {
    val isHolo = OtaPulseTheme.holographicConfig.isEnabled
    val cardShape = RoundedCornerShape(24.dp)
    OtaCard(
        shape = cardShape,
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (isHolo) 10.dp else 2.dp,
                shape = cardShape,
                ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    if (isHolo) {
                        Brush.radialGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                                MaterialTheme.colorScheme.surface,
                                Color.Transparent
                            )
                        )
                    } else {
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
                                Color.Transparent
                            )
                        )
                    }
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // App Icon Box
                if (appIconBitmap != null) {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        shadowElevation = 8.dp,
                        modifier = Modifier.size(80.dp)
                    ) {
                        Image(
                            bitmap = appIconBitmap,
                            contentDescription = "App Icon",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        modifier = Modifier.size(80.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.SystemUpdate,
                                contentDescription = null,
                                modifier = Modifier.size(42.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // App Name
                Text(
                    text = "OTA Pulse",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Version & Online Pill (Clickable to copy)
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                    modifier = Modifier.clickable { onCopyVersion() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(OtaPulseTheme.extendedColors.arbSafe)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        val displayCurrentVersion = if (currentVersion.startsWith("v", ignoreCase = true)) currentVersion else "v$currentVersion"
                        Text(
                            text = "$displayCurrentVersion PRO",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            ),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Minimalist Tagline
                Text(
                    text = stringResource(R.string.about_app_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Minimal Update Action Button
                if (pendingUpdateInfo != null) {
                    val displayUpdateVersion = if (pendingUpdateInfo.version.startsWith("v", ignoreCase = true)) pendingUpdateInfo.version else "v${pendingUpdateInfo.version}"
                    OtaPrimaryButton(
                        text = "Update Available: $displayUpdateVersion • Install",
                        onClick = onCheckOrInstallUpdate,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    OtaTonalButton(
                        text = if (isCheckingUpdate) "Checking for Updates..." else "Check for Updates",
                        icon = if (isCheckingUpdate) null else Icons.Rounded.Refresh,
                        isLoading = false,
                        enabled = !isCheckingUpdate && remainingSeconds == 0,
                        onClick = onCheckOrInstallUpdate,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun MinimalBentoCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    OtaCard(
        modifier = modifier,
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = tint.copy(alpha = 0.15f),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = tint,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun MinimalCreatorCard(onClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "creator_card_anim")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val rotation by infiniteTransition.animateFloat(
        initialValue = -2f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "rotation"
    )

    val isHolo = OtaPulseTheme.holographicConfig.isEnabled

    OtaCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Box {
            if (isHolo) {
                val gradientShift by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(3000, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "gradient_shift"
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.linearGradient(
                                0.0f to Color.Transparent,
                                0.5f to MaterialTheme.colorScheme.tertiary.copy(alpha = 0.08f),
                                1.0f to Color.Transparent,
                                start = androidx.compose.ui.geometry.Offset(0f, 0f),
                                end = androidx.compose.ui.geometry.Offset(1000f * gradientShift, 1000f * gradientShift)
                            )
                        )
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f),
                    modifier = Modifier
                        .size(48.dp)
                        .graphicsLayer { rotationZ = rotation }
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_avatar_placeholder),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Abhinav Verma",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Lead Architect & Developer",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    modifier = Modifier.graphicsLayer {
                        scaleX = pulseScale
                        scaleY = pulseScale
                    }
                ) {
                    Text(
                        text = "Chat",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun MinimalSupportCard(
    upiId: String,
    onOpenPayPal: () -> Unit,
    onCopyUpi: () -> Unit
) {
    OtaCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Row 1: PayPal
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenPayPal() }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFE91E63).copy(alpha = 0.15f),
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.Favorite,
                            contentDescription = null,
                            tint = Color(0xFFE91E63),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Text(
                    text = "Buy Me a Coffee",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )

                Surface(
                    shape = CircleShape,
                    color = Color(0xFFE91E63).copy(alpha = 0.18f)
                ) {
                    Text(
                        text = "Donate",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFFE91E63),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
            )

            // Row 2: UPI ID
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onCopyUpi() }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = ImageVector.vectorResource(id = R.drawable.ic_upi),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "UPI ID",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = upiId,
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "Copy",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                    )
                }
            }
        }
    }
}
