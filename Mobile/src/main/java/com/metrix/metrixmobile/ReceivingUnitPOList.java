package com.metrix.metrixmobile;

import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import android.view.View;

import com.metrix.architecture.assistants.MetrixDialogAssistant;
import com.metrix.architecture.constants.MetrixTransactionTypes;
import com.metrix.architecture.database.MetrixCursor;
import com.metrix.architecture.database.MetrixDatabaseManager;
import com.metrix.architecture.designer.MetadataRecyclerViewAdapter;
import com.metrix.architecture.designer.MetrixListScreenManager;
import com.metrix.architecture.designer.MetrixRecyclerViewListener;
import com.metrix.architecture.designer.MetrixScreenManager;
import com.metrix.architecture.managers.MetrixUpdateManager;
import com.metrix.architecture.metadata.MetrixKeys;
import com.metrix.architecture.metadata.MetrixTransaction;
import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.architecture.utilities.LogManager;
import com.metrix.architecture.utilities.MetrixActivityHelper;
import com.metrix.architecture.utilities.MetrixCurrentKeysHelper;
import com.metrix.architecture.utilities.MetrixDatabases;
import com.metrix.architecture.utilities.MetrixStringHelper;
import com.metrix.metrixmobile.system.MetrixActivity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

public class ReceivingUnitPOList extends MetrixActivity implements MetrixRecyclerViewListener {
    private MetadataRecyclerViewAdapter mAdapter;
    private RecyclerView recyclerView;
    private int mSelectedPosition;
    private String mRcvId, mRcvSeq;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        mRcvId = MetrixCurrentKeysHelper.getKeyValue("receiving_detail", "receiving_id");
        mRcvSeq = MetrixCurrentKeysHelper.getKeyValue("receiving_detail", "sequence");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.receiving_unit_po_list);
        recyclerView = findViewById(R.id.recyclerView);
        MetrixListScreenManager.setupVerticalRecyclerView(recyclerView, R.drawable.rv_item_divider);
    }

    @Override
    public void onStart() {
        super.onStart();
        setReceivingActionBarTitle();
        populateList();
    }

    private void populateList() {
        String query = MetrixListScreenManager.generateListQuery(this, "receiving_unit", String.format("receiving_id = '%1$s' and receiving_sequence = %2$s", mRcvId, mRcvSeq));

        String maxRows = MetrixDatabaseManager.getFieldStringValue("metrix_app_params", "param_value", "param_name='MAX_ROWS'");
        if (!MetrixStringHelper.isNullOrEmpty(maxRows)) {
            query = query + " limit " + maxRows;
        }

        MetrixCursor cursor = null;
        List<HashMap<String, String>> table = new ArrayList<HashMap<String, String>>();
        try {
            cursor = MetrixDatabaseManager.rawQueryMC(query, null);

            if (cursor == null || !cursor.moveToFirst()) {
                return;
            }

            while (cursor.isAfterLast() == false) {
                HashMap<String, String> row = MetrixListScreenManager.generateRowFromCursor(this, cursor);
                table.add(row);
                cursor.moveToNext();
            }

            table = MetrixListScreenManager.performScriptListPopulation(this, table);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        if (mAdapter == null) {
            mAdapter = new MetadataRecyclerViewAdapter(this, table, R.layout.list_item_basic,
                    R.id.table_layout, R.layout.list_item_table_row, R.color.IFSGold, 0, null, 0, R.id.sliver, null, "receiving_unit.metrix_row_id", this);
            recyclerView.setAdapter(mAdapter);
        } else {
            mAdapter.updateData(table);
        }
    }

    @Override
    public void onListItemClick(int position, @NonNull HashMap<String, String> listItemData, @NonNull View view) {
        mSelectedPosition = position;
        if(scriptEventConsumesListTap(this, view, MetrixScreenManager.getScreenId(this))) return;

        final OnClickListener modifyListener = (dialog, which) -> {
            try {
                HashMap<String, String> selectedItem = mAdapter.getListData().get(mSelectedPosition);
                Intent intent = MetrixActivityHelper.createActivityIntent(ReceivingUnitPOList.this, ReceivingUnitPO.class, MetrixTransactionTypes.UPDATE, "metrix_row_id", selectedItem.get("receiving_unit.metrix_row_id"));
                MetrixActivityHelper.startNewActivity(mCurrentActivity, intent);
            } catch (Exception e) {
                LogManager.getInstance().error(e);
            }
        };

        final OnClickListener deleteListener = (dialog, which) -> {
            try {
                HashMap<String, String> selectedItem = mAdapter.getListData().get(mSelectedPosition);
                String metrixRowIdValue = selectedItem.get("receiving_unit.metrix_row_id");

                Hashtable<String, String> keys = new Hashtable<String, String>();
                MetrixKeys tableKeys = MetrixDatabases.getMetrixTablePrimaryKeys("receiving_unit");
                for (Map.Entry<String, String> key : tableKeys.keyInfo.entrySet()) {
                    String columnName = key.getKey();
                    String columnNameWithTable = String.format("%s.%s", "receiving_unit", columnName);
                    if (selectedItem.containsKey(columnNameWithTable)) {
                        String currentKeyValue = selectedItem.get(columnNameWithTable);
                        keys.put(columnName, currentKeyValue);
                    }
                }

                MetrixTransaction transactionInfo = MetrixTransaction.getTransaction("receiving", "receiving_id");
                MetrixUpdateManager.delete(ReceivingUnitPOList.this, "receiving_unit", metrixRowIdValue, keys, AndroidResourceHelper.getMessage("ReceivingUnit"), transactionInfo);
                mAdapter.getListData().remove(mSelectedPosition);
                mAdapter.notifyItemRemoved(mSelectedPosition);

                reloadFreshActivity();
            } catch (Exception e) {
                LogManager.getInstance().error(e);
            }
        };

        MetrixDialogAssistant.showEditOrDeleteDialog(AndroidResourceHelper.getMessage("UnitLCase"), modifyListener, deleteListener, this);
    }

    @Override
    public void onListItemLongClick(int position, @NonNull HashMap<String, String> listItemData, @NonNull View view) {

    }
}
