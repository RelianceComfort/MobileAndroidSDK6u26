package com.metrix.architecture.metadata;

import java.util.ArrayList;
import java.util.List;

/**
 * Define the meta data for a table to be displayed by a lookup.
 *
 * @since 5.4
 */
public class MetrixLookupTableDef {

	/**
	 * The name of the table to be bound.
	 */
	public String tableName;
	
	/**
	 * The name of the parent table to which this table will be joined.
	 */
	public String parentTableName;
	
	/**
	 * The name of the primary keys on the parent table. This will be used
	 * to join the tables together.
	 */
	public List<String> parentKeyColumns;
	
	/**
	 * The name of the foreign keys on the child table. This will be used
	 * to join the tables together.
	 */
	public List<String> childKeyColumns;
	
	/**
	 * The operator to use on the join ('equi', 'left outer').
	 */
	public String operator;

	/**
	 * A convenience constructor.
	 */
	public MetrixLookupTableDef() {
		this.parentKeyColumns = new ArrayList<String>();
		this.childKeyColumns = new ArrayList<String>();
	}

	/**
	 * A convenience constructor.
	 * 
	 * @param tableName The name of the table to display data from.
	 */
	public MetrixLookupTableDef(String tableName) {
		this.parentKeyColumns = new ArrayList<String>();
		this.childKeyColumns = new ArrayList<String>();
		this.tableName = tableName;
	}

	/**
	 * A convenience constructor.
	 * 
	 * @param tableName The name of the table to display data from.
	 * @param parentTableName The name of the parent table to join this table to.
	 * @param parentKeyColumn The name of the primary key of the parent table.
	 * @param childKeyColumn The name of the foreign key on this table.
	 * @param joinOperator How the two tables should be joined.
	 * 
	 * <pre>
	 * MetrixLookupDef lookupDef = new MetrixLookupDef("part_need", true);
	 * lookupDef.tableNames.add(new MetrixLookupTableDef("part",
	 *  "part_need", "part_need.part_id", "part.part_id", "="));
	 * </pre>
	 */
	public MetrixLookupTableDef(String tableName, String parentTableName,
			String parentKeyColumn, String childKeyColumn, String joinOperator) {
		this.tableName = tableName;
		this.parentTableName = tableName;
		this.operator = joinOperator;

		this.parentKeyColumns = new ArrayList<String>();
		this.parentKeyColumns.add(parentKeyColumn);
		this.childKeyColumns = new ArrayList<String>();
		this.childKeyColumns.add(childKeyColumn);
	}

}
