package com.csc306.coursework.model

import android.content.Context
import com.csc306.coursework.R
import org.apache.commons.lang3.StringUtils
import java.util.concurrent.TimeUnit

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
    var isLiked: Boolean = false
    var likedBy: MutableList<String>? = null

    fun likedBy(userUid: String) {
        if (likedBy == null) {
            likedBy = mutableListOf()
        }
        likedBy!!.add(userUid)
    }

    fun toArticleDTO(): ArticleDTO {
        return ArticleDTO(source, publishDateMillis, imageURL, title, description, articleURL, titleKeywords)
    }

    fun getTimeSincePublishedString(context: Context): String {
        val diff: Long = System.currentTimeMillis() - publishDateMillis
        val daysAgo: Long = TimeUnit.MILLISECONDS.toDays(diff)
        if (daysAgo > 0) {
            return daysAgo.toString() + context.getString(R.string.days_ago)
        }
        val hoursAgo: Long = TimeUnit.MILLISECONDS.toHours(diff)
        if (hoursAgo > 0) {
            return hoursAgo.toString() + context.getString(R.string.hours_ago)
        }
        val minutesAgo: Long = TimeUnit.MILLISECONDS.toMinutes(diff)
        if (minutesAgo > 0) {
            return minutesAgo.toString() + context.getString(R.string.minutes_ago)
        }
        val secondsAgo: Long = TimeUnit.MILLISECONDS.toSeconds(diff)
        if (secondsAgo > 0) {
            return secondsAgo.toString() + context.getString(R.string.seconds_ago)
        }
        return context.getString(R.string.now)
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