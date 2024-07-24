package com.example.japanese.chat

import android.util.Log
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class ChatAi {
    interface AiCallback {
        fun onSuccess(message:String)
        fun onFailure(errorMessage: String)
    }

    private val url = "https://api.groq.com/openai/v1/chat/completions"
    private val AI_MODEL = "llama-3.1-8b-instant"
    private val GROQ_API_KEY = "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"

    private val AI_SYSTEM_CHAT = """
        You are a chatbot AI that communicates with the user in Japanese. The goal is to help the user practice and become familiar with the vocabulary they are learning. Follow these guidelines:
        1. Use only the Japanese expressions provided in the "language_tools" in the input.
        2. Speak as much as possible in Japanese using the user's vocabulary.
        3. If the user asks for explanations in their language, provide them in the correct language.
        4. If you need to use words not in the lessons, provide the reading of the Japanese word as well as its translation in French in parentheses immediately after the new word.
        5. Ensure that your responses are not empty.
        
        You will also be given previous messages to keep track of the conversation and know who said what. If there are no previous messages, you should start the conversation.
        
        INPUT: {"language_tools":"THE_LANGUAGE_TOOLS_IN_JSON_FORMAT", "previous_messages":"THE_PREVIOUS_MESSAGES"} 
        OUTPUT: {"message":"YOUR_MESSAGE"}

    """.trimIndent()

    fun chat(message: String, languageItem:JSONArray, context: JSONArray, callback: AiCallback) {
        val prompt = """{"languages_tools" = $languageItem, "previous_messages" = $context"""

        Log.d("AiSuggestion", "Prompt : $prompt")
        val jsonBody = JSONObject()
        jsonBody.put("model", AI_MODEL)
        val messagesArray = JSONArray()

        val systemMessage = JSONObject()
        systemMessage.put("role", "system")
        systemMessage.put("content", AI_SYSTEM_CHAT)
        messagesArray.put(systemMessage)

        val userMessage = JSONObject()
        userMessage.put("role", "user")
        userMessage.put("content", prompt)
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
            override fun onFailure(call: Call, e: IOException) {}

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body
                if (response.isSuccessful && responseBody != null) {
                    // Handle successful response
                    val responseJson = JSONObject(responseBody.string())
                    var responseMessage = responseJson.getJSONArray("choices")
                        .getJSONObject(0)
                        .getJSONObject("message")
                        .getString("content")
                    // Use the callback to return the result
                    val start = responseMessage.indexOf('{')
                    val end = responseMessage.lastIndexOf('}')
                    try {
                        val messageObject = JSONObject(responseMessage.substring(start, end + 1))
                        val aiMessage = messageObject.getString("message").trim()
                        if (aiMessage.isEmpty()){
                            chat(message, languageItem, context, callback)
                        } else {
                            callback.onSuccess(aiMessage)
                        }
                    } catch (e: Exception) {
                       //chat(message, languageItem, context, callback)
                    }
                }
            }
        })
    }
}