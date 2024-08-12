package com.metrix.metrixmobile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import androidx.core.view.MenuItemCompat;
import android.view.Menu;
import android.view.MenuItem;

import com.metrix.architecture.assistants.MetrixApplicationAssistant;
import com.metrix.architecture.attachment.AttachmentWidgetManager;
import com.metrix.architecture.database.MetrixDatabaseManager;
import com.metrix.architecture.database.MobileApplication;
import com.metrix.architecture.designer.MetrixScreenManager;
import com.metrix.architecture.designer.MetrixWorkflowManager;
import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.architecture.utilities.MetrixActivityHelper;
import com.metrix.architecture.utilities.MetrixCurrentKeysHelper;
import com.metrix.architecture.utilities.MetrixStringHelper;
//import com.metrix.metrixmobile.survey.SurveyManager;

import com.metrix.architecture.utilities.MetrixUIHelper;
import com.metrix.metrixmobile.survey.MetrixSurveyRunner;
import com.metrix.metrixmobile.survey.SurveyInstanceList;

public class DebriefMetrixActionView {

	/***
	 * Populate action menu with the use of Android ActionBar
	 * @param menu
	 */
	public static void onCreateMetrixActionView(Activity activity, Menu menu) {

		String workflowName = MetrixWorkflowManager.getCurrentWorkflowName(MobileApplication.getAppContext());
		if (MetrixStringHelper.isNullOrEmpty(workflowName))
			workflowName = MetrixWorkflowManager.DEBRIEF_WORKFLOW;
		int i =0;
		Map<String, String> jumpToItems = MetrixWorkflowManager.getJumpToMenuForWorkflow(activity, workflowName);
		if (jumpToItems != null && jumpToItems.size() > 0) {
			for (String key : jumpToItems.keySet()) {
				if (!DebriefMetrixActionView.itemShouldBeSkipped(key)) {

					MenuItem menuItem = menu.add(0, i, 1, jumpToItems.get(key));
					MenuItemCompat.setShowAsAction(menuItem, MenuItemCompat.SHOW_AS_ACTION_NEVER);
				}
				i++;
			}
		}
	}

	/***
	 * Handling each action menu item click events
	 * @param activity
	 * @param item
	 */
	@SuppressLint("DefaultLocale")
	public static void onMetrixActionMenuItemSelected(Activity activity, MenuItem item) {
		String workflowName = MetrixWorkflowManager.getCurrentWorkflowName(MobileApplication.getAppContext());
		if (MetrixStringHelper.isNullOrEmpty(workflowName))
			workflowName = MetrixWorkflowManager.DEBRIEF_WORKFLOW;
		int workFlowItemIndex = 0;
		Map<String, String> jumpToItems = MetrixWorkflowManager.getJumpToMenuForWorkflow(activity, workflowName);
		if (jumpToItems == null)
			return;
		for (String key : jumpToItems.keySet()) {
			if (item!= null && item.getTitle()!=null && jumpToItems.get(key).compareToIgnoreCase(item.getTitle().toString()) == 0 && item.getItemId() == workFlowItemIndex) {
				if (DebriefMetrixActionView.itemNeedsComplexHandling(key)) {
					DebriefMetrixActionView.handleComplexActionMenuItem(activity, key);
				}else {
					if(MetrixApplicationAssistant.screenNameHasClassInCode(key)){
						Intent intent = MetrixActivityHelper.createActivityIntent(activity, key);
						MetrixActivityHelper.startNewActivity(activity, intent);
					}
					else{
						int screenId = MetrixScreenManager.getScreenId(key);
						HashMap<String, String> screenPropertyMap = MetrixScreenManager.getScreenProperties(screenId);
						if(screenPropertyMap != null){
							String screenType = screenPropertyMap.get("screen_type");
							if(!MetrixStringHelper.isNullOrEmpty(screenType))
							{
								if(screenType.toLowerCase().contains("standard"))
								{
									Intent	intent = MetrixActivityHelper.createActivityIntent(activity, "com.metrix.metrixmobile.system", "MetadataDebriefActivity");
									intent.putExtra("ScreenID", screenId);
									MetrixActivityHelper.startNewActivity(activity, intent);
								}
								else if(screenType.toLowerCase().contains("list"))
								{
									Intent	intent = MetrixActivityHelper.createActivityIntent(activity, "com.metrix.metrixmobile.system", "MetadataListDebriefActivity");
									intent.putExtra("ScreenID", screenId);
									MetrixActivityHelper.startNewActivity(activity, intent);
								}
								else
									MetrixUIHelper.showSnackbar(activity, AndroidResourceHelper.getMessage("YYCSWrongScreenType", screenType));
							}
						}
					}
				}
			}
			workFlowItemIndex ++;
		}
	}

	private static boolean itemShouldBeSkipped(String item) {
		String taskId = MetrixCurrentKeysHelper.getKeyValue("task", "task_id");

		if (MetrixStringHelper.isNullOrEmpty(taskId))
			return true;

		if (item.compareToIgnoreCase("DebriefTaskSteps") == 0) {
			int count = MetrixDatabaseManager.getCount("task_steps", "task_id=" + taskId);

			if (count <= 0) {
				return true;
			}
		} else if (item.compareToIgnoreCase("DebriefSurvey1") == 0 ||
				item.compareToIgnoreCase("DebriefSurvey2") == 0 ||
				item.compareToIgnoreCase("DebriefSurvey3") == 0 ||
				item.compareToIgnoreCase("DebriefSurvey4") == 0 ||
				item.compareToIgnoreCase("DebriefSurvey5") == 0) {
			final ArrayList<HashMap<String,String>> surveyHashMaps = MetrixSurveyRunner.getApplicableSurveys(taskId, "", "");
			if (surveyHashMaps == null || surveyHashMaps.size() == 0)
				return true;

		}

		return false;
	}

	public static boolean itemNeedsComplexHandling(String item) {
		if ((item.compareToIgnoreCase("DebriefProductRemove") == 0) ||
				(item.compareToIgnoreCase("DebriefSurvey1") == 0) ||
				(item.compareToIgnoreCase("DebriefSurvey2") == 0) ||
				(item.compareToIgnoreCase("DebriefSurvey3") == 0) ||
				(item.compareToIgnoreCase("DebriefSurvey4") == 0) ||
				(item.compareToIgnoreCase("DebriefSurvey5") == 0) ||
				(item.compareToIgnoreCase("DebriefTaskAttachment") == 0) ||
				(item.compareToIgnoreCase("DebriefAttachmentList") == 0) ||
				(item.compareToIgnoreCase("DebriefTaskTextFeed") == 0)){
			return true;
		} else {
			return false;
		}
	}

	@SuppressLint("DefaultLocale")
	public static void handleComplexActionMenuItem(final Activity activity, String item) {
		final String taskId = MetrixCurrentKeysHelper.getKeyValue("task", "task_id");
		if (item.compareToIgnoreCase("DebriefProductRemove") == 0) {
			Intent intent = MetrixActivityHelper.createActivityIntent(activity, DebriefProductRemove.class);
			MetrixCurrentKeysHelper.setKeyValue("place", "place_id", MetrixDatabaseManager.getFieldStringValue("task", "place_id_cust", "task_id = " + taskId));
			MetrixActivityHelper.startNewActivity(activity, intent);
		} else if (item.compareToIgnoreCase("DebriefSurvey1") == 0 ||
				item.compareToIgnoreCase("DebriefSurvey2") == 0 ||
				item.compareToIgnoreCase("DebriefSurvey3") == 0 ||
				item.compareToIgnoreCase("DebriefSurvey4") == 0 ||
				item.compareToIgnoreCase("DebriefSurvey5") == 0) {

			final ArrayList<HashMap<String,String>> surveyHashMaps = MetrixSurveyRunner.getApplicableSurveys(taskId, "", "");
			if (surveyHashMaps != null && surveyHashMaps.size() > 0)
			{
				if (surveyHashMaps.size() == 1)
				{
					Intent intent = null;
					HashMap<String,String> hm = surveyHashMaps.get(0);
					String showNewValue = hm.get("show_new");
					String surveyId = hm.get("survey_id");
					if (showNewValue != null)
					{
						if (showNewValue.toUpperCase().equals("Y"))
						{
							Intent instanceIntent = new Intent();
							instanceIntent.setClass(activity, SurveyInstanceList.class);
							instanceIntent.putExtra("surveyId", surveyId);
							instanceIntent.putExtra("taskId", taskId);
							MetrixActivityHelper.startNewActivity(activity, instanceIntent);
						}
						else
						{
							intent = MetrixSurveyRunner.createSurveyIntentForTask(activity, taskId, "", surveyId, "");
						}
					}
					else
					{
						intent = MetrixSurveyRunner.createSurveyIntentForTask(activity, taskId, "", surveyId, "");
					}

					if (intent != null) {
						intent.putExtra("isFromMenu", "Y");
						MetrixActivityHelper.startNewActivity(activity, intent);
					}
				}
				else if (surveyHashMaps.size() > 1)
				{
					final ArrayList<String> surveyOptions = new ArrayList<String>();

					for (HashMap<String,String> hm : surveyHashMaps) {
						String description = hm.get("description");
						surveyOptions.add(description);
					}

					surveyOptions.add("All");
					final int surveyOptionsCount = surveyOptions.size();

					AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(activity);
					CharSequence[] items = surveyOptions.toArray(new CharSequence[surveyOptions.size()]);

					dialogBuilder.setTitle(AndroidResourceHelper.getMessage("Select"));
					dialogBuilder.setItems(items, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();
							Intent intent = null;
							//Object selectedItem = ((AlertDialog)dialog).getListView().getItemAtPosition(which);
							if (which == surveyOptionsCount - 1)
							{
								intent = MetrixSurveyRunner.createSurveyIntentForTask(activity, taskId, "", "", "");
							}
							else
							{
								HashMap<String,String> hm = surveyHashMaps.get(which);
								String showNewValue = hm.get("show_new");
								String surveyId = hm.get("survey_id");
								if (showNewValue != null)
								{
									if (showNewValue.toUpperCase().equals("Y"))
									{
										Intent instanceIntent = new Intent();
										instanceIntent.setClass(activity, SurveyInstanceList.class);
										instanceIntent.putExtra("surveyId", surveyId);
										instanceIntent.putExtra("taskId", taskId);
										MetrixActivityHelper.startNewActivity(activity, instanceIntent);
									}
									else
									{
										intent = MetrixSurveyRunner.createSurveyIntentForTask(activity, taskId, "", surveyId, "");
									}
								}
								else
								{
									intent = MetrixSurveyRunner.createSurveyIntentForTask(activity, taskId, "", surveyId, "");
								}
							}

							if (intent != null) {
								intent.putExtra("isFromMenu", "Y");
								MetrixActivityHelper.startNewActivity(activity, intent);
							}
						}
					});

					dialogBuilder.create().show();
				}
			}
		}else if (item.compareToIgnoreCase("DebriefTaskAttachment") == 0) {
			Intent intent = MetrixActivityHelper.createActivityIntent(activity, DebriefTaskAttachment.class);
			MetrixActivityHelper.startNewActivity(activity, intent);
		}else if (item.compareToIgnoreCase("DebriefTaskTextFeed") == 0) {
			Intent intent = MetrixActivityHelper.createActivityIntent(activity, DebriefTaskTextList.class);
			intent.putExtra("from_context_menu", true);
			MetrixActivityHelper.startNewActivity(activity, intent);
		}else if (item.compareToIgnoreCase("DebriefAttachmentList") == 0) {
			AttachmentWidgetManager.openFromWorkFlow(activity, "Debrief");
		}

	}
}

