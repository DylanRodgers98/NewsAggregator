package com.csc306.coursework

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.csc306.coursework.database.DatabaseManager
import com.dfl.newsapi.NewsApiRepository
import com.dfl.newsapi.enums.Language
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.util.*

class FirstTimeSetupActivity : AppCompatActivity() {

    private val newsApi: NewsApiRepository = NewsApiRepository("bf38b882f6a6421096d8acd384d33b71")

    private val databaseManager: DatabaseManager = DatabaseManager(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        updateSources()
    }

    private fun updateSources() {
        val defaultLanguage: String = Locale.getDefault().language
        val languageCode: Language = Language.valueOf(defaultLanguage.toUpperCase(Locale.getDefault()))
        newsApi.getSources(language = languageCode)
            .subscribeOn(Schedulers.io())
            .toFlowable()
            .blockingSubscribe({
                Log.i(localClassName, "Updating ${it.sources.size} sources in database")
                databaseManager.updateSources(it.sources)
                Log.i(localClassName, "Sources updated in database")
            }, { err ->
                Log.e(localClassName, "An error occurred when updating source info:", err)
            })
    }

}