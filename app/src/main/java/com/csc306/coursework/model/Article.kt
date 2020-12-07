package com.csc306.coursework.model

import org.apache.commons.lang3.StringUtils

class Article(
    val source: String,
    val publishDateMillis: Long,
    val imageURL: String?,
    val title: String,
    val description: String?,
    val articleURL: String
) {
    // no-arg constructor for bean mapping
    constructor() : this(
        StringUtils.EMPTY,
        0L,
        null,
        StringUtils.EMPTY,
        null,
        StringUtils.EMPTY
    )

    var titleKeywords: Map<String, Double>? = null
    var isLikedByCurrentUser: Boolean = false
    private var likedBy: MutableList<String>? = null

    fun likedBy(userUid: String) {
        if (likedBy == null) {
            likedBy = mutableListOf()
        }
        likedBy!!.add(userUid)
    }

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