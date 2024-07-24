package com.example.japanese.lesson.minnaNoNihongoLesson

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
import com.example.japanese.lesson.userLesson.LanguageItem
import com.example.japanese.lesson.userLesson.LessonContentAdapter
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore

class ChapterActivity : AppCompatActivity() {

    private val db = Firebase.firestore
    private val auth = Firebase.auth
    private val userId:String = auth.currentUser!!.uid
    private var languageItemList = arrayListOf<LanguageItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_chapter)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val chapter = intent.getStringExtra("chapter")?:""
        if (chapter == ""){
            finish()
        }

        val lessonNameTextView = findViewById<TextView>(R.id.lessonNameTextView)

        val lessonContentRecyclerView = findViewById<RecyclerView>(R.id.lessonContentRecyclerView)
        lessonContentRecyclerView.layoutManager = LinearLayoutManager(this)

        val lessonContentAdapter = LessonContentAdapter(languageItemList)
        lessonContentRecyclerView.adapter = lessonContentAdapter


        val quizButton = findViewById<Button>(R.id.quizButton)
        quizButton.setOnClickListener {
            val intent = Intent(this, QuizActivity::class.java)
            intent.putExtra("chapter", chapter)
            startActivity(intent)
        }

        val backButton = findViewById<ImageButton>(R.id.backButton)
        backButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        db.collection("users").document(userId).collection("profiles").document(chapter)
            .get()
            .addOnSuccessListener { documentSnapshot ->
                var profile = documentSnapshot.toObject(Profile::class.java)
                if (profile == null){
                    db.collection("lessons").document(chapter)
                        .get()
                        .addOnSuccessListener { querySnapshot ->
                            val lesson = querySnapshot.toObject(MinnaNoNihongoLesson::class.java)
                            lesson?.let { l ->
                                val profileRef = db.collection("users").document(userId).collection("profiles").document(chapter)
                                val newProfile = Profile(profileRef.id, chapter, l.content)
                                profileRef.set(newProfile)
                                profile = newProfile
                            }
                        }

                }

                profile?.let{ p ->
                    lessonNameTextView.text = "Chapter ${p.chapter}"
                    languageItemList.addAll(p.content)
                    lessonContentAdapter.notifyDataSetChanged()
                }
            }
    }
}