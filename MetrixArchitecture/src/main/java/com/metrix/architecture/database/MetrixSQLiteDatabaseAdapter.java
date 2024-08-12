package com.metrix.architecture.database;

import android.content.ContentValues;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.CursorWindow;
import android.database.SQLException;
import android.database.sqlite.SQLiteCursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.DatabaseUtils.*;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.*;

import com.metrix.architecture.BuildConfig;
import com.metrix.architecture.utilities.*;

/**
 * This class contains database type specific implementations for the abstract
 * methods defined on MetrixDatabaseAdapter for the SQLite database library.
 *
 * @since 5.6
 */
class MetrixSQLiteDatabaseAdapter extends MetrixDatabaseAdapter {

	private final Context context;

	private MetrixDatabaseHelper databaseHelper;
	private SQLiteDatabase database;

	@SuppressWarnings("deprecation")
	public int getType(Cursor cursor, int i) throws Exception {
		SQLiteCursor sqLiteCursor = (SQLiteCursor) cursor;
		CursorWindow cursorWindow = sqLiteCursor.getWindow();
		int pos = cursor.getPosition();
		int type = -1;
		if (cursorWindow.isNull(pos, i)) {
			type = FIELD_TYPE_NULL;
		} else if (cursorWindow.isLong(pos, i)) {
			type = FIELD_TYPE_INTEGER;
		} else if (cursorWindow.isFloat(pos, i)) {
			type = FIELD_TYPE_FLOAT;
		} else if (cursorWindow.isString(pos, i)) {
			type = FIELD_TYPE_STRING;
		} else if (cursorWindow.isBlob(pos, i)) {
			type = FIELD_TYPE_BLOB;
		}

		return type;
	}

	public MetrixSQLiteDatabaseAdapter(Context context) {
		super(context);
		this.context = context;

		databaseHelper = MetrixDatabaseHelper.getHelper(context);

		try {
			database = databaseHelper.getWritableDatabase();
		} catch (Exception ex) {
			LogManager.getInstance(context).error(ex);
			database = databaseHelper.getReadableDatabase();
		}
	}

	public MetrixSQLiteDatabaseAdapter(Context context, int databaseVersion, int rid_system, int rid_business) {
		super(context);
		this.context = context;

		databaseHelper = MetrixDatabaseHelper.getHelper(context, databaseVersion, rid_system, rid_business);

		try {
			database = databaseHelper.getWritableDatabase();
		} catch (Exception ex) {
			LogManager.getInstance(context).error(ex);
			database = databaseHelper.getReadableDatabase();
		}
	}

	/**
	 * Opens a new database adapter and returns it.
	 *
	 * @return the new database adapter.
	 * @throws SQLException
	 */
	public MetrixDatabaseAdapter open(boolean foreignKeyEnabled) throws SQLException {
		try {
			this.database = databaseHelper.getWritableDatabase();

			if (this.database.isReadOnly() == false && foreignKeyEnabled) {
				// enable foreign key relationship
				this.database.execSQL("PRAGMA foreign_keys=ON;");
			}

		} catch (Exception ex) {
			LogManager.getInstance(context).error(ex);
			databaseHelper.onCreate(this.database);
		}
		return this;
	}

	/**
	 * Closes the database helper.
	 */
	public void close() {
		try {
			databaseHelper.close();
		} catch (Exception ex) {
			LogManager.getInstance(context).error(ex);
		}
	}

	/**
	 * Inserts a row into a table with the received values.
	 *
	 * @param databaseName
	 *            the database to insert the data into.
	 * @param activity
	 *            the currently running activity.
	 * @param tableName
	 *            the table to insert the data into.
	 * @param fields
	 *            the fields and value to insert.
	 * @return the row ID of the newly inserted row, or -1 if an error occurred
	 */
	@SuppressWarnings("deprecation")
	public long insertRow(String tableName, ArrayList<DataField> fields) {
		long retvalue = 0;
		// Create a single InsertHelper to handle this set of insertions.
		InsertHelper ih = null;

		try {
			ih = new InsertHelper(this.database, tableName);
			ih.prepareForInsert();
			for (DataField field : fields) {
				int idCol = ih.getColumnIndex(field.name);
				ih.bind(idCol, field.value);
			}

			retvalue = ih.execute();
		} catch (SQLException ex) {
			LogManager.getInstance(context).error("Database InsertRow in table " + tableName + " " + ex.getMessage());
		} catch (Exception e) {
			LogManager.getInstance(context).error("Database InsertRow Caught Exception in table " + tableName + " " + e.getMessage());
		} finally {
			ih.close();
		}

		return retvalue;
	}

	/**
	 * Deletes a row from the database based on the received where clause.
	 *
	 * @param databaseName
	 *            the database to delete the data from.
	 * @param activity
	 *            the currently running activity.
	 * @param tableName
	 *            the table to delete the row(s) from.
	 * @param whereClause
	 *            the where clause to use on the delete.
	 * @return TRUE if the delete was successful, FALSE otherwise.
	 */
	public boolean deleteRow(String tableName, String whereClause) {
		try {
			return this.database.delete(tableName, whereClause, null) > 0;
		} catch (Exception ex) {
			LogManager.getInstance(context).error(ex);
			return false;
		}
	}

	/**
	 * Gets all rows from the table whose name was received.
	 *
	 * @param databaseName
	 *            the database to get the data from.
	 * @param activity
	 *            the currently running activity.
	 * @param tableName
	 *            the table to get the row(s) from.
	 * @param columns
	 *            a list of columns to select.
	 * @param whereClause
	 * 		 	  where clause without where.
	 * @param orderBy
	 *            order by clause without order by.
	 * @return a cursor containing the selected rows.
	 */
	public Cursor getRows(String tableName, String[] columns, String whereClause, String orderBy) {
		try {
			return this.database.query(tableName, columns, whereClause, null, null, null, orderBy);
		} catch (Exception ex) {
			LogManager.getInstance(context).error(ex);
			return null;
		}
	}

	/**
	 * Executes a sql statement.
	 *
	 * @param databaseName
	 *            the database to run the sql statement against.
	 * @param activity
	 *            the currently running activity.
	 * @param sql
	 *            the sql statement.
	 * @return TRUE if the statement was successfully run, FALSE otherwise.
	 */
	public boolean executeSql(String sql) {
		try {
			if(BuildConfig.DEBUG) {
				LogManager.getInstance(context).debug(sql);
			}

			this.database.execSQL(sql);
			return true;
		} catch (Exception ex) {
			LogManager.getInstance(context).error(ex);
			return false;
		}
	}

	/**
	 * Executes a sql query and returns the result in a cursor.
	 *
	 * @param databaseName
	 *            the database to run the sql statement against.
	 * @param activity
	 *            the currently running activity.
	 * @param sql
	 *            the sql statement.
	 * @param selectionArgs
	 *            You may include ?s in where clause in the query, which will be
	 *            replaced by the values from selectionArgs. The values will be
	 *            bound as Strings.
	 * @return the cursor with the resulting data.
	 */
	public Cursor rawQuery(String sql, String[] selectionArgs) {
		try {
			if(BuildConfig.DEBUG) {
				LogManager.getInstance(context).debug(sql);
			}

			return this.database.rawQuery(sql, selectionArgs);
		} catch (SQLiteException ex) {
			LogManager.getInstance(context).error(ex);
			return null;
		}
	}

	/**
	 * Executes an array of sql statements.
	 *
	 * @param sqlArray
	 *            an array of sql statements to issue.
	 * @return TRUE if all statements were successfully run, FALSE otherwise.
	 */
	public boolean executeSqlArray(ArrayList<String> sqlArray) {
		try {
			this.database.beginTransaction();

			for (String sql : sqlArray) {
				this.database.execSQL(sql);
			}

			this.database.setTransactionSuccessful();
			return true;
		} catch (SQLException ex) {
			LogManager.getInstance(context).error(ex);
			return false;
		} finally {
			this.database.endTransaction();
		}
	}

	public void beginTransaction() {
		try {
			this.database.beginTransaction();
		} catch (Exception ex) {
			LogManager.getInstance(context).error(ex);
		}
	}

	public void endTransaction() {
		try {
			this.database.endTransaction();
		} catch (Exception ex) {
			LogManager.getInstance(context).error(ex);
		}
	}

	public void setTransactionSuccessful() {
		try {
			this.database.setTransactionSuccessful();
		} catch (Exception ex) {
			LogManager.getInstance(context).error(ex);
		}
	}

	/**
	 * Executes an array of sql statements.
	 *
	 * @param statements
	 *            an array of sql statements to issue.
	 * @return TRUE if all statements were successfully run, FALSE otherwise.
	 */
	@SuppressWarnings("finally")
	public boolean executeSqlArray(String[] statements) {
		Boolean exceptionThrow = false;

		try {
			this.database.beginTransaction();

			for (String statement : statements) {
				if(BuildConfig.DEBUG) {
					LogManager.getInstance(context).debug(statement);
				}
				this.database.execSQL(statement);
			}

			this.database.setTransactionSuccessful();
		} catch (SQLException ex) {
			LogManager.getInstance(context).error(ex);
			exceptionThrow = true;
			throw ex;
		} finally {
			this.database.endTransaction();

			if (exceptionThrow)
				return false;
			else
				return true;
		}
	}

	/**
	 * Gets a row of data from the database based on a where clause.
	 *
	 * @param databaseName
	 *            the database to run the sql statement against.
	 * @param activity
	 *            the currently running activity.
	 * @param tableName
	 *            the table to get the rows from.
	 * @param columns
	 *            the columns to select.
	 * @param whereClause
	 *            the where clause to filter the select on.
	 * @return a cursor containing the row(s).
	 */
	public Cursor getRow(String tableName, String[] columns, String whereClause) throws SQLException {
		try {
			Cursor mCursor = this.database.query(true, tableName, columns, whereClause, null, null, null, null, null);
			if (mCursor != null) {
				mCursor.moveToFirst();
			}
			return mCursor;
		} catch (SQLException ex) {
			LogManager.getInstance(context).error(ex);
			throw ex;
		}
	}

	/**
	 * Gets a specific column value from a row based on a where clause.
	 *
	 * @param distinct
	 * @param table
	 *            the table to get the rows from.
	 * @param columnName
	 *            the column to select
	 * @param selection
	 *            the where clause to filter the select on.
	 * @param selectionArgs
	 * @param groupBy
	 * @param having
	 * @param orderBy
	 * @param limit
	 * @return The resulting value as a string.
	 */
	public String getFieldStringValue(boolean distinct, String table, String columnName, String selection, String[] selectionArgs, String groupBy, String having, String orderBy, String limit) {
		String value = "";
		String[] columns = { columnName };
		MetrixCursor cursor = null;

		try {
			cursor = new MetrixCursor(this.database.query(distinct, table, columns, selection, selectionArgs, groupBy, having, orderBy, limit));

			if (cursor != null && cursor.getCount() > 0) {
				cursor.moveToFirst();
				int colIndex = cursor.getColumnIndex(columnName);
				value = cursor.getString(colIndex);
			}
		} catch (SQLException ex) {
			LogManager.getInstance(context).error(ex);
			throw ex;
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}

		return value;
	}

	/**
	 * Gets a hash table of column values from a row based on a where clause.
	 *
	 * @param databaseName
	 *            the database to run the sql statement against.
	 * @param activity
	 *            the currently running activity.
	 * @param tableName
	 *            the table to get the rows from.
	 * @param column
	 *            the column to select
	 * @param whereClause
	 *            the where clause to filter the select on.
	 * @return a hash table containing name value pairs of the requested columns
	 *         and their values.
	 * @throws SQLException
	 */
	public Hashtable<String, String> getFieldStringValues(String tableName, String[] columnNames, String whereClause) throws SQLException {
		MetrixCursor cursor = null;
		Hashtable<String, String> results = new Hashtable<String, String>();

		try {
			cursor = new MetrixCursor(this.database.query(true, tableName, columnNames, whereClause, null, null, null, null, null));

			if (cursor != null && cursor.getCount() > 0) {
				cursor.moveToFirst();

				for (String columnName : columnNames) {
					int index = cursor.getColumnIndex(columnName);
					String value = cursor.getString(index);

					if (value == null)
						value = "";
					results.put(columnName, value);
				}
			}
		} catch (SQLException ex) {
			LogManager.getInstance(context).error(ex);
			throw ex;
		} finally {
			if (cursor != null) {
				cursor.close();
			}

		}

		return results;
	}

	/**
	 * Return datarow list with multiple
	 *
	 * @param tableName
	 * @param columnNames
	 * @param selection
	 * @param selectionArgs
	 * @param groupBy
	 * @param orderBy
	 * @param limit
	 * @return
	 * @throws SQLException
	 */
	public ArrayList<Hashtable<String, String>> getFieldStringValuesList(String tableName, String[] columnNames, String selection, String[] selectionArgs, String groupBy, String orderBy, String limit) throws SQLException {
		MetrixCursor cursor = null;
		ArrayList<Hashtable<String, String>> resultsList = new ArrayList<Hashtable<String, String>>();

		try {
			cursor = new MetrixCursor(this.database.query(true, tableName, columnNames, selection, selectionArgs, groupBy, null, orderBy, limit));

			if (cursor == null || !cursor.moveToFirst()) {
				return null;
			}

			while (cursor.isAfterLast() == false) {
				Hashtable<String, String> results = new Hashtable<String, String>();
				for (String columnName : columnNames) {
					int index = cursor.getColumnIndex(columnName);
					String value = cursor.getString(index);
					if (value == null)
						value = "";

					results.put(columnName, value);
				}

				resultsList.add(results);
				cursor.moveToNext();
			}
		} catch (SQLException ex) {
			LogManager.getInstance(context).error(ex);
			throw ex;
		} finally {
			if (cursor != null) {
				cursor.close();
			}

		}

		return resultsList;
	}

	/**
	 * Update a specific row based on the where condition with new values passed
	 * in an array of data field.
	 *
	 * @param databaseName
	 *            the database to run the sql statement against.
	 * @param activity
	 *            the currently running activity.
	 * @param tableName
	 *            the table to get the rows from.
	 * @param fields
	 *            an array of DataField identifying the new column values.
	 * @param whereClause
	 *            the where clause to filter the select on.
	 * @return TRUE if the row was successfully updated, FALSE otherwise.
	 */
	public boolean updateRow(String tableName, ArrayList<DataField> fields, String whereCondition) {
		ContentValues args = new ContentValues();

		try {
			for (DataField field : fields) {
				args.put(field.name, field.value);
			}

			return this.database.update(tableName, args, whereCondition, null) > 0;
		} catch (SQLException ex) {
			LogManager.getInstance(context).error(ex);
			return false;
		}
	}
}
