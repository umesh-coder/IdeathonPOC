package com.example.ideathonpoc.ui.modelfiles

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.os.Handler
import android.util.AttributeSet
import android.view.View
import android.widget.Toast
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat
import com.example.ideathonpoc.R

class OverlayView(context: Context?, attrs: AttributeSet?) : View(context, attrs) {

    private var glovesCountListener: GlovesCountListener? = null
    private var shoesCountListener: ShoesCountListener? = null


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

    private var countdownValue: Int? = null
    private val countdownPaint = Paint()

    private var isScanning = true  // Flag to check if it's scanning

    private val handler = Handler()
    private var gloves_count : Int = 0
    private var requiredHelmetColor = "White"

    init {
        initPaints()
        countdownPaint.color = Color.WHITE
        countdownPaint.textSize = 200f
        countdownPaint.textAlign = Paint.Align.CENTER
        countdownPaint.isFakeBoldText = true
        startScanning()
    }

    fun setRequiredHelmetColor(color: String) {
        requiredHelmetColor = color

    }
    private fun getMostFrequentHelmetColor(): String? {
        val colorCounts = results
            .filter { it.clsName == "Helmet" && it.helmetColor != null }
            .groupBy { it.helmetColor }
            .mapValues { it.value.size }

        return colorCounts.maxByOrNull { it.value }?.key
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
        val mostFrequentHelmetColor = getMostFrequentHelmetColor()

        // Calculate the starting Y position to draw elements at the bottom center
        val totalHeight = requiredItems.size * lineHeight
        val startY = height - totalHeight - 50f  // 50f is extra bottom margin

        requiredItems.forEachIndexed { index, item ->
            val isHelmet = item.contains("Helmet", ignoreCase = true)
            val helmetDetected = results.any { it.clsName == "Helmet" }
            val helmetColorMatched = isHelmet && helmetDetected && mostFrequentHelmetColor == requiredHelmetColor
            val rectPaint = when {
                isScanning -> rectPaintScanning
//                isHelmet && helmetColorMatched -> rectPaintDetected
                detectedItems.contains(item)  -> rectPaintDetected
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
//                isHelmet && helmetColorMatched -> R.drawable.success_icon
                detectedItems.contains(item)  -> R.drawable.success_icon
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
        countdownValue?.let { value ->
            val centerX = width / 2f
            val centerY = height / 2f
            val countdownText = if (value == 0) "GO!" else value.toString()
            canvas.drawText(countdownText, centerX, centerY, countdownPaint)
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

    private fun setCount() {
        val gloves = results.count { it.clsName == "Gloves" }
        val shoes = results.count { it.clsName == "Safety Shoe" }
        // Notify listener about the gloves count
        glovesCountListener?.onGlovesCountUpdated(gloves)
        shoesCountListener?.onShoesCountUpdated(shoes)

    }

    fun setGlovesCountListener(listener: GlovesCountListener) {
        glovesCountListener = listener
//        invalidate()
    }

    fun setShoesCountListener(listener: ShoesCountListener) {
        shoesCountListener = listener
//        invalidate()
    }


    fun setDetectedItems(items: Set<String>) {
        setCount()
        detectedItems.addAll(items)
        // Optionally invalidate to refresh the view
        invalidate()
    }
    fun setCountdown(value: Int?) {
        countdownValue = value
        invalidate()
    }

    interface GlovesCountListener {
        fun onGlovesCountUpdated(count: Int)
    }

    interface ShoesCountListener {
        fun onShoesCountUpdated(count: Int)
    }
}
