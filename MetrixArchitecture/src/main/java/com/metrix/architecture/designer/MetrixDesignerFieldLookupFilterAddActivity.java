package com.metrix.architecture.designer;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.metrix.architecture.assistants.MetrixControlAssistant;
import com.metrix.architecture.assistants.MetrixDialogAssistant;
import com.metrix.architecture.database.MetrixDatabaseManager;
import com.metrix.architecture.database.MobileApplication;
import com.metrix.architecture.metadata.MetrixPerformMessage;
import com.metrix.architecture.services.IPostMonitor;
import com.metrix.architecture.services.MetrixIntentService;
import com.metrix.architecture.services.MetrixRemoteExecutor;
import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.architecture.utilities.Global.ActivityType;
import com.metrix.architecture.utilities.LogManager;
import com.metrix.architecture.utilities.MetrixActivityHelper;
import com.metrix.architecture.utilities.MetrixCurrentKeysHelper;
import com.metrix.architecture.utilities.MetrixFloatHelper;
import com.metrix.architecture.utilities.MetrixPublicCache;
import com.metrix.architecture.utilities.MetrixStringHelper;
import com.metrix.architecture.utilities.MetrixUIHelper;
import com.metrix.architecture.utilities.SettingsHelper;
import com.metrix.architecture.utilities.SyncPauseAlertDialog;

import java.util.ArrayList;
import java.util.Hashtable;

public class MetrixDesignerFieldLookupFilterAddActivity extends MetrixDesignerActivity {
	private Button mSaveButton;
	private static CheckBox mNoQuotes;
	private static Spinner mLogicalOperator, mTable, mColumn, mOperator;
	private static EditText mLeftParens, mRightOperand, mRightParens;
	private ImageView mRightOperandButton;
	private TextView mEmphasis, mRightOperandLabel, mRightOperandDescLabel, mRightOperandDesc, mNoQuotesLabel;
	private AlertDialog mAddLookupFilterDialog;
	private String mScreenName, mFieldName;
	protected MetrixUIHelper mUIHelper = new MetrixUIHelper(this);
	private MetrixDesignerResourceData mLookupFilterAddResourceData;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mLookupFilterAddResourceData = (MetrixDesignerResourceData) MetrixPublicCache.instance.getItem("MetrixDesignerFieldLookupFilterAddActivityResourceData");

		setContentView(mLookupFilterAddResourceData.LayoutResourceID);
		populateScreen();
	}

	@Override
	public void onStart() {
		super.onStart();

		helpText = mLookupFilterAddResourceData.HelpTextString;

		mHeadingText = getIntent().getStringExtra("headingText");
		mScreenName = MetrixCurrentKeysHelper.getKeyValue("mm_screen", "screen_name");
		mFieldName = MetrixCurrentKeysHelper.getKeyValue("mm_field", "field_name");
		if (mActionBarTitle != null) {
			mActionBarTitle.setText(mHeadingText);
		}

		mEmphasis = (TextView) findViewById(mLookupFilterAddResourceData.getExtraResourceID("R.id.zzmd_field_lookup_filter_add_emphasis"));
		String fullText = AndroidResourceHelper.getMessage("ScnInfoMxDesFldLkupFltrAdd", mFieldName, mScreenName);
		mEmphasis.setText(fullText);

		mSaveButton = (Button) findViewById(mLookupFilterAddResourceData.getExtraResourceID("R.id.save"));
		mSaveButton.setOnClickListener(this);


		AndroidResourceHelper.setResourceValues(mSaveButton, "Save");
	}

	@Override
	protected void bindService() {
		bindService(new Intent(MetrixDesignerFieldLookupFilterAddActivity.this, MetrixIntentService.class), mConnection, Context.BIND_AUTO_CREATE);
		mIsBound = true;
	}

	@Override
	protected void unbindService() {
		if (mIsBound) {
			try {
				if (service != null) {
					service.removeListener(listener);
					unbindService(mConnection);
				}
			} catch (Exception ex) {
				LogManager.getInstance().error(ex);
			} finally {
				mIsBound = false;
			}
		}
	}

	private ServiceConnection mConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder binder) {
			try {
				service = (IPostMonitor) binder;
				service.registerListener(listener);
			} catch (Throwable t) {
				LogManager.getInstance().error(t);
			}
		}

		public void onServiceDisconnected(ComponentName className) {
			service = null;
		}
	};

	protected com.metrix.architecture.services.IPostListener listener = new com.metrix.architecture.services.IPostListener() {
		public void newSyncStatus(final ActivityType activityType, final String message) {
			runOnUiThread(new Runnable() {
				public void run() {
					if (MetrixStringHelper.valueIsEqual(message, "{\"END_GMFLF\":null}")) {
						MobileApplication.stopSync(mCurrentActivity);
						MobileApplication.startSync(mCurrentActivity);
						mUIHelper.dismissLoadingDialog();
						Intent intent = MetrixActivityHelper.createActivityIntent(MetrixDesignerFieldLookupFilterAddActivity.this, MetrixDesignerFieldLookupFilterActivity.class);
						intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
						// re-getting this data, as the intent has the bad habit of using previous extras
						String designName = MetrixDatabaseManager.getFieldStringValue("mm_design", "name", "design_id = " + MetrixCurrentKeysHelper.getKeyValue("mm_design", "design_id"));
						String revNumber = MetrixDatabaseManager.getFieldStringValue("mm_revision", "revision_number", "revision_id = " + MetrixCurrentKeysHelper.getKeyValue("mm_revision", "revision_id"));
						intent.putExtra("headingText", String.format("%1$s (%2$s %3$s)", designName, AndroidResourceHelper.getMessage("Rev"), revNumber));
						MetrixActivityHelper.startNewActivity(MetrixDesignerFieldLookupFilterAddActivity.this, intent);
					} else {
						processPostListener(activityType, message);
					}
				}
			});
		}
	};

	private void populateScreen() {
		mLogicalOperator = (Spinner) findViewById(mLookupFilterAddResourceData.getExtraResourceID("R.id.logical_operator"));
		mLeftParens = (EditText) findViewById(mLookupFilterAddResourceData.getExtraResourceID("R.id.left_parens"));
		mTable = (Spinner) findViewById(mLookupFilterAddResourceData.getExtraResourceID("R.id.table"));
		mColumn = (Spinner) findViewById(mLookupFilterAddResourceData.getExtraResourceID("R.id.column"));
		mOperator = (Spinner) findViewById(mLookupFilterAddResourceData.getExtraResourceID("R.id.operator"));
		mRightOperandLabel = (TextView) findViewById(mLookupFilterAddResourceData.getExtraResourceID("R.id.right_operand_label"));
		mRightOperand = (EditText) findViewById(mLookupFilterAddResourceData.getExtraResourceID("R.id.right_operand"));
		mRightOperandButton = (ImageView) findViewById(mLookupFilterAddResourceData.getExtraResourceID("R.id.right_operand_button"));
		mRightOperandDescLabel = (TextView) findViewById(mLookupFilterAddResourceData.getExtraResourceID("R.id.right_operand_description_label"));
		mRightOperandDesc = (TextView) findViewById(mLookupFilterAddResourceData.getExtraResourceID("R.id.right_operand_description"));
		mNoQuotesLabel = (TextView) findViewById(mLookupFilterAddResourceData.getExtraResourceID("R.id.no_quotes_label"));
		mNoQuotes = (CheckBox) findViewById(mLookupFilterAddResourceData.getExtraResourceID("R.id.no_quotes"));
		mRightParens = (EditText) findViewById(mLookupFilterAddResourceData.getExtraResourceID("R.id.right_parens"));

		TextView mAddLkupFilter = (TextView) findViewById(mLookupFilterAddResourceData.getExtraResourceID("R.id.add_lookup_filter"));
		TextView mLogicalOperatorLbl = (TextView) findViewById(mLookupFilterAddResourceData.getExtraResourceID("R.id.logical_operator_lbl"));
		TextView mLeftParensLbl = (TextView) findViewById(mLookupFilterAddResourceData.getExtraResourceID("R.id.left_parens_lbl"));
		TextView mTableLbl = (TextView) findViewById(mLookupFilterAddResourceData.getExtraResourceID("R.id.table_lbl"));
		TextView mColumnLbl = (TextView) findViewById(mLookupFilterAddResourceData.getExtraResourceID("R.id.column_lbl"));
		TextView mOperatorLbl = (TextView) findViewById(mLookupFilterAddResourceData.getExtraResourceID("R.id.operator_lbl"));
		TextView mRightParensLbl = (TextView) findViewById(mLookupFilterAddResourceData.getExtraResourceID("R.id.right_parens_lbl"));


		AndroidResourceHelper.setResourceValues(mAddLkupFilter, "AddLookupFilter");
		AndroidResourceHelper.setResourceValues(mLogicalOperatorLbl, "LogicalOperator");
		AndroidResourceHelper.setResourceValues(mLeftParensLbl, "LeftParens");
		AndroidResourceHelper.setResourceValues(mTableLbl, "Table");
		AndroidResourceHelper.setResourceValues(mColumnLbl, "Column");
		AndroidResourceHelper.setResourceValues(mOperatorLbl, "Operator");
		AndroidResourceHelper.setResourceValues(mRightOperandLabel, "RightOperand");
		AndroidResourceHelper.setResourceValues(mNoQuotesLabel, "NoQuotes");
		AndroidResourceHelper.setResourceValues(mRightParensLbl, "RightParens");

		// add Required hint to Right Operand
		mRightOperand.setHint(AndroidResourceHelper.getMessage("Required"));

		StringBuilder logOperatorSpnQuery = new StringBuilder();
		logOperatorSpnQuery.append("select mm_message_def_view.message_text, metrix_code_table.code_value from metrix_code_table ");
		logOperatorSpnQuery.append("join mm_message_def_view on metrix_code_table.message_id = mm_message_def_view.message_id and mm_message_def_view.message_type = 'CODE' ");
		logOperatorSpnQuery.append("where metrix_code_table.code_name = 'MM_FIELD_LKUP_FILTER_LOGICAL_OPERATOR' ");
		logOperatorSpnQuery.append("order by mm_message_def_view.message_text asc");
		MetrixControlAssistant.populateSpinnerFromQuery(this, mLogicalOperator, logOperatorSpnQuery.toString(), true);

		// populate Table spinner with any tables that already exist on the current lookup, and make Column spinner dependent on this choice
		String lkupID = MetrixCurrentKeysHelper.getKeyValue("mm_field_lkup", "lkup_id");
		String tableQuery = "select distinct table_name from mm_field_lkup_table where lkup_id = " + lkupID;
		MetrixControlAssistant.populateSpinnerFromQuery(this, mTable, tableQuery, true);
		mTable.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				setColumnSpinnerBasedOnSelectedTable();
			}

			public void onNothingSelected(AdapterView<?> parent) {
				setColumnSpinnerBasedOnSelectedTable();
			}
		});

		StringBuilder operatorSpnQuery = new StringBuilder();
		operatorSpnQuery.append("select mm_message_def_view.message_text, metrix_code_table.code_value from metrix_code_table ");
		operatorSpnQuery.append("join mm_message_def_view on metrix_code_table.message_id = mm_message_def_view.message_id and mm_message_def_view.message_type = 'CODE' ");
		operatorSpnQuery.append("where metrix_code_table.code_name = 'MM_FIELD_LKUP_FILTER_OPERATOR' ");
		operatorSpnQuery.append("order by mm_message_def_view.message_text asc");
		MetrixControlAssistant.populateSpinnerFromQuery(this, mOperator, operatorSpnQuery.toString(), true);
		mOperator.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				setRightOperandVisibilityBasedOnSelectedOperator();
			}

			public void onNothingSelected(AdapterView<?> parent) {
				setRightOperandVisibilityBasedOnSelectedOperator();
			}
		});

		mRightOperandDescLabel.setVisibility(View.GONE);
		mRightOperandDesc.setVisibility(View.GONE);
		mRightOperand.addTextChangedListener(new ClientScriptOrLiteralTextWatcher(mRightOperandDescLabel, mRightOperandDesc));
		mRightOperandButton.setOnClickListener(this);
	}

	@Override
	public void onClick(View v) {
		super.onClick(v);

		int viewId = v.getId();
		if (viewId == mLookupFilterAddResourceData.getExtraResourceID("R.id.save")) {
			try {
				// Make sure all relevant fields have a value before allowing save
				String table = "";
				String column = "";
				String operator = "";

				if (mTable.getSelectedItem() != null)
					table = MetrixControlAssistant.getValue(mTable);
				if (mColumn.getSelectedItem() != null)
					column = MetrixControlAssistant.getValue(mColumn);
				if (mOperator.getSelectedItem() != null)
					operator = MetrixControlAssistant.getValue(mOperator);

				boolean allNecessaryFieldsPopulated = !MetrixStringHelper.isNullOrEmpty(table) && !MetrixStringHelper.isNullOrEmpty(column) && !MetrixStringHelper.isNullOrEmpty(operator);
				if (allNecessaryFieldsPopulated && !MetrixStringHelper.valueIsEqual(operator, "IS_NULL") && !MetrixStringHelper.valueIsEqual(operator, "IS_NOT_NULL")) {
					String rightOperand = mRightOperand.getText().toString();
					allNecessaryFieldsPopulated = allNecessaryFieldsPopulated && !MetrixStringHelper.isNullOrEmpty(rightOperand);
				}

				if (!allNecessaryFieldsPopulated) {
					Toast.makeText(this, AndroidResourceHelper.getMessage("AddLookupFilterError"), Toast.LENGTH_LONG).show();
					return;
				}

				String leftParens = MetrixControlAssistant.getValue(mLeftParens);
				boolean leftParensIsValid = true;
				if (!MetrixStringHelper.isNullOrEmpty(leftParens)) {
					int leftParensCount = MetrixFloatHelper.convertNumericFromUIToNumber(leftParens).intValue();
					leftParensIsValid = leftParensCount >= 0 && !leftParens.contains(MetrixFloatHelper.getDecimalSeparator());
				}

				String rightParens = MetrixControlAssistant.getValue(mRightParens);
				boolean rightParensIsValid = true;
				if (!MetrixStringHelper.isNullOrEmpty(rightParens)) {
					int rightParensCount = MetrixFloatHelper.convertNumericFromUIToNumber(rightParens).intValue();
					rightParensIsValid = rightParensCount >= 0 && !rightParens.contains(MetrixFloatHelper.getDecimalSeparator());
				}

				if (!leftParensIsValid || !rightParensIsValid) {
					Toast.makeText(this, AndroidResourceHelper.getMessage("LookupFilterParensError"), Toast.LENGTH_LONG).show();
					return;
				}

				mAddLookupFilterDialog = new AlertDialog.Builder(this).create();
				mAddLookupFilterDialog.setMessage(AndroidResourceHelper.getMessage("AddLookupFilterConfirm"));
				mAddLookupFilterDialog.setButton(DialogInterface.BUTTON_POSITIVE, AndroidResourceHelper.getMessage("Yes"), addLookupFilterListener);
				mAddLookupFilterDialog.setButton(DialogInterface.BUTTON_NEGATIVE, AndroidResourceHelper.getMessage("No"), addLookupFilterListener);
				mAddLookupFilterDialog.show();
			} catch (Exception e) {
				LogManager.getInstance(this).error(e);
			}
		} else if (viewId == mLookupFilterAddResourceData.getExtraResourceID("R.id.right_operand_button")) {
			LinearLayout rowLayout = (LinearLayout) v.getParent();
			doClientScriptSelection(mLookupFilterAddResourceData.getExtraResourceID("R.id.right_operand"), rowLayout);
		}
	}

	private void setColumnSpinnerBasedOnSelectedTable() {
		try {
			String selectedTable = MetrixControlAssistant.getValue(mTable);
			if (!MetrixStringHelper.isNullOrEmpty(selectedTable)) {
				ArrayList<String> columnList = getColumnNames(selectedTable);
				columnList.add(0, "");
				MetrixControlAssistant.populateSpinnerFromList(this, mColumn, columnList);
			} else {
				// pass in a valid query, but use a where clause that ensures no results returned (blank out spinner)
				MetrixControlAssistant.populateSpinnerFromQuery(this, mColumn, "select distinct column_name from mm_field where 1=2", true);
			}
		} catch (Exception e) {
			LogManager.getInstance(this).error(e);
		}
	}

	private void setRightOperandVisibilityBasedOnSelectedOperator() {
		try {
			String selectedOperator = MetrixControlAssistant.getValue(mOperator);
			if (!MetrixStringHelper.isNullOrEmpty(selectedOperator) && (MetrixStringHelper.valueIsEqual(selectedOperator, "IS_NULL") || MetrixStringHelper.valueIsEqual(selectedOperator, "IS_NOT_NULL"))) {
				mRightOperandLabel.setVisibility(View.GONE);
				mRightOperand.setVisibility(View.GONE);
				mRightOperandButton.setVisibility(View.GONE);
				mRightOperandDescLabel.setVisibility(View.GONE);
				mRightOperandDesc.setVisibility(View.GONE);
				mNoQuotesLabel.setVisibility(View.GONE);
				mNoQuotes.setVisibility(View.GONE);
			} else {
				mRightOperandLabel.setVisibility(View.VISIBLE);
				mRightOperand.setVisibility(View.VISIBLE);
				mRightOperandButton.setVisibility(View.VISIBLE);
				if (!MetrixStringHelper.isNullOrEmpty(mRightOperand.getText().toString())) {
					mRightOperandDescLabel.setVisibility(View.INVISIBLE);
					mRightOperandDesc.setVisibility(View.VISIBLE);
				}
				mNoQuotesLabel.setVisibility(View.VISIBLE);
				mNoQuotes.setVisibility(View.VISIBLE);
				mNoQuotes.setEnabled(true);
				if (!MetrixStringHelper.isNullOrEmpty(selectedOperator)) {
					if (MetrixStringHelper.valueIsEqual(selectedOperator, "LIKE") || MetrixStringHelper.valueIsEqual(selectedOperator, "NOT_LIKE")) {
						mNoQuotes.setChecked(false);
						mNoQuotes.setEnabled(false);
					} else if (MetrixStringHelper.valueIsEqual(selectedOperator, "IN") || MetrixStringHelper.valueIsEqual(selectedOperator, "NOT_IN")) {
						mNoQuotes.setChecked(true);
						mNoQuotes.setEnabled(false);
					}
				}
			}
		} catch (Exception e) {
			LogManager.getInstance(this).error(e);
		}
	}

	DialogInterface.OnClickListener addLookupFilterListener = new DialogInterface.OnClickListener() {
		@Override
		public void onClick(DialogInterface dialog, int which) {
			switch (which) {
				case DialogInterface.BUTTON_POSITIVE:    // Yes
					// wire up Yes button to call perform_generate_mobile_field_lkup_filter
					if (SettingsHelper.getSyncPause(mCurrentActivity))
					{
						SyncPauseAlertDialog syncPauseAlertDialog = MetrixDialogAssistant.showSyncPauseAlertDialog(mCurrentActivity);
						if (syncPauseAlertDialog != null) {
							syncPauseAlertDialog.setOnSyncPauseAlertButtonClickListner(new SyncPauseAlertDialog.OnSyncPauseAlertButtonClickListner() {
								@Override
								public void OnSyncPauseAlertButtonClick(DialogInterface dialog, int which) {
									startAddLookupFilterListener();
								}
							});
						}
					} else
						startAddLookupFilterListener();
					break;

				case DialogInterface.BUTTON_NEGATIVE:    // No (do nothing)
					break;
			}
		}
	};

	private void startAddLookupFilterListener() {
		Thread thread = new Thread(new Runnable(){
			@Override
			public void run() {
				MobileApplication.stopSync(mCurrentActivity);
				MobileApplication.startSync(mCurrentActivity, 5);

				if (doLookupFilterAddition(mCurrentActivity) == false) {
					MobileApplication.stopSync(mCurrentActivity);
					MobileApplication.startSync(mCurrentActivity);
					MetrixDesignerFieldLookupFilterAddActivity.this.runOnUiThread(new Runnable() {
						public void run() {
							Toast.makeText(mCurrentActivity, AndroidResourceHelper.getMessage("MobileServiceUnavailable"), Toast.LENGTH_LONG).show();

							if (mAddLookupFilterDialog != null) {
								mAddLookupFilterDialog.dismiss();
							}
						}
					});
					return;
				}

				if (mAddLookupFilterDialog != null) {
					mAddLookupFilterDialog.dismiss();
				}

				// start waiting dialog on-screen
				mUIHelper = new MetrixUIHelper(mCurrentActivity);
				mUIHelper.showLoadingDialog(AndroidResourceHelper.getMessage("AddLookupFilterInProgress"));
			}
		});

		thread.start();
	}

	public static boolean doLookupFilterAddition(Activity activity) {
		MetrixRemoteExecutor remote = new MetrixRemoteExecutor(MobileApplication.getAppContext(), 5);
		String baseUrl = MetrixStringHelper.getString(MetrixPublicCache.instance.getItem("MetrixServiceAddress"));

		if (ping(baseUrl, remote) == false)
			return false;

		Hashtable<String, String> params = new Hashtable<String, String>();
		int device_id = SettingsHelper.getDeviceSequence(MobileApplication.getAppContext());
		try {
			String lkupID = MetrixCurrentKeysHelper.getKeyValue("mm_field_lkup", "lkup_id");
			String logicalOperator = MetrixControlAssistant.getValue(mLogicalOperator);
			String leftParens = MetrixControlAssistant.getValue(mLeftParens);
			String table = MetrixControlAssistant.getValue(mTable);
			String column = MetrixControlAssistant.getValue(mColumn);
			String operator = MetrixControlAssistant.getValue(mOperator);
			String rightOperand = mRightOperand.getText().toString();
			String noQuotes = MetrixControlAssistant.getValue(mNoQuotes);
			String rightParens = MetrixControlAssistant.getValue(mRightParens);

			params.put("lkup_id", lkupID);
			params.put("table_name", table);
			params.put("column_name", column);
			params.put("operator", operator);
			if (!MetrixStringHelper.valueIsEqual(operator, "IS_NULL") && !MetrixStringHelper.valueIsEqual(operator, "IS_NOT_NULL") && !MetrixStringHelper.isNullOrEmpty(rightOperand))
				params.put("right_operand", rightOperand);
			params.put("device_sequence", String.valueOf(device_id));
			if (!MetrixStringHelper.isNullOrEmpty(logicalOperator))
				params.put("logical_operator", String.valueOf(logicalOperator));
			params.put("created_revision_id", MetrixCurrentKeysHelper.getKeyValue("mm_revision", "revision_id"));
			if (!MetrixStringHelper.isNullOrEmpty(leftParens))
				params.put("left_parens", leftParens);
			if (!MetrixStringHelper.isNullOrEmpty(rightParens))
				params.put("right_parens", rightParens);
			if (!MetrixStringHelper.valueIsEqual(operator, "IS_NULL") && !MetrixStringHelper.valueIsEqual(operator, "IS_NOT_NULL") && !MetrixStringHelper.isNullOrEmpty(noQuotes))
				params.put("no_quotes", noQuotes);

			MetrixPerformMessage performGMFLF = new MetrixPerformMessage("perform_generate_mobile_field_lkup_filter", params);
			performGMFLF.save();
		} catch (Exception ex) {
			LogManager.getInstance().error(ex);
			return false;
		}

		return true;
	}
}
