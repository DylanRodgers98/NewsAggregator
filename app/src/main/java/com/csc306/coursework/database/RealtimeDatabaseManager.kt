package com.csc306.coursework.database

import android.content.Context
import com.csc306.coursework.model.Article
import com.csc306.coursework.model.ArticleTitleAnalyser
import com.google.firebase.database.*
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

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

    fun likeArticle(userUid: String, article: Article, context: Context) {
        likeOrDislikeArticle(userUid, article, context, true)
    }

    fun dislikeArticle(userUid: String, article: Article, context: Context) {
        likeOrDislikeArticle(userUid, article, context, false)
    }

    private fun likeOrDislikeArticle(userUid: String, article: Article, context: Context, isLike: Boolean) {
        val articleUrl = firebaseDatabasePathEncode(article.articleURL)

        val likeOrDislikePath = if (isLike) LIKES_PATH else DISLIKES_PATH
        val likeOrDislikeRef: DatabaseReference = mDatabase.getReference(USERS_PATH)
            .child(userUid)
            .child(likeOrDislikePath)

        likeOrDislikeRef.orderByValue().equalTo(articleUrl)
            .addListenerForSingleValueEvent(ThrowingValueEventListener { likeOrDislikeSnapshot ->
                if (!likeOrDislikeSnapshot.exists()) {
                    likeOrDislikeRef.push().setValue(articleUrl)

                    val articleRef: DatabaseReference = mDatabase.getReference(ARTICLES_PATH).child(articleUrl)
                    articleRef.addListenerForSingleValueEvent(ThrowingValueEventListener { articleSnapshot ->
                        if (articleSnapshot.exists()) {
                            val mapStringAnyType = object : GenericTypeIndicator<Map<String, Any>>() {}
                            val data: Map<String, Any>? = articleSnapshot.getValue(mapStringAnyType)
                            val titleKeywords = data?.get(TITLE_KEYWORDS_PATH) as Map<String, Double>
                            updateSwipedKeywords(userUid, titleKeywords, isLike)
                        } else {
                            if (article.titleKeywords == null) {
                                ArticleTitleAnalyser(context).execute(article).get()
                            }
                            articleRef.setValue(article)
                            updateSwipedKeywords(userUid, article.titleKeywords!!, isLike)
                        }
                    })
                }
            })
    }

    private fun firebaseDatabasePathEncode(str: String): String {
        val urlEncoded = URLEncoder.encode(str, StandardCharsets.UTF_8.toString())
        return urlEncoded.replace(".", "%2E")
            .replace("#", "%23")
            .replace("$", "%24")
            .replace("[", "%5B")
            .replace("]", "%5D")
    }

    private fun firebaseDatabasePathDecode(str: String): String {
        val percentDecoded = str.replace("%2E", ".")
            .replace("%23", "#")
            .replace("%24", "$")
            .replace("%5B", "[")
            .replace("%5D", "]")
        return URLDecoder.decode(percentDecoded, StandardCharsets.UTF_8.toString())
    }

    private fun updateSwipedKeywords(userUid: String, keywords: Map<String, Double>, isPositive: Boolean) {
        keywords.entries.forEach {
            val keyword = it.key
            val salience = it.value
            val keywordRef: DatabaseReference = mDatabase.getReference(USERS_PATH)
                .child(userUid)
                .child(SWIPED_KEYWORDS_PATH)
                .child(keyword)

            keywordRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var totalSalience = if (isPositive) salience else -salience
                    var count = 1L

                    if (snapshot.exists()) {
                        val data = snapshot.value as Map<String, Any>
                        totalSalience += data[TOTAL_SALIENCE_PATH] as Double
                        count += data[COUNT_PATH] as Long
                    }

                    keywordRef.setValue(mapOf(
                        TOTAL_SALIENCE_PATH to totalSalience,
                        COUNT_PATH to count
                    ))
                }

                override fun onCancelled(error: DatabaseError) {
                    throw error.toException()
                }
            })
        }
    }

    companion object {
        private const val USERS_PATH = "users"
        private const val FOLLOWING_PATH = "following"
        private const val CATEGORIES_PATH = "categories"
        private const val LIKES_PATH = "likes"
        private const val DISLIKES_PATH = "dislikes"
        private const val ARTICLES_PATH = "articles"
        private const val SWIPED_KEYWORDS_PATH = "swipedKeywords"
        private const val TOTAL_SALIENCE_PATH = "totalSalience"
        private const val COUNT_PATH = "count"
        private const val TITLE_KEYWORDS_PATH = "titleKeywords"
    }

}