package com.camtech.android.tweetbot.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

import com.camtech.android.tweetbot.data.HistoryContract.HistoryEntry.*

class HistoryDbHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    private companion object {
        private const val DATABASE_VERSION = 1
        private const val DATABASE_NAME = "history.db"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTable = "CREATE TABLE $TABLE_NAME ($_ID INTEGER PRIMARY KEY AUTOINCREMENT, $COLUMN_KEYWORD TEXT, $COLUMN_OCCURRENCES INTEGER, UNIQUE($COLUMN_KEYWORD) ON CONFLICT REPLACE);"
        // UNIQUE(COLUMN_KEYWORD) is used to make sure that no duplicate
        // key words are added. In the event that the same word is added
        // again, the number of occurrences will be overridden for that word
        db.execSQL(createTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // It isn't likely that the schema for this database is
        // going to change so onUpgrade hasn't been implemented yet
    }
}
