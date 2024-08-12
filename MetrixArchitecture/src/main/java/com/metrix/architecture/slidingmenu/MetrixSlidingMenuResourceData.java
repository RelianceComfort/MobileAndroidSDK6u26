package com.metrix.architecture.slidingmenu;

import java.util.HashMap;

public class MetrixSlidingMenuResourceData {
	
	private HashMap<String, Integer> mSlidingMenuResourceIDs;
	private HashMap<String, String> mSlidingMenuResourceStrings;
	
	public MetrixSlidingMenuResourceData(HashMap<String, Integer> mSlidingMenuResourceIDs, HashMap<String, String> mSlidingMenuResourceStrings){
		this.mSlidingMenuResourceIDs = mSlidingMenuResourceIDs;
		this.mSlidingMenuResourceStrings = mSlidingMenuResourceStrings;
	}

	public HashMap<String, Integer> getSlidingMenuResourceIDs() {
		return mSlidingMenuResourceIDs;
	}

	public HashMap<String, String> getSlidingMenuResourceStrings() {
		return mSlidingMenuResourceStrings;
	}
}
