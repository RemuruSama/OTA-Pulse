package com.abhinav.otapulse.feature.otatools.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abhinav.otapulse.R
import com.abhinav.otapulse.core.common.OtaJsonOutputHelper
import com.abhinav.otapulse.core.model.OtaUpdate
import com.abhinav.otapulse.core.preferences.ThemePreferences
import com.abhinav.otapulse.core.ui.components.OtaCard
import com.abhinav.otapulse.core.ui.components.OtaPrimaryButton
import com.abhinav.otapulse.core.ui.components.OtaTonalButton
import com.abhinav.otapulse.core.ui.components.OtaTopAppBar
import com.abhinav.otapulse.core.ui.theme.OtaPulseTheme
import com.abhinav.otapulse.core.ui.theme.ThemeMode
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class JsonOutputActivity : AppCompatActivity() {

    @Inject
    lateinit var themePreferences: ThemePreferences

    private var otaUpdate: OtaUpdate? = null
    private var exportRegionName: String? = null

    private val saveJsonLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        val update = otaUpdate ?: return@registerForActivityResult
        uri ?: return@registerForActivityResult

        OtaJsonOutputHelper.saveToUri(this, uri, update)
            .onSuccess {
                Toast.makeText(this, getString(R.string.json_output_saved_to_file, uri.lastPathSegment ?: "file"), Toast.LENGTH_LONG).show()
            }
            .onFailure {
                Toast.makeText(this, getString(R.string.json_output_save_failed), Toast.LENGTH_SHORT).show()
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        otaUpdate = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(EXTRA_OTA_UPDATE, OtaUpdate::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(EXTRA_OTA_UPDATE)
        }
        val update = otaUpdate
        if (update == null || update.rawJson.isNullOrBlank()) {
            Toast.makeText(this, getString(R.string.json_output_unavailable), Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        exportRegionName = intent.getStringExtra(EXTRA_REGION_NAME)
        val jsonOutput = OtaJsonOutputHelper.getJsonOutput(update)

        setContent {
            val themeSettings by themePreferences.themeSettingsFlow.collectAsState(
                initial = themePreferences.getThemeSettings()
            )

            OtaPulseTheme(
                themeMode = themeSettings.themeMode,
                darkTheme = when (themeSettings.nightMode) {
                    AppCompatDelegate.MODE_NIGHT_NO -> false
                    AppCompatDelegate.MODE_NIGHT_YES -> true
                    else -> isSystemInDarkTheme()
                },
                amoledDark = themeSettings.amoledDark,
                dynamicColor = themeSettings.dynamicColor
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    JsonOutputScreen(
                        jsonOutput = jsonOutput,
                        onBack = { finish() },
                        onCopy = {
                            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("OTA JSON Output", jsonOutput))
                            Toast.makeText(this@JsonOutputActivity, getString(R.string.copy), Toast.LENGTH_SHORT).show()
                        },
                        onDownload = {
                            OtaJsonOutputHelper.exportToDownloads(this@JsonOutputActivity, update, exportRegionName)
                                .onSuccess { fileName ->
                                    Toast.makeText(
                                        this@JsonOutputActivity,
                                        getString(R.string.json_output_saved, fileName),
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                                .onFailure {
                                    Toast.makeText(this@JsonOutputActivity, getString(R.string.json_output_save_failed), Toast.LENGTH_SHORT).show()
                                }
                        }
                    )
                }
            }
        }
    }

    companion object {
        private const val EXTRA_OTA_UPDATE = "extra_ota_update"
        private const val EXTRA_REGION_NAME = "extra_region_name"

        fun createIntent(context: Context, otaUpdate: OtaUpdate, regionName: String? = null): Intent =
            Intent(context, JsonOutputActivity::class.java)
                .putExtra(EXTRA_OTA_UPDATE, otaUpdate)
                .putExtra(EXTRA_REGION_NAME, regionName)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JsonOutputScreen(
    jsonOutput: String,
    onBack: () -> Unit,
    onCopy: () -> Unit,
    onDownload: () -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val isHolo = OtaPulseTheme.holographicConfig.isEnabled

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = Color.Transparent,
        topBar = {
            OtaTopAppBar(
                title = stringResource(R.string.json_output_title),
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.nav_back),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(),
                color = if (isHolo) Color.Transparent else MaterialTheme.colorScheme.surfaceContainer,
                shadowElevation = 0.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OtaTonalButton(
                        text = stringResource(R.string.copy),
                        onClick = onCopy,
                        icon = Icons.Rounded.ContentCopy,
                        modifier = Modifier.weight(1f)
                    )
                    OtaPrimaryButton(
                        text = stringResource(R.string.button_download_json),
                        onClick = onDownload,
                        icon = Icons.Rounded.Download,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            OtaCard(
                modifier = Modifier.fillMaxSize()
            ) {
                val scrollState = rememberScrollState()
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(16.dp)
                ) {
                    SelectionContainer {
                        Text(
                            text = jsonOutput,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                lineHeight = 18.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}
