package com.example.japanese

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

abstract class BaseActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
