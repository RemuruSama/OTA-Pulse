package com.abhinav.otapulse.core.common

import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.abhinav.otapulse.R

/**
 * Consolidates OTA update sharing logic that was previously duplicated
 * across [OtaDetailsDialogFragment], [HomeUpdateFragment], and
 * [ManualQueryFragment].
 *
 * Shares an image card (via [OtaCardGenerator]) with a plain-text
 * fallback so apps that don't support images still receive useful content.
 */
object OtaShareHelper {

    /**
     * Generates a shareable card image from [data] and opens the system
     * share chooser.  The intent carries both `image/png` (EXTRA_STREAM)
     * and `text/plain` (EXTRA_TEXT) for maximum compatibility.
     */
    fun shareOtaCard(context: Context, data: OtaCardData) {
        try {
            val imageUri = OtaCardGenerator.generate(context, data)
            val shareText = buildShareText(data)

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, imageUri)
                putExtra(Intent.EXTRA_TEXT, shareText)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(
                Intent.createChooser(intent, "Share OTA Update for ${data.deviceName}")
            )
        } catch (e: Exception) {
            Toast.makeText(
                context,
                context.getString(R.string.card_share_error),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    /**
     * Builds the plain-text share message that was previously inlined in
     * every fragment.  Kept as the `EXTRA_TEXT` fallback so apps that
     * ignore images (or prefer text) still get the full OTA summary.
     */
    fun buildShareText(data: OtaCardData): String = """
        • 𝗖𝗵𝗮𝗻𝗴𝗲𝗹𝗼𝗴: ${data.changelogUrl ?: "Not available"}
        • 𝗗𝗼𝘄𝗻𝗹𝗼𝗮𝗱: ${data.downloadUrl}

        ━━━━━━━━━━━━━━━━━
        • @abhinav_v1
    """.trimIndent()
}
