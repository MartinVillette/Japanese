package com.example.japanese

import android.content.Intent
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.japanese.chat.ChatActivity
import com.example.japanese.lesson.userLesson.EditLessonActivity
import com.example.japanese.lesson.userLesson.Lesson
import com.example.japanese.lesson.userLesson.LessonsAdapter
import com.example.japanese.authentication.SignInActivity
import com.example.japanese.lesson.minnaNoNihongoLesson.ChaptersActivity
import com.example.japanese.lesson.userLesson.LessonsActivity
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore

class MainActivity : BaseActivity() {

    private val auth = Firebase.auth
    private val db = Firebase.firestore

    override fun onUserLoggedIn() {
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val logoutButton = findViewById<Button>(R.id.logOutButton)
        logoutButton.setOnClickListener {
            auth.signOut()
            val intent = Intent(this, SignInActivity::class.java)
            startActivity(intent)
            finish()
        }

        val lessonsButton = findViewById<ExtendedFloatingActionButton>(R.id.lessonsButton)
        lessonsButton.setOnClickListener {
            val intent = Intent(this, LessonsActivity::class.java)
            startActivity(intent)
        }

        val chatButton = findViewById<ExtendedFloatingActionButton>(R.id.chatButton)
        chatButton.setOnClickListener {
            val intent = Intent(this, ChatActivity::class.java)
            startActivity(intent)
        }

        val minnaNoNihongoButton = findViewById<ExtendedFloatingActionButton>(R.id.minnaNoNihongoButton)
        minnaNoNihongoButton.setOnClickListener {
            val intent = Intent(this, ChaptersActivity::class.java)
            startActivity(intent)
        }

    }
}