package com.csc306.coursework.model

import com.google.cloud.language.v1beta2.Document
import com.google.cloud.language.v1beta2.Entity
import com.google.cloud.language.v1beta2.LanguageServiceClient
import java.time.LocalDateTime
import java.util.*

class Article(
    val source: String,
    val publishDate: LocalDateTime,
    val imageURL: String,
    val title: String,
    val description: String,
    val articleURL: String
) {
    private var titleKeywords: Map<String, Float>? = null

    fun getTitleKeywords(): Map<String, Float> {
        if (titleKeywords == null) {
            analyseArticleTitle()
        }
        return titleKeywords!!
    }

    private fun analyseArticleTitle() {
        LanguageServiceClient.create().use { client ->
            val document: Document = Document.newBuilder()
                .setContent(title.toLowerCase(Locale.getDefault()))
                .setType(Document.Type.PLAIN_TEXT)
                .build()
            val entities: List<Entity> = client.analyzeEntities(document).entitiesList
            titleKeywords = entities.associateBy({ it.name }, { it.salience })
        }
    }
}