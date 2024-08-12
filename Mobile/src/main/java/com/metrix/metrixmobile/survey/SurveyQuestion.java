package com.metrix.metrixmobile.survey;

import com.metrix.architecture.designer.MetrixSkinManager;
import com.metrix.architecture.utilities.MetrixStringHelper;
import com.metrix.metrixmobile.R;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;

public class SurveyQuestion extends LinearLayout {
	protected Activity mActivityInfo;
	protected boolean mIsCompleted = false;
	protected String mLighterColorString = "";
	private String mPrimaryColorString = "";
	private OnIsCompletedChangedListener mOnIsCompleteListener;
	private OnBackPressedListener mOnBackPressedListener;
	private SurveyQuestionData mQuestion;
	private SurveyAnswerData mAnswer = new SurveyAnswerData();
	
	public SurveyQuestion(Context context) {
		super(context);

		if (context instanceof Activity) {
			mActivityInfo = (Activity) context;
		}

		setupColorStrings();
		Initilise();
	}
	
	public SurveyQuestion(Context context, AttributeSet attrs) {
		super(context, attrs);

		if (context instanceof Activity) {
			mActivityInfo = (Activity) context;
		}

		setupColorStrings();
		Initilise();
	}
	
	private void setupColorStrings() {
		mPrimaryColorString = MetrixSkinManager.getPrimaryColor();
		if (!MetrixStringHelper.isNullOrEmpty(mPrimaryColorString)) {
			mLighterColorString = MetrixSkinManager.generateLighterVersionOfColor(mPrimaryColorString, 0.875f, true);
			mLighterColorString = mLighterColorString.replace("#", "");
		}		
	}
	
	@SuppressWarnings("deprecation")
	private void Initilise()
	{		
		LayoutParams lp = new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
		lp.setMargins(0, 0, 0, 10);
		setLayoutParams(lp);
		setFocusableInTouchMode(true);
		setFocusable(true);
		setOrientation(LinearLayout.VERTICAL);
		if (!MetrixStringHelper.isNullOrEmpty(mPrimaryColorString)) {
			setBackgroundDrawableUsingSkinColors();		
		} else {
			setBackgroundResource(R.drawable.survey_question_background);			
		}
		setPadding(10, 10, 10, 10);
		onCreate();
	}
	
	protected void onCreate() {
		
	}
	
	protected void setContentView(int layoutResID) {
        LayoutInflater li = (LayoutInflater)getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        li.inflate(layoutResID, this, true);
	}
	
	public boolean isCompleted() {
		return mAnswer.isCompleted();
	}
	
	protected boolean canAutoContinue() {
		if(mQuestion == null) {
			return true;
		} else { 
			return mQuestion.canAutoContinue();			
		}
	}
	
	public void setIsCompleted(boolean isComplete) {
		mAnswer.setIsCompleted(isComplete);
		notifyCompletion();
	}
	
	private void notifyCompletion() {
		if(mIsCompleted != mAnswer.isCompleted()) {
			mIsCompleted = mAnswer.isCompleted();
			
			if (!mIsCompleted) {
				if (!MetrixStringHelper.isNullOrEmpty(mPrimaryColorString)) {
					setBackgroundDrawableUsingSkinColors();
				} else {
					setBackgroundResource(R.drawable.survey_question_background);			
				}
			} else {
				setBackgroundResource(R.drawable.survey_question_background_complete);
			}
			
			setFocusableInTouchMode(!mIsCompleted);
			setFocusable(!mIsCompleted);
			
			onIsCompletedChanged(mIsCompleted);
		}
	}
	
	@SuppressWarnings("deprecation")
	private void setBackgroundDrawableUsingSkinColors() {
		int lighterColorInt = (int)Long.parseLong(mLighterColorString, 16);
		int[] colors = new int[2];
		colors[0] = Color.rgb((lighterColorInt >> 16) & 0xFF, (lighterColorInt >> 8) & 0xFF, (lighterColorInt >> 0) & 0xFF);
		colors[1] = Color.rgb((lighterColorInt >> 16) & 0xFF, (lighterColorInt >> 8) & 0xFF, (lighterColorInt >> 0) & 0xFF);					
		GradientDrawable gradient = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, colors);
		float scale = getResources().getDisplayMetrics().density;
		float radius = 7f * scale + 0.5f;
		int strokeSize = (int) (2f * scale + 0.5f);
		gradient.setCornerRadius(radius);
		gradient.setStroke(strokeSize, Color.parseColor(mPrimaryColorString));
		setBackgroundDrawable(gradient);		
	}
	
	public void setOnIsCompletedChangedListener(OnIsCompletedChangedListener listener) {
        mOnIsCompleteListener = listener;
    }
	
	protected void onIsCompletedChanged(boolean isComplete) {
		fireOnIsCompletedChangedListener(isComplete);
	}
	
	private void fireOnIsCompletedChangedListener(boolean isComplete) {
		if (mOnIsCompleteListener != null) {
			mOnIsCompleteListener.onIsCompletedChanged(this, isComplete);
		}
    }
	
	public interface OnIsCompletedChangedListener 
    {
        void onIsCompletedChanged(SurveyQuestion view, boolean isComplete);
    }
	
	public void setOnBackPressedListener(OnBackPressedListener listener) {
        mOnBackPressedListener = listener;
    }
	
	private boolean fireOnBackPressedListener() {
		if (mOnBackPressedListener != null) {
			return mOnBackPressedListener.onBackPressed(this);
		}
		return false;
    }
	
	public interface OnBackPressedListener
    {
        boolean onBackPressed(SurveyQuestion view);
    }
	
	public SurveyQuestionData getQuestion() {
		return mQuestion;
	}

	public void setQuestion(SurveyQuestionData question) {
		if(question != mQuestion) {
			this.mQuestion = question;
			this.mAnswer = new SurveyAnswerData();
			onQuestionChanged(question);
			onAnswerChanged(this.mAnswer);
			notifyCompletion();
		}
	}
	
	protected void onQuestionChanged(SurveyQuestionData newQuestion) {
		
	}
	
	public SurveyAnswerData getAnswer() {
		return mAnswer;
	}

	public void setAnswer(SurveyAnswerData answer) {
		
		if(answer == null) {
			answer = new SurveyAnswerData();
		}
		
		if(answer != mAnswer) {
			this.mAnswer = answer;
			onAnswerChanged(answer);
			notifyCompletion();
		}
	}
	
	protected void onAnswerChanged(SurveyAnswerData answer) {
		
	}
	

	@Override
	public boolean dispatchKeyEventPreIme(KeyEvent event) {
		
		if (event.getKeyCode() == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
			if(fireOnBackPressedListener()) {
				return true;
			}
	    }
		
		return super.dispatchKeyEventPreIme(event);
	}
	
	protected boolean onNextPressed() {
		return false;
	}
	
	public static SurveyQuestion createForQuestionData(Context context, SurveyQuestionData data) {
		SurveyQuestion ques = createForAnswerType(context, data.getAnswerType());
		if(ques != null) {
			ques.setQuestion(data);
		}
		return ques;
	}

	public static SurveyQuestion createForAnswerType(Context context, String type) {
		
		if (type.equals(SurveyQuestionData.SurveyAnswerType.OPEN_ENDED)) {
			return new SurveyOpenEndedQuestion(context);
		}
		
		if (type.equals(SurveyQuestionData.SurveyAnswerType.STATEMENT)) {
			return new SurveyStatement(context);
		}
		
		if (type.equals(SurveyQuestionData.SurveyAnswerType.YES_NO)) {
			return new SurveyYesNoQuestion(context);
		}
		
		if (type.equals(SurveyQuestionData.SurveyAnswerType.MULTI_CHOICE)) {
			return new SurveyMultiChoiceQuestion(context);
		}
		
		if (type.equals(SurveyQuestionData.SurveyAnswerType.RADIO_CHOICE)) {
			return new SurveyRadioChoiceQuestion(context);
		}
		
		if (type.equals(SurveyQuestionData.SurveyAnswerType.NUMBER)) {
			return new SurveyNumericQuestion(context);
		}

		if (type.equals(SurveyQuestionData.SurveyAnswerType.DATE)) {
			return new SurveyDateQuestion(context);
		}

		if (type.equals(SurveyQuestionData.SurveyAnswerType.ATTACHMENT)) {
			return new SurveyAttachmentQuestion(context);
		}
		
		if (type.equals(SurveyQuestionData.SurveyAnswerType.SIGNATURE)) {
			return new SurveySignatureQuestion(context);
		}

		return null;
	}

	public static void hideKeyboard(View v, EditText editTextAnswer) {
		Activity host = (Activity) v.getContext();
		InputMethodManager inputMethodManager = (InputMethodManager) host.getSystemService(Activity.INPUT_METHOD_SERVICE);
		inputMethodManager.hideSoftInputFromWindow(editTextAnswer.getWindowToken(), 0);
	}
}
