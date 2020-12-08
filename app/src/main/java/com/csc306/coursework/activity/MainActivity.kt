package com.csc306.coursework.activity

import android.app.SearchManager
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.csc306.coursework.R
import com.csc306.coursework.adapter.ArticleListAdapter
import com.csc306.coursework.database.DatabaseManager
import com.csc306.coursework.database.RealtimeDatabaseManager
import com.csc306.coursework.database.ThrowingValueEventListener
import com.csc306.coursework.model.Article
import com.csc306.coursework.newsapi.NewsAPIService
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class MainActivity : AppCompatActivity() {

    private lateinit var mDatabaseManager: DatabaseManager

    private lateinit var mNewsApi: NewsAPIService

    private lateinit var mAuth: FirebaseAuth

    private lateinit var mRecyclerView: RecyclerView

    private var mSortByLikability: Boolean = false

    private var mLatestArticles: MutableList<Article>? = null

    private var mRecommendedArticles: MutableList<Article>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mDatabaseManager = DatabaseManager(this)
        mNewsApi = NewsAPIService(this)
        mAuth = FirebaseAuth.getInstance()
        setContentView(R.layout.activity_recycler_and_toolbar)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        toolbar.title = getString(R.string.following)
        setSupportActionBar(toolbar)

        mRecyclerView = findViewById(R.id.recycler_view)
        mRecyclerView.layoutManager = LinearLayoutManager(this)
    }

    override fun onStart() {
        super.onStart()
        getArticles()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.toolbar_articles, menu)

        val searchMenuItem: MenuItem = menu.findItem(R.id.search)
        val searchView: SearchView = searchMenuItem.actionView as SearchView
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextChange(newText: String?): Boolean = true
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (query != null) {
                    val intent: Intent = Intent(applicationContext, SearchResultsActivity::class.java)
                        .putExtra(SearchManager.QUERY, query)
                    startActivity(intent)
                }
                return true
            }
        })

        val switchTo: MenuItem = menu.findItem(R.id.toolbar_switch_to)
        switchTo.title = getString(if (mSortByLikability) {
            R.string.switch_to_latest
        } else {
            R.string.switch_to_recommended
        })

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.toolbar_refresh -> {
                Snackbar.make(mRecyclerView, getString(R.string.refreshing), Snackbar.LENGTH_SHORT).show()
                getArticles()
                return true
            }
            R.id.toolbar_switch_to -> {
                when (item.title) {
                    getString(R.string.switch_to_latest) -> {
                        mSortByLikability = false
                        switchTo(mLatestArticles)
                        item.title = getString(R.string.switch_to_recommended)
                        return true
                    }
                    getString(R.string.switch_to_recommended) -> {
                        mSortByLikability = true
                        switchTo(mRecommendedArticles)
                        item.title = getString(R.string.switch_to_latest)
                        return true
                    }
                }
            }
            R.id.toolbar_settings -> {
                startActivity(Intent(applicationContext, SettingsActivity::class.java))
                return true
            }
            R.id.toolbar_log_out -> {
                mAuth.signOut()
                startActivity(Intent(applicationContext, AuthenticationActivity::class.java))
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun switchTo(articles: MutableList<Article>?) {
        mRecyclerView.adapter = null
        if (articles == null) {
            getArticles()
        } else {
            buildAdapter(articles)
        }
    }

    private fun getArticles() {
        val userUid: String = mAuth.currentUser!!.uid
        RealtimeDatabaseManager.getUserFollowingCategories(userUid, ThrowingValueEventListener {
            val stringListType = object : GenericTypeIndicator<List<String>>() {}
            val categories: Array<String> = it.getValue(stringListType)?.toTypedArray() ?: emptyArray()
            val sourceIds: String = mDatabaseManager.getSourceIdsForCategories(categories)
            mLatestArticles = mNewsApi.getTopHeadlines(sourceIds)
            if (!mSortByLikability) {
                buildAdapter(mLatestArticles!!)
            }
            sortArticlesByLikability(mLatestArticles!!)
        })
    }

    private fun sortArticlesByLikability(articles: MutableList<Article>) {
        val userUid: String = mAuth.currentUser!!.uid
        val oldestArticle: Long = articles.minOf { it.publishDateMillis }
        RealtimeDatabaseManager.sortArticlesByLikability(userUid, oldestArticle, articles, this) {
            mRecommendedArticles = it.toMutableList()
            if (mSortByLikability) {
                buildAdapter(mRecommendedArticles!!)
            }
        }
    }

    private fun buildAdapter(articles: MutableList<Article>) {
        val adapter = ArticleListAdapter(articles, mAuth, this)
        mRecyclerView.adapter = adapter

        val itemTouchHelper = ItemTouchHelper(ArticleListAdapter.SwipeCallback(adapter))
        itemTouchHelper.attachToRecyclerView(mRecyclerView)
    }

}