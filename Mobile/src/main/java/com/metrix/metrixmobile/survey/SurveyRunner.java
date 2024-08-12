package com.metrix.metrixmobile.survey;

import android.os.Bundle;


public interface SurveyRunner {
	
	SurveyQuestionData getNextQuestion();
	
	SurveyQuestionData getCurrentQuestion();
	
	SurveyAnswerData getPreviousAnswer(String questionId);
	
	void provideAnswer(String questionId, SurveyAnswerData data);
	
	void cancelQuestion(String questionId);
	
	boolean complete(Bundle surveyResult);
}
