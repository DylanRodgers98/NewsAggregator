package com.csc306.coursework

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.csc306.coursework.adapter.FollowCategoriesAdapter
import com.csc306.coursework.database.DatabaseManager
import com.csc306.coursework.model.Category
import com.dfl.newsapi.NewsApiRepository
import com.dfl.newsapi.enums.Language
import io.reactivex.schedulers.Schedulers
import java.util.*

class FirstTimeSetupActivity : AppCompatActivity() {

    private val mNewsApi: NewsApiRepository = NewsApiRepository("bf38b882f6a6421096d8acd384d33b71")

    private val mDatabaseManager: DatabaseManager = DatabaseManager(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recycler_and_toolbar)

        clearFollowedCategories()
        updateSources()

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        toolbar.title = getString(R.string.follow_categories)
        setSupportActionBar(toolbar)

        val recyclerView: RecyclerView = findViewById(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = FollowCategoriesAdapter(Category.values(), this)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.toolbar_first_time_setup, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.toolbar_next) {
            startActivity(Intent(applicationContext, MainActivity::class.java))
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun clearFollowedCategories() {
        getSharedPreferences(FollowCategoriesAdapter.CATEGORIES_FOLLOWING, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
    }

    @SuppressLint("CheckResult")
    private fun updateSources() {
        val defaultLanguage: String = Locale.getDefault().language
        val languageCode: Language = Language.valueOf(defaultLanguage.toUpperCase(Locale.getDefault()))
        mNewsApi.getSources(language = languageCode)
            .subscribeOn(Schedulers.io())
            .toFlowable()
            .subscribe({
                Log.i(localClassName, "Updating ${it.sources.size} sources in database")
                mDatabaseManager.updateSources(it.sources)
                Log.i(localClassName, "Sources updated in database")
            }, { err ->
                Log.e(localClassName, "An error occurred when updating source info:", err)
            })
    }

}