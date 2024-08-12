package com.metrix.architecture.scripting;

import java.util.ArrayList;

public class ClientScriptIfStatement extends ClientScriptStatement {
	public static final String mScriptTypeName = "client_script_if_statement";
	public ArrayList<String> mConditionExpression = new ArrayList<String>();
	public ClientScriptStatement mIfStatement = null;
	public ClientScriptElseStatement mElseStatement = null;
}
