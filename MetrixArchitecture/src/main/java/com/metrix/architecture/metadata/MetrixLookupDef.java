package com.metrix.architecture.metadata;

import java.util.ArrayList;
import java.util.List;

import com.metrix.architecture.constants.MetrixLookupFormat;

/**
 * Contains meta data that describes how a lookup should be performed. Lookups
 * are typically launched by the user selecting an EditText. A new activity will
 * then get launched to display valid results for the user who can then select a
 * row which will be used to populate data on the original layout. Typically you
 * would use a lookup as opposed to a spinner if there are a lot of rows or if
 * you want the user to be able to apply a filter to the rows.
 * 
 * @since 5.4
 */
public class MetrixLookupDef {

	/**
	 * Meta data describing the tables that will be used to display data in the
	 * lookup.
	 */
	public List<MetrixLookupTableDef> tableNames;

	/**
	 * Meta data describing the columns that will be displayed by the lookup and
	 * optionally views on the originating layout that will be populated with
	 * their data when a row is selected by the user.
	 */
	public List<MetrixLookupColumnDef> columnNames;

	/**
	 * Meta data describing how the tables should be filtered in the lookup.
	 */
	public List<MetrixLookupFilterDef> filters;

	/**
	 * TRUE if the query should be issued as soon as the lookup is opened, FALSE
	 * otherwise.
	 */
	public Boolean performInitialSearch;

	/**
	 * The layout title that should be displayed.
	 */
	public String title;

	/**
	 * (Optional) Order bys which should be applied to the lookup results. If no
	 * order bys are included, then the lookup results will be sorted by the 
	 * header column.
	 */
	public List<MetrixOrderByDef> orderBys;
	
	/**
	 * (Optional) MetrixLookupFormat enumeration value defining which lookup
	 * layout to use. If one is not set, the default layout will be used.
	 */
	public MetrixLookupFormat format;
	
	/**
	 * (Optional) Allow the user to select a value that they typed in but that
	 * isn't in the list of acceptable values.
	 * @since 5.6
	 */
	public Boolean allowValueNotInList;
	
	/**
	 * (Optional) Initial search criteria to be loaded and displayed on the
	 * lookup screen.
	 * @since 5.6
	 */
	public String initialSearchCriteria;
	
	/**
	 * A convenience constructor.
	 */
	public MetrixLookupDef() {
		this.tableNames = new ArrayList<MetrixLookupTableDef>();
		this.columnNames = new ArrayList<MetrixLookupColumnDef>();
		this.filters = new ArrayList<MetrixLookupFilterDef>();
		this.orderBys = new ArrayList<MetrixOrderByDef>();
		this.performInitialSearch = true;
		this.allowValueNotInList = false;
	}

	/**
	 * A convenience constructor.
	 * 
	 * @param tableName
	 *            The name of the table whose data should be displayed.
	 *
	 * <pre>
	 * MetrixLookupDef lookupDef = new MetrixLookupDef("part");
	 * lookupDef.columnNames.add(new MetrixLookupColumnDef("part.part_id", R.id.part_usage__part_id));
	 * lookupDef.columnNames.add(new MetrixLookupColumnDef("part.internal_descriptn"));
	 * lookupDef.title = MetrixStringHelper.formatLookupTitle(this, AndroidResourceHelper.getMessage("LookupHelpA"), AndroidResourceHelper.getMessage("Part"));
	 * </pre>
	 */
	public MetrixLookupDef(String tableName) {
		this.tableNames = new ArrayList<MetrixLookupTableDef>();
		this.columnNames = new ArrayList<MetrixLookupColumnDef>();
		this.filters = new ArrayList<MetrixLookupFilterDef>();
		this.orderBys = new ArrayList<MetrixOrderByDef>();
		this.performInitialSearch = true;
		this.allowValueNotInList = false;

		this.tableNames.add(new MetrixLookupTableDef(tableName));
	}

	public MetrixLookupDef(String tableName, MetrixLookupFormat format) {
		this.tableNames = new ArrayList<MetrixLookupTableDef>();
		this.columnNames = new ArrayList<MetrixLookupColumnDef>();
		this.filters = new ArrayList<MetrixLookupFilterDef>();
		this.orderBys = new ArrayList<MetrixOrderByDef>();
		this.performInitialSearch = true;
		this.format = format;
		this.allowValueNotInList = false;
		
		this.tableNames.add(new MetrixLookupTableDef(tableName));		
	}
	
	/**
	 * A convenience constructor.
	 * 
	 * @param tableName
	 *            The name of the table whose data should be displayed.
	 * @param title
	 *            The title of the lookup layout to be shown.
	 */
	public MetrixLookupDef(String tableName, String title) {
		this.tableNames = new ArrayList<MetrixLookupTableDef>();
		this.columnNames = new ArrayList<MetrixLookupColumnDef>();
		this.filters = new ArrayList<MetrixLookupFilterDef>();
		this.performInitialSearch = true;
		this.orderBys = new ArrayList<MetrixOrderByDef>();
		this.title = title;
		this.allowValueNotInList = false;

		this.tableNames.add(new MetrixLookupTableDef(tableName));
	}

	/**
	 * A convenience constructor.
	 * 
	 * @param tableName
	 *            The name of the table whose data should be displayed.
	 * @param performInitialSearch
	 *            TRUE if the query should be issued when the lookup is first
	 *            opened, FALSE otherwise.
	 * 
	 * <pre>
	 * MetrixLookupDef lookupDef = new MetrixLookupDef("stock_serial_id", true);
	 * lookupDef.columnNames.add(new MetrixLookupColumnDef("stock_serial_id.part_id"));
	 * lookupDef.columnNames.add(new MetrixLookupColumnDef("stock_serial_id.serial_id",
	 * 	R.id.truck_part_selected_serial));
	 * lookupDef.title = MetrixStringHelper.formatLookupTitle(this, R.string.LookupHelpA, 
	 * 	R.string.SerialId);
	 * lookupDef.filters.add(new MetrixLookupFilterDef("stock_serial_id.part_id", "=", partId));
	 * lookupDef.filters.add(new MetrixLookupFilterDef("stock_serial_id.usable", "=", "Y"));
	 * </pre>
	 */
	public MetrixLookupDef(String tableName, boolean performInitialSearch) {
		this.tableNames = new ArrayList<MetrixLookupTableDef>();
		this.columnNames = new ArrayList<MetrixLookupColumnDef>();
		this.filters = new ArrayList<MetrixLookupFilterDef>();
		this.orderBys = new ArrayList<MetrixOrderByDef>();
		this.performInitialSearch = performInitialSearch;
		this.allowValueNotInList = false;

		this.tableNames.add(new MetrixLookupTableDef(tableName));
	}

	public MetrixLookupDef(String tableName, MetrixLookupFormat format, boolean performInitialSearch) {
		this.tableNames = new ArrayList<MetrixLookupTableDef>();
		this.columnNames = new ArrayList<MetrixLookupColumnDef>();
		this.filters = new ArrayList<MetrixLookupFilterDef>();
		this.orderBys = new ArrayList<MetrixOrderByDef>();
		this.performInitialSearch = performInitialSearch;
		this.format = format;
		this.allowValueNotInList = false;
	
		this.tableNames.add(new MetrixLookupTableDef(tableName));
	}

	/**
	 * A convenience constructor.
	 * 
	 * @param tableName
	 *            The name of the table whose data should be displayed.
	 * @param performInitialSearch
	 *            TRUE if the query should be issued when the lookup is first
	 *            opened, FALSE otherwise.
	 * @param title
	 *            The title of the lookup layout to be shown.
	 */
	public MetrixLookupDef(String tableName, boolean performInitialSearch, String title) {
		this.tableNames = new ArrayList<MetrixLookupTableDef>();
		this.columnNames = new ArrayList<MetrixLookupColumnDef>();
		this.filters = new ArrayList<MetrixLookupFilterDef>();
		this.orderBys = new ArrayList<MetrixOrderByDef>();
		this.performInitialSearch = performInitialSearch;
		this.title = title;
		this.allowValueNotInList = false;

		this.tableNames.add(new MetrixLookupTableDef(tableName));
	}
	
	/**
	 * Checks whether multiple views will be populated when a row is selected
	 * from the lookup.
	 * 
	 * @return TRUE if multiple views will be populated, FALSE otherwise.
	 * @since 5.6
	 */
	public boolean populatesMultipleViews() {
		if (this.columnNames == null || this.columnNames.size() < 2) {
			return false;
		}
		
		boolean foundView = false;
		for (MetrixLookupColumnDef columnDef : this.columnNames) {
			if (columnDef.controlId != 0) {
				if (foundView) {
					return false;
				} else {
					foundView = true;
				}
			}
		}
		
		return true;
	}
}
