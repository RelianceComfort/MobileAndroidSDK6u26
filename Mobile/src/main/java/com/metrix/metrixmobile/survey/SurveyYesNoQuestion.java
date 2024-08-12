package com.metrix.metrixmobile.survey;

import com.metrix.metrixmobile.R;
import com.metrix.metrixmobile.survey.SurveyQuestionData.SurveyRemarkType;
import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.architecture.utilities.MetrixStringHelper;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.TextView.OnEditorActionListener;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;

public class SurveyYesNoQuestion extends SurveyQuestion {
	
	public final static String ANSWER_NO = "NO";
	public final static String ANSWER_YES = "YES";
	public final static String ANSWER_SKIP = "SKIP";
	
	private TextView mQuestion;
	private EditText mRemark;
	private Button mNext;
	private RadioButton mAnswerYes;
	private RadioButton mAnswerNo;
	private RadioButton mAnswerSkip;
	private boolean mUpdatingAnswer = false;
	
	
	public SurveyYesNoQuestion(Context context) {
		super(context);
	}
	
	public SurveyYesNoQuestion(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	public void onCreate() {
		super.onCreate();
		
		setContentView(R.layout.survey_yes_no_question);
		
		mQuestion = (TextView)this.findViewById(R.id.survey_ynq_question);
		
		mRemark = (EditText)this.findViewById(R.id.survey_ynq_remark);
		mRemark.setVisibility(GONE);
		mRemark.setOnFocusChangeListener(new OnFocusChangeListener() {
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if (hasFocus && !isCompleted()) {
					InputMethodManager keyboard = (InputMethodManager)getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
					keyboard.showSoftInput(mRemark, 0);
		        }
			}
		});
		mRemark.addTextChangedListener(new TextWatcher() {
			
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
			}
			
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}
			
			@Override
			public void afterTextChanged(Editable s) {
				updateNextEnabled();
			}
		});
		mRemark.setOnEditorActionListener(new OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				if(actionId == EditorInfo.IME_ACTION_NEXT) {
					if(mNext.isEnabled()) {
						next();
					} else {
						return false;
					}
				}
				return true;
			}
		});
		
		mNext = (Button)this.findViewById(R.id.survey_ynq_continue);
		mNext.setText(AndroidResourceHelper.getMessage("Next"));
		mNext.setVisibility(GONE);
		mNext.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				next();
			}
		});
		
		
		mAnswerYes = (RadioButton)this.findViewById(R.id.survey_ynq_yes);
		mAnswerYes.setText(AndroidResourceHelper.getMessage("Yes"));
		mAnswerYes.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				onAnswerChecked(ANSWER_YES);
			}
		});
		mAnswerYes.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if (isChecked && !mUpdatingAnswer) {
					onAnswerChecked(ANSWER_YES);
				}
			}
		});
		
		mAnswerNo = (RadioButton)this.findViewById(R.id.survey_ynq_no);
		mAnswerNo.setText(AndroidResourceHelper.getMessage("No"));
		mAnswerNo.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				onAnswerChecked(ANSWER_NO);
			}
		});
		mAnswerNo.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if (isChecked && !mUpdatingAnswer) {
					onAnswerChecked(ANSWER_NO);
				}
			}
		});
		
		mAnswerSkip = (RadioButton)this.findViewById(R.id.survey_ynq_skip);
		mAnswerSkip.setText(AndroidResourceHelper.getMessage("Skip"));
		mAnswerSkip.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				onAnswerChecked(ANSWER_SKIP);
			}
		});
		mAnswerSkip.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if (isChecked && !mUpdatingAnswer) {
					onAnswerChecked(ANSWER_SKIP);
				}
			}
		});
		
		this.setOnFocusChangeListener(new OnFocusChangeListener() {
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if(hasFocus && requiresRemark() && !isCompleted()) {
					mRemark.requestFocus();
				}
			}
		});
		
		updateNextEnabled();
	}
	
	private boolean requiresRemark() {
		SurveyQuestionData question = getQuestion();
		
		if (question == null) {
			return false;
		}
		
		String remarkType = question.getRemarkType();
		String answer = getAnswer().getAnswer();
		
		if(remarkType != null && answer != null) {
			
			if(remarkType.equalsIgnoreCase(SurveyRemarkType.ON_BOTH) && 
					(answer.equalsIgnoreCase(ANSWER_YES) || 
				     answer.equalsIgnoreCase(ANSWER_NO))) {
				return true;
			}
			
			if(remarkType.equalsIgnoreCase(SurveyRemarkType.ON_YES) && 
					answer.equalsIgnoreCase(ANSWER_YES)) {
				return true;
			}
			
			if(remarkType.equalsIgnoreCase(SurveyRemarkType.ON_NO) && 
					answer.equalsIgnoreCase(ANSWER_NO)) {
				return true;
			}
		}
		
		return false;
	}
	
	private void onAnswerChecked(String answer) {
		
		getAnswer().setAnswer(answer);
		
		updateNextVisibility();
		updateNextEnabled();
		
		if (requiresRemark()) {
			mRemark.setVisibility(VISIBLE);
			mRemark.requestFocus();
		} else {
			mRemark.setVisibility(GONE);
			if(!isCompleted() && canAutoContinue()) {
				next();
			}
		}
	}
	
	private void updateNextVisibility() {
		SurveyQuestionData question = getQuestion();
		
		if(question == null) {
			return;
		}
		
		int visibility = VISIBLE;

		if (isCompleted()) {
			visibility = GONE;
		} else if (!requiresRemark()) {
			
			SurveyAnswerData answer = getAnswer();
			if(answer == null) {
				visibility = GONE;
			} else if(MetrixStringHelper.isNullOrEmpty(answer.getAnswer())){
				visibility = GONE;
			}
		}		
	
		mNext.setVisibility(visibility);
	}
	
	@Override
	protected void onQuestionChanged(SurveyQuestionData newQuestion) {
		super.onQuestionChanged(newQuestion);
		
		String continueText = newQuestion.getContinueText();
		if (continueText == null) {
			continueText = AndroidResourceHelper.getMessage("Next");
		}
		
		mNext.setText(continueText);
		
		mQuestion.setText(newQuestion.getQuestion());
		mAnswerSkip.setVisibility(newQuestion.isOptional() ? VISIBLE : GONE);
		
		updateNextVisibility();
		updateNextEnabled();
	}
	
	private void updateNextEnabled() {
		boolean enabled = false;
		
		if (!requiresRemark()) {

			SurveyAnswerData answer = getAnswer();
			if (answer != null && !MetrixStringHelper.isNullOrEmpty(answer.getAnswer())) {
				enabled = true;
			}
		
		} else if(mRemark.getText().length() > 0) {
			enabled = true;
		}
		
		mNext.setEnabled(enabled);
	}
	
	private void next()
	{
		String remark = null;
		if (requiresRemark()) {
			remark = mRemark.getText().toString();
		}
		getAnswer().setRemark(remark);
		
		setIsCompleted(true);
	}
	
	@Override
	protected void onAnswerChanged(SurveyAnswerData answer) {
		super.onAnswerChanged(answer);
		
		mUpdatingAnswer = true;
		
		String prevAnswer = answer.getAnswer();
		if(prevAnswer != null) {
			if(prevAnswer.equalsIgnoreCase(ANSWER_YES)) {
				mAnswerYes.setChecked(true);
			} else if(prevAnswer.equalsIgnoreCase(ANSWER_NO)) {
				mAnswerNo.setChecked(true);
			} else if(prevAnswer.equalsIgnoreCase(ANSWER_SKIP)) {
				mAnswerSkip.setChecked(true);
			}
		}
		
		mUpdatingAnswer = false;
		
		mRemark.setText(answer.getRemark());
		
		if (requiresRemark()) {
			mRemark.setVisibility(VISIBLE);
		} else {
			mRemark.setVisibility(GONE);
		}
		
		updateNextVisibility();
		updateNextEnabled();
	}
	
	@Override
	protected void onIsCompletedChanged(boolean isComplete) {
		super.onIsCompletedChanged(isComplete);
		
		updateNextVisibility();
		updateNextEnabled();
		
		mAnswerYes.setEnabled(!isComplete);
		mAnswerNo.setEnabled(!isComplete);
		mAnswerSkip.setEnabled(!isComplete);
		mRemark.setEnabled(!isComplete);
	}
	
}
