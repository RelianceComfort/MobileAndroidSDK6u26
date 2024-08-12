package com.metrix.metrixmobile.survey;

import com.metrix.architecture.assistants.MetrixControlAssistant;
import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.architecture.utilities.MetrixStringHelper;
import com.metrix.metrixmobile.R;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

public class SurveyNumericQuestion extends SurveyQuestion {
	
	private TextView mQuestion;
	private EditText mAnswer;
	private Button mNext;

	public SurveyNumericQuestion(Context context) {
		super(context);
	}
	
	public SurveyNumericQuestion(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	public EditText getEditText() {
		return mAnswer;
	}

	public void onCreate() {
		super.onCreate();
		
		setContentView(R.layout.survey_numeric_question);
		
		mQuestion = findViewById(R.id.survey_numq_question);
		mAnswer = findViewById(R.id.survey_numq_answer);
		mAnswer.setId(MetrixControlAssistant.generateViewId());

		mAnswer.setOnFocusChangeListener(new OnFocusChangeListener() {
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if (hasFocus && !isCompleted()) {
					InputMethodManager keyboard = (InputMethodManager)getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
					keyboard.showSoftInput(mAnswer, 0);
		        }
			}
		});
		mAnswer.addTextChangedListener(new TextWatcher() {
			
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
		mAnswer.setOnEditorActionListener(new OnEditorActionListener() {
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

		mNext = (Button)this.findViewById(R.id.survey_numq_continue);
		mNext.setText(AndroidResourceHelper.getMessage("Next"));
		mNext.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				next();
			}
		});
		
		this.setOnFocusChangeListener(new OnFocusChangeListener() {
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				// display Keyboard on focus.
				if(hasFocus && !isCompleted()) {
					mAnswer.requestFocus();
				}
				if(!hasFocus) {
					hideKeyboard(v, mAnswer);
				}
			}
		});
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
		updateNextEnabled();
	}
	
	private void updateNextEnabled() {
		SurveyQuestionData question = getQuestion();
		
		if(question == null) {
			return;
		}
		
		boolean enabled = false;
		
		if(question.isOptional()) {
			enabled = true;
		}
		
		if(mAnswer.getText().length() > 0) {
			enabled = true;
		}
		
		mNext.setEnabled(enabled);
	}
	
	private void next(){
		String answer = mAnswer.getText().toString();
		if (answer == "") {
			answer = null;
		}
		getAnswer().setAnswer(!MetrixStringHelper.isNullOrEmpty(answer)? answer : AndroidResourceHelper.getMessage("SkippedAnswer") );
		mAnswer.setText(getAnswer().mAnswer);
		setIsCompleted(true);
	}
	
	@Override
	protected void onAnswerChanged(SurveyAnswerData answer) {
		super.onAnswerChanged(answer);
		
		mAnswer.setText(answer.getAnswer());
	}
	
	@Override
	protected void onIsCompletedChanged(boolean isComplete) {
		super.onIsCompletedChanged(isComplete);
		mNext.setVisibility(isComplete ? GONE : VISIBLE);
		mAnswer.setEnabled(!isComplete);
	}
	
}
