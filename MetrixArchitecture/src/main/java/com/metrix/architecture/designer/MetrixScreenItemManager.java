package com.metrix.architecture.designer;

import java.util.HashMap;

import com.metrix.architecture.database.MetrixDatabaseManager;
import com.metrix.architecture.scripting.ClientScriptDef;
import com.metrix.architecture.scripting.MetrixClientScriptManager;
import com.metrix.architecture.utilities.MetrixPublicCache;
import com.metrix.architecture.utilities.MetrixStringHelper;

import android.app.Activity;
import android.view.View;

public class MetrixScreenItemManager extends MetrixDesignerManager {
	
	/**
	 * For the particular activity and view, discover if there is a script to run.
	 * 
	 * @return The ClientScriptDef for the script to run.
	 * 
	 * @since 5.6.3
	 */
	public static ClientScriptDef getEventForView(Activity activity, View v) {
		String activityName = activity.getClass().getSimpleName();
		int screenId = MetrixScreenManager.getScreenId(activityName);
		
		return getEventForView(v, screenId);
	}
	
	/**
	 * For the particular activity and view, discover if there is a script to run.
	 * 
	 * @return The ClientScriptDef for the script to run.
	 * 
	 * @since 5.6.3
	 */
	@SuppressWarnings("unchecked")
	public static ClientScriptDef getEventForView(View v, int screenId) {
		int controlID = v.getId();
		ClientScriptDef scriptDef = null;
		HashMap<Integer, String> scriptingScreenItemAliasMap = (HashMap<Integer, String>)MetrixPublicCache.instance.getItem("scriptingScreenItemAliasMap");
			
		if (scriptingScreenItemAliasMap != null && scriptingScreenItemAliasMap.containsKey(controlID)) {
			String itemName = scriptingScreenItemAliasMap.get(controlID);
			String scriptID = MetrixDatabaseManager.getFieldStringValue("use_mm_screen_item", "event", String.format("screen_id = %1$s and item_name = '%2$s' and active = 'Y'", String.valueOf(screenId), itemName));
			if (!MetrixStringHelper.isNullOrEmpty(scriptID)) {
				String eventType = MetrixDatabaseManager.getFieldStringValue("use_mm_screen_item", "event_type", String.format("screen_id = %1$s and item_name = '%2$s'", String.valueOf(screenId), itemName));
				scriptDef = MetrixClientScriptManager.getScriptDefForScriptID(scriptID);
				scriptDef.mIsValidation = MetrixStringHelper.valueIsEqual(eventType, "VALIDATION");
			}
		}
		
		return scriptDef;
	}

	public static ClientScriptDef getEventForAlias(String itemName, int screenId) {
		ClientScriptDef scriptDef = null;

		String scriptID = MetrixDatabaseManager.getFieldStringValue("use_mm_screen_item", "event", String.format("screen_id = %1$s and item_name = '%2$s' and active = 'Y'", String.valueOf(screenId), itemName));
		if (!MetrixStringHelper.isNullOrEmpty(scriptID)) {
			String eventType = MetrixDatabaseManager.getFieldStringValue("use_mm_screen_item", "event_type", String.format("screen_id = %1$s and item_name = '%2$s'", String.valueOf(screenId), itemName));
			scriptDef = MetrixClientScriptManager.getScriptDefForScriptID(scriptID);
			scriptDef.mIsValidation = MetrixStringHelper.valueIsEqual(eventType, "VALIDATION");
		}

		return scriptDef;
	}
}
