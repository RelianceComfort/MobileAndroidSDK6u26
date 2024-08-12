package com.metrix.metrixmobile.survey;

import android.content.Context;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.metrixmobile.R;

import java.util.ArrayList;

public class SurveyRadioChoiceQuestion extends SurveyQuestion {
	private TextView mQuestion;
	private TextView mAnswerFeedback;
	private TextView mComment;
	private RadioGroup mRadioContainer;
	private Button mNext;
	private EditText mCommentTextField;
	private TextView mCommentLabel;
	private LinearLayout mCommentBlock;
	
	private boolean mUpdatingAnswer;
     
	public SurveyRadioChoiceQuestion(Context context) {
		super(context);
	}
	
	public SurveyRadioChoiceQuestion(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	public void onCreate() {
		super.onCreate();
		
		setContentView(R.layout.survey_radio_choice_question);
		
		mQuestion = (TextView)this.findViewById(R.id.survey_rcq_question);
		mAnswerFeedback = (TextView)this.findViewById(R.id.survey_rcq_feedback);
		mComment = (TextView)this.findViewById(R.id.survey_rcq_comment);

		mAnswerFeedback.setVisibility(GONE);
		mComment.setVisibility(GONE);
		
		mRadioContainer = (RadioGroup)this.findViewById(R.id.survey_rcq_radio_container);

		mCommentBlock = findViewById(R.id.survey_rcq_comment_block);
		mCommentLabel = findViewById(R.id.survey_rcq_comment_lbl);
		mCommentLabel.setText(AndroidResourceHelper.getMessage("CommentRequired"));
		mCommentTextField = findViewById(R.id.survey_rcq_comment_fld);
		mCommentTextField.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {}

			@Override
			public void afterTextChanged(Editable s) {
				getAnswer().setComment(s.toString());
				updateNextVisibility();
			}
		});

		mNext = (Button)this.findViewById(R.id.survey_rcq_continue);
		mNext.setText(AndroidResourceHelper.getMessage("Next"));
		mNext.setVisibility(GONE);
		mNext.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				next();
			}
		});

	}
	
	private void onAnswerChecked(RadioButton answer) {
		
		ArrayList<Integer> selectedItems = new ArrayList<Integer>();
		selectedItems.add((Integer)answer.getTag());
		getAnswer().setMultiChoiceSelections(selectedItems);
		
		MetrixQuestionData question = (MetrixQuestionData)getQuestion();
		String answerFeedback = "";
		if ((Integer)answer.getTag() >= 0)
		{
			answerFeedback = question.getAnswerFeedback((Integer)answer.getTag());
		}		
		if (answerFeedback != null && answerFeedback.length() > 0)
		{
			mAnswerFeedback.setText(AndroidResourceHelper.getMessage("Feedback1Args", answerFeedback));
			mAnswerFeedback.setVisibility(VISIBLE);
		}
		else
		{
			mAnswerFeedback.setText("");
			mAnswerFeedback.setVisibility(GONE);
		}
		
		boolean continueNext = true;
		
		String commentRequired = "N";
		if ((Integer)answer.getTag() >= 0)
		{
			commentRequired = question.getCommentRequired((Integer)answer.getTag());
		}
		if (commentRequired != null && commentRequired.length() > 0)
		{
			if (commentRequired.toUpperCase().equals("Y"))
			{
				continueNext = false;
				mCommentBlock.setVisibility(VISIBLE);
			} else {
				mCommentBlock.setVisibility(GONE);
			}
		}

		updateNextVisibility();
		
		if (continueNext == true)
		{
			updateNextVisibility();
			
			if(!isCompleted() && canAutoContinue()) {
				next();
			}
		}
	}
	
	private void updateNextVisibility() {
		MetrixQuestionData question = (MetrixQuestionData) getQuestion();
		
		if(question == null) {
			return;
		}
		
		int visibility = VISIBLE;

		if (isCompleted()) {
			visibility = GONE;
		} else  {
			
			SurveyAnswerData answer = getAnswer();
			if(answer == null) {
				visibility = GONE;
			} else if (answer.getMultiChoiceSelections() == null || 
					answer.getMultiChoiceSelections().length != 1){
				visibility = GONE;
			} else if (answer.getMultiChoiceSelections().length == 1 &&
					answer.getMultiChoiceSelections()[0] >= 0 &&
					question.getCommentRequired(answer.getMultiChoiceSelections()[0]).equalsIgnoreCase("Y") &&
					TextUtils.isEmpty(mCommentTextField.getText())) {
				visibility = GONE;
			}
		}

		mNext.setVisibility(visibility);
	}

	private void updateCommentVisibility() {
		MetrixQuestionData question = (MetrixQuestionData) getQuestion();
		int[] choices = getAnswer().getMultiChoiceSelections();
		boolean commentRequired = false;
		if (choices != null && choices.length == 1 && choices[0] >= 0) {
			commentRequired = "Y".equalsIgnoreCase(question.getCommentRequired(choices[0]));
		}
		if (commentRequired) {
			if (isCompleted()) {
				mComment.setText(AndroidResourceHelper.getMessage("Comments1Args", getAnswer().getComment()));
				mComment.setVisibility(VISIBLE);
				mCommentBlock.setVisibility(GONE);
			} else {
				mCommentTextField.setText(getAnswer().getComment());
				mCommentBlock.setVisibility(VISIBLE);
				mComment.setVisibility(GONE);
			}
		} else {
			mCommentBlock.setVisibility(GONE);
			mComment.setVisibility(GONE);
		}
	}
	
	@Override
	protected void onQuestionChanged(SurveyQuestionData newQuestion) {
		super.onQuestionChanged(newQuestion);
		
		String continueText = newQuestion.getContinueText();
		if (continueText == null) {
			continueText =AndroidResourceHelper.getMessage("Next");
		}
		
		mNext.setText(continueText);
		
		mQuestion.setText(newQuestion.getQuestion());
		
		updateOptions();
		updateNextVisibility();
	}
	
	private void updateOptions() {
		
		mRadioContainer.removeAllViews();
		
		SurveyQuestionData question = getQuestion();
		if(question != null && question.getChoiceAnswers() != null){
			
			CharSequence[] options = question.getChoiceAnswers();
			for (int i = 0; i < options.length; i++) {
				CharSequence option = options[i];
				addRadioButton(i, option);
			}
			
			if (question.isOptional()) {
				addRadioButton(-1,AndroidResourceHelper.getMessage("Skip"));
			}
		}
		
	}
	
//	@Override
//	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
//
//		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
//		
//		int totalWidth = 0;
//		int optionCount = mRadioContainer.getChildCount();
//		for (int i = 0; i < optionCount; i++) {
//			
//			RadioButton button = (RadioButton)mRadioContainer.getChildAt(i);
//			ViewGroup.MarginLayoutParams vlp = (MarginLayoutParams)button.getLayoutParams();
//			totalWidth += vlp.leftMargin + button.getMeasuredWidth() + vlp.rightMargin;
//		}
//		
//		int questionWidth = MeasureSpec.getSize(widthMeasureSpec);
//		
//		ViewGroup.MarginLayoutParams lp = (MarginLayoutParams)mNext.getLayoutParams();
//		mNext.measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED);
//		int nextWidth = lp.leftMargin + mNext.getMeasuredWidth() + lp.rightMargin;
//		
//		int availableWidth = questionWidth - nextWidth - 30;
//		
//		int oldOrientation = mRadioContainer.getOrientation();
//		int newOrientation = HORIZONTAL;
//		if (totalWidth > availableWidth) {
//			newOrientation = VERTICAL;
//		}
//		
//		if (oldOrientation != newOrientation) {
//			final int orientation = newOrientation;
//			mRadioContainer.setOrientation(orientation);
//		}
//	}
	

	private void addRadioButton(int i, CharSequence option) {
		final RadioButton button = new RadioButton(this.getContext());
		button.setTextAppearance(getContext(), R.style.RadioButton_Survey);
		button.setText(option);
		button.setTag(i);
//		button.setOnClickListener(new OnClickListener() {
//			@Override
//			public void onClick(View v) {
//				onAnswerChecked(button);
//			}
//		});
		button.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if (isChecked && !mUpdatingAnswer) {
					onAnswerChecked(button);
				}
			}
		});
		
		RadioGroup.LayoutParams layoutParams = new RadioGroup.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		int margin = (int)(15 * getContext().getResources().getDisplayMetrics().density);
		layoutParams.setMargins(0, 0, margin, 0);
		button.setLayoutParams(layoutParams);
		mRadioContainer.addView(button);
	}
	
	private void next()
	{
		setIsCompleted(true);

		if (getAnswer().getMultiChoiceSelections() != null && getAnswer().getMultiChoiceSelections()[0] == -1){
			getAnswer().setAnswer(AndroidResourceHelper.getMessage("SkippedAnswer"));
		}
	}
	
	@Override
	protected void onAnswerChanged(SurveyAnswerData answer) {
		super.onAnswerChanged(answer);
		
		mUpdatingAnswer = true;
		
		int[] choices = answer.getMultiChoiceSelections();
		if (choices != null && choices.length == 1) {
			
			int choice = choices[0];
			int optionCount = mRadioContainer.getChildCount();
			for (int i = 0; i < optionCount; i++) {
				
				RadioButton button = (RadioButton)mRadioContainer.getChildAt(i);
				int viewChoice = ((Integer)button.getTag()).intValue();	
				
				if(viewChoice == choice) {
					button.setChecked(true);
					break;
				}
			}
		}

		//handle all skip option
		if (choices == null && answer.mAnswer == AndroidResourceHelper.getMessage("SkippedAnswer")){
			int optionCount = mRadioContainer.getChildCount();
			RadioButton button = (RadioButton)mRadioContainer.getChildAt(optionCount-1);
			button.setChecked(true);
		}
		
		mUpdatingAnswer = false;

		updateCommentVisibility();
		updateNextVisibility();
	}
	
	@Override
	protected void onIsCompletedChanged(boolean isComplete) {
		super.onIsCompletedChanged(isComplete);
		
		int optionCount = mRadioContainer.getChildCount();
		boolean optionSelected = false;
		for (int i = 0; i < optionCount; i++) {
			
			RadioButton button = (RadioButton)mRadioContainer.getChildAt(i);
			button.setEnabled(!isComplete);
			if(button.isChecked())
				optionSelected = true;
		}

		//Handling Skipped Survey
		if(optionCount != 0){
			if(isCompleted() && canAutoContinue() && !optionSelected){
				if(!getQuestion().isOptional()){
					addRadioButton(-1,AndroidResourceHelper.getMessage("Skip"));
					optionCount++;
				}

				RadioButton button2 = (RadioButton)mRadioContainer.getChildAt(optionCount -1);
				button2.setChecked(true);
				button2.setEnabled(!isComplete);
			}
		}

		updateCommentVisibility();
		updateNextVisibility();
	}
}
