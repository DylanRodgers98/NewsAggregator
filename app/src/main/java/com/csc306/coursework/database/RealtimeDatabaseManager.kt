package com.csc306.coursework.database

import com.csc306.coursework.model.Article
import com.google.firebase.database.*

class RealtimeDatabaseManager {

    private var mDatabase: FirebaseDatabase = FirebaseDatabase.getInstance()

    fun setUserFollowingCategories(userUid: String, categoriesFollowing: List<String>) {
        mDatabase.getReference(USERS_PATH)
            .child(userUid)
            .child(FOLLOWING_PATH)
            .child(CATEGORIES_PATH)
            .setValue(categoriesFollowing)
    }

    fun getUserFollowingCategories(userUid: String, valueEventListener: ValueEventListener) {
        mDatabase.getReference(USERS_PATH)
            .child(userUid)
            .child(FOLLOWING_PATH)
            .child(CATEGORIES_PATH)
            .addListenerForSingleValueEvent(valueEventListener)
    }

    fun likeArticle(userUid: String, article: Article) {
        likeOrDislikeArticle(userUid, article, LIKES_PATH)
    }

    fun dislikeArticle(userUid: String, article: Article) {
        likeOrDislikeArticle(userUid, article, DISLIKES_PATH)
    }

    private fun likeOrDislikeArticle(userUid: String, article: Article, likeOrDislikePath: String) {
        val databaseReference = mDatabase.getReference(USERS_PATH)
            .child(userUid)
            .child(likeOrDislikePath)

        getArticleKeyOrSave(article) { key ->
            databaseReference
                .orderByValue()
                .equalTo(key)
                .limitToFirst(1)
                .addListenerForSingleValueEvent(DoIfNotExists {
                    databaseReference.push().setValue(key)
                })
        }
    }

    private fun getArticleKeyOrSave(article: Article, cb: (key: String) -> Unit) {
        val keyRetriever: ValueEventListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val key: String = if (snapshot.exists()) {
                    if (snapshot.childrenCount != 1L) {
                        throw IllegalStateException("Couldn't get key for article from Realtime Database")
                    }
                    snapshot.children.first().key ?: throw IllegalStateException("Couldn't get key for article from Realtime Database")
                } else {
                    saveArticle(article)
                }
                cb(key)
            }

            override fun onCancelled(error: DatabaseError) {
                throw error.toException()
            }
        }

        mDatabase.getReference(ARTICLES_PATH)
            .orderByChild(ARTICLE_URL_KEY)
            .equalTo(article.articleURL)
            .limitToFirst(1)
            .addListenerForSingleValueEvent(keyRetriever)
    }

    private fun saveArticle(article: Article): String {
        val databaseReference: DatabaseReference = mDatabase.getReference(ARTICLES_PATH)
        val key: String = databaseReference.push().key
            ?: throw IllegalStateException("Couldn't push key for article to Realtime Database")
        databaseReference.child(key).setValue(article)
        return key
    }

    inner class DoIfNotExists(val cb: () -> Unit) : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            if (!snapshot.exists()) {
                cb()
            }
        }

        override fun onCancelled(error: DatabaseError) {
            throw error.toException()
        }
    }

    companion object {
        private const val USERS_PATH = "users"
        private const val FOLLOWING_PATH = "following"
        private const val CATEGORIES_PATH = "categories"
        private const val LIKES_PATH = "likes"
        private const val DISLIKES_PATH = "dislikes"
        private const val ARTICLES_PATH = "articles"
        private const val ARTICLE_URL_KEY = "articleURL"
    }

}