package com.example.japanese

import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class MainActivity : BaseActivity() {

    private val GROQ_API_KEY = "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"

    override fun onUserLoggedIn() {
        setContentView(R.layout.activity_main)

        val url = "https://api.groq.com/openai/v1/chat/completions"
        val userPrompt = "What is your favorite color ?"

        val jsonBody = JSONObject()
        jsonBody.put("model", "llama3-70b-8192")
        val messagesArray = JSONArray()
        val userMessage = JSONObject()
        userMessage.put("role", "user")
        userMessage.put("content", userPrompt)

        messagesArray.put(userMessage)

        jsonBody.put("messages", messagesArray)

        val requestBody = jsonBody.toString().toRequestBody("application/json".toMediaTypeOrNull())
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $GROQ_API_KEY")
            .post(requestBody)
            .build()

        val okHttpClient = OkHttpClient()

        okHttpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                // Handle network errors
                runOnUiThread {
                    Log.e("API Error", "Error: ${e.message}")
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body
                if (response.isSuccessful && responseBody != null){
                    // Handle successful response
                    val responseJson = JSONObject(responseBody.string())
                    val message = responseJson.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content")
                    // Handle API response and update UI
                    runOnUiThread {
                        Log.d("API Response", message)
                    }
                } else {
                    // Handle unsuccessful response
                    runOnUiThread {
                        Log.e("API Error", "Error: ${response.code}")
                    }
                    return
                }
            }
        })
    }
}