package com.metrix.metrixmobile.survey;

public class SurveyQuestionData {
	
	private String mQuestionId;
	private String mAnswerType;
	private String mQuestion;
	private boolean mOptional;
	private String mRemarkType;
	private CharSequence[] mChoiceAnswers;
	private int mMaxChoiceAnswersToSelect;
	private boolean mHighlighted;
	private String mContinueText;
	private SurveySection mSection;
	private boolean mCanAutoContinue = true;
	private boolean mIsFinalStatement = false;

	public String getQuestionId() {
		return mQuestionId;
	}

	public void setQuestionId(String questionId) {
		this.mQuestionId = questionId;
	}

	public String getAnswerType() {
		return mAnswerType;
	}

	public void setAnswerType(String answerType) {
		this.mAnswerType = answerType;
	}
	
	public String getQuestion() {
		return mQuestion;
	}

	public void setQuestion(String question) {
		this.mQuestion = question;
	}

	public boolean isOptional() {
		return mOptional;
	}

	public void setOptional(boolean optional) {
		this.mOptional = optional;
	}

	public String getRemarkType() {
		return mRemarkType;
	}

	public void setRemarkType(String remarkType) {
		this.mRemarkType = remarkType;
	}

	public CharSequence[] getChoiceAnswers() {
		return mChoiceAnswers;
	}

	public void setChoiceAnswers(CharSequence[] choiceAnswers) {
		this.mChoiceAnswers = choiceAnswers;
	}

	public int getMaxChoiceAnswersToSelect() {
		return mMaxChoiceAnswersToSelect;
	}

	public void setMaxChoiceAnswersToSelect(int maxChoiceAnswersToSelect) {
		this.mMaxChoiceAnswersToSelect = maxChoiceAnswersToSelect;
	}

	public boolean isHighlighted() {
		return mHighlighted;
	}

	public void setHighlighted(boolean highlight) {
		this.mHighlighted = highlight;
	}

	public String getContinueText() {
		return mContinueText;
	}

	public void setContinueText(String continueText) {
		this.mContinueText = continueText;
	}

	public SurveySection getSection() {
		return mSection;
	}

	public void setSection(SurveySection section) {
		this.mSection = section;
	}

	public boolean canAutoContinue() {
		return mCanAutoContinue;
	}

	public void setCanAutoContinue(boolean mCanAutoContinue) {
		this.mCanAutoContinue = mCanAutoContinue;
	}
	
	public boolean isFinalStatement() {
		return mIsFinalStatement;
	}

	public void setIsFinalStatement(boolean isFinalStatement) {
		this.mIsFinalStatement = isFinalStatement;
	}

	public class SurveyAnswerType {
	    public static final String NUMBER 	= "NUMBER";
	    public static final String DATE 	= "DATE";
	    public static final String OPEN_ENDED	= "OPEN_ENDED";
	    public static final String YES_NO 		= "YES_NO";
	    public static final String MULTI_CHOICE = "MULTI_CHOICE";
	    public static final String RADIO_CHOICE = "RADIO";
		public static final String STATEMENT 	= "STATEMENT";
		public static final String ATTACHMENT 	= "ATTACHMENT";
		public static final String SIGNATURE 	= "SIGNATURE";
	}
	
	public class SurveyRemarkType {
		 public static final String NONE 	= "N/A";
		 public static final String ON_YES 	= "ONYES";
		 public static final String ON_NO 	= "ONNO";
		 public static final String ON_BOTH = "ONBOTH";
	}
	
}
