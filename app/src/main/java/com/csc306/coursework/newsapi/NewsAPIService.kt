package com.csc306.coursework.newsapi

import android.content.Context
import android.util.Log
import com.csc306.coursework.R
import com.csc306.coursework.model.Article
import com.csc306.coursework.model.Category
import com.csc306.coursework.model.Source
import com.dfl.newsapi.NewsApiRepository
import com.dfl.newsapi.enums.Language
import com.dfl.newsapi.model.ArticleDto
import com.dfl.newsapi.model.SourceDto
import io.reactivex.schedulers.Schedulers
import java.time.OffsetDateTime
import java.util.*

class NewsAPIService(context: Context) {

    private val mNewsApi = NewsApiRepository(context.getString(R.string.news_api_key))

    fun getSources(languageCode: Language): List<Source> {
        return mNewsApi.getSources(language = languageCode)
            .subscribeOn(Schedulers.io())
            .toFlowable()
            .flatMapIterable { it.sources }
            .map { buildSource(it) }
            .toList()
            .blockingGet()
    }

    fun getTopHeadlines(sourceIds: String): MutableList<Article> {
        return mNewsApi.getTopHeadlines(sources = sourceIds)
            .subscribeOn(Schedulers.io())
            .toFlowable()
            .flatMapIterable { it.articles }
            .map { buildArticle(it) }
            .toList()
            .blockingGet()
    }

    fun getEverything(query: String): MutableList<Article> {
        return mNewsApi.getEverything(q = query)
            .subscribeOn(Schedulers.io())
            .toFlowable()
            .flatMapIterable { it.articles }
            .map { buildArticle(it) }
            .toList()
            .blockingGet()
    }

    private fun buildSource(sourceDto: SourceDto): Source {
        return Source(
            sourceDto.id,
            sourceDto.name,
            Category.valueOf(sourceDto.category.toUpperCase(Locale.getDefault()))
        )
    }

    private fun buildArticle(articleDto: ArticleDto): Article {
        return Article(
            articleDto.source.name,
            OffsetDateTime.parse(articleDto.publishedAt).toInstant().toEpochMilli(),
            articleDto.urlToImage,
            articleDto.title,
            articleDto.description,
            articleDto.url,
        )
    }

}