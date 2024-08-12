package com.metrix.architecture.metadata;

import java.util.HashMap;

/**
 * A data structure to store the metrix primary keys information that will
 * be different from the local database primary key set 
 */
public class MetrixKeys {
	/**
	 * The name of the table these keys are defined for.
	 */
	public String tableName;

	/**
	 * The KeyInfo store the pairs of the columnName and dataType.  If to 
	 * store the foreign key information, it will be child key_name and parent 
	 * key_name
	 */
	public HashMap <String, String> keyInfo = new HashMap<String, String>();
	
}
