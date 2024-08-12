package com.metrix.metrixmobile.survey;

import java.util.Calendar;

import com.metrix.architecture.assistants.MetrixApplicationAssistant;
import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.architecture.utilities.LogManager;
import com.metrix.architecture.utilities.MetrixDateTimeHelper;
import com.metrix.architecture.utilities.MetrixStringHelper;
import com.metrix.metrixmobile.R;

import android.app.DatePickerDialog;
import android.content.Context;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

public class SurveyDateQuestion extends SurveyQuestion{
	
	private TextView mQuestion;
	private EditText mAnswer;
	private Button mNext;

	public SurveyDateQuestion(Context context) {
		super(context);
	}
	
	public SurveyDateQuestion(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	public void showDate()
	{
		String value = mAnswer.getText().toString();
		Calendar calendar = MetrixDateTimeHelper.getDate(MetrixDateTimeHelper.DATE_FORMAT, value);
		DatePickerDialog dialog = new DatePickerDialog(getContext(), MetrixApplicationAssistant.getSafeDialogThemeStyleID(), new DatePickerDialog.OnDateSetListener() {
			@Override
			public void onDateSet(DatePicker view, int year, int monthOfYear,
					int dayOfMonth) {
				try {
					mAnswer.setText(MetrixDateTimeHelper.formatDate(year, monthOfYear, dayOfMonth));
				} catch (Exception e) {
					LogManager.getInstance().error(e);
				}
			}
		}, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
		dialog.show();
	}

	public void onCreate() {
		super.onCreate();
		
		setContentView(R.layout.survey_date_question);
		
		mQuestion = (TextView)this.findViewById(R.id.survey_dateq_question);
		
		//mAnswer = (EditText)this.findViewById(R.id.survey_dateq_answer);
		LinearLayout ll = (LinearLayout)this.getChildAt(1);
		EditText et = (EditText)ll.getChildAt(0);
		mAnswer = et;

		mAnswer.setInputType(InputType.TYPE_NULL); // Replaced from XML into code, and make sure no keyboard type association
		mAnswer.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.calendar, 0);
		
		mAnswer.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				showDate();
			}
		});

		mAnswer.setOnFocusChangeListener(new OnFocusChangeListener() {
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if (!hasFocus) {
					hideKeyboard(v, mAnswer);
				}
			}
		});

		if(mAnswer.getText() != null){
			mAnswer.setSelection(mAnswer.getText().length());
		}

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

		//mNext = (Button)this.findViewById(R.id.survey_dateq_continue);
		Button btn = (Button)ll.getChildAt(1);
		mNext = btn;
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
	
	private void next(){
		String answerText = mAnswer.getText().toString();
		String answer = MetrixDateTimeHelper.convertDateTimeFromUIToDB(answerText);
		
		if (answer == "") {
			answer = null;
		}
		getAnswer().setAnswer(answer);
		setIsCompleted(true);
	}
	
	@Override
	protected void onAnswerChanged(SurveyAnswerData answer) {
		super.onAnswerChanged(answer);
		
		String answerDt = MetrixDateTimeHelper.convertDateTimeFromDBToUI(answer.getAnswer(), MetrixDateTimeHelper.DATE_FORMAT);
		mAnswer.setText(answerDt);
		
		//mAnswer.setText(answer.getAnswer());
	}
	
	@Override
	protected void onIsCompletedChanged(boolean isComplete) {
		super.onIsCompletedChanged(isComplete);
		mNext.setVisibility(isComplete ? GONE : VISIBLE);
		mAnswer.setEnabled(!isComplete);
		if(isComplete && MetrixStringHelper.isNullOrEmpty(mAnswer.getText().toString())){
			getAnswer().setAnswer(AndroidResourceHelper.getMessage("SkippedAnswer"));
			mAnswer.setText(getAnswer().mAnswer);
		}
	}
	
}
