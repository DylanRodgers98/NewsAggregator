package com.csc306.coursework.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import androidx.core.database.sqlite.transaction
import com.csc306.coursework.model.LikabilityDTO
import com.dfl.newsapi.model.SourceDto
import java.util.*

class DatabaseManager(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.transaction {
            this.execSQL("CREATE TABLE $TABLE_SOURCE($COLUMN_ID INTEGER PRIMARY KEY, $COLUMN_SOURCE_ID TEXT NOT NULL, $COLUMN_CATEGORY TEXT NOT NULL)")
            this.execSQL("CREATE TABLE $TABLE_SWIPED_KEYWORD($COLUMN_ID INTEGER PRIMARY KEY, $COLUMN_KEYWORD TEXT NOT NULL, $COLUMN_TOTAL_SALIENCE REAL NOT NULL, $COLUMN_COUNT REAL NOT NULL)") // COLUMN_COUNT must be of type REAL so that TOTAL_SALIENCE / COUNT can return a floating point number
        }
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.transaction {
            this.execSQL("DROP TABLE IF EXISTS $TABLE_SOURCE")
            this.execSQL("DROP TABLE IF EXISTS $TABLE_SWIPED_KEYWORD")
        }
        onCreate(db)
    }

    fun updateSources(sources: List<SourceDto>) {
        this.writableDatabase.transaction {
            this.execSQL("DROP TABLE IF EXISTS $TABLE_SOURCE")
            sources.forEach { source ->
                val values = ContentValues()
                values.put(COLUMN_SOURCE_ID, source.id)
                values.put(COLUMN_CATEGORY, source.category)
                this.insert(TABLE_SOURCE, null, values)
            }
        }
    }

    fun getSourcesForCategories(categories: Array<String>): String? {
        val query: String = Collections.nCopies(categories.size, "WHERE $COLUMN_CATEGORY='?'")
            .joinToString(
                prefix = "SELECT GROUP_CONCAT($COLUMN_ID) FROM $TABLE_SOURCE ",
                separator = " OR "
            )
        this.readableDatabase.rawQuery(query, categories).use { cursor ->
            if (cursor.moveToFirst()) {
                return cursor.getString(0)
            }
        }
        return null
    }

    fun updateLikabilityForKeywords(titleKeywords: Map<String, Float>) {
        this.writableDatabase.transaction {
            titleKeywords.entries.forEach {
                val keyword: String = it.key
                val salience: Float = it.value

                var currentTotalSalience = 0.0F
                var currentCount = 0.0F

                val query = "SELECT $COLUMN_TOTAL_SALIENCE, $COLUMN_COUNT FROM $TABLE_SWIPED_KEYWORD WHERE $COLUMN_KEYWORD='?'"
                this.rawQuery(query, arrayOf(keyword)).use { cursor ->
                    if (cursor.moveToFirst()) {
                        currentTotalSalience = cursor.getFloat(0)
                        currentCount = cursor.getFloat(1)
                    }
                }

                val values = ContentValues()
                values.put(COLUMN_KEYWORD, keyword)
                values.put(COLUMN_TOTAL_SALIENCE, currentTotalSalience + salience)
                values.put(COLUMN_COUNT, currentCount + 1)
                this.insertWithOnConflict(TABLE_SWIPED_KEYWORD, null, values, SQLiteDatabase.CONFLICT_REPLACE)
            }
        }
    }

    fun getLikabilityForKeywords(keywords: Collection<String>): List<LikabilityDTO> {
        val query = "SELECT $COLUMN_KEYWORD, $COLUMN_TOTAL_SALIENCE / $COLUMN_COUNT FROM $TABLE_SWIPED_KEYWORD WHERE $COLUMN_KEYWORD IN (?)"
        val likabilityDTOs: MutableList<LikabilityDTO> = mutableListOf()
        this.readableDatabase.rawQuery(query, arrayOf(keywords.joinToString())).use { cursor ->
            if (cursor.moveToFirst()) {
                do {
                    val keyword: String = cursor.getString(0)
                    val likability: Float = cursor.getFloat(1)
                    likabilityDTOs.add(LikabilityDTO(keyword, likability))
                } while (cursor.moveToNext())
            }
        }
        return likabilityDTOs
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