package com.metrix.architecture.scripting;

public class ClientScriptWhileStatement extends ClientScriptStatement {
	public static final String mScriptTypeName = "client_script_while_statement";
	public ClientScriptStatement mConditionStatement = null;
	public ClientScriptStatement mLoopStatement = null;
}
