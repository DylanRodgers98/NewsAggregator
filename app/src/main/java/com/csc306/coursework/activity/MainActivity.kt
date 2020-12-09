package com.csc306.coursework.activity

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.csc306.coursework.R
import com.csc306.coursework.adapter.ArticleListAdapter
import com.csc306.coursework.adapter.CategorySelectionAdapter
import com.csc306.coursework.database.RealtimeDatabaseManager
import com.csc306.coursework.database.ThrowingValueEventListener
import com.csc306.coursework.model.Article
import com.csc306.coursework.model.Category
import com.csc306.coursework.model.FollowingCategory
import com.csc306.coursework.model.Source
import com.csc306.coursework.newsapi.NewsAPIService
import com.csc306.coursework.scheduler.NewArticlesScheduler
import com.csc306.coursework.scheduler.NewArticlesService
import com.dfl.newsapi.enums.Language
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.GenericTypeIndicator
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var mNewsApi: NewsAPIService

    private lateinit var mAuth: FirebaseAuth

    private lateinit var mUserUid: String

    private lateinit var mRecyclerView: RecyclerView

    private var mSortByLikability: Boolean = false

    private var mLatestArticles: MutableList<Article>? = null

    private var mRecommendedArticles: MutableList<Article>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mNewsApi = NewsAPIService(this)
        mAuth = FirebaseAuth.getInstance()
        mUserUid = mAuth.currentUser!!.uid

        updateSourcesIfNecessary()

        setContentView(R.layout.activity_recycler_and_toolbar)
        setSupportActionBar(findViewById(R.id.toolbar))

        mRecyclerView = findViewById(R.id.recycler_view)
        mRecyclerView.layoutManager = LinearLayoutManager(this)
    }

    private fun updateSourcesIfNecessary() {
        RealtimeDatabaseManager.doSourcesNeedUpdating { shouldUpdate ->
            if (shouldUpdate) {
                val defaultLanguage: String = Locale.getDefault().language
                val languageCode: Language = Language.valueOf(defaultLanguage.toUpperCase(Locale.getDefault()))
                val categorySourceMap: Map<String, List<Source>> = mNewsApi.getSources(languageCode)
                RealtimeDatabaseManager.updateSources(categorySourceMap)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        getArticles()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.toolbar_main, menu)

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
            R.id.toolbar_profile -> {
                val intent: Intent = Intent(applicationContext, UserProfileActivity::class.java)
                    .putExtra(UserProfileActivity.USER_UID, mUserUid)
                startActivity(intent)
                return true
            }
            R.id.toolbar_choose_category -> {
                startActivity(Intent(applicationContext, ChooseCategoryActivity::class.java))
                return true
            }
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
                NewArticlesScheduler.stop(this)
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
        val categoryString: String? = intent.getStringExtra(CategorySelectionAdapter.CATEGORY)
        if (categoryString == null || FollowingCategory.valueOf(categoryString) == FollowingCategory.FOLLOWING) {
            supportActionBar!!.title = getString(FollowingCategory.FOLLOWING.nameStringResource)
            getFollowedCategories()
        } else {
            val category: Category = Category.valueOf(categoryString)
            supportActionBar!!.title = getString(category.nameStringResource)
            getArticles(listOf(category.toString()))
        }
    }

    private fun getFollowedCategories() {
        RealtimeDatabaseManager.getUserFollowingCategories(mUserUid, ThrowingValueEventListener {
            val stringListType = object : GenericTypeIndicator<List<String>>() {}
            val categories: List<String> = it.getValue(stringListType) ?: emptyList()
            getArticles(categories)
        })
    }

    private fun getArticles(categories: List<String>) {
        RealtimeDatabaseManager.getSourceIdsForCategories(categories) { sourceIds ->
            val articles: MutableList<Article> = mNewsApi.getTopHeadlines(sourceIds)
            getSharedPreferences(NewArticlesService.SERVICE_PREFERENCES, Context.MODE_PRIVATE).edit()
                .putLong(NewArticlesService.LAST_UPDATED, System.currentTimeMillis())
                .apply()
            val oldestArticle: Long = articles.minOf { it.publishDateMillis }
            RealtimeDatabaseManager.addArticlesLikedByFollowedUsers(mUserUid, oldestArticle, articles) {
                it.sortByDescending { article -> article.publishDateMillis }
                mLatestArticles = it
                if (!mSortByLikability) {
                    buildAdapter(mLatestArticles!!)
                }
                sortArticlesByLikability(mLatestArticles!!)
            }
        }
    }

    private fun sortArticlesByLikability(articles: MutableList<Article>) {
        RealtimeDatabaseManager.sortArticlesByLikability(mUserUid, articles, this) {
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