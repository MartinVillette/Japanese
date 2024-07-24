package com.example.japanese.lesson.userLesson

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.japanese.BaseActivity
import com.example.japanese.R
import com.example.japanese.authentication.SignInActivity
import com.example.japanese.chat.ChatActivity
import com.example.japanese.lesson.minnaNoNihongoLesson.ChaptersActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore

class LessonsActivity : BaseActivity() {
    private val auth = Firebase.auth
    private val db = Firebase.firestore

    override fun onUserLoggedIn() {
        enableEdgeToEdge()
        setContentView(R.layout.activity_lessons)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        findViewById<ImageButton>(R.id.backButton).setOnClickListener {
            finish()
        }

        val newLesson = findViewById<FloatingActionButton>(R.id.newLessonButton)
        newLesson.setOnClickListener {
            val intent = Intent(this, EditLessonActivity::class.java)
            startActivity(intent)
        }

        updateLessonList()
    }

    private fun updateLessonList(){
        val lessonsRecyclerView = findViewById<RecyclerView>(R.id.lessonsRecyclerView)
        lessonsRecyclerView.layoutManager = LinearLayoutManager(this)

        val lessonList = ArrayList<Lesson>()
        val lessonsAdapter = LessonsAdapter(lessonList)
        lessonsRecyclerView.adapter = lessonsAdapter
        auth.currentUser?.uid?.let { userId ->
            db.collection("users").document(userId).collection("lessons")
                .get()
                .addOnSuccessListener { querySnapshot ->
                    val lessons = querySnapshot.toObjects(Lesson::class.java)
                    lessonList.addAll(lessons)
                    lessonsAdapter.notifyDataSetChanged()
                }
        }
    }
}