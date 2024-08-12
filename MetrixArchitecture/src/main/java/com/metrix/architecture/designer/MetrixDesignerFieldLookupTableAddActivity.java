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
import android.widget.Button;
import android.widget.EditText;
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

public class MetrixDesignerFieldLookupTableAddActivity extends MetrixDesignerActivity {
	private Button mSaveButton;
	private static Spinner mTable, mParentTable;
	private static EditText mParentKeyColumns, mChildKeyColumns;
	private TextView mEmphasis;
	private AlertDialog mAddLookupTableDialog;
	private String mScreenName, mFieldName;
	protected MetrixUIHelper mUIHelper = new MetrixUIHelper(this);
	private MetrixDesignerResourceData mLookupTableAddResourceData;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mLookupTableAddResourceData = (MetrixDesignerResourceData) MetrixPublicCache.instance.getItem("MetrixDesignerFieldLookupTableAddActivityResourceData");

		setContentView(mLookupTableAddResourceData.LayoutResourceID);
		populateScreen();
	}

	@Override
	public void onStart() {
		super.onStart();

		helpText = mLookupTableAddResourceData.HelpTextString;

		mHeadingText = getIntent().getStringExtra("headingText");
		mScreenName = MetrixCurrentKeysHelper.getKeyValue("mm_screen", "screen_name");
		mFieldName = MetrixCurrentKeysHelper.getKeyValue("mm_field", "field_name");
		if (mActionBarTitle != null) {
			mActionBarTitle.setText(mHeadingText);
		}

		mEmphasis = (TextView) findViewById(mLookupTableAddResourceData.getExtraResourceID("R.id.zzmd_field_lookup_table_add_emphasis"));
		String fullText = AndroidResourceHelper.getMessage("ScnInfoMxDesFldLkupTblAdd", mFieldName, mScreenName);
		mEmphasis.setText(fullText);

		mSaveButton = (Button) findViewById(mLookupTableAddResourceData.getExtraResourceID("R.id.save"));
		mSaveButton.setOnClickListener(this);

		TextView mAddlkupTbl = (TextView) findViewById(mLookupTableAddResourceData.getExtraResourceID("R.id.add_lookup_table"));
		TextView mTableLbl = (TextView) findViewById(mLookupTableAddResourceData.getExtraResourceID("R.id.table_lbl"));
		TextView mPrntTblLbl = (TextView) findViewById(mLookupTableAddResourceData.getExtraResourceID("R.id.parent_table_lbl"));
		TextView mPrntColLbl = (TextView) findViewById(mLookupTableAddResourceData.getExtraResourceID("R.id.parent_columns_lbl"));
		TextView mChldColLbl = (TextView) findViewById(mLookupTableAddResourceData.getExtraResourceID("R.id.child_columns_lbl"));


		AndroidResourceHelper.setResourceValues(mAddlkupTbl, "AddLookupTable");
		AndroidResourceHelper.setResourceValues(mTableLbl, "Table");
		AndroidResourceHelper.setResourceValues(mPrntTblLbl, "ParentTable");
		AndroidResourceHelper.setResourceValues(mPrntColLbl, "ParentColumns");
		AndroidResourceHelper.setResourceValues(mChldColLbl, "ChildColumns");
		AndroidResourceHelper.setResourceValues(mSaveButton, "Save");
	}

	@Override
	protected void bindService() {
		bindService(new Intent(MetrixDesignerFieldLookupTableAddActivity.this, MetrixIntentService.class), mConnection, Context.BIND_AUTO_CREATE);
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
					if (MetrixStringHelper.valueIsEqual(message, "{\"END_GMFLT\":null}")) {
						MobileApplication.stopSync(mCurrentActivity);
						MobileApplication.startSync(mCurrentActivity);
						mUIHelper.dismissLoadingDialog();
						Intent intent = MetrixActivityHelper.createActivityIntent(MetrixDesignerFieldLookupTableAddActivity.this, MetrixDesignerFieldLookupTableActivity.class);
						intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
						// re-getting this data, as the intent has the bad habit of using previous extras
						String designName = MetrixDatabaseManager.getFieldStringValue("mm_design", "name", "design_id = " + MetrixCurrentKeysHelper.getKeyValue("mm_design", "design_id"));
						String revNumber = MetrixDatabaseManager.getFieldStringValue("mm_revision", "revision_number", "revision_id = " + MetrixCurrentKeysHelper.getKeyValue("mm_revision", "revision_id"));
						intent.putExtra("headingText", String.format("%1$s (%2$s %3$s)", designName, AndroidResourceHelper.getMessage("Rev"), revNumber));
						MetrixActivityHelper.startNewActivity(MetrixDesignerFieldLookupTableAddActivity.this, intent);
					} else {
						processPostListener(activityType, message);
					}
				}
			});
		}
	};

	private void populateScreen() {
		mTable = (Spinner) findViewById(mLookupTableAddResourceData.getExtraResourceID("R.id.table"));
		mParentTable = (Spinner) findViewById(mLookupTableAddResourceData.getExtraResourceID("R.id.parent_table"));
		mParentKeyColumns = (EditText) findViewById(mLookupTableAddResourceData.getExtraResourceID("R.id.parent_key_columns"));
		mChildKeyColumns = (EditText) findViewById(mLookupTableAddResourceData.getExtraResourceID("R.id.child_key_columns"));

		// populate parent table spinner with any tables that already exist on the current lookup
		String lkupID = MetrixCurrentKeysHelper.getKeyValue("mm_field_lkup", "lkup_id");
		String parentTableQuery = "select distinct table_name from mm_field_lkup_table where lkup_id = " + lkupID;
		MetrixControlAssistant.populateSpinnerFromQuery(this, mParentTable, parentTableQuery, true);

		// populate table spinner with all business tables found in client DB, EXCEPT those already in use on this lookup
		ArrayList<String> tableNameList = getBusinessDataTableNames();
		tableNameList.add(0, "");
		ArrayList<Hashtable<String, String>> usedTableNameArray = MetrixDatabaseManager.getFieldStringValuesList("select distinct table_name from mm_field_lkup_table where lkup_id = " + lkupID);
		if (usedTableNameArray != null && usedTableNameArray.size() > 0) {
			for (Hashtable<String, String> item : usedTableNameArray) {
				String usedTableName = item.get("table_name");
				if (!MetrixStringHelper.isNullOrEmpty(usedTableName))
					tableNameList.remove(usedTableName);
			}
		}
		MetrixControlAssistant.populateSpinnerFromList(this, mTable, tableNameList);
	}

	@Override
	public void onClick(View v) {
		super.onClick(v);

		int viewId = v.getId();
		if (viewId == mLookupTableAddResourceData.getExtraResourceID("R.id.save")) {
			try {
				// Make sure we have a table before allowing save
				String table = "";
				if (mTable.getSelectedItem() != null)
					table = MetrixControlAssistant.getValue(mTable);

				if (MetrixStringHelper.isNullOrEmpty(table)) {
					Toast.makeText(this, AndroidResourceHelper.getMessage("AddLookupTableTableError"), Toast.LENGTH_LONG).show();
					return;
				}

				String parentTable = "";
				if (mParentTable.getSelectedItem() != null)
					parentTable = MetrixControlAssistant.getValue(mParentTable);

				// Make sure that, if parent_table is defined, that it is not the same as table
				if (!MetrixStringHelper.isNullOrEmpty(parentTable) && MetrixStringHelper.valueIsEqual(table, parentTable)) {
					Toast.makeText(this, AndroidResourceHelper.getMessage("AddLookupTableParentTableError"), Toast.LENGTH_LONG).show();
					return;
				}

				mAddLookupTableDialog = new AlertDialog.Builder(this).create();
				mAddLookupTableDialog.setMessage(AndroidResourceHelper.getMessage("AddLookupTableConfirm"));
				mAddLookupTableDialog.setButton(DialogInterface.BUTTON_POSITIVE, AndroidResourceHelper.getMessage("Yes"), addLookupTableListener);
				mAddLookupTableDialog.setButton(DialogInterface.BUTTON_NEGATIVE, AndroidResourceHelper.getMessage("No"), addLookupTableListener);
				mAddLookupTableDialog.show();
			} catch (Exception e) {
				LogManager.getInstance(this).error(e);
			}
		}
	}

	DialogInterface.OnClickListener addLookupTableListener = new DialogInterface.OnClickListener() {
		@Override
		public void onClick(DialogInterface dialog, int which) {
			switch (which) {
				case DialogInterface.BUTTON_POSITIVE:    // Yes
					// wire up Yes button to call perform_generate_mobile_field_lkup_table
					if (SettingsHelper.getSyncPause(mCurrentActivity))
					{
						SyncPauseAlertDialog syncPauseAlertDialog = MetrixDialogAssistant.showSyncPauseAlertDialog(mCurrentActivity);
						if (syncPauseAlertDialog != null) {
							syncPauseAlertDialog.setOnSyncPauseAlertButtonClickListner(new SyncPauseAlertDialog.OnSyncPauseAlertButtonClickListner() {
								@Override
								public void OnSyncPauseAlertButtonClick(DialogInterface dialog, int which) {
									startAddLookupTableListener();
								}
							});
						}
					} else
						startAddLookupTableListener();
					break;

				case DialogInterface.BUTTON_NEGATIVE:    // No (do nothing)
					break;
			}
		}
	};

	private void startAddLookupTableListener() {
		Thread thread = new Thread(new Runnable(){
			@Override
			public void run() {
				MobileApplication.stopSync(mCurrentActivity);
				MobileApplication.startSync(mCurrentActivity, 5);

				if (doLookupTableAddition(mCurrentActivity) == false) {
					MobileApplication.stopSync(mCurrentActivity);
					MobileApplication.startSync(mCurrentActivity);
					MetrixDesignerFieldLookupTableAddActivity.this.runOnUiThread(new Runnable() {
						public void run() {
							Toast.makeText(mCurrentActivity, AndroidResourceHelper.getMessage("MobileServiceUnavailable"), Toast.LENGTH_LONG).show();

							if (mAddLookupTableDialog != null) {
								mAddLookupTableDialog.dismiss();
							}
						}
					});
					return;
				}

				if (mAddLookupTableDialog != null) {
					mAddLookupTableDialog.dismiss();
				}

				// start waiting dialog on-screen
				mUIHelper = new MetrixUIHelper(mCurrentActivity);
				mUIHelper.showLoadingDialog(AndroidResourceHelper.getMessage("AddLookupTableInProgress"));
			}
		});

		thread.start();
	}

	public static boolean doLookupTableAddition(Activity activity) {
		MetrixRemoteExecutor remote = new MetrixRemoteExecutor(MobileApplication.getAppContext(), 5);
		String baseUrl = MetrixStringHelper.getString(MetrixPublicCache.instance.getItem("MetrixServiceAddress"));

		if (ping(baseUrl, remote) == false)
			return false;

		Hashtable<String, String> params = new Hashtable<String, String>();
		int device_id = SettingsHelper.getDeviceSequence(MobileApplication.getAppContext());
		try {
			String lkupID = MetrixCurrentKeysHelper.getKeyValue("mm_field_lkup", "lkup_id");
			String table = MetrixControlAssistant.getValue(mTable);
			String parentTable = MetrixControlAssistant.getValue(mParentTable);
			String parentKeyColumns = mParentKeyColumns.getText().toString();
			String childKeyColumns = mChildKeyColumns.getText().toString();

			params.put("lkup_id", lkupID);
			params.put("table_name", table);
			params.put("device_sequence", String.valueOf(device_id));
			if (!MetrixStringHelper.isNullOrEmpty(parentTable))
				params.put("parent_table_name", parentTable);
			if (!MetrixStringHelper.isNullOrEmpty(parentKeyColumns))
				params.put("parent_key_columns", parentKeyColumns);
			if (!MetrixStringHelper.isNullOrEmpty(childKeyColumns))
				params.put("child_key_columns", childKeyColumns);
			params.put("created_revision_id", MetrixCurrentKeysHelper.getKeyValue("mm_revision", "revision_id"));

			MetrixPerformMessage performGMFLT = new MetrixPerformMessage("perform_generate_mobile_field_lkup_table", params);
			performGMFLT.save();
		} catch (Exception ex) {
			LogManager.getInstance().error(ex);
			return false;
		}

		return true;
	}
}
