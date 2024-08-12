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
import com.metrix.architecture.utilities.MetrixPublicCache;
import com.metrix.architecture.utilities.MetrixStringHelper;
import com.metrix.architecture.utilities.MetrixUIHelper;
import com.metrix.architecture.utilities.SettingsHelper;
import com.metrix.architecture.utilities.SyncPauseAlertDialog;

import java.util.ArrayList;
import java.util.Hashtable;

public class MetrixDesignerFieldLookupOrderbyAddActivity extends MetrixDesignerActivity {
	private Button mSaveButton;
	private static Spinner mTable, mColumn, mSortOrder;
	private TextView mEmphasis;
	private AlertDialog mAddLookupOrderbyDialog;
	private String mScreenName, mFieldName;
	protected MetrixUIHelper mUIHelper = new MetrixUIHelper(this);
	private MetrixDesignerResourceData mLookupOrderbyAddResourceData;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mLookupOrderbyAddResourceData = (MetrixDesignerResourceData) MetrixPublicCache.instance.getItem("MetrixDesignerFieldLookupOrderbyAddActivityResourceData");

		setContentView(mLookupOrderbyAddResourceData.LayoutResourceID);
		populateScreen();
	}

	@Override
	public void onStart() {
		super.onStart();

		helpText = mLookupOrderbyAddResourceData.HelpTextString;

		mHeadingText = getIntent().getStringExtra("headingText");
		mScreenName = MetrixCurrentKeysHelper.getKeyValue("mm_screen", "screen_name");
		mFieldName = MetrixCurrentKeysHelper.getKeyValue("mm_field", "field_name");
		if (mActionBarTitle != null) {
			mActionBarTitle.setText(mHeadingText);
		}

		mEmphasis = (TextView) findViewById(mLookupOrderbyAddResourceData.getExtraResourceID("R.id.zzmd_field_lookup_orderby_add_emphasis"));
		String fullText = AndroidResourceHelper.getMessage("ScnInfoMxDesFldLkupOrdByAdd", mFieldName, mScreenName);
		mEmphasis.setText(fullText);

		mSaveButton = (Button) findViewById(mLookupOrderbyAddResourceData.getExtraResourceID("R.id.save"));
		mSaveButton.setOnClickListener(this);

		TextView mAddLkupOrderBy = (TextView) findViewById(mLookupOrderbyAddResourceData.getExtraResourceID("R.id.add_lookup_order_by"));
		TextView mTableLbl = (TextView) findViewById(mLookupOrderbyAddResourceData.getExtraResourceID("R.id.table_lbl"));
		TextView mColmnLbl = (TextView) findViewById(mLookupOrderbyAddResourceData.getExtraResourceID("R.id.column_lbl"));
		TextView mSortOrderLbl = (TextView) findViewById(mLookupOrderbyAddResourceData.getExtraResourceID("R.id.sort_order_lbl"));


		AndroidResourceHelper.setResourceValues(mAddLkupOrderBy, "AddLookupOrderBy");
		AndroidResourceHelper.setResourceValues(mTableLbl, "Table");
		AndroidResourceHelper.setResourceValues(mColmnLbl, "Column");
		AndroidResourceHelper.setResourceValues(mSortOrderLbl, "SortOrder");
		AndroidResourceHelper.setResourceValues(mSaveButton, "Save");
	}

	@Override
	protected void bindService() {
		bindService(new Intent(MetrixDesignerFieldLookupOrderbyAddActivity.this, MetrixIntentService.class), mConnection, Context.BIND_AUTO_CREATE);
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
					if (MetrixStringHelper.valueIsEqual(message, "{\"END_GMFLO\":null}")) {
						MobileApplication.stopSync(mCurrentActivity);
						MobileApplication.startSync(mCurrentActivity);
						mUIHelper.dismissLoadingDialog();
						Intent intent = MetrixActivityHelper.createActivityIntent(MetrixDesignerFieldLookupOrderbyAddActivity.this, MetrixDesignerFieldLookupOrderbyActivity.class);
						intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
						// re-getting this data, as the intent has the bad habit of using previous extras
						String designName = MetrixDatabaseManager.getFieldStringValue("mm_design", "name", "design_id = " + MetrixCurrentKeysHelper.getKeyValue("mm_design", "design_id"));
						String revNumber = MetrixDatabaseManager.getFieldStringValue("mm_revision", "revision_number", "revision_id = " + MetrixCurrentKeysHelper.getKeyValue("mm_revision", "revision_id"));
						intent.putExtra("headingText", String.format("%1$s (%2$s %3$s)", designName, AndroidResourceHelper.getMessage("Rev"), revNumber));
						MetrixActivityHelper.startNewActivity(MetrixDesignerFieldLookupOrderbyAddActivity.this, intent);
					} else {
						processPostListener(activityType, message);
					}
				}
			});
		}
	};

	private void populateScreen() {
		mTable = (Spinner) findViewById(mLookupOrderbyAddResourceData.getExtraResourceID("R.id.table"));
		mColumn = (Spinner) findViewById(mLookupOrderbyAddResourceData.getExtraResourceID("R.id.column"));
		mSortOrder = (Spinner) findViewById(mLookupOrderbyAddResourceData.getExtraResourceID("R.id.sort_order"));

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

		StringBuilder sortOrderSpnQuery = new StringBuilder();
		sortOrderSpnQuery.append("select mm_message_def_view.message_text, metrix_code_table.code_value from metrix_code_table ");
		sortOrderSpnQuery.append("join mm_message_def_view on metrix_code_table.message_id = mm_message_def_view.message_id and mm_message_def_view.message_type = 'CODE' ");
		sortOrderSpnQuery.append("where metrix_code_table.code_name = 'MM_FIELD_LKUP_ORDERBY_SORT_ORDER' ");
		sortOrderSpnQuery.append("order by mm_message_def_view.message_text asc");
		MetrixControlAssistant.populateSpinnerFromQuery(this, mSortOrder, sortOrderSpnQuery.toString(), true);
	}

	@Override
	public void onClick(View v) {
		super.onClick(v);

		int viewId = v.getId();
		if (viewId == mLookupOrderbyAddResourceData.getExtraResourceID("R.id.save")) {
			try {
				// Make sure all relevant fields have a value before allowing save
				String table = "";
				String column = "";
				String sortOrder = "";

				if (mTable.getSelectedItem() != null)
					table = MetrixControlAssistant.getValue(mTable);
				if (mColumn.getSelectedItem() != null)
					column = MetrixControlAssistant.getValue(mColumn);
				if (mSortOrder.getSelectedItem() != null)
					sortOrder = MetrixControlAssistant.getValue(mSortOrder);

				if (MetrixStringHelper.isNullOrEmpty(table) || MetrixStringHelper.isNullOrEmpty(column) || MetrixStringHelper.isNullOrEmpty(sortOrder)) {
					Toast.makeText(this, AndroidResourceHelper.getMessage("AddLookupOrderbyError"), Toast.LENGTH_LONG).show();
					return;
				}

				mAddLookupOrderbyDialog = new AlertDialog.Builder(this).create();
				mAddLookupOrderbyDialog.setMessage(AndroidResourceHelper.getMessage("AddLookupOrderbyConfirm"));
				mAddLookupOrderbyDialog.setButton(DialogInterface.BUTTON_POSITIVE, AndroidResourceHelper.getMessage("Yes"), addLookupOrderbyListener);
				mAddLookupOrderbyDialog.setButton(DialogInterface.BUTTON_NEGATIVE, AndroidResourceHelper.getMessage("No"), addLookupOrderbyListener);
				mAddLookupOrderbyDialog.show();
			} catch (Exception e) {
				LogManager.getInstance(this).error(e);
			}
		}
	}

	private void setColumnSpinnerBasedOnSelectedTable() {
		try {
			String selectedTable = MetrixControlAssistant.getValue(mTable);
			if (!MetrixStringHelper.isNullOrEmpty(selectedTable)) {
				ArrayList<String> columnList = getColumnNames(selectedTable);
				columnList.add(0, "");
				ArrayList<Hashtable<String, String>> usedColumns = MetrixDatabaseManager.getFieldStringValuesList(String.format("select distinct column_name from mm_field_lkup_orderby where lkup_id = %1$s and table_name = '%2$s'", MetrixCurrentKeysHelper.getKeyValue("mm_field_lkup", "lkup_id"), selectedTable));
				if (usedColumns != null && usedColumns.size() > 0) {
					for (int i = 0; i < usedColumns.size(); i++) {
						Hashtable<String, String> usedColumnItem = usedColumns.get(i);
						columnList.remove(usedColumnItem.get("column_name"));
					}
				}
				MetrixControlAssistant.populateSpinnerFromList(this, mColumn, columnList);
			} else {
				// pass in a valid query, but use a where clause that ensures no results returned (blank out spinner)
				MetrixControlAssistant.populateSpinnerFromQuery(this, mColumn, "select distinct column_name from mm_field where 1=2", true);
			}
		} catch (Exception e) {
			LogManager.getInstance(this).error(e);
		}
	}

	DialogInterface.OnClickListener addLookupOrderbyListener = new DialogInterface.OnClickListener() {
		@Override
		public void onClick(DialogInterface dialog, int which) {
			switch (which) {
				case DialogInterface.BUTTON_POSITIVE:    // Yes
					// wire up Yes button to call perform_generate_mobile_field_lkup_orderby
					if (SettingsHelper.getSyncPause(mCurrentActivity))
					{
						SyncPauseAlertDialog syncPauseAlertDialog = MetrixDialogAssistant.showSyncPauseAlertDialog(mCurrentActivity);
						if (syncPauseAlertDialog != null) {
							syncPauseAlertDialog.setOnSyncPauseAlertButtonClickListner(new SyncPauseAlertDialog.OnSyncPauseAlertButtonClickListner() {
								@Override
								public void OnSyncPauseAlertButtonClick(DialogInterface dialog, int which) {
									startAddLookupOrderbyListener();
								}
							});
						}
					} else
						startAddLookupOrderbyListener();
					break;

				case DialogInterface.BUTTON_NEGATIVE:    // No (do nothing)
					break;
			}
		}
	};

	private void startAddLookupOrderbyListener() {
		Thread thread = new Thread(new Runnable(){
			@Override
			public void run() {
				MobileApplication.stopSync(mCurrentActivity);
				MobileApplication.startSync(mCurrentActivity, 5);

				if (doLookupOrderbyAddition(mCurrentActivity) == false) {
					MobileApplication.stopSync(mCurrentActivity);
					MobileApplication.startSync(mCurrentActivity);
					MetrixDesignerFieldLookupOrderbyAddActivity.this.runOnUiThread(new Runnable() {
						public void run() {
							Toast.makeText(mCurrentActivity, AndroidResourceHelper.getMessage("MobileServiceUnavailable"), Toast.LENGTH_LONG).show();

							if (mAddLookupOrderbyDialog != null) {
								mAddLookupOrderbyDialog.dismiss();
							}
						}
					});
					return;
				}

				if (mAddLookupOrderbyDialog != null) {
					mAddLookupOrderbyDialog.dismiss();
				}

				// start waiting dialog on-screen
				mUIHelper = new MetrixUIHelper(mCurrentActivity);
				mUIHelper.showLoadingDialog(AndroidResourceHelper.getMessage("AddLookupOrderbyInProgress"));
			}
		});

		thread.start();
	}

	public static boolean doLookupOrderbyAddition(Activity activity) {
		MetrixRemoteExecutor remote = new MetrixRemoteExecutor(MobileApplication.getAppContext(), 5);
		String baseUrl = MetrixStringHelper.getString(MetrixPublicCache.instance.getItem("MetrixServiceAddress"));

		if (ping(baseUrl, remote) == false)
			return false;

		Hashtable<String, String> params = new Hashtable<String, String>();
		int device_id = SettingsHelper.getDeviceSequence(MobileApplication.getAppContext());
		try {
			String lkupID = MetrixCurrentKeysHelper.getKeyValue("mm_field_lkup", "lkup_id");
			String table = MetrixControlAssistant.getValue(mTable);
			String column = MetrixControlAssistant.getValue(mColumn);
			String sortOrder = MetrixControlAssistant.getValue(mSortOrder);

			params.put("lkup_id", lkupID);
			params.put("table_name", table);
			params.put("column_name", column);
			params.put("sort_order", sortOrder);
			params.put("device_sequence", String.valueOf(device_id));
			params.put("created_revision_id", MetrixCurrentKeysHelper.getKeyValue("mm_revision", "revision_id"));

			MetrixPerformMessage performGMFLO = new MetrixPerformMessage("perform_generate_mobile_field_lkup_orderby", params);
			performGMFLO.save();
		} catch (Exception ex) {
			LogManager.getInstance().error(ex);
			return false;
		}

		return true;
	}
}
