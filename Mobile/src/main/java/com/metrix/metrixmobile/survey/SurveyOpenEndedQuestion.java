package com.metrix.metrixmobile.survey;

import com.metrix.architecture.assistants.MetrixControlAssistant;
import com.metrix.architecture.designer.MetrixFieldManager;
import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.architecture.utilities.MetrixStringHelper;
import com.metrix.metrixmobile.R;

import android.content.Context;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

public class SurveyOpenEndedQuestion extends SurveyQuestion {
	
	private TextView mQuestion;
	private EditText mAnswer;
	private Button mNext;

	public SurveyOpenEndedQuestion(Context context) {
		super(context);
	}
	
	public SurveyOpenEndedQuestion(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	public EditText getEditText() {
		return mAnswer;
	}

	
	public void onCreate() {
		super.onCreate();
		
		setContentView(R.layout.survey_open_ended_question);
		
		mQuestion = findViewById(R.id.survey_oeq_question);
		mAnswer = findViewById(R.id.survey_oeq_answer);
		mAnswer.setId(MetrixControlAssistant.generateViewId());

		mAnswer.setOnFocusChangeListener(new OnFocusChangeListener() {
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if (hasFocus && !isCompleted()) {
					InputMethodManager keyboard = (InputMethodManager)getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
					keyboard.showSoftInput(mAnswer, 0);
		        }
				if(!hasFocus) {
					hideKeyboard(v, mAnswer);
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
				String answer = mAnswer.getText().toString().trim();
				if (answer == "") {
					answer = null;
				}
				getAnswer().setAnswer(answer);
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
		
		mNext = (Button)this.findViewById(R.id.survey_oeq_continue);
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
				if(hasFocus && !isCompleted()) {
					mAnswer.requestFocus();
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
	
	private void next()
	{
		String answer = mAnswer.getText().toString();
		if (answer == "") {
			answer = null;
		}

		//In an optional question, the next button will be enabled always. In such cases if the next button is clicked the text box will take a value of "Skipped Answer", automatically
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
