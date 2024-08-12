package com.metrix.architecture.metadata;

import com.metrix.architecture.constants.MetrixRelationOperands;
import com.metrix.architecture.utilities.AndroidResourceHelper;

/**
 * Defines the meta data which identifies how two different tables bound
 * to the current layout should be joined together.
 * 
 *  @since 5.4
 *
 */
public class MetrixRelationDef {

	/**
	 * The name of the parent table in the relationship.
	 */
	public String parentTable;
	
	/** 
	 * The parent table's primary key.
	 */
	public String parentColumn;
	
	/**
	 * The name of the child table's associated foriegn key.
	 */
	public String childColumn;
	
	/**
	 * Describes how to two tables should be joined together.
	 */
	public MetrixRelationOperands join;

	/**
	 * A convenience constructor.
	 * 
	 * @param parentTable The name of the parent table in the relationship.
	 * @param parentColumn The parent table's primary key.
	 * @param childColumn The name of the child table's associated foriegn key.
	 * @param join Describes how to two tables should be joined together.
	 */
	public MetrixRelationDef(String parentTable, String parentColumn,
			String childColumn, MetrixRelationOperands join) {
		this.parentTable = parentTable;
		this.parentColumn = parentColumn;
		this.childColumn = childColumn;
		this.join = join;
	}

	@Override
	public String toString() {
		StringBuilder value = new StringBuilder();

		value.append(AndroidResourceHelper.getMessage("ParentTable1Args", this.parentTable));
		value.append(", ");
		value.append(AndroidResourceHelper.getMessage("ParentColumn1Args", this.parentColumn));
		value.append(", ");
		value.append(AndroidResourceHelper.getMessage("ChildColumn1Args", this.childColumn));
		value.append(", ");
		value.append(AndroidResourceHelper.getMessage("Join1Args", this.join));

		return value.toString();
	}
}
