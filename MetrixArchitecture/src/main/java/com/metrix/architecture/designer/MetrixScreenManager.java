package com.metrix.architecture.designer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TabHost;
import android.widget.TextView;

import com.metrix.architecture.database.MetrixCursor;
import com.metrix.architecture.database.MetrixDatabaseManager;
import com.metrix.architecture.scripting.ClientScriptDef;
import com.metrix.architecture.scripting.MetrixClientScriptManager;
import com.metrix.architecture.superclasses.MetrixBaseActivity;
import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.architecture.utilities.LogManager;
import com.metrix.architecture.utilities.MetrixStringHelper;

/**
 * This class contains methods which can be used to apply the meta data defined in the
 * mobile designer related to screen level properties.
 * 
 * @since 5.6.1
 */
@SuppressLint({ "UseSparseArrays", "DefaultLocale" }) 
public class MetrixScreenManager extends MetrixDesignerManager {
	private static Map<String, Integer> screenIds = new HashMap<String, Integer>();
	private static Map<Integer, HashMap<String, String>> allScreenProperties = new HashMap<Integer, HashMap<String,String>>();
	
	public static void clearScreenIdsCache() {
		MetrixScreenManager.screenIds.clear();
	}
	
	/**
	 * Clear all cached screen properties.
	 */
	public static void clearScreenPropertiesCache() {
		MetrixScreenManager.allScreenProperties.clear();
	}
	
	/**
	 * Clear cached screen properties of a particular screen by activity.
	 * @param activity
	 */
	public static void clearScreenPropertiesOfSpecificScreenCache(Activity activity) {
		String activityName = activity.getClass().getSimpleName();
		int screenId = MetrixScreenManager.getScreenId(activityName);
		clearScreenPropertiesOfSpecificScreenCache(screenId);
	}
	
	/**
	 * Clear cached screen properties of a particular screen by screen id.
	 * @param screenId
	 */
	public static void clearScreenPropertiesOfSpecificScreenCache(int screenId) {
		if(MetrixScreenManager.allScreenProperties.containsKey(screenId))
			MetrixScreenManager.allScreenProperties.remove(screenId);
	}
	
	/***
	 * Determines if a screen should be read only.
	 *
	 * @param screenName the name of the screen.
	 * @return
	 * @since 5.6.1
	 * @modifed 5.6.3
	 */
	public static boolean screenIsReadOnly(String screenName) {
		if (MetrixStringHelper.isNullOrEmpty(screenName)) {
			throw new IllegalArgumentException(AndroidResourceHelper.getMessage("TheScrnNameParamReq"));
		}

		Boolean readOnly = false;
		int screenId = getScreenId(screenName);
		
		HashMap<String, String> currentScreenProperties = getScreenProperties(screenId);
		if (currentScreenProperties != null && currentScreenProperties.containsKey("read_only")) {
			String readOnlyValue = currentScreenProperties.get("read_only");
			if ((!MetrixStringHelper.isNullOrEmpty(readOnlyValue)) && (readOnlyValue.compareToIgnoreCase("Y") == 0))
				readOnly = true;
		}
		return readOnly;
	}
	
	/***
	 * Determines screen_id, based on screen_name passed in.
	 * 
	 * @param screenName the name of the screen.
	 * @return
	 * @since 5.6.1
	 */
	public static int getScreenId(String screenName) {
		if (MetrixStringHelper.isNullOrEmpty(screenName)) {
			throw new IllegalArgumentException(AndroidResourceHelper.getMessage("TheScrnNameParamReq"));
		}

		if (MetrixScreenManager.screenIds != null && MetrixScreenManager.screenIds.containsKey(screenName)) {
			return MetrixScreenManager.screenIds.get(screenName);
		} else {
			String query = "SELECT use_mm_screen.screen_id FROM use_mm_screen " + 
						   "WHERE use_mm_screen.screen_name = '" + screenName + "'";
		
			MetrixCursor cursor = null;
			try {
				cursor = MetrixDatabaseManager.rawQueryMC(query, null);
	
				if (cursor == null || !cursor.moveToFirst()) {
					return -1;
				}
	
				while (cursor.isAfterLast() == false) {
					int screenId = cursor.getInt(0);
					screenIds.put(screenName, screenId);
					return screenId;
				}
			} finally {
				if (cursor != null) {
					cursor.close();
				}
			}
			return -1;
		}
	}

	public static int getScreenId(Activity activity){
		return getScreenId(getScreenName(activity));
	}
	
	/***
	 * Determines screen_type, based on activity passed in.
	 * 
	 * @param activity The current activity.
	 * @return The matching screen_type value for this activity.
	 * @since 5.6.2
	 */
	public static String getScreenType(Activity activity) {
		String activityName = activity.getClass().getSimpleName();
		int screenId = getScreenId(activityName);
		
		return getScreenType(screenId);
	}

	/***
	 * Determines screen_type, based on activity passed in.
	 * 
	 * @param screenId The current screen id.
	 * @return The matching screen_type value for this screen.
	 * @since 5.6.2
	 */
	public static String getScreenType(int screenId) {
		HashMap<String, String> currentScreenProperties = getScreenProperties(screenId);
		if (currentScreenProperties != null && currentScreenProperties.containsKey("screen_type"))
			return currentScreenProperties.get("screen_type");
		return null;
	}

	public static ClientScriptDef getTapEventScriptDef(int screenId){
		HashMap<String, String> currentScreenProps = getScreenProperties(screenId);
		if (currentScreenProps != null && currentScreenProps.containsKey("tap_event"))
			return MetrixClientScriptManager.getScriptDefForScriptID(currentScreenProps.get("tap_event"));
		return null;
	}

	public static ClientScriptDef getMapScriptDef(int screenId){
		HashMap<String, String> currentScreenProps = getScreenProperties(screenId);
		if (currentScreenProps != null && currentScreenProps.containsKey("map_script"))
			return MetrixClientScriptManager.getScriptDefForScriptID(currentScreenProps.get("map_script"));
		return null;
	}

	public static String getForceOrder(Activity activity) {
		String activityName = activity.getClass().getSimpleName();
		int screenId = getScreenId(activityName);
		
		return getForceOrder(screenId);
	}
	
	public static String getForceOrder(int screenId) {		
		HashMap<String, String> currentScreenProperties = getScreenProperties(screenId);
		if (currentScreenProperties != null && currentScreenProperties.containsKey("force_order"))
			return currentScreenProperties.get("force_order");
		return null;
	}
	
	/***
	 * Determines if fields exist for this screen, based on screen_id passed in.
	 * 
	 * @param screenId the identifier of the screen.
	 * @return
	 * @since 5.6.1
	 */
	public static boolean screenHasFields(int screenId) {
		if (screenId <= 0) {
			throw new IllegalArgumentException(AndroidResourceHelper.getMessage("TheScreenIdParamReq"));
		}

		String query = "SELECT use_mm_field.field_id FROM use_mm_field " + 
					   "WHERE use_mm_field.screen_id = " + screenId;
		
		MetrixCursor cursor = null;
		try {
			cursor = MetrixDatabaseManager.rawQueryMC(query, null);
	
			if (cursor == null || !cursor.moveToFirst()) {
				return false;
			}
	
			while (cursor.isAfterLast() == false) {
				return true;
			}
		} finally {
			cursor.close();
		}
		
		return false;		
	}

	public static boolean screenHasMap(int screenId) {
		final HashMap<String, String> currentScreenProps = getScreenProperties(screenId);
		if (currentScreenProps != null && currentScreenProps.containsKey("map_enabled")) {
			final String flag = currentScreenProps.get("map_enabled");
			if (!MetrixStringHelper.isNullOrEmpty(flag))
				return MetrixStringHelper.valueIsEqual("y", flag.toLowerCase());
		}
		return false;
	}
	
	public static ClientScriptDef getScriptDefForScreenRefresh(Activity activity) {
		ClientScriptDef scriptDef = null;
		try {
			String activityName = activity.getClass().getSimpleName();
			int screenId = MetrixScreenManager.getScreenId(activityName);

			scriptDef = getScriptDefForScreenRefresh(screenId);
		} catch (Exception e) {
			LogManager.getInstance().error(e);
		}
		return scriptDef;
	}
	
	/**
	 * Get the ClientScriptDef for a screen's refresh event.
	 * 
	 * @return A ClientScriptDef object representing the script to run.
	 * 
	 * @since 5.6.3
	 */
	public static ClientScriptDef getScriptDefForScreenRefresh(int screenId) {
		ClientScriptDef scriptDef = null;
		try {

			String scriptId = null;
			HashMap<String, String> currentScreenProperties = getScreenProperties(screenId);
			if (currentScreenProperties != null && currentScreenProperties.containsKey("refresh_event"))
				scriptId = currentScreenProperties.get("refresh_event");
			
			if (!MetrixStringHelper.isNullOrEmpty(scriptId))
				scriptDef = MetrixClientScriptManager.getScriptDefForScriptID(scriptId);
		} catch (Exception e) {
			LogManager.getInstance().error(e);
		}
		return scriptDef;
	}

	/**
	 * Get the ClientScriptDef for a screen's populate script.
	 *
	 * @return A ClientScriptDef object representing the script to run.
	 *
	 * @since 5.7.0
	 */
	public static ClientScriptDef getScriptDefForScreenPopulation(int screenId) {
		ClientScriptDef scriptDef = null;
		try {
			String scriptId = null;
			HashMap<String, String> currentScreenProperties = getScreenProperties(screenId);
			if (currentScreenProperties != null && currentScreenProperties.containsKey("populate_script"))
				scriptId = currentScreenProperties.get("populate_script");

			if (!MetrixStringHelper.isNullOrEmpty(scriptId))
				scriptDef = MetrixClientScriptManager.getScriptDefForScriptID(scriptId);
		} catch (Exception e) {
			LogManager.getInstance().error(e);
		}
		return scriptDef;
	}
	
	public static String setLabelTipAndHelp(Activity activity, ViewGroup layout){
		String activityName = activity.getClass().getSimpleName();
		int screenId = getScreenId(activityName);
		
		return setLabelTipAndHelp(layout, screenId);
	}
	
	/**
	 * Populate the text of Label and Tip controls at top of screen
	 * and help text, based on screen property values.
	 * This method currently assumes that there is a unique value for each property
	 * on each screen.  Sets Label and Tip by control Tag and returns the Help text.
	 * 
	 * @param layout The current activity's layout.
	 * @return The correct Help string from screen property values.
	 * @since 5.6.1
	 */
	public static String setLabelTipAndHelp(ViewGroup layout, int screenId) {
		HashMap<String, String> currentScreenProperties = getScreenProperties(screenId);
		
		String localizedHelp = null;
		if (currentScreenProperties != null){ 
			if(currentScreenProperties.containsKey("help"))
				localizedHelp = currentScreenProperties.get("help");		

			if (layout != null) {
				String localizedLabel = null;
				if(currentScreenProperties.containsKey("label"))
					localizedLabel = currentScreenProperties.get("label");	

				String localizedTip = null;
				if(currentScreenProperties.containsKey("tip"))
					localizedTip = currentScreenProperties.get("tip");	
				
				TextView tvLabel = (TextView) layout.findViewWithTag("SCREEN_LABEL");
				TextView tvTip = (TextView) layout.findViewWithTag("SCREEN_TIP");
				
				if (tvLabel != null && !MetrixStringHelper.isNullOrEmpty(localizedLabel)) {
					tvLabel.setText(localizedLabel);
				}
				
				if (tvTip != null && !MetrixStringHelper.isNullOrEmpty(localizedTip)) {
					tvTip.setText(localizedTip);
				}
			}
		}
		return localizedHelp;
	}
	
	public static String setLabelTipAndHelpForTabs(Activity activity, ViewGroup layout, View rootView, ArrayList<Integer> resourceList){
		String activityName = activity.getClass().getSimpleName();
		int screenId = getScreenId(activityName);
		
		return setLabelTipAndHelpForTabs(layout, rootView, resourceList, screenId);
	}
	
	/**Populate the text of Label and Tip controls at top of screen for each tab
	 * and help text, based on screen property values.
	 * This method currently assumes that there is a unique value for each property
	 * on each screen.
	 * Splits the Label & Tip Values using Delimiter"|".
	 * Sets the Label & Tip value by control Tag for each Tab and returns the Help text.
	 * @param layout One of the Activities layout called 'TableLayout'
	 * @param rootView Root View of the Activity
	 * @param resourceList Consist of each tab layout 
	 * @return The correct Help string from screen property values.
	 * @since 5.6.1
	 */
	public static String setLabelTipAndHelpForTabs(ViewGroup layout, View rootView, ArrayList<Integer> resourceList, int screenId) {
		HashMap<String, String> currentScreenProperties = getScreenProperties(screenId);
		
		String localizedHelp = null;
		if (currentScreenProperties != null){ 
			if(currentScreenProperties.containsKey("help"))
				localizedHelp = currentScreenProperties.get("help");	
		
			if (layout != null) {
				String localizedLabel = null;
				if(currentScreenProperties.containsKey("label"))
					localizedLabel = currentScreenProperties.get("label");	

				String localizedTip = null;
				if(currentScreenProperties.containsKey("tip"))
					localizedTip = currentScreenProperties.get("tip");	
	
				String[] localizedLabels = null;
				String[] localizedTips = null;
	
				if (!MetrixStringHelper.isNullOrEmpty(localizedLabel)) {
					localizedLabels = localizedLabel.split("\\|");
				}
	
				if (!MetrixStringHelper.isNullOrEmpty(localizedTip)) {
					localizedTips = localizedTip.split("\\|");
				}
	
				for (int i = 0; i < resourceList.size(); i++) {
					View tabView = rootView.findViewById(resourceList.get(i));
	
					if (tabView != null) {
						String lLabel = null;
						if (i <= (localizedLabels.length - 1)) {
							lLabel = localizedLabels[i];
							TextView tvLabel = (TextView) tabView
									.findViewWithTag("SCREEN_LABEL");
							if (tvLabel != null
									&& !MetrixStringHelper
											.isNullOrEmpty(lLabel)) {
								tvLabel.setText(lLabel.trim());
							}
						}
	
						String lTip = null;
						if (i <= (localizedTips.length - 1)) {
							lTip = localizedTips[i];
							TextView tvTip = (TextView) tabView
									.findViewWithTag("SCREEN_TIP");
							if (tvTip != null
									&& !MetrixStringHelper.isNullOrEmpty(lTip)) {
								tvTip.setText(lTip.trim());
							}
						}
					}
				}
	
			}
		}
		return localizedHelp;
	}
	
	/**Identifies whether there's a tab existing or not in the current activity by the Tag
	 * and returns true/false
	 * @param view layout One of the activities layout called 'TableLayout'
	 * @return existing of a tab in the current activity or not.
	 * @since 5.6.1
	 */
	public static boolean isTabAssociated(View view) {
		boolean status = false;
		if (view != null) {
			View rootView = view.getRootView();
			if (rootView != null) {
				TabHost tabHost = (TabHost) rootView.findViewWithTag("TAB_HOST");
				if (tabHost != null)
					status = true;
			}
		}
		return status;
	}
	
	/**
	 * Returns the screen name by screen id.
	 * @param screenId
	 * @return screen id.
	 */
	@SuppressLint("DefaultLocale") 
	public static String getScreenName(int screenId) {	
		HashMap<String, String> currentScreenProperties = getScreenProperties(screenId);
		if (currentScreenProperties != null && currentScreenProperties.containsKey("screen_name"))
			return currentScreenProperties.get("screen_name");
		return null;
	}

	/**
	 * Returns the screen name by analyzing the activity provided.
	 * @param activity
	 * @return screen name.
     */
	public static String getScreenName(Activity activity) {
		String screenName = activity.getClass().getSimpleName();
		// retrieve the screen name of the visible tab child screen,
		// which is stamped onto the tag of the currently shown tab
		View rootView = activity.findViewById(android.R.id.content);
		TabHost tabHost = (TabHost)rootView.findViewById(android.R.id.tabhost);
		if (tabHost != null) {
			screenName = tabHost.getCurrentTabTag();
		} else if (activity instanceof MetrixBaseActivity) {
			MetrixBaseActivity metrixBaseActivity = (MetrixBaseActivity) activity;
			if (metrixBaseActivity.isCodelessScreen)
				screenName = metrixBaseActivity.codeLessScreenName;
		}
		return screenName;
	}

	/**
	 * Returns screen properties by activity. 
	 * @param activity
	 * @return screen properties.
	 */
	public static HashMap<String, String> getScreenProperties(Activity activity){
		String activityName = activity.getClass().getSimpleName();
		int screenId = MetrixScreenManager.getScreenId(activityName);
		
		return getScreenProperties(screenId);
	}
	
	/**
	 * Returns screen properties by screen id. 
	 * @param screenId
	 * @return screen properties.
	 */
	public static HashMap<String, String> getScreenProperties(int screenId) {
		MetrixCursor cursor = null;
		
		HashMap<String, String> currentScreenProperties = allScreenProperties.get(screenId);
		if(currentScreenProperties == null)
		{
			currentScreenProperties = new HashMap<String,String>();
			
			try {
				String query = String.format(
						"SELECT use_mm_screen.action_bar_script, use_mm_screen.allow_delete, use_mm_screen.allow_modify, " +
						"use_mm_screen.description, use_mm_screen.design_id, use_mm_screen.force_order, " +
						"use_mm_screen.help, use_mm_screen.label, use_mm_screen.linked_screen_id, use_mm_screen.populate_script, use_mm_screen.primary_table, " +
						"use_mm_screen.read_only, use_mm_screen.refresh_event, use_mm_screen.screen_name, " +
						"use_mm_screen.screen_type, use_mm_screen.searchable, use_mm_screen.tip, use_mm_screen.where_clause_script, use_mm_screen.tap_event, " +
						"use_mm_screen.map_enabled, use_mm_screen.map_script " +
						"FROM use_mm_screen " +
						"WHERE use_mm_screen.screen_id = %d", screenId);
				
					cursor = MetrixDatabaseManager.rawQueryMC(query, null);
					if (cursor == null || !cursor.moveToFirst()) {
						throw new Exception(String.format("getScreenProperties: No screen(ID:%s) metadata found!", screenId));
					}
	
					while (cursor.isAfterLast() == false) {
						currentScreenProperties.put("action_bar_script", cursor.getString(0));
						currentScreenProperties.put("allow_delete", cursor.getString(1));
						currentScreenProperties.put("allow_modify", cursor.getString(2));
						currentScreenProperties.put("description", cursor.getString(3));
						currentScreenProperties.put("design_id", cursor.getString(4));
						currentScreenProperties.put("force_order", cursor.getString(5));
						currentScreenProperties.put("help", cursor.getString(6));
						currentScreenProperties.put("label", cursor.getString(7));
						currentScreenProperties.put("linked_screen_id", cursor.getString(8));
						currentScreenProperties.put("populate_script", cursor.getString(9));
						currentScreenProperties.put("primary_table", cursor.getString(10));
						currentScreenProperties.put("read_only", cursor.getString(11));
						currentScreenProperties.put("refresh_event", cursor.getString(12));
						currentScreenProperties.put("screen_name", cursor.getString(13));
						currentScreenProperties.put("screen_type", cursor.getString(14));
						currentScreenProperties.put("searchable", cursor.getString(15));
						currentScreenProperties.put("tip", cursor.getString(16));
						currentScreenProperties.put("where_clause_script", cursor.getString(17));
						currentScreenProperties.put("tap_event", cursor.getString(18));
						currentScreenProperties.put("map_enabled", cursor.getString(19));
						currentScreenProperties.put("map_script", cursor.getString(20));
						break;
					}
					allScreenProperties.put(screenId, currentScreenProperties);

			} catch (Exception ex) {
				LogManager.getInstance().error(ex);
			}finally {
				if (cursor != null) {
					cursor.close();
				}
			}
		}
		return currentScreenProperties;
	}
	
	/**
	 * Check modifying is allowed by activity.
	 * @param activity
	 * @return true if modifying is allowed.
	 */
	public static boolean isModifyAllowed(Activity activity){
		String activityName = activity.getClass().getSimpleName();
		int screenId = MetrixScreenManager.getScreenId(activityName);
		
		return isModifyAllowed(screenId);
	}
	
	/**
	 * Check modifying is allowed by screen id.
	 * @param screenId
	 * @return true if modifying is allowed.
	 */
	public static boolean isModifyAllowed(int screenId){
		boolean modifyAllowed = false;
		HashMap<String, String> currentScreenProperties = MetrixScreenManager.getScreenProperties(screenId);
		if (currentScreenProperties != null && currentScreenProperties.containsKey("allow_modify"))
		{
			String strModifyAllowed = currentScreenProperties.get("allow_modify");
			if(MetrixStringHelper.valueIsEqual(strModifyAllowed, "Y"))
				modifyAllowed = true;
		}
		
		return modifyAllowed;
	}
	
	/**
	 * Check deleting is allowed by activity.
	 * @param activity
	 * @return true if deleting is allowed.
	 */
	public static boolean isDeleteAllowed(Activity activity){
		String activityName = activity.getClass().getSimpleName();
		int screenId = MetrixScreenManager.getScreenId(activityName);
		
		return isDeleteAllowed(screenId);
	}
	
	/**
	 * Check deleting is allowed by screen id.
	 * @return true if deleting is allowed.
	 */
	public static boolean isDeleteAllowed(int screenId){
		boolean deleteAllowed = false;
		HashMap<String, String> currentScreenProperties = MetrixScreenManager.getScreenProperties(screenId);
		if (currentScreenProperties != null && currentScreenProperties.containsKey("allow_delete"))
		{
			String strModifyAllowed = currentScreenProperties.get("allow_delete");
			if(MetrixStringHelper.valueIsEqual(strModifyAllowed, "Y"))
				deleteAllowed = true;
		}
		
		return deleteAllowed;
	}
	
	/**
	 * Check whether this activity is a standard screen type, set to allow modify only.
	 * @param activity
	 * @return true if modifying is allowed.
	 */
	public static boolean isStandardUpdateOnly(Activity activity){
		String activityName = activity.getClass().getSimpleName();
		int screenId = MetrixScreenManager.getScreenId(activityName);	
		return isStandardUpdateOnly(screenId);
	}
	
	/**
	 * Check whether this screen id corresponds with a standard screen type, set to allow modify only.
	 * @param screenId
	 * @return true if modifying is allowed.
	 */
	public static boolean isStandardUpdateOnly(int screenId){
		boolean isStandardUpdateOnly = false;
		HashMap<String, String> currentScreenProperties = MetrixScreenManager.getScreenProperties(screenId);
		if (currentScreenProperties != null && currentScreenProperties.containsKey("allow_modify") && currentScreenProperties.containsKey("screen_type"))
		{
			String screenType = currentScreenProperties.get("screen_type");
			String strModifyAllowed = currentScreenProperties.get("allow_modify");
			if (MetrixStringHelper.valueIsEqual(screenType, "STANDARD") && MetrixStringHelper.valueIsEqual(strModifyAllowed, "Y"))
				isStandardUpdateOnly = true;
		}	
		return isStandardUpdateOnly;
	}
	
	/**
	 * Gets the linked screen name by activity.
	 * @param activity
	 * @return linked screen name.
	 */
	public static String getLinkedScreenName(Activity activity){
		String activityName = activity.getClass().getSimpleName();
		int screenId = MetrixScreenManager.getScreenId(activityName);
		
		return getLinkedScreenName(screenId);
	}
	
	/**
	 * Gets the linked screen name by screen id.
	 * @param screenId
	 * @return linked screen name.
	 */
	public static String getLinkedScreenName(int screenId){
		HashMap<String, String> currentScreenProperties = MetrixScreenManager.getScreenProperties(screenId);
		if (currentScreenProperties != null && currentScreenProperties.containsKey("linked_screen_id"))
		{
			String strLinkedScreenId = currentScreenProperties.get("linked_screen_id");
			if(strLinkedScreenId == null)return null;
			int linkedScreenId = Integer.valueOf(strLinkedScreenId);
			return MetrixScreenManager.getScreenName(linkedScreenId);
		}
		return null;
	}
	
	//Tablet UI Optimization
	public static boolean shouldRunTabletSpecificLandUI(Activity activity) {
		if(activity instanceof MetrixBaseActivity) {
			return ((MetrixBaseActivity)activity).shouldRunTabletSpecificUIMode;
		}
		return false;
	}
	//End Tablet UI Optimization
}
