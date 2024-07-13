package com.example.japanese

import android.content.Intent
import android.graphics.RenderEffect
import android.graphics.Shader
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.Log
import android.widget.Button
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.japanese.chat.ChatActivity
import com.example.japanese.lesson.EditLessonActivity
import com.example.japanese.lesson.Lesson
import com.example.japanese.lesson.LessonsAdapter
import com.example.japanese.minnaNoNihongo.ChaptersActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class MainActivity : BaseActivity() {

    private val auth = Firebase.auth
    private val db = Firebase.firestore

    override fun onUserLoggedIn() {
        setContentView(R.layout.activity_main)

        val logoutButton = findViewById<Button>(R.id.logOutButton)
        logoutButton.setOnClickListener {
            auth.signOut()
            //finish()
        }

        val chatButton = findViewById<Button>(R.id.chatButton)
        chatButton.setOnClickListener {
            val intent = Intent(this, ChatActivity::class.java)
            startActivity(intent)
        }

        val minnaNoNihongoButton = findViewById<Button>(R.id.minnaNoNihongoButton)
        minnaNoNihongoButton.setOnClickListener {
            val intent = Intent(this, ChaptersActivity::class.java)
            startActivity(intent)
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