package com.csc306.coursework

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import androidx.appcompat.widget.AppCompatTextView
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
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import org.apache.commons.lang3.mutable.Mutable
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.util.*

class MainActivity : AppCompatActivity() {

    private val newsApi: NewsApiRepository = NewsApiRepository("bf38b882f6a6421096d8acd384d33b71")

    private val databaseManager: DatabaseManager = DatabaseManager(this)

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
        recyclerView.adapter = ArticleListAdapter(getArticles(), this)
//        recyclerView.adapter = FollowCategoriesAdapter(getCategories(),this)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.toolbar, menu)
        return super.onCreateOptionsMenu(menu)
    }

    private fun getArticles(): MutableList<Article> {
        val categoriesFollowing: MutableList<String> = mutableListOf()
        getSharedPreferences(FollowCategoriesAdapter.CATEGORIES_FOLLOWING, Context.MODE_PRIVATE).all.entries.forEach {
            val isFollowing = it.value
            if (isFollowing is Boolean && isFollowing) {
                val category: String = it.key
                categoriesFollowing.add(category.toLowerCase(Locale.getDefault()))
            }
        }

        val sourceIds: String = databaseManager.getSourceIdsForCategories(categoriesFollowing.toTypedArray())

        return newsApi.getTopHeadlines(sources = sourceIds)
            .subscribeOn(Schedulers.io())
            .toFlowable()
            .flatMapIterable { it.articles }
            .map { article ->
                Article(
                    article.source.name,
                    OffsetDateTime.parse(article.publishedAt).toLocalDateTime(),
                    article.urlToImage,
                    article.title,
                    article.description,
                    article.url
                )
            }
            .toList()
            .blockingGet()
//        return arrayListOf(
//            Article(
//                "BBC News",
//                LocalDateTime.of(2020, 11, 15, 9, 0),
//                "https://ichef.bbci.co.uk/news/800/cpsprodpb/B1F6/production/_115085554_gettyimages-1229248614-1.jpg",
//                "Coronavirus: Russia steps up restrictions as infections surge",
//                "Masks must be worn in all public areas and bars and restaurants should close overnight, officials say.",
//                ""
//            ),
//            Article(
//                "TMZ",
//                LocalDateTime.of(2020, 11, 12, 18, 0),
//                "https://imagez.tmz.com/image/cd/16by9/2020/10/29/cd4b3de3ea8f4d35a13b2ac513b0638e_xl.jpg",
//                "Scarlett Johansson and Colin Jost Married in Private Ceremony - TMZ",
//                "Scarlett Johansson and Colin Jost have tied the knot.",
//                ""
//            ),
//            Article(
//                "National Geographic",
//                LocalDateTime.of(2020, 11, 10, 12, 0),
//                "https://www.nationalgeographic.com/content/dam/science/2020/08/17/environmental-policy-tracker/environmental-policy-tracker-1169904873.jpg",
//                "Trump allows logging in Alaska’s Tongass National Forest",
//                "A running list of the 2020 presidential candidates' dueling visions for U.S. environmental policy.",
//                ""
//            )
//        )
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

    private fun getCategories(): MutableList<Category> {
        return arrayListOf(
            Category(getString(R.string.business), R.drawable.business),
            Category(getString(R.string.entertainment), R.drawable.entertainment),
            Category(getString(R.string.general), R.drawable.general),
            Category(getString(R.string.health), R.drawable.health),
            Category(getString(R.string.science), R.drawable.science),
            Category(getString(R.string.sports), R.drawable.sports),
            Category(getString(R.string.technology), R.drawable.technology))
    }

}