package com.csc306.coursework

import android.annotation.SuppressLint
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.csc306.coursework.adapter.ArticleListAdapter
import com.csc306.coursework.database.DatabaseManager
import com.csc306.coursework.database.RealtimeDatabaseManager
import com.csc306.coursework.database.ThrowingValueEventListener
import com.csc306.coursework.model.Article
import com.csc306.coursework.model.ArticleLikabilityComparator
import com.dfl.newsapi.NewsApiRepository
import com.dfl.newsapi.model.ArticleDto
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import io.reactivex.schedulers.Schedulers
import java.time.OffsetDateTime

class MainActivity : AppCompatActivity() {

    private lateinit var mDatabaseManager: DatabaseManager

    private lateinit var mNewsApi: NewsApiRepository

    private lateinit var mAuth: FirebaseAuth

    private lateinit var mArticleComparator: ArticleLikabilityComparator

    private lateinit var mRecyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mDatabaseManager = DatabaseManager(this)
        mNewsApi = NewsApiRepository(getString(R.string.news_api_key))
        mAuth = FirebaseAuth.getInstance()
        mArticleComparator = ArticleLikabilityComparator(mDatabaseManager, this)
        setContentView(R.layout.activity_recycler_and_toolbar)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        toolbar.title = getString(R.string.following)
        setSupportActionBar(toolbar)

        mRecyclerView = findViewById(R.id.recycler_view)
        mRecyclerView.layoutManager = LinearLayoutManager(this)
    }

    override fun onStart() {
        super.onStart()
        buildRecyclerViewAdapter()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.toolbar_articles, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.toolbar_refresh -> {
                Snackbar.make(mRecyclerView, getString(R.string.refreshing), Snackbar.LENGTH_SHORT).show()
                buildRecyclerViewAdapter()
                return true
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

    private fun buildRecyclerViewAdapter() {
        val userUid: String = mAuth.currentUser!!.uid
        RealtimeDatabaseManager.getUserFollowingCategories(userUid, ThrowingValueEventListener {
            val stringListType = object : GenericTypeIndicator<List<String>>() {}
            val categoriesFollowing: List<String>? = it.getValue(stringListType)
            getArticles(categoriesFollowing?.toTypedArray() ?: emptyArray())
        })
    }

    @SuppressLint("CheckResult")
    private fun getArticles(categoriesFollowing: Array<String>) {
        val sourceIds: String = mDatabaseManager.getSourceIdsForCategories(categoriesFollowing)
        val articles: MutableList<Article> = mNewsApi.getTopHeadlines(sources = sourceIds)
            .subscribeOn(Schedulers.io())
            .toFlowable()
            .flatMapIterable { it.articles }
            .map { buildArticle(it) }
            .toList()
            .blockingGet()

        getArticlesLikedByFollowedUsers(articles)
    }

    private fun buildArticle(articleDto: ArticleDto): Article {
        return Article(
            articleDto.source.name,
            OffsetDateTime.parse(articleDto.publishedAt).toInstant().toEpochMilli(),
            articleDto.urlToImage,
            articleDto.title,
            articleDto.description,
            articleDto.url,
        )
    }

    private fun getArticlesLikedByFollowedUsers(articles: MutableList<Article>) {
        val userUid: String = mAuth.currentUser!!.uid
        RealtimeDatabaseManager.getArticlesLikedByFollowedUsers(userUid) {
            it.forEach { article ->
                val index: Int = articles.indexOf(article)
                if (index > -1) {
                    articles[index] = article
                } else {
                    articles.add(article)
                }
            }
            removeDislikedArticles(articles)
        }
    }

    private fun removeDislikedArticles(articles: MutableList<Article>) {
        val userUid: String = mAuth.currentUser!!.uid
        RealtimeDatabaseManager.removeDislikedArticlesFromList(userUid, articles) {
            getTitleKeywords(it)
        }
    }

    private fun getTitleKeywords(articles: MutableList<Article>) {
        RealtimeDatabaseManager.getArticleTitleKeywords(articles, this) {
            attachAdapterToRecyclerView(it)
        }
    }

    private fun attachAdapterToRecyclerView(articles: MutableList<Article>) {
        articles.sortWith(mArticleComparator)

        val adapter = ArticleListAdapter(articles, mAuth, mDatabaseManager, this)
        mRecyclerView.adapter = adapter

        val itemTouchHelper = ItemTouchHelper(ArticleListAdapter.SwipeCallback(adapter))
        itemTouchHelper.attachToRecyclerView(mRecyclerView)
    }

}