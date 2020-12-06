package com.csc306.coursework.model

class Article(
    val source: String,
    val publishDateMillis: Long,
    val imageURL: String?,
    val title: String,
    val description: String?,
    val articleURL: String
) {
    var titleKeywords: Map<String, Double>? = null

    override fun equals(other: Any?): Boolean {
        return other is Article
                && other.source == this.source
                && other.publishDateMillis == this.publishDateMillis
                && other.imageURL == this.imageURL
                && other.title == this.title
                && other.description == this.description
                && other.articleURL == this.articleURL
    }

    override fun hashCode(): Int {
        var result = source.hashCode()
        result = 31 * result + publishDateMillis.hashCode()
        result = 31 * result + (imageURL?.hashCode() ?: 0)
        result = 31 * result + title.hashCode()
        result = 31 * result + (description?.hashCode() ?: 0)
        result = 31 * result + articleURL.hashCode()
        return result
    }
}