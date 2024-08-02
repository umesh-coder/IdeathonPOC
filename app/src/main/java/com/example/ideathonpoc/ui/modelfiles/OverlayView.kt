package com.example.ideathonpoc.ui.modelfiles

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View

class OverlayView(context: Context?, attrs: AttributeSet?) : View(context, attrs) {

    private var results = listOf<BoundingBox>()
    private var boxPaints = mutableMapOf<String, Paint>()
    private var textBackgroundPaint = Paint()
    private var textPaint = Paint()

    private var bounds = Rect()
    private var requiredItems = listOf<String>()
    private var detectedItems = mutableSetOf<String>()

    init {
        initPaints()
    }

    fun clear() {
        textPaint.reset()
        textBackgroundPaint.reset()
        boxPaints.clear()
        invalidate()
        initPaints()
    }

    private fun initPaints() {
        textBackgroundPaint.color = Color.BLACK
        textBackgroundPaint.style = Paint.Style.FILL
        textBackgroundPaint.textSize = 50f

        textPaint.color = Color.WHITE
        textPaint.style = Paint.Style.FILL
        textPaint.textSize = 50f

        // Initialize 7 different colors for the 7 classes
        val colors = listOf(
            Color.RED,
            Color.GREEN,
            Color.BLUE,
            Color.YELLOW,
            Color.CYAN,
            Color.MAGENTA,
            Color.WHITE
        )

        // Assuming you have 7 class names, replace these with your actual class names
        val classNames = listOf("Gloves", "Helmet", "Mask", "Person", "Safety Glasses", "Safety Shoe", "Safety Vest")

        classNames.forEachIndexed { index, className ->
            boxPaints[className] = Paint().apply {
                color = colors[index]
                strokeWidth = 8F
                style = Paint.Style.STROKE
            }
        }
    }

    private fun getBoxPaint(className: String): Paint {
        return boxPaints[className] ?: boxPaints.values.first()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        results.forEach { box ->
            val left = box.x1 * width
            val top = box.y1 * height
            val right = box.x2 * width
            val bottom = box.y2 * height

            val boxPaint = getBoxPaint(box.clsName)
            canvas.drawRect(left, top, right, bottom, boxPaint)

            val drawableText = "${box.clsName} (${String.format("%.2f", box.cnf)})"

            textBackgroundPaint.getTextBounds(drawableText, 0, drawableText.length, bounds)
            val textWidth = bounds.width()
            val textHeight = bounds.height()
            canvas.drawRect(
                left,
                top,
                left + textWidth + BOUNDING_RECT_TEXT_PADDING,
                top + textHeight + BOUNDING_RECT_TEXT_PADDING,
                textBackgroundPaint
            )
            canvas.drawText(drawableText, left, top + bounds.height(), textPaint)
        }
        // Draw ticks and crosses for detected and required items in the top-left corner
        val startX = 10f
        val startY = 10f
        val lineHeight = 60f

        requiredItems.forEachIndexed { index, item ->
            val statusText = if (detectedItems.contains(item)) "✔ $item" else "❌ $item"
            val x = startX
            val y = startY + (index + 1) * lineHeight

            textPaint.color = if (detectedItems.contains(item)) Color.GREEN else Color.RED
            canvas.drawText(statusText, x, y, textPaint)
        }
    }

    fun setResults(boundingBoxes: List<BoundingBox>) {
        results = boundingBoxes
        invalidate()
    }
    fun setRequiredItems(items: List<String>) {
        requiredItems = items
        invalidate()
    }

    fun setDetectedItems(items: Set<String>) {
        detectedItems = items.toMutableSet()
        invalidate()
    }

    companion object {
        private const val BOUNDING_RECT_TEXT_PADDING = 8
    }
}