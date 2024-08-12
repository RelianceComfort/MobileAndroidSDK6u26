package com.metrix.metrixmobile;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.metrix.architecture.database.MetrixDatabaseManager;
import com.metrix.architecture.designer.MetrixSkinManager;
import com.metrix.architecture.designer.MetrixWorkflowManager;
import com.metrix.architecture.managers.MetrixUpdateManager;
import com.metrix.architecture.metadata.MetrixTransaction;
import com.metrix.architecture.ui.widget.ObservableScrollView;
import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.architecture.utilities.MetrixActivityHelper;
import com.metrix.architecture.utilities.MetrixDateTimeHelper;
import com.metrix.architecture.utilities.MetrixPublicCache;
import com.metrix.architecture.utilities.MetrixStringHelper;
import com.metrix.architecture.utilities.ResourceValueObject;
import com.metrix.metrixmobile.survey.SurveyInstanceList;
import com.metrix.metrixmobile.survey.SurveyQuestionView;
import com.metrix.architecture.utilities.MetrixCurrentKeysHelper;
import com.metrix.metrixmobile.survey.MetrixSurveyRunner;
import com.metrix.metrixmobile.system.DebriefActivity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;

public class DebriefSurvey extends DebriefActivity {
	private Boolean surveyRun = false;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.debrief_payments);
	}

	/*
         * (non-Javadoc)
         *
         * @see android.app.Activity#onStart()
         */
	public void onStart() {
		super.onStart();
		mLayout = (ViewGroup) findViewById(R.id.table_layout);

		String surveyGroupId = getIntent().getStringExtra("surveyGroupId");
		String isFromMenu = getIntent().getStringExtra("isFromMenu");

		if (!surveyRun) {
			String taskId = MetrixCurrentKeysHelper.getKeyValue("task", "task_id");
			Intent intent = MetrixSurveyRunner.createSurveyIntentForTask(this, taskId, surveyGroupId, "", "");

			//Check to see if we should skip this survey if it is completed. Only should happen when
			//getting there from the workflow.
			if(intent != null)
			{
				if (isFromMenu.equals("N"))
				{	
					Bundle initData = intent.getBundleExtra("EXTRA_SURVEY_INITIALIZE");
					MetrixSurveyRunner surveyRunner = new MetrixSurveyRunner(this,initData);
					if (surveyRunner.isSurveyShowOnce(surveyGroupId))
					{
						if (surveyRunner.isSurveyComplete(surveyGroupId))
						{
							MetrixWorkflowManager.advanceWorkflow(DebriefSurvey.this);
							surveyRun = true;
						}
					}
				}
			}
			
			if (!surveyRun)
			{
				if(intent != null) {
					startActivityForResult(intent, 0);
				}
				else
				{
					MetrixWorkflowManager.advanceWorkflow(DebriefSurvey.this);
					surveyRun = true;
				}
			}
		} 
		else 
		{	
			String taskId = MetrixCurrentKeysHelper.getKeyValue("task", "task_id");
			Intent intent = MetrixSurveyRunner.createSurveyIntentForTask(this, taskId, surveyGroupId, "", "");
			if (intent != null)
			{
				if (isFromMenu.equals("N"))
				{	
					Bundle initData = intent.getBundleExtra("EXTRA_SURVEY_INITIALIZE");
					MetrixSurveyRunner surveyRunner = new MetrixSurveyRunner(this,initData);
					if (surveyRunner.isSurveyShowOnce(surveyGroupId))
					{
						if (surveyRunner.isSurveyComplete(surveyGroupId))
						{
							finish();
						}
					}
				}
			}
			else
			{
				// no valid surveys to show. Just quit so it goes back to previous screen.
				finish();
			}
			surveyRun = false;
		}
	}
			
	
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode == RESULT_OK) {
			MetrixWorkflowManager.advanceWorkflow(DebriefSurvey.this);
			surveyRun = true;
		} else {
			// the user hit the back button
			finish();
		}
	}
}
