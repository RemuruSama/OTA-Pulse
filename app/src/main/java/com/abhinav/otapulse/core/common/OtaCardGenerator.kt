package com.abhinav.otapulse.core.common

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
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
    val regionName: String? = null,
    val androidVersion: String?,
    val securityPatch: String?,
    val size: String,
    val arbStatus: String?,
    val md5: String,
    val downloadUrl: String = "",
    val changelogUrl: String? = null,
    val nvId: String? = null,
    val projectId: String? = null,
    val buildDate: String? = null,
    val targetVersion: String? = null
)

/**
 * Generates a visually polished bitmap card summarising an OTA update.
 */
object OtaCardGenerator {

    private const val CARD_WIDTH = 1200
    private const val CARD_HEIGHT = 980
    private const val CORNER_RADIUS = 48f

    fun generate(context: Context, data: OtaCardData): Uri {
        val colors = resolveColors(context)
        val bitmap = Bitmap.createBitmap(CARD_WIDTH, CARD_HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Image background
        val imageBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = colors.surfaceContainerHigh }
        canvas.drawRect(0f, 0f, CARD_WIDTH.toFloat(), CARD_HEIGHT.toFloat(), imageBg)

        // Main card
        val cardRect = RectF(32f, 32f, CARD_WIDTH - 32f, CARD_HEIGHT - 32f)
        val cardBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = colors.surface }
        canvas.drawRoundRect(cardRect, CORNER_RADIUS, CORNER_RADIUS, cardBg)

        // Left red accent strip
        canvas.save()
        val clipPath = Path().apply { addRoundRect(cardRect, CORNER_RADIUS, CORNER_RADIUS, Path.Direction.CW) }
        canvas.clipPath(clipPath)
        val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = colors.primary }
        canvas.drawRect(cardRect.left, cardRect.top, cardRect.left + 8f, cardRect.bottom, accentPaint)
        canvas.restore()

        val paddingLeft = cardRect.left + 8f + 48f
        val paddingRight = cardRect.right - 48f

        // --- Header ---
        val headerIconRadius = 48f
        var cy = cardRect.top + 48f + headerIconRadius
        
        val logoDrawable = ContextCompat.getDrawable(context, R.mipmap.ic_launcher)
        if (logoDrawable != null) {
            val cx = paddingLeft + headerIconRadius
            logoDrawable.setBounds((cx - headerIconRadius).toInt(), (cy - headerIconRadius).toInt(), (cx + headerIconRadius).toInt(), (cy + headerIconRadius).toInt())
            logoDrawable.draw(canvas)
        }
        

        val titleX = paddingLeft + headerIconRadius * 2 + 32f
        val titlePaint = textPaint(56f, colors.primary, Typeface.create(Typeface.DEFAULT, Typeface.BOLD))
        canvas.drawText(context.getString(R.string.card_gen_app_title), titleX, cy - 6f, titlePaint)
        val subtitlePaint = textPaint(32f, colors.onSurfaceVariant)
        canvas.drawText(context.getString(R.string.card_gen_update_alert), titleX, cy + 40f, subtitlePaint)

        // --- Top Right Header Pills (Region & Watermark) ---
        val hasRegion = !data.regionName.isNullOrBlank()
        if (hasRegion) {
            drawHeaderPill(context, canvas, paddingRight, cy - 32f, data.regionName!!, R.drawable.ic_language, colors.primaryContainer, colors.primary)
            drawHeaderPill(context, canvas, paddingRight, cy + 36f, "Shared via OTA Pulse  •  @abhinav_v1", R.drawable.ic_share_stroke, colors.surfaceContainer, colors.primary)
        } else {
            drawHeaderPill(context, canvas, paddingRight, cy, "Shared via OTA Pulse  •  @abhinav_v1", R.drawable.ic_share_stroke, colors.surfaceContainer, colors.primary)
        }

        // --- Divider ---
        var y = cy + headerIconRadius + 40f
        val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = colors.outlineVariant; strokeWidth = 2f }
        canvas.drawLine(paddingLeft, y, paddingRight, y, dividerPaint)

        // --- Device Info ---
        cy = y + 40f + headerIconRadius
        drawCircleIcon(context, canvas, paddingLeft + headerIconRadius, cy, headerIconRadius, R.drawable.ic_device, 56, colors.primaryContainer, colors.primary)
        
        val namePaint = textPaint(48f, colors.onSurface, Typeface.create(Typeface.DEFAULT, Typeface.BOLD))
        canvas.drawText(data.deviceName, titleX, cy - 4f, namePaint)
        val versionPaint = textPaint(32f, colors.onSurfaceVariant)
        canvas.drawText(data.versionName ?: "Unknown", titleX, cy + 40f, versionPaint)

        // --- Stats Grid ---
        y = cy + headerIconRadius + 36f
        val gap = 20f
        val colW3 = (paddingRight - paddingLeft - 2 * gap) / 3f
        val cellH = 116f

        val androidVer = data.androidVersion?.replace(Regex("(?i)android\\s*"), "")?.trim() ?: "—"
        val displayAndroidVer = androidVer.ifEmpty { "—" }

        // Row 1
        drawGridCell(context, canvas, paddingLeft, y, colW3, cellH, R.drawable.ic_android, "Android", displayAndroidVer, colors)
        drawGridCell(context, canvas, paddingLeft + colW3 + gap, y, colW3, cellH, R.drawable.ic_securitypatch, "Security Patch", data.securityPatch ?: "—", colors)
        drawGridCell(context, canvas, paddingLeft + 2 * (colW3 + gap), y, colW3, cellH, R.drawable.ic_storage, "Size", data.size, colors)

        // Row 2
        y += cellH + gap
        val arbText = data.arbStatus ?: "N/A"
        val arbColor = when {
            arbText.equals("Safe", ignoreCase = true) -> colors.arbSafe
            arbText.contains("Protected", ignoreCase = true) -> colors.arbProtected
            else -> colors.onSurface
        }
        drawGridCell(context, canvas, paddingLeft, y, colW3, cellH, R.drawable.ic_security, "ARB Status", arbText, colors, arbColor)
        drawGridCell(context, canvas, paddingLeft + colW3 + gap, y, colW3, cellH, R.drawable.ic_tag_stroke, "NV Identifier", data.nvId ?: "N/A", colors)
        drawGridCell(context, canvas, paddingLeft + 2 * (colW3 + gap), y, colW3, cellH, R.drawable.ic_code, "Project ID", data.projectId ?: "N/A", colors)

        // Row 3
        y += cellH + gap
        drawGridCell(context, canvas, paddingLeft, y, colW3, cellH, R.drawable.ic_calendar, "Build Date", data.buildDate ?: "N/A", colors)
        val targetVerDisplay = data.targetVersion ?: "N/A"
        drawGridCell(context, canvas, paddingLeft + colW3 + gap, y, 2 * colW3 + gap, cellH, R.drawable.ic_ota_version, "Target Version", targetVerDisplay, colors)

        // Row 4
        y += cellH + gap
        val md5Display = data.md5.ifBlank { "N/A" }
        drawGridCell(context, canvas, paddingLeft, y, paddingRight - paddingLeft, cellH, 0, "MD5 Checksum", md5Display, colors)

        return saveBitmap(context, bitmap)
    }

    private fun drawHeaderPill(
        context: Context, canvas: Canvas,
        rightX: Float, centerY: Float,
        text: String, iconRes: Int,
        bgColor: Int, contentColor: Int
    ) {
        val paint = textPaint(24f, contentColor, Typeface.create(Typeface.DEFAULT, Typeface.BOLD))
        val pillH = 56f
        val pillW = paint.measureText(text) + (if (iconRes != 0) 84f else 48f)
        val leftX = rightX - pillW
        val topY = centerY - pillH / 2f
        val bottomY = centerY + pillH / 2f

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = bgColor }
        canvas.drawRoundRect(leftX, topY, rightX, bottomY, pillH / 2f, pillH / 2f, bgPaint)

        var textX = leftX + 24f
        if (iconRes != 0) {
            val drawable = ContextCompat.getDrawable(context, iconRes)?.mutate()
            if (drawable != null) {
                drawable.setTint(contentColor)
                val iconSize = 32
                val iconCx = leftX + 36f
                drawable.setBounds((iconCx - iconSize / 2).toInt(), (centerY - iconSize / 2).toInt(), (iconCx + iconSize / 2).toInt(), (centerY + iconSize / 2).toInt())
                drawable.draw(canvas)
            }
            textX = leftX + 64f
        }

        val fontMetrics = paint.fontMetrics
        val textY = centerY - (fontMetrics.ascent + fontMetrics.descent) / 2f
        canvas.drawText(text, textX, textY, paint)
    }

    private fun drawGridCell(
        context: Context, canvas: Canvas,
        x: Float, y: Float, w: Float, h: Float,
        iconRes: Int, label: String, value: String,
        colors: CardColors,
        valueColor: Int? = null
    ) {
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = colors.surfaceContainer }
        canvas.drawRoundRect(x, y, x + w, y + h, 32f, 32f, bgPaint)
        
        val iconCx = x + 40f
        val iconCy = y + h / 2f
        drawCircleIcon(context, canvas, iconCx, iconCy, 28f, iconRes, 32, colors.primaryContainer, colors.primary)
        
        val textX = iconCx + 28f + 16f
        val labelPaint = textPaint(24f, colors.onSurfaceVariant)
        canvas.drawText(label, textX, y + 44f, labelPaint)
        
        val valPaint = textPaint(36f, valueColor ?: colors.onSurface, Typeface.create(Typeface.DEFAULT, Typeface.BOLD))
        val displayVal = fitTextOrEllipsize(value, valPaint, w - (textX - x) - 16f)
        canvas.drawText(displayVal, textX, y + h - 24f, valPaint)
    }

    private fun drawCircleIcon(
        context: Context, canvas: Canvas, 
        cx: Float, cy: Float, radius: Float,
        iconRes: Int, iconSize: Int,
        bgColor: Int, iconColor: Int
    ) {
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = bgColor }
        canvas.drawCircle(cx, cy, radius, bgPaint)
        
        if (iconRes != 0) {
            val drawable = ContextCompat.getDrawable(context, iconRes)?.mutate()
            if (drawable != null) {
                drawable.setTint(iconColor)
                val half = iconSize / 2
                drawable.setBounds((cx - half).toInt(), (cy - half).toInt(), (cx + half).toInt(), (cy + half).toInt())
                drawable.draw(canvas)
            }
        } else {
            val hashPaint = textPaint(36f, iconColor, Typeface.create(Typeface.DEFAULT, Typeface.BOLD))
            hashPaint.textAlign = Paint.Align.CENTER
            val metrics = hashPaint.fontMetrics
            val textY = cy - (metrics.ascent + metrics.descent) / 2
            canvas.drawText("#", cx, textY, hashPaint)
        }
    }

    private data class CardColors(
        val surface: Int,
        val surfaceContainer: Int,
        val surfaceContainerHigh: Int,
        val onSurface: Int,
        val onSurfaceVariant: Int,
        val primary: Int,
        val primaryContainer: Int,
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
            outlineVariant = attr(com.google.android.material.R.attr.colorOutlineVariant, Color.LTGRAY),
            arbSafe = ContextCompat.getColor(context, R.color.arb_safe),
            arbProtected = ContextCompat.getColor(context, R.color.arb_protected)
        )
    }

    private fun textPaint(sizePx: Float, color: Int, typeface: Typeface = Typeface.DEFAULT): Paint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            this.textSize = sizePx
            this.typeface = typeface
        }

    private fun fitTextOrEllipsize(text: String, paint: Paint, maxWidth: Float, minTextSize: Float = 22f): String {
        var currentSize = paint.textSize
        while (currentSize >= minTextSize && paint.measureText(text) > maxWidth) {
            currentSize -= 2f
            paint.textSize = currentSize
        }
        if (paint.measureText(text) <= maxWidth) {
            return text
        }
        return ellipsize(text, paint, maxWidth)
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

    private fun saveBitmap(context: Context, bitmap: Bitmap): Uri {
        val dir = File(context.cacheDir, "shared_cards")
        dir.mkdirs()

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
