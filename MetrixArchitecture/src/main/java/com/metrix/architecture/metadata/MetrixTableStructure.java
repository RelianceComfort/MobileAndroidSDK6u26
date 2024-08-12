package com.metrix.architecture.metadata;

import java.util.HashMap;

/**
 * A data class to store the Metrix table and column information of each of the
 * business tables on the device.
 * 
 * @since 5.4
 */
public class MetrixTableStructure {
	/**
	 * The name of the table.
	 */
	public String mTableName = "";

	/**
	 * The primary keys of the table.
	 */
	public MetrixKeys mMetrixPrimaryKeys = new MetrixKeys();

	/**
	 * The columns of the table.
	 */
	public HashMap<String, TableColumnDef> mColumns = new HashMap<String, TableColumnDef>();

	/**
	 * The foreign keys of the table along with the parent table name.
	 */
	public HashMap<String, MetrixKeys> mForeignKeys = new HashMap<String, MetrixKeys>();

	/**
	 * A convenience constructor.
	 */
	public MetrixTableStructure() {
	}

	/**
	 * A convenience constructor.
	 * 
	 * @param tableName
	 *            The name of the table.
	 * @param metrixPrimaryKeys
	 *            The primary keys of the table.
	 * @param columnsDef
	 *            The columns of the table.
	 */
	public MetrixTableStructure(String tableName, MetrixKeys metrixPrimaryKeys, HashMap<String, TableColumnDef> columnsDef) {
		this.mTableName = tableName;
		this.mMetrixPrimaryKeys = metrixPrimaryKeys;
		this.mColumns = columnsDef;
	}

	/**
	 * A convenience constructor.
	 * 
	 * @param tableName
	 *            The name of the table.
	 * @param metrixPrimaryKeys
	 *            The primary keys of the table.
	 * @param columnsDef
	 *            The columns of the table.
	 * @param foreignKeys
	 *            The foreign keys of the table along with the parent table
	 *            name.
	 */
	public MetrixTableStructure(String tableName, MetrixKeys metrixPrimaryKeys, HashMap<String, TableColumnDef> columnsDef,
			HashMap<String, MetrixKeys> foreignKeys) {
		this.mTableName = tableName;
		this.mMetrixPrimaryKeys = metrixPrimaryKeys;
		this.mColumns = columnsDef;
		this.mForeignKeys = foreignKeys;
	}

	/**
	 * Determine if a column is a primary key.
	 * 
	 * @param columnName The column name.
	 * @return TRUE if the column is a foreign key, FALSE otherwise.
	 * 
	 * @since 5.6.1
	 */
	public boolean isPrimaryKey(String columnName) {
		if (mMetrixPrimaryKeys != null) {
			for (String keyColumnName : mMetrixPrimaryKeys.keyInfo.keySet()) {
				if (columnName.compareToIgnoreCase(keyColumnName) == 0) {
					return true;
				}
			}
		}
		
		return false;
	}
	/**
	 * Determine if a column is a foreign key.
	 * 
	 * @param columnName
	 *            The column name.
	 * @return TRUE if the column is a foreign key, FALSE otherwise.
	 */
	public boolean isForeignKey(String columnName) {
		if (mForeignKeys != null) {
			for (MetrixKeys keys : mForeignKeys.values()) {
				if (keys.keyInfo != null) {
					if (keys.keyInfo.containsKey(columnName))
						return true;
				}
			}
		}
		return false;
	}

	/**
	 * Gets the parent table associated to a foreign key column.
	 * 
	 * @param columnName
	 *            The name of the foreign key column.
	 * @return The related parent table name.
	 */
	public String getParentTableName(String columnName) {
		String parentTable = "";

		if (mForeignKeys != null) {
			for (MetrixKeys keys : mForeignKeys.values()) {
				if (keys.keyInfo != null) {
					if (keys.keyInfo.containsKey(columnName)) {
						parentTable = keys.tableName;
					}
				}
			}
		}

		return parentTable;
	}

	/**
	 * Gets the name of the parent primary key column name based on the child
	 * foreign key column name.
	 * 
	 * @param columnName
	 *            The name of the foreign key column.
	 * @return The name of the parent primary key column.
	 * @since 5.6
	 */
	public String getParentColumnName(String columnName) {
		String parentColumnName = "";

		if (mForeignKeys != null) {
			for (MetrixKeys keys : mForeignKeys.values()) {
				if (keys.keyInfo != null) {
					if (keys.keyInfo.containsKey(columnName)) {
						parentColumnName = keys.keyInfo.get(columnName);
					}
				}
			}
		}

		return parentColumnName;
	}

	/**
	 * Identifies whether or not the table contains columns named created_by and
	 * created_dttm.
	 * 
	 * @return TRUE if the columns exist, FALSE otherwise.
	 * @since 5.6
	 */
	public boolean containsCreatedColumns() {
		if (mColumns.containsKey("created_by") && (mColumns.containsKey("created_dttm"))) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Identifies whether or not the table contains columns named modified_by
	 * and modified_dttm.
	 * 
	 * @return TRUE if the columns exist, FALSE otherwise.
	 * @since 5.6
	 */
	public boolean containsModifiedColumns() {
		if (mColumns.containsKey("modified_by") && (mColumns.containsKey("modified_dttm"))) {
			return true;
		} else {
			return false;
		}
	}
}
