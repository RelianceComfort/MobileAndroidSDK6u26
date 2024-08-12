package com.metrix.metrixmobile.survey;

import android.os.Bundle;

public class MetrixSurveyActivity extends SurveyActivity {
	
	/** 
	 * Force all survey activities to use a specific runner
	 * ONLY USED FOR TESTING
	 */
	public static SurveyRunner test_SurveyRunner;


	@Override
	protected SurveyRunner createSurveyRunner(Bundle initData) {
		
		if (test_SurveyRunner != null) {
			return test_SurveyRunner;
		}
		
		return new MetrixSurveyRunner(this, initData);
	}
}
