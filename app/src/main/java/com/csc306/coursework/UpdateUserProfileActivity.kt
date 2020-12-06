package com.csc306.coursework

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.drawToBitmap
import com.csc306.coursework.database.RealtimeDatabaseManager
import com.csc306.coursework.model.UserProfile
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.squareup.picasso.Picasso
import org.apache.commons.lang3.StringUtils
import java.io.ByteArrayOutputStream
import java.lang.Exception

class UpdateUserProfileActivity : AppCompatActivity() {

    private lateinit var mAuth: FirebaseAuth

    private lateinit var mStorage: FirebaseStorage

    private var isFirstTimeSetup: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mAuth = FirebaseAuth.getInstance()
        mStorage = FirebaseStorage.getInstance()
        isFirstTimeSetup = intent.getBooleanExtra(FirstTimeSetupActivity.IS_FIRST_TIME_SETUP, false)

        setContentView(R.layout.activity_update_user_profile)
        setSupportActionBar(findViewById(R.id.toolbar))

        val btnChangeProfilePic: Button = findViewById(R.id.btn_change_profile_pic)
        btnChangeProfilePic.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT).setType(IMAGE_TYPE_FILTER)
            val title: String = getString(R.string.choose_profile_pic)
            startActivityForResult(Intent.createChooser(intent, title), CHOOSE_PROFILE_PIC_REQUEST_CODE)
        }
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
            Snackbar.make(item.actionView, getString(R.string.saving_profile), Snackbar.LENGTH_LONG).show()
            updateUserProfile({
                if (isFirstTimeSetup) {
                    startActivity(Intent(applicationContext, MainActivity::class.java))
                } else {
                    finish()
                }
            }, { ex ->
                val msg: String = getString(R.string.error_saving_profile) + StringUtils.SPACE + ex
                Snackbar.make(item.actionView, msg, Snackbar.LENGTH_LONG).show()
            })
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && requestCode == CHOOSE_PROFILE_PIC_REQUEST_CODE && data != null) {
            val profilePicImageView: ImageView = findViewById(R.id.profile_pic)
            profilePicImageView.setImageURI(data.data)
        }
    }

    private fun getUserProfile() {
        val userUid: String = mAuth.currentUser!!.uid
        RealtimeDatabaseManager.getUserProfile(userUid) { userProfile ->
            if (userProfile != null) {
                val displayNameView: TextView = findViewById(R.id.input_display_name)
                displayNameView.text = userProfile.displayName
                val locationTextView: TextView = findViewById(R.id.input_location)
                locationTextView.text = userProfile.location
                val profilePicImageView: ImageView = findViewById(R.id.profile_pic)
                Picasso.get().load(userProfile.profilePicURL).into(profilePicImageView)
            }
        }
    }

    private fun updateUserProfile(onSuccess: () -> Unit, onFailure: (exception: Exception) -> Unit) {
        val userUid: String = mAuth.currentUser!!.uid

        val profilePicImageView: ImageView = findViewById(R.id.profile_pic)
        val outputStream = ByteArrayOutputStream()
        profilePicImageView.drawToBitmap().compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
        val imageData: ByteArray = outputStream.toByteArray()

        val imageRef: StorageReference = mStorage.reference.child("$PROFILE_PIC_PATH/$userUid.jpg")
        imageRef.putBytes(imageData).continueWithTask { task ->
            if (!task.isSuccessful) {
                task.exception?.let { throw it }
            }
            imageRef.downloadUrl
        } .addOnSuccessListener { uri ->
            val displayNameView: TextView = findViewById(R.id.input_display_name)
            val displayName: String = displayNameView.text.toString()
            val locationTextView: TextView = findViewById(R.id.input_location)
            val location: String = locationTextView.text.toString()
            val userProfile = UserProfile(displayName, location, uri.toString())
            RealtimeDatabaseManager.updateUserProfile(userUid, userProfile, onSuccess)
        } .addOnFailureListener { ex ->
            onFailure(ex)
        }
    }

    companion object {
        private const val IMAGE_TYPE_FILTER = "image/*"
        private const val CHOOSE_PROFILE_PIC_REQUEST_CODE = 1
        private const val PROFILE_PIC_PATH = "profilePics"
    }

}