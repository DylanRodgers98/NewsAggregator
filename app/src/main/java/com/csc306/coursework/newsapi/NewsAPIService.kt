package com.csc306.coursework.newsapi

import android.content.Context
import com.csc306.coursework.R
import com.csc306.coursework.model.Article
import com.csc306.coursework.model.Source
import com.dfl.newsapi.NewsApiRepository
import com.dfl.newsapi.enums.Language
import com.dfl.newsapi.model.ArticleDto
import com.dfl.newsapi.model.SourceDto
import io.reactivex.schedulers.Schedulers
import java.time.OffsetDateTime

class NewsAPIService(context: Context) {

    private val mNewsApi = NewsApiRepository(context.getString(R.string.news_api_key))

    fun getSources(languageCode: Language): Map<String, MutableList<Source>> {
        val sources: List<SourceDto> = mNewsApi.getSources(language = languageCode)
            .subscribeOn(Schedulers.io())
            .toFlowable()
            .flatMapIterable { it.sources }
            .toList()
            .blockingGet()

        val categorySourceMap: MutableMap<String, MutableList<Source>> = mutableMapOf()
        sources.forEach {
            categorySourceMap.computeIfAbsent(it.category) { mutableListOf() }.add(buildSource(it))
        }
        return categorySourceMap
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
        return Source(sourceDto.id, sourceDto.name)
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