package com.metrix.architecture.metadata;

import com.metrix.architecture.utilities.AndroidResourceHelper;

import java.text.Format;

/**
 * Contains meta data used to define which columns should be displayed 
 * when a lookup is performed and optionally which views on the 
 * calling layout should be populated with the column values when a row
 * is selected.
 *
 * @since 5.4
 */
public class MetrixLookupColumnDef {

	/**
	 * The name of the database column to be displayed as part of the lookup.
	 */
	public String columnName;
	
	/**
	 * Optionally, the R.id of the view to be populated with the database 
	 * column's value on a selected row.
	 */
	public int controlId;
	
	/**
	 * Optionally, the field_id of the field to be populated with the database 
	 * column's value on a selected row.
	 */
	public int linkedFieldId;

	/**
	 * Optionally, a boolean indicator that defines whether or not this value
	 * should be displayed. Otherwise it will be bound but hidden. The default
	 * is TRUE.
	 * 
	 * @since 5.6
	 */
	public Boolean alwaysHide;
	
	/**
	 * An instance of a formatter that should be applied to the value of the
	 * column as part of the initial data binding to the layout.
	 * 
	 * @since 5.6
	 */
	public Format formatter;
	
	/**
	 * A convenience constructor.
	 */
	public MetrixLookupColumnDef() {
		this.controlId = 0;
		this.linkedFieldId = 0;
	}

	/**
	 * A convenience constructor.
	 * 
	 * @param columnName
	 * 
	 * <pre>
	 * lookupDef.columnNames.add(new MetrixLookupColumnDef("part.internal_descriptn"));
	 * </pre>
	 */
	public MetrixLookupColumnDef(String columnName) {
		this.columnName = columnName;
		this.controlId = 0;
		this.linkedFieldId = 0;
		this.alwaysHide = false;
	}
	
	/**
	 * A convenience constructor.
	 * 
	 * @param columnName
	 * @param formatter
	 * @since 5.6
	 */
	public MetrixLookupColumnDef(String columnName, Format formatter) {
		this.columnName = columnName;
		this.controlId = 0;
		this.linkedFieldId = 0;
		this.alwaysHide = false;
		this.formatter = formatter;
	}

	/**
	 * A convenience constructor.
	 * 
	 * @param columnName
	 * @param alwaysHide
	 * @since 5.6
	 */
	public MetrixLookupColumnDef(String columnName, Boolean alwaysHide) {
		this.columnName = columnName;
		this.controlId = 0;
		this.linkedFieldId = 0;
		this.alwaysHide = alwaysHide;
	}

	/**
	 * A convenience constructor.
	 * 
	 * @param columnName
	 * @param alwaysHide
	 * @param formatter
	 * @since 5.6
	 */
	public MetrixLookupColumnDef(String columnName, Boolean alwaysHide, Format formatter) {
		this.columnName = columnName;
		this.controlId = 0;
		this.linkedFieldId = 0;
		this.alwaysHide = alwaysHide;
		this.formatter = formatter;
	}

	/**
	 * A convenience constructor.
	 * 
	 * @param columnName
	 * @param controlId
	 * 
	 * <pre>
	 * lookupDef.columnNames.add(new MetrixLookupColumnDef("part.part_id", R.id.part_disp__part_id));
	 * </pre>
	 */
	public MetrixLookupColumnDef(String columnName, int controlId) {
		this.columnName = columnName;
		this.controlId = controlId;
		this.linkedFieldId = 0;
		this.alwaysHide = false;
	}

	/**
	 * A convenience constructor.
	 * @param columnName
	 * @param controlId
	 * @param formatter
	 * @since 5.6
	 */
	public MetrixLookupColumnDef(String columnName, int controlId, Format formatter) {
		this.columnName = columnName;
		this.controlId = controlId;
		this.linkedFieldId = 0;
		this.alwaysHide = false;
		this.formatter = formatter;
	}

	/**
	 * A convenience constructor.
	 * 
	 * @param columnName
	 * @param controlId
	 * @param alwaysHide
	 * 
	 * <pre>
	 * lookupDef.columnNames.add(new MetrixLookupColumnDef("part.part_id", R.id.part_disp__part_id, false));
	 * </pre>
	 * @since 5.6
	 */
	public MetrixLookupColumnDef(String columnName, int controlId, Boolean alwaysHide) {
		this.columnName = columnName;
		this.controlId = controlId;
		this.linkedFieldId = 0;
		this.alwaysHide = alwaysHide;
	}

	/**
	 * A convenience constructor.
	 * @param columnName
	 * @param controlId
	 * @param alwaysHide
	 * @param formatter
	 * @since 5.6
	 */
	public MetrixLookupColumnDef(String columnName, int controlId, Boolean alwaysHide, Format formatter) {
		this.columnName = columnName;
		this.controlId = controlId;
		this.linkedFieldId = 0;
		this.alwaysHide = alwaysHide;
		this.formatter = formatter;
	}

	@Override
	public String toString() {
		StringBuilder value = new StringBuilder();

		value.append(AndroidResourceHelper.getMessage("ColumnName1Args", this.columnName));
		value.append(", ");
		if (alwaysHide) {
			value.append(AndroidResourceHelper.getMessage("Display1Args", AndroidResourceHelper.getMessage("FalseLCase")));
		} else {
			value.append(AndroidResourceHelper.getMessage("Display1Args", AndroidResourceHelper.getMessage("TrueLCase")));
		}
		return value.toString();
	}
}
