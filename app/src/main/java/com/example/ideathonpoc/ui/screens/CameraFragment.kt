package com.example.ideathonpoc.ui.screens

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.text.Html
import android.util.Log
import android.util.Size
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.ideathonpoc.R
import com.example.ideathonpoc.databinding.FragmentCameraBinding
import com.example.ideathonpoc.ui.modelfiles.BoundingBox
import com.example.ideathonpoc.ui.modelfiles.Constants
import com.example.ideathonpoc.ui.modelfiles.Detector
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraFragment : Fragment(), Detector.DetectorListener {

    private var _binding: FragmentCameraBinding? = null
    private val binding get() = _binding!!
    private var latestFrame: Bitmap? = null
    private var isFrontCamera = true
    private var cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

    private var preview: Preview? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private lateinit var detector: Detector
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var requiredSafetyItems: List<String>
    private lateinit var textToSpeech: TextToSpeech
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var detectionRunnable: Runnable
    private var isDetectionRunning = false
    private var selectedPermit: String? = null
    private  var mediaPlayer: MediaPlayer = MediaPlayer()

    private val countdownDuration = 4
    private var countdownTimer: CountDownTimer? = null

    private var isFragmentVisible = true


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
        requiredSafetyItems = arguments?.getStringArrayList("REQUIRED_ITEMS") ?: listOf()
        selectedPermit = arguments?.getString("PERMIT")
        textToSpeech = TextToSpeech(requireContext()) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech.language = Locale.UK
            }
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.overlay.setRequiredItems(requiredSafetyItems)


        try {
            initializeDetector()

            if (allPermissionsGranted()) {
                startCamera()
            } else {
                requestPermissionLauncher.launch(REQUIRED_PERMISSIONS)
            }

            cameraExecutor = Executors.newSingleThreadExecutor()

            requireActivity().onBackPressedDispatcher.addCallback(
                viewLifecycleOwner,
                object : OnBackPressedCallback(true) {
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
        startDetectionTimer()
    }

    private fun initializeDetector() {
        detector = Detector(
            requireContext(),
            Constants.MODEL_PATH,
            Constants.LABELS_PATH,
            this,
            requiredSafetyItems
        )
        detector.setup()
    }

    override fun onPause() {
        super.onPause()
        isFragmentVisible = false
        cleanupResources()
        try {
            imageAnalyzer?.clearAnalyzer()
            cameraProvider?.unbindAll()
        } catch (e: Exception) {
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        cleanupResources()
        countdownTimer?.cancel()
        textToSpeech.shutdown()
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
            handler.removeCallbacks(detectionRunnable)
            isDetectionRunning = false
            imageAnalyzer?.clearAnalyzer()
            cameraProvider?.unbindAll()
            detector.clear()
            textToSpeech.stop()
            textToSpeech.shutdown()
            cameraExecutor.shutdownNow()
        } catch (e: Exception) {
        }
    }


    override fun onResume() {
        super.onResume()
        isFragmentVisible = true

    }

    override fun onStop() {
        super.onStop()
        cleanupResources()

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
            Toast.makeText(activity, "Camera initialization failed.", Toast.LENGTH_SHORT).show()
            navigateBack()
            return
        }


        try {
            val rotation = binding.viewFinder.display.rotation

            preview = Preview.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_DEFAULT)
                .setTargetRotation(rotation)
                .build()

            analyzeImage()



        } catch (exc: Exception) {
            Log.e(TAG, "Use case binding failed", exc)
            navigateBack()
        }
    }

    private fun analyzeImage() {
        imageAnalyzer = null;
        imageAnalyzer = ImageAnalysis.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_DEFAULT)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setTargetRotation(binding.viewFinder.display.rotation)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
            .build()

        imageAnalyzer?.setAnalyzer(cameraExecutor) { imageProxy ->
            if (isDetectionRunning) {
                processImage(imageProxy)
            }
        }

        cameraProvider?.unbindAll()

        camera = cameraProvider?.bindToLifecycle(
            viewLifecycleOwner,
            cameraSelector,
            preview,
            imageAnalyzer
        )
        preview?.setSurfaceProvider(binding.viewFinder.surfaceProvider)
    }

    private fun startCountdown() {
        countdownTimer = object : CountDownTimer((countdownDuration * 1000).toLong(), 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsLeft = (millisUntilFinished / 1000).toInt()
                binding.overlay.setCountdown(secondsLeft)
            }

            override fun onFinish() {
                binding.overlay.setCountdown(null)
                // Play shutter sound and take screenshot
                val resID = resources.getIdentifier("shutter_sound", "raw", activity?.packageName)
                playMedia(context, resID)
                takeScreenshot()
            }
        }.start()
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
                    setDetectedItems(detector.detectedItems)
                    setResults(boundingBoxes)
                    invalidate()
                }
            }
        }

    }

    companion object {
        private const val TAG = "CameraFragment"
        private const val DETECTION_TIMEOUT = 10000L
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }

    override fun onAllRequiredItemsDetected() {
        handler.removeCallbacks(detectionRunnable)
        isDetectionRunning = false
        // This method will be called when all required safety items are detected
//        startCountdown()
        binding.root.postDelayed({
            val resID = resources.getIdentifier("success_sound", "raw", activity?.packageName)
            playMedia(context,resID)
            Toast.makeText(activity, "Taking your picture dont move",Toast.LENGTH_SHORT).show()
            startCountdown()
        }, 1000)


        // Navigate to ResultActivity after a delay

    }

    private fun startDetectionTimer() {
        if(!isFragmentVisible){
            return
        }
        detectionRunnable = Runnable {
            if (isDetectionRunning) {
                handler.removeCallbacks(detectionRunnable)
                isDetectionRunning = false
                val resID = resources.getIdentifier("failure_sound", "raw", activity?.packageName)
                playMedia(context,resID)
                showMissingItemsDialog()

            }
        }
        handler.postDelayed(detectionRunnable, DETECTION_TIMEOUT)
        isDetectionRunning = true
    }

    private fun playMedia(context: Context?, resID: Int) {
        try {
            mediaPlayer = MediaPlayer.create(context, resID)
            mediaPlayer.start()
            handler.postDelayed({  mediaPlayer.stop()},2000)

        } catch (exception: Exception) {
            exception.printStackTrace()
        }
    }

    private fun resetDetectionTimer() {
        handler.removeCallbacks(detectionRunnable)
        startDetectionTimer()
    }

    private fun showMissingItemsDialog() {
        val missingItems = requiredSafetyItems.filterNot { it in detector.detectedItems }
        if (missingItems.isNotEmpty()) {

            val message =
                Html.fromHtml("${missingItems.joinToString(", ")}  missing in your <b>P P E</b>, please wear right <b>P P E</b> to proceed for job.")

            Log.e(TAG, "Languages: " + textToSpeech.availableLanguages)
            val locale = Locale("hi_IN")
            textToSpeech.setLanguage(locale)
            textToSpeech.setSpeechRate(0.8f)
            if (textToSpeech.isLanguageAvailable(locale) == TextToSpeech.LANG_AVAILABLE) {
                textToSpeech.setLanguage(locale)
            } else {
                textToSpeech.setLanguage(Locale.ENGLISH)
            }
            if(!isFragmentVisible){
                return
            }
            textToSpeech.speak(message, TextToSpeech.QUEUE_FLUSH, null, null)

            val dialog = AlertDialog.Builder(requireContext())
                .setTitle("Missing Safety Items")
                .setMessage(message)
                .setCancelable(false)
                .create()

            dialog.setButton(AlertDialog.BUTTON_POSITIVE, "Rescan") { _, _ ->
                textToSpeech.stop()
                detector.detectedItems
                startDetectionTimer()
            }
            dialog.show()
            val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)

            positiveButton.setOnClickListener {
                dialog.dismiss()
                textToSpeech.stop()
                detector.detectedItems
                isDetectionRunning = true
                analyzeImage()
                startDetectionTimer()



            }

            /*object : CountDownTimer(5000, 1000) {
                override fun onTick(millisUntilFinished: Long) {
                    val secondsLeft = millisUntilFinished / 1000
                    positiveButton.text = "Rescan ($secondsLeft)"
                }

                override fun onFinish() {
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick()
                }

            }.start()*/
        }
    }

//    private fun takeScreenshot() {
//        val imageCapture = ImageCapture.Builder()
//            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
//            .setTargetRotation(binding.viewFinder.display.rotation)
//            .build()
//
//        // Unbind existing use cases and rebind with imageCapture
//        cameraProvider?.unbindAll()
//        camera = cameraProvider?.bindToLifecycle(
//            viewLifecycleOwner,
//            cameraSelector,
//            preview,
//            imageAnalyzer,
//            imageCapture
//        )
//
//        val outputDirectory = getOutputDirectory()
//        val photoFile = File(
//            outputDirectory,
//            "Screenshot_${System.currentTimeMillis()}.jpg"
//        )
//
//        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
//
//        imageCapture.takePicture(
//            outputOptions,
//            ContextCompat.getMainExecutor(requireContext()),
//            object : ImageCapture.OnImageSavedCallback {
//                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
//                    val savedUri = output.savedUri ?: Uri.fromFile(photoFile)
//                    Log.d(TAG, "Photo capture succeeded: $savedUri")
//
//                    // Navigate to ResultActivity with the captured image path
//                    navigateToResultActivity(photoFile.absolutePath, System.currentTimeMillis())
//                }
//
//                override fun onError(exc: ImageCaptureException) {
//                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
//                }
//            }
//        )
//    }

    private fun takeScreenshot() {

//        startCountdown()
//        Handler(Looper.getMainLooper()).postDelayed({
            val resID = resources.getIdentifier("shutter_sound", "raw", activity?.packageName)
            playMedia(context,resID)
        val imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .setTargetRotation(binding.viewFinder.display.rotation)
            .setTargetResolution(Size(640, 480)) // Set target resolution for portrait mode
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

                    // Adjust rotation if necessary
                    adjustImageRotation(photoFile)

                    // Navigate to ResultActivity with the captured image path
                    navigateToResultActivity(photoFile.absolutePath, System.currentTimeMillis())
                }

                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                }
            }

        )
//        }, 1000)
    }

    private fun adjustImageRotation(photoFile: File) {
        try {
            val bitmap = BitmapFactory.decodeFile(photoFile.path)
            val matrix = Matrix()


            // Adjust the rotation to ensure portrait mode
            if (isFrontCamera) {
                matrix.postRotate(270f) // Rotate by 90 degrees if necessary?
            } else {
                matrix.postRotate(90f)
            }
            val rotatedBitmap = Bitmap.createBitmap(
                bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
            )

            // Save the rotated bitmap back to the file
            FileOutputStream(photoFile).use { out ->
                rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
            }

            // Recycle the bitmaps to free memory
            bitmap.recycle()
            rotatedBitmap.recycle()
        } catch (e: Exception) {
            Log.e(TAG, "Error adjusting image rotation", e)
        }
    }

    private fun getOutputDirectory(): File {
        val mediaDir = requireContext().externalMediaDirs.firstOrNull()?.let {
            File(it, resources.getString(R.string.app_name)).apply { mkdirs() }
        }
        return if (mediaDir != null && mediaDir.exists()) mediaDir else requireContext().filesDir
    }

    private fun navigateToResultActivity(imagePath: String, timestamp: Long) {
        Handler(Looper.getMainLooper()).postDelayed({
            // Get the FragmentManager
            val fragmentManager = requireActivity().supportFragmentManager
            // Pop the current fragment from the back stack
            fragmentManager.popBackStack()
            val intent = Intent(requireContext(), ResultActivity::class.java).apply {
                putExtra("imagePath", imagePath)
                putExtra("timestamp", timestamp)
            }
            startActivity(intent)
        }, 100) // 3000 milliseconds delay (5 seconds)
    }


}
