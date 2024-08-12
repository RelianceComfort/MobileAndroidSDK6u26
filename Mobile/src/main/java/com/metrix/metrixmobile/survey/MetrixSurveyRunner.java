package com.metrix.metrixmobile.survey;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.database.DatabaseUtils;
import android.os.Bundle;
import android.text.TextUtils;

import com.metrix.architecture.constants.MetrixTransactionTypes;
import com.metrix.architecture.database.MetrixCursor;
import com.metrix.architecture.database.MetrixDatabaseManager;
import com.metrix.architecture.managers.MetrixUpdateManager;
import com.metrix.architecture.metadata.MetrixSqlData;
import com.metrix.architecture.metadata.MetrixTransaction;
import com.metrix.architecture.metadata.MetrixUpdateMessage;
import com.metrix.architecture.metadata.MetrixUpdateMessage.MetrixUpdateMessageTransactionType;
import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.architecture.utilities.DataField;
import com.metrix.architecture.utilities.MetrixActivityHelper;
import com.metrix.architecture.utilities.MetrixCurrentKeysHelper;
import com.metrix.architecture.utilities.MetrixDateTimeHelper;
import com.metrix.architecture.utilities.MetrixStringHelper;
import com.metrix.metrixmobile.survey.MetrixQuestionData.QuestionType;
import com.metrix.metrixmobile.survey.SurveyQuestionData.SurveyAnswerType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map.Entry;

@SuppressLint("DefaultLocale")
public class MetrixSurveyRunner extends SurveyRunnerBase<MetrixQuestionData> {

	public final static String METRIX_SURVEY_IDS = "METRIX_SURVEY_IDS";
	public final static String METRIX_SURVEY_DATA = "METRIX_SURVEY_DATA";
	public final static String METRIX_TASK_ID = "METRIX_TASK_ID";
	public final static String SURVEY_INIT_DATA = "SURVEY_INIT_DATA";
	public final static String SURVEY_INSTANCE_DTTM = "SURVEY_INSTANCE_DTTM";
	private final static String TYPE_SERVICE = "SERVICE";
	private final static String TYPE_CUSTOMER = "CUSTOMER";
	private final static String TYPE_REGULATORY = "REGULATORY";
	
	private Activity mActivity;
	private Bundle mInitData;
	private Integer mTaskId;
	private int mNextResultId;
	private boolean mChangedAnswers = false;

	public MetrixSurveyRunner(Activity activity, Bundle initData) {
		
		mActivity = activity;
		mInitData = initData;
		mTaskId = initData.getInt(METRIX_TASK_ID);
		
		initialize();
	}
	
	@Override
	protected void loadQuestions() {
		
		int[] surveyIds = mInitData.getIntArray(METRIX_SURVEY_IDS);
		ArrayList<MetrixQuestionData> questions = getQuestionsForSurveys(surveyIds);
		
		// Save button
		if(questions.size() > 0) {
			MetrixQuestionData finalQuestion = new MetrixQuestionData();
			finalQuestion.survey = null;
			finalQuestion.questionType = MetrixQuestionData.QuestionType.FINAL;
			finalQuestion.setQuestion("");
			finalQuestion.setContinueText(AndroidResourceHelper.getMessage("Save"));
			finalQuestion.setIsFinalStatement(true);
			finalQuestion.setSection(null);
			finalQuestion.updateQuestionData();
			questions.add(finalQuestion);
		}
		
		mQuestions = questions;
		
		boolean isAllAnswered = true;
		String currentSurveyId = "";
		
		for (Integer surveyId: surveyIds)
		{
			currentSurveyId = Integer.toString(surveyId);
			
			for (MetrixQuestionData mqd : mQuestions)
			{
				if (mqd.survey != null)
				{
					if (Integer.toString(mqd.survey.id) == currentSurveyId)
					{
						SurveyAnswerData sad = getPreviousAnswer(mqd.getQuestionId());
						if (sad != null)
						{
							String answerText = sad.getAnswer();
							if (answerText != null)
							{
								//Do nothing. This question has been answered.
							}
							else if (MetrixStringHelper.valueIsEqual(mqd.controlType.toUpperCase(), "ATTACHMENT") && sad.getAttachmentId() != 0) {
								//Do nothing. This attachment question has been answered.
							}
							else if(MetrixStringHelper.valueIsEqual(mqd.controlType.toUpperCase(), "SIGNATURE") && sad.getAttachmentId() != 0) {
								//Do nothing. This signature question has been answered.
							}
							else
							{
								//This question has not been answered. Return false;
								isAllAnswered = false;
							}
						}
						else
						{
							//This question has not been answered. Return false;
							isAllAnswered = false;
						}
					}
				}				
			}
			
			//Process survey.
			for (MetrixQuestionData mqdata : mQuestions) 
			{
				if (mqdata.survey != null)
				{
					if (Integer.toString(mqdata.survey.id) == currentSurveyId)
					{
						mqdata.survey.all_answered	= isAllAnswered;
					}
				}									
			}
			
			//reset
			isAllAnswered = true;
		}
	}

	protected ArrayList<MetrixQuestionData> getQuestionsForSurveys(int[] surveyIds) {
		
		ArrayList<MetrixQuestionData> questions = new ArrayList<MetrixQuestionData>();
		
		if (surveyIds != null && surveyIds.length > 0)
		{
			for (int surveyIdCurrent : surveyIds)
			{
				int taskId = mInitData.getInt(MetrixSurveyRunner.METRIX_TASK_ID);
				ArrayList<HashMap<String,String>> hmList = getApplicableSurveys(Integer.toString(taskId),"",Integer.toString(surveyIdCurrent));
				if (hmList == null || hmList.size() == 0)
					continue;
				
				HashMap<String,String> hm = (HashMap<String,String>)hmList.get(0);
				if (hm == null)
					continue;
				
				String surveyId = hm.get("survey_id");
				
				MetrixSurveyData survey = new MetrixSurveyData();
				survey.id = Integer.parseInt(surveyId);
				
				String lockResultsValue = hm.get("lock_results");
				if (lockResultsValue != null)
				{
					if (lockResultsValue.toUpperCase().equals("Y"))
					{
						survey.lock_results = true;
					}
				}
				
				String showNewValue = hm.get("show_new");
				if (showNewValue != null)
				{
					if (showNewValue.toUpperCase().equals("Y"))
					{
						survey.instanceDttm = mInitData.getString(SURVEY_INSTANCE_DTTM);
						survey.show_new = true;
					}
				}
				
				String showOnceValue = hm.get("show_once");
				if (showOnceValue != null)
				{
					if (showOnceValue.toUpperCase().equals("Y"))
					{
						survey.show_once = true;
					}
				}
				
				String allowSkipValue = hm.get("allow_skip");
				if (allowSkipValue != null)
				{
					if (allowSkipValue.toUpperCase().equals("Y"))
					{
						survey.allow_skip = true;
					}
				}
				
				MetrixCursor cursor = null;
				try {
					
					String whereClause = "survey_id = " + surveyId; 
					cursor = MetrixDatabaseManager.getRowsMC("survey",
							new String[] { 
								"description",
								"last_survey"
							}, whereClause, "order_num ASC");
					
					if (cursor == null || !cursor.moveToFirst()) {
						continue;
					}
					
					survey.description = cursor.getString(0);
					survey.last_survey = "Y".equals(cursor.getString(1));
					
				} finally {
					if (cursor != null) {
						cursor.close();
						cursor = null;
					}
				}
				
				SurveySection section = new SurveySection(survey.description, null, survey.allow_skip);
				
				try {
					
					String questWhereClause = "survey_id = " + surveyId;
					cursor = MetrixDatabaseManager.getRowsMC("survey_question",
							new String[] { 
								"question_id",
								"question",
								"control_type",
								"required",
								"max_answers"								
								}, 
								questWhereClause, "sequence is NULL, sequence asc, question_id ASC");
		
					if (cursor == null || !cursor.moveToFirst()) {
						continue;
					}
		
					while (cursor.isAfterLast() == false) {
						MetrixQuestionData question = new MetrixQuestionData();
						questions.add(question);
						question.survey = survey;
						question.questionId = cursor.getInt(0);
						question.setQuestion(cursor.getString(1));
						question.controlType = cursor.getString(2);
						question.setOptional("N".equals(cursor.getString(3)));
						question.setSection(section);
						question.updateQuestionData();
						int maxAnswers = cursor.getInt(4);
						if (MetrixStringHelper.valueIsEqual(question.controlType.toUpperCase(), "COMBOBOX"))
						{
							question.setMaxChoiceAnswersToSelect(1);
						}
						else if (MetrixStringHelper.valueIsEqual(question.controlType.toUpperCase(), "MULTICHOICE"))
						{
							int numChoices = 0;
							if (question.getChoiceAnswers() != null)
							{
								numChoices = question.getChoiceAnswers().length;
							}
							if (maxAnswers > 1)
							{
								question.setMaxChoiceAnswersToSelect(maxAnswers);
							}
							else
							{
								question.setMaxChoiceAnswersToSelect(numChoices);
							}
						}
						cursor.moveToNext();
					}
					
				} finally {
					if (cursor != null) {
						cursor.close();
					}
				}
				
			}
		}
		
//		@SuppressWarnings("unchecked")
//		ArrayList<HashMap<String,String>> surveyHashMaps = (ArrayList<HashMap<String,String>>) mInitData.getSerializable(SURVEY_INIT_DATA);
//		if (surveyHashMaps != null && surveyHashMaps.size() > 0)
//		{
//			for (HashMap<String,String> hm : surveyHashMaps) {
//				
//			}
//		}

		return questions;
	}
	
	@Override
	protected boolean isQuestionValid(MetrixQuestionData question) {
		
		if (question.questionType == QuestionType.FINAL) {
			return true;
		}
		
		for (int i = 0; i < mQuestions.size(); i++) {
			
			MetrixQuestionData other = mQuestions.get(i);
			if(other == question) {
				break;
			}
			
			// If we have a previous questions in the same survey that have child 
			// questions (a next survey) then we do not show this question
			if(other.parent != null && other.parent.survey == question.survey) {
				return false;
			}
			
			// If we have a previous that belongs to a different survey that is a
			// last survey we do not display any more questions
			if(other.survey.last_survey && other.survey != question.survey) {
				return false;
			}
			
		}
		
		return true;
	}
	
	@Override
	public void cancelQuestion(String questionId) {
		mChangedAnswers = true;
		super.cancelQuestion(questionId);
		
		MetrixQuestionData question = getQuestionById(questionId);
		if (question.parent != null) {
			
			int prevIndex = mQuestions.indexOf(question) - 1;
			if(prevIndex >= 0) {
				
				MetrixQuestionData prevQuestion = mQuestions.get(prevIndex);
				if(prevQuestion.survey != question.survey) {
					// User is pressing back on the first question in this 'child survey'
					// We must remove the child survey questions
					for (int i = 0; i < mQuestions.size(); ) {
						if(mQuestions.get(i).survey == question.survey) {
							mQuestions.remove(i);
						} else {
							i++;
						}
					}
				}
			}
			
		}
	}
	
	@Override
	public void provideAnswer(String questionId, SurveyAnswerData data) {
		
		MetrixQuestionData question = getQuestionById(questionId);
		SurveyAnswerData previousAnswer = mAnswers.get(questionId);
		if(previousAnswer != data && question.questionType == MetrixQuestionData.QuestionType.QUESTION) {
			mChangedAnswers = true;
		}
		
		super.provideAnswer(questionId, data);
		
		if (question.getAnswerType() == SurveyAnswerType.MULTI_CHOICE || 
			question.getAnswerType() == SurveyAnswerType.RADIO_CHOICE) {
			
			// Check if we need to open a new survey
			int[] choices = data.getMultiChoiceSelections(); // -1 is a skip choice
			if(choices != null && choices.length == 1 && choices[0] != -1) {
				//TODO Fix multiple choice 
				MetrixQuestionData.AnswerChoice answerItem = question.getAnswerChoice(choices[0]);
				if(answerItem.nextSurveyId != null) {
					
					ArrayList<MetrixQuestionData> newQuestions = getQuestionsForSurveys(new int[] { answerItem.nextSurveyId.intValue() });
					
					for (MetrixQuestionData metrixQuestionData : newQuestions) {
						metrixQuestionData.parent = question;
					}
					
					mQuestions.addAll(mQuestions.indexOf(question) + 1, newQuestions);
				}
			}
		}
	}

	@Override
	public SurveyAnswerData getPreviousAnswer(String questionId) {
		
		SurveyAnswerData answer = super.getPreviousAnswer(questionId);
		
		if(answer == null) {
			MetrixQuestionData question = getQuestionById(questionId);
			
			if(question.survey != null) {
				MetrixCursor cursor = null;
				try {
					
					String whereClause = "";
					if (question.survey.instanceDttm != null && !question.survey.instanceDttm.isEmpty())
					{
						String instanceDttmMinusMilliSeconds = question.survey.instanceDttm.substring(0, 19);
						whereClause = "survey_id=" + question.survey.id +
								" and task_id=" + mTaskId +
								" and question_id=" + question.questionId + " and substr(instance_dttm, 1,19) == '" + instanceDttmMinusMilliSeconds + "'";
					}
					else
					{
						whereClause = "survey_id=" + question.survey.id + 
								" and task_id=" + mTaskId +
								" and question_id=" + question.questionId;						
					}
					
					cursor = MetrixDatabaseManager.getRowMC("survey_result",
							new String[] { 
								"answer_id",
								"answer",
								"attachment_id",
								"comments"
							}, whereClause);		
					
					ArrayList<Integer> selectedItems = new ArrayList<Integer>();
					answer = new SurveyAnswerData();

					while (cursor.isAfterLast() == false) {

						answer.setAnswer(cursor.getString(1));

						String attachmentIdString = cursor.getString(2);
						answer.setAttachmentId(MetrixStringHelper.isNullOrEmpty(attachmentIdString) ? 0 : Integer.parseInt(attachmentIdString));

						String comments = cursor.getString(3);
						if (!TextUtils.isEmpty(comments)) {
							answer.setComment(comments);
						}
						if (!cursor.isNull(0)) {
							int answerId = cursor.getInt(0);
							
							int answerIndex = question.getAnswerIndex(answerId);
							if (answerIndex >= 0) {
								selectedItems.add(answerIndex);
								if (!MetrixStringHelper.isNullOrEmpty(comments)) {
									answer.setMultiChoiceComment(answerId, comments);
								}
							}
						}
						else
						{
							//Just in case answer_id is null, search for the index based on value.							
							int answerIndex = question.getAnswerIndexByValue(cursor.getString(1));
							if (answerIndex >= 0) {
								selectedItems.add(answerIndex);
								if (!MetrixStringHelper.isNullOrEmpty(comments)) {
									answer.setMultiChoiceComment(question.getAnswerChoice(answerIndex).answerId, comments);
								}
							}
						}

						answer.setIsCompleted(true);
						mAnswers.put(questionId, answer);

						cursor.moveToNext();
					}
					
					if (selectedItems.size() > 0)
						answer.setMultiChoiceSelections(selectedItems);
					
				} finally {
					if (cursor != null) {
						cursor.close();
						cursor = null;
					}
				}
			}
			
		}
		
		return answer;
	}
	
	@Override
	public boolean complete(Bundle surveyResult) {
		
		if (!mChangedAnswers) {
			// No work to do
			return true;
		}
		
		// Find out what result id should be given to the next answer
		String lastResultId = MetrixDatabaseManager.getFieldStringValue(true, 
				"survey_result", 
				"result_id",
				"result_id < 0",
				null, null, null, 
				"result_id ASC",
				null);
		
		if (lastResultId == null || lastResultId.length() == 0) {
			mNextResultId = -1;
		} else {
			mNextResultId = Integer.valueOf(lastResultId) - 1;
		}
				
		ArrayList<MetrixSqlData> dataChanges = new ArrayList<MetrixSqlData>();
		
		HashSet<Integer> surveys = new HashSet<Integer>();
		for (MetrixQuestionData question : mQuestions) {
			if(question.survey != null) {
				surveys.add(question.survey.id);
			}
		}
		
		for (Integer surveyId : surveys) {
			deletePreviousSurveyAnswers(surveyId.intValue());
		}
		
		for (Entry<String, SurveyAnswerData> item : mAnswers.entrySet()) {
			MetrixQuestionData question = getQuestionById(item.getKey());
	    	if(question != null) {
	    		createDbUpdatesForAnswer(question, item.getValue(), dataChanges);
	    	}
		}
		
	    if(dataChanges.size() > 0) {
	    	MetrixTransaction transactionInfo = MetrixTransaction.getTransaction("task", "task_id");
			MetrixUpdateManager.update(dataChanges, true, transactionInfo,  AndroidResourceHelper.getMessage("UpdateSurveyAnswers"), mActivity);
	    }

		return true;
	}
	
	private void deletePreviousSurveyAnswers(int surveyId) {
		
		MetrixCursor cursor = null;
		try {
						
			String whereClause = "survey_id=" + surveyId + " and task_id=" + mTaskId;
			
			for (MetrixQuestionData question : mQuestions) {
				if(question.survey != null) {
					if (question.survey.id == surveyId)
					{
						if (question.survey.instanceDttm != null && question.survey.instanceDttm.length() > 0)
						{
							String instanceDttmMinusMilliSeconds = question.survey.instanceDttm.substring(0, 19);
							whereClause = "survey_id=" + question.survey.id + 
									" and task_id=" + mTaskId +
									" and substr(instance_dttm, 1,19) == '" + instanceDttmMinusMilliSeconds + "'";
						}						
						break;
					}
				}
			}
			
			cursor = MetrixDatabaseManager.getRowMC("survey_result",
					new String[] { 
					"question_id",
					"result_id",
					"(select next_survey_id from survey_answer where survey_answer.answer_id = survey_result.answer_id)"
					}, whereClause);

			ArrayList<Integer> subSurveys = new ArrayList<>();

			if (cursor != null && cursor.moveToFirst()) {
			
				while (cursor.isAfterLast() == false) {

					int next_survey_id = cursor.getInt(2);
					if (next_survey_id > 0) {
						subSurveys.add(next_survey_id);
					}

					int resultId = cursor.getInt(1);
					if (resultId >= 0) {
						
						String questionId = cursor.getString(0);

						MetrixUpdateMessage message = new MetrixUpdateMessage("survey_result", MetrixUpdateMessageTransactionType.Delete);
						message.values.put("survey_id", String.valueOf(surveyId));
						message.values.put("question_id", questionId);
						message.values.put("result_id", String.valueOf(resultId));
						message.save();
					}
			
					cursor.moveToNext();
				}
				
				MetrixDatabaseManager.deleteRow("survey_result", whereClause);
			}

			for (Integer subSurveyId : subSurveys) {
				deletePreviousSurveyAnswers(subSurveyId);
			}
			
		} finally {
			if (cursor != null) {
				cursor.close();
				cursor = null;
			}
		}
	}
	
	private void createDbUpdatesForAnswer(MetrixQuestionData question, SurveyAnswerData answer, ArrayList<MetrixSqlData> into) {
		if (question.questionType != MetrixQuestionData.QuestionType.QUESTION) {
			// No need to save an answer non survey questions
			return;
		}

		if (question.getAnswerType().equals(SurveyQuestionData.SurveyAnswerType.YES_NO)) {
			if(answer.getAnswer().equalsIgnoreCase(SurveyYesNoQuestion.ANSWER_SKIP)) {
				// No need to save an answer for skipped yes/no questions
				return;
			}
		}

		if (question.getAnswerType().equals(SurveyQuestionData.SurveyAnswerType.MULTI_CHOICE)) {
			MetrixSqlData data = new MetrixSqlData("survey_result", MetrixTransactionTypes.INSERT);
			prepareSqlAnswer(question, answer, mNextResultId, data);
			int[] choices = answer.getMultiChoiceSelections(); // -1 is a skip choice
			if(choices != null && choices.length != 0 && choices[0] != -1) {
				int choiceIndex = 0;
				while (choiceIndex <= choices.length - 1)
				{
						data = new MetrixSqlData("survey_result", MetrixTransactionTypes.INSERT);
						prepareSqlAnswer(question, answer, mNextResultId, data);
						addNewAnswer(question,choices,data,answer,choiceIndex);
						into.add(data);
						mNextResultId--;
						choiceIndex++;
				}
			}else {
				data.dataFields.add(new DataField("answer", answer.getAnswer()));
				into.add(data);
				mNextResultId--;
			}
		}
		else if (question.getAnswerType().equals(SurveyAnswerType.RADIO_CHOICE)) {
			MetrixSqlData data = new MetrixSqlData("survey_result", MetrixTransactionTypes.INSERT);
			prepareSqlAnswer(question, answer, mNextResultId, data);
			int[] choices = answer.getMultiChoiceSelections();
			if (choices != null && choices.length != 0 && choices[0] != -1){  //-1 skip answer handle separately
				addNewAnswer(question,choices,data,answer,0);
			}else {
				data.dataFields.add(new DataField("answer", answer.getAnswer()));
			}
			into.add(data);
			mNextResultId--;
		}
		else {
			// Only save answers that have not been skipped, but allow optional blank ATTACHMENT answers to proceed
			if (!MetrixStringHelper.isNullOrEmpty(answer.getAnswer())
					|| (MetrixStringHelper.valueIsEqual(question.controlType.toUpperCase(), "ATTACHMENT") && (answer.getAttachmentId() != 0 || question.isOptional()))
					|| (MetrixStringHelper.valueIsEqual(question.controlType.toUpperCase(), "SIGNATURE") && (answer.getAttachmentId() != 0 || question.isOptional())))
			{
				MetrixSqlData data = new MetrixSqlData("survey_result", MetrixTransactionTypes.INSERT);
				prepareSqlAnswer(question, answer, mNextResultId, data);
				//data.dataFields.add(new DataField("answer", answer.getAnswer()));
				if (question.getAnswerType().equals(SurveyQuestionData.SurveyAnswerType.DATE)) {
					if (answer.getAnswer().equals( AndroidResourceHelper.getMessage("SkippedAnswer")))
					{
						data.dataFields.add(new DataField("answer", answer.getAnswer()));
					}
					else
					{
						//String answerDt = MetrixDateTimeHelper.convertDateTimeFromUIToDB(answer.getAnswer());                
						data.dataFields.add(new DataField("answer", answer.getAnswer()));
					}
				} else if (question.getAnswerType().equals(SurveyAnswerType.ATTACHMENT)) {
					// Skip setting the answer data field here - already handled in prepareSqlAnswer()
				} else if(question.getAnswerType().equals(SurveyAnswerType.SIGNATURE)) {
					// Skip setting the answer data field here - already handled in prepareSqlAnswer()
				}else {
					data.dataFields.add(new DataField("answer", answer.getAnswer()));
				}
				into.add(data);
				mNextResultId--;
			}
		}
	}

	private void addNewAnswer(MetrixQuestionData question, int[] choices, MetrixSqlData data, SurveyAnswerData answer, int choiceIndex) {
		MetrixQuestionData.AnswerChoice answerItem = question.getAnswerChoice(choices[choiceIndex]);
		data.dataFields.add(new DataField("answer", answerItem.answer));
		data.dataFields.add(new DataField("answer_id", answerItem.answerId));
		if (!TextUtils.isEmpty(answer.getMultiChoiceComment(answerItem.answerId))) {
			data.dataFields.add(new DataField("comments", answer.getMultiChoiceComment(answerItem.answerId)));
		} else if (answer.getComment() != null && answer.getComment().length() > 0) {
			data.dataFields.add(new DataField("comments", answer.getComment()));
		}
	}

	private void prepareSqlAnswer(MetrixQuestionData question, SurveyAnswerData answer, int resultId, MetrixSqlData data) {
		data.dataFields.add(new DataField("survey_id", question.survey.id));
		data.dataFields.add(new DataField("question_id", question.questionId));
		data.dataFields.add(new DataField("result_id", resultId));
		data.dataFields.add(new DataField("question", question.getQuestion()));
		data.dataFields.add(new DataField("task_id", MetrixCurrentKeysHelper.getKeyValue("task", "task_id")));
		if (question.survey.instanceDttm != null && !question.survey.instanceDttm.isEmpty()) {
			data.dataFields.add(new DataField("instance_dttm", question.survey.instanceDttm));
		}

		// For Attachment questions, populate both attachment_id and answer here.
		if ((MetrixStringHelper.valueIsEqual(question.controlType.toUpperCase(), "ATTACHMENT") || MetrixStringHelper.valueIsEqual(question.controlType.toUpperCase(), "SIGNATURE") )&& (answer.getAttachmentId() != 0 || question.isOptional())) {
			String attachmentIDString = String.valueOf(answer.getAttachmentId());
			if (MetrixStringHelper.isNegativeValue(attachmentIDString)) {
				// Try to get the updated value from mm_attachment_id_map
				String candidateValue = MetrixDatabaseManager.getFieldStringValue("mm_attachment_id_map", "positive_key", String.format("negative_key = %s", attachmentIDString));
				if (!MetrixStringHelper.isNullOrEmpty(candidateValue))
					attachmentIDString = candidateValue;
			}

			// If we are in an optional question with no relevant Attachment ID, we still need to fill survey_result.answer
			String answerText = AndroidResourceHelper.getMessage("SkippedAnswer");
			if (!MetrixStringHelper.isNullOrEmpty(attachmentIDString) && !MetrixStringHelper.valueIsEqual(attachmentIDString, "0")) {
				data.dataFields.add(new DataField("attachment_id", attachmentIDString));

				answerText = String.format("%1$s %2$s", AndroidResourceHelper.getMessage("Attachment"), attachmentIDString);
				String fileName = MetrixDatabaseManager.getFieldStringValue(String.format("select attachment_name from attachment where attachment_id = %s", attachmentIDString));
				if (!MetrixStringHelper.isNullOrEmpty(fileName))
					answerText = fileName;
			}
			data.dataFields.add(new DataField("answer", answerText));
		}
	}
	
	@SuppressLint("DefaultLocale")
	public static ArrayList<Hashtable<String,String>> getQualifyingSurveysWithSurveyType(String taskId, String surveyType, ArrayList<Hashtable<String,String>> surveyResultsHashTable, String surveyGroupIn)
	{		
		ArrayList<Hashtable<String,String>> qualifyingSurveys = new ArrayList<Hashtable<String,String>>();
		
		if (surveyResultsHashTable != null)
		{
			String contractId = "";
		    String modelId = "";
		    String placeIdCust = MetrixDatabaseManager.getFieldStringValue("task", "place_id_cust", "task_id=" + taskId);
			String globalName = MetrixDatabaseManager.getFieldStringValue("place", "global_name", "place_id=" + DatabaseUtils.sqlEscapeString(placeIdCust));
			String stateProv = MetrixDatabaseManager.getFieldStringValue(String.format("select state_prov from address where address_id in (select address_id from task where task_id = %s)", taskId));
		    String requestUnitId = MetrixDatabaseManager.getFieldStringValue("task", "request_unit_id", "task_id=" + taskId);
		    String taskTemplateId = MetrixDatabaseManager.getFieldStringValue("task", "task_template_id", "task_id=" + taskId);
		    
		    if (!MetrixStringHelper.isNullOrEmpty(requestUnitId))
		    {
		        contractId = MetrixDatabaseManager.getFieldStringValue("request_unit", "contract_id", "request_unit_id='" + requestUnitId + "'");
		        modelId = MetrixDatabaseManager.getFieldStringValue("request_unit", "model_id", "request_unit_id='" + requestUnitId + "'");
		    }
		    
		    for (Hashtable<String,String> ht : surveyResultsHashTable) {
				String surveyReferenceId = ht.get("reference_id");
		        String surveyContractId = ht.get("contract_id");
		        String surveyModelId = ht.get("model_id");
		        String surveyTaskTemplateId = ht.get("task_template_id");
		        String surveyGroup = ht.get("survey_group");
		        
		      //If the survey group does not match then just skip it.
		        if (surveyGroupIn != null && !surveyGroupIn.isEmpty())
		        {
		            if (!surveyGroup.equals(surveyGroupIn))
		                continue;
		        }
		        
		      //If the survey's contract_id is populated, make sure the task's request_unit contract_id matches
		        if (surveyContractId != null && !surveyContractId.isEmpty())
		        {
		            if (!surveyContractId.equals(contractId))
		                continue;
		        }
		        
		      //If the survey's task_template_id is populated, make sure the task's task_template_id matches
		        if (surveyTaskTemplateId != null && !surveyTaskTemplateId.isEmpty())
		        {
		        	boolean foundMatch = false;
		        	if (surveyTaskTemplateId.contains(","))
		        	{
		        		String[] taskTemplateIds = surveyTaskTemplateId.split(",");
		        		for( int i=0; i <= taskTemplateIds.length-1; i++)
		                {
		        			String ttId = taskTemplateIds[i];
		        			if (ttId.equals(taskTemplateId))
		        			{
		        				foundMatch = true;
		        				break;
		        			}
		                }
		        	}
		        	else if (surveyTaskTemplateId.contains("|"))
		        	{
		        		String[] taskTemplateIds = surveyTaskTemplateId.split("\\|");
		        		for( int i=0; i <= taskTemplateIds.length-1; i++)
		                {
		        			String ttId = taskTemplateIds[i];
		        			if (ttId.equals(taskTemplateId))
		        			{
		        				foundMatch = true;
		        				break;
		        			}
		                }
		        	}
		        	else
		        	{
						if (taskTemplateId != null && !taskTemplateId.isEmpty()) {
							if (surveyTaskTemplateId.toUpperCase().equals(taskTemplateId.toUpperCase())) {
								foundMatch = true;
							}
						}	                
		        	}
		        	
		        	if (!foundMatch)
		        		continue;
		        }
		        
		        //If the survey's model_id is populated, make sure the task's request_unit model_id matches
				if (surveyModelId != null && !surveyModelId.isEmpty())
		        {
		            if (!surveyModelId.equals(modelId))
		                continue;
		        }
		        
		      //If the survey's type is CUSTOMER and it's reference_id is populated, make sure it matches the global_name of the task place.
		        if (surveyType.toUpperCase().equals("CUSTOMER"))
		        {
		        	if (surveyReferenceId != null && !surveyReferenceId.isEmpty())
		            {
		                if (!surveyReferenceId.equals(globalName))
		                    continue;
		            }
		        }
		        
		      //If the survey's type is CUSTOMER and it's reference_id is populated, make sure it matches the state_prov of the task place.
		        if (surveyType.toUpperCase().equals("REGULATORY"))
		        {
		        	if (surveyReferenceId != null && !surveyReferenceId.isEmpty())
		            {
		                if (!surveyReferenceId.equals(stateProv))
		                    continue;
		            }
		        }
		        
		        qualifyingSurveys.add(ht);
			}
		}
		
		return qualifyingSurveys;
	}
	
	public static ArrayList<HashMap<String,String>> getApplicableSurveys(String taskId, String surveyGroupId, String surveyIdIn) {
		ArrayList<Integer> surveys = new ArrayList<Integer>();
		ArrayList<Hashtable<String,String>> surveyDataList = new ArrayList<Hashtable<String,String>>();
		String baseQuery = "";
		if (surveyIdIn.length() > 0)
		{
			baseQuery = "select survey_id,lock_results,show_new,description,show_once,allow_skip,contract_id,task_template_id,survey_group,model_id,reference_id from survey where type = '%s' order by order_num";
		}
		else
		{
			baseQuery = "select survey_id,lock_results,show_new,description,show_once,allow_skip,contract_id,task_template_id,survey_group,model_id,reference_id from survey where type = '%s' and category != 'SUB SURVEY' order by order_num";
		}
		
		// Get the service surveys
		try {
			ArrayList<Hashtable<String,String>> serviceSurveysHashtable = MetrixDatabaseManager.getFieldStringValuesList(String.format(baseQuery,MetrixSurveyRunner.TYPE_SERVICE));
			ArrayList<Hashtable<String,String>> serviceSurveys = getQualifyingSurveysWithSurveyType(taskId,MetrixSurveyRunner.TYPE_SERVICE,serviceSurveysHashtable,surveyGroupId);
			
			for (Hashtable<String,String> ht : serviceSurveys) {
				Integer surveyId = Integer.parseInt(ht.get("survey_id"));
				if (surveyIdIn.length() > 0)
				{
					if (ht.get("survey_id").equals(surveyIdIn))
					{
						surveys.add(surveyId);
						surveyDataList.add(ht);
					}
				}
				else
				{
					surveys.add(surveyId);
					surveyDataList.add(ht);
				}				
			}
			
		} finally {
		}
		
		// Get the customer surveys
		try {						
			ArrayList<Hashtable<String,String>> customerSurveysHashtable = MetrixDatabaseManager.getFieldStringValuesList(String.format(baseQuery,MetrixSurveyRunner.TYPE_CUSTOMER));
			ArrayList<Hashtable<String,String>> customerSurveys = getQualifyingSurveysWithSurveyType(taskId,MetrixSurveyRunner.TYPE_CUSTOMER,customerSurveysHashtable,surveyGroupId);
			
			for (Hashtable<String,String> ht : customerSurveys) {
				Integer surveyId = Integer.parseInt(ht.get("survey_id"));
				if (surveyIdIn.length() > 0)
				{
					if (ht.get("survey_id").equals(surveyIdIn))
					{
						surveys.add(surveyId);
						surveyDataList.add(ht);
					}
				}
				else
				{
					surveys.add(surveyId);
					surveyDataList.add(ht);
				}
			}
			
		} finally {
		}
		
		// Get the regulatory surveys
		try {						
			ArrayList<Hashtable<String,String>> regulatorySurveysHashtable = MetrixDatabaseManager.getFieldStringValuesList(String.format(baseQuery,MetrixSurveyRunner.TYPE_REGULATORY));
			ArrayList<Hashtable<String,String>> regulatorySurveys = getQualifyingSurveysWithSurveyType(taskId,MetrixSurveyRunner.TYPE_REGULATORY,regulatorySurveysHashtable,surveyGroupId);
			
			for (Hashtable<String,String> ht : regulatorySurveys) {
				Integer surveyId = Integer.parseInt(ht.get("survey_id"));
				if (surveyIdIn.length() > 0)
				{
					if (ht.get("survey_id").equals(surveyIdIn))
					{
						surveys.add(surveyId);
						surveyDataList.add(ht);
					}
				}
				else
				{
					surveys.add(surveyId);
					surveyDataList.add(ht);
				}
			}
			
		} finally {
		}
		
		
		ArrayList<HashMap<String,String>> surveyDataMap = new ArrayList<HashMap<String,String>>();		
		if (surveyDataList != null && surveyDataList.size() > 0)
		{
			for (Hashtable<String,String> ht : surveyDataList) {				
				HashMap<String,String> map = new HashMap<String,String>();
				map.putAll(ht);
				surveyDataMap.add(map);
			}
		}
				
		
		return surveyDataMap;
	}
	
	public static Intent createSurveyIntentForTask(Activity activity, String taskId, String surveyGroupId, String surveyIdIn, String instanceDttmIn) {

		
		ArrayList<HashMap<String,String>> surveyHashMaps = getApplicableSurveys(taskId,surveyGroupId,surveyIdIn);
		
		if (surveyHashMaps != null && surveyHashMaps.size() > 0)
		{
			int[] ids  = new int[surveyHashMaps.size()];
			int i = 0;
			for (HashMap<String,String> hm : surveyHashMaps) {
				Integer surveyId = Integer.parseInt(hm.get("survey_id"));
				ids[i] = surveyId;
				i++;
			}
			
			Intent intent = MetrixActivityHelper.createActivityIntent(activity, MetrixSurveyActivity.class);
			Bundle surveyData = new Bundle();
			
			if (MetrixStringHelper.isNullOrEmpty(instanceDttmIn))
			{
				String instanceDttm = MetrixDateTimeHelper.getCurrentDate(MetrixDateTimeHelper.DATE_TIME_FORMAT_WITH_SECONDS, true);
				surveyData.putString(MetrixSurveyRunner.SURVEY_INSTANCE_DTTM,instanceDttm);
			}
			else
			{
				surveyData.putString(MetrixSurveyRunner.SURVEY_INSTANCE_DTTM,instanceDttmIn);
			}			
			surveyData.putIntArray(MetrixSurveyRunner.METRIX_SURVEY_IDS, ids);
			surveyData.putInt(MetrixSurveyRunner.METRIX_TASK_ID, Integer.valueOf(taskId));
			surveyData.putSerializable(MetrixSurveyRunner.SURVEY_INIT_DATA, surveyHashMaps);
			intent.putExtra(SurveyActivity.SURVEY_INITIALIZE_EXTRA, surveyData);
			return intent;
		}
		
		return null;
	}
		
	@SuppressLint("DefaultLocale")
	public boolean isSurveyShowOnce(String surveyGroup)
	{
		boolean isShowOnce = false;
		String taskId = MetrixCurrentKeysHelper.getKeyValue("task", "task_id");
		ArrayList<HashMap<String,String>> surveyHashMaps = getApplicableSurveys(taskId,surveyGroup,"");
		
		if (surveyHashMaps != null && surveyHashMaps.size() > 0)
		{			
			for (HashMap<String,String> hm : surveyHashMaps) {
				String showOnce = hm.get("show_once");
				if (showOnce != null && showOnce.toUpperCase().equals("Y"))
				{
					isShowOnce = true;
					break;
				}	
			}
		}
		
		return isShowOnce;
	}
	
	public boolean isSurveyComplete(String surveyGroup)	{
		String taskId = MetrixCurrentKeysHelper.getKeyValue("task", "task_id");
		ArrayList<HashMap<String,String>> surveyHashMaps = getApplicableSurveys(taskId,surveyGroup,"");
		
		if (surveyHashMaps != null && surveyHashMaps.size() > 0) {
			int[] ids  = new int[surveyHashMaps.size()];
			int i = 0;
			for (HashMap<String,String> hm : surveyHashMaps) {
				Integer surveyId = Integer.parseInt(hm.get("survey_id"));
				ids[i] = surveyId;
				i++;
			}
			
			ArrayList<MetrixQuestionData> questions = getQuestionsForSurveys(ids);
			for (MetrixQuestionData qd : questions) {
				if (qd.getAnswerType().equals(SurveyAnswerType.STATEMENT))
					continue;
				
				SurveyAnswerData sad = getPreviousAnswer(qd.getQuestionId());
				if (sad != null) {
					String answerText = sad.getAnswer();
					if (answerText != null) {
						//Do nothing. This question has been answered.
					} else if (MetrixStringHelper.valueIsEqual(qd.controlType.toUpperCase(), "ATTACHMENT") && sad.getAttachmentId() != 0) {
						//Do nothing. This attachment question has been answered.
					} else if(MetrixStringHelper.valueIsEqual(qd.controlType.toUpperCase(), "SIGNATURE") && sad.getAttachmentId() != 0) {

					} else {
						//This question has not been answered. Return false;
						return false;
					}
				} else {
					//This question has not been answered. Return false;
					return false;
				}
			}
		}
		
		return true;
	}
}

