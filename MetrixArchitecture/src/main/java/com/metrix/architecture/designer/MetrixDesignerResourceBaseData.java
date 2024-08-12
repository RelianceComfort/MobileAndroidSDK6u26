package com.metrix.architecture.designer;

import java.util.HashMap;

import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.architecture.utilities.LogManager;
import com.metrix.architecture.utilities.MetrixStringHelper;

public class MetrixDesignerResourceBaseData {
	public int ActionBarTitleResourceID;
	public int ActionBarHelpResourceID;
	public int ActionBarHelpIconResourceID;
	public HashMap<String, Integer> ExtraResourceIDs;
	
	public String ActionBarTitleString;
	public HashMap<String, String> ExtraResourceStrings;
	
	private int actionBarLayoutID;
	private int actionBarIconID;
	private int acttionBarNavigationDrawerDefaultColor;
	
	public MetrixDesignerResourceBaseData(int actionBarLayoutID, int actBarTitleResID, int actBarHelpResID, int actBarHelpIconResID, String actBarTitleString, int actBarIcon, HashMap<String, Integer> extraResIDs, HashMap<String, String> extraResStrings) {
		ActionBarTitleResourceID = actBarTitleResID;
		ActionBarHelpResourceID = actBarHelpResID;
		ActionBarHelpIconResourceID = actBarHelpIconResID;
		
		ActionBarTitleString = actBarTitleString;
		
		ExtraResourceIDs = extraResIDs;
		ExtraResourceStrings = extraResStrings;
		
		
		this.actionBarLayoutID = actionBarLayoutID;
		this.actionBarIconID = actBarIcon;
	}
	
	public boolean extraResourceIDsIsNull() {
		return (ExtraResourceIDs == null);
	}
	
	public boolean extraResourceStringsIsNull() {
		return (ExtraResourceStrings == null);
	}
	
	public int getExtraResourceID(String resourceIdentifier) {
		int retValue = 0;
		
		try {
			if (!extraResourceIDsIsNull()) {
				retValue = (int) ExtraResourceIDs.get(resourceIdentifier);
			}
		} catch (Exception ex) {
			LogManager.getInstance().error(ex);
			retValue = 0;
		}		
		
		if (retValue == 0) {
			LogManager.getInstance().error(AndroidResourceHelper.getMessage("CannotFindResrcIdFor1Args", resourceIdentifier));
		}
		
		return retValue;
	}
	
	public String getExtraResourceString(String resourceIdentifier) {
		String retValue = "";
		
		try {
			if (!extraResourceStringsIsNull()) {
				retValue = ExtraResourceStrings.get(resourceIdentifier);
			}
		} catch (Exception ex) {
			LogManager.getInstance().error(ex);
			retValue = "";
		}		
		
		if (MetrixStringHelper.isNullOrEmpty(retValue)) {
			LogManager.getInstance().error(AndroidResourceHelper.getMessage("CannotFindResStrFor1Args", resourceIdentifier));
		}
		
		return retValue;
	}
	
	public int getActionBarLayoutID() {
		return actionBarLayoutID;
	}
	
	public int getActionBarIconID() {
		return actionBarIconID;
	}
	
	public int getActtionBarNavigationDrawerDefaultColor() {
		return acttionBarNavigationDrawerDefaultColor;
	}
}
