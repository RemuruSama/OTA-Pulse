package com.abhinav.otapulse.core.common

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import android.util.TypedValue
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.abhinav.otapulse.R
import java.io.File
import java.io.FileOutputStream

/**
 * Data that drives the shareable OTA card image.
 */
data class OtaCardData(
    val deviceName: String,
    val versionName: String?,
    val regionName: String,
    val androidVersion: String?,
    val securityPatch: String?,
    val size: String,
    val arbStatus: String?,
    val md5: String,
    val downloadUrl: String = "",
    val changelogUrl: String? = null
)

/**
 * Generates a visually polished bitmap card summarising an OTA update.
 *
 * The card is rendered entirely with the [Canvas] API so it works offline
 * and automatically adapts to the current Material You / light / dark /
 * AMOLED theme via runtime attribute resolution.
 */
object OtaCardGenerator {

    // ── Card dimensions (pixels) ────────────────────────────────────────────
    private const val CARD_WIDTH = 1080
    private const val CARD_HEIGHT = 720
    private const val CORNER_RADIUS = 36f
    private const val PADDING_H = 48f
    private const val PADDING_V = 40f
    private const val ACCENT_STRIP_WIDTH = 16f
    private const val CELL_CORNER = 16f
    private const val CELL_GAP = 12f
    private const val CELL_PAD = 16f

    // ── Public API ──────────────────────────────────────────────────────────

    /**
     * Renders a card bitmap, saves it to the app cache, and returns a
     * content [Uri] that can be shared via [android.content.Intent.ACTION_SEND].
     */
    fun generate(context: Context, data: OtaCardData): Uri {
        val colors = resolveColors(context)
        val bitmap = Bitmap.createBitmap(CARD_WIDTH, CARD_HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        drawBackground(canvas, colors)
        drawLeftAccentStrip(canvas, colors)

        var y = PADDING_V
        y = drawBrand(context, canvas, y, colors)
        y = drawDeviceInfo(canvas, y, data, colors)
        y = drawDivider(canvas, y, colors)
        y = drawStatsRow1(canvas, y, data, colors)
        y = drawStatsRow2(canvas, y, data, colors)
        y = drawStatsRow3(canvas, y, data, colors)
        drawFooter(canvas, colors)

        return saveBitmap(context, bitmap)
    }

    // ── Color resolution ────────────────────────────────────────────────────

    private data class CardColors(
        val surface: Int,
        val surfaceContainer: Int,
        val surfaceContainerHigh: Int,
        val onSurface: Int,
        val onSurfaceVariant: Int,
        val primary: Int,
        val primaryContainer: Int,
        val onPrimaryContainer: Int,
        val outlineVariant: Int,
        val arbSafe: Int,
        val arbProtected: Int
    )

    private fun resolveColors(context: Context): CardColors {
        fun attr(attrRes: Int, fallback: Int): Int {
            val tv = TypedValue()
            return if (context.theme.resolveAttribute(attrRes, tv, true)) tv.data else fallback
        }
        return CardColors(
            surface = attr(com.google.android.material.R.attr.colorSurface, Color.WHITE),
            surfaceContainer = attr(com.google.android.material.R.attr.colorSurfaceContainer, 0xFFF0F0F0.toInt()),
            surfaceContainerHigh = attr(com.google.android.material.R.attr.colorSurfaceContainerHigh, 0xFFE8E8E8.toInt()),
            onSurface = attr(com.google.android.material.R.attr.colorOnSurface, Color.BLACK),
            onSurfaceVariant = attr(com.google.android.material.R.attr.colorOnSurfaceVariant, Color.DKGRAY),
            primary = attr(androidx.appcompat.R.attr.colorPrimary, 0xFFBA1A1A.toInt()),
            primaryContainer = attr(com.google.android.material.R.attr.colorPrimaryContainer, 0xFFFFDAD5.toInt()),
            onPrimaryContainer = attr(com.google.android.material.R.attr.colorOnPrimaryContainer, 0xFF410002.toInt()),
            outlineVariant = attr(com.google.android.material.R.attr.colorOutlineVariant, Color.LTGRAY),
            arbSafe = ContextCompat.getColor(context, R.color.arb_safe),
            arbProtected = ContextCompat.getColor(context, R.color.arb_protected)
        )
    }

    // ── Drawing helpers ─────────────────────────────────────────────────────

    private fun drawBackground(canvas: Canvas, colors: CardColors) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = colors.surfaceContainerHigh }
        canvas.drawRoundRect(0f, 0f, CARD_WIDTH.toFloat(), CARD_HEIGHT.toFloat(), CORNER_RADIUS, CORNER_RADIUS, paint)

        // Subtle border
        val border = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colors.outlineVariant
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        canvas.drawRoundRect(1f, 1f, CARD_WIDTH - 1f, CARD_HEIGHT - 1f, CORNER_RADIUS, CORNER_RADIUS, border)
    }

    private fun drawLeftAccentStrip(canvas: Canvas, colors: CardColors) {
        // Gradient accent strip on the left, clipped to the card shape
        canvas.save()
        val clipPath = Path()
        clipPath.addRoundRect(0f, 0f, CARD_WIDTH.toFloat(), CARD_HEIGHT.toFloat(), CORNER_RADIUS, CORNER_RADIUS, Path.Direction.CW)
        canvas.clipPath(clipPath)

        val stripPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, 0f, CARD_HEIGHT.toFloat(),
                colors.primary, adjustAlpha(colors.primary, 0.5f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, ACCENT_STRIP_WIDTH, CARD_HEIGHT.toFloat(), stripPaint)
        canvas.restore()
    }

    private fun drawBrand(context: Context, canvas: Canvas, startY: Float, colors: CardColors): Float {
        var y = startY + 8f

        // Brand text and layout calculations
        val text = "OTA Pulse | 𝗨𝗽𝗱𝗮𝘁𝗲 𝗔𝗹𝗲𝗿𝘁"
        val brandPaint = textPaint(44f, colors.primary, Typeface.create(Typeface.DEFAULT, Typeface.BOLD))
        val textWidth = brandPaint.measureText(text)
        
        val logoSize = 64
        val gap = 16f
        val totalWidth = logoSize + gap + textWidth
        val pulseLeft = (CARD_WIDTH - totalWidth) / 2f

        // Draw app logo
        val drawable = ContextCompat.getDrawable(context, R.mipmap.ic_launcher)
        if (drawable != null) {
            val top = y.toInt() - 10
            drawable.setBounds(pulseLeft.toInt(), top, (pulseLeft + logoSize).toInt(), top + logoSize)
            drawable.draw(canvas)
        }

        canvas.drawText(text, pulseLeft + logoSize + gap, y + 40f, brandPaint)

        return y + 70f
    }

    private fun drawDeviceInfo(canvas: Canvas, startY: Float, data: OtaCardData, colors: CardColors): Float {
        var y = startY + 10f

        // Device name
        val namePaint = textPaint(42f, colors.onSurface, Typeface.create(Typeface.DEFAULT, Typeface.BOLD))
        val displayName = ellipsize(data.deviceName, namePaint, CARD_WIDTH - PADDING_H * 2)
        canvas.drawText(displayName, PADDING_H, y + 48f, namePaint)
        y += 56f

        // Version name
        val versionPaint = textPaint(30f, colors.onSurfaceVariant)
        val version = data.versionName ?: "Unknown"
        val displayVersion = ellipsize(version, versionPaint, CARD_WIDTH - PADDING_H * 2 - 200f)
        canvas.drawText(displayVersion, PADDING_H, y + 30f, versionPaint)

        // Region pill
        val regionText = data.regionName
        val pillPaint = textPaint(24f, colors.onPrimaryContainer)
        val pillTextWidth = pillPaint.measureText(regionText)
        val pillH = 32f
        val pillW = pillTextWidth + 24f
        val pillX = CARD_WIDTH - PADDING_H - pillW
        val pillY = y + 8f

        val pillBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = colors.primaryContainer }
        canvas.drawRoundRect(pillX, pillY, pillX + pillW, pillY + pillH, pillH / 2, pillH / 2, pillBg)

        val pillMetrics = pillPaint.fontMetrics
        val pillTextY = pillY + pillH / 2 - (pillMetrics.ascent + pillMetrics.descent) / 2
        canvas.drawText(regionText, pillX + 12f, pillTextY, pillPaint)

        y += 42f
        return y
    }

    private fun drawDivider(canvas: Canvas, startY: Float, colors: CardColors): Float {
        val y = startY + 14f
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colors.outlineVariant
            strokeWidth = 1.5f
        }
        canvas.drawLine(PADDING_H, y, CARD_WIDTH - PADDING_H, y, paint)
        return y + 18f
    }

    private fun drawStatsRow1(canvas: Canvas, startY: Float, data: OtaCardData, colors: CardColors): Float {
        val availableWidth = CARD_WIDTH - PADDING_H * 2
        val cellW = (availableWidth - CELL_GAP) / 2f
        val cellH = 90f
        val y = startY

        val androidVer = data.androidVersion?.replace(Regex("(?i)android\\s*"), "")?.trim() ?: "—"
        val displayAndroidVer = androidVer.ifEmpty { "—" }
        
        drawStatCell(canvas, PADDING_H, y, cellW, cellH, "Android Version", displayAndroidVer, colors)
        drawStatCell(canvas, PADDING_H + cellW + CELL_GAP, y, cellW, cellH, "Security Patch", data.securityPatch ?: "—", colors)

        return y + cellH + CELL_GAP
    }

    private fun drawStatsRow2(canvas: Canvas, startY: Float, data: OtaCardData, colors: CardColors): Float {
        val availableWidth = CARD_WIDTH - PADDING_H * 2
        val cellW = (availableWidth - CELL_GAP) / 2f
        val cellH = 90f
        val y = startY

        drawStatCell(canvas, PADDING_H, y, cellW, cellH, "Size", data.size, colors)

        // ARB status — color-coded
        val arbText = data.arbStatus ?: "N/A"
        val arbColor = when {
            arbText.equals("Safe", ignoreCase = true) -> colors.arbSafe
            arbText.contains("Protected", ignoreCase = true) -> colors.arbProtected
            else -> colors.onSurface
        }
        drawStatCell(canvas, PADDING_H + cellW + CELL_GAP, y, cellW, cellH, "ARB Status", arbText, colors, valueColor = arbColor)

        return y + cellH + CELL_GAP
    }

    private fun drawStatsRow3(canvas: Canvas, startY: Float, data: OtaCardData, colors: CardColors): Float {
        val availableWidth = CARD_WIDTH - PADDING_H * 2
        val cellH = 90f
        val y = startY

        // MD5 — full width
        val md5Display = data.md5.ifBlank { "N/A" }
        drawStatCell(canvas, PADDING_H, y, availableWidth, cellH, "MD5", md5Display, colors)

        return y + cellH + CELL_GAP
    }

    private fun drawStatCell(
        canvas: Canvas,
        x: Float, y: Float, w: Float, h: Float,
        label: String, value: String,
        colors: CardColors,
        valueColor: Int? = null
    ) {
        // Background
        val bg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = colors.surfaceContainer }
        canvas.drawRoundRect(x, y, x + w, y + h, CELL_CORNER, CELL_CORNER, bg)

        // Label
        val labelPaint = textPaint(24f, colors.onSurfaceVariant)
        canvas.drawText(label, x + CELL_PAD, y + CELL_PAD + 22f, labelPaint)

        // Value
        val valPaint = textPaint(32f, valueColor ?: colors.onSurface, Typeface.create(Typeface.DEFAULT, Typeface.BOLD))
        val displayVal = ellipsize(value, valPaint, w - CELL_PAD * 2)
        canvas.drawText(displayVal, x + CELL_PAD, y + h - CELL_PAD - 2f, valPaint)
    }

    private fun drawFooter(canvas: Canvas, colors: CardColors) {
        val y1 = CARD_HEIGHT - PADDING_V - 22f
        val y2 = CARD_HEIGHT - PADDING_V + 4f

        val footerPaint = textPaint(24f, colors.onSurfaceVariant)
        val text1 = "Shared via OTA Pulse"
        val w1 = footerPaint.measureText(text1)
        canvas.drawText(text1, (CARD_WIDTH - w1) / 2, y1, footerPaint)

        val handlePaint = textPaint(22f, colors.primary)
        val text2 = "@abhinav_v1"
        val w2 = handlePaint.measureText(text2)
        canvas.drawText(text2, (CARD_WIDTH - w2) / 2, y2, handlePaint)
    }

    // ── Utilities ───────────────────────────────────────────────────────────

    private fun textPaint(sizePx: Float, color: Int, typeface: Typeface = Typeface.DEFAULT): Paint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            this.textSize = sizePx
            this.typeface = typeface
        }

    private fun ellipsize(text: String, paint: Paint, maxWidth: Float): String {
        if (paint.measureText(text) <= maxWidth) return text
        val ellipsis = "…"
        var end = text.length
        while (end > 0 && paint.measureText(text.substring(0, end) + ellipsis) > maxWidth) {
            end--
        }
        return text.substring(0, end) + ellipsis
    }

    private fun adjustAlpha(color: Int, factor: Float): Int {
        val alpha = (Color.alpha(color) * factor).toInt().coerceIn(0, 255)
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color))
    }

    private fun saveBitmap(context: Context, bitmap: Bitmap): Uri {
        val dir = File(context.cacheDir, "shared_cards")
        dir.mkdirs()

        // Clean up stale cards (older than 1 hour)
        val cutoff = System.currentTimeMillis() - 3_600_000L
        dir.listFiles()?.filter { it.lastModified() < cutoff }?.forEach { it.delete() }

        val file = File(dir, "ota_card_${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        bitmap.recycle()

        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }
}
