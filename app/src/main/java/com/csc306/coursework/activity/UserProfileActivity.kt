package com.csc306.coursework.activity

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.csc306.coursework.R
import com.csc306.coursework.adapter.ArticleListAdapter
import com.csc306.coursework.scheduler.NewArticlesScheduler
import com.csc306.coursework.database.RealtimeDatabaseManager
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.squareup.picasso.Picasso
import org.apache.commons.lang3.StringUtils

class UserProfileActivity : AppCompatActivity() {

    private lateinit var mAuth: FirebaseAuth

    private lateinit var mRecyclerView: RecyclerView

    private lateinit var mUserUid: String

    private lateinit var mBtnFollow: Button

    private lateinit var mCurrentUserUid: String

    private var mIsFollowing: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mAuth = FirebaseAuth.getInstance()
        mUserUid = intent.getStringExtra(USER_UID)!!
        mCurrentUserUid = mAuth.currentUser!!.uid

        setContentView(R.layout.activity_user_profile)
        setSupportActionBar(findViewById(R.id.toolbar))

        mBtnFollow = findViewById(R.id.btn_follow)
        mBtnFollow.setOnClickListener {
            if (isProfileOfCurrentUser()) {
                startActivity(Intent(applicationContext, UpdateUserProfileActivity::class.java))
            } else if (mIsFollowing) {
                unfollowUser()
            } else {
                followUser()
            }
        }
        setButtonAndToolbarText()
        getUserProfile()

        mRecyclerView = findViewById(R.id.recycler_view)
        mRecyclerView.layoutManager = LinearLayoutManager(this)
        getUserLikes()
    }

    private fun isProfileOfCurrentUser(): Boolean {
        return mUserUid == mCurrentUserUid
    }

    private fun setButtonAndToolbarText() {
        if (isProfileOfCurrentUser()) {
            mBtnFollow.text = getString(R.string.edit_profile)
            supportActionBar!!.title = getString(R.string.profile)
        } else {
            RealtimeDatabaseManager.isFollowingUser(mCurrentUserUid, mUserUid) { isFollowing ->
                mIsFollowing = isFollowing
                mBtnFollow.text = getString(if (isFollowing) R.string.unfollow else R.string.follow)
            }
        }
    }

    private fun followUser() {
        RealtimeDatabaseManager.followUser(mCurrentUserUid, mUserUid) {
            mIsFollowing = true
            mBtnFollow.text = getString(R.string.unfollow)
        }
    }

    private fun unfollowUser() {
        RealtimeDatabaseManager.unfollowUser(mCurrentUserUid, mUserUid) {
            mIsFollowing = false
            mBtnFollow.text = getString(R.string.follow)
        }
    }

    private fun getUserProfile() {
        RealtimeDatabaseManager.getUserProfile(mUserUid) { userProfile ->
            if (userProfile == null) {
                throw Exception("Failed to get profile for user with uid $mUserUid") // TODO: CREATE SPECIFIC EXCEPTION
            }
            val displayNameTextView: TextView = findViewById(R.id.display_name)
            displayNameTextView.text = userProfile.displayName
            supportActionBar!!.title = userProfile.displayName + getString(R.string.user_likes_toolbar_title)
            val locationTextView: TextView = findViewById(R.id.location)
            locationTextView.text = userProfile.location
            if (StringUtils.isNotBlank(userProfile.profilePicURI)) {
                val profilePicImageView: ImageView = findViewById(R.id.profile_pic)
                Picasso.get().load(userProfile.profilePicURI).into(profilePicImageView)
            }
        }
    }

    private fun getUserLikes() {
        RealtimeDatabaseManager.getUserLikes(mUserUid) { articles ->
            val adapter = ArticleListAdapter(articles ?: mutableListOf(), mAuth, this)
            mRecyclerView.adapter = adapter

            val itemTouchHelper = ItemTouchHelper(ArticleListAdapter.SwipeCallback(adapter))
            itemTouchHelper.attachToRecyclerView(mRecyclerView)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.toolbar_profile, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.toolbar_refresh -> {
                Snackbar.make(mRecyclerView, getString(R.string.refreshing), Snackbar.LENGTH_SHORT).show()
                getUserLikes()
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

    companion object {
        const val USER_UID = "USER_UID"
    }

}