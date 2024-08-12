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

import com.metrix.architecture.assistants.MetrixControlAssistant;
import com.metrix.architecture.constants.MetrixConstraintOperands;
import com.metrix.architecture.constants.MetrixSaveResult;
import com.metrix.architecture.constants.MetrixTransactionTypes;
import com.metrix.architecture.database.MetrixDatabaseManager;
import com.metrix.architecture.designer.MetrixScreenManager;
import com.metrix.architecture.designer.MetrixWorkflowManager;
import com.metrix.architecture.managers.MetrixUpdateManager;
import com.metrix.architecture.metadata.MetrixActivityDef;
import com.metrix.architecture.metadata.MetrixConstraintDef;
import com.metrix.architecture.metadata.MetrixFormDef;
import com.metrix.architecture.metadata.MetrixTableDef;
import com.metrix.architecture.metadata.MetrixTransaction;
import com.metrix.architecture.scripting.ClientScriptDef;
import com.metrix.architecture.scripting.MetrixClientScriptManager;
import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.architecture.utilities.LogManager;
import com.metrix.architecture.utilities.MetrixActivityHelper;
import com.metrix.architecture.utilities.MetrixStringHelper;
import com.metrix.architecture.utilities.MetrixUIHelper;
import com.metrix.metrixmobile.R;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MetadataQuoteActivity extends QuoteActivity {
    private HashMap<String, String> currentScreenProperties;
    private String primaryTable, linkedScreenId, whereClauseScript;
    private FloatingActionButton mAddButton, mSaveButton, mNextButton, mCorrectErrorButton;
    private Button mViewPreviousEntriesButton;
    private boolean screenExistsInCurrentWorkflow, isStandardUpdateOnly, gotHereFromLinkedScreen;
    private List<FloatingActionButton> mFABList;
    private List<FloatingActionButton> mFABsToShow;

    @SuppressLint("DefaultLocale")
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        screenExistsInCurrentWorkflow = MetrixWorkflowManager.isScreenExistsInCurrentWorkflow(this, codeLessScreenId);
        isStandardUpdateOnly = MetrixScreenManager.isStandardUpdateOnly(codeLessScreenId);
        gotHereFromLinkedScreen = (this.getIntent().getExtras() != null && this.getIntent().getExtras().containsKey("NavigatedFromLinkedScreen"));

        setContentView(R.layout.yycsmd_quote_activity);

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
        mAddButton = (FloatingActionButton) findViewById(R.id.save);
        mSaveButton = (FloatingActionButton) findViewById(R.id.update);
        mNextButton = (FloatingActionButton) findViewById(R.id.next);
        mCorrectErrorButton = (FloatingActionButton) findViewById(R.id.correct_error);
        mViewPreviousEntriesButton = (Button) findViewById(R.id.view_previous_entries);

        if (mAddButton != null) {
            mAddButton.setOnClickListener(this);
        }
        if (mSaveButton != null) {
            mSaveButton.setOnClickListener(this);
        }
        if (mNextButton != null) {
            mNextButton.setOnClickListener(this);
        }
        if (mCorrectErrorButton != null) {
            mCorrectErrorButton.setOnClickListener(this);
        }
        if (mViewPreviousEntriesButton != null) {
            mViewPreviousEntriesButton.setOnClickListener(this);
            AndroidResourceHelper.setResourceValues(mViewPreviousEntriesButton, "List1Arg");
        }

        hideAllButtons();
        mFABList = new ArrayList<FloatingActionButton>();
        float offset = 0;
        if (!mHandlingErrors) {
            if (isStandardUpdateOnly) {
                MetrixControlAssistant.setButtonVisibility(mSaveButton, View.VISIBLE);
                offset = getResources().getDimension((R.dimen.fab_offset_single));
                if (screenExistsInCurrentWorkflow) {
                    MetrixControlAssistant.setButtonVisibility(mNextButton, View.VISIBLE);
                    mFABList.add(mNextButton);
                    offset = getResources().getDimension((R.dimen.fab_offset_double));
                }
                mFABList.add(mSaveButton);
            } else {
                if (gotHereFromLinkedScreen) {
                    MetrixControlAssistant.setButtonVisibility(mSaveButton, View.VISIBLE);
                    mFABList.add(mSaveButton);
                    offset = getResources().getDimension((R.dimen.fab_offset_single));
                } else {
                    MetrixControlAssistant.setButtonVisibility(mAddButton, View.VISIBLE);
                    offset = getResources().getDimension((R.dimen.fab_offset_single));
                    if (screenExistsInCurrentWorkflow) {
                        MetrixControlAssistant.setButtonVisibility(mNextButton, View.VISIBLE);
                        mFABList.add(mNextButton);
                        offset = getResources().getDimension((R.dimen.fab_offset_double));
                    }
                    mFABList.add(mAddButton);

                    if (!MetrixStringHelper.isNullOrEmpty(linkedScreenId)) {
                        MetrixControlAssistant.setButtonVisibility(mViewPreviousEntriesButton, View.VISIBLE);
                        // The amount subtracted it equal to the button height plus the top and bottom margins of the button
                        offset -= getResources().getDimension((R.dimen.button_height)) + (2 * getResources().getDimension((R.dimen.md_margin)));
                    }
                }
            }
        }

        fabRunnable = this::showFABs;

        NestedScrollView scrollView = findViewById(R.id.scroll_view);
        mLayout.setPadding(mLayout.getPaddingLeft(),mLayout.getPaddingTop(),mLayout.getPaddingRight(),(int)offset);
        scrollView.setOnScrollChangeListener((NestedScrollView v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) -> {
            if ((scrollY > oldScrollY) || (scrollY < oldScrollY)) {
                fabHandler.removeCallbacks(fabRunnable);
                hideFABs(mFABList);
                fabHandler.postDelayed(fabRunnable, fabDelay);
            }
        });
    }

    @SuppressLint("DefaultLocale")
    @Override
    public void onClick(View v) {
        if (scriptEventConsumesClick(this, v))
            return;

        Intent intent;
        switch (v.getId()) {
            case R.id.save:
                MetrixTransaction transactionInfo = MetrixTransaction.getTransaction("quote", "quote_id");
                MetrixSaveResult saveResult = MetrixUpdateManager.update(this, mLayout, mFormDef, transactionInfo, false, null, false, String.format("MQA-%s", codeLessScreenName));
                if (saveResult == MetrixSaveResult.ERROR)
                    return;

                intent = MetrixActivityHelper.createActivityIntent(this, MetadataQuoteActivity.class);
                intent.putExtra("ScreenID", codeLessScreenId);
                MetrixActivityHelper.startNewActivityAndFinish(this, intent);
                break;

            case R.id.next:
                if (anyOnStartValuesChanged()) {
                    transactionInfo = MetrixTransaction.getTransaction("quote", "quote_id");
                    saveResult = MetrixUpdateManager.update(this, mLayout, mFormDef, transactionInfo, true, null, false, String.format("MQA-%s", codeLessScreenName), true);
                    if (saveResult == MetrixSaveResult.ERROR || saveResult == MetrixSaveResult.ERROR_WITH_CONTINUE)
                        return;
                } else
                    MetrixWorkflowManager.advanceWorkflow(this);
                break;

            case R.id.update:
                if (anyOnStartValuesChanged()) {
                    transactionInfo = MetrixTransaction.getTransaction("quote", "quote_id");
                    saveResult = MetrixUpdateManager.update(this, mLayout, mFormDef, transactionInfo, true, null, false, String.format("MQA-%s", codeLessScreenName));
                    if (saveResult == MetrixSaveResult.ERROR || saveResult == MetrixSaveResult.ERROR_WITH_CONTINUE)
                        return;

                    if (isStandardUpdateOnly) {
                        //we should show the same/current page
                        intent = MetrixActivityHelper.createActivityIntent(this, MetadataQuoteActivity.class);
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
                transactionInfo = MetrixTransaction.getTransaction("quote", "quote_id");
                saveResult = MetrixUpdateManager.update(this, mLayout, mFormDef, transactionInfo, false, SyncServiceMonitor.class, true, String.format("MQA-%s", codeLessScreenName));
                if (saveResult == MetrixSaveResult.ERROR)
                    MetrixUIHelper.showSnackbar(this, R.id.coordinator_layout, AndroidResourceHelper.getMessage("Error"));
                break;

            case R.id.view_previous_entries:
                if (!MetrixStringHelper.isNullOrEmpty(linkedScreenId)) {
                    int intLinkedScreenId = Integer.valueOf(linkedScreenId);
                    String screenType = MetrixScreenManager.getScreenType(intLinkedScreenId);
                    if (!MetrixStringHelper.isNullOrEmpty(screenType)) {
                        if (screenType.toLowerCase().contains("list")) {
                            intent = MetrixActivityHelper.createActivityIntent(this, MetadataListQuoteActivity.class);
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
        final boolean finalAdvanceWorkflow = advanceWorkflow;

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
                        } else if (finalAdvanceWorkflow || finalFinishCurrentActivity) {
                            // next activity and advance workflow should be mutually exclusive
                            // finishCurrentActivity should still fire, regardless of whether we have a next activity specified
                            if (finalAdvanceWorkflow)
                                MetrixWorkflowManager.advanceWorkflow(currentActivity);

                            if (finalFinishCurrentActivity)
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

