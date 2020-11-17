package com.csc306.coursework

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.csc306.coursework.adapter.ArticleListAdapter
import com.csc306.coursework.adapter.FollowCategoriesAdapter
import com.csc306.coursework.database.DatabaseManager
import com.csc306.coursework.model.Article
import com.csc306.coursework.model.Category
import com.dfl.newsapi.NewsApiRepository
import com.dfl.newsapi.model.ArticleDto
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import io.reactivex.schedulers.Schedulers
import java.time.OffsetDateTime

class MainActivity : AppCompatActivity() {

    private val mNewsApi: NewsApiRepository = NewsApiRepository("bf38b882f6a6421096d8acd384d33b71")

    private val mDatabaseManager: DatabaseManager = DatabaseManager(this)

    private lateinit var mAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mAuth = FirebaseAuth.getInstance()
        setContentView(R.layout.activity_recycler_and_toolbar)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        toolbar.title = getString(R.string.following)
        setSupportActionBar(toolbar)
//        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val recyclerView: RecyclerView = findViewById(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = ArticleListAdapter(getArticles(), this)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.toolbar_following, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.toolbar_refresh -> {
                val recyclerView: RecyclerView = findViewById(R.id.recycler_view)
                Snackbar.make(recyclerView, getString(R.string.refreshing), Snackbar.LENGTH_SHORT).show()
                recyclerView.adapter = ArticleListAdapter(getArticles(), this)
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

    private fun getArticles(): MutableList<Article> {
        val sourceIds: String = mDatabaseManager.getSourceIdsForCategories(getCategoriesFollowing())
        return mNewsApi.getTopHeadlines(sources = sourceIds)
            .subscribeOn(Schedulers.io())
            .toFlowable()
            .flatMapIterable { it.articles }
            .map { buildArticle(it) }
            .toList()
            .blockingGet()
    }

    private fun buildArticle(articleDto: ArticleDto): Article {
        return Article(
            articleDto.source.name,
            OffsetDateTime.parse(articleDto.publishedAt).toLocalDateTime(),
            articleDto.urlToImage,
            articleDto.title,
            articleDto.description,
            articleDto.url
        )
    }

    private fun getCategoriesFollowing(): Array<String> {
        val categoriesFollowing: MutableList<String> = mutableListOf()
        Category.values().forEach {
            val categoryName = it.toString()
            val isFollowing = getSharedPreferences(FollowCategoriesAdapter.CATEGORIES_FOLLOWING, Context.MODE_PRIVATE)
                .getBoolean(categoryName, false)
            if (isFollowing) {
                categoriesFollowing.add(categoryName)
            }
        }
        return categoriesFollowing.toTypedArray()
    }

}