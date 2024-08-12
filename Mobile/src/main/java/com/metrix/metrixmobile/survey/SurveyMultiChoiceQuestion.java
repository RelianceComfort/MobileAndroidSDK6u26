package com.metrix.metrixmobile.survey;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.metrix.architecture.assistants.MetrixControlAssistant;
import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.architecture.utilities.LogManager;
import com.metrix.architecture.utilities.MetrixUIHelper;
import com.metrix.metrixmobile.R;

import java.util.ArrayList;

@SuppressLint("InflateParams")
public class SurveyMultiChoiceQuestion extends SurveyQuestion {
	
	private TextView mAnswerFeedback;
	private TextView mComment;
	private EditText mCommentText;
	private TextView mStatement;
	private TextView mSelected;
	private Button mSelect;
	private Button mSkip;
	private Button mNext;
	private EditText mCommentTextField;
	private TextView mCommentLabel;
	private LinearLayout mCommentBlock;
	private Button selectionDialogOkButton;
	
	private CharSequence[] mOptions = new CharSequence[0];
	private CharSequence[] mComments = new CharSequence[0];
	private boolean[] mSelections = new boolean[mOptions.length];
	private int mMaxChoicesToSelect = 1;
     
	public SurveyMultiChoiceQuestion(Context context) {
		super(context);
	}
	
	public SurveyMultiChoiceQuestion(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	public void onCreate() {
		super.onCreate();
		
		setContentView(R.layout.survey_multi_choice_question);
		
		mStatement = (TextView)this.findViewById(R.id.survey_mcq_text);        
        mSelected = (TextView)this.findViewById(R.id.survey_mcq_selections);
        
		mAnswerFeedback = (TextView)this.findViewById(R.id.survey_mcq_feedback);
		mComment = (TextView)this.findViewById(R.id.survey_mcq_comment);
		
		mAnswerFeedback.setVisibility(GONE);
		mComment.setVisibility(GONE);
        
        mSelect = (Button)this.findViewById(R.id.survey_mcq_select);
        mSelect.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				showSelectionDialog();
			}
		});
        
        mSkip = (Button)this.findViewById(R.id.survey_mcq_skip);
        mSkip.setText(AndroidResourceHelper.getMessage("Skip"));
        mSkip.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				skip();
			}
		});

		mCommentBlock = findViewById(R.id.survey_mcq_comment_block);
		mCommentLabel = findViewById(R.id.survey_mcq_comment_lbl);
		mCommentLabel.setText(AndroidResourceHelper.getMessage("CommentRequired"));
		mCommentTextField = findViewById(R.id.survey_mcq_comment_fld);
		mCommentTextField.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {}

			@Override
			public void afterTextChanged(Editable s) {
				if (!isCompleted()) {
					getAnswer().setComment(s.toString());
				}
				updateNextVisibility();
			}
		});

		mNext = (Button)this.findViewById(R.id.survey_mcq_next);
        mNext.setText(AndroidResourceHelper.getMessage("Next"));
        mNext.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				next();
			}
		});

		updateSelectionDisplay();
		updateFeedbackDisplay();
		try{
        	MetrixControlAssistant.setValue(findViewById(R.id.survey_mcq_select), AndroidResourceHelper.getMessage("Select"));
        }catch(Exception ex){
        	LogManager.getInstance().error(ex);
        }
	}

	private void showSelectionDialog() {

		AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getContext());
		if(mMaxChoicesToSelect == 1) {
			dialogBuilder
				.setTitle(AndroidResourceHelper.getMessage("Select"))
				.setItems(mOptions, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						
						for (int i = 0; i < mSelections.length; i++) {
							mSelections[i] = i == which;
						}
						
						onSelectionCompleted();	
						dialog.dismiss();
					}
				});
			dialogBuilder.create().show();
		} else {
			ScrollView sv = new ScrollView(getContext());
			LinearLayout ll = new LinearLayout(getContext());
			sv.addView(ll);
			dialogBuilder.setView(sv);

			final MetrixQuestionData questionData = (MetrixQuestionData) getQuestion();
			ll.setOrientation(VERTICAL);
			LayoutInflater inflater = mActivityInfo.getLayoutInflater();

			for (int i = 0; i < mOptions.length; i++) {
				final int itemIndex = i;
				LinearLayout view = (LinearLayout) inflater.inflate(R.layout.survey_multi_choice_selection_item, ll, false);
				final CheckedTextView optionText = view.findViewById(R.id.optionText);
				LinearLayout commentRequiredBlock = view.findViewById(R.id.commentRequiredBlock);
				TextView commentRequiredLabel = view.findViewById(R.id.commentRequiredLabel);
				EditText commentRequiredField = view.findViewById(R.id.commentField);

				commentRequiredLabel.setText(AndroidResourceHelper.getMessage("CommentRequired"));
				optionText.setText(mOptions[i]);
				optionText.setChecked(mSelections[i]);
				if (mSelections[i] && "Y".equalsIgnoreCase(questionData.getCommentRequired(itemIndex))) {
					commentRequiredBlock.setVisibility(VISIBLE);
					commentRequiredField.setText(mComments[i]);
				}

				optionText.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						mSelections[itemIndex] = !mSelections[itemIndex];
						optionText.setChecked(mSelections[itemIndex]);
						commentRequiredBlock.setVisibility("Y".equals(questionData.getCommentRequired(itemIndex)) && mSelections[itemIndex] ? VISIBLE : GONE);
						updateSelectionDialogOkButton();
					}
				});

				commentRequiredField.addTextChangedListener(new TextWatcher() {
					@Override
					public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

					@Override
					public void onTextChanged(CharSequence s, int start, int before, int count) {}

					@Override
					public void afterTextChanged(Editable s) {
						mComments[itemIndex] = commentRequiredField.getText();
						updateSelectionDialogOkButton();
					}
				});

				ll.setDescendantFocusability(FOCUS_AFTER_DESCENDANTS);
				ll.addView(view);
			}

			dialogBuilder
				.setTitle(AndroidResourceHelper.getMessage("SelectUpTo", mMaxChoicesToSelect))
				.setPositiveButton(AndroidResourceHelper.getMessage("OK"), new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						
						SurveyQuestionData question = getQuestion();
						
						if(question != null && !question.isOptional()) {
							int checkedItems = 0;
				            for (int i = 0; i < mSelections.length; i++) {
								if(mSelections[i]) {
									checkedItems++;
								}
							}
				            
				            if (checkedItems == 0)
				            {
				            	MetrixUIHelper.showSnackbar((Activity) getContext(),AndroidResourceHelper.getMessage("PleaseSelect"));
				            	showSelectionDialog();
				            	return;
				            }
						}

						onSelectionCompleted();
						selectionDialogOkButton = null;
					}
				} );
			AlertDialog d = dialogBuilder.show();
			d.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
			selectionDialogOkButton = d.getButton(AlertDialog.BUTTON_POSITIVE);
		}
	}

	private void updateSelectionDialogOkButton() {
		if (selectionDialogOkButton != null) {
			boolean shouldEnable = true;
			MetrixQuestionData question = (MetrixQuestionData) getQuestion();
			for (int i = 0; i < mSelections.length; i++) {
				if (mSelections[i] && "Y".equalsIgnoreCase(question.getCommentRequired(i)) && TextUtils.isEmpty(mComments[i])) {
					shouldEnable = false;
					break;
				}
			}
			selectionDialogOkButton.setEnabled(shouldEnable);
		}
	}

	private void onSelectionCompleted() {
		
		updateSelectionDisplay();
		updateFeedbackDisplay();

		ArrayList<Integer> selectedItems = new ArrayList<Integer>();
		
		for (int i = 0; i < mSelections.length; i++) {
			if(mSelections[i]) {
				selectedItems.add(i);
			}
		}

		SurveyAnswerData answer = getAnswer();
		answer.setMultiChoiceSelections(selectedItems);

		boolean continueNext = true;
		MetrixQuestionData question = (MetrixQuestionData) getQuestion();
		if (mMaxChoicesToSelect > 1) {
			for (int i = 0; i < mComments.length; i++) {
				if (!TextUtils.isEmpty(mComments[i])) {
					answer.setMultiChoiceComment(question.getAnswerChoice(i).answerId, String.valueOf(mComments[i]));
				}
			}
		} else if (selectedItems.size() == 1) {
			String commentRequired = question.getCommentRequired(selectedItems.get(0));
			if (commentRequired != null && commentRequired.length() > 0)
			{
				if (commentRequired.toUpperCase().equals("Y"))
				{
					continueNext = false;
					mCommentBlock.setVisibility(VISIBLE);
				} else {
					mCommentBlock.setVisibility(GONE);
					getAnswer().setComment("");
				}
			}
		}

		if (continueNext && canAutoContinue())
		{
			next();
		} else {
			updateNextVisibility();
		}
	}
	
	private void updateSelectionDisplay() {
		StringBuilder sb = new StringBuilder();
		
		if (mSelections != null) {
			for (int i = 0; i < mSelections.length; i++) {
				if(mSelections[i]) {
					
					if(sb.length() > 0) {
						sb.append(", ");
					}
					
					sb.append(mOptions[i]);
				}
			}
		}
		
		if(sb.length() == 0) {
			if(isCompleted() && canAutoContinue()){
				sb.append(AndroidResourceHelper.getMessage("SkippedAnswer"));
				getAnswer().setAnswer(AndroidResourceHelper.getMessage("SkippedAnswer"));
			}else{
				sb.append(AndroidResourceHelper.getMessage("NoneSelected"));
			}
		}
		
		mSelected.setText(sb);
	}

	private void updateFeedbackDisplay() {
		MetrixQuestionData question = (MetrixQuestionData) getQuestion();
		StringBuilder sb = new StringBuilder();
		boolean feedbackFound = false;
		if (mSelections != null) {
			for (int i = 0; i < mSelections.length; i++) {
				if (mSelections[i]) {
					String feedback = question.getAnswerFeedback(i);
					if (!TextUtils.isEmpty(feedback)) {
						if (feedbackFound) {
							sb.append(", ");
						}
						sb.append(feedback);
						feedbackFound = true;
					}
				}
			}
		}

		if (feedbackFound) {
			mAnswerFeedback.setText(AndroidResourceHelper.getMessage("Feedback1Args", sb.toString()));
			mAnswerFeedback.setVisibility(VISIBLE);
		} else {
			mAnswerFeedback.setText("");
			mAnswerFeedback.setVisibility(GONE);
		}
	}

	private void updateCommentDisplay() {
		MetrixQuestionData question = (MetrixQuestionData) getQuestion();
		if (isCompleted() || mMaxChoicesToSelect > 1) {
			mCommentBlock.setVisibility(GONE);
			boolean commentsFound = false;
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < mSelections.length; i++) {
				if (mSelections[i]) {
					if (mMaxChoicesToSelect == 1 && "Y".equalsIgnoreCase(question.getCommentRequired(i))) {
						String comment = getAnswer().getComment();
						if (!TextUtils.isEmpty(comment)) {
							sb.append(comment);
							commentsFound = true;
						}
						break;
					} else if (mMaxChoicesToSelect > 1 && "Y".equalsIgnoreCase(question.getCommentRequired(i))) {
						if (commentsFound) {
							sb.append(", ");
						}
						if (!TextUtils.isEmpty(mComments[i])) {
							sb.append(mComments[i]);
							commentsFound = true;
						}
					}
				}
			}
			if (commentsFound) {
				mComment.setText(AndroidResourceHelper.getMessage("Comments1Args", sb.toString()));
				mComment.setVisibility(VISIBLE);
			} else {
				mComment.setText("");
				mComment.setVisibility(GONE);
			}
		} else if (mMaxChoicesToSelect == 1) {
			mComment.setVisibility(GONE);
			for (int i = 0; i < mSelections.length; i++) {
				if (mSelections[i] && "Y".equalsIgnoreCase(question.getCommentRequired(i))) {
					mCommentTextField.setText(getAnswer().getComment());
					mCommentBlock.setVisibility(VISIBLE);
					break;
				}
			}
		} else {
			mComment.setVisibility(GONE);
		}
	}

	private void updateOptions() {
		SurveyQuestionData question = getQuestion();
		if(question == null || question.getChoiceAnswers() == null){
			mOptions = new CharSequence[0];
			mSelections =  new boolean[mOptions.length];
			mComments = new CharSequence[mOptions.length];
			mMaxChoicesToSelect = 1;
		} else {
			mOptions = question.getChoiceAnswers();
			mSelections =  new boolean[mOptions.length];
			mComments = new CharSequence[mOptions.length];
			mMaxChoicesToSelect = question.getMaxChoiceAnswersToSelect();
		}
	}

	private void updateSkipVisibility() {
		SurveyQuestionData question = getQuestion();
		
		if(question == null) {
			return;
		}
		
		int visibility = GONE;
		
		if(!isCompleted() && question.isOptional()) {
			visibility = VISIBLE;
		}
		
		mSkip.setVisibility(visibility);
	}
	
	private void updateNextVisibility() {
		MetrixQuestionData question = (MetrixQuestionData) getQuestion();
		
		if(question == null) {
			return;
		}
		
		int visibility = VISIBLE;
	
		SurveyAnswerData answer = getAnswer();
		
		if (isCompleted()) {
			visibility = GONE;
		} else if(answer == null) {
			visibility = GONE;
		} else if (mMaxChoicesToSelect == 1 &&
				answer.getMultiChoiceSelections() != null && answer.getMultiChoiceSelections().length == 1 &&
				"Y".equalsIgnoreCase(question.getCommentRequired(answer.getMultiChoiceSelections()[0])) &&
				TextUtils.isEmpty(answer.getComment())) {
			visibility = GONE;
		} else {
			Boolean requiresChoice = canAutoContinue() || !question.isOptional();
			if (requiresChoice) {
				int[] choices = answer.getMultiChoiceSelections();
				if(choices == null || choices.length == 0) {
					visibility = GONE;
				}
			}			
		}		
				
		mNext.setVisibility(visibility);
	}
	
	private void skip() {
		
		for (int i = 0; i < mSelections.length; i++) {
			mSelections[i] = false;
		}

		onSelectionCompleted();
	}
	
	private void next() {
		setIsCompleted(true);
	}
	
	@Override
	protected void onQuestionChanged(SurveyQuestionData newQuestion) {
		super.onQuestionChanged(newQuestion);
		
		String continueText = newQuestion.getContinueText();
		if (continueText == null) {
			continueText = AndroidResourceHelper.getMessage("Next");
		}
		
		mNext.setText(continueText);
		
		mStatement.setText(newQuestion.getQuestion());
		updateOptions();
		updateSkipVisibility();
		updateNextVisibility();
	}
	
	@Override
	protected void onAnswerChanged(SurveyAnswerData answer) {
		super.onAnswerChanged(answer);
		
		int[] choices = answer.getMultiChoiceSelections();

		for (int i = 0; i < mSelections.length; i++) {
			mSelections[i] = false;
			if(choices != null) {
				for (int j = 0; j < choices.length; j++) {
					if(choices[j] == i) {
						mSelections[i] = true;
						break;
					}
				}
			}
		}

		if (mMaxChoicesToSelect == 1) {
			mCommentTextField.setText(answer.getComment());
		} else {
			MetrixQuestionData question = (MetrixQuestionData) getQuestion();
			for (int i = 0; i < mOptions.length; i++) {
				int answerId = question.getAnswerChoice(i).answerId;
				mComments[i] = answer.getMultiChoiceComment(answerId);
			}
		}

		updateSelectionDisplay();
		updateFeedbackDisplay();
		updateFeedbackDisplay();
		updateCommentDisplay();
		updateNextVisibility();
	}
	
	@Override
	protected void onIsCompletedChanged(boolean isComplete) {
		super.onIsCompletedChanged(isComplete);
		mSelect.setVisibility(isComplete ? GONE : VISIBLE);
		updateSkipVisibility();
		updateNextVisibility();
		updateSelectionDisplay();
		updateFeedbackDisplay();
		updateCommentDisplay();
	}
}