package com.csc306.coursework.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.dfl.newsapi.model.SourceDto
import java.util.*

class DatabaseManager(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE $TABLE_SOURCE($COLUMN_ID TEXT PRIMARY KEY, $COLUMN_CATEGORY TEXT NOT NULL)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_SOURCE")
        onCreate(db)
    }

    fun updateSources(sources: List<SourceDto>) {
        this.writableDatabase.beginTransaction()
        try {
            this.writableDatabase.delete(TABLE_SOURCE, null, null)
            sources.forEach { source ->
                val values = ContentValues()
                values.put(COLUMN_ID, source.id)
                values.put(COLUMN_CATEGORY, source.category)
                this.writableDatabase.insert(TABLE_SOURCE, null, values)
            }
            this.writableDatabase.setTransactionSuccessful()
        } finally {
            this.writableDatabase.endTransaction()
        }
    }

    fun getSourcesForCategories(categories: Array<String>): String? {
        val query: String = Collections.nCopies(categories.size, "WHERE $COLUMN_CATEGORY='?'")
            .joinToString(
                prefix = "SELECT GROUP_CONCAT($COLUMN_ID) FROM $TABLE_SOURCE ",
                separator = " OR "
            )
        this.writableDatabase.rawQuery(query, categories).use { cursor ->
            if (cursor.moveToFirst()) {
                return cursor.getString(0)
            }
        }
        return null
    }

    companion object {
        const val DATABASE_NAME: String = "CSC306_COURSEWORK_DATABASE"
        const val DATABASE_VERSION: Int = 1
        const val TABLE_SOURCE: String = "SOURCE"
        const val COLUMN_ID: String = "ID"
        const val COLUMN_CATEGORY: String = "CATEGORY"
    }

}