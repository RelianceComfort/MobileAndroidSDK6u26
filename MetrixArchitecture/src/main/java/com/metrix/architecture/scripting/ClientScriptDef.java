package com.metrix.architecture.scripting;

import java.util.ArrayList;
import java.util.HashMap;

public class ClientScriptDef {
	public static final String mElementName = "client_script_def";
	public boolean mIsValidation = false;
	public String mClientScriptId;
	public int mVersionNumber;
	public HashMap<String, Object> mVariables = new HashMap<String, Object>();
	public ArrayList<ClientScriptStatement> mStatements = new ArrayList<ClientScriptStatement>();
	
	/**
	 * This method clears the variable collection.
	 * 
	 * @since 5.6.3
	 */
	public void clearVariables() {
		if (this.mVariables == null)
			this.mVariables = new HashMap<String, Object>();
		else
			this.mVariables.clear();
	}
}
