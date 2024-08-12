package com.metrix.metrixmobile;

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
import com.metrix.architecture.managers.MetrixUpdateManager;
import com.metrix.architecture.metadata.MetrixColumnDef;
import com.metrix.architecture.metadata.MetrixConstraintDef;
import com.metrix.architecture.metadata.MetrixFormDef;
import com.metrix.architecture.metadata.MetrixTableDef;
import com.metrix.architecture.metadata.MetrixTransaction;
import com.metrix.architecture.superclasses.MetrixControlState;
import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.architecture.utilities.LogManager;
import com.metrix.architecture.utilities.MetrixActivityHelper;
import com.metrix.architecture.utilities.MetrixCurrentKeysHelper;
import com.metrix.architecture.utilities.MetrixUIHelper;
import com.metrix.metrixmobile.system.MetrixActivity;
import com.metrix.metrixmobile.system.SyncServiceMonitor;

import java.util.ArrayList;
import java.util.List;

public class ReceivingUnitPO extends MetrixActivity {
    private FloatingActionButton mAddButton, mSaveButton, mFinishButton, mCorrectErrorButton;
    private Button mViewPreviousEntriesButton;
    private String mRcvId, mRcvSeq;
    private List<FloatingActionButton> mFABList;
    private List<FloatingActionButton> mFABsToShow;

    public void onCreate(Bundle savedInstanceState) {
        mRcvId = MetrixCurrentKeysHelper.getKeyValue("receiving_detail", "receiving_id");
        mRcvSeq = MetrixCurrentKeysHelper.getKeyValue("receiving_detail", "sequence");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.receiving_unit_po);
    }

    public void onStart() {
        super.onStart();
        setReceivingActionBarTitle();
        displayPreviousCount();
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        // restore the PK values, which we don't do generically, but we need to do here (because we need to remember the compound key values)...
        String currentActivityName = mCurrentActivity.getClass().getSimpleName();
        if (mFormDef != null && mFormDef.tables.size() > 0) {
            for (MetrixTableDef table : mFormDef.tables) {
                for (MetrixColumnDef col : table.columns) {
                    if (!col.primaryKey) continue;
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

        super.onRestoreInstanceState(savedInstanceState);
    }

    protected void defineForm() {
        MetrixTableDef metrixTableDef = null;
        if (this.mActivityDef != null) {
            metrixTableDef = new MetrixTableDef("receiving_unit", this.mActivityDef.TransactionType);
            if (this.mActivityDef.Keys != null) {
                metrixTableDef.constraints.add(new MetrixConstraintDef("metrix_row_id", MetrixConstraintOperands.EQUALS, String.valueOf(this.mActivityDef.Keys.get("metrix_row_id")), double.class));
            }
        } else {
            metrixTableDef = new MetrixTableDef("receiving_unit", MetrixTransactionTypes.INSERT);
        }

        this.mFormDef = new MetrixFormDef(metrixTableDef);
    }

    protected void displayPreviousCount() {
        int count = MetrixDatabaseManager.getCount("receiving_unit", String.format("receiving_id = '%1$s' and receiving_sequence = %2$s", mRcvId, mRcvSeq));
        try {
            MetrixControlAssistant.setValue(R.id.view_previous_entries, (ViewGroup) findViewById(R.id.view_previous_entries_bar), AndroidResourceHelper.getMessage("List1Arg", String.valueOf(count)));
        } catch (Exception e) {
            LogManager.getInstance(this).error(e);
        }
    }

    protected void setListeners() {
        if (mFABList == null)
            mFABList = new ArrayList<FloatingActionButton>();
        else
            mFABList.clear();

        mAddButton = (FloatingActionButton) findViewById(R.id.save);
        mSaveButton = (FloatingActionButton) findViewById(R.id.update);
        mFinishButton = (FloatingActionButton) findViewById(R.id.next);
        mCorrectErrorButton = (FloatingActionButton) findViewById(R.id.correct_error);
        mViewPreviousEntriesButton = (Button) findViewById(R.id.view_previous_entries);

        if (mAddButton != null) {
            mAddButton.setOnClickListener(this);
            mFABList.add(mAddButton);
        }
        if (mSaveButton != null) {
            mSaveButton.setOnClickListener(this);
            mFABList.add(mSaveButton);
        }
        if (mFinishButton != null) {
            mFinishButton.setOnClickListener(this);
            mFABList.add(mFinishButton);
        }
        if (mCorrectErrorButton != null) {
            mCorrectErrorButton.setOnClickListener(this);
            mFABList.add(mCorrectErrorButton);
        }
        if (mViewPreviousEntriesButton != null) {
            mViewPreviousEntriesButton.setOnClickListener(this);
        }

        if (this.mActivityDef != null && this.mActivityDef.TransactionType == MetrixTransactionTypes.UPDATE) {
            this.displaySaveButtonOnAddNextBar();
        }

        fabRunnable = this::showFABs;

        NestedScrollView scrollView = findViewById(R.id.scroll_view);
        mLayout.setPadding(mLayout.getPaddingLeft(),mLayout.getPaddingTop(),mLayout.getPaddingRight(), generateOffsetForFABs(mFABList));
        scrollView.setOnScrollChangeListener((NestedScrollView v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) -> {
            if ((scrollY > oldScrollY) || (scrollY < oldScrollY)) {
                fabHandler.removeCallbacks(fabRunnable);
                if(mFABsToShow != null)
                    mFABsToShow.clear();
                else
                    mFABsToShow = new ArrayList<>();

                hideFABs(mFABList);
                fabHandler.postDelayed(fabRunnable, fabDelay);
            }
        });
    }

    @Override
    public void onClick(View v) {
        if (scriptEventConsumesClick(this, v))
            return;

        Intent intent;
        switch (v.getId()) {
            case R.id.save:
                MetrixTransaction transactionInfo = MetrixTransaction.getTransaction("receiving", "receiving_id");
                MetrixSaveResult saveResult = MetrixUpdateManager.update(this, mLayout, mFormDef, transactionInfo, false, null, false, AndroidResourceHelper.getMessage("ReceivingUnit"));
                if (saveResult == MetrixSaveResult.ERROR)
                    return;

                intent = MetrixActivityHelper.createActivityIntent(this, ReceivingUnitPO.class);
                MetrixActivityHelper.startNewActivityAndFinish(this, intent);
                break;

            case R.id.next: // Finish button
                if (anyOnStartValuesChanged()) {
                    transactionInfo = MetrixTransaction.getTransaction("receiving", "receiving_id");
                    saveResult = MetrixUpdateManager.update(this, mLayout, mFormDef, transactionInfo, true, null, false, AndroidResourceHelper.getMessage("ReceivingUnit"));
                    if (saveResult == MetrixSaveResult.ERROR || saveResult == MetrixSaveResult.ERROR_WITH_CONTINUE)
                        return;
                }
                finish();
                break;

            case R.id.update:
                if (anyOnStartValuesChanged()) {
                    transactionInfo = MetrixTransaction.getTransaction("receiving", "receiving_id");
                    saveResult = MetrixUpdateManager.update(this, mLayout, mFormDef, transactionInfo, true, null, false, AndroidResourceHelper.getMessage("ReceivingUnit"));
                    if (saveResult == MetrixSaveResult.ERROR || saveResult == MetrixSaveResult.ERROR_WITH_CONTINUE)
                        return;
                }
                finish();
                break;

            case R.id.correct_error:
                transactionInfo = MetrixTransaction.getTransaction("receiving", "receiving_id");
                saveResult = MetrixUpdateManager.update(this, mLayout, mFormDef, transactionInfo, false, SyncServiceMonitor.class, true, AndroidResourceHelper.getMessage("ReceivingUnit"));
                if (saveResult == MetrixSaveResult.ERROR)
                    MetrixUIHelper.showSnackbar(this, R.id.coordinator_layout, AndroidResourceHelper.getMessage("Error"));
                break;

            case R.id.view_previous_entries:
                intent = MetrixActivityHelper.createActivityIntent(this, ReceivingUnitPOList.class);
                MetrixActivityHelper.startNewActivity(this, intent);
                break;

            default:
                super.onClick(v);
        }
    }
}
