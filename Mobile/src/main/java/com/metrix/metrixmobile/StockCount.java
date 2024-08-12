package com.metrix.metrixmobile;

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import com.metrix.architecture.database.MetrixCursor;
import com.metrix.architecture.database.MetrixDatabaseManager;
import com.metrix.architecture.designer.MetadataRecyclerViewAdapter;
import com.metrix.architecture.designer.MetrixListScreenManager;
import com.metrix.architecture.designer.MetrixRecyclerViewListener;
import com.metrix.architecture.designer.MetrixScreenManager;
import com.metrix.architecture.utilities.LogManager;
import com.metrix.architecture.utilities.MetrixActivityHelper;
import com.metrix.architecture.utilities.MetrixCurrentKeysHelper;
import com.metrix.architecture.utilities.MetrixPublicCache;
import com.metrix.architecture.utilities.MetrixStringHelper;
import com.metrix.metrixmobile.system.MetrixActivity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static com.metrix.architecture.utilities.StockCountHelper.runId;

public class StockCount extends MetrixActivity implements MetrixRecyclerViewListener {
    private MetadataRecyclerViewAdapter mAdapter;
    private RecyclerView mRunIdRecyclerView;
    List<HashMap<String, String>> runIdList;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.stock_count);
        mRunIdRecyclerView = findViewById(R.id.recyclerView);
        MetrixListScreenManager.setupVerticalRecyclerView(mRunIdRecyclerView, R.drawable.rv_item_divider);
    }

    public void onStart() {
        super.onStart();

        mLayout = (ViewGroup) findViewById(R.id.table_layout);
        populateList();
    }

    private void populateList() {
        runIdList = new ArrayList<HashMap<String, String>>();

        String whereClause = "where stock_count.posted is null and (stock_count.submitted is null or stock_count.submitted = 'N') order by stock_count.run_id asc";
        StringBuilder query = new StringBuilder();
        query.append(MetrixListScreenManager.generateListQuery(this, "stock_count", whereClause));

        String maxRows = MetrixDatabaseManager.getFieldStringValue("metrix_app_params", "param_value", "param_name='MAX_ROWS'");
        if (!MetrixStringHelper.isNullOrEmpty(maxRows)) {
            query.append(" limit " + maxRows);
        }
        MetrixCursor cursor = null;

        try {
            cursor = MetrixDatabaseManager.rawQueryMC(query.toString(), null);
            if (cursor != null && cursor.moveToFirst()) {
                while (cursor.isAfterLast() == false) {
                    HashMap<String, String> row = MetrixListScreenManager.generateRowFromCursor(this, cursor);
                    runIdList.add(row);
                    cursor.moveToNext();
                }
                runIdList = MetrixListScreenManager.performScriptListPopulation(this, runIdList);
            }
        } catch (Exception e) {
            LogManager.getInstance(this).error(e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        if (mAdapter == null) {
            mAdapter = new MetadataRecyclerViewAdapter(this, runIdList, R.layout.list_item_basic,
                    R.id.table_layout, R.layout.list_item_table_row, R.color.IFSGold, 0, null, 0, R.id.sliver, null, "stock_count.metrix_row_id", this);
            mRunIdRecyclerView.setAdapter(mAdapter);
        } else {
            mAdapter.updateData(runIdList);
        }
    }

    @Override
    public void onListItemClick(int position, @NonNull HashMap<String, String> listItemData, @NonNull View view) {
        if(scriptEventConsumesListTap(StockCount.this, view, MetrixScreenManager.getScreenId(StockCount.this))) return;

        runId = listItemData.get("stock_count.run_id");

        Intent intent = new Intent(StockCount.this, StockCountPartList.class);
        MetrixPublicCache.instance.addItem("stock_count.run_id", runId);
        MetrixPublicCache.instance.addItem("stock_count.filter", "");
        //for scripting purpose
        MetrixCurrentKeysHelper.setKeyValue("stock_count", "run_id", runId);
        MetrixActivityHelper.startNewActivity(StockCount.this, intent);
    }

    @Override
    public void onListItemLongClick(int position, @NonNull HashMap<String, String> listItemData, @NonNull View view) {

    }
}

