package com.metrix.architecture.scripting;

import java.util.ArrayList;

public class ClientScriptStatement {
	public ArrayList<String> mTokens;
	public ArrayList<ClientScriptStatement> mChildStatements = new ArrayList<ClientScriptStatement>();
	public static final String mScriptTypeName = "client_script_statement";
}
