package com.metrix.metrixmobile;

import android.app.Activity;
import android.view.View;
import android.widget.AutoCompleteTextView;

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

// This class exists to ensure that the architecture does not interpret the corresponding tab child as a codeless screen.
// It will also provide manager-class functionality for this particular tab.
public class CustTabProducts {
    public static void populateProductList(Activity activity, String screenName, int screenID, RecyclerView recyclerView, MetrixTabScreenManager.TabChildInfo tci, String maxRows, String searchCriteria) {
        try {

            List<HashMap<String, String>> table = getProductListData(screenName, screenID, maxRows, null);
            table = MetrixListScreenManager.performScriptListPopulation(activity, screenID, screenName, table);

            tci.mCount = table.size();

            if (recyclerView != null){
                final MetadataRecyclerViewAdapter adapter = new MetadataRecyclerViewAdapter(activity, table, R.layout.list_item_basic,
                        R.id.table_layout, R.layout.list_item_table_row, R.color.IFSGold, 0, null,
                        0, R.id.sliver, null, screenID, "product.metrix_row_id", null);

                recyclerView.setAdapter(adapter);
                tci.mListAdapter = adapter;
            }
            else{
                tci.mListAdapter.updateData(table);
            }

        } catch (Exception e) {
            LogManager.getInstance().error(e);
        }
    }

    public static void updateProductList(Activity activity, String screenName, int screenID, RecyclerView recyclerView, MetrixTabScreenManager.TabChildInfo tci, String maxRows, String searchCriteria) {
        // No compound columns.
        List<HashMap<String, String>> table = getProductListData(screenName, screenID, maxRows, null);
        table = MetrixListScreenManager.applySearchCriteria(table, searchCriteria);

        table = MetrixListScreenManager.performScriptListPopulation(activity, screenID, screenName, table);

        if (tci.mListAdapter != null) {
            tci.mListAdapter.updateData(table);
        }
    }

    private static List<HashMap<String, String>> getProductListData(String screenName, int screenID,String maxRows, String searchCriteria) {

        String placeId = MetrixCurrentKeysHelper.getKeyValue("place", "place_id");
        String query = MetrixListScreenManager.generateListQuery("product", "product.place_id = '" + placeId + "' order by product.model_id collate nocase", searchCriteria, screenID);

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
