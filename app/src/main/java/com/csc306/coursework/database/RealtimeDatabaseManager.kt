package com.csc306.coursework.database

import android.content.Context
import com.csc306.coursework.async.ArticleTitleAnalyser
import com.csc306.coursework.model.Article
import com.csc306.coursework.model.Category
import com.csc306.coursework.model.Source
import com.csc306.coursework.model.UserProfile
import com.google.firebase.database.*
import java.net.URLDecoder
import java.net.URLEncoder

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
    private const val USER_PROFILE_PATH = "profile"
    private const val KEYWORD_LIKABILITY_PATH = "keywordLikability"
    private const val TOTAL_SALIENCE_PATH = "totalSalience"
    private const val COUNT_PATH = "count"
    private const val SOURCES_PATH = "sources"
    private const val NAME_PATH = "name"
    private const val PROFILE_PATH = "profile"
    private const val DISPLAY_NAME_PATH = "displayName"
    private const val LOCATION_PATH = "location"
    private const val PROFILE_PIC_URI_PATH = "profilePicURI"
    private const val ID_PATH = "id"
    private const val UPDATED_AT_PATH = "updatedAt"
    private const val ARTICLE_LIMIT = 20
    private const val LIKED_ARTICLE_LIMIT = 5
    private const val MAX_PATH_LENGTH = 400
    private const val SOURCES_UPDATE_INTERVAL = 1000 * 60 * 60 * 24 // 24 hours
    private val MAP_STRING_STRING_TYPE = object : GenericTypeIndicator<Map<String, String>>() { }
    private val MAP_STRING_ANY_TYPE = object : GenericTypeIndicator<Map<String, Any>>() { }
    private val MAP_STRING_MAP_STRING_ANY_TYPE = object : GenericTypeIndicator<Map<String, Map<String, Any>>>() { }
    private val MAP_STRING_DOUBLE_TYPE = object : GenericTypeIndicator<Map<String, Double>>() { }

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
        val articleURL: String = firebaseEncode(article.articleURL)

        val articleRef: DatabaseReference = mDatabase.getReference(ARTICLES_PATH).child(articleURL)
        articleRef.addListenerForSingleValueEvent(ThrowingValueEventListener {
            if (!it.exists()) {
                articleRef.setValue(article.toArticleDTO())
            }
        })

        val userRef: DatabaseReference = mDatabase.getReference(USERS_PATH).child(userUid)

        updateLikabilityForKeywords(article, context, userRef, true, false)

        val userLikesRef: DatabaseReference = userRef.child(LIKES_PATH)
        userLikesRef.orderByChild(ARTICLE_URL_PATH).equalTo(articleURL)
            .addListenerForSingleValueEvent(ThrowingValueEventListener {
                if (!it.exists()) {
                    userLikesRef.push().setValue(mapOf(
                        ARTICLE_URL_PATH to articleURL,
                        LIKED_AT_PATH to System.currentTimeMillis()
                    ))
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

    fun undoLike(userUid: String, article: Article, context: Context) {
        val userRef: DatabaseReference = mDatabase.getReference(USERS_PATH).child(userUid)
        val userLikesRef: DatabaseReference = userRef.child(LIKES_PATH)
        userLikesRef.orderByChild(ARTICLE_URL_PATH).equalTo(firebaseEncode(article.articleURL))
            .addListenerForSingleValueEvent(ThrowingValueEventListener {
                if (it.exists()) {
                    val dislikeKey: String = it.getValue(MAP_STRING_ANY_TYPE)!!.keys.first()
                    userLikesRef.child(dislikeKey).removeValue()
                    updateLikabilityForKeywords(article, context, userRef, false, true)
                }
            })
    }

    fun undoDislike(userUid: String, article: Article, context: Context) {
        val userRef: DatabaseReference = mDatabase.getReference(USERS_PATH).child(userUid)
        val userLikesRef: DatabaseReference = userRef.child(DISLIKES_PATH)
        userLikesRef.orderByChild(ARTICLE_URL_PATH).equalTo(firebaseEncode(article.articleURL))
            .addListenerForSingleValueEvent(ThrowingValueEventListener {
                if (it.exists()) {
                    val likeKey: String = it.getValue(MAP_STRING_ANY_TYPE)!!.keys.first()
                    userLikesRef.child(likeKey).removeValue()
                    updateLikabilityForKeywords(article, context, userRef, true, false)
                }
            })
    }

    fun dislikeArticle(userUid: String, article: Article, context: Context) {
        val articleURL: String = firebaseEncode(article.articleURL)

        val articleRef: DatabaseReference = mDatabase.getReference(ARTICLES_PATH).child(articleURL)
        articleRef.addListenerForSingleValueEvent(ThrowingValueEventListener {
            if (!it.exists()) {
                articleRef.setValue(article.toArticleDTO())
            }
        })

        val userRef: DatabaseReference = mDatabase.getReference(USERS_PATH).child(userUid)

        updateLikabilityForKeywords(article, context, userRef, false, true)

        val userDislikesRef: DatabaseReference = userRef.child(DISLIKES_PATH)
        userDislikesRef.orderByChild(ARTICLE_URL_PATH).equalTo(articleURL)
            .addListenerForSingleValueEvent(ThrowingValueEventListener {
                if (!it.exists()) {
                    userDislikesRef.push().setValue(mapOf(
                        ARTICLE_URL_PATH to articleURL,
                        DISLIKED_AT_PATH to System.currentTimeMillis()
                    ))
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

    private fun updateLikabilityForKeywords(article: Article, context: Context, userRef: DatabaseReference, isPositive: Boolean, isUndo: Boolean) {
        if (article.titleKeywords == null) {
            article.titleKeywords = ArticleTitleAnalyser(context).execute(article).get()
        }

        val keywordLikabilityRef: DatabaseReference = userRef.child(KEYWORD_LIKABILITY_PATH)

        article.titleKeywords?.forEach { (keyword, salience) ->
            var totalSalience: Double = if (isPositive) salience else -salience
            var count = if (isUndo) -1.0 else 1.0

            val encodedKeyword: String = firebaseEncode(keyword)
            val keywordRef: DatabaseReference = keywordLikabilityRef.child(encodedKeyword)
            keywordRef.addListenerForSingleValueEvent(ThrowingValueEventListener {
                if (it.exists()) {
                    val keywordLikability: Map<String, Double> = it.getValue(MAP_STRING_DOUBLE_TYPE)!!
                    totalSalience += keywordLikability[TOTAL_SALIENCE_PATH] ?: 0.0
                    count += keywordLikability[COUNT_PATH] ?: 0.0
                }
                keywordRef.setValue(mapOf(
                    TOTAL_SALIENCE_PATH to totalSalience,
                    COUNT_PATH to count
                ))
            })
        }
    }

    fun sortArticlesByLikability(userUid: String, articles: MutableList<Article>, context: Context, doneCallback: (articles: List<Article>) -> Unit) {
        removeDislikedArticlesFromList(userUid, articles) { articlesWithoutDislikes ->
            getArticleTitleKeywords(articlesWithoutDislikes, context) { analysedArticles ->
                sortArticlesByLikability(userUid, analysedArticles) { sortedArticles ->
                    doneCallback(sortedArticles)
                }
            }
        }
    }

    fun addArticlesLikedByFollowedUsers(userUid: String, oldestLikedAtMillis: Long, articles: List<Article>, doneCallback: (articles: MutableList<Article>) -> Unit) {
        mDatabase.getReference(USERS_PATH)
            .child(userUid)
            .child(FOLLOWING_PATH)
            .child(USERS_PATH)
            .addListenerForSingleValueEvent(ThrowingValueEventListener {
                val followedUserUids: List<String> = it.children.map { snapshot ->
                    snapshot.getValue(String::class.java)!!
                }
                getArticlesLikedByUsers(followedUserUids, oldestLikedAtMillis, articles) { articlesWithLikes ->
                    hasUserLikedAnyArticles(userUid, articlesWithLikes) { returnArticles ->
                        doneCallback(returnArticles)
                    }
                }
            })
    }

    private fun getArticlesLikedByUsers(userUids: List<String>, oldestLikedAtMillis: Long, articles: List<Article>, doneCallback: (articles: MutableList<Article>) -> Unit) {
        val articlesMap: MutableMap<Article, Article> = articles.associateBy({ it }, { it }).toMutableMap()
        getArticlesLikedByUsers(userUids.iterator(), oldestLikedAtMillis, articlesMap, doneCallback)
    }

    private fun getArticlesLikedByUsers(iterator: Iterator<String>, oldestLikedAtMillis: Long, articles: MutableMap<Article, Article>, doneCallback: (articles: MutableList<Article>) -> Unit) {
        if (iterator.hasNext()) {
            val userUid: String = iterator.next()
            getUserLikes(userUid, true, LIKED_ARTICLE_LIMIT, oldestLikedAtMillis) { displayName, likedArticles ->
                likedArticles?.forEach { article ->
                    articles.getOrPut(article) { article }.likedBy(displayName!!)
                }
                getArticlesLikedByUsers(iterator, oldestLikedAtMillis, articles, doneCallback)
            }
        } else {
            doneCallback(articles.values.toMutableList())
        }
    }

    private fun hasUserLikedAnyArticles(userUid: String, articles: MutableList<Article>, doneCallback: (articles: MutableList<Article>) -> Unit) {
        hasUserLikedAnyArticles(userUid, articles.iterator().withIndex(), articles, doneCallback)
    }

    private fun hasUserLikedAnyArticles(userUid: String, iterator: Iterator<IndexedValue<Article>>, articles: MutableList<Article>, doneCallback: (articles: MutableList<Article>) -> Unit) {
        if (iterator.hasNext()) {
            val indexedArticle: IndexedValue<Article> = iterator.next()
            val encodedURL: String = firebaseEncode(indexedArticle.value.articleURL)
            mDatabase.getReference(USERS_PATH)
                .child(userUid)
                .child(LIKES_PATH)
                .orderByChild(ARTICLE_URL_PATH)
                .equalTo(encodedURL)
                .addListenerForSingleValueEvent(ThrowingValueEventListener {
                    if (it.exists()) {
                        articles[indexedArticle.index].isLiked = true
                    }
                    hasUserLikedAnyArticles(userUid, iterator, articles, doneCallback)
                })
        } else {
            doneCallback(articles)
        }
    }

    private fun removeDislikedArticlesFromList(userUid: String, articles: MutableList<Article>, doneCallback: (articles: MutableList<Article>) -> Unit) {
        removeDislikedArticlesFromList(userUid, articles.iterator(), mutableListOf(), doneCallback)
    }

    private fun removeDislikedArticlesFromList(userUid: String, iterator: Iterator<Article>, articles: MutableList<Article>, doneCallback: (articles: MutableList<Article>) -> Unit) {
        if (iterator.hasNext()) {
            val article: Article = iterator.next()
            val articleURL: String = firebaseEncode(article.articleURL)

            mDatabase.getReference(USERS_PATH)
                .child(userUid)
                .child(DISLIKES_PATH)
                .orderByChild(ARTICLE_URL_PATH)
                .equalTo(articleURL)
                .addListenerForSingleValueEvent(ThrowingValueEventListener {
                    if (!it.exists()) {
                        articles.add(article)
                    }
                    removeDislikedArticlesFromList(userUid, iterator, articles, doneCallback)
                })
        } else {
            doneCallback(articles)
        }
    }

    private fun getArticleTitleKeywords(articles: List<Article>, context: Context, doneCallback: (articles: MutableList<Article>) -> Unit) {
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
                        val articleFromDatabase: Article = it.getValue(Article::class.java)!!
                        article.titleKeywords = articleFromDatabase.titleKeywords
                    } else {
                        val titleKeywords: Map<String, Double>? = ArticleTitleAnalyser(context).execute(article).get()
                        // encode keys to remove illegal characters for storing in Firebase
                        article.titleKeywords = titleKeywords?.mapKeys { entry -> firebaseEncode(entry.key) }
                        articleRef.setValue(article.toArticleDTO())
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

    private fun sortArticlesByLikability(userUid: String, articles: MutableList<Article>, doneCallback: (articles: List<Article>) -> Unit) {
        sortArticlesByLikability(userUid, articles.iterator().withIndex(), articles, doneCallback)
    }

    private fun sortArticlesByLikability(userUid: String, iterator: Iterator<IndexedValue<Article>>, articles:  MutableList<Article>, doneCallback: (articles: List<Article>) -> Unit) {
        if (iterator.hasNext()) {
            val article: IndexedValue<Article> = iterator.next()
            if (article.value.titleKeywords != null) {
                getLikabilityForArticle(userUid, article.value.titleKeywords!!) { likabilityFactor ->
                    articles[article.index].likabilityFactor = likabilityFactor
                    sortArticlesByLikability(userUid, iterator, articles, doneCallback)
                }
            } else {
                sortArticlesByLikability(userUid, iterator, articles, doneCallback)
            }
        } else {
            articles.sortByDescending { it.likabilityFactor }
            doneCallback(articles)
        }
    }

    private fun getLikabilityForArticle(userUid: String, keywords: Map<String, Double>, doneCallback: (likabilityFactor: Double) -> Unit) {
        getLikabilityForArticle(userUid, keywords.iterator(), 0.0, doneCallback)
    }

    private fun getLikabilityForArticle(userUid: String, iterator: Iterator<Map.Entry<String, Double>>, currentLikabilityFactor: Double, doneCallback: (likabilityFactor: Double) -> Unit) {
        if (iterator.hasNext()) {
            val keywordSalience: Map.Entry<String, Double> = iterator.next()
            val encodedKeyword: String = firebaseEncode(keywordSalience.key)
            val salience: Double = keywordSalience.value

            mDatabase.getReference(USERS_PATH)
                .child(userUid)
                .child(KEYWORD_LIKABILITY_PATH)
                .child(encodedKeyword)
                .addListenerForSingleValueEvent(ThrowingValueEventListener {
                    var newLikabilityFactor = currentLikabilityFactor
                    if (it.exists()) {
                        val data: Map<String, Double> = it.getValue(MAP_STRING_DOUBLE_TYPE)!!
                        val totalSalience: Double = data[TOTAL_SALIENCE_PATH] ?: 0.0
                        val count: Double = data[COUNT_PATH] ?: 0.0
                        val likabilityFactor: Double = (totalSalience / count)
                        newLikabilityFactor += salience * likabilityFactor
                    }
                    getLikabilityForArticle(userUid, iterator, newLikabilityFactor, doneCallback)
                })
        } else {
            doneCallback(currentLikabilityFactor)
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
            .take(MAX_PATH_LENGTH)
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

    fun getUserLikes(userUid: String, doneCallback: (articles: MutableList<Article>?) -> Unit) {
        getUserLikes(userUid, false, ARTICLE_LIMIT) { _, articles ->
            doneCallback(articles)
        }
    }

    private fun getUserLikes(userUid: String, getDisplayName: Boolean, limit: Int, doneCallback: (displayName: String?, articles: MutableList<Article>?) -> Unit) {
        getUserLikes(userUid, getDisplayName, limit, null, doneCallback)
    }

    private fun getUserLikes(userUid: String, getDisplayName: Boolean, limit: Int, oldestLikedAtMillis: Long?, doneCallback: (displayName: String?, articles: MutableList<Article>?) -> Unit) {
        val userRef: DatabaseReference = mDatabase.getReference(USERS_PATH).child(userUid)

        var likesQuery: Query = userRef.child(LIKES_PATH)
            .orderByChild(LIKED_AT_PATH)
            .limitToFirst(limit)

        if (oldestLikedAtMillis != null) {
            likesQuery = likesQuery.startAt(oldestLikedAtMillis.toDouble())
        }

        likesQuery.addListenerForSingleValueEvent(ThrowingValueEventListener { likes ->
            if (likes.exists()) {
                val articleURLs: List<String> = likes.children.map { snapshot ->
                    snapshot.getValue(MAP_STRING_ANY_TYPE)!![ARTICLE_URL_PATH] as String
                }.reversed() // list ordered by likedAt in ascending order, so need to reverse

                if (getDisplayName) {
                    userRef.child(PROFILE_PATH)
                        .child(DISPLAY_NAME_PATH)
                        .addListenerForSingleValueEvent(ThrowingValueEventListener {
                            val displayName: String = it.getValue(String::class.java)!!
                            getArticlesByURLs(articleURLs) { articles ->
                                doneCallback(displayName, articles)
                            }
                        })
                } else {
                    getArticlesByURLs(articleURLs) { articles ->
                        doneCallback(null, articles)
                    }
                }
            } else {
                doneCallback(null, null)
            }
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

    fun findUsersWithDisplayName(displayName: String, doneCallback: (users: Map<String, UserProfile>) -> Unit) {
        mDatabase.getReference(USERS_PATH)
            .orderByChild("$USER_PROFILE_PATH/$DISPLAY_NAME_PATH")
            .equalTo(displayName)
            .addListenerForSingleValueEvent(ThrowingValueEventListener {
                if (it.exists()) {
                    val users: MutableMap<String, UserProfile> = mutableMapOf()
                    val usersData: Map<String, Map<String, Any>> = it.getValue(MAP_STRING_MAP_STRING_ANY_TYPE)!!
                    usersData.entries.forEach { (userUid, userData) ->
                        val profileData: Map<String, String> = userData[PROFILE_PATH] as Map<String, String>
                        val userProfile = UserProfile(
                            profileData.getValue(DISPLAY_NAME_PATH),
                            profileData[LOCATION_PATH],
                            profileData[PROFILE_PIC_URI_PATH]
                        )
                        users[userUid] = userProfile
                    }
                    doneCallback(users)
                } else {
                    doneCallback(emptyMap())
                }
            })
    }

    fun doSourcesNeedUpdating(doneCallback: (shouldUpdate: Boolean) -> Unit) {
        mDatabase.getReference(SOURCES_PATH)
            .child(UPDATED_AT_PATH)
            .addListenerForSingleValueEvent(ThrowingValueEventListener {
                if (it.exists()) {
                    val updatedAt: Long = it.getValue(Long::class.java)!!
                    if (System.currentTimeMillis() >= updatedAt + SOURCES_UPDATE_INTERVAL) {
                        doneCallback(true)
                    } else {
                        doneCallback(false)
                    }
                } else {
                    doneCallback(true)
                }
            })
    }

    fun updateSources(sources: Map<String, List<Source>>) {
        val sourcesRef: DatabaseReference = mDatabase.getReference(SOURCES_PATH)
        sourcesRef.updateChildren(sources)
        sourcesRef.child(UPDATED_AT_PATH).setValue(System.currentTimeMillis())
    }

    fun getSourceIdsForCategories(categories: List<String>, doneCallback: (sourcesString: String) -> Unit) {
        getSourceIdsForCategories(categories.iterator(), mutableListOf(), doneCallback)
    }

    private fun getSourceIdsForCategories(iterator: Iterator<String>, sourceIds: MutableList<String>, doneCallback: (sourcesString: String) -> Unit) {
        if (iterator.hasNext()) {
            val category: String = iterator.next()
            mDatabase.getReference(SOURCES_PATH)
                .child(category)
                .orderByChild(ID_PATH)
                .addListenerForSingleValueEvent(ThrowingValueEventListener { snapshot ->
                    val currentCategorySourceIds: List<String> = snapshot.children.map {
                        it.getValue(Source::class.java)!!.id
                    }
                    sourceIds.addAll(currentCategorySourceIds)
                    getSourceIdsForCategories(iterator, sourceIds, doneCallback)
                })
        } else {
            doneCallback(sourceIds.joinToString())
        }
    }

    fun findSourcesByName(name: String, doneCallback: (sources: List<Source>) -> Unit) {
        findSourcesByName(name, Category.values().iterator(), mutableListOf(), doneCallback)
    }

    private fun findSourcesByName(name: String, iterator: Iterator<Category>, sources: MutableList<Source>, doneCallback: (sources: List<Source>) -> Unit) {
        if (iterator.hasNext()) {
            val category: Category = iterator.next()
            mDatabase.getReference(SOURCES_PATH)
                .child(category.toString())
                .orderByChild(NAME_PATH)
                .equalTo(name)
                .addListenerForSingleValueEvent(ThrowingValueEventListener {
                    val currentSources: List<Source> = it.children.map { snapshot ->
                        snapshot.getValue(Source::class.java)!!
                    }
                    sources.addAll(currentSources)
                    findSourcesByName(name, iterator, sources, doneCallback)
                })
        } else {
            doneCallback(sources)
        }
    }

}