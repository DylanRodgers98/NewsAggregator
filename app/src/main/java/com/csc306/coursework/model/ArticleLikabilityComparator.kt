package com.csc306.coursework.model

import com.csc306.coursework.database.DatabaseManager
import kotlin.Comparator

class ArticleLikabilityComparator(private val databaseManager: DatabaseManager) : Comparator<Article> {

    override fun compare(article1: Article, article2: Article): Int {
        val sortingIndexArticle1: Float = getSortingIndex(article1.getTitleKeywords())
        val sortingIndexArticle2: Float = getSortingIndex(article2.getTitleKeywords())

        if (sortingIndexArticle1 < sortingIndexArticle2) return -1
        if (sortingIndexArticle1 > sortingIndexArticle2) return 1
        return 0
    }

    private fun getSortingIndex(titleKeywords: Map<String, Float>): Float {
        var articleLikability = 0.0F
        databaseManager.getLikabilityForKeywords(titleKeywords.keys).forEach {
            val keywordSalience: Float? = titleKeywords[it.keyword]
            if (keywordSalience != null) {
                articleLikability += keywordSalience * it.likabilityFactor
            }
        }
        return articleLikability
    }

}