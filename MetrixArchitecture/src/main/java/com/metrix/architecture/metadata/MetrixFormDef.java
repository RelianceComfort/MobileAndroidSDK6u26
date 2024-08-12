package com.metrix.architecture.metadata;

import java.util.ArrayList;
import java.util.List;

/**
 * Containts the meta data for all of the bound data for this layout as well
 * as the meta data for any unbound spinners.
 *
 * @since 5.4
 */
public class MetrixFormDef {

	/**
	 * The MetrixTableDef meta data for each table that has data bound to the
	 * current layout.
	 */
	public List<MetrixTableDef> tables;
	
	/**
	 * The MetrixDropDownDef meta data for each unbound spinner on the current
	 * layout.
	 */
	public List<MetrixDropDownDef> lookups;

	/**
	 * A convenience constructor.
	 * 
	 * @param tableDef
	 */
	public MetrixFormDef(MetrixTableDef tableDef) {
		this.tables = new ArrayList<MetrixTableDef>();
		this.lookups = new ArrayList<MetrixDropDownDef>();

		this.tables.add(tableDef);
	}

	/**
	 * A convenience constructor.
	 * 
	 * @param tableDef A MetrixTableDef for a table bound to the current layout.
	 * @param lookupDef A MetrixLookupDef for a unbound spinner.
	 */
	public MetrixFormDef(MetrixTableDef tableDef, MetrixDropDownDef lookupDef) {
		this.tables = new ArrayList<MetrixTableDef>();
		this.lookups = new ArrayList<MetrixDropDownDef>();

		this.tables.add(tableDef);
		this.lookups.add(lookupDef);
	}

	/**
	 * A convenience constructor.
	 * 
	 * @param tableDefs An array of MetrixTableDef for all of the tables who
	 * have data bound on the current layout.
	 */
	public MetrixFormDef(List<MetrixTableDef> tableDefs) {
		this.tables = tableDefs;
		this.lookups = new ArrayList<MetrixDropDownDef>();
	}

	/**
	 * A convenience constructor.
	 * 
	 * @param tableDefs An array of MetrixTableDef for all of the tables who
	 * have data bound on the current layout.
	 * @param lookupDefs An array of MetrixDropDownDef for all of the unbound
	 * spinners on the current layout.
	 */
	public MetrixFormDef(List<MetrixTableDef> tableDefs,
			List<MetrixDropDownDef> lookupDefs) {
		this.tables = tableDefs;
		this.lookups = lookupDefs;
	}

	/**
	 * A convenience constructor.
	 * 
	 * @param tableDefs An array of MetrixTableDef for all of the tables who
	 * have data bound on the current layout.
	 * @param lookupDef A MetrixLookupDef for a unbound spinner.
	 */
	public MetrixFormDef(List<MetrixTableDef> tableDefs,
			MetrixDropDownDef lookupDef) {
		this.tables = tableDefs;
		this.lookups = new ArrayList<MetrixDropDownDef>();

		this.lookups.add(lookupDef);
	}

	/**
	 * Gets the names of all of the tables currently bound to this layout.
	 * 
	 * @return All of the tables that are currently bound.
	 */
	public List<String> getTableNames() {
		List<String> tableNames = new ArrayList<String>();

		for (MetrixTableDef tableDef : this.tables) {
			tableNames.add(tableDef.tableName);
		}

		return tableNames;
	}

	/**
	 * Return TRUE if there are more than one table bound to this layout
	 * and their related, FALSE otherwise.
	 * 
	 * @return A boolean indicator of bound table relations.
	 */
	public boolean containsRelations() {
		for (MetrixTableDef tableDef : this.tables) {
			if (tableDef.hasRelations()) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Returns TRUE if any of the bound tables have constraints associated
	 * to them, FALSE otherwise.
	 * 
	 * @return A boolean indicator of data constraints.
	 */
	public boolean containsConstraints() {
		for (MetrixTableDef tableDef : this.tables) {
			if (tableDef.hasConstraints()) {
				return true;
			}
		}

		return false;
	}
	
	/**
	 * Returns the resource id associated with the column def created
	 * for the table name and column name received. If a column def 
	 * isn't found, this method will return 0.
	 * 
	 * @param tableName The name of the table the column is on.
	 * @param columnName The name of the column.
	 * @return The resource id of the control associated to the column.
	 * 
	 * @since 5.6.1
	 */
	public int getId(String tableName, String columnName) {
		for (MetrixTableDef tableDef : this.tables) {
			if (tableDef.tableName.compareToIgnoreCase(tableName) == 0) {
				for (MetrixColumnDef columnDef : tableDef.columns) {
					if (columnDef.columnName.compareToIgnoreCase(columnName) == 0) {
						return columnDef.id;
					}
				}
				break;
			}
		}
		return 0;
	}
	
	/**
	 * Returns the resource id associated with the column def created
	 * for the field_id received. If a column def isn't found, this method will return 0.
	 * 
	 * @param fieldId The unique identifier of the corresponding field metadata.
	 * @return The resource id of the control associated to the column.
	 * 
	 * @since 5.6.3
	 */
	public int getId(int fieldId) {
		for (MetrixTableDef tableDef : this.tables) {
			for (MetrixColumnDef columnDef : tableDef.columns) {
				if (columnDef.fieldId != null && columnDef.fieldId == fieldId) {
					return columnDef.id;
				}
			}
		}
		return 0;
	}
	
	/**
	 * Returns the column def created
	 * for the table name and column name received. If a column def 
	 * isn't found, this method will return null.
	 * 
	 * @param tableName The name of the table the column is on.
	 * @param columnName The name of the column.
	 * @return The column def of the control associated to the column.
	 * 
	 * @since 5.6.3
	 */
	public MetrixColumnDef getColumnDef(String tableName, String columnName) {
		for (MetrixTableDef tableDef : this.tables) {
			if (tableDef.tableName.compareToIgnoreCase(tableName) == 0) {
				for (MetrixColumnDef columnDef : tableDef.columns) {
					if (columnDef.columnName.compareToIgnoreCase(columnName) == 0) {
						return columnDef;
					}
				}
				break;
			}
		}
		return null;
	}
}
