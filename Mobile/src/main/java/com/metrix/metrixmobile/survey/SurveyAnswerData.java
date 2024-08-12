package com.metrix.metrixmobile.survey;

import java.util.List;

import android.os.Bundle;
import android.util.SparseArray;

public class SurveyAnswerData {
	public String mAnswer;
	private int mAttachmentId = 0;
	private String mRemark;
	private String mComment;
	private boolean mIsCompleted;
	private int[] mMultiChoiceSelections;
	private SparseArray<String> multiChoiceComments = new SparseArray<>();
	public String mAnswerId;

	public SurveyAnswerData(String answer, String answerId) {
		mAnswer = answer;
		mAnswerId = answerId;
	}

	public SurveyAnswerData() {}
	
	public String getAnswer() {
		return mAnswer;
	}
	
	public void setAnswer(String answer) {
		this.mAnswer = answer;
	}
	
	public int[] getMultiChoiceSelections() {
		return mMultiChoiceSelections;
	}
	
	public void setMultiChoiceSelections(int[] multiChoiceSelections) { this.mMultiChoiceSelections = multiChoiceSelections; }
	
	public void setMultiChoiceSelections(List<Integer> multiChoiceSelections) {
		this.mMultiChoiceSelections = new int[multiChoiceSelections.size()];
	    for (int i = 0; i < mMultiChoiceSelections.length; i++) {
	    	mMultiChoiceSelections[i] = multiChoiceSelections.get(i).intValue();
	    }
	}

	public String getMultiChoiceComment(int answerId) {
		return multiChoiceComments.get(answerId, null);
	}

	public void setMultiChoiceComment(int answerId, String comment) {
		multiChoiceComments.put(answerId, comment);
	}

	public int getAttachmentId() {
		return mAttachmentId;
	}

	public void setAttachmentId(int attachmentId) {
		this.mAttachmentId = attachmentId;
	}

	public String getRemark() {
		return mRemark;
	}
	
	public void setRemark(String remark) {
		this.mRemark = remark;
	}
	
	public String getComment() {
		return mComment;
	}
	
	public void setComment(String comment) {
		this.mComment = comment;
	}

	public boolean isCompleted() {
		return mIsCompleted;
	}

	public void setIsCompleted(boolean isComplete) {
		this.mIsCompleted = isComplete;
	}
	
	public void save(Bundle bundle) {
		bundle.putString("answer", mAnswer);
		bundle.putInt("attachment_id", mAttachmentId);
		bundle.putString("remark", mRemark);
		bundle.putBoolean("isComplete", mIsCompleted);
		bundle.putIntArray("choices", mMultiChoiceSelections);
		bundle.putString("comment", mComment);
		int[] mcKeys = new int[multiChoiceComments.size()];
		String[] mcComments = new String[multiChoiceComments.size()];
		for (int i = 0; i < multiChoiceComments.size(); i++) {
			mcKeys[i] = multiChoiceComments.keyAt(i);
			mcComments[i] = multiChoiceComments.get(multiChoiceComments.keyAt(i));
		}
		bundle.putIntArray("multiChoiceCommentKeys", mcKeys);
		bundle.putStringArray("multiChoiceComments", mcComments);
	}
	
	public void restore(Bundle bundle) {
		mAnswer = bundle.getString("answer");
		mAttachmentId = bundle.getInt("attachment_id");
		mRemark = bundle.getString("remark");
		mIsCompleted = bundle.getBoolean("isComplete");
		mMultiChoiceSelections = bundle.getIntArray("choices");
		mComment = bundle.getString("comment");
		int[] mcKeys = bundle.getIntArray("multiChoiceCommentKeys");
		String[] mcComments = bundle.getStringArray("multiChoiceComments");
		if (mcKeys.length == mcComments.length) {
			multiChoiceComments.clear();
			for (int i = 0; i < mcKeys.length; i++) {
				multiChoiceComments.put(mcKeys[i], mcComments[i]);
			}
		}
	}
}
