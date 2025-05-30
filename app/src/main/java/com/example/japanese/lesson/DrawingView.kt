package com.example.japanese.lesson


import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import com.google.mlkit.vision.digitalink.Ink
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class DrawingView(context: Context, attrs: AttributeSet) : View(context, attrs) {
    private val paths = mutableListOf<Pair<Path, Paint>>()
    private var currentPath = Path()
    private var currentPaint = Paint()
    private var inkBuilder = Ink.builder()
    private var strokeBuilder = Ink.Stroke.builder()

    private var lastX = 0f
    private var lastY = 0f
    private var lastVelocity = 0f

    private val baseStrokeWidth = 20f
    private val minStrokeWidth = 5f
    private val maxStrokeWidth = 30f

    private var touchBlocked = false

    init {
        setupPaint()
    }

    private fun setupPaint() {
        val typedValue = TypedValue()
        context.theme.resolveAttribute(com.example.japanese.R.attr.drawingColor, typedValue, true)
        val drawingColor = typedValue.data

        currentPaint = Paint().apply {
            color = drawingColor
            style = Paint.Style.STROKE
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
            strokeWidth = baseStrokeWidth
            isAntiAlias = true
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        paths.forEach { (path, paint) -> canvas.drawPath(path, paint) }
        canvas.drawPath(currentPath, currentPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (touchBlocked) return true

        val x = event.x
        val y = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                currentPath = Path()
                currentPath.moveTo(x, y)
                strokeBuilder = Ink.Stroke.builder()
                strokeBuilder.addPoint(Ink.Point.create(x, y))
                lastX = x
                lastY = y
                lastVelocity = 0f
                currentPaint.strokeWidth = baseStrokeWidth
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = abs(x - lastX)
                val dy = abs(y - lastY)
                if (dx >= 1 || dy >= 1) {
                    val velocity = (dx + dy) / 2
                    lastVelocity = lastVelocity * 0.4f + velocity * 0.6f // Smooth velocity
                    val strokeWidth = calculateStrokeWidth(lastVelocity)

                    // Update the current paint's stroke width
                    currentPaint.strokeWidth = strokeWidth

                    // Draw a quadratic bezier curve for smoother lines
                    val midX = (lastX + x) / 2
                    val midY = (lastY + y) / 2
                    currentPath.quadTo(lastX, lastY, midX, midY)

                    strokeBuilder.addPoint(Ink.Point.create(x, y))
                    lastX = x
                    lastY = y
                }
            }
            MotionEvent.ACTION_UP -> {
                currentPath.lineTo(x, y)
                paths.add(Pair(Path(currentPath), Paint(currentPaint)))
                inkBuilder.addStroke(strokeBuilder.build())
            }
        }
        invalidate()
        return true
    }

    private fun calculateStrokeWidth(velocity: Float): Float {
        // Inverse relationship: higher velocity = thinner stroke
        val normalizedVelocity = min(max(velocity, 0f), 100f) / 100f
        return maxStrokeWidth - (maxStrokeWidth - minStrokeWidth) * normalizedVelocity
    }

    fun getInk(): Ink = inkBuilder.build()

    fun clear() {
        paths.clear()
        currentPath = Path()
        inkBuilder = Ink.builder()
        strokeBuilder = Ink.Stroke.builder()
        invalidate()
    }

    fun undo() {
        if (paths.isNotEmpty()) {
            paths.removeLast()
            val currentInk = inkBuilder.build()
            inkBuilder = Ink.builder()
            for (i in 0 until currentInk.strokes.size - 1) {
                inkBuilder.addStroke(currentInk.strokes[i])
            }
            currentPath.reset() // Clear the current path
            invalidate()
        }
    }

    fun getPaths(): List<Pair<Path, Paint>> = paths

    fun drawCheckSign() {
        val centerX = width / 2f
        val centerY = height / 2f
        val size = minOf(width, height) * 0.4f

        val checkPath = Path().apply {
            moveTo(centerX - size * 0.3f, centerY)
            lineTo(centerX - size * 0.1f, centerY + size * 0.3f)
            lineTo(centerX + size * 0.4f, centerY - size * 0.3f)
        }

        val checkPaint = Paint(currentPaint).apply {
            color = Color.GREEN
            strokeWidth = baseStrokeWidth * 1.5f
        }

        paths.add(Pair(checkPath, checkPaint))
        invalidate()
    }

    fun drawCrossSign() {
        val centerX = width / 2f
        val centerY = height / 2f
        val size = minOf(width, height) * 0.3f

        val crossPath = Path().apply {
            moveTo(centerX - size, centerY - size)
            lineTo(centerX + size, centerY + size)
            moveTo(centerX + size, centerY - size)
            lineTo(centerX - size, centerY + size)
        }

        val crossPaint = Paint(currentPaint).apply {
            color = Color.RED
            strokeWidth = baseStrokeWidth * 1.5f
        }

        paths.add(Pair(crossPath, crossPaint))
        invalidate()
    }

    fun blockTouch(block: Boolean) {
        touchBlocked = block
    }
}