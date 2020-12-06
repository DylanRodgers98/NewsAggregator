package com.csc306.coursework.async

import android.content.Context
import android.os.AsyncTask
import com.csc306.coursework.R
import com.csc306.coursework.model.Article
import com.csc306.coursework.model.EntitiesDTO
import com.google.gson.Gson
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class ArticleTitleAnalyser(private val context: Context) : AsyncTask<Article, Void, Map<String, Double>>() {

    override fun doInBackground(vararg params: Article): Map<String, Double>? {
        val article: Article = params[0]
        val apiKey: String = context.getString(R.string.gcp_api_key)
        val request: Request = Request.Builder()
            .url("https://language.googleapis.com/v1beta2/documents:analyzeEntities?key=$apiKey")
            .post("{\"document\": {\"type\": \"PLAIN_TEXT\",\"content\": \"${article.title}\"}}"
                .toRequestBody("application/json".toMediaTypeOrNull()))
            .build()
        OKHTTP_CLIENT.newCall(request).execute().use { response ->
            val dto: EntitiesDTO = GSON.fromJson(response.body?.string(), EntitiesDTO::class.java)
            return dto.entities?.filter { it.salience > 0 }?.associateBy({ it.name }, { it.salience })
        }
    }

    companion object {
        private val OKHTTP_CLIENT = OkHttpClient()
        private val GSON: Gson = Gson()
    }

}