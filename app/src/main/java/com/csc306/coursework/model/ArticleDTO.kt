package com.csc306.coursework.model

data class ArticleDTO(
    val source: String,
    val publishDateMillis: Long,
    val imageURL: String?,
    val title: String,
    val description: String?,
    val articleURL: String,
    val titleKeywords: Map<String, Double>?
)