package com.example.ideathonpoc.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.view.drawToBitmap
import androidx.fragment.app.Fragment
import com.example.ideathonpoc.R
import com.example.ideathonpoc.databinding.FragmentCameraBinding
import com.example.ideathonpoc.ui.modelfiles.BoundingBox
import com.example.ideathonpoc.ui.modelfiles.Constants
import com.example.ideathonpoc.ui.modelfiles.Detector
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.file.Files.createFile
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraFragment : Fragment(), Detector.DetectorListener {

    private var _binding: FragmentCameraBinding? = null
    private val binding get() = _binding!!
    private var latestFrame: Bitmap? = null
    private var isFrontCamera = false
    private var cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

    private var preview: Preview? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private lateinit var detector: Detector
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var requiredSafetyItems: List<String>

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.CAMERA] == true) {
            startCamera()
        } else {
            Log.e(TAG, "Camera permission denied")
            navigateBack()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCameraBinding.inflate(inflater, container, false)
        requiredSafetyItems = listOf("Helmet", "Safety Vest","Gloves")

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        try {
            detector = Detector(requireContext(), Constants.MODEL_PATH, Constants.LABELS_PATH, this,requiredSafetyItems)
            detector.setup()

            if (allPermissionsGranted()) {
                startCamera()
            } else {
                requestPermissionLauncher.launch(REQUIRED_PERMISSIONS)
            }

            cameraExecutor = Executors.newSingleThreadExecutor()

            requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    navigateBack()
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "Error in onViewCreated", e)
            navigateBack()
        }

        binding.switchCameraButton.setOnClickListener {
            switchCamera()
        }
    }

    override fun onPause() {
        super.onPause()
        try {
            imageAnalyzer?.clearAnalyzer()
            cameraProvider?.unbindAll()
        } catch (e: Exception) {
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        cleanupResources()
        _binding = null
    }

    private fun navigateBack() {
        cleanupResources()
        parentFragmentManager.popBackStack()
    }

    private fun switchCamera() {
        isFrontCamera = !isFrontCamera
        cameraSelector = if (isFrontCamera) {
            CameraSelector.DEFAULT_FRONT_CAMERA
        } else {
            CameraSelector.DEFAULT_BACK_CAMERA
        }
        startCamera()
    }

    private fun cleanupResources() {
        try {
            imageAnalyzer?.clearAnalyzer()
            cameraProvider?.unbindAll()
            detector.clear()
            cameraExecutor.shutdownNow()
        } catch (e: Exception) {
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
                bindCameraUseCases()
            } catch (e: Exception) {
                Log.e(TAG, "Error starting camera", e)
                navigateBack()
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun bindCameraUseCases() {
        val cameraProvider = cameraProvider ?: run {
            Log.e(TAG, "Camera initialization failed.")
            navigateBack()
            return
        }

        try {
            val rotation = binding.viewFinder.display.rotation

            preview = Preview.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setTargetRotation(rotation)
                .build()

            imageAnalyzer = ImageAnalysis.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setTargetRotation(binding.viewFinder.display.rotation)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build()

            imageAnalyzer?.setAnalyzer(cameraExecutor) { imageProxy ->
                processImage(imageProxy)
            }

            cameraProvider.unbindAll()

            camera = cameraProvider.bindToLifecycle(
                viewLifecycleOwner,
                cameraSelector,
                preview,
                imageAnalyzer
            )

            preview?.setSurfaceProvider(binding.viewFinder.surfaceProvider)
        } catch (exc: Exception) {
            Log.e(TAG, "Use case binding failed", exc)
            navigateBack()
        }
    }

    private fun processImage(imageProxy: ImageProxy) {
        try {
            val bitmapBuffer = Bitmap.createBitmap(
                imageProxy.width,
                imageProxy.height,
                Bitmap.Config.ARGB_8888
            )
            imageProxy.use { bitmapBuffer.copyPixelsFromBuffer(imageProxy.planes[0].buffer) }

            val matrix = Matrix().apply {
                postRotate(imageProxy.imageInfo.rotationDegrees.toFloat())
                if (isFrontCamera) {
                    postScale(
                        -1f,
                        1f,
                        imageProxy.width.toFloat(),
                        imageProxy.height.toFloat()
                    )
                }
            }

            val rotatedBitmap = Bitmap.createBitmap(
                bitmapBuffer, 0, 0, bitmapBuffer.width, bitmapBuffer.height,
                matrix, true
            )

            latestFrame = rotatedBitmap
            detector.detect(rotatedBitmap)
        } catch (e: Exception) {
            Log.e(TAG, "Error processing image", e)
        } finally {
            imageProxy.close()
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onEmptyDetect() {
        _binding?.overlay?.post {
            _binding?.overlay?.invalidate()
        }
    }

    override fun onDetect(boundingBoxes: List<BoundingBox>, inferenceTime: Long) {
        _binding?.let { binding ->
            binding.root.post {
                binding.inferenceTime.text = "${inferenceTime}ms"
                binding.overlay.apply {
                    setResults(boundingBoxes)
                    invalidate()
                }
            }
        }
    }

    companion object {
        private const val TAG = "CameraFragment"
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }

    override fun onAllRequiredItemsDetected() {
        // This method will be called when all required safety items are detected
        binding.root.postDelayed({
            takeScreenshot()
        }, 100)
    }


    private fun takeScreenshot() {
        val imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()

        // Unbind existing use cases and rebind with imageCapture
        cameraProvider?.unbindAll()
        camera = cameraProvider?.bindToLifecycle(
            viewLifecycleOwner,
            cameraSelector,
            preview,
            imageAnalyzer,
            imageCapture
        )

        val outputDirectory = getOutputDirectory()
        val photoFile = File(
            outputDirectory,
            "Screenshot_${System.currentTimeMillis()}.jpg"
        )

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(requireContext()),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val savedUri = output.savedUri ?: Uri.fromFile(photoFile)
                    Log.d(TAG, "Photo capture succeeded: $savedUri")

                    // Load the captured image
                    val capturedBitmap = BitmapFactory.decodeFile(photoFile.absolutePath)

                    // Get the overlay bitmap
                    val overlayBitmap = binding.overlay.drawToBitmap()

                    // Combine the bitmaps
                    val combinedBitmap = combineBitmaps(capturedBitmap, overlayBitmap)

                    // Save the combined bitmap
                    saveCombinedBitmap(combinedBitmap)
                }

                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                }
            }
        )
    }

    private fun getOutputDirectory(): File {
        val mediaDir = requireContext().externalMediaDirs.firstOrNull()?.let {
            File(it, resources.getString(R.string.app_name)).apply { mkdirs() }
        }
        return if (mediaDir != null && mediaDir.exists()) mediaDir else requireContext().filesDir
    }

    private fun combineBitmaps(background: Bitmap, overlay: Bitmap): Bitmap {
        val combined = Bitmap.createBitmap(background.width, background.height, background.config)
        val canvas = Canvas(combined)
        canvas.drawBitmap(background, Matrix(), null)

        // Scale the overlay to match the background size
        val scaledOverlay = Bitmap.createScaledBitmap(overlay, background.width, background.height, true)
        canvas.drawBitmap(scaledOverlay, Matrix(), null)

        return combined
    }

    private fun saveCombinedBitmap(bitmap: Bitmap) {
        val timestamp = System.currentTimeMillis()
        val fileName = "safety_screenshot_$timestamp.jpg"
        val file = File(requireContext().filesDir, fileName)

        try {
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
            }
            Log.d(TAG, "Combined screenshot saved: ${file.absolutePath}")
            Log.d(TAG, "File size: ${file.length()} bytes")
            navigateToResultActivity(file.absolutePath, timestamp)
        } catch (e: IOException) {
            Log.e(TAG, "Failed to save combined screenshot", e)
        }
    }
    private fun navigateToResultActivity(imagePath: String, timestamp: Long) {
        val intent = Intent(requireContext(), ResultActivity::class.java).apply {
            putExtra("imagePath", imagePath)
            putExtra("timestamp", timestamp)
        }
        startActivity(intent)
    }
}
