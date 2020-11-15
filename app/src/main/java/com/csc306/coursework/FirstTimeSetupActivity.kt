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

    private val newsApi: NewsApiRepository = NewsApiRepository(getString(R.string.api_key))

    private var disposableApiCall: Disposable? = null

    private val database: DatabaseManager = DatabaseManager(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        updateSources()
    }

    private fun updateSources() {
        disposableApiCall = newsApi.getSources(language = Language.valueOf(Locale.getDefault().language))
            .subscribeOn(Schedulers.io())
            .toFlowable()
            .subscribe({ database.updateSources(it.sources) },
                { err -> Log.e(localClassName, "An error occurred when getting source info: ", err) })
    }

    override fun onStop() {
        super.onStop()
        disposableApiCall?.dispose()
    }

}