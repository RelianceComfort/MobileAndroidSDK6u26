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
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.widget.AdapterView;
import android.widget.Button;
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
import com.metrix.architecture.metadata.MetrixLookupColumnDef;
import com.metrix.architecture.metadata.MetrixLookupDef;
import com.metrix.architecture.metadata.MetrixLookupFilterDef;
import com.metrix.architecture.metadata.MetrixPerformMessage;
import com.metrix.architecture.services.IPostMonitor;
import com.metrix.architecture.services.MetrixIntentService;
import com.metrix.architecture.services.MetrixRemoteExecutor;
import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.architecture.utilities.Global;
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

public class MetrixDesignerHomeMenuAddActivity extends MetrixDesignerActivity implements OnFocusChangeListener {
    private Button mImageIdSelect, mImageIdClear, mSaveButton;
    private static Spinner mScreenId;
    private static EditText mItemName, mLabel, mCountScript, mTapEvent, mDesc;
    private static TextView mItemNameLabel, mLabelLabel, mCountScriptLabel, mScreenIdLabel, mTapEventLabel, mImageIdLabel, mImageId, mDescriptionLabel;
    private ImageView mImageIdPreview;
    private TextView mHeadingLabel, mEmphasis, mLabelDescLabel, mLabelDesc, mCountScriptDescLabel, mCountScriptDesc, mTapEventDescLabel, mTapEventDesc;
    private AlertDialog mAddHomeItemDialog;
    protected MetrixUIHelper mUIHelper = new MetrixUIHelper(this);
    private MetrixDesignerResourceData mHomeItemAddResourceData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mHomeItemAddResourceData = (MetrixDesignerResourceData) MetrixPublicCache.instance.getItem("MetrixDesignerHomeMenuAddActivityResourceData");

        setContentView(mHomeItemAddResourceData.LayoutResourceID);
        populateScreen();
    }

    @Override
    public void onStart() {
        super.onStart();

        helpText = mHomeItemAddResourceData.HelpTextString;

        mHeadingText = getIntent().getStringExtra("headingText");
        if (mActionBarTitle != null) {
            mActionBarTitle.setText(mHeadingText);
        }

        mHeadingLabel = (TextView) findViewById(mHomeItemAddResourceData.getExtraResourceID("R.id.zzmd_home_item_add_label"));
        mHeadingLabel.setText(AndroidResourceHelper.getMessage("AddHomeMenuItemLabel"));
        mEmphasis = (TextView) findViewById(mHomeItemAddResourceData.getExtraResourceID("R.id.zzmd_home_item_add_emphasis"));
        mEmphasis.setText(AndroidResourceHelper.getMessage("AddHomeMenuItemTip"));

        mSaveButton = (Button) findViewById(mHomeItemAddResourceData.getExtraResourceID("R.id.save"));
        mSaveButton.setText(AndroidResourceHelper.getMessage("Save"));
        mSaveButton.setOnClickListener(this);
    }

    @Override
    protected void bindService() {
        bindService(new Intent(MetrixDesignerHomeMenuAddActivity.this, MetrixIntentService.class), mConnection, Context.BIND_AUTO_CREATE);
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
                    if (MetrixStringHelper.valueIsEqual(message, "{\"END_GMHI\":null}")) {
                        MobileApplication.stopSync(mCurrentActivity);
                        MobileApplication.startSync(mCurrentActivity);
                        mUIHelper.dismissLoadingDialog();
                        Intent intent = MetrixActivityHelper.createActivityIntent(MetrixDesignerHomeMenuAddActivity.this, MetrixDesignerHomeMenuEnablingActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        // re-getting this data, as the intent has the bad habit of using previous extras
                        String designName = MetrixDatabaseManager.getFieldStringValue("mm_design", "name", "design_id = " + MetrixCurrentKeysHelper.getKeyValue("mm_design", "design_id"));
                        String revNumber = MetrixDatabaseManager.getFieldStringValue("mm_revision", "revision_number", "revision_id = " + MetrixCurrentKeysHelper.getKeyValue("mm_revision", "revision_id"));
                        intent.putExtra("headingText", String.format("%1$s (%2$s %3$s)", designName, AndroidResourceHelper.getMessage("Rev"), revNumber));
                        MetrixActivityHelper.startNewActivity(MetrixDesignerHomeMenuAddActivity.this, intent);
                    } else {
                        processPostListener(activityType, message);
                    }
                }
            });
        }
    };

    private void populateScreen() {
        mScreenId = (Spinner) findViewById(mHomeItemAddResourceData.getExtraResourceID("R.id.screen_id"));
        mItemName = (EditText) findViewById(mHomeItemAddResourceData.getExtraResourceID("R.id.item_name"));
        mLabel = (EditText) findViewById(mHomeItemAddResourceData.getExtraResourceID("R.id.label"));
        mCountScript = (EditText) findViewById(mHomeItemAddResourceData.getExtraResourceID("R.id.count_script"));
        mTapEvent = (EditText) findViewById(mHomeItemAddResourceData.getExtraResourceID("R.id.tap_event"));
        mDesc = (EditText) findViewById(mHomeItemAddResourceData.getExtraResourceID("R.id.description"));

        mItemNameLabel = (TextView) findViewById(mHomeItemAddResourceData.getExtraResourceID("R.id.item_name_label"));
        mLabelLabel = (TextView) findViewById(mHomeItemAddResourceData.getExtraResourceID("R.id.label_label"));
        mCountScriptLabel = (TextView) findViewById(mHomeItemAddResourceData.getExtraResourceID("R.id.count_script_label"));
        mScreenIdLabel = (TextView) findViewById(mHomeItemAddResourceData.getExtraResourceID("R.id.screen_id_label"));
        mTapEventLabel = (TextView) findViewById(mHomeItemAddResourceData.getExtraResourceID("R.id.tap_event_label"));
        mImageIdLabel = (TextView) findViewById(mHomeItemAddResourceData.getExtraResourceID("R.id.image_id_label"));
        mImageId = (TextView) findViewById(mHomeItemAddResourceData.getExtraResourceID("R.id.image_id"));
        mDescriptionLabel = (TextView) findViewById(mHomeItemAddResourceData.getExtraResourceID("R.id.description_label"));

        mImageIdSelect = (Button) findViewById(mHomeItemAddResourceData.getExtraResourceID("R.id.image_id_select"));
        mImageIdClear = (Button) findViewById(mHomeItemAddResourceData.getExtraResourceID("R.id.image_id_clear"));
        mImageIdPreview = (ImageView) findViewById(mHomeItemAddResourceData.getExtraResourceID("R.id.image_id_preview"));

        mLabelDescLabel = (TextView) findViewById(mHomeItemAddResourceData.getExtraResourceID("R.id.label_description_label"));
        mLabelDesc = (TextView) findViewById(mHomeItemAddResourceData.getExtraResourceID("R.id.label_description"));
        mCountScriptDescLabel = (TextView) findViewById(mHomeItemAddResourceData.getExtraResourceID("R.id.count_script_description_label"));
        mCountScriptDesc = (TextView) findViewById(mHomeItemAddResourceData.getExtraResourceID("R.id.count_script_description"));
        mTapEventDescLabel = (TextView) findViewById(mHomeItemAddResourceData.getExtraResourceID("R.id.tap_event_description_label"));
        mTapEventDesc = (TextView) findViewById(mHomeItemAddResourceData.getExtraResourceID("R.id.tap_event_description"));

        mItemNameLabel.setText(AndroidResourceHelper.getMessage("Name"));
        mLabelLabel.setText(AndroidResourceHelper.getMessage("Label"));
        mCountScriptLabel.setText(AndroidResourceHelper.getMessage("CountScript"));
        mScreenIdLabel.setText(AndroidResourceHelper.getMessage("Screen"));
        mTapEventLabel.setText(AndroidResourceHelper.getMessage("TapEvent"));
        mImageIdLabel.setText(AndroidResourceHelper.getMessage("Image"));
        mImageIdSelect.setText(AndroidResourceHelper.getMessage("Select"));
        mImageIdClear.setText(AndroidResourceHelper.getMessage("Clear"));
        mDescriptionLabel.setText(AndroidResourceHelper.getMessage("Description"));

        String screenIdQuery = "select mm_screen.screen_name, mm_screen.screen_id from mm_screen"
            + " where tab_parent_id is null and screen_type not in ('ATTACHMENT_API_CARD', 'ATTACHMENT_API_LIST')"
            + " and revision_id = " + MetrixCurrentKeysHelper.getKeyValue("mm_revision", "revision_id")
            + " and screen_id not in (select linked_screen_id from mm_screen where linked_screen_id is not null and screen_id in (select screen_id from mm_workflow_screen))"
            + " and screen_id not in (select screen_id from mm_workflow_screen where screen_id is not null)"
            + " and screen_id not in (select screen_id from mm_home_item where screen_id is not null)"
            + " order by mm_screen.screen_name asc";
        MetrixControlAssistant.populateSpinnerFromQuery(this, mScreenId, screenIdQuery, true);
        mScreenId.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) { reactToSelectedScreen(); }
            public void onNothingSelected(AdapterView<?> parent) { reactToSelectedScreen(); }
        });

        mLabel.setOnFocusChangeListener(this);
        mLabel.addTextChangedListener(new MessageTextWatcher("MM_HOME_LABEL", mLabelDescLabel, mLabelDesc));
        mLabelDescLabel.setVisibility(View.GONE);
        mLabelDesc.setVisibility(View.GONE);
        mCountScript.setOnFocusChangeListener(this);
        mCountScript.addTextChangedListener(new ClientScriptTextWatcher(mCountScriptDescLabel, mCountScriptDesc));
        mCountScriptDescLabel.setVisibility(View.GONE);
        mCountScriptDesc.setVisibility(View.GONE);
        mTapEvent.setOnFocusChangeListener(this);
        mTapEvent.addTextChangedListener(new ClientScriptTextWatcher(mTapEventDescLabel, mTapEventDesc));
        mTapEvent.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) { reactToSelectedTapEvent(); }
        });
        mTapEventDescLabel.setVisibility(View.GONE);
        mTapEventDesc.setVisibility(View.GONE);

        mImageIdSelect.setOnClickListener(this);
        mImageIdClear.setOnClickListener(this);
        mImageId.addTextChangedListener(new ImageTextWatcher(mImageIdPreview, mHomeItemAddResourceData.getExtraResourceID("R.drawable.no_image80x80")));
    }

    @Override
    public void onClick(View v) {
        super.onClick(v);

        int viewId = v.getId();
        if (viewId == mHomeItemAddResourceData.getExtraResourceID("R.id.save")) {
            try {
                // validate that Item Name is filled in and is unique for Home Menu items in this revision
                String itemName = MetrixControlAssistant.getValue(mItemName);
                boolean errorEncountered = false;
                if (MetrixStringHelper.isNullOrEmpty(itemName))
                    errorEncountered = true;
                else {
                    int itemCount = MetrixDatabaseManager.getCount("mm_home_item", String.format("revision_id = %1$s and item_name = '%2$s'", MetrixCurrentKeysHelper.getKeyValue("mm_revision", "revision_id"), itemName));
                    if (itemCount > 0)
                        errorEncountered = true;
                }

                if (errorEncountered) {
                    Toast.makeText(this, AndroidResourceHelper.getMessage("AddHomeMenuItemNameError"), Toast.LENGTH_LONG).show();
                    return;
                }

                mAddHomeItemDialog = new AlertDialog.Builder(this).create();
                mAddHomeItemDialog.setMessage(AndroidResourceHelper.getMessage("AddHomeMenuItemConfirm"));
                mAddHomeItemDialog.setButton(DialogInterface.BUTTON_POSITIVE, AndroidResourceHelper.getMessage("Yes"), addHomeMenuItemListener);
                mAddHomeItemDialog.setButton(DialogInterface.BUTTON_NEGATIVE, AndroidResourceHelper.getMessage("No"), addHomeMenuItemListener);
                mAddHomeItemDialog.show();
            } catch (Exception e) {
                LogManager.getInstance(this).error(e);
            }
        } else if (viewId == mHomeItemAddResourceData.getExtraResourceID("R.id.image_id_select")) {
            LinearLayout layout = (LinearLayout) mImageIdSelect.getParent().getParent();
            MetrixPublicCache.instance.addItem("imagePickerTextView", mImageId);
            MetrixPublicCache.instance.addItem("imagePickerParentLayout", layout);
            Intent intent = MetrixActivityHelper.createActivityIntent(MetrixDesignerHomeMenuAddActivity.this, "com.metrix.architecture.ui.widget", "ImagePicker");
            startActivityForResult(intent, 8181);
        } else if (viewId == mHomeItemAddResourceData.getExtraResourceID("R.id.image_id_clear")) {
            mImageId.setText("");
        }
    }

    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        if (hasFocus) {
            LinearLayout rowLayout = (LinearLayout) v.getParent();
            int viewId = v.getId();
            if (viewId == mHomeItemAddResourceData.getExtraResourceID("R.id.count_script")
                || viewId == mHomeItemAddResourceData.getExtraResourceID("R.id.tap_event"))
                doClientScriptSelection(viewId, rowLayout);
            else if (viewId == mHomeItemAddResourceData.getExtraResourceID("R.id.label"))
                doHomeLabelMessageSelection(viewId, rowLayout);
        }
    }

    private void reactToSelectedScreen() {
        try {
            String selectedScreen = MetrixControlAssistant.getValue(mScreenId);
            if (!MetrixStringHelper.isNullOrEmpty(selectedScreen)) {
                MetrixControlAssistant.setValue(mTapEvent, "");
                mTapEvent.setEnabled(false);
            } else {
                mTapEvent.setEnabled(true);
            }
        } catch (Exception e) {
            LogManager.getInstance().error(e);
        }
    }

    private void reactToSelectedTapEvent() {
        try {
            String selectedTapEvent = MetrixControlAssistant.getValue(mTapEvent);
            if (!MetrixStringHelper.isNullOrEmpty(selectedTapEvent)) {
                MetrixControlAssistant.setValue(mScreenId, "");
                mScreenId.setEnabled(false);
            } else {
                mScreenId.setEnabled(true);
            }
        } catch (Exception e) {
            LogManager.getInstance().error(e);
        }
    }

    private void doHomeLabelMessageSelection(int viewToPopulateId, LinearLayout parentLayout) {
        MetrixLookupDef lookupDef = new MetrixLookupDef("mm_message_def_view");
        lookupDef.columnNames.add(new MetrixLookupColumnDef("mm_message_def_view.message_id", viewToPopulateId));
        lookupDef.columnNames.add(new MetrixLookupColumnDef("mm_message_def_view.message_text"));
        lookupDef.filters.add(new MetrixLookupFilterDef("mm_message_def_view.message_type", "=", "MM_HOME_LABEL"));

        Intent intent = MetrixActivityHelper.createActivityIntent(this, "com.metrix.metrixmobile.system", "Lookup");
        intent.putExtra("NoOptionsMenu", true);
        MetrixPublicCache.instance.addItem("lookupDef", lookupDef);
        MetrixPublicCache.instance.addItem("lookupParentLayout", parentLayout);
        startActivityForResult(intent, 2727);
    }

    DialogInterface.OnClickListener addHomeMenuItemListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            switch (which) {
                case DialogInterface.BUTTON_POSITIVE:	// Yes
                    // wire up Yes button to call perform_generate_mobile_home_item
                    if (SettingsHelper.getSyncPause(mCurrentActivity)) {
                        SyncPauseAlertDialog syncPauseAlertDialog = MetrixDialogAssistant.showSyncPauseAlertDialog(mCurrentActivity);
                        if (syncPauseAlertDialog != null) {
                            syncPauseAlertDialog.setOnSyncPauseAlertButtonClickListner(new SyncPauseAlertDialog.OnSyncPauseAlertButtonClickListner() {
                                @Override
                                public void OnSyncPauseAlertButtonClick(DialogInterface dialog, int which) { startAddHomeMenuItemListener(); }
                            });
                        }
                    }
                    else
                        startAddHomeMenuItemListener();
                    break;

                case DialogInterface.BUTTON_NEGATIVE:	// No (do nothing)
                    break;
            }
        }
    };

    private void startAddHomeMenuItemListener() {
        Thread thread = new Thread(new Runnable(){
            @Override
            public void run() {
                MobileApplication.stopSync(mCurrentActivity);
                MobileApplication.startSync(mCurrentActivity, 5);

                if (doHomeMenuItemAddition() == false) {
                    MobileApplication.stopSync(mCurrentActivity);
                    MobileApplication.startSync(mCurrentActivity);
                    MetrixDesignerHomeMenuAddActivity.this.runOnUiThread(new Runnable() {
                        public void run() {
                            Toast.makeText(mCurrentActivity, AndroidResourceHelper.getMessage("MobileServiceUnavailable"), Toast.LENGTH_LONG).show();
                            if (mAddHomeItemDialog != null) {
                                mAddHomeItemDialog.dismiss();
                            }
                        }
                    });
                    return;
                }

                if (mAddHomeItemDialog != null) {
                    mAddHomeItemDialog.dismiss();
                }

                // start waiting dialog on-screen
                mUIHelper = new MetrixUIHelper(mCurrentActivity);
                mUIHelper.showLoadingDialog(AndroidResourceHelper.getMessage("AddHomeMenuItemInProgress"));
            }
        });

        thread.start();
    }

    public static boolean doHomeMenuItemAddition() {
        MetrixRemoteExecutor remote = new MetrixRemoteExecutor(MobileApplication.getAppContext(), 5);
        String baseUrl = MetrixStringHelper.getString(MetrixPublicCache.instance.getItem("MetrixServiceAddress"));

        if (ping(baseUrl, remote) == false)
            return false;

        Hashtable<String, String> params = new Hashtable<String, String>();
        int device_id = SettingsHelper.getDeviceSequence(MobileApplication.getAppContext());

        try {
            String itemName = MetrixControlAssistant.getValue(mItemName);
            String description = MetrixControlAssistant.getValue(mDesc);
            String label = MetrixControlAssistant.getValue(mLabel);
            String countScript = MetrixControlAssistant.getValue(mCountScript);
            String screenId = MetrixControlAssistant.getValue(mScreenId);
            String tapEvent = MetrixControlAssistant.getValue(mTapEvent);
            String imageId = MetrixControlAssistant.getValue(mImageId);

            params.put("design_id", MetrixCurrentKeysHelper.getKeyValue("mm_design", "design_id"));
            params.put("revision_id", MetrixCurrentKeysHelper.getKeyValue("mm_revision", "revision_id"));
            params.put("item_name", itemName);
            params.put("device_sequence", String.valueOf(device_id));
            if (!MetrixStringHelper.isNullOrEmpty(description))
                params.put("description", description);
            if (!MetrixStringHelper.isNullOrEmpty(label))
                params.put("label", label);
            if (!MetrixStringHelper.isNullOrEmpty(countScript))
                params.put("count_script", countScript);
            if (!MetrixStringHelper.isNullOrEmpty(screenId))
                params.put("screen_id", screenId);
            if (!MetrixStringHelper.isNullOrEmpty(tapEvent))
                params.put("tap_event", tapEvent);
            if (!MetrixStringHelper.isNullOrEmpty(imageId))
                params.put("image_id", imageId);

            MetrixPerformMessage performGMHI = new MetrixPerformMessage("perform_generate_mobile_home_item", params);
            performGMHI.save();
        } catch (Exception ex) {
            LogManager.getInstance().error(ex);
            return false;
        }

        return true;
    }
}
