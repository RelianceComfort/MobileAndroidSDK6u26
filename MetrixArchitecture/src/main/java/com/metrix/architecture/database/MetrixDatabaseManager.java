package com.metrix.architecture.database;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;

import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import com.metrix.architecture.assistants.MetrixApplicationAssistant;
import com.metrix.architecture.metadata.MetrixSqlData;
import com.metrix.architecture.utilities.*;

/**
 * The MetrixDatabaseManager contains a collection of methods which can be
 * used to easily interact with the SQLite database on the device used by
 * the application.
 */
public class MetrixDatabaseManager {

	private static final String METRIX_QUERY_ADAPTER = "MetrixQueryAdapter";
	private static final String METRIX_UPDATE_ADAPTER = "MetrixUpdateAdapter";
	private static final String METRIX_INIT_ADAPTER = "MetrixInitAdapter";

	/**
	 * Creates the database adapters when the application is first opened. You should
	 * never need to invoke this method, it's strictly to be used internally by the
	 * application.
	 *
	 * @since 5.5
	 */
	public static void createDatabaseAdapters(Context context, int version, int rid_system, int rid_business) {
		MetrixDatabaseAdapter queryAdapter = new MetrixSQLiteDatabaseAdapter(context,
				version, rid_system, rid_business);
		queryAdapter.open();

//		Cursor cursor = queryAdapter.rawQuery("SELECT count(*) FROM sqlite_master;", null); 
//		if(cursor==null || cursor.moveToFirst() == false){			
//			throw new InvalidObjectException("Database is not opened");
//		}
//		else {
//			String value = cursor.getString(0);			
//			if(MetrixStringHelper.isNullOrEmpty(value)||value.compareTo("1")==0)
//				throw new InvalidObjectException("Database is not opened");
//		}

		MobileApplication.QueryAdapter = queryAdapter;

		MetrixDatabaseAdapter updateAdapter = new MetrixSQLiteDatabaseAdapter(context,
				version, rid_system, rid_business);
		updateAdapter.open();
		MobileApplication.UpdateAdapter = updateAdapter;

		MetrixDatabaseAdapter initAdapter = new MetrixSQLiteDatabaseAdapter(context,
				version, rid_system, rid_business);

		initAdapter.open(false);
		MobileApplication.InitAdapter = initAdapter;

		MetrixPublicCache.instance.removeItem(METRIX_QUERY_ADAPTER);
		MetrixPublicCache.instance.removeItem(METRIX_UPDATE_ADAPTER);
		MetrixPublicCache.instance.removeItem(METRIX_INIT_ADAPTER);

		MetrixPublicCache.instance.addItem(METRIX_QUERY_ADAPTER, queryAdapter);
		MetrixPublicCache.instance.addItem(METRIX_UPDATE_ADAPTER, updateAdapter);
		MetrixPublicCache.instance.addItem(METRIX_INIT_ADAPTER, initAdapter);

		MobileApplication.DatabaseLoaded = true;
	}

	/**
	 * Gets a database adapter for the database whose name is received.
	 *
	 * @param databaseName the name of the database the adapter is for.
	 * @return the database adapter.
	 * @since 5.4
	 */
	private static MetrixDatabaseAdapter getDatabaseAdapter(String databaseName) {
		if (databaseName == MetrixDatabases.QUERY_CONNECTION) {
			return (MetrixDatabaseAdapter) MetrixPublicCache.instance.getItem(METRIX_QUERY_ADAPTER);
		} else {
			return (MetrixDatabaseAdapter) MetrixPublicCache.instance.getItem(METRIX_UPDATE_ADAPTER);
		}
	}

	/**
	 * Gets a database adapter for the database whose name is received.
	 *
	 * @param databaseName the name of the database the adapter is for.
	 * @return the database adapter.
	 */
	private static MetrixDatabaseAdapter getDatabaseAdapter(String databaseName, boolean foreignKeyEnabled) {
		if (foreignKeyEnabled == false) {

			return (MetrixDatabaseAdapter) MetrixPublicCache.instance.getItem(METRIX_INIT_ADAPTER);
		}

		if (databaseName == MetrixDatabases.QUERY_CONNECTION) {
			return (MetrixDatabaseAdapter) MetrixPublicCache.instance.getItem(METRIX_QUERY_ADAPTER);
		} else {
			return (MetrixDatabaseAdapter) MetrixPublicCache.instance.getItem(METRIX_UPDATE_ADAPTER);
		}
	}

	public static void closeDatabase() {
		MobileApplication.DatabaseLoaded = false;

		MetrixDatabaseAdapter initAdapter = (MetrixDatabaseAdapter) MetrixPublicCache.instance.getItem(METRIX_INIT_ADAPTER);
		if (initAdapter != null)
			initAdapter.close();

		MetrixDatabaseAdapter queryAdapter = (MetrixDatabaseAdapter) MetrixPublicCache.instance.getItem(METRIX_QUERY_ADAPTER);
		if (queryAdapter != null)
			queryAdapter.close();

		MetrixDatabaseAdapter updateAdapter = (MetrixDatabaseAdapter) MetrixPublicCache.instance.getItem(METRIX_UPDATE_ADAPTER);
		if (updateAdapter != null)
			updateAdapter.close();
	}

	/**
	 * Inserts a row into a table with the received values.
	 *
	 * @param tableName    the table to insert the data into.
	 * @param fields       the fields and value to insert.
	 * @return the number of rows inserted.
	 */
	public static long insertRow(String tableName, ArrayList<DataField> fields) {
		MetrixDatabaseAdapter adapter = MetrixDatabaseManager.getDatabaseAdapter(MetrixDatabases.UPDATE_CONNECTION);
		return adapter.insertRow(tableName, fields);
	}

	/**
	 * Deletes a row from the database based on the received where clause.
	 *
	 * @param tableName    the table to delete the row(s) from.
	 * @param whereClause  the where clause to use on the delete.
	 * @return TRUE if the delete was successful, FALSE otherwise.
	 */
	public static boolean deleteRow(String tableName, String whereClause) {
		MetrixDatabaseAdapter adapter = MetrixDatabaseManager.getDatabaseAdapter(MetrixDatabases.UPDATE_CONNECTION);
		return adapter.executeSql("delete from " + tableName + " where " + whereClause);
	}

	/**
	 * Gets the value of an application parameter by name.
	 *
	 * @param name The name of the parameter whose value to return.
	 * @return The value of the parameter.
	 * @since 5.6 Patch 1
	 */
	public static String getAppParam(String name) {
		if (MetrixStringHelper.isNullOrEmpty(name)) {
			throw new IllegalArgumentException(AndroidResourceHelper.getMessage("TheNameParamIsReq"));
		}

		String value = "";
		try {
			value = MetrixDatabaseManager.getFieldStringValue("metrix_app_params", "param_value", "param_name='" + name + "'");
		} catch (Exception ex) {
			LogManager.getInstance().error(ex);
		}

		return value;
	}

	/**
	 * Gets the value of an application parameter by name (from cust_app_params or metrix_app_params).
	 *
	 * @param name The name of the parameter whose value to return.
	 * @return The value of the parameter, null if the app param was not found in either table.
	 * @since 5.7.0
	 */
	public static String getCustomOrBaselineAppParam(String name) {
		if (MetrixStringHelper.isNullOrEmpty(name))
			throw new IllegalArgumentException(AndroidResourceHelper.getMessage("TheNameParamIsReq"));

		int custCount = MetrixDatabaseManager.getCount("cust_app_params", String.format("param_name = '%s'", name));
		if (custCount > 0)
			return MetrixDatabaseManager.getFieldStringValue("cust_app_params", "param_value", String.format("param_name = '%s'", name));

		int baselineCount = MetrixDatabaseManager.getCount("metrix_app_params", String.format("param_name = '%s'", name));
		if (baselineCount > 0)
			return MetrixDatabaseManager.getFieldStringValue("metrix_app_params", "param_value", String.format("param_name = '%s'", name));

		return null;
	}

	/**
	 * Gets a count of the rows which match the received where clause.
	 *
	 * @param tableName
	 *            the table to perform the query on.
	 * @param whereClause
	 *            the where clause to apply.
	 * @return an integer identifying the number of rows which match the where
	 *         clause.
	 * @throws SQLException
	 */
	public static int getCount(String tableName, String whereClause) throws SQLException {
		MetrixDatabaseAdapter adapter = MetrixDatabaseManager.getDatabaseAdapter(MetrixDatabases.UPDATE_CONNECTION);
		return adapter.getCount(tableName, whereClause);
	}

	/**
	 * Gets a count of the rows which match the received where clause.
	 *
	 * @param tableNames
	 *            a list of tables to perform the query on.
	 * @param whereClause
	 *            the where clause to apply.
	 * @return an integer identifying the number of rows which match the where
	 *         clause.
	 * @throws SQLException
	 */
	public static int getCount(String[] tableNames, String whereClause) throws SQLException {
		MetrixDatabaseAdapter adapter = MetrixDatabaseManager.getDatabaseAdapter(MetrixDatabases.UPDATE_CONNECTION);
		return adapter.getCount(tableNames, whereClause);
	}
	
	/**
	 * Gets a row of data from the database based on a row id.
	 *
	 * @param tableName
	 *            the table to get the rows from.
	 * @param columns
	 *            the columns to select.
	 * @param rowId
	 *            the id for the row to select.
	 * @return a cursor containing the row.
	 */
	@Deprecated
	public static Cursor getRow(String tableName, String[] columns, long rowId) {
		MetrixDatabaseAdapter adapter = MetrixDatabaseManager.getDatabaseAdapter(MetrixDatabases.QUERY_CONNECTION);
		return adapter.getRow(tableName, columns, rowId);
	}

	/**
	 * Gets a row of data from the database based on a where clause.
	 *
	 * @param tableName
	 *            the table to get the rows from.
	 * @param columns
	 *            the columns to select.
	 * @param whereClause
	 *            the where clause to filter the select on.
	 * @return a cursor containing the row(s).
	 */
	@Deprecated
	public static Cursor getRow(String tableName, String[] columns, String whereClause) {
		MetrixDatabaseAdapter adapter = MetrixDatabaseManager.getDatabaseAdapter(MetrixDatabases.QUERY_CONNECTION);
		return adapter.getRow(tableName, columns, whereClause);
	}
	
	/**
	 * Gets all rows from the table whose name was received.
	 *
	 * @param tableName
	 *            the table to get the row(s) from.
	 * @param columns
	 *            a list of columns to select.
	 * @return a cursor containing the selected rows.
	 */
	@Deprecated
	public static Cursor getRows(String tableName, String[] columns) {
		MetrixDatabaseAdapter adapter = MetrixDatabaseManager.getDatabaseAdapter(MetrixDatabases.QUERY_CONNECTION);
		return adapter.getRows(tableName, columns);
	}
	
	/**
	 * Gets all rows from the table whose name was received.
	 *
	 * @param tableName
	 *            the table to get the row(s) from.
	 * @param columns
	 *            a list of columns to select.
	 * @param filter
	 *            where clause without where.
	 * @return a cursor containing the selected rows.
	 */
	@Deprecated
	public static Cursor getRows(String tableName, String[] columns, String filter) {
		MetrixDatabaseAdapter adapter = MetrixDatabaseManager.getDatabaseAdapter(MetrixDatabases.QUERY_CONNECTION);
		return adapter.getRows(tableName, columns, filter);
	}
	
	/**
	 * Gets all rows from the table whose name was received.
	 *
	 * @param tableName
	 *            the table to get the row(s) from.
	 * @param columns
	 *            a list of columns to select.
	 * @param filter
	 *            where clause without where.
	 * @param orderBy
	 *            order by clause without order by.
	 * @return a cursor containing the selected rows.
	 */
	@Deprecated
	public static Cursor getRows(String tableName, String[] columns, String filter, String orderBy) {
		MetrixDatabaseAdapter adapter = MetrixDatabaseManager.getDatabaseAdapter(MetrixDatabases.QUERY_CONNECTION);
		return adapter.getRows(tableName, columns, filter, orderBy);
	}
	
	/**
	 * Executes a sql query and returns the result in a cursor.
	 *
	 * @param sql
	 *            the sql statement.
	 * @param selectionArgs
	 *            You may include ?s in where clause in the query, which will be
	 *            replaced by the values from selectionArgs. The values will be
	 *            bound as Strings.
	 * @return the cursor with the resulting data.
	 */
	@Deprecated
	public static Cursor rawQuery(String sql, String[] selectionArgs) {
		MetrixDatabaseAdapter adapter = MetrixDatabaseManager.getDatabaseAdapter(MetrixDatabases.QUERY_CONNECTION);
		return adapter.rawQuery(sql, selectionArgs);
	}
		
	/**
	 * Gets a row of data from the database based on a row id.
	 *
	 * @param tableName
	 *            the table to get the rows from.
	 * @param columns
	 *            the columns to select.
	 * @param rowId
	 *            the id for the row to select.
	 * @return a MetrixCursor containing the row.
	 */
	public static MetrixCursor getRowMC(String tableName, String[] columns, long rowId) throws SQLException {
		MetrixDatabaseAdapter adapter = MetrixDatabaseManager.getDatabaseAdapter(MetrixDatabases.QUERY_CONNECTION);
		Cursor cursor = adapter.getRow(tableName, columns, rowId);		
		if(cursor == null)
			return null;
		
		return new MetrixCursor(cursor);
	}

	/**
	 * Gets a row of data from the database based on a where clause.
	 *
	 * @param tableName
	 *            the table to get the rows from.
	 * @param columns
	 *            the columns to select.
	 * @param whereClause
	 *            the where clause to filter the select on.
	 * @return a MetrixCursor containing the row(s).
	 */
	public static MetrixCursor getRowMC(String tableName, String[] columns, String whereClause) throws SQLException {
		MetrixDatabaseAdapter adapter = MetrixDatabaseManager.getDatabaseAdapter(MetrixDatabases.QUERY_CONNECTION);
		Cursor cursor = adapter.getRow(tableName, columns, whereClause);
		if(cursor == null)
			return null;
		return new MetrixCursor(cursor);
	}
	
	/**
	 * Gets all rows from the table whose name was received.
	 *
	 * @param tableName
	 *            the table to get the row(s) from.
	 * @param columns
	 *            a list of columns to select.
	 * @return a MetrixCursor containing the selected rows.
	 */
	public static MetrixCursor getRowsMC(String tableName, String[] columns) throws SQLException {
		MetrixDatabaseAdapter adapter = MetrixDatabaseManager.getDatabaseAdapter(MetrixDatabases.QUERY_CONNECTION);
		Cursor cursor = adapter.getRows(tableName, columns);
		if(cursor == null)
			return null;
		
		return new MetrixCursor(cursor);		
	}
	
	/**
	 * Gets all rows from the table whose name was received.
	 *
	 * @param tableName
	 *            the table to get the row(s) from.
	 * @param columns
	 *            a list of columns to select.
	 * @param filter
	 *            where clause without where.
	 * @return a MetrixCursor containing the selected rows.
	 */
	public static MetrixCursor getRowsMC(String tableName, String[] columns, String filter) throws SQLException {
		MetrixDatabaseAdapter adapter = MetrixDatabaseManager.getDatabaseAdapter(MetrixDatabases.QUERY_CONNECTION);
		Cursor cursor = adapter.getRows(tableName, columns, filter);
		if(cursor == null)
			return null;
		
		return new MetrixCursor(cursor);	
	}
	
	/**
	 * Gets all rows from the table whose name was received.
	 *
	 * @param tableName
	 *            the table to get the row(s) from.
	 * @param columns
	 *            a list of columns to select.
	 * @param filter
	 *            where clause without where.
	 * @param orderBy
	 *            order by clause without order by.
	 * @return a MetrixCursor containing the selected rows.
	 */
	public static MetrixCursor getRowsMC(String tableName, String[] columns, String filter, String orderBy) throws SQLException {
		MetrixDatabaseAdapter adapter = MetrixDatabaseManager.getDatabaseAdapter(MetrixDatabases.QUERY_CONNECTION);
		Cursor cursor = adapter.getRows(tableName, columns, filter, orderBy);
		if(cursor == null)
			return null;
		
		return new MetrixCursor(cursor);
	}
	
	/**
	 * Executes a sql query and returns the result in a cursor.
	 *
	 * @param sql
	 *            the sql statement.
	 * @param selectionArgs
	 *            You may include ?s in where clause in the query, which will be
	 *            replaced by the values from selectionArgs. The values will be
	 *            bound as Strings.
	 * @return the MetrixCursor with the resulting data.
	 */
	public static MetrixCursor rawQueryMC(String sql, String[] selectionArgs) throws SQLException {
		MetrixDatabaseAdapter adapter = MetrixDatabaseManager.getDatabaseAdapter(MetrixDatabases.QUERY_CONNECTION);
		Cursor cursor = adapter.rawQuery(sql, selectionArgs);
		if(cursor == null)
			return null;
		
		return new MetrixCursor(cursor);		
	}

	public static boolean isDecimalColumn(Cursor cursor, int index) {
		try {
			MetrixDatabaseAdapter adapter = MetrixDatabaseManager.getDatabaseAdapter(MetrixDatabases.QUERY_CONNECTION);
			return ((adapter.getType(cursor, index)) == MetrixDatabaseAdapter.FIELD_TYPE_FLOAT);
		} catch (Exception e) {
			LogManager.getInstance().error(e);
		}
		return false;
	}
	
	/**
	 * Executes a sql statement.
	 *
	 * @param sql
	 *            the sql statement.
	 * @return TRUE if the statement was successfully run, FALSE otherwise.
	 */
	public static boolean executeSql(String sql) {
		MetrixDatabaseAdapter adapter = MetrixDatabaseManager.getDatabaseAdapter(MetrixDatabases.UPDATE_CONNECTION);
		return adapter.executeSql(sql);
	}

	/**
	 * Executes an array of sql statements.
	 *
	 * @param sqlArray
	 *            an array of sql statements to issue.
	 * @return TRUE if all statements were successfully run, FALSE otherwise.
	 */
	public static boolean executeSqlArray(ArrayList<String> sqlArray) {
		MetrixDatabaseAdapter adapter = MetrixDatabaseManager.getDatabaseAdapter(MetrixDatabases.UPDATE_CONNECTION);
		return adapter.executeSqlArray(sqlArray);
	}

	/**
	 * Executes an array of sql statements.
	 *
	 * @param sqlArray
	 *            an array of sql statements to issue.
	 * @return TRUE if all statements were successfully run, FALSE otherwise.
	 */
	public static boolean executeSqlArray(ArrayList<String> sqlArray, boolean foreignKeyConstraints) {
		if (foreignKeyConstraints) {
			MetrixDatabaseAdapter adapter = MetrixDatabaseManager.getDatabaseAdapter(MetrixDatabases.UPDATE_CONNECTION);
			return adapter.executeSqlArray(sqlArray);
		} else {
			MetrixDatabaseAdapter adapter = MetrixDatabaseManager.getDatabaseAdapter(MetrixDatabases.UPDATE_CONNECTION, false);
			return adapter.executeSqlArray(sqlArray);
		}

	}

	/**
	 * Begin the database transaction
	 */
	public static void begintransaction() {
		MetrixDatabaseAdapter adapter = MetrixDatabaseManager.getDatabaseAdapter(MetrixDatabases.UPDATE_CONNECTION);
		adapter.beginTransaction();
	}

	/**
	 * End the database transaction
	 */
	public static void endTransaction() {
		MetrixDatabaseAdapter adapter = MetrixDatabaseManager.getDatabaseAdapter(MetrixDatabases.UPDATE_CONNECTION);
		adapter.endTransaction();
	}

	/**
	 * A wrapper method for set the database transaction successful
	 */
	public static void setTransactionSuccessful() {
		MetrixDatabaseAdapter adapter = MetrixDatabaseManager.getDatabaseAdapter(MetrixDatabases.UPDATE_CONNECTION);
		adapter.setTransactionSuccessful();
	}

	/**
	 * Execute list of the data rows within a transaction
	 * 
	 * @param dataList
	 *            ArrayList<MetrixSqlData>
	 * @return
	 */
	public static boolean executeDataFieldArray(ArrayList<MetrixSqlData> dataList, boolean startTransaction) {
		MetrixDatabaseAdapter adapter = MetrixDatabaseManager.getDatabaseAdapter(MetrixDatabases.UPDATE_CONNECTION);
		return adapter.executeDataFieldArray(dataList, startTransaction);
	}

	/**
	 * Executes a single transaction
	 * 
	 * @param data
	 *            MetrixSqlData
	 * @return
	 */
	public static boolean executeDataField(MetrixSqlData data, boolean startTransaction) {
		ArrayList<MetrixSqlData> dataList = new ArrayList<MetrixSqlData>();
		return MetrixDatabaseManager.executeDataFieldArray(dataList, startTransaction);
	}

	/**
	 * Executes an array of sql statements.
	 *
	 * @param statements
	 *            an array of sql statements to issue.
	 * @return TRUE if all statements were successfully run, FALSE otherwise.
	 */
	public static boolean executeSqlArray(String[] statements) {
		MetrixDatabaseAdapter adapter = MetrixDatabaseManager.getDatabaseAdapter(MetrixDatabases.UPDATE_CONNECTION);
		return adapter.executeSqlArray(statements);
	}
	
	/**
	 * Gets a specific column value from a row based on a where clause.
	 *
	 * @param tableName
	 *            the table to get the rows from.
	 * @param columnName
	 *            the column to select
	 * @param whereClause
	 *            the where clause to filter the select on.
	 * @return a cursor containing the row(s).
	 * @throws SQLException
	 */
	public static String getFieldStringValue(String tableName, String columnName, String whereClause) throws SQLException {
		try {
			MetrixDatabaseAdapter adapter = MetrixDatabaseManager.getDatabaseAdapter(MetrixDatabases.QUERY_CONNECTION);
			return adapter.getFieldStringValue(tableName, columnName, whereClause);
		}
		catch(Exception ex){
			LogManager.getInstance().error(ex);
			return "";
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
	public static String getFieldStringValue(boolean distinct, String table, String columnName, String selection, String[] selectionArgs, String groupBy, String having, String orderBy, String limit) {
		try {
			MetrixDatabaseAdapter adapter = MetrixDatabaseManager.getDatabaseAdapter(MetrixDatabases.QUERY_CONNECTION);
			return adapter.getFieldStringValue(distinct, table, columnName, selection, selectionArgs, groupBy, having, orderBy, limit);
		}
		catch(Exception ex){
			LogManager.getInstance().error(ex);
			return "";
		}
	}

	/**
	 * Gets a hash table of column values from a row based on a where clause.
	 *
	 * @param tableName
	 *            the table to get the rows from.
	 * @param columnNames
	 *            the columns to select
	 * @param whereClause
	 *            the where clause to filter the select on.
	 * @return a hash table containing name value pairs of the requested columns
	 *         and their values.
	 * @throws SQLException
	 */
	public static Hashtable<String, String> getFieldStringValues(String tableName, String[] columnNames, String whereClause) throws SQLException {
		try {
			MetrixDatabaseAdapter adapter = MetrixDatabaseManager.getDatabaseAdapter(MetrixDatabases.QUERY_CONNECTION);
			return adapter.getFieldStringValues(tableName, columnNames, whereClause);
		}	
		catch(Exception ex){
			LogManager.getInstance().error(ex);
			return new Hashtable<String, String>();
		}
	}

	/**
	 * Gets a ArrayList of rows defined with hash table of column values from a
	 * row based on a where clause. It is like rows of data object
	 *
	 * @param tableName
	 *            the table to get the rows from.
	 * @param columnNames
	 *            the columns to select
	 * @param whereClause
	 *            the where clause to filter the select on.
	 * @return a hash table containing name value pairs of the requested columns
	 *         and their values.
	 * @throws SQLException
	 */
	public static ArrayList<Hashtable<String, String>> getFieldStringValuesList(String tableName, String[] columnNames, String whereClause) {
		MetrixDatabaseAdapter adapter = MetrixDatabaseManager.getDatabaseAdapter(MetrixDatabases.QUERY_CONNECTION);
		return adapter.getFieldStringValuesList(tableName, columnNames, whereClause);
	}

	/**
	 * Gets a ArrayList of rows defined with hash table of column values from a
	 * row based on a where clause. It is like rows of data object
	 * 
	 * @param tableName
	 * @param columnNames
	 * @param selectioin
	 * @param selectionArgs
	 * @param groupBy
	 * @param orderBy
	 * @param limit
	 * @return
	 */
	public static ArrayList<Hashtable<String, String>> getFieldStringValuesList(String tableName, String[] columnNames, String selectioin, String[] selectionArgs, String groupBy, String orderBy, String limit) {
		MetrixDatabaseAdapter adapter = MetrixDatabaseManager.getDatabaseAdapter(MetrixDatabases.QUERY_CONNECTION);
		return adapter.getFieldStringValuesList(tableName, columnNames, selectioin, selectionArgs, groupBy, orderBy, limit);
	}

	/**
	 * Gets a specific column value from a row, using a raw sql statement.
	 * 
	 * @param sqlStatement
	 * @return
	 */
	public static String getFieldStringValue(String sqlStatement) {
		MetrixDatabaseAdapter adapter = MetrixDatabaseManager.getDatabaseAdapter(MetrixDatabases.QUERY_CONNECTION);
		ArrayList<Hashtable<String, String>> resultArray = adapter.getFieldStringValuesList(sqlStatement);
		if (resultArray != null && resultArray.size() > 0) {
			return (String)resultArray.get(0).values().toArray()[0];
		} else
			return "";
	}
	
	/**
	 * Gets a ArrayList of rows defined with hash table of column values from a
	 * raw sql statement.
	 * 
	 * @param sqlStatement
	 * @return
	 */
	public static ArrayList<Hashtable<String, String>> getFieldStringValuesList(String sqlStatement) {
		MetrixDatabaseAdapter adapter = MetrixDatabaseManager.getDatabaseAdapter(MetrixDatabases.QUERY_CONNECTION);
		return adapter.getFieldStringValuesList(sqlStatement);
	}

	/**
	 * Update a specific row based on the where condition with new values passed
	 * in an array of data field.
	 *
	 * @param tableName
	 *            the table to get the rows from.
	 * @param fields
	 *            an array of DataField identifying the new column values.
	 * @param whereCondition
	 *            the where clause to filter the select on.
	 * @return TRUE if the row was successfully updated, FALSE otherwise.
	 */
	public static boolean updateRow(String tableName, ArrayList<DataField> fields, String whereCondition) {
		MetrixDatabaseAdapter adapter = MetrixDatabaseManager.getDatabaseAdapter(MetrixDatabases.UPDATE_CONNECTION);
		return adapter.updateRow(tableName, fields, whereCondition);
	}

	/**
	 * Generates a new temporary primary key value for a table which will later
	 * be replaced by a permanent key generated by M5.
	 *
	 * @param tableName
	 *            the table to generate the key for.
	 * @return the generated primary key.
	 */
	public static int generatePrimaryKey(String tableName) {
		MetrixDatabaseAdapter adapter = MetrixDatabaseManager.getDatabaseAdapter(MetrixDatabases.UPDATE_CONNECTION);
		return adapter.generatePrimaryKey(tableName);
	}

	/**
	 * Predicts a new temporary primary key value for a table which will later
	 * be replaced by a permanent key generated by M5.
	 *
	 * @param tableName
	 *            the table to generate the key for.
	 * @return the predicted primary key.
	 */
	public static int getCurrentPrimaryKey(String tableName) {
		MetrixDatabaseAdapter adapter = MetrixDatabaseManager.getDatabaseAdapter(MetrixDatabases.UPDATE_CONNECTION);
		return adapter.getCurrentPrimaryKey(tableName);
	}

	/**
	 * Generates a new log id primary key value for a transaction's log row.
	 * 
	 * @param tableName
	 *            the table to generate the key for.
	 * @return the generated primary key.
	 */
	public static int generateLogId(String tableName) {
		MetrixDatabaseAdapter adapter = MetrixDatabaseManager.getDatabaseAdapter(MetrixDatabases.UPDATE_CONNECTION);
		return adapter.generateLogId(tableName);
	}

	/**
	 * Generates a new row id key value for a table.
	 * 
	 * @param tableName
	 *            the table to generate the key for.
	 * @return the generated primary key.
	 */
	public static int generateRowId(String tableName) {
		MetrixDatabaseAdapter adapter = MetrixDatabaseManager.getDatabaseAdapter(MetrixDatabases.UPDATE_CONNECTION);
		return adapter.generateRowId(tableName);
	}

	public static int generateTransactionId(String tableName) {
		MetrixDatabaseAdapter adapter = MetrixDatabaseManager.getDatabaseAdapter(MetrixDatabases.UPDATE_CONNECTION);
		return adapter.generateTransactionId(tableName);
	}
	
	/**
	 * This method imports the database from local resource to replace the current database.
	 * @param resourceId 
	 * @param fileName The name of the file to import the database from.
	 * @param context The application context.
	 * @param inUI Whether or not this call is coming from the user interface.
	 * @param rid_system Android resource id for the system tables script.
	 * @param rid_business Android resource id for the business tables script.
	 * @since 5.6
	 */
	public static void performDatabaseImport(int resourceId, String fileName, Context context, boolean inUI, int rid_system, int rid_business) {
		MetrixDatabaseAdapter adapter = MetrixDatabaseManager.getDatabaseAdapter(MetrixDatabases.UPDATE_CONNECTION);
		adapter.performDatabaseImport(resourceId, fileName, context, inUI);

		// Reload the database
		try {
			MetrixDatabaseManager.createDatabaseAdapters(context.getApplicationContext(), MetrixApplicationAssistant.getMetaIntValue(context, "DatabaseVersion"), rid_system,
				rid_business);
		}
		catch(Exception ex) {
			LogManager.getInstance().error(ex);
		}
	}
	
	/**
	 * This method deletes the application's database.
	 * @param context The application context.
	 * @since 5.6
	 */
	public static void deleteDatabase(Context context) {
		MetrixDatabaseAdapter.deleteDatabase(context);
		MobileApplication.DatabaseLoaded = false;
	}
	
	public static boolean importDatabase(String dbPath, Context context) throws IOException {
		MetrixDatabaseAdapter adapter = MetrixDatabaseManager.getDatabaseAdapter(MetrixDatabases.UPDATE_CONNECTION);
		return adapter.importDatabase(dbPath, context);
	}
	
	/**
	 * This method exports a database to the specified file path.
	 * @param dbPath The path of the file which should be created with the database export.
	 * @param context The application context.
	 * @return TRUE if the export worked, FALSE otherwise.
	 * @throws IOException
	 * @since 5.6
	 */
	public static boolean exportDatabase(String dbPath, Context context) throws IOException {
		MetrixDatabaseAdapter adapter = MetrixDatabaseManager.getDatabaseAdapter(MetrixDatabases.UPDATE_CONNECTION);
		return adapter.exportDatabase(dbPath, context);
	}
	
	/**
	 * This method exports a database to the specified file path.
	 * @param fileName The path of the file which should be created with the database export.
	 * @param context The application context.
	 * @param inUI Whether or not this call is coming from the user interface.
	 * @param rid_system Android resource id for the system tables script.
	 * @param rid_business Android resource id for the business tables script.
	 * @since 5.6
	 */
	public static void performDatabaseExport(String fileName, Context context, boolean inUI, int rid_system, int rid_business) {
		MetrixDatabaseAdapter adapter = MetrixDatabaseManager.getDatabaseAdapter(MetrixDatabases.UPDATE_CONNECTION);
		adapter.performDatabaseExport(fileName, context, inUI);
		
		// Reload the database
		try {
			MetrixDatabaseManager.createDatabaseAdapters(context.getApplicationContext(), MetrixApplicationAssistant.getMetaIntValue(context, "DatabaseVersion"), rid_system,
				rid_business);
		}
		catch(Exception ex) {
			LogManager.getInstance().error(ex);
		}
	}
}