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
import android.view.View.OnFocusChangeListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.metrix.architecture.assistants.MetrixControlAssistant;
import com.metrix.architecture.assistants.MetrixDialogAssistant;
import com.metrix.architecture.database.MetrixDatabaseManager;
import com.metrix.architecture.database.MobileApplication;
import com.metrix.architecture.metadata.MetrixLookupColumnDef;
import com.metrix.architecture.metadata.MetrixLookupDef;
import com.metrix.architecture.metadata.MetrixLookupFilterDef;
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

public class MetrixDesignerFieldLookupAddActivity extends MetrixDesignerActivity implements OnFocusChangeListener {
	private Button mSaveButton;
	private static CheckBox mInitialSearch;
	private static Spinner mInitialTable;
	private static EditText mTitle;
	private TextView mEmphasis, mTitleDescLabel, mTitleDesc;
	private AlertDialog mAddFieldLookupDialog;
	private String mScreenName, mFieldName;
	protected MetrixUIHelper mUIHelper = new MetrixUIHelper(this);
	private MetrixDesignerResourceData mFieldLookupAddResourceData;

	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mFieldLookupAddResourceData = (MetrixDesignerResourceData) MetrixPublicCache.instance.getItem("MetrixDesignerFieldLookupAddActivityResourceData");

        setContentView(mFieldLookupAddResourceData.LayoutResourceID);
        populateScreen();
    }

	@Override
	public void onStart() {
		super.onStart();

        helpText = mFieldLookupAddResourceData.HelpTextString;

		mHeadingText = getIntent().getStringExtra("headingText");
		mScreenName = MetrixCurrentKeysHelper.getKeyValue("mm_screen", "screen_name");
		mFieldName = MetrixCurrentKeysHelper.getKeyValue("mm_field", "field_name");
		if (mActionBarTitle != null) {
			mActionBarTitle.setText(mHeadingText);
		}

		mEmphasis = (TextView) findViewById(mFieldLookupAddResourceData.getExtraResourceID("R.id.zzmd_field_lookup_add_emphasis"));
        String fullText = AndroidResourceHelper.getMessage("ScnInfoMxDesFldLkupAdd", mFieldName, mScreenName);
        mEmphasis.setText(fullText);

        mSaveButton = (Button) findViewById(mFieldLookupAddResourceData.getExtraResourceID("R.id.save"));
        mSaveButton.setOnClickListener(this);
        AndroidResourceHelper.setResourceValues(mSaveButton, "Save");
    }

	@Override
	protected void bindService() {
		bindService(new Intent(MetrixDesignerFieldLookupAddActivity.this, MetrixIntentService.class), mConnection, Context.BIND_AUTO_CREATE);
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
					if (MetrixStringHelper.valueIsEqual(message, "{\"END_GMFL\":null}")) {
						MobileApplication.stopSync(mCurrentActivity);
						MobileApplication.startSync(mCurrentActivity);
						mUIHelper.dismissLoadingDialog();
						String fieldID = MetrixCurrentKeysHelper.getKeyValue("mm_field", "field_id");
						String lkupID = MetrixDatabaseManager.getFieldStringValue("mm_field_lkup", "lkup_id", "field_id = " + fieldID);
						MetrixCurrentKeysHelper.setKeyValue("mm_field_lkup", "lkup_id", lkupID);
						Intent intent = MetrixActivityHelper.createActivityIntent(MetrixDesignerFieldLookupAddActivity.this, MetrixDesignerFieldLookupPropActivity.class);
						// re-getting this data, as the intent has the bad habit of using previous extras
						String designName = MetrixDatabaseManager.getFieldStringValue("mm_design", "name", "design_id = " + MetrixCurrentKeysHelper.getKeyValue("mm_design", "design_id"));
						String revNumber = MetrixDatabaseManager.getFieldStringValue("mm_revision", "revision_number", "revision_id = " + MetrixCurrentKeysHelper.getKeyValue("mm_revision", "revision_id"));
                        intent.putExtra("headingText", String.format("%1$s (%2$s %3$s)", designName, AndroidResourceHelper.getMessage("Rev"), revNumber));
                        MetrixActivityHelper.startNewActivityAndFinish(MetrixDesignerFieldLookupAddActivity.this, intent);
                    } else {
						processPostListener(activityType, message);
					}
				}
			});
		}
	};

	private void populateScreen() {
        mInitialSearch = (CheckBox) findViewById(mFieldLookupAddResourceData.getExtraResourceID("R.id.initial_search_chk"));
        mInitialTable = (Spinner) findViewById(mFieldLookupAddResourceData.getExtraResourceID("R.id.initial_table_spn"));
        mTitle = (EditText) findViewById(mFieldLookupAddResourceData.getExtraResourceID("R.id.title_txt"));
        mTitleDescLabel = (TextView) findViewById(mFieldLookupAddResourceData.getExtraResourceID("R.id.title_description_label"));
        mTitleDesc = (TextView) findViewById(mFieldLookupAddResourceData.getExtraResourceID("R.id.title_description"));

        // add Required hint to Title
        mTitle.setHint(AndroidResourceHelper.getMessage("Required"));

        TextView mAddField = (TextView) findViewById(mFieldLookupAddResourceData.getExtraResourceID("R.id.add_field_lookup"));
        TextView mTitleLbl = (TextView) findViewById(mFieldLookupAddResourceData.getExtraResourceID("R.id.title_lbl"));
        TextView mInitSr = (TextView) findViewById(mFieldLookupAddResourceData.getExtraResourceID("R.id.initial_search_lbl"));
        TextView mInitTbl = (TextView) findViewById(mFieldLookupAddResourceData.getExtraResourceID("R.id.initial_table_lbl"));
        AndroidResourceHelper.setResourceValues(mAddField, "AddFieldLookup");
        AndroidResourceHelper.setResourceValues(mTitleLbl, "Title");
        AndroidResourceHelper.setResourceValues(mInitSr, "InitialSearch");
        AndroidResourceHelper.setResourceValues(mInitTbl, "InitialTable");

		// populate initial table spinner with all business tables found in client DB
		ArrayList<String> tableNameList = getBusinessDataTableNames();
		tableNameList.add(0, "");
		MetrixControlAssistant.populateSpinnerFromList(this, mInitialTable, tableNameList);

		mTitle.setOnFocusChangeListener(this);
		mTitle.addTextChangedListener(new MessageTextWatcher("MM_LOOKUP_TITLE", mTitleDescLabel, mTitleDesc));

		mTitleDescLabel.setVisibility(View.GONE);
		mTitleDesc.setVisibility(View.GONE);
	}

	@Override
	public void onFocusChange(View v, boolean hasFocus) {
        if (hasFocus) {
            LinearLayout rowLayout = (LinearLayout) v.getParent();
			int viewId = v.getId();
			if (viewId == mFieldLookupAddResourceData.getExtraResourceID("R.id.title_txt"))
				doLookupTitleMessageSelection(viewId, rowLayout);
        }
    }

	private void doLookupTitleMessageSelection(int viewToPopulateId, LinearLayout parentLayout) {
		MetrixLookupDef lookupDef = new MetrixLookupDef("mm_message_def_view");
		lookupDef.columnNames.add(new MetrixLookupColumnDef("mm_message_def_view.message_id", viewToPopulateId));
        lookupDef.columnNames.add(new MetrixLookupColumnDef("mm_message_def_view.message_text"));
        lookupDef.filters.add(new MetrixLookupFilterDef("mm_message_def_view.message_type", "=", "MM_LOOKUP_TITLE"));

		Intent intent = MetrixActivityHelper.createActivityIntent(this, "com.metrix.metrixmobile.system", "Lookup");
		intent.putExtra("NoOptionsMenu", true);
		MetrixPublicCache.instance.addItem("lookupDef", lookupDef);
		MetrixPublicCache.instance.addItem("lookupParentLayout", parentLayout);
		startActivityForResult(intent, 2727);
	}

	@Override
	public void onClick(View v) {
		super.onClick(v);

		int viewId = v.getId();
		if (viewId == mFieldLookupAddResourceData.getExtraResourceID("R.id.save")) {
			try {
				// Make sure we have a title before allowing save
                String title = mTitle.getText().toString();
                if (MetrixStringHelper.isNullOrEmpty(title)) {
                    Toast.makeText(this, AndroidResourceHelper.getMessage("AddFieldLookupTitleError"), Toast.LENGTH_LONG).show();
                    return;
				}

				mAddFieldLookupDialog = new AlertDialog.Builder(this).create();
                mAddFieldLookupDialog.setMessage(AndroidResourceHelper.getMessage("AddFieldLookupConfirm"));
                mAddFieldLookupDialog.setButton(DialogInterface.BUTTON_POSITIVE, AndroidResourceHelper.getMessage("Yes"), addFieldLookupListener);
                mAddFieldLookupDialog.setButton(DialogInterface.BUTTON_NEGATIVE, AndroidResourceHelper.getMessage("No"), addFieldLookupListener);
                mAddFieldLookupDialog.show();
            } catch (Exception e) {
				LogManager.getInstance(this).error(e);
			}
        }
    }

    DialogInterface.OnClickListener addFieldLookupListener = new DialogInterface.OnClickListener() {
		@Override
		public void onClick(DialogInterface dialog, int which) {
			switch (which) {
                case DialogInterface.BUTTON_POSITIVE:    // Yes
                    // wire up Yes button to call perform_generate_mobile_field_lkup
                    if (SettingsHelper.getSyncPause(mCurrentActivity)) {
                        SyncPauseAlertDialog syncPauseAlertDialog = MetrixDialogAssistant.showSyncPauseAlertDialog(mCurrentActivity);
                        if (syncPauseAlertDialog != null) {
                            syncPauseAlertDialog.setOnSyncPauseAlertButtonClickListner(new SyncPauseAlertDialog.OnSyncPauseAlertButtonClickListner() {
                                @Override
                                public void OnSyncPauseAlertButtonClick(DialogInterface dialog, int which) {
                                    startAddFieldLookupListener();
                                }
                            });
                        }
                    } else
                        startAddFieldLookupListener();
                    break;

                case DialogInterface.BUTTON_NEGATIVE:    // No (do nothing)
                    break;
            }
		}
	};

	private void startAddFieldLookupListener() {
		Thread thread = new Thread(new Runnable(){
            @Override
            public void run() {
                MobileApplication.stopSync(mCurrentActivity);
                MobileApplication.startSync(mCurrentActivity, 5);

                if (doFieldLookupAddition(mCurrentActivity) == false) {
                    MobileApplication.stopSync(mCurrentActivity);
                    MobileApplication.startSync(mCurrentActivity);
                    MetrixDesignerFieldLookupAddActivity.this.runOnUiThread(new Runnable() {
                        public void run() {
                            Toast.makeText(mCurrentActivity, AndroidResourceHelper.getMessage("MobileServiceUnavailable"), Toast.LENGTH_LONG).show();

                            if (mAddFieldLookupDialog != null) {
                                mAddFieldLookupDialog.dismiss();
                            }
                        }
                    });
                    return;
                }

                if (mAddFieldLookupDialog != null) {
                    mAddFieldLookupDialog.dismiss();
                }

                // start waiting dialog on-screen
                mUIHelper = new MetrixUIHelper(mCurrentActivity);
                mUIHelper.showLoadingDialog(AndroidResourceHelper.getMessage("AddFieldLookupInProgress"));
            }
        });

		thread.start();
	}

	public static boolean doFieldLookupAddition(Activity activity) {
		MetrixRemoteExecutor remote = new MetrixRemoteExecutor(MobileApplication.getAppContext(), 5);
		String baseUrl = MetrixStringHelper.getString(MetrixPublicCache.instance.getItem("MetrixServiceAddress"));

		if (ping(baseUrl, remote) == false)
			return false;

        Hashtable<String, String> params = new Hashtable<String, String>();
		int device_id = SettingsHelper.getDeviceSequence(MobileApplication.getAppContext());
        try {
            String fieldID = MetrixCurrentKeysHelper.getKeyValue("mm_field", "field_id");
			String title = mTitle.getText().toString();
			String initialTable = MetrixControlAssistant.getValue(mInitialTable);
			String performInitialSearch = mInitialSearch.isChecked() ? "Y" : "N";

            params.put("field_id", fieldID);
			params.put("title", title);
			params.put("device_sequence", String.valueOf(device_id));
			params.put("perform_initial_search", performInitialSearch);
			if (!MetrixStringHelper.isNullOrEmpty(initialTable))
				params.put("table_name", initialTable);
			params.put("created_revision_id", MetrixCurrentKeysHelper.getKeyValue("mm_revision", "revision_id"));

            MetrixPerformMessage performGMFL = new MetrixPerformMessage("perform_generate_mobile_field_lkup", params);
			performGMFL.save();
		} catch (Exception ex) {
			LogManager.getInstance().error(ex);
			return false;
		}

        return true;
	}
}
