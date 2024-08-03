package com.example.ideathonpoc.ui.modelfiles


import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat
import com.example.ideathonpoc.R

class OverlayView(context: Context?, attrs: AttributeSet?) : View(context, attrs) {

    private var results = listOf<BoundingBox>()
    private var requiredItems = listOf<String>()
    private var detectedItems = mutableSetOf<String>()

    private val rectPaintDetected = Paint()
    private val rectPaintUndetected = Paint()
    private val textPaint = Paint()
    private val borderPaint = Paint()
    private val iconSize = 80 // Size for the icons
    private val padding = 20f
    private val textBounds = Rect()

    private var countdownValue: Int? = null
    private val countdownPaint = Paint()

    init {
        initPaints()
        countdownPaint.color = Color.WHITE
        countdownPaint.textSize = 200f
        countdownPaint.textAlign = Paint.Align.CENTER
        countdownPaint.isFakeBoldText = true
    }

    private fun initPaints() {
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
            val isDetected = detectedItems.contains(item)
            val rectPaint = if (isDetected) rectPaintDetected else rectPaintUndetected

            // Measure text size
            textPaint.getTextBounds(item, 0, item.length, textBounds)
            val textWidth = textBounds.width()

            // Calculate rectangle dimensions
            val totalWidth = textWidth + padding * 4 + iconTextSpacing + iconSize
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

            // Draw SVG
            // icon
            val iconRes = if (isDetected) R.drawable.success_icon else R.drawable.failure_icon  // Replace with your actual drawable names
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
            val textY =
                rectTop + rectHeight / 2 + textBounds.height() / 2 - 10  // Centered vertically within the rect
            canvas.drawText(item, textX, textY, textPaint)
        }

        countdownValue?.let { value ->
            val centerX = width / 2f
            val centerY = height / 2f
            val countdownText = if (value == 0) "GO!" else value.toString()
            canvas.drawText(countdownText, centerX, centerY, countdownPaint)
        }
    }

    fun setResults(boundingBoxes: List<BoundingBox>) {
        results = boundingBoxes
//        invalidate()
    }

    fun setRequiredItems(items: List<String>) {
        requiredItems = items
    }

    fun setDetectedItems(items: Set<String>) {
        detectedItems.addAll(items)
    }

    fun setCountdown(value: Int?) {
        countdownValue = value
        invalidate()
    }
}
