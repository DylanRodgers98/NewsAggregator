package com.csc306.coursework.activity

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.drawToBitmap
import com.csc306.coursework.R
import com.csc306.coursework.database.RealtimeDatabaseManager
import com.csc306.coursework.model.UserProfile
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.squareup.picasso.Picasso
import org.apache.commons.lang3.StringUtils
import java.io.ByteArrayOutputStream
import kotlin.Exception

class UpdateUserProfileActivity : AppCompatActivity() {

    private lateinit var mAuth: FirebaseAuth

    private lateinit var mStorage: FirebaseStorage

    private var isFirstTimeSetup: Boolean = false

    private lateinit var mDisplayNameTextView: TextView

    private lateinit var mLocationTextView: TextView

    private lateinit var mProfilePicImageView: ImageView

    private var isProfilePicChanged: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mAuth = FirebaseAuth.getInstance()
        mStorage = FirebaseStorage.getInstance()
        isFirstTimeSetup = intent.getBooleanExtra(FirstTimeSetupActivity.IS_FIRST_TIME_SETUP, false)

        setContentView(R.layout.activity_update_user_profile)
        setSupportActionBar(findViewById(R.id.toolbar))

        mDisplayNameTextView = findViewById(R.id.input_display_name)
        mLocationTextView = findViewById(R.id.input_location)
        mProfilePicImageView = findViewById(R.id.profile_pic)

        val btnChangeProfilePic: Button = findViewById(R.id.btn_change_profile_pic)
        btnChangeProfilePic.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT).setType(IMAGE_TYPE_FILTER)
            val title: String = getString(R.string.choose_profile_pic)
            startActivityForResult(Intent.createChooser(intent, title), CHOOSE_PROFILE_PIC_REQUEST_CODE)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && requestCode == CHOOSE_PROFILE_PIC_REQUEST_CODE && data != null) {
            mProfilePicImageView.setImageURI(data.data)
            isProfilePicChanged = true
        }
    }

    override fun onStart() {
        super.onStart()
        if (!isFirstTimeSetup) {
            getUserProfile()
        }
    }

    private fun getUserProfile() {
        val userUid: String = mAuth.currentUser!!.uid
        RealtimeDatabaseManager.getUserProfile(userUid) { userProfile ->
            if (userProfile != null) {
                mDisplayNameTextView.text = userProfile.displayName
                mLocationTextView.text = userProfile.location
                if (StringUtils.isNotBlank(userProfile.profilePicURI)) {
                    Picasso.get().load(userProfile.profilePicURI).into(mProfilePicImageView)
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.toolbar_save, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.toolbar_save) {
            val view: View = findViewById(android.R.id.content)
            Snackbar.make(view, getString(R.string.saving_profile), Snackbar.LENGTH_LONG).show()
            try {
                updateUserProfile {
                    if (isFirstTimeSetup) {
                        startActivity(Intent(applicationContext, MainActivity::class.java))
                    } else {
                        finish()
                    }
                }
            } catch (ex: Exception) {
                val msg: String = getString(R.string.error_saving_profile) + StringUtils.SPACE + ex
                Snackbar.make(view, msg, Snackbar.LENGTH_LONG).show()
            }
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun updateUserProfile(onSuccess: () -> Unit) {
        val userUid: String = mAuth.currentUser!!.uid
        if (isProfilePicChanged) {
            val imageRef: StorageReference = mStorage.reference
                .child("$PROFILE_PIC_PATH/$userUid.jpg")

            val outputStream = ByteArrayOutputStream()
            mProfilePicImageView.drawToBitmap()
                .compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
            val imageData: ByteArray = outputStream.toByteArray()

            imageRef.putBytes(imageData).continueWithTask { task ->
                if (!task.isSuccessful) {
                    task.exception?.let { throw it }
                }
                imageRef.downloadUrl
            }.addOnSuccessListener { uri ->
                updateUserProfile(userUid, uri.toString(), onSuccess)
            }
        } else {
            updateUserProfile(userUid, null, onSuccess)
        }
    }

    private fun updateUserProfile(userUid: String, profilePicUri: String?, onSuccess: () -> Unit) {
        val displayName: String = mDisplayNameTextView.text.toString()
        val location: String = mLocationTextView.text.toString()
        val userProfile = UserProfile(displayName, location, profilePicUri)
        RealtimeDatabaseManager.updateUserProfile(userUid, userProfile, onSuccess)
    }

    companion object {
        private const val IMAGE_TYPE_FILTER = "image/*"
        private const val CHOOSE_PROFILE_PIC_REQUEST_CODE = 1
        private const val PROFILE_PIC_PATH = "profilePics"
    }

}