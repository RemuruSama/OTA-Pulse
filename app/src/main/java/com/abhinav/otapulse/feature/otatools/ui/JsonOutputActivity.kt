package com.abhinav.otapulse.feature.otatools.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import com.abhinav.otapulse.R
import com.abhinav.otapulse.core.common.OtaJsonOutputHelper
import com.abhinav.otapulse.core.model.OtaUpdate
import com.abhinav.otapulse.databinding.DialogJsonOutputBinding
import com.google.android.material.color.MaterialColors

class JsonOutputActivity : AppCompatActivity() {

    private lateinit var binding: DialogJsonOutputBinding
    private var otaUpdate: OtaUpdate? = null

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
        WindowCompat.setDecorFitsSystemWindows(window, false)
        binding = DialogJsonOutputBinding.inflate(layoutInflater)
        setContentView(binding.root)

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

        setupEdgeToEdge()
        binding.toolbarJson.navigationIcon?.setTint(
            MaterialColors.getColor(binding.toolbarJson, com.google.android.material.R.attr.colorOnSurface)
        )
        binding.toolbarJson.setNavigationOnClickListener { finish() }
        val jsonOutput = OtaJsonOutputHelper.getJsonOutput(update)
        binding.tvJsonOutput.text = jsonOutput

        binding.btnSaveJson.setOnClickListener {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("OTA JSON Output", jsonOutput))
            Toast.makeText(this, getString(R.string.copy), Toast.LENGTH_SHORT).show()
        }

        binding.btnDownloadJson.setOnClickListener {
            OtaJsonOutputHelper.exportToDownloads(this, update)
                .onSuccess { fileName ->
                    Toast.makeText(
                        this,
                        getString(R.string.json_output_saved, fileName),
                        Toast.LENGTH_LONG
                    ).show()
                }
                .onFailure {
                    Toast.makeText(this, getString(R.string.json_output_save_failed), Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun setupEdgeToEdge() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.jsonOutputRoot) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val actionBarSizeAttrs = obtainStyledAttributes(intArrayOf(androidx.appcompat.R.attr.actionBarSize))
            val actionBarSize = actionBarSizeAttrs.getDimensionPixelSize(0, binding.toolbarJson.layoutParams.height)
            actionBarSizeAttrs.recycle()
            binding.toolbarJson.updateLayoutParams {
                height = actionBarSize + systemBars.top
            }
            binding.toolbarJson.updatePadding(top = systemBars.top)
            binding.jsonActionBar.updatePadding(bottom = 16 + systemBars.bottom)
            insets
        }
    }

    companion object {
        private const val EXTRA_OTA_UPDATE = "extra_ota_update"

        fun createIntent(context: Context, otaUpdate: OtaUpdate): Intent =
            Intent(context, JsonOutputActivity::class.java).putExtra(EXTRA_OTA_UPDATE, otaUpdate)
    }
}
