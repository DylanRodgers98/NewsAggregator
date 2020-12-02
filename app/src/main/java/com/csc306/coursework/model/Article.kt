package com.csc306.coursework.model

import java.time.LocalDateTime

class Article(
    val source: String,
    val publishDate: LocalDateTime,
    val imageURL: String?,
    val title: String,
    val description: String?,
    val articleURL: String
) {
    var titleKeywords: Map<String, Float>? = null
}