package com.metrix.architecture.designer;

import java.util.HashMap;
import java.util.Map;
import android.annotation.SuppressLint;
import com.metrix.architecture.database.MetrixCursor;
import com.metrix.architecture.database.MetrixDatabaseManager;
import com.metrix.architecture.utilities.LogManager;

public class MetrixHomeMenuManager extends MetrixDesignerManager {
	private static final int LIMIT = 8;

	@SuppressLint("UseSparseArrays")
	private static Map<Integer, HashMap<String, String>> homeMenuItems = new HashMap<Integer, HashMap<String, String>>();

	/***
	 * Gets a LinkedHashMap of all of the items to be included
	 * in the global menu, in order.
	 * 
	 * @return the menu items.
	 *  
	 * @since 5.6.1
	 */
	public static Map<Integer, HashMap<String, String>> getHomeMenuItems() {
		if (homeMenuItems == null || homeMenuItems.size() == 0) {
			if (MetrixHomeMenuManager.cacheHomeMenuItems()) {
				return homeMenuItems;
			} else {
				return null;
			}
		} else {
			return homeMenuItems;
		}
	}
	
	//resetting cached
	public static void clearHomeMenuItemCache(){
		homeMenuItems.clear();
	}
	
	public static Boolean cacheHomeMenuItems() {
		String query = 	"SELECT use_mm_home_item.item_name, use_mm_home_item.label, use_mm_home_item.image_id, " +
						"use_mm_home_item.screen_id, use_mm_home_item.count_script, use_mm_home_item.tap_event " +
						"FROM use_mm_home_item " +
						"WHERE use_mm_home_item.display_order > 0 " +
						"ORDER BY use_mm_home_item.display_order ASC " + 
						"LIMIT " + LIMIT;

		MetrixCursor cursor = null;
		
		try {
			cursor = MetrixDatabaseManager.rawQueryMC(query, null);
			
			if (cursor == null || !cursor.moveToFirst()) {
				return false;
			}
			
			homeMenuItems.clear();

			int homeCounter = 1;	// display_order may be relative, so use a local counter to enforce proper indexing
			while (cursor.isAfterLast() == false) {
				HashMap<String, String> homeItemData = new HashMap<String, String>();
				homeItemData.put("item_name", cursor.getString(0));
				homeItemData.put("label", cursor.getString(1));
				homeItemData.put("image_id", cursor.getString(2));
				homeItemData.put("screen_id", cursor.getString(3));
				homeItemData.put("count_script", cursor.getString(4));
				homeItemData.put("tap_event", cursor.getString(5));
				
				// display_order indexes the home items; all other data found by string key in HashMap
				homeMenuItems.put(homeCounter, homeItemData);
				homeCounter++;
				
				cursor.moveToNext();
			}
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
		return true;
	}

	public static Boolean hasEvenItemCount() {
		Boolean isEven = true;

		try {
			Map<Integer, HashMap<String, String>> tempItemSet = getHomeMenuItems();
			if (tempItemSet != null)
				isEven = (tempItemSet.size() % 2 == 0);
		} catch (Exception e) {
			LogManager.getInstance().error(e);
		}

		return isEven;
	}
}
