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
import android.widget.CheckBox;
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
import com.metrix.architecture.utilities.SpinnerKeyValuePair;
import com.metrix.architecture.utilities.SyncPauseAlertDialog;

import java.util.ArrayList;
import java.util.Hashtable;

public class MetrixDesignerFieldLookupColumnAddActivity extends MetrixDesignerActivity {
	private Button mSaveButton;
	private static Spinner mColumn, mLinkedField;
	private static CheckBox mAlwaysHide;
	private TextView mEmphasis;
	private AlertDialog mAddLookupColumnDialog;
	private String mScreenName, mFieldName, mTableName;
	protected MetrixUIHelper mUIHelper = new MetrixUIHelper(this);
	private MetrixDesignerResourceData mLookupColumnAddResourceData;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mLookupColumnAddResourceData = (MetrixDesignerResourceData) MetrixPublicCache.instance.getItem("MetrixDesignerFieldLookupColumnAddActivityResourceData");

		setContentView(mLookupColumnAddResourceData.LayoutResourceID);

		mTableName = MetrixCurrentKeysHelper.getKeyValue("mm_field_lkup_table", "table_name");
		populateScreen();
	}

	@Override
	public void onStart() {
		super.onStart();

		helpText = mLookupColumnAddResourceData.HelpTextString;

		mHeadingText = getIntent().getStringExtra("headingText");
		mScreenName = MetrixCurrentKeysHelper.getKeyValue("mm_screen", "screen_name");
		mFieldName = MetrixCurrentKeysHelper.getKeyValue("mm_field", "field_name");
		if (mActionBarTitle != null) {
			mActionBarTitle.setText(mHeadingText);
		}

		mEmphasis = (TextView) findViewById(mLookupColumnAddResourceData.getExtraResourceID("R.id.zzmd_field_lookup_column_add_emphasis"));
		String fullText = AndroidResourceHelper.getMessage("ScnInfoMxDesFldLkupColAdd", mTableName, mFieldName, mScreenName);
		mEmphasis.setText(fullText);

		mSaveButton = (Button) findViewById(mLookupColumnAddResourceData.getExtraResourceID("R.id.save"));
		mSaveButton.setOnClickListener(this);
		AndroidResourceHelper.setResourceValues(mSaveButton, "Save");
	}

	@Override
	protected void bindService() {
		bindService(new Intent(MetrixDesignerFieldLookupColumnAddActivity.this, MetrixIntentService.class), mConnection, Context.BIND_AUTO_CREATE);
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
					if (MetrixStringHelper.valueIsEqual(message, "{\"END_GMFLC\":null}")) {
						MobileApplication.stopSync(mCurrentActivity);
						MobileApplication.startSync(mCurrentActivity);
						mUIHelper.dismissLoadingDialog();
						Intent intent = MetrixActivityHelper.createActivityIntent(MetrixDesignerFieldLookupColumnAddActivity.this, MetrixDesignerFieldLookupColumnActivity.class);
						intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
						// re-getting this data, as the intent has the bad habit of using previous extras
						String designName = MetrixDatabaseManager.getFieldStringValue("mm_design", "name", "design_id = " + MetrixCurrentKeysHelper.getKeyValue("mm_design", "design_id"));
						String revNumber = MetrixDatabaseManager.getFieldStringValue("mm_revision", "revision_number", "revision_id = " + MetrixCurrentKeysHelper.getKeyValue("mm_revision", "revision_id"));
						intent.putExtra("headingText", String.format("%1$s (%2$s %3$s)", designName, AndroidResourceHelper.getMessage("Rev"), revNumber));
						MetrixActivityHelper.startNewActivity(MetrixDesignerFieldLookupColumnAddActivity.this, intent);
					} else {
						processPostListener(activityType, message);
					}
				}
			});
		}
	};

	private void populateScreen() {
		mColumn = (Spinner) findViewById(mLookupColumnAddResourceData.getExtraResourceID("R.id.column"));
		mLinkedField = (Spinner) findViewById(mLookupColumnAddResourceData.getExtraResourceID("R.id.linked_field_id"));
		mAlwaysHide = (CheckBox) findViewById(mLookupColumnAddResourceData.getExtraResourceID("R.id.always_hide"));

		TextView mAddLkup = (TextView) findViewById(mLookupColumnAddResourceData.getExtraResourceID("R.id.add_lookup_column"));
		TextView mClmn = (TextView) findViewById(mLookupColumnAddResourceData.getExtraResourceID("R.id.column_lbl"));
		TextView mLnkField = (TextView) findViewById(mLookupColumnAddResourceData.getExtraResourceID("R.id.linked_field_lbl"));
		TextView mAlwHide = (TextView) findViewById(mLookupColumnAddResourceData.getExtraResourceID("R.id.always_hide_lbl"));

		AndroidResourceHelper.setResourceValues(mAddLkup, "AddLookupColumn");
		AndroidResourceHelper.setResourceValues(mClmn, "Column");
		AndroidResourceHelper.setResourceValues(mLnkField, "LinkedField");
		AndroidResourceHelper.setResourceValues(mAlwHide, "AlwaysHide");



		// populate Column spinner with all columns found in client DB for current table, excluding any columns already used in this lookup
		String lkupTableID = MetrixCurrentKeysHelper.getKeyValue("mm_field_lkup_table", "lkup_table_id");
		ArrayList<String> columnList = getColumnNames(mTableName);
		columnList.add(0, "");
		ArrayList<Hashtable<String, String>> usedColumns = MetrixDatabaseManager.getFieldStringValuesList(String.format("select distinct column_name from mm_field_lkup_column where lkup_table_id = %s", lkupTableID));
		if (usedColumns != null && usedColumns.size() > 0) {
			for (int i = 0; i < usedColumns.size(); i++) {
				Hashtable<String, String> usedColumnItem = usedColumns.get(i);
				columnList.remove(usedColumnItem.get("column_name"));
			}
		}
		MetrixControlAssistant.populateSpinnerFromList(this, mColumn, columnList);

		// populate Linked Field spinner with all fields on this screen, except those that already serve as linked fields somewhere on the current lookup (across all tables)
		String screenID = MetrixCurrentKeysHelper.getKeyValue("mm_screen", "screen_id");
		String lkupID = MetrixCurrentKeysHelper.getKeyValue("mm_field_lkup", "lkup_id");
		//we should omit control types such as buttons as they're not allowed to treat as linked fields
		ArrayList<Hashtable<String, String>> screenFieldArray = MetrixDatabaseManager.getFieldStringValuesList(String.format("select field_id, table_name, column_name from mm_field where screen_id = %1$s and field_id not in (select distinct linked_field_id from mm_field_lkup_column where linked_field_id is not null and lkup_table_id in (select lkup_table_id from mm_field_lkup_table where lkup_id = %2$s)) and control_type not in ('%3$s') order by table_name, column_name asc", screenID, lkupID, "BUTTON"));
		ArrayList<SpinnerKeyValuePair> linkedFieldSet = new ArrayList<SpinnerKeyValuePair>();
		linkedFieldSet.add(new SpinnerKeyValuePair("", ""));
		if (screenFieldArray != null && screenFieldArray.size() > 0) {
			for (Hashtable<String, String> fieldData : screenFieldArray) {
				String fieldID = fieldData.get("field_id");
				String fieldName = String.format("%1$s.%2$s", fieldData.get("table_name"), fieldData.get("column_name"));
				SpinnerKeyValuePair item = new SpinnerKeyValuePair(fieldName, fieldID);
				linkedFieldSet.add(item);
			}
		}
		MetrixControlAssistant.populateSpinnerFromPair(this, mLinkedField, linkedFieldSet);
	}

	@Override
	public void onClick(View v) {
		super.onClick(v);

		int viewId = v.getId();
		if (viewId == mLookupColumnAddResourceData.getExtraResourceID("R.id.save")) {
			try {
				// Make sure we have a column before allowing save
				String column = "";
				if (mColumn.getSelectedItem() != null)
					column = MetrixControlAssistant.getValue(mColumn);

				if (MetrixStringHelper.isNullOrEmpty(column)) {
					Toast.makeText(this, AndroidResourceHelper.getMessage("AddLookupColumnColumnError"), Toast.LENGTH_LONG).show();
					return;
				}

				mAddLookupColumnDialog = new AlertDialog.Builder(this).create();
				mAddLookupColumnDialog.setMessage(AndroidResourceHelper.getMessage("AddLookupColumnConfirm"));
				mAddLookupColumnDialog.setButton(DialogInterface.BUTTON_POSITIVE, AndroidResourceHelper.getMessage("Yes"), addLookupColumnListener);
				mAddLookupColumnDialog.setButton(DialogInterface.BUTTON_NEGATIVE, AndroidResourceHelper.getMessage("No"), addLookupColumnListener);
				mAddLookupColumnDialog.show();
			} catch (Exception e) {
				LogManager.getInstance(this).error(e);
			}
		}
	}

	DialogInterface.OnClickListener addLookupColumnListener = new DialogInterface.OnClickListener() {
		@Override
		public void onClick(DialogInterface dialog, int which) {
			switch (which) {
				case DialogInterface.BUTTON_POSITIVE:    // Yes
					// wire up Yes button to call perform_generate_mobile_field_lkup_column
					if (SettingsHelper.getSyncPause(mCurrentActivity))
					{
						SyncPauseAlertDialog syncPauseAlertDialog = MetrixDialogAssistant.showSyncPauseAlertDialog(mCurrentActivity);
						if (syncPauseAlertDialog != null) {
							syncPauseAlertDialog.setOnSyncPauseAlertButtonClickListner(new SyncPauseAlertDialog.OnSyncPauseAlertButtonClickListner() {
								@Override
								public void OnSyncPauseAlertButtonClick(DialogInterface dialog, int which) {
									startAddLookupColumnListener();
								}
							});
						}
					} else
						startAddLookupColumnListener();
					break;

				case DialogInterface.BUTTON_NEGATIVE:    // No (do nothing)
					break;
			}
		}
	};

	private void startAddLookupColumnListener() {
		Thread thread = new Thread(new Runnable(){
			@Override
			public void run() {
				MobileApplication.stopSync(mCurrentActivity);
				MobileApplication.startSync(mCurrentActivity, 5);

				if (doLookupColumnAddition(mCurrentActivity) == false) {
					MobileApplication.stopSync(mCurrentActivity);
					MobileApplication.startSync(mCurrentActivity);
					MetrixDesignerFieldLookupColumnAddActivity.this.runOnUiThread(new Runnable() {
						public void run() {
							Toast.makeText(mCurrentActivity, AndroidResourceHelper.getMessage("MobileServiceUnavailable"), Toast.LENGTH_LONG).show();

							if (mAddLookupColumnDialog != null) {
								mAddLookupColumnDialog.dismiss();
							}
						}
					});
					return;
				}

				if (mAddLookupColumnDialog != null) {
					mAddLookupColumnDialog.dismiss();
				}

				// start waiting dialog on-screen
				mUIHelper = new MetrixUIHelper(mCurrentActivity);
				mUIHelper.showLoadingDialog(AndroidResourceHelper.getMessage("AddLookupColumnInProgress"));
			}
		});

		thread.start();
	}

	public static boolean doLookupColumnAddition(Activity activity) {
		MetrixRemoteExecutor remote = new MetrixRemoteExecutor(MobileApplication.getAppContext(), 5);
		String baseUrl = MetrixStringHelper.getString(MetrixPublicCache.instance.getItem("MetrixServiceAddress"));

		if (ping(baseUrl, remote) == false)
			return false;

		Hashtable<String, String> params = new Hashtable<String, String>();
		int device_id = SettingsHelper.getDeviceSequence(MobileApplication.getAppContext());
		try {
			String lkupTableID = MetrixCurrentKeysHelper.getKeyValue("mm_field_lkup_table", "lkup_table_id");
			String column = MetrixControlAssistant.getValue(mColumn);
			String linkedFieldID = MetrixControlAssistant.getValue(mLinkedField);
			String alwaysHide = mAlwaysHide.isChecked() ? "Y" : "N";

			params.put("lkup_table_id", lkupTableID);
			params.put("column_name", column);
			params.put("device_sequence", String.valueOf(device_id));
			if (!MetrixStringHelper.isNullOrEmpty(linkedFieldID))
				params.put("linked_field_id", linkedFieldID);
			params.put("always_hide", alwaysHide);
			params.put("created_revision_id", MetrixCurrentKeysHelper.getKeyValue("mm_revision", "revision_id"));

			MetrixPerformMessage performGMFLC = new MetrixPerformMessage("perform_generate_mobile_field_lkup_column", params);
			performGMFLC.save();
		} catch (Exception ex) {
			LogManager.getInstance().error(ex);
			return false;
		}

		return true;
	}
}
