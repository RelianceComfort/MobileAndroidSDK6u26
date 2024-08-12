package com.metrix.architecture.scripting;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Spinner;

import com.metrix.architecture.assistants.MetrixApplicationAssistant;
import com.metrix.architecture.assistants.MetrixControlAssistant;
import com.metrix.architecture.assistants.MetrixLocationAssistant;
import com.metrix.architecture.attachment.AttachmentWidgetManager;
import com.metrix.architecture.attachment.FSMAttachmentCard;
import com.metrix.architecture.attachment.FSMAttachmentList;
import com.metrix.architecture.constants.MetrixSaveResult;
import com.metrix.architecture.constants.MetrixTransactionTypes;
import com.metrix.architecture.database.MetrixDatabaseManager;
import com.metrix.architecture.designer.MetrixListScreenManager;
import com.metrix.architecture.designer.MetrixScreenManager;
import com.metrix.architecture.designer.MetrixWorkflowManager;
import com.metrix.architecture.managers.MetrixUpdateManager;
import com.metrix.architecture.metadata.MetrixColumnDef;
import com.metrix.architecture.metadata.MetrixFormDef;
import com.metrix.architecture.metadata.MetrixKeys;
import com.metrix.architecture.metadata.MetrixSqlData;
import com.metrix.architecture.metadata.MetrixTransaction;
import com.metrix.architecture.superclasses.MetrixBaseActivity;
import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.architecture.utilities.DataField;
import com.metrix.architecture.utilities.LogManager;
import com.metrix.architecture.utilities.MetrixActivityHelper;
import com.metrix.architecture.utilities.MetrixCurrentKeysHelper;
import com.metrix.architecture.utilities.MetrixDatabases;
import com.metrix.architecture.utilities.MetrixDate;
import com.metrix.architecture.utilities.MetrixDateTime;
import com.metrix.architecture.utilities.MetrixDateTimeHelper;
import com.metrix.architecture.utilities.MetrixFloatHelper;
import com.metrix.architecture.utilities.MetrixLibraryHelper;
import com.metrix.architecture.utilities.MetrixPublicCache;
import com.metrix.architecture.utilities.MetrixRoleHelper;
import com.metrix.architecture.utilities.MetrixStringHelper;
import com.metrix.architecture.utilities.MetrixTime;
import com.metrix.architecture.utilities.MetrixTimeSpan;
import com.metrix.architecture.utilities.MetrixUIHelper;
import com.metrix.architecture.utilities.User;

import java.lang.ref.WeakReference;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;

public class MetrixClientScriptManager {
	public static String mListPopulationScreenName;
	public static final String HIDDEN_BY_SCRIPT = "HIDDEN_BY_SCRIPT";

	// key: metrix_client_script_view.unique_vs
	private static HashMap<String, ClientScriptDef> clientScripts = new HashMap<String, ClientScriptDef>();
	private static WeakReference<Activity> mCurrentActivity;
	private static WeakReference<MetrixFormDef> mFormDef;
	private static WeakReference<ViewGroup> mLayout;
	
	private static final String BREAK_STRING = "SCRIPT_BREAK";
	private static final String CONTINUE_STRING = "SCRIPT_CONTINUE";
	private static final String NULL_PLACEHOLDER_STRING = "SCRIPT_NULL";
	private static final String RETURN_STRING = "SCRIPT_RETURN";
	private static final String SEPARATOR = ",";
	private static final List<String> FUNCTION_TOKEN_LIST = Collections.unmodifiableList(Arrays.asList(
			"addToDataTransaction(",
			"advanceWorkflow(",
			"alert(",
			"clearFromCache(",
			"closeCurrentScreen(",
			"confirm(",
			"days(",
			"executeScript(",
			"generateDataTransaction(",
			"generatePrimaryKey(",
			"getAppParam(",
			"getControlValue(",
			"getCurrentKeys(",
			"getCurrentListData(",
			"getCurrentListRowControlValue(",
			"getCurrentScreenName(",
			"getCurrentTransactionType(",
			"getCurrentWorkflowName(",
			"getDBValue(",
			"getDBValues(",
			"getFromCache(",
			"getJoinTableAlias(",
			"getLatitude(",
			"getLongitude(",
			"getMessage(",
			"getRowFromListData(",
			"getUserInfo(",
			"getValueFromListDataRow(",
			"goToScreen(",
			"hours(",
			"initialValuesHaveChanged(",
			"isNullOrEmptyString(",
			"isRoleFunctionEnabled(",
			"jumpBackToScreen(",
			"launchBrowser(",
			"launchURI(",
			"launchEmailApp(",
			"launchMapByAddress(",
			"launchSMSApp(",
			"log(",
			"minutes(",
			"new Date(",
			"openAttachmentWidget(",
			"populateListFromQuery(",
			"round(",
			"saveChanges(",
			"saveDataTransaction(",
			"seconds(",
			"setCache(",
			"setControlEnabled(",
			"setControlRequired(",
			"setControlValue(",
			"setControlVisibility(",
			"setCurrentKeys(",
			"setCurrentWorkflow(",
			"setDBValue(",
			"setRowOnListData(",
			"setValueOnListDataRow(",
			"showPhoneDialer(",
			"stringFormat(",
			"stringReplace(",
			"toggleValueChangedEvent("));
	private static final List<String> TOKEN_LIST = Collections.unmodifiableList(Arrays.asList(".length", "\r\n", "\t", "&&", "||", "//",
			">=", "!=", "<=", "<>", "==", "++", "--", "=", ">", "<", "+", "-", "*", "/", "%", "(", ")", "{", "}", "?", ":", ";", ",", " "));

	public static void clearClientScriptCache() {
		if (clientScripts == null)
			clientScripts = new HashMap<String, ClientScriptDef>();
		else
			clientScripts.clear();
	}

	/**
	 * Get the ClientScriptDef for a script identifier.
	 * 
	 * @return A ClientScriptDef object representing the script to run.
	 * 
	 * @since 5.6.3
	 */
	public static ClientScriptDef getScriptDefForScriptID(String scriptId) {
		ClientScriptDef scriptDef = null;
		try {
			if (!MetrixStringHelper.isNullOrEmpty(scriptId)) {
				if (clientScripts == null)
					clientScripts = new HashMap<String, ClientScriptDef>();
				
				if (clientScripts.containsKey(scriptId)) {
					scriptDef = clientScripts.get(scriptId);
				} else {
					String scriptJSON = MetrixDatabaseManager.getFieldStringValue("metrix_client_script_view", "script", String.format("unique_vs = '%s'", scriptId));
					if (!MetrixStringHelper.isNullOrEmpty(scriptJSON)) {
						scriptDef = ClientScriptParser.deserializeScriptJSON(scriptJSON);
						clientScripts.put(scriptId, scriptDef);
					}
				}
			}
		} catch (Exception e) {
			LogManager.getInstance().error(e);
		}
		return scriptDef;
	}

	/**
	 * Get the ClientScriptDef for a client script id by finding
	 * a. highest published version
	 * *OR*
	 * b. the only pending version, if no published version is found
	 *
	 * @return A ClientScriptDef object representing the script to run.
	 *
	 * @since 5.7.0
	 */
	public static ClientScriptDef getScriptDefForUnversionedScriptIdentifier(String unversionedScriptId) {
		ClientScriptDef scriptDef = null;
		try {
			String versionToUse = MetrixDatabaseManager.getFieldStringValue(String.format("select version_number from metrix_client_script_view where client_script_id = '%s' and status = 'PUBLISHED' order by version_number desc limit 1", unversionedScriptId));
			if (MetrixStringHelper.isNullOrEmpty(versionToUse))
				versionToUse = MetrixDatabaseManager.getFieldStringValue(String.format("select version_number from metrix_client_script_view where client_script_id = '%s' and status = 'PENDING' order by version_number desc limit 1", unversionedScriptId));

			if (!MetrixStringHelper.isNullOrEmpty(versionToUse)) {
				String uniqueVS = String.format("%1$s__%2$s", unversionedScriptId, versionToUse);
				scriptDef = getScriptDefForScriptID(uniqueVS);
			}
		} catch (Exception e) {
			LogManager.getInstance().error(e);
		}
		return scriptDef;
	}

	/**
	 * Execute the code represented by the ClientScriptDef.
	 * 
	 * @return A boolean value representing execution success.
	 * 
	 * @since 5.6.3
	 */
	public static boolean executeScript(WeakReference<Activity> activity, ClientScriptDef scriptDef) {
		try {
			if (scriptDef != null && scriptDef.mStatements != null && scriptDef.mStatements.size() > 0) {
				mCurrentActivity = activity;
				mFormDef = new WeakReference<MetrixFormDef>((MetrixFormDef) MetrixPublicCache.instance.getItem("theCurrentFormDef"));
				mLayout = new WeakReference<ViewGroup>((ViewGroup) MetrixPublicCache.instance.getItem("theCurrentLayout"));
				scriptDef.clearVariables();
				for (ClientScriptStatement statement : scriptDef.mStatements) {
					Object stmtResult = executeStatement(statement, scriptDef);
					if (stmtResult instanceof MetrixScriptFlowControl) {
						MetrixScriptFlowControl msfc = (MetrixScriptFlowControl)stmtResult;
						if (msfc.mKeyString == RETURN_STRING) {
							Object returnValue = MetrixPublicCache.instance.getItem("scriptingReturnValue");
							MetrixPublicCache.instance.removeItem("scriptingReturnValue");
							if (returnValue != null && returnValue instanceof Boolean) {
								Boolean success = (Boolean)returnValue;
								return success;		// this will be particularly useful for VALIDATION events
							} else
								return true;	// still stop execution here, since we did encounter a return
						}
					}
				}
			}
		} catch (Exception e) {
			LogManager.getInstance().error(e);
			toastClientScriptExceptionToDeveloper(e);
			return false;
		}
		return true;
	}
	
	/**
	 * Execute the code represented by the ClientScriptDef.
	 * 
	 * @return A string value provided by the script as output.
	 * 
	 * @since 5.6.3
	 */
	public static String executeScriptReturningString(WeakReference<Activity> activity, ClientScriptDef scriptDef) {
		try {
			if (scriptDef != null && scriptDef.mStatements != null && scriptDef.mStatements.size() > 0) {
				mCurrentActivity = activity;
				mFormDef = new WeakReference<MetrixFormDef>((MetrixFormDef) MetrixPublicCache.instance.getItem("theCurrentFormDef"));
				mLayout = new WeakReference<ViewGroup>((ViewGroup) MetrixPublicCache.instance.getItem("theCurrentLayout"));
				scriptDef.clearVariables();
				for (ClientScriptStatement statement : scriptDef.mStatements) {
					Object stmtResult = executeStatement(statement, scriptDef);
					if (stmtResult instanceof MetrixScriptFlowControl) {
						MetrixScriptFlowControl msfc = (MetrixScriptFlowControl)stmtResult;
						if (msfc.mKeyString == RETURN_STRING) {
							Object returnValue = MetrixPublicCache.instance.getItem("scriptingReturnValue");
							MetrixPublicCache.instance.removeItem("scriptingReturnValue");
							if (returnValue != null && returnValue instanceof String)
								return (String)returnValue;
							else
								return null;	// still stop execution here, since we did encounter a return
						}
					}
				}
			}
		} catch (Exception e) {
			LogManager.getInstance().error(e);
			toastClientScriptExceptionToDeveloper(e);
		}
		return null;
	}

	/**
	 * Execute the code represented by the ClientScriptDef.
	 *
	 * @return An object provided by the script as output.
	 *
	 * @since 5.7.0
	 */
	public static Object executeScriptReturningObject(WeakReference<Activity> activity, ClientScriptDef scriptDef) {
		try {
			if (scriptDef != null && scriptDef.mStatements != null && scriptDef.mStatements.size() > 0) {
				mCurrentActivity = activity;
				mFormDef = new WeakReference<MetrixFormDef>((MetrixFormDef) MetrixPublicCache.instance.getItem("theCurrentFormDef"));
				mLayout = new WeakReference<ViewGroup>((ViewGroup) MetrixPublicCache.instance.getItem("theCurrentLayout"));
				scriptDef.clearVariables();
				for (ClientScriptStatement statement : scriptDef.mStatements) {
					Object stmtResult = executeStatement(statement, scriptDef);
					if (stmtResult instanceof MetrixScriptFlowControl) {
						MetrixScriptFlowControl msfc = (MetrixScriptFlowControl)stmtResult;
						if (msfc.mKeyString == RETURN_STRING) {
							Object returnValue = MetrixPublicCache.instance.getItem("scriptingReturnValue");
							MetrixPublicCache.instance.removeItem("scriptingReturnValue");
							return returnValue;
						}
					}
				}
			}
		} catch (Exception e) {
			LogManager.getInstance().error(e);
			toastClientScriptExceptionToDeveloper(e);
		}
		return null;
	}
	
	private static void toastClientScriptExceptionToDeveloper(Exception e) {
		String personType = MetrixDatabaseManager.getFieldStringValue("person", "metrix_user_type", "person_id = '" + User.getUser().personId + "'");
		if (MetrixStringHelper.valueIsEqual(personType, "STUDIO")) {
			MetrixUIHelper.showSnackbar(mCurrentActivity.get(), e.getMessage());
		}
	}
	
	private static Object executeStatement(ClientScriptStatement statement, ClientScriptDef scriptDef) throws Exception {
		try {
			if (statement instanceof ClientScriptBlockStatement) {
				ClientScriptBlockStatement blockStmt = (ClientScriptBlockStatement)statement;
				if (blockStmt.mChildStatements != null && blockStmt.mChildStatements.size() > 0) {
					for (ClientScriptStatement childStmt : blockStmt.mChildStatements) {
						Object result = executeStatement(childStmt, scriptDef);
						if (result instanceof MetrixScriptFlowControl) {
							MetrixScriptFlowControl msfc = (MetrixScriptFlowControl)result;
							return msfc;
						}
					}
				}
			} else if (statement instanceof ClientScriptBreakStatement) {
				return new MetrixScriptFlowControl(BREAK_STRING);
			} else if (statement instanceof ClientScriptContinueStatement) {
				return new MetrixScriptFlowControl(CONTINUE_STRING);
			} else if (statement instanceof ClientScriptElseStatement) {
				ClientScriptElseStatement elseStmt = (ClientScriptElseStatement)statement;
				if (elseStmt.mStatement != null) {
					Object result = executeStatement(elseStmt.mStatement, scriptDef);
					if (result instanceof MetrixScriptFlowControl) {
						MetrixScriptFlowControl msfc = (MetrixScriptFlowControl)result;
						return msfc;
					}
				}
			} else if (statement instanceof ClientScriptForStatement) {
				ClientScriptForStatement forStmt = (ClientScriptForStatement)statement;
				executeStatement(forStmt.mInitStatement, scriptDef);
				while ((Boolean)executeStatement(forStmt.mConditionStatement, scriptDef)) {
					Object result = executeStatement(forStmt.mLoopStatement, scriptDef);
					if (result instanceof MetrixScriptFlowControl) {
						MetrixScriptFlowControl msfc = (MetrixScriptFlowControl)result;
						if (msfc.mKeyString == CONTINUE_STRING) { /* do nothing, and let loop continue */ }
						else if (msfc.mKeyString == BREAK_STRING)
							break;
						else if (msfc.mKeyString == RETURN_STRING)
							return msfc;
					}
					executeStatement(forStmt.mEndStatement, scriptDef);
				}
			} else if (statement instanceof ClientScriptFunctionStatement) {
				ClientScriptFunctionStatement fcnStmt = (ClientScriptFunctionStatement)statement;
				ArrayList<String> expressionTokens = fcnStmt.mExpression;
				if (expressionTokens != null && expressionTokens.size() > 0) {
					String expressionAsString = convertTokensToText(expressionTokens);
					ArrayList<String> postfixTokens = convertInfixToPostfix(expressionAsString, expressionTokens);
					Object expressionValue = evaluatePostfixTokens(expressionAsString, postfixTokens, scriptDef);
					return expressionValue;
				}				
			} else if (statement instanceof ClientScriptIfStatement) {
				ClientScriptIfStatement ifStmt = (ClientScriptIfStatement)statement;
				if (ifStmt.mConditionExpression == null || ifStmt.mConditionExpression.size() == 0)
					throw new Exception(AndroidResourceHelper.getMessage("ExIfStmtMissingCon2Args", scriptDef.mClientScriptId, scriptDef.mVersionNumber));
				
				String expressionAsString = convertTokensToText(ifStmt.mConditionExpression);
				ArrayList<String> postfixTokens = convertInfixToPostfix(expressionAsString, ifStmt.mConditionExpression);
				Object conditionValue = evaluatePostfixTokens(expressionAsString, postfixTokens, scriptDef);			
				if (conditionValue instanceof Boolean) {
					Boolean conditionBool = (Boolean)conditionValue;
					if (conditionBool && ifStmt.mIfStatement != null) {
						Object result = executeStatement(ifStmt.mIfStatement, scriptDef);
						if (result instanceof MetrixScriptFlowControl) {
							MetrixScriptFlowControl msfc = (MetrixScriptFlowControl)result;
							return msfc;
						}
					}
					else if (!conditionBool && ifStmt.mElseStatement != null) {
						Object result = executeStatement(ifStmt.mElseStatement, scriptDef);
						if (result instanceof MetrixScriptFlowControl) {
							MetrixScriptFlowControl msfc = (MetrixScriptFlowControl)result;
							return msfc;
						}
					}
				}
			} else if (statement instanceof ClientScriptReturnStatement) {
				ClientScriptReturnStatement returnStmt = (ClientScriptReturnStatement)statement;
				ArrayList<String> expressionTokens = returnStmt.mExpression;
				if (expressionTokens != null && expressionTokens.size() > 0) {
					String expressionAsString = convertTokensToText(expressionTokens);
					ArrayList<String> postfixTokens = convertInfixToPostfix(expressionAsString, expressionTokens);
					MetrixPublicCache.instance.addItem("scriptingReturnValue", evaluatePostfixTokens(expressionAsString, postfixTokens, scriptDef));				
				}
				return new MetrixScriptFlowControl(RETURN_STRING);
			} else if (statement instanceof ClientScriptVarStatement) {
				// This block handles two types of statements that share most of the same pattern:
                // var statements such as: var variableName = "value"; or var variableName1 = "value", variableName2;
                // "var"-less variable assignments such as: variableName = "value";
				ClientScriptVarStatement varStmt = (ClientScriptVarStatement)statement;
				if (varStmt.mVariableNames != null) {
					for (String variableName : varStmt.mVariableNames) {
						ArrayList<String> expressionTokens = (varStmt.mExpressionTokens.containsKey(variableName)) ? varStmt.mExpressionTokens.get(variableName) : null;
						Object variableValue = null;
						
						if (expressionTokens != null && expressionTokens.size() > 0) {
							String expressionAsString = convertTokensToText(expressionTokens);
							ArrayList<String> postfixTokens = convertInfixToPostfix(expressionAsString, expressionTokens);
							variableValue = evaluatePostfixTokens(expressionAsString, postfixTokens, scriptDef);
						}
						
						scriptDef.mVariables.put(variableName, variableValue);
					}
				}
			} else if (statement instanceof ClientScriptWhileStatement) {
				ClientScriptWhileStatement whileStmt = (ClientScriptWhileStatement)statement;
				while ((Boolean)executeStatement(whileStmt.mConditionStatement, scriptDef)) {
					Object result = executeStatement(whileStmt.mLoopStatement, scriptDef);
					if (result instanceof MetrixScriptFlowControl) {
						MetrixScriptFlowControl msfc = (MetrixScriptFlowControl)result;
						if (msfc.mKeyString == CONTINUE_STRING) { /* do nothing, and let loop continue */ }
						else if (msfc.mKeyString == BREAK_STRING)
							break;
						else if (msfc.mKeyString == RETURN_STRING)
							return msfc;
					}
				}
			}
		} catch (Exception e) {
			// something bad actually happened; augment for log and notification and rethrow
			String msg = e.getMessage();
			if (!msg.startsWith("[Script ID: ")) {
				Exception augmentedEx = new Exception(String.format("[Script ID: %1$s, v%2$s] %3$s", scriptDef.mClientScriptId, scriptDef.mVersionNumber, msg), e);
				throw augmentedEx;
			} else
				throw e;
		}
		
		return true;
	}
	
	private static String convertTokensToText(ArrayList<String> tokens) {
		String text = "";	
		if (tokens != null && tokens.size() > 0) {
			StringBuilder textBuilder = new StringBuilder();
			for (String token : tokens) {
				textBuilder.append(token);
				textBuilder.append(" ");
			}
			text = textBuilder.toString().trim();
		}	
		return text;
	}
	
	private static ArrayList<String> convertInfixToPostfix(String expressionIn, ArrayList<String> infixTokensIn) throws Exception {
		ArrayDeque<String> operatorStack = new ArrayDeque<String>();
		ArrayList<String> postfixTokens = new ArrayList<String>();	
		try {
			for (String token : infixTokensIn) {
				int operatorPrecedence = -1;
				int operandNumber = -1;
							
				if (MetrixStringHelper.valueIsEqual(token, "(")) {
					// we are starting a parenthetical notation
					operatorStack.push(token);
				} else if (MetrixStringHelper.valueIsEqual(token, ")")) {
					// we are finishing a parenthetical notation
					String operatorValue = "";				
					do {
						if (operatorStack.size() == 0)
							throw new Exception (AndroidResourceHelper.getMessage("ExMismatchedParen1Args", expressionIn));
						
						operatorValue = operatorStack.pop();
						if (!MetrixStringHelper.valueIsEqual(operatorValue, "("))
							postfixTokens.add(operatorValue);
					} while (!MetrixStringHelper.valueIsEqual(operatorValue, "("));
				} else {
					// first try to process as an operator
					int[] operatorNums = isOperator(token);
					operatorPrecedence = operatorNums[0];
					operandNumber = operatorNums[1];
					if (operatorPrecedence > -1 && operandNumber > -1) {
						if (operatorStack.size() == 0) {
							// If this is our only operator, then push it on our stack.
							operatorStack.push(token);
						} else {
							// If we have other operators already, check the precedence of the top operator.
	                        // Push if the top operator has lower precedence;
							// otherwise pop the current top and then push the new.
							String topOperator = operatorStack.peek();
							int topPrecedence = isOperator(topOperator)[0];
							while (topPrecedence >= operatorPrecedence) {
								postfixTokens.add(operatorStack.pop());
								
								if (operatorStack.size() == 0)
									break;
								
								topOperator = operatorStack.peek();
								topPrecedence = isOperator(topOperator)[0];
							}
							
							operatorStack.push(token);
						}
					} else {
						// otherwise, just tack onto postfixTokens
						postfixTokens.add(token);
					}
				}
			}
			
			// Add remaining operators from the stack to the postfix token list.
			while (operatorStack.size() > 0) {
				String operatorValue = operatorStack.pop();
				
				// However, none of these operators should be parentheses.
				if (MetrixStringHelper.valueIsEqual(operatorValue, "(") || MetrixStringHelper.valueIsEqual(operatorValue, ")")) {
					throw new Exception (AndroidResourceHelper.getMessage("'ExMismatchedParen1Args", expressionIn));
				}
				
				postfixTokens.add(operatorValue);
			}
		} catch (Exception e) {
			LogManager.getInstance().error(e);
			throw e;
		}
		
		return postfixTokens;
	}
	
	@SuppressLint("DefaultLocale")
	private static int[] isOperator(String tokenIn) {
		// resultArray[0] = PrecedenceOut
		// resultArray[1] = OperandNumberOut
		// token is NOT operator IF both numbers are -1
		int[] resultArray = new int[] {-1, -1};
		
		if (!MetrixStringHelper.isNullOrEmpty(tokenIn)) {
			tokenIn = tokenIn.toLowerCase();
			if (MetrixStringHelper.valueIsEqual(tokenIn, ".length")) {
				resultArray[0] = 9;
				resultArray[1] = 1;
			} else if (MetrixStringHelper.valueIsEqual(tokenIn, "++") || MetrixStringHelper.valueIsEqual(tokenIn, "--")) {
				resultArray[0] = 8;
				resultArray[1] = 1;
			} else if (MetrixStringHelper.valueIsEqual(tokenIn, "*") || MetrixStringHelper.valueIsEqual(tokenIn, "/") || MetrixStringHelper.valueIsEqual(tokenIn, "%")) {
				resultArray[0] = 7;
				resultArray[1] = 2;
			} else if (MetrixStringHelper.valueIsEqual(tokenIn, "+") || MetrixStringHelper.valueIsEqual(tokenIn, "-")) {
				resultArray[0] = 6;
				resultArray[1] = 2;
			} else if (MetrixStringHelper.valueIsEqual(tokenIn, "<") || MetrixStringHelper.valueIsEqual(tokenIn, "<=")
					|| MetrixStringHelper.valueIsEqual(tokenIn, ">") || MetrixStringHelper.valueIsEqual(tokenIn, ">=")) {
				resultArray[0] = 5;
				resultArray[1] = 2;
			} else if (MetrixStringHelper.valueIsEqual(tokenIn, "==") || MetrixStringHelper.valueIsEqual(tokenIn, "!=")) {
				resultArray[0] = 4;
				resultArray[1] = 2;
			} else if (MetrixStringHelper.valueIsEqual(tokenIn, "&&")) {
				resultArray[0] = 3;
				resultArray[1] = 2;
			} else if (MetrixStringHelper.valueIsEqual(tokenIn, "||")) {
				resultArray[0] = 2;
				resultArray[1] = 2;
			} else if (MetrixStringHelper.valueIsEqual(tokenIn, "?")) {
				// First part of the ternary operator
				resultArray[0] = 1;
				resultArray[1] = 0; // This portion of the two-part operator needs no action.
			} else if (MetrixStringHelper.valueIsEqual(tokenIn, ":")) {
				// Second part of the ternary operator
				resultArray[0] = 1;
				resultArray[1] = 3; // This portion takes care of the actual work.
			} else if (tokenIn.startsWith("[") && tokenIn.endsWith("]")) {
				resultArray[0] = 9;
				resultArray[1] = 1;
			}
		}
		
		return resultArray;
	}
	
	@SuppressLint("DefaultLocale") @SuppressWarnings("unchecked")
	private static Object executeOperation(String expressionIn, Object firstOperandIn, String operatorIn, Object secondOperandIn, Object thirdOperandIn, ClientScriptDef clientScript, boolean isForDBQuery) throws Exception {
		try {
			operatorIn = operatorIn.toLowerCase();
			
			// Some operations require us to know both the name and value of a variable 
			// so we may start with a variable name and need to determine the value.
            // Determine whether we need to get any values from variables first.
            // At this point only single operand operations need to know the variable name.
			String firstVariableName = null;
			Object firstOperandValue = null;
			Object secondOperandValue = null;
			Object thirdOperandValue = null;
			
			if (firstOperandIn instanceof ClientScriptVariableItem) {
				firstVariableName = ((ClientScriptVariableItem)firstOperandIn).mName;
				firstOperandValue = getVariableValue(clientScript, firstVariableName);
			} else
				firstOperandValue = firstOperandIn;
			
			if (secondOperandIn instanceof ClientScriptVariableItem)
				secondOperandValue = getVariableValue(clientScript, ((ClientScriptVariableItem)secondOperandIn).mName);
			else
				secondOperandValue = secondOperandIn;
			
			if (thirdOperandIn instanceof ClientScriptVariableItem)
				thirdOperandValue = getVariableValue(clientScript, ((ClientScriptVariableItem)thirdOperandIn).mName);
			else
				thirdOperandValue = thirdOperandIn;
			
			
			boolean op1IsHashtable = firstOperandValue instanceof Hashtable<?,?>;
			boolean op1IsArrayList = firstOperandValue instanceof ArrayList<?>;
			boolean op1IsNull = firstOperandValue == null;
			boolean op2IsNull = secondOperandValue == null;
			boolean op3IsNull = thirdOperandValue == null;
			boolean op1IsDate = firstOperandValue instanceof Date;
			boolean op2IsDate = secondOperandValue instanceof Date;
			boolean op1IsTimeSpan = firstOperandValue instanceof MetrixTimeSpan;
			boolean op2IsTimeSpan = secondOperandValue instanceof MetrixTimeSpan;
			boolean op1IsDouble = firstOperandValue instanceof Double;
			boolean op2IsDouble = secondOperandValue instanceof Double;
			boolean op1IsBoolean = firstOperandValue instanceof Boolean;
			boolean op2IsBoolean = secondOperandValue instanceof Boolean;
			boolean op1IsString = firstOperandValue instanceof String;
			boolean op2IsString = secondOperandValue instanceof String;
			
			Hashtable<String, Object> hashtable1 = null;
			ArrayList<Hashtable<String, Object>> arrayListOfHashtables1 = null;
			Date date1 = null;
			Date date2 = null;
			MetrixTimeSpan timeSpan1 = null;
			MetrixTimeSpan timeSpan2 = null;
			Double dbl1 = null;
			Double dbl2 = null;
			Boolean bool1 = null;
			Boolean bool2 = null;
			String string1 = null;
			String string2 = null;
			
			
			if (op1IsHashtable)
				hashtable1 = (Hashtable<String, Object>)firstOperandValue;
			else if (op1IsArrayList)
				arrayListOfHashtables1 = (ArrayList<Hashtable<String, Object>>)firstOperandValue;
			else if (op1IsDate)
				date1 = (Date)firstOperandValue;
			else if (op1IsTimeSpan)
				timeSpan1 = (MetrixTimeSpan)firstOperandValue;
			else if (op1IsDouble)
				dbl1 = (Double)firstOperandValue;
			else if (op1IsBoolean)
				bool1 = (Boolean)firstOperandValue;
			else if (op1IsString)
				string1 = (String)firstOperandValue;
			
			if (op2IsDate)
				date2 = (Date)secondOperandValue;
			else if (op2IsTimeSpan)
				timeSpan2 = (MetrixTimeSpan)secondOperandValue;
			else if (op2IsDouble)
				dbl2 = (Double)secondOperandValue;
			else if (op2IsBoolean)
				bool2 = (Boolean)secondOperandValue;
			else if (op2IsString)
				string2 = (String)secondOperandValue;
				
			if (MetrixStringHelper.valueIsEqual(operatorIn, "==")) {
				if (!op1IsNull && !op2IsNull) {
					if (op1IsDate && op2IsDate)
						return (date1.compareTo(date2) == 0);
					else if (op1IsTimeSpan && op2IsTimeSpan)
						return (timeSpan1.mMilliseconds == timeSpan2.mMilliseconds);
					else if (op1IsDouble && op2IsDouble)
						return dbl1.compareTo(dbl2) == 0;
					else if (op1IsBoolean && op2IsBoolean)
						return bool1 == bool2;
					else if (op1IsString && op2IsString)
						return MetrixStringHelper.valueIsEqual(string1, string2);
				} else
					return (op1IsNull && op2IsNull);
			} else if (MetrixStringHelper.valueIsEqual(operatorIn, "!=")) {
				if (!op1IsNull && !op2IsNull) {
					if (op1IsDate && op2IsDate)
						return (date1.compareTo(date2) != 0);
					else if (op1IsTimeSpan && op2IsTimeSpan)
						return (timeSpan1.mMilliseconds != timeSpan2.mMilliseconds);
					else if (op1IsDouble && op2IsDouble)
						return dbl1.compareTo(dbl2) != 0;
					else if (op1IsBoolean && op2IsBoolean)
						return bool1 != bool2;
					else if (op1IsString && op2IsString)
						return (!MetrixStringHelper.valueIsEqual(string1, string2));
				} else
					return ((!op1IsNull && op2IsNull) || (op1IsNull && !op2IsNull));
			} else if (MetrixStringHelper.valueIsEqual(operatorIn, "<")) {
				if (!op1IsNull && !op2IsNull) {
					if (op1IsDate && op2IsDate)
						return (date1.compareTo(date2) < 0);
					else if (op1IsTimeSpan && op2IsTimeSpan)
						return (timeSpan1.mMilliseconds < timeSpan2.mMilliseconds);
					else if (op1IsDouble && op2IsDouble)
						return dbl1.compareTo(dbl2) < 0;
					else if (op1IsString && op2IsString)
						return (string1.compareTo(string2) < 0);
				} else
					return false;				
			} else if (MetrixStringHelper.valueIsEqual(operatorIn, "<=")) {
				if (!op1IsNull && !op2IsNull) {
					if (op1IsDate && op2IsDate)
						return (date1.compareTo(date2) <= 0);
					else if (op1IsTimeSpan && op2IsTimeSpan)
						return (timeSpan1.mMilliseconds <= timeSpan2.mMilliseconds);
					else if (op1IsDouble && op2IsDouble)
						return dbl1.compareTo(dbl2) <= 0;
					else if (op1IsString && op2IsString)
						return (string1.compareTo(string2) <= 0);				
				} else if (op1IsNull && op2IsNull)
					return true;
				else
					return false;
			} else if (MetrixStringHelper.valueIsEqual(operatorIn, ">")) {
				if (!op1IsNull && !op2IsNull) {
					if (op1IsDate && op2IsDate)
						return (date1.compareTo(date2) > 0);
					else if (op1IsTimeSpan && op2IsTimeSpan)
						return (timeSpan1.mMilliseconds > timeSpan2.mMilliseconds);
					else if (op1IsDouble && op2IsDouble)
						return dbl1.compareTo(dbl2) > 0;
					else if (op1IsString && op2IsString)
						return (string1.compareTo(string2) > 0);
				} else
					return false;				
			} else if (MetrixStringHelper.valueIsEqual(operatorIn, ">=")) {
				if (!op1IsNull && !op2IsNull) {
					if (op1IsDate && op2IsDate)
						return (date1.compareTo(date2) >= 0);
					else if (op1IsTimeSpan && op2IsTimeSpan)
						return (timeSpan1.mMilliseconds >= timeSpan2.mMilliseconds);
					else if (op1IsDouble && op2IsDouble)
						return dbl1.compareTo(dbl2) >= 0;
					else if (op1IsString && op2IsString)
						return (string1.compareTo(string2) >= 0);					
				} else if (op1IsNull && op2IsNull)
					return true;
				else
					return false;
			} else if (MetrixStringHelper.valueIsEqual(operatorIn, ":")) {
				// Ternary Operation. 
                // The First operand should be a boolean but the second and third can be anything.
                // If the first operand is true, then return the second operand ... otherwise return the third.
				if (op1IsBoolean) {
					if (bool1)
						return (op2IsNull ? NULL_PLACEHOLDER_STRING : secondOperandValue);
					else
						return (op3IsNull ? NULL_PLACEHOLDER_STRING : thirdOperandValue);
				} else
					throw new Exception (AndroidResourceHelper.getMessage("ExCannotExecuteTernOp2Args", expressionIn, (op1IsNull ? "{null}" : firstOperandValue.toString())));
			} else if (MetrixStringHelper.valueIsEqual(operatorIn, "&&")) {
				if (!op1IsNull && !op2IsNull) {
					if (op1IsBoolean && op2IsBoolean)
						return bool1 && bool2;
				}
			} else if (MetrixStringHelper.valueIsEqual(operatorIn, "||")) {
				if (!op1IsNull && !op2IsNull) {
					if (op1IsBoolean && op2IsBoolean)
						return bool1 || bool2;
				}
			} else if (MetrixStringHelper.valueIsEqual(operatorIn, "*")) {
				if (!op1IsNull && !op2IsNull) {
					if (op1IsDouble && op2IsDouble)
						return dbl1 * dbl2;
				} else if ((!op1IsNull && op1IsDouble) || (!op2IsNull && op2IsDouble))
					return Double.valueOf(0);
				else if (op1IsNull && op2IsNull)
					return NULL_PLACEHOLDER_STRING;
			} else if (MetrixStringHelper.valueIsEqual(operatorIn, "/")) {
				if (!op1IsNull && !op2IsNull) {
					if (op1IsDouble && op2IsDouble)
						return dbl1 / dbl2;
				} else if (!op2IsNull && op2IsDouble)
					return Double.valueOf(0);
			} else if (MetrixStringHelper.valueIsEqual(operatorIn, "%")) {
				if (!op1IsNull && !op2IsNull) {
					if (op1IsDouble && op2IsDouble) {
						BigDecimal bd1 = BigDecimal.valueOf(dbl1);
						BigDecimal bd2 = BigDecimal.valueOf(dbl2);
						return bd1.remainder(bd2).doubleValue();
					}
				} else if (!op2IsNull && op2IsDouble)
					return Double.valueOf(0);
			} else if (MetrixStringHelper.valueIsEqual(operatorIn, "+")) {
				// where we attempt to do string concatenation against mixed data types,
				// use isForDBQuery to determine how to format Doubles/Dates
				if (!op1IsNull && !op2IsNull) {
					if (op1IsDouble && op2IsDouble)
						return dbl1 + dbl2;
					else if (op1IsString && op2IsString)
						return string1 + string2;
					else if (op1IsString && op2IsDouble) {
						if (isForDBQuery)
							return string1 + MetrixFloatHelper.convertNumericFromForcedLocaleToDB(String.valueOf(dbl2), Locale.US);
						else
							return string1 + MetrixFloatHelper.currentLocaleNumericValue(dbl2);
					} else if (op1IsString && op2IsBoolean)
						return string1 + String.valueOf(bool2);
					else if (op1IsString && op2IsDate) {
						if (isForDBQuery)
							return string1 + MetrixDateTimeHelper.convertDateTimeFromDateToDB(date2);
						else
							return string1 + MetrixDateTimeHelper.convertDateTimeFromDateToUI(date2);
					} else if (op1IsDouble && op2IsString) {
						if (isForDBQuery)
							return MetrixFloatHelper.convertNumericFromForcedLocaleToDB(String.valueOf(dbl1), Locale.US) + string2;
						else
							return MetrixFloatHelper.currentLocaleNumericValue(dbl1) + string2;
					} else if (op1IsBoolean && op2IsString)
						return String.valueOf(bool1) + string2;
					else if (op1IsDate && op2IsString) {
						if (isForDBQuery)
							return MetrixDateTimeHelper.convertDateTimeFromDateToDB(date1) + string2;
						else
							return MetrixDateTimeHelper.convertDateTimeFromDateToUI(date1) + string2;
					} else if (op1IsDate && op2IsTimeSpan)
    					return MetrixDateTimeHelper.adjustDate(date1, timeSpan2, true);
					else if (op1IsTimeSpan && op2IsDate)
    					return MetrixDateTimeHelper.adjustDate(date2, timeSpan1, true);
					else if (op1IsTimeSpan && op2IsTimeSpan)
						return new MetrixTimeSpan(timeSpan1.mMilliseconds + timeSpan2.mMilliseconds);
				} else if (!op1IsNull && (op1IsString || op1IsDouble || op1IsDate))
					return firstOperandValue;
				else if (!op2IsNull && (op2IsString || op2IsDouble || op2IsDate))
					return secondOperandValue;
				else
					return NULL_PLACEHOLDER_STRING;
			} else if (MetrixStringHelper.valueIsEqual(operatorIn, "-")) {
				if (!op1IsNull && !op2IsNull) {
					if (op1IsDouble && op2IsDouble)
						return dbl1 - dbl2;
					else if (op1IsDate && op2IsTimeSpan)
    					return MetrixDateTimeHelper.adjustDate(date1, timeSpan2, false);
					else if (op1IsDate && op2IsDate)
						return MetrixDateTimeHelper.getDateDifference(date1, date2);
					else if (op1IsTimeSpan && op2IsTimeSpan)
						return new MetrixTimeSpan(timeSpan1.mMilliseconds - timeSpan2.mMilliseconds);
				} else if (!op1IsNull && op1IsDouble)
					return dbl1;
				else if (!op2IsNull && op2IsDouble)
					return Double.valueOf(0 - dbl2);
				else
					return NULL_PLACEHOLDER_STRING;
			} else if (MetrixStringHelper.valueIsEqual(operatorIn, "++")) {
				if (!op1IsNull) {
					if (op1IsDouble) {
						Double incrValue = Double.valueOf(dbl1 + 1);
						if (!MetrixStringHelper.isNullOrEmpty(firstVariableName))
							clientScript.mVariables.put(firstVariableName, incrValue);
						return incrValue;
					}
				}
			} else if (MetrixStringHelper.valueIsEqual(operatorIn, "--")) {
				if (!op1IsNull) {
					if (op1IsDouble) {
						Double decrValue = Double.valueOf(dbl1 - 1);
						if (!MetrixStringHelper.isNullOrEmpty(firstVariableName))
							clientScript.mVariables.put(firstVariableName, decrValue);
						return decrValue;
					}
				}
			} else if (MetrixStringHelper.valueIsEqual(operatorIn, ".length")) {
				if (!op1IsNull) {
					if (op1IsString)
						return Double.valueOf(string1.length());
					else if (op1IsDouble)
						return Double.valueOf(String.valueOf(dbl1).length());
					else if (op1IsTimeSpan)
						return Double.valueOf(timeSpan1.mMilliseconds);
					else if (op1IsArrayList)
						return Double.valueOf(arrayListOfHashtables1.size());
					else if (op1IsHashtable)
						return Double.valueOf(hashtable1.size());
				}
			}
			
			if (operatorIn.startsWith("[") && operatorIn.endsWith("]")) {
				String subscriptExpression = operatorIn.substring(1, operatorIn.length() - 1);
				Object subscriptValue = executeSubExpression(subscriptExpression, clientScript);
				
				if (!op1IsNull) {
					if (op1IsArrayList) {
						if (subscriptValue instanceof Double) {
							Double subscriptDouble = (Double)subscriptValue;
							return arrayListOfHashtables1.get(subscriptDouble.intValue());
						}
					} else if (op1IsHashtable) {
						if (subscriptValue instanceof String) {
							return hashtable1.get(String.valueOf(subscriptValue));
						}
					}
				}
			}
			
			// If we get this far, we have an operation that can't be performed.
			String leftOperandString = op1IsNull ? "{null}" : firstOperandValue.toString();
			String rightOperandString = op2IsNull ? "{null}" : secondOperandValue.toString();
			throw new Exception(AndroidResourceHelper.getMessage("ExTheOpCannotPerform4Args", leftOperandString, operatorIn, rightOperandString, expressionIn));
		} catch (Exception e) {
			LogManager.getInstance().error(e);
			throw e;
		}
	}

	private static Object evaluatePostfixTokens(String expressionIn, ArrayList<String> postfixTokensIn, ClientScriptDef clientScript) throws Exception {
		return evaluatePostfixTokens(expressionIn, postfixTokensIn, clientScript, false);
	}
	
	@SuppressLint({ "SimpleDateFormat", "DefaultLocale" }) @SuppressWarnings({"unchecked", "MissingPermission"})
	private static Object evaluatePostfixTokens(String expressionIn, ArrayList<String> postfixTokensIn, ClientScriptDef clientScript, boolean isForDBQuery) throws Exception {
		ArrayDeque<Object> operandStack = new ArrayDeque<Object>();
		for (String token : postfixTokensIn) {
			// Determine whether the token is an operand or an operator.
			int operandNumber = isOperator(token)[1];
			if (operandNumber > -1) {
				Object firstOperand = null;
                Object secondOperand = null;
                Object thirdOperand = null;
                
                if (operandNumber == 0) {
                    // This operator does not actually perform an action.  We can just ignore it and move on.
                    // The only current operator like this is the ? portion of the ternary operator.
                } else {
                	if (operandNumber == 1) {
                		if (operandStack.size() < 1)
                			throw new Exception(AndroidResourceHelper.getMessage("TheExpConUnexpectedSyn4Args", expressionIn, token, String.valueOf(operandNumber), String.valueOf(operandStack.size())));
                	
                		firstOperand = operandStack.pop();
                		firstOperand = (firstOperand == NULL_PLACEHOLDER_STRING) ? null : firstOperand;
                		secondOperand = null;
                		thirdOperand = null;
                	} else if (operandNumber == 2) {
                		if (operandStack.size() < 2)
                			throw new Exception(AndroidResourceHelper.getMessage("TheExpConUnexpectedSyn4Args", expressionIn, token, String.valueOf(operandNumber), String.valueOf(operandStack.size())));
                	
                		secondOperand = operandStack.pop();
                		secondOperand = (secondOperand == NULL_PLACEHOLDER_STRING) ? null : secondOperand;
                		firstOperand = operandStack.pop();
                		firstOperand = (firstOperand == NULL_PLACEHOLDER_STRING) ? null : firstOperand;
                		thirdOperand = null;
                	} else if (operandNumber == 3) {
                		if (operandStack.size() < 3)
                			throw new Exception(AndroidResourceHelper.getMessage("TheExpConUnexpectedSyn4Args", expressionIn, token, String.valueOf(operandNumber), String.valueOf(operandStack.size())));
                	
                		thirdOperand = operandStack.pop();
                		thirdOperand = (thirdOperand == NULL_PLACEHOLDER_STRING) ? null : thirdOperand;
                        secondOperand = operandStack.pop();
                        secondOperand = (secondOperand == NULL_PLACEHOLDER_STRING) ? null : secondOperand;
                        firstOperand = operandStack.pop();
                        firstOperand = (firstOperand == NULL_PLACEHOLDER_STRING) ? null : firstOperand;
                	}
                	
                	operandStack.push(executeOperation(expressionIn, firstOperand, token, secondOperand, thirdOperand, clientScript, isForDBQuery));
                }
			} else {
				Object operandValue = null;
				if ((token.startsWith("\"") && token.endsWith("\"")) || (token.startsWith("'") && token.endsWith("'")))
					operandValue = token.substring(1, token.length() - 1);
				else if (MetrixStringHelper.isDouble(token))
					operandValue = Double.valueOf(token);
				else if (MetrixStringHelper.valueIsEqual(token, "null"))
					operandValue = NULL_PLACEHOLDER_STRING;
				else if (MetrixStringHelper.valueIsEqual(token, "true"))
					operandValue = true;
				else if (MetrixStringHelper.valueIsEqual(token, "false"))
					operandValue = false;
				else if (token.startsWith("new Date(") && token.endsWith(")")) {
					String tokenToSplit = token.substring(9, token.length() - 1);
					ArrayList<String> tokenComponents = splitStringIntoParams(tokenToSplit);

					// The Date object supports the following types of signatures:
					// new Date()
					// new Date(milliseconds)
					// new Date("September 3, 2014 15:58")
					// new Date(year, month, day, hours, minutes, seconds, milliseconds)
					if (tokenComponents != null
							&& ((tokenComponents.size() == 7 && stringListHasNullEntries(tokenComponents))
							|| (tokenComponents.size() != 1 && tokenComponents.size() != 7)))
						throw new Exception(AndroidResourceHelper.getMessage("TheExpConUnexpectedSyn2Args", token, AndroidResourceHelper.getMessage("DateCreation")));

					try {
						if (tokenComponents == null || tokenComponents.size() == 0 || (tokenComponents.size() == 1 && MetrixStringHelper.isNullOrEmpty(tokenComponents.get(0))))
							operandValue = new Date();
						else if (tokenComponents.size() == 1) {
							Object paramObject = executeSubExpression(tokenComponents.get(0), clientScript);
							if (paramObject != null) {
								if (paramObject instanceof Double) {
									Double paramValue = (Double) paramObject;
									GregorianCalendar calendar = new GregorianCalendar(1970, Calendar.JANUARY, 1);
									calendar.setTimeInMillis(calendar.getTimeInMillis() + paramValue.longValue());
									operandValue = calendar.getTime();
								} else {
									DateFormat dateFormatter = new SimpleDateFormat("MMMM d, yyyy H:mm");
									operandValue = dateFormatter.parse(String.valueOf(paramObject));
								}
							}
						} else if (tokenComponents.size() == 7) {
							int year = ((Double) executeSubExpression(tokenComponents.get(0), clientScript)).intValue();
							// January is 0 in Java, 1 in .NET (so we auto-translate here)
							int month = ((Double) executeSubExpression(tokenComponents.get(1), clientScript)).intValue() - 1;
							int day = ((Double) executeSubExpression(tokenComponents.get(2), clientScript)).intValue();
							int hours = ((Double) executeSubExpression(tokenComponents.get(3), clientScript)).intValue();
							int minutes = ((Double) executeSubExpression(tokenComponents.get(4), clientScript)).intValue();
							int seconds = ((Double) executeSubExpression(tokenComponents.get(5), clientScript)).intValue();
							int ms = ((Double) executeSubExpression(tokenComponents.get(6), clientScript)).intValue();

							GregorianCalendar calendar = new GregorianCalendar();
							calendar.set(year, month, day, hours, minutes, seconds);
							calendar.setTimeInMillis(calendar.getTimeInMillis() + ms);
							operandValue = calendar.getTime();
						}
					} catch (Exception e) {
						LogManager.getInstance().error(e);
                        throw new Exception(AndroidResourceHelper.getMessage("TheExpConUnexpectedSyn2Args", token, AndroidResourceHelper.getMessage("DateCreation")));
					}
				} else if (token.startsWith("days(") && token.endsWith(")")) {
					String tokenToSplit = token.substring(5, token.length() - 1);
					ArrayList<String> tokenComponents = splitStringIntoParams(tokenToSplit);

					if (tokenComponents == null || tokenComponents.size() != 1 || MetrixStringHelper.isNullOrEmpty(tokenComponents.get(0)))
						throw new Exception(AndroidResourceHelper.getMessage("ExTheExpConUnexpected2Args", token, AndroidResourceHelper.getMessage("Days12")));

					Object paramObject = executeSubExpression(tokenComponents.get(0), clientScript);
					if (paramObject != null && paramObject instanceof Double) {
						Double days = (Double) paramObject;
						MetrixTimeSpan ts = new MetrixTimeSpan();
						ts.addDays(days.longValue());
						operandValue = ts;
					}
				} else if (token.startsWith("hours(") && token.endsWith(")")) {
					String tokenToSplit = token.substring(6, token.length() - 1);
					ArrayList<String> tokenComponents = splitStringIntoParams(tokenToSplit);

					if (tokenComponents == null || tokenComponents.size() != 1 || MetrixStringHelper.isNullOrEmpty(tokenComponents.get(0)))
                        throw new Exception(AndroidResourceHelper.getMessage("ExTheExpConUnexpected2Args", token, AndroidResourceHelper.getMessage("Hours12")));

					Object paramObject = executeSubExpression(tokenComponents.get(0), clientScript);
					if (paramObject != null && paramObject instanceof Double) {
						Double hours = (Double) paramObject;
						MetrixTimeSpan ts = new MetrixTimeSpan();
						ts.addHours(hours.longValue());
						operandValue = ts;
					}
				} else if (token.startsWith("minutes(") && token.endsWith(")")) {
					String tokenToSplit = token.substring(8, token.length() - 1);
					ArrayList<String> tokenComponents = splitStringIntoParams(tokenToSplit);

					if (tokenComponents == null || tokenComponents.size() != 1 || MetrixStringHelper.isNullOrEmpty(tokenComponents.get(0)))
                        throw new Exception(AndroidResourceHelper.getMessage("ExTheExpConUnexpected2Args", token, AndroidResourceHelper.getMessage("Minutes12")));

					Object paramObject = executeSubExpression(tokenComponents.get(0), clientScript);
					if (paramObject != null && paramObject instanceof Double) {
						Double minutes = (Double) paramObject;
						MetrixTimeSpan ts = new MetrixTimeSpan();
						ts.addMinutes(minutes.longValue());
						operandValue = ts;
					}
				} else if (token.startsWith("seconds(") && token.endsWith(")")) {
					String tokenToSplit = token.substring(8, token.length() - 1);
					ArrayList<String> tokenComponents = splitStringIntoParams(tokenToSplit);

					if (tokenComponents == null || tokenComponents.size() != 1 || MetrixStringHelper.isNullOrEmpty(tokenComponents.get(0)))
                        throw new Exception(AndroidResourceHelper.getMessage("ExTheExpConUnexpected2Args", token, AndroidResourceHelper.getMessage("Seconds12")));

					Object paramObject = executeSubExpression(tokenComponents.get(0), clientScript);
					if (paramObject != null && paramObject instanceof Double) {
						Double seconds = (Double) paramObject;
						MetrixTimeSpan ts = new MetrixTimeSpan();
						ts.addSeconds(seconds.longValue());
						operandValue = ts;
					}
				} else if (token.startsWith("getControlValue(") && token.endsWith(")")) {
					String tokenToSplit = token.substring(16, token.length() - 1);
					ArrayList<String> tokenComponents = splitStringIntoParams(tokenToSplit);

					if (tokenComponents == null || tokenComponents.size() != 2 || MetrixStringHelper.isNullOrEmpty(tokenComponents.get(0)) || MetrixStringHelper.isNullOrEmpty(tokenComponents.get(1)))
                        throw new Exception(AndroidResourceHelper.getMessage("ExTheExpConUnexpected2Args", token, AndroidResourceHelper.getMessage("GetControlValue")));

					Object tableNameObject = executeSubExpression(tokenComponents.get(0), clientScript);
					String tableName = tableNameObject == null ? "" : String.valueOf(tableNameObject);

					Object columnNameObject = executeSubExpression(tokenComponents.get(1), clientScript);
					String columnName = columnNameObject == null ? "" : String.valueOf(columnNameObject);

					MetrixFormDef thisFormDef = mFormDef.get();
					String controlValue = MetrixControlAssistant.getValue(thisFormDef, mLayout.get(), tableName, columnName);
					MetrixColumnDef thisColDef = thisFormDef.getColumnDef(tableName, columnName);
					if (controlValue != null && thisColDef != null && (thisColDef.dataType == Date.class || thisColDef.dataType == MetrixDate.class || thisColDef.dataType == MetrixTime.class || thisColDef.dataType == MetrixDateTime.class)) {
						operandValue = MetrixDateTimeHelper.convertDateTimeFromUIToDate(controlValue, false);
					} else if (controlValue != null && (MetrixStringHelper.isDouble(controlValue) || MetrixStringHelper.isDoubleWithoutLeadingZero(controlValue))
							&& thisColDef != null && (thisColDef.dataType == int.class || thisColDef.dataType == double.class)) {
						if (MetrixStringHelper.isNullOrEmpty(controlValue) || MetrixStringHelper.valueIsEqual(controlValue, "-"))
							operandValue = Double.valueOf(0);
						else
							operandValue = Double.valueOf(MetrixFloatHelper.convertNumericFromUIToNumber(controlValue).doubleValue());
					} else
						operandValue = controlValue;
				} else if (token.startsWith("getDBValues(") && token.endsWith(")")) {
					String tokenToSplit = token.substring(12, token.length() - 1);
					ArrayList<String> tokenComponents = splitStringIntoParams(tokenToSplit);

					if (tokenComponents == null || tokenComponents.size() != 1 || MetrixStringHelper.isNullOrEmpty(tokenComponents.get(0)))
                        throw new Exception(AndroidResourceHelper.getMessage("ExTheExpConUnexpected2Args", token, AndroidResourceHelper.getMessage("GetDbValues")));

					Object sqlStmtObject = executeSubExpression(tokenComponents.get(0), clientScript, true);
					String sqlStmt = sqlStmtObject == null ? "" : String.valueOf(sqlStmtObject);

					ArrayList<Hashtable<String, String>> tempList = MetrixDatabaseManager.getFieldStringValuesList(sqlStmt);
					operandValue = translateDBStringsToDataTypes(tempList);
				} else if (token.startsWith("getDBValue(") && token.endsWith(")")) {
					String tokenToSplit = token.substring(11, token.length() - 1);
					ArrayList<String> tokenComponents = splitStringIntoParams(tokenToSplit);

					if (tokenComponents == null || tokenComponents.size() != 1 || MetrixStringHelper.isNullOrEmpty(tokenComponents.get(0)))
                        throw new Exception(AndroidResourceHelper.getMessage("ExTheExpConUnexpected2Args", token, AndroidResourceHelper.getMessage("GetDbValue")));

					Object sqlStmtObject = executeSubExpression(tokenComponents.get(0), clientScript, true);
					String sqlStmt = sqlStmtObject == null ? "" : String.valueOf(sqlStmtObject);

					String tempValue = MetrixDatabaseManager.getFieldStringValue(sqlStmt);
					operandValue = translateStringToObject(tempValue, true);
				} else if (token.startsWith("setControlVisibility(") && token.endsWith(")")) {
					String tokenToSplit = token.substring(21, token.length() - 1);
					ArrayList<String> tokenComponents = splitStringIntoParams(tokenToSplit);

					if (tokenComponents == null || (tokenComponents.size() != 2 && tokenComponents.size() != 3)
							|| (tokenComponents.size() == 2 && (MetrixStringHelper.isNullOrEmpty(tokenComponents.get(0)) || MetrixStringHelper.isNullOrEmpty(tokenComponents.get(1))))
							|| (tokenComponents.size() == 3 && (MetrixStringHelper.isNullOrEmpty(tokenComponents.get(0)) || MetrixStringHelper.isNullOrEmpty(tokenComponents.get(1)) || MetrixStringHelper.isNullOrEmpty(tokenComponents.get(2)))))
                        throw new Exception(AndroidResourceHelper.getMessage("ExTheExpConUnexpected2Args", token, AndroidResourceHelper.getMessage("SetControlVisibility")));

					if (tokenComponents.size() == 2) {
						// this will only work if the controlAlias is contained within our extant scriptingControlAliasMap
						operandValue = false;
						Object controlAliasObject = executeSubExpression(tokenComponents.get(0), clientScript);
						String controlAlias = controlAliasObject == null ? "" : String.valueOf(controlAliasObject);

						Object valueObject = executeSubExpression(tokenComponents.get(1), clientScript);
						boolean isVisible = Boolean.valueOf(String.valueOf(valueObject));
						int visibleState = isVisible ? View.VISIBLE : View.GONE;
						final String tag = isVisible ? null : HIDDEN_BY_SCRIPT;

						Activity currActivity = mCurrentActivity.get();
						if (currActivity instanceof FSMAttachmentList) {
							// Handle ATTACHMENT_WIDGET_* aliases
							FSMAttachmentList attachmentList = (FSMAttachmentList)currActivity;
							if (MetrixStringHelper.valueIsEqual(controlAlias, AttachmentWidgetManager.ATTACHMENT_WIDGET_PHOTO))
								attachmentList.mAttachmentAdditionControl.setCameraBtnVisibility(isVisible);
							else if (MetrixStringHelper.valueIsEqual(controlAlias, AttachmentWidgetManager.ATTACHMENT_WIDGET_VIDEO))
								attachmentList.mAttachmentAdditionControl.setVideoBtnVisibility(isVisible);
							else if (MetrixStringHelper.valueIsEqual(controlAlias, AttachmentWidgetManager.ATTACHMENT_WIDGET_FILE))
								attachmentList.mAttachmentAdditionControl.setFileBtnVisibility(isVisible);
						}
						if (currActivity instanceof FSMAttachmentCard && MetrixStringHelper.valueIsEqual(controlAlias, "BUTTON_SAVE")) {
							// Handle BUTTON_SAVE alias
							FSMAttachmentCard attachmentCard = (FSMAttachmentCard)currActivity;
							if (!attachmentCard.mSaveDisabledViaConfiguration) {
								// Only allow changes to button state if not disabled via configuration
								((View)attachmentCard.mCustomSaveButton).setVisibility(visibleState);
								attachmentCard.mCustomSaveButton.setTag(tag);
								attachmentCard.resetFABOffset();
							}
						} else if (MetrixPublicCache.instance.containsKey("scriptingControlAliasMap")) {
							HashMap<String, Integer> scriptingControlAliasMap = (HashMap<String, Integer>) MetrixPublicCache.instance.getItem("scriptingControlAliasMap");
							if (scriptingControlAliasMap.containsKey(controlAlias)) {
								int controlID = scriptingControlAliasMap.get(controlAlias);
								View v = currActivity.findViewById(controlID);
								if (v != null) {
									v.setVisibility(visibleState);
									if (MetrixStringHelper.valueIsEqual("BUTTON_ADD", controlAlias) ||
											MetrixStringHelper.valueIsEqual("BUTTON_SAVE", controlAlias) ||
											MetrixStringHelper.valueIsEqual("BUTTON_NEXT", controlAlias)) {
										v.setTag(tag);

										if (currActivity instanceof MetrixBaseActivity) {
											MetrixBaseActivity mActivity = (MetrixBaseActivity) currActivity;
											mActivity.resetFABOffset();
										}
									}
								}
								if (MetrixStringHelper.valueIsEqual(controlAlias, "QUICK_ACTIONBAR_ATTACHMENT") || MetrixStringHelper.valueIsEqual(controlAlias, "QUICK_ACTIONBAR_NOTES")) {
									// change state of parallel count badge
									int badgeID = scriptingControlAliasMap.get(controlAlias + "_BADGE");
									View v2 = currActivity.findViewById(badgeID);
									if (v2 != null) {
										v2.setVisibility(visibleState);
									}
								}
								operandValue = true;
							}
						}
					} else {
						Object tableNameObject = executeSubExpression(tokenComponents.get(0), clientScript);
						String tableName = tableNameObject == null ? "" : String.valueOf(tableNameObject);

						Object columnNameObject = executeSubExpression(tokenComponents.get(1), clientScript);
						String columnName = columnNameObject == null ? "" : String.valueOf(columnNameObject);

						Object valueObject = executeSubExpression(tokenComponents.get(2), clientScript);
						boolean isVisible = Boolean.valueOf(String.valueOf(valueObject));
						int visibleState = isVisible ? View.VISIBLE : View.GONE;

						MetrixFormDef formDef = mFormDef.get();
						ViewGroup layout = mLayout.get();
						MetrixColumnDef thisColDef = formDef.getColumnDef(tableName, columnName);
						if (thisColDef != null) {
							MetrixControlAssistant.setVisibility(thisColDef.id, layout, visibleState);
							if (thisColDef.labelId != null)
								MetrixControlAssistant.setVisibility(thisColDef.labelId, layout, visibleState);
							operandValue = true;
						}
					}
				} else if (token.startsWith("setControlEnabled(") && token.endsWith(")")) {
					String tokenToSplit = token.substring(18, token.length() - 1);
					ArrayList<String> tokenComponents = splitStringIntoParams(tokenToSplit);

					if (tokenComponents == null || (tokenComponents.size() != 2 && tokenComponents.size() != 3)
							|| (tokenComponents.size() == 2 && (MetrixStringHelper.isNullOrEmpty(tokenComponents.get(0)) || MetrixStringHelper.isNullOrEmpty(tokenComponents.get(1))))
							|| (tokenComponents.size() == 3 && (MetrixStringHelper.isNullOrEmpty(tokenComponents.get(0)) || MetrixStringHelper.isNullOrEmpty(tokenComponents.get(1)) || MetrixStringHelper.isNullOrEmpty(tokenComponents.get(2)))))
                        throw new Exception(AndroidResourceHelper.getMessage("ExTheExpConUnexpected2Args", token, AndroidResourceHelper.getMessage("SetControlEnabled")));

					if (tokenComponents.size() == 2) {
						// this will only work if the controlAlias is contained within our extant scriptingControlAliasMap
						operandValue = false;
						Object controlAliasObject = executeSubExpression(tokenComponents.get(0), clientScript);
						String controlAlias = controlAliasObject == null ? "" : String.valueOf(controlAliasObject);

						Object valueObject = executeSubExpression(tokenComponents.get(1), clientScript);
						boolean isEnabled = Boolean.valueOf(String.valueOf(valueObject));

						Activity currActivity = mCurrentActivity.get();
						if (currActivity instanceof FSMAttachmentList) {
							// Handle ATTACHMENT_WIDGET_* aliases
							FSMAttachmentList attachmentList = (FSMAttachmentList)currActivity;
							if (MetrixStringHelper.valueIsEqual(controlAlias, AttachmentWidgetManager.ATTACHMENT_WIDGET_PHOTO))
								attachmentList.mAttachmentAdditionControl.setCameraBtnEnabled(isEnabled);
							else if (MetrixStringHelper.valueIsEqual(controlAlias, AttachmentWidgetManager.ATTACHMENT_WIDGET_VIDEO))
								attachmentList.mAttachmentAdditionControl.setVideoBtnEnabled(isEnabled);
							else if (MetrixStringHelper.valueIsEqual(controlAlias, AttachmentWidgetManager.ATTACHMENT_WIDGET_FILE))
								attachmentList.mAttachmentAdditionControl.setFileBtnEnabled(isEnabled);
						} else if (currActivity instanceof FSMAttachmentCard && MetrixStringHelper.valueIsEqual(controlAlias, "BUTTON_SAVE")) {
							// Handle BUTTON_SAVE alias
							FSMAttachmentCard attachmentCard = (FSMAttachmentCard)currActivity;
							if (!attachmentCard.mSaveDisabledViaConfiguration) {
								// Only allow changes to button state if not disabled via configuration
								attachmentCard.mCustomSaveButton.setEnabled(isEnabled);
							}
						} else if (MetrixPublicCache.instance.containsKey("scriptingControlAliasMap")) {
							HashMap<String, Integer> scriptingControlAliasMap = (HashMap<String, Integer>) MetrixPublicCache.instance.getItem("scriptingControlAliasMap");
							if (scriptingControlAliasMap.containsKey(controlAlias)) {
								int controlID = scriptingControlAliasMap.get(controlAlias);
								View v = currActivity.findViewById(controlID);
								if (v != null) {
									v.setEnabled(isEnabled);
								}

								if (MetrixStringHelper.valueIsEqual(controlAlias, "QUICK_ACTIONBAR_ATTACHMENT") || MetrixStringHelper.valueIsEqual(controlAlias, "QUICK_ACTIONBAR_NOTES")) {
									// change state of parallel count badge
									int badgeID = scriptingControlAliasMap.get(controlAlias + "_BADGE");
									View v2 = currActivity.findViewById(badgeID);
									if (v2 != null) {
										v2.setEnabled(isEnabled);
									}
								}
								operandValue = true;
							}
						}
					} else {
						Object tableNameObject = executeSubExpression(tokenComponents.get(0), clientScript);
						String tableName = tableNameObject == null ? "" : String.valueOf(tableNameObject);

						Object columnNameObject = executeSubExpression(tokenComponents.get(1), clientScript);
						String columnName = columnNameObject == null ? "" : String.valueOf(columnNameObject);

						Object valueObject = executeSubExpression(tokenComponents.get(2), clientScript);
						boolean isEnabled = Boolean.valueOf(String.valueOf(valueObject));

						MetrixControlAssistant.setEnabled(mFormDef.get(), mLayout.get(), tableName, columnName, isEnabled);
						operandValue = true;
					}
				} else if (token.startsWith("setControlRequired(") && token.endsWith(")")) {
					// In Mobile, we will only handle the three-parameter version.
					String tokenToSplit = token.substring(19, token.length() - 1);
					ArrayList<String> tokenComponents = splitStringIntoParams(tokenToSplit);

					if (tokenComponents == null || tokenComponents.size() != 3 || MetrixStringHelper.isNullOrEmpty(tokenComponents.get(0)) || MetrixStringHelper.isNullOrEmpty(tokenComponents.get(1)) || MetrixStringHelper.isNullOrEmpty(tokenComponents.get(2)))
                        throw new Exception(AndroidResourceHelper.getMessage("ExTheExpConUnexpected2Args", token, AndroidResourceHelper.getMessage("SetControlRequired")));

					Object tableNameObject = executeSubExpression(tokenComponents.get(0), clientScript);
					String tableName = tableNameObject == null ? "" : String.valueOf(tableNameObject);

					Object columnNameObject = executeSubExpression(tokenComponents.get(1), clientScript);
					String columnName = columnNameObject == null ? "" : String.valueOf(columnNameObject);

					Object valueObject = executeSubExpression(tokenComponents.get(2), clientScript);
					boolean isRequired = Boolean.valueOf(String.valueOf(valueObject));

					MetrixControlAssistant.setRequired(mFormDef.get(), mLayout.get(), tableName, columnName, isRequired);
					operandValue = true;
				} else if (token.startsWith("setDBValue(") && token.endsWith(")")) {
					// This is being treated as a non-transactional save directly to client DB.
					String tokenToSplit = token.substring(11, token.length() - 1);
					ArrayList<String> tokenComponents = splitStringIntoParams(tokenToSplit);

					if (tokenComponents == null || tokenComponents.size() != 1 || MetrixStringHelper.isNullOrEmpty(tokenComponents.get(0)))
                        throw new Exception(AndroidResourceHelper.getMessage("ExTheExpConUnexpected2Args", token, AndroidResourceHelper.getMessage("SetDbValue")));

					Object sqlStmtObject = executeSubExpression(tokenComponents.get(0), clientScript, true);
					String sqlStmt = sqlStmtObject == null ? "" : String.valueOf(sqlStmtObject);

					operandValue = MetrixDatabaseManager.executeSql(sqlStmt);
				} else if (token.startsWith("setControlValue(") && token.endsWith(")")) {
					String tokenToSplit = token.substring(16, token.length() - 1);
					ArrayList<String> tokenComponents = splitStringIntoParams(tokenToSplit);

					if (tokenComponents == null || tokenComponents.size() != 3 || MetrixStringHelper.isNullOrEmpty(tokenComponents.get(0)) || MetrixStringHelper.isNullOrEmpty(tokenComponents.get(1)) || MetrixStringHelper.isNullOrEmpty(tokenComponents.get(2)))
                        throw new Exception(AndroidResourceHelper.getMessage("ExTheExpConUnexpected2Args", token, AndroidResourceHelper.getMessage("SetControlValue")));

					Object tableNameObject = executeSubExpression(tokenComponents.get(0), clientScript);
					String tableName = tableNameObject == null ? "" : String.valueOf(tableNameObject);

					Object columnNameObject = executeSubExpression(tokenComponents.get(1), clientScript);
					String columnName = columnNameObject == null ? "" : String.valueOf(columnNameObject);

					Object valueObject = executeSubExpression(tokenComponents.get(2), clientScript);
					String value = "";

					if (valueObject != null) {
						if (valueObject instanceof Date) {
							Date tempDate = (Date) valueObject;
							MetrixColumnDef colDef = mFormDef.get().getColumnDef(tableName, columnName);
							if (colDef.dataType == MetrixDate.class)
								value = MetrixDateTimeHelper.convertDateTimeFromDateToUIDateOnly(tempDate);
							else if (colDef.dataType == MetrixTime.class)
								value = MetrixDateTimeHelper.convertDateTimeFromDateToUITimeOnly(tempDate);
							else
								value = MetrixDateTimeHelper.convertDateTimeFromDateToUI(tempDate);
						} else if (valueObject instanceof Double) {
							String tempValue = String.valueOf(valueObject);
							value = MetrixFloatHelper.convertNumericFromForcedLocaleToUI(tempValue, Locale.US);
						} else
							value = enforceNewlines(String.valueOf(valueObject));
					}

					MetrixControlAssistant.setValue(mFormDef.get(), mLayout.get(), tableName, columnName, value);
					operandValue = true;
				} else if (token.startsWith("alert(") && token.endsWith(")")) {
					String tokenToSplit = token.substring(6, token.length() - 1);
					ArrayList<String> tokenComponents = splitStringIntoParams(tokenToSplit);

					if (tokenComponents == null || tokenComponents.size() != 1 || MetrixStringHelper.isNullOrEmpty(tokenComponents.get(0)))
                        throw new Exception(AndroidResourceHelper.getMessage("ExTheExpConUnexpected2Args", token, AndroidResourceHelper.getMessage("AlertMessage")));

					String alertMessage = tokenComponents.get(0);
					Object expressionResult = executeSubExpression(alertMessage, clientScript);
					String resultString = expressionResult == null ? "" : enforceNewlines(String.valueOf(expressionResult));

					MetrixUIHelper.showSnackbar(mCurrentActivity.get(), resultString);
					operandValue = true;
				} else if (token.startsWith("confirm(") && token.endsWith(")")) {
                	/* // non-functional code
                	String tokenToSplit = token.substring(8, token.length() - 1);
                	ArrayList<String> tokenComponents = splitStringIntoParams(tokenToSplit);
                	
                	if (tokenComponents == null || tokenComponents.size() != 1 || MetrixStringHelper.isNullOrEmpty(tokenComponents.get(0)))
                		throw new Exception(String.format("EXCEPTION: The expression (%s) contains unexpected syntax.  Expected usage: confirm(\"Message to confirm upon.\")", token));
                	
                	String confirmMessage = tokenComponents.get(0);
                	Object expressionResult = executeSubExpression(confirmMessage, clientScript);
                	final String resultString = expressionResult == null ? "" : String.valueOf(expressionResult);

                	MetrixPublicCache.instance.addItem("scriptingConfirmResult", true);
                	final Activity currActivity = mCurrentActivity.get();
                	if (currActivity != null) {
                		currActivity.runOnUiThread(new Runnable() {
                			public void run() {
                				new AlertDialog.Builder(currActivity)
                					.setMessage(resultString)
                					.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
										public void onClick(DialogInterface arg0, int arg1) {
											MetrixPublicCache.instance.addItem("scriptingConfirmResult", true);
										}
									})
									.setNegativeButton("No", new DialogInterface.OnClickListener() {
										public void onClick(DialogInterface dialog, int which) {
											MetrixPublicCache.instance.addItem("scriptingConfirmResult", false);
										}
									}).create().show();
                			}
                		});
                	}
                	
                	operandValue = (Boolean)MetrixPublicCache.instance.getItem("scriptingConfirmResult");
                	*/
					LogManager.getInstance().error(new Exception(String.format("EXCEPTION: The expression (%s) contains a confirm method, which is not supported.  This will be handled as always TRUE.", token)));
					operandValue = true;
				} else if (token.startsWith("getMessage(") && token.endsWith(")")) {
					// if this method appears, simply use the Message ID parameter value
					String tokenToSplit = token.substring(11, token.length() - 1);
					ArrayList<String> tokenComponents = splitStringIntoParams(tokenToSplit);

					if (tokenComponents == null || tokenComponents.size() != 2 || MetrixStringHelper.isNullOrEmpty(tokenComponents.get(0)) || MetrixStringHelper.isNullOrEmpty(tokenComponents.get(1)))
                        throw new Exception(AndroidResourceHelper.getMessage("ExTheExpConUnexpected2Args", token, AndroidResourceHelper.getMessage("GetMessage")));

					Object messageIdObject = executeSubExpression(tokenComponents.get(0), clientScript);
					String messageId = messageIdObject == null ? "" : String.valueOf(messageIdObject);

					Object messageTypeObject = executeSubExpression(tokenComponents.get(1), clientScript);
					String messageType = (messageTypeObject == null ? "" : String.valueOf(messageTypeObject));

					operandValue = AndroidResourceHelper.getMessageFromScript(messageId, messageType);

				} else if (token.startsWith("log(") && token.endsWith(")")) {
					String tokenToSplit = token.substring(4, token.length() - 1);
					ArrayList<String> tokenComponents = splitStringIntoParams(tokenToSplit);

					if (tokenComponents == null || tokenComponents.size() != 1 || MetrixStringHelper.isNullOrEmpty(tokenComponents.get(0)))
                        throw new Exception(AndroidResourceHelper.getMessage("ExTheExpConUnexpected2Args", token, AndroidResourceHelper.getMessage("LogMessage")));

					Object logObject = executeSubExpression(tokenComponents.get(0), clientScript);
					String logMessage = logObject == null ? "" : String.valueOf(logObject);

					LogManager.getInstance().info(logMessage);
					operandValue = true;
				} else if (token.startsWith("saveChanges(") && token.endsWith(")")) {
					String tokenToSplit = token.substring(12, token.length() - 1);

					String currActivityName = MetrixScreenManager.getScreenName(mCurrentActivity.get());
					String friendlyName = "(SSC) " + currActivityName;

					MetrixTransaction transactionInfo;
					//if there's only one table and one primary key get all transaction info
					if(mFormDef.get() != null && mFormDef.get().tables != null && mFormDef.get().tables.size() ==1){
						String tableName = mFormDef.get().tables.get(0).tableName;
						MetrixKeys tableKeys = MetrixDatabases.getMetrixTablePrimaryKeys(tableName);
						String primaryKey;
						//if there's multiple primary keys, get the transaction info with only the transactionId
						if(tableKeys != null && tableKeys.keyInfo != null && tableKeys.keyInfo.keySet() != null && tableKeys.keyInfo.keySet().size() == 1)
							primaryKey = (String) tableKeys.keyInfo.keySet().toArray()[0];
						else
							primaryKey = "";

						transactionInfo = MetrixTransaction.getTransaction(tableName,primaryKey);
					}else{
						transactionInfo = new MetrixTransaction();
					}
					if (MetrixStringHelper.isNullOrEmpty(tokenToSplit)) {
						MetrixSaveResult saveResult = MetrixUpdateManager.update(mCurrentActivity.get(), mLayout.get(), mFormDef.get(), transactionInfo, false, null, false, friendlyName);
						operandValue = (saveResult == MetrixSaveResult.SUCCESSFUL);
					} else {
						ArrayList<String> tokenComponents = splitStringIntoParams(tokenToSplit);
						if (tokenComponents == null || tokenComponents.size() != 3 || MetrixStringHelper.isNullOrEmpty(tokenComponents.get(0)) || MetrixStringHelper.isNullOrEmpty(tokenComponents.get(1)) || MetrixStringHelper.isNullOrEmpty(tokenComponents.get(2)))
                            throw new Exception(AndroidResourceHelper.getMessage("ExTheExpConUnexpected2Args", token, AndroidResourceHelper.getMessage("SaveChanges")));

						Object allowContinueOnErrorObject = executeSubExpression(tokenComponents.get(0), clientScript);
						boolean allowContinueOnError = Boolean.valueOf(String.valueOf(allowContinueOnErrorObject));
						Object closeCurrentScreenObject = executeSubExpression(tokenComponents.get(1), clientScript);
						boolean closeCurrentScreen = Boolean.valueOf(String.valueOf(closeCurrentScreenObject));
						Object advanceWorkflowObject = executeSubExpression(tokenComponents.get(2), clientScript);
						boolean advanceWorkflow = Boolean.valueOf(String.valueOf(advanceWorkflowObject));

						MetrixSaveResult saveResult = MetrixUpdateManager.update(mCurrentActivity.get(), mLayout.get(), mFormDef.get(), transactionInfo, allowContinueOnError, null, closeCurrentScreen, friendlyName, advanceWorkflow);
						operandValue = (saveResult == MetrixSaveResult.SUCCESSFUL);
					}
				} else if (token.startsWith("goToScreen(") && token.endsWith(")")) {
					String tokenToSplit = token.substring(11, token.length() - 1);
					ArrayList<String> tokenComponents = splitStringIntoParams(tokenToSplit);

					if (tokenComponents == null || (tokenComponents.size() != 1 && tokenComponents.size() != 2)
							|| (tokenComponents.size() == 1 && MetrixStringHelper.isNullOrEmpty(tokenComponents.get(0)))
							|| (tokenComponents.size() == 2 && (MetrixStringHelper.isNullOrEmpty(tokenComponents.get(0)) || MetrixStringHelper.isNullOrEmpty(tokenComponents.get(1)))))
                        throw new Exception(AndroidResourceHelper.getMessage("ExTheExpConUnexpected2Args", token, AndroidResourceHelper.getMessage("GoToScreen")));

					Object screenObject = executeSubExpression(tokenComponents.get(0), clientScript);
					String screenName = screenObject == null ? "" : String.valueOf(screenObject);

					if (AttachmentWidgetManager.isAttachmentAPIScreenName(screenName)) {
						// Disallow Attachment API direct navigation via script
						LogManager.getInstance().error(String.format("goToScreen(): Screen name (%s) is not supported.", screenName));
						operandValue = false;
					} else {
						boolean closeCurrentScreen = false;
						if (tokenComponents.size() == 2) {
							Object closeCurrentScreenObject = executeSubExpression(tokenComponents.get(1), clientScript);
							closeCurrentScreen = Boolean.valueOf(String.valueOf(closeCurrentScreenObject));
						}

						Activity currActivity = mCurrentActivity.get();
						String currActivityName = MetrixScreenManager.getScreenName(currActivity);

						//Mapping class can be found within the code-base.
						if (MetrixApplicationAssistant.screenNameHasClassInCode(screenName)) {
							Intent intent = MetrixActivityHelper.createActivityIntent(currActivity, screenName);
							// if we are navigating to the screen we are currently on, finish off old instance
							if (MetrixStringHelper.valueIsEqual(currActivityName, screenName) || closeCurrentScreen)
								MetrixActivityHelper.startNewActivityAndFinish(currActivity, intent);
							else
								MetrixActivityHelper.startNewActivity(currActivity, intent);
						} else {
							// This is where the codeless screens come into action
							int screenId = MetrixScreenManager.getScreenId(screenName);
							if (screenId > -1) {
								String screenType = MetrixScreenManager.getScreenType(screenId);
								if (MetrixWorkflowManager.isScreenInWorkflowScreenForCurrentWorkflow(currActivity.getApplicationContext(), screenId)) {
									String currentWorkflowName = MetrixWorkflowManager.getCurrentWorkflowName(currActivity);
									if (!MetrixStringHelper.isNullOrEmpty(screenType)) {
										if (screenType.toLowerCase().contains("standard")) {
											if (!MetrixStringHelper.isNullOrEmpty(currentWorkflowName)) {
												Intent intent = null;
												if (currentWorkflowName.toLowerCase().contains("debrief")) {
													intent = MetrixActivityHelper.createActivityIntent(currActivity, "com.metrix.metrixmobile.system", "MetadataDebriefActivity");
													intent.putExtra("ScreenID", screenId);
													if (MetrixStringHelper.valueIsEqual(currActivityName, screenName) || closeCurrentScreen)
														MetrixActivityHelper.startNewActivityAndFinish(currActivity, intent);
													else
														MetrixActivityHelper.startNewActivity(currActivity, intent);
												} else if (currentWorkflowName.toLowerCase().contains("schedule")) {
													intent = MetrixActivityHelper.createActivityIntent(currActivity, "com.metrix.metrixmobile.system", "MetadataScheduleActivity");
													intent.putExtra("ScreenID", screenId);
													if (MetrixStringHelper.valueIsEqual(currActivityName, screenName) || closeCurrentScreen)
														MetrixActivityHelper.startNewActivityAndFinish(currActivity, intent);
													else
														MetrixActivityHelper.startNewActivity(currActivity, intent);
												} else if (currentWorkflowName.toLowerCase().contains("quote")) {
													intent = MetrixActivityHelper.createActivityIntent(currActivity, "com.metrix.metrixmobile.system", "MetadataQuoteActivity");
													intent.putExtra("ScreenID", screenId);
													if (MetrixStringHelper.valueIsEqual(currActivityName, screenName) || closeCurrentScreen)
														MetrixActivityHelper.startNewActivityAndFinish(currActivity, intent);
													else
														MetrixActivityHelper.startNewActivity(currActivity, intent);
												} else
													LogManager.getInstance().error(String.format("Current workflow(%s) isn't valid.", currentWorkflowName));
											} else
												LogManager.getInstance().error(String.format("Current workflow(%s) doesn't exist.", currentWorkflowName));
										} else if (screenType.toLowerCase().contains("list")) {
											if (!MetrixStringHelper.isNullOrEmpty(currentWorkflowName)) {
												Intent intent = null;
												if (currentWorkflowName.toLowerCase().contains("debrief")) {
													intent = MetrixActivityHelper.createActivityIntent(currActivity, "com.metrix.metrixmobile.system", "MetadataListDebriefActivity");
													intent.putExtra("ScreenID", screenId);
													if (MetrixStringHelper.valueIsEqual(currActivityName, screenName) || closeCurrentScreen)
														MetrixActivityHelper.startNewActivityAndFinish(currActivity, intent);
													else
														MetrixActivityHelper.startNewActivity(currActivity, intent);
												} else if (currentWorkflowName.toLowerCase().contains("schedule")) {
													intent = MetrixActivityHelper.createActivityIntent(currActivity, "com.metrix.metrixmobile.system", "MetadataListScheduleActivity");
													intent.putExtra("ScreenID", screenId);
													if (MetrixStringHelper.valueIsEqual(currActivityName, screenName) || closeCurrentScreen)
														MetrixActivityHelper.startNewActivityAndFinish(currActivity, intent);
													else
														MetrixActivityHelper.startNewActivity(currActivity, intent);
												} else if (currentWorkflowName.toLowerCase().contains("quote")) {
													intent = MetrixActivityHelper.createActivityIntent(currActivity, "com.metrix.metrixmobile.system", "MetadataListQuoteActivity");
													intent.putExtra("ScreenID", screenId);
													if (MetrixStringHelper.valueIsEqual(currActivityName, screenName) || closeCurrentScreen)
														MetrixActivityHelper.startNewActivityAndFinish(currActivity, intent);
													else
														MetrixActivityHelper.startNewActivity(currActivity, intent);
												} else
													LogManager.getInstance().error(String.format("Current workflow(%s) isn't valid.", currentWorkflowName));
											} else
												LogManager.getInstance().error(String.format("Current workflow(%s) doesn't exist.", currentWorkflowName));
										} else
											LogManager.getInstance().error(String.format("Screen type(%s) doesn't support.", screenType));
									}
								} else if (MetrixStringHelper.valueIsEqual(screenType, "TAB_PARENT")) {
									// allow this scripting method to advance to a codeless tab screen, which is not in a workflow
									Intent intent = MetrixActivityHelper.createActivityIntent(currActivity, "com.metrix.metrixmobile.system", "MetadataTabActivity");
									intent.putExtra("ScreenID", screenId);
									if (MetrixStringHelper.valueIsEqual(currActivityName, screenName) || closeCurrentScreen)
										MetrixActivityHelper.startNewActivityAndFinish(currActivity, intent);
									else
										MetrixActivityHelper.startNewActivity(currActivity, intent);
								} else if (MetrixStringHelper.valueIsEqual(screenType, "STANDARD")) {
									// allow this scripting method to advance to a non-workflow codeless standard screen
									Intent intent = MetrixActivityHelper.createActivityIntent(currActivity, "com.metrix.metrixmobile.system", "MetadataStandardActivity");
									intent.putExtra("ScreenID", screenId);
									if (MetrixStringHelper.valueIsEqual(currActivityName, screenName) || closeCurrentScreen)
										MetrixActivityHelper.startNewActivityAndFinish(currActivity, intent);
									else
										MetrixActivityHelper.startNewActivity(currActivity, intent);
								} else if (screenType.toLowerCase().contains("list")) {
									// allow this scripting method to advance to a non-workflow codeless list screen
									Intent intent = MetrixActivityHelper.createActivityIntent(currActivity, "com.metrix.metrixmobile.system", "MetadataListActivity");
									intent.putExtra("ScreenID", screenId);
									if (MetrixStringHelper.valueIsEqual(currActivityName, screenName) || closeCurrentScreen)
										MetrixActivityHelper.startNewActivityAndFinish(currActivity, intent);
									else
										MetrixActivityHelper.startNewActivity(currActivity, intent);
								} else
									LogManager.getInstance().error(String.format("Screen (screen_id=%s) doesn't exist in current workflow or is not a tab parent.", screenId));
							} else {
								LogManager.getInstance().error(String.format("Can't find a correct codeless screen (screen_id=%s) - no meta-data information is available.", screenId));
								throw new Exception(AndroidResourceHelper.getMessage("CantFindACorrectCodeless1Args", screenId));
							}
						}
						operandValue = true;
					}
				} else if (token.startsWith("advanceWorkflow(") && token.endsWith(")")) {
					String tokenToSplit = token.substring(16, token.length() - 1);

					if (!MetrixStringHelper.isNullOrEmpty(tokenToSplit))
                        throw new Exception(AndroidResourceHelper.getMessage("ExTheExpConUnexpected2Args", token, AndroidResourceHelper.getMessage("AdvanceWorkflow")));

					MetrixWorkflowManager.advanceWorkflow(mCurrentActivity.get());
					operandValue = true;
				} else if (token.startsWith("initialValuesHaveChanged(") && token.endsWith(")")) {
					String tokenToSplit = token.substring(25, token.length() - 1);

					if (!MetrixStringHelper.isNullOrEmpty(tokenToSplit))
                        throw new Exception(AndroidResourceHelper.getMessage("ExTheExpConUnexpected2Args", token, AndroidResourceHelper.getMessage("InitValHaveChanged")));

					operandValue = MetrixControlAssistant.anyOnStartValuesChanged(mFormDef.get(), mLayout.get());
				} else if (token.startsWith("isNullOrEmptyString(") && token.endsWith(")")) {
					String tokenToSplit = token.substring(20, token.length() - 1);
					ArrayList<String> tokenComponents = splitStringIntoParams(tokenToSplit);

					if (tokenComponents == null || tokenComponents.size() != 1 || MetrixStringHelper.isNullOrEmpty(tokenComponents.get(0)))
                        throw new Exception(AndroidResourceHelper.getMessage("ExTheExpConUnexpected2Args", token, AndroidResourceHelper.getMessage("IsNullOrEmptyString")));

					Object valueObject = executeSubExpression(tokenComponents.get(0), clientScript);
					boolean isBlank = false;
					if (valueObject == null)
						isBlank = true;
					else if (valueObject instanceof String) {
						String strValue = (String) valueObject;
						isBlank = MetrixStringHelper.isNullOrEmpty(strValue);
					}

					operandValue = isBlank;
				} else if (token.startsWith("populateListFromQuery(") && token.endsWith(")")) {
					String tokenToSplit = token.substring(22, token.length() - 1);
					ArrayList<String> tokenComponents = splitStringIntoParams(tokenToSplit);

					if (tokenComponents == null || tokenComponents.size() != 4 || MetrixStringHelper.isNullOrEmpty(tokenComponents.get(0)) || MetrixStringHelper.isNullOrEmpty(tokenComponents.get(1)) || MetrixStringHelper.isNullOrEmpty(tokenComponents.get(2)) || MetrixStringHelper.isNullOrEmpty(tokenComponents.get(3)))
                        throw new Exception(AndroidResourceHelper.getMessage("ExTheExpConUnexpected2Args", token, AndroidResourceHelper.getMessage("PopulateListFromQuery")));

					Object tableNameObject = executeSubExpression(tokenComponents.get(0), clientScript);
					String tableName = tableNameObject == null ? "" : String.valueOf(tableNameObject);

					Object columnNameObject = executeSubExpression(tokenComponents.get(1), clientScript);
					String columnName = columnNameObject == null ? "" : String.valueOf(columnNameObject);

					Object sqlStmtObject = executeSubExpression(tokenComponents.get(2), clientScript, true);
					String sqlStmt = sqlStmtObject == null ? "" : String.valueOf(sqlStmtObject);

					Object displayNullObject = executeSubExpression(tokenComponents.get(3), clientScript);
					boolean displayNull = Boolean.valueOf(String.valueOf(displayNullObject));

					View v = MetrixControlAssistant.getControl(mFormDef.get(), mLayout.get(), tableName, columnName);
					if (v instanceof Spinner) {
						MetrixControlAssistant.populateSpinnerFromQuery(mCurrentActivity.get(), v, sqlStmt, displayNull);
						operandValue = true;
					} else
						operandValue = false;
				} else if (token.startsWith("setCurrentKeys(") && token.endsWith(")")) {
					String tokenToSplit = token.substring(15, token.length() - 1);
					ArrayList<String> tokenComponents = splitStringIntoParams(tokenToSplit);

					if (tokenComponents == null || tokenComponents.size() != 3 || MetrixStringHelper.isNullOrEmpty(tokenComponents.get(0)) || MetrixStringHelper.isNullOrEmpty(tokenComponents.get(1)) || MetrixStringHelper.isNullOrEmpty(tokenComponents.get(2)))
                        throw new Exception(AndroidResourceHelper.getMessage("ExTheExpConUnexpected2Args", token, AndroidResourceHelper.getMessage("SetCurrentKeys")));

					Object tableNameObject = executeSubExpression(tokenComponents.get(0), clientScript);
					String tableName = tableNameObject == null ? "" : String.valueOf(tableNameObject);

					Object columnNameObject = executeSubExpression(tokenComponents.get(1), clientScript);
					String columnName = columnNameObject == null ? "" : String.valueOf(columnNameObject);

					Object valueObject = executeSubExpression(tokenComponents.get(2), clientScript);
					String value = "";

					if (valueObject != null) {
						if (valueObject instanceof Date) {
							Date tempDate = (Date) valueObject;
							value = MetrixDateTimeHelper.convertDateTimeFromDateToUI(tempDate);
						} else if (valueObject instanceof Double) {
							String tempValue = String.valueOf(valueObject);
							value = MetrixFloatHelper.convertNumericFromForcedLocaleToUI(tempValue, Locale.US);
						} else
							value = String.valueOf(valueObject);
					}

					MetrixCurrentKeysHelper.setKeyValue(tableName, columnName, value);
					operandValue = true;
				} else if (token.startsWith("getCurrentKeys(") && token.endsWith(")")) {
					String tokenToSplit = token.substring(15, token.length() - 1);
					ArrayList<String> tokenComponents = splitStringIntoParams(tokenToSplit);

					if (tokenComponents == null || tokenComponents.size() != 2 || MetrixStringHelper.isNullOrEmpty(tokenComponents.get(0)) || MetrixStringHelper.isNullOrEmpty(tokenComponents.get(1)))
                        throw new Exception(AndroidResourceHelper.getMessage("ExTheExpConUnexpected2Args", token, AndroidResourceHelper.getMessage("GetCurrentKeys")));

					Object tableNameObject = executeSubExpression(tokenComponents.get(0), clientScript);
					String tableName = tableNameObject == null ? "" : String.valueOf(tableNameObject);

					Object columnNameObject = executeSubExpression(tokenComponents.get(1), clientScript);
					String columnName = columnNameObject == null ? "" : String.valueOf(columnNameObject);

					String tempValue = MetrixCurrentKeysHelper.getKeyValue(tableName, columnName);
					Date tryDate = MetrixDateTimeHelper.convertDateTimeFromUIToDate(tempValue, false);
					if (tryDate != null) {
						operandValue = tryDate;
					} else if (MetrixStringHelper.isDouble(tempValue)) {
						Double dblValue = Double.valueOf((MetrixFloatHelper.convertNumericFromUIToNumber(tempValue)).doubleValue());
						operandValue = dblValue;
					} else
						operandValue = tempValue;
				} else if (token.startsWith("getCurrentTransactionType(") && token.endsWith(")")) {
					String tokenToSplit = token.substring(26, token.length() - 1);

					if (!MetrixStringHelper.isNullOrEmpty(tokenToSplit))
                        throw new Exception(AndroidResourceHelper.getMessage("ExTheExpConUnexpected2Args", token, AndroidResourceHelper.getMessage("GetCurrTransType")));

					operandValue = "";
					MetrixFormDef formDef = mFormDef.get();
					if (formDef != null && formDef.tables != null && formDef.tables.size() > 0) {
						operandValue = formDef.tables.get(0).transactionType.toString();
					}
				} else if (token.startsWith("setCache(") && token.endsWith(")")) {
					String tokenToSplit = token.substring(9, token.length() - 1);
					ArrayList<String> tokenComponents = splitStringIntoParams(tokenToSplit);

					if (tokenComponents == null || tokenComponents.size() != 2 || MetrixStringHelper.isNullOrEmpty(tokenComponents.get(0)) || MetrixStringHelper.isNullOrEmpty(tokenComponents.get(1)))
                        throw new Exception(AndroidResourceHelper.getMessage("ExTheExpConUnexpected2Args", token, AndroidResourceHelper.getMessage("SetCache")));

					Object cacheNameObject = executeSubExpression(tokenComponents.get(0), clientScript);
					String cacheName = cacheNameObject == null ? "" : String.valueOf(cacheNameObject);

					Object valueObject = executeSubExpression(tokenComponents.get(1), clientScript);
					if (valueObject == null) {
						valueObject = "";
					}

					MetrixPublicCache.instance.addItem(cacheName, valueObject);
					operandValue = true;
				} else if (token.startsWith("getFromCache(") && token.endsWith(")")) {
					String tokenToSplit = token.substring(13, token.length() - 1);
					ArrayList<String> tokenComponents = splitStringIntoParams(tokenToSplit);

					if (tokenComponents == null || tokenComponents.size() != 1 || MetrixStringHelper.isNullOrEmpty(tokenComponents.get(0)))
                        throw new Exception(AndroidResourceHelper.getMessage("ExTheExpConUnexpected2Args", token, AndroidResourceHelper.getMessage("GetFromCache")));

					Object cacheNameObject = executeSubExpression(tokenComponents.get(0), clientScript);
					String cacheName = cacheNameObject == null ? "" : String.valueOf(cacheNameObject);

					operandValue = MetrixPublicCache.instance.getItem(cacheName);
				} else if (token.startsWith("clearFromCache(") && token.endsWith(")")) {
					String tokenToSplit = token.substring(15, token.length() - 1);
					ArrayList<String> tokenComponents = splitStringIntoParams(tokenToSplit);

					if (tokenComponents == null || tokenComponents.size() != 1 || MetrixStringHelper.isNullOrEmpty(tokenComponents.get(0)))
                        throw new Exception(AndroidResourceHelper.getMessage("ExTheExpConUnexpected2Args", token, AndroidResourceHelper.getMessage("ClearFromCache")));

					Object cacheNameObject = executeSubExpression(tokenComponents.get(0), clientScript);
					String cacheName = cacheNameObject == null ? "" : String.valueOf(cacheNameObject);

					MetrixPublicCache.instance.removeItem(cacheName);
					operandValue = true;
				} else if (token.startsWith("toggleValueChangedEvent(") && token.endsWith(")")) {
					String tokenToSplit = token.substring(24, token.length() - 1);
					ArrayList<String> tokenComponents = splitStringIntoParams(tokenToSplit);

					if (tokenComponents == null || tokenComponents.size() != 3 || MetrixStringHelper.isNullOrEmpty(tokenComponents.get(0)) || MetrixStringHelper.isNullOrEmpty(tokenComponents.get(1)) || MetrixStringHelper.isNullOrEmpty(tokenComponents.get(2)))
                        throw new Exception(AndroidResourceHelper.getMessage("ExTheExpConUnexpected2Args", token, AndroidResourceHelper.getMessage("ToggleValueChangedEvent")));

					Object tableNameObject = executeSubExpression(tokenComponents.get(0), clientScript);
					String tableName = tableNameObject == null ? "" : String.valueOf(tableNameObject);

					Object columnNameObject = executeSubExpression(tokenComponents.get(1), clientScript);
					String columnName = columnNameObject == null ? "" : String.valueOf(columnNameObject);

					Object isEnabledObject = executeSubExpression(tokenComponents.get(2), clientScript);
					boolean isEnabled = Boolean.valueOf(String.valueOf(isEnabledObject));

					tableName = tableName.toUpperCase();
					columnName = columnName.toUpperCase();
					final String cacheName = String.format("%1$s_%2$s_FVC_SCRIPTDISABLE", tableName, columnName);
					if (isEnabled) {
						// OS executes the entire script *before* handling value change events
						// therefore, we need to add a delay to the removal of cacheName from the cache,
						// so that value change events will pick up the existence of cacheName
						View v = MetrixControlAssistant.getControl(mFormDef.get(), mLayout.get(), tableName, columnName);
						v.postDelayed(new Runnable() {
							public void run() {
								MetrixPublicCache.instance.removeItem(cacheName);
							}
						}, 500);
					} else
						MetrixPublicCache.instance.addItem(cacheName, true);

					operandValue = true;
				} else if (token.startsWith("getCurrentScreenName(") && token.endsWith(")")) {
					String tokenToSplit = token.substring(21, token.length() - 1);

					if (!MetrixStringHelper.isNullOrEmpty(tokenToSplit))
                        throw new Exception(AndroidResourceHelper.getMessage("ExTheExpConUnexpected2Args", token, AndroidResourceHelper.getMessage("GetCurrScreenName")));

					String currActivityName = MetrixScreenManager.getScreenName(mCurrentActivity.get());
					operandValue = currActivityName;
				} else if (token.startsWith("getCurrentWorkflowName(") && token.endsWith(")")) {
					String tokenToSplit = token.substring(23, token.length() - 1);

					if (!MetrixStringHelper.isNullOrEmpty(tokenToSplit))
                        throw new Exception(AndroidResourceHelper.getMessage("ExTheExpConUnexpected2Args", token, AndroidResourceHelper.getMessage("GetCurrWorkflowName")));

					operandValue = MetrixWorkflowManager.getCurrentWorkflowName(mCurrentActivity.get());
				} else if (token.startsWith("round(") && token.endsWith(")")) {
					String tokenToSplit = token.substring(6, token.length() - 1);
					ArrayList<String> tokenComponents = splitStringIntoParams(tokenToSplit);

					if (tokenComponents == null || tokenComponents.size() != 2 || MetrixStringHelper.isNullOrEmpty(tokenComponents.get(0)) || MetrixStringHelper.isNullOrEmpty(tokenComponents.get(1)))
                        throw new Exception(AndroidResourceHelper.getMessage("ExTheExpConUnexpected2Args", token, AndroidResourceHelper.getMessage("Round")));

					Object valueObject = executeSubExpression(tokenComponents.get(0), clientScript);
					Object digitsObject = executeSubExpression(tokenComponents.get(1), clientScript);
					operandValue = valueObject;
					try {
						if (valueObject != null && digitsObject != null) {
							Double dblValue = (Double) valueObject;
							int intDigits = (int) Math.round((Double) digitsObject);
							operandValue = MetrixFloatHelper.round(dblValue, intDigits);
						}
					} catch (Exception e) {
						LogManager.getInstance().debug("Scripting method round() failed.  Returning original value.  Exception: " + e.getMessage());
						operandValue = valueObject;
					}
				} else if (token.startsWith("executeScript(") && token.endsWith(")")) {
					String tokenToSplit = token.substring(14, token.length() - 1);
					ArrayList<String> tokenComponents = splitStringIntoParams(tokenToSplit);

					if (tokenComponents == null || (tokenComponents.size() != 1 && tokenComponents.size() != 2)
							|| (tokenComponents.size() == 1 && (MetrixStringHelper.isNullOrEmpty(tokenComponents.get(0))))
							|| (tokenComponents.size() == 2 && (MetrixStringHelper.isNullOrEmpty(tokenComponents.get(0)) || MetrixStringHelper.isNullOrEmpty(tokenComponents.get(1)))))
                        throw new Exception(AndroidResourceHelper.getMessage("ExTheExpConUnexpected2Args", token, AndroidResourceHelper.getMessage("ExecuteScript")));

					operandValue = null;
					ClientScriptDef nestedScriptDef = null;
					if (tokenComponents.size() == 1) {
						Object clientScriptIdObject = executeSubExpression(tokenComponents.get(0), clientScript);
						String clientScriptId = clientScriptIdObject == null ? "" : String.valueOf(clientScriptIdObject);
						nestedScriptDef = getScriptDefForUnversionedScriptIdentifier(clientScriptId);
					} else {
						Object clientScriptIdObject = executeSubExpression(tokenComponents.get(0), clientScript);
						String clientScriptId = clientScriptIdObject == null ? "" : String.valueOf(clientScriptIdObject);

						Object versionNumObject = executeSubExpression(tokenComponents.get(1), clientScript);
						if (versionNumObject instanceof Double) {
							Double dblVersion = (Double) versionNumObject;
							int versionNum = (int) Math.round(dblVersion);
							String nestedUniqueVS = String.format("%1$s__%2$s", clientScriptId, String.valueOf(versionNum));
							nestedScriptDef = getScriptDefForScriptID(nestedUniqueVS);
						}
					}

					if (nestedScriptDef != null)
						operandValue = executeScriptReturningObject(mCurrentActivity, nestedScriptDef);
				} else if (token.startsWith("getUserInfo(") && token.endsWith(")")) {
					String tokenToSplit = token.substring(12, token.length() - 1);
					ArrayList<String> tokenComponents = splitStringIntoParams(tokenToSplit);

					if (tokenComponents == null || tokenComponents.size() != 1 || MetrixStringHelper.isNullOrEmpty(tokenComponents.get(0)))
                        throw new Exception(AndroidResourceHelper.getMessage("ExTheExpConUnexpected2Args", token, AndroidResourceHelper.getMessage("GetUserInfo")));

					Object keywordObject = executeSubExpression(tokenComponents.get(0), clientScript);
					String keyword = keywordObject == null ? "" : String.valueOf(keywordObject);

					operandValue = null;
					User currentUser = User.getUser();
					if (MetrixStringHelper.valueIsEqual(keyword, "PersonID"))
						operandValue = currentUser.personId;
					else if (MetrixStringHelper.valueIsEqual(keyword, "Name"))
						operandValue = currentUser.name;
					else if (MetrixStringHelper.valueIsEqual(keyword, "FirstName"))
						operandValue = currentUser.firstName;
					else if (MetrixStringHelper.valueIsEqual(keyword, "LastName"))
						operandValue = currentUser.lastName;
					else if (MetrixStringHelper.valueIsEqual(keyword, "Currency"))
						operandValue = currentUser.currency;
					else if (MetrixStringHelper.valueIsEqual(keyword, "WorksFromPlace"))
						operandValue = currentUser.worksFromPlace;
					else if (MetrixStringHelper.valueIsEqual(keyword, "StockFromPlace"))
						operandValue = currentUser.stockFromPlace;
					else if (MetrixStringHelper.valueIsEqual(keyword, "StockFromLocation"))
						operandValue = currentUser.stockFromLocation;
					else if (MetrixStringHelper.valueIsEqual(keyword, "EmailAddress"))
						operandValue = currentUser.emailAddress;
					else if (MetrixStringHelper.valueIsEqual(keyword, "PhoneNumber"))
						operandValue = currentUser.phoneNumber;
					else if (MetrixStringHelper.valueIsEqual(keyword, "MobileNumber"))
						operandValue = currentUser.mobileNumber;
					else if (MetrixStringHelper.valueIsEqual(keyword, "DeviceSequence"))
						operandValue = Double.valueOf(currentUser.sequence);
					else if (MetrixStringHelper.valueIsEqual(keyword, "CorporateCurrency"))
						operandValue = currentUser.corporateCurrency;
					else if (MetrixStringHelper.valueIsEqual(keyword, "LaborRate"))
						operandValue = currentUser.laborRate;
					else if (MetrixStringHelper.valueIsEqual(keyword, "LocaleCode"))
						operandValue = currentUser.localeCode;

					if (operandValue == null)
						operandValue = "";
				} else if (token.startsWith("stringFormat(") && token.endsWith(")")) {
					String tokenToSplit = token.substring(13, token.length() - 1);
					ArrayList<String> tokenComponents = splitStringIntoParams(tokenToSplit);

					if (tokenComponents == null || tokenComponents.size() < 1 || MetrixStringHelper.isNullOrEmpty(tokenComponents.get(0)))
                        throw new Exception(AndroidResourceHelper.getMessage("ExTheExpConUnexpected2Args", token, AndroidResourceHelper.getMessage("StringFormat")));

					Object formatStringObject = executeSubExpression(tokenComponents.get(0), clientScript);
					String formatString = formatStringObject == null ? "" : String.valueOf(formatStringObject);

					String resolvedString = formatString;
					if (tokenComponents.size() > 1) {
						for (int i = 1; i < tokenComponents.size(); i++) {
							Object paramObject = executeSubExpression(tokenComponents.get(i), clientScript, isForDBQuery);
							String stringParam = translateObjectToString(paramObject, isForDBQuery);
							String searchString = "{" + String.valueOf((i - 1)) + "}";
							resolvedString = doStringReplace(resolvedString, searchString, stringParam);
						}
					}
					operandValue = resolvedString;
				} else if (token.startsWith("stringReplace(") && token.endsWith(")")) {
					String tokenToSplit = token.substring(14, token.length() - 1);
					ArrayList<String> tokenComponents = splitStringIntoParams(tokenToSplit);

					if (tokenComponents == null || tokenComponents.size() != 3 || MetrixStringHelper.isNullOrEmpty(tokenComponents.get(0)) || MetrixStringHelper.isNullOrEmpty(tokenComponents.get(1)) || MetrixStringHelper.isNullOrEmpty(tokenComponents.get(2)))
                        throw new Exception(AndroidResourceHelper.getMessage("ExTheExpConUnexpected2Args", token, AndroidResourceHelper.getMessage("StringReplace","{0}")));

					Object origStringObject = executeSubExpression(tokenComponents.get(0), clientScript);
					String origString = origStringObject == null ? "" : String.valueOf(origStringObject);
					Object searchObject = executeSubExpression(tokenComponents.get(1), clientScript, isForDBQuery);
					String searchString = translateObjectToString(searchObject, isForDBQuery);
					Object replacementObject = executeSubExpression(tokenComponents.get(2), clientScript, isForDBQuery);
					String replacement = translateObjectToString(replacementObject, isForDBQuery);

					operandValue = doStringReplace(origString, searchString, replacement);
				} else if (token.startsWith("getLatitude(") && token.endsWith(")")) {
					String tokenToSplit = token.substring(12, token.length() - 1);

					if (!MetrixStringHelper.isNullOrEmpty(tokenToSplit))
                        throw new Exception(AndroidResourceHelper.getMessage("ExTheExpConUnexpected2Args", token, AndroidResourceHelper.getMessage("GetLatitude")));

					operandValue = null;
					Location currentLocation = MetrixLocationAssistant.getCurrentLocation(mCurrentActivity.get());
					if (currentLocation != null)
						operandValue = currentLocation.getLatitude();
				} else if (token.startsWith("getLongitude(") && token.endsWith(")")) {
					String tokenToSplit = token.substring(13, token.length() - 1);

					if (!MetrixStringHelper.isNullOrEmpty(tokenToSplit))
                        throw new Exception(AndroidResourceHelper.getMessage("ExTheExpConUnexpected2Args", token, AndroidResourceHelper.getMessage("GetLongitude")));

					operandValue = null;
					Location currentLocation = MetrixLocationAssistant.getCurrentLocation(mCurrentActivity.get());
					if (currentLocation != null)
						operandValue = currentLocation.getLongitude();
				} else if (token.startsWith("jumpBackToScreen(") && token.endsWith(")")) {
					String tokenToSplit = token.substring(17, token.length() - 1);
					ArrayList<String> tokenComponents = splitStringIntoParams(tokenToSplit);

					if (tokenComponents == null || tokenComponents.size() != 1 || MetrixStringHelper.isNullOrEmpty(tokenComponents.get(0)))
                        throw new Exception(AndroidResourceHelper.getMessage("ExTheExpConUnexpected2Args", token, AndroidResourceHelper.getMessage("JumpBackToScreen")));

					Object screenObject = executeSubExpression(tokenComponents.get(0), clientScript);
					String screenName = screenObject == null ? "" : String.valueOf(screenObject);

					if (AttachmentWidgetManager.isAttachmentAPIScreenName(screenName)) {
						// Disallow Attachment API direct navigation via script
						LogManager.getInstance().error(String.format("jumpBackToScreen(): Screen name (%s) is not supported.", screenName));
						operandValue = false;
					} else {
						Activity currActivity = mCurrentActivity.get();
						if (MetrixApplicationAssistant.screenNameHasClassInCode(screenName)) {
							// Mapping class can be found within the code base
							Intent intent = MetrixActivityHelper.createActivityIntent(currActivity, screenName, Intent.FLAG_ACTIVITY_CLEAR_TOP);
							MetrixActivityHelper.startNewActivity(currActivity, intent);
						} else {
							// This is where codeless screens come into action
							throw new Exception(AndroidResourceHelper.getMessage("CantUseJumpBackTo1Args", screenName));
						}
						operandValue = true;
					}
				} else if (token.startsWith("getAppParam(") && token.endsWith(")")) {
					String tokenToSplit = token.substring(12, token.length() - 1);
					ArrayList<String> tokenComponents = splitStringIntoParams(tokenToSplit);

					if (tokenComponents == null || tokenComponents.size() != 1 || MetrixStringHelper.isNullOrEmpty(tokenComponents.get(0)))
                        throw new Exception(AndroidResourceHelper.getMessage("ExTheExpConUnexpected2Args", token, AndroidResourceHelper.getMessage("GetAppParam")));

					Object paramNameObject = executeSubExpression(tokenComponents.get(0), clientScript);
					String paramName = paramNameObject == null ? "" : String.valueOf(paramNameObject);

					String paramString = MetrixDatabaseManager.getCustomOrBaselineAppParam(paramName);
					// since we just pulled this value from the DB, always translate from DB format
					operandValue = translateStringToObject(paramString, true);
				} else if (token.startsWith("isRoleFunctionEnabled(") && token.endsWith(")")) {
					String tokenToSplit = token.substring(22, token.length() - 1);
					ArrayList<String> tokenComponents = splitStringIntoParams(tokenToSplit);

					if (tokenComponents == null || tokenComponents.size() != 1 || MetrixStringHelper.isNullOrEmpty(tokenComponents.get(0)))
                        throw new Exception(AndroidResourceHelper.getMessage("ExTheExpConUnexpected2Args", token, AndroidResourceHelper.getMessage("IsRoleFuncEnabled")));

					Object functionNameObject = executeSubExpression(tokenComponents.get(0), clientScript);
					String functionName = functionNameObject == null ? "" : String.valueOf(functionNameObject);

					operandValue = MetrixRoleHelper.isGPSFunctionEnabled(functionName);
				} else if (token.startsWith("getJoinTableAlias(") && token.endsWith(")")) {
					String tokenToSplit = token.substring(18, token.length() - 1);
					ArrayList<String> tokenComponents = splitStringIntoParams(tokenToSplit);

					if (tokenComponents == null || tokenComponents.size() != 2 || MetrixStringHelper.isNullOrEmpty(tokenComponents.get(0)) || MetrixStringHelper.isNullOrEmpty(tokenComponents.get(1)))
                        throw new Exception(AndroidResourceHelper.getMessage("ExTheExpConUnexpected2Args", token, AndroidResourceHelper.getMessage("GetJoinTableAlias")));

					Object tableNameObject = executeSubExpression(tokenComponents.get(0), clientScript);
					String tableName = tableNameObject == null ? "" : String.valueOf(tableNameObject);

					Object columnNameObject = executeSubExpression(tokenComponents.get(1), clientScript);
					String columnName = columnNameObject == null ? "" : String.valueOf(columnNameObject);

					String screenName = MetrixScreenManager.getScreenName(mCurrentActivity.get());
					int screenId = MetrixScreenManager.getScreenId(screenName);
					String fieldName = String.format("%1$s.%2$s", tableName, columnName);
					operandValue = MetrixListScreenManager.getJoinTableAliasForFieldName(fieldName, screenId);
				} else if (token.startsWith("closeCurrentScreen(") && token.endsWith(")")) {
					String tokenToSplit = token.substring(19, token.length() - 1);

					if (!MetrixStringHelper.isNullOrEmpty(tokenToSplit))
                        throw new Exception(AndroidResourceHelper.getMessage("ExTheExpConUnexpected2Args", token, AndroidResourceHelper.getMessage("CloseCurrScreen")));

					Activity thisActivity = mCurrentActivity.get();
					if (thisActivity instanceof MetrixBaseActivity) {
						MetrixBaseActivity mActivity = (MetrixBaseActivity) thisActivity;
						mActivity.onBackPressed();
					}
					thisActivity.finish();

					MetrixPublicCache.instance.addItem("scriptingReturnValue", "STOP_EXECUTION");
					return new MetrixScriptFlowControl(RETURN_STRING);
				} else if (token.startsWith("generateDataTransaction(") && token.endsWith(")")) {
					String tokenToSplit = token.substring(24, token.length() - 1);
					ArrayList<String> tokenComponents = splitStringIntoParams(tokenToSplit);

					// tokenComponents[2] can be null/empty, but it must be provided
					if (tokenComponents == null || tokenComponents.size() != 3 || MetrixStringHelper.isNullOrEmpty(tokenComponents.get(0)) || MetrixStringHelper.isNullOrEmpty(tokenComponents.get(1)))
                        throw new Exception(AndroidResourceHelper.getMessage("ExTheExpConUnexpected2Args", token, AndroidResourceHelper.getMessage("GenerateDataTrans")));

					Object tableNameObject = executeSubExpression(tokenComponents.get(0), clientScript);
					String tableName = tableNameObject == null ? "" : String.valueOf(tableNameObject);

					Object transactionTypeObject = executeSubExpression(tokenComponents.get(1), clientScript);
					String transactionType = transactionTypeObject == null ? "" : String.valueOf(transactionTypeObject);

					Object updateDeleteFilterObject = executeSubExpression(tokenComponents.get(2), clientScript);
					String updateDeleteFilter = updateDeleteFilterObject == null ? "" : String.valueOf(updateDeleteFilterObject);

					if (!(MetrixStringHelper.valueIsEqual(transactionType, "INSERT") || MetrixStringHelper.valueIsEqual(transactionType, "UPDATE") || MetrixStringHelper.valueIsEqual(transactionType, "DELETE")))
                        throw new Exception(AndroidResourceHelper.getMessage("TheExpConUnexpSyn2Args", token, AndroidResourceHelper.getMessage("GenerateDataTransShouldUse")));

					MetrixSqlData dataTrans = null;
					if (MetrixStringHelper.valueIsEqual(transactionType, "INSERT"))
						dataTrans = new MetrixSqlData(tableName, MetrixTransactionTypes.INSERT, "");
					else if (MetrixStringHelper.valueIsEqual(transactionType, "UPDATE"))
						dataTrans = new MetrixSqlData(tableName, MetrixTransactionTypes.UPDATE, updateDeleteFilter);
					else if (MetrixStringHelper.valueIsEqual(transactionType, "DELETE"))
						dataTrans = new MetrixSqlData(tableName, MetrixTransactionTypes.DELETE, updateDeleteFilter);
					operandValue = dataTrans;
				} else if (token.startsWith("addToDataTransaction(") && token.endsWith(")")) {
					String tokenToSplit = token.substring(21, token.length() - 1);
					ArrayList<String> tokenComponents = splitStringIntoParams(tokenToSplit);

					if (tokenComponents == null || tokenComponents.size() != 3 || MetrixStringHelper.isNullOrEmpty(tokenComponents.get(0)) || MetrixStringHelper.isNullOrEmpty(tokenComponents.get(1)) || MetrixStringHelper.isNullOrEmpty(tokenComponents.get(2)))
                        throw new Exception(AndroidResourceHelper.getMessage("ExTheExpConUnexpected2Args", token, AndroidResourceHelper.getMessage("AddToDataTrans")));

					Object dataTransactionObject = executeSubExpression(tokenComponents.get(0), clientScript);
					if (!(dataTransactionObject instanceof MetrixSqlData))
                        throw new Exception(AndroidResourceHelper.getMessage("TheExpConUnexpSyn2Args", token, AndroidResourceHelper.getMessage("DataTransIsIncorrType")));

					Object columnNameObject = executeSubExpression(tokenComponents.get(1), clientScript);
					String columnName = columnNameObject == null ? "" : String.valueOf(columnNameObject);

					// force DB conversion, since this is being fed into MetrixSqlData object
					Object valueObject = executeSubExpression(tokenComponents.get(2), clientScript);
					String valueString = translateObjectToString(valueObject, true);

					MetrixSqlData dataTrans = (MetrixSqlData)dataTransactionObject;
					dataTrans.dataFields.add(new DataField(columnName, valueString));
					operandValue = dataTrans;
				} else if (token.startsWith("saveDataTransaction(") && token.endsWith(")")) {
					String tokenToSplit = token.substring(20, token.length() - 1);
					ArrayList<String> tokenComponents = splitStringIntoParams(tokenToSplit);

					if (tokenComponents == null || tokenComponents.size() != 2 || MetrixStringHelper.isNullOrEmpty(tokenComponents.get(0)) || MetrixStringHelper.isNullOrEmpty(tokenComponents.get(1)))
                        throw new Exception(AndroidResourceHelper.getMessage("ExTheExpConUnexpected2Args", token, AndroidResourceHelper.getMessage("SaveDataTrans")));

					Object dataTransactionObject = executeSubExpression(tokenComponents.get(0), clientScript);
					if (!(dataTransactionObject instanceof MetrixSqlData))
                        throw new Exception(AndroidResourceHelper.getMessage("TheExpConUnexpSyn2Args", token, AndroidResourceHelper.getMessage("DataTransIsIncorrType")));

					Object friendlyNameObject = executeSubExpression(tokenComponents.get(1), clientScript);
					String friendlyName = friendlyNameObject == null ? "" : String.valueOf(friendlyNameObject);

					ArrayList<MetrixSqlData> sqlDataArray = new ArrayList<MetrixSqlData>();
                    MetrixSqlData dataTrans = (MetrixSqlData)dataTransactionObject;
                    if (dataTrans.transactionType == MetrixTransactionTypes.INSERT)
                    {
                        MetrixKeys tableKeys = MetrixDatabases.getMetrixTablePrimaryKeys(dataTrans.tableName);
                        if (tableKeys != null && tableKeys.keyInfo != null && tableKeys.keyInfo.keySet() != null && tableKeys.keyInfo.keySet().size() > 0)
                        {
                            ArrayList<String> missingKeys = new ArrayList<String>(tableKeys.keyInfo.keySet());
                            for (DataField field : dataTrans.dataFields)
                            {
                                if (missingKeys.contains(field.name))
                                    missingKeys.remove(field.name);
                            }

                            if (missingKeys.size() > 0)
                            {
                                long primaryKey = MetrixDatabaseManager.generatePrimaryKey(dataTrans.tableName);
                                for (String pkColumnName : missingKeys)
                                {
                                    dataTrans.dataFields.add(new DataField(pkColumnName, String.valueOf(primaryKey)));
                                }
                            }
                        }
                    }
					sqlDataArray.add(dataTrans);
					MetrixTransaction transactionInfo = new MetrixTransaction();
					operandValue = MetrixUpdateManager.update(sqlDataArray, true, transactionInfo, friendlyName, mCurrentActivity.get());
				} else if (token.startsWith("getValueFromListDataRow(") && token.endsWith(")")) {
					String tokenToSplit = token.substring(24, token.length() - 1);
					ArrayList<String> tokenComponents = splitStringIntoParams(tokenToSplit);

					if (tokenComponents == null || tokenComponents.size() != 3 || MetrixStringHelper.isNullOrEmpty(tokenComponents.get(0)) || MetrixStringHelper.isNullOrEmpty(tokenComponents.get(1)) || MetrixStringHelper.isNullOrEmpty(tokenComponents.get(2)))
                        throw new Exception(AndroidResourceHelper.getMessage("ExTheExpConUnexpected2Args", token, AndroidResourceHelper.getMessage("GetValFromListData")));

					Object rowObject = executeSubExpression(tokenComponents.get(0), clientScript);
					if (!(rowObject instanceof HashMap))
                        throw new Exception(AndroidResourceHelper.getMessage("TheExpConUnexpSyn2Args", token, AndroidResourceHelper.getMessage("ListDataRowIsIncorr")));

					Object tableNameObject = executeSubExpression(tokenComponents.get(1), clientScript);
					String tableName = tableNameObject == null ? "" : String.valueOf(tableNameObject);

					Object columnNameObject = executeSubExpression(tokenComponents.get(2), clientScript);
					String columnName = columnNameObject == null ? "" : String.valueOf(columnNameObject);

					// this string value has UI format, so force isForDBQuery to FALSE
					HashMap<String, String> row = (HashMap<String, String>)rowObject;
					String stringValue = row.get(String.format("%1$s.%2$s", tableName, columnName));
					Object objectValue = translateStringToObject(stringValue, false);
					operandValue = objectValue;
				} else if (token.startsWith("getCurrentListData(") && token.endsWith(")")) {
					String tokenToSplit = token.substring(19, token.length() - 1);

					if (!MetrixStringHelper.isNullOrEmpty(tokenToSplit))
                        throw new Exception(AndroidResourceHelper.getMessage("ExTheExpConUnexpected2Args", token, AndroidResourceHelper.getMessage("GetCurrListData")));

					String cacheName = String.format("%s__CurrentListData", mListPopulationScreenName);
					operandValue = MetrixPublicCache.instance.getItem(cacheName);
				} else if (token.startsWith("setValueOnListDataRow(") && token.endsWith(")")) {
					String tokenToSplit = token.substring(22, token.length() - 1);
					ArrayList<String> tokenComponents = splitStringIntoParams(tokenToSplit);

					if (tokenComponents == null || tokenComponents.size() != 4 || MetrixStringHelper.isNullOrEmpty(tokenComponents.get(0)) || MetrixStringHelper.isNullOrEmpty(tokenComponents.get(1)) || MetrixStringHelper.isNullOrEmpty(tokenComponents.get(2)) || MetrixStringHelper.isNullOrEmpty(tokenComponents.get(3)))
                        throw new Exception(AndroidResourceHelper.getMessage("ExTheExpConUnexpected2Args", token, AndroidResourceHelper.getMessage("SetValOnListData")));

					Object rowObject = executeSubExpression(tokenComponents.get(0), clientScript);
					if (!(rowObject instanceof HashMap))
                        throw new Exception(AndroidResourceHelper.getMessage("TheExpConUnexpSyn2Args", token, AndroidResourceHelper.getMessage("ListDataRowIsIncorr")));

					Object tableNameObject = executeSubExpression(tokenComponents.get(1), clientScript);
					String tableName = tableNameObject == null ? "" : String.valueOf(tableNameObject);

					Object columnNameObject = executeSubExpression(tokenComponents.get(2), clientScript);
					String columnName = columnNameObject == null ? "" : String.valueOf(columnNameObject);

					// force UI conversion, since this is being fed into a list item
					Object valueObject = executeSubExpression(tokenComponents.get(3), clientScript);
					String valueString = enforceNewlines(translateObjectToString(valueObject, false));

					HashMap<String, String> row = (HashMap<String, String>)rowObject;
					row.put(String.format("%1$s.%2$s", tableName, columnName), valueString);
					operandValue = row;
				} else if (token.startsWith("getRowFromListData(") && token.endsWith(")")) {
					String tokenToSplit = token.substring(19, token.length() - 1);
					ArrayList<String> tokenComponents = splitStringIntoParams(tokenToSplit);

					if (tokenComponents == null || tokenComponents.size() != 2 || MetrixStringHelper.isNullOrEmpty(tokenComponents.get(0)) || MetrixStringHelper.isNullOrEmpty(tokenComponents.get(1)))
                        throw new Exception(AndroidResourceHelper.getMessage("ExTheExpConUnexpected2Args", token, AndroidResourceHelper.getMessage("GetRowFromListData")));

					Object dataListObject = executeSubExpression(tokenComponents.get(0), clientScript);
					if (!(dataListObject instanceof ArrayList))
                        throw new Exception(AndroidResourceHelper.getMessage("TheExpConUnexpSyn2Args", token, AndroidResourceHelper.getMessage("ListDataSetIsIncorr")));

					Object indexObject = executeSubExpression(tokenComponents.get(1), clientScript);
					int actualIndex = -1;
					try {
						if (indexObject != null) { actualIndex = (int) Math.round((Double) indexObject); }
					} catch (Exception e) {
                        throw new Exception(AndroidResourceHelper.getMessage("TheExpConUnexpSyn2Args", token, AndroidResourceHelper.getMessage("IndexIsIncorrDataType")));
					}

					operandValue = null;
					ArrayList<HashMap<String, String>> dataList = (ArrayList<HashMap<String, String>>)dataListObject;
					if (actualIndex > -1 && actualIndex < dataList.size())
						operandValue = dataList.get(actualIndex);
				} else if (token.startsWith("setRowOnListData(") && token.endsWith(")")) {
					String tokenToSplit = token.substring(17, token.length() - 1);
					ArrayList<String> tokenComponents = splitStringIntoParams(tokenToSplit);

					if (tokenComponents == null || tokenComponents.size() != 3 || MetrixStringHelper.isNullOrEmpty(tokenComponents.get(0)) || MetrixStringHelper.isNullOrEmpty(tokenComponents.get(1)) || MetrixStringHelper.isNullOrEmpty(tokenComponents.get(2)))
                        throw new Exception(AndroidResourceHelper.getMessage("ExTheExpConUnexpected2Args", token, AndroidResourceHelper.getMessage("SetRowOnListData")));

					Object dataListObject = executeSubExpression(tokenComponents.get(0), clientScript);
					if (!(dataListObject instanceof ArrayList))
                        throw new Exception(AndroidResourceHelper.getMessage("TheExpConUnexpSyn2Args", token, AndroidResourceHelper.getMessage("ListDataSetIsIncorr")));

					Object rowObject = executeSubExpression(tokenComponents.get(1), clientScript);
					if (!(rowObject instanceof HashMap))
                        throw new Exception(AndroidResourceHelper.getMessage("TheExpConUnexpSyn2Args", token, AndroidResourceHelper.getMessage("ListDataRowIsIncorr")));

					Object indexObject = executeSubExpression(tokenComponents.get(2), clientScript);
					int actualIndex = -1;
					try {
						if (indexObject != null) { actualIndex = (int) Math.round((Double) indexObject); }
					} catch (Exception e) {
                        throw new Exception(AndroidResourceHelper.getMessage("TheExpConUnexpSyn2Args", token, AndroidResourceHelper.getMessage("IndexIsIncorrDataType")));
					}

					ArrayList<HashMap<String, String>> dataList = (ArrayList<HashMap<String, String>>)dataListObject;
					if (actualIndex > -1 && actualIndex < dataList.size()) {
						HashMap<String, String> row = (HashMap<String, String>)rowObject;
						dataList.set(actualIndex, row);
					}
					operandValue = dataList;
				} else if (token.startsWith("launchBrowser(") && token.endsWith(")")) {
					String tokenToSplit = token.substring(14, token.length() - 1);
					ArrayList<String> tokenComponents = splitStringIntoParams(tokenToSplit);

					if (tokenComponents == null || tokenComponents.size() != 1 || MetrixStringHelper.isNullOrEmpty(tokenComponents.get(0)))
                        throw new Exception(AndroidResourceHelper.getMessage("ExTheExpConUnexpected2Args", token, AndroidResourceHelper.getMessage("LaunchBrowser")));

					Object urlObject = executeSubExpression(tokenComponents.get(0), clientScript);
					String url = urlObject == null ? "" : String.valueOf(urlObject);
					if (!MetrixStringHelper.isNullOrEmpty(url)) {
						if (!url.startsWith("http://") && !url.startsWith("https://"))
							url = "http://" + url;

						Activity currActivity = mCurrentActivity.get();
						Uri webURI = Uri.parse(url);
						Intent browserIntent = new Intent(Intent.ACTION_VIEW, webURI);
						if (browserIntent.resolveActivity(currActivity.getPackageManager()) != null) {
							currActivity.startActivity(browserIntent);
						}
					}
				} else if (token.startsWith("launchURI(") && token.endsWith(")")) {
					String tokenToSplit = token.substring(10, token.length() - 1);
					ArrayList<String> tokenComponents = splitStringIntoParams(tokenToSplit);

					if (tokenComponents == null || tokenComponents.size() != 1 || MetrixStringHelper.isNullOrEmpty(tokenComponents.get(0)))
                        throw new Exception(AndroidResourceHelper.getMessage("ExTheExpConUnexpected2Args", token, AndroidResourceHelper.getMessage("LaunchBrowser")));

					Object urlObject = executeSubExpression(tokenComponents.get(0), clientScript);
					if (urlObject != null) {
						Uri uri = Uri.parse(String.valueOf(urlObject));
						if (uri == null) {
							throw new Exception(AndroidResourceHelper.getMessage("ExInvalidURIFormat", String.valueOf(urlObject)));
						}
						Intent launchIntent = new Intent(Intent.ACTION_VIEW, uri);
						try {
							mCurrentActivity.get().startActivity(launchIntent);
						} catch (ActivityNotFoundException exception) {
							MetrixUIHelper.showSnackbar(mCurrentActivity.get(), AndroidResourceHelper.getMessage("NoAppAvailableToHandleURI", uri.getScheme() == null ? uri.toString() : uri.getScheme()));
						}
					}
				} else if (token.startsWith("setCurrentWorkflow(") && token.endsWith(")")) {
					String tokenToSplit = token.substring(19, token.length() - 1);
					ArrayList<String> tokenComponents = splitStringIntoParams(tokenToSplit);

					if (tokenComponents == null || tokenComponents.size() != 1 || MetrixStringHelper.isNullOrEmpty(tokenComponents.get(0)))
                        throw new Exception(AndroidResourceHelper.getMessage("ExTheExpConUnexpected2Args", token, AndroidResourceHelper.getMessage("SetCurrWorkflow")));

					Object workflowNameObject = executeSubExpression(tokenComponents.get(0), clientScript);
					String workflowName = workflowNameObject == null ? "" : String.valueOf(workflowNameObject);
					MetrixWorkflowManager.setCurrentWorkflowName(mCurrentActivity.get(), workflowName);
				} else if(token.startsWith("launchMapByAddress(") && token.endsWith(")")){
					Activity currActivity = mCurrentActivity.get();
					if(currActivity != null) {
						if (MetrixLibraryHelper.googleMapsIsInstalled(mCurrentActivity.get())) {
							String tokenToSplit = token.substring(19, token.length() - 1);
							ArrayList<String> tokenComponents = splitStringIntoParams(tokenToSplit);

							if (tokenComponents == null || tokenComponents.size() != 1 || MetrixStringHelper.isNullOrEmpty(tokenComponents.get(0)))
                                throw new Exception(AndroidResourceHelper.getMessage("ExTheExpConUnexpected2Args", token, AndroidResourceHelper.getMessage("LaunchMapByAddr")));

							Object addressObject = executeSubExpression(tokenComponents.get(0), clientScript);
							String address = addressObject == null ? "" : String.valueOf(addressObject);
							Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=" + address));
							currActivity.startActivity(intent);
						}
						else {
							MetrixUIHelper.showSnackbar(mCurrentActivity.get(), AndroidResourceHelper.getMessage("NoGoogleMapsInstalled"));
						}
					}
				} else if(token.startsWith("showPhoneDialer(") && token.endsWith(")")){
					Activity currActivity = mCurrentActivity.get();
					if(currActivity != null) {
						if (currActivity.getPackageManager().hasSystemFeature(PackageManager.FEATURE_TELEPHONY)) {
							String tokenToSplit = token.substring(16, token.length() - 1);
							ArrayList<String> tokenComponents = splitStringIntoParams(tokenToSplit);

							if (tokenComponents == null || tokenComponents.size() != 1 || MetrixStringHelper.isNullOrEmpty(tokenComponents.get(0)))
                                throw new Exception(AndroidResourceHelper.getMessage("ExTheExpConUnexpected2Args", token, AndroidResourceHelper.getMessage("ShowPhoneDialer")));

							Object phoneNumberObject = executeSubExpression(tokenComponents.get(0), clientScript);
							String phoneNumber = phoneNumberObject == null ? "" : String.valueOf(phoneNumberObject).trim();

							Intent intent = new Intent(Intent.ACTION_DIAL);
							intent.setData(Uri.parse("tel:" + phoneNumber));
							currActivity.startActivity(intent);
						}
						else {
							MetrixUIHelper.showSnackbar(mCurrentActivity.get(), AndroidResourceHelper.getMessage("NoTelephonyServiceAvailable"));
						}
					}
				} else if(token.startsWith("launchEmailApp(") && token.endsWith(")")){
					String tokenToSplit = token.substring(15, token.length() - 1);
					ArrayList<String> tokenComponents = splitStringIntoParams(tokenToSplit);

					if (tokenComponents == null || tokenComponents.size() != 1 || MetrixStringHelper.isNullOrEmpty(tokenComponents.get(0)))
                        throw new Exception(AndroidResourceHelper.getMessage("ExTheExpConUnexpected2Args", token, AndroidResourceHelper.getMessage("LaunchEmailApp")));

					Object emailAddressObject = executeSubExpression(tokenComponents.get(0), clientScript);
					String emailAddress = emailAddressObject == null ? "" : String.valueOf(emailAddressObject).trim();
					Activity currActivity = mCurrentActivity.get();
					if(currActivity != null) {
						Intent intent = null;
						try {
							intent = new Intent(Intent.ACTION_VIEW, Uri.parse(String.format("mailto:%s", emailAddress)));
							intent.putExtra(Intent.EXTRA_SUBJECT, "");
							intent.putExtra(Intent.EXTRA_TEXT, "");
							currActivity.startActivity(intent);
						} catch (android.content.ActivityNotFoundException ex) {
							//In-case if Gmail application isn't found..
							intent = new Intent(Intent.ACTION_SEND);
							intent.setType("message/rfc822");
							intent.putExtra(Intent.EXTRA_EMAIL, new String[]{emailAddress});
							intent.putExtra(Intent.EXTRA_SUBJECT, "");
							intent.putExtra(Intent.EXTRA_TEXT, "");
							currActivity.startActivity(Intent.createChooser(intent, AndroidResourceHelper.getMessage("SendMail")));
						}
					}
				} else if (token.startsWith("getCurrentListRowControlValue(") && token.endsWith(")")) {
					String tokenToSplit = token.substring(30, token.length() - 1);
					ArrayList<String> tokenComponents = splitStringIntoParams(tokenToSplit);

					if (tokenComponents == null || tokenComponents.size() != 2 || MetrixStringHelper.isNullOrEmpty(tokenComponents.get(0)) || MetrixStringHelper.isNullOrEmpty(tokenComponents.get(1)))
                        throw new Exception(AndroidResourceHelper.getMessage("ExTheExpConUnexpected2Args", token, AndroidResourceHelper.getMessage("GetCurrListRowCont")));

					Object tableNameObject = executeSubExpression(tokenComponents.get(0), clientScript);
					String tableName = tableNameObject == null ? "" : String.valueOf(tableNameObject);

					Object columnNameObject = executeSubExpression(tokenComponents.get(1), clientScript);
					String columnName = columnNameObject == null ? "" : String.valueOf(columnNameObject);

					ViewGroup currentListRowContainer = (ViewGroup)MetrixPublicCache.instance.getItem("CurrentListRowContainer");
					if(currentListRowContainer == null)
                        throw new Exception(AndroidResourceHelper.getMessage("Exception1Args", AndroidResourceHelper.getMessage("CurrListObjCannotBeNull")));

					String controlValue = MetrixControlAssistant.GetCurrentListRowControlValue(currentListRowContainer, tableName, columnName);
					operandValue = controlValue;
				} else if(token.startsWith("launchSMSApp(") && token.endsWith(")")) {
					Activity currActivity = mCurrentActivity.get();
					if(currActivity != null) {
						if (currActivity.getPackageManager().hasSystemFeature(PackageManager.FEATURE_TELEPHONY)) {
							String tokenToSplit = token.substring(13, token.length() - 1);
							ArrayList<String> tokenComponents = splitStringIntoParams(tokenToSplit);

							if (tokenComponents == null || tokenComponents.size() != 1 || MetrixStringHelper.isNullOrEmpty(tokenComponents.get(0)))
                                throw new Exception(AndroidResourceHelper.getMessage("ExTheExpConUnexpected2Args", token, AndroidResourceHelper.getMessage("LaunchSMSApp")));

							Object phoneNumberObject = executeSubExpression(tokenComponents.get(0), clientScript);
							String phoneNumber = phoneNumberObject == null ? "" : String.valueOf(phoneNumberObject).trim();

							if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
								Intent smsIntent = new Intent(Intent.ACTION_SENDTO);
								smsIntent.addCategory(Intent.CATEGORY_DEFAULT);
								smsIntent.setType("vnd.android-dir/mms-sms");
								smsIntent.setData(Uri.parse("sms:" + phoneNumber));
								currActivity.startActivity(smsIntent);
							} else {
								//For early versions, the old code remain
								Intent intent = new Intent(Intent.ACTION_VIEW);
								intent.putExtra("address", phoneNumber);
								intent.setType("vnd.android-dir/mms-sms");
								currActivity.startActivity(intent);
							}
						} else {
							MetrixUIHelper.showSnackbar(mCurrentActivity.get(), AndroidResourceHelper.getMessage("NoTelephonyServiceAvailable"));
						}
					}
				} else if (token.startsWith("generatePrimaryKey(") && token.endsWith(")")) {
					String tokenToSplit = token.substring(19, token.length() - 1);
					ArrayList<String> tokenComponents = splitStringIntoParams(tokenToSplit);

					if (tokenComponents == null || tokenComponents.size() != 1 || MetrixStringHelper.isNullOrEmpty(tokenComponents.get(0)))
                        throw new Exception(AndroidResourceHelper.getMessage("ExTheExpConUnexpected2Args", token, AndroidResourceHelper.getMessage("GeneratePrimaryKey")));

					Object tableNameObject = executeSubExpression(tokenComponents.get(0), clientScript);
					String tableName = tableNameObject == null ? "" : String.valueOf(tableNameObject).trim();

					int newPKValue = MetrixDatabaseManager.generatePrimaryKey(tableName);
					operandValue = String.valueOf(newPKValue);
				} else if (token.startsWith("openAttachmentWidget(") && token.endsWith(")")) {
					String tokenToSplit = token.substring(21, token.length() - 1);
					ArrayList<String> tokenComponents = splitStringIntoParams(tokenToSplit);

					if (tokenComponents == null || tokenComponents.size() != 4 || MetrixStringHelper.isNullOrEmpty(tokenComponents.get(0)))
						throw new Exception(AndroidResourceHelper.getMessage("ExTheExpConUnexpected2Args", token, AndroidResourceHelper.getMessage("OpenAttachmentWidget")));

					Object attachmentListScreenNameObject = executeSubExpression(tokenComponents.get(0), clientScript);
					String attachmentListScreenName = attachmentListScreenNameObject == null ? "" : String.valueOf(attachmentListScreenNameObject).trim();

					if (MetrixStringHelper.isNullOrEmpty(attachmentListScreenName))
						throw new Exception(AndroidResourceHelper.getMessage("ExTheExpConUnexpected2Args", token, AndroidResourceHelper.getMessage("OpenAttachmentWidget")));

					Object attachmentCardScreenNameObject = executeSubExpression(tokenComponents.get(1), clientScript);
					String attachmentCardScreenName = attachmentCardScreenNameObject == null ? "" : String.valueOf(attachmentCardScreenNameObject).trim();

					Object transactionIdTableNameObject = executeSubExpression(tokenComponents.get(2), clientScript);
					String transactionIdTableName = transactionIdTableNameObject == null ? "" : String.valueOf(transactionIdTableNameObject).trim();

					Object transactionIdColumnNameObject = executeSubExpression(tokenComponents.get(3), clientScript);
					String transactionIdColumnName = transactionIdColumnNameObject == null ? "" : String.valueOf(transactionIdColumnNameObject).trim();

					Object workflowNameObject = null;
					if(tokenComponents.size() > 4)
						workflowNameObject = executeSubExpression(tokenComponents.get(4), clientScript);
					String workflowName = workflowNameObject == null ? "" : String.valueOf(workflowNameObject).trim();

					// Do not proceed if current transaction is an INSERT
					String transactionType = "";
					MetrixFormDef formDef = mFormDef.get();
					if (formDef != null && formDef.tables != null && formDef.tables.size() > 0) {
						transactionType = formDef.tables.get(0).transactionType.toString();
					}

					if (MetrixStringHelper.valueIsEqual(transactionType.toUpperCase(), "INSERT")) {
						MetrixUIHelper.showSnackbar(mCurrentActivity.get(), AndroidResourceHelper.getMessage("OpenAttachWidgetTransTypeError"));
					} else {
						AttachmentWidgetManager.openFromScript(mCurrentActivity, attachmentListScreenName, attachmentCardScreenName, transactionIdTableName, transactionIdColumnName, workflowName);
					}
				} else {
                	// If we get here, then we should have a variable name.
                	ClientScriptVariableItem item = new ClientScriptVariableItem();
                	item.mName = token;
                	operandValue = item;
                }
                
                if (operandValue == null) operandValue = NULL_PLACEHOLDER_STRING;
                operandStack.push(operandValue);
			}
		}
		
		// Our stack should contain a single value, our final result.  Otherwise we've got more operands than operators.
		if (operandStack.size() != 1)
            throw new Exception(AndroidResourceHelper.getMessage("ExTheExpConUnexpected2Args", expressionIn, AndroidResourceHelper.getMessage("TheExpConMoreOps")));
		
		Object finalValue = operandStack.pop();
		finalValue = (finalValue == NULL_PLACEHOLDER_STRING) ? null : finalValue;
		if (finalValue instanceof ClientScriptVariableItem) {
			ClientScriptVariableItem item = (ClientScriptVariableItem)finalValue;
			return getVariableValue(clientScript, item.mName);
		}		
		return finalValue;
	}

	private static String translateObjectToString(Object paramObject, Boolean isForDBQuery) {
		// default value will be empty string (i.e., if object is null or not one of the following data types)
		String stringParam = "";

		if (paramObject instanceof String) {
			stringParam = (String) paramObject;
		} else if (paramObject instanceof Double) {
			Double numParam = (Double)paramObject;
			if (isForDBQuery)
				stringParam = MetrixFloatHelper.convertNumericFromForcedLocaleToDB(String.valueOf(numParam), Locale.US);
			else
				stringParam = MetrixFloatHelper.currentLocaleNumericValue(numParam);
		} else if (paramObject instanceof Boolean) {
			Boolean boolParam = (Boolean)paramObject;
			stringParam = String.valueOf(boolParam);
		} else if (paramObject instanceof Date) {
			Date dateParam = (Date)paramObject;
			if (isForDBQuery)
				stringParam = MetrixDateTimeHelper.convertDateTimeFromDateToDB(dateParam);
			else
				stringParam = MetrixDateTimeHelper.convertDateTimeFromDateToUI(dateParam);
		}

		return stringParam;
	}

	private static Object translateStringToObject(String paramString, Boolean isForDBQuery) {
		Object objParam = null;

		if (paramString != null) {
			if (isForDBQuery) {
				Date tryDate = MetrixDateTimeHelper.convertDateTimeFromDBToDate(paramString, false);
				if (tryDate != null && !MetrixStringHelper.isValidPhoneNumber(paramString)) {
					objParam = tryDate;
				} else if (!MetrixStringHelper.isNullOrEmpty(paramString) && MetrixStringHelper.isDouble(paramString) && !MetrixStringHelper.isZeroPrefixNonNumber(paramString)) {
					Double dblValue = Double.valueOf((MetrixFloatHelper.convertNumericFromDBToNumber(paramString)).doubleValue());
					objParam = dblValue;
				} else
					objParam = paramString;
			} else {
				Date tryDate = MetrixDateTimeHelper.convertDateTimeFromUIToDate(paramString, false);
				if (tryDate != null && !MetrixStringHelper.isValidPhoneNumber(paramString)) {
					objParam = tryDate;
				} else if (!MetrixStringHelper.isNullOrEmpty(paramString) && MetrixStringHelper.isDouble(paramString) && !MetrixStringHelper.isZeroPrefixNonNumber(paramString)) {
					Double dblValue = Double.valueOf((MetrixFloatHelper.convertNumericFromUIToNumber(paramString)).doubleValue());
					objParam = dblValue;
				} else
					objParam = paramString;
			}
		}

		return objParam;
	}

	private static ArrayList<Hashtable<String, Object>> translateDBStringsToDataTypes(ArrayList<Hashtable<String, String>> origList) {
		ArrayList<Hashtable<String, Object>> newList = null;
		
		if (origList != null && origList.size() > 0) {
			newList = new ArrayList<Hashtable<String, Object>>();
			for (Hashtable<String, String> item : origList) {
				if (item != null && item.size() > 0) {
					Hashtable<String, Object> newItem = new Hashtable<String, Object>();
					for (Entry<String, String> entry : item.entrySet()) {
						Object newValue = null;
						String origValue = entry.getValue();
						Date tryDate = MetrixDateTimeHelper.convertDateTimeFromDBToDate(origValue, false);
	                	if (tryDate != null && !MetrixStringHelper.isValidPhoneNumber(origValue)) {
	                		newValue = tryDate;
	                	} else if (!MetrixStringHelper.isNullOrEmpty(origValue) && MetrixStringHelper.isDouble(origValue) && !MetrixStringHelper.isZeroPrefixNonNumber(origValue)) {
	                		Double dblValue = Double.valueOf((MetrixFloatHelper.convertNumericFromDBToNumber(origValue)).doubleValue());
	                		newValue = dblValue;
	                	} else
	                		newValue = origValue;
	                	
	                	newItem.put(entry.getKey(), newValue);
					}
					newList.add(newItem);
				}
			}
		}
		
		return newList;
	}
	
	private static String enforceNewlines(String origString) {
		if (!origString.contains("\\n"))
			return origString;
		
		StringBuilder newString = new StringBuilder();
		String[] lines = origString.split("\\\\n");
		for (String line : lines) {
			newString.append(String.format("%s\n", line));
		}	
		return newString.toString().trim();
	}
	
	private static Object getVariableValue(ClientScriptDef clientScript, String variableName) throws Exception {
		if (clientScript == null || clientScript.mVariables == null || clientScript.mVariables.size() == 0 || !clientScript.mVariables.containsKey(variableName))
        throw new Exception(AndroidResourceHelper.getMessage("Exception1Args", AndroidResourceHelper.getMessage("AttemptedToGetValVar1Args", variableName)));
		
		return clientScript.mVariables.get(variableName);
	}
	
	private static Object executeSubExpression(String expressionIn, ClientScriptDef clientScript) throws Exception {
		return executeSubExpression(expressionIn, clientScript, false);
	}
	
	private static Object executeSubExpression(String expressionIn, ClientScriptDef clientScript, boolean isForDBQuery) throws Exception {
		// A sub-expression has not yet been parsed into tokens.  Do that now.
        ArrayList<String> expressionTokens = getTokens(expressionIn);

        // Convert the infix tokens to postfix for processing.
        ArrayList<String> expressionPostfixTokens = convertInfixToPostfix(expressionIn, expressionTokens);

        // Execute the postfix tokens.
        return evaluatePostfixTokens(expressionIn, expressionPostfixTokens, clientScript, isForDBQuery);
	}
	
	private static ArrayList<String> getTokens(String expressionIn) throws Exception {
		ArrayList<String> tokens =  new ArrayList<String>();
		
		String tempString = expressionIn;
		StringBuilder currentWord = new StringBuilder();
		
		// Allow for string values enclosed by either double quote or single quote characters.
		boolean doubleQuoteLiteralStarted = false;
        boolean singleQuoteLiteralStarted = false;
        
        while (!MetrixStringHelper.isNullOrEmpty(tempString)) { 
        	if (!singleQuoteLiteralStarted && tempString.startsWith("\"") && !tempString.startsWith("\\\""))
        		doubleQuoteLiteralStarted = !doubleQuoteLiteralStarted;
        	if (!doubleQuoteLiteralStarted && tempString.startsWith("'") && !tempString.startsWith("\\'"))
        		singleQuoteLiteralStarted = !singleQuoteLiteralStarted;

        	ClientScriptToken currToken = startsWithToken(tempString);
        	if (!doubleQuoteLiteralStarted && !singleQuoteLiteralStarted && currToken != null) {        		
        		// We have encountered a token so add our current word to the list and then the token.
        		if (currentWord != null && currentWord.length() > 0)
        			tokens.add(currentWord.toString());
        		
        		currentWord.setLength(0);
        		
        		// Spaces are used to separate words but aren't a token themselves.
        		if (!MetrixStringHelper.isNullOrEmpty(currToken.token) && !currToken.token.equals(" "))
        			tokens.add(currToken.token);
        		
        		// skip the separator
        		tempString = tempString.substring(currToken.token.length());
        	} else {
        		if (doubleQuoteLiteralStarted && tempString.startsWith("\\\"")) {
                    currentWord.append(tempString.substring(0, 2));
					if (tempString.length() > 2)
					    tempString = tempString.substring(2);
					else
					    tempString = "";
        		} else if (singleQuoteLiteralStarted && tempString.startsWith("\\'")) {
                    currentWord.append(tempString.substring(0, 2));
					if (tempString.length() > 2)
					    tempString = tempString.substring(2);
					else
					    tempString = "";
        		} else {
                    currentWord.append(tempString.substring(0, 1));
					if (tempString.length() > 1)
					    tempString = tempString.substring(1);
					else
					    tempString = "";
        		}
        	}	
        }
        
        if (currentWord != null && currentWord.length() > 0)
        	tokens.add(currentWord.toString());
        
        if (doubleQuoteLiteralStarted || singleQuoteLiteralStarted)
            throw new Exception((AndroidResourceHelper.getMessage("Exception1Args", AndroidResourceHelper.getMessage("ExpWasNotTerminateProperly1Args", expressionIn))));
		
		return tokens;
	}
	
	private static ClientScriptToken startsWithToken(String expressionIn) {
		ClientScriptToken currToken = new ClientScriptToken();
		currToken.token = "";
		currToken.type = ClientScriptTokenTypes.Undefined;
		// Using traditional for-loop instead of enhanced foreach loop saved about 100 ms execution time
		final int funcSize = FUNCTION_TOKEN_LIST.size();
		for (int i = 0; i < funcSize; i++) {
			String tokenPossibility = FUNCTION_TOKEN_LIST.get(i);
			if (expressionIn.startsWith(tokenPossibility)) {
				// Since we want to support nested function calls, the first ) we find may not be the end of this functions definition.
                // We'll keep track of the number of ( so we know when we have the correct number of corresponding ) and the function is finished.
				int parenNests = 1;
				
				// We also need to ignore ( and ) in string values.
             	// Allow for string values enclosed by either double quote or single quote characters.
				boolean doubleQuoteLiteralStarted = false;
		        boolean singleQuoteLiteralStarted = false;
		        Character prevChar = null;
		        
		        for (int index = tokenPossibility.length(); index < expressionIn.length(); index++) {
		        	Character currentChar = expressionIn.charAt(index);
		        	if (!singleQuoteLiteralStarted && currentChar.equals('"') && (prevChar == null || !prevChar.equals('\\')))
		        		doubleQuoteLiteralStarted = !doubleQuoteLiteralStarted;
		        	if (!doubleQuoteLiteralStarted && currentChar.equals('\'') && (prevChar == null || !prevChar.equals('\\')))
		        		singleQuoteLiteralStarted = !singleQuoteLiteralStarted;
		        	
		        	if (!doubleQuoteLiteralStarted && !singleQuoteLiteralStarted) {
		        		if (currentChar.equals('('))
		        			parenNests++;
		        		else if (currentChar.equals(')')) {
		        			parenNests--;
		        			
		        			if (parenNests == 0) {
		        				currToken.token = expressionIn.substring(0, index + 1);
		        				currToken.type = ClientScriptTokenTypes.Function;
		        				return currToken;
		        			}
		        		}
		        	}
                    prevChar = currentChar;
		        }
			}
		}
		
		// Check for array sub-scripting.  Things like [0] and ["column_name"].
        // We want these as their own token in its entirety.
        if (expressionIn.startsWith("[")) {
        	// Since we want to support nested function calls, the first ] we find may not be the end of this array index.
            // We'll keep track of the number of [ so we know when we have the correct number of corresponding ] 
        	// and the sub-script is finished.
        	int indexNests = 1;
        	
        	// We also need to ignore [ and ] in string values.
            // Allow for string values enclosed by either double quote or single quote characters.
        	boolean doubleQuoteLiteralStarted = false;
	        boolean singleQuoteLiteralStarted = false;
	        Character prevChar = null;
	        
	        for (int index = 1; index < expressionIn.length(); index++) {
	        	Character currentChar = expressionIn.charAt(index);
	        	if (!singleQuoteLiteralStarted && currentChar.equals('"') && (prevChar == null || !prevChar.equals('\\')))
	        		doubleQuoteLiteralStarted = !doubleQuoteLiteralStarted;
	        	if (!doubleQuoteLiteralStarted && currentChar.equals('\'') && (prevChar == null || !prevChar.equals('\\')))
	        		singleQuoteLiteralStarted = !singleQuoteLiteralStarted;
	        	
	        	if (!doubleQuoteLiteralStarted && !singleQuoteLiteralStarted) {
	        		if (currentChar.equals('['))
	        			indexNests++;
	        		else if (currentChar.equals(']')) {
	        			indexNests--;
	        			
	        			if (indexNests == 0) {
	        				currToken.token = expressionIn.substring(0, index + 1);
	        				currToken.type = ClientScriptTokenTypes.Operator;
	        				return currToken;
	        			}
	        		}
	        	}
	        	
	        	prevChar = currentChar;
	        }
        }

        // Put longest token possibilities first just so we don't accidentally think
        // we have a > when the token was actually a >=, for example.
		final int tokenSize = TOKEN_LIST.size();
		for (int i = 0; i < tokenSize; i++) {
			String tokenPossibility = TOKEN_LIST.get(i);
			if (expressionIn.startsWith(tokenPossibility)) {
				currToken.token = tokenPossibility;
				currToken.type = ClientScriptTokenTypes.Operator;
				return currToken;
			}
		}
        
		return null;
	}

	private static ArrayList<String> splitStringIntoParams(String stringValue) {
		// For parameters we always want to split by commas.
		ArrayList<String> splitString = new ArrayList<String>();
		
		if (MetrixStringHelper.isNullOrEmpty(stringValue) || !stringValue.contains(SEPARATOR))
			splitString.add(stringValue);
		else {
			String tempString = stringValue;
			StringBuilder currentWord = new StringBuilder();
			
			// There are a number of different special considerations we need to make 
			// when breaking down the value into parameters.
            // We need to handle double quote literal, single quote literals, and handle embedded function calls.
			boolean doubleLiteralStarted = false;
            boolean singleLiteralStarted = false;
            
            while (!MetrixStringHelper.isNullOrEmpty(tempString)) {
            	if (!singleLiteralStarted && tempString.startsWith("\"") && !tempString.startsWith("\\\""))
            		doubleLiteralStarted = !doubleLiteralStarted;
            	if (!doubleLiteralStarted && tempString.startsWith("'") && !tempString.startsWith("\\'"))
            		singleLiteralStarted = !singleLiteralStarted;
            	
            	if (!doubleLiteralStarted && !singleLiteralStarted && tempString.startsWith(SEPARATOR)) {
            		splitString.add(currentWord.toString());
            		currentWord.setLength(0);
            		tempString = tempString.substring(SEPARATOR.length());
            	} else {
            		if (doubleLiteralStarted && tempString.startsWith("\\\"")) {
            			currentWord.append(tempString.substring(0, 2));
            			if (tempString.length() > 2)
            				tempString = tempString.substring(2);
            			else
            				tempString = "";
            		} else if (singleLiteralStarted && tempString.startsWith("\\'")) {
            			currentWord.append(tempString.substring(0, 2));
            			if (tempString.length() > 2)
            				tempString = tempString.substring(2);
            			else
            				tempString = "";
            		} else {
            			// This handles embedded functions.  We don't want to accidentally split this function's
                        // parameters up by an embedded function's parameters.
            			ClientScriptToken currToken = startsWithToken(tempString);
            			if (currToken != null && currToken.type == ClientScriptTokenTypes.Function) {
            				currentWord.append(currToken.token);
            				if (tempString.length() > currToken.token.length())
                                tempString = tempString.substring(currToken.token.length());
                            else
                                tempString = "";
            			} else {
            				// Just a normal character has been found.  Add it to the current word.
            				currentWord.append(tempString.substring(0, 1));
                            if (tempString.length() > 1)
                                tempString = tempString.substring(1);
                            else
                                tempString = "";
            			}
            		}
            	}
            }
            
            splitString.add(currentWord.toString());
		}
		
		return splitString;
	}

	private static boolean stringListHasNullEntries(ArrayList<String> stringList) {
		for (String item : stringList) {
			if (MetrixStringHelper.isNullOrEmpty(item))
				return true;
		}
		return false;
	}

	private static String doStringReplace(String origString, String searchString, String replacement) {
		return origString.replace(searchString, replacement);
	}
}