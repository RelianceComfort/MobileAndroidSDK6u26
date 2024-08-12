package com.metrix.architecture.attachment;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Point;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.view.Display;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TimePicker;

import androidx.core.widget.NestedScrollView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.metrix.architecture.actionbar.MetrixActionBarManager;
import com.metrix.architecture.assistants.MetrixApplicationAssistant;
import com.metrix.architecture.assistants.MetrixControlAssistant;
import com.metrix.architecture.constants.MetrixTransactionTypes;
import com.metrix.architecture.database.MetrixDatabaseManager;
import com.metrix.architecture.database.MobileApplication;
import com.metrix.architecture.designer.MetrixDesignerResourceBaseData;
import com.metrix.architecture.designer.MetrixFieldManager;
import com.metrix.architecture.designer.MetrixScreenItemManager;
import com.metrix.architecture.designer.MetrixScreenManager;
import com.metrix.architecture.designer.MetrixSkinManager;
import com.metrix.architecture.managers.MetrixFormManager;
import com.metrix.architecture.managers.MetrixUpdateManager;
import com.metrix.architecture.metadata.MetrixColumnDef;
import com.metrix.architecture.metadata.MetrixFormDef;
import com.metrix.architecture.metadata.MetrixSqlData;
import com.metrix.architecture.metadata.MetrixTableDef;
import com.metrix.architecture.metadata.MetrixTransaction;
import com.metrix.architecture.scripting.ClientScriptDef;
import com.metrix.architecture.scripting.MetrixClientScriptManager;
import com.metrix.architecture.services.IPostMonitor;
import com.metrix.architecture.services.MetrixIntentService;
import com.metrix.architecture.services.MetrixRemoteExecutor;
import com.metrix.architecture.services.MetrixServiceManager;
import com.metrix.architecture.services.MetrixSyncManager;
import com.metrix.architecture.services.RemoteMessagesHandler;
import com.metrix.architecture.superclasses.MetrixBaseActivity;
import com.metrix.architecture.superclasses.MetrixControlState;
import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.architecture.utilities.DataField;
import com.metrix.architecture.utilities.Global;
import com.metrix.architecture.utilities.LogManager;
import com.metrix.architecture.utilities.MetrixActivityHelper;
import com.metrix.architecture.utilities.MetrixAttachmentHelper;
import com.metrix.architecture.utilities.MetrixAttachmentManager;
import com.metrix.architecture.utilities.MetrixDate;
import com.metrix.architecture.utilities.MetrixDateTime;
import com.metrix.architecture.utilities.MetrixDateTimeHelper;
import com.metrix.architecture.utilities.MetrixPublicCache;
import com.metrix.architecture.utilities.MetrixStringHelper;
import com.metrix.architecture.utilities.MetrixTime;
import com.metrix.architecture.utilities.MetrixUIHelper;
import com.metrix.architecture.utilities.SettingsHelper;
import com.metrix.architecture.utilities.User;

import java.io.File;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.metrix.architecture.database.MobileApplication.ARCH_GET_LOOKUP_RESULT;

import org.json.JSONException;

public class AttachmentAPIBaseActivity extends MetrixBaseActivity implements View.OnClickListener, View.OnFocusChangeListener {
    public static final int MOBILE_GLOBAL_GET_LOOKUP_RESULT = 1234;
    public static final int EDIT_FULLTEXT = 100;

    protected MetrixDesignerResourceBaseData mBaseResourceData;
    protected IPostMonitor service = null;
    protected Activity mCurrentActivity = null;
    protected TextView mActionBarTitle;
    protected MetrixUIHelper coreUIHelper = new MetrixUIHelper(this);
    protected MetrixFormDef mFormDef;
    protected ViewGroup mLayout;

    protected boolean mInitializationStarted = false;
    protected boolean mIsBound;
    protected boolean mLookupReturn = false;

    protected ViewGroup mCoordinatorLayout;
    protected List<FloatingActionButton> mFABList;
    protected List<FloatingActionButton> mFABsToShow;
    protected Handler fabHandler = new Handler();
    protected Runnable fabRunnable;
    protected long fabDelay = 1000;

    private static boolean navigateBack;

    private boolean formDefined = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        if (MetrixStringHelper.isNullOrEmpty(MobileApplication.ApplicationNullIfCrashed)) {
            finish();
        }

        if (savedInstanceState != null && !navigateBack) {
            MetrixPublicCache.instance.addItem("orientationChange_Occurred", true);
        }

        super.onCreate(savedInstanceState);

        mBaseResourceData = (MetrixDesignerResourceBaseData) MetrixPublicCache.instance.getItem("FSMAttachmentAPIBaseResources");

        mCurrentActivity = this;
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

        //Explicitly hides the keyboard since android:windowSoftInputMode="stateHidden" not working,
        //when navigating back to a previous activity with soft-keyboard stays open/visible
        MetrixActivityHelper.explicitlyHideSoftKeyboard(this.getWindow());
        LogManager.getInstance(this).info("{0} onStart()", mCurrentActivity.getLocalClassName());

        MetrixActionBarManager.getInstance().setupActionBar(this, mBaseResourceData.getActionBarLayoutID(), true);
        mActionBarTitle = MetrixActionBarManager.getInstance().setupActionBarTitle(this, mBaseResourceData.ActionBarTitleResourceID, mBaseResourceData.ActionBarTitleString, null);

        // Always hide the split action bar
        View splitActionBar = findViewById(mBaseResourceData.getExtraResourceID("R.id.split_action_bar"));
        if (splitActionBar != null)
            splitActionBar.setVisibility(View.GONE);

        mLayout = (ViewGroup) findViewById(getTableLayoutID());

        if (mLookupReturn == false && !MetrixPublicCache.instance.containsKey("onRestart_Occurred")) {
            // If we are not returning from a lookup, and we are not doing an activity restart,
            // do the form setup, value defaulting, scripted refresh event, etc.

            MetrixPublicCache.instance.addItem("initialScreenValuesSet", false);

            if (!formDefined) {
                defineForm();
                if (mLayout != null && mFormDef != null) {
                    MetrixFieldManager.addFieldsToScreen(this, mLayout, mFormDef, AttachmentWidgetManager.getAttachmentCardScreenId());
                    setupDateTimePickers();
                }
                formDefined = true;
            }

            if (mFABList == null)
                mFABList = new ArrayList<>();

            if (mLayout != null && mFormDef != null) {
                MetrixFormManager.setupForm(this, mLayout, this.mFormDef);
                populateNonStandardControls();
                MetrixFieldManager.defaultValues(this, mFormDef, mLayout, AttachmentWidgetManager.getAttachmentCardScreenId());
            }

            setSkinBasedColorsOnRelevantControls(mLayout, MetrixSkinManager.getPrimaryColor(), MetrixSkinManager.getSecondaryColor(), MetrixSkinManager.getHyperlinkColor(), mLayout, true);
            setSkinBasedColorsOnButtons(((ViewGroup)findViewById(android.R.id.content)).getChildAt(0), this);

            setListeners();

            boolean stopExec = runScreenRefreshScriptEvent();
            if (stopExec)
                return;

            cacheOnStartValues();

            Handler handler = new Handler(Looper.getMainLooper());
            handler.postDelayed(new Runnable() {
                public void run() {
                    MetrixPublicCache.instance.addItem("initialScreenValuesSet", true);
                }
            }, 500);
        } else if (mLookupReturn == false && MetrixPublicCache.instance.containsKey("backButtonPress_Occurred") && !MetrixPublicCache.instance.containsKey("goingBackFromLookup_AttachmentAPI")) {
            // If we are not returning from a lookup, but we are navigating back to this screen (one of many ways onRestart is called),
            // execute the screen refresh script event.
            boolean stopExec = runScreenRefreshScriptEvent();
            if (stopExec)
                return;
        }

        mLookupReturn = false;
        toggleOrientationLock(false);
        MetrixPublicCache.instance.removeItem("goingBackFromLookup_AttachmentAPI");
        MetrixPublicCache.instance.removeItem("onRestart_Occurred");
        MetrixPublicCache.instance.removeItem("backButtonPress_Occurred");
    }

    @Override
    public void onStop() {
        super.onStop();
        this.unbindService();
    }

    @Override
    public void onRestart() {
        super.onRestart();
        this.bindService();

        // only note that restart occurred if it is NOT being caused by popping the activity back stack
        // but do allow onRestart-related behavior if we are coming back from a Lookup activity
        if (!MetrixPublicCache.instance.containsKey("backButtonPress_Occurred"))
            MetrixPublicCache.instance.addItem("onRestart_Occurred", true);
        else if (MetrixPublicCache.instance.containsKey("goingBackFromLookup_AttachmentAPI")) {
            MetrixPublicCache.instance.addItem("onRestart_Occurred", true);
        }
    }

    @Override
    protected void onDestroy() {
        unbindService();
        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle savedInstanceState) {
        String currentActivityName = mCurrentActivity.getClass().getSimpleName();
        if (mFormDef != null && mFormDef.tables.size() > 0) {
            for (MetrixTableDef table : mFormDef.tables) {
                for (MetrixColumnDef col : table.columns) {
                    String cacheKey = String.format("%1$s__%2$s__%3$s", currentActivityName, table.tableName, col.columnName);
                    View thisCtrl = MetrixControlAssistant.getControl(mFormDef, mLayout, table.tableName, col.columnName);
                    MetrixControlState thisCtrlState = new MetrixControlState(thisCtrl.isEnabled(), col.required, (thisCtrl.getVisibility() == View.VISIBLE), MetrixControlAssistant.getValue(mFormDef, mLayout, table.tableName, col.columnName));
                    thisCtrlState.mSpinnerItems = MetrixControlAssistant.getSpinnerItems(thisCtrl);
                    savedInstanceState.putSerializable(cacheKey, thisCtrlState);
                }
            }
        }

        navigateBack = false;
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        if (navigateBack) {
            navigateBack = false;
            return;
        }

        MetrixPublicCache.instance.addItem("initialScreenValuesSet", false);

        String currentActivityName = mCurrentActivity.getClass().getSimpleName();
        if (mFormDef != null && mFormDef.tables.size() > 0) {
            for (MetrixTableDef table : mFormDef.tables) {
                for (MetrixColumnDef col : table.columns) {
                    if (col.primaryKey) continue;
                    String cacheKey = String.format("%1$s__%2$s__%3$s", currentActivityName, table.tableName, col.columnName);
                    MetrixControlState thisCtrlState = (MetrixControlState)savedInstanceState.getSerializable(cacheKey);
                    if (thisCtrlState != null) {
                        MetrixControlAssistant.setEnabled(mFormDef, mLayout, table.tableName, col.columnName, thisCtrlState.mIsEnabled);
                        MetrixControlAssistant.setRequired(mFormDef, mLayout, table.tableName, col.columnName, thisCtrlState.mIsRequired);
                        MetrixControlAssistant.setVisibility(col.id, mLayout, ((thisCtrlState.mIsVisible) ? View.VISIBLE : View.GONE));
                        if (col.labelId != null)
                            MetrixControlAssistant.setVisibility(col.labelId, mLayout, ((thisCtrlState.mIsVisible) ? View.VISIBLE : View.GONE));
                        if (thisCtrlState.mSpinnerItems != null)
                            MetrixControlAssistant.populateSpinnerFromGenericList(this, findViewById(col.id), thisCtrlState.mSpinnerItems);
                        MetrixControlAssistant.setValue(mFormDef, mLayout, table.tableName, col.columnName, thisCtrlState.mValue);
                    }
                }
            }
        }

        Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(new Runnable() { public void run() { MetrixPublicCache.instance.addItem("initialScreenValuesSet", true); }}, 500);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onClick(View v) {
        matchIdAndLaunchDialog(v);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        if (hasFocus) {
            matchIdAndLaunchDialog(v);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case ARCH_GET_LOOKUP_RESULT:
            case MOBILE_GLOBAL_GET_LOOKUP_RESULT:
                mLookupReturn = true;
                break;
            case EDIT_FULLTEXT:
                if (resultCode == RESULT_OK) {
                    String tableName = data.getStringExtra("table_name");
                    String columnName = data.getStringExtra("column_name");
                    String columnValue = data.getStringExtra("column_value");

                    for (MetrixTableDef table : mFormDef.tables) {
                        if (table.tableName.compareToIgnoreCase(tableName) == 0) {
                            for (MetrixColumnDef columnDef : table.columns) {
                                if (columnDef.columnName.compareToIgnoreCase(columnName) == 0) {
                                    try {
                                        View view = MetrixControlAssistant.getControl(columnDef.id, mLayout);

                                        if (view instanceof EditText)
                                            MetrixControlAssistant.setTag(columnDef, mLayout, columnValue);
                                    } catch (Exception ex) {
                                        LogManager.getInstance().error(ex);
                                    }
                                    break;
                                }
                            }
                        }
                    }

                }
                break;
            default:
                mLookupReturn = false;
        }
    }

    protected int getCoordinatorLayoutID() { return mBaseResourceData.ExtraResourceIDs.get("R.id.coordinator_layout"); }
    protected int getScrollViewID() { return mBaseResourceData.ExtraResourceIDs.get("R.id.scroll_view"); }
    protected int getTableLayoutID() { return mBaseResourceData.ExtraResourceIDs.get("R.id.table_layout"); }

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
        public void newSyncStatus(final Global.ActivityType activityType, final String message) {
            runOnUiThread(new Runnable() {
                public void run() {
                    processPostListener(activityType, message);
                }
            });
        }
    };

    protected void processPostListener(Global.ActivityType activityType, String message) {
        if (activityType == Global.ActivityType.InitializationStarted) {
            mInitializationStarted = true;
            LogManager.getInstance(mCurrentActivity).debug("Initialization, run on the base activity.");
            coreUIHelper.showLoadingDialog(AndroidResourceHelper.getMessage("Initializing"));
        } else if (activityType == Global.ActivityType.InitializationEnded) {
            mInitializationStarted = false;
            coreUIHelper.dismissLoadingDialog();
            User.setUser(User.getUser().personId, mCurrentActivity);
            Intent intent = MetrixActivityHelper.getInitialActivityIntent(mCurrentActivity);
            MetrixActivityHelper.startNewActivityAndFinish(mCurrentActivity, intent);
        } else if (activityType == Global.ActivityType.PasswordChangedFromServer) {
            handleServerPasswordChange();
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

    protected boolean scriptEventConsumesClick(Activity activity, String itemName) {
        boolean isConsumed = false;

        try {
            ClientScriptDef scriptDef = MetrixScreenItemManager.getEventForAlias(itemName, AttachmentWidgetManager.getAttachmentCardScreenId());
            if (scriptDef != null) {
                MetrixPublicCache.instance.addItem("theCurrentFormDef", mFormDef);
                MetrixPublicCache.instance.addItem("theCurrentLayout", mLayout);
                boolean success = MetrixClientScriptManager.executeScript(new WeakReference<Activity>(activity), scriptDef);
                if ((success && !scriptDef.mIsValidation) || !success)
                    isConsumed = true;
            }
        } catch (Exception e) {
            LogManager.getInstance().error(e);
            isConsumed = true;
        }

        return isConsumed;
    }

    protected void setupDateTimePickers() {
        if (this.mFormDef != null && this.mFormDef.tables != null) {
            for (MetrixTableDef tableDef : this.mFormDef.tables) {
                for (MetrixColumnDef columnDef : tableDef.columns) {
                    View view = MetrixControlAssistant.getControl(columnDef.id, mLayout);
                    if (view != null && (columnDef.dataType == MetrixDate.class || columnDef.dataType == MetrixDateTime.class || columnDef.dataType == MetrixTime.class) && view instanceof EditText) {
                        EditText editText = (EditText)view;
                        if (editText.isEnabled()) {
                            editText.setOnClickListener(this);
                            editText.setOnFocusChangeListener(this);
                            editText.setCompoundDrawablesWithIntrinsicBounds(0, 0, mBaseResourceData.ExtraResourceIDs.get("R.drawable.calendar"), 0);
                        }
                    }
                }
            }
        }
    }

    @Override
    protected Dialog onCreateDialog(final int id) {
        try {
            if (mFormDef == null || mFormDef.tables == null)
                return null;

            for (MetrixTableDef table : mFormDef.tables) {
                for (MetrixColumnDef columnDef : table.columns) {
                    if (columnDef.id == id) {
                        if (columnDef.dataType == MetrixDate.class) {
                            String value = MetrixControlAssistant.getValue(id, mLayout);
                            Calendar calendar = MetrixDateTimeHelper.getDate(MetrixDateTimeHelper.DATE_FORMAT, value);
                            return new DatePickerDialog(this, MetrixApplicationAssistant.getSafeDialogThemeStyleID(), new DatePickerDialog.OnDateSetListener() {
                                @Override
                                public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                                    try {
                                        MetrixControlAssistant.setValue(id, mLayout, MetrixDateTimeHelper.formatDate(year, monthOfYear, dayOfMonth));
                                    } catch (Exception e) {
                                        LogManager.getInstance().error(e);
                                    }
                                }
                            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
                        } else if (columnDef.dataType == MetrixTime.class) {
                            String value = MetrixControlAssistant.getValue(id, mLayout);
                            Calendar calendar = MetrixDateTimeHelper.getDate(MetrixDateTimeHelper.TIME_FORMAT, value);
                            return new TimePickerDialog(this, MetrixApplicationAssistant.getSafeDialogThemeStyleID(), new TimePickerDialog.OnTimeSetListener() {
                                @Override
                                public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                                    try {
                                        MetrixControlAssistant.setValue(id, mLayout, MetrixDateTimeHelper.formatTime(hourOfDay, minute));
                                    } catch (Exception e) {
                                        LogManager.getInstance().error(e);
                                    }
                                }
                            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), MetrixDateTimeHelper.use24HourTimeFormat());
                        } else if (columnDef.dataType == MetrixDateTime.class) {
                            Class mobileUIHelperClass = Class.forName("com.metrix.architecture.ui.widget.MobileUIHelper");
                            Method showDialogMethod = mobileUIHelperClass.getDeclaredMethod("showDateTimeDialog", Activity.class, int.class);
                            showDialogMethod.invoke(null, this, id);
                        }
                        break;
                    }
                }
            }
        } catch (Exception e) {
            LogManager.getInstance().error(e);
        }

        return null;
    }

    @SuppressLint("NewApi")
    @SuppressWarnings("deprecation")
    protected void toggleOrientationLock(boolean doLock) {
        // if !doLock AND cache indicates that we've lock orientation previously, unlock it
        // otherwise if doLock, then lock orientation AND cache that we have done so
        // the caching should prevent useless setRequestedOrientation calls
        if (!doLock && MetrixPublicCache.instance.containsKey("METRIX_ORIENTATION_LOCK_ACTIVE")) {
            this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
            MetrixPublicCache.instance.removeItem("METRIX_ORIENTATION_LOCK_ACTIVE");
        } else if (doLock) {
            Display display = this.getWindowManager().getDefaultDisplay();
            int rotation = display.getRotation();
            int height;
            int width;
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB_MR2) {
                width = display.getWidth();
                height = display.getHeight();
            } else {
                Point size = new Point();
                display.getSize(size);
                width = size.x;
                height = size.y;
            }

            switch (rotation) {
                case Surface.ROTATION_90:
                    if (width > height)
                        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                    else
                        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT);
                    break;
                case Surface.ROTATION_180:
                    if (height > width)
                        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT);
                    else
                        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
                    break;
                case Surface.ROTATION_270:
                    if (width > height)
                        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
                    else
                        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                    break;
                default :
                    if (height > width)
                        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                    else
                        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            }

            MetrixPublicCache.instance.addItem("METRIX_ORIENTATION_LOCK_ACTIVE", true);
        }
    }

    protected void completeFABSetup() {
        resetFABOffset();
        fabRunnable = this::showFABs;

        NestedScrollView scrollView = findViewById(getScrollViewID());
        scrollView.setOnScrollChangeListener((NestedScrollView v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) -> {
            if ((scrollY > oldScrollY) || (scrollY < oldScrollY)) {
                fabHandler.removeCallbacks(fabRunnable);
                hideFABs(mFABList);
                fabHandler.postDelayed(fabRunnable, fabDelay);
            }
        });
    }

    protected void hideFABs(List<FloatingActionButton> fabList) {
        if (mFABsToShow == null)
            mFABsToShow = new ArrayList<>();
        for (FloatingActionButton fab : fabList){
            if (fab.isOrWillBeShown()) {
                mFABsToShow.add(fab);
                fab.hide();
            }
        }
    }

    protected void showFABs() {
        for (FloatingActionButton fab : mFABsToShow){
            if (fab.isOrWillBeHidden()) {
                final Object tag = fab.getTag();
                if (!(tag instanceof String && MetrixStringHelper.valueIsEqual((String)tag, MetrixClientScriptManager.HIDDEN_BY_SCRIPT)))
                    fab.show();
            }
        }
    }

    protected int generateOffsetForFABs(List<FloatingActionButton> fabList) {
        Resources res = getResources();
        int offset = (int)res.getDimension(mBaseResourceData.ExtraResourceIDs.get("R.dimen.md_margin"));
        int visibleFABs = 0;
        for (FloatingActionButton fab : fabList){
            if (fab.isOrWillBeShown()) {
                visibleFABs++;
            }
        }

        if (visibleFABs > 0)
            offset += (int)res.getDimension(mBaseResourceData.ExtraResourceIDs.get("R.dimen.fab_offset_single"));

        if (visibleFABs > 1) {
            int extraFABs = visibleFABs - 1;
            offset += extraFABs * (int)res.getDimension(mBaseResourceData.ExtraResourceIDs.get("R.dimen.fab_offset_difference"));
        }

        return offset;
    }

    @Override
    public void resetFABOffset() {
        // Do nothing here.  Individual Add/Card screens will override.
    }

    protected void defineForm() {}
    protected void populateNonStandardControls() {}
    protected void setListeners() {}

    @Override
    protected void defaultValues() {}
    @Override
    protected void setHyperlinkBehavior() {}
    @Override
    protected void displayPreviousCount() {}
    @Override
    protected void beforeStartForError() {}
    @Override
    protected void beforeUpdateForError() {}
    @Override
    public void showIgnoreErrorDialog(String message, Class<?> nextActivity, boolean finishCurrentActivity, boolean advanceWorkflow) {}

    @Override
    public void onBackPressed() {
        if(!navigateBack)
            navigateBack = true;
        MetrixPublicCache.instance.addItem("backButtonPress_Occurred", true);

        if (MetrixStringHelper.valueIsEqual(this.getClass().getSimpleName(), "Lookup"))
            MetrixPublicCache.instance.addItem("goingBackFromLookup_AttachmentAPI", true);

        super.onBackPressed();
    }

    public void setSkinBasedColorsOnRelevantControls(ViewGroup group, String primaryColor, String secondaryColor, String hyperlinkColor, ViewGroup initialGroup, boolean doParent) {
        if (!MetrixStringHelper.isNullOrEmpty(primaryColor) || !MetrixStringHelper.isNullOrEmpty(secondaryColor) || !MetrixStringHelper.isNullOrEmpty(hyperlinkColor)) {
            if (group != null && group.getChildCount() > 0) {
                for (int i = 0; i < group.getChildCount(); i++) {
                    View v = group.getChildAt(i);
                    if (v != null && v instanceof TextView) {
                        TextView tv = (TextView) v;
                        String tag = (tv.getTag() != null) ? tv.getTag().toString() : "";
                        if (!MetrixStringHelper.isNullOrEmpty(primaryColor)
                                && (MetrixStringHelper.valueIsEqual(tag, "SCREEN_LABEL") || MetrixStringHelper.valueIsEqual(tag, "TextViewBase.Title"))) {
                            tv.setTextColor(Color.parseColor(primaryColor));
                        } else if (!MetrixStringHelper.isNullOrEmpty(secondaryColor) && MetrixStringHelper.valueIsEqual(tag, "TextViewBase.Heading")) {
                            tv.setTextColor(Color.parseColor(secondaryColor));
                        } else if (!MetrixStringHelper.isNullOrEmpty(secondaryColor) && MetrixStringHelper.valueIsEqual(tag, "TextViewBase.Region")) {
                            tv.setBackgroundColor(Color.parseColor(secondaryColor));
                        } else if (!MetrixStringHelper.isNullOrEmpty(hyperlinkColor) && tv.getAutoLinkMask() > 0) {
                            tv.setLinkTextColor(Color.parseColor(hyperlinkColor));
                        }
                    } else if (v != null && v instanceof ViewGroup && v != initialGroup) {
                        setSkinBasedColorsOnRelevantControls((ViewGroup)v, primaryColor, secondaryColor, hyperlinkColor, initialGroup, false);
                    }
                }
            }

            if (doParent && group != null) {
                ViewParent parent = group.getParent();
                if (parent != null && parent instanceof ViewGroup && parent != initialGroup) {
                    setSkinBasedColorsOnRelevantControls((ViewGroup)parent, primaryColor, secondaryColor, hyperlinkColor, initialGroup, false);
                }
            }
        }
    }

    public void setSkinBasedColorsOnButtons(View parent, Activity activity) {
        if (parent != null && parent instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) parent;
            if (group != null && group.getChildCount() > 0) {
                for (int i = 0; i < group.getChildCount(); i++) {
                    View v = group.getChildAt(i);
                    if (v != null && v instanceof Button) {
                        Button btn = (Button) v;
                        String btnTag = (btn.getTag() != null) ? btn.getTag().toString() : "";
                        String primaryColor = MetrixSkinManager.getPrimaryColor();
                        if (MetrixStringHelper.valueIsEqual(btnTag, "ButtonBase.Normal.ActionBar")) {
                            setMaterialDesignForButtons(btn,primaryColor, activity);
                        }
                    } else if (v != null && v instanceof FloatingActionButton) {
                        FloatingActionButton fab = (FloatingActionButton) v;
                        String btnTag = (fab.getTag() != null) ? fab.getTag().toString() : "";
                        if (MetrixStringHelper.valueIsEqual(btnTag, "FloatingActionBtnBase")) {
                            setSkinBasedColorsOnFloatingActionButtons(fab, MetrixSkinManager.getPrimaryColor(), activity);
                        }
                    } else if (v != null && v instanceof ViewGroup)
                        setSkinBasedColorsOnButtons(v, activity);
                }
            }
        }
    }

    public void setMaterialDesignForButtons(Button btn, String buttonBackgroundColor, Activity activity){
        int buttonBackgroundColorInt;
        if (!MetrixStringHelper.isNullOrEmpty(buttonBackgroundColor)) {
            buttonBackgroundColor = buttonBackgroundColor.substring(1); // Get rid of the # character at the beginning
            buttonBackgroundColor = MetrixStringHelper.isNullOrEmpty(buttonBackgroundColor) ? "FFFFFF" : buttonBackgroundColor;
            buttonBackgroundColorInt = (int)Long.parseLong(buttonBackgroundColor, 16);
        } else {
            buttonBackgroundColorInt = activity.getBaseContext().getResources().getColor(mBaseResourceData.ExtraResourceIDs.get("R.color.IFSPurple"));
            buttonBackgroundColor = String.format("%06X", (0xFFFFFF & buttonBackgroundColorInt));
        }

        String disabledColor = MetrixSkinManager.generateLighterVersionOfColor(buttonBackgroundColor, 0.4f, false);
        int disabledColorInt = (int)Long.parseLong(disabledColor, 16);
        int[] colors = new int[2];
        colors[0] = Color.rgb((buttonBackgroundColorInt >> 16) & 0xFF, (buttonBackgroundColorInt >> 8) & 0xFF, (buttonBackgroundColorInt >> 0) & 0xFF);
        colors[1] = Color.rgb((disabledColorInt >> 16) & 0xFF, (disabledColorInt >> 8) & 0xFF, (disabledColorInt >> 0) & 0xFF);
        int[][] states = new int[][] {
                new int[] { android.R.attr.state_enabled}, // enabled
                new int[] {-android.R.attr.state_enabled} // disabled
        };
        int[] stateColors = new int[] {
                colors[0],
                colors[1]
        };

        ColorStateList csl = new ColorStateList(states, stateColors);
        btn.setTextAppearance(activity, mBaseResourceData.ExtraResourceIDs.get("R.style.ButtonBase_Normal_ActionBar"));
        btn.setBackgroundTintList(csl);
    }

    public void setSkinBasedColorsOnFloatingActionButtons(FloatingActionButton fab, String primaryColor, Activity activity) {
        String btnTag = (fab.getTag() != null) ? fab.getTag().toString() : "";
        if (MetrixStringHelper.valueIsEqual(btnTag, "FloatingActionBtnBase")) {
            int primaryColorInt;
            if (!MetrixStringHelper.isNullOrEmpty(primaryColor)) {
                primaryColor = primaryColor.substring(1); // Get rid of the # character at the beginning
                primaryColor = MetrixStringHelper.isNullOrEmpty(primaryColor) ? "FFFFFF" : primaryColor;
                primaryColorInt = (int)Long.parseLong(primaryColor, 16);
            } else {
                primaryColorInt = activity.getBaseContext().getResources().getColor(mBaseResourceData.ExtraResourceIDs.get("R.color.IFSPurple"));
                primaryColor = String.format("%06X", (0xFFFFFF & primaryColorInt));
            }

            String disabledColor = MetrixSkinManager.generateLighterVersionOfColor(primaryColor, 0.4f, false);
            int disabledColorInt = (int)Long.parseLong(disabledColor, 16);

            int[] colors = new int[2];
            colors[0] = Color.rgb((primaryColorInt >> 16) & 0xFF, (primaryColorInt >> 8) & 0xFF, (primaryColorInt >> 0) & 0xFF);
            colors[1] = Color.rgb((disabledColorInt >> 16) & 0xFF, (disabledColorInt >> 8) & 0xFF, (disabledColorInt >> 0) & 0xFF);

            int[][] states = new int[][] {
                    new int[] { android.R.attr.state_enabled}, // enabled
                    new int[] {-android.R.attr.state_enabled} // disabled
            };

            int[] stateColors = new int[] {
                    colors[0],
                    colors[1]
            };

            ColorStateList csl = new ColorStateList(states, stateColors);
            fab.setBackgroundTintList(csl);
        }
    }

    private boolean runScreenRefreshScriptEvent() {
        boolean stopExecution = false;
        boolean isAttachmentList = (this instanceof FSMAttachmentList);

        MetrixPublicCache.instance.addItem("theCurrentFormDef", mFormDef);
        MetrixPublicCache.instance.addItem("theCurrentLayout", mLayout);

        if (!MetrixPublicCache.instance.containsKey("orientationChange_Occurred")) {
            int thisScreenID = (isAttachmentList) ? AttachmentWidgetManager.getAttachmentListScreenId() : AttachmentWidgetManager.getAttachmentCardScreenId();
            Object result = MetrixClientScriptManager.executeScriptReturningObject(new WeakReference<Activity>(this), MetrixScreenManager.getScriptDefForScreenRefresh(thisScreenID));
            if (!stopExecution && result instanceof String) {
                String strResult = (String)result;
                stopExecution = (strResult == "STOP_EXECUTION");
            }
        }

        MetrixPublicCache.instance.removeItem("orientationChange_Occurred");

        return stopExecution;
    }

    private void cacheOnStartValues() {
        if (this.mFormDef != null && this.mFormDef.tables.size() > 0) {
            Hashtable<Integer, String> onStartValues = new Hashtable<Integer, String>();

            for (MetrixTableDef tableDef : this.mFormDef.tables) {
                for (MetrixColumnDef columnDef : tableDef.columns) {
                    onStartValues.put(columnDef.id, MetrixControlAssistant.getValue(columnDef.id, mLayout));
                }
            }

            MetrixPublicCache.instance.addItem("MetrixOnStartValues", onStartValues);
        }
    }

    private void matchIdAndLaunchDialog(View v) {
        try {
            for (MetrixTableDef table : mFormDef.tables) {
                for (MetrixColumnDef columnDef : table.columns) {
                    if (columnDef.id == v.getId()) {
                        if (columnDef.dataType == MetrixDate.class || columnDef.dataType == MetrixTime.class || columnDef.dataType == MetrixDateTime.class) {
                            showDialog(columnDef.id);
                        } else {
                            Class fullTextEditClass = Class.forName("com.metrix.metrixmobile.system.FullTextEdit");
                            Intent intent = new Intent(this, fullTextEditClass);
                            intent.putExtra("table_name", table.tableName);
                            intent.putExtra("column_name", columnDef.columnName);
                            intent.putExtra("column_value", MetrixControlAssistant.getValue(columnDef, mLayout));

                            if (MetrixStringHelper.isNullOrEmpty(columnDef.friendlyName)) {
                                intent.putExtra("friendly_name", "");
                            } else {
                                intent.putExtra("friendly_name", columnDef.friendlyName);
                            }
                            startActivityForResult(intent, EDIT_FULLTEXT);
                        }
                        break;
                    }
                }
            }
        } catch (Exception e) {
            LogManager.getInstance().error(e);
        }
    }

    public boolean save(String fileName, boolean isExistingFile, boolean isFromList) {
        if (fileName == null || fileName.length() <= 0) {
            MetrixUIHelper.showSnackbar(this, getCoordinatorLayoutID(), AndroidResourceHelper.getMessage("SelectUploadFile"));
            return false;
        }

        try {
            File file = new File(fileName);
            if (file.length() <= 0) {
                MetrixUIHelper.showSnackbar(this, getCoordinatorLayoutID(), AndroidResourceHelper.getMessage("FileContentEmpty"));
                return false;
            }

            boolean usingCardFields = false;
            if(mLayout != null && mFormDef != null) {
                usingCardFields = (mLayout.getVisibility() == View.VISIBLE);
                // Validate Card fields are properly populated before proceeding, only if the Card fields are showing
                if (usingCardFields && !MetrixUpdateManager.fieldsAreValid(this, mLayout, mFormDef, false, null, false, false))
                    return false;
            }

            String[] filePieces = fileName.split("\\.");
            String fileExtension = "";
            if (filePieces.length > 1) {
                fileExtension = filePieces[filePieces.length - 1];
            }

            if (!isExistingFile) {
                if (file.exists()) {
                    //region #Attempt a file copy to the attachment folder
                    MetrixAttachmentManager metrixAttachmentManager = MetrixAttachmentManager.getInstance();
                    String copyPath = metrixAttachmentManager.getAttachmentPath() + "/" + file.getName();
                    try {
                        boolean success;
                        if (MetrixAttachmentHelper.checkImageFile(file.getPath())) {
                            success = MetrixAttachmentHelper.resizeImageFile(file.getPath(), copyPath, this);
                            if (!success)
                                return false;
                        } else {
                            if (!copyPath.equals(fileName)) {
                                success = MetrixAttachmentManager.getInstance().copyFileToNewLocation(file, new File(copyPath), this);
                                if (!success)
                                    return false;
                            }
                        }
                        fileName = copyPath;
                    } catch (Exception e) {
                        LogManager.getInstance(this).error(e);
                        throw new Exception("Error occurred while attaching an existing file ");
                    }
                    //endregion
                }
            }

            DataField attachmentIdDataField = null;
            boolean shouldUploadAttachment = true;
            String attachmentIdToUse = "";
            if (isExistingFile) {
                // If we are working with an existing attachment, then don't try to insert an attachment row again.
                attachmentIdToUse = MetrixDatabaseManager.getFieldStringValue("attachment", "attachment_id", "attachment_name = '" + file.getName() + "'");
                if (!MetrixStringHelper.isNullOrEmpty(attachmentIdToUse)) {
                    attachmentIdDataField = new DataField("attachment_id", attachmentIdToUse);
                    shouldUploadAttachment = false;
                }
            }

            ArrayList<MetrixSqlData> attachmentTrans = new ArrayList<>();
            if (shouldUploadAttachment) {
                // If user tries to change these columns via Card fields, ignore them.
                List<String> reservedAttachmentColumnNames = new ArrayList<>();
                reservedAttachmentColumnNames.add("attachment_id");
                reservedAttachmentColumnNames.add("attachment_name");
                reservedAttachmentColumnNames.add("created_dttm");
                reservedAttachmentColumnNames.add("file_extension");
                reservedAttachmentColumnNames.add("metrix_row_id"); // We don't add a DataField here, but we don't want user interference, either.
                reservedAttachmentColumnNames.add("mobile_path");
                reservedAttachmentColumnNames.add("file_size");
                reservedAttachmentColumnNames.add("stored");

                //region #Set up Attachment INSERT MetrixSqlData
                MetrixSqlData attachmentData = new MetrixSqlData("attachment", MetrixTransactionTypes.INSERT);
                attachmentIdToUse = String.valueOf(MetrixDatabaseManager.generatePrimaryKey("attachment"));
                attachmentIdDataField = new DataField("attachment_id", attachmentIdToUse);
                attachmentData.dataFields.add(attachmentIdDataField);
                attachmentData.dataFields.add(new DataField("attachment_name", file.getName()));
                attachmentData.dataFields.add(new DataField("created_dttm", MetrixDateTimeHelper.getCurrentDate(MetrixDateTimeHelper.DATE_TIME_FORMAT_WITH_SECONDS, true)));
                attachmentData.dataFields.add(new DataField("file_extension", fileExtension));
                attachmentData.dataFields.add(new DataField("mobile_path", fileName));  // mobile_path is the local path of the attachment file
                attachmentData.dataFields.add(new DataField("file_size", file.length()));
                attachmentData.dataFields.add(new DataField("stored", "N"));

                // Only consider ATTACHMENT fields from the Card that aren't reserved column names.
                if (usingCardFields) {
                    for (MetrixTableDef table : mFormDef.tables) {
                        if (MetrixStringHelper.valueIsEqual(table.tableName.toUpperCase(), "ATTACHMENT")) {
                            for (MetrixColumnDef columnDef : table.columns) {
                                if (!reservedAttachmentColumnNames.contains(columnDef.columnName)) {
                                    String controlValue = MetrixControlAssistant.getValue(columnDef.id, mLayout);
                                    attachmentData.dataFields.add(new DataField(columnDef.columnName, controlValue));
                                }
                            }
                            break;
                        }
                    }
                }
                //endregion

                attachmentTrans.add(attachmentData);
            }

            boolean successful = false;
            if (isFromList) {
                //region #Generate link table INSERT MetrixSqlData and SAVE the transaction
                LinkedHashMap<String, Object> keyMap = AttachmentWidgetManager.getKeyNameValueMap();
                if (keyMap == null || keyMap.isEmpty())
                    successful = false;
                else {
                    MetrixSqlData linkTableData = new MetrixSqlData(AttachmentWidgetManager.getLinkTable(), MetrixTransactionTypes.INSERT);
                    linkTableData.dataFields.add(attachmentIdDataField);
                    for (Map.Entry<String, Object> row : keyMap.entrySet()) {
                        String columnName = row.getKey();
                        String columnValue = AttachmentWidgetManager.convertKeyValueToDBString(columnName, row.getValue());
                        linkTableData.dataFields.add(new DataField(columnName, columnValue));
                    }
                    attachmentTrans.add(linkTableData);

                    MetrixTransaction transactionInfo = AttachmentWidgetManager.getTransactionInfo();
                    successful = MetrixUpdateManager.update(attachmentTrans, true, transactionInfo, AndroidResourceHelper.getMessage("Attachment"), this);
                }
                //endregion
            } else {
                //region #Save the attachment transaction (if there is one), pass the Attachment ID into the Attachment Field, and navigate back
                successful = true;
                if (shouldUploadAttachment) {
                    MetrixTransaction transactionInfo = AttachmentWidgetManager.getTransactionInfo();
                    successful = MetrixUpdateManager.update(attachmentTrans, true, transactionInfo, AndroidResourceHelper.getMessage("Attachment"), this);
                }

                if (successful) {
                    // Update Attachment Field UI if we successfully added the attachment OR we grabbed an existing attachment
                    AttachmentField.LaunchingAttachmentFieldData fieldData = AttachmentWidgetManager.getAttachmentFieldData();
                    if (fieldData != null) {
                        fieldData.attachmentId = attachmentIdToUse;
                        Intent data = new Intent();
                        fieldData.saveToIntent(data);
                        setResult(RESULT_OK, data);
                    }
                }
                //endregion
            }

            if (successful && shouldUploadAttachment) {
                // We successfully saved a new attachment which presently has a negative Attachment ID.
                // Upsert a row in mm_attachment_id_map accordingly, so that Sync will provide the positive key when the time comes.
                MetrixDatabaseManager.executeSql(String.format("insert or replace into mm_attachment_id_map (negative_key, positive_key) values (%s, NULL)", attachmentIdToUse));
            }

            if (!successful) {
                MetrixUIHelper.showSnackbar(this, getCoordinatorLayoutID(), AndroidResourceHelper.getMessage("DataErrorOnUpload"));
                return false;
            }
        } catch (Exception e) {
            LogManager.getInstance(this).error(e);
            return false;
        }
        return true;
    }

    // for OnDemand
    protected static String getToken(String baseServiceUrl, MetrixRemoteExecutor remoteExecutor) {
        String getTokenServiceUrl = MetrixSyncManager.generateRestfulServiceUrl(baseServiceUrl, Global.MessageType.Messages, null, 0, null, "token");

        try {
            String response = remoteExecutor.executeGet(getTokenServiceUrl).replace("\\", "").replace("\"", "");

            if (response != null) {
                return response;
            }
        } catch (RemoteMessagesHandler.HandlerException ex) {
            LogManager.getInstance().error(ex);
            return "";
        } catch (JSONException ex) {
            LogManager.getInstance().error(ex);
            return "";
        }

        return "";
    }

    protected boolean removeAnyFailedDownloadFiles() {
        // Note: Key = fileNames and Value = attachmentIds (odd, but taken from existing convention)
        // Hashtable<String, String> FAILED_DOWNLOAD_FILES
        // Hashtable<String, String> DOWNLOAD_FILES

        boolean removedOnDemandSpinner = false;

        if (MetrixPublicCache.instance.containsKey("FAILED_DOWNLOAD_FILES")) {
            Hashtable<String, String> failedDownloads = (Hashtable<String, String>) MetrixPublicCache.instance.getItem("FAILED_DOWNLOAD_FILES");

            // Removed from "DOWNLOAD_FILES" if present.
            if (MetrixPublicCache.instance.containsKey("DOWNLOAD_FILES")) {

                Hashtable<String, String> downloadFiles = (Hashtable<String, String>) MetrixPublicCache.instance.getItem("DOWNLOAD_FILES");
                Enumeration<String> downloadFiles_Keys = downloadFiles.keys();

                while (downloadFiles_Keys.hasMoreElements()) {
                    String fileName = downloadFiles_Keys.nextElement();

                    if (failedDownloads.contains(fileName)) {

                        failedDownloads.remove(fileName); // Removed from "FAILED_DOWNLOAD_FILES"
                        downloadFiles.remove(fileName); // ALso remove from "DOWNLOAD_FILES"
                        removedOnDemandSpinner = true; // remove spinner UI
                    }
                }

                if (failedDownloads.size() == 0) {
                    MetrixPublicCache.instance.removeItem("FAILED_DOWNLOAD_FILES");
                } else {
                    MetrixPublicCache.instance.addItem("FAILED_DOWNLOAD_FILES", failedDownloads);
                }

                if (downloadFiles.size() == 0) {
                    MetrixPublicCache.instance.removeItem("DOWNLOAD_FILES");
                } else {
                    MetrixPublicCache.instance.addItem("DOWNLOAD_FILES", downloadFiles);
                }
            } else {
                MetrixPublicCache.instance.removeItem("FAILED_DOWNLOAD_FILES");
                removedOnDemandSpinner = true;
            }

            if (removedOnDemandSpinner) {
                MobileApplication.stopSync(mCurrentActivity);
                MobileApplication.startSync(mCurrentActivity);
            }
        }
        return removedOnDemandSpinner;
    }
}
