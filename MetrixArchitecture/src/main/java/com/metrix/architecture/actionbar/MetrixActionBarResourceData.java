package com.metrix.architecture.actionbar;

import java.util.HashMap;

public class MetrixActionBarResourceData {

	private HashMap<String, Integer> mActionBarResourceIDs;
	private HashMap<String, String> mActionBarResourceStrings;
	
	public MetrixActionBarResourceData(HashMap<String, Integer> mActionBarResourceIDs, HashMap<String, String> mActionBarResourceStrings){
		this.mActionBarResourceIDs = mActionBarResourceIDs;
		this.mActionBarResourceStrings = mActionBarResourceStrings;
	}

	public HashMap<String, Integer> getActionBarResourceIDs() {
		return mActionBarResourceIDs;
	}

	public HashMap<String, String> getActionBarResourceStrings() {
		return mActionBarResourceStrings;
	}

}
