package com.metrix.architecture.designer;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.metrix.architecture.database.MetrixCursor;
import com.metrix.architecture.database.MetrixDatabaseManager;
import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.architecture.utilities.LogManager;
import com.metrix.architecture.utilities.MetrixPublicCache;
import com.metrix.architecture.utilities.MetrixStringHelper;
import com.metrix.architecture.utilities.User;

/**
 * This class contains methods which can be used to apply the meta data defined in the
 * mobile designer related to global menu level properties.
 * 
 * @since 5.6.1
 */
public class MetrixGlobalMenuManager extends MetrixDesignerManager {
	public static final Map<String, String> iconMap = initIconMap();
	private static Map<Integer, HashMap<String, String>> globalMenuItems = new HashMap<Integer, HashMap<String, String>>();

	/***
	 * Gets a Map of all of the items to be included
	 * in the global menu, in order.
	 * 
	 * @return the menu items.
	 *  
	 * @since 5.6.1
	 */
	public static Map<Integer, HashMap<String, String>> getGlobalMenuItems() {
		if (globalMenuItems == null || globalMenuItems.size() == 0) {
			if (MetrixGlobalMenuManager.cacheGlobalMenuItems()) {
				return globalMenuItems;
			} else {
				return null;
			}
		} else {
			return globalMenuItems;
		}
	}
	
	public static void clearGlobalMenuItemCache() {
		globalMenuItems.clear();
	}
	
	public static Boolean cacheGlobalMenuItems() {
		String query = "SELECT use_mm_menu_item.item_name, use_mm_menu_item.label, use_mm_menu_item.screen_id, use_mm_menu_item.count_script, "
					+ "use_mm_menu_item.tap_event, use_mm_menu_item.hide_if_zero, use_mm_menu_item.icon_name "
					+ "FROM use_mm_menu_item "
					+ "WHERE use_mm_menu_item.display_order > 0 "
					+ "ORDER BY use_mm_menu_item.display_order ASC";

		MetrixCursor cursor = null;
		int menuCounter = 0;	// display_order may be relative, so use a local counter to enforce proper indexing

		try {
			cursor = MetrixDatabaseManager.rawQueryMC(query, null);
			
			if (cursor == null || !cursor.moveToFirst()) {
				return false;
			}
	
			globalMenuItems.clear();

			while (cursor.isAfterLast() == false) {
				HashMap<String, String> menuItemData = new HashMap<String, String>();
				menuItemData.put("item_name", cursor.getString(0));
				menuItemData.put("label", cursor.getString(1));
				menuItemData.put("screen_id", cursor.getString(2));
				menuItemData.put("count_script", cursor.getString(3));
				menuItemData.put("tap_event", cursor.getString(4));
				menuItemData.put("hide_if_zero", cursor.getString(5));

				String iconName = cursor.getString(6);
				String iconResourceString = iconMap.get(iconName);
				if (MetrixStringHelper.isNullOrEmpty(iconResourceString))
					iconResourceString = "";
				menuItemData.put("icon_name", iconResourceString);

				// display_order indexes the menu items; all other data found by string key in HashMap
				globalMenuItems.put(menuCounter, menuItemData);
				menuCounter++;

				cursor.moveToNext();
			}
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
		
		String personType = MetrixDatabaseManager.getFieldStringValue("person", "metrix_user_type", "person_id = '" + User.getUser().personId + "'");
		if (MetrixStringHelper.valueIsEqual(personType, "STUDIO")) {
			// if STUDIO user, make sure that he can get to Mobile Designer
			@SuppressWarnings("unchecked")
			HashMap<String, String> itemNameToTextMap = (HashMap<String, String>) MetrixPublicCache.instance.getItem("MetrixDesignerGlobalMenuResources");

			HashMap<String, String> menuItemData = new HashMap<String, String>();
			menuItemData.put("item_name", "Designer");
			menuItemData.put("label", itemNameToTextMap.get("Designer"));
			globalMenuItems.put(menuCounter, menuItemData);
			menuCounter++;

			menuItemData = new HashMap<String, String>();
			menuItemData.put("item_name", "Designer - Screen");
			menuItemData.put("label", itemNameToTextMap.get("Designer - Screen"));
			globalMenuItems.put(menuCounter, menuItemData);
			menuCounter++;

			menuItemData = new HashMap<String, String>();
			menuItemData.put("item_name", "Designer - Fields");
			menuItemData.put("label", itemNameToTextMap.get("Designer - Fields"));
			globalMenuItems.put(menuCounter, menuItemData);
			menuCounter++;

			menuItemData = new HashMap<String, String>();
			menuItemData.put("item_name", "Designer - Field Order");
			menuItemData.put("label", itemNameToTextMap.get("Designer - Field Order"));
			globalMenuItems.put(menuCounter, menuItemData);
			menuCounter++;
		}
		
		return true;
	}

	public static void populateDesignerGlobalMenuResources() {
		populateDesignerGlobalMenuResources(false);
	}

	public static void populateDesignerGlobalMenuResources(boolean forceUpdate) {
		if (forceUpdate || !MetrixPublicCache.instance.containsKey("MetrixDesignerGlobalMenuResources")) {
			HashMap<String, String> itemNameToTextMap = new HashMap<String, String>();
			itemNameToTextMap.put("Designer", AndroidResourceHelper.getMessage("Designer"));
			itemNameToTextMap.put("Designer - Field Order", AndroidResourceHelper.getMessage("DesignerFieldOrder"));
			itemNameToTextMap.put("Designer - Fields", AndroidResourceHelper.getMessage("DesignerFields"));
			itemNameToTextMap.put("Designer - Screen", AndroidResourceHelper.getMessage("DesignerScreen"));
			MetrixPublicCache.instance.addItem("MetrixDesignerGlobalMenuResources", itemNameToTextMap);
		}
	}

	public static Boolean stringExistsInDesignerGlobalMenuResources(String text) {
		Boolean valueFound = false;
		try {
			populateDesignerGlobalMenuResources();
			@SuppressWarnings("unchecked")
			HashMap<String, String> itemNameToTextMap = (HashMap<String, String>)MetrixPublicCache.instance.getItem("MetrixDesignerGlobalMenuResources");
			for (String value : itemNameToTextMap.values()) {
				if (MetrixStringHelper.valueIsEqual(value, text)) {
					valueFound = true;
					break;
				}
			}
		} catch (Exception e) {
			LogManager.getInstance().error(e);
		}
		return valueFound;
	}

	private static Map<String, String> initIconMap() {
		Map<String, String> initMap = new HashMap<String, String>();
		initMap.put("ABOUT", "R.drawable.sliding_menu_about");
		initMap.put("BUILDINGS", "R.drawable.sliding_menu_customers");
		initMap.put("CALENDAR", "R.drawable.sliding_menu_calendar");
		initMap.put("CATEGORIES", "R.drawable.sliding_menu_categories");
		initMap.put("CLOCK", "R.drawable.sliding_menu_shift");
		initMap.put("DATABASE", "R.drawable.sliding_menu_query");
		initMap.put("DESIGNER", "R.drawable.sliding_menu_designer");
		initMap.put("EXCLAMATION", "R.drawable.sliding_menu_escalations");
		initMap.put("FOLDER", "R.drawable.sliding_menu_jobs");
		initMap.put("HOME", "R.drawable.sliding_menu_home");
		initMap.put("ITEM_LIST", "R.drawable.sliding_menu_quotes");
		initMap.put("PLAY_SCREEN", "R.drawable.sliding_menu_preview");
		initMap.put("PROFILE", "R.drawable.sliding_menu_profile");
		initMap.put("QR_SCANNER", "R.drawable.sliding_menu_scan_stock");
		initMap.put("SCAN_TOOL", "R.drawable.sliding_menu_receiving");
		initMap.put("SETTINGS", "R.drawable.sliding_menu_settings");
		initMap.put("CLOSE_APP", "R.drawable.sliding_menu_close_app");
		initMap.put("SYNC", "R.drawable.sliding_menu_sync");
		initMap.put("TEAM", "R.drawable.sliding_menu_team");
		initMap.put("THEME", "R.drawable.sliding_menu_skins");
		initMap.put("TRUCK", "R.drawable.sliding_menu_stock");
		return Collections.unmodifiableMap(initMap);
	}
}
