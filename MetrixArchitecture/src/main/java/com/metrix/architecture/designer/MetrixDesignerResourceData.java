package com.metrix.architecture.designer;

import java.util.HashMap;

import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.architecture.utilities.LogManager;
import com.metrix.architecture.utilities.MetrixStringHelper;

public class MetrixDesignerResourceData {
	public int LayoutResourceID;
	public int ListViewResourceID;
	public int ListViewItemResourceID;
	public HashMap<String, Integer> ExtraResourceIDs;
	
	public String HelpTextString;
	public HashMap<String, String> ExtraResourceStrings;
	
	public MetrixDesignerResourceData(int layoutResID, int lvResID, int lviResID, String helpText, HashMap<String, Integer> extraResIDs, HashMap<String, String> extraResStrings) {
		LayoutResourceID = layoutResID;
		ListViewResourceID = lvResID;
		ListViewItemResourceID = lviResID;
		
		HelpTextString = helpText;
		 
		ExtraResourceIDs = extraResIDs;
		ExtraResourceStrings = extraResStrings;
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
}