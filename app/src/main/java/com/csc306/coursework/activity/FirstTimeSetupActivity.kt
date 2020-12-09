package com.csc306.coursework.activity

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.csc306.coursework.R
import com.csc306.coursework.adapter.FollowCategoriesAdapter
import com.csc306.coursework.database.CategoriesFollowingValueEventListener
import com.csc306.coursework.database.RealtimeDatabaseManager
import com.csc306.coursework.scheduler.NewArticlesScheduler
import com.google.firebase.auth.FirebaseAuth

class FirstTimeSetupActivity : AppCompatActivity() {

    private lateinit var mUserUid: String

    private lateinit var mRecyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mUserUid = FirebaseAuth.getInstance().currentUser!!.uid

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
            NewArticlesScheduler.start(this)
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