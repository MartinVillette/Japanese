package com.example.japanese.lesson.userLesson

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.japanese.R
import com.example.japanese.lesson.ai.SuggestionAi
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject


class EditLessonActivity : AppCompatActivity(), SuggestionAi.SuggestionCallback  {

    private lateinit var languageItemsContainer: LinearLayout
    private lateinit var suggestionsLayout: LinearLayout
    private var languageItemsList = arrayListOf<LanguageItem>()
    private lateinit var lessonId: String
    private lateinit var  suggestionLayout: LinearLayout
    private val db = Firebase.firestore
    private val auth = Firebase.auth
    private val userId = auth.currentUser!!.uid
    private val suggestionAi = SuggestionAi()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_edit_lesson)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }


        languageItemsContainer = findViewById(R.id.languageItemsContainer)
        val newItemFAB = findViewById<FloatingActionButton>(R.id.newItemButton)
        newItemFAB.setOnClickListener {
            addLanguageItemClick()
        }

        val backButton = findViewById<ImageButton>(R.id.backButton)
        backButton.setOnClickListener {
            finish()
        }

        val saveLessonButton = findViewById<Button>(R.id.saveLessonButton)
        saveLessonButton.setOnClickListener {
            saveLesson()
        }

        lessonId = intent.getStringExtra("lessonId")?:""
        if (lessonId == ""){
            newLesson()
        } else {
            loadLesson()
        }

        suggestionLayout = findViewById(R.id.suggestionLayout)
        val suggestionWord = findViewById<TextView>(R.id.suggestionWordTextView)
        val suggestionExpression = findViewById<TextView>(R.id.suggestionExpressionTextView)
        val suggestionReading = findViewById<TextView>(R.id.suggestionReadingTextView)
        suggestionLayout.setOnClickListener{
            addLanguageItem(suggestionWord.text.toString(), suggestionExpression.text.toString(), suggestionReading.text.toString())
            callForNewWordSuggestion()
        }

        val refreshSuggestionButton = findViewById<ImageButton>(R.id.refreshSuggestionButton)
        refreshSuggestionButton.setOnClickListener{
            callForNewWordSuggestion()
        }
    }

    override fun onSuccess(suggestions: JSONArray, expressionEditText: EditText,  readingEditText: EditText) {
        // Handle the successful response here
        runOnUiThread {
            // Update UI with the suggestion

            for (i in 0 until suggestions.length()) {
                val suggestion: JSONObject = suggestions.getJSONObject(i)
                val expression: String = suggestion.getString("expression")
                val reading: String = suggestion.getString("reading")

                Log.d("AiSuggestion", "Expression: $expression, Reading: $reading")
                val button = Button(this)
                button.text = expression

                // Set an OnClickListener for the button
                button.setOnClickListener {
                    expressionEditText.setText(expression)
                    readingEditText.setText(reading)
                }
                suggestionsLayout.addView(button)
            }
        }
    }

    override fun onFailure(errorMessage: String) {
        // Handle the error response here
        runOnUiThread {}
    }

    override fun onSuccessNewWord(word: String, expression: String, reading: String) {
        runOnUiThread {
            // Update UI with the suggestion
            val wordTextView = findViewById<TextView>(R.id.suggestionWordTextView)
            val expressionTextView = findViewById<TextView>(R.id.suggestionExpressionTextView)
            val readingTextView = findViewById<TextView>(R.id.suggestionReadingTextView)
            wordTextView.text = word
            expressionTextView.text = expression
            readingTextView.text = reading
        }
    }

    private fun newLesson(){
        val newLessonRef = db.collection("users").document(userId).collection("lessons").document()
        lessonId = newLessonRef.id
    }

    private fun loadLesson(){
        db.collection("users").document(userId).collection("lessons").document(lessonId)
            .get()
            .addOnSuccessListener {
                val lesson = it.toObject(Lesson::class.java)
                if (lesson == null){
                    newLesson()
                }
                lesson?.let{ l ->
                    val lessonNameEditText = findViewById<EditText>(R.id.lessonNameEditText)
                    lessonNameEditText.setText(l.name)
                    for (languageItem in l.content){
                        addLanguageItem(languageItem.meaning, languageItem.expression, languageItem.reading)
                    }
                    callForNewWordSuggestion()
                }
            }
    }

    private fun showAddItemPopup(languageItemView:View, languageItem: LanguageItem) {
        // Inflate the popup layout
        val inflater = layoutInflater
        val popupView = inflater.inflate(R.layout.popup_add_language_item, null)

        // Create the AlertDialog
        val dialogBuilder = AlertDialog.Builder(this)
            .setView(popupView)

        // Find the views in the inflated layout
        val wordEditText = popupView.findViewById<EditText>(R.id.wordEditText)
        val expressionEditText = popupView.findViewById<EditText>(R.id.expressionEditText)
        val readingEditText = popupView.findViewById<EditText>(R.id.readingEditText)
        val saveButton = popupView.findViewById<Button>(R.id.saveLanguageItemButton)
        suggestionsLayout = popupView.findViewById(R.id.suggestionsLinearLayout)

        wordEditText.setText(languageItem.meaning)
        expressionEditText.setText(languageItem.expression)
        readingEditText.setText(languageItem.reading)

        val dialog = dialogBuilder.create()

        // Set the save button click listener
        saveButton.setOnClickListener {
            val newWord = wordEditText.text.toString()
            val newExpression = expressionEditText.text.toString()
            val newReading = readingEditText.text.toString()

            languageItemView.findViewById<TextView>(R.id.wordTextView).text = newWord
            languageItemView.findViewById<TextView>(R.id.expressionTextView).text = newExpression
            languageItem.meaning = newWord
            languageItem.expression = newExpression
            languageItem.reading = newReading

            dialog.dismiss()
        }

        wordEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val newWord = s.toString()
                languageItem.meaning = newWord
                callForSuggestion(newWord, expressionEditText, readingEditText)
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
        expressionEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                languageItem.expression = s.toString()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
        readingEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                languageItem.reading = s.toString()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // Show the AlertDialog
        dialog.show()
    }

    private fun addLanguageItemClick(word:String="", expression:String="", reading:String="") {

        val (languageItemView, languageItem) = addLanguageItem(word, expression, reading)
        showAddItemPopup(languageItemView, languageItem)
        val wordTextView = languageItemView.findViewById<TextView>(R.id.wordTextView)
        wordTextView.requestFocus()
        wordTextView.postDelayed({
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(wordTextView, InputMethodManager.SHOW_IMPLICIT)
        }, 200)
    }

    private fun addLanguageItem(word:String="", expression:String="", reading:String=""): Pair<View, LanguageItem> {
        val inflater = LayoutInflater.from(this)
        val languageItemView = inflater.inflate(R.layout.item_new_language_item, languageItemsContainer, false)

        val wordTextView = languageItemView.findViewById<TextView>(R.id.wordTextView)
        val expressionTextView = languageItemView.findViewById<TextView>(R.id.expressionTextView)
        wordTextView.text = word
        expressionTextView.text = expression

        val languageItem = LanguageItem(word, expression, reading)
        languageItemsList.add(languageItem)

        val deleteButton = languageItemView.findViewById<ImageButton>(R.id.deleteButton)
        deleteButton.setOnClickListener {
            removeLanguageItem(languageItem, languageItemView)
        }

        languageItemsContainer.addView(languageItemView)
        languageItemView.setOnClickListener {
            showAddItemPopup(languageItemView, languageItem)
        }

        return Pair(languageItemView, languageItem)

        /*
        editTextWord.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val newWord = s.toString()
                languageItem.word = newWord
                callForSuggestion(newWord, editTextTranslation)
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
        editTextTranslation.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                languageItem.translation = s.toString()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
        */

        //return wordTextView
    }

    private fun removeLanguageItem(languageItem: LanguageItem, languageItemView: View) {
        languageItemsList.remove(languageItem)
        languageItemsContainer.removeView(languageItemView)
    }

    private fun saveLesson() {

        refactorLanguageItemsList()
        val lessonName = findViewById<EditText>(R.id.lessonNameEditText).text.toString()
        if (lessonName.isNotEmpty() && isLanguageItemsListValid()){

            val lessonRef = db.collection("users").document(userId).collection("lessons").document(lessonId)

            val lesson = Lesson(
                id = lessonId,
                userId = userId,
                name = lessonName,
                content = languageItemsList
            )
            Log.e("save","isSaving")
            lessonRef.set(lesson)
                .addOnSuccessListener {
                    Log.e("save","saved")
                    // Handle success
                    val intent = Intent(this, LessonActivity::class.java)
                    intent.putExtra("lessonId", lessonRef.id)
                    startActivity(intent)
                    setResult(Activity.RESULT_OK)
                    finish()
                }
                .addOnFailureListener { e ->
                    // Handle failure
                    Log.w("SaveLesson", "Error saving lesson", e)
                }
        }
    }

    private fun refactorLanguageItemsList(){
        val tempList = ArrayList<LanguageItem>()

        for (languageItem in languageItemsList){
            if (!(languageItem.meaning.isEmpty() && languageItem.expression.isEmpty())){
                languageItem.meaning = languageItem.meaning.trim()
                languageItem.expression = languageItem.expression.trim()
                languageItem.reading = languageItem.reading.trim()
                tempList.add(languageItem)
            }
        }
        languageItemsList = tempList
    }

    private fun isLanguageItemsListValid(): Boolean {
        for (languageItem in languageItemsList) {
            if (languageItem.meaning.isEmpty() || languageItem.expression.isEmpty()){
                return false
            }
        }
        return true
    }

    private fun callForNewWordSuggestion(){
        val context = JSONArray()
        for (languageItem in languageItemsList){
            val jsonObject = JSONObject()
            jsonObject.put("meaning", languageItem.meaning)
            jsonObject.put("expression", languageItem.expression)
            jsonObject.put("reading", languageItem.reading)
            context.put(jsonObject)
        }

        if (context.length() == 0){
            suggestionLayout.visibility = View.GONE
        } else {
            suggestionLayout.visibility = View.VISIBLE
        }
        val lessonNameEditText = findViewById<EditText>(R.id.lessonNameEditText)
        val title = lessonNameEditText.text.toString().trim()
        suggestionAi.getNewWordSuggestion(title, context, this)
    }

    private fun callForSuggestion(word: String, expressionEditText: EditText, readingEditText: EditText) {
        val context = JSONArray()
        for (languageItem in languageItemsList){
            if (languageItem.meaning != word){
                val jsonObject = JSONObject()
                jsonObject.put("meaning", languageItem.meaning)
                jsonObject.put("expression", languageItem.expression)
                jsonObject.put("reading", languageItem.reading)
                context.put(jsonObject)
            }
        }
        val lessonNameEditText = findViewById<EditText>(R.id.lessonNameEditText)
        val title = lessonNameEditText.text.toString().trim()
        suggestionAi.getWordSuggestion(word, title, context, expressionEditText, readingEditText, this)
    }
}