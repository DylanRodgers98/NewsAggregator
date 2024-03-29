package com.csc306.coursework.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.csc306.coursework.R
import com.csc306.coursework.adapter.FollowCategoriesAdapter
import com.csc306.coursework.database.CategoriesFollowingValueEventListener
import com.csc306.coursework.database.RealtimeDatabaseManager
import com.google.firebase.auth.FirebaseAuth

class FollowCategoriesActivity : AppCompatActivity() {

    private lateinit var mUserUid: String

    private lateinit var mRecyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mUserUid = FirebaseAuth.getInstance().currentUser!!.uid

        setContentView(R.layout.activity_recycler_and_toolbar)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        toolbar.title = getString(R.string.edit_following)
        setSupportActionBar(toolbar)

        mRecyclerView = findViewById(R.id.recycler_view)
        mRecyclerView.layoutManager = LinearLayoutManager(this)
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
        val valueEventListener = CategoriesFollowingValueEventListener { categoryFollowStateArray ->
            mRecyclerView.adapter = FollowCategoriesAdapter(categoryFollowStateArray, this)
        }
        RealtimeDatabaseManager.getUserFollowingCategories(mUserUid, valueEventListener)
    }

    private fun updateUser() {
        val adapter: FollowCategoriesAdapter = mRecyclerView.adapter as FollowCategoriesAdapter
        val categoriesFollowing: List<String> = adapter.categoryState
            .filter { it!!.second } // filter categories selected to be followed
            .map { it!!.first.toString() } // map category name

        RealtimeDatabaseManager.setUserFollowingCategories(mUserUid, categoriesFollowing)
    }

}