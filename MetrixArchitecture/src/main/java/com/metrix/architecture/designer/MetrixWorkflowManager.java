package com.metrix.architecture.designer;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Parcelable;

import com.metrix.architecture.assistants.MetrixApplicationAssistant;
import com.metrix.architecture.attachment.AttachmentWidgetManager;
import com.metrix.architecture.attachment.FSMAttachmentList;
import com.metrix.architecture.database.MetrixCursor;
import com.metrix.architecture.database.MetrixDatabaseManager;
import com.metrix.architecture.database.MobileApplication;
import com.metrix.architecture.scripting.ClientScriptDef;
import com.metrix.architecture.scripting.MetrixClientScriptManager;
import com.metrix.architecture.superclasses.MetrixBaseActivity;
import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.architecture.utilities.LogManager;
import com.metrix.architecture.utilities.MetrixActivityHelper;
import com.metrix.architecture.utilities.MetrixCurrentKeysHelper;
import com.metrix.architecture.utilities.MetrixPublicCache;
import com.metrix.architecture.utilities.MetrixStringHelper;
import com.metrix.architecture.utilities.MetrixUIHelper;
import com.metrix.architecture.utilities.SettingsHelper;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * This class contains methods which can be used to apply the meta data defined in the
 * mobile designer related to workflow level properties.
 * 
 * @since 5.6.1
 */
public class MetrixWorkflowManager extends MetrixDesignerManager {	
	public static final String DEBRIEF_WORKFLOW = "Debrief";
	public static final String SCHEDULE_WORKFLOW = "Schedule";
	public static final String QUOTE_WORKFLOW = "Quote";

	private static Map<String, String> workflows = new LinkedHashMap<String, String>();
	private static Map<String, List<WorkflowScreen>> workflowScreens = new LinkedHashMap<String, List<WorkflowScreen>>();
	private static Map<String, List<WorkflowJumpToMenuItem>> workflowJumpToMenus = new LinkedHashMap<String, List<WorkflowJumpToMenuItem>>();
	private static String currentWorkflowName = "";
	private static Map<String, List<Hashtable<String, String>>> debriefNavigationList = new HashMap<String, List<Hashtable<String, String>>>();
	
	public static void clearAllWorkflowCaches() {
		currentWorkflowName = "";
		SettingsHelper.saveStringSetting(MobileApplication.getAppContext(), "CURRENT_WORKFLOW_NAME", "", true);

		workflows.clear();
		workflowScreens.clear();
		workflowJumpToMenus.clear();
		debriefNavigationList.clear();
	}
	
	/***
	 * Advances to the next screen in a workflow.
	 * 
	 * @param activity the current activity being displayed.
	 * @since 5.6.1
	 */
	public static void advanceWorkflow(Activity activity) {
		MetrixWorkflowManager.advanceWorkflow(getCurrentWorkflowName(activity.getApplicationContext()), activity);
	}
	
	/***
	 * Advances to the next screen in a workflow.
	 * 
	 * @param workflowName the name of the current workflow.
	 * @param activity the current activity being displayed.
	 * @since 5.6.1
	 */
	public static void advanceWorkflow(String workflowName, Activity activity) {
		if (MetrixStringHelper.isNullOrEmpty(workflowName)) {
			throw new IllegalArgumentException(AndroidResourceHelper.getMessage("TheWorkflowNameParamReq"));
		}

		if (activity == null) {
			throw new IllegalArgumentException(AndroidResourceHelper.getMessage("TheActivityParamIsReq"));
		}

		setCurrentWorkflowName(activity.getApplicationContext(), workflowName);		
		MetrixWorkflowManager.advanceWorkflow(workflowName, activity, null, null);
	}

	/***
	 * Advances to the next screen in a workflow.
	 * 
	 * @param activity the current activity being displayed.
	 * @param extraKey a key for a parcelable parameter to be included in the intent.
	 * @param extraValue the value of a parcelable parameter to be included in the intent.
	 * @since 5.6.1
	 */
	public static void advanceWorkflow(Activity activity, String extraKey, Parcelable extraValue) {
		MetrixWorkflowManager.advanceWorkflow(getCurrentWorkflowName(activity.getApplicationContext()), activity, extraKey, extraValue);
	}
	
	/***
	 * Advances to the next screen in a workflow.
	 * 
	 * @param workflowName the name of the current workflow.
	 * @param activity the current activity being displayed.
	 * @param extraKey a key for a parcelable parameter to be included in the intent.
	 * @param extraValue the value of a parcelable parameter to be included in the intent.
	 * @since 5.6.1
	 */
	@SuppressLint("DefaultLocale") 
	public static void advanceWorkflow(String workflowName, Activity activity, String extraKey, Parcelable extraValue) {
		if (MetrixStringHelper.isNullOrEmpty(workflowName)) {
			throw new IllegalArgumentException(AndroidResourceHelper.getMessage("TheWorkflowNameParamReq"));
		}

		if (activity == null) {
			throw new IllegalArgumentException(AndroidResourceHelper.getMessage("TheActivityParamIsReq"));
		}
		
		try {
			String workflowId = getWorkflowId(workflowName);
			if (MetrixStringHelper.isNullOrEmpty(workflowId)) {
				if (workflowName.toLowerCase().contains("debrief~")) {
					workflowId = MetrixWorkflowManager.getWorkflowId(MetrixWorkflowManager.DEBRIEF_WORKFLOW);
					workflowName = MetrixWorkflowManager.DEBRIEF_WORKFLOW;
				} else if (workflowName.toLowerCase().contains("schedule~")) {
					workflowId = MetrixWorkflowManager.getWorkflowId(MetrixWorkflowManager.SCHEDULE_WORKFLOW);
					workflowName = MetrixWorkflowManager.SCHEDULE_WORKFLOW;
				} else if (workflowName.toLowerCase().contains("quote~")) {
					workflowId = MetrixWorkflowManager.getWorkflowId(MetrixWorkflowManager.QUOTE_WORKFLOW);
					workflowName = MetrixWorkflowManager.QUOTE_WORKFLOW;
				}
			}
			
			setCurrentWorkflowName(activity.getApplicationContext(), workflowName);
			
			if (MetrixStringHelper.isNullOrEmpty(workflowId)) {
				return;
			}
			
			String surveyGroupId = "";
			String nextActivityName = "";
			
			if (activity instanceof MetrixBaseActivity) {
				MetrixBaseActivity metrixBaseActivity = (MetrixBaseActivity)activity;
				String currentActivityname = ""; 
				if (metrixBaseActivity.isCodelessScreen)
					currentActivityname = metrixBaseActivity.codeLessScreenName; 
				else {
					currentActivityname = activity.getClass().getSimpleName();

					if(activity instanceof  FSMAttachmentList) {
						// it is in workflow, should use workflowScreenName
						currentActivityname = ((FSMAttachmentList)activity).workflowScreenName;
					}
				}
				
				nextActivityName = getNextScreen(workflowId, activity, currentActivityname);
				
				//The activity/class exists within the code-base, so that we can advance to the next screen.
				if (MetrixApplicationAssistant.screenNameHasClassInCode(nextActivityName)) {
					if (nextActivityName.toLowerCase().contains("debriefsurvey1")) {
						nextActivityName = "DebriefSurvey";
						surveyGroupId = "1";
					} else if (nextActivityName.toLowerCase().contains("debriefsurvey2")) {
						nextActivityName = "DebriefSurvey";
						surveyGroupId = "2";
					} else if (nextActivityName.toLowerCase().contains("debriefsurvey3")) {
						nextActivityName = "DebriefSurvey";
						surveyGroupId = "3";
					} else if (nextActivityName.toLowerCase().contains("debriefsurvey4")) {
						nextActivityName = "DebriefSurvey";
						surveyGroupId = "4";
					} else if (nextActivityName.toLowerCase().contains("debriefsurvey5")) {
						nextActivityName = "DebriefSurvey";
						surveyGroupId = "5";
					}
					
					if (MetrixStringHelper.isNullOrEmpty(nextActivityName)) {
						return;
					}
					
					Intent intent = MetrixActivityHelper.createActivityIntent(activity, nextActivityName);
					if (!MetrixStringHelper.isNullOrEmpty(extraKey)) {
						intent.putExtra(extraKey, extraValue);
					}
					
					if (surveyGroupId.length() > 0) {
						intent.putExtra("surveyGroupId", surveyGroupId);
						intent.putExtra("isFromMenu", "N");
					}

					if(nextActivityName.equalsIgnoreCase("FSMAttachmentList")) {
						intent.putExtra("fromWorkFlow", workflowName);
						String transactionTable = "";
						String transactionId = "";
						if (workflowName.toLowerCase().contains("debrief~")) {
							workflowName = MetrixWorkflowManager.DEBRIEF_WORKFLOW;
							transactionTable = "task";
							transactionId = "task_id";
						} else if (workflowName.toLowerCase().contains("quote~")) {
							workflowName = MetrixWorkflowManager.QUOTE_WORKFLOW;
							transactionTable = "quote";
							transactionId = "quote_id";
						}
						WeakReference<Activity> currentActivity = new WeakReference<Activity>(activity);
						AttachmentWidgetManager.openFromScript(currentActivity, "BaseAttachmentList", "BaseAttachmentCard", transactionTable, transactionId, workflowName);
						return;
					}
			//		The code below will clear activity stack once if go through joblist. 
			//		String currentActivityName = activity.getClass().getSimpleName();
			//		if(currentActivityName.toLowerCase().contains("joblist")){
			//			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			//		}
							
					MetrixActivityHelper.startNewActivity(activity, intent);
				}
				else {
					int screenId = MetrixScreenManager.getScreenId(nextActivityName);
					if (screenId > -1) {
						String screenType = MetrixScreenManager.getScreenType(screenId);
						if (!MetrixStringHelper.isNullOrEmpty(screenType)) {
							if (screenType.toLowerCase().contains("standard")) {
								if (!MetrixStringHelper.isNullOrEmpty(workflowName)) {
									Intent intent = null;
									if (workflowName.toLowerCase().contains("debrief")) {
										intent = MetrixActivityHelper.createActivityIntent(activity, "com.metrix.metrixmobile.system", "MetadataDebriefActivity");
										intent.putExtra("ScreenID", screenId);
										MetrixActivityHelper.startNewActivity(activity, intent);
									} else if (workflowName.toLowerCase().contains("schedule")) {
										intent = MetrixActivityHelper.createActivityIntent(activity, "com.metrix.metrixmobile.system", "MetadataScheduleActivity");
										intent.putExtra("ScreenID", screenId);
										MetrixActivityHelper.startNewActivity(activity, intent);
									} else if (workflowName.toLowerCase().contains("quote")) {
										intent = MetrixActivityHelper.createActivityIntent(activity, "com.metrix.metrixmobile.system", "MetadataQuoteActivity");
										intent.putExtra("ScreenID", screenId);
										MetrixActivityHelper.startNewActivity(activity, intent);
									} else
										LogManager.getInstance().error(String.format("Current workflow(%s) isn't valid.", workflowName));
								} else
									LogManager.getInstance().error(String.format("Current workflow(%s) doesn't exist.", workflowName));
							} else if(screenType.toLowerCase().contains("list")) {
								if(screenType.equalsIgnoreCase("ATTACHMENT_API_LIST")) {
									AttachmentWidgetManager.openFromWorkFlow(activity, workflowName);
									return;
								}
								if (!MetrixStringHelper.isNullOrEmpty(workflowName)) {
									Intent intent = null;
									if (workflowName.toLowerCase().contains("debrief")) {
										intent = MetrixActivityHelper.createActivityIntent(activity, "com.metrix.metrixmobile.system", "MetadataListDebriefActivity");
										intent.putExtra("ScreenID", screenId);
										MetrixActivityHelper.startNewActivity(activity, intent);
									} else if(workflowName.toLowerCase().contains("schedule")) {
										intent = MetrixActivityHelper.createActivityIntent(activity, "com.metrix.metrixmobile.system", "MetadataListScheduleActivity");
										intent.putExtra("ScreenID", screenId);
										MetrixActivityHelper.startNewActivity(activity, intent);
									} else if(workflowName.toLowerCase().contains("quote")) {
										intent = MetrixActivityHelper.createActivityIntent(activity, "com.metrix.metrixmobile.system", "MetadataListQuoteActivity");
										intent.putExtra("ScreenID", screenId);
										MetrixActivityHelper.startNewActivity(activity, intent);
									} else
										LogManager.getInstance().error(String.format("Current workflow(%s) isn't valid.", workflowName));
								} else
									LogManager.getInstance().error(String.format("Current workflow(%s) doesn't exist.", workflowName));
							} else
								LogManager.getInstance().error(String.format("Screen type(%s) doesn't support.", screenType));
						}
					} else {
						LogManager.getInstance().error(String.format("Can't find a correct codeless screen(screen_id=%s) - no meta-data information is available.", screenId));
	                    throw new Exception(String.format("Can't find a correct codeless screen(screen_id=%s) - no meta-data information is available.", screenId));
					}
				}
			}
		} catch(Exception ex) {
			LogManager.getInstance().error(ex);
		}
	}
	
	/***
	 * Gets the id of a workflow based on it's name.
	 * 
	 * @param workflowName the workflow's name.
	 * @return the workflow's id.
	 * @since 5.6.1
	 */
	public static String getWorkflowId(String workflowName) {
		if (workflows.size() == 0) {
			String query = "SELECT use_mm_workflow.name, use_mm_workflow.workflow_id FROM use_mm_workflow";
			
			MetrixCursor cursor = null;
			try {
				cursor = MetrixDatabaseManager.rawQueryMC(query, null);
				
				if (cursor == null || !cursor.moveToFirst()) {
					return "";
				}
	
				while (cursor.isAfterLast() == false) {
					workflows.put(cursor.getString(0), cursor.getString(1));
					cursor.moveToNext();
				}
			} finally {
				if (cursor != null) {
					cursor.close();
				}
			}
		}
		
		return workflows.get(workflowName);
	}
	
	public static boolean IsWorkflowExist(String workflowName) {
		String workflowId = MetrixWorkflowManager.getWorkflowId(workflowName);
		
		if(MetrixStringHelper.isNullOrEmpty(workflowId))
			return false;
		else
			return true;
	}
	
	private static String getNextScreen(String workflowId, Activity activity, String currentScreenName) {
		if (workflowScreens.containsKey(workflowId)) {
			List<WorkflowScreen> screens = workflowScreens.get(workflowId);
			if (screens == null || screens.size() == 0) {
				workflowScreens.remove(workflowId);
				if (!cacheScreensForWorkflow(workflowId)) {
					return "";
				}
			}
		} else {
			if (!cacheScreensForWorkflow(workflowId)) {
				return "";
			}
		}
	
		if (currentScreenName.equals("DebriefSurvey")) {
			String groupId = activity.getIntent().getStringExtra("surveyGroupId");
			currentScreenName = currentScreenName + groupId;
		}

		List<WorkflowScreen> screens = workflowScreens.get(workflowId);
		int position = 0;
		int currPosition = -1;
		for (WorkflowScreen screen : screens) {
			if (screen.ScreenName.compareToIgnoreCase(currentScreenName) == 0) {
				currPosition = position;
				break;
			}
			position++;
		}

		if (currPosition > -1) {
			position = currPosition + 1;
			while (position < screens.size()) {
				WorkflowScreen nextScreenCandidate = screens.get(position);
				if (MetrixStringHelper.isNullOrEmpty(nextScreenCandidate.ScriptID))
					return nextScreenCandidate.ScreenName;
				else {
					ClientScriptDef workflowScript = MetrixClientScriptManager.getScriptDefForScriptID(nextScreenCandidate.ScriptID);
					if (workflowScript != null) {
						boolean shouldIncludeInWorkflow = MetrixClientScriptManager.executeScript(new WeakReference<Activity>(activity), workflowScript);
						if (shouldIncludeInWorkflow)
							return nextScreenCandidate.ScreenName;
					} else
						return nextScreenCandidate.ScreenName;
				}

				position++;
			}
		}

		return screens.get(0).ScreenName;
	}

	public static boolean cacheScreensForWorkflow(String workflowId) {
		String query = "SELECT use_mm_screen.screen_name, use_mm_screen.workflow_script, use_mm_workflow_screen.screen_id " +
				"FROM use_mm_workflow_screen " +
				" inner join use_mm_screen on use_mm_workflow_screen.screen_id = use_mm_screen.screen_id " +
				"WHERE use_mm_workflow_screen.workflow_id = " + workflowId + " " +
				"AND use_mm_workflow_screen.step_order > 0 " +
				"ORDER BY use_mm_workflow_screen.step_order ASC";

		MetrixCursor cursor = null;
		try {
			cursor = MetrixDatabaseManager.rawQueryMC(query, null);
			
			if (cursor == null || !cursor.moveToFirst()) {
				return false;
			}

			List<WorkflowScreen> screens = new ArrayList<WorkflowScreen>();
			while (cursor.isAfterLast() == false) {
				WorkflowScreen singleScreen = new WorkflowScreen();
				singleScreen.ScreenName = cursor.getString(0);
				singleScreen.ScriptID = cursor.getString(1);
				screens.add(singleScreen);
				cursor.moveToNext();
			}

			workflowScreens.put(workflowId, screens);
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
		return true;
	}
	
	/***
	 * Gets all of the items, in order, for the jump to menu associated to
	 * a workflow.
	 * 
	 * @param workflowName the name of the workflow.
	 * @return a LinkedHashMap containing all of the items for the menu.
	 * @since 5.6.1
	 */
	public static Map<String, String> getJumpToMenuForWorkflow(Activity activity, String workflowName) {
		if (MetrixStringHelper.isNullOrEmpty(workflowName)) {
			throw new IllegalArgumentException(AndroidResourceHelper.getMessage("TheWorkflowNameParamReq"));
		}

		String workflowId = MetrixWorkflowManager.getWorkflowId(workflowName);

		if (workflowJumpToMenus.containsKey(workflowId)) {
			List<WorkflowJumpToMenuItem> screens = workflowJumpToMenus.get(workflowId);
			if (screens == null || screens.size() == 0) {
				workflowJumpToMenus.remove(workflowId);
				if (!cacheScreensForWorkflowJumpToMenus(workflowId)) {
					return null;
				}
			}
		}
		else {
			if (!cacheScreensForWorkflowJumpToMenus(workflowId)) {
				return null;
			}
		}

		List<WorkflowJumpToMenuItem> candidateScreens = workflowJumpToMenus.get(workflowId);
		Map<String, String> filteredMenu = new LinkedHashMap<String, String>();
		for (WorkflowJumpToMenuItem entry : candidateScreens) {
			if (!MetrixStringHelper.isNullOrEmpty(entry.ScriptID)) {
				ClientScriptDef contextMenuScript = MetrixClientScriptManager.getScriptDefForScriptID(entry.ScriptID);
				if (contextMenuScript != null) {
					boolean shouldIncludeInContextMenu = MetrixClientScriptManager.executeScript(new WeakReference<Activity>(activity), contextMenuScript);
					if (!shouldIncludeInContextMenu)
						continue;
				}
			}

			filteredMenu.put(entry.ScreenName, entry.Label);
		}

		return filteredMenu;
	}

	public static boolean cacheScreensForWorkflowJumpToMenus(String workflowId) {
		String query = "SELECT use_mm_screen.screen_name, use_mm_screen.label, use_mm_screen.context_menu_script, use_mm_workflow_screen.screen_id " +
				   "FROM use_mm_workflow_screen " +
				   " inner join use_mm_screen on use_mm_workflow_screen.screen_id = use_mm_screen.screen_id " +
				   "WHERE use_mm_workflow_screen.workflow_id = " + workflowId + " " + 
				        "AND use_mm_workflow_screen.jump_order > 0 " + 
				   "ORDER BY use_mm_workflow_screen.jump_order ASC";
		
		MetrixCursor nameCursor = null;
		try {
			nameCursor = MetrixDatabaseManager.rawQueryMC(query, null);
			
			if (nameCursor == null || !nameCursor.moveToFirst()) {
				return false;
			}

			List<WorkflowJumpToMenuItem> menuItems = new ArrayList<WorkflowJumpToMenuItem>();
			while (nameCursor.isAfterLast() == false) {
				WorkflowJumpToMenuItem menuItem = new WorkflowJumpToMenuItem();
				menuItem.ScreenName = nameCursor.getString(0);
				menuItem.Label = nameCursor.getString(1);
				menuItem.ScriptID = nameCursor.getString(2);
				menuItems.add(menuItem);
				nameCursor.moveToNext();
			}
			
			workflowJumpToMenus.put(workflowId, menuItems);
		} finally {
			if (nameCursor != null) {
				nameCursor.close();
			}
		}

		return true;
	}

	public static String getDebriefWorkflowNameForTaskType(String taskType) {
		String workflowToUse = DEBRIEF_WORKFLOW;
		if (!MetrixStringHelper.isNullOrEmpty(taskType)) {
			String trialWorkflow = String.format("Debrief~%s", taskType);
			int rowCount = MetrixDatabaseManager.getCount("use_mm_workflow", String.format("name = '%s'", trialWorkflow));
			if (rowCount > 0)
				workflowToUse = trialWorkflow;
		}
		return workflowToUse;
	}

	public static String getCurrentWorkflowName(Context context) {
		// DEFAULT: assume it's DEBRIEF_WORKFLOW (should be most of the time)
		String workflowToUse = DEBRIEF_WORKFLOW;	
		
		// however, let's do try to assign it intelligently...
		// first, try currentWorkflowName
		if (!MetrixStringHelper.isNullOrEmpty(currentWorkflowName)) {
			workflowToUse = currentWorkflowName;
		} else {
			// if that didn't work, try SettingsHelper value
			String settingsWorkflow = SettingsHelper.getStringSetting(context, "CURRENT_WORKFLOW_NAME");
			if (!MetrixStringHelper.isNullOrEmpty(settingsWorkflow)) {
				workflowToUse = settingsWorkflow;
			}
		}		
		
		return workflowToUse;
	}

	public static void setCurrentWorkflowName(Context context, String workflowToUse) {
		// update both currentWorkflowName and SettingsHelper with passed-in value
		currentWorkflowName = workflowToUse;
		SettingsHelper.saveStringSetting(context, "CURRENT_WORKFLOW_NAME", workflowToUse, false);
	}

	/**
	 * Returns whether this screen exists as a workflow screen.  It will not take mm_screen.workflow_script into account.
	 * @param context
	 * @param screenId
	 * @return
	 */
	public static boolean isScreenExistsInCurrentWorkflow(Context context, int screenId) {
        boolean status = false;
        String theWorkflowName = getCurrentWorkflowName(context);
        if (!MetrixStringHelper.isNullOrEmpty(theWorkflowName)) {
            String theWorkflowId = getWorkflowId(theWorkflowName);
            if (!MetrixStringHelper.isNullOrEmpty(theWorkflowId)) {
                if (workflowScreens.containsKey(theWorkflowId)) {
                    String screenName = MetrixScreenManager.getScreenName(screenId);
                    List<WorkflowScreen> screensList = workflowScreens.get(theWorkflowId);
                    if (screenListContainsScreenName(screensList, screenName))
                        status = true;
                } else {
                    long count = MetrixDatabaseManager.getCount("use_mm_workflow_screen", String.format("use_mm_workflow_screen.screen_id=%d AND use_mm_workflow_screen.workflow_id=%s AND use_mm_workflow_screen.step_order > 0", screenId, theWorkflowId));
                    if (count > 0)
                        status = true;
                }
            }
        }
        return status;
    }

	/**
	 * Returns whether this screen exists as a workflow screen.  It will not take mm_screen.workflow_script into account.
	 * @param context
	 * @param screenName
	 * @return
	 */
	public static boolean isScreenNameExistsInCurrentWorkflow(Context context, String screenName) {
		boolean status = false;
		String theWorkflowName = getCurrentWorkflowName(context);
		if (!MetrixStringHelper.isNullOrEmpty(theWorkflowName)) {
			String theWorkflowId = getWorkflowId(theWorkflowName);
			if (!MetrixStringHelper.isNullOrEmpty(theWorkflowId)) {
				if (workflowScreens.containsKey(theWorkflowId)) {
					List<WorkflowScreen> screensList = workflowScreens.get(theWorkflowId);
					if (screenListContainsScreenName(screensList, screenName))
						status = true;
				} else {
					long count = MetrixDatabaseManager.getCount(new String []{"use_mm_screen", "use_mm_workflow_screen"}, String.format("use_mm_workflow_screen.screen_id=use_mm_screen.screen_id and use_mm_screen.screen_name=%s AND use_mm_workflow_screen.workflow_id=%s AND use_mm_workflow_screen.step_order > 0", screenName, theWorkflowId));
					if (count > 0)
						status = true;
				}
			}
		}
		return status;
	}

	/**
	 * Returns whether this screen exists as a workflow screen, even if step order is -1.  It will not take mm_screen.workflow_script into account.
	 * @param context
	 * @param screenId
	 * @return
	 */
	public static boolean isScreenInWorkflowScreenForCurrentWorkflow(Context context, int screenId) {
		boolean status = false;
		String theWorkflowName = getCurrentWorkflowName(context);
		if (!MetrixStringHelper.isNullOrEmpty(theWorkflowName)) {
			String theWorkflowId = getWorkflowId(theWorkflowName);
			if (!MetrixStringHelper.isNullOrEmpty(theWorkflowId)) {
				long count = MetrixDatabaseManager.getCount("use_mm_workflow_screen", String.format("use_mm_workflow_screen.screen_id=%d AND use_mm_workflow_screen.workflow_id=%s", screenId, theWorkflowId));
				if (count > 0)
					status = true;
			}
		}
		return status;
	}
	
	public static List<HashMap<String, String>> getDebriefNavigationListItems(Activity currentActivity, String[] debriefNavigationListFrom, int[] debriefNavigationListTo) {
		List<HashMap<String, String>> table = new ArrayList<HashMap<String, String>>();
		try {
			//check whether cached items are available.
			String currentWorkflowId = MetrixWorkflowManager.getWorkflowId(MetrixWorkflowManager.getCurrentWorkflowName(currentActivity));
			List<Hashtable<String, String>> thisWFNavList = debriefNavigationList.get(currentWorkflowId);
			if (thisWFNavList == null || thisWFNavList.isEmpty()) {
				String query = "select s.screen_name, s.label, s.workflow_script from use_mm_screen s"
						+ " join use_mm_workflow_screen ws on ws.screen_id = s.screen_id"
						+ " where ws.workflow_id = " + currentWorkflowId
						+ " and ws.step_order is not null and ws.step_order > 0 order by ws.step_order asc";
				thisWFNavList = MetrixDatabaseManager.getFieldStringValuesList(query);
				debriefNavigationList.put(currentWorkflowId, thisWFNavList);
			}

			for (Hashtable<String, String> item : thisWFNavList) {
				HashMap<String, String> row = new HashMap<String, String>();

				String screenName = item.get("screen_name");
				String label = item.get("label");
				String workflowScript = item.get("workflow_script");

				if (!MetrixStringHelper.isNullOrEmpty(workflowScript)) {
					ClientScriptDef workflowScriptDef = MetrixClientScriptManager.getScriptDefForScriptID(workflowScript);
					if (workflowScriptDef != null) {
						boolean shouldIncludeInWorkflow = MetrixClientScriptManager.executeScript(new WeakReference<Activity>(currentActivity), workflowScriptDef);
						if (!shouldIncludeInWorkflow)
							continue;
					}
				}

				row.put(debriefNavigationListFrom[0], screenName);
				row.put(debriefNavigationListFrom[1], label);
				int itemCount = MetrixUIHelper.getPreviousDebriefItemCount(screenName, currentActivity);
				row.put(debriefNavigationListFrom[2], String.valueOf(itemCount));
				table.add(row);
			}
		} catch (Exception e) {
			LogManager.getInstance().error(e);
		}
		return table;
	}

	private static boolean screenListContainsScreenName(List<WorkflowScreen> screenList, String screenName) {
		for (WorkflowScreen screen : screenList) {
			if (MetrixStringHelper.valueIsEqual(screenName, screen.ScreenName))
				return true;
		}
		return false;
	}

	private static class WorkflowScreen {
		public String ScreenName;
		public String ScriptID;
	}

	private static class WorkflowJumpToMenuItem {
		public String ScreenName;
		public String Label;
		public String ScriptID;
	}

	public static String getQuoteWorkflowNameForQuoteType(String quoteType) {
		String workflowToUse = QUOTE_WORKFLOW;
		if (!MetrixStringHelper.isNullOrEmpty(quoteType)) {
			String trialWorkflow = String.format("Quote~%s", quoteType);
			int rowCount = MetrixDatabaseManager.getCount("use_mm_workflow", String.format("name = '%s'", trialWorkflow));
			if (rowCount > 0)
				workflowToUse = trialWorkflow;
		}
		return workflowToUse;
	}
}