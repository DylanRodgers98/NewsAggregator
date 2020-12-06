package com.csc306.coursework.model

import android.content.Context
import com.csc306.coursework.async.ArticleTitleAnalyser
import com.csc306.coursework.database.DatabaseManager
import kotlin.Comparator

class ArticleLikabilityComparator(
    private val databaseManager: DatabaseManager,
    private val context: Context
) : Comparator<Article> {

    override fun compare(article1: Article, article2: Article): Int {
        val sortingIndexArticle1: Double = getSortingIndex(article1)
        val sortingIndexArticle2: Double = getSortingIndex(article2)

        if (sortingIndexArticle1 < sortingIndexArticle2) return -1
        if (sortingIndexArticle1 > sortingIndexArticle2) return 1
        return 0
    }

    private fun getSortingIndex(article: Article): Double {
        if (article.titleKeywords == null) {
            article.titleKeywords = ArticleTitleAnalyser(context).execute(article).get()
        }
        var articleLikability = 0.0
        // title keywords may still be null after analysis, so need to check here
        if (article.titleKeywords != null) {
            databaseManager.getLikabilityForKeywords(article.titleKeywords!!.keys).forEach {
                val keywordSalience: Double? = article.titleKeywords!![it.keyword]
                if (keywordSalience != null) {
                    articleLikability += keywordSalience * it.likabilityFactor
                }
            }
        }
        return articleLikability
    }

}