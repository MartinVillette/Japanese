package com.example.japanese.chat

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.japanese.R
import com.example.japanese.authentication.User
import com.example.japanese.lesson.LanguageItem
import com.example.japanese.lesson.Lesson
import com.example.japanese.minnaNoNihongo.MinnaNoNihongoLesson
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import org.json.JSONArray
import org.json.JSONObject

class ChatActivity : AppCompatActivity(), ChatAi.AiCallback  {

    private var messages = ArrayList<ChatMessage>()
    private var languageItems = ArrayList<LanguageItem>()
    private lateinit var chatAdapter: ChatAdapter
    private val auth = Firebase.auth
    private val db = Firebase.firestore
    private val chatAI = ChatAi()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_chat)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val chatRecyclerView = findViewById<RecyclerView>(R.id.chatRecyclerView)
        chatRecyclerView.layoutManager = LinearLayoutManager(this)
        chatAdapter = ChatAdapter(messages)
        chatRecyclerView.adapter = chatAdapter

        auth.currentUser?.uid?.let { userId ->
            db.collection("users").document(userId).collection("lessons")
                .get()
                .addOnSuccessListener { querySnapshot ->
                    val lessons = querySnapshot.toObjects(Lesson::class.java)
                    for (lesson in lessons){
                        val lessonContent = lesson.content
                        languageItems.addAll(lessonContent)
                    }

                    callForAiResponse()
                }
        }

        auth.currentUser?.uid?.let { userId ->
            db.collection("users").document(userId)
                .get()
                .addOnSuccessListener { querySnapshot ->
                    val user = querySnapshot.toObject(User::class.java)
                    if (user != null){
                        val lessons = user.lessons
                        for (lesson in lessons){
                            val lessonContent = lesson.content
                            languageItems.addAll(lessonContent)
                        }
                        val minnaNoNihongo = user.minnaNoNihongo
                        for (chapter in 1..minnaNoNihongo) {
                            // Your code to be executed within the loop
                            db.collection("lessons").document(chapter.toString())
                                .get()
                                .addOnSuccessListener { minnaNoNihongoLesson ->
                                    val lesson = minnaNoNihongoLesson.toObject(MinnaNoNihongoLesson::class.java)
                                    if (lesson != null){
                                        languageItems.addAll(lesson.content)
                                    }
                                }
                        }
                        callForAiResponse()
                    }
                }
        }

        val sendMessageButton = findViewById<FloatingActionButton>(R.id.sendMessageButton)
        val messageEditText = findViewById<EditText>(R.id.messageEditText)

        sendMessageButton.setOnClickListener {
            val message = messageEditText.text.toString().trim()
            if (message.isNotEmpty()) {
                sendMessage(message)
            }
        }
    }

    override fun onSuccess(message: String) {
        runOnUiThread {
            Log.d("AiSuggestion", "Ai : $message")
            if (message.isNotEmpty()){
                val aiMessage = ChatMessage(false, message)
                messages.add(aiMessage)
                chatAdapter.notifyItemInserted(messages.size - 1)
                //chatAdapter.notifyDataSetChanged()
            }
        }
    }

    override fun onFailure(errorMessage: String) {
        runOnUiThread {}
    }

    private fun sendMessage(message: String){
        val messageEditText = findViewById<EditText>(R.id.messageEditText)
        val userMessage = ChatMessage(true, message)
        messages.add(userMessage)
        chatAdapter.notifyItemInserted(messages.size - 1)
        messageEditText.setText("")
        callForAiResponse(message)
    }

    private fun callForAiResponse(message: String=""){
        val items = JSONArray()
        for (languageItem in languageItems){
            val item = JSONObject()
            item.put("word", languageItem.word)
            item.put("expression", languageItem.expression)
            item.put("reading", languageItem.reading)
            items.put(item)
        }
        val context = JSONArray()
        for (contextMessage in messages){
            val item = JSONObject()
            item.put("role", if (contextMessage.isUser) "user" else "assistant")
            item.put("content", contextMessage.content)
            context.put(item)
        }
        chatAI.chat(message, items, context,this)
    }
}