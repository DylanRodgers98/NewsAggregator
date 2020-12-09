package com.csc306.coursework.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.csc306.coursework.R
import com.csc306.coursework.scheduler.NewArticlesScheduler
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth

class AuthenticationActivity : AppCompatActivity() {

    private lateinit var mAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mAuth = FirebaseAuth.getInstance()
        setContentView(R.layout.activity_authentication)
    }

    override fun onStart() {
        super.onStart()
        if (mAuth.currentUser != null) {
            startActivity(Intent(applicationContext, MainActivity::class.java))
        } else {
            val logInButton: Button = findViewById(R.id.btn_login)
            logInButton.setOnClickListener { view -> logIn(view) }
            val signUpButton: Button = findViewById(R.id.btn_sign_up)
            signUpButton.setOnClickListener { view -> signUp(view) }
        }
    }

    private fun logIn(view: View) {
        closeKeyboard()
        val email: String = getEmailTextView().text.toString()
        val password: String = getPasswordTextView().text.toString()
        doIfAuthNonEmpty(email, password, view) {
            mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(getOnCompleteListener(view, getString(R.string.failed_login)))
        }
    }

    private fun signUp(view: View) {
        closeKeyboard()
        val email: String = getEmailTextView().text.toString()
        val password: String = getPasswordTextView().text.toString()
        doIfAuthNonEmpty(email, password, view) {
            mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(getOnCompleteListener(view, getString(R.string.failed_sign_up)))
        }
    }

    private fun doIfAuthNonEmpty(email: String, password: String, view: View, function: () -> Unit) {
        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Snackbar.make(view, getString(R.string.empty_auth), Snackbar.LENGTH_LONG).show()
        } else {
            function()
        }
    }

    private fun closeKeyboard() {
        val inputManager: InputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputManager.hideSoftInputFromWindow(currentFocus?.windowToken, InputMethodManager.HIDE_NOT_ALWAYS)
    }

    private fun getOnCompleteListener(view: View, errorMessage: String): OnCompleteListener<AuthResult> {
        return OnCompleteListener {
            if (it.isSuccessful) {
                clearTextBoxes()
                val isNewUser: Boolean = it.result!!.additionalUserInfo!!.isNewUser
                if (!isNewUser) {
                    NewArticlesScheduler.start(this)
                }
                val activity = if (isNewUser) FirstTimeSetupActivity::class.java else MainActivity::class.java
                startActivity(Intent(applicationContext, activity))
            } else {
                Snackbar.make(view, errorMessage, Snackbar.LENGTH_LONG).show()
            }
        }
    }

    private fun clearTextBoxes() {
        getEmailTextView().text = ""
        getPasswordTextView().text = ""
    }

    private fun getEmailTextView(): TextView {
        return findViewById(R.id.input_email)
    }

    private fun getPasswordTextView(): TextView {
        return findViewById(R.id.input_password)
    }

}