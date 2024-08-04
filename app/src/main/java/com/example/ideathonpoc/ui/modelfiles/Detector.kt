package com.example.ideathonpoc.ui.modelfiles

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.os.SystemClock
import android.util.Log
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.common.ops.CastOp
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import kotlin.math.log
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

class Detector(
    private val context: Context,
    private val modelPath: String,
    private val labelPath: String,
    private val detectorListener: DetectorListener,
    private val requiredSafetyItems: List<String>
) {

    private var interpreter: Interpreter? = null
    private var labels = mutableListOf<String>()
    val detectedItems = mutableSetOf<String>()
    private var tensorWidth = 0
    private var tensorHeight = 0
    private var numChannel = 0
    private var numElements = 0

    private val imageProcessor = ImageProcessor.Builder()
        .add(NormalizeOp(INPUT_MEAN, INPUT_STANDARD_DEVIATION))
        .add(CastOp(INPUT_IMAGE_TYPE))
        .build()

    fun findNearestLimitedColor(rgb: Int): String {
        val red = Color.red(rgb)
        val green = Color.green(rgb)
        val blue = Color.blue(rgb)

        return limitedColors.minByOrNull { namedColor ->
            val namedRed = Color.red(namedColor.rgb)
            val namedGreen = Color.green(namedColor.rgb)
            val namedBlue = Color.blue(namedColor.rgb)

            val distance = sqrt(
                (namedRed - red).toDouble().pow(2.0) +
                        (namedGreen - green).toDouble().pow(2.0) +
                        (namedBlue - blue).toDouble().pow(2.0)
            )
            distance
        }?.name ?: "Unknown"
    }
    fun setup() {
        val model = FileUtil.loadMappedFile(context, modelPath)
        val options = Interpreter.Options()
        options.numThreads = 4
        interpreter = Interpreter(model, options)

        val inputShape = interpreter?.getInputTensor(0)?.shape() ?: return
        val outputShape = interpreter?.getOutputTensor(0)?.shape() ?: return

        tensorWidth = inputShape[1]
        tensorHeight = inputShape[2]
        numChannel = outputShape[1]
        numElements = outputShape[2]

        try {
            val inputStream: InputStream = context.assets.open(labelPath)
            val reader = BufferedReader(InputStreamReader(inputStream))

            var line: String? = reader.readLine()
            while (line != null && line != "") {
                labels.add(line)
                line = reader.readLine()
            }

            reader.close()
            inputStream.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
    fun analyzeColorInBoundingBox(frame: Bitmap, box: BoundingBox): String {
        val x = (box.x1 * frame.width).roundToInt()
        val y = (box.y1 * frame.height).roundToInt()
        val width = ((box.x2 - box.x1) * frame.width).roundToInt()
        val height = ((box.y2 - box.y1) * frame.height).roundToInt()

        // Ensure we don't go out of bounds
        val safeX = x.coerceIn(0, frame.width - 1)
        val safeY = y.coerceIn(0, frame.height - 1)
        val safeWidth = width.coerceAtMost(frame.width - safeX)
        val safeHeight = height.coerceAtMost(frame.height - safeY)

        var redSum = 0
        var greenSum = 0
        var blueSum = 0
        var pixelCount = 0

        for (i in safeX until (safeX + safeWidth)) {
            for (j in safeY until (safeY + safeHeight)) {
                val pixel = frame.getPixel(i, j)
                redSum += Color.red(pixel)
                greenSum += Color.green(pixel)
                blueSum += Color.blue(pixel)
                pixelCount++
            }
        }

        // Calculate average color
        val avgRed = (redSum / pixelCount).coerceIn(0, 255)
        val avgGreen = (greenSum / pixelCount).coerceIn(0, 255)
        val avgBlue = (blueSum / pixelCount).coerceIn(0, 255)

        val avgColor = Color.rgb(avgRed, avgGreen, avgBlue)
        val nearestColorName = findNearestLimitedColor(avgColor)
        return nearestColorName
    }

    fun clear() {
        interpreter?.close()
        interpreter = null
    }

    fun detect(frame: Bitmap) {
        interpreter ?: return
        if (tensorWidth == 0) return
        if (tensorHeight == 0) return
        if (numChannel == 0) return
        if (numElements == 0) return

        var inferenceTime = SystemClock.uptimeMillis()

        val resizedBitmap = Bitmap.createScaledBitmap(frame, tensorWidth, tensorHeight, false)

        val tensorImage = TensorImage(DataType.FLOAT32)
        tensorImage.load(resizedBitmap)
        val processedImage = imageProcessor.process(tensorImage)
        val imageBuffer = processedImage.buffer

        val output =
            TensorBuffer.createFixedSize(intArrayOf(1, numChannel, numElements), OUTPUT_IMAGE_TYPE)
        interpreter?.run(imageBuffer, output.buffer)


        val bestBoxes = bestBox(output.floatArray)
        inferenceTime = SystemClock.uptimeMillis() - inferenceTime


        if (bestBoxes == null) {
            detectorListener.onEmptyDetect()
        }else {
            bestBoxes.map { box ->
                if (box.clsName == "Helmet") {
                    val color = analyzeColorInBoundingBox(frame, box)
                    box.helmetColor = color
//                    Log.e("Helmet color", "detect: $color", )
                }
//                Log.e("box", "detect: ${box.helmetColor}", )
            }
            Log.e("box", "detect: ${bestBoxes.map { it.helmetColor }}", )


            detectorListener.onDetect(bestBoxes, inferenceTime)
        }


        // Clear detected items after each frame
//        detectedItems.clear()


    }

    //    private fun bestBox(array: FloatArray) : List<BoundingBox>? {
//
//        val boundingBoxes = mutableListOf<BoundingBox>()
//
//        for (c in 0 until numElements) {
//            var maxConf = -1.0f
//            var maxIdx = -1
//            var j = 4
//            var arrayIdx = c + numElements * j
//            while (j < numChannel){
//                if (array[arrayIdx] > maxConf) {
//                    maxConf = array[arrayIdx]
//                    maxIdx = j - 4
//                }
//                j++
//                arrayIdx += numElements
//            }
//
//            if (maxConf > CONFIDENCE_THRESHOLD) {
//                val clsName = labels[maxIdx]
//                val cx = array[c] // 0
//                val cy = array[c + numElements] // 1
//                val w = array[c + numElements * 2]
//                val h = array[c + numElements * 3]
//                val x1 = cx - (w/2F)
//                val y1 = cy - (h/2F)
//                val x2 = cx + (w/2F)
//                val y2 = cy + (h/2F)
//                if (x1 < 0F || x1 > 1F) continue
//                if (y1 < 0F || y1 > 1F) continue
//                if (x2 < 0F || x2 > 1F) continue
//                if (y2 < 0F || y2 > 1F) continue
//
//                boundingBoxes.add(
//                    BoundingBox(
//                        x1 = x1, y1 = y1, x2 = x2, y2 = y2,
//                        cx = cx, cy = cy, w = w, h = h,
//                        cnf = maxConf, cls = maxIdx, clsName = clsName
//                    )
//                )
//            }
//        }
//
//        if (boundingBoxes.isEmpty()) return null
//
//        return applyNMS(boundingBoxes)
//    }

    private fun bestBox(array: FloatArray): List<BoundingBox>? {
        val boundingBoxes = mutableListOf<BoundingBox>()
        val excludeClasses = setOf(
            "NO-Safety",
            "No-Glasses",
            "No-Glove",
            "No-Helmet",
            "No-Mask",
            "noi"
        )

        for (c in 0 until numElements) {
            var maxConf = -1.0f
            var maxIdx = -1
            var j = 4
            var arrayIdx = c + numElements * j
            while (j < numChannel) {
                if (array[arrayIdx] > maxConf) {
                    maxConf = array[arrayIdx]
                    maxIdx = j - 4
                }
                j++
                arrayIdx += numElements
            }

            if (maxConf > CONFIDENCE_THRESHOLD) {
                val clsName = labels[maxIdx]
                if (clsName !in excludeClasses) {
                    val cx = array[c]
                    val cy = array[c + numElements]
                    val w = array[c + numElements * 2]
                    val h = array[c + numElements * 3]
                    val x1 = cx - (w / 2F)
                    val y1 = cy - (h / 2F)
                    val x2 = cx + (w / 2F)
                    val y2 = cy + (h / 2F)
                    if (x1 < 0F || x1 > 1F || y1 < 0F || y1 > 1F || x2 < 0F || x2 > 1F || y2 < 0F || y2 > 1F) continue

                    boundingBoxes.add(
                        BoundingBox(
                            x1 = x1, y1 = y1, x2 = x2, y2 = y2,
                            cx = cx, cy = cy, w = w, h = h,
                            cnf = maxConf, cls = maxIdx, clsName = clsName, helmetColor =""
                        )
                    )

                    // Mark the item as detected
                    if ( clsName in requiredSafetyItems ) {
                        detectedItems.add(clsName)
                    }
                }
            }
        }

        // Check if all required items have been detected
        if (detectedItems.size == requiredSafetyItems.size && detectedItems.containsAll(requiredSafetyItems)) {
            detectorListener.onAllRequiredItemsDetected()
//            detectedItems.clear()
        }

        if (boundingBoxes.isEmpty()) return null

        return applyNMS(boundingBoxes)
    }


    private fun applyNMS(boxes: List<BoundingBox>): MutableList<BoundingBox> {
        val sortedBoxes = boxes.sortedByDescending { it.cnf }.toMutableList()
        val selectedBoxes = mutableListOf<BoundingBox>()

        while (sortedBoxes.isNotEmpty()) {
            val first = sortedBoxes.first()
            selectedBoxes.add(first)
            sortedBoxes.remove(first)

            val iterator = sortedBoxes.iterator()
            while (iterator.hasNext()) {
                val nextBox = iterator.next()
                val iou = calculateIoU(first, nextBox)
                if (iou >= IOU_THRESHOLD) {
                    iterator.remove()
                }
            }
        }

        return selectedBoxes
    }

    private fun calculateIoU(box1: BoundingBox, box2: BoundingBox): Float {
        val x1 = maxOf(box1.x1, box2.x1)
        val y1 = maxOf(box1.y1, box2.y1)
        val x2 = minOf(box1.x2, box2.x2)
        val y2 = minOf(box1.y2, box2.y2)
        val intersectionArea = maxOf(0F, x2 - x1) * maxOf(0F, y2 - y1)
        val box1Area = box1.w * box1.h
        val box2Area = box2.w * box2.h
        return intersectionArea / (box1Area + box2Area - intersectionArea)
    }

    interface DetectorListener {
        fun onEmptyDetect()
        fun onDetect(boundingBoxes: List<BoundingBox>, inferenceTime: Long)
        fun onAllRequiredItemsDetected()
    }

    companion object {
        private const val INPUT_MEAN = 0f
        private const val INPUT_STANDARD_DEVIATION = 255f
        private val INPUT_IMAGE_TYPE = DataType.FLOAT32
        private val OUTPUT_IMAGE_TYPE = DataType.FLOAT32
        private const val CONFIDENCE_THRESHOLD = 0.5F
        private const val IOU_THRESHOLD = 0.5F
    }
}
data class NamedColor(val name: String, val rgb: Int)

val limitedColors = listOf(
    NamedColor("White", Color.rgb(255, 255, 255)),
    NamedColor("Orange", Color.rgb(255, 165, 0)),
    NamedColor("Blue", Color.rgb(0, 0, 255))
)