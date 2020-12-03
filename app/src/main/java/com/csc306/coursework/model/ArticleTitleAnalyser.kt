package com.csc306.coursework.model

import android.content.Context
import android.os.AsyncTask
import com.csc306.coursework.R
import com.google.gson.Gson
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody

class ArticleTitleAnalyser(val context: Context) : AsyncTask<Article, Void, Unit>() {

    override fun doInBackground(vararg params: Article) {
        val apiKey: String = context.getString(R.string.gcp_api_key)
        val article = params[0]
        val request: Request = Request.Builder()
            .url("https://language.googleapis.com/v1beta2/documents:analyzeEntities?key=$apiKey")
            .post("{\"document\": {\"type\": \"PLAIN_TEXT\",\"content\": \"${article.title}\"}}".toRequestBody("application/json".toMediaTypeOrNull()))
            .build()
        val responseBody: ResponseBody? = OkHttpClient().newCall(request).execute().body
        val DTO: EntitiesDTO = Gson().fromJson(responseBody?.string(), EntitiesDTO::class.java)
        article.titleKeywords = DTO.entities.filter { it.salience > 0 }.associateBy({ it.name }, { it.salience })
    }

}