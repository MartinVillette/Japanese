package com.example.japanese.lesson.ai

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

class QuizAi {
    interface EvaluationCallback {
        fun onSuccess(isCorrect: Boolean)
        fun onFailure(errorMessage: String)
    }

    private val url = "https://api.groq.com/openai/v1/chat/completions"
    private val AI_MODEL = "llama-3.1-405b-reasoning"
    private val GROQ_API_KEY = "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"

    private val AI_SYSTEM_EVALUATION = """                                                                                                                                                                                                                                                                                                                              
        You are an AI that helps detect if a user is making a translation mistake or if they know their vocabulary. You will be given an input and a languageItem. A languageItem is a JSON object that contains a Japanese expression, its reading, and its meaning. Your task is to determine if the input is a correct translation of the meaning in the languageItem. Specifically, you will compare the input to the expression and reading fields of the languageItem. Be aware that the input might have slight variations, such as missing or including parenthesis content.
        Consider the following when making your determination:
        - Ignore parenthesis in the expression and reading fields.
        - Treat optional readings or expressions within parenthesis as valid.
        - Ignore any spaces or punctuation when comparing the input.
        - Account for common variations in Japanese expressions.
        Return true if the input correctly matches the expression or the reading, disregarding the parenthesis and minor variations. Return false if the input contains mistakes, is empty or does not sufficiently match.
        
        INPUT :{
                  "input": "INPUT_TEXT",
                  "languageItem": {
                    "expression": "EXPRESSION_IN_JAPANESE",
                    "reading": "READING_IN_JAPANESE",
                    "meaning": "MEANING_IN_THE_USER_LANGUAGE"
                  }
                }
    OUTPUT : true/false
    """.trimIndent()

    fun isTranslationCorrect(input:String, languageItem: JSONObject, callback: EvaluationCallback ) {
        val prompt = """{
            "input" = "${input.trim()}",
            "languageItem" = $languageItem}""".trimIndent()

        Log.d("Evaluation", "Prompt : $prompt")

        val jsonBody = JSONObject()
        jsonBody.put("model", AI_MODEL)
        val messagesArray = JSONArray()

        val systemMessage = JSONObject()
        systemMessage.put("role", "system")
        systemMessage.put("content", AI_SYSTEM_EVALUATION)
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
                    val message = responseJson.getJSONArray("choices")
                        .getJSONObject(0)
                        .getJSONObject("message")
                        .getString("content").lowercase()
                    Log.d("Evaluation", "Evaluation : $message")
                    try {
                        if ("true" in message) {
                            callback.onSuccess(true)
                        } else if ("false" in message) {
                            callback.onSuccess(false)
                        } else {
                            callback.onFailure("Error in response")
                        }
                    } catch (e: Exception) {
                        callback.onFailure(e.toString())
                    }
                }
            }
        })
    }
}