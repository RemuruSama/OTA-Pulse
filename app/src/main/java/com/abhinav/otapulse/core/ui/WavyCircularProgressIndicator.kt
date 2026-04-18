package com.abhinav.otapulse.core.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PathMeasure
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.appcompat.R as AppCompatR
import com.google.android.material.color.MaterialColors
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * WavyCircularProgressIndicator
 *
 * Canvas-based custom View that mimics the squiggly circular progress
 * animation seen on Android 14+ AOSP (Google Play Protect style).
 *
 * Works correctly at any size — amplitude is capped to 20% of radius,
 * preventing the "asterisk" artifact at small dp sizes.
 *
 * Drop-in replacement for CircularProgressIndicator. No custom attributes
 * required; sane defaults work for both 20dp and 48dp+ sizes.
 *
 * Usage:
 *   <com.abhinav.otapulse.core.ui.WavyCircularProgressIndicator
 *       android:id="@+id/selectorProgress"
 *       android:layout_width="20dp"
 *       android:layout_height="20dp"
 *       android:visibility="gone" />
 *
 * Performance notes:
 *   - The wavy path is built with 180 segments (half the original 360) —
 *     visually indistinguishable at screen sizes but halves trig work per frame.
 *   - The path is only rebuilt when wavePhase actually changed since the last
 *     draw, avoiding re-computation on rotation-only frames.
 *   - All three ValueAnimators call invalidate() from their update listeners so
 *     arc phase and wave phase are always current when onDraw() executes.
 */
class WavyCircularProgressIndicator @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // ── Public properties ────────────────────────────────────────────────────

    /** Wave amplitude in px. Auto-capped to 20% of radius at draw time. */
    var waveAmplitude: Float = 2.5f * context.resources.displayMetrics.density
        set(value) { field = value; invalidate() }

    /** Number of wave crests around the full circle. */
    var waveCount: Int = 6
        set(value) { field = value; invalidate() }

    /** Stroke width in px. */
    var strokeWidth: Float = 2f * context.resources.displayMetrics.density
        set(value) { field = value; paint.strokeWidth = value; invalidate() }

    /** Indicator color. Defaults to ?attr/colorPrimary. */
    var indicatorColor: Int = resolveColorPrimary(context)
        set(value) { field = value; paint.color = value; invalidate() }

    /** Full rotation period in ms. */
    var rotationSpeedMs: Long = 1200L
        set(value) { field = value; if (isAttachedToWindow) restartAnimators() }

    /** Arc grow/shrink cycle period in ms. */
    var arcSpeedMs: Long = 900L
        set(value) { field = value; if (isAttachedToWindow) restartAnimators() }

    // ── Internal ─────────────────────────────────────────────────────────────

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        strokeWidth = this@WavyCircularProgressIndicator.strokeWidth
        color = indicatorColor
    }

    private var rotationAngle = 0f   // 0..360  — overall spin
    private var arcPhase      = 0f   // 0..1    — arc grow/shrink
    private var wavePhase     = 0f   // 0..2π   — ripple along path

    // Path cache: only rebuilt when wavePhase changes between frames.
    private var lastBuiltWavePhase = Float.NaN

    private var rotationAnimator: ValueAnimator? = null
    private var arcAnimator:      ValueAnimator? = null
    private var waveAnimator:     ValueAnimator? = null

    private val wavyPath        = Path()
    private val clippedPath     = Path()
    private val fullPathMeasure = PathMeasure()

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        startAnimators()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopAnimators()
    }

    override fun setVisibility(visibility: Int) {
        super.setVisibility(visibility)
        if (visibility == VISIBLE) {
            if (rotationAnimator?.isRunning != true) startAnimators()
        } else {
            stopAnimators()
        }
    }

    // ── Draw ──────────────────────────────────────────────────────────────────

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val cx = width  / 2f
        val cy = height / 2f
        val safeRadius = min(cx, cy) - strokeWidth - waveAmplitude - 1f
        if (safeRadius <= 2f) return

        // KEY FIX: cap amplitude to 20% of radius — prevents asterisk at small sizes
        val clampedAmplitude = min(waveAmplitude, safeRadius * 0.20f)

        // Only rebuild the wavy path when wavePhase has actually changed.
        // On rotation-only frames this is a no-op, saving 180 trig calls.
        if (wavePhase != lastBuiltWavePhase) {
            buildWavyCirclePath(cx, cy, safeRadius, clampedAmplitude)
            lastBuiltWavePhase = wavePhase
            fullPathMeasure.setPath(wavyPath, false)
        }

        val totalLength = fullPathMeasure.length
        if (totalLength <= 0f) return

        // Arc length oscillates between 15% and 80% of total path
        val eased     = ((sin(arcPhase * Math.PI * 2 - Math.PI / 2) + 1.0) / 2.0).toFloat()
        val arcLength = totalLength * (0.15f + eased * 0.65f)

        // Start offset drives the rotation
        val startOffset = (rotationAngle / 360f) * totalLength

        clippedPath.reset()
        fullPathMeasure.getSegment(startOffset, startOffset + arcLength, clippedPath, true)

        // Wrap-around when arc end exceeds total path length
        val endOffset = startOffset + arcLength
        if (endOffset > totalLength) {
            fullPathMeasure.getSegment(0f, endOffset - totalLength, clippedPath, true)
        }

        canvas.drawPath(clippedPath, paint)
    }

    /**
     * Builds the wavy circle path into [wavyPath].
     * Uses 180 segments — half the original 360 — which is visually smooth
     * at all realistic screen sizes and halves the trigonometric workload.
     */
    private fun buildWavyCirclePath(
        cx: Float,
        cy: Float,
        radius: Float,
        clampedAmplitude: Float
    ) {
        wavyPath.reset()
        val steps = 180
        for (i in 0..steps) {
            val angle = (i.toDouble() / steps) * 2.0 * Math.PI
            val wave  = sin(angle * waveCount + wavePhase) * clampedAmplitude
            val r     = radius + wave
            val x     = (cx + r * cos(angle - Math.PI / 2)).toFloat()
            val y     = (cy + r * sin(angle - Math.PI / 2)).toFloat()
            if (i == 0) wavyPath.moveTo(x, y) else wavyPath.lineTo(x, y)
        }
        wavyPath.close()
    }

    // ── Animators ─────────────────────────────────────────────────────────────

    private fun startAnimators() {
        rotationAnimator = ValueAnimator.ofFloat(0f, 360f).apply {
            duration = rotationSpeedMs
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener {
                rotationAngle = it.animatedValue as Float
                invalidate()   // drives the draw loop
            }
            start()
        }

        arcAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = arcSpeedMs
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener {
                arcPhase = it.animatedValue as Float
                // invalidate() is already triggered by rotationAnimator on the
                // same vsync; a second call here is a no-op if the View is
                // already dirty, but ensures arc is current if rotation is paused.
                invalidate()
            }
            start()
        }

        waveAnimator = ValueAnimator.ofFloat(0f, (2.0 * Math.PI).toFloat()).apply {
            duration = (rotationSpeedMs * 0.75f).toLong()
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener {
                wavePhase = it.animatedValue as Float
                // Trigger invalidate so the path cache check in onDraw runs;
                // path will only be rebuilt if wavePhase actually changed.
                invalidate()
            }
            start()
        }
    }

    private fun stopAnimators() {
        rotationAnimator?.cancel()
        arcAnimator?.cancel()
        waveAnimator?.cancel()
        // Reset cache so next start() rebuilds the path fresh.
        lastBuiltWavePhase = Float.NaN
    }

    private fun restartAnimators() {
        stopAnimators()
        startAnimators()
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun resolveColorPrimary(context: Context): Int {
        return MaterialColors.getColor(context, AppCompatR.attr.colorPrimary, 0xFF6200EE.toInt())
    }
}
