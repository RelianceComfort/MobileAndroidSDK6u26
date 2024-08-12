package com.metrix.architecture.managers;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.ViewGroup;

import com.metrix.architecture.assistants.MetrixApplicationAssistant;
import com.metrix.architecture.assistants.MetrixControlAssistant;
import com.metrix.architecture.constants.MetrixSaveResult;
import com.metrix.architecture.constants.MetrixTransactionTypes;
import com.metrix.architecture.database.MetrixDatabaseManager;
import com.metrix.architecture.database.MobileApplication;
import com.metrix.architecture.designer.MetrixWorkflowManager;
import com.metrix.architecture.metadata.MetrixColumnDef;
import com.metrix.architecture.metadata.MetrixFormDef;
import com.metrix.architecture.metadata.MetrixKeys;
import com.metrix.architecture.metadata.MetrixSqlData;
import com.metrix.architecture.metadata.MetrixTableDef;
import com.metrix.architecture.metadata.MetrixTableStructure;
import com.metrix.architecture.metadata.MetrixTransaction;
import com.metrix.architecture.superclasses.MetrixBaseActivity;
import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.architecture.utilities.DataField;
import com.metrix.architecture.utilities.Global;
import com.metrix.architecture.utilities.Global.MessageStatus;
import com.metrix.architecture.utilities.Global.UploadType;
import com.metrix.architecture.utilities.LogManager;
import com.metrix.architecture.utilities.MetrixActivityHelper;
import com.metrix.architecture.utilities.MetrixCurrentKeysHelper;
import com.metrix.architecture.utilities.MetrixDate;
import com.metrix.architecture.utilities.MetrixDateTime;
import com.metrix.architecture.utilities.MetrixDateTimeHelper;
import com.metrix.architecture.utilities.MetrixPublicCache;
import com.metrix.architecture.utilities.MetrixStringHelper;
import com.metrix.architecture.utilities.MetrixTime;
import com.metrix.architecture.utilities.MetrixUIHelper;
import com.metrix.architecture.utilities.User;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map.Entry;

/**
 * This class contains methods which manage CRUD transactions performed by the
 * activities.
 *
 * @since 5.4
 */
public class MetrixUpdateManager {

	/**
	 * Updates the database with the data collected on the received layout based
	 * upon the activities' meta data.
	 *
	 * @param activity
	 *            the activity requesting the update.
	 * @param layout
	 *            the layout showing the data for the update.
	 * @param metrixFormDef
	 *            the meta data for the layout.
	 * @return true if the update was successful, false otherwise.
	 *
	 *         <pre>
	 * MetrixTransaction transactionInfo = MetrixTransaction.getTransaction(&quot;task&quot;, &quot;task_id&quot;);
	 * MetrixUpdateManager.update(this, mLayout, mFormDef, transactionInfo, false, DebriefEscalation.class, true, &quot;Escalation&quot;);
	 * </pre>
	 */
	public static MetrixSaveResult update(Activity activity, ViewGroup layout, MetrixFormDef metrixFormDef, MetrixTransaction transactionInfo,
										  boolean allowContinueOnError, Class<?> nextActivity, boolean finishCurrentActivity, String transactionFriendlyName) {
		return MetrixUpdateManager.update(activity, layout, metrixFormDef, true, transactionInfo, allowContinueOnError, nextActivity, finishCurrentActivity,
				transactionFriendlyName, false);
	}

	public static MetrixSaveResult update(Activity activity, ViewGroup layout, MetrixFormDef metrixFormDef, MetrixTransaction transactionInfo,
										  boolean allowContinueOnError, Class<?> nextActivity, boolean finishCurrentActivity, String transactionFriendlyName, boolean advanceWorkflow) {
		return MetrixUpdateManager.update(activity, layout, metrixFormDef, true, transactionInfo, allowContinueOnError, nextActivity, finishCurrentActivity,
				transactionFriendlyName, false, advanceWorkflow);
	}

	/**
	 * Updates the database with the data collected on the received layout based
	 * upon the activities' meta data.
	 *
	 * @param activity
	 *            the activity requesting the update.
	 * @param layout
	 *            the layout showing the data for the update.
	 * @param metrixFormDef
	 *            the meta data for the layout.
	 * @param writeTransactionLog
	 *            a transaction log row should be written for this transaction
	 *            so that it can be synced with M5.
	 * @return true if the update was successful, false otherwise.
	 *
	 *         <pre>
	 * MetrixTransaction transactionInfo = MetrixTransaction.getTransaction(&quot;task&quot;, &quot;task_id&quot;);
	 * MetrixUpdateManager.update(this, mLayout, mFormDef, transactionInfo, false, DebriefEscalation.class, true, &quot;Escalation&quot;);
	 * </pre>
	 */
	public static MetrixSaveResult update(Activity activity, ViewGroup layout, MetrixFormDef metrixFormDef, boolean writeTransactionLog,
										  MetrixTransaction transactionInfo, boolean allowContinueOnError, Class<?> nextActivity, boolean finishCurrentActivity,
										  String transactionFriendlyName, boolean waitingPrevious) {
		return MetrixUpdateManager.update(activity, layout, metrixFormDef, writeTransactionLog, transactionInfo, allowContinueOnError, nextActivity, finishCurrentActivity, transactionFriendlyName, waitingPrevious, false);
	}

	@SuppressWarnings("finally")
	public static MetrixSaveResult update(Activity activity, ViewGroup layout, MetrixFormDef metrixFormDef, boolean writeTransactionLog,
										  MetrixTransaction transactionInfo, boolean allowContinueOnError, Class<?> nextActivity, boolean finishCurrentActivity,
										  String transactionFriendlyName, boolean waitingPrevious, boolean advanceWorkflow) {

		if (activity == null) {
			throw new IllegalArgumentException(AndroidResourceHelper.getMessage("TheActivityParamIsReq"));
		}

		if (layout == null) {
			throw new IllegalArgumentException(AndroidResourceHelper.getMessage("TheLayoutParameterIsRequired"));
		}

		if (metrixFormDef == null) {
			throw new IllegalArgumentException(AndroidResourceHelper.getMessage("TheMetrixFormdefParam"));
		}

		if (transactionInfo == null) {
			throw new IllegalArgumentException(AndroidResourceHelper.getMessage("TheTransInfoParamReq"));
		}

		MetrixSaveResult errorResultStatus = MetrixSaveResult.ERROR;
		if (allowContinueOnError) {
			errorResultStatus = MetrixSaveResult.ERROR_WITH_CONTINUE;
		}
		boolean exceptionThrow = false;
		boolean transactionsSuccessful = false;

		if (!changesFound(activity, layout, metrixFormDef)) {
			return errorResultStatus;
		}

		if (!fieldsAreValid(activity, layout, metrixFormDef, allowContinueOnError, nextActivity, finishCurrentActivity, advanceWorkflow)) {
			return errorResultStatus;
		}

		try {
			updatePrimaryKeyValuesIfNeeded(layout, metrixFormDef);

			MetrixDatabaseManager.begintransaction();
			transactionsSuccessful = performTransactions(layout, metrixFormDef, writeTransactionLog, transactionInfo, transactionFriendlyName, activity,
					waitingPrevious);

			updateLayoutOriginalValues(activity, layout, metrixFormDef);

			if (transactionsSuccessful)
				MetrixDatabaseManager.setTransactionSuccessful();
		} catch (Exception ex) {
			MetrixUIHelper.showSnackbar(activity, ex.getLocalizedMessage().toString());
			LogManager.getInstance().error(ex);
			exceptionThrow = true;
			throw ex;
		} finally {
			MetrixDatabaseManager.endTransaction();
			// resume the sync after the update
			resumeSync();

			if (exceptionThrow || transactionsSuccessful == false)
				return errorResultStatus;
			else {
				if (nextActivity != null) {
					if (finishCurrentActivity) {
						Intent intent = MetrixActivityHelper.createActivityIntent(activity, nextActivity);
						MetrixActivityHelper.startNewActivityAndFinish(activity, intent);
					} else {
						Intent intent = MetrixActivityHelper.createActivityIntent(activity, nextActivity);
						MetrixActivityHelper.startNewActivity(activity, intent);
					}
				} else {
					// next activity and advance workflow should be mutually exclusive
					// finishCurrentActivity should still fire, regardless of whether we have a next activity specified

					if (advanceWorkflow)
						MetrixWorkflowManager.advanceWorkflow(activity);

					if (finishCurrentActivity)
						activity.finish();
				}

				return MetrixSaveResult.SUCCESSFUL;
			}
		}
	}

	/**
	 * Performs a database update based on the received list of MetrixSqlData.
	 * This is typically used to perform a non-databound transaction.
	 *
	 * @param dataList
	 *            Details about the data to be updated.
	 * @param writeTransactionLog
	 *            TRUE if this change should be synced, FALSE otherwise.
	 * @param transactionInfo
	 *            The sync transaction this should be part of.
	 * @param transactionFriendlyName
	 *            A friendly description of this transaction displayed on the
	 *            sync layouts.
	 * @param activity
	 *            The current activity.
	 * @return TRUE if the transaction was successful, FALSE otherwise.
	 */
	public static boolean update(ArrayList<MetrixSqlData> dataList, boolean writeTransactionLog, MetrixTransaction transactionInfo,
								 String transactionFriendlyName, Activity activity) {

		if (dataList == null) {
			throw new IllegalArgumentException(AndroidResourceHelper.getMessage("TheDatalistParamIsReq"));
		}

		if (transactionInfo == null) {
			throw new IllegalArgumentException(AndroidResourceHelper.getMessage("TheTransInfoParamReq"));
		}

		return update(dataList, writeTransactionLog, transactionInfo, transactionFriendlyName, activity, false);
	}

	/**
	 * Performs a database update based on the received list of MetrixSqlData.
	 * This is typically used to perform a non-databound transaction.
	 *
	 * @param dataList
	 *            Details about the data to be updated.
	 * @param writeTransactionLog
	 *            TRUE if this change should be synced, FALSE otherwise.
	 * @param transactionInfo
	 *            The sync transaction this should be part of.
	 * @param transactionFriendlyName
	 *            A friendly description of this transaction displayed on the
	 *            sync layouts.
	 * @param activity
	 *            The current activity.
	 * @param waitingPrevious
	 * @return TRUE if the transaction was successful, FALSE otherwise.
	 *
	 *         <pre>
	 * ArrayList&lt;MetrixSqlData&gt; partNeedsToUpdate = new ArrayList&lt;MetrixSqlData&gt;();
	 * String rowId = MetrixDatabaseManager.getFieldStringValue(&quot;part_need&quot;, &quot;metrix_row_id&quot;, &quot;request_id='&quot; + requestId + &quot;' and sequence=&quot; + partNeedSequence);
	 *
	 * MetrixSqlData data = new MetrixSqlData(&quot;part_need&quot;, MetrixTransactionTypes.UPDATE, &quot;metrix_row_id=&quot; + rowId);
	 * data.dataFields.add(new DataField(&quot;metrix_row_id&quot;, rowId));
	 * data.dataFields.add(new DataField(&quot;request_id&quot;, requestId));
	 * data.dataFields.add(new DataField(&quot;sequence&quot;, partNeedSequence));
	 * data.dataFields.add(new DataField(&quot;parts_used&quot;, &quot;Y&quot;));
	 * partNeedsToUpdate.add(data);
	 *
	 * MetrixTransaction transactionInfo = new MetrixTransaction();
	 * boolean successful = MetrixUpdateManager.update(partNeedsToUpdate, true, transactionInfo, &quot;Part Needs&quot;, this);
	 * </pre>
	 */
	public static boolean update(ArrayList<MetrixSqlData> dataList, boolean writeTransactionLog, MetrixTransaction transactionInfo,
								 String transactionFriendlyName, Activity activity, boolean waitingPrevious) {

		if (dataList == null) {
			throw new IllegalArgumentException(AndroidResourceHelper.getMessage("TheDatalistParamIsReq"));
		}

		if (transactionInfo == null) {
			throw new IllegalArgumentException(AndroidResourceHelper.getMessage("TheTransInfoParamReq"));
		}

		boolean exceptionThrow = false;
		boolean transactionsSuccessful = false;

		try {
			MetrixDatabaseManager.begintransaction();
			transactionsSuccessful = performTransactions(dataList, writeTransactionLog, transactionInfo, transactionFriendlyName, activity, waitingPrevious);

			if (transactionsSuccessful) {
				MetrixDatabaseManager.setTransactionSuccessful();
			}
		} catch (Exception ex) {
			exceptionThrow = true;
		} finally {
			MetrixDatabaseManager.endTransaction();
			// resume the sync after the update
			resumeSync();

			if (exceptionThrow || transactionsSuccessful == false) {
				return false;
			}
		}

		return true;
	}

	public static boolean delete(Activity activity, String tableName, String metrixRowId, String primaryKeyName, String primaryKeyValue, String friendlyName, MetrixTransaction transaction) {
		Hashtable<String, String> primaryKeys = new Hashtable<String, String>();
		primaryKeys.put(primaryKeyName,  primaryKeyValue);
		return MetrixUpdateManager.delete(activity, tableName, metrixRowId, primaryKeys, friendlyName, transaction);
	}

	public static boolean delete(Activity activity, String tableName, String metrixRowId, Hashtable<String, String> primaryKeys, String friendlyName, MetrixTransaction transaction) {
		try {
			MetrixDatabaseManager.begintransaction();

			MetrixSqlData data = new MetrixSqlData();
			data.transactionType = MetrixTransactionTypes.DELETE;
			data.tableName = tableName;
			data.rowId = Double.valueOf(metrixRowId);
			data.dataFields.add(new DataField("metrix_row_id", data.rowId));

			for (Entry<String, String> entry : primaryKeys.entrySet()) {
				// Check if we need to updated primary keys
				if (MetrixStringHelper.isNegativeValue(entry.getValue())) {
					String currentKey = MetrixDatabaseManager.getFieldStringValue(true, tableName, entry.getKey(), "metrix_row_id = ?", new String[]{metrixRowId}, null, null, null, null);
					if (!MetrixStringHelper.isNullOrEmpty(currentKey) && !MetrixStringHelper.valueIsEqual(currentKey, entry.getValue())) {
						entry.setValue(currentKey);
					}
				}
				data.dataFields.add(new DataField(entry.getKey(), entry.getValue()));
			}

			data.filter = tableName + ".metrix_row_id = " + data.rowId;
			if (MetrixUpdateManager.performTransaction(data, true, transaction, friendlyName, activity, false)) {
				MetrixDatabaseManager.setTransactionSuccessful();
			}

			return true;
		}
		catch (Exception e) {
			LogManager.getInstance().error(e);
			return false;
		}
		finally {
			MetrixDatabaseManager.endTransaction();
		}
	}

	/**
	 * Indicates whether or not changes were discovered on the current activity.
	 *
	 * @param activity
	 *            the current activity.
	 * @param layout
	 *            the layout related to the activity.
	 * @param metrixFormDef
	 *            the meta data for the layout.
	 * @return TRUE if changes were found, FALSE otherwise.
	 */
	private static boolean changesFound(Activity activity, ViewGroup layout, MetrixFormDef metrixFormDef) {
		if (activity == null) {
			throw new IllegalArgumentException(AndroidResourceHelper.getMessage("TheActivityParamIsReq"));
		}

		if (layout == null) {
			throw new IllegalArgumentException(AndroidResourceHelper.getMessage("TheLayoutParameterIsRequired"));
		}

		if (metrixFormDef == null) {
			throw new IllegalArgumentException(AndroidResourceHelper.getMessage("TheMetrixFormdefParam"));
		}

		@SuppressWarnings({ "unchecked", "rawtypes" })
		HashMap<String, String> originalValues = (HashMap) MetrixPublicCache.instance.getItem(MetrixManagerConstants.METRIX_LAYOUT_ORIGINAL_VALUES);

		for (MetrixTableDef tableDef : metrixFormDef.tables) {
			if (MetrixStringHelper.isNullOrEmpty(tableDef.tableName)==false && tableDef.tableName.compareToIgnoreCase("custom") == 0) {
				continue;
			}
			if ((tableDef.transactionType == MetrixTransactionTypes.INSERT) || (tableDef.transactionType == MetrixTransactionTypes.CORRECT_ERROR)
					|| (tableDef.transactionType == MetrixTransactionTypes.UPDATE)) {
				for (MetrixColumnDef columnDef : tableDef.columns) {
					String originalValue = null;
					if (originalValues != null) {
						originalValue = originalValues.get(tableDef.tableName + "." + columnDef.columnName);
					}

					String currentValue = MetrixControlAssistant.getValue(columnDef, layout);

					if (originalValue != null) {
						if (!originalValue.equals(currentValue)) {
							return true;
						}
					} else if (originalValue == null && currentValue != null) {
						return true;
					}
				}
			}
		}

		return false;
	}

	private static void updateLayoutOriginalValues(Activity activity, ViewGroup layout, MetrixFormDef metrixFormDef) {
		if (activity == null) {
			throw new IllegalArgumentException(AndroidResourceHelper.getMessage("TheActivityParamIsReq"));
		}

		if (layout == null) {
			throw new IllegalArgumentException(AndroidResourceHelper.getMessage("TheLayoutParameterIsRequired"));
		}

		if (metrixFormDef == null) {
			throw new IllegalArgumentException(AndroidResourceHelper.getMessage("TheMetrixFormdefParam"));
		}

		@SuppressWarnings({ "unchecked", "rawtypes" })
		HashMap<String, String> originalValues = (HashMap) MetrixPublicCache.instance.getItem(MetrixManagerConstants.METRIX_LAYOUT_ORIGINAL_VALUES);

		for (MetrixTableDef tableDef : metrixFormDef.tables) {
			if (MetrixStringHelper.isNullOrEmpty(tableDef.tableName)==false && tableDef.tableName.compareToIgnoreCase("custom") == 0) {
				continue;
			}
			if ((tableDef.transactionType == MetrixTransactionTypes.INSERT) || (tableDef.transactionType == MetrixTransactionTypes.CORRECT_ERROR)
					|| (tableDef.transactionType == MetrixTransactionTypes.UPDATE)) {
				for (MetrixColumnDef columnDef : tableDef.columns) {
					String originalValue = null;
					if (originalValues != null) {
						originalValue = originalValues.get(tableDef.tableName + "." + columnDef.columnName);
					}
					else {
						originalValues = new HashMap<String, String>();
					}

					String currentValue = MetrixControlAssistant.getValue(columnDef, layout);

					if (originalValue != null) {
						if (!originalValue.equals(currentValue)) {
							originalValues.put(tableDef.tableName + "." + columnDef.columnName, currentValue);
						}
					} else if (originalValue == null && currentValue != null) {
						originalValues.put(tableDef.tableName + "." + columnDef.columnName, currentValue);
					}
				}
			}
		}

		MetrixPublicCache.instance.addItem(MetrixManagerConstants.METRIX_LAYOUT_ORIGINAL_VALUES, originalValues);
	}

	/**
	 * Determines whether or not the controls on the received layout contain
	 * valid values based on the meta data.
	 *
	 * @param activity
	 *            the activity requesting the validation.
	 * @param layout
	 *            the layout related to the activity.
	 * @param metrixFormDef
	 *            the meta data for the layout.
	 * @return true if all of the controls have valid values, false otherwise.
	 */
	public static boolean fieldsAreValid(Activity activity, ViewGroup layout, MetrixFormDef metrixFormDef, boolean allowContinueOnError,
										  Class<?> nextActivity, boolean finishCurrentActivity, boolean advanceWorkflow) {

		if (activity == null) {
			throw new IllegalArgumentException(AndroidResourceHelper.getMessage("TheActivityParamIsReq"));
		}

		if (layout == null) {
			throw new IllegalArgumentException(AndroidResourceHelper.getMessage("TheLayoutParameterIsRequired"));
		}

		if (metrixFormDef == null) {
			throw new IllegalArgumentException(AndroidResourceHelper.getMessage("TheMetrixFormdefParam"));
		}

		String errorMessage = "";

		errorMessage = requiredFieldsAreSet(activity, layout, metrixFormDef);
		if (MetrixStringHelper.isNullOrEmpty(errorMessage)) {
			errorMessage = dataTypesAreValid(activity, layout, metrixFormDef);
			if (MetrixStringHelper.isNullOrEmpty(errorMessage)) {
				errorMessage = fieldsPassValidations(activity, layout, metrixFormDef);
			}
		}

		if (!MetrixStringHelper.isNullOrEmpty(errorMessage)) {
			if (allowContinueOnError) {
				MetrixBaseActivity baseActivity = (MetrixBaseActivity) activity;
				baseActivity.showIgnoreErrorDialog(errorMessage, nextActivity, finishCurrentActivity, advanceWorkflow);
			} else {
				MetrixUIHelper.showSnackbar(activity, errorMessage);
			}

			return false;
		} else {
			return true;
		}
	}

	/**
	 * Ensures that all controls identified as being required in the meta data
	 * have been populated.
	 *
	 * @param activity
	 *            the activity requesting the validation.
	 * @param layout
	 *            the layout related to the activity.
	 * @param metrixFormDef
	 *            the meta data for the layout.
	 * @return true if all of the required controls have values, false otherwise
	 */
	public static String requiredFieldsAreSet(Activity activity, ViewGroup layout, MetrixFormDef metrixFormDef) {

		if (activity == null) {
			throw new IllegalArgumentException(AndroidResourceHelper.getMessage("TheActivityParamIsReq"));
		}

		if (layout == null) {
			throw new IllegalArgumentException(AndroidResourceHelper.getMessage("TheLayoutParameterIsRequired"));
		}

		if (metrixFormDef == null) {
			throw new IllegalArgumentException(AndroidResourceHelper.getMessage("TheMetrixFormdefParam"));
		}

		StringBuilder message = null;

		for (MetrixTableDef tableDef : metrixFormDef.tables) {
			for (MetrixColumnDef columnDef : tableDef.columns) {
				if (columnDef.required && MetrixStringHelper.isNullOrEmpty(MetrixControlAssistant.getValue(columnDef, layout))) {
					if (message == null) {
						message = new StringBuilder();
						message.append(AndroidResourceHelper.getMessage("RequiredFieldsNotPopulated"));
					} else {
						message.append(", ");
					}

					if (MetrixStringHelper.isNullOrEmpty(columnDef.friendlyName)) {
						message.append(columnDef.columnName);
					} else {
						message.append(columnDef.friendlyName);
					}
				}
			}
		}

		if (message != null) {
			return message.toString();
		} else {
			return "";
		}
	}

	/**
	 * Ensures that the data contained in each control matches the data type
	 * identified in the meta data.
	 *
	 * @param activity
	 *            the activity requesting the validation.
	 * @param layout
	 *            the layout related to the activity.
	 * @param metrixFormDef
	 *            the meta data for the layout.
	 * @return true if all of the controls values are valid, false otherwise
	 */
	private static String dataTypesAreValid(Activity activity, ViewGroup layout, MetrixFormDef metrixFormDef) {

		if (activity == null) {
			throw new IllegalArgumentException(AndroidResourceHelper.getMessage("TheActivityParamIsReq"));
		}

		if (layout == null) {
			throw new IllegalArgumentException(AndroidResourceHelper.getMessage("TheLayoutParameterIsRequired"));
		}

		if (metrixFormDef == null) {
			throw new IllegalArgumentException(AndroidResourceHelper.getMessage("TheMetrixFormdefParam"));
		}

		StringBuilder message = null;

		for (MetrixTableDef tableDef : metrixFormDef.tables) {
			for (MetrixColumnDef columnDef : tableDef.columns) {
				if (columnDef.dataType == int.class || columnDef.dataType == double.class) {
					String value = MetrixControlAssistant.getValue(columnDef, layout);

					if (!MetrixStringHelper.isNullOrEmpty(value)) {
						if (columnDef.dataType == int.class && !MetrixStringHelper.isInteger(value)) {
							if (message == null) {
								message = new StringBuilder();
								message.append(AndroidResourceHelper.getMessage("FieldsAcceptOnlyNumeric"));
							} else {
								message.append(", ");
							}

							if (MetrixStringHelper.isNullOrEmpty(columnDef.friendlyName)) {
								message.append(columnDef.columnName);
							} else {
								message.append(columnDef.friendlyName);
							}
						} else if (columnDef.dataType == double.class && !MetrixStringHelper.isDouble(value)) {
							if (message == null) {
								message = new StringBuilder();
								message.append(AndroidResourceHelper.getMessage("FieldsAcceptOnlyNumeric"));
							} else {
								message.append(", ");
							}

							if (MetrixStringHelper.isNullOrEmpty(columnDef.friendlyName)) {
								message.append(columnDef.columnName);
							} else {
								message.append(columnDef.friendlyName);
							}
						}
					}
				}
			}
		}

		if (message != null) {
			return message.toString();
		} else {
			return "";
		}
	}

	/**
	 * Ensures that the control values all pass any custom validations set in
	 * the meta data for them.
	 *
	 * @param activity
	 *            the activity requesting the validation.
	 * @param layout
	 *            the layout related to the activity.
	 * @param metrixFormDef
	 *            the meta data for the layout.
	 * @return true if all of the required controls have valid values, false
	 *         otherwise
	 */
	private static String fieldsPassValidations(Activity activity, ViewGroup layout, MetrixFormDef metrixFormDef) {

		if (activity == null) {
			throw new IllegalArgumentException(AndroidResourceHelper.getMessage("TheActivityParamIsReq"));
		}

		if (layout == null) {
			throw new IllegalArgumentException(AndroidResourceHelper.getMessage("TheLayoutParameterIsRequired"));
		}

		if (metrixFormDef == null) {
			throw new IllegalArgumentException(AndroidResourceHelper.getMessage("TheMetrixFormdefParam"));
		}

		StringBuilder message = null;

		for (MetrixTableDef tableDef : metrixFormDef.tables) {
			for (MetrixColumnDef columnDef : tableDef.columns) {
				if (!MetrixStringHelper.isNullOrEmpty(columnDef.validation)) {
					String value = MetrixControlAssistant.getValue(columnDef, layout);

					if (!value.matches(columnDef.validation)) {
						if (message == null) {
							message = new StringBuilder();
							message.append(AndroidResourceHelper.getMessage("FieldsContainInvalidValues"));
						} else {
							message.append(", ");
						}

						if (MetrixStringHelper.isNullOrEmpty(columnDef.friendlyName)) {
							message.append(columnDef.columnName);
						} else {
							message.append(columnDef.friendlyName);
						}
					}
				}
			}
		}

		if (message != null) {
			return message.toString();
		} else {
			return "";
		}
	}

	/**
	 * Ensures that the primary keys have up-to-date values (used directly before updating data).
	 *
	 * @param layout
	 *            the layout related to the activity.
	 * @param metrixFormDef
	 *            the meta data for the layout.
	 *
	 * @since 5.6.2
	 */
	private static void updatePrimaryKeyValuesIfNeeded(ViewGroup layout, MetrixFormDef metrixFormDef) {
		for (MetrixTableDef tableDef : metrixFormDef.tables) {
			if (tableDef.tableName.compareToIgnoreCase("custom") == 0) {
				continue;
			}

			String rowId = "";
			if ((tableDef.transactionType == MetrixTransactionTypes.UPDATE)) {
				for (MetrixColumnDef columnDef : tableDef.columns) {
					if (columnDef.columnName.compareToIgnoreCase(Global.MetrixRowId) == 0) {
						rowId = MetrixControlAssistant.getValue(columnDef, layout);
						break;
					}
				}

				@SuppressWarnings({ "unchecked", "rawtypes" })
				HashMap<String, String> originalValues = (HashMap) MetrixPublicCache.instance.getItem(MetrixManagerConstants.METRIX_LAYOUT_ORIGINAL_VALUES);
				for (MetrixColumnDef columnDef : tableDef.columns) {
					String originalValue = originalValues.get(tableDef.tableName + "." + columnDef.columnName);
					if (!MetrixStringHelper.isNullOrEmpty(originalValue)) {
						if (columnDef.dataType != Date.class  && columnDef.dataType != MetrixDate.class && columnDef.dataType != MetrixTime.class && columnDef.dataType != MetrixDateTime.class) {
							if (columnDef.dataType == int.class || columnDef.dataType == double.class) {
								if (columnDef.primaryKey == false) {
									continue;
								}
							}

							if (MetrixStringHelper.isNegativeValue(originalValue) && columnDef.primaryKey) {
								String newOriginalValue = MetrixDatabaseManager.getFieldStringValue(tableDef.tableName, columnDef.columnName, String.format("metrix_row_id = %s", rowId));
								if (!MetrixStringHelper.isNullOrEmpty(newOriginalValue) && !MetrixStringHelper.valueIsEqual(originalValue, newOriginalValue)) {
									originalValue = newOriginalValue;
									MetrixControlAssistant.setValue(metrixFormDef, layout, tableDef.tableName, columnDef.columnName, newOriginalValue);
									originalValues.put((tableDef.tableName + "." + columnDef.columnName), newOriginalValue);
									MetrixPublicCache.instance.addItem(MetrixManagerConstants.METRIX_LAYOUT_ORIGINAL_VALUES, originalValues);
								}
							}
						}
					}
				}
			}
		}
	}

	/**
	 * Execute the sql statements to save the bound data to the database and
	 * optionally write a transaction log.
	 *
	 * @param layout
	 *            the layout related to the activity.
	 * @param metrixFormDef
	 *            the meta data for the layout.
	 * @param writeTransactionLog
	 *            a transaction log row should be written for this transaction
	 *            so that it can be synced with M5.
	 * @return an array list containing all of the sql statements generated.
	 */
	@Deprecated
	public static boolean performTransactions(ViewGroup layout, MetrixFormDef metrixFormDef, boolean writeTransactionLog, MetrixTransaction transactionInfo,
											  String transactionFriendlyName, Activity activity, boolean waitingPrevious) {

		if (activity == null) {
			throw new IllegalArgumentException(AndroidResourceHelper.getMessage("TheActivityParamIsReq"));
		}

		if (layout == null) {
			throw new IllegalArgumentException(AndroidResourceHelper.getMessage("TheLayoutParameterIsRequired"));
		}

		if (metrixFormDef == null) {
			throw new IllegalArgumentException(AndroidResourceHelper.getMessage("TheMetrixFormdefParam"));
		}

		if (transactionInfo == null) {
			throw new IllegalArgumentException(AndroidResourceHelper.getMessage("TheTransInfoParamReq"));
		}

		boolean sqlSuccess = false;
		int index = 0;
		HashMap<String, MetrixTableStructure> tablesDefinition = MobileApplication.getTableDefinitionsFromCache();

		for (MetrixTableDef tableDef : metrixFormDef.tables) {
			if (MetrixStringHelper.isNullOrEmpty(tableDef.tableName)==false && tableDef.tableName.compareToIgnoreCase("custom") == 0) {
				continue;
			}

			if (tableDef.transactionType != MetrixTransactionTypes.SELECT) {
				StringBuilder sqlFilter = new StringBuilder();
				MetrixSqlData dataRow = new MetrixSqlData();
				long rowId = 0;

				dataRow.transactionType = tableDef.transactionType;
				dataRow.tableName = tableDef.tableName;
				MetrixTableStructure tableDefinition = tablesDefinition.get(dataRow.tableName);

				boolean pausedSync = false;
				if (tableDefinition.mForeignKeys != null && tableDefinition.mForeignKeys.size() > 0) {
					for (MetrixColumnDef columnDef : tableDef.columns) {
						if (tableDefinition.isForeignKey(columnDef.columnName)) {
							String value = MetrixControlAssistant.getValue(columnDef, layout);
							if (MetrixStringHelper.isNegativeValue(value)) {
								String currentValue = "";

								// If column has multiple partent tables, it needs to loop through to get the valid foreign key
								if (tableDefinition.mForeignKeys != null && tableDefinition.mForeignKeys.size() > 0) {
									for (MetrixKeys keys : tableDefinition.mForeignKeys.values()) {
										if (keys.keyInfo != null && keys.keyInfo.containsKey(columnDef.columnName)) {
											String parentTable = keys.tableName;

											if (MetrixStringHelper.valueIsEqual(parentTable, "attachment") && MetrixStringHelper.valueIsEqual(keys.keyInfo.get(columnDef.columnName), "attachment_id")) {
												// Use mm_attachment_id_map to get currentValue
												String candidateValue = MetrixDatabaseManager.getFieldStringValue("mm_attachment_id_map", "positive_key", String.format("negative_key = %s", value));
												if (!MetrixStringHelper.isNullOrEmpty(candidateValue)) {
													currentValue = candidateValue;
													break;
												}
											} else {
												currentValue = MetrixCurrentKeysHelper.getKeyValue(parentTable, keys.keyInfo.get(columnDef.columnName));
												if (!MetrixStringHelper.isNullOrEmpty(currentValue))
													break;
											}
										}
									}
								}

								if (value.compareToIgnoreCase(currentValue) != 0 && !MetrixStringHelper.isNullOrEmpty(currentValue)) {
									pauseSync();
									pausedSync = true;
									try {
										MetrixControlAssistant.setValue(columnDef, layout, currentValue);
									} catch (Exception e) {
										LogManager.getInstance().error(e);
									}
								}
							}
						}
					}
				}

				if (tableDef.transactionType == MetrixTransactionTypes.INSERT) {
					for (MetrixColumnDef columnDef : tableDef.columns) {
						DataField field = new DataField();

						String fieldValue = MetrixControlAssistant.getValue(columnDef, layout);

						field.name = columnDef.columnName;
						field.value = fieldValue;

						if (columnDef.dataType == int.class || columnDef.dataType == double.class) {
							field.type = "numeric";
						} else if (columnDef.dataType == String.class) {
							field.type = "varchar";
						}
						dataRow.dataFields.add(field);
					}

					setCreatedAndModifiedColumns(tableDefinition, dataRow.dataFields);
					rowId = MetrixDatabaseManager.insertRow(dataRow.tableName, dataRow.dataFields);
					sqlSuccess = rowId > 0;
				} else if ((tableDef.transactionType == MetrixTransactionTypes.UPDATE) || (tableDef.transactionType == MetrixTransactionTypes.CORRECT_ERROR)) {
					@SuppressWarnings({ "unchecked", "rawtypes" })
					HashMap<String, String> originalValues = (HashMap) MetrixPublicCache.instance.getItem(MetrixManagerConstants.METRIX_LAYOUT_ORIGINAL_VALUES);

					for (MetrixColumnDef columnDef : tableDef.columns) {
						String originalValue = originalValues.get(tableDef.tableName + "." + columnDef.columnName);
						String currentValue = MetrixControlAssistant.getValue(columnDef, layout);

						DataField field = new DataField();

						if (MetrixStringHelper.valueIsEqual(originalValue, currentValue) == false) {
							if (columnDef.dataType == int.class || columnDef.dataType == double.class) {
								field.type = "numeric";
							} else if (columnDef.dataType == String.class) {
								field.type = "varchar";
							}

							field.name = columnDef.columnName;
							field.value = currentValue;

							dataRow.dataFields.add(field);
						}
					}

					setModifiedColumns(tableDefinition, dataRow.dataFields);
				}

				if ((tableDef.transactionType == MetrixTransactionTypes.UPDATE) || (tableDef.transactionType == MetrixTransactionTypes.CORRECT_ERROR)
						|| (tableDef.transactionType == MetrixTransactionTypes.DELETE)) {

					for (MetrixColumnDef columnDef : tableDef.columns) {
						if (columnDef.columnName.compareToIgnoreCase(Global.MetrixRowId) == 0) {
							rowId = Integer.valueOf(MetrixControlAssistant.getValue(columnDef, layout));
							break;
						}
					}

					@SuppressWarnings({ "unchecked", "rawtypes" })
					HashMap<String, String> originalValues = (HashMap) MetrixPublicCache.instance.getItem(MetrixManagerConstants.METRIX_LAYOUT_ORIGINAL_VALUES);

					boolean columnAdded = false;
					for (MetrixColumnDef columnDef : tableDef.columns) {
						String originalValue = originalValues.get(tableDef.tableName + "." + columnDef.columnName);
						if (!MetrixStringHelper.isNullOrEmpty(originalValue)) {
							if (columnDef.dataType != Date.class  && columnDef.dataType != MetrixDate.class && columnDef.dataType != MetrixTime.class && columnDef.dataType != MetrixDateTime.class) {
								if (columnDef.dataType == int.class || columnDef.dataType == double.class) {
									if (columnDef.primaryKey == false) {
										continue;
									}
								}

								if (columnAdded) {
									sqlFilter.append(" and ");
								}

								sqlFilter.append(columnDef.columnName);
								sqlFilter.append("=");

								if (columnDef.dataType == int.class || columnDef.dataType == double.class) {
									sqlFilter.append(originalValue);
								} else if (columnDef.dataType == String.class) {
									sqlFilter.append("'");
									sqlFilter.append(originalValue.replace("'", "''"));
									sqlFilter.append("'");
								}

								columnAdded = true;
							}
						}
					}
					dataRow.filter = sqlFilter.toString();

					if ((tableDef.transactionType == MetrixTransactionTypes.UPDATE) || (tableDef.transactionType == MetrixTransactionTypes.CORRECT_ERROR)) {
						if (dataRow.dataFields.size() == 0) {
							sqlSuccess = true;
						} else {
							sqlSuccess = MetrixDatabaseManager.updateRow(dataRow.tableName, dataRow.dataFields, sqlFilter.toString());
						}
					} else {
						sqlSuccess = MetrixDatabaseManager.deleteRow(dataRow.tableName, sqlFilter.toString());
					}
				}

				if (sqlSuccess == false)
					return false;

				if (writeTransactionLog) {
					boolean writeStatus = writeTransactionLog(tableDef, layout, rowId, index, transactionInfo, transactionFriendlyName, activity,
							waitingPrevious);
					if (writeStatus == false) {
						return writeStatus;
					}
				}

				if (pausedSync) {
					resumeSync();
				}
			}
			index++;
		}

		return true;
	}

	/**
	 * Performs a series of database transactions.
	 *
	 * It changes to deprecated and external caller should use update method
	 * instead of performTransactions to save data
	 *
	 * @param dataList
	 * @param writeTransactionLog
	 * @param transactionInfo
	 * @param transactionFriendlyName
	 * @param activity
	 * @return
	 */
	@Deprecated
	public static boolean performTransactions(ArrayList<MetrixSqlData> dataList, boolean writeTransactionLog, MetrixTransaction transactionInfo,
											  String transactionFriendlyName, Activity activity) {

		if (dataList == null) {
			throw new IllegalArgumentException(AndroidResourceHelper.getMessage("TheDatalistParamIsReq"));
		}

		if (transactionInfo == null) {
			throw new IllegalArgumentException(AndroidResourceHelper.getMessage("TheTransInfoParamReq"));
		}

		return performTransactions(dataList, writeTransactionLog, transactionInfo, transactionFriendlyName, activity, false);
	}

	/**
	 * Method is used for save the Data list into the database and optionally
	 * created the transaction log
	 *
	 * It changes to deprecated and external caller should use update method
	 * instead of performTransactions to save data
	 *
	 * @param dataList
	 * @param writeTransactionLog
	 * @param transactionInfo
	 * @param transactionFriendlyName
	 * @param activity
	 * @param waitingPrevious
	 * @return
	 */
	@Deprecated
	public static boolean performTransactions(ArrayList<MetrixSqlData> dataList, boolean writeTransactionLog, MetrixTransaction transactionInfo,
											  String transactionFriendlyName, Activity activity, boolean waitingPrevious) {

		if (dataList == null) {
			throw new IllegalArgumentException(AndroidResourceHelper.getMessage("TheDatalistParamIsReq"));
		}

		if (transactionInfo == null) {
			throw new IllegalArgumentException(AndroidResourceHelper.getMessage("TheTransInfoParamReq"));
		}

		boolean successful = MetrixDatabaseManager.executeDataFieldArray(dataList, false);

		if (successful && writeTransactionLog) {
			successful = writeTransactionLog(dataList, transactionInfo, transactionFriendlyName, activity, waitingPrevious);
		}
		return successful;
	}

	/**
	 * Performs a database transaction.
	 *
	 * It changes to deprecated and external caller should use update method
	 * instead of performTransactions to save data
	 *
	 * @param data
	 * @param writeTransactionLog
	 * @param transactionInfo
	 * @param transactionFriendlyName
	 * @param activity
	 * @param waitingPrevious
	 * @return
	 */
	@Deprecated
	public static boolean performTransaction(MetrixSqlData data, boolean writeTransactionLog, MetrixTransaction transactionInfo,
											 String transactionFriendlyName, Activity activity, boolean waitingPrevious) {
		if (data == null) {
			throw new IllegalArgumentException(AndroidResourceHelper.getMessage("TheDataParamIsReq"));
		}

		if (transactionInfo == null) {
			throw new IllegalArgumentException(AndroidResourceHelper.getMessage("TheTransInfoParamReq"));
		}

		ArrayList<MetrixSqlData> dataList = new ArrayList<MetrixSqlData>();
		dataList.add(data);
		return MetrixUpdateManager.performTransactions(dataList, writeTransactionLog, transactionInfo, transactionFriendlyName, activity, waitingPrevious);
	}

	/**
	 * Performs a database transaction.
	 *
	 * It changes to deprecated and external caller should use update method
	 * instead of performTransactions to save data
	 *
	 * @param data
	 * @param writeTransactionLog
	 * @param transactionInfo
	 * @param transactionFriendlyName
	 * @param activity
	 * @return
	 */
	@Deprecated
	public static boolean performTransaction(MetrixSqlData data, boolean writeTransactionLog, MetrixTransaction transactionInfo,
											 String transactionFriendlyName, Activity activity) {
        if (data == null) {
            throw new IllegalArgumentException(AndroidResourceHelper.getMessage("TheDataParamIsReq"));
        }

        if (transactionInfo == null) {
            throw new IllegalArgumentException(AndroidResourceHelper.getMessage("TheTransInfoParamReq"));
        }

		return performTransaction(data, writeTransactionLog, transactionInfo, transactionFriendlyName, activity, false);
	}

	/**
	 * Build and execute sql statements to write the transaction logs and
	 * message_out.
	 *
	 * @param tableDef
	 *            the table being updated.
	 * @param layout
	 *            the current layout.
	 * @return an array list of sql statements for transaction logging.
	 */
	private static boolean writeTransactionLog(MetrixTableDef tableDef, ViewGroup layout, long rowId, int index, MetrixTransaction transactionInfo,
											   String transactionFriendlyName, Activity activity, boolean waitingPrevious) {
		ArrayList<DataField> logFields = new ArrayList<DataField>();
		String sentStatus = waitingPrevious == true ? MessageStatus.WAITING_PREVIOUS.toString() : MessageStatus.READY.toString();

		StringBuilder logStatement = new StringBuilder();
		HashMap<String, MetrixTableStructure> tablesDefinition = MobileApplication.getTableDefinitionsFromCache();
		MetrixTableStructure tableDefinition = tablesDefinition.get(tableDef.tableName);

		if (tableDef.transactionType == MetrixTransactionTypes.INSERT || tableDef.transactionType == MetrixTransactionTypes.DELETE) {

			DataField rowField = new DataField();
			rowField.name = Global.MetrixRowId;
			rowField.type = "numeric";
			rowField.value = "" + rowId;
			logFields.add(rowField);

			for (MetrixColumnDef columnDef : tableDef.columns) {
				String fieldValue = MetrixControlAssistant.getValue(columnDef, layout);

				boolean foreignKey = false;

				DataField field = new DataField();
				field.name = columnDef.columnName;

				if (tableDefinition.mForeignKeys != null) {
					if (tableDefinition.isForeignKey(columnDef.columnName)) {
						foreignKey = true;
					}
				}

				if (foreignKey) {
					field.value = fieldValue;

					if (waitingPrevious == false) {
						if (MetrixStringHelper.isNegativeValue(field.value)) {
							sentStatus = MessageStatus.WAITING.toString();
						}
					}
				} else {
					field.value = fieldValue;
				}

				if (tableDef.transactionType == MetrixTransactionTypes.DELETE) {
					if (columnDef.primaryKey) {
						if (MetrixStringHelper.isNegativeValue(fieldValue)) {
							sentStatus = MessageStatus.WAITING.toString();
						}
					}
				}

				field.tableName = tableDef.tableName;
				if (columnDef.dataType == int.class || columnDef.dataType == double.class) {
					field.type = "numeric";
				} else if (columnDef.dataType == Date.class || columnDef.dataType == MetrixDate.class || columnDef.dataType == MetrixTime.class || columnDef.dataType == MetrixDateTime.class) {
					field.type = "varchar";
				} else {
					field.type = "varchar";
				}

				logFields.add(field);
			}
			setCreatedAndModifiedColumns(tableDefinition, logFields);
		} else if (tableDef.transactionType == MetrixTransactionTypes.UPDATE) {
			@SuppressWarnings({ "unchecked", "rawtypes" })
			HashMap<String, String> originalValues = (HashMap) MetrixPublicCache.instance.getItem(MetrixManagerConstants.METRIX_LAYOUT_ORIGINAL_VALUES);

			Context appContext = (Context) MetrixPublicCache.instance.getItem(Global.MobileApplication);
			boolean isIfs = MetrixApplicationAssistant.getMetaStringValue(appContext, "ClientOwner").compareToIgnoreCase("IFS") == 0;
			HashMap<MetrixColumnDef, String> columnsToAdd = new HashMap<MetrixColumnDef, String>();

			for (MetrixColumnDef columnDef : tableDef.columns) {
				String originalValue = originalValues.get(tableDef.tableName + "." + columnDef.columnName);
				String currentValue = MetrixControlAssistant.getValue(columnDef, layout);

				boolean foreignKey = false;

				if (tableDefinition.mForeignKeys != null) {
					if (tableDefinition.isForeignKey(columnDef.columnName)) {
						foreignKey = true;
					}
				}

				if (foreignKey) {
					if (waitingPrevious == false) {
						if (MetrixStringHelper.isNegativeValue(currentValue)) {
							sentStatus = MessageStatus.WAITING.toString();
						}
					}
				}

				boolean ifsSysCol = isIfs && ("obj_id".equalsIgnoreCase(columnDef.columnName) || "obj_version".equalsIgnoreCase(columnDef.columnName));

				if (columnDef.primaryKey) {
					if (MetrixStringHelper.isNegativeValue(currentValue)) {
						sentStatus = MessageStatus.WAITING.toString();
					}
					columnsToAdd.put(columnDef, originalValue);
				} else {
					if (MetrixStringHelper.valueIsEqual(originalValue, currentValue) == false || ifsSysCol) {
						if (MetrixStringHelper.isNullOrEmpty(currentValue)) {
							columnsToAdd.put(columnDef, "=");
						} else {
							columnsToAdd.put(columnDef, currentValue);
						}
					}
				}
			}

			DataField rowField = new DataField();
			rowField.name = Global.MetrixRowId;
			rowField.type = "numeric";
			rowField.value = "" + rowId;

			logFields.add(rowField);

			for (Entry<MetrixColumnDef, String> entry : columnsToAdd.entrySet()) {
				logStatement.append(", ");

				DataField field = new DataField();

				MetrixColumnDef columnDef = entry.getKey();
				String value = entry.getValue();

				field.name = columnDef.columnName;

				if (columnDef.dataType == int.class || columnDef.dataType == double.class) {
					field.type = "numeric";
					field.value = value;
				} else if (columnDef.dataType == Date.class || columnDef.dataType == MetrixDate.class || columnDef.dataType == MetrixTime.class || columnDef.dataType == MetrixDateTime.class) {
					field.type = "varchar";
					field.value = value;
				} else {
					field.type = "varchar";
					field.value = value;
				}

				logFields.add(field);
			}
			setModifiedColumns(tableDefinition, logFields);
		} else if (tableDef.transactionType == MetrixTransactionTypes.CORRECT_ERROR) {
			@SuppressWarnings({ "unchecked", "rawtypes" })
			HashMap<String, String> originalValues = (HashMap) MetrixPublicCache.instance.getItem(MetrixManagerConstants.METRIX_LAYOUT_ORIGINAL_VALUES);

			MetrixSqlData dataRow = new MetrixSqlData();
			dataRow.tableName = tableDef.tableName;

			for (MetrixColumnDef columnDef : tableDef.columns) {
				String originalValue = originalValues.get(tableDef.tableName + "." + columnDef.columnName);
				String currentValue = MetrixControlAssistant.getValue(columnDef, layout);
				DataField field = new DataField();

				if (MetrixStringHelper.valueIsEqual(originalValue, currentValue) == false) {
					field.name = columnDef.columnName;

					if (columnDef.dataType == int.class || columnDef.dataType == double.class) {
						field.type = "numeric";
						field.value = MetrixControlAssistant.getValue(columnDef, layout);
					} else if (columnDef.dataType == String.class) {
						field.type = "varchar";
						field.value = MetrixControlAssistant.getValue(columnDef, layout);
					} else if (columnDef.dataType == Date.class || columnDef.dataType == MetrixDate.class || columnDef.dataType == MetrixTime.class || columnDef.dataType == MetrixDateTime.class) {
						field.type = "varchar";
						field.value = MetrixControlAssistant.getValue(columnDef, layout);
					}

					dataRow.dataFields.add(field);
				}
			}

			String logId = activity.getIntent().getExtras().getString("HandleErrorLogId");

			boolean sqlSuccess = MetrixDatabaseManager.updateRow(dataRow.tableName + "_log", dataRow.dataFields, "metrix_log_id=" + logId);

			if (!sqlSuccess) {
				return false;
			}
		}

		if (tableDef.transactionType == MetrixTransactionTypes.CORRECT_ERROR) {
			logStatement = new StringBuilder();
			logStatement.append("update mm_message_out set status = 'READY' where message_id=");
			logStatement.append(activity.getIntent().getExtras().getString("HandleErrorMessageId"));

			if (!MetrixDatabaseManager.executeSql(logStatement.toString())) {
				return false;
			}

			logStatement = new StringBuilder();
			logStatement.append("delete from mm_message_in where related_message_id=");
			logStatement.append(activity.getIntent().getExtras().getString("HandleErrorMessageId"));

			return MetrixDatabaseManager.executeSql(logStatement.toString());
		} else {
			int currentCodelessScreenId = -1;
			long logId = 0;
			logId = MetrixDatabaseManager.insertRow(tableDef.tableName + "_log", logFields);
			if (logId <= 0) {
				return false;
			}

			if (activity != null) {
				if(activity instanceof MetrixBaseActivity)
				{
					MetrixBaseActivity metrixBaseActivity = (MetrixBaseActivity)activity;
					if(metrixBaseActivity.isCodelessScreen){
						//codeless list screen in tablet UI landscape mode - so we should get associate standard screen id
						if(metrixBaseActivity.isCodelessListScreenInTabletUIMode){
							currentCodelessScreenId = metrixBaseActivity.linkedScreenIdInTabletUIMode;
						}
						else
							currentCodelessScreenId = metrixBaseActivity.codeLessScreenId;
					}
				}
			}

			logStatement = new StringBuilder();
			logStatement
					.append("insert into mm_message_out (person_id, transaction_type, transaction_id, transaction_key_id, transaction_key_name, status, transaction_desc, activity_name, table_name, ");
			if(currentCodelessScreenId > -1)
				logStatement.append("screen_id, ");
			logStatement.append(Global.MetrixLogId);
			logStatement.append(", created_dttm, modified_dttm) values (");
			logStatement.append("'");
			logStatement.append(User.getUser().personId);
			logStatement.append("', '");
			logStatement.append(tableDef.transactionType);
			logStatement.append("', ");
			logStatement.append(transactionInfo.mTransaction_id);
			logStatement.append(", '" + transactionInfo.transactionKeyId + "'");
			logStatement.append(", '" + transactionInfo.transactionKeyName + "'");
			logStatement.append(", '" + sentStatus + "'");
			logStatement.append(", '" + transactionFriendlyName + "'");

			if (activity != null) {
				logStatement.append(", '" + activity.getClass().getName() + "'");			} else {
				logStatement.append(", ''");
			}

			logStatement.append(", '" + tableDef.tableName);
			logStatement.append("', ");

			if(currentCodelessScreenId > -1){
				logStatement.append(currentCodelessScreenId);
				logStatement.append(", ");
			}

			logStatement.append(logId);
			logStatement.append(", '");
			logStatement.append(MetrixDateTimeHelper.getCurrentDate(MetrixDateTimeHelper.DATE_TIME_FORMAT_WITH_SECONDS, true));
			logStatement.append("', '");
			logStatement.append(MetrixDateTimeHelper.getCurrentDate(MetrixDateTimeHelper.DATE_TIME_FORMAT_WITH_SECONDS, true));
			logStatement.append("')");

			return MetrixDatabaseManager.executeSql(logStatement.toString());
		}
	}

	private static void setModifiedColumns(MetrixTableStructure tableDefinition, ArrayList<DataField> logFields) {
		if (tableDefinition.containsModifiedColumns()) {
			boolean foundModifiedBy = false;
			boolean foundModifiedDttm = false;

			for (DataField field : logFields) {
				if (field.name.compareToIgnoreCase("modified_by") == 0) {
					foundModifiedBy = true;
				}
				if (field.name.compareToIgnoreCase("modified_dttm") == 0) {
					foundModifiedDttm = true;
				}
				if (foundModifiedBy && foundModifiedDttm) {
					break;
				}
			}

			if (!foundModifiedBy) {
				DataField field = new DataField();
				field.name = "modified_by";
				field.tableName = tableDefinition.mTableName;
				field.value = User.getUser().personId;
				field.type = "varchar";
				logFields.add(field);
			}

			if (!foundModifiedDttm) {
				DataField field = new DataField();
				field.name = "modified_dttm";
				field.tableName = tableDefinition.mTableName;
				field.value = MetrixDateTimeHelper.getCurrentDate(MetrixDateTimeHelper.DATE_TIME_FORMAT, true);
				field.type = "varchar";
				logFields.add(field);
			}
		}
	}

	private static void setCreatedAndModifiedColumns(MetrixTableStructure tableDefinition, ArrayList<DataField> logFields) {
		if (tableDefinition.containsCreatedColumns()) {
			boolean foundCreatedBy = false;
			boolean foundCreatedDttm = false;

			for (DataField field: logFields) {
				if (field.name.compareToIgnoreCase("created_by") == 0) {
					foundCreatedBy = true;
				}
				if (field.name.compareToIgnoreCase("created_dttm") == 0) {
					foundCreatedDttm = true;
				}
				if (foundCreatedBy && foundCreatedDttm) {
					break;
				}
			}

			if (!foundCreatedBy) {
				DataField field = new DataField();
				field.name = "created_by";
				field.tableName = tableDefinition.mTableName;
				field.value = User.getUser().personId;
				field.type = "varchar";
				logFields.add(field);
			}

			if (!foundCreatedDttm) {
				DataField field = new DataField();
				field.name = "created_dttm";
				field.tableName = tableDefinition.mTableName;
				field.value = MetrixDateTimeHelper.getCurrentDate(MetrixDateTimeHelper.DATE_TIME_FORMAT, true);
				field.type = "varchar";
				logFields.add(field);
			}
		}

		setModifiedColumns(tableDefinition, logFields);
	}

	/**
	 * Build and execute sql statements to write the transaction logs and
	 * message_out.
	 *
	 * @param dataList the details of the transactions which were
	 *            performed.
	 */
	private static boolean writeTransactionLog(ArrayList<MetrixSqlData> dataList, MetrixTransaction transactionInfo, String transactionFriendlyName,
											   Activity activity, boolean waitingPrevious) {
		StringBuilder logStatement = null;
		boolean insertLogSuccess = false;
		String currentListActivityFullName = null;

		for (MetrixSqlData dataRow : dataList) {
			String sentStatus = waitingPrevious == true ? MessageStatus.WAITING_PREVIOUS.toString() : MessageStatus.READY.toString();

			ArrayList<DataField> logFields = new ArrayList<DataField>();

			if (dataRow.transactionType == MetrixTransactionTypes.INSERT) {
				DataField rowField = new DataField();
				rowField.name = Global.MetrixRowId;
				rowField.type = "numeric";
				rowField.value = "" + dataRow.rowId;
				logFields.add(rowField);

				HashMap<String, MetrixTableStructure> tablesDefinition = MobileApplication.getTableDefinitionsFromCache();
				MetrixTableStructure tableDefinition = tablesDefinition.get(dataRow.tableName);

				for (DataField columnDef : dataRow.dataFields) {
					boolean foreignKey = false;
					if (tableDefinition.mForeignKeys != null) {
						if (tableDefinition.isForeignKey(columnDef.name)) {
							// the primary key is negative value and it is
							// foreign key of the other table needs to be
							// waiting status
							foreignKey = true;
						}
					}

					DataField field = new DataField();
					field.name = columnDef.name;
					if (foreignKey) {
						field.value = columnDef.value;

						if (waitingPrevious == false) {
							if (MetrixStringHelper.isNegativeValue(field.value)) {
								sentStatus = MessageStatus.WAITING.toString();
							}
						}
					} else {
						field.value = columnDef.value;
					}
					field.tableName = columnDef.tableName;
					field.type = columnDef.type;
					logFields.add(field);
				}
			} else {
				HashMap<String, MetrixTableStructure> tablesDefinition = MobileApplication.getTableDefinitionsFromCache();
				MetrixTableStructure tableDefinition = tablesDefinition.get(dataRow.tableName);

				for (DataField columnDef : dataRow.dataFields) {
					boolean primaryKey = false;
					boolean foreignKey = false;

					if (tableDefinition.mForeignKeys != null) {
						if (tableDefinition.isForeignKey(columnDef.name)) {
							// the primary key is negative value and it is
							// foreign key of the other table needs to be
							// waiting status
							foreignKey = true;
						}
					}

					if (tableDefinition.mMetrixPrimaryKeys != null) {
						if (tableDefinition.isPrimaryKey(columnDef.name)) {
							// the primary key is negative value and it is
							// for updating or deleting, then it needs to be in
							// waiting queue
							primaryKey = true;
						}
					}

					DataField field = new DataField();
					field.name = columnDef.name;
					if (primaryKey || foreignKey) {
						field.name = columnDef.name;
						field.value = columnDef.value;
						field.tableName = columnDef.tableName;
						field.type = columnDef.type;

						if (waitingPrevious == false) {
							if (MetrixStringHelper.isNegativeValue(field.value)) {
								sentStatus = MessageStatus.WAITING.toString();
							}
						}
					} else {
						field.name = columnDef.name;
						field.value = columnDef.value;
						field.tableName = columnDef.tableName;
						field.type = columnDef.type;

						if (MetrixStringHelper.isNullOrEmpty(columnDef.value)) {
							field.value = "=";
						}
					}

					logFields.add(field);
				}
			}
			int currentCodelessScreenId = -1;
			long logId = MetrixDatabaseManager.insertRow(dataRow.tableName + "_log", logFields);

			if (logId <= 0) {
				return false;
			}

			if (activity != null) {
				if(activity instanceof MetrixBaseActivity)
				{
					MetrixBaseActivity metrixBaseActivity = (MetrixBaseActivity)activity;
					currentListActivityFullName = metrixBaseActivity.listActivityFullNameInTabletUIMode;
					if(metrixBaseActivity.isCodelessScreen){
						//codeless list screen in tablet UI landscape mode - so we should get associate standard screen id
						if(metrixBaseActivity.isCodelessListScreenInTabletUIMode)
							currentCodelessScreenId = metrixBaseActivity.linkedScreenIdInTabletUIMode;
						else
							currentCodelessScreenId = metrixBaseActivity.codeLessScreenId;
					}
				}
			}

			logStatement = new StringBuilder();
			logStatement
					.append("insert into mm_message_out (person_id, transaction_type, transaction_id, transaction_key_id, transaction_key_name, status, transaction_desc, activity_name, table_name, ");
			if(currentCodelessScreenId > -1)
				logStatement.append("screen_id, ");
			logStatement.append(Global.MetrixLogId);
			logStatement.append(", created_dttm, modified_dttm) values (");
			logStatement.append("'");
			logStatement.append(User.getUser().personId);
			logStatement.append("', '");
			logStatement.append(dataRow.transactionType);
			logStatement.append("', ");
			logStatement.append(transactionInfo.mTransaction_id); // give a
			// default
			// transaction_id
			logStatement.append(", '" + transactionInfo.transactionKeyId + "'");
			logStatement.append(", '" + transactionInfo.transactionKeyName + "'");
			logStatement.append(", '" + sentStatus + "'");
			logStatement.append(", '" + transactionFriendlyName + "'");

			if(!MetrixStringHelper.isNullOrEmpty(currentListActivityFullName))
				logStatement.append(", '" + currentListActivityFullName + "'");
			else{
				if (activity != null) {
					logStatement.append(", '" + activity.getClass().getName() + "'");				} else {
					logStatement.append(", ''");
				}
			}

			logStatement.append(", '");
			logStatement.append(dataRow.tableName);
			logStatement.append("', ");

			if(currentCodelessScreenId > -1){
				logStatement.append(currentCodelessScreenId);
				logStatement.append(", ");
			}

			logStatement.append(logId);
			logStatement.append(", '");
			logStatement.append(MetrixDateTimeHelper.getCurrentDate(MetrixDateTimeHelper.DATE_TIME_FORMAT_WITH_SECONDS, true));
			logStatement.append("', '");
			logStatement.append(MetrixDateTimeHelper.getCurrentDate(MetrixDateTimeHelper.DATE_TIME_FORMAT_WITH_SECONDS, true));
			logStatement.append("')");

			if(dataRow.tableName.compareToIgnoreCase("attachment")==0){
				// add a separate mm_message_out for binary file to be sent out
				// add attachment field to indicate it is Binary data in mm_message_out for different process
				String attachmentBinaryLog = logStatement.toString().replace("mm_message_out (person_id", "mm_message_out (attachment, person_id")
						.replace("modified_dttm) values (", "modified_dttm) values ('BINARY', ");

				insertLogSuccess = MetrixDatabaseManager.executeSql(attachmentBinaryLog);
			}
			else {
				insertLogSuccess = MetrixDatabaseManager.executeSql(logStatement.toString());
			}

			if (insertLogSuccess == false)
				return insertLogSuccess;
		}

		return insertLogSuccess;
	}

	/**
	 * @param keyId
	 * @param keyName
	 * @return
	 */
	public static int getTransactionId(String keyId, String keyName) {

		if (MetrixStringHelper.getString(MetrixPublicCache.instance.getItem("SYNC_TYPE")).compareToIgnoreCase(UploadType.TRANSACTION_INDEPENDENT.toString()) != 0) {
			return 0;
		}

		if (MetrixStringHelper.isNullOrEmpty(keyId) || MetrixStringHelper.isNullOrEmpty(keyName)) {
			return MetrixDatabaseManager.generateTransactionId("mm_message_out");
		}

		String transactionId = MetrixDatabaseManager.getFieldStringValue("mm_message_out", "transaction_id", "transaction_key_id='" + keyId
				+ "' and transaction_key_name='" + keyName + "'");

		if (MetrixStringHelper.isInteger(transactionId)) {
			return Integer.parseInt(transactionId);
		} else {
			return MetrixDatabaseManager.generateTransactionId("mm_message_out");
		}
	}

	/**
	 * Temporarily disables sync processing on the device. This should almost
	 * never be used!
	 */
	public static void pauseSync() {
		MetrixPublicCache.instance.removeItem("EnableSyncProcess");
		MetrixPublicCache.instance.addItem("EnableSyncProcess", false);
	}

	/**
	 * Resumes temporarily paused sync processing. This should almost never be
	 * used!
	 */
	public static void resumeSync() {
		if (Boolean.parseBoolean(MetrixStringHelper.getString(MetrixPublicCache.instance.getItem("EnableSyncProcess"))) == false) {
			MetrixPublicCache.instance.removeItem("EnableSyncProcess");
			MetrixPublicCache.instance.addItem("EnableSyncProcess", true);
		}
	}
}

