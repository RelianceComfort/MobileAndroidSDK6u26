package com.metrix.architecture.designer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Locale;

import org.json.JSONException;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.IBinder;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.core.content.IntentCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.metrix.architecture.actionbar.MetrixActionBarManager;
import com.metrix.architecture.actionbar.MetrixActionBarResourceData;
import com.metrix.architecture.assistants.MetrixDialogAssistant;
import com.metrix.architecture.constants.MetrixTransactionTypes;
import com.metrix.architecture.database.MetrixCursor;
import com.metrix.architecture.database.MetrixDatabaseManager;
import com.metrix.architecture.database.MobileApplication;
import com.metrix.architecture.managers.MetrixUpdateManager;
import com.metrix.architecture.metadata.MetrixLookupColumnDef;
import com.metrix.architecture.metadata.MetrixLookupDef;
import com.metrix.architecture.metadata.MetrixPerformMessage;
import com.metrix.architecture.metadata.MetrixSqlData;
import com.metrix.architecture.metadata.MetrixTableStructure;
import com.metrix.architecture.metadata.MetrixTransaction;
import com.metrix.architecture.scripting.MetrixClientScriptManager;
import com.metrix.architecture.services.IPostMonitor;
import com.metrix.architecture.services.MetrixIntentService;
import com.metrix.architecture.services.MetrixRemoteExecutor;
import com.metrix.architecture.services.MetrixServiceManager;
import com.metrix.architecture.services.MetrixSyncManager;
import com.metrix.architecture.services.RemoteMessagesHandler.HandlerException;
import com.metrix.architecture.slidingmenu.MetrixSlidingMenuAdapter;
import com.metrix.architecture.slidingmenu.MetrixSlidingMenuItem;
import com.metrix.architecture.slidingmenu.MetrixSlidingMenuManager;
import com.metrix.architecture.slidingmenu.MetrixSlidingMenuResourceData;
import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.architecture.utilities.DataField;
import com.metrix.architecture.utilities.Global.ActivityType;
import com.metrix.architecture.utilities.Global.MessageType;
import com.metrix.architecture.utilities.LogManager;
import com.metrix.architecture.utilities.MetrixActivityHelper;
import com.metrix.architecture.utilities.MetrixAttachmentHelper;
import com.metrix.architecture.utilities.MetrixCurrentKeysHelper;
import com.metrix.architecture.utilities.MetrixPublicCache;
import com.metrix.architecture.utilities.MetrixStringHelper;
import com.metrix.architecture.utilities.MetrixUIHelper;
import com.metrix.architecture.utilities.SettingsHelper;
import com.metrix.architecture.utilities.SyncPauseAlertDialog;
import com.metrix.architecture.utilities.User;

public class MetrixDesignerActivity extends AppCompatActivity implements OnClickListener, MetrixSlidingMenuAdapter.OnItemClickListener {

    protected MetrixDesignerResourceBaseData mBaseResourceData;
    protected boolean mIsBound;
    protected boolean mInitializationStarted = false;
    protected IPostMonitor service = null;
    protected AlertDialog mPreviewAlert, mRefreshScriptsAlert;
    protected String helpText;
    protected String mHeadingText;
    protected Activity mCurrentActivity = null;
    protected TextView mActionBarTitle;
    protected boolean mAllowChanges, mProcessedTargetIntent;
    protected MetrixUIHelper coreUIHelper = new MetrixUIHelper(this);

    private MetrixSlidingMenuResourceData mMetrixSlidingMenuResourceData;
    private HashMap<String, Integer> mSlidingMenuResources;

    private MetrixActionBarResourceData mMetrixActionBarResourceData;
    private HashMap<String, Integer> mActionBarResources;

    private ActionBarDrawerToggle mDrawerToggle;
    private LinearLayout mDrawerLinearLayout;
    private DrawerLayout mDrawerLayout;
    private RecyclerView mMetrixSlidingMenu;
    private ActionBar mSupportActionBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (MetrixStringHelper.isNullOrEmpty(MobileApplication.ApplicationNullIfCrashed)) {
            finish();
        }

        super.onCreate(savedInstanceState);

        mBaseResourceData = (MetrixDesignerResourceBaseData) MetrixPublicCache.instance.getItem("MetrixDesignerActivityResourceData");

        mMetrixSlidingMenuResourceData = (MetrixSlidingMenuResourceData) MetrixPublicCache.instance.getItem("MetrixSlidingMenuResourceData");
        if (mMetrixSlidingMenuResourceData != null)
            mSlidingMenuResources = (HashMap<String, Integer>) mMetrixSlidingMenuResourceData.getSlidingMenuResourceIDs();

        mMetrixActionBarResourceData = (MetrixActionBarResourceData) MetrixPublicCache.instance.getItem("MetrixActionBarResourceData");
        if (mMetrixActionBarResourceData != null)
            mActionBarResources = (HashMap<String, Integer>) mMetrixActionBarResourceData.getActionBarResourceIDs();

        mCurrentActivity = this;
        mProcessedTargetIntent = false;

        MetrixServiceManager.setup(this);
        bindService();
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (MobileApplication.serverSidePasswordChangeOccurredInBackground()) {
            MetrixDatabaseManager.executeSql("update user_credentials set hidden_chg_occurred = null");
            handleServerPasswordChange();
            return;
        }

        LogManager.getInstance(this).info("{0} onStart()", mCurrentActivity.getLocalClassName());

        mSupportActionBar = MetrixActionBarManager.getInstance().setupActionBar(this, mBaseResourceData.getActionBarLayoutID(), false);
        mActionBarTitle = MetrixActionBarManager.getInstance().setupActionBarTitle(this, mBaseResourceData.ActionBarTitleResourceID, mBaseResourceData.ActionBarTitleString, null);
        //MetrixActionBarManager.getInstance().setActionBarDefaultIcon(mBaseResourceData.getActionBarIconID(), mSupportActionBar, 24, 24);

        mDrawerLinearLayout = (LinearLayout) findViewById(mSlidingMenuResources.get("R.id.drawer"));
        mDrawerLayout = (DrawerLayout) findViewById(mSlidingMenuResources.get("R.id.drawer_layout"));
        if ((mDrawerLayout != null) && (mDrawerLinearLayout != null))
            if (mSupportActionBar != null)
                mDrawerToggle = MetrixSlidingMenuManager.getInstance().setUpSlidingDrawer(this, mSupportActionBar, mDrawerLinearLayout, mDrawerLayout, mSlidingMenuResources.get("R.drawable.ic_drawer"), mSlidingMenuResources.get("R.string.drawer_open"), mSlidingMenuResources.get("R.string.drawer_close"), null, mActionBarResources.get("R.drawable.ellipsis_vertical"));
        mMetrixSlidingMenu = findViewById(mSlidingMenuResources.get("R.id.recyclerview_sliding_menu"));
        MetrixListScreenManager.setupVerticalRecyclerView(mMetrixSlidingMenu, mSlidingMenuResources.get("R.drawable.rv_global_menu_item_divider"));

        String currentRevisionStatus = MetrixCurrentKeysHelper.getKeyValue("mm_revision", "status");
        mAllowChanges = (MetrixStringHelper.valueIsEqual(currentRevisionStatus, "PENDING"));
    }

    public void onStop() {
        super.onStop();
        this.unbindService();
    }

    public void onRestart() {
        super.onRestart();
        this.bindService();
    }

    @Override
    protected void onDestroy() {
        unbindService();
        super.onDestroy();
    }

    protected void bindService() {
        bindService(new Intent(this, MetrixIntentService.class), mConnection, Context.BIND_AUTO_CREATE);
        mIsBound = true;
    }

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

    protected ServiceConnection mConnection = new ServiceConnection() {
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
                    processPostListener(activityType, message);
                }
            });
        }
    };

    protected void processPostListener(ActivityType activityType, String message) {
        if (activityType == ActivityType.InitializationStarted) {
            mInitializationStarted = true;
            LogManager.getInstance(mCurrentActivity).debug("Initialization, run on the base activity.");
            coreUIHelper.showLoadingDialog(AndroidResourceHelper.getMessage("Initializing"));
        } else if (activityType == ActivityType.InitializationEnded) {
            mInitializationStarted = false;
            coreUIHelper.dismissLoadingDialog();
            User.setUser(User.getUser().personId, mCurrentActivity);
            Intent intent = MetrixActivityHelper.getInitialActivityIntent(mCurrentActivity);
            MetrixActivityHelper.startNewActivityAndFinish(mCurrentActivity, intent);
        } else if (activityType == ActivityType.PasswordChangedFromServer) {
            handleServerPasswordChange();
        } else if (MetrixStringHelper.valueIsEqual(message, "{\"END_GMASCR\":null}")) {
            MobileApplication.stopSync(mCurrentActivity);
            MobileApplication.startSync(mCurrentActivity);
            coreUIHelper.dismissLoadingDialog();
        }
    }

    @SuppressWarnings("deprecation")
    protected void handleServerPasswordChange() {
        SettingsHelper.saveStringSetting(mCurrentActivity, "SETTING_PASSWORD_UPDATED", "Y", false);
        MobileApplication.stopSync(mCurrentActivity);
        final AlertDialog mNewPassAlert = new AlertDialog.Builder(mCurrentActivity).create();
        mNewPassAlert.setCancelable(false);
        mNewPassAlert.setTitle(AndroidResourceHelper.getMessage("PasswordChangedMessage"));
        mNewPassAlert.setMessage(AndroidResourceHelper.getMessage("PasswordChangedTitle"));
        mNewPassAlert.setButton(AndroidResourceHelper.getMessage("OK"), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(final DialogInterface dialog, final int which) {
                if (mNewPassAlert != null) {
                    mNewPassAlert.dismiss();
                }


                try {
                    final Intent intent = new Intent();
                    intent.setClass(mCurrentActivity, Class.forName("com.metrix.metrixmobile.system.Login"));
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    mCurrentActivity.startActivity(intent);
                } catch (final Exception e) {
                    LogManager.getInstance().error(e);
                }
            }
        });
        mNewPassAlert.show();
    }

    /*
     * (non-Javadoc)
     *
     * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
     */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {

        String currActivityName = mCurrentActivity.getClass().getSimpleName();

        ArrayList<MetrixSlidingMenuItem> slidingMenuItems = new ArrayList<MetrixSlidingMenuItem>();

        // Skins flow will get a different Options menu
        if (currActivityName.contains("Skin")) {
            MetrixSlidingMenuItem slidingMenuItemSkin = new MetrixSlidingMenuItem(AndroidResourceHelper.getMessage("Designer"), mBaseResourceData.getExtraResourceID("R.drawable.sliding_menu_designer"));
            slidingMenuItems.add(slidingMenuItemSkin);
        } else {
            if (currActivityName.startsWith("MetrixDesignerFieldLookup")) {
                // only show Current Field option if we are on a Field Lookup screen
                MetrixSlidingMenuItem slidingMenuItemCurrentField = new MetrixSlidingMenuItem(AndroidResourceHelper.getMessage("CurrentField"), mBaseResourceData.getExtraResourceID("R.drawable.sliding_menu_designer"));
                slidingMenuItems.add(slidingMenuItemCurrentField);
            }

            if (!MetrixStringHelper.valueIsEqual(currActivityName, "MetrixDesignerDesignActivity")
                    && !MetrixStringHelper.valueIsEqual(currActivityName, "MetrixDesignerDesignAddActivity")
                    && !MetrixStringHelper.valueIsEqual(currActivityName, "MetrixDesignerRevisionActivity")
                    && !MetrixStringHelper.valueIsEqual(currActivityName, "MetrixDesignerCategoriesActivity")) {
                // only show Categories option if we are "below" Categories screen in flow hierarchy
                MetrixSlidingMenuItem slidingMenuItemCategory = new MetrixSlidingMenuItem(AndroidResourceHelper.getMessage("Categories"), mBaseResourceData.getExtraResourceID("R.drawable.sliding_menu_categories"));
                slidingMenuItems.add(slidingMenuItemCategory);
            }

            if (!MetrixStringHelper.valueIsEqual(currActivityName, "MetrixDesignerDesignActivity")
                    && !MetrixStringHelper.valueIsEqual(currActivityName, "MetrixDesignerDesignAddActivity")
                    && !MetrixStringHelper.valueIsEqual(currActivityName, "MetrixDesignerRevisionActivity")) {
                // only show Preview option if we have a design and revision selected (so, if we're not on Design/Revision screens)
                MetrixSlidingMenuItem slidingMenuItemPreview = new MetrixSlidingMenuItem(AndroidResourceHelper.getMessage("Preview"), mBaseResourceData.getExtraResourceID("R.drawable.sliding_menu_preview"));
                slidingMenuItems.add(slidingMenuItemPreview);
            }

            MetrixSlidingMenuItem slidingMenuItemRefreshScripts = new MetrixSlidingMenuItem(AndroidResourceHelper.getMessage("RefreshScripts"), mBaseResourceData.getExtraResourceID("R.drawable.sliding_menu_sync"));
            slidingMenuItems.add(slidingMenuItemRefreshScripts);

            MetrixSlidingMenuItem slidingMenuItemSkin = new MetrixSlidingMenuItem(AndroidResourceHelper.getMessage("Skins"), mBaseResourceData.getExtraResourceID("R.drawable.sliding_menu_skins"));
            slidingMenuItems.add(slidingMenuItemSkin);
        }

        MetrixSlidingMenuItem slidingMenuItemClose = new MetrixSlidingMenuItem(AndroidResourceHelper.getMessage("Close"), mBaseResourceData.getExtraResourceID("R.drawable.sliding_menu_close_app"));
        slidingMenuItems.add(slidingMenuItemClose);

        MetrixSlidingMenuItem slidingMenuItemHelp = new MetrixSlidingMenuItem(AndroidResourceHelper.getMessage("DesignerHelp"), mBaseResourceData.getExtraResourceID("R.drawable.sliding_menu_about"));
        slidingMenuItems.add(slidingMenuItemHelp);

        final MetrixSlidingMenuAdapter slidingMenuAdapter = new MetrixSlidingMenuAdapter(mSlidingMenuResources.get("R.layout.sliding_menu_item"), mSlidingMenuResources.get("R.id.textview_sliding_menu_item_name"),
                mSlidingMenuResources.get("R.id.textview_sliding_menu_item_count"), mSlidingMenuResources.get("R.id.imageview_sliding_menu_item_icon"), slidingMenuItems, this);

        if (mMetrixSlidingMenu != null)
            mMetrixSlidingMenu.setAdapter(slidingMenuAdapter);

        return super.onPrepareOptionsMenu(menu);
    }

    /*
     * (non-Javadoc)
     *
     * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
     */
    @Override
    public void onClick(View v) {

    }

    DialogInterface.OnClickListener previewRefreshListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            switch (which) {
                case DialogInterface.BUTTON_POSITIVE:    // Yes
                    if (SettingsHelper.getSyncPause(mCurrentActivity)) {
                        SyncPauseAlertDialog syncPauseAlertDialog = MetrixDialogAssistant.showSyncPauseAlertDialog(mCurrentActivity);
                        if (syncPauseAlertDialog != null) {
                            syncPauseAlertDialog.setOnSyncPauseAlertButtonClickListner(new SyncPauseAlertDialog.OnSyncPauseAlertButtonClickListner() {
                                @Override
                                public void OnSyncPauseAlertButtonClick(DialogInterface dialog, int which) {
                                    startPreviewRefreshListner();
                                }
                            });
                        }
                    } else
                        startPreviewRefreshListner();
                    break;

                case DialogInterface.BUTTON_NEGATIVE:    // No (do nothing)
                    break;
            }
        }
    };

    private void startPreviewRefreshListner() {
        clearDataFromUseTables();
        insertCurrentDesignerDataIntoUseTables();
        resolveMessageTranslationsInUseTables();
        MetrixDesignerHelper.refreshMetadataCaches();

        if (mPreviewAlert != null) {
            mPreviewAlert.dismiss();
        }

        // KEST-2522 Fix
        // Clearing unwanted data from MetrixPublicCache since we are going to MOBILE_INITIAL_SCREEN anyway

        MetrixPublicCache.instance.removeItem("MobileDesignerSourceActivityNamespace");
        MetrixPublicCache.instance.removeItem("MobileDesignerSourceActivitySimpleName");
        if (MetrixPublicCache.instance.containsKey("MobileDesignerSourceCodelessActivityScreenId")) {
            MetrixPublicCache.instance.removeItem("MobileDesignerSourceCodelessActivityScreenId");
        }

        if (MetrixPublicCache.instance.containsKey("CurrentBundle")) {
            MetrixPublicCache.instance.removeItem("CurrentBundle");
        }

        if (MetrixPublicCache.instance.containsKey("MobileDesignerSourceFromLinkedScreen")) {
            MetrixPublicCache.instance.removeItem("MobileDesignerSourceFromLinkedScreen");
        }

        Intent intent = MetrixActivityHelper.getInitialActivityIntent(this);
        MetrixActivityHelper.startNewActivity(this, intent);
    }

    DialogInterface.OnClickListener refreshScriptsRefreshListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            switch (which) {
                case DialogInterface.BUTTON_POSITIVE:    // Yes
                    if (SettingsHelper.getSyncPause(mCurrentActivity)) {
                        SyncPauseAlertDialog syncPauseAlertDialog = MetrixDialogAssistant.showSyncPauseAlertDialog(mCurrentActivity);
                        if (syncPauseAlertDialog != null) {
                            syncPauseAlertDialog.setOnSyncPauseAlertButtonClickListner(new SyncPauseAlertDialog.OnSyncPauseAlertButtonClickListner() {
                                @Override
                                public void OnSyncPauseAlertButtonClick(DialogInterface dialog, int which) {
                                    startRefreshScriptsRefreshListener();
                                }
                            });
                        }
                    } else
                        startRefreshScriptsRefreshListener();
                    break;

                case DialogInterface.BUTTON_NEGATIVE:    // No (do nothing)
                    break;
            }
        }
    };

    private void startRefreshScriptsRefreshListener() {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                MobileApplication.stopSync(mCurrentActivity);
                MobileApplication.startSync(mCurrentActivity, 5);

                clearScriptMetadata();

                if (doAllScriptsGet(mCurrentActivity) == false) {
                    MobileApplication.stopSync(mCurrentActivity);
                    MobileApplication.startSync(mCurrentActivity);
                    MetrixDesignerActivity.this.runOnUiThread(new Runnable() {
                        public void run() {
                            Toast.makeText(mCurrentActivity, AndroidResourceHelper.getMessage("MobileServiceUnavailable"), Toast.LENGTH_LONG).show();

                            if (mRefreshScriptsAlert != null) {
                                mRefreshScriptsAlert.dismiss();
                            }
                        }
                    });
                    return;
                }

                if (mRefreshScriptsAlert != null) {
                    mRefreshScriptsAlert.dismiss();
                }

                coreUIHelper = new MetrixUIHelper(mCurrentActivity);
                coreUIHelper.showLoadingDialog(AndroidResourceHelper.getMessage("GetAllScriptsInProgress"));
            }
        });
        thread.start();
    }

    public static boolean doAllScriptsGet(Activity activity) {
        MetrixRemoteExecutor remote = new MetrixRemoteExecutor(MobileApplication.getAppContext(), 5);
        String baseUrl = MetrixStringHelper.getString(MetrixPublicCache.instance.getItem("MetrixServiceAddress"));

        if (ping(baseUrl, remote) == false)
            return false;

        try {
            MetrixClientScriptManager.clearClientScriptCache();
            Hashtable<String, String> params = new Hashtable<String, String>();
            int device_id = SettingsHelper.getDeviceSequence(MobileApplication.getAppContext());

            params.put("device_sequence", String.valueOf(device_id));

            MetrixPerformMessage performGMASCR = new MetrixPerformMessage("perform_get_mobile_all_scripts", params);
            performGMASCR.save();
        } catch (Exception ex) {
            LogManager.getInstance().error(ex);
            return false;
        }

        return true;
    }

    public static ArrayList<String> getBusinessDataTableNames() {
        ArrayList<String> unwantedList = MobileApplication.getMobileDesignTableNames();
        ArrayList<String> businessTableNameList = new ArrayList<String>();
        ArrayList<Hashtable<String, String>> rawList = MetrixDatabaseManager.getFieldStringValuesList("select replace(upper(name), '_LOG', '') AS table_name from sqlite_master where type = 'table' AND name LIKE '%_log' order by table_name asc");
        for (int i = 0; i < rawList.size(); i++) {
            Hashtable<String, String> rawListItem = rawList.get(i);
            String candidateTableName = rawListItem.get("table_name");
            if (!unwantedList.contains(candidateTableName.toLowerCase(Locale.US)))
                businessTableNameList.add(candidateTableName);
        }

        return businessTableNameList;
    }

    public static ArrayList<String> getColumnNames(String tableName) {
        HashMap<String, MetrixTableStructure> tableStructures = MobileApplication.getTableDefinitionsFromCache();
        MetrixTableStructure tableStructure = tableStructures.get(tableName.toLowerCase(Locale.US));
        ArrayList<String> columnList = new ArrayList<String>();
        if (tableStructure != null) {
            for (String columnName : tableStructure.mColumns.keySet()) {
                columnList.add(columnName.toUpperCase(Locale.US));
            }
            Collections.sort(columnList);
        }
        return columnList;
    }

    private void clearDataFromUseTables() {
        ArrayList<String> mobileDesignTableNames = new ArrayList<String>();
        mobileDesignTableNames.add("mm_user_assignment_view");
        mobileDesignTableNames.add("use_mm_design");
        mobileDesignTableNames.add("use_mm_design_set");
        mobileDesignTableNames.add("use_mm_field");
        mobileDesignTableNames.add("use_mm_field_lkup");
        mobileDesignTableNames.add("use_mm_field_lkup_column");
        mobileDesignTableNames.add("use_mm_field_lkup_filter");
        mobileDesignTableNames.add("use_mm_field_lkup_orderby");
        mobileDesignTableNames.add("use_mm_field_lkup_table");
        mobileDesignTableNames.add("use_mm_filter_sort_item");
        mobileDesignTableNames.add("use_mm_home_item");
        mobileDesignTableNames.add("use_mm_menu_item");
        mobileDesignTableNames.add("use_mm_revision");
        mobileDesignTableNames.add("use_mm_screen");
        mobileDesignTableNames.add("use_mm_screen_item");
        mobileDesignTableNames.add("use_mm_skin");
        mobileDesignTableNames.add("use_mm_workflow");
        mobileDesignTableNames.add("use_mm_workflow_screen");

        ArrayList<String> statements = new ArrayList<String>();
        for (String tableName : mobileDesignTableNames) {
            statements.add("delete from " + tableName);
        }
        MetrixDatabaseManager.executeSqlArray(statements, false);
    }

    protected static void clearDesignMetadata(String designSetID, String designID) {
        ArrayList<String> statements = new ArrayList<String>();
        statements.add(String.format("delete from mm_field_lkup_column where lkup_table_id in (select lkup_table_id from mm_field_lkup_table where lkup_id in (select lkup_id from mm_field_lkup where field_id in (select field_id from mm_field where screen_id in (select screen_id from mm_screen where design_id = %s))))", designID));
        statements.add(String.format("delete from mm_field_lkup_table where lkup_id in (select lkup_id from mm_field_lkup where field_id in (select field_id from mm_field where screen_id in (select screen_id from mm_screen where design_id = %s)))", designID));
        statements.add(String.format("delete from mm_field_lkup_filter where lkup_id in (select lkup_id from mm_field_lkup where field_id in (select field_id from mm_field where screen_id in (select screen_id from mm_screen where design_id = %s)))", designID));
        statements.add(String.format("delete from mm_field_lkup_orderby where lkup_id in (select lkup_id from mm_field_lkup where field_id in (select field_id from mm_field where screen_id in (select screen_id from mm_screen where design_id = %s)))", designID));
        statements.add(String.format("delete from mm_field_lkup where field_id in (select field_id from mm_field where screen_id in (select screen_id from mm_screen where design_id = %s))", designID));
        statements.add(String.format("delete from mm_field where screen_id in (select screen_id from mm_screen where design_id = %s)", designID));
        statements.add(String.format("delete from mm_home_item where design_id = %s", designID));
        statements.add(String.format("delete from mm_menu_item where design_id = %s", designID));
        statements.add(String.format("delete from mm_workflow_screen where workflow_id in (select workflow_id from mm_workflow where design_id = %s)", designID));
        statements.add(String.format("delete from mm_workflow where design_id = %s", designID));
        statements.add(String.format("delete from mm_filter_sort_item where screen_id in (select screen_id from mm_screen where design_id = %s)", designID));
        statements.add(String.format("delete from mm_screen_item where screen_id in (select screen_id from mm_screen where design_id = %s)", designID));
        statements.add(String.format("delete from mm_screen where design_id = %s", designID));
        statements.add(String.format("delete from mm_revision where design_set_id = %s", designSetID));
        statements.add(String.format("delete from mm_design where design_id = %s", designID));
        statements.add(String.format("delete from mm_design_set where design_set_id = %s", designSetID));

        MetrixDatabaseManager.executeSqlArray(statements, false);
    }

    protected static void clearRevisionMetadata(String revisionID) {
        ArrayList<String> statements = new ArrayList<String>();
        statements.add(String.format("delete from mm_field_lkup_column where lkup_table_id in (select lkup_table_id from mm_field_lkup_table where lkup_id in (select lkup_id from mm_field_lkup where field_id in (select field_id from mm_field where screen_id in (select screen_id from mm_screen where revision_id = %s))))", revisionID));
        statements.add(String.format("delete from mm_field_lkup_table where lkup_id in (select lkup_id from mm_field_lkup where field_id in (select field_id from mm_field where screen_id in (select screen_id from mm_screen where revision_id = %s)))", revisionID));
        statements.add(String.format("delete from mm_field_lkup_filter where lkup_id in (select lkup_id from mm_field_lkup where field_id in (select field_id from mm_field where screen_id in (select screen_id from mm_screen where revision_id = %s)))", revisionID));
        statements.add(String.format("delete from mm_field_lkup_orderby where lkup_id in (select lkup_id from mm_field_lkup where field_id in (select field_id from mm_field where screen_id in (select screen_id from mm_screen where revision_id = %s)))", revisionID));
        statements.add(String.format("delete from mm_field_lkup where field_id in (select field_id from mm_field where screen_id in (select screen_id from mm_screen where revision_id = %s))", revisionID));
        statements.add(String.format("delete from mm_field where screen_id in (select screen_id from mm_screen where revision_id = %s)", revisionID));
        statements.add(String.format("delete from mm_home_item where revision_id = %s", revisionID));
        statements.add(String.format("delete from mm_menu_item where revision_id = %s", revisionID));
        statements.add(String.format("delete from mm_workflow_screen where workflow_id in (select workflow_id from mm_workflow where revision_id = %s)", revisionID));
        statements.add(String.format("delete from mm_workflow where revision_id = %s", revisionID));
        statements.add(String.format("delete from mm_filter_sort_item where screen_id in (select screen_id from mm_screen where revision_id = %s)", revisionID));
        statements.add(String.format("delete from mm_screen_item where screen_id in (select screen_id from mm_screen where revision_id = %s)", revisionID));
        statements.add(String.format("delete from mm_screen where revision_id = %s", revisionID));
        statements.add(String.format("delete from mm_revision where revision_id = %s", revisionID));

        MetrixDatabaseManager.executeSqlArray(statements, false);
    }

    protected static void clearSkinMetadata() {
        MetrixDatabaseManager.executeSql("delete from mm_skin");
    }

    protected static void clearScriptMetadata() {
        MetrixDatabaseManager.executeSql("delete from metrix_client_script_view");
    }

    private void insertCurrentDesignerDataIntoUseTables() {
        String designSetID = MetrixCurrentKeysHelper.getKeyValue("mm_design_set", "design_set_id");
        String designID = MetrixCurrentKeysHelper.getKeyValue("mm_design", "design_id");
        String revisionID = MetrixCurrentKeysHelper.getKeyValue("mm_revision", "revision_id");
        String skinID = MetrixDatabaseManager.getFieldStringValue("mm_revision", "skin_id", String.format("revision_id = %s", revisionID));
        User user = User.getUser();

        String query = "";
        ArrayList<String> statements = new ArrayList<String>();

        // revision may not have a skin
        if (!MetrixStringHelper.isNullOrEmpty(skinID)) {
            query = String.format("insert into use_mm_skin select * from mm_skin where skin_id = %s", skinID);
            statements.add(query);
        }

        query = String.format("insert into mm_user_assignment_view (design_id, person_id, precedence, revision_id) values (%1$s, '%2$s', 0, %3$s)",
                designID, user.personId, revisionID);
        statements.add(query);

        query = "insert into use_mm_design select * from mm_design where design_id = " + designID;
        statements.add(query);

        query = "insert into use_mm_design_set select * from mm_design_set where design_set_id = " + designSetID;
        statements.add(query);

        query = String.format("insert into use_mm_field select * from mm_field where screen_id in (select screen_id from mm_screen where design_id = %1$s and revision_id = %2$s)", designID, revisionID);
        statements.add(query);

        query = String.format("insert into use_mm_field_lkup select * from mm_field_lkup where field_id in (select field_id from mm_field where screen_id in (select screen_id from mm_screen where design_id = %1$s and revision_id = %2$s))", designID, revisionID);
        statements.add(query);

        query = String.format("insert into use_mm_field_lkup_table select * from mm_field_lkup_table where lkup_id in (select lkup_id from mm_field_lkup where field_id in (select field_id from mm_field where screen_id in (select screen_id from mm_screen where design_id = %1$s and revision_id = %2$s)))", designID, revisionID);
        statements.add(query);

        query = String.format("insert into use_mm_field_lkup_column select * from mm_field_lkup_column where lkup_table_id in (select lkup_table_id from mm_field_lkup_table where lkup_id in (select lkup_id from mm_field_lkup where field_id in (select field_id from mm_field where screen_id in (select screen_id from mm_screen where design_id = %1$s and revision_id = %2$s))))", designID, revisionID);
        statements.add(query);

        query = String.format("insert into use_mm_field_lkup_filter select * from mm_field_lkup_filter where lkup_id in (select lkup_id from mm_field_lkup where field_id in (select field_id from mm_field where screen_id in (select screen_id from mm_screen where design_id = %1$s and revision_id = %2$s)))", designID, revisionID);
        statements.add(query);

        query = String.format("insert into use_mm_field_lkup_orderby select * from mm_field_lkup_orderby where lkup_id in (select lkup_id from mm_field_lkup where field_id in (select field_id from mm_field where screen_id in (select screen_id from mm_screen where design_id = %1$s and revision_id = %2$s)))", designID, revisionID);
        statements.add(query);

        query = String.format("insert into use_mm_filter_sort_item select * from mm_filter_sort_item where screen_id in (select screen_id from mm_screen where design_id = %1$s and revision_id = %2$s)", designID, revisionID);
        statements.add(query);

        query = String.format("insert into use_mm_home_item select * from mm_home_item where design_id = %1$s and revision_id = %2$s", designID, revisionID);
        statements.add(query);

        query = String.format("insert into use_mm_menu_item select * from mm_menu_item where design_id = %1$s and revision_id = %2$s", designID, revisionID);
        statements.add(query);

        query = "insert into use_mm_revision select * from mm_revision where revision_id = " + revisionID;
        statements.add(query);

        query = String.format("insert into use_mm_screen select * from mm_screen where design_id = %1$s and revision_id = %2$s", designID, revisionID);
        statements.add(query);

        query = String.format("insert into use_mm_screen_item select * from mm_screen_item where screen_id in (select screen_id from mm_screen where design_id = %1$s and revision_id = %2$s)", designID, revisionID);
        statements.add(query);

        query = String.format("insert into use_mm_workflow select * from mm_workflow where design_id = %1$s and revision_id = %2$s", designID, revisionID);
        statements.add(query);

        query = String.format("insert into use_mm_workflow_screen select * from mm_workflow_screen where workflow_id in (select workflow_id from mm_workflow where design_id = %1$s and revision_id = %2$s)", designID, revisionID);
        statements.add(query);

        MetrixDatabaseManager.executeSqlArray(statements, false);
    }

    private void resolveMessageTranslationsInUseTables() {
        // generate a full dictionary of <localization_key, message_text> from current, local mm_message_def_view data
        HashMap<String, String> mmMessageDefViewMap = new HashMap<String, String>();
        String query = "select localization_key, message_text from mm_message_def_view";
        MetrixCursor cursor = null;
        try {
            cursor = MetrixDatabaseManager.rawQueryMC(query, null);

            if (cursor == null || !cursor.moveToFirst()) {
                return;
            }

            while (cursor.isAfterLast() == false) {
                mmMessageDefViewMap.put(cursor.getString(0), cursor.getString(1));
                cursor.moveToNext();
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        if (mmMessageDefViewMap != null && mmMessageDefViewMap.size() > 0) {
            ArrayList<String> statements = new ArrayList<String>();
            // for each USE_ message property value, try to resolve to string literal, using the above dictionary
            // SCREEN: HELP / LABEL / TIP / TAB_TITLE
            // FIELD: LABEL
            // FIELD LOOKUP: TITLE
            // FILTER/SORT: LABEL
            // HOME: LABEL
            // MENU: LABEL

            // Handle use_mm_screen
            String screenItemQuery = "select use_mm_screen.screen_id, use_mm_screen.help, use_mm_screen.label, use_mm_screen.tip, use_mm_screen.tab_title from use_mm_screen";
            MetrixCursor screenItemCursor = null;
            try {
                screenItemCursor = MetrixDatabaseManager.rawQueryMC(screenItemQuery, null);

                if (screenItemCursor != null && screenItemCursor.moveToFirst()) {
                    while (screenItemCursor.isAfterLast() == false) {
                        String currScreenID = screenItemCursor.getString(0);
                        String help = screenItemCursor.getString(1);
                        String label = screenItemCursor.getString(2);
                        String tip = screenItemCursor.getString(3);
                        String tabTitle = screenItemCursor.getString(4);

                        StringBuilder setBlock = new StringBuilder();
                        if (!MetrixStringHelper.isNullOrEmpty(help)) {
                            String helpLocalizationKey = help + "_MM_SCREEN_HELP";
                            if (mmMessageDefViewMap.containsKey(helpLocalizationKey)) {
                                String resolvedHelp = mmMessageDefViewMap.get(helpLocalizationKey);
                                if (!MetrixStringHelper.isNullOrEmpty(resolvedHelp))
                                    setBlock.append(String.format("help = '%s', ", resolvedHelp.replace("'", "''")));
                            }
                        }
                        if (!MetrixStringHelper.isNullOrEmpty(label)) {
                            String labelLocalizationKey = label + "_MM_SCREEN_LABEL";
                            if (mmMessageDefViewMap.containsKey(labelLocalizationKey)) {
                                String resolvedLabel = mmMessageDefViewMap.get(labelLocalizationKey);
                                if (!MetrixStringHelper.isNullOrEmpty(resolvedLabel))
                                    setBlock.append(String.format("label = '%s', ", resolvedLabel.replace("'", "''")));
                            }
                        }
                        if (!MetrixStringHelper.isNullOrEmpty(tip)) {
                            String tipLocalizationKey = tip + "_MM_SCREEN_TIP";
                            if (mmMessageDefViewMap.containsKey(tipLocalizationKey)) {
                                String resolvedTip = mmMessageDefViewMap.get(tipLocalizationKey);
                                if (!MetrixStringHelper.isNullOrEmpty(resolvedTip))
                                    setBlock.append(String.format("tip = '%s', ", resolvedTip.replace("'", "''")));
                            }
                        }
                        if (!MetrixStringHelper.isNullOrEmpty(tabTitle)) {
                            String tabTitleLocalizationKey = tabTitle + "_MM_TAB_TITLE";
                            if (mmMessageDefViewMap.containsKey(tabTitleLocalizationKey)) {
                                String resolvedTabTitle = mmMessageDefViewMap.get(tabTitleLocalizationKey);
                                if (!MetrixStringHelper.isNullOrEmpty(resolvedTabTitle))
                                    setBlock.append(String.format("tab_title = '%s', ", resolvedTabTitle.replace("'", "''")));
                            }
                        }

                        if (!MetrixStringHelper.isNullOrEmpty(setBlock.toString())) {
                            String finalSetBlock = setBlock.toString();
                            finalSetBlock = finalSetBlock.substring(0, finalSetBlock.lastIndexOf(","));
                            statements.add(String.format("update use_mm_screen set %1$s where screen_id = %2$s", finalSetBlock, currScreenID));
                        }

                        screenItemCursor.moveToNext();
                    }
                }
            } finally {
                if (screenItemCursor != null) {
                    screenItemCursor.close();
                }
            }

            // Handle use_mm_field
            String fieldQuery = "select use_mm_field.field_id, use_mm_field.label from use_mm_field";
            MetrixCursor fieldCursor = null;
            try {
                fieldCursor = MetrixDatabaseManager.rawQueryMC(fieldQuery, null);

                if (fieldCursor != null && fieldCursor.moveToFirst()) {
                    while (fieldCursor.isAfterLast() == false) {
                        String currFieldID = fieldCursor.getString(0);
                        String propValue = fieldCursor.getString(1);
                        String thisLocalizationKey = propValue + "_LABEL";

                        if (mmMessageDefViewMap.containsKey(thisLocalizationKey)) {
                            String resolvedMessage = mmMessageDefViewMap.get(thisLocalizationKey);
                            if (!MetrixStringHelper.isNullOrEmpty(resolvedMessage))
                                statements.add(String.format("update use_mm_field set label = '%1$s' where field_id = %2$s", resolvedMessage.replace("'", "''"), currFieldID));
                        }
                        fieldCursor.moveToNext();
                    }
                }
            } finally {
                if (fieldCursor != null) {
                    fieldCursor.close();
                }
            }

            // Handle use_mm_field_lkup
            String fieldLkupQuery = "select use_mm_field_lkup.lkup_id, use_mm_field_lkup.title from use_mm_field_lkup";
            MetrixCursor fieldLkupCursor = null;
            try {
                fieldLkupCursor = MetrixDatabaseManager.rawQueryMC(fieldLkupQuery, null);

                if (fieldLkupCursor != null && fieldLkupCursor.moveToFirst()) {
                    while (fieldLkupCursor.isAfterLast() == false) {
                        String currLkupID = fieldLkupCursor.getString(0);
                        String propValue = fieldLkupCursor.getString(1);
                        String thisLocalizationKey = propValue + "_MM_LOOKUP_TITLE";

                        if (mmMessageDefViewMap.containsKey(thisLocalizationKey)) {
                            String resolvedMessage = mmMessageDefViewMap.get(thisLocalizationKey);
                            if (!MetrixStringHelper.isNullOrEmpty(resolvedMessage))
                                statements.add(String.format("update use_mm_field_lkup set title = '%1$s' where lkup_id = %2$s", resolvedMessage.replace("'", "''"), currLkupID));
                        }
                        fieldLkupCursor.moveToNext();
                    }
                }
            } finally {
                if (fieldLkupCursor != null) {
                    fieldLkupCursor.close();
                }
            }

            // Handle use_mm_home_item
            String homeItemQuery = "select use_mm_home_item.item_id, use_mm_home_item.label from use_mm_home_item";
            MetrixCursor homeItemCursor = null;
            try {
                homeItemCursor = MetrixDatabaseManager.rawQueryMC(homeItemQuery, null);

                if (homeItemCursor != null && homeItemCursor.moveToFirst()) {
                    while (homeItemCursor.isAfterLast() == false) {
                        String currItemID = homeItemCursor.getString(0);
                        String propValue = homeItemCursor.getString(1);
                        String thisLocalizationKey = propValue + "_MM_HOME_LABEL";

                        if (mmMessageDefViewMap.containsKey(thisLocalizationKey)) {
                            String resolvedMessage = mmMessageDefViewMap.get(thisLocalizationKey);
                            if (!MetrixStringHelper.isNullOrEmpty(resolvedMessage))
                                statements.add(String.format("update use_mm_home_item set label = '%1$s' where item_id = %2$s", resolvedMessage.replace("'", "''"), currItemID));
                        }
                        homeItemCursor.moveToNext();
                    }
                }
            } finally {
                if (homeItemCursor != null) {
                    homeItemCursor.close();
                }
            }

            // Handle use_mm_menu_item
            String menuItemQuery = "select use_mm_menu_item.item_id, use_mm_menu_item.label from use_mm_menu_item";
            MetrixCursor menuItemCursor = null;
            try {
                menuItemCursor = MetrixDatabaseManager.rawQueryMC(menuItemQuery, null);

                if (menuItemCursor != null && menuItemCursor.moveToFirst()) {
                    while (menuItemCursor.isAfterLast() == false) {
                        String currItemID = menuItemCursor.getString(0);
                        String propValue = menuItemCursor.getString(1);
                        String thisLocalizationKey = propValue + "_MM_MENU_LABEL";

                        if (mmMessageDefViewMap.containsKey(thisLocalizationKey)) {
                            String resolvedMessage = mmMessageDefViewMap.get(thisLocalizationKey);
                            if (!MetrixStringHelper.isNullOrEmpty(resolvedMessage))
                                statements.add(String.format("update use_mm_menu_item set label = '%1$s' where item_id = %2$s", resolvedMessage.replace("'", "''"), currItemID));
                        }
                        menuItemCursor.moveToNext();
                    }
                }
            } finally {
                if (menuItemCursor != null) {
                    menuItemCursor.close();
                }
            }

            // Handle use_mm_filter_sort_item
            String filterSortItemQuery = "select use_mm_filter_sort_item.item_id, use_mm_filter_sort_item.label from use_mm_filter_sort_item";
            MetrixCursor filterSortItemCursor = null;
            try {
                filterSortItemCursor = MetrixDatabaseManager.rawQueryMC(filterSortItemQuery, null);

                if (filterSortItemCursor != null && filterSortItemCursor.moveToFirst()) {
                    while (filterSortItemCursor.isAfterLast() == false) {
                        String currItemID = filterSortItemCursor.getString(0);
                        String propValue = filterSortItemCursor.getString(1);
                        String thisLocalizationKey = propValue + "_MM_FILTER_SORT";

                        if (mmMessageDefViewMap.containsKey(thisLocalizationKey)) {
                            String resolvedMessage = mmMessageDefViewMap.get(thisLocalizationKey);
                            if (!MetrixStringHelper.isNullOrEmpty(resolvedMessage))
                                statements.add(String.format("update use_mm_filter_sort_item set label = '%1$s' where item_id = %2$s", resolvedMessage.replace("'", "''"), currItemID));
                        }
                        filterSortItemCursor.moveToNext();
                    }
                }
            } finally {
                if (filterSortItemCursor != null) {
                    filterSortItemCursor.close();
                }
            }

            MetrixDatabaseManager.executeSqlArray(statements, false);
        }
    }

    protected static boolean ping(String baseServiceUrl, MetrixRemoteExecutor remoteExecutor) {
        String pingServiceUrl = MetrixSyncManager.generateRestfulServiceUrl(baseServiceUrl, MessageType.Messages, null, 0, null, null);

        try {
            String response = remoteExecutor.executeGet(pingServiceUrl).replace("\\", "");

            if (response != null) {
                if (response.contains("true"))
                    return true;
            }
        } catch (HandlerException ex) {
            LogManager.getInstance().error(ex);
            return false;
        } catch (JSONException ex) {
            LogManager.getInstance().error(ex);
            return false;
        }

        return false;
    }

    @Override
    public void onSlidingMenuItemClick(MetrixSlidingMenuItem clickedItem) {
        final String title = clickedItem.getTitle();
        if (!MetrixStringHelper.isNullOrEmpty(title)) {
            if (MetrixStringHelper.valueIsEqual(title, AndroidResourceHelper.getMessage("CurrentField"))) {
                Intent intent = MetrixActivityHelper.createActivityIntent(mCurrentActivity, MetrixDesignerFieldPropActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intent.putExtra("headingText", mHeadingText);
                MetrixActivityHelper.startNewActivity(mCurrentActivity, intent);
            } else if (MetrixStringHelper.valueIsEqual(title, AndroidResourceHelper.getMessage("Categories"))) {
                Intent intent = MetrixActivityHelper.createActivityIntent(mCurrentActivity, MetrixDesignerCategoriesActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intent.putExtra("headingText", mHeadingText);
                MetrixActivityHelper.startNewActivity(mCurrentActivity, intent);
            } else if (MetrixStringHelper.valueIsEqual(title, AndroidResourceHelper.getMessage("Preview"))) {
                mPreviewAlert = new AlertDialog.Builder(mCurrentActivity).create();
                mPreviewAlert.setTitle(mBaseResourceData.ActionBarTitleString);
                mPreviewAlert.setMessage(AndroidResourceHelper.getMessage("MobileDesignerPreviewConfirm"));
                mPreviewAlert.setButton(DialogInterface.BUTTON_POSITIVE, AndroidResourceHelper.getMessage("Yes"), previewRefreshListener);
                mPreviewAlert.setButton(DialogInterface.BUTTON_NEGATIVE, AndroidResourceHelper.getMessage("No"), previewRefreshListener);
                mPreviewAlert.show();
            } else if (MetrixStringHelper.valueIsEqual(title, AndroidResourceHelper.getMessage("RefreshScripts"))) {
                mRefreshScriptsAlert = new AlertDialog.Builder(mCurrentActivity).create();
                mRefreshScriptsAlert.setTitle(AndroidResourceHelper.getMessage("RefreshScripts"));
                mRefreshScriptsAlert.setMessage(AndroidResourceHelper.getMessage("RefreshAllScriptsConfirm"));
                mRefreshScriptsAlert.setButton(DialogInterface.BUTTON_POSITIVE, AndroidResourceHelper.getMessage("Yes"), refreshScriptsRefreshListener);
                mRefreshScriptsAlert.setButton(DialogInterface.BUTTON_NEGATIVE, AndroidResourceHelper.getMessage("No"), refreshScriptsRefreshListener);
                mRefreshScriptsAlert.show();
            } else if (MetrixStringHelper.valueIsEqual(title, AndroidResourceHelper.getMessage("Skins"))) {
                Class<? extends Activity> currentActivityClass = mCurrentActivity.getClass();
                String fullName = currentActivityClass.getName();
                String simpleName = currentActivityClass.getSimpleName();
                String namespace = fullName.substring(0, fullName.lastIndexOf(simpleName) - 1);
                MetrixPublicCache.instance.addItem("MobileSkinsSourceActivityNamespace", namespace);
                MetrixPublicCache.instance.addItem("MobileSkinsSourceActivitySimpleName", simpleName);
                MetrixPublicCache.instance.addItem("MobileSkinsSourceActivityHeadingText", mHeadingText);

                Intent intent = MetrixActivityHelper.createActivityIntent(mCurrentActivity, MetrixDesignerSkinActivity.class);
                MetrixActivityHelper.startNewActivity(mCurrentActivity, intent);
            } else if (MetrixStringHelper.valueIsEqual(title, AndroidResourceHelper.getMessage("Designer"))) {
                String sourceNamespace = (String) MetrixPublicCache.instance.getItem("MobileSkinsSourceActivityNamespace");
                String sourceSimpleName = (String) MetrixPublicCache.instance.getItem("MobileSkinsSourceActivitySimpleName");
                String sourceHeadingText = (String) MetrixPublicCache.instance.getItem("MobileSkinsSourceActivityHeadingText");

                Intent intent = MetrixActivityHelper.createActivityIntent(mCurrentActivity, sourceNamespace, sourceSimpleName);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intent.putExtra("headingText", sourceHeadingText);
                MetrixActivityHelper.startNewActivity(mCurrentActivity, intent);
            } else if (MetrixStringHelper.valueIsEqual(title, AndroidResourceHelper.getMessage("Close"))) {
                String sourceNamespace = (String) MetrixPublicCache.instance.getItem("MobileDesignerSourceActivityNamespace");
                String sourceSimpleName = (String) MetrixPublicCache.instance.getItem("MobileDesignerSourceActivitySimpleName");

                //Catering the codeless screen scenario.
                int codelessScreenId = -1;
                if (MetrixPublicCache.instance.containsKey("MobileDesignerSourceCodelessActivityScreenId"))
                    codelessScreenId = (Integer) MetrixPublicCache.instance.getItem("MobileDesignerSourceCodelessActivityScreenId");

                Intent intent = MetrixActivityHelper.createActivityIntent(mCurrentActivity, sourceNamespace, sourceSimpleName);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

                // Try and restore calling activity's intent extras from public cache - KEST-2516
                Bundle oldArgs = (Bundle) MetrixPublicCache.instance.getItem("CurrentBundle");
                if (oldArgs != null) {
                    intent.putExtras(oldArgs);
                    MetrixPublicCache.instance.removeItem("CurrentBundle");
                }

                intent.putExtra("ScreenID", codelessScreenId);
                if (MetrixPublicCache.instance.containsKey("MobileDesignerSourceFromLinkedScreen"))
                    intent.putExtra("NavigatedFromLinkedScreen", true);
                MetrixActivityHelper.startNewActivity(mCurrentActivity, intent);
            } else if (MetrixStringHelper.valueIsEqual(title, AndroidResourceHelper.getMessage("DesignerHelp"))) {
                Intent intent = MetrixActivityHelper.createActivityIntent(mCurrentActivity, ArchHelp.class);
                String message = null;
                if (!MetrixStringHelper.isNullOrEmpty(helpText))
                    message = helpText;
                else
                    message = AndroidResourceHelper.getMessage("NoHelpDetailsAvailable");
                message = message + "\r\n \r\n" + mCurrentActivity.getClass().getSimpleName();
                intent.putExtra("help_text", message);
                MetrixActivityHelper.startNewActivity(mCurrentActivity, intent);
            }
        }

        if (mDrawerLayout != null)
            mDrawerLayout.closeDrawer(mDrawerLinearLayout);
    }

    protected static void turnOffOtherFilterSortDefaults(String screenId, String itemType) {
        try {
            ArrayList<Hashtable<String, String>> defaultItems = MetrixDatabaseManager.getFieldStringValuesList(String.format("select metrix_row_id, item_id from mm_filter_sort_item where screen_id = %1$s and item_type = '%2$s' and is_default = 'Y'", screenId, itemType));
            if (defaultItems != null && !defaultItems.isEmpty()) {
                ArrayList<MetrixSqlData> itemsToUpdate = new ArrayList<MetrixSqlData>();
                for (Hashtable<String, String> row : defaultItems) {
                    String metrixRowId = row.get("metrix_row_id");
                    String itemId = row.get("item_id");

                    MetrixSqlData data = new MetrixSqlData("mm_filter_sort_item", MetrixTransactionTypes.UPDATE, "metrix_row_id="+ metrixRowId);
                    data.dataFields.add(new DataField("metrix_row_id", metrixRowId));
                    data.dataFields.add(new DataField("item_id", itemId));
                    data.dataFields.add(new DataField("is_default", "N"));
                    itemsToUpdate.add(data);
                }

                if (itemsToUpdate.size() > 0) {
                    MetrixTransaction transactionInfo = new MetrixTransaction();
                    MetrixUpdateManager.update(itemsToUpdate, true, transactionInfo, AndroidResourceHelper.getMessage("FilterSortUpdate"), null);
                }
            }
        } catch (Exception ex) {
            LogManager.getInstance().error(ex);
        }
    }

    /**
     * Used in the context of an inflated zzmd_prop_lookup_line.xml
     * Provides the opportunity to display the message_text after selecting a message_id.
     */
    protected class MessageTextWatcher implements TextWatcher {
        private String mMessageType;
        private TextView mDescLabel;
        private TextView mDescText;
        private TextView mDefaultValueDataType;

        public MessageTextWatcher(String messageType, TextView tvLabel, TextView tvText) {
            mMessageType = messageType;
            mDescLabel = tvLabel;
            mDescText = tvText;
        }

        public MessageTextWatcher(String messageType, TextView tvLabel, TextView tvText, TextView tvDataType) {
            mMessageType = messageType;
            mDescLabel = tvLabel;
            mDescText = tvText;
            mDefaultValueDataType = tvDataType;
        }

        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        public void onTextChanged(CharSequence s, int start, int before, int count) {
            mDescLabel.setVisibility(View.GONE);
            mDescText.setVisibility(View.GONE);
            String propValue = s.toString();
            if (!MetrixStringHelper.isNullOrEmpty(propValue)) {
                String description = MetrixDatabaseManager.getFieldStringValue("mm_message_def_view", "message_text", String.format("message_type = '%1$s' and message_id = '%2$s'", mMessageType, propValue));
                if (!MetrixStringHelper.isNullOrEmpty(description)) {
                    mDescText.setText(description);
                    mDescLabel.setVisibility(View.INVISIBLE);
                    mDescText.setVisibility(View.VISIBLE);
                }

                Hashtable<String, String> result = MetrixDatabaseManager.getFieldStringValues("metrix_code_table", new String[]{"code_value"}, String.format("code_name = '%1$s' and message_id = '%2$s'", "MM_DEFAULT_VALUE", propValue));
                if (result != null && result.size() > 0) {
                    if (mDefaultValueDataType != null)
                        mDefaultValueDataType.setText(result.get("comments"));
                } else {
                    if (mDefaultValueDataType != null)
                        mDefaultValueDataType.setText(null);
                }
            }
        }

        public void afterTextChanged(Editable s) {
        }
    }

    /**
     * Used in the context of an inflated zzmd_prop_lookup_line.xml
     * Provides the opportunity to display descriptive text upon selecting a versioned client script.
     */
    protected class ClientScriptTextWatcher implements TextWatcher {
        private TextView mDescLabel;
        private TextView mDescText;

        public ClientScriptTextWatcher(TextView tvLabel, TextView tvText) {
            mDescLabel = tvLabel;
            mDescText = tvText;
        }

        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        public void onTextChanged(CharSequence s, int start, int before, int count) {
            mDescLabel.setVisibility(View.GONE);
            mDescText.setVisibility(View.GONE);
            String propValue = s.toString();
            if (!MetrixStringHelper.isNullOrEmpty(propValue)) {
                String description = "";
                String scriptName = MetrixDatabaseManager.getFieldStringValue("metrix_client_script_view", "name", String.format("unique_vs = '%s'", propValue));
                String scriptVersion = MetrixDatabaseManager.getFieldStringValue("metrix_client_script_view", "version_number", String.format("unique_vs = '%s'", propValue));
                if (!MetrixStringHelper.isNullOrEmpty(scriptName) && !MetrixStringHelper.isNullOrEmpty(scriptVersion)) {
                    if (MetrixStringHelper.valueIsEqual(scriptVersion, "0"))
                        description = String.format("%1$s (%2$s)", scriptName, AndroidResourceHelper.getMessage("Baseline"));
                    else
                        description = String.format("%1$s (%2$s %3$s)", scriptName, AndroidResourceHelper.getMessage("Version"), scriptVersion);

                    mDescText.setText(description);
                    mDescLabel.setVisibility(View.INVISIBLE);
                    mDescText.setVisibility(View.VISIBLE);
                }
            }
        }

        public void afterTextChanged(Editable s) {
        }
    }

    /**
     * Used in the context of an inflated zzmd_prop_lookup_line.xml
     * Provides the opportunity to display descriptive text upon selecting a versioned client script.
     * If the string is non-empty but is not a client script identifier, display "Literal Value".
     */
    protected class ClientScriptOrLiteralTextWatcher implements TextWatcher {
        private TextView mDescLabel;
        private TextView mDescText;

        public ClientScriptOrLiteralTextWatcher(TextView tvLabel, TextView tvText) {
            mDescLabel = tvLabel;
            mDescText = tvText;
        }

        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        public void onTextChanged(CharSequence s, int start, int before, int count) {
            mDescLabel.setVisibility(View.GONE);
            mDescText.setVisibility(View.GONE);
            String propValue = s.toString();
            if (!MetrixStringHelper.isNullOrEmpty(propValue)) {
                String description = "";
                String scriptName = MetrixDatabaseManager.getFieldStringValue("metrix_client_script_view", "name", String.format("unique_vs = '%s'", propValue));
                String scriptVersion = MetrixDatabaseManager.getFieldStringValue("metrix_client_script_view", "version_number", String.format("unique_vs = '%s'", propValue));
                if (!MetrixStringHelper.isNullOrEmpty(scriptName) && !MetrixStringHelper.isNullOrEmpty(scriptVersion)) {
                    if (MetrixStringHelper.valueIsEqual(scriptVersion, "0"))
                        description = String.format("%1$s (%2$s)", scriptName, AndroidResourceHelper.getMessage("Baseline"));
                    else
                        description = String.format("%1$s (%2$s %3$s)", scriptName, AndroidResourceHelper.getMessage("Version"), scriptVersion);
                } else
                    description = AndroidResourceHelper.getMessage("LiteralValue");

                mDescText.setText(description);
                mDescLabel.setVisibility(View.INVISIBLE);
                mDescText.setVisibility(View.VISIBLE);
            }
        }

        public void afterTextChanged(Editable s) {
        }
    }

    /**
     * Used in the context of a metrix_image_view lookup
     * Provides the opportunity to display the image after selecting an image_id.
     */
    protected class ImageTextWatcher implements TextWatcher {
        private ImageView mPreview;
        private int mBlankImageResourceID;

        public ImageTextWatcher(ImageView ivPreview, int blankImageResourceID) {
            mPreview = ivPreview;
            mBlankImageResourceID = blankImageResourceID;
        }

        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        public void onTextChanged(CharSequence s, int start, int before, int count) {
            boolean imageUsed = false;
            String imageIDValue = s.toString();
            if (!MetrixStringHelper.isNullOrEmpty(imageIDValue)) {
                imageUsed = MetrixAttachmentHelper.applyImageWithNoScale(imageIDValue, mPreview);
            }

            if (!imageUsed) {
                mPreview.setImageDrawable(mPreview.getContext().getResources().getDrawable(mBlankImageResourceID));
            }
        }

        public void afterTextChanged(Editable s) {
        }
    }

    protected void doClientScriptSelection(int viewToPopulateId, LinearLayout parentLayout) {
        MetrixLookupDef lookupDef = new MetrixLookupDef("metrix_client_script_view");
        lookupDef.columnNames.add(new MetrixLookupColumnDef("metrix_client_script_view.unique_vs", viewToPopulateId));
        lookupDef.columnNames.add(new MetrixLookupColumnDef("metrix_client_script_view.name"));

        Intent intent = MetrixActivityHelper.createActivityIntent(this, "com.metrix.metrixmobile.system", "Lookup");
        intent.putExtra("NoOptionsMenu", true);
        MetrixPublicCache.instance.addItem("lookupDef", lookupDef);
        MetrixPublicCache.instance.addItem("lookupParentLayout", parentLayout);
        startActivityForResult(intent, 2727);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        if (mDrawerToggle != null)
            mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (mDrawerToggle != null)
            mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(mBaseResourceData.getExtraResourceID("R.menu.main"), menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mDrawerToggle != null) {
            if (mDrawerToggle.onOptionsItemSelected(item))
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public ActionBar getMetrixActionBar() {
        return mSupportActionBar;
    }
}

