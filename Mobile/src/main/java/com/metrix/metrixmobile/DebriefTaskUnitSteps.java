package com.metrix.metrixmobile;

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.recyclerview.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;

import com.metrix.architecture.assistants.MetrixControlAssistant;
import com.metrix.architecture.constants.MetrixTransactionTypes;
import com.metrix.architecture.database.MetrixCursor;
import com.metrix.architecture.database.MetrixDatabaseManager;
import com.metrix.architecture.designer.MetadataRecyclerViewAdapter;
import com.metrix.architecture.designer.MetrixListScreenManager;
import com.metrix.architecture.designer.MetrixRecyclerViewListener;
import com.metrix.architecture.designer.MetrixScreenManager;
import com.metrix.architecture.managers.MetrixUpdateManager;
import com.metrix.architecture.metadata.MetrixSqlData;
import com.metrix.architecture.metadata.MetrixTransaction;
import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.architecture.utilities.DataField;
import com.metrix.architecture.utilities.LogManager;
import com.metrix.architecture.utilities.MetrixActivityHelper;
import com.metrix.architecture.utilities.MetrixCurrentKeysHelper;
import com.metrix.architecture.utilities.MetrixDateTimeHelper;
import com.metrix.architecture.utilities.MetrixStringHelper;
import com.metrix.architecture.utilities.ResourceValueObject;
import com.metrix.metrixmobile.system.BottomOffsetDecoration;
import com.metrix.metrixmobile.system.DebriefListViewActivity;
import com.metrix.metrixmobile.system.MetadataDebriefActivity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class DebriefTaskUnitSteps extends DebriefListViewActivity implements View.OnClickListener, MetrixRecyclerViewListener {
    private RecyclerView recyclerView;
    private FloatingActionButton mSaveButton;
    private MetadataRecyclerViewAdapter mAdapter;
    private BottomOffsetDecoration mBottomOffset;
    private List<FloatingActionButton> mFABList;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // We are NOT implementing Tablet UI for this screen
        setContentView(R.layout.debrief_task_unit_steps_list);
        recyclerView = findViewById(R.id.task_steps_recyclerview);
        MetrixListScreenManager.setupVerticalRecyclerView(recyclerView, R.drawable.rv_item_divider);
        setCustomListeners();
    }

    public void onStart() {
        resourceStrings.add(new ResourceValueObject(R.id.action_bar_error, "ViewSyncErrors"));

        super.onStart();

        mLayout = (ViewGroup) findViewById(R.id.table_layout);

        setupActionBar();
        populateList();
    }

    protected void setCustomListeners() {
        if (mFABList == null)
            mFABList = new ArrayList<FloatingActionButton>();
        else
            mFABList.clear();

        mSaveButton = (FloatingActionButton) findViewById(R.id.update);
        if (mSaveButton != null) {
            mSaveButton.setOnClickListener(this);
            MetrixControlAssistant.setButtonVisibility(mSaveButton, View.VISIBLE);
            mFABList.add(mSaveButton);
        }

        mBottomOffset = new BottomOffsetDecoration(generateOffsetForFABs(mFABList));

        fabRunnable = this::showFABs;

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
        String whereClause = String.format("task_steps.task_unit_id = %1$s and task_steps.task_id = %2$s order by task_steps.sequence",
                MetrixCurrentKeysHelper.getKeyValue("task_unit", "task_unit_id"),
                MetrixCurrentKeysHelper.getKeyValue("task", "task_id"));
        String query = MetrixListScreenManager.generateListQuery(this, "task_steps", whereClause);

        MetrixCursor cursor = null;
        List<HashMap<String, String>> table = new ArrayList<HashMap<String, String>>();
        try {
            cursor = MetrixDatabaseManager.rawQueryMC(query, null);

            if (cursor == null || !cursor.moveToFirst()) {
                return;
            }

            final String requiredText = AndroidResourceHelper.getMessage("Required");
            final String optionalText = AndroidResourceHelper.getMessage("Optional");

            while (cursor.isAfterLast() == false) {
                HashMap<String, String> row = MetrixListScreenManager.generateRowFromCursor(this, cursor);
                String required = row.get("task_steps.required");
                if (required.compareToIgnoreCase("Y") == 0) {
                    row.put("custom.required_text", requiredText);
                } else {
                    row.put("custom.required_text", optionalText);
                }

                table.add(row);
                cursor.moveToNext();
            }

            table = MetrixListScreenManager.performScriptListPopulation(this, table);
        } catch (Exception e) {
            LogManager.getInstance().error(e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        if (mAdapter == null) {
            mAdapter = new MetadataRecyclerViewAdapter(this, table, R.layout.list_item_checkbox,
                    R.id.table_layout, R.layout.list_item_table_row, R.color.IFSGold, R.id.list_checkbox, "task_steps.completed", 0, R.id.sliver,  null, "task_steps.metrix_row_id", this);
            recyclerView.addItemDecoration(mBottomOffset);
            recyclerView.setAdapter(mAdapter);
        } else {
            mAdapter.updateData(table);
        }
    }

    @Override
    public void onClick(View v) {
        if (scriptEventConsumesClick(this, v))
            return;

        switch (v.getId()) {
            case R.id.update:
                // Save changes and go back to DebriefTaskUnitList
                updateTaskSteps();
                finish();
                break;
            default:
                super.onClick(v);
        }
    }

    private void updateTaskSteps() {
        try {
            for (HashMap<String, String> item : mAdapter.getListData()) {
                if (item != null) {
                    String checkState = item.get("checkboxState");
                    String origCompletedValue = item.get("task_steps.completed");

                    if (!MetrixStringHelper.isNullOrEmpty(checkState) && !MetrixStringHelper.valueIsEqual(checkState, origCompletedValue)) {
                        String metrixRowId = item.get("task_steps.metrix_row_id");
                        String stepId = item.get("task_steps.task_step_id");
                        String completedAsOf = "";
                        if (MetrixStringHelper.valueIsEqual(checkState, "Y")) {
                            completedAsOf = MetrixDateTimeHelper.getCurrentDate(MetrixDateTimeHelper.DATE_TIME_FORMAT, MetrixDateTimeHelper.ISO8601.Yes, true);
                        }

                        ArrayList<MetrixSqlData> taskStepsToUpdate = new ArrayList<MetrixSqlData>();
                        MetrixSqlData data = new MetrixSqlData("task_steps", MetrixTransactionTypes.UPDATE, "metrix_row_id = " + metrixRowId);
                        data.dataFields.add(new DataField("metrix_row_id", metrixRowId));
                        data.dataFields.add(new DataField("task_step_id", stepId));
                        data.dataFields.add(new DataField("completed", checkState));
                        data.dataFields.add(new DataField("completed_as_of", completedAsOf));
                        taskStepsToUpdate.add(data);

                        MetrixTransaction transactionInfo = new MetrixTransaction();
                        MetrixUpdateManager.update(taskStepsToUpdate, true, transactionInfo, AndroidResourceHelper.getMessage("Steps"), this);
                    }
                }
            }
        } catch (Exception e) {
            LogManager.getInstance().error(e);
        }
    }

    @Override
    public void onListItemClick(int position, @NonNull HashMap<String, String> listItemData, @NonNull View view) {
        if (scriptEventConsumesListTap(this, view, MetrixScreenManager.getScreenId(this))) return;

        MetrixCurrentKeysHelper.setKeyValue("task_steps", "metrix_row_id", listItemData.get("task_steps.metrix_row_id"));
        MetrixCurrentKeysHelper.setKeyValue("task_steps", "task_step_id", listItemData.get("task_steps.task_step_id"));

        this.updateTaskSteps();

        Intent intent = MetrixActivityHelper.createActivityIntent(this, MetadataDebriefActivity.class, MetrixTransactionTypes.UPDATE,
                "metrix_row_id", listItemData.get("task_steps.metrix_row_id"));
        intent.putExtra("ScreenID", MetrixScreenManager.getScreenId("DebriefTaskStep"));
        intent.putExtra("NavigatedFromLinkedScreen", true);
        MetrixActivityHelper.startNewActivity(this, intent);
    }

    @Override
    public void onListItemLongClick(int position, @NonNull HashMap<String, String> listItemData, @NonNull View view) {

    }
}
