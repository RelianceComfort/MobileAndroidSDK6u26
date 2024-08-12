package com.metrix.architecture.utilities;

import java.util.HashMap;

import com.metrix.architecture.database.MobileApplication;
import com.metrix.architecture.metadata.MetrixTableStructure;
import com.metrix.architecture.metadata.TableColumnDef;
import com.metrix.architecture.metadata.MetrixKeys;

public class MetrixDatabases {
	public static String DATABASE_PATH = "/data/data/com.metrix.metrixmobile/databases/";
	/*public static String DATA_DATABASE = "mobiledb.sqlite";
	public static String SYNC_DATABASE = "mobilesync.sqlite";*/
	public static String QUERY_CONNECTION = "QueryConnection";
	public static String UPDATE_CONNECTION = "UpdateConnection";
	public static String METRIXTABLEDEFINITION = "MetrixTableDefinition";
	
	public static TableColumnDef getTableColumnDefinition(String table_name, String column_name){
		HashMap<String, MetrixTableStructure> tableDefinition = MobileApplication.getTableDefinitionsFromCache();
		return tableDefinition.get(table_name).mColumns.get(column_name);		
	}
	
	public static MetrixKeys getMetrixTablePrimaryKeys(String table_name){
		HashMap<String, MetrixTableStructure> tableDefinition = MobileApplication.getTableDefinitionsFromCache();
		return tableDefinition.get(table_name).mMetrixPrimaryKeys;		
	}	
}
