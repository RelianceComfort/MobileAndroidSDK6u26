package com.metrix.architecture.metadata;

import com.metrix.architecture.utilities.AndroidResourceHelper;

/**
 * Defines the meta data for how query results should be ordered. Used with
 * MetrixTableDef.
 */
public class MetrixOrderByDef {

	/**
	 * The name of the column to sort on.
	 */
	public String columnName;
	
	/**
	 * The sort order to apply ('ASC' or 'DES')
	 */
	public String sortOrder;

	/**
	 * A convenience constructor
	 */
	public MetrixOrderByDef() {

	}

	/**
	 * A convenience constructor
	 * 
	 * @param columnName The name of the column to sort on.
	 * @param sortOrder The sort order to apply ('ASC' or 'DES')
	 */
	public MetrixOrderByDef(String columnName, String sortOrder) {
		this.columnName = columnName;
		this.sortOrder = sortOrder;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(AndroidResourceHelper.getMessage("ColumnName1Args", this.columnName));
		sb.append(", ");
		sb.append(AndroidResourceHelper.getMessage("SortOrder1Args", this.sortOrder));
		return sb.toString();
	}
}
