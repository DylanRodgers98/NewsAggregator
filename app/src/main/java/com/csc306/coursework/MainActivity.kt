package com.csc306.coursework

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import androidx.appcompat.widget.AppCompatTextView
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.csc306.coursework.adapter.CategorySelectionAdapter
import com.csc306.coursework.adapter.FollowCategoriesAdapter
import com.csc306.coursework.database.DatabaseManager
import com.csc306.coursework.model.Article
import com.csc306.coursework.model.Category
import com.dfl.newsapi.NewsApiRepository
import com.dfl.newsapi.model.ArticleDto
import io.reactivex.schedulers.Schedulers
import java.time.OffsetDateTime

class MainActivity : AppCompatActivity() {

    private val mNewsApi: NewsApiRepository = NewsApiRepository("bf38b882f6a6421096d8acd384d33b71")

    private val mDatabaseManager: DatabaseManager = DatabaseManager(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        toolbar.title = "DylanRodgers98"
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val username: AppCompatTextView = findViewById(R.id.username)
        username.text = "DylanRodgers98"
        val location: AppCompatTextView = findViewById(R.id.location)
        location.text = "Swansea, Wales"

        val recyclerView: RecyclerView = findViewById(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this)
//        recyclerView.adapter = ArticleListAdapter(getArticles(), this)
//        recyclerView.adapter = FollowCategoriesAdapter(Category.values(), this)
        recyclerView.adapter = CategorySelectionAdapter(Category.values(), this)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.toolbar, menu)
        return super.onCreateOptionsMenu(menu)
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

//    override fun onOptionsItemSelected(item: MenuItem): Boolean {
//        val toolbarView: View = findViewById(R.id.toolbar)
//        when (item.itemId) {
//            R.id.back -> {
//                val snackbar = Snackbar.make(toolbarView, "You clicked back", Snackbar.LENGTH_LONG)
//                snackbar.show()
//                return true
//            }
//        }
//        return super.onOptionsItemSelected(item)
//    }

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