package com.metrix.architecture.scripting;

public class ClientScriptForStatement extends ClientScriptStatement {
	public static final String mScriptTypeName = "client_script_for_statement";
	public ClientScriptStatement mInitStatement = null;
	public ClientScriptStatement mConditionStatement = null;
	public ClientScriptStatement mEndStatement = null;
	public ClientScriptStatement mLoopStatement = null;
}
