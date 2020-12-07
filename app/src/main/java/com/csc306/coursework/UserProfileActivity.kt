package com.csc306.coursework

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.csc306.coursework.adapter.ArticleListAdapter
import com.csc306.coursework.database.DatabaseManager
import com.csc306.coursework.database.RealtimeDatabaseManager
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.squareup.picasso.Picasso

class UserProfileActivity : AppCompatActivity() {

    private lateinit var mAuth: FirebaseAuth

    private lateinit var mRecyclerView: RecyclerView

    private lateinit var mToolbar: Toolbar

    private lateinit var mUserUid: String

    private lateinit var mBtnFollow: Button

    private var mIsFollowing: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mAuth = FirebaseAuth.getInstance()
        mUserUid = intent.getStringExtra(USER_UID)!!

        setContentView(R.layout.activity_user_profile)

        mToolbar = findViewById(R.id.toolbar)
        setSupportActionBar(mToolbar)

        mBtnFollow = findViewById(R.id.btn_follow)
        mBtnFollow.setOnClickListener {
            if (mIsFollowing) {
                unfollowUser()
            } else {
                followUser()
            }
        }

        getUserProfile()
        isCurrentUserFollowing()

        mRecyclerView = findViewById(R.id.recycler_view)
        mRecyclerView.layoutManager = LinearLayoutManager(this)
    }

    private fun getUserProfile() {
        RealtimeDatabaseManager.getUserProfile(mUserUid) { userProfile ->
            if (userProfile == null) {
                throw Exception("Failed to get profile for user with uid $mUserUid") // TODO: CREATE SPECIFIC EXCEPTION
            }
            val displayNameTextView: TextView = findViewById(R.id.display_name)
            displayNameTextView.text = userProfile.displayName
            mToolbar.title = userProfile.displayName + getString(R.string.user_likes_toolbar_title)
            val locationTextView: TextView = findViewById(R.id.location)
            locationTextView.text = userProfile.location
            if (userProfile.profilePicURI != null) {
                val profilePicImageView: ImageView = findViewById(R.id.profile_pic)
                Picasso.get().load(userProfile.profilePicURI).into(profilePicImageView)
            }
        }
    }

    private fun isCurrentUserFollowing() {
        val currentUserUid: String = mAuth.currentUser!!.uid
        RealtimeDatabaseManager.isFollowingUser(currentUserUid, mUserUid) { isFollowing ->
            mIsFollowing = isFollowing
            mBtnFollow.text = getString(if (isFollowing) R.string.unfollow else R.string.follow)
        }
    }

    private fun followUser() {
        val currentUserUid: String = mAuth.currentUser!!.uid
        RealtimeDatabaseManager.followUser(currentUserUid, mUserUid) {
            mIsFollowing = true
            mBtnFollow.text = getString(R.string.unfollow)
        }
    }

    private fun unfollowUser() {
        val currentUserUid: String = mAuth.currentUser!!.uid
        RealtimeDatabaseManager.unfollowUser(currentUserUid, mUserUid) {
            mIsFollowing = false
            mBtnFollow.text = getString(R.string.follow)
        }
    }

    override fun onStart() {
        super.onStart()
        getUserLikes()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.toolbar_articles, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.toolbar_refresh -> {
                Snackbar.make(mRecyclerView, getString(R.string.refreshing), Snackbar.LENGTH_SHORT).show()
                getUserLikes()
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

    private fun getUserLikes() {
        RealtimeDatabaseManager.getUserLikes(mUserUid) { articles ->
            val adapter = ArticleListAdapter(articles, mAuth, this)
            mRecyclerView.adapter = adapter

            val itemTouchHelper = ItemTouchHelper(ArticleListAdapter.SwipeCallback(adapter))
            itemTouchHelper.attachToRecyclerView(mRecyclerView)
        }
    }

    companion object {
        const val USER_UID = "USER_UID"
    }

}