package com.metrix.metrixmobile;

import android.app.Activity;
import androidx.recyclerview.widget.RecyclerView;

import com.metrix.architecture.database.MetrixCursor;
import com.metrix.architecture.database.MetrixDatabaseManager;
import com.metrix.architecture.designer.MetadataRecyclerViewAdapter;
import com.metrix.architecture.designer.MetrixListScreenManager;
import com.metrix.architecture.designer.MetrixTabScreenManager;
import com.metrix.architecture.utilities.LogManager;
import com.metrix.architecture.utilities.MetrixCurrentKeysHelper;
import com.metrix.architecture.utilities.MetrixStringHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class DebReqTasks {
    public static void populateTaskList(Activity activity, String screenName, int screenID, RecyclerView recyclerView, MetrixTabScreenManager.TabChildInfo tci, String maxRows, String searchCriteria) {
        try {
            List<HashMap<String, String>> table = getTaskListData(screenName, screenID, maxRows, null);
            table = MetrixListScreenManager.performScriptListPopulation(activity, screenID, screenName, table);
            tci.mCount = table.size();

            if (recyclerView != null) {
                final MetadataRecyclerViewAdapter adapter = new MetadataRecyclerViewAdapter(activity, table, R.layout.list_item_basic,
                        R.id.table_layout, R.layout.list_item_table_row, R.color.IFSGold, 0, null,
                        0, R.id.sliver, null, screenID, "mma_task_history_view.metrix_row_id", null);

                recyclerView.setAdapter(adapter);
                tci.mListAdapter = adapter;
            } else {
                tci.mListAdapter.updateData(table);
            }
        } catch (Exception e) {
            LogManager.getInstance().error(e);
        }
    }

    public static void updateTaskList(Activity activity, String screenName, int screenID, MetrixTabScreenManager.TabChildInfo tci, String maxRows, String searchCriteria){
        List<HashMap<String, String>> table = getTaskListData(screenName, screenID, maxRows, null);
        table = MetrixListScreenManager.applySearchCriteria(table, searchCriteria);
        table = MetrixListScreenManager.performScriptListPopulation(activity, screenID, screenName, table);

        if (tci.mListAdapter != null) {
            tci.mListAdapter.updateData(table);
        }
    }

    private static List<HashMap<String, String>> getTaskListData(String screenName, int screenID, String maxRows, String searchCriteria) {

        String requestId = MetrixCurrentKeysHelper.getKeyValue("request", "request_id");
        String query = MetrixListScreenManager.generateListQuery("task", "task.request_id = '" + requestId + "' order by task.plan_start_dttm desc", searchCriteria, screenID);

        if (!MetrixStringHelper.isNullOrEmpty(maxRows)) {
            query = query + " limit " + maxRows;
        }

        MetrixCursor cursor = null;
        List<HashMap<String, String>> table = new ArrayList<HashMap<String, String>>();
        try {
            cursor = MetrixDatabaseManager.rawQueryMC(query, null);

            if (cursor == null || !cursor.moveToFirst()) {
                return table;
            }

            while (cursor.isAfterLast() == false) {
                HashMap<String, String> row = MetrixListScreenManager.generateRowFromCursor(cursor, screenID);

                String firstName = row.get("person.first_name");
                String lastName = row.get("person.last_name");

                String fullName = "";
                if (!MetrixStringHelper.isNullOrEmpty(firstName) || !MetrixStringHelper.isNullOrEmpty(lastName)) {
                    if (MetrixStringHelper.isNullOrEmpty(firstName)) {
                        fullName = String.format("%s", lastName);
                    } else {
                        fullName = String.format("%1$s %2$s", firstName, lastName);
                    }
                }
                row.put("custom.full_name", fullName);

                String address = row.get("address.address");
                if (MetrixStringHelper.isNullOrEmpty(address)) {
                    row.put("custom.full_address", "");
                } else {
                    String city = row.get("address.city");
                    row.put("custom.full_address", address + ", " + city);
                }

                table.add(row);
                cursor.moveToNext();
            }

        } catch (Exception e) {
            LogManager.getInstance().error(e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return table;
    }
}
