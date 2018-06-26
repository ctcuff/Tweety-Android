package com.camtech.android.tweetbot.data;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import static com.camtech.android.tweetbot.data.HistoryContract.HistoryEntry;

public class HistoryContentProvider extends ContentProvider {
    private static final String TAG = HistoryContentProvider.class.getSimpleName();

    private static final int HISTORY = 100;
    private static final int HISTORY_ID = 101;

    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        sUriMatcher.addURI(HistoryContract.CONTENT_AUTHORITY, HistoryContract.PATH_HISTORY, HISTORY);
        sUriMatcher.addURI(HistoryContract.CONTENT_AUTHORITY, HistoryContract.PATH_HISTORY + "/#", HISTORY_ID);
    }

    public HistoryDbHelper dbHelper;

    @Override
    public boolean onCreate() {
        dbHelper = new HistoryDbHelper(getContext());
        return true;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection, @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        SQLiteDatabase database = dbHelper.getReadableDatabase();
        Cursor cursor;
        switch (sUriMatcher.match(uri)) {
            case HISTORY:
                cursor = database.query(
                        HistoryEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder);
                break;
            case HISTORY_ID:
                selection = HistoryEntry._ID + "=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};
                cursor = database.query(
                        HistoryEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder);
                break;
            default:
                throw new IllegalArgumentException("Cannot query, unknown URI " + uri);
        }
        cursor.setNotificationUri(getContext().getContentResolver(), uri);
        return cursor;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case HISTORY:
                return HistoryEntry.CONTENT_LIST_TYPE;
            case HISTORY_ID:
                return HistoryEntry.CONTENT_ITEM_TYPE;
            default:
                throw new IllegalStateException("Unknown URI " + uri + " with match " + match);
        }
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        switch (sUriMatcher.match(uri)) {
            case HISTORY:
                SQLiteDatabase database = dbHelper.getWritableDatabase();
                long id = database.insert(HistoryEntry.TABLE_NAME, null, values);
                // If the ID is -1, then the insertion failed. Log an error and return null.
                if (id == -1) {
                    Log.e(TAG, "Failed to insert row for " + uri);
                    return null;
                }
                // Notify listeners that the data has changed
                getContext().getContentResolver().notifyChange(uri, null);
                // Once we know the ID of the new row in the table,
                // return the new URI with the ID appended to the end of it
                return ContentUris.withAppendedId(uri, id);
            default:
                throw new IllegalArgumentException("Insertion is not supported for " + uri);
        }
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        SQLiteDatabase database = dbHelper.getWritableDatabase();

        int rowsDeleted;

        final int match = sUriMatcher.match(uri);
        switch (match) {
            case HISTORY:
                rowsDeleted = database.delete(HistoryEntry.TABLE_NAME, selection, selectionArgs);
                break;
            case HISTORY_ID:
                selection = HistoryEntry._ID + "=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};
                rowsDeleted = database.delete(HistoryEntry.TABLE_NAME, selection, selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Deletion is not supported for " + uri);
        }
        if (rowsDeleted != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return rowsDeleted;
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection, @Nullable String[] selectionArgs) {
        // Since we won't support updating keywords,
        // this doesn't need to be implemented yet
        throw new UnsupportedOperationException();
    }
}
