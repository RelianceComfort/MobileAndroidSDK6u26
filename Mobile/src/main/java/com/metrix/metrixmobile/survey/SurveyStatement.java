package com.metrix.metrixmobile.survey;

import com.metrix.metrixmobile.R;
import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.architecture.utilities.MetrixStringHelper;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

public class SurveyStatement extends SurveyQuestion {
	
	private TextView mStatement;
	private Button mContinue;
	
	public SurveyStatement(Context context) {
		super(context);
	}
	
	public SurveyStatement(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	public void onCreate() {
		super.onCreate();
		
		setContentView(R.layout.survey_statement);
		
		mStatement = (TextView)this.findViewById(R.id.survey_statement_text);
        
        mContinue = (Button)this.findViewById(R.id.survey_statement_continue);
        mContinue.setText(AndroidResourceHelper.getMessage("Continue"));
        mContinue.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				next();
			}
		});
	}
	
	@Override
	protected void onQuestionChanged(SurveyQuestionData newQuestion) {
		super.onQuestionChanged(newQuestion);
		
		String continueText = newQuestion.getContinueText();
		if (continueText == null) {
			continueText = AndroidResourceHelper.getMessage("Continue");
		}
		
		boolean hasStatement = !MetrixStringHelper.isNullOrEmpty(newQuestion.getQuestion());
		
		mContinue.setText(continueText);
		mStatement.setText(newQuestion.getQuestion());
		mStatement.setTypeface(null, newQuestion.isHighlighted() ? (int)Typeface.BOLD : (int)Typeface.NORMAL);
		mStatement.setVisibility(hasStatement ? View.VISIBLE : View.GONE);
		
		ViewGroup.MarginLayoutParams continueMargins = (ViewGroup.MarginLayoutParams)mContinue.getLayoutParams();
		continueMargins.setMargins(0, (hasStatement ? 9 : 3), 0, 3);
	}
	
	private void next() {
		getAnswer().setAnswer(AndroidResourceHelper.getMessage("StatementOnly"));		
		setIsCompleted(true);
	}
	
	@Override
	protected void onIsCompletedChanged(boolean isComplete) {
		super.onIsCompletedChanged(isComplete);
		mContinue.setVisibility(isComplete ? GONE : VISIBLE);
		boolean hasStatement = (mStatement.getText() != null && mStatement.getText().length() > 0);
		this.setVisibility((!hasStatement && isComplete) ? GONE : VISIBLE);
	}
	
}
