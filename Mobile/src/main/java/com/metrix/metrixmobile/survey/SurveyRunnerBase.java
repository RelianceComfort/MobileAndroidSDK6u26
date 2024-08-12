package com.metrix.metrixmobile.survey;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;


public abstract class SurveyRunnerBase<T extends SurveyQuestionData> implements SurveyRunner {
	
	protected ArrayList<T> mQuestions;
	protected LinkedHashMap<String, SurveyAnswerData> mAnswers;
	protected HashSet<String> mQuestionsAsked;
	
	protected void initialize() {
		
		mQuestionsAsked = new HashSet<String>();
		mAnswers = new LinkedHashMap<String, SurveyAnswerData>();
		
		loadQuestions();
		loadPreviousAnswers();
	}
	
	protected abstract void loadQuestions();
	
	protected void loadPreviousAnswers() {
		
	}

	@Override
	public SurveyQuestionData getNextQuestion() {
		
		if(mQuestions != null) {
			for (int i = 0; i < mQuestions.size(); i++) {
				T question = mQuestions.get(i);				
				if (!isQuestionAsked(question) && isQuestionValid(question)) {
					prepareQuestionForAsking(question, i);
					mQuestionsAsked.add(question.getQuestionId());
					return question;
				}
			}
		}
		
		return null;
	}
	
	@Override
	public SurveyQuestionData getCurrentQuestion() {
		String currentQuestionId = "";		
		Iterator<String> itr = mQuestionsAsked.iterator();

		 while(itr.hasNext())
			 currentQuestionId = (String)itr.next();

		if(mQuestions != null) {
			for (int i = 0; i < mQuestions.size(); i++) {
				MetrixQuestionData question = (MetrixQuestionData)mQuestions.get(i);				
				if (question.getQuestionId() == currentQuestionId) {
					return question;
				}
			}
		}
		
		return null;
	}
	
	protected T getQuestionById(String questionId) {
		
		if(mQuestions != null) {
			for (int i = 0; i < mQuestions.size(); i++) { 
				T question = mQuestions.get(i);
				if(question.getQuestionId().equals(questionId)) {
					return question;
				}
			}
		}
		
		return null;
	}

	private boolean isQuestionAsked(T question) {
		return mQuestionsAsked.contains(question.getQuestionId());
	}
	
	protected boolean isQuestionValid(T question) {
		return true;
	}
	
	protected void prepareQuestionForAsking(T question, int index) {
		
	}

	@Override
	public SurveyAnswerData getPreviousAnswer(String questionId) {
		return mAnswers.get(questionId);
	}

	@Override
	public void provideAnswer(String questionId, SurveyAnswerData data) {
		mAnswers.put(questionId, data);
	}

	@Override
	public void cancelQuestion(String questionId) {
		mAnswers.remove(questionId);
		mQuestionsAsked.remove(questionId);
	}

}
