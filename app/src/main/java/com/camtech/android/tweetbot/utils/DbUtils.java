package com.camtech.android.tweetbot.utils;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.support.v4.util.Pair;
import android.util.Log;

import com.camtech.android.tweetbot.data.HistoryContract;

import java.util.ArrayList;
import java.util.List;

import static com.camtech.android.tweetbot.data.HistoryContract.HistoryEntry.COLUMN_KEYWORD;
import static com.camtech.android.tweetbot.data.HistoryContract.HistoryEntry.COLUMN_OCCURRENCES;
import static com.camtech.android.tweetbot.data.HistoryContract.HistoryEntry.CONTENT_URI;

public class DbUtils {

    private static final String TAG = DbUtils.class.getSimpleName();

    public static final String KEYWORD_ASC = COLUMN_KEYWORD + " ASC";
    public static final String KEYWORD_DESC = COLUMN_KEYWORD + " DESC";
    public static final String OCCURRENCES_ASC = COLUMN_OCCURRENCES + " ASC";
    public static final String OCCURRENCES_DESC = COLUMN_OCCURRENCES + " DESC";

    /**
     * Returns a single {@link Pair} containing the key word along with
     * its number of occurrences
     * */
    public static Pair<String, Integer> getKeyWord(Context context, String keyWord) {
        if (isDbEmpty(context)) return null;
        Cursor cursor = null;
        try {
            // We only care about grabbing the keyword
            // and the number of occurrences
            String[] projection = {COLUMN_KEYWORD, COLUMN_OCCURRENCES};
            String[] selectionArgs = new String[]{"%" + keyWord + "%"};
            cursor = context.getContentResolver().query(CONTENT_URI, projection, COLUMN_KEYWORD + " LIKE ?", selectionArgs, null);
            // Loop through the db to see if the word we're searching
            // for exists in the database
            if (cursor != null && cursor.moveToFirst()) {
                String keyWordInDb = cursor.getString(cursor.getColumnIndex(COLUMN_KEYWORD));
                int numOccurrences = cursor.getInt(cursor.getColumnIndex(COLUMN_OCCURRENCES));
                return Pair.create(keyWordInDb, numOccurrences);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) cursor.close();
        }
        return null;
    }

    /**
     * Gets the very last keyword, along with its number of occurrences, from the database
     *
     * @return a {@link Pair} containing the keyword and its number of occurrences
     */
    public static Pair<String, Integer> getMostRecentWord(Context context) {
        if (isDbEmpty(context)) return null;
        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(CONTENT_URI, null, null, null, null);
            if (cursor != null) {
                if (cursor.moveToLast()) {
                    String keyWord = cursor.getString(cursor.getColumnIndex(COLUMN_KEYWORD));
                    int numOccurrences = cursor.getInt(cursor.getColumnIndex(COLUMN_OCCURRENCES));
                    return Pair.create(keyWord, numOccurrences);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) cursor.close();
        }
        return null;
    }

    /**
     * Returns a {@link List} of {@link Pair} objects containing every key word
     * and its number of occurrences in the database. Passing {@code null} for the
     * sort order defaults to the SQL default sort order. If not null, one of
     * {@link #KEYWORD_ASC}, {@link #KEYWORD_DESC}, {@link #OCCURRENCES_ASC}, or {@link #OCCURRENCES_DESC}
     * should be used
     * */
    public static List<Pair<String, Integer>> getAllKeyWords(Context context, String sortOrder) {
        if (isDbEmpty(context)) return null;
        Cursor cursor = null;
        List<Pair<String, Integer>> pairs = new ArrayList<>();
        try {
            cursor = context.getContentResolver().query(CONTENT_URI, null, null, null, sortOrder);
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    while (!cursor.isAfterLast()) {
                        String keyWord = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_KEYWORD));
                        int numOccurrences = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_OCCURRENCES));
                        pairs.add(Pair.create(keyWord, numOccurrences));
                        cursor.moveToNext();
                    }
                }
                return pairs;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return null;
    }

    /**
     * Deletes a single keyword from the database if it exists
     * */
    public static void deleteKeyWord(Context context, String keyWord) {
        try {
            String[] selectionArgs = new String[]{keyWord};
            int rowsDeleted = context.getContentResolver().delete(CONTENT_URI, COLUMN_KEYWORD + " =?", selectionArgs);
            Log.i(TAG, rowsDeleted + " rows deleted");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Deletes every keyword from the database
     * */
    public static void deleteAllKeyWords(Context context) {
        try {
            int rowsDeleted = context.getContentResolver().delete(CONTENT_URI, null, null);
            Log.i(TAG, rowsDeleted + " rows deleted");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Adds a single keyword to the database
     * */
    public static void addKeyWord(Context context, String keyword, int numOccurrences) {
        ContentValues values = new ContentValues();
        values.put(HistoryContract.HistoryEntry.COLUMN_KEYWORD, keyword);
        values.put(HistoryContract.HistoryEntry.COLUMN_OCCURRENCES, numOccurrences);
        Uri uri = context.getContentResolver().insert(HistoryContract.HistoryEntry.CONTENT_URI, values);
        Log.i(TAG, "URI: " + (uri != null ? uri.toString() : ":("));
    }

    public static boolean doesWordExist(Context context, String keyWord) {
        if (isDbEmpty(context)) return false;
        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(
                    CONTENT_URI,
                    new String[]{COLUMN_KEYWORD},
                    null,
                    null,
                    null);
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    while (!cursor.isAfterLast()) {
                        String keyWordInDb = cursor.getString(cursor.getColumnIndex(COLUMN_KEYWORD));
                        if (keyWordInDb.equals(keyWord)) {
                            return true;
                        }
                        cursor.moveToNext();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) cursor.close();
        }
        return false;
    }

    /**
     * Checks if the database is empty. This is used in the beginning of most
     * methods to prevent long unnecessary searching.
     * */
    private static boolean isDbEmpty(Context context) {
        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(CONTENT_URI, new String[]{COLUMN_KEYWORD}, null, null, null);
            return cursor != null && cursor.getCount() <= 0;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return false;
    }
}
