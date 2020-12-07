package com.csc306.coursework.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteOpenHelper
import com.dfl.newsapi.model.SourceDto
import java.util.*

class DatabaseManager(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(database: SQLiteDatabase) {
        doWithinTransaction(database) { db ->
            db.execSQL("CREATE TABLE $TABLE_SOURCE($COLUMN_ID INTEGER PRIMARY KEY, $COLUMN_SOURCE_ID TEXT NOT NULL, $COLUMN_CATEGORY TEXT NOT NULL)")
            db.execSQL("CREATE TABLE $TABLE_SWIPED_KEYWORD($COLUMN_ID INTEGER PRIMARY KEY, $COLUMN_KEYWORD TEXT NOT NULL, $COLUMN_TOTAL_SALIENCE REAL NOT NULL, $COLUMN_COUNT REAL NOT NULL)") // COLUMN_COUNT must be of type REAL so that TOTAL_SALIENCE / COUNT can return a floating point number
        }
    }

    override fun onUpgrade(database: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        doWithinTransaction(database) { db ->
            db.execSQL("DROP TABLE IF EXISTS $TABLE_SOURCE")
            db.execSQL("DROP TABLE IF EXISTS $TABLE_SWIPED_KEYWORD")
        }
        onCreate(database)
    }

    fun updateSources(sources: List<SourceDto>) {
        doWithinTransaction(this.writableDatabase) { db ->
            db.delete(TABLE_SOURCE, null, null)
            sources.forEach { source ->
                val values = ContentValues()
                values.put(COLUMN_SOURCE_ID, source.id)
                values.put(COLUMN_CATEGORY, source.category)
                db.insert(TABLE_SOURCE, null, values)
            }
        }
    }

    fun getSourceIdsForCategories(categories: Array<String>): String {
        var query = "SELECT GROUP_CONCAT($COLUMN_SOURCE_ID) FROM $TABLE_SOURCE"
        if (categories.isNotEmpty()) {
            query += " WHERE " + Collections.nCopies(categories.size, "$COLUMN_CATEGORY=?")
                .joinToString(" OR ")
        }
        this.readableDatabase.rawQuery(query, categories).use { cursor ->
            if (cursor.moveToFirst()) {
                return cursor.getString(0)
            }
        }
        throw SQLiteException("Found no sources in database. Has first time setup taken place?")
    }

    private fun doWithinTransaction(db: SQLiteDatabase, databaseAction: (db: SQLiteDatabase) -> Unit) {
        db.beginTransaction()
        try {
            databaseAction(db)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    companion object {
        const val DATABASE_NAME: String = "CSC306_COURSEWORK_DATABASE"
        const val DATABASE_VERSION: Int = 1
        const val TABLE_SOURCE: String = "SOURCE"
        const val COLUMN_ID: String = "ID"
        const val COLUMN_SOURCE_ID: String = "SOURCE_ID"
        const val COLUMN_CATEGORY: String = "CATEGORY"
        const val TABLE_SWIPED_KEYWORD: String = "SWIPED_KEYWORD"
        const val COLUMN_KEYWORD: String = "KEYWORD"
        const val COLUMN_TOTAL_SALIENCE: String = "TOTAL_SALIENCE"
        const val COLUMN_COUNT: String = "COUNT"
    }

}