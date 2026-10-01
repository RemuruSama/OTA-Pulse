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
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
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
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.SystemUpdateAlt
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.delay
import com.abhinav.otapulse.R
import com.abhinav.otapulse.core.common.openExternalBrowser
import com.abhinav.otapulse.core.common.openInAppBrowser
import com.abhinav.otapulse.core.common.HapticType
import com.abhinav.otapulse.core.common.haptic
import androidx.compose.ui.platform.LocalView
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
    val view = LocalView.current

    val currentVersion = remember {
        try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "5.0.1"
        } catch (e: Exception) {
            "5.0.1"
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
                    Toast.makeText(context, context.getString(R.string.about_up_to_date), Toast.LENGTH_SHORT).show()
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
                        view.haptic(HapticType.CLICK)
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
                        view.haptic(HapticType.CLICK)
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText(context.getString(R.string.about_version_label), currentVersion))
                        Toast.makeText(context, context.getString(R.string.about_copied_version, currentVersion), Toast.LENGTH_SHORT).show()
                    }
                )
            }

            // 2. Upgrade Guide Card (via Local OTA Update) - before creator
            StaggeredItem(visible = showSections, index = 1) {
                UpgradeGuideCard(
                    onCopyGuide = {
                        view.haptic(HapticType.CLICK)
                        val guideText = """
Upgrade Guide: Local OTA Update

1. Download ROM Package:
Download the latest ROM upgrade zip package from the specified server.

2. Copy to Phone Storage:
Copy the ROM upgrade package to the phone storage (root directory or Downloads). Do not extract it manually.

3. Enable Developer Mode:
Go to Settings -> About device -> Version -> Click Build number 7 times and enter the password, now you are in the developer mode.

4. Select Local Install:
Go back to Settings -> About device -> Up to date -> Click the top right button -> Local install -> Click on the corresponding installation package.

5. Extract & Upgrade:
Click Extract -> Upgrade -> Wait until system upgrade is completed to 100%.

6. Restart Device:
After the upgrade is complete, click Restart. Update successful!
                        """.trimIndent()
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("OTA Upgrade Guide", guideText))
                        Toast.makeText(context, context.getString(R.string.about_upgrade_guide_copied), Toast.LENGTH_SHORT).show()
                    }
                )
            }

            // 3. Lead Architect Spotlight Card (Clean Single Row)
            StaggeredItem(visible = showSections, index = 2) {
                MinimalCreatorCard(
                    onClick = {
                        view.haptic(HapticType.CLICK)
                        try {
                            context.openInAppBrowser("https://t.me/CodeSenseiX")
                        } catch (e: Exception) {
                            Toast.makeText(context, context.getString(R.string.about_err_telegram), Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }

            // 4. Support & Funding Card (Minimalist Rows)
            var showCryptoDialog by remember { mutableStateOf(false) }
            val upiId = stringResource(R.string.upi_id_value)
            val upiCopiedToast = stringResource(R.string.upi_id_copied_toast)
            val cryptoAddress = stringResource(R.string.crypto_usdt_address)
            val cryptoNetwork = stringResource(R.string.crypto_network_name)
            val cryptoCopiedToast = stringResource(R.string.crypto_address_copied_toast)

            if (showCryptoDialog) {
                CryptoWalletDialog(
                    usdtAddress = cryptoAddress,
                    networkName = cryptoNetwork,
                    onDismiss = { showCryptoDialog = false },
                    onCopyAddress = {
                        view.haptic(HapticType.HEAVY_CLICK)
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("USDT Address", cryptoAddress))
                        Toast.makeText(context, cryptoCopiedToast, Toast.LENGTH_SHORT).show()
                    }
                )
            }

            StaggeredItem(visible = showSections, index = 3) {
                MinimalSupportCard(
                    upiId = upiId,
                    onOpenPayPal = {
                        view.haptic(HapticType.CLICK)
                        try {
                            context.openExternalBrowser("https://paypal.me/Abhinavftp?country.x=IN&locale.x=en_GB")
                        } catch (e: Exception) {
                            Toast.makeText(context, context.getString(R.string.about_err_paypal), Toast.LENGTH_SHORT).show()
                        }
                    },
                    onCopyUpi = {
                        view.haptic(HapticType.HEAVY_CLICK)
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText(context.getString(R.string.about_upi_id_label), upiId))
                        Toast.makeText(context, upiCopiedToast, Toast.LENGTH_SHORT).show()
                    },
                    onPayUpi = {
                        view.haptic(HapticType.CLICK)
                        try {
                            val upiUri = Uri.parse("upi://pay?pa=$upiId&pn=Abhinav&cu=INR")
                            val intent = Intent(Intent.ACTION_VIEW, upiUri)
                            val chooser = Intent.createChooser(intent, "Pay via UPI")
                            context.startActivity(chooser)
                        } catch (e: Exception) {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText(context.getString(R.string.about_upi_id_label), upiId))
                            Toast.makeText(context, "No UPI app found. $upiCopiedToast", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onOpenCrypto = {
                        view.haptic(HapticType.CLICK)
                        showCryptoDialog = true
                    },
                    onCopyCrypto = {
                        view.haptic(HapticType.HEAVY_CLICK)
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("USDT Address", cryptoAddress))
                        Toast.makeText(context, cryptoCopiedToast, Toast.LENGTH_SHORT).show()
                    }
                )
            }

            // 5. Community & Links Bento Grid (Clean 3-item Row) - at last
            StaggeredItem(visible = showSections, index = 4) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MinimalBentoCard(
                        title = stringResource(R.string.about_website_title),
                        subtitle = stringResource(R.string.about_official_sub),
                        icon = Icons.Rounded.Public,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            view.haptic(HapticType.CLICK)
                            try {
                                context.openExternalBrowser("https://remurusama.github.io/OTA-Pulse/")
                            } catch (e: Exception) {
                                Toast.makeText(context, context.getString(R.string.about_err_website), Toast.LENGTH_SHORT).show()
                            }
                        }
                    )

                    MinimalBentoCard(
                        title = stringResource(R.string.about_github_title),
                        subtitle = stringResource(R.string.about_source_sub),
                        icon = ImageVector.vectorResource(id = R.drawable.ic_github),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            view.haptic(HapticType.CLICK)
                            try {
                                context.openExternalBrowser("https://github.com/RemuruSama/OTA-Pulse")
                            } catch (e: Exception) {
                                Toast.makeText(context, context.getString(R.string.about_err_github), Toast.LENGTH_SHORT).show()
                            }
                        }
                    )

                    MinimalBentoCard(
                        title = stringResource(R.string.about_telegram_title),
                        subtitle = stringResource(R.string.about_community_sub),
                        icon = Icons.AutoMirrored.Rounded.Send,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            view.haptic(HapticType.CLICK)
                            try {
                                context.openInAppBrowser("https://t.me/abhinav_v1")
                            } catch (e: Exception) {
                                Toast.makeText(context, context.getString(R.string.about_err_telegram), Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 6. Minimal Signature Footer
            StaggeredItem(visible = showSections, index = 5) {
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
                            view.haptic(HapticType.CLICK)
                            Toast.makeText(context, context.getString(R.string.about_engine_online) + " 🚀", Toast.LENGTH_SHORT).show()
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
                            contentDescription = stringResource(R.string.about_app_icon_cd),
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MinimalSupportCard(
    upiId: String,
    onOpenPayPal: () -> Unit,
    onCopyUpi: () -> Unit,
    onPayUpi: () -> Unit,
    onOpenCrypto: () -> Unit,
    onCopyCrypto: () -> Unit
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

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Buy Me a Coffee",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "PayPal",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = Color(0xFFE91E63)
                    )
                }

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

            // Row 2: UPI (Tap to pay, Hold to copy)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = onPayUpi,
                        onLongClick = onCopyUpi
                    )
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
                        text = stringResource(R.string.about_upi_id_label),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = upiId,
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Tap to pay • Hold to copy",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "Pay",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
            )

            // Row 3: Crypto Wallet (USDT BEP20)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = onOpenCrypto,
                        onLongClick = onCopyCrypto
                    )
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFF0B90B).copy(alpha = 0.18f),
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = ImageVector.vectorResource(id = R.drawable.ic_crypto_wallet),
                            contentDescription = null,
                            tint = Color(0xFFD49B00),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.crypto_wallet_title),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Tap for QR • Hold to copy",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = Color(0xFFD49B00)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    shape = CircleShape,
                    color = Color(0xFFF0B90B).copy(alpha = 0.20f)
                ) {
                    Text(
                        text = "QR",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFFB38300),
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CryptoWalletDialog(
    usdtAddress: String,
    networkName: String,
    onDismiss: () -> Unit,
    onCopyAddress: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        OtaCard(
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF0B90B).copy(alpha = 0.18f),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = ImageVector.vectorResource(id = R.drawable.ic_crypto_wallet),
                                contentDescription = null,
                                tint = Color(0xFFD49B00),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.crypto_wallet_title),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "USDT • $networkName",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(id = R.drawable.ic_close),
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // QR Code Display
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                    shadowElevation = 2.dp
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.qr_crypto_usdt),
                        contentDescription = "USDT BEP20 QR Code",
                        modifier = Modifier
                            .size(220.dp)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Fit
                    )
                }

                // Network Tag
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFF0B90B).copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, Color(0xFFF0B90B).copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFF0B90B),
                            modifier = Modifier.size(7.dp)
                        ) {}
                        Text(
                            text = "Network: $networkName",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Address Box
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "USDT Address (BEP20)",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = usdtAddress,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Medium,
                                fontSize = 11.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Action button: Copy Address
                OtaPrimaryButton(
                    text = "Copy USDT Address",
                    icon = Icons.Rounded.ContentCopy,
                    onClick = onCopyAddress,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun UpgradeGuideCard(
    onCopyGuide: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val isHolo = OtaPulseTheme.holographicConfig.isEnabled
    val cardShape = RoundedCornerShape(20.dp)
    val view = LocalView.current

    val steps = listOf(
        Triple(
            "01",
            "Download ROM Package",
            "Download the latest ROM upgrade zip package from the specified server."
        ),
        Triple(
            "02",
            "Copy to Phone Storage",
            "Copy the ROM upgrade package to the phone storage root or Downloads (keep as .zip)."
        ),
        Triple(
            "03",
            "Enable Developer Mode",
            "Go to Settings -> About device -> Version -> Click Build number 7 times and enter the password, now you are in the developer mode."
        ),
        Triple(
            "04",
            "Select Local Install",
            "Go back to Settings -> About device -> Up to date -> Click the top right button -> Local install -> Click on the corresponding installation package."
        ),
        Triple(
            "05",
            "Extract & Upgrade",
            "Click Extract -> Upgrade -> Wait until system upgrade is completed to 100%."
        ),
        Triple(
            "06",
            "Restart Device",
            "After the upgrade is complete, click Restart. Update successful!"
        )
    )

    OtaCard(
        shape = cardShape,
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (isHolo) 6.dp else 1.dp,
                shape = cardShape,
                ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
            ),
        onClick = {
            view.haptic(HapticType.CLICK)
            expanded = !expanded
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.SystemUpdateAlt,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.about_upgrade_guide_title),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.about_upgrade_guide_sub),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = if (expanded) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = if (expanded) "Hide" else "Guide",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (expanded) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Icon(
                            imageVector = if (expanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = if (expanded) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                    ) {
                        Text(
                            text = stringResource(R.string.about_upgrade_guide_badge),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }

                    steps.forEach { (num, title, desc) ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = num,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace
                                            ),
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = title,
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = desc,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // Notice / Tip Box
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Keep phone battery ≥ 40% and do not power off or restart manually during installation.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Action button: Copy Guide
                    OtaTonalButton(
                        text = stringResource(R.string.about_copy_guide),
                        icon = Icons.Rounded.ContentCopy,
                        onClick = onCopyGuide,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    )
                }
            }
        }
    }
}



