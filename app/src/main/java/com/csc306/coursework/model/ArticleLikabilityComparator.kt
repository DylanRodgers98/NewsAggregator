package com.csc306.coursework.model

import android.content.Context
import com.csc306.coursework.database.DatabaseManager
import kotlin.Comparator

class ArticleLikabilityComparator(private val databaseManager: DatabaseManager, private val context: Context) : Comparator<Article> {

    override fun compare(article1: Article, article2: Article): Int {
        val sortingIndexArticle1: Float = getSortingIndex(article1)
        val sortingIndexArticle2: Float = getSortingIndex(article2)

        if (sortingIndexArticle1 < sortingIndexArticle2) return -1
        if (sortingIndexArticle1 > sortingIndexArticle2) return 1
        return 0
    }

    private fun getSortingIndex(article: Article): Float {
        if (article.titleKeywords == null) {
            ArticleTitleAnalyser(context).execute(article).get()
        }
        var articleLikability = 0.0F
        databaseManager.getLikabilityForKeywords(article.titleKeywords!!.keys).forEach {
            val keywordSalience: Float? = article.titleKeywords!![it.keyword]
            if (keywordSalience != null) {
                articleLikability += keywordSalience * it.likabilityFactor
            }
        }
        return articleLikability
    }

}