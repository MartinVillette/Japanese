package com.example.japanese.lesson.userLesson

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.japanese.MainActivity
import com.example.japanese.R
import com.example.japanese.lesson.QuizActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore

class LessonActivity : AppCompatActivity() {

    private val db = Firebase.firestore
    private val auth = Firebase.auth
    private val userId:String = auth.currentUser!!.uid
    private var languageItemList = arrayListOf<LanguageItem>()

    private val REQUEST_CODE = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_lesson)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val lessonId = intent.getStringExtra("lessonId")?:""
        if (lessonId == ""){
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }

        val lessonNameTextView = findViewById<TextView>(R.id.lessonNameTextView)

        val lessonContentRecyclerView = findViewById<RecyclerView>(R.id.lessonContentRecyclerView)
        lessonContentRecyclerView.layoutManager = LinearLayoutManager(this)

        val lessonContentAdapter = LessonContentAdapter(languageItemList)
        lessonContentRecyclerView.adapter = lessonContentAdapter

        val backButton = findViewById<ImageButton>(R.id.backButton)
        backButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        val quizButton = findViewById<Button>(R.id.quizButton)
        quizButton.setOnClickListener {
            val intent = Intent(this, QuizActivity::class.java)
            intent.putExtra("lessonId", lessonId)
            startActivity(intent)
        }

        val editLessonFAB = findViewById<FloatingActionButton>(R.id.editLessonFAB)
        editLessonFAB.setOnClickListener {
            val intent = Intent(this, EditLessonActivity::class.java)
            intent.putExtra("lessonId", lessonId)
            startActivityForResult(intent, REQUEST_CODE)
        }


        db.collection("users").document(userId).collection("lessons").document(lessonId)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val lesson = querySnapshot.toObject(Lesson::class.java)
                if (lesson == null){
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                }

                lesson?.let{ l ->
                    lessonNameTextView.text = l.name
                    languageItemList.addAll(l.content)
                    lessonContentAdapter.notifyDataSetChanged()
                }
            }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            finish() // Finish PreviousActivity
        }
    }
}