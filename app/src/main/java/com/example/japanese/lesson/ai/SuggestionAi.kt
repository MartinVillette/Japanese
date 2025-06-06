package com.example.japanese.lesson.ai

import android.util.Log
import android.widget.EditText
import android.widget.TextView
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

class SuggestionAi {

    interface SuggestionCallback {
        fun onSuccess(suggestions: JSONArray, expressionEditText: EditText, readingEditText: EditText)
        fun onFailure(errorMessage: String)
        fun onSuccessNewWord(word: String, expression: String, reading:String)
    }

    private val url = "https://api.groq.com/openai/v1/chat/completions"
    private val AI_MODEL = "llama-3.1-405b-reasoning"

    private val AI_SYSTEM_TRANSLATION = """
        You are an AI that makes language suggestions for learning Japanese purposes. Given a meaning in a certain language, you will provide multiple Japanese suggestions for it. These suggestions will primarily be translations of the given word. You will also be given a context with all the previous words and translations provided. Ensure that your suggestions use the same format as the context. Your output will be a JSON text with a list of suggestions, each containing the Japanese expression and its reading. If the lesson title is relevant, base your suggestions on it.
        
        INPUT: {"title":"TITLE_OF_THE_CONTEXT", "context":JSON_FORMAT_CONTEXT, "meaning":"THE_MEANING_TO_TRANSLATE"} 
        OUTPUT: {"suggestions":[{"expression":"THE_SUGGESTION_IN_JAPANESE", "reading":"THE_SUGGESTED_READING_IN_JAPANESE"}]}
    """.trimIndent()
    private val AI_SYSTEM_WORD = """
        You are an AI that makes language suggestions for learning Japanese purposes. Given a word in a certain language, you are going to output the most probable word in the same language. For example, if I give you as a context French words and as a word 'jamb', you have to output 'jambon', since it may be the most probable word. If the input is already an existing word, then the suggestion has to be this word and nothing else. You can't output a word that is already in the context. You are also given the title of the lesson. If this title is relevant, then your output should be based on it. 
        INPUT : {"title":"TITLE_OF_THE_CONTEXT", "context":LIST_OF_WORD_OF_THE_LANGUAGE_CONTEXT, "meaning":"INPUT_MEANING"} 
        OUTPUT : {"suggestion":"THE_SUGGESTION_OF_THE_WORD_IN_THE_CONTEXT_LANGUAGE"}
    """.trimIndent()
    private val AI_SYSTEM_NEW_WORD = """
        You are an AI that makes language suggestions for learning Japanese purposes. Given a context of a pair of words in a certain language and in Japanese, you are going to output a pair of words that could be useful for the user. The output is simply the word in the user's language and its translation in Japanese. This pair has to make sense and be useful for the user. You are also given the title of the lesson. If this title is relevant, then your output should be based on it. 
        INPUT = {"title": "TITLE_OF_THE_CONTEXT", "context": "LIST_OF_PAIR_OF_WORD"}
        OUTPUT = {"meaning": "SUGGESTED_MEANING_IN_USER_LANGUAGE", "expression": "SUGGESTED_EXPRESSION_IN_JAPANESE_LANGUAGE", "reading": "SUGGESTED_READING_IN_JAPANESE_LANGUAGE"}
    """.trimIndent()
    private val GROQ_API_KEY = "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"

    fun getWordSuggestion(word: String, title: String, context:JSONArray, expressionEditText:EditText, readingEditText:EditText, callback: SuggestionCallback) {

        val wordList = ArrayList<String>()
        for (i in 0 until context.length()) {
            val jsonObject = context.getJSONObject(i)
            val contextWord = jsonObject.getString("meaning")
            wordList.add(contextWord)
        }

        val prompt = "{'title' = ${title.trim()}, 'context' = $wordList, 'meaning' = ${word.trim()}"

        val jsonBody = JSONObject()
        jsonBody.put("model", AI_MODEL)
        val messagesArray = JSONArray()

        val systemMessage = JSONObject()
        systemMessage.put("role", "system")
        systemMessage.put("content", AI_SYSTEM_WORD)
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
        var suggestionWord = word.trim()

        okHttpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {}

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body
                if (response.isSuccessful && responseBody != null) {
                    // Handle successful response
                    val responseJson = JSONObject(responseBody.string())
                    var message = responseJson.getJSONArray("choices")
                        .getJSONObject(0)
                        .getJSONObject("message")
                        .getString("content")
                    // Use the callback to return the result
                    val start = message.indexOf('{')
                    val end = message.lastIndexOf('}')
                    try {
                        message = message.substring(start, end + 1)
                        val messageObject = JSONObject(message)
                        suggestionWord = messageObject.getString("suggestion")
                    } catch (e: Exception) {
                        getWordSuggestion(word, title, context, expressionEditText, readingEditText, callback)
                    }
                }
                Log.d("Suggestion", "Suggestion : $suggestionWord")
                getTranslationSuggestion(suggestionWord, title, context, expressionEditText, readingEditText, callback)
            }
        })
    }

    fun getTranslationSuggestion(word: String, title:String, context:JSONArray, expressionEditText:EditText, readingEditText:EditText, callback: SuggestionCallback) {
        val prompt = "{'title'=${title.trim()} ,'context' = $context, 'meaning' = $word"

        val jsonBody = JSONObject()
        jsonBody.put("model", AI_MODEL)
        val messagesArray = JSONArray()

        val systemMessage = JSONObject()
        systemMessage.put("role", "system")
        systemMessage.put("content", AI_SYSTEM_TRANSLATION)
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
            override fun onFailure(call: Call, e: IOException) {
                callback.onFailure(e.message ?: "Unknown error")
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body
                if (response.isSuccessful && responseBody != null) {
                    // Handle successful response
                    val responseJson = JSONObject(responseBody.string())
                    var message = responseJson.getJSONArray("choices")
                        .getJSONObject(0)
                        .getJSONObject("message")
                        .getString("content")
                    // Use the callback to return the result
                    val start = message.indexOf('{')
                    val end = message.lastIndexOf('}')
                    try {
                        message = message.substring(start, end + 1)
                        val messageObject = JSONObject(message)
                        val suggestions = messageObject.getJSONArray("suggestions")
                        callback.onSuccess(suggestions, expressionEditText, readingEditText)
                    } catch (e: Exception) {
                        getTranslationSuggestion(word, title, context, expressionEditText, readingEditText, callback)
                    }
                } else {
                    // Handle unsuccessful response
                    callback.onFailure("Unsuccessful response")
                }
            }
        })


    }

    fun getNewWordSuggestion(title:String, context: JSONArray, callback: SuggestionCallback) {
        val prompt = "{'title' = ${title.trim()}, 'context' = $context}"

        val jsonBody = JSONObject()
        jsonBody.put("model", AI_MODEL)
        val messagesArray = JSONArray()

        val systemMessage = JSONObject()
        systemMessage.put("role", "system")
        systemMessage.put("content", AI_SYSTEM_NEW_WORD)
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
                        .getString("content")

                    val start = message.indexOf('{')
                    val end = message.lastIndexOf('}')

                    try {
                        val messageObject = JSONObject(message.substring(start, end + 1))
                        val word = messageObject.getString("meaning")
                        val expression = messageObject.getString("expression")
                        val reading = messageObject.getString("reading")
                        callback.onSuccessNewWord(word, expression, reading)
                    } catch (e: Exception) {
                        getNewWordSuggestion(title, context, callback)
                    }
                }
            }
        })
    }
}