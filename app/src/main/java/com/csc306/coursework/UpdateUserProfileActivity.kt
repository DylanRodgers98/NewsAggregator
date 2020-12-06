package com.csc306.coursework

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.csc306.coursework.database.RealtimeDatabaseManager
import com.csc306.coursework.model.UserProfile
import com.google.firebase.auth.FirebaseAuth

class UpdateUserProfileActivity : AppCompatActivity() {

    private lateinit var mAuth: FirebaseAuth

    private var isFirstTimeSetup: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mAuth = FirebaseAuth.getInstance()
        isFirstTimeSetup = intent.getBooleanExtra(FirstTimeSetupActivity.IS_FIRST_TIME_SETUP, false)

        setContentView(R.layout.activity_update_user_profile)
        setSupportActionBar(findViewById(R.id.toolbar))
    }

    override fun onStart() {
        super.onStart()
        if (!isFirstTimeSetup) {
            getUserProfile()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.toolbar_save, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.toolbar_save) {
            updateUserProfile {
                if (isFirstTimeSetup) {
                    startActivity(Intent(applicationContext, MainActivity::class.java))
                } else {
                    finish()
                }
            }
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun getUserProfile() {
        val userUid: String = mAuth.currentUser!!.uid
        RealtimeDatabaseManager.getUserProfile(userUid) {
            if (it != null) {
                val displayNameView: TextView = findViewById(R.id.input_display_name)
                displayNameView.text = it.displayName
                val locationTextView: TextView = findViewById(R.id.input_location)
                locationTextView.text = it.location
            }
        }
    }

    private fun updateUserProfile(onComplete: () -> Unit) {
        val userUid: String = mAuth.currentUser!!.uid
        val displayNameView: TextView = findViewById(R.id.input_display_name)
        val displayName: String = displayNameView.text.toString()
        val locationTextView: TextView = findViewById(R.id.input_location)
        val location: String = locationTextView.text.toString()
        val userProfile = UserProfile(displayName, location, "")
        RealtimeDatabaseManager.updateUserProfile(userUid, userProfile, onComplete)
    }

}