package com.metrix.architecture.designer;

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
import com.metrix.architecture.constants.MetrixTransactionTypes;
import com.metrix.architecture.database.MetrixDatabaseManager;
import com.metrix.architecture.database.MobileApplication;
import com.metrix.architecture.managers.MetrixUpdateManager;
import com.metrix.architecture.metadata.MetrixLookupColumnDef;
import com.metrix.architecture.metadata.MetrixLookupDef;
import com.metrix.architecture.metadata.MetrixLookupFilterDef;
import com.metrix.architecture.metadata.MetrixPerformMessage;
import com.metrix.architecture.metadata.MetrixSqlData;
import com.metrix.architecture.metadata.MetrixTransaction;
import com.metrix.architecture.services.IPostMonitor;
import com.metrix.architecture.services.MetrixIntentService;
import com.metrix.architecture.services.MetrixRemoteExecutor;
import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.architecture.utilities.DataField;
import com.metrix.architecture.utilities.Global;
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

public class MetrixDesignerFilterSortAddActivity extends MetrixDesignerActivity implements View.OnFocusChangeListener {
    private Button mSaveButton;
    private static CheckBox mIsDefault, mFullFilter;
    private static Spinner mItemType;
    private static EditText mItemName, mLabel, mContent;
    private static TextView mItemNameLabel, mItemTypeLabel, mLabelLabel, mContentLabel, mIsDefaultLabel, mFullFilterLabel;
    private ImageView mContentButton;
    private TextView mHeadingLabel, mTip, mLabelDescLabel, mLabelDesc, mContentDescLabel, mContentDesc;
    private AlertDialog mAddFilterSortDialog;
    private static String mScreenId, mScreenName, mSelectedItemType;
    protected MetrixUIHelper mUIHelper = new MetrixUIHelper(this);
    private MetrixDesignerResourceData mFSAResourceData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mFSAResourceData = (MetrixDesignerResourceData) MetrixPublicCache.instance.getItem("MetrixDesignerFilterSortAddActivityResourceData");

        setContentView(mFSAResourceData.LayoutResourceID);
        populateScreen();
    }

    @Override
    public void onStart() {
        super.onStart();

        helpText = mFSAResourceData.HelpTextString;

        mHeadingText = getIntent().getStringExtra("headingText");
        mScreenId = MetrixCurrentKeysHelper.getKeyValue("mm_screen", "screen_id");
        mScreenName = MetrixCurrentKeysHelper.getKeyValue("mm_screen", "screen_name");
        if (mActionBarTitle != null) {
            mActionBarTitle.setText(mHeadingText);
        }

        mHeadingLabel = (TextView) findViewById(mFSAResourceData.getExtraResourceID("R.id.zzmd_filter_sort_add_label"));
        mHeadingLabel.setText(AndroidResourceHelper.getMessage("AddFilterSortLabel"));
        mTip = (TextView) findViewById(mFSAResourceData.getExtraResourceID("R.id.zzmd_filter_sort_add_tip"));
        mTip.setText(AndroidResourceHelper.getMessage("AddFilterSortTip1Args", mScreenName));

        mSaveButton = (Button) findViewById(mFSAResourceData.getExtraResourceID("R.id.save"));
        mSaveButton.setText(AndroidResourceHelper.getMessage("Save"));
        mSaveButton.setOnClickListener(this);
    }

    @Override
    protected void bindService() {
        bindService(new Intent(MetrixDesignerFilterSortAddActivity.this, MetrixIntentService.class), mConnection, Context.BIND_AUTO_CREATE);
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
        public void newSyncStatus(final Global.ActivityType activityType, final String message) {
            runOnUiThread(new Runnable() {
                public void run() {
                    if (MetrixStringHelper.valueIsEqual(message, "{\"END_GMFSI\":null}")) {
                        MobileApplication.stopSync(mCurrentActivity);
                        MobileApplication.startSync(mCurrentActivity);
                        mUIHelper.dismissLoadingDialog();
                        Intent intent = MetrixActivityHelper.createActivityIntent(MetrixDesignerFilterSortAddActivity.this, MetrixDesignerFilterSortEnablingActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        // re-getting this data, as the intent has the bad habit of using previous extras
                        String designName = MetrixDatabaseManager.getFieldStringValue("mm_design", "name", "design_id = " + MetrixCurrentKeysHelper.getKeyValue("mm_design", "design_id"));
                        String revNumber = MetrixDatabaseManager.getFieldStringValue("mm_revision", "revision_number", "revision_id = " + MetrixCurrentKeysHelper.getKeyValue("mm_revision", "revision_id"));
                        intent.putExtra("headingText", String.format("%1$s (%2$s %3$s)", designName, AndroidResourceHelper.getMessage("Rev"), revNumber));
                        intent.putExtra("filterSortCodeValue", mSelectedItemType);
                        MetrixActivityHelper.startNewActivity(MetrixDesignerFilterSortAddActivity.this, intent);
                    } else {
                        processPostListener(activityType, message);
                    }
                }
            });
        }
    };

    private void populateScreen() {
        try {
            mItemName = (EditText) findViewById(mFSAResourceData.getExtraResourceID("R.id.item_name"));
            mItemType = (Spinner) findViewById(mFSAResourceData.getExtraResourceID("R.id.item_type"));
            mLabel = (EditText) findViewById(mFSAResourceData.getExtraResourceID("R.id.label"));
            mContent = (EditText) findViewById(mFSAResourceData.getExtraResourceID("R.id.content"));
            mContentButton = (ImageView) findViewById(mFSAResourceData.getExtraResourceID("R.id.content_button"));
            mIsDefault = (CheckBox) findViewById(mFSAResourceData.getExtraResourceID("R.id.is_default"));
            mFullFilter = (CheckBox) findViewById(mFSAResourceData.getExtraResourceID("R.id.full_filter"));

            mItemNameLabel = (TextView) findViewById(mFSAResourceData.getExtraResourceID("R.id.item_name_label"));
            mItemTypeLabel = (TextView) findViewById(mFSAResourceData.getExtraResourceID("R.id.item_type_label"));
            mLabelLabel = (TextView) findViewById(mFSAResourceData.getExtraResourceID("R.id.label_label"));
            mContentLabel = (TextView) findViewById(mFSAResourceData.getExtraResourceID("R.id.content_label"));
            mIsDefaultLabel = (TextView) findViewById(mFSAResourceData.getExtraResourceID("R.id.is_default_label"));
            mFullFilterLabel = (TextView) findViewById(mFSAResourceData.getExtraResourceID("R.id.full_filter_label"));

            mLabelDescLabel = (TextView) findViewById(mFSAResourceData.getExtraResourceID("R.id.label_description_label"));
            mLabelDesc = (TextView) findViewById(mFSAResourceData.getExtraResourceID("R.id.label_description"));
            mContentDescLabel = (TextView) findViewById(mFSAResourceData.getExtraResourceID("R.id.content_description_label"));
            mContentDesc = (TextView) findViewById(mFSAResourceData.getExtraResourceID("R.id.content_description"));

            mItemNameLabel.setText(AndroidResourceHelper.getMessage("Name"));
            mItemTypeLabel.setText(AndroidResourceHelper.getMessage("ItemType"));
            mLabelLabel.setText(AndroidResourceHelper.getMessage("Label"));
            mContentLabel.setText(AndroidResourceHelper.getMessage("Content"));
            mIsDefaultLabel.setText(AndroidResourceHelper.getMessage("Default"));
            mFullFilterLabel.setText(AndroidResourceHelper.getMessage("FullFilter"));

            String itemTypeQuery = "select mm_message_def_view.message_text, metrix_code_table.code_value from metrix_code_table"
                    + " join mm_message_def_view on metrix_code_table.message_id = mm_message_def_view.message_id and mm_message_def_view.message_type = 'CODE'"
                    + " where metrix_code_table.code_name = 'MM_FILTER_SORT_ITEM_TYPE'"
                    + " order by mm_message_def_view.message_text asc";
            MetrixControlAssistant.populateSpinnerFromQuery(this, mItemType, itemTypeQuery, true);
            mItemType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    reactToSelectedItemType();
                }

                public void onNothingSelected(AdapterView<?> parent) {
                    reactToSelectedItemType();
                }
            });
            mFullFilterLabel.setVisibility(View.GONE);
            mFullFilter.setVisibility(View.GONE);

            mLabel.setOnFocusChangeListener(this);
            mLabel.addTextChangedListener(new MessageTextWatcher("MM_FILTER_SORT", mLabelDescLabel, mLabelDesc));
            mLabelDescLabel.setVisibility(View.GONE);
            mLabelDesc.setVisibility(View.GONE);

            mContentButton.setOnClickListener(this);
            mContent.addTextChangedListener(new ClientScriptOrLiteralTextWatcher(mContentDescLabel, mContentDesc));
            mContentDescLabel.setVisibility(View.GONE);
            mContentDesc.setVisibility(View.GONE);

            String itemTypeNav = getIntent().getStringExtra("filterSortCodeValue");
            MetrixControlAssistant.setValue(mItemType, itemTypeNav);
        } catch (Exception e) {
            LogManager.getInstance().error(e);
        }
    }

    @Override
    public void onClick(View v) {
        super.onClick(v);

        int viewId = v.getId();
        if (viewId == mFSAResourceData.getExtraResourceID("R.id.save")) {
            try {
                // Make sure all relevant fields have a value before allowing save
                String itemName = MetrixControlAssistant.getValue(mItemName);
                String itemType = "";
                if (mItemType.getSelectedItem() != null)
                    itemType = MetrixControlAssistant.getValue(mItemType);

                boolean allNecessaryFieldsPopulated = !MetrixStringHelper.isNullOrEmpty(itemName) && !MetrixStringHelper.isNullOrEmpty(itemType);
                boolean isUniqueName = true;
                if (allNecessaryFieldsPopulated) {
                    int repeatCount = MetrixDatabaseManager.getCount("mm_filter_sort_item", String.format("item_name = '%1$s' and screen_id = %2$s and item_type = '%3$s'", itemName, mScreenId, itemType));
                    isUniqueName = (repeatCount <= 0);
                }

                if (!allNecessaryFieldsPopulated || !isUniqueName) {
                    Toast.makeText(this, AndroidResourceHelper.getMessage("AddFilterSortError"), Toast.LENGTH_LONG).show();
                    return;
                }

                mAddFilterSortDialog = new AlertDialog.Builder(this).create();
                mAddFilterSortDialog.setMessage(AndroidResourceHelper.getMessage("AddFilterSortConfirm"));
                mAddFilterSortDialog.setButton(DialogInterface.BUTTON_POSITIVE, AndroidResourceHelper.getMessage("Yes"), addFilterSortListener);
                mAddFilterSortDialog.setButton(DialogInterface.BUTTON_NEGATIVE, AndroidResourceHelper.getMessage("No"), addFilterSortListener);
                mAddFilterSortDialog.show();
            } catch (Exception e) {
                LogManager.getInstance(this).error(e);
            }
        } else if (viewId == mFSAResourceData.getExtraResourceID("R.id.content_button")) {
            LinearLayout rowLayout = (LinearLayout) v.getParent();
            doClientScriptSelection(mFSAResourceData.getExtraResourceID("R.id.content"), rowLayout);
        }
    }

    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        if (hasFocus) {
            LinearLayout rowLayout = (LinearLayout) v.getParent();
            int viewId = v.getId();
            if (viewId == mFSAResourceData.getExtraResourceID("R.id.label"))
                doFilterSortMessageSelection(viewId, rowLayout);
        }
    }

    private void reactToSelectedItemType() {
        try {
            String selectedType = MetrixControlAssistant.getValue(mItemType);
            if (!MetrixStringHelper.isNullOrEmpty(selectedType) && MetrixStringHelper.valueIsEqual(selectedType, "FILTER")) {
                mFullFilterLabel.setVisibility(View.VISIBLE);
                mFullFilter.setVisibility(View.VISIBLE);
            } else {
                mFullFilter.setChecked(false);
                mFullFilterLabel.setVisibility(View.GONE);
                mFullFilter.setVisibility(View.GONE);
            }
        } catch (Exception e) {
            LogManager.getInstance().error(e);
        }
    }

    private void doFilterSortMessageSelection(int viewToPopulateId, LinearLayout parentLayout) {
        MetrixLookupDef lookupDef = new MetrixLookupDef("mm_message_def_view");
        lookupDef.columnNames.add(new MetrixLookupColumnDef("mm_message_def_view.message_id", viewToPopulateId));
        lookupDef.columnNames.add(new MetrixLookupColumnDef("mm_message_def_view.message_text"));
        lookupDef.filters.add(new MetrixLookupFilterDef("mm_message_def_view.message_type", "=", "MM_FILTER_SORT"));

        Intent intent = MetrixActivityHelper.createActivityIntent(this, "com.metrix.metrixmobile.system", "Lookup");
        intent.putExtra("NoOptionsMenu", true);
        MetrixPublicCache.instance.addItem("lookupDef", lookupDef);
        MetrixPublicCache.instance.addItem("lookupParentLayout", parentLayout);
        startActivityForResult(intent, 2727);
    }

    DialogInterface.OnClickListener addFilterSortListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            switch (which) {
                case DialogInterface.BUTTON_POSITIVE:	// Yes
                    // wire up Yes button to call perform_generate_mobile_filter_sort_item
                    if (SettingsHelper.getSyncPause(mCurrentActivity)) {
                        SyncPauseAlertDialog syncPauseAlertDialog = MetrixDialogAssistant.showSyncPauseAlertDialog(mCurrentActivity);
                        if (syncPauseAlertDialog != null) {
                            syncPauseAlertDialog.setOnSyncPauseAlertButtonClickListner(new SyncPauseAlertDialog.OnSyncPauseAlertButtonClickListner() {
                                @Override
                                public void OnSyncPauseAlertButtonClick(DialogInterface dialog, int which) { startAddFilterSortItemListener(); }
                            });
                        }
                    }
                    else
                        startAddFilterSortItemListener();
                    break;

                case DialogInterface.BUTTON_NEGATIVE:	// No (do nothing)
                    break;
            }
        }
    };

    private void startAddFilterSortItemListener() {
        Thread thread = new Thread(new Runnable(){
            @Override
            public void run() {
                MobileApplication.stopSync(mCurrentActivity);
                MobileApplication.startSync(mCurrentActivity, 5);

                if (doFilterSortItemAddition() == false) {
                    MobileApplication.stopSync(mCurrentActivity);
                    MobileApplication.startSync(mCurrentActivity);
                    MetrixDesignerFilterSortAddActivity.this.runOnUiThread(new Runnable() {
                        public void run() {
                            Toast.makeText(mCurrentActivity, AndroidResourceHelper.getMessage("MobileServiceUnavailable"), Toast.LENGTH_LONG).show();
                            if (mAddFilterSortDialog != null) {
                                mAddFilterSortDialog.dismiss();
                            }
                        }
                    });
                    return;
                }

                if (mAddFilterSortDialog != null) {
                    mAddFilterSortDialog.dismiss();
                }

                // start waiting dialog on-screen
                mUIHelper = new MetrixUIHelper(mCurrentActivity);
                mUIHelper.showLoadingDialog(AndroidResourceHelper.getMessage("AddFilterSortInProgress"));
            }
        });

        thread.start();
    }

    public static boolean doFilterSortItemAddition() {
        MetrixRemoteExecutor remote = new MetrixRemoteExecutor(MobileApplication.getAppContext(), 5);
        String baseUrl = MetrixStringHelper.getString(MetrixPublicCache.instance.getItem("MetrixServiceAddress"));

        if (ping(baseUrl, remote) == false)
            return false;

        Hashtable<String, String> params = new Hashtable<String, String>();
        int device_id = SettingsHelper.getDeviceSequence(MobileApplication.getAppContext());

        try {
            String itemName = MetrixControlAssistant.getValue(mItemName);
            mSelectedItemType = MetrixControlAssistant.getValue(mItemType);
            String label = MetrixControlAssistant.getValue(mLabel);
            String content = MetrixControlAssistant.getValue(mContent);
            String isDefault = MetrixControlAssistant.getValue(mIsDefault);
            String fullFilter = MetrixControlAssistant.getValue(mFullFilter);

            params.put("screen_id", mScreenId);
            params.put("item_name", itemName);
            params.put("item_type", mSelectedItemType);
            params.put("device_sequence", String.valueOf(device_id));
            if (!MetrixStringHelper.isNullOrEmpty(label))
                params.put("label", label);
            if (!MetrixStringHelper.isNullOrEmpty(content))
                params.put("content", content);
            if (!MetrixStringHelper.isNullOrEmpty(isDefault))
                params.put("default", isDefault);
            if (!MetrixStringHelper.isNullOrEmpty(fullFilter))
                params.put("full_filter", fullFilter);
            params.put("created_revision_id", MetrixCurrentKeysHelper.getKeyValue("mm_revision", "revision_id"));

            if (MetrixStringHelper.valueIsEqual(isDefault, "Y"))
                turnOffOtherFilterSortDefaults(mScreenId, mSelectedItemType);

            MetrixPerformMessage performGMFSI = new MetrixPerformMessage("perform_generate_mobile_filter_sort_item", params);
            performGMFSI.save();
        } catch (Exception ex) {
            LogManager.getInstance().error(ex);
            return false;
        }

        return true;
    }
}