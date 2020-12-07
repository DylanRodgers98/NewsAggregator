package com.csc306.coursework.database

import android.content.Context
import com.csc306.coursework.async.ArticleTitleAnalyser
import com.csc306.coursework.model.Article
import com.csc306.coursework.model.UserProfile
import com.google.firebase.database.*
import java.net.URLDecoder
import java.net.URLEncoder
import java.util.*
import kotlin.collections.ArrayList

object RealtimeDatabaseManager {

    private const val USERS_PATH = "users"
    private const val FOLLOWING_PATH = "following"
    private const val CATEGORIES_PATH = "categories"
    private const val LIKES_PATH = "likes"
    private const val LIKED_AT_PATH = "likedAt"
    private const val DISLIKES_PATH = "dislikes"
    private const val DISLIKED_AT_PATH = "dislikedAt"
    private const val ARTICLE_URL_PATH = "articleURL"
    private const val ARTICLES_PATH = "articles"
    private const val TITLE_KEYWORDS_PATH = "titleKeywords"
    private const val USER_PROFILE_PATH = "profile"
    private const val ARTICLE_LIMIT = 20
    private const val LIKED_ARTICLE_LIMIT = 5
    private val MAP_STRING_STRING_TYPE = object : GenericTypeIndicator<Map<String, String>>() { }
    private val MAP_STRING_ANY_TYPE = object : GenericTypeIndicator<Map<String, Any>>() { }

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

    fun likeArticle(userUid: String, article: Article, callback: () -> Unit) {
        val articleURL: String = firebaseEncode(article.articleURL)

        val articleRef: DatabaseReference = mDatabase.getReference(ARTICLES_PATH).child(articleURL)
        articleRef.addListenerForSingleValueEvent(ThrowingValueEventListener {
            if (!it.exists()) {
                articleRef.setValue(article)
            }
        })

        val userRef: DatabaseReference = mDatabase.getReference(USERS_PATH).child(userUid)

        val userLikesRef: DatabaseReference = userRef.child(LIKES_PATH)
        userLikesRef.orderByChild(ARTICLE_URL_PATH).equalTo(articleURL)
            .addListenerForSingleValueEvent(ThrowingValueEventListener {
                if (!it.exists()) {
                    userLikesRef.push().setValue(mapOf(
                        ARTICLE_URL_PATH to articleURL,
                        LIKED_AT_PATH to System.currentTimeMillis()
                    ))
                    callback()
                }
            })

        val userDislikesRef: DatabaseReference = userRef.child(DISLIKES_PATH)
        userDislikesRef.orderByChild(ARTICLES_PATH).equalTo(articleURL)
            .addListenerForSingleValueEvent(ThrowingValueEventListener {
                if (it.exists()) {
                    userDislikesRef.child(it.key!!).removeValue()
                }
            })
    }

    fun dislikeArticle(userUid: String, article: Article, callback: () -> Unit) {
        val articleURL: String = firebaseEncode(article.articleURL)

        val articleRef: DatabaseReference = mDatabase.getReference(ARTICLES_PATH).child(articleURL)
        articleRef.addListenerForSingleValueEvent(ThrowingValueEventListener {
            if (!it.exists()) {
                articleRef.setValue(article)
            }
        })

        val userRef: DatabaseReference = mDatabase.getReference(USERS_PATH).child(userUid)

        val userDislikesRef: DatabaseReference = userRef.child(DISLIKES_PATH)
        userDislikesRef.orderByChild(ARTICLE_URL_PATH).equalTo(articleURL)
            .addListenerForSingleValueEvent(ThrowingValueEventListener {
                if (!it.exists()) {
                    userDislikesRef.push().setValue(mapOf(
                        ARTICLE_URL_PATH to articleURL,
                        DISLIKED_AT_PATH to System.currentTimeMillis()
                    ))
                    callback()
                }
            })

        val userLikesRef: DatabaseReference = userRef.child(LIKES_PATH)
        userLikesRef.orderByChild(ARTICLES_PATH).equalTo(articleURL)
            .addListenerForSingleValueEvent(ThrowingValueEventListener {
                if (it.exists()) {
                    userLikesRef.child(it.key!!).removeValue()
                }
            })
    }

    fun removeDislikedArticlesFromList(userUid: String, articles: MutableList<Article>, doneCallback: (articles: MutableList<Article>) -> Unit) {
        val query: Query = mDatabase.getReference(USERS_PATH)
            .child(userUid)
            .child(DISLIKES_PATH)
            .orderByChild(ARTICLE_URL_PATH)

        removeDislikedArticlesFromList(query, articles.iterator().withIndex(), articles, doneCallback)
    }

    private fun removeDislikedArticlesFromList(query: Query, iterator: Iterator<IndexedValue<Article>>, articles: MutableList<Article>, doneCallback: (articles: MutableList<Article>) -> Unit) {
        if (iterator.hasNext()) {
            val newArticles: MutableList<Article> = articles.toMutableList()
            val article: IndexedValue<Article> = iterator.next()
            val articleURL: String = firebaseEncode(article.value.articleURL)
            query.equalTo(articleURL)
                .addListenerForSingleValueEvent(ThrowingValueEventListener {
                    if (it.exists()) {
                        newArticles.removeAt(article.index)
                    }
                    removeDislikedArticlesFromList(query, iterator, newArticles, doneCallback)
                })
        } else {
            doneCallback(articles)
        }
    }

    fun getArticleTitleKeywords(articles: List<Article>, context: Context, doneCallback: (articles: MutableList<Article>) -> Unit) {
        getArticleTitleKeywords(articles.iterator(), mutableListOf(), context, doneCallback)
    }

    private fun getArticleTitleKeywords(iterator: Iterator<Article>, articles: MutableList<Article>, context: Context, doneCallback: (articles: MutableList<Article>) -> Unit) {
        if (iterator.hasNext()) {
            val article: Article = iterator.next()
            if (article.titleKeywords == null) {
                val articleURL: String = firebaseEncode(article.articleURL)
                val articleRef: DatabaseReference = mDatabase.getReference(ARTICLES_PATH).child(articleURL)
                articleRef.addListenerForSingleValueEvent(ThrowingValueEventListener {
                    if (it.exists()) {
                        val articleData: Map<String, Any> = it.getValue(MAP_STRING_ANY_TYPE)!!
                        val titleKeywords: Map<String, Double>? = articleData[TITLE_KEYWORDS_PATH] as Map<String, Double>?
                        article.titleKeywords = titleKeywords
                    } else {
                        val titleKeywords: Map<String, Double>? = ArticleTitleAnalyser(context).execute(article).get()
                        // encode keys to remove illegal characters for storing in Firebase
                        article.titleKeywords = titleKeywords?.mapKeys { entry -> firebaseEncode(entry.key) }
                        articleRef.setValue(article)
                    }
                    articles.add(article)
                    getArticleTitleKeywords(iterator, articles, context, doneCallback)
                })
            } else {
                getArticleTitleKeywords(iterator, articles, context, doneCallback)
            }
        } else {
            // decode keys to reinstate illegal characters for storing in Firebase
            articles.forEach {
                it.titleKeywords = it.titleKeywords?.mapKeys { entry -> firebaseDecode(entry.key) }
            }
            doneCallback(articles)
        }
    }

    private fun firebaseEncode(str: String): String {
        return URLEncoder.encode(str, Charsets.UTF_8.toString())
            .replace("/", "%2F")
            .replace(".", "%2E")
            .replace("#", "%23")
            .replace("$", "%24")
            .replace("[", "%5B")
            .replace("]", "%5D")
    }

    private fun firebaseDecode(str: String): String {
        val percentDecoded: String = str
            .replace("%2F", "/")
            .replace("%2E", ".")
            .replace("%23", "#")
            .replace("%24", "$")
            .replace("%5B", "[")
            .replace("%5D", "]")
        return URLDecoder.decode(percentDecoded, Charsets.UTF_8.toString())
    }

    fun updateUserProfile(userUid: String, userProfile: UserProfile, onComplete: () -> Unit) {
        mDatabase.getReference(USERS_PATH)
            .child(userUid)
            .child(USER_PROFILE_PATH)
            .setValue(userProfile)
            .addOnCompleteListener {
                if (it.isSuccessful) {
                    onComplete()
                }
            }
    }

    fun getUserProfile(userUid: String, doneCallback: (userProfile: UserProfile?) -> Unit) {
        mDatabase.getReference(USERS_PATH)
            .child(userUid)
            .child(USER_PROFILE_PATH)
            .addListenerForSingleValueEvent(ThrowingValueEventListener {
                val userProfile: UserProfile? = it.getValue(UserProfile::class.java)
                doneCallback(userProfile)
            })
    }

    fun getUserLikes(userUid: String, doneCallback: (articles: MutableList<Article>) -> Unit) {
        getUserLikes(userUid, ARTICLE_LIMIT, doneCallback)
    }

    private fun getUserLikes(userUid: String, limit: Int, doneCallback: (articles: MutableList<Article>) -> Unit) {
        mDatabase.getReference(USERS_PATH)
            .child(userUid)
            .child(LIKES_PATH)
            .orderByChild(LIKED_AT_PATH)
            .limitToFirst(limit)
            .addListenerForSingleValueEvent(ThrowingValueEventListener {
                val articleURLs: List<String> = it.children.map { snapshot ->
                    snapshot.getValue(MAP_STRING_ANY_TYPE)!![ARTICLE_URL_PATH] as String
                }.reversed() // list ordered by likedAt in ascending order, so need to reverse
                getArticlesByURLs(articleURLs, doneCallback)
            })
    }

    private fun getArticlesByURLs(articleURLs: List<String>, doneCallback: (articles: MutableList<Article>) -> Unit) {
        getArticlesByURLs(articleURLs.iterator(), mutableListOf(), doneCallback)
    }

    private fun getArticlesByURLs(iterator: Iterator<String>, articles: MutableList<Article>, doneCallback: (articles: MutableList<Article>) -> Unit) {
        if (iterator.hasNext()) {
            val articleURL: String = iterator.next()
            mDatabase.getReference(ARTICLES_PATH)
                .child(articleURL)
                .addListenerForSingleValueEvent(ThrowingValueEventListener {
                    if (it.exists()) {
                        val article: Article = it.getValue(Article::class.java)!!
                        articles.add(article)
                    }
                    getArticlesByURLs(iterator, articles, doneCallback)
                })
        } else {
            doneCallback(articles)
        }
    }

    fun isFollowingUser(userUid: String, userUidToQuery: String, doneCallback: (isFollowing: Boolean) -> Unit) {
        mDatabase.getReference(USERS_PATH)
            .child(userUid)
            .child(FOLLOWING_PATH)
            .child(USERS_PATH)
            .orderByValue()
            .equalTo(userUidToQuery)
            .addListenerForSingleValueEvent(ThrowingValueEventListener {
                doneCallback(it.exists())
            })
    }

    fun followUser(userUid: String, userToFollowUid: String, onSuccess: () -> Unit) {
        val userFollowingUsersRef: DatabaseReference = mDatabase.getReference(USERS_PATH)
            .child(userUid)
            .child(FOLLOWING_PATH)
            .child(USERS_PATH)

        userFollowingUsersRef.orderByValue().equalTo(userToFollowUid)
            .addListenerForSingleValueEvent(ThrowingValueEventListener {
                if (!it.exists()) {
                    userFollowingUsersRef.push().setValue(userToFollowUid)
                        .addOnSuccessListener { onSuccess() }
                }
            })
    }

    fun unfollowUser(userUid: String, userToFollowUid: String, onSuccess: () -> Unit) {
        val userFollowingUsersRef: DatabaseReference = mDatabase.getReference(USERS_PATH)
            .child(userUid)
            .child(FOLLOWING_PATH)
            .child(USERS_PATH)

        userFollowingUsersRef.orderByValue().equalTo(userToFollowUid)
            .addListenerForSingleValueEvent(ThrowingValueEventListener {
                if (it.exists()) {
                    val followingData: Map<String, String> = it.getValue(MAP_STRING_STRING_TYPE)!!
                    if (followingData.size == 1) {
                        val key: String = followingData.keys.first()
                        userFollowingUsersRef.child(key).removeValue()
                            .addOnSuccessListener { onSuccess() }
                    }
                }
            })
    }

    fun getArticlesLikedByFollowedUsers(userUid: String, doneCallback: (articles: MutableList<Article>) -> Unit) {
        mDatabase.getReference(USERS_PATH)
            .child(userUid)
            .child(FOLLOWING_PATH)
            .child(USERS_PATH)
            .addListenerForSingleValueEvent(ThrowingValueEventListener {
                val followedUserUids: MutableList<String> = mutableListOf()
                it.children.forEach { snapshot ->
                    val data: Map<String, String> = snapshot.getValue(MAP_STRING_STRING_TYPE)!!
                    if (data.size == 1) {
                        followedUserUids.add(data.values.first())
                    }
                }
                getArticlesLikedByUsers(followedUserUids, doneCallback)
            })
    }

    private fun getArticlesLikedByUsers(userUids: List<String>, doneCallback: (articles: MutableList<Article>) -> Unit) {
        getArticlesLikedByUsers(userUids.iterator(), mutableListOf(), doneCallback)
    }

    private fun getArticlesLikedByUsers(iterator: Iterator<String>, articles: MutableList<Article>, doneCallback: (articles: MutableList<Article>) -> Unit) {
        if (iterator.hasNext()) {
            val userUid: String = iterator.next()
            getUserLikes(userUid, LIKED_ARTICLE_LIMIT) { likedArticles ->
                likedArticles.forEach { article ->
                    val matchingArticle: Article? = articles.find { it == article }
                    if (matchingArticle != null) {
                        matchingArticle.likedBy(userUid)
                    } else {
                        article.likedBy(userUid)
                        articles.add(article)
                    }
                }
                getArticlesLikedByUsers(iterator, articles, doneCallback)
            }
        } else {
            doneCallback(articles)
        }
    }

}