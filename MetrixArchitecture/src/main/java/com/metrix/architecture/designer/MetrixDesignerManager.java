package com.metrix.architecture.designer;

import java.util.HashMap;
import java.util.Locale;

import com.metrix.architecture.database.MobileApplication;
import com.metrix.architecture.metadata.MetrixTableStructure;
import com.metrix.architecture.utilities.MetrixStringHelper;

public class MetrixDesignerManager {
	protected static boolean clientDBContainsTable(String tableName) {
		if (MetrixStringHelper.isNullOrEmpty(tableName)) {
			return false;
		}
		
		if (MetrixStringHelper.valueIsEqual(tableName.toLowerCase(Locale.US), "custom")) {
			return true;
		}
		
		HashMap<String, MetrixTableStructure> tableStructures = MobileApplication.getTableDefinitionsFromCache();
		MetrixTableStructure tableDefinition = tableStructures.get(tableName);
		return (tableDefinition != null);
	}
}
