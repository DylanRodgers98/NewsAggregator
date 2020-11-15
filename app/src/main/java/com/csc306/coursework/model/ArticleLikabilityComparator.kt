package com.csc306.coursework.model

import com.csc306.coursework.database.DatabaseManager
import com.google.cloud.language.v1beta2.Document
import com.google.cloud.language.v1beta2.Entity
import com.google.cloud.language.v1beta2.LanguageServiceClient
import java.util.*
import kotlin.Comparator

class ArticleLikabilityComparator(private val databaseManager: DatabaseManager) : Comparator<Article> {

    override fun compare(article1: Article, article2: Article): Int {
        val sortingIndexArticle1: Float = getSortingIndex(article1)
        val sortingIndexArticle2: Float = getSortingIndex(article2)

        if (sortingIndexArticle1 < sortingIndexArticle2) return -1
        if (sortingIndexArticle1 > sortingIndexArticle2) return 1
        return 0
    }

    private fun getSortingIndex(article: Article): Float {
        if (article.titleKeywords == null) {
            analyseArticleTitle(article)
        }
        return calculateArticleLikability(article.titleKeywords!!)
    }

    private fun analyseArticleTitle(article: Article) {
        LanguageServiceClient.create().use { client ->
            val document: Document = Document.newBuilder()
                .setContent(article.title.toLowerCase(Locale.getDefault()))
                .setType(Document.Type.PLAIN_TEXT)
                .build()
            val entities: List<Entity> = client.analyzeEntities(document).entitiesList
            article.titleKeywords = entities.associateBy({ it.name }, { it.salience })
        }
    }

    private fun calculateArticleLikability(titleKeywords: Map<String, Float>): Float {
        val keywordLikability: MutableMap<String, Float> = mutableMapOf()
        databaseManager.getLikabilityForKeywords(titleKeywords.keys).forEach {
            val salience: Float? = titleKeywords[it.keyword]
            if (salience != null) {
                keywordLikability[it.keyword] = salience * it.likabilityFactor
            }
        }
        return keywordLikability.values.sum()
    }

}