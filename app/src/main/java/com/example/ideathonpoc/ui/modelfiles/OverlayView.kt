package com.example.ideathonpoc.ui.modelfiles

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.os.Handler
import android.util.AttributeSet
import android.view.View
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat
import com.example.ideathonpoc.R

class OverlayView(context: Context?, attrs: AttributeSet?) : View(context, attrs) {

    private var results = listOf<BoundingBox>()
    private var requiredItems = listOf<String>()
    private var detectedItems = mutableSetOf<String>()

    private val rectPaintScanning = Paint()
    private val rectPaintDetected = Paint()
    private val rectPaintUndetected = Paint()
    private val textPaint = Paint()
    private val borderPaint = Paint()
    private val iconSize = 80 // Size for the icons
    private val padding = 20f
    private val textBounds = Rect()

    private var isScanning = true  // Flag to check if it's scanning

    private val handler = Handler()

    init {
        initPaints()
        startScanning()
    }

    private fun initPaints() {
        // Paint for scanning items
        rectPaintScanning.color = Color.parseColor("#5C48DA")
        rectPaintScanning.style = Paint.Style.FILL

        // Paint for detected items (Green background)
        rectPaintDetected.color = Color.parseColor("#4CAF50")
        rectPaintDetected.style = Paint.Style.FILL

        // Paint for undetected items (Red background)
        rectPaintUndetected.color = Color.parseColor("#F44336")
        rectPaintUndetected.style = Paint.Style.FILL

        // Paint for text
        textPaint.color = Color.WHITE
        textPaint.textSize = 50f
        textPaint.isAntiAlias = true

        // Paint for border (White)
        borderPaint.color = Color.WHITE
        borderPaint.style = Paint.Style.STROKE
        borderPaint.strokeWidth = 8f
        borderPaint.isAntiAlias = true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val centerX = width / 2f
        val lineHeight = 150f
        val rectHeight = 100f
        val rectCornerRadius = 50f
        val iconTextSpacing = 5f  // Spacing between icon and text

        // Calculate the starting Y position to draw elements at the bottom center
        val totalHeight = requiredItems.size * lineHeight
        val startY = height - totalHeight - 50f  // 50f is extra bottom margin

        requiredItems.forEachIndexed { index, item ->
            val rectPaint = when {
                isScanning -> rectPaintScanning
                detectedItems.contains(item) -> rectPaintDetected
                else -> rectPaintUndetected
            }

            // Measure text size
            textPaint.getTextBounds(item, 0, item.length, textBounds)
            val textWidth = textBounds.width()

            // Adjust width based on scanning state
            val extraWidthForScanning = if (isScanning) 180f else 0f  // Add extra width during scanning
            val totalWidth = textWidth + padding * 4 + iconTextSpacing + iconSize + extraWidthForScanning

            // Calculate rectangle dimensions
            val rectLeft = centerX - totalWidth / 2
            val rectTop = startY + (index * lineHeight)
            val rectRight = rectLeft + totalWidth
            val rectBottom = rectTop + rectHeight

            // Draw rounded rectangle
            canvas.drawRoundRect(
                rectLeft,
                rectTop,
                rectRight,
                rectBottom,
                rectCornerRadius,
                rectCornerRadius,
                rectPaint
            )

            // Draw white border around the rectangle
            canvas.drawRoundRect(
                rectLeft,
                rectTop,
                rectRight,
                rectBottom,
                rectCornerRadius,
                rectCornerRadius,
                borderPaint
            )

            // Draw SVG icon
            val iconRes = when {
                isScanning -> R.drawable.scan_icon
                detectedItems.contains(item) -> R.drawable.success_icon
                else -> R.drawable.failure_icon
            }
            val iconDrawable = VectorDrawableCompat.create(resources, iconRes, null)
            iconDrawable?.setBounds(
                rectLeft.toInt() + padding.toInt(),
                (rectTop.toInt() + (rectHeight - iconSize) / 2).toInt(),
                rectLeft.toInt() + padding.toInt() + iconSize,
                (rectTop.toInt() + (rectHeight + iconSize) / 2).toInt()
            )
            iconDrawable?.draw(canvas)

            // Draw text
            val textX = rectLeft + padding + iconSize + iconTextSpacing
            val textY = rectTop + rectHeight / 2 + textBounds.height() / 2 - 10  // Centered vertically within the rect
            val displayText = if (isScanning) "$item scanning" else item
            canvas.drawText(displayText, textX, textY, textPaint)
        }
    }

    private fun startScanning() {
        // Schedule to stop scanning and show results after 5 seconds
        handler.postDelayed({
            isScanning = false
            invalidate()  // Trigger a redraw to show results
        }, 5000)  // 5000 milliseconds = 5 seconds
    }

    fun setResults(boundingBoxes: List<BoundingBox>) {
        results = boundingBoxes
        // Optionally invalidate to refresh the view
        invalidate()
    }

    fun setRequiredItems(items: List<String>) {
        requiredItems = items
        // Optionally invalidate to refresh the view
        invalidate()
    }

    fun setDetectedItems(items: Set<String>) {
        detectedItems.addAll(items)
        // Optionally invalidate to refresh the view
        invalidate()
    }
}
