package com.csc306.coursework.activity

import android.app.SearchManager
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.csc306.coursework.R
import com.csc306.coursework.adapter.SearchResultsAdapter
import com.csc306.coursework.database.RealtimeDatabaseManager
import com.csc306.coursework.model.Article
import com.csc306.coursework.newsapi.NewsAPIService

class SearchResultsActivity : AppCompatActivity() {

    private lateinit var mNewsApi: NewsAPIService

    private lateinit var mRecyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mNewsApi = NewsAPIService(this)
        setContentView(R.layout.activity_recycler_and_toolbar)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        toolbar.title = getString(R.string.search)
        setSupportActionBar(toolbar)

        mRecyclerView = findViewById(R.id.recycler_view)
        mRecyclerView.layoutManager = LinearLayoutManager(this)

        val query = intent.getStringExtra(SearchManager.QUERY)
        if (query != null) {
            search(query)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.toolbar_search, menu)

        val searchMenuItem: MenuItem = menu.findItem(R.id.search)
        val searchView: SearchView = searchMenuItem.actionView as SearchView
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextChange(newText: String?): Boolean = true
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (query != null) {
                    search(query)
                }
                return true
            }
        })

        return super.onCreateOptionsMenu(menu)
    }

    private fun search(query: String) {
        supportActionBar!!.title = getString(R.string.search) + ": \"" + query + "\""
        RealtimeDatabaseManager.findUsersWithDisplayName(query) { userProfiles ->
            RealtimeDatabaseManager.findSourcesByName(query) { sources ->
                val articles: MutableList<Article> = mNewsApi.getEverythingByQuery(query)
                mRecyclerView.adapter = SearchResultsAdapter(userProfiles, sources, articles, this)
            }
        }
    }

}