package com.example.japanese.minnaNoNihongo

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.japanese.R
import com.example.japanese.authentication.User
import com.example.japanese.lesson.ChapterAdapter
import com.example.japanese.lesson.Lesson
import com.example.japanese.lesson.LessonsAdapter
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore

class ChaptersActivity : AppCompatActivity() {

    private val db = Firebase.firestore
    private val auth = Firebase.auth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_chapters)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val chaptersRecyclerView = findViewById<RecyclerView>(R.id.chaptersRecyclerView)
        chaptersRecyclerView.layoutManager = LinearLayoutManager(this)

        val chaptersList = ArrayList<Int>()
        val chaptersAdapter = ChapterAdapter(chaptersList)
        chaptersRecyclerView.adapter = chaptersAdapter

        auth.currentUser?.uid?.let { userId ->
            db.collection("users").document(userId)
                .get()
                .addOnSuccessListener { documentSnapshot ->
                    val user = documentSnapshot.toObject(User::class.java)
                    val lastChapter = user?.minnaNoNihongo ?: 1
                    for (chapter in 1..lastChapter) {
                        // Your code to be executed within the loop
                        chaptersList.add(chapter)
                    }
                    chaptersAdapter.notifyDataSetChanged()
                }
        }
    }
}