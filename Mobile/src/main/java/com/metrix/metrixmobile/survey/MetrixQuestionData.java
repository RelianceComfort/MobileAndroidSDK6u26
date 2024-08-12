package com.metrix.metrixmobile.survey;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

import com.metrix.architecture.database.MetrixCursor;
import com.metrix.architecture.database.MetrixDatabaseManager;

public class MetrixQuestionData extends SurveyQuestionData {
	
	public enum QuestionType {
		QUESTION,
		FINAL
	}
	
	public MetrixSurveyData survey;
	public QuestionType questionType = QuestionType.QUESTION;
	public int questionId;
	public String controlType;
	public MetrixQuestionData parent;
	
	private ArrayList<AnswerChoice> mAnswerChoices;
	
	public void updateQuestionData() {
		
		switch (questionType) {
		case FINAL:
			setQuestionId(QuestionType.FINAL.toString());
			setAnswerType(SurveyAnswerType.STATEMENT);
			break;
		default:
			setQuestionId(survey.id + "_" + String.valueOf(questionId));
			setAnswerType(convertToSurveyAnswerType(controlType));
			updateAnswersChoices();
			break;
		}

		if (getAnswerType().equals(SurveyAnswerType.MULTI_CHOICE) ||
			getAnswerType().equals(SurveyAnswerType.RADIO_CHOICE)) {
			updateMultiChoiceOptions();
			//setMaxChoiceAnswersToSelect(1);
		}
	}
	
	private String convertToSurveyAnswerType(String controlType) {
		
		if("COMBOBOX".toUpperCase().equals(controlType.toUpperCase())) {
			return SurveyAnswerType.MULTI_CHOICE;
		}
		
		if("MULTICHOICE".toUpperCase().equals(controlType.toUpperCase())) {
			return SurveyAnswerType.MULTI_CHOICE;
		}
		
		if("RADIOBUTTON".toUpperCase().equals(controlType.toUpperCase())) {
			return SurveyAnswerType.RADIO_CHOICE;
		}
		
		if("STATEMENT".toUpperCase().equals(controlType.toUpperCase())) {
			return SurveyAnswerType.STATEMENT;
		}
		
		if("NUMBER".toUpperCase().equals(controlType.toUpperCase())) {
			return SurveyAnswerType.NUMBER;
		}
		
		if("DATE".toUpperCase().equals(controlType.toUpperCase())) {
			return SurveyAnswerType.DATE;
		}

		if("ATTACHMENT".toUpperCase().equals(controlType.toUpperCase())) {
			return SurveyAnswerType.ATTACHMENT;
		}
		
		if("SIGNATURE".toUpperCase().equals(controlType.toUpperCase())) {
			return SurveyAnswerType.SIGNATURE;
		}

		return SurveyAnswerType.OPEN_ENDED;
	}
	
	private void updateAnswersChoices() {
		mAnswerChoices = new ArrayList<AnswerChoice>();
		
		MetrixCursor cursor = null;
		try {
			cursor = MetrixDatabaseManager.getRowsMC("survey_answer", 
							new String[] { 
								"answer_id",
								"answer",
								"sequence",
								"next_survey_id",
								"answer_feedback",
								"comment_required",
								"comments"
							},
							"survey_id = " + survey.id + " and question_id = " + questionId + " and active = 'Y' order by sequence is NULL, sequence asc");

			if (cursor != null && cursor.moveToFirst()) {
				
				while (cursor.isAfterLast() == false) {
					int sequence = cursor.isNull(2) ? 0 : cursor.getInt(2);
					Integer nextSurveyId = cursor.isNull(3) ? null : cursor.getInt(3);
					AnswerChoice item = new AnswerChoice(cursor.getInt(0), cursor.getString(1), sequence, nextSurveyId, cursor.getString(4), cursor.getString(5), cursor.getString(6));
					mAnswerChoices.add(item);
					cursor.moveToNext();
				}
				
			}
			
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
		
		Collections.sort(mAnswerChoices);
	}
	
	private void updateMultiChoiceOptions() {

		String[] choicesArray = new String[mAnswerChoices.size()];
	    Iterator<AnswerChoice> iterator = mAnswerChoices.iterator();
	    for (int i = 0; i < choicesArray.length; i++)
	    {
	    	choicesArray[i] = iterator.next().answer;
	    }
	    
		setChoiceAnswers(choicesArray);
	}

	public AnswerChoice getAnswerChoice(int index) {	
		
		if(mAnswerChoices == null || index >= mAnswerChoices.size()) {
			return null;
		}
		
		return mAnswerChoices.get(index);		
	}
	
	public String getAnswerFeedback(int index) {	
		
		if(mAnswerChoices == null || index >= mAnswerChoices.size()) {
			return null;
		}
		
		return mAnswerChoices.get(index).answerFeedback;		
	}
	
	public String getCommentRequired(int index) {	
		
		if(mAnswerChoices == null || index >= mAnswerChoices.size()) {
			return null;
		}
		
		return mAnswerChoices.get(index).commentRequired;		
	}
	
    public boolean isAnswerCommentRequired(int index) {	
		
		if(mAnswerChoices == null || index >= mAnswerChoices.size()) {
			return false;
		}
		
		if (mAnswerChoices.get(index).commentRequired != null)
		{
			if (mAnswerChoices.get(index).commentRequired.toUpperCase() == "Y")
				return true;
		}

		return false;	
	}
	
	public int getAnswerIndex(int answerId) {	
		
		if(mAnswerChoices == null) {
			return -1;
		}
		
		for (int i = 0; i < mAnswerChoices.size(); i++) {
			AnswerChoice choice = mAnswerChoices.get(i);
			if(choice.answerId == answerId) {
				return i;
			}
		}
		
		return -1;		
	}
	
	public int getAnswerIndexByValue(String answerValue) {	
		
		if(mAnswerChoices == null) {
			return -1;
		}
		
		for (int i = 0; i < mAnswerChoices.size(); i++) {
			AnswerChoice choice = mAnswerChoices.get(i);			
			if(choice.answer.equalsIgnoreCase(answerValue)) {
				return i;
			}
		}
		
		return -1;		
	}	

	public class AnswerChoice implements Comparable<AnswerChoice> { 
	
		public final int answerId; 
	  	public final String answer; 
	  	public final String answerFeedback;
	  	public final String comments;
	  	public final String commentRequired;
	  	public final int sequence;
	  	public final Integer nextSurveyId;
	  
		public AnswerChoice(int answerId, String itemDescription, int sequence, Integer nextSurveyid, String answerFeedbackIn, String commentRequiredIn, String commentsIn) { 
			this.answerId = answerId;
			this.answer = itemDescription;
			this.sequence = sequence;
			this.nextSurveyId = nextSurveyid;
			this.answerFeedback = answerFeedbackIn;
			this.commentRequired = commentRequiredIn;
			this.comments = commentsIn;
		}

		@Override
		public int compareTo(AnswerChoice another) {
			if(this.sequence > another.sequence) return 1;
			if(this.sequence < another.sequence) return -1;
			if(this.answerId > another.answerId) return 1;
			if(this.answerId < another.answerId) return -1;
			return 0;
		} 
	}

}
