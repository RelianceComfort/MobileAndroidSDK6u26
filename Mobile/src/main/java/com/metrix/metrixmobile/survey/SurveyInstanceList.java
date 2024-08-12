package com.metrix.metrixmobile.survey;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.metrix.architecture.assistants.MetrixControlAssistant;
import com.metrix.architecture.database.MetrixCursor;
import com.metrix.architecture.database.MetrixDatabaseManager;
import com.metrix.architecture.designer.MetrixListScreenManager;
import com.metrix.architecture.ui.widget.SimpleRecyclerViewAdapter;
import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.architecture.utilities.LogManager;
import com.metrix.architecture.utilities.MetrixActivityHelper;
import com.metrix.architecture.utilities.MetrixDateTimeHelper;
import com.metrix.architecture.utilities.MetrixStringHelper;
import com.metrix.metrixmobile.R;
import android.content.Intent;
import android.database.SQLException;
import android.graphics.Color;
import android.os.Bundle;
import androidx.recyclerview.widget.RecyclerView;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import com.metrix.metrixmobile.system.MetrixActivity;

public class SurveyInstanceList extends MetrixActivity implements SimpleRecyclerViewAdapter.OnItemClickListener {
	private RecyclerView recyclerView;
	private SimpleRecyclerViewAdapter mAdapter;
	private String currentTaskId;
	private String currentSurveyId;
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.survey_instance_list);
		recyclerView = findViewById(R.id.recyclerView);
		MetrixListScreenManager.setupVerticalRecyclerView(recyclerView, R.drawable.rv_item_divider);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onStart()
	 */
	@Override
	public void onStart() {
		super.onStart();

		AndroidResourceHelper.setResourceValues(this.findViewById(R.id.add), "Add", false);

		String surveyId = getIntent().getStringExtra("surveyId");
		String taskId = getIntent().getStringExtra("taskId");
		currentTaskId = taskId;
		currentSurveyId = surveyId;
		populateList(surveyId, taskId);
		
		String description = MetrixDatabaseManager.getFieldStringValue("survey", "description", String.format("survey_id='%s'", surveyId));
		String screenTitle = String.format("%s (%s)", AndroidResourceHelper.getMessage("Survey"), description);
		
		TextView tvTitle = (TextView) findViewById(R.id.title);
		TextView tvTip = (TextView) findViewById(R.id.screentip);
		Button addBtn = (Button) findViewById(R.id.add);
		tvTitle.setText(screenTitle);
		tvTip.setText(AndroidResourceHelper.getMessage("PleaseSelSurveyInstance"));
		
		addBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = MetrixSurveyRunner.createSurveyIntentForTask(SurveyInstanceList.this, currentTaskId, "", currentSurveyId, "");
				if (intent != null) {
					intent.putExtra("isFromMenu", "Y");
					MetrixActivityHelper.startNewActivityAndFinish(SurveyInstanceList.this, intent);
				}
			}
		});
	}
	
	/**
	 * Populate the job list with the tasks assigned to the user.
	 */	
	private void populateList(String surveyId, String taskId) {
		List<HashMap<String, Spannable>> surveyInstanceData = new ArrayList<HashMap<String, Spannable>>();

		// If show_once is Y, don't try to show a list and just show a new instance of the survey.
		String showOnce = MetrixDatabaseManager.getFieldStringValue("survey", "show_once", String.format("survey_id = %s", surveyId));
		if (!MetrixStringHelper.valueIsEqual(showOnce, "Y")) {
			String theBaseQuery = String.format("select distinct(instance_dttm) from survey_result where survey_id = %s and task_id = %s order by instance_dttm", surveyId, taskId);
			MetrixCursor cursor = MetrixDatabaseManager.rawQueryMC(theBaseQuery, null);
			try {
				if (cursor == null || !cursor.moveToFirst()) {
					Intent intent = MetrixSurveyRunner.createSurveyIntentForTask(this, taskId, "", surveyId, "");
					if (intent != null) {
						intent.putExtra("isFromMenu", "Y");
						MetrixActivityHelper.startNewActivityAndFinish(this, intent);
					}
					return;
				}

				if (cursor.getCount() >= 1) {
					String previousInstance = "";
					while (cursor.isAfterLast() == false) {
						// For each instance, get up to the first three answers and display along with timestamp.
						String currentInstance = cursor.getString(0);
						if (previousInstance.equals(currentInstance)) {
							cursor.moveToNext();
							continue;
						} else {
							MetrixCursor answerCursor = getInstanceAnswers(surveyId,taskId,cursor.getString(0));
							if (answerCursor == null || !answerCursor.moveToFirst()) {
								return;
							}

							try {
								int pass = 1;
								HashMap<String, Spannable> instanceHashMap = new HashMap<String, Spannable>();
								String instanceDttmUI = MetrixDateTimeHelper.convertDateTimeFromDBToUI(cursor.getString(0));
								Spannable dttmSpan = new SpannableString(cursor.getString(0));
								Spannable instanceDttmUISpan = new SpannableString(instanceDttmUI);
								instanceHashMap.put("instance_dttm_db", dttmSpan);
								instanceHashMap.put("instance_dttm", instanceDttmUISpan);

								while (answerCursor.isAfterLast() == false) {
									String questionKey = String.format("%s%s", "question", Integer.toString(pass));
									String questionId = answerCursor.getString(2);
									String question = MetrixDatabaseManager.getFieldStringValue("survey_question", "question", String.format("survey_id='%s' and question_id = %s", surveyId, questionId));
									String controlType = MetrixDatabaseManager.getFieldStringValue("survey_question", "control_type", String.format("survey_id='%s' and question_id = %s", surveyId, questionId));
									if (controlType.toUpperCase().equals("DATE")) {
										String answerDttmUI = answerCursor.getString(0);
										if(!answerDttmUI.equals(AndroidResourceHelper.getMessage("SkippedAnswer"))){
											answerDttmUI = MetrixDateTimeHelper.convertDateTimeFromDBToUI(answerCursor.getString(0));
										}
										String answer = String.format("%s%s%s", "{", answerDttmUI, "}");
										String questionAndAnswer = String.format("%s %s", question, answer);
										Spannable qaSpan = new SpannableString(questionAndAnswer);
										qaSpan.setSpan(new ForegroundColorSpan(Color.parseColor("#8427E2")), question.length() + 1, questionAndAnswer.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
										qaSpan.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), question.length() + 1, questionAndAnswer.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
										instanceHashMap.put(questionKey, qaSpan);
									} else {
										String answer = String.format("%s %s %s", "{", answerCursor.getString(0), "}");
										String questionAndAnswer = String.format("%s %s", question, answer);
										Spannable qaSpan = new SpannableString(questionAndAnswer);
										qaSpan.setSpan(new ForegroundColorSpan(Color.parseColor("#8427E2")), question.length() + 1, questionAndAnswer.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
										qaSpan.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), question.length() + 1, questionAndAnswer.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
										instanceHashMap.put(questionKey, qaSpan);
									}

									if (pass > 2)
										break;
									pass++;

									answerCursor.moveToNext();
								}

								surveyInstanceData.add(instanceHashMap);
								previousInstance = currentInstance;
							} catch(SQLException ex) {
								LogManager.getInstance(this).error(ex);
							} catch(Exception ex) {
								LogManager.getInstance(this).error(ex);
							} finally {
								answerCursor.close();
							}
						}

						cursor.moveToNext();
					}
				} else {
					Intent intent = MetrixSurveyRunner.createSurveyIntentForTask(this, taskId, "", surveyId, "");
					if (intent != null) {
						intent.putExtra("isFromMenu", "Y");
						MetrixActivityHelper.startNewActivityAndFinish(this, intent);
					}
				}
			} catch(SQLException ex) {
				LogManager.getInstance(this).error(ex);
			} catch(Exception ex) {
				LogManager.getInstance(this).error(ex);
			} finally {
				if (cursor != null)
					cursor.close();
			}
		}

		if (surveyInstanceData.size() > 0) {
			int[] toFields = new int[] {R.id.instancedttm, R.id.question1, R.id.question2, R.id.question3};
			String[] fromFields = new String[] { "instance_dttm", "question1", "question2", "question3"};

			if (mAdapter == null) {
				mAdapter = new SimpleRecyclerViewAdapter(surveyInstanceData, R.layout.survey_instance_list_item_layout, fromFields, toFields, new int[] {}, null);
				mAdapter.setClickListener(this);
				recyclerView.setAdapter(mAdapter);
			} else {
				mAdapter.updateData(surveyInstanceData);
			}
		} else {
			Intent intent = MetrixSurveyRunner.createSurveyIntentForTask(this, taskId, "", surveyId, "");
			if (intent != null) {
				intent.putExtra("isFromMenu", "Y");
				MetrixActivityHelper.startNewActivityAndFinish(this, intent);
			}
		}
	}
	
	public MetrixCursor getInstanceAnswers(String surveyId, String taskId, String instanceDttm) {
		String instanceDttmMinusMilliSeconds = instanceDttm.substring(0, 19);
	    String theRawQuery = String.format("select answer,comments,question_id from survey_result where survey_id = %s and task_id = %s and substr(instance_dttm, 1,19) == '%s' order by abs(result_id)",surveyId, taskId, instanceDttmMinusMilliSeconds);
	    
		MetrixCursor cursor = MetrixDatabaseManager.rawQueryMC(theRawQuery, null);
		return cursor;
	}

	@Override
	public void onSimpleRvItemClick(int position, Object item, View view) {
		try {
			@SuppressWarnings("unchecked")
			HashMap<String, Spannable> selectedItem = (HashMap<String, Spannable>) item;
			String instanceDttm = ((Spannable)selectedItem.get("instance_dttm_db")).toString();

			Intent intent = MetrixSurveyRunner.createSurveyIntentForTask(SurveyInstanceList.this, currentTaskId, "", currentSurveyId, instanceDttm);
			if (intent != null) {
				intent.putExtra("isFromMenu", "Y");
				MetrixActivityHelper.startNewActivityAndFinish(SurveyInstanceList.this, intent);
			}
		} catch (Exception e) {
			LogManager.getInstance().error(e);
		}
	}
}
 