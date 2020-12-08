package com.csc306.coursework.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.csc306.coursework.R
import com.csc306.coursework.adapter.FollowCategoriesAdapter
import com.csc306.coursework.database.CategoriesFollowingValueEventListener
import com.csc306.coursework.database.DatabaseManager
import com.csc306.coursework.database.RealtimeDatabaseManager
import com.dfl.newsapi.NewsApiRepository
import com.dfl.newsapi.enums.Language
import com.google.firebase.auth.FirebaseAuth
import io.reactivex.schedulers.Schedulers
import java.util.*

class FirstTimeSetupActivity : AppCompatActivity() {

    private lateinit var mDatabaseManager: DatabaseManager

    private lateinit var mUserUid: String

    private lateinit var mNewsApi: NewsApiRepository

    private lateinit var mRecyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mDatabaseManager = DatabaseManager(this)
        mUserUid = FirebaseAuth.getInstance().currentUser!!.uid
        mNewsApi = NewsApiRepository(getString(R.string.news_api_key))

        updateSources()

        setContentView(R.layout.activity_recycler_and_toolbar)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        toolbar.title = getString(R.string.follow_categories)
        setSupportActionBar(toolbar)

        mRecyclerView = findViewById(R.id.recycler_view)
        mRecyclerView.layoutManager = LinearLayoutManager(this)
    }

    override fun onStart() {
        super.onStart()
        buildFollowCategoriesAdapter()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.toolbar_next, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.toolbar_next) {
            setUserFollowingCategories()
            val intent = Intent(applicationContext, UpdateUserProfileActivity::class.java)
                .putExtra(IS_FIRST_TIME_SETUP, true)
            startActivity(intent)
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun buildFollowCategoriesAdapter() {
        val valueEventListener = CategoriesFollowingValueEventListener { categoryFollowStateArray ->
            mRecyclerView.adapter = FollowCategoriesAdapter(categoryFollowStateArray, this)
        }
        RealtimeDatabaseManager.getUserFollowingCategories(mUserUid, valueEventListener)
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

    private fun setUserFollowingCategories() {
        val adapter: FollowCategoriesAdapter = mRecyclerView.adapter as FollowCategoriesAdapter
        val categoriesFollowing: List<String> = adapter.categoryState
            .filter { it!!.second } // filter categories selected to be followed
            .map { it!!.first.toString() } // map category name

        RealtimeDatabaseManager.setUserFollowingCategories(mUserUid, categoriesFollowing)
    }

    companion object {
        const val IS_FIRST_TIME_SETUP = "IS_FIRST_TIME_SETUP"
    }

}