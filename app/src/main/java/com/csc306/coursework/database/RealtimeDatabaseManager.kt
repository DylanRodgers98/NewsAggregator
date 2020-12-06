package com.csc306.coursework.database

import android.content.Context
import com.csc306.coursework.async.ArticleTitleAnalyser
import com.csc306.coursework.model.Article
import com.google.firebase.database.*
import java.net.URLEncoder

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

    fun likeArticle(userUid: String, article: Article, callback: () -> Unit) {
        val articleURL: String = firebaseEncodeUrl(article.articleURL)

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
        val articleURL: String = firebaseEncodeUrl(article.articleURL)

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
                    userDislikesRef.setValue(mapOf(
                        ARTICLE_URL_PATH to article,
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

    fun getUserDislikedArticles(userUid: String, earliestDislikedAt: Long, valueEventListener: ValueEventListener) {
        mDatabase.getReference(USERS_PATH)
            .child(userUid)
            .child(DISLIKES_PATH)
            .orderByChild(DISLIKED_AT_PATH)
            .startAt(earliestDislikedAt.toDouble())
            .addListenerForSingleValueEvent(valueEventListener)
    }

    fun removeDislikedArticles(userUid: String, articles: MutableList<Article>, doneCallback: (articles: List<Article>) -> Unit) {
        val query: Query = mDatabase.getReference(USERS_PATH)
            .child(userUid)
            .child(DISLIKES_PATH)
            .orderByChild(ARTICLE_URL_PATH)
        iterateRemoveDislikedArticles(query, articles.iterator().withIndex(), articles, doneCallback)
    }

    private fun iterateRemoveDislikedArticles(query: Query, iterator: Iterator<IndexedValue<Article>>, articles: MutableList<Article>, doneCallback: (articles: List<Article>) -> Unit) {
        if (iterator.hasNext()) {
            val article: IndexedValue<Article> = iterator.next()
            val articleURL: String = firebaseEncodeUrl(article.value.articleURL)
            query.equalTo(articleURL)
                .addListenerForSingleValueEvent(ThrowingValueEventListener {
                    if (it.exists()) {
                        articles.removeAt(article.index)
                    }
                    iterateRemoveDislikedArticles(query, iterator, articles, doneCallback)
                })
        } else {
            doneCallback(articles)
        }
    }

    fun getArticleTitleKeywords(articles: List<Article>, context: Context, doneCallback: (articles: List<Article>) -> Unit) {
        iterateArticleTitleKeywords(articles.iterator(), mutableListOf(), context, doneCallback)
    }

    private fun iterateArticleTitleKeywords(iterator: Iterator<Article>, articles: MutableList<Article>, context: Context, doneCallback: (articles: List<Article>) -> Unit) {
        if (iterator.hasNext()) {
            val article: Article = iterator.next()
            if (article.titleKeywords == null) {
                val articleURL: String = firebaseEncodeUrl(article.articleURL)
                val articleRef: DatabaseReference = mDatabase.getReference(ARTICLES_PATH).child(articleURL)
                articleRef.addListenerForSingleValueEvent(ThrowingValueEventListener {
                    if (it.exists()) {
                        val mapStringAnyType = object : GenericTypeIndicator<Map<String, Any>>() {}
                        val articleData: Map<String, Any> = it.getValue(mapStringAnyType)!!
                        val titleKeywords: Map<String, Double> = articleData[TITLE_KEYWORDS_PATH] as Map<String, Double>
                        article.titleKeywords = titleKeywords
                    } else {
                        article.titleKeywords = ArticleTitleAnalyser(context).execute(article).get()
                        articleRef.setValue(article)
                    }
                    articles.add(article)
                    iterateArticleTitleKeywords(iterator, articles, context, doneCallback)
                })
            } else {
                iterateArticleTitleKeywords(iterator, articles, context, doneCallback)
            }
        } else {
            doneCallback(articles)
        }
    }

    private fun firebaseEncodeUrl(url: String): String {
        return URLEncoder.encode(url, Charsets.UTF_8.toString())
            .replace("$", "%24")
            .replace("#", "%23")
            .replace(".", "%2E")
            .replace("[", "%5B")
            .replace("]", "%5D")
    }

    companion object {
        private const val USERS_PATH = "users"
        private const val FOLLOWING_PATH = "following"
        private const val CATEGORIES_PATH = "categories"
        private const val LIKES_PATH = "likes"
        private const val LIKED_AT_PATH = "likedAt"
        private const val DISLIKES_PATH = "dislikes"
        private const val DISLIKED_AT_PATH = "dislikedAt"
        const val ARTICLE_URL_PATH = "articleURL"
        private const val ARTICLES_PATH = "articles"
        private const val TITLE_KEYWORDS_PATH = "titleKeywords"
    }

}