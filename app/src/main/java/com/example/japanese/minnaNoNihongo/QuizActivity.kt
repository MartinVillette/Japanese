package com.example.japanese.minnaNoNihongo

import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.japanese.R
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.digitalink.DigitalInkRecognition
import com.google.mlkit.vision.digitalink.DigitalInkRecognitionModel
import com.google.mlkit.vision.digitalink.DigitalInkRecognitionModelIdentifier
import com.google.mlkit.vision.digitalink.DigitalInkRecognizer
import com.google.mlkit.vision.digitalink.DigitalInkRecognizerOptions
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions

class QuizActivity : AppCompatActivity() {

    private lateinit var drawingView: DrawingView
    private lateinit var recognizer: DigitalInkRecognizer


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_quiz)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        drawingView = findViewById(R.id.drawing_view)
        findViewById<Button>(R.id.undo_button).setOnClickListener { drawingView.undo() }
        findViewById<Button>(R.id.clear_button).setOnClickListener { drawingView.clear() }
        findViewById<Button>(R.id.validate_button).setOnClickListener { recognizeCharacter() }


        val modelIdentifier = DigitalInkRecognitionModelIdentifier.fromLanguageTag("ja")

        if (modelIdentifier == null) {
            Toast.makeText(this, "Model not supported", Toast.LENGTH_SHORT).show()
            return
        }

        val model = DigitalInkRecognitionModel.builder(modelIdentifier).build()

        recognizer = DigitalInkRecognition.getClient(
            DigitalInkRecognizerOptions.builder(model).build()
        )
    }

    private fun recognizeCharacter() {
        val ink = drawingView.getInk()

        recognizer.recognize(ink)
            .addOnSuccessListener { result ->
                val recognizedText = result.candidates[0].text
                Toast.makeText(this, "Recognized: $recognizedText", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Recognition failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroy() {
        super.onDestroy()
        recognizer.close()
    }
}