package com.metrix.architecture.designer;

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
import com.metrix.architecture.utilities.SyncPauseAlertDialog;

import java.util.Hashtable;

public class MetrixDesignerGlobalMenuAddActivity extends MetrixDesignerActivity implements OnFocusChangeListener {
    private Button mSaveButton;
    private static CheckBox mHideIfZero;
    private static Spinner mScreenId, mIconName;
    private static EditText mItemName, mLabel, mCountScript, mTapEvent, mDesc;
    private static TextView mItemNameLabel, mLabelLabel, mCountScriptLabel, mScreenIdLabel, mTapEventLabel, mIconNameLabel, mHideIfZeroLabel, mDescriptionLabel;
    private ImageView mIconNamePreview;
    private TextView mHeadingLabel, mEmphasis, mLabelDescLabel, mLabelDesc, mCountScriptDescLabel, mCountScriptDesc, mTapEventDescLabel, mTapEventDesc;
    private AlertDialog mAddMenuItemDialog;
    protected MetrixUIHelper mUIHelper = new MetrixUIHelper(this);
    private MetrixDesignerResourceData mMenuItemAddResourceData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mMenuItemAddResourceData = (MetrixDesignerResourceData) MetrixPublicCache.instance.getItem("MetrixDesignerGlobalMenuAddActivityResourceData");

        setContentView(mMenuItemAddResourceData.LayoutResourceID);
        populateScreen();
    }

    @Override
    public void onStart() {
        super.onStart();

        helpText = mMenuItemAddResourceData.HelpTextString;

        mHeadingText = getIntent().getStringExtra("headingText");
        if (mActionBarTitle != null) {
            mActionBarTitle.setText(mHeadingText);
        }

        mHeadingLabel = (TextView) findViewById(mMenuItemAddResourceData.getExtraResourceID("R.id.zzmd_menu_item_add_label"));
        mHeadingLabel.setText(AndroidResourceHelper.getMessage("AddGlobalMenuItemLabel"));
        mEmphasis = (TextView) findViewById(mMenuItemAddResourceData.getExtraResourceID("R.id.zzmd_menu_item_add_emphasis"));
        mEmphasis.setText(AndroidResourceHelper.getMessage("AddGlobalMenuItemTip"));

        mSaveButton = (Button) findViewById(mMenuItemAddResourceData.getExtraResourceID("R.id.save"));
        mSaveButton.setText(AndroidResourceHelper.getMessage("Save"));
        mSaveButton.setOnClickListener(this);
    }

    @Override
    protected void bindService() {
        bindService(new Intent(MetrixDesignerGlobalMenuAddActivity.this, MetrixIntentService.class), mConnection, Context.BIND_AUTO_CREATE);
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
                    if (MetrixStringHelper.valueIsEqual(message, "{\"END_GMMI\":null}")) {
                        MobileApplication.stopSync(mCurrentActivity);
                        MobileApplication.startSync(mCurrentActivity);
                        mUIHelper.dismissLoadingDialog();
                        Intent intent = MetrixActivityHelper.createActivityIntent(MetrixDesignerGlobalMenuAddActivity.this, MetrixDesignerGlobalMenuEnablingActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        // re-getting this data, as the intent has the bad habit of using previous extras
                        String designName = MetrixDatabaseManager.getFieldStringValue("mm_design", "name", "design_id = " + MetrixCurrentKeysHelper.getKeyValue("mm_design", "design_id"));
                        String revNumber = MetrixDatabaseManager.getFieldStringValue("mm_revision", "revision_number", "revision_id = " + MetrixCurrentKeysHelper.getKeyValue("mm_revision", "revision_id"));
                        intent.putExtra("headingText", String.format("%1$s (%2$s %3$s)", designName, AndroidResourceHelper.getMessage("Rev"), revNumber));
                        MetrixActivityHelper.startNewActivity(MetrixDesignerGlobalMenuAddActivity.this, intent);
                    } else {
                        processPostListener(activityType, message);
                    }
                }
            });
        }
    };

    private void populateScreen() {
        mHideIfZero = (CheckBox) findViewById(mMenuItemAddResourceData.getExtraResourceID("R.id.hide_if_zero"));
        mScreenId = (Spinner) findViewById(mMenuItemAddResourceData.getExtraResourceID("R.id.screen_id"));
        mIconName = (Spinner) findViewById(mMenuItemAddResourceData.getExtraResourceID("R.id.icon_name"));
        mItemName = (EditText) findViewById(mMenuItemAddResourceData.getExtraResourceID("R.id.item_name"));
        mLabel = (EditText) findViewById(mMenuItemAddResourceData.getExtraResourceID("R.id.label"));
        mCountScript = (EditText) findViewById(mMenuItemAddResourceData.getExtraResourceID("R.id.count_script"));
        mTapEvent = (EditText) findViewById(mMenuItemAddResourceData.getExtraResourceID("R.id.tap_event"));
        mDesc = (EditText) findViewById(mMenuItemAddResourceData.getExtraResourceID("R.id.description"));

        mItemNameLabel = (TextView) findViewById(mMenuItemAddResourceData.getExtraResourceID("R.id.item_name_label"));
        mLabelLabel = (TextView) findViewById(mMenuItemAddResourceData.getExtraResourceID("R.id.label_label"));
        mCountScriptLabel = (TextView) findViewById(mMenuItemAddResourceData.getExtraResourceID("R.id.count_script_label"));
        mScreenIdLabel = (TextView) findViewById(mMenuItemAddResourceData.getExtraResourceID("R.id.screen_id_label"));
        mTapEventLabel = (TextView) findViewById(mMenuItemAddResourceData.getExtraResourceID("R.id.tap_event_label"));
        mIconNameLabel = (TextView) findViewById(mMenuItemAddResourceData.getExtraResourceID("R.id.icon_name_label"));
        mHideIfZeroLabel = (TextView) findViewById(mMenuItemAddResourceData.getExtraResourceID("R.id.hide_if_zero_label"));
        mDescriptionLabel = (TextView) findViewById(mMenuItemAddResourceData.getExtraResourceID("R.id.description_label"));

        mLabelDescLabel = (TextView) findViewById(mMenuItemAddResourceData.getExtraResourceID("R.id.label_description_label"));
        mLabelDesc = (TextView) findViewById(mMenuItemAddResourceData.getExtraResourceID("R.id.label_description"));
        mCountScriptDescLabel = (TextView) findViewById(mMenuItemAddResourceData.getExtraResourceID("R.id.count_script_description_label"));
        mCountScriptDesc = (TextView) findViewById(mMenuItemAddResourceData.getExtraResourceID("R.id.count_script_description"));
        mTapEventDescLabel = (TextView) findViewById(mMenuItemAddResourceData.getExtraResourceID("R.id.tap_event_description_label"));
        mTapEventDesc = (TextView) findViewById(mMenuItemAddResourceData.getExtraResourceID("R.id.tap_event_description"));

        mIconNamePreview = (ImageView) findViewById(mMenuItemAddResourceData.getExtraResourceID("R.id.icon_name_preview"));

        mItemNameLabel.setText(AndroidResourceHelper.getMessage("Name"));
        mLabelLabel.setText(AndroidResourceHelper.getMessage("Label"));
        mCountScriptLabel.setText(AndroidResourceHelper.getMessage("CountScript"));
        mScreenIdLabel.setText(AndroidResourceHelper.getMessage("Screen"));
        mTapEventLabel.setText(AndroidResourceHelper.getMessage("TapEvent"));
        mIconNameLabel.setText(AndroidResourceHelper.getMessage("IconName"));
        mHideIfZeroLabel.setText(AndroidResourceHelper.getMessage("HideIfZero"));
        mDescriptionLabel.setText(AndroidResourceHelper.getMessage("Description"));

        String screenIdQuery = "select mm_screen.screen_name, mm_screen.screen_id from mm_screen"
                + " where tab_parent_id is null and screen_type not in ('ATTACHMENT_API_CARD', 'ATTACHMENT_API_LIST')"
                + " and revision_id = " + MetrixCurrentKeysHelper.getKeyValue("mm_revision", "revision_id")
                + " and screen_id not in (select linked_screen_id from mm_screen where linked_screen_id is not null and screen_id in (select screen_id from mm_workflow_screen))"
                + " and screen_id not in (select screen_id from mm_workflow_screen where screen_id is not null)"
                + " and screen_id not in (select screen_id from mm_menu_item where screen_id is not null)"
                + " order by mm_screen.screen_name asc";
        MetrixControlAssistant.populateSpinnerFromQuery(this, mScreenId, screenIdQuery, true);
        mScreenId.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) { reactToSelectedScreen(); }
            public void onNothingSelected(AdapterView<?> parent) { reactToSelectedScreen(); }
        });

        String iconNameQuery = "select mm_message_def_view.message_text, metrix_code_table.code_value from metrix_code_table"
            + " join mm_message_def_view on metrix_code_table.message_id = mm_message_def_view.message_id and mm_message_def_view.message_type = 'CODE'"
            + " where metrix_code_table.code_name = 'MM_MENU_ITEM_ICONS'"
            + " order by mm_message_def_view.message_text asc";
        MetrixControlAssistant.populateSpinnerFromQuery(this, mIconName, iconNameQuery, true);
        mIconName.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) { reactToSelectedIconName(); }
            public void onNothingSelected(AdapterView<?> parent) { reactToSelectedIconName(); }
        });

        mLabel.setOnFocusChangeListener(this);
        mLabel.addTextChangedListener(new MessageTextWatcher("MM_MENU_LABEL", mLabelDescLabel, mLabelDesc));
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
    }

    @Override
    public void onClick(View v) {
        super.onClick(v);

        int viewId = v.getId();
        if (viewId == mMenuItemAddResourceData.getExtraResourceID("R.id.save")) {
            try {
                // validate that Item Name is filled in and is unique for Global Menu items in this revision
                String itemName = MetrixControlAssistant.getValue(mItemName);
                boolean errorEncountered = false;
                if (MetrixStringHelper.isNullOrEmpty(itemName))
                    errorEncountered = true;
                else {
                    int itemCount = MetrixDatabaseManager.getCount("mm_menu_item", String.format("revision_id = %1$s and item_name = '%2$s'", MetrixCurrentKeysHelper.getKeyValue("mm_revision", "revision_id"), itemName));
                    if (itemCount > 0)
                        errorEncountered = true;
                }

                if (errorEncountered) {
                    Toast.makeText(this, AndroidResourceHelper.getMessage("AddGlobalMenuItemNameError"), Toast.LENGTH_LONG).show();
                    return;
                }

                mAddMenuItemDialog = new AlertDialog.Builder(this).create();
                mAddMenuItemDialog.setMessage(AndroidResourceHelper.getMessage("AddGlobalMenuItemConfirm"));
                mAddMenuItemDialog.setButton(DialogInterface.BUTTON_POSITIVE, AndroidResourceHelper.getMessage("Yes"), addGlobalMenuItemListener);
                mAddMenuItemDialog.setButton(DialogInterface.BUTTON_NEGATIVE, AndroidResourceHelper.getMessage("No"), addGlobalMenuItemListener);
                mAddMenuItemDialog.show();
            } catch (Exception e) {
                LogManager.getInstance(this).error(e);
            }
        }
    }

    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        if (hasFocus) {
            LinearLayout rowLayout = (LinearLayout) v.getParent();
            int viewId = v.getId();
            if (viewId == mMenuItemAddResourceData.getExtraResourceID("R.id.count_script")
                    || viewId == mMenuItemAddResourceData.getExtraResourceID("R.id.tap_event"))
                doClientScriptSelection(viewId, rowLayout);
            else if (viewId == mMenuItemAddResourceData.getExtraResourceID("R.id.label"))
                doMenuLabelMessageSelection(viewId, rowLayout);
        }
    }

    private void reactToSelectedIconName() {
        try {
            String selectedIconName = MetrixControlAssistant.getValue(mIconName);
            if (!MetrixStringHelper.isNullOrEmpty(selectedIconName)) {
                String drawableResString = MetrixGlobalMenuManager.iconMap.get(selectedIconName);
                mIconNamePreview.setImageResource(mMenuItemAddResourceData.getExtraResourceID(drawableResString));
            } else {
                mIconNamePreview.setImageResource(mMenuItemAddResourceData.getExtraResourceID("R.drawable.no_image24x24"));
            }
        } catch (Exception e) {
            LogManager.getInstance().error(e);
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

    private void doMenuLabelMessageSelection(int viewToPopulateId, LinearLayout parentLayout) {
        MetrixLookupDef lookupDef = new MetrixLookupDef("mm_message_def_view");
        lookupDef.columnNames.add(new MetrixLookupColumnDef("mm_message_def_view.message_id", viewToPopulateId));
        lookupDef.columnNames.add(new MetrixLookupColumnDef("mm_message_def_view.message_text"));
        lookupDef.filters.add(new MetrixLookupFilterDef("mm_message_def_view.message_type", "=", "MM_MENU_LABEL"));

        Intent intent = MetrixActivityHelper.createActivityIntent(this, "com.metrix.metrixmobile.system", "Lookup");
        intent.putExtra("NoOptionsMenu", true);
        MetrixPublicCache.instance.addItem("lookupDef", lookupDef);
        MetrixPublicCache.instance.addItem("lookupParentLayout", parentLayout);
        startActivityForResult(intent, 2727);
    }

    DialogInterface.OnClickListener addGlobalMenuItemListener = new DialogInterface.OnClickListener() {
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
                                public void OnSyncPauseAlertButtonClick(DialogInterface dialog, int which) { startAddGlobalMenuItemListener(); }
                            });
                        }
                    }
                    else
                        startAddGlobalMenuItemListener();
                    break;

                case DialogInterface.BUTTON_NEGATIVE:	// No (do nothing)
                    break;
            }
        }
    };

    private void startAddGlobalMenuItemListener() {
        Thread thread = new Thread(new Runnable(){
            @Override
            public void run() {
                MobileApplication.stopSync(mCurrentActivity);
                MobileApplication.startSync(mCurrentActivity, 5);

                if (doGlobalMenuItemAddition() == false) {
                    MobileApplication.stopSync(mCurrentActivity);
                    MobileApplication.startSync(mCurrentActivity);
                    MetrixDesignerGlobalMenuAddActivity.this.runOnUiThread(new Runnable() {
                        public void run() {
                            Toast.makeText(mCurrentActivity, AndroidResourceHelper.getMessage("MobileServiceUnavailable"), Toast.LENGTH_LONG).show();
                            if (mAddMenuItemDialog != null) {
                                mAddMenuItemDialog.dismiss();
                            }
                        }
                    });
                    return;
                }

                if (mAddMenuItemDialog != null) {
                    mAddMenuItemDialog.dismiss();
                }

                // start waiting dialog on-screen
                mUIHelper = new MetrixUIHelper(mCurrentActivity);
                mUIHelper.showLoadingDialog(AndroidResourceHelper.getMessage("AddGlobalMenuItemInProgress"));
            }
        });

        thread.start();
    }

    public static boolean doGlobalMenuItemAddition() {
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
            String iconName = MetrixControlAssistant.getValue(mIconName);
            String hideIfZero = MetrixControlAssistant.getValue(mHideIfZero);

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
            if (!MetrixStringHelper.isNullOrEmpty(iconName))
                params.put("icon_name", iconName);
            if (!MetrixStringHelper.isNullOrEmpty(hideIfZero))
                params.put("hide_if_zero", hideIfZero);

            MetrixPerformMessage performGMMI = new MetrixPerformMessage("perform_generate_mobile_menu_item", params);
            performGMMI.save();
        } catch (Exception ex) {
            LogManager.getInstance().error(ex);
            return false;
        }

        return true;
    }
}
