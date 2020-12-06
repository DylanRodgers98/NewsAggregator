package com.csc306.coursework

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.csc306.coursework.adapter.FollowCategoriesAdapter
import com.csc306.coursework.database.CategoriesFollowingValueEventListener
import com.csc306.coursework.database.RealtimeDatabaseManager
import com.google.firebase.auth.FirebaseAuth

class SettingsActivity : AppCompatActivity() {

    private lateinit var mRealtimeDatabaseManager: RealtimeDatabaseManager

    private lateinit var mAuth: FirebaseAuth

    private lateinit var recyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mRealtimeDatabaseManager = RealtimeDatabaseManager()
        mAuth = FirebaseAuth.getInstance()

        setContentView(R.layout.activity_recycler_and_toolbar)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        toolbar.title = getString(R.string.settings)
        setSupportActionBar(toolbar)

        recyclerView = findViewById(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this)
    }

    override fun onStart() {
        super.onStart()
        buildFollowCategoriesAdapter()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        updateUser()
    }

    private fun buildFollowCategoriesAdapter() {
        val userUid: String = mAuth.currentUser!!.uid
        val valueEventListener = CategoriesFollowingValueEventListener { categoryFollowStateArray ->
            recyclerView.adapter = FollowCategoriesAdapter(categoryFollowStateArray, this)
        }
        mRealtimeDatabaseManager.getUserFollowingCategories(userUid, valueEventListener)
    }

    private fun updateUser() {
        val userUid: String = mAuth.currentUser!!.uid
        val adapter: FollowCategoriesAdapter = recyclerView.adapter as FollowCategoriesAdapter
        val categoriesFollowing: List<String> = adapter.categoryState
            .filter { it!!.second } // filter categories selected to be followed
            .map { it!!.first.toString() } // map category name

        mRealtimeDatabaseManager.setUserFollowingCategories(userUid, categoriesFollowing)
    }

}