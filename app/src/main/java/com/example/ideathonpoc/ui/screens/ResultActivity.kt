package com.example.ideathonpoc.ui.screens

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.ideathonpoc.R
import com.example.ideathonpoc.databinding.ActivityResultBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ResultActivity : AppCompatActivity() {

    private lateinit var binding: ActivityResultBinding
    private lateinit var textToSpeech: TextToSpeech
    private val handler = Handler(Looper.getMainLooper())


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResultBinding.inflate(layoutInflater)
        setContentView(binding.root)
        handler.postDelayed(Runnable {
            binding.resultOutputContainer.visibility = View.VISIBLE
            binding.successAnimationView.visibility = View.GONE
        }, 3000)

        textToSpeech = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = textToSpeech.setLanguage(Locale.UK)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e(TAG, "Language not supported")
                } else {
                    speakOut()
                }
            } else {
                Log.e(TAG, "Initialization failed")
            }
        }


        val imagePath = intent.getStringExtra("imagePath")
        val timestamp = intent.getLongExtra("timestamp", 0L)

        imagePath?.let { path ->
            val bitmap = BitmapFactory.decodeFile(path)
            val imageView = findViewById<ImageView>(R.id.screenshotImageView)
            imageView.setImageBitmap(bitmap)
//            imageView.rotation = 90f // Rotate the image by 90 degrees?

        }

        timestamp.let { time ->
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val formattedTime = sdf.format(Date(time))
            binding.timestampTextView.text = "Date and Time : $formattedTime"
        }
        Log.e(TAG, "onCreate: testing")

    }

    private fun speakOut() {
        val message = "You wore right PPE, you can proceed with your Job, Stay Vigilant, Stay Safe"
        textToSpeech.speak(message, TextToSpeech.QUEUE_FLUSH, null, null)

    }

    override fun onDestroy() {
        super.onDestroy()
        textToSpeech.stop()
        textToSpeech.shutdown()

    }

    override fun onBackPressed() {
        Toast.makeText(this, "Back pressed", Toast.LENGTH_SHORT).show()
        super.onBackPressed()
    }

    companion object {
        private const val TAG = "ResultActivity"
    }
}