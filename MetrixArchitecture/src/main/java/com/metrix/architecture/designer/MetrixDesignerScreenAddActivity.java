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
import android.widget.CompoundButton;
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

import java.util.ArrayList;
import java.util.Hashtable;

public class MetrixDesignerScreenAddActivity extends MetrixDesignerActivity {
	private Button mSaveButton;
	private static CheckBox mTabChild;
	private static EditText mScreenName, mDesc;
	private static Spinner mScreenType, mPrimaryTable, mWorkflow, mTabParent, mMenuOption;
	private TextView mPrimaryTableLabel, mWorkflowLabel, mTabParentLabel, mMenuOptionLabel, mTabChildLabel;
	private AlertDialog mAddScreenDialog;
	protected MetrixUIHelper mUIHelper = new MetrixUIHelper(this);
	private MetrixDesignerResourceData mScreenAddResourceData;

	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mScreenAddResourceData = (MetrixDesignerResourceData) MetrixPublicCache.instance.getItem("MetrixDesignerScreenAddActivityResourceData");

        setContentView(mScreenAddResourceData.LayoutResourceID);
    }

    @Override
    public void onStart() {
		super.onStart();

        helpText = mScreenAddResourceData.HelpTextString;

		mHeadingText = getIntent().getStringExtra("headingText");
		if (mActionBarTitle != null) {
			mActionBarTitle.setText(mHeadingText);
		}

        mSaveButton = (Button) findViewById(mScreenAddResourceData.getExtraResourceID("R.id.save"));
        mSaveButton.setText(AndroidResourceHelper.getMessage("Save"));
        mSaveButton.setOnClickListener(this);

		populateScreen();
	}

	@Override
	protected void bindService() {
		bindService(new Intent(MetrixDesignerScreenAddActivity.this, MetrixIntentService.class), mConnection, Context.BIND_AUTO_CREATE);
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
					if (MetrixStringHelper.valueIsEqual(message, "{\"END_GMS\":null}")) {
						MobileApplication.stopSync(mCurrentActivity);
                        MobileApplication.startSync(mCurrentActivity);
                        mUIHelper.dismissLoadingDialog();
						Intent intent = MetrixActivityHelper.createActivityIntent(MetrixDesignerScreenAddActivity.this, MetrixDesignerScreenActivity.class);
						intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
						// re-getting this data, as the intent has the bad habit of using previous extras
						String designName = MetrixDatabaseManager.getFieldStringValue("mm_design", "name", "design_id = " + MetrixCurrentKeysHelper.getKeyValue("mm_design", "design_id"));
						String revNumber = MetrixDatabaseManager.getFieldStringValue("mm_revision", "revision_number", "revision_id = " + MetrixCurrentKeysHelper.getKeyValue("mm_revision", "revision_id"));
                        intent.putExtra("headingText", String.format("%1$s (%2$s %3$s)", designName, AndroidResourceHelper.getMessage("Rev"), revNumber));
                        MetrixActivityHelper.startNewActivity(MetrixDesignerScreenAddActivity.this, intent);
                    } else {
						processPostListener(activityType, message);
					}
				}
			});
		}
	};

	private void populateScreen() {
		try {
			mScreenName = (EditText) findViewById(mScreenAddResourceData.getExtraResourceID("R.id.screen_name"));
			mScreenType = (Spinner) findViewById(mScreenAddResourceData.getExtraResourceID("R.id.screen_type"));
			mPrimaryTable = (Spinner) findViewById(mScreenAddResourceData.getExtraResourceID("R.id.primary_table"));
			mWorkflow = (Spinner) findViewById(mScreenAddResourceData.getExtraResourceID("R.id.workflow"));
			mTabParent = (Spinner) findViewById(mScreenAddResourceData.getExtraResourceID("R.id.tab_parent"));
			mMenuOption = (Spinner) findViewById(mScreenAddResourceData.getExtraResourceID("R.id.menu_option"));
			mTabChild = (CheckBox) findViewById(mScreenAddResourceData.getExtraResourceID("R.id.tab_child"));
			mDesc = (EditText) findViewById(mScreenAddResourceData.getExtraResourceID("R.id.description"));
			mPrimaryTableLabel = (TextView) findViewById(mScreenAddResourceData.getExtraResourceID("R.id.primary_table_label"));
			mWorkflowLabel = (TextView) findViewById(mScreenAddResourceData.getExtraResourceID("R.id.workflow_label"));
			mTabParentLabel = (TextView) findViewById(mScreenAddResourceData.getExtraResourceID("R.id.tab_parent_label"));
			mMenuOptionLabel = (TextView) findViewById(mScreenAddResourceData.getExtraResourceID("R.id.menu_option_label"));
			mTabChildLabel = (TextView) findViewById(mScreenAddResourceData.getExtraResourceID("R.id.tab_child_label"));

            TextView mAddScr = (TextView) findViewById(mScreenAddResourceData.getExtraResourceID("R.id.add_screen"));
            TextView mScrInfo = (TextView) findViewById(mScreenAddResourceData.getExtraResourceID("R.id.screen_info_metrix_designer_screen_add"));
            TextView mNamelbl = (TextView) findViewById(mScreenAddResourceData.getExtraResourceID("R.id.name"));
            TextView mScrTypLbl = (TextView) findViewById(mScreenAddResourceData.getExtraResourceID("R.id.screen_type_label"));
            TextView mDescLbl = (TextView) findViewById(mScreenAddResourceData.getExtraResourceID("R.id.description_label"));

            AndroidResourceHelper.setResourceValues(mAddScr, "AddScreen");
            AndroidResourceHelper.setResourceValues(mScrInfo, "ScnInfoMxDesScnAdd");
            AndroidResourceHelper.setResourceValues(mNamelbl, "Name");
            AndroidResourceHelper.setResourceValues(mScrTypLbl, "ScreenType");
            AndroidResourceHelper.setResourceValues(mPrimaryTableLabel, "Table");
            AndroidResourceHelper.setResourceValues(mWorkflowLabel, "Workflow");
            AndroidResourceHelper.setResourceValues(mTabParentLabel, "TabParent");
            AndroidResourceHelper.setResourceValues(mTabChildLabel, "TabChild");
            AndroidResourceHelper.setResourceValues(mDescLbl, "Description");

			// add required hint to Screen Name
            mScreenName.setHint(AndroidResourceHelper.getMessage("Required"));

            // use AndroidResourceHelper to populate resource strings, using mm_message_def_view
            mMenuOptionLabel.setText(AndroidResourceHelper.getMessage("MenuOption"));

			StringBuilder screenTypeQuery = new StringBuilder();
			screenTypeQuery.append("select mm_message_def_view.message_text, metrix_code_table.code_value from metrix_code_table ");
			screenTypeQuery.append("join mm_message_def_view on metrix_code_table.message_id = mm_message_def_view.message_id and mm_message_def_view.message_type = 'CODE' ");
			screenTypeQuery.append("where metrix_code_table.code_name = 'MM_SCREEN_TYPE' ");
			screenTypeQuery.append("order by mm_message_def_view.message_text asc");
			MetrixControlAssistant.populateSpinnerFromQuery(this, mScreenType, screenTypeQuery.toString(), false);
			MetrixControlAssistant.setValue(mScreenType, "STANDARD");
			mScreenType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
				public void onItemSelected(AdapterView<?> parent, View view, int position, long id) { reactToSelectedScreenType(); }
				public void onNothingSelected(AdapterView<?> parent) { reactToSelectedScreenType(); }
			});

			// populate table spinner with all business tables found in client DB
			ArrayList<String> tableNameList = getBusinessDataTableNames();
			tableNameList.add(0, "");
			MetrixControlAssistant.populateSpinnerFromList(this, mPrimaryTable, tableNameList);

			// populate workflow spinner with all workflows from this revision
			MetrixControlAssistant.populateSpinnerFromQuery(this, mWorkflow, String.format("select name, workflow_id from mm_workflow where revision_id = %s order by name asc", MetrixCurrentKeysHelper.getKeyValue("mm_revision", "revision_id")), true);
			mWorkflow.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
				public void onItemSelected(AdapterView<?> parent, View view, int position, long id) { reactToSelectedWorkflow(); }
				public void onNothingSelected(AdapterView<?> parent) {
					reactToSelectedWorkflow();
				}
			});

			// populate tab parent spinner with all TAB_PARENT type screens from this revision
			MetrixControlAssistant.populateSpinnerFromQuery(this, mTabParent, String.format("select screen_name, screen_id from mm_screen where revision_id = %s and screen_type = 'TAB_PARENT' order by screen_name asc", MetrixCurrentKeysHelper.getKeyValue("mm_revision", "revision_id")), true);
			mTabParent.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
				public void onItemSelected(AdapterView<?> parent, View view, int position, long id) { reactToSelectedTabParent(); }
				public void onNothingSelected(AdapterView<?> parent) {
					reactToSelectedTabParent();
				}
			});
			mTabParentLabel.setVisibility(View.GONE);
			mTabParent.setVisibility(View.GONE);

			StringBuilder menuOptionQuery = new StringBuilder();
			menuOptionQuery.append("select mm_message_def_view.message_text, metrix_code_table.code_value from metrix_code_table ");
			menuOptionQuery.append("join mm_message_def_view on metrix_code_table.message_id = mm_message_def_view.message_id and mm_message_def_view.message_type = 'CODE' ");
			menuOptionQuery.append("where metrix_code_table.code_name = 'MM_SCREEN_ADD_MENU_OPTIONS' ");
			menuOptionQuery.append("order by mm_message_def_view.message_text asc");
			MetrixControlAssistant.populateSpinnerFromQuery(this, mMenuOption, menuOptionQuery.toString(), true);

			mTabChild.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(CompoundButton chk, boolean isChecked) { reactToTabChildChange(isChecked); }
			});
		} catch (Exception e) {
			LogManager.getInstance().error(e);
		}
	}

	@Override
	public void onClick(View v) {
		super.onClick(v);

		int viewId = v.getId();
		if (viewId == mScreenAddResourceData.getExtraResourceID("R.id.save")) {
			try {
				// make sure Screen Name is populated AND that a screen with that screen_name doesn't already exist in this revision
				boolean generateValidationMsg = false;
				String screenName = MetrixControlAssistant.getValue(mScreenName);
				if (MetrixStringHelper.isNullOrEmpty(screenName)) {
					generateValidationMsg = true;
				} else {
					int instancesOfScreenName = MetrixDatabaseManager.getCount("mm_screen", String.format("revision_id = %1$s and screen_name = '%2$s'", MetrixCurrentKeysHelper.getKeyValue("mm_revision", "revision_id"), screenName));
					if (instancesOfScreenName > 0) {
						generateValidationMsg = true;
					}
				}

				if (generateValidationMsg) {
                    Toast.makeText(this, AndroidResourceHelper.getMessage("AddScreenNameError"), Toast.LENGTH_LONG).show();
                    return;
				}

				if (mTabChild.isChecked()) {
					// A tab child cannot use a Tab Parent screen type
					String currScreenType = MetrixControlAssistant.getValue(mScreenType);
					if (MetrixStringHelper.valueIsEqual(currScreenType, "TAB_PARENT")) {
                        Toast.makeText(this, AndroidResourceHelper.getMessage("AddScreenInvalidTabChildType"), Toast.LENGTH_LONG).show();
                        return;
					}

					// A tab child must have a Tab Parent defined
					String currTabParent = MetrixControlAssistant.getValue(mTabParent);
					if (MetrixStringHelper.isNullOrEmpty(currTabParent)) {
                        Toast.makeText(this, AndroidResourceHelper.getMessage("AddScreenInvalidTabChildParent"), Toast.LENGTH_LONG).show();
                        return;
					}
				}

				mAddScreenDialog = new AlertDialog.Builder(this).create();
                mAddScreenDialog.setMessage(AndroidResourceHelper.getMessage("AddScreenConfirm"));
                mAddScreenDialog.setButton(DialogInterface.BUTTON_POSITIVE, AndroidResourceHelper.getMessage("Yes"), addScreenListener);
                mAddScreenDialog.setButton(DialogInterface.BUTTON_NEGATIVE, AndroidResourceHelper.getMessage("No"), addScreenListener);
                mAddScreenDialog.show();
            } catch (Exception e) {
				LogManager.getInstance(this).error(e);
			}
        }
    }

	private void reactToSelectedScreenType() {
		try {
			String selectedScreenType = MetrixControlAssistant.getValue(mScreenType);
			if (MetrixStringHelper.valueIsEqual(selectedScreenType, "TAB_PARENT")) {
				MetrixControlAssistant.setValue(mPrimaryTable, "");
				MetrixControlAssistant.setValue(mWorkflow, "");
				if (mTabParent.getCount() > 0) { MetrixControlAssistant.setValue(mTabParent, ""); }
				mTabChild.setChecked(false);

				mPrimaryTableLabel.setVisibility(View.GONE);
				mPrimaryTable.setVisibility(View.GONE);
				mWorkflowLabel.setVisibility(View.GONE);
				mWorkflow.setVisibility(View.GONE);
				mTabParentLabel.setVisibility(View.GONE);
				mTabParent.setVisibility(View.GONE);
				mTabChildLabel.setVisibility(View.GONE);
				mTabChild.setVisibility(View.GONE);
				mMenuOptionLabel.setVisibility(View.VISIBLE);
				mMenuOption.setVisibility(View.VISIBLE);
			} else if (MetrixStringHelper.valueIsEqual(selectedScreenType, "ATTACHMENT_API_CARD") || MetrixStringHelper.valueIsEqual(selectedScreenType, "ATTACHMENT_API_LIST")) {
				MetrixControlAssistant.setValue(mPrimaryTable, "");
				MetrixControlAssistant.setValue(mWorkflow, "");
				MetrixControlAssistant.setValue(mMenuOption, "");
				if (mTabParent.getCount() > 0) { MetrixControlAssistant.setValue(mTabParent, ""); }
				mTabChild.setChecked(false);

				mPrimaryTableLabel.setVisibility(View.GONE);
				mPrimaryTable.setVisibility(View.GONE);

				mWorkflowLabel.setVisibility(View.GONE);
				mWorkflow.setVisibility(View.GONE);
				mTabParentLabel.setVisibility(View.GONE);
				mTabParent.setVisibility(View.GONE);
				mTabChildLabel.setVisibility(View.GONE);
				mTabChild.setVisibility(View.GONE);
				mMenuOptionLabel.setVisibility(View.GONE);
				mMenuOption.setVisibility(View.GONE);
			} else {
				mPrimaryTableLabel.setVisibility(View.VISIBLE);
				mPrimaryTable.setVisibility(View.VISIBLE);
				mTabChildLabel.setVisibility(View.VISIBLE);
				mTabChild.setVisibility(View.VISIBLE);
				if (!mTabChild.isChecked()) {
					mWorkflowLabel.setVisibility(View.VISIBLE);
					mWorkflow.setVisibility(View.VISIBLE);
				} else {
					mTabParentLabel.setVisibility(View.VISIBLE);
					mTabParent.setVisibility(View.VISIBLE);
				}
				mMenuOptionLabel.setVisibility(View.VISIBLE);
				mMenuOption.setVisibility(View.VISIBLE);
			}
		} catch (Exception e) {
			LogManager.getInstance().error(e);
		}
	}

	private void reactToSelectedWorkflow() {
		try {
			boolean isTabChild = mTabChild.isChecked();
			String selectedWorkflow = MetrixControlAssistant.getValue(mWorkflow);
			if (!(MetrixStringHelper.isNullOrEmpty(selectedWorkflow)) || isTabChild) {
				MetrixControlAssistant.setValue(mMenuOption, "");
				mMenuOptionLabel.setVisibility(View.GONE);
				mMenuOption.setVisibility(View.GONE);
			} else {
				mMenuOptionLabel.setVisibility(View.VISIBLE);
				mMenuOption.setVisibility(View.VISIBLE);
			}
		} catch (Exception e) {
			LogManager.getInstance().error(e);
		}
	}

	private void reactToSelectedTabParent() {
		try {
			boolean enableScreenType = true;
			String selectedTabParent = MetrixControlAssistant.getValue(mTabParent);
			if (!MetrixStringHelper.isNullOrEmpty(selectedTabParent)) {
				int tabChildCount = MetrixDatabaseManager.getCount("mm_screen", String.format("tab_parent_id = %s", selectedTabParent));
				if (tabChildCount == 0) {
					// first tab child must be STANDARD
					MetrixControlAssistant.setValue(mScreenType, "STANDARD");
					enableScreenType = false;
				}
			}
			mScreenType.setEnabled(enableScreenType);
		} catch (Exception e) {
			LogManager.getInstance().error(e);
		}
	}

	private void reactToTabChildChange(boolean tabChildChecked) {
		try {
			if (tabChildChecked) {
				MetrixControlAssistant.setValue(mWorkflow, "");
				mWorkflowLabel.setVisibility(View.GONE);
				mWorkflow.setVisibility(View.GONE);
				MetrixControlAssistant.setValue(mMenuOption, "");
				mMenuOptionLabel.setVisibility(View.GONE);
				mMenuOption.setVisibility(View.GONE);
				mTabParentLabel.setVisibility(View.VISIBLE);
				mTabParent.setVisibility(View.VISIBLE);
			} else {
				mScreenType.setEnabled(true);
				if (mTabParent.getCount() > 0) { MetrixControlAssistant.setValue(mTabParent, ""); }
				mWorkflowLabel.setVisibility(View.VISIBLE);
				mWorkflow.setVisibility(View.VISIBLE);
				mMenuOptionLabel.setVisibility(View.VISIBLE);
				mMenuOption.setVisibility(View.VISIBLE);
				mTabParentLabel.setVisibility(View.GONE);
				mTabParent.setVisibility(View.GONE);
			}
		} catch (Exception e) {
			LogManager.getInstance().error(e);
		}
	}

	DialogInterface.OnClickListener addScreenListener = new DialogInterface.OnClickListener() {
		@Override
		public void onClick(DialogInterface dialog, int which) {
			switch (which) {
				case DialogInterface.BUTTON_POSITIVE:	// Yes
					// wire up Yes button to call perform_generate_mobile_field
					if (SettingsHelper.getSyncPause(mCurrentActivity)) {
						SyncPauseAlertDialog syncPauseAlertDialog = MetrixDialogAssistant.showSyncPauseAlertDialog(mCurrentActivity);
						if (syncPauseAlertDialog != null) {
							syncPauseAlertDialog.setOnSyncPauseAlertButtonClickListner(new SyncPauseAlertDialog.OnSyncPauseAlertButtonClickListner() {
								@Override
								public void OnSyncPauseAlertButtonClick(DialogInterface dialog, int which) { startAddScreenListener(); }
							});
						}
					}
					else
						startAddScreenListener();
					break;

				case DialogInterface.BUTTON_NEGATIVE:	// No (do nothing)
					break;
			}
		}
	};

	private void startAddScreenListener() {
		Thread thread = new Thread(new Runnable(){
            @Override
            public void run() {
                MobileApplication.stopSync(mCurrentActivity);
                MobileApplication.startSync(mCurrentActivity, 5);

                if (doScreenAddition(mCurrentActivity) == false) {
                    MobileApplication.stopSync(mCurrentActivity);
                    MobileApplication.startSync(mCurrentActivity);
                    MetrixDesignerScreenAddActivity.this.runOnUiThread(new Runnable() {
                        public void run() {
                            Toast.makeText(mCurrentActivity, AndroidResourceHelper.getMessage("MobileServiceUnavailable"), Toast.LENGTH_LONG).show();

                            if (mAddScreenDialog != null) {
                                mAddScreenDialog.dismiss();
                            }
                        }
                    });
                    return;
                }

                if (mAddScreenDialog != null) {
                    mAddScreenDialog.dismiss();
                }

                // start waiting dialog on-screen
                mUIHelper = new MetrixUIHelper(mCurrentActivity);
                mUIHelper.showLoadingDialog(AndroidResourceHelper.getMessage("AddScreenInProgress"));
            }
        });

		thread.start();
	}

	public static boolean doScreenAddition(Activity activity) {
		MetrixRemoteExecutor remote = new MetrixRemoteExecutor(MobileApplication.getAppContext(), 5);
		String baseUrl = MetrixStringHelper.getString(MetrixPublicCache.instance.getItem("MetrixServiceAddress"));

		if (ping(baseUrl, remote) == false)
			return false;

		Hashtable<String, String> params = new Hashtable<String, String>();
		int device_id = SettingsHelper.getDeviceSequence(MobileApplication.getAppContext());

        try {
			String screenName = mScreenName.getText().toString();
			String screenType = MetrixControlAssistant.getValue(mScreenType);
			boolean isTabParent = MetrixStringHelper.valueIsEqual(screenType, "TAB_PARENT");
			String tableName = MetrixControlAssistant.getValue(mPrimaryTable);
			SpinnerKeyValuePair pair = (SpinnerKeyValuePair) mWorkflow.getSelectedItem();
			String workflowID = (pair == null) ? "" : pair.spinnerValue;
			SpinnerKeyValuePair tabParentPair = (SpinnerKeyValuePair) mTabParent.getSelectedItem();
			String tabParentID = (tabParentPair == null) ? "" : tabParentPair.spinnerValue;
			SpinnerKeyValuePair menuOptionPair = (SpinnerKeyValuePair) mMenuOption.getSelectedItem();
			String menuOption = (menuOptionPair == null) ? "" : menuOptionPair.spinnerValue;
			boolean isTabChild = mTabChild.isChecked();
			String description = mDesc.getText().toString();

			if (MetrixStringHelper.valueIsEqual(screenType, "ATTACHMENT_API_CARD") || MetrixStringHelper.valueIsEqual(screenType, "ATTACHMENT_API_LIST"))
				tableName = "ATTACHMENT";

            params.put("design_id", MetrixCurrentKeysHelper.getKeyValue("mm_design", "design_id"));
			params.put("revision_id", MetrixCurrentKeysHelper.getKeyValue("mm_revision", "revision_id"));
			params.put("screen_name", screenName);
			params.put("screen_type", screenType);
			params.put("device_sequence", String.valueOf(device_id));
			if (!isTabParent && !isTabChild && !MetrixStringHelper.isNullOrEmpty(workflowID)) {
				// clear out mm_workflow_screen for this workflowID, as the server-side MPM will return all rows
				MetrixDatabaseManager.executeSql(String.format("delete from mm_workflow_screen where workflow_id = %s", workflowID));
				params.put("workflow_id", workflowID);
			}
			if (!MetrixStringHelper.isNullOrEmpty(description))
				params.put("description", description);
			if (!isTabParent && !MetrixStringHelper.isNullOrEmpty(tableName))
				params.put("table_name", tableName);
			if (isTabChild && !MetrixStringHelper.isNullOrEmpty(tabParentID))
				params.put("tab_parent_id", tabParentID);
			if (MetrixStringHelper.isNullOrEmpty(workflowID) && !isTabChild && !MetrixStringHelper.isNullOrEmpty(menuOption))
				params.put("menu_option", menuOption);

			MetrixPerformMessage performGMS = new MetrixPerformMessage("perform_generate_mobile_screen", params);
			performGMS.save();
		} catch (Exception ex) {
			LogManager.getInstance().error(ex);
			return false;
		}

        return true;
	}
}

