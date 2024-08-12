package com.metrix.metrixmobile;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import android.app.Activity;
import android.content.Intent;
import android.provider.Settings;

import com.metrix.architecture.assistants.MetrixApplicationAssistant;
import com.metrix.architecture.database.MetrixDatabaseManager;
import com.metrix.architecture.database.MobileApplication;
import com.metrix.architecture.designer.MetrixGlobalMenuManager;
import com.metrix.architecture.designer.MetrixScreenManager;
import com.metrix.architecture.scripting.ClientScriptDef;
import com.metrix.architecture.scripting.MetrixClientScriptManager;
import com.metrix.architecture.slidingmenu.MetrixSlidingMenuItem;
import com.metrix.architecture.superclasses.MetrixBaseActivity;
import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.architecture.utilities.LogManager;
import com.metrix.architecture.utilities.MetrixActivityHelper;
import com.metrix.architecture.utilities.MetrixStringHelper;
import com.metrix.architecture.utilities.SettingsHelper;
import com.metrix.metrixmobile.global.MetrixWorkStatusAssistant;
import com.metrix.metrixmobile.global.MobileGlobal;
import com.metrix.metrixmobile.oidc.AuthStateManager;
import com.metrix.metrixmobile.oidc.LogoutHandler;
import com.metrix.metrixmobile.system.Help;

import net.openid.appauth.AuthState;

public class OptionsMenu {
	public static void onPrepareOptionsMenu(Activity activity, ArrayList<MetrixSlidingMenuItem> slidingMenuItems) {
		Map<Integer, HashMap<String, String>> globalMenuItems = MetrixGlobalMenuManager.getGlobalMenuItems();
		
		if (globalMenuItems != null && globalMenuItems.size() > 0) {
			slidingMenuItems.clear();

			for (int i = 0; i < globalMenuItems.size(); i++) {
				HashMap<String, String> globalMenuItem = globalMenuItems.get(i);
				String itemName = globalMenuItem.get("item_name");
				String label = globalMenuItem.get("label");
				String countScript = globalMenuItem.get("count_script");
				String hideIfZeroString = globalMenuItem.get("hide_if_zero");
				boolean hideIfZero = MetrixStringHelper.valueIsEqual(hideIfZeroString, "Y");
				String iconResourceString = globalMenuItem.get("icon_name");	// this is already translated into a resource string
				int iconResourceID = getIconResourceID(iconResourceString);

				if (itemName.contains("Designer")) {
					boolean addThisItem = false;

					int thisScreenID = -1;
					String activitySimpleName = null;
					if (activity != null) {
						activitySimpleName = activity.getClass().getSimpleName();
						if (activity instanceof MetrixBaseActivity) {
							MetrixBaseActivity metrixBaseActivity = (MetrixBaseActivity)activity;
							if (metrixBaseActivity.isCodelessScreen){
								thisScreenID = metrixBaseActivity.codeLessScreenId;
								activitySimpleName = metrixBaseActivity.codeLessScreenName;
							} else
								thisScreenID = MetrixScreenManager.getScreenId(activitySimpleName);
						} else
							thisScreenID = MetrixScreenManager.getScreenId(activitySimpleName);
					}

					if (itemName.compareToIgnoreCase("Designer") == 0) {
						addThisItem = true;		// always show basic "Designer" in menu, if STUDIO user (managed at MetrixGlobalMenuManager)
						if (MetrixStringHelper.isNullOrEmpty(label))
							label = AndroidResourceHelper.getMessage("Designer");
					} else if (itemName.compareToIgnoreCase("Designer - Screen") == 0) {
						// get activity's simple name and see if there is a parallel use_mm_screen.screen_name - if so, display SCREEN option
						// if activity's simple name is Home, also allow SCREEN option, though it will go elsewhere ultimately
						if (thisScreenID > 0 || MetrixStringHelper.valueIsEqual(activitySimpleName, "Home")) {
							addThisItem = true;
							if (MetrixStringHelper.isNullOrEmpty(label))
								label = AndroidResourceHelper.getMessage("DesignerScreen");
						}
					} else if (thisScreenID > 0 && MetrixScreenManager.screenHasFields(thisScreenID)) {
						// if activity's use_mm_screen.screen_id has matching use_mm_field records, also display FIELD and FIELD ORDER options
						addThisItem = true;
						if (MetrixStringHelper.isNullOrEmpty(label)) {
							if (itemName.compareToIgnoreCase("Designer - Fields") == 0)
								label = AndroidResourceHelper.getMessage("DesignerFields");
							else
								label = AndroidResourceHelper.getMessage("DesignerFieldOrder");
						}
					}

					if (addThisItem) {
						MetrixSlidingMenuItem slidingMenuItem = new MetrixSlidingMenuItem(label, R.drawable.sliding_menu_designer, i);
						slidingMenuItems.add(slidingMenuItem);
					}

					continue;	// we have done full handling of this Designer item by this point
				}

				boolean displayCount = false;
				String countString = "";
				if (!MetrixStringHelper.isNullOrEmpty(countScript)) {
					ClientScriptDef countScriptDef = MetrixClientScriptManager.getScriptDefForScriptID(countScript);
					if (countScriptDef != null) {
						Object countObj = MetrixClientScriptManager.executeScriptReturningObject(new WeakReference<Activity>(activity), countScriptDef);
						if (countObj != null) {
							if (countObj instanceof String)
								countString = String.valueOf(countObj);
							else if (countObj instanceof Double) {
								Double countDbl = (Double) countObj;
								countString = String.valueOf(countDbl.intValue());
							}

							if (hideIfZero && (MetrixStringHelper.isNullOrEmpty(countString) || MetrixStringHelper.valueIsEqual(countString, "0"))) {
								continue;	// skip rendering this item, as it should be hidden
							} else if (!MetrixStringHelper.isNullOrEmpty(countString) && !MetrixStringHelper.valueIsEqual(countString, "0")) {
								displayCount = true;
							}

							// If we get here, and this is the Query menu item, make sure we don't display the count text
							if (itemName.compareToIgnoreCase("Query") == 0)
								displayCount = false;
						}
					}
				}

				MetrixSlidingMenuItem slidingMenuItem = null;
				if (displayCount)
					slidingMenuItem = new MetrixSlidingMenuItem(label, countString, iconResourceID, i);
				else
					slidingMenuItem = new MetrixSlidingMenuItem(label, iconResourceID, i);
				slidingMenuItems.add(slidingMenuItem);
			}
		}
	}
	
	public static void onOptionsItemSelected(Activity activity, MetrixSlidingMenuItem slidingMenuItem, String helpText, Boolean handlingErrors) {
		Map<Integer, HashMap<String, String>> menuItems = MetrixGlobalMenuManager.getGlobalMenuItems();
		
		int index = slidingMenuItem.getIndex();
		HashMap<String, String> globalMenuItem = menuItems.get(index);
		String itemName = globalMenuItem.get("item_name");
		String screenIdString = globalMenuItem.get("screen_id");
		String tapEvent = globalMenuItem.get("tap_event");

		if (MetrixStringHelper.valueIsEqual(itemName, "Close App")) {
			LogoutHandler logoutHandler = new LogoutHandler(activity);
			boolean requireLogin = SettingsHelper.getManualLogin(MobileApplication.getAppContext());

			if(requireLogin)
				logoutHandler.logout();
			else {
				activity.finishAffinity();
			}
		} else if (MetrixStringHelper.valueIsEqual(itemName, "Work Status")) {
			MetrixWorkStatusAssistant statusAssistant = new MetrixWorkStatusAssistant();
			statusAssistant.displayStatusDialog(activity, activity, false);
		} else if (MetrixStringHelper.valueIsEqual(itemName, "Help")) {
			Intent intent = MetrixActivityHelper.createActivityIntent(activity, Help.class);
			String message = null;
			if (!MetrixStringHelper.isNullOrEmpty(helpText))
				message = helpText;
			else
				message = AndroidResourceHelper.getMessage("NoHelpDetailsAvailable");

			if (handlingErrors != null)
				if (handlingErrors.booleanValue())
					message = message + "\r\n \r\n" + AndroidResourceHelper.getMessage("ErrorMess1Arg", MobileGlobal.mErrorInfo.errorMessage);

			if (activity != null) {
				String screenName = activity.getClass().getSimpleName();
				if (activity instanceof MetrixBaseActivity) {
					MetrixBaseActivity metrixBaseActivity = (MetrixBaseActivity)activity;
					if (metrixBaseActivity.isCodelessScreen) {
						screenName = metrixBaseActivity.codeLessScreenName;
						if (metrixBaseActivity.showListScreenLinkedScreenInTabletUIMode)
							screenName = MetrixScreenManager.getLinkedScreenName(metrixBaseActivity.codeLessScreenId);
					}
				}
				message = message + "\r\n \r\n" + AndroidResourceHelper.getMessage("ScreenColon1Arg", screenName);
			}

			intent.putExtra("help_text", message);
			MetrixActivityHelper.startNewActivity(activity, intent);
		} else if (itemName.contains("Designer")) {
			Intent intent = MetrixActivityHelper.createActivityIntent(activity, "com.metrix.architecture.designer", "MetrixDesignerDesignActivity");
			if (!MetrixStringHelper.valueIsEqual(itemName, "Designer")) {
				String activityName = null;
				int intScreenID = -1;
				if (activity != null) {
					activityName = activity.getClass().getSimpleName();
					if (activity instanceof MetrixBaseActivity) {
						MetrixBaseActivity metrixBaseActivity = (MetrixBaseActivity)activity;
						if (metrixBaseActivity.isCodelessScreen) {
							activityName = metrixBaseActivity.codeLessScreenName;
							intScreenID = metrixBaseActivity.codeLessScreenId;
						} else
							intScreenID = MetrixScreenManager.getScreenId(activityName);
					}
					else
						intScreenID = MetrixScreenManager.getScreenId(activityName);
				}

				if (MetrixStringHelper.valueIsEqual(itemName, "Designer - Screen") && MetrixStringHelper.valueIsEqual(activityName, "Home")) {
					String homeDesignID = MetrixDatabaseManager.getFieldStringValue("use_mm_home_item", "design_id", "");
					String homeRevisionID = MetrixDatabaseManager.getFieldStringValue("use_mm_home_item", "revision_id", "");
					String homeDesignSetID = MetrixDatabaseManager.getFieldStringValue("use_mm_design", "design_set_id", "design_id = " + homeDesignID);
					intent.putExtra("targetDesignerActivity", "MetrixDesignerHomeMenuEnablingActivity");
					intent.putExtra("targetDesignerDesignID", homeDesignID);
					intent.putExtra("targetDesignerRevisionID", homeRevisionID);
					intent.putExtra("targetDesignerDesignSetID", homeDesignSetID);
					MetrixActivityHelper.startNewActivity(activity, intent);
					return;
				}

				String screenID = String.valueOf(intScreenID);
				String designID = MetrixDatabaseManager.getFieldStringValue("use_mm_screen", "design_id", "screen_id = " + screenID);
				String revisionID = MetrixDatabaseManager.getFieldStringValue("use_mm_screen", "revision_id", "screen_id = " + screenID);
				String designSetID = MetrixDatabaseManager.getFieldStringValue("use_mm_design", "design_set_id", "design_id = " + designID);

				if (MetrixStringHelper.valueIsEqual(itemName, "Designer - Screen")) {
					intent.putExtra("targetDesignerActivity", "MetrixDesignerScreenPropActivity");
				} else if (MetrixStringHelper.valueIsEqual(itemName, "Designer - Fields")) {
					intent.putExtra("targetDesignerActivity", "MetrixDesignerFieldActivity");
				} else if (MetrixStringHelper.valueIsEqual(itemName, "Designer - Field Order")) {
					intent.putExtra("targetDesignerActivity", "MetrixDesignerFieldOrderActivity");
				}

				intent.putExtra("targetDesignerDesignID", designID);
				intent.putExtra("targetDesignerRevisionID", revisionID);
				intent.putExtra("targetDesignerDesignSetID", designSetID);
				intent.putExtra("targetDesignerScreenID", screenID);
				intent.putExtra("targetDesignerScreenName", activityName);
			}

			MetrixActivityHelper.startNewActivity(activity, intent);
		} else if (!MetrixStringHelper.isNullOrEmpty(screenIdString)) {
			int screenId = Integer.valueOf(screenIdString);
			String screenName = MetrixScreenManager.getScreenName(screenId);
			String currentScreenName = MetrixScreenManager.getScreenName(activity);
			if (MetrixApplicationAssistant.screenNameHasClassInCode(screenName)) {
				Intent intent = MetrixActivityHelper.createActivityIntent(activity, screenName);
				if (MetrixStringHelper.valueIsEqual(currentScreenName, screenName))
					MetrixActivityHelper.startNewActivityAndFinish(activity, intent);
				else
					MetrixActivityHelper.startNewActivity(activity, intent);
			} else {
				// Right now, we only support codeless tab parents and codeless non-workflow screens.
				String screenType = MetrixScreenManager.getScreenType(screenId);
				if (MetrixStringHelper.valueIsEqual(screenType, "TAB_PARENT")) {
					Intent intent = MetrixActivityHelper.createActivityIntent(activity, "com.metrix.metrixmobile.system", "MetadataTabActivity");
					intent.putExtra("ScreenID", screenId);
					MetrixActivityHelper.startNewActivity(activity, intent);
				} else if (MetrixStringHelper.valueIsEqual(screenType, "STANDARD")) {
					Intent intent = MetrixActivityHelper.createActivityIntent(activity, "com.metrix.metrixmobile.system", "MetadataStandardActivity");
					intent.putExtra("ScreenID", screenId);
					MetrixActivityHelper.startNewActivity(activity, intent);
				} else if (screenType.toLowerCase().contains("list")) {
					Intent intent = MetrixActivityHelper.createActivityIntent(activity, "com.metrix.metrixmobile.system", "MetadataListActivity");
					intent.putExtra("ScreenID", screenId);
					MetrixActivityHelper.startNewActivity(activity, intent);
				} else
					LogManager.getInstance().error(String.format("The %1$s screen (screen_id=%2$s) cannot be used as a codeless screen from the Global menu.", screenName, screenIdString));
			}
		} else if (!MetrixStringHelper.isNullOrEmpty(tapEvent)) {
			ClientScriptDef tapEventScriptDef = MetrixClientScriptManager.getScriptDefForScriptID(tapEvent);
			if (tapEventScriptDef != null)
				MetrixClientScriptManager.executeScript(new WeakReference<Activity>(activity), tapEventScriptDef);
		} else {
			LogManager.getInstance().error(String.format("Cannot handle click event for Global menu item named %s.", itemName));
		}
	}

	private static int getIconResourceID(String iconResourceString) {
		if (MetrixStringHelper.isNullOrEmpty(iconResourceString))
			return R.drawable.sliding_menu_empty;

		if (MetrixStringHelper.valueIsEqual(iconResourceString, "R.drawable.sliding_menu_about"))
			return R.drawable.sliding_menu_about;
		if (MetrixStringHelper.valueIsEqual(iconResourceString, "R.drawable.sliding_menu_calendar"))
			return R.drawable.sliding_menu_calendar;
		if (MetrixStringHelper.valueIsEqual(iconResourceString, "R.drawable.sliding_menu_categories"))
			return R.drawable.sliding_menu_categories;
		if (MetrixStringHelper.valueIsEqual(iconResourceString, "R.drawable.sliding_menu_customers"))
			return R.drawable.sliding_menu_customers;
		if (MetrixStringHelper.valueIsEqual(iconResourceString, "R.drawable.sliding_menu_designer"))
			return R.drawable.sliding_menu_designer;
		if (MetrixStringHelper.valueIsEqual(iconResourceString, "R.drawable.sliding_menu_escalations"))
			return R.drawable.sliding_menu_escalations;
		if (MetrixStringHelper.valueIsEqual(iconResourceString, "R.drawable.sliding_menu_home"))
			return R.drawable.sliding_menu_home;
		if (MetrixStringHelper.valueIsEqual(iconResourceString, "R.drawable.sliding_menu_jobs"))
			return R.drawable.sliding_menu_jobs;
		if (MetrixStringHelper.valueIsEqual(iconResourceString, "R.drawable.sliding_menu_quotes"))
			return R.drawable.sliding_menu_quotes;
		if (MetrixStringHelper.valueIsEqual(iconResourceString, "R.drawable.sliding_menu_preview"))
			return R.drawable.sliding_menu_preview;
		if (MetrixStringHelper.valueIsEqual(iconResourceString, "R.drawable.sliding_menu_profile"))
			return R.drawable.sliding_menu_profile;
		if (MetrixStringHelper.valueIsEqual(iconResourceString, "R.drawable.sliding_menu_query"))
			return R.drawable.sliding_menu_query;
		if (MetrixStringHelper.valueIsEqual(iconResourceString, "R.drawable.sliding_menu_receiving"))
			return R.drawable.sliding_menu_receiving;
		if (MetrixStringHelper.valueIsEqual(iconResourceString, "R.drawable.sliding_menu_scan_stock"))
			return R.drawable.sliding_menu_scan_stock;
		if (MetrixStringHelper.valueIsEqual(iconResourceString, "R.drawable.sliding_menu_settings"))
			return R.drawable.sliding_menu_settings;
		if (MetrixStringHelper.valueIsEqual(iconResourceString, "R.drawable.sliding_menu_shift"))
			return R.drawable.sliding_menu_shift;
		if (MetrixStringHelper.valueIsEqual(iconResourceString, "R.drawable.sliding_menu_close_app"))
			return R.drawable.sliding_menu_close_app;
		if (MetrixStringHelper.valueIsEqual(iconResourceString, "R.drawable.sliding_menu_skins"))
			return R.drawable.sliding_menu_skins;
		if (MetrixStringHelper.valueIsEqual(iconResourceString, "R.drawable.sliding_menu_stock"))
			return R.drawable.sliding_menu_stock;
		if (MetrixStringHelper.valueIsEqual(iconResourceString, "R.drawable.sliding_menu_sync"))
			return R.drawable.sliding_menu_sync;
		if (MetrixStringHelper.valueIsEqual(iconResourceString, "R.drawable.sliding_menu_team"))
			return R.drawable.sliding_menu_team;

		return R.drawable.sliding_menu_empty;
	}
}
