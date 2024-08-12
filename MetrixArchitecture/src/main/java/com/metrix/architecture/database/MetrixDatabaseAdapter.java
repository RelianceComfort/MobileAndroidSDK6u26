package com.metrix.architecture.database;

import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.os.Environment;
import android.widget.Toast;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;

import com.metrix.architecture.constants.MetrixTransactionTypes;
import com.metrix.architecture.metadata.MetrixSqlData;
import com.metrix.architecture.utilities.*;

abstract class MetrixDatabaseAdapter {
	public static final String KEY_ROWID = "row_id";
	private final Context context;

	protected static final int FIELD_TYPE_BLOB = 4;
	protected static final int FIELD_TYPE_FLOAT = 2;
	protected static final int FIELD_TYPE_INTEGER = 1;
	protected static final int FIELD_TYPE_NULL = 0;
	protected static final int FIELD_TYPE_STRING = 3;

	public MetrixDatabaseAdapter(Context context) {
		this.context = context;
	}

	/**
	 * Opens a new database adapter and returns it.
	 *
	 * @return the new database adapter.
	 * @throws SQLException
	 */
	public MetrixDatabaseAdapter open() throws SQLException {
		return open(true);
	}

	public abstract MetrixDatabaseAdapter open(boolean foreignKeyEnabled) throws SQLException;

	/**
	 * Closes the database helper.
	 */
	public abstract void close();

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
	public abstract long insertRow(String tableName, ArrayList<DataField> fields);

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
	public abstract boolean deleteRow(String tableName, String whereClause);

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
	 * @return a cursor containing the selected rows.
	 */
	public Cursor getRows(String tableName, String[] columns) {
		return this.getRows(tableName, columns, null, null);
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
	 *
	 * @return a cursor containing the selected rows.
	 */
	public Cursor getRows(String tableName, String[] columns, String whereClause) {
		return this.getRows(tableName, columns, whereClause, null);
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
	public abstract Cursor getRows(String tableName, String[] columns, String whereClause, String orderBy);

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
	public abstract boolean executeSql(String sql);

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
	public abstract Cursor rawQuery(String sql, String[] selectionArgs);

	/**
	 * Executes an array of sql statements.
	 *
	 * @param sqlArray
	 *            an array of sql statements to issue.
	 * @return TRUE if all statements were successfully run, FALSE otherwise.
	 */
	public abstract boolean executeSqlArray(ArrayList<String> sqlArray);

	public abstract void beginTransaction();

	public abstract void endTransaction();

	public abstract void setTransactionSuccessful();

	/**
	 * Executes an array of sql statements.
	 *
	 * @param statements
	 *            an array of sql statements to issue.
	 * @return TRUE if all statements were successfully run, FALSE otherwise.
	 */
	public abstract boolean executeSqlArray(String[] statements);

	/**
	 * Executes an array of sql statements.
	 *
	 * @param statements
	 *            an array of sql statements to issue.
	 * @return TRUE if all statements were successfully run, FALSE otherwise.
	 */
	@SuppressWarnings("finally")
	public boolean executeDataFieldArray(ArrayList<MetrixSqlData> dataList, boolean startTransaction) {
		Boolean exceptionThrow = false;
		Boolean sqlSuccess = false;

		try {
			if (startTransaction) {
				this.beginTransaction();
			}

			for (MetrixSqlData dataRow : dataList) {

				if (dataRow.transactionType == MetrixTransactionTypes.INSERT) {
					dataRow.rowId = this.insertRow(dataRow.tableName, dataRow.dataFields);
					sqlSuccess = dataRow.rowId > 0;
				} else if (dataRow.transactionType == MetrixTransactionTypes.UPDATE) {
					sqlSuccess = this.updateRow(dataRow.tableName, dataRow.dataFields, dataRow.filter);
				} else if (dataRow.transactionType == MetrixTransactionTypes.DELETE) {
					sqlSuccess = this.deleteRow(dataRow.tableName, dataRow.filter);
				}
			}

			if (startTransaction) {
				this.setTransactionSuccessful();
			}
		} catch (SQLException ex) {
			LogManager.getInstance(context).error(ex);
			exceptionThrow = true;
			throw ex;
		} finally {
			if (startTransaction) {
				this.endTransaction();
			}

			if (exceptionThrow)
                sqlSuccess = false;
		}

		return sqlSuccess;
	}

	public abstract int getType(Cursor cursor, int i) throws Exception;

	/**
	 * Gets a count of the rows which match the received where clause.
	 *
	 * @param databaseName
	 *            the database to get the count from.
	 * @param activity
	 *            the currently running activity.
	 * @param tableName
	 *            the table to perform the query on.
	 * @param whereClause
	 *            the where clause to apply.
	 * @return an integer identifying the number of rows which match the where
	 *         clause.
	 * @throws SQLException
	 */
	public int getCount(String tableName, String whereClause) throws SQLException {
		if (MetrixStringHelper.isNullOrEmpty(tableName)) {
			return 0;
		}

		StringBuilder query = new StringBuilder("select count(*) from ");
		query.append(tableName);

		if (!MetrixStringHelper.isNullOrEmpty(whereClause)) {
			query.append(" where ");
			query.append(whereClause);
		}

		Cursor cursor = null;
		try {
			cursor = this.rawQuery(query.toString(), null);

			if (cursor == null || cursor.getCount() == 0) {
				return 0;
			} else {
				cursor.moveToFirst();
				return cursor.getInt(0);
			}
        }catch (Exception ex) {
            LogManager.getInstance(context).error(ex);
            return 0;
        }
        finally {
            if (cursor != null) {
                cursor.close();
            }
        }
	}

	/**
	 * Gets a count of the rows which match the received where clause.
	 *
	 * @param databaseName
	 *            the database to get the count from.
	 * @param activity
	 *            the currently running activity.
	 * @param tableNames
	 *            a list of tables to perform the query on.
	 * @param whereClause
	 *            the where clause to apply.
	 * @return an integer identifying the number of rows which match the where
	 *         clause.
	 * @throws SQLException
	 */
	public int getCount(String[] tableNames, String whereClause) throws SQLException {
		if (tableNames.length == 0) {
			return 0;
		}

		StringBuilder query = new StringBuilder("select count(*) from ");

		if (tableNames.length == 1) {
			query.append(tableNames[0]);
		} else {
			int i = 0;
			for (String tableName : tableNames) {
				if (i > 0) {
					query.append(", ");
				}
				query.append(tableName);
				i = i + 1;
			}
		}

		if (!MetrixStringHelper.isNullOrEmpty(whereClause)) {
			query.append(" where ");
			query.append(whereClause);
		}

		Cursor cursor = null;
		try {
			cursor = this.rawQuery(query.toString(), null);

			if (cursor == null || cursor.getCount() == 0) {
				return 0;
			} else {
				cursor.moveToFirst();
				return cursor.getInt(0);
			}
		}catch (Exception ex) {
			LogManager.getInstance(context).error(ex);
			return 0;
		}
		finally {
			if (cursor != null) {
				cursor.close();
			}
		}
	}

	/**
	 * Gets a row of data from the database based on a row id.
	 *
	 * @param databaseName
	 *            the database to run the sql statement against.
	 * @param activity
	 *            the currently running activity.
	 * @param tableName
	 *            the table to get the rows from.
	 * @param columns
	 *            the columns to select.
	 * @param rowId
	 *            the id for the row to select.
	 * @return a cursor containing the row.
	 */
	public Cursor getRow(String tableName, String[] columns, long rowId) throws SQLException {
		return this.getRow(tableName, columns, KEY_ROWID + "=" + rowId);
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
	public abstract Cursor getRow(String tableName, String[] columns, String whereClause) throws SQLException;

	/**
	 * Gets a specific column value from a row based on a where clause.
	 *
	 * @param tableName
	 *            the table to get the rows from.
	 * @param columnName
	 *            the column to select
	 * @param whereClause
	 *            the where clause to filter the select on.
	 * @return The resulting value as a string.
	 * @throws SQLException
	 */
	public String getFieldStringValue(String tableName, String columnName, String whereClause) throws SQLException {
		return getFieldStringValue(true, tableName, columnName, whereClause, null, null, null, null, null);
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
	public abstract String getFieldStringValue(boolean distinct, String table, String columnName, String selection, String[] selectionArgs, String groupBy, String having, String orderBy, String limit);

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
	public abstract Hashtable<String, String> getFieldStringValues(String tableName, String[] columnNames, String whereClause) throws SQLException;

	/**
	 * Gets a ArrayList of rows defined with hash table of column values from a
	 * row based on a where clause. It is like rows of data object
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
	public ArrayList<Hashtable<String, String>> getFieldStringValuesList(String tableName, String[] columnNames, String whereClause) throws SQLException {
		return getFieldStringValuesList(tableName,columnNames, whereClause, null, null, null, null);
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
	public abstract ArrayList<Hashtable<String, String>> getFieldStringValuesList(String tableName, String[] columnNames, String selection, String[] selectionArgs, String groupBy, String orderBy, String limit) throws SQLException;

	public ArrayList<Hashtable<String, String>> getFieldStringValuesList(String sqlStatement) {
		MetrixCursor cursor = null;
		ArrayList<Hashtable<String, String>> valueList = new ArrayList<Hashtable<String, String>>();

		try {
			cursor = MetrixDatabaseManager.rawQueryMC(sqlStatement, null);

			if (cursor == null || !cursor.moveToFirst()) {
				return null;
			}

			while (cursor.isAfterLast() == false) {
				Hashtable<String, String> rowData = new Hashtable<String, String>();

				for (int i = 0; i < cursor.getColumnCount(); i++) {
					String value = cursor.getString(i);
					if (value == null)
						value = "";

					rowData.put(cursor.getColumnName(i), value);
				}

				valueList.add(rowData);
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

		return valueList;
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
	public abstract boolean updateRow(String tableName, ArrayList<DataField> fields, String whereCondition);

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
	 * @param field
	 *            a DataField identifying the new column value.
	 * @param whereClause
	 *            the where clause to filter the select on.
	 * @return TRUE if the row was successfully updated, FALSE otherwise.
	 */
	public boolean updateRow(String tableName, DataField field, String whereCondition) {
		ArrayList<DataField> fields = new ArrayList<DataField>(1);
		fields.add(field);
		return this.updateRow(tableName, fields, whereCondition);
	}

	/**
	 * Generates a new temporary primary key value for a table which will later
	 * be replaced by a permanent key generated by M5.
	 *
	 * @param tableName
	 *            the table to generate the key for.
	 * @return the generated primary key.
	 */
	public int generatePrimaryKey(String tableName) {
		try {
			String lastValue = this.getFieldStringValue("mm_counter_and_id", "last_value_used", "counter_name='" + tableName + "'");

			int value;

			if (MetrixStringHelper.isNullOrEmpty(lastValue)) {
				value = -1;

				ArrayList<DataField> dataFields = new ArrayList<DataField>();
				dataFields.add(new DataField("counter_name", tableName));

				dataFields.add(new DataField("starting_value", "-1"));
				dataFields.add(new DataField("minimum_value", "-100000"));
				dataFields.add(new DataField("last_value_used", "-1"));
				dataFields.add(new DataField("description", tableName));

				this.insertRow("mm_counter_and_id", dataFields);
			} else {
				value = Integer.parseInt(lastValue) - 1;

				this.updateRow("mm_counter_and_id", new DataField("last_value_used", value), "counter_name='" + tableName + "'");
			}

			return value;
		} catch (SQLException ex) {
			LogManager.getInstance(context).error(ex);
			throw ex;
		}
	}

	/**
	 * Predicts a new temporary primary key value for a table which will later
	 * be replaced by a permanent key generated by M5.
	 *
	 * @param tableName
	 *            the table to generate the key for.
	 * @return the predicted primary key.
	 */
	public int getCurrentPrimaryKey(String tableName) {
		try {
			String lastValue = this.getFieldStringValue("mm_counter_and_id", "last_value_used", "counter_name='" + tableName + "'");

			int value;

			if (MetrixStringHelper.isNullOrEmpty(lastValue)) {
				value = -1;
			} else {
				value = Integer.parseInt(lastValue);
			}

			return value;
		} catch (SQLException ex) {
			LogManager.getInstance(context).error(ex);
			throw ex;
		}
	}

	/**
	 * Generates a new log id primary key value for a transaction's log row.
	 *
	 * @param tableName
	 *            the table to generate the key for.
	 * @return the generated primary key.
	 */
	public int generateLogId(String tableName) {
		try {
			String lastValue = this.getFieldStringValue("mm_counter_and_id", "last_value_used", "counter_name='" + tableName + "_log_id'");

			int value;

			if (MetrixStringHelper.isNullOrEmpty(lastValue)) {
				value = 1;

				ArrayList<DataField> dataFields = new ArrayList<DataField>();
				dataFields.add(new DataField("counter_name", tableName + "_log_id"));

				dataFields.add(new DataField("starting_value", "1"));
				dataFields.add(new DataField("minimum_value", "100000"));
				dataFields.add(new DataField("last_value_used", "1"));
				dataFields.add(new DataField("description", tableName + " log id"));

				this.insertRow("mm_counter_and_id", dataFields);
			} else {
				value = Integer.parseInt(lastValue) + 1;

				this.updateRow("mm_counter_and_id", new DataField("last_value_used", value), "counter_name='" + tableName + "_log_id'");
			}

			return value;
		} catch (SQLException ex) {
			LogManager.getInstance(context).error(ex);
			throw ex;
		}
	}

	/**
	 * Generates a new row id key value for a table.
	 *
	 * @param tableName
	 *            the table to generate the key for.
	 * @return the generated primary key.
	 */
	public int generateRowId(String tableName) {
		try {
			String lastValue = this.getFieldStringValue("mm_counter_and_id", "last_value_used", "counter_name='" + tableName + "_row_id'");

			int value;

			if (MetrixStringHelper.isNullOrEmpty(lastValue)) {
				value = 1;

				ArrayList<DataField> dataFields = new ArrayList<DataField>();
				dataFields.add(new DataField("counter_name", tableName + "_row_id"));

				dataFields.add(new DataField("starting_value", "1"));
				dataFields.add(new DataField("minimum_value", "100000"));
				dataFields.add(new DataField("last_value_used", "1"));
				dataFields.add(new DataField("description", tableName + " row id"));

				this.insertRow("mm_counter_and_id", dataFields);
			} else {
				value = Integer.parseInt(lastValue) + 1;

				this.updateRow("mm_counter_and_id", new DataField("last_value_used", value), "counter_name='" + tableName + "_row_id'");
			}

			return value;
		} catch (SQLException ex) {
			LogManager.getInstance(context).error(ex);
			throw ex;
		}
	}

	/**
	 * Generates a new transaction id key value for mm_message_out.
	 *
	 * @param tableName
	 *            the table to generate the key for.
	 * @return the generated transactionID.
	 */
	public int generateTransactionId(String tableName) {
		try {
			String lastValue = this.getFieldStringValue("mm_counter_and_id", "last_value_used", "counter_name='" + tableName + "_transaction_id'");

			int value;

			if (MetrixStringHelper.isNullOrEmpty(lastValue)) {
				value = 1;

				ArrayList<DataField> dataFields = new ArrayList<DataField>();
				dataFields.add(new DataField("counter_name", tableName + "_transaction_id"));

				dataFields.add(new DataField("starting_value", "1"));
				dataFields.add(new DataField("minimum_value", "100000"));
				dataFields.add(new DataField("last_value_used", "1"));
				dataFields.add(new DataField("description", tableName + " transaction id"));

				this.insertRow("mm_counter_and_id", dataFields);
			} else {
				value = Integer.parseInt(lastValue) + 1;

				this.updateRow("mm_counter_and_id", new DataField("last_value_used", value), "counter_name='" + tableName + "_transaction_id'");
			}

			return value;
		} catch (SQLException ex) {
			LogManager.getInstance(context).error(ex);
			throw ex;
		}
	}

	/**
	 * Import the database from local resource to replace the current database.
	 *
	 * @since 5.6
	 */
	public void performDatabaseImport(int resourceId, String fileName, Context context, boolean inUI) {
		InputStream fis = null;
		OutputStream stream = null;

		try {
			fis = context.getResources().openRawResource(resourceId); // R.raw.metrix_demo
			String localBackupPath = getPath(fileName); // "metrix_demo_import.db"
			stream = new BufferedOutputStream(new FileOutputStream(localBackupPath));
			int bufferSize = 1024;
			byte[] buffer = new byte[bufferSize];
			int len = 0;

			while ((len = fis.read(buffer)) != -1) {
				stream.write(buffer, 0, len);
			}

			if (stream != null)
				stream.close();
			fis.close();

			MetrixDatabaseManager.closeDatabase();

			if (importDatabase(localBackupPath, context)) {
				if (inUI)
					Toast.makeText(context, AndroidResourceHelper.getMessage("ImportDBSuccess"), Toast.LENGTH_LONG).show();
			} else {
				if (inUI)
					Toast.makeText(context, AndroidResourceHelper.getMessage("ImportDBFail"), Toast.LENGTH_LONG).show();
			}

			File backupFile = new File(localBackupPath);
			backupFile.delete();
		} catch (Exception ex) {
			LogManager.getInstance().error(ex);
		}
	}

	/**
	 * This method allows you to delete the database file.
	 * @param context The application context.
	 * @since 5.6
	 */
	public static void deleteDatabase(Context context) {
		try {
			MetrixDatabaseManager.closeDatabase();
		}
		catch(Exception ex) {

		}

		try {
			File dbFile = new File(MetrixFileHelper.getMetrixDatabasePath(context));
			if (!dbFile.exists()) {
				dbFile.delete();
			}
		} catch (Exception ex) {
			LogManager.getInstance().error(ex);
		}
	}

	/**
	 * This method allows you to import a database file from a specified file path.
	 * @param dbPath The file path to the database to import.
	 * @param context The application context.
	 * @return TRUE if the import worked, FALSE otherwise
	 * @throws IOException
	 * @since 5.6
	 */
	public boolean importDatabase(String dbPath, Context context) throws IOException {
		try {
			// Close the SQLiteOpenHelper so it will commit the created empty
			// database to internal storage.
			File dbNewFile = new File(dbPath);
			File dbOldFile = new File(MetrixFileHelper.getMetrixDatabasePath(context));
			File dbOldJournalFile = new File(MetrixFileHelper.getMetrixDatabasePath(context)+"-journal");

			if(dbOldJournalFile.exists())
				dbOldJournalFile.delete();

			if(dbOldFile.exists())
				dbOldFile.delete();

			if (dbNewFile.exists()) {
				MetrixFileHelper.copyFile(new FileInputStream(dbNewFile), new FileOutputStream(dbOldFile));
				return true;
			}
			return false;
		}
		catch(Exception ex) {
			return false;
		}
	}

	/**
	 * This method allow you to export a file to a specified file path.
	 * @param dbPath The file path to export the database to.
	 * @param context The application context.
	 * @return TRUE if the export worked, FALSE otherwise
	 * @throws IOException
	 * @since 5.6
	 */
	public boolean exportDatabase(String dbPath, Context context) throws IOException {
		// Close the SQLiteOpenHelper so it will commit the created empty
		// database to internal storage.
		File dbNewFile = new File(dbPath);
		File dbOldFile = new File(MetrixFileHelper.getMetrixDatabasePath(context));
		if (dbOldFile.exists()) {
			MetrixFileHelper.copyFile(new FileInputStream(dbOldFile), new FileOutputStream(dbNewFile));
			return true;
		}
		return false;
	}

	/**
	 * This method exports the database file to the local drive.
	 * @param fileName The name of the file that database should be exported to.
	 * @param context The application context.
	 * @param inUI Whether or not this call is coming from the user interface layer.
	 * @since 5.6
	 */
	public void performDatabaseExport(String fileName, Context context, boolean inUI) {
		try {
			String localExportPath = getPath(fileName); // "metrix_demo_export.db"

			MetrixDatabaseManager.closeDatabase();
			boolean success = exportDatabase(localExportPath, context);

			if (success) {
				if (inUI)
					Toast.makeText(context, AndroidResourceHelper.getMessage("ExportDBSuccess", localExportPath), Toast.LENGTH_LONG).show();
			} else {
				if (inUI)
					Toast.makeText(context, AndroidResourceHelper.getMessage("ExportDBFail"), Toast.LENGTH_LONG).show();
			}
		} catch (Exception ex) {
			LogManager.getInstance().error(ex);
		}
	}

	/**
	 * This method allows you to get the full path to a database based on the database's name.
	 * @param dbFilename The database's file name.
	 * @return The full path.
	 * @since 5.6
	 */
	protected String getPath(String dbFilename) {
		String mTag = this.context.getPackageName();  //"com.metrix.metrixmobile";

		try {
			String state = Environment.getExternalStorageState();
			if (state.equals(Environment.MEDIA_MOUNTED)) {
				File dir = new File(context.getExternalFilesDir(null), "");
				if (!dir.exists()) {
					LogManager.getInstance().warn(mTag + " Could not get application directory: " + dir.getAbsolutePath());
					dir.mkdir();
				}
				File dbFile = new File(dir, dbFilename);

				return dbFile.getAbsolutePath();
			} else {
				LogManager.getInstance().warn(mTag + " Could not access database file because external storage state was " + state);
			}
		} catch (Exception ioe) {
			LogManager.getInstance().error(ioe);
		}

		return null;
	}

	protected String getDatabasePath(Context context) {
		return MetrixFileHelper.getMetrixDatabasePath(context);
	}
}
