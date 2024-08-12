package com.metrix.metrixmobile.system;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.core.widget.NestedScrollView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.metrix.architecture.assistants.MetrixControlAssistant;
import com.metrix.architecture.constants.MetrixConstraintOperands;
import com.metrix.architecture.constants.MetrixSaveResult;
import com.metrix.architecture.constants.MetrixTransactionTypes;
import com.metrix.architecture.database.MetrixDatabaseManager;
import com.metrix.architecture.designer.MetrixScreenManager;
import com.metrix.architecture.managers.MetrixUpdateManager;
import com.metrix.architecture.metadata.MetrixActivityDef;
import com.metrix.architecture.metadata.MetrixConstraintDef;
import com.metrix.architecture.metadata.MetrixFormDef;
import com.metrix.architecture.metadata.MetrixTableDef;
import com.metrix.architecture.metadata.MetrixTransaction;
import com.metrix.architecture.scripting.ClientScriptDef;
import com.metrix.architecture.scripting.MetrixClientScriptManager;
import com.metrix.architecture.superclasses.MetrixButtonState;
import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.architecture.utilities.LogManager;
import com.metrix.architecture.utilities.MetrixActivityHelper;
import com.metrix.architecture.utilities.MetrixStringHelper;
import com.metrix.architecture.utilities.MetrixUIHelper;
import com.metrix.metrixmobile.R;

import java.lang.ref.WeakReference;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MetadataStandardActivity extends MetrixActivity {
    private HashMap<String, String> currentScreenProperties;
    private String primaryTable, linkedScreenId, whereClauseScript;
    private FloatingActionButton mAddButton, mSaveButton, mNextButton, mCorrectErrorButton;
    private Button mViewPreviousEntriesButton;
    private boolean isStandardUpdateOnly, gotHereFromLinkedScreen;
    private List<FloatingActionButton> mFABList;
    private float mOffset;

    @SuppressLint("DefaultLocale")
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        isStandardUpdateOnly = MetrixScreenManager.isStandardUpdateOnly(codeLessScreenId);
        gotHereFromLinkedScreen = (this.getIntent().getExtras() != null && this.getIntent().getExtras().containsKey("NavigatedFromLinkedScreen"));

        setContentView(R.layout.yycsmd_standard_activity);

        try {
            currentScreenProperties = MetrixScreenManager.getScreenProperties(codeLessScreenId);

            if (currentScreenProperties != null) {
                if (currentScreenProperties.containsKey("primary_table")) {
                    primaryTable = currentScreenProperties.get("primary_table");
                    if (MetrixStringHelper.isNullOrEmpty(primaryTable)) {
                        throw new Exception("Primary table is required for code-less screen");
                    }
                    primaryTable = primaryTable.toLowerCase();
                }
                if (currentScreenProperties.containsKey("linked_screen_id"))
                    linkedScreenId = currentScreenProperties.get("linked_screen_id");
                if (currentScreenProperties.containsKey("where_clause_script"))
                    whereClauseScript = currentScreenProperties.get("where_clause_script");
            }
        } catch (Exception e) {
            LogManager.getInstance(this).error(e);
        }

        if (isStandardUpdateOnly) {
            ClientScriptDef clientScriptDef = MetrixClientScriptManager.getScriptDefForScriptID(whereClauseScript);
            if (MetrixStringHelper.isNullOrEmpty(primaryTable) || MetrixStringHelper.isNullOrEmpty(whereClauseScript) || clientScriptDef == null) {
                LogManager.getInstance().error(String.format("Standard update-only screen (ID %d) is missing metadata", codeLessScreenId));
                return;
            }

            String whereClause = MetrixClientScriptManager.executeScriptReturningString(new WeakReference<Activity>(this), clientScriptDef);
            String metrixRowID = MetrixDatabaseManager.getFieldStringValue(primaryTable, "metrix_row_id", whereClause);
            mActivityDef = new MetrixActivityDef(MetrixTransactionTypes.UPDATE, "metrix_row_id", metrixRowID);
        }
    }

    public void onStart() {
        super.onStart();
        setupActionBar();
        displayPreviousCount();
        mLayout = (ViewGroup) findViewById(R.id.table_layout);
    }

    @SuppressLint("DefaultLocale")
    protected void defineForm() {
        MetrixTableDef metrixTableDef = null;
        if (this.mActivityDef != null) {
            metrixTableDef = new MetrixTableDef(primaryTable, this.mActivityDef.TransactionType);
            if (this.mActivityDef.Keys != null) {
                metrixTableDef.constraints.add(new MetrixConstraintDef("metrix_row_id", MetrixConstraintOperands.EQUALS, String.valueOf(this.mActivityDef.Keys.get("metrix_row_id")), double.class));
            }
        } else {
            metrixTableDef = new MetrixTableDef(primaryTable, MetrixTransactionTypes.INSERT);
        }

        this.mFormDef = new MetrixFormDef(metrixTableDef);
    }

    protected void setupActionBar() {
        TextView actionBarTitle = (TextView) findViewById(R.id.action_bar_title);
        String actionBarScript = currentScreenProperties.get("action_bar_script");
        if (actionBarTitle != null && !MetrixStringHelper.isNullOrEmpty(actionBarScript)) {
            ClientScriptDef actionBarScriptDef = MetrixClientScriptManager.getScriptDefForScriptID(actionBarScript);
            Object result = MetrixClientScriptManager.executeScriptReturningObject(new WeakReference<Activity>(this), actionBarScriptDef);

            String title;
            if (result == null)
                title = "";
            else if (result instanceof String)
                title = (String) result;
            else if (result instanceof Double && (((Double) result) % 1 == 0))
                title = new DecimalFormat("#").format(result);
            else
                title = result.toString();

            actionBarTitle.setText(title);
        }
    }

    protected void displayPreviousCount() {
        if (!MetrixStringHelper.isNullOrEmpty(linkedScreenId)) {
            try {
                if (!MetrixStringHelper.isNullOrEmpty(whereClauseScript)) {
                    ClientScriptDef clientScriptDef = MetrixClientScriptManager.getScriptDefForScriptID(whereClauseScript);
                    if (clientScriptDef != null) {
                        String result = MetrixClientScriptManager.executeScriptReturningString(new WeakReference<Activity>(mCurrentActivity), clientScriptDef);
                        int count = MetrixDatabaseManager.getCount(primaryTable, result);
                        MetrixControlAssistant.setValue(R.id.view_previous_entries, (ViewGroup) findViewById(R.id.view_previous_entries_bar), AndroidResourceHelper.getMessage("List1Arg", String.valueOf(count)));
                    } else
                        MetrixControlAssistant.setValue(R.id.view_previous_entries, (ViewGroup) findViewById(R.id.view_previous_entries_bar), AndroidResourceHelper.getMessage("List"));
                } else
                    MetrixControlAssistant.setValue(R.id.view_previous_entries, (ViewGroup) findViewById(R.id.view_previous_entries_bar), AndroidResourceHelper.getMessage("List"));
            } catch (Exception e) {
                LogManager.getInstance(this).error(e);
            }
        }
    }

    protected void setListeners() {
        if (mFABList == null)
            mFABList = new ArrayList<FloatingActionButton>();
        else
            mFABList.clear();

        mAddButton = (FloatingActionButton) findViewById(R.id.save);
        mSaveButton = (FloatingActionButton) findViewById(R.id.update);
        mNextButton = (FloatingActionButton) findViewById(R.id.next);
        mCorrectErrorButton = (FloatingActionButton) findViewById(R.id.correct_error);
        mViewPreviousEntriesButton = (Button) findViewById(R.id.view_previous_entries);

        if (mAddButton != null) {
            mAddButton.setOnClickListener(this);
            mAddButton.setContentDescription("btnSave");
        }
        if (mSaveButton != null) {
            mSaveButton.setOnClickListener(this);
            mSaveButton.setContentDescription("btnUpdate");
        }
        if (mNextButton != null) {
            mNextButton.setOnClickListener(this);
            mNextButton.setContentDescription("btnNext");
        }
        if (mCorrectErrorButton != null) {
            mCorrectErrorButton.setOnClickListener(this);
            mCorrectErrorButton.setContentDescription("btnCorrect_error");
        }
        if (mViewPreviousEntriesButton != null) {
            mViewPreviousEntriesButton.setOnClickListener(this);
            mViewPreviousEntriesButton.setContentDescription("btnList");
            AndroidResourceHelper.setResourceValues(mViewPreviousEntriesButton, "List1Arg");
        }

        hideAllButtons();
        if (!mHandlingErrors) {
            // By default, mark these with a tag denoting they are hidden
            mAddButton.setTag(MetrixClientScriptManager.HIDDEN_BY_SCRIPT);
            mSaveButton.setTag(MetrixClientScriptManager.HIDDEN_BY_SCRIPT);
            mNextButton.setTag(MetrixClientScriptManager.HIDDEN_BY_SCRIPT);

            // Determine proper visibility; if visible, clear tag
            if (isStandardUpdateOnly) {
                MetrixControlAssistant.setButtonVisibility(mSaveButton, View.VISIBLE);
                MetrixControlAssistant.setButtonVisibility(mNextButton, View.VISIBLE);
                mSaveButton.setTag(null);
                mNextButton.setTag(null);
            } else {
                if (gotHereFromLinkedScreen) {
                    MetrixControlAssistant.setButtonVisibility(mSaveButton, View.VISIBLE);
                    mSaveButton.setTag(null);
                } else {
                    MetrixControlAssistant.setButtonVisibility(mAddButton, View.VISIBLE);
                    MetrixControlAssistant.setButtonVisibility(mNextButton, View.VISIBLE);
                    mAddButton.setTag(null);
                    mNextButton.setTag(null);
                }
            }

            // Add all buttons to mFABList, so that client scripting can show/hide them and the scrolling framework will still operate well
            mFABList.add(mNextButton);
            mFABList.add(mSaveButton);
            mFABList.add(mAddButton);
        }

        resetFABOffset();
        fabRunnable = this::showFABs;

        NestedScrollView scrollView = findViewById(R.id.scroll_view);
        scrollView.setOnScrollChangeListener((NestedScrollView v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) -> {
            if ((scrollY > oldScrollY) || (scrollY < oldScrollY)) {
                fabHandler.removeCallbacks(fabRunnable);
                hideFABs(mFABList);
                fabHandler.postDelayed(fabRunnable, fabDelay);
            }
        });
    }

    @Override
    public void resetFABOffset() {
        try {
            // For OSP-2116, we implement a specific take on this requirement.
            // This probably should be put in MetrixActivity when OSP-2144 is executed (TODO for OSP-2144)

            // Poke the FAB rendering first
            hideFABs(mFABList);
            showFABs();

            // Now that FABs are showing, set the offset on mLayout
            mOffset = 0;
            if (!mHandlingErrors && !MetrixStringHelper.isNullOrEmpty(linkedScreenId)) {
                MetrixControlAssistant.setButtonVisibility(mViewPreviousEntriesButton, View.VISIBLE);
                // The amount subtracted it equal to the button height plus the top and bottom margins of the button
                mOffset -= getResources().getDimension((R.dimen.button_height)) + (2 * getResources().getDimension((R.dimen.md_margin)));
            }
            mOffset += generateOffsetForFABs(mFABList);
            mLayout.setPadding(mLayout.getPaddingLeft(),mLayout.getPaddingTop(),mLayout.getPaddingRight(),(int)mOffset);
        } catch (Exception e) {
            LogManager.getInstance().error(e);
        }
    }

    @Override
    protected void saveBasicButtonState(Bundle savedInstanceState, String currentActivityName) {
        // For OSP-2116, we implement a specific take on this requirement.
        // Since we rely on the tag to determine dynamically-set FAB visibility, we need to remember this when saving state
        // This probably should be put in MetrixActivity when OSP-2144 is executed (TODO for OSP-2144)

        FloatingActionButton addButton = (FloatingActionButton) findViewById(R.id.save);
        FloatingActionButton saveButton = (FloatingActionButton) findViewById(R.id.update);
        FloatingActionButton nextButton = (FloatingActionButton) findViewById(R.id.next);
        Button listButton = (Button) findViewById(R.id.view_previous_entries);

        MetrixButtonState currState;
        String cacheKey;

        if (addButton != null) {
            currState = new MetrixButtonState("", addButton.isEnabled(), (addButton.getVisibility() == View.VISIBLE), (String)addButton.getTag());
            cacheKey = String.format("%1$s__%2$s", currentActivityName, String.valueOf(R.id.save));
            savedInstanceState.putSerializable(cacheKey, currState);
        }

        if (saveButton != null) {
            currState = new MetrixButtonState("", saveButton.isEnabled(), (saveButton.getVisibility() == View.VISIBLE), (String)saveButton.getTag());
            cacheKey = String.format("%1$s__%2$s", currentActivityName, String.valueOf(R.id.update));
            savedInstanceState.putSerializable(cacheKey, currState);
        }

        if (nextButton != null) {
            currState = new MetrixButtonState("", nextButton.isEnabled(), (nextButton.getVisibility() == View.VISIBLE), (String)nextButton.getTag());
            cacheKey = String.format("%1$s__%2$s", currentActivityName, String.valueOf(R.id.next));
            savedInstanceState.putSerializable(cacheKey, currState);
        }

        if (listButton != null) {
            currState = new MetrixButtonState(listButton.getText().toString(), listButton.isEnabled(), (listButton.getVisibility() == View.VISIBLE));
            cacheKey = String.format("%1$s__%2$s", currentActivityName, String.valueOf(R.id.view_previous_entries));
            savedInstanceState.putSerializable(cacheKey, currState);
        }
    }

    @Override
    protected void restoreBasicButtonState(Bundle savedInstanceState, String currentActivityName) {
        // For OSP-2116, we implement a specific take on this requirement.
        // Since we rely on the tag to determine dynamically-set FAB visibility, we need to set this when restoring state
        // This probably should be put in MetrixActivity when OSP-2144 is executed (TODO for OSP-2144)

        FloatingActionButton addButton = (FloatingActionButton) findViewById(R.id.save);
        FloatingActionButton saveButton = (FloatingActionButton) findViewById(R.id.update);
        FloatingActionButton nextButton = (FloatingActionButton) findViewById(R.id.next);
        Button listButton = (Button) findViewById(R.id.view_previous_entries);
        MetrixButtonState currState;
        String cacheKey;

        if (addButton != null) {
            cacheKey = String.format("%1$s__%2$s", currentActivityName, String.valueOf(R.id.save));
            currState = (MetrixButtonState)savedInstanceState.getSerializable(cacheKey);
            if (currState != null) {
                addButton.setEnabled(currState.mIsEnabled);
                MetrixControlAssistant.setButtonVisibility(addButton, (currState.mIsVisible) ? View.VISIBLE : View.GONE);
                addButton.setTag(currState.mTag);
            }
        }

        if (saveButton != null) {
            cacheKey = String.format("%1$s__%2$s", currentActivityName, String.valueOf(R.id.update));
            currState = (MetrixButtonState)savedInstanceState.getSerializable(cacheKey);
            if (currState != null) {
                saveButton.setEnabled(currState.mIsEnabled);
                MetrixControlAssistant.setButtonVisibility(saveButton, (currState.mIsVisible) ? View.VISIBLE : View.GONE);
                saveButton.setTag(currState.mTag);
            }
        }

        if (nextButton != null) {
            cacheKey = String.format("%1$s__%2$s", currentActivityName, String.valueOf(R.id.next));
            currState = (MetrixButtonState)savedInstanceState.getSerializable(cacheKey);
            if (currState != null) {
                nextButton.setEnabled(currState.mIsEnabled);
                MetrixControlAssistant.setButtonVisibility(nextButton, (currState.mIsVisible) ? View.VISIBLE : View.GONE);
                nextButton.setTag(currState.mTag);
            }
        }

        if (listButton != null) {
            cacheKey = String.format("%1$s__%2$s", currentActivityName, String.valueOf(R.id.view_previous_entries));
            currState = (MetrixButtonState)savedInstanceState.getSerializable(cacheKey);
            if (currState != null) {
                listButton.setText(currState.mLabel);
                listButton.setEnabled(currState.mIsEnabled);
                MetrixControlAssistant.setButtonVisibility(listButton, (currState.mIsVisible) ? View.VISIBLE : View.GONE);
            }
        }

        resetFABOffset();
    }

    @SuppressLint("DefaultLocale")
    @Override
    public void onClick(View v) {
        if (scriptEventConsumesClick(this, v))
            return;

        Intent intent;
        switch (v.getId()) {
            case R.id.save:
                MetrixTransaction transactionInfo = new MetrixTransaction();
                MetrixSaveResult saveResult = MetrixUpdateManager.update(this, mLayout, mFormDef, transactionInfo, false, null, false, String.format("MSTA-%s", codeLessScreenName));
                if (saveResult == MetrixSaveResult.ERROR)
                    return;

                intent = MetrixActivityHelper.createActivityIntent(this, MetadataStandardActivity.class);
                intent.putExtra("ScreenID", codeLessScreenId);
                MetrixActivityHelper.startNewActivityAndFinish(this, intent);
                break;

            case R.id.next:
                if (anyOnStartValuesChanged()) {
                    transactionInfo = new MetrixTransaction();
                    saveResult = MetrixUpdateManager.update(this, mLayout, mFormDef, transactionInfo, true, null, true, String.format("MSTA-%s", codeLessScreenName), false);
                    if (saveResult == MetrixSaveResult.ERROR || saveResult == MetrixSaveResult.ERROR_WITH_CONTINUE)
                        return;
                } else
                    this.finish();
                break;

            case R.id.update:
                if (anyOnStartValuesChanged()) {
                    transactionInfo = new MetrixTransaction();
                    saveResult = MetrixUpdateManager.update(this, mLayout, mFormDef, transactionInfo, true, null, false, String.format("MSTA-%s", codeLessScreenName));
                    if (saveResult == MetrixSaveResult.ERROR || saveResult == MetrixSaveResult.ERROR_WITH_CONTINUE)
                        return;

                    if (isStandardUpdateOnly) {
                        //we should show the same/current page
                        intent = MetrixActivityHelper.createActivityIntent(this, MetadataStandardActivity.class);
                        intent.putExtra("ScreenID", codeLessScreenId);
                        MetrixActivityHelper.startNewActivityAndFinish(this, intent);
                    } else
                        finish();
                } else {
                    if (!isStandardUpdateOnly)
                        finish();
                }
                break;

            case R.id.correct_error:
                transactionInfo = new MetrixTransaction();
                saveResult = MetrixUpdateManager.update(this, mLayout, mFormDef, transactionInfo, false, SyncServiceMonitor.class, true, String.format("MSTA-%s", codeLessScreenName));
                if (saveResult == MetrixSaveResult.ERROR)
                    MetrixUIHelper.showSnackbar(this, R.id.coordinator_layout, AndroidResourceHelper.getMessage("Error"));
                break;

            case R.id.view_previous_entries:
                if (!MetrixStringHelper.isNullOrEmpty(linkedScreenId)) {
                    int intLinkedScreenId = Integer.valueOf(linkedScreenId);
                    String screenType = MetrixScreenManager.getScreenType(intLinkedScreenId);
                    if (!MetrixStringHelper.isNullOrEmpty(screenType)) {
                        if (screenType.toLowerCase().contains("list")) {
                            intent = MetrixActivityHelper.createActivityIntent(this, MetadataListActivity.class);
                            intent.putExtra("ScreenID", intLinkedScreenId);
                            intent.putExtra("NavigatedFromLinkedScreen", true);
                            MetrixActivityHelper.startNewActivity(this, intent);
                        } else
                            MetrixUIHelper.showSnackbar(this, R.id.coordinator_layout, AndroidResourceHelper.getMessage("YYCSWrongScreenType", screenType));
                    }
                }
                break;

            default:
                super.onClick(v);
        }
    }

    private void hideAllButtons() {
        MetrixControlAssistant.setButtonVisibility(mAddButton, View.GONE);
        MetrixControlAssistant.setButtonVisibility(mSaveButton, View.GONE);
        MetrixControlAssistant.setButtonVisibility(mNextButton, View.GONE);
        MetrixControlAssistant.setButtonVisibility(mViewPreviousEntriesButton, View.GONE);
    }

    @Override
    public void showIgnoreErrorDialog(String message, Class<?> nextActivity, boolean finishCurrentActivity, boolean advanceWorkflow) {
        if (!message.endsWith(".")) {
            message = message + ".";
        }
        message = message + " " + AndroidResourceHelper.getMessage("ContinueWithoutSaving");

        final Class<?> finalNextActivity = nextActivity;
        final boolean finalFinishCurrentActivity = finishCurrentActivity;
        final Activity currentActivity = this;

        new AlertDialog.Builder(this).setCancelable(false).setMessage(message).setPositiveButton(AndroidResourceHelper.getMessage("Yes"),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        if (finalNextActivity != null) {
                            if (finalFinishCurrentActivity) {
                                Intent intent = MetrixActivityHelper.createActivityIntent(currentActivity, finalNextActivity);
                                MetrixActivityHelper.startNewActivityAndFinish(currentActivity, intent);
                            } else {
                                Intent intent = MetrixActivityHelper.createActivityIntent(currentActivity, finalNextActivity);
                                MetrixActivityHelper.startNewActivity(currentActivity, intent);
                            }
                        } else if (finalFinishCurrentActivity) {
                            currentActivity.finish();
                        } else {
                            // this is the Save case, where we are not advancing workflow, nor have specified that we are finishing or going to a screen
                            if (isStandardUpdateOnly) {
                                //we should show the same/current page
                                Intent intent = MetrixActivityHelper.createActivityIntent(currentActivity, MetadataQuoteActivity.class);
                                intent.putExtra("ScreenID", codeLessScreenId);
                                MetrixActivityHelper.startNewActivityAndFinish(currentActivity, intent);
                            } else
                                currentActivity.finish();
                        }
                    }
                }).setNegativeButton(AndroidResourceHelper.getMessage("No"), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
            }
        }).create().show();
    }
}
