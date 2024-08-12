package com.metrix.architecture.utilities;

import java.util.ArrayList;
import java.util.Hashtable;

public class MetrixCurrentKeysHelper {

	private static String KEYS_CACHE_NAME = "MM_CURRENT_KEYS";

	@SuppressWarnings("unchecked")
	private synchronized static Hashtable<String, String> getCache() {
		if (!MetrixPublicCache.instance.containsKey(KEYS_CACHE_NAME)) {
			MetrixPublicCache.instance.addItem(KEYS_CACHE_NAME,
					new Hashtable<String, String>());
		}
		return (Hashtable<String, String>) MetrixPublicCache.instance
				.getItem(KEYS_CACHE_NAME);
	}

	private synchronized static String getUniqueName(String tableName, String keyName) {
		return tableName + "." + keyName;
	}
	
	/**
	 * Get the current value of a key defined by the table name and key name.
	 * 
	 * @param tableName
	 * @param keyName
	 * @return String
	 */
	public synchronized static String getKeyValue(String tableName, String keyName) {
		Hashtable<String, String> keys = getCache();
		String uniqueName = getUniqueName(tableName, keyName);

		if (keys.containsKey(uniqueName)) {
			return (String) keys.get(uniqueName);
		} else {
			return "";
		}
	}

	/**
	 * Does the specified key exist in the MM_CURRENT_KEYS table?
	 * 
	 * @param tableName
	 * @param keyName
	 * @return
	 */
	public synchronized static boolean keyExists(String tableName, String keyName) {
		Hashtable<String, String> keys = getCache();
		String uniqueName = getUniqueName(tableName, keyName);

		if (keys.containsKey(uniqueName)) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Set the key value either by inserting or updating the MM_CURRENT_KEYS
	 * table. This method should be invoked from screens wishing to set or
	 * update the key.
	 * 
	 * @param tableName
	 * @param keyName
	 * @param keyValue
	 */
	public synchronized static void setKeyValue(String tableName, String keyName,
			String value) {
		Hashtable<String, String> keys = getCache();
		String uniqueName = getUniqueName(tableName, keyName);

		if (keyExists(tableName, keyName)) {
			keys.remove(uniqueName);
		}
		keys.put(uniqueName, value);
	}

	/**
	 * Set the key value either by inserting or updating the MM_CURRENT_KEYS
	 * table. This method should only be invoked from the sync process.
	 * 
	 * @param tableName
	 * @param keyName
	 * @param keyValue
	 */
	public synchronized static void setKeyValueFromSync(String tableName, String keyName,
			String newValue, String oldValue) {
		Hashtable<String, String> keys = getCache();
		
		if (keyExists(tableName, keyName)) {
			String keyValue = getKeyValue(tableName, keyName);
			
			if (oldValue.compareTo(keyValue) == 0) {
				String uniqueName = getUniqueName(tableName, keyName);
				keys.put(uniqueName, newValue);
			}
		} 
	}

	/**
	 * Get the names of the keys currently being managed for a table.
	 * 
	 * @param tableName
	 * @return ArrayList<String>
	 */
	public synchronized static ArrayList<String> getKeyNames(String tableName) {
		ArrayList<String> results = new ArrayList<String>();
		Hashtable<String, String> keys = getCache();

		if (keys != null) {
			for (String key : keys.keySet()) {
				String[] splitValue = key.split("\\.");
				String keyTableName = splitValue[0];
				if (MetrixStringHelper.valueIsEqual(keyTableName, tableName))
					results.add(splitValue[1]);
			}
		}

		return results;
	}
}
