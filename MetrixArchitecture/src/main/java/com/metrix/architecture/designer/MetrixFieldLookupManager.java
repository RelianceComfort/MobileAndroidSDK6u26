package com.metrix.architecture.designer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import android.annotation.SuppressLint;
import com.metrix.architecture.database.MetrixCursor;
import com.metrix.architecture.database.MetrixDatabaseManager;
import com.metrix.architecture.metadata.MetrixLookupColumnDef;
import com.metrix.architecture.metadata.MetrixLookupDef;
import com.metrix.architecture.metadata.MetrixLookupFilterDef;
import com.metrix.architecture.metadata.MetrixLookupTableDef;
import com.metrix.architecture.metadata.MetrixOrderByDef;
import com.metrix.architecture.scripting.ClientScriptDef;
import com.metrix.architecture.scripting.MetrixClientScriptManager;
import com.metrix.architecture.utilities.LogManager;
import com.metrix.architecture.utilities.MetrixFloatHelper;
import com.metrix.architecture.utilities.MetrixStringHelper;

@SuppressLint("UseSparseArrays")
public class MetrixFieldLookupManager extends MetrixDesignerManager {
	public static final Map<String, String> symbolMap = initSymbolMap();	
	private static HashMap<Integer, MetrixLookupDef> fieldLookups = null;
	
	public static void clearFieldLookupCache() {
		fieldLookups = null;
	}
	
	/**
	 * Returns whether the field represented by this field_id has a corresponding lookup.
	 * 
	 * @param fieldId The unique identifier of the field.
	 * 
	 * @since 5.6.3
	 */
	public static boolean fieldHasLookup(Integer fieldId) {
		if (fieldId == null)
			return false;
		
		HashMap<Integer, MetrixLookupDef> lookupSet = getFieldLookups();
		if (lookupSet != null)
			return lookupSet.containsKey(fieldId);
		
		return false;
	}
	
	/**
	 * Returns the lookup definition that corresponds with this field.
	 * 
	 * @param fieldId The unique identifier of the field.
	 * 
	 * @since 5.6.3
	 */
	public static MetrixLookupDef getFieldLookup(Integer fieldId) {
		if (fieldId == null)
			return null;
		
		HashMap<Integer, MetrixLookupDef> lookupSet = getFieldLookups();
		if (lookupSet != null && lookupSet.containsKey(fieldId))
			return lookupSet.get(fieldId);
		
		return null;
	}
	
	private static HashMap<Integer, MetrixLookupDef> getFieldLookups() {
		HashMap<Integer, MetrixLookupDef> lookupSet = fieldLookups;
		if (lookupSet == null) {
			// regenerate cache from DB
			lookupSet = new HashMap<Integer, MetrixLookupDef>();
			MetrixCursor lookupCursor = null;			
			try {
				String query = "select lkup_id, field_id, title, perform_initial_search from use_mm_field_lkup";
				lookupCursor = MetrixDatabaseManager.rawQueryMC(query, null);		
				if (lookupCursor != null && lookupCursor.moveToFirst()) {
					while (lookupCursor.isAfterLast() == false) {
						Integer lkupId = lookupCursor.getInt(0);
						Integer fieldId = lookupCursor.getInt(1);
						String title = lookupCursor.getString(2);
						String performInitialSearch = lookupCursor.getString(3);
						boolean doInitialSearch = MetrixStringHelper.valueIsEqual(performInitialSearch, "Y");
						
						MetrixLookupDef lookupDef = generateLookupFromID(lkupId, title, doInitialSearch, true);
						lookupSet.put(fieldId, lookupDef);
						lookupCursor.moveToNext();
					}					
					fieldLookups = lookupSet;
				}
			} catch (Exception ex) {
				LogManager.getInstance().error(ex);
			} finally {
				if (lookupCursor != null && (!lookupCursor.isClosed())) {
					lookupCursor.close();
				}
			}
		}
		return lookupSet;
	}
	
	public static MetrixLookupDef generateLookupFromID(Integer lkupId, String title, boolean doInitialSearch, boolean calledOutsideDesigner) {
		if (lkupId == null)
			return null;
		
		// This method is also called for validation within the Designer, which should NOT use the use_* tables
		String lkupTableName = "mm_field_lkup_table";
		String lkupColumnName = "mm_field_lkup_column";
		String lkupFilterName = "mm_field_lkup_filter";
		String lkupOrderbyName = "mm_field_lkup_orderby";
		if (calledOutsideDesigner) {
			lkupTableName = "use_mm_field_lkup_table";
			lkupColumnName = "use_mm_field_lkup_column";
			lkupFilterName = "use_mm_field_lkup_filter";
			lkupOrderbyName = "use_mm_field_lkup_orderby";
		}
		
		MetrixLookupDef lookupDef = new MetrixLookupDef();
		lookupDef.title = title;
		lookupDef.performInitialSearch = doInitialSearch;
		
		// TABLES and COLUMNS
		// keep an ordered list of used table names, so that we know what tables have been set up already and in what order we should add them to the lookup
		ArrayList<String> orderedTableNames = new ArrayList<String>();
		HashMap<String, MetrixLookupTableDef> unorderedTableDefs = new HashMap<String, MetrixLookupTableDef>();		
		ArrayList<MetrixLookupColumnDef> invisibleColumns = new ArrayList<MetrixLookupColumnDef>();
		MetrixCursor tableColumnCursor = null;
		try {
					
			String tableColumnQuery = "select t.table_name, t.parent_table_name, t.parent_key_columns, t.child_key_columns,"
				+ " c.column_name, c.linked_field_id, c.always_hide"
				+ " from " + lkupTableName + " t join " + lkupColumnName + " c on t.lkup_table_id = c.lkup_table_id"
				+ " where t.lkup_id = " + lkupId + " order by c.applied_order asc";
			tableColumnCursor = MetrixDatabaseManager.rawQueryMC(tableColumnQuery, null);
			if (tableColumnCursor != null && tableColumnCursor.moveToFirst()) {
				while (tableColumnCursor.isAfterLast() == false) {
					String tableName = tableColumnCursor.getString(0).toLowerCase();
					if (!orderedTableNames.contains(tableName)) {
						String parentTableName = tableColumnCursor.getString(1);
						String parentKeyColumns = tableColumnCursor.getString(2);
						String childKeyColumns = tableColumnCursor.getString(3);
						
						if (!MetrixStringHelper.isNullOrEmpty(parentTableName)) { parentTableName = parentTableName.toLowerCase(); }
						if (!MetrixStringHelper.isNullOrEmpty(parentKeyColumns)) { parentKeyColumns = parentKeyColumns.toLowerCase(); }
						if (!MetrixStringHelper.isNullOrEmpty(childKeyColumns)) { childKeyColumns = childKeyColumns.toLowerCase(); }
						
						MetrixLookupTableDef tableDef = new MetrixLookupTableDef();
						tableDef.tableName = tableName;
						tableDef.parentTableName = parentTableName;
						if (!MetrixStringHelper.isNullOrEmpty(parentKeyColumns)) {
							String[] splitParentKeys = parentKeyColumns.split("\\|");
							for (int i = 0; i < splitParentKeys.length; i++) {						
								tableDef.parentKeyColumns.add(String.format("%1$s.%2$s", parentTableName, splitParentKeys[i]));
							}
						}
						if (!MetrixStringHelper.isNullOrEmpty(childKeyColumns)) {
							String[] splitChildKeys = childKeyColumns.split("\\|");
							for (int i = 0; i < splitChildKeys.length; i++) {
								tableDef.childKeyColumns.add(String.format("%1$s.%2$s", tableName, splitChildKeys[i]));
							}
						}
						unorderedTableDefs.put(tableName, tableDef);						
							
						if (MetrixStringHelper.isNullOrEmpty(parentTableName)) {
							orderedTableNames.add(0, tableName);
						} else if (orderedTableNames.contains(parentTableName)) {
							int parentIndex = orderedTableNames.indexOf(parentTableName);
							orderedTableNames.add(parentIndex + 1, tableName);
						} else
							orderedTableNames.add(tableName);
					}
					
					String columnName = tableColumnCursor.getString(4).toLowerCase();
					String linkedFieldId = tableColumnCursor.getString(5);
					String alwaysHide = tableColumnCursor.getString(6);
					
					MetrixLookupColumnDef columnDef = new MetrixLookupColumnDef();
					columnDef.columnName = String.format("%1$s.%2$s", tableName, columnName);
					columnDef.alwaysHide = MetrixStringHelper.valueIsEqual(alwaysHide, "Y");
					if (!MetrixStringHelper.isNullOrEmpty(linkedFieldId))
						columnDef.linkedFieldId = Integer.valueOf(linkedFieldId);
					
					if (columnDef.alwaysHide)
						invisibleColumns.add(columnDef);
					else
						lookupDef.columnNames.add(columnDef);
					
					tableColumnCursor.moveToNext();
				}
			}
		} catch (Exception ex) {
			LogManager.getInstance().error(ex);
		} finally {
			if (tableColumnCursor != null && (!tableColumnCursor.isClosed()))
				tableColumnCursor.close();
		}
		
		// at this point, use orderedTableNames to put table defs into lookup in proper order
		if (orderedTableNames != null && orderedTableNames.size() > 0) {
			for (String tableName : orderedTableNames) {
				lookupDef.tableNames.add(unorderedTableDefs.get(tableName));
			}
		}
		
		// at this point, add in any definitions for invisible columns (to ensure they come after the visible ones)
		if (invisibleColumns != null && invisibleColumns.size() > 0) {
			for (MetrixLookupColumnDef invisibleCol : invisibleColumns) {
				lookupDef.columnNames.add(invisibleCol);
			}
		}
		
		// FILTERS
		MetrixCursor filterCursor = null;
		try {
			String filterQuery = "select table_name, column_name, operator, right_operand, logical_operator, left_parens, right_parens, no_quotes"
				+ " from " + lkupFilterName + " where lkup_id = " + lkupId + " order by applied_order asc";
			filterCursor = MetrixDatabaseManager.rawQueryMC(filterQuery, null);
			if (filterCursor != null && filterCursor.moveToFirst()) {
				while (filterCursor.isAfterLast() == false) {
					String tableName = filterCursor.getString(0).toLowerCase();
					String columnName = filterCursor.getString(1).toLowerCase();
					String rawOperator = filterCursor.getString(2);
					String operator = "=";
					if (symbolMap.containsKey(rawOperator))
						operator = symbolMap.get(rawOperator);
					String rightOperand = filterCursor.getString(3);
					String logicalOperator = filterCursor.getString(4);
					String leftParensString = filterCursor.getString(5);
					String rightParensString = filterCursor.getString(6);
					String noQuotesString = filterCursor.getString(7);
					
					if (MetrixStringHelper.isNullOrEmpty(rightOperand))
						rightOperand = "";
					
					if (MetrixStringHelper.isNullOrEmpty(logicalOperator))
						logicalOperator = "";
					else
						logicalOperator = logicalOperator.toLowerCase();

					if (MetrixStringHelper.isNullOrEmpty(leftParensString))
						leftParensString = "0";
					if (MetrixStringHelper.isNullOrEmpty(rightParensString))
						rightParensString = "0";
					
					MetrixLookupFilterDef filterDef = new MetrixLookupFilterDef();
					filterDef.leftOperand = String.format("%1$s.%2$s", tableName, columnName);
					filterDef.operator = operator;
					filterDef.rightOperand = rightOperand;
					ClientScriptDef scriptDef = MetrixClientScriptManager.getScriptDefForScriptID(filterDef.rightOperand);
					if (scriptDef != null)
						filterDef.scriptForRightOperand = rightOperand;
					filterDef.logicalOperator = logicalOperator;
					filterDef.leftParens = MetrixFloatHelper.convertNumericFromDBToNumber(leftParensString).intValue();
					filterDef.rightParens = MetrixFloatHelper.convertNumericFromDBToNumber(rightParensString).intValue();
					filterDef.noQuotes = (MetrixStringHelper.valueIsEqual(noQuotesString, "Y"));
					lookupDef.filters.add(filterDef);
					
					filterCursor.moveToNext();
				}
			}
		} catch (Exception ex) {
			LogManager.getInstance().error(ex);
		} finally {
			if (filterCursor != null && (!filterCursor.isClosed()))
				filterCursor.close();
		}
		
		// ORDERBYS
		MetrixCursor orderbyCursor = null;
		try {
			String orderbyQuery = "select table_name, column_name, sort_order"
				+ " from " + lkupOrderbyName + " where lkup_id = " + lkupId + " order by applied_order asc";
			orderbyCursor = MetrixDatabaseManager.rawQueryMC(orderbyQuery, null);
			if (orderbyCursor != null && orderbyCursor.moveToFirst()) {
				while (orderbyCursor.isAfterLast() == false) {
					String tableName = orderbyCursor.getString(0).toLowerCase();
					String columnName = orderbyCursor.getString(1).toLowerCase();
					String sortOrder = orderbyCursor.getString(2).toLowerCase();
					
					MetrixOrderByDef orderbyDef = new MetrixOrderByDef();
					orderbyDef.columnName = String.format("%1$s.%2$s", tableName, columnName);
					orderbyDef.sortOrder = sortOrder;
					lookupDef.orderBys.add(orderbyDef);
					
					orderbyCursor.moveToNext();
				}
			}
		} catch (Exception ex) {
			LogManager.getInstance().error(ex);
		} finally {
			if (orderbyCursor != null && (!orderbyCursor.isClosed()))
				orderbyCursor.close();
		}
		
		return lookupDef;
	}
	
	private static Map<String, String> initSymbolMap() {
		Map<String, String> initMap = new HashMap<String, String>();
		initMap.put("EQUALS", "=");
		initMap.put("GREATER_THAN", ">");
		initMap.put("GREATER_THAN_EQUAL", ">=");
		initMap.put("LESS_THAN", "<");
		initMap.put("LESS_THAN_EQUAL", "<=");
		initMap.put("NOT_EQUALS", "!=");
		initMap.put("IS_NULL", "is null");
		initMap.put("IS_NOT_NULL", "is not null");
		initMap.put("LIKE", "like");
		initMap.put("NOT_LIKE", "not like");
		initMap.put("IN", "in");
		initMap.put("NOT_IN", "not in");
		return Collections.unmodifiableMap(initMap);
	}
}