package com.mediasandesh.news

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.view.View
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

class SplashAnimationView(context: Context) : View(context) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val red = Color.rgb(215, 25, 32)
    private val darkRed = Color.argb(225, 155, 10, 18)
    private val lightRed = Color.argb(245, 245, 45, 48)
    private val arcRect = RectF()
    private val path = Path()
    private var progress = 0f
    private val density = resources.displayMetrics.density

    fun setProgress(value: Float) {
        progress = value.coerceIn(0f, 1f)
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        val base = min(w, h) * 0.37f
        glowPaint.shader = RadialGradient(
            w / 2f, h * 0.39f, base * 1.35f,
            Color.argb(42, 215, 25, 32), Color.TRANSPARENT, Shader.TileMode.CLAMP
        )
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val cx = w / 2f
        val cy = h * 0.39f
        val base = min(w, h) * 0.37f

        canvas.drawCircle(cx, cy, base * 1.35f, glowPaint)

        paint.style = Paint.Style.STROKE
        paint.strokeCap = Paint.Cap.ROUND
        val rings = floatArrayOf(0.97f, 1.04f, 1.11f)
        for (i in rings.indices) {
            paint.strokeWidth = (if (i == 0) 3f else 1.5f) * density
            paint.color = Color.argb(230 - i * 50, 215, 25, 32)
            canvas.drawCircle(cx, cy, base * rings[i], paint)
        }

        val r = base * 1.11f
        arcRect.set(cx - r, cy - r, cx + r, cy + r)
        paint.strokeWidth = 4.5f * density
        paint.color = red
        val start = (progress * 360f) % 360f
        canvas.drawArc(arcRect, start, 92f, false, paint)

        val angle = Math.toRadians((start + 92f).toDouble())
        paint.style = Paint.Style.FILL
        paint.color = Color.WHITE
        canvas.drawCircle(cx + r * cos(angle).toFloat(), cy + r * sin(angle).toFloat(), 3f * density, paint)

        val wave = sin(progress * Math.PI * 2.0).toFloat() * 8f * density

        path.reset()
        path.moveTo(0f, h * 0.80f + wave)
        path.cubicTo(w * 0.22f, h * 0.75f + wave, w * 0.48f, h * 0.84f + wave, w * 0.72f, h * 0.78f + wave)
        path.cubicTo(w * 0.86f, h * 0.75f + wave, w * 0.96f, h * 0.78f + wave, w, h * 0.76f + wave)
        path.lineTo(w, h)
        path.lineTo(0f, h)
        path.close()
        paint.color = darkRed
        canvas.drawPath(path, paint)

        path.reset()
        path.moveTo(0f, h * 0.85f - wave)
        path.cubicTo(w * 0.25f, h * 0.80f - wave, w * 0.50f, h * 0.88f - wave, w * 0.75f, h * 0.82f - wave)
        path.cubicTo(w * 0.88f, h * 0.79f - wave, w * 0.96f, h * 0.83f - wave, w, h * 0.81f - wave)
        path.lineTo(w, h)
        path.lineTo(0f, h)
        path.close()
        paint.color = lightRed
        canvas.drawPath(path, paint)
    }
}
