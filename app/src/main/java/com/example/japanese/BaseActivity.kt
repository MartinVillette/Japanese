package com.example.japanese

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.Toolbar
import androidx.appcompat.app.AppCompatActivity
import com.example.japanese.authentication.SignInActivity
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

abstract class BaseActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_base)
        auth = Firebase.auth
    }

    override fun onStart() {
        super.onStart()
        val currentUser = auth.currentUser
        if (currentUser == null) {
            // User is not logged in, redirect to LoginActivity
            val intent = Intent(this, SignInActivity::class.java)
            startActivity(intent)
            finish() // Optional: Close current activity to prevent going back
        } else {
            // User is logged in, proceed with normal activity setup
            onUserLoggedIn()
        }
    }

    abstract fun onUserLoggedIn() // Method for child activities to implement
}
