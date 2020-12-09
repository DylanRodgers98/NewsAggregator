package com.csc306.coursework.scheduler

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.JobIntentService
import androidx.core.app.NotificationCompat
import com.csc306.coursework.R
import com.csc306.coursework.activity.MainActivity
import com.csc306.coursework.database.DatabaseManager
import com.csc306.coursework.database.RealtimeDatabaseManager
import com.csc306.coursework.database.ThrowingValueEventListener
import com.csc306.coursework.model.Article
import com.csc306.coursework.newsapi.NewsAPIService
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.GenericTypeIndicator

class NewArticlesService : JobIntentService() {

    private lateinit var mAuth: FirebaseAuth

    private lateinit var mDatabaseManager: DatabaseManager

    private lateinit var mNewsApi: NewsAPIService

    private lateinit var mNotificationManager: NotificationManager

    override fun onCreate() {
        super.onCreate()
        mAuth = FirebaseAuth.getInstance()
        mDatabaseManager = DatabaseManager(this)
        mNewsApi = NewsAPIService(this)
        mNotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    override fun onHandleWork(intent: Intent) {
        val userUid: String? = mAuth.currentUser?.uid
        if (userUid != null) {
            val sharedPreferences = getSharedPreferences(SERVICE_PREFERENCES, Context.MODE_PRIVATE)
            val lastUpdated: Long = sharedPreferences.getLong(LAST_UPDATED, DEFAULT_LAST_UPDATED)
            if (lastUpdated != DEFAULT_LAST_UPDATED) {
                RealtimeDatabaseManager.getUserFollowingCategories(userUid, ThrowingValueEventListener {
                    val categories: Array<String> = it.getValue(STRING_LIST_TYPE)?.toTypedArray()
                        ?: emptyArray()
                    val sourceIds: String = mDatabaseManager.getSourceIdsForCategories(categories)
                    val articles: MutableList<Article> = mNewsApi.getTopHeadlines(sourceIds)
                    sharedPreferences.edit().putLong(LAST_UPDATED, System.currentTimeMillis()).apply()
                    articles.removeIf { article -> article.publishDateMillis < lastUpdated }
                    if (articles.size > 0) {
                        publishNotification(articles.size)
                    }
                })
            }
        }
    }

    private fun publishNotification(articlesSize: Int) {
        val intent = Intent(this, MainActivity::class.java)
            .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        val pendingIntent: PendingIntent = PendingIntent.getActivity(this, REQUEST_CODE, intent, PENDING_INTENT_FLAGS)
        val notification: Notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle(getString(R.string.new_articles_notification_title))
            .setContentText(getString(R.string.new_articles_notification_text))
            .setNumber(articlesSize)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        mNotificationManager.notify(NOTIFICATION_ID, notification)
    }

    companion object {
        private const val DEFAULT_LAST_UPDATED = -1L
        private const val NOTIFICATION_ID = 0
        private const val REQUEST_CODE = 0
        private const val PENDING_INTENT_FLAGS = 0
        private const val CHANNEL_ID = "NEW_ARTICLES_NOTIFICATION_CHANNEL"
        const val SERVICE_PREFERENCES = "NEW_ARTICLES_SERVICE"
        const val LAST_UPDATED = "LAST_UPDATED"
        private val STRING_LIST_TYPE = object : GenericTypeIndicator<List<String>>() {}
    }

}