/*******************************************************************************
 * Copyright (c) 2012 MASConsult Ltd
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package eu.masconsult.bgbanking.provider;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.provider.BaseColumns;
import android.text.TextUtils;
import android.util.Log;
import eu.masconsult.bgbanking.BankingApplication;

/**
 * Content provider adapter that does nothing
 */
public class BankingProvider extends ContentProvider {

    // Used for debugging and logging
    private static final String TAG = BankingApplication.TAG + "BankingProvider";

    /**
     * The database that the provider uses as its underlying data store
     */
    private static final String DATABASE_NAME = "bgbanking.db";

    /**
     * The database version
     */
    private static final int DATABASE_VERSION = 3;

    /*
     * Constants used by the Uri matcher to choose an action based on the
     * pattern of the incoming URI
     */
    // The incoming URI matches the Accounts URI pattern
    private static final int ACCOUNTS = 1;

    // The incoming URI matches the Account ID URI pattern
    private static final int ACCOUNT_ID = 2;

    /**
     * A UriMatcher instance
     */
    private static final UriMatcher sUriMatcher;

    // Handle to a new DatabaseHelper.
    private DatabaseHelper mOpenHelper;

    /**
     * A block that instantiates and sets static objects
     */
    static {

        /*
         * Creates and initializes the URI matcher
         */
        // Create a new instance
        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

        // Add a pattern that routes URIs terminated with "accounts" to an
        // accounts operation
        sUriMatcher.addURI(BankingContract.AUTHORITY, "accounts", ACCOUNTS);

        // Add a pattern that routes URIs terminated with "account" plus an
        // integer
        // to an account ID operation
        sUriMatcher.addURI(BankingContract.AUTHORITY, "accounts/#", ACCOUNT_ID);
    }

    /**
     * This class helps open, create, and upgrade the database file. Set to
     * package visibility for testing purposes.
     */
    static class DatabaseHelper extends SQLiteOpenHelper {

        DatabaseHelper(Context context) {

            // calls the super constructor, requesting the default cursor
            // factory.
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        /**
         * Creates the underlying database with table name and column names
         * taken from the NotePad class.
         */
        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE " + BankingContract.BankAccount.TABLE_NAME + " ("
                    + BaseColumns._ID + " INTEGER PRIMARY KEY,"
                    + BankingContract.BankAccount.ACCOUNT_TYPE + " TEXT NOT NULL,"
                    + BankingContract.BankAccount.ACCOUNT_NAME + " TEXT NOT NULL,"
                    + BankingContract.BankAccount.COLUMN_NAME_IBAN + " TEXT,"
                    + BankingContract.BankAccount.COLUMN_NAME_CURRENCY + " TEXT,"
                    + BankingContract.BankAccount.COLUMN_NAME_BALANCE + " FLOAT,"
                    + BankingContract.BankAccount.COLUMN_NAME_AVAILABLE_BALANCE + " FLOAT,"
                    + BankingContract.BankAccount.COLUMN_NAME_LAST_TRANSACTION_DATE + " TEXT"
                    + ");");
            db.execSQL("CREATE INDEX " + BankingContract.BankAccount.TABLE_NAME + "_account ON "
                    + BankingContract.BankAccount.TABLE_NAME + " ("
                    + BankingContract.BankAccount.ACCOUNT_TYPE + ", "
                    + BankingContract.BankAccount.ACCOUNT_NAME + ")");
        }

        /**
         * Demonstrates that the provider must consider what happens when the
         * underlying datastore is changed. In this sample, the database is
         * upgraded the database by destroying the existing data. A real
         * application should upgrade the database in place.
         */
        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

            // Logs that the database is being upgraded
            Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
                    + newVersion + ", which will destroy all old data");

            // Kills the table and existing data
            db.execSQL("DROP TABLE IF EXISTS " + BankingContract.BankAccount.TABLE_NAME);

            // Recreates the database with a new version
            onCreate(db);
        }
    }

    @Override
    public boolean onCreate() {
        Log.v(TAG, "onCreate()");

        // Creates a new helper object. Note that the database itself isn't
        // opened until
        // something tries to access it, and it's only created if it doesn't
        // already exist.
        mOpenHelper = new DatabaseHelper(getContext());

        // Assumes that any failures will be reported by a thrown exception.
        return true;
    }

    @Override
    public String getType(Uri uri) {
        Log.v(TAG, "getType(): " + uri);
        /**
         * Chooses the MIME type based on the incoming URI pattern
         */
        switch (sUriMatcher.match(uri)) {

        // If the pattern is for accounts, returns the general
        // content type.
            case ACCOUNTS:
                return BankingContract.BankAccount.CONTENT_TYPE;

                // If the pattern is for account IDs, returns the account ID
                // content
                // type.
            case ACCOUNT_ID:
                return BankingContract.BankAccount.CONTENT_ITEM_TYPE;

                // If the URI pattern doesn't match any permitted patterns,
                // throws an exception.
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    /**
     * This method is called when a client calls
     * {@link android.content.ContentResolver#query(Uri, String[], String, String[], String)}
     * . Queries the database and returns a cursor containing the results.
     * 
     * @return A cursor containing the results of the query. The cursor exists
     *         but is empty if the query returns no results or an exception
     *         occurs.
     * @throws IllegalArgumentException if the incoming URI pattern is invalid.
     */
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
        Log.v(TAG, "query(): " + uri);

        // Constructs a new query builder and sets its table name
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        qb.setTables(BankingContract.BankAccount.TABLE_NAME);

        /**
         * Choose the projection and adjust the "where" clause based on URI
         * pattern-matching.
         */
        switch (sUriMatcher.match(uri)) {
        // If the incoming URI is for account, chooses the Accounts projection
            case ACCOUNTS:
                break;

            /*
             * If the incoming URI is for a single account identified by its ID,
             * chooses the account ID projection, and appends
             * "_ID = <accountID>" to the where clause, so that it selects that
             * single account
             */
            case ACCOUNT_ID:
                qb.appendWhere(
                        BaseColumns._ID + // the name of the ID column
                                "=" +
                                // the position of the account ID itself in the
                                // incoming URI
                                uri.getPathSegments().get(
                                        BankingContract.BankAccount.ACCOUNT_ID_PATH_POSITION));
                break;

            default:
                // If the URI doesn't match any of the known patterns, throw an
                // exception.
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        String orderBy;
        // If no sort order is specified, uses the default
        if (TextUtils.isEmpty(sortOrder)) {
            orderBy = BankingContract.BankAccount.DEFAULT_SORT_ORDER;
        } else {
            // otherwise, uses the incoming sort order
            orderBy = sortOrder;
        }

        // Opens the database object in "read" mode, since no writes need to be
        // done.
        SQLiteDatabase db = mOpenHelper.getReadableDatabase();

        /*
         * Performs the query. If no problems occur trying to read the database,
         * then a Cursor object is returned; otherwise, the cursor variable
         * contains null. If no records were selected, then the Cursor object is
         * empty, and Cursor.getCount() returns 0.
         */
        Cursor c = qb.query(
                db, // The database to query
                projection, // The columns to return from the query
                selection, // The columns for the where clause
                selectionArgs, // The values for the where clause
                null, // don't group the rows
                null, // don't filter by row groups
                orderBy // The sort order
                );

        // Tells the Cursor what URI to watch, so it knows when its source data
        // changes
        c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
    }

    /**
     * This is called when a client calls
     * {@link android.content.ContentResolver#insert(Uri, ContentValues)}.
     * Inserts a new row into the database. This method sets up default values
     * for any columns that are not included in the incoming map. If rows were
     * inserted, then listeners are notified of the change.
     * 
     * @return The row ID of the inserted row.
     * @throws SQLException if the insertion fails.
     */
    @Override
    public Uri insert(Uri uri, ContentValues initialValues) {
        Log.v(TAG, "insert(): " + uri);

        // Validates the incoming URI. Only the full provider URI is allowed for
        // inserts.
        if (sUriMatcher.match(uri) != ACCOUNTS) {
            throw new IllegalArgumentException("Unknown URI " + uri);
        }

        // A map to hold the new record's values.
        ContentValues values;

        // If the incoming values map is not null, uses it for the new values.
        if (initialValues != null) {
            values = new ContentValues(initialValues);

        } else {
            // Otherwise, create a new value map
            values = new ContentValues();
        }

        // Opens the database object in "write" mode.
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();

        // Performs the insert and returns the ID of the new note.
        long rowId = db.insert(
                BankingContract.BankAccount.TABLE_NAME, // The table to
                                                        // insert into.
                null, // A hack, SQLite sets this column value to null
                      // if values is empty.
                values // A map of column names, and the values to insert
                       // into the columns.
                );

        // If the insert succeeded, the row ID exists.
        if (rowId > 0) {
            // Creates a URI with the note ID pattern and the new row ID
            // appended to it.
            Uri accountUri = ContentUris.withAppendedId(
                    BankingContract.BankAccount.CONTENT_ID_URI_BASE, rowId);

            // Notifies observers registered against this provider that the data
            // changed.
            getContext().getContentResolver().notifyChange(accountUri, null);
            return accountUri;
        }

        // If the insert didn't succeed, then the rowID is <= 0. Throws an
        // exception.
        throw new SQLException("Failed to insert row into " + uri);
    }

    /**
     * This is called when a client calls
     * {@link android.content.ContentResolver#update(Uri,ContentValues,String,String[])}
     * Updates records in the database. The column names specified by the keys
     * in the values map are updated with new data specified by the values in
     * the map. If the incoming URI matches the note ID URI pattern, then the
     * method updates the one record specified by the ID in the URI; otherwise,
     * it updates a set of records. The record or records must match the input
     * selection criteria specified by where and whereArgs. If rows were
     * updated, then listeners are notified of the change.
     * 
     * @param uri The URI pattern to match and update.
     * @param values A map of column names (keys) and new values (values).
     * @param where An SQL "WHERE" clause that selects records based on their
     *            column values. If this is null, then all records that match
     *            the URI pattern are selected.
     * @param whereArgs An array of selection criteria. If the "where" param
     *            contains value placeholders ("?"), then each placeholder is
     *            replaced by the corresponding element in the array.
     * @return The number of rows updated.
     * @throws IllegalArgumentException if the incoming URI pattern is invalid.
     */
    @Override
    public int update(Uri uri, ContentValues values, String where, String[] whereArgs) {
        Log.v(TAG, "update(): " + uri);

        // Opens the database object in "write" mode.
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int count;
        String finalWhere;

        // Does the update based on the incoming URI pattern
        switch (sUriMatcher.match(uri)) {

        // If the incoming URI matches the general accounts pattern, does the
        // update based on
        // the incoming data.
            case ACCOUNTS:
                Log.d(TAG, "updating " + where);
                // Does the update and returns the number of rows updated.
                count = db.update(
                        BankingContract.BankAccount.TABLE_NAME, // The
                                                                // database
                                                                // table
                                                                // name.
                        values, // A map of column names and new values to use.
                        where, // The where clause column names.
                        whereArgs // The where clause column values to select
                                  // on.
                        );
                break;

            // If the incoming URI matches a single account ID, does the update
            // based on the incoming
            // data, but modifies the where clause to restrict it to the
            // particular account ID.
            case ACCOUNT_ID:
                // From the incoming URI, get the note ID
                String accountId = uri.getPathSegments().get(
                        BankingContract.BankAccount.ACCOUNT_ID_PATH_POSITION);

                /*
                 * Starts creating the final WHERE clause by restricting it to
                 * the incoming account ID.
                 */
                finalWhere =
                        BaseColumns._ID + // The ID column name
                                " = "
                                + accountId; // test for equality the incoming
                                             // account
                                             // ID

                // If there were additional selection criteria, append them to
                // the final WHERE
                // clause
                if (where != null) {
                    finalWhere = finalWhere + " AND " + where;
                }

                Log.d(TAG, "updating " + finalWhere);
                // Does the update and returns the number of rows updated.
                count = db.update(
                        BankingContract.BankAccount.TABLE_NAME, // The
                                                                // database
                                                                // table
                                                                // name.
                        values, // A map of column names and new values to use.
                        finalWhere, // The final WHERE clause to use
                                    // placeholders for whereArgs
                        whereArgs // The where clause column values to select
                                  // on, or
                                  // null if the values are in the where
                                  // argument.
                        );
                break;
            // If the incoming pattern is invalid, throws an exception.
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        /*
         * Gets a handle to the content resolver object for the current context,
         * and notifies it that the incoming URI changed. The object passes this
         * along to the resolver framework, and observers that have registered
         * themselves for the provider are notified.
         */
        getContext().getContentResolver().notifyChange(uri, null);

        // Returns the number of rows updated.
        return count;
    }

    /**
     * This is called when a client calls
     * {@link android.content.ContentResolver#delete(Uri, String, String[])}.
     * Deletes records from the database. If the incoming URI matches the note
     * ID URI pattern, this method deletes the one record specified by the ID in
     * the URI. Otherwise, it deletes a a set of records. The record or records
     * must also match the input selection criteria specified by where and
     * whereArgs. If rows were deleted, then listeners are notified of the
     * change.
     * 
     * @return If a "where" clause is used, the number of rows affected is
     *         returned, otherwise 0 is returned. To delete all rows and get a
     *         row count, use "1" as the where clause.
     * @throws IllegalArgumentException if the incoming URI pattern is invalid.
     */
    @Override
    public int delete(Uri uri, String where, String[] whereArgs) {
        Log.v(TAG, "delete(): " + uri);

        // Opens the database object in "write" mode.
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        String finalWhere;

        int count;

        // Does the delete based on the incoming URI pattern.
        switch (sUriMatcher.match(uri)) {

        // If the incoming pattern matches the general pattern for accounts,
        // does
        // a delete based on the incoming "where" columns and arguments.
            case ACCOUNTS:
                count = db.delete(
                        BankingContract.BankAccount.TABLE_NAME, // The
                                                                // database
                                                                // table name
                        where, // The incoming where clause column names
                        whereArgs // The incoming where clause values
                        );
                break;

            // If the incoming URI matches a single account ID, does the delete
            // based on the
            // incoming data, but modifies the where clause to restrict it to
            // the
            // particular account ID.
            case ACCOUNT_ID:
                /*
                 * Starts a final WHERE clause by restricting it to the desired
                 * note ID.
                 */
                finalWhere =
                        BaseColumns._ID + // The ID column name
                                " = "
                                + // test for equality
                                uri.getPathSegments()
                                        . // the incoming account ID
                                        get(BankingContract.BankAccount.ACCOUNT_ID_PATH_POSITION);

                // If there were additional selection criteria, append them to
                // the final
                // WHERE clause
                if (where != null) {
                    finalWhere = finalWhere + " AND " + where;
                }

                // Performs the delete.
                count = db.delete(
                        BankingContract.BankAccount.TABLE_NAME, // The
                                                                // database
                                                                // table
                                                                // name.
                        finalWhere, // The final WHERE clause
                        whereArgs // The incoming where clause values.
                        );
                break;

            // If the incoming pattern is invalid, throws an exception.
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        /*
         * Gets a handle to the content resolver object for the current context,
         * and notifies it that the incoming URI changed. The object passes this
         * along to the resolver framework, and observers that have registered
         * themselves for the provider are notified.
         */
        getContext().getContentResolver().notifyChange(uri, null);

        // Returns the number of rows deleted.
        return count;
    }

}
