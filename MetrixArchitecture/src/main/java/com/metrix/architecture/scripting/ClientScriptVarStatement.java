package com.metrix.architecture.scripting;

import java.util.ArrayList;
import java.util.HashMap;

public class ClientScriptVarStatement extends ClientScriptStatement {
	public static final String mScriptTypeName = "client_script_var_statement";
	public ArrayList<String> mVariableNames = new ArrayList<String>();
	public HashMap<String, ArrayList<String>> mExpressionTokens = new HashMap<String, ArrayList<String>>();
}
