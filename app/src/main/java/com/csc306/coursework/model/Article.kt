package com.csc306.coursework.model

import java.time.LocalDateTime

class Article(
    val source: String,
    val published: LocalDateTime,
    val image: Int,
    val title: String,
    val description: String,
    val articleURL: String
)