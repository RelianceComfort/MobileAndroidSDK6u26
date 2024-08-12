package com.metrix.architecture.scripting;

import java.util.ArrayList;
import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.metrix.architecture.utilities.LogManager;
import com.metrix.architecture.utilities.MetrixStringHelper;

public class ClientScriptParser {
	public static ClientScriptDef deserializeScriptJSON(String jsonString) throws Exception {
		jsonString = jsonString.replace("u+0027", "'").replace("u+0022", "\"").replace("u+005c", "\\");
		ClientScriptDef scriptDef = null;
		try {
			JSONObject jsonMessageObj = new JSONObject(jsonString);
			JSONObject clientScriptObj = jsonMessageObj.optJSONObject(ClientScriptDef.mElementName);
			if (clientScriptObj != null) {
				scriptDef = new ClientScriptDef();
				scriptDef.mClientScriptId = clientScriptObj.optString("client_script_id");
				scriptDef.mVersionNumber = clientScriptObj.optInt("version_number");
				scriptDef.mIsValidation = false;
				
				JSONObject statementsObj = clientScriptObj.optJSONObject("statements");
				if (statementsObj != null) {
					scriptDef.mStatements = new ArrayList<ClientScriptStatement>();
					JSONObject singleStatement = statementsObj.optJSONObject("statement");
					if (singleStatement != null) {
						scriptDef.mStatements.add(generateStatement(singleStatement));						
					} else {
						JSONArray statementArray = statementsObj.optJSONArray("statement");
						if (statementArray != null) {
							for (int i = 0; i < statementArray.length(); i++) {
								JSONObject statement = statementArray.getJSONObject(i);
								if (statement != null) {
									scriptDef.mStatements.add(generateStatement(statement));
								}
							}
						}
					}
				}
			}	
		} catch (JSONException e) {
			LogManager.getInstance().error(e);
		}		
		
		return scriptDef;
	}
	
	private static ClientScriptStatement generateStatement(JSONObject statement) throws Exception {
		try {
			String statementType = statement.optString("statement_type");
			
			if (MetrixStringHelper.valueIsEqual(statementType, ClientScriptBlockStatement.mScriptTypeName)) {
				JSONObject childStmtObj = statement.optJSONObject("child_statements");
				if (childStmtObj != null) {
					ClientScriptBlockStatement blockStmt = new ClientScriptBlockStatement();
					blockStmt.mChildStatements = new ArrayList<ClientScriptStatement>();
					JSONArray statementArray = childStmtObj.optJSONArray("statement");
					if (statementArray != null) {
						for (int i = 0; i < statementArray.length(); i++) {
							blockStmt.mChildStatements.add(generateStatement(statementArray.optJSONObject(i)));
						}
					} else {
						JSONObject statementObj = childStmtObj.optJSONObject("statement");
						blockStmt.mChildStatements.add(generateStatement(statementObj));
					}
					return blockStmt;
				}
			} else if (MetrixStringHelper.valueIsEqual(statementType, ClientScriptBreakStatement.mScriptTypeName)) {
				return new ClientScriptBreakStatement();
			} else if (MetrixStringHelper.valueIsEqual(statementType, ClientScriptCommentStatement.mScriptTypeName)) {
				// ignore comment blocks
				return new ClientScriptStatement();
			} else if (MetrixStringHelper.valueIsEqual(statementType, ClientScriptContinueStatement.mScriptTypeName)) {
				return new ClientScriptContinueStatement();
			} else if (MetrixStringHelper.valueIsEqual(statementType, ClientScriptElseStatement.mScriptTypeName)) {
				JSONObject innerStmtObj = statement.optJSONObject("statement");
				if (innerStmtObj != null) {
					ClientScriptElseStatement elseStmt = new ClientScriptElseStatement();
					JSONObject actualInnerStmtObj = innerStmtObj.optJSONObject("statement");
					if (actualInnerStmtObj != null) {					
						elseStmt.mStatement = generateStatement(actualInnerStmtObj);
					} else {
						elseStmt.mStatement = generateStatement(innerStmtObj);
					}				
					return elseStmt;
				}
			} else if (MetrixStringHelper.valueIsEqual(statementType, ClientScriptForStatement.mScriptTypeName)) {			
				JSONObject initStmtObj = statement.optJSONObject("init_statement");
				JSONObject conditionStmtObj = statement.optJSONObject("condition_statement");
				JSONObject endStmtObj = statement.optJSONObject("end_statement");
				JSONObject loopStmtObj = statement.optJSONObject("loop_statement");		
				if (initStmtObj != null && conditionStmtObj != null && endStmtObj != null && loopStmtObj != null) {
					// we expect any for loop to have all four standard elements
					ClientScriptForStatement forStmt = new ClientScriptForStatement();
					
					JSONObject initStmt = initStmtObj.optJSONObject("statement");
					forStmt.mInitStatement = generateStatement(initStmt);
					JSONObject conditionStmt = conditionStmtObj.optJSONObject("statement");
					forStmt.mConditionStatement = generateStatement(conditionStmt);
					JSONObject endStmt = endStmtObj.optJSONObject("statement");
					forStmt.mEndStatement = generateStatement(endStmt);
					JSONObject loopStmt = loopStmtObj.optJSONObject("statement");
					forStmt.mLoopStatement = generateStatement(loopStmt);
					
					return forStmt;
				}
			} else if (MetrixStringHelper.valueIsEqual(statementType, ClientScriptFunctionStatement.mScriptTypeName)) {
				ClientScriptFunctionStatement fcnStmt = new ClientScriptFunctionStatement();
				JSONObject expressionObj = statement.optJSONObject("expression");
				if (expressionObj != null) {
					fcnStmt.mExpression = new ArrayList<String>();
					JSONArray tokenArray = expressionObj.optJSONArray("token");
					if (tokenArray != null) {					
						for (int i = 0; i < tokenArray.length(); i++) {
							fcnStmt.mExpression.add(tokenArray.optString(i));
						}
					} else {
						fcnStmt.mExpression.add(expressionObj.optString("token"));
					}
				}
				return fcnStmt;
			} else if (MetrixStringHelper.valueIsEqual(statementType, ClientScriptIfStatement.mScriptTypeName)) {
				JSONObject ifStatementObj = statement.optJSONObject("if_statement");
				JSONObject conditionExprObj = statement.optJSONObject("condition_expression");
				if (ifStatementObj != null && conditionExprObj != null) {
					ClientScriptIfStatement ifStmt = new ClientScriptIfStatement();
					
					JSONObject ifBlockStmt = ifStatementObj.optJSONObject("statement");
					ifStmt.mIfStatement = generateStatement(ifBlockStmt);
					
					ifStmt.mConditionExpression = new ArrayList<String>();
					JSONArray tokenArray = conditionExprObj.optJSONArray("token");
					if (tokenArray != null) {					
						for (int i = 0; i < tokenArray.length(); i++) {
							ifStmt.mConditionExpression.add(tokenArray.optString(i));
						}
					} else {
						ifStmt.mConditionExpression.add(conditionExprObj.optString("token"));
					}
					
					JSONObject elseStatementObj = statement.optJSONObject("else_statement");
					if (elseStatementObj != null) {
						JSONObject elseBlockStmt = elseStatementObj.optJSONObject("statement");
						ifStmt.mElseStatement = (ClientScriptElseStatement)generateStatement(elseBlockStmt);
					}
					
					return ifStmt;
				}			
			} else if (MetrixStringHelper.valueIsEqual(statementType, ClientScriptReturnStatement.mScriptTypeName)) {
				ClientScriptReturnStatement returnStmt = new ClientScriptReturnStatement();
				JSONObject expressionObj = statement.optJSONObject("expression");
				if (expressionObj != null) {
					returnStmt.mExpression = new ArrayList<String>();
					JSONArray tokenArray = expressionObj.optJSONArray("token");
					if (tokenArray != null) {					
						for (int i = 0; i < tokenArray.length(); i++) {
							returnStmt.mExpression.add(tokenArray.optString(i));
						}
					} else {
						returnStmt.mExpression.add(expressionObj.optString("token"));
					}
				}				
				return returnStmt;
			} else if (MetrixStringHelper.valueIsEqual(statementType, ClientScriptUndeterminedStatement.mScriptTypeName)) {
				// ignore undetermined statements
				return new ClientScriptStatement();
			} else if (MetrixStringHelper.valueIsEqual(statementType, ClientScriptVarStatement.mScriptTypeName)) {
				ClientScriptVarStatement varStmt = new ClientScriptVarStatement();
				
				JSONObject exprTokensObj = statement.optJSONObject("expression_tokens");
				if (exprTokensObj != null) {
					varStmt.mExpressionTokens = new HashMap<String, ArrayList<String>>();
					JSONObject exprTokenObj = exprTokensObj.optJSONObject("expression_token");
					if (exprTokenObj != null) {
						String thisVarName = exprTokenObj.optString("variable_name");
						ArrayList<String> thisTokenSet = new ArrayList<String>();
						JSONObject expressionObj = exprTokenObj.optJSONObject("expression");
						if (expressionObj != null) {
							JSONArray tokenArray = expressionObj.optJSONArray("token");
							if (tokenArray != null) {					
								for (int j = 0; j < tokenArray.length(); j++) {
									thisTokenSet.add(tokenArray.optString(j));
								}
							} else {
								thisTokenSet.add(expressionObj.optString("token"));
							}
						}
						varStmt.mExpressionTokens.put(thisVarName, thisTokenSet);
					} else {
						JSONArray exprTokenArray = exprTokensObj.optJSONArray("expression_token");
						if (exprTokenArray != null) {					
							for (int i = 0; i < exprTokenArray.length(); i++) {
								exprTokenObj = exprTokenArray.optJSONObject(i);
								if (exprTokenObj != null) {
									String thisVarName = exprTokenObj.optString("variable_name");
									ArrayList<String> thisTokenSet = new ArrayList<String>();
									JSONObject expressionObj = exprTokenObj.optJSONObject("expression");
									if (expressionObj != null) {
										JSONArray tokenArray = expressionObj.optJSONArray("token");
										if (tokenArray != null) {					
											for (int j = 0; j < tokenArray.length(); j++) {
												thisTokenSet.add(tokenArray.optString(j));
											}
										} else {
											thisTokenSet.add(expressionObj.optString("token"));
										}
									}
									varStmt.mExpressionTokens.put(thisVarName, thisTokenSet);
								}
							}
						}
					}
				}
				
				JSONObject varNamesObj = statement.optJSONObject("variable_names");
				if (varNamesObj != null) {
					varStmt.mVariableNames = new ArrayList<String>();
					JSONArray varNameArray = varNamesObj.optJSONArray("variable_name");
					if (varNameArray != null) {					
						for (int i = 0; i < varNameArray.length(); i++) {
							varStmt.mVariableNames.add(varNameArray.optString(i));
						}
					} else {
						varStmt.mVariableNames.add(varNamesObj.optString("variable_name"));
					}
				}				
				
				return varStmt;
			} else if (MetrixStringHelper.valueIsEqual(statementType, ClientScriptWhileStatement.mScriptTypeName)) {		
				JSONObject conditionStmtObj = statement.optJSONObject("condition_statement");
				JSONObject loopStmtObj = statement.optJSONObject("loop_statement");
				if (conditionStmtObj != null && loopStmtObj != null) {
					// we expect any while loop to have both standard elements
					ClientScriptWhileStatement whileStmt = new ClientScriptWhileStatement();
					
					JSONObject conditionStmt = conditionStmtObj.optJSONObject("statement");
					whileStmt.mConditionStatement = generateStatement(conditionStmt);
					JSONObject loopStmt = loopStmtObj.optJSONObject("statement");
					whileStmt.mLoopStatement = generateStatement(loopStmt);
					
					return whileStmt;
				}
			}
		} catch (Exception e) {
			LogManager.getInstance().error(e);
			throw e;
		}
		
		return new ClientScriptStatement();
	}
}
