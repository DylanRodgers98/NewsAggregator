package com.csc306.coursework.activity

import android.app.SearchManager
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.csc306.coursework.database.RealtimeDatabaseManager
import com.csc306.coursework.model.Article
import com.csc306.coursework.newsapi.NewsAPIService

class SearchResultsActivity : AppCompatActivity() {

    private lateinit var mNewsApi: NewsAPIService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mNewsApi = NewsAPIService(this)
        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        if (Intent.ACTION_SEARCH == intent.action) {
            val query = intent.getStringExtra(SearchManager.QUERY)
            if (query != null) {
                RealtimeDatabaseManager.findUsersWithDisplayName(query) {

                }
                RealtimeDatabaseManager.findSourcesByQuery(query) {

                }
                val articles: MutableList<Article> = mNewsApi.getEverything(query)
            }
        }
    }

}