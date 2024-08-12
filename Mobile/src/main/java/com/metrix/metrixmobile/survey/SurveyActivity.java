package com.metrix.metrixmobile.survey;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.EditText;
import android.widget.TextView;

import com.metrix.architecture.assistants.MetrixBarcodeAssistant;
import com.metrix.architecture.database.MetrixDatabaseManager;
import com.metrix.architecture.metadata.MetrixColumnDef;
import com.metrix.architecture.signature.SignatureField;
import com.metrix.architecture.utilities.MetrixPublicCache;
import com.metrix.architecture.utilities.MetrixStringHelper;

import com.metrix.architecture.ui.widget.ObservableScrollView;
import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.architecture.utilities.MetrixActionView;
import com.metrix.metrixmobile.R;
import com.metrix.metrixmobile.survey.SurveyQuestionData.SurveyAnswerType;
import com.metrix.metrixmobile.system.DebriefActivity;

public abstract class SurveyActivity extends DebriefActivity implements View.OnClickListener, ObservableScrollView.ScrollViewListener {
	
	public static String SURVEY_INITIALIZE_EXTRA = "EXTRA_SURVEY_INITIALIZE";
	public static String SURVEY_RESULT_EXTRA = "EXTRA_SURVEY_RESULT";
	private final String SURVEY_SAVED_ANSWERS = "SURVEY_SAVED_ANSWERS";
	private final int BARCODE_SCAN_MENU_ID = 5;
	private ViewGroup mSurveyContainer;
	private SurveyRunner mSurveyRunner;
	private Bundle mSavedAnswers;
	private Bundle mSurveyInitData;
	@SuppressWarnings("unused")
	private ArrayList<Hashtable<String,String>> mSurveyInitDataList;
	private Boolean mExitOnBack;
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if(shouldRunTabletSpecificUIMode)
			setContentView(R.layout.tb_land_survey);
		else
			setContentView(R.layout.survey);
		mSurveyContainer = (ViewGroup)findViewById(R.id.survey_question_container);
		
		ObservableScrollView scrollView = (ObservableScrollView)findViewById(R.id.survey_scrollview);
		scrollView.setScrollViewListener(this);

		if (savedInstanceState != null) {
			mSavedAnswers = savedInstanceState.getBundle(SURVEY_SAVED_ANSWERS);
		}
		
		if(mSavedAnswers == null) {
			mSavedAnswers = new Bundle();
		}
				
		mSurveyInitData = getIntent().getBundleExtra(SURVEY_INITIALIZE_EXTRA);
		if(mSurveyInitData == null) {
			mSurveyInitData = new Bundle();
		}
		
		mSurveyRunner = createSurveyRunner(mSurveyInitData);
	}
		
	public void onStart() {
		super.onStart();
		this.mLayout = (ViewGroup) findViewById(R.id.table_layout);
		this.helpText = AndroidResourceHelper.getMessage("ThisScreenAllowsYouTo");
		
		continueSurvey();
		mExitOnBack = true;
	}
	
	private void continueSurvey() {
		
		if(this.isFinishing()) {
			return;			
		}
		
		for (int i = 0; i < mSurveyContainer.getChildCount(); i++) {
			Object view = mSurveyContainer.getChildAt(i);
			if(view instanceof SurveyQuestion) {
				SurveyQuestion question = (SurveyQuestion)view;				
				if (mSurveyContainer.getChildAt(i - 1) != null)
				{
					Object previousView = mSurveyContainer.getChildAt(i);
					if(previousView instanceof SurveySectionView)
					{
						MetrixQuestionData qd = (MetrixQuestionData)question.getQuestion();
						boolean allowSkipAll = qd.getSection().getAllowSkipAll();
						if (allowSkipAll)
						{
							SurveySectionView sectionView = (SurveySectionView)previousView;
							ImageView skipAllButton = sectionView.getSkipAllButton();
							skipAllButton.setVisibility(View.VISIBLE);
						}
					}
				}
				if(!question.isCompleted()) {
					question.requestFocus();
					return;				
				}	
			}
		}
		
		SurveyQuestionData qd = mSurveyRunner.getNextQuestion();
		
		if(qd == null) {
			Bundle surveyResult = new Bundle();
			boolean success = mSurveyRunner.complete(surveyResult);
			final ComponentName caller = getCallingActivity();

			if (caller != null) {
				// This activity was called by another activity to obtain a result. Pass the result back to the caller and finish
				Intent resultIntent = new Intent();
				resultIntent.putExtra(SURVEY_RESULT_EXTRA, surveyResult);
				setResult(success ? Activity.RESULT_OK : Activity.RESULT_CANCELED, resultIntent);
				finish();
			} else {
				// This activity was opened normally (probably through jump menu for quick edits). Just go back to the caller
				onBackPressed();
			}
			
		} else {
			SurveyQuestion newQuestion = createQuestionView(qd);
			addQuestion(newQuestion);
			
			SurveyAnswerData previousAnswer = getPreviousAnswer(qd);
			if(previousAnswer != null) {
				if (qd.getAnswerType() == SurveyAnswerType.DATE)
				{
					//String answerDt = MetrixDateTimeHelper.convertDateTimeFromDBToUI(previousAnswer.getAnswer(), MetrixDateTimeHelper.DATE_FORMAT);
					//previousAnswer.setAnswer(answerDt);
					newQuestion.setAnswer(previousAnswer);
				}
				else
				{
					newQuestion.setAnswer(previousAnswer);
				}				
			}
			else
			{
				if (qd.getAnswerType() == SurveyAnswerType.STATEMENT)
				{
					MetrixQuestionData mqd = (MetrixQuestionData)qd;
					if (mqd.isOptional())
					{
						SurveyAnswerData answer = new SurveyAnswerData();
						answer.setAnswer(AndroidResourceHelper.getMessage("StatementOnly"));
						newQuestion.setAnswer(answer);
					}
				}
			}
			
			if (qd.getAnswerType() == SurveyAnswerType.OPEN_ENDED || qd.getAnswerType() == SurveyAnswerType.NUMBER)
			{
				if (newQuestion instanceof SurveyOpenEndedQuestion)
				{
					SurveyOpenEndedQuestion openEndedQ = (SurveyOpenEndedQuestion)newQuestion;
					EditText editText = openEndedQ.getEditText();
					wireupEditTextForBarcodeScanning(editText);
				}
				
				if (newQuestion instanceof SurveyNumericQuestion)
				{
					SurveyNumericQuestion openEndedQ = (SurveyNumericQuestion)newQuestion;
					EditText editText = openEndedQ.getEditText();
					wireupEditTextForBarcodeScanning(editText);
				}
			}

			if(qd.getAnswerType() == SurveyAnswerType.SIGNATURE)
			{
				if(newQuestion instanceof SurveySignatureQuestion)
				{
					SurveySignatureQuestion surveySignatureQuestion = (SurveySignatureQuestion)newQuestion;

					if(MetrixPublicCache.instance.containsKey("LaunchingSignatureFieldValue")){
						HashMap<String, Object> launchingSignatureFieldValue = (HashMap<String, Object>)MetrixPublicCache.instance.getItem("LaunchingSignatureFieldValue");
						String launchingSignatureQuestionId = "";
						if(launchingSignatureFieldValue.containsKey("QuestionId"))
							launchingSignatureQuestionId = (String)launchingSignatureFieldValue.get("QuestionId");
						String launchingSignatureAttachmentId = (String)launchingSignatureFieldValue.get("AttachmentId");

						if(launchingSignatureQuestionId.equals(surveySignatureQuestion.getQuestion().getQuestionId())) {
							SignatureField signatureField = (SignatureField) surveySignatureQuestion.mSignatureField;
							signatureField.mHiddenAttachmentIdTextView.setText(launchingSignatureAttachmentId);
							MetrixColumnDef launchingSignatureColumnDef = new MetrixColumnDef();
							launchingSignatureColumnDef.readOnlyInMetadata = false;
							launchingSignatureColumnDef.allowClear = true;
							launchingSignatureColumnDef.readOnly = false;
							launchingSignatureColumnDef.messageId = "";
							launchingSignatureColumnDef.transactionIdTableName = "task";
							launchingSignatureColumnDef.transactionIdColumnName = "task_id";
							signatureField.setupFromConfiguration(this, launchingSignatureColumnDef, "survey_result");
						}
					}
				}
			}

			if (qd.isFinalStatement()) {
				// Close the keyboard on a final question so the user can review
				InputMethodManager keyboard = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
				keyboard.hideSoftInputFromWindow(mSurveyContainer.getWindowToken(), 0);
			}
			
			// Continue the survey to make sure the correct question is focused
			// This question may have been answered creating another question
			continueSurvey();
		}
	}
	
	private void addQuestion(SurveyQuestion question) {
		
		question.setOnIsCompletedChangedListener(new SurveyQuestion.OnIsCompletedChangedListener() {
			@Override
			public void onIsCompletedChanged(SurveyQuestion view, boolean isComplete) {
				onQuestionIsCompletedChanged(view, isComplete);
			}
		});
		
		question.setOnBackPressedListener(new SurveyQuestion.OnBackPressedListener() {
			@Override
			public boolean onBackPressed(SurveyQuestion view) {
				return onQuestionBackPressed();
			}
		});		
		
		MetrixQuestionData mqd = (MetrixQuestionData)question.getQuestion();
		if (mqd.survey != null){
			if (mqd.survey.lock_results && mqd.survey.all_answered)
			{
				//Do not allow the user to modify the answer. Answers are locked when the survey is completed.
			}
			else
			{
				registerForMetrixActionView(question, getMetrixActionBar().getCustomView());
			}
		}
		else
		{
			registerForMetrixActionView(question, getMetrixActionBar().getCustomView());
		}
		
		SurveySection section = question.getQuestion().getSection();
		SurveyQuestion previousQuestion = getPreviousQuestion(0);
		if (previousQuestion == null || !SurveySection.equals(previousQuestion.getQuestion().getSection(), section)) {
		
			if(section != null) {
				SurveySectionView sectionView = new SurveySectionView(this);
				ImageView mAllowSkipAll = (ImageView)sectionView.getSkipAllButton(); 
				mAllowSkipAll.setOnClickListener(this);
				mAllowSkipAll.setTag(mqd.survey.id);
				sectionView.setSection(section);
				mSurveyContainer.addView(sectionView);
				section.setSkipAllButton(mAllowSkipAll);
			}
		}
		else
		{
			//Only give the option to skip all after the first question has been shown. Hide after that.
			ImageView mAllowSkipAll = section.getSkipAllButton();
			if (mAllowSkipAll != null)
				mAllowSkipAll.setVisibility(View.GONE);
		}
		
		mSurveyContainer.addView(question);
	}
	
	@Override
	public void onClick(View v) {
		
		//Give a blank answer for every question in this survey.				
		SurveyAnswerData answer = new SurveyAnswerData();
		answer.setAnswer(AndroidResourceHelper.getMessage("SkippedAnswer"));
		
		int surveyIdToSkip = (Integer)((ImageView)v).getTag();
		
		boolean hasQuestion = true;
		while (hasQuestion == true)
		{
			for (int i = 0; i < mSurveyContainer.getChildCount(); i++) {
				Object view = mSurveyContainer.getChildAt(i);
				if(view instanceof SurveyQuestion) {					
					SurveyQuestion currentQuestion = (SurveyQuestion)view;
					MetrixQuestionData currentQd = (MetrixQuestionData)currentQuestion.getQuestion();
					
					if (currentQd != null && currentQd.survey != null)
					{
						if (currentQd.survey.id == surveyIdToSkip)
						{
							if(!currentQuestion.isCompleted()) {
								currentQuestion.setAnswer(answer);		
								currentQuestion.setIsCompleted(true);
							}
						}
						else
						{
							hasQuestion = false;
						}
					}
					else
					{
						hasQuestion = false;
					}
				}
			}
			
			hasQuestion = false;
			
			// Close the keyboard on a final question so the user can review
			InputMethodManager keyboard = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
			keyboard.hideSoftInputFromWindow(mSurveyContainer.getWindowToken(), 0);
		}
	}
		
	
	private void onQuestionIsCompletedChanged(SurveyQuestion view, boolean isComplete) {
		if (isComplete) {
			mExitOnBack = false;
			mSurveyRunner.provideAnswer(view.getQuestion().getQuestionId(), view.getAnswer());
			continueSurvey();
		}
	}
	
	private boolean onQuestionBackPressed() {
		
		SurveyQuestion previousQuestion = getPreviousQuestion(1);
    	
		if(!mExitOnBack && previousQuestion != null) {
	    	revertToQuestion(previousQuestion);
	        return true;
    	}
    	
    	return false;
	}
	
	private int getQuestionCount() {
		
		int count = 0;
		for (int i = 0; i < mSurveyContainer.getChildCount(); i++) {
			Object view = mSurveyContainer.getChildAt(i);
			if(view instanceof SurveyQuestion) {
				count++;	
			}
		}
		
		return count;
	}
	
	private SurveyAnswerData getPreviousAnswer(SurveyQuestionData questionData) {
		Bundle savedAnswer = mSavedAnswers.getBundle(questionData.getQuestionId());
		
		if(savedAnswer != null) {
			SurveyAnswerData answer = new SurveyAnswerData();
			answer.restore(savedAnswer);
			return answer;
		}
		
		return mSurveyRunner.getPreviousAnswer(questionData.getQuestionId());
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		
		for (int i = 0; i < mSurveyContainer.getChildCount(); i++) {
			Object view = mSurveyContainer.getChildAt(i);
			if(view instanceof SurveyQuestion) {
				SurveyQuestion question = (SurveyQuestion)view;
				saveAnswer(question);
			}
		}
		
		outState.putBundle(SURVEY_SAVED_ANSWERS, mSavedAnswers);
	}
	
	private void saveAnswer(SurveyQuestion question) {
		Bundle savedAnswer = new Bundle();
		question.getAnswer().save(savedAnswer);
		mSavedAnswers.putBundle(question.getQuestion().getQuestionId(), savedAnswer);
	}
	
	private void revertToQuestion(SurveyQuestion question) {
	
		int surveyItems = mSurveyContainer.getChildCount();
		int currentIndex = surveyItems - 1;
		while (currentIndex > 0) {
			
			Object view = mSurveyContainer.getChildAt(currentIndex);
			if(view instanceof SurveyQuestion) {
				SurveyQuestion currentQuestion = (SurveyQuestion)view;
				
				if(currentQuestion == question) {
					currentQuestion.setIsCompleted(false);
					currentQuestion.requestFocus();
					//re-enable skip all if this is the first question in the section
					boolean allowSkipAll = currentQuestion.getQuestion().getSection().getAllowSkipAll();
					if (allowSkipAll)
					{
						int previousViewIndex = currentIndex - 1;
						if (previousViewIndex >= 0)
						{
							Object previousView = mSurveyContainer.getChildAt(previousViewIndex);
							if(previousView instanceof SurveySectionView)
							{
								SurveySectionView secView = (SurveySectionView)previousView;
								ImageView mAllowSkipAll = (ImageView)secView.getSkipAllButton();
								mAllowSkipAll.setVisibility(View.VISIBLE);
							}
						}
					}
			    	break;
				} else {
					SurveyAnswerData answer = currentQuestion.getAnswer();
					answer.setIsCompleted(false);
		    		saveAnswer(currentQuestion);
		    		mSurveyRunner.cancelQuestion(currentQuestion.getQuestion().getQuestionId());
				}
			}
			
			mSurveyContainer.removeViewAt(currentIndex);
			
			currentIndex--;
		}
		
		mExitOnBack = false;
		continueSurvey();
	}

	@Override
	public void onScrollChanged(ObservableScrollView scrollView, int x, int y, int oldx, int oldy) {
		if(oldy > y) {
			InputMethodManager keyboard = (InputMethodManager)scrollView.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
			keyboard.hideSoftInputFromWindow(scrollView.getWindowToken(), 0);
		}
	}
	
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		
	    if (keyCode == KeyEvent.KEYCODE_BACK) {
	    	if(!mExitOnBack && getQuestionCount() > 1) {
	    		return true;
	    	}
	    }
	    
	    return super.onKeyDown(keyCode, event);
	}
	
	private SurveyQuestion getPreviousQuestion(int offset) {
		
		for (int i = mSurveyContainer.getChildCount() - 1; i >= 0; i--) {
			Object view = mSurveyContainer.getChildAt(i);
			if(view instanceof SurveyQuestion) {
				if(offset == 0) {
					return (SurveyQuestion)view;
				}	
				offset--;
			}
		}
		
		return null;
	}
		
	protected abstract SurveyRunner createSurveyRunner(Bundle initData);
	
	protected SurveyQuestion createQuestionView(SurveyQuestionData questionData){
		 return SurveyQuestion.createForQuestionData(this, questionData);
	}
	
	@Override
	public boolean OnCreateMetrixActionView(View view, Integer... position) {
		if(view instanceof SurveyQuestion){
			
			MetrixActionView metrixActionView;
			final SurveyQuestion question = (SurveyQuestion)view;
			if(question.isCompleted()) {				
				metrixActionView = getMetrixActionView();
				Menu menu = metrixActionView.getMenu();
				menu.clear();	// make sure that the item added below is the only item in the context menu
				MenuItem menuItem = menu.add(Menu.NONE, mSurveyContainer.indexOfChild(view), 0, AndroidResourceHelper.getMessage("ModifyAnswer"));
				
				menuItem.setOnMenuItemClickListener(new OnMenuItemClickListener() {
					@Override
					public boolean onMenuItemClick(MenuItem item) {
						revertToQuestion(question);
						return false;
					}
				});
				
				//Didn't call the super method, forced me to do a slight change due to the special behavior in Survey screen(s)
				metrixActionView.show();
				return true;
			}
		}
		else if (view instanceof EditText){
			String barcodingEnabled = MetrixDatabaseManager.getFieldStringValue("metrix_app_params", "param_value", "param_name='ENABLE_BARCODE_SCANNING'");
			if (!MetrixStringHelper.isNullOrEmpty(barcodingEnabled) && barcodingEnabled.compareToIgnoreCase("Y") == 0) {
				MetrixActionView metrixActionView = getMetrixActionView();
				Menu menu = metrixActionView.getMenu();
				if(!menu.hasVisibleItems() || menu.findItem(BARCODE_SCAN_MENU_ID) == null ) {
					MenuItem menuItem = menu.add(0, BARCODE_SCAN_MENU_ID, 0, AndroidResourceHelper.getMessage("ScanBarcode"));
					menuItem.setOnMenuItemClickListener(new OnMenuItemClickListener() {
						@Override
						public boolean onMenuItemClick(MenuItem item) {
							MetrixBarcodeAssistant.scanBarcode((TextView) view);
							return false;
						}
					});
				}
	            MetrixPublicCache.instance.addItem("METRIX_VIEW_DISPLAYING_CONTEXT_MENU", view.getId());
	
	            metrixActionView.show();
	            return true;
			}
		}
		return false;
	}
	
	//Tablet UI Optimization
	@Override
	public boolean isTabletSpecificLandscapeUIRequired()
	{
		return true;
	}
	//End Tablet UI Optimization
		
}
