package com.metrix.metrixmobile.system;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.recyclerview.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;

import com.metrix.architecture.assistants.MetrixControlAssistant;
import com.metrix.architecture.assistants.MetrixDialogAssistant;
import com.metrix.architecture.constants.MetrixTransactionTypes;
import com.metrix.architecture.database.MetrixCursor;
import com.metrix.architecture.database.MetrixDatabaseManager;
import com.metrix.architecture.designer.MetadataRecyclerViewAdapter;
import com.metrix.architecture.designer.MetrixListScreenManager;
import com.metrix.architecture.designer.MetrixRecyclerViewListener;
import com.metrix.architecture.designer.MetrixScreenManager;
import com.metrix.architecture.designer.MetrixWorkflowManager;
import com.metrix.architecture.managers.MetrixUpdateManager;
import com.metrix.architecture.metadata.MetrixKeys;
import com.metrix.architecture.metadata.MetrixTransaction;
import com.metrix.architecture.scripting.ClientScriptDef;
import com.metrix.architecture.scripting.MetrixClientScriptManager;
import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.architecture.utilities.LogManager;
import com.metrix.architecture.utilities.MetrixActivityHelper;
import com.metrix.architecture.utilities.MetrixDatabases;
import com.metrix.architecture.utilities.MetrixStringHelper;
import com.metrix.architecture.utilities.MetrixUIHelper;
import com.metrix.metrixmobile.R;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

public class MetadataListQuoteActivity extends QuoteActivity implements View.OnClickListener, MetrixRecyclerViewListener {
    private RecyclerView recyclerView;
    private MetadataRecyclerViewAdapter mAdapter;
    private int mSelectedPosition;
    private HashMap<String, String> currentScreenProperties;
    private String primaryTable, whereClauseScript, linkedScreenId;
    private boolean allowDelete;
    private boolean allowModify;
    private FloatingActionButton mNextButton, mAddButton;
    private boolean screenExistsInCurrentWorkflow, gotHereFromLinkedScreen;
    private BottomOffsetDecoration mBottomOffset;
    private List<FloatingActionButton> mFABList;

    @SuppressLint("DefaultLocale")
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.yycsmd_list_quote_activity);
        recyclerView = findViewById(R.id.recyclerView);
        MetrixListScreenManager.setupVerticalRecyclerView(recyclerView, R.drawable.rv_item_divider);

        screenExistsInCurrentWorkflow = MetrixWorkflowManager.isScreenExistsInCurrentWorkflow(this, codeLessScreenId);
        gotHereFromLinkedScreen = (this.getIntent().getExtras() != null && this.getIntent().getExtras().containsKey("NavigatedFromLinkedScreen"));

        try {
            currentScreenProperties = MetrixScreenManager.getScreenProperties(codeLessScreenId);
            if (currentScreenProperties != null) {
                if (currentScreenProperties.containsKey("primary_table")) {
                    primaryTable = currentScreenProperties.get("primary_table");
                    if (MetrixStringHelper.isNullOrEmpty(primaryTable)) {
                        throw new Exception("Primary table is required for code-less screen");
                    }
                    //This is needed (Ex: NON_PART_USAGE)
                    primaryTable = primaryTable.toLowerCase();
                }

                if (currentScreenProperties.containsKey("linked_screen_id"))
                    linkedScreenId = currentScreenProperties.get("linked_screen_id");
                if (currentScreenProperties.containsKey("where_clause_script"))
                    whereClauseScript = currentScreenProperties.get("where_clause_script");
                if (currentScreenProperties.containsKey("allow_delete")) {
                    String strAllowDelete = currentScreenProperties.get("allow_delete");
                    if (MetrixStringHelper.valueIsEqual(strAllowDelete, "Y"))
                        allowDelete = true;
                }

                if (currentScreenProperties.containsKey("allow_modify")) {
                    String strAllowModify = currentScreenProperties.get("allow_modify");
                    if (MetrixStringHelper.valueIsEqual(strAllowModify, "Y"))
                        allowModify = true;
                }
            }
        } catch (Exception e) {
            LogManager.getInstance(this).error(e);
        }
    }

    public void onStart() {
        super.onStart();
        mLayout = (ViewGroup) findViewById(R.id.table_layout);

        setupActionBar();
        populateList();
    }

    protected void setListeners() {
        mAddButton = (FloatingActionButton) findViewById(R.id.save);
        mNextButton = (FloatingActionButton) findViewById(R.id.next);
        if (mFABList == null)
            mFABList = new ArrayList<FloatingActionButton>();
        else
            mFABList.clear();

        if (mAddButton != null) {
            mAddButton.setOnClickListener(this);
        }
        if (mNextButton != null)
            mNextButton.setOnClickListener(this);

        hideAllButtons();
        if (!MetrixStringHelper.isNullOrEmpty(linkedScreenId) && !gotHereFromLinkedScreen) {
            MetrixControlAssistant.setButtonVisibility(mAddButton, View.VISIBLE);
            mFABList.add(mAddButton);
        }

        if (screenExistsInCurrentWorkflow && !gotHereFromLinkedScreen) {
            MetrixControlAssistant.setButtonVisibility(mNextButton, View.VISIBLE);
            mFABList.add(mNextButton);
        }

        mBottomOffset = new BottomOffsetDecoration(generateOffsetForFABs(mFABList));

        fabRunnable = () -> showFABs();

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (dy > 0 || dy < 0) {
                    fabHandler.removeCallbacks(fabRunnable);
                    hideFABs(mFABList);
                    fabHandler.postDelayed(fabRunnable, fabDelay);
                }
            }
        });
    }

    private void populateList() {
        try {
            ClientScriptDef clientScriptDef = MetrixClientScriptManager.getScriptDefForScriptID(whereClauseScript);
            String result = MetrixClientScriptManager.executeScriptReturningString(new WeakReference<Activity>(mCurrentActivity), clientScriptDef);
            String query = MetrixListScreenManager.generateListQuery(primaryTable, result, codeLessScreenId);

            String maxRows = MetrixDatabaseManager.getFieldStringValue("metrix_app_params", "param_value", "param_name='MAX_ROWS'");
            if (!MetrixStringHelper.isNullOrEmpty(maxRows)) {
                query = query + " limit " + maxRows;
            }

            MetrixCursor cursor = MetrixDatabaseManager.rawQueryMC(query, null);
            if (cursor == null || !cursor.moveToFirst()) {
                return;
            }

            List<HashMap<String, String>> table = new ArrayList<HashMap<String, String>>();
            while (cursor.isAfterLast() == false) {
                HashMap<String, String> row = MetrixListScreenManager.generateRowFromCursor(cursor, codeLessScreenId);
                table.add(row);
                cursor.moveToNext();
            }
            cursor.close();

            table = MetrixListScreenManager.performScriptListPopulation(this, codeLessScreenId, codeLessScreenName, table);

            if (mAdapter == null) {
                mAdapter = new MetadataRecyclerViewAdapter(this, table, R.layout.list_item_basic,
                        R.id.table_layout, R.layout.list_item_table_row, R.color.IFSGold, 0, null, 0, R.id.sliver, null, codeLessScreenId, primaryTable.toLowerCase() + ".metrix_row_id", this);
                recyclerView.addItemDecoration(mBottomOffset);
                recyclerView.setAdapter(mAdapter);
            } else {
                mAdapter.updateData(table);
            }
        } catch (Exception e) {
            LogManager.getInstance(this).error(e);
        }
    }

    @SuppressLint("DefaultLocale")
    @Override
    public void onClick(View v) {
        if (scriptEventConsumesClick(this, v))
            return;

        switch (v.getId()) {
            case R.id.save:
                if (!MetrixStringHelper.isNullOrEmpty(linkedScreenId)) {
                    int intLinkedScreenId = Integer.valueOf(linkedScreenId);
                    String screenType = MetrixScreenManager.getScreenType(intLinkedScreenId);
                    if (!MetrixStringHelper.isNullOrEmpty(screenType)) {
                        if (screenType.toLowerCase().contains("standard")) {
                            Intent intent = MetrixActivityHelper.createActivityIntent(this, MetadataQuoteActivity.class);
                            intent.putExtra("ScreenID", intLinkedScreenId);
                            intent.putExtra("NavigatedFromLinkedScreen", true);
                            MetrixActivityHelper.startNewActivity(this, intent);
                        } else
                            MetrixUIHelper.showSnackbar(this, R.id.coordinator_layout, AndroidResourceHelper.getMessage("YYCSWrongScreenType", screenType));
                    }
                }
                break;

            case R.id.next:
                MetrixWorkflowManager.advanceWorkflow(this);
                break;
            default:
                super.onClick(v);
        }
    }

    private void hideAllButtons() {
        MetrixControlAssistant.setButtonVisibility(mAddButton, View.GONE);
        MetrixControlAssistant.setButtonVisibility(mNextButton, View.GONE);
    }

    @Override
    public void onListItemClick(int position, @NonNull HashMap<String, String> listItemData, @NonNull View view) {
        mSelectedPosition = position;
        if(scriptEventConsumesListTap(this, view, MetrixScreenManager.getScreenId(this))) return;

        final DialogInterface.OnClickListener modifyListener = (dialog, which) -> {
            try {
                HashMap<String, String> selectedItem = mAdapter.getListData().get(mSelectedPosition);
                if (selectedItem == null) return;

                if (!MetrixStringHelper.isNullOrEmpty(linkedScreenId)) {
                    int intLinkedScreenId = Integer.valueOf(linkedScreenId);
                    String screenType = MetrixScreenManager.getScreenType(intLinkedScreenId);
                    if (!MetrixStringHelper.isNullOrEmpty(screenType)) {
                        if(screenType.toLowerCase().contains("standard")) {
                            String metrixRowIdKeyName = String.format("%s.%s", primaryTable, "metrix_row_id");
                            Intent intent = MetrixActivityHelper.createActivityIntent(MetadataListQuoteActivity.this, MetadataQuoteActivity.class, MetrixTransactionTypes.UPDATE,
                                    "metrix_row_id", selectedItem.get(metrixRowIdKeyName));
                            intent.putExtra("ScreenID", intLinkedScreenId);
                            intent.putExtra("NavigatedFromLinkedScreen", true);
                            MetrixActivityHelper.startNewActivity(mCurrentActivity, intent);
                        } else
                            MetrixUIHelper.showSnackbar(mCurrentActivity, R.id.coordinator_layout, AndroidResourceHelper.getMessage("YYCSWrongScreenType", screenType));
                    }
                }
            } catch (Exception e) {
                LogManager.getInstance().error(e);
            }
        };

        final DialogInterface.OnClickListener deleteListener = (dialog, which) -> {
            try {
                HashMap<String, String> selectedItem = mAdapter.getListData().get(mSelectedPosition);
                if (selectedItem == null) return;

                String metrixRowIdKeyName = String.format("%s.%s", primaryTable, "metrix_row_id");
                String metrixRowIdValue = selectedItem.get(metrixRowIdKeyName);

                Hashtable<String, String> keys = new Hashtable<String, String>();
                MetrixKeys tableKeys = MetrixDatabases.getMetrixTablePrimaryKeys(primaryTable);
                for (Map.Entry<String, String> key : tableKeys.keyInfo.entrySet()) {
                    String columnName = key.getKey();
                    String columnNameWithTable = String.format("%s.%s", primaryTable, columnName);
                    if (selectedItem.containsKey(columnNameWithTable)) {
                        String currentKeyValue = selectedItem.get(columnNameWithTable);
                        keys.put(columnName, currentKeyValue);
                    }
                }

                MetrixTransaction transactionInfo = MetrixTransaction.getTransaction("quote", "quote_id");
                MetrixUpdateManager.delete(MetadataListQuoteActivity.this, primaryTable.toLowerCase(), metrixRowIdValue, keys, String.format("MLQA-%s", codeLessScreenName), transactionInfo);
                mAdapter.getListData().remove(mSelectedPosition);
                mAdapter.notifyItemRemoved(mSelectedPosition);
            } catch (Exception e) {
                LogManager.getInstance().error(e);
            }
        };

        if (allowModify && allowDelete)
            MetrixDialogAssistant.showEditOrDeleteDialog(AndroidResourceHelper.getMessage("YYCSListItmModDelItmTxt"), modifyListener, deleteListener, this);
        else if (allowModify && !allowDelete)
            MetrixDialogAssistant.showModifyDialog(AndroidResourceHelper.getMessage("YYCSListItmModDelItmTxt"), modifyListener, null, this);
        else if (!allowModify && allowDelete)
            MetrixDialogAssistant.showConfirmDeleteDialog(AndroidResourceHelper.getMessage("YYCSListItmModDelItmTxt"), deleteListener, null, this);
        else
            MetrixUIHelper.showSnackbar(mCurrentActivity, R.id.coordinator_layout, AndroidResourceHelper.getMessage("YYCSListItmModDelNotAllowed"));
    }

    @Override
    public void onListItemLongClick(int position, @NonNull HashMap<String, String> listItemData, @NonNull View view) {

    }
}
