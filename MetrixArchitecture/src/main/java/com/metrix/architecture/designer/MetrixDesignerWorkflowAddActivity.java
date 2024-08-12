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
import com.metrix.architecture.utilities.SpinnerKeyValuePair;
import com.metrix.architecture.utilities.SyncPauseAlertDialog;

import java.util.Hashtable;

public class MetrixDesignerWorkflowAddActivity extends MetrixDesignerActivity {
	private Button mSaveButton;
	private static EditText mDesc;
	private static Spinner mBaseWorkflow, mType;
	private AlertDialog mAddWorkflowDialog;
	protected MetrixUIHelper mUIHelper = new MetrixUIHelper(this);
	private static MetrixDesignerResourceData mWorkflowAddResourceData;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mWorkflowAddResourceData = (MetrixDesignerResourceData) MetrixPublicCache.instance.getItem("MetrixDesignerWorkflowAddActivityResourceData");

		setContentView(mWorkflowAddResourceData.LayoutResourceID);
	}

	@Override
	public void onStart() {
		super.onStart();

		helpText = mWorkflowAddResourceData.HelpTextString;

		mHeadingText = getIntent().getStringExtra("headingText");
		if (mActionBarTitle != null) {
			mActionBarTitle.setText(mHeadingText);
		}

		mSaveButton = (Button) findViewById(mWorkflowAddResourceData.getExtraResourceID("R.id.save"));
		mSaveButton.setOnClickListener(this);

		TextView mAddwrkFlw = (TextView) findViewById(mWorkflowAddResourceData.getExtraResourceID("R.id.add_workflow"));
		TextView mScrInfo = (TextView) findViewById(mWorkflowAddResourceData.getExtraResourceID("R.id.screen_info_metrix_designer_workflow_add"));
		TextView mBseWrkFlw = (TextView) findViewById(mWorkflowAddResourceData.getExtraResourceID("R.id.base_workflow_lbl"));
		TextView mTypLbl = (TextView) findViewById(mWorkflowAddResourceData.getExtraResourceID("R.id.type_lbl"));
		TextView mDescLbl = (TextView) findViewById(mWorkflowAddResourceData.getExtraResourceID("R.id.description_lbl"));

		AndroidResourceHelper.setResourceValues(mAddwrkFlw, "AddWorkflow");
		AndroidResourceHelper.setResourceValues(mScrInfo, "ScnInfoMxDesWfAdd");
		AndroidResourceHelper.setResourceValues(mBseWrkFlw, "BaseWorkflow");
		AndroidResourceHelper.setResourceValues(mTypLbl, "Type");
		AndroidResourceHelper.setResourceValues(mDescLbl, "Description");
		AndroidResourceHelper.setResourceValues(mSaveButton, "Save");

		populateScreen();
	}

	@Override
	protected void bindService() {
		bindService(new Intent(MetrixDesignerWorkflowAddActivity.this, MetrixIntentService.class), mConnection, Context.BIND_AUTO_CREATE);
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
					if (MetrixStringHelper.valueIsEqual(message, "{\"END_GMW\":null}")) {
						MobileApplication.stopSync(mCurrentActivity);
						MobileApplication.startSync(mCurrentActivity);
						mUIHelper.dismissLoadingDialog();
						Intent intent = MetrixActivityHelper.createActivityIntent(MetrixDesignerWorkflowAddActivity.this, MetrixDesignerWorkflowActivity.class);
						intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
						// re-getting this data, as the intent has the bad habit of using previous extras
						String designName = MetrixDatabaseManager.getFieldStringValue("mm_design", "name", "design_id = " + MetrixCurrentKeysHelper.getKeyValue("mm_design", "design_id"));
						String revNumber = MetrixDatabaseManager.getFieldStringValue("mm_revision", "revision_number", "revision_id = " + MetrixCurrentKeysHelper.getKeyValue("mm_revision", "revision_id"));
						intent.putExtra("headingText", String.format("%1$s (%2$s %3$s)", designName, AndroidResourceHelper.getMessage("Rev"), revNumber));
						MetrixActivityHelper.startNewActivity(MetrixDesignerWorkflowAddActivity.this, intent);
					} else {
						processPostListener(activityType, message);
					}
				}
			});
		}
	};

	private void populateScreen() {
		mBaseWorkflow = (Spinner) findViewById(mWorkflowAddResourceData.getExtraResourceID("R.id.base_workflow"));
		mType = (Spinner) findViewById(mWorkflowAddResourceData.getExtraResourceID("R.id.type"));
		mDesc = (EditText) findViewById(mWorkflowAddResourceData.getExtraResourceID("R.id.description"));

		String revision_id = MetrixCurrentKeysHelper.getKeyValue("mm_revision", "revision_id");
		MetrixControlAssistant.populateSpinnerFromQuery(this, mBaseWorkflow, String.format("select distinct name from mm_workflow where revision_id = %s and name not null and name not like '%%~%%' order by name asc", revision_id), true);

		mBaseWorkflow.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				setTypeListBasedOnWorkflowSelection();
			}

			public void onNothingSelected(AdapterView<?> parent) {
				setTypeListBasedOnWorkflowSelection();
			}
		});
	}

	@Override
	public void onClick(View v) {
		super.onClick(v);

		int viewId = v.getId();
		if (viewId == mWorkflowAddResourceData.getExtraResourceID("R.id.save")) {
			try {
				boolean generateValidationMsg = false;
				String selectedBase = MetrixControlAssistant.getValue(mBaseWorkflow);
				SpinnerKeyValuePair typePair = (SpinnerKeyValuePair) mType.getSelectedItem();
				String selectedType = (typePair == null) ? "" : typePair.spinnerValue;
				if (MetrixStringHelper.isNullOrEmpty(selectedBase) || MetrixStringHelper.isNullOrEmpty(selectedType)) {
					generateValidationMsg = true;
				} else {
					int instancesOfWorkflow = MetrixDatabaseManager.getCount("mm_workflow", String.format("revision_id = %1$s and name = '%2$s~%3$s'", MetrixCurrentKeysHelper.getKeyValue("mm_revision", "revision_id"), selectedBase, selectedType));
					if (instancesOfWorkflow > 0)
						generateValidationMsg = true;
				}

				if (generateValidationMsg) {
					Toast.makeText(this, AndroidResourceHelper.getMessage("AddWorkflowNameError"), Toast.LENGTH_LONG).show();
					return;
				}

				mAddWorkflowDialog = new AlertDialog.Builder(this).create();
				mAddWorkflowDialog.setMessage(AndroidResourceHelper.getMessage("AddWorkflowConfirm"));
				mAddWorkflowDialog.setButton(DialogInterface.BUTTON_POSITIVE, AndroidResourceHelper.getMessage("Yes"), addWorkflowListener);
				mAddWorkflowDialog.setButton(DialogInterface.BUTTON_NEGATIVE, AndroidResourceHelper.getMessage("No"), addWorkflowListener);
				mAddWorkflowDialog.show();
			} catch (Exception e) {
				LogManager.getInstance(this).error(e);
			}
		}
	}

	private void setTypeListBasedOnWorkflowSelection() {
		try {
			String selectedWorkflow = MetrixControlAssistant.getValue(mBaseWorkflow);
			String revision_id = MetrixCurrentKeysHelper.getKeyValue("mm_revision", "revision_id");
			if (MetrixStringHelper.isNullOrEmpty(selectedWorkflow))
				MetrixControlAssistant.clearSpinner(this, mType);
			else if (MetrixStringHelper.valueIsEqual(selectedWorkflow, "Debrief"))
				MetrixControlAssistant.populateSpinnerFromQuery(this, mType, String.format("select distinct description, code_value from global_code_table where code_name='TASK_TYPE' and code_value not in (select substr(name, 9) from mm_workflow where name like 'Debrief~%%' and revision_id = %s) order by code_value asc", revision_id), true);
			else if (MetrixStringHelper.valueIsEqual(selectedWorkflow, "Schedule"))
				MetrixControlAssistant.populateSpinnerFromQuery(this, mType, String.format("select distinct description, code_value from global_code_table where code_name='REQ_TYPE' and code_value not in (select substr(name, 10) from mm_workflow where name like 'Schedule~%%' and revision_id = %s) order by code_value asc", revision_id), true);
			else if (MetrixStringHelper.valueIsEqual(selectedWorkflow, "Quote"))
				MetrixControlAssistant.populateSpinnerFromQuery(this, mType, String.format("select distinct description, code_value from global_code_table where code_name='QUOTE_TYPE' and code_value not in (select substr(name, 7) from mm_workflow where name like 'Quote~%%' and revision_id = %s) order by code_value asc", revision_id), true);
		} catch (Exception e) {
			LogManager.getInstance(this).error(e);
		}
	}

	DialogInterface.OnClickListener addWorkflowListener = new DialogInterface.OnClickListener() {
		@Override
		public void onClick(DialogInterface dialog, int which) {
			switch (which) {
				case DialogInterface.BUTTON_POSITIVE:    // Yes
					// wire up Yes button to call perform_generate_mobile_field
					if (SettingsHelper.getSyncPause(mCurrentActivity))
					{
						SyncPauseAlertDialog syncPauseAlertDialog = MetrixDialogAssistant.showSyncPauseAlertDialog(mCurrentActivity);
						if (syncPauseAlertDialog != null) {
							syncPauseAlertDialog.setOnSyncPauseAlertButtonClickListner(new SyncPauseAlertDialog.OnSyncPauseAlertButtonClickListner() {
								@Override
								public void OnSyncPauseAlertButtonClick(DialogInterface dialog, int which) {
									startAddWorkflowListener();
								}
							});
						}
					} else
						startAddWorkflowListener();
					break;

				case DialogInterface.BUTTON_NEGATIVE:    // No (do nothing)
					break;
			}
		}
	};

	private void startAddWorkflowListener() {
		Thread thread = new Thread(new Runnable(){
			@Override
			public void run() {
				MobileApplication.stopSync(mCurrentActivity);
				MobileApplication.startSync(mCurrentActivity, 5);

				if (doWorkflowAddition(mCurrentActivity) == false) {
					MobileApplication.stopSync(mCurrentActivity);
					MobileApplication.startSync(mCurrentActivity);
					MetrixDesignerWorkflowAddActivity.this.runOnUiThread(new Runnable() {
						public void run() {
							Toast.makeText(mCurrentActivity, AndroidResourceHelper.getMessage("MobileServiceUnavailable"), Toast.LENGTH_LONG).show();
							if (mAddWorkflowDialog != null) {
								mAddWorkflowDialog.dismiss();
							}
						}
					});
					return;
				}

				if (mAddWorkflowDialog != null) {
					mAddWorkflowDialog.dismiss();
				}

				// start waiting dialog on-screen
				mUIHelper = new MetrixUIHelper(mCurrentActivity);
				mUIHelper.showLoadingDialog(AndroidResourceHelper.getMessage("AddWorkflowInProgress"));
			}
		});

		thread.start();
	}

	public static boolean doWorkflowAddition(Activity activity) {
		MetrixRemoteExecutor remote = new MetrixRemoteExecutor(MobileApplication.getAppContext(), 5);
		String baseUrl = MetrixStringHelper.getString(MetrixPublicCache.instance.getItem("MetrixServiceAddress"));

		if (ping(baseUrl, remote) == false)
			return false;

		Hashtable<String, String> params = new Hashtable<String, String>();
		int device_id = SettingsHelper.getDeviceSequence(MobileApplication.getAppContext());

		try {
			String selectedBase = MetrixControlAssistant.getValue(mBaseWorkflow);
			SpinnerKeyValuePair typePair = (SpinnerKeyValuePair) mType.getSelectedItem();
			String selectedType = typePair.spinnerValue;
			String typeDesc = typePair.spinnerKey;
			String description = mDesc.getText().toString();

			params.put("design_id", MetrixCurrentKeysHelper.getKeyValue("mm_design", "design_id"));
			params.put("revision_id", MetrixCurrentKeysHelper.getKeyValue("mm_revision", "revision_id"));
			params.put("workflow_base_name", selectedBase);
			params.put("device_sequence", String.valueOf(device_id));
			if (!MetrixStringHelper.isNullOrEmpty(description))
				params.put("description", description);
			else
				params.put("description", AndroidResourceHelper.getMessage("AddWorkflowDescriptionParam", selectedBase, typeDesc));
			params.put("type_filter", selectedType);

			MetrixPerformMessage performGMW = new MetrixPerformMessage("perform_generate_mobile_workflow", params);
			performGMW.save();
		}
		catch (Exception ex) {
			LogManager.getInstance().error(ex);
			return false;
		}

		return true;
	}
}
