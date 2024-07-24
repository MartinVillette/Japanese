package com.example.japanese.lesson

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.japanese.MainActivity
import com.example.japanese.R
import com.example.japanese.authentication.User
import com.example.japanese.lesson.userLesson.LanguageItem
import com.example.japanese.lesson.userLesson.Lesson
import com.example.japanese.lesson.userLesson.LessonActivity
import com.example.japanese.lesson.ai.QuizAi
import com.example.japanese.lesson.minnaNoNihongoLesson.ChapterActivity
import com.example.japanese.lesson.minnaNoNihongoLesson.Profile
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.vision.digitalink.DigitalInkRecognition
import com.google.mlkit.vision.digitalink.DigitalInkRecognitionModel
import com.google.mlkit.vision.digitalink.DigitalInkRecognitionModelIdentifier
import com.google.mlkit.vision.digitalink.DigitalInkRecognizer
import com.google.mlkit.vision.digitalink.DigitalInkRecognizerOptions
import org.json.JSONObject

class QuizActivity : AppCompatActivity(), QuizAi.EvaluationCallback  {

    private val db  = Firebase.firestore
    private val auth = Firebase.auth
    private val userId = auth.currentUser!!.uid
    private var languageItemList = arrayListOf<LanguageItem>()
    private lateinit var currentLanguageItem: LanguageItem
    private var currentIndex: Int = 0
    private val quizAi = QuizAi()
    private lateinit var title: String
    private lateinit var drawingView:DrawingView
    private lateinit var correctionLayout:LinearLayout
    private lateinit var nextButton: Button
    private lateinit var recognizer: DigitalInkRecognizer
    private lateinit var model: DigitalInkRecognitionModel
    private lateinit var inputEditText:EditText
    private val remoteModelManager: RemoteModelManager = RemoteModelManager.getInstance()

    private val WAITING_TIME_MS = 1000L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_quiz)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val lessonNameTextView = findViewById<TextView>(R.id.lessonNameTextView)

        val chapter = intent.getStringExtra("chapter")?:""
        val lessonId = intent.getStringExtra("lessonId")?:""
        if (chapter == "" && lessonId == ""){
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        } else if (chapter != ""){
            //Lesson then
            db.collection("users").document(userId).collection("profiles").document(chapter)
                .get()
                .addOnSuccessListener { documentSnapshot ->
                    val profile = documentSnapshot.toObject(Profile::class.java)!!
                    languageItemList.addAll(profile.content)
                    title = "Chapter ${profile.chapter}"
                    lessonNameTextView.text = title
                    languageItemList.shuffle()
                    newQuiz()
                }
        } else if (lessonId != ""){
            //Chapter then
            db.collection("users").document(userId).collection("lessons").document(lessonId)
                .get()
                .addOnSuccessListener { documentSnapshot ->
                    val lesson = documentSnapshot.toObject(Lesson::class.java)!!
                    title = lesson.name
                    lessonNameTextView.text = title
                    languageItemList.addAll(lesson.content)
                    languageItemList.shuffle()
                    newQuiz()
                }
        }

        drawingView = findViewById(R.id.drawingView)
        correctionLayout = findViewById(R.id.correctionLayout)
        findViewById<FloatingActionButton>(R.id.undoButton).setOnClickListener { undo() }
        findViewById<FloatingActionButton>(R.id.clearButton).setOnClickListener { drawingView.clear() }
        findViewById<FloatingActionButton>(R.id.validateButton).setOnClickListener { recognizeCharacter() }
        findViewById<FloatingActionButton>(R.id.submitButton).setOnClickListener { submitQuiz() }
        inputEditText = findViewById(R.id.inputEditText)
        nextButton = findViewById(R.id.nextButton)
        nextButton.setOnClickListener {
            drawingView.clear()
            drawingView.blockTouch(false) // Unblock touch events
            drawingView.visibility = View.VISIBLE
            correctionLayout.visibility = View.GONE

            currentIndex++
            if (currentIndex < languageItemList.size) {
                newQuiz()
            }
        }

        val modelIdentifier = DigitalInkRecognitionModelIdentifier.fromLanguageTag("ja")

        if (modelIdentifier == null) {
            Toast.makeText(this, "Model not supported", Toast.LENGTH_SHORT).show()
            return
        }

        model = DigitalInkRecognitionModel.builder(modelIdentifier).build()

        downloadModelIfNeeded{success ->
            if (success) {
                setupRecognizer(model)
            } else {
                Toast.makeText(this, "Model download failed", Toast.LENGTH_SHORT).show()
            }
        }

        recognizer = DigitalInkRecognition.getClient(
            DigitalInkRecognizerOptions.builder(model).build()
        )

        val backButton = findViewById<ImageButton>(R.id.backButton)
        backButton.setOnClickListener {
            finish()
        }
    }

    override fun onSuccess(isCorrect: Boolean) {
        runOnUiThread {
            drawingView.clear()
            drawingView.blockTouch(true) // Block touch events

            if (isCorrect) {
                drawingView.drawCheckSign()
                Handler(Looper.getMainLooper()).postDelayed({
                    drawingView.clear()
                    drawingView.blockTouch(false) // Unblock touch events

                    currentIndex++
                    if (currentIndex < languageItemList.size) {
                        newQuiz()
                    }
                }, WAITING_TIME_MS)
            } else {
                drawingView.drawCrossSign()
                Handler(Looper.getMainLooper()).postDelayed({
                    drawingView.visibility = View.GONE
                    correctionLayout.visibility = View.VISIBLE
                }, WAITING_TIME_MS)
            }
        }
    }

    override fun onFailure(errorMessage: String) {
        runOnUiThread{
            Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
        }
    }

    private fun undo() {
        if (drawingView.getPaths().isEmpty()){
            removeCharacterBeforeCursor()
        } else {
            drawingView.undo()
        }
    }

    private fun removeCharacterBeforeCursor(){
        val cursorPosition = inputEditText.selectionStart
        val text = inputEditText.text.toString()

        if (cursorPosition > 0 && text.isNotEmpty()) {
            val newText = StringBuilder(text)
                .deleteCharAt(cursorPosition - 1)
                .toString()

            inputEditText.setText(newText)
            inputEditText.setSelection(cursorPosition - 1)
        }
    }

    private fun newQuiz(){
        currentLanguageItem = languageItemList[currentIndex]
        findViewById<TextView>(R.id.meaningTextView).text = currentLanguageItem.meaning
        findViewById<TextView>(R.id.expressionTextView).text = currentLanguageItem.expression
        findViewById<TextView>(R.id.readingTextView).text = currentLanguageItem.reading
        inputEditText.setText("")
    }

    private fun submitQuiz(){
        val text = inputEditText.text.toString().trim()
        val jsonLanguageItem = JSONObject()
        jsonLanguageItem.put("expression", currentLanguageItem.expression)
        jsonLanguageItem.put("reading", currentLanguageItem.reading)
        jsonLanguageItem.put("meaning", currentLanguageItem.meaning)
        quizAi.isTranslationCorrect(text, jsonLanguageItem, this)
    }

    private fun downloadModelIfNeeded(onComplete: (Boolean) -> Unit) {
        remoteModelManager.getDownloadedModels(DigitalInkRecognitionModel::class.java)
            .addOnSuccessListener { downloadedModels ->
                if (downloadedModels.contains(model)) {
                    onComplete(true)
                } else {
                    val conditions = DownloadConditions.Builder()
                        .requireWifi()
                        .build()

                    remoteModelManager.download(model, conditions)
                        .addOnSuccessListener {
                            onComplete(true)
                        }
                        .addOnFailureListener {
                            Log.e("QuizActivity", "Model download failed", it)
                            onComplete(false)
                        }
                }
            }
            .addOnFailureListener {
                Log.e("QuizActivity", "Model download check failed", it)
                onComplete(false)
            }
    }

    private fun setupRecognizer(model: DigitalInkRecognitionModel) {
        recognizer = DigitalInkRecognition.getClient(
            DigitalInkRecognizerOptions.builder(model).build()
        )
    }

    private fun recognizeCharacter() {
        val ink = drawingView.getInk()

        recognizer.recognize(ink)
            .addOnSuccessListener { result ->
                val recognizedText = result.candidates[0].text
                insertStringAtCursor(inputEditText, recognizedText)
                drawingView.clear()
            }
            .addOnFailureListener { e ->
                Log.e("QuizActivity", "Recognition failed", e)
            }
    }

    private fun insertStringAtCursor(editText: EditText, text: String) {
        val editable = editText.editableText
        val start = editText.selectionStart
        editable.insert(start, text)
    }

    override fun onDestroy() {
        super.onDestroy()
        recognizer.close()
    }

}