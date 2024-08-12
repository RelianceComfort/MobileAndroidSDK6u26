package com.metrix.metrixmobile;

import android.app.Activity;
import androidx.recyclerview.widget.RecyclerView;
import android.view.View;

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
public class ProdHistAttributes {
    public static void populateAttributeList(Activity activity, String screenName, int screenID, RecyclerView recyclerView, MetrixTabScreenManager.TabChildInfo tci, String maxRows, String searchCriteria) {
        try {
            List<HashMap<String, String>> table = getAttributeListData(screenName, screenID, maxRows, null);
            table = MetrixListScreenManager.performScriptListPopulation(activity, screenID, screenName, table);

            tci.mCount = table.size();

            if (recyclerView != null) {
                final MetadataRecyclerViewAdapter adapter = new MetadataRecyclerViewAdapter(activity, table, R.layout.list_item_basic,
                        R.id.table_layout, R.layout.list_item_table_row, R.color.IFSGold, 0, null, 0, R.id.sliver,
                        null, screenID, "attribute.metrix_row_id", null);
                recyclerView.setAdapter(adapter);
                tci.mListAdapter = adapter;

                recyclerView.setVisibility(tci.mCount > 0 ? View.VISIBLE : View.GONE);
            } else {
                tci.mListAdapter.updateData(table);
            }

        } catch (Exception e) {
            LogManager.getInstance().error(e);
        }
    }

    public static void updateAttributeList(Activity activity, String screenName, int screenID, MetrixTabScreenManager.TabChildInfo tci, String maxRows, String searchCriteria) {

        List<HashMap<String, String>> table = getAttributeListData(screenName, screenID, maxRows, null);
        table = MetrixListScreenManager.applySearchCriteria(table, searchCriteria);
        table = MetrixListScreenManager.performScriptListPopulation(activity, screenID, screenName, table);

        if (tci.mListAdapter != null) {
            tci.mListAdapter.updateData(table);
        }
    }

    private static List<HashMap<String, String>> getAttributeListData(String screenName, int screenID, String maxRows, String searchCriteria) {

        String productId = MetrixCurrentKeysHelper.getKeyValue("product", "product_id");
        String query = MetrixListScreenManager.generateListQuery("attribute", "attribute.table_name = 'PRODUCT' and attribute.foreign_key_char1 = '" + productId + "' order by attribute.metrix_row_id desc", searchCriteria, screenID);

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

                String attrName = row.get("attribute.attr_name");
                String attrValue = row.get("attribute.attr_value");

                String attrNameDesc = MetrixDatabaseManager.getFieldStringValue("attribute_name", "description", String.format("attr_name = '%s'", attrName));
                String attrValueDesc = MetrixDatabaseManager.getFieldStringValue("attribute_value_lkup_view", "description", String.format("table_name = 'PRODUCT' and attr_name = '%1$s' and code_value = '%2$s'", attrName, attrValue));

                String description = attrName;
                String displayValue = attrValue;
                if (!MetrixStringHelper.isNullOrEmpty(attrNameDesc))
                    description = attrNameDesc;
                if (!MetrixStringHelper.isNullOrEmpty(attrValueDesc))
                    displayValue = attrValueDesc;

                row.put("custom.description", description);
                row.put("custom.value", displayValue);

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
