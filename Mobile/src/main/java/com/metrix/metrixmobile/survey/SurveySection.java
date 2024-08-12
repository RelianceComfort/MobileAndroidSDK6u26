package com.metrix.metrixmobile.survey;

import android.widget.ImageView;

public class SurveySection {
	private final String mHeader;
	private final String mDetail;
	private final boolean mAllowSkipAll;
	private ImageView mSkipAllButton;
	
	public SurveySection(String header, String detail, boolean allowSkipAll) {
		mHeader = header;
		mDetail = detail;
		mAllowSkipAll = allowSkipAll;
	}
	
	public String getHeader() {
		return mHeader;
	}
	
	public void setSkipAllButton(ImageView skipAllButton) {
		mSkipAllButton = skipAllButton;
	}
	
	public ImageView getSkipAllButton() {
		return mSkipAllButton;
	}
	
	public String getDetail() {
		return mDetail;
	}
	
	public boolean getAllowSkipAll() {
		return mAllowSkipAll;
	}
	
	 @Override
	public boolean equals(Object o) {

		if(o == null || !(o instanceof SurveySection)) {
			return false;
		}
		
		SurveySection other = (SurveySection)o;

		return objEqual(mHeader, other.mHeader) && 
			   objEqual(mDetail, other.mDetail) && 
			   objEqual(mAllowSkipAll, other.mAllowSkipAll);
	}
	 
	private boolean objEqual(Object x, Object y) {
		 
		 if (x== null && y == null) {
			 return true;
		 }
		 
		 if(x == null || y == null) {
			 return false;
		 }
		 
		 return x.equals(y);
	}
	 
	@Override
	public int hashCode() {
		
		int h = 17;
		
		if(mHeader != null) {
			h ^= mHeader.hashCode();			
		}
		
		h ^= 13;
		
		if(mDetail != null) {
			h ^= mDetail.hashCode();			
		}
		
		return h;
	}
	
	public static boolean equals(SurveySection x, SurveySection y) {
		if (x== null && y == null) {
			return true;
		}
	
		if(x != null) {
			return x.equals(y);
		}

		return y.equals(x);
	}

}
