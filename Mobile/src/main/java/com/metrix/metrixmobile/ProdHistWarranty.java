package com.metrix.metrixmobile;

import android.app.Activity;
import androidx.recyclerview.widget.RecyclerView;

import com.metrix.architecture.database.MetrixCursor;
import com.metrix.architecture.database.MetrixDatabaseManager;
import com.metrix.architecture.designer.MetadataRecyclerViewAdapter;
import com.metrix.architecture.designer.MetrixListScreenManager;
import com.metrix.architecture.designer.MetrixTabScreenManager;
import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.architecture.utilities.LogManager;
import com.metrix.architecture.utilities.MetrixCurrentKeysHelper;
import com.metrix.architecture.utilities.MetrixDateTimeHelper;
import com.metrix.architecture.utilities.MetrixStringHelper;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// This class exists to ensure that the architecture does not interpret the corresponding tab child as a codeless screen.
// It will also provide manager-class functionality for this particular tab.
public class ProdHistWarranty {
    public static void populateWarrantyList(Activity activity, String screenName, int screenID, RecyclerView recyclerView, MetrixTabScreenManager.TabChildInfo tci, String maxRows, String searchCriteria) {
        try {

            List<HashMap<String, String>> table = getWarrantyListData(screenName, screenID, maxRows, null);
            table = MetrixListScreenManager.performScriptListPopulation(activity, screenID, screenName, table);

            tci.mCount = table.size();

            if (recyclerView != null) {
                final MetadataRecyclerViewAdapter adapter = new MetadataRecyclerViewAdapter(activity, table, R.layout.list_item_basic,
                        R.id.table_layout, R.layout.list_item_table_row, R.color.IFSGold, 0, null,
                        0, R.id.sliver, null, screenID, "warranty_coverage.metrix_row_id", null);

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

    public static void updateWarrantyList(Activity activity, String screenName, int screenID, MetrixTabScreenManager.TabChildInfo tci, String maxRows, String searchCriteria) {

        List<HashMap<String, String>> table = getWarrantyListData(screenName, screenID, maxRows, null);
        table = MetrixListScreenManager.applySearchCriteria(table, searchCriteria);
        table = MetrixListScreenManager.performScriptListPopulation(activity, screenID, screenName, table);

        if (tci.mListAdapter != null) {
            tci.mListAdapter.updateData(table);
        }
    }

    private static List<HashMap<String, String>> getWarrantyListData(String screenName, int screenID, String maxRows, String searchCriteria) {

        String productId = MetrixCurrentKeysHelper.getKeyValue("product", "product_id");
        String query = MetrixListScreenManager.generateListQuery("warranty_coverage", "warranty_coverage.product_id = '" + productId + "' order by warranty_coverage.effective_dt desc", searchCriteria, screenID);

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

            //Lazy load messages
            final Map<String, String> messageMap = new HashMap<String, String>();

            while (cursor.isAfterLast() == false) {
                HashMap<String, String> row = MetrixListScreenManager.generateRowFromCursor(cursor, screenID);

                String effDt = row.get("warranty_coverage.effective_dt");
                if (!MetrixStringHelper.isNullOrEmpty(effDt))
                    row.put("custom.effective_dt", effDt);
                else
                    row.put("custom.effective_dt", getMessage(messageMap, "NoEffectiveDt"));

                String endDt = "";
                String daysCov = row.get("warranty_coverage.days_coverage");
                String monthsCov = row.get("warranty_coverage.months_coverage");
                String yearsCov = row.get("warranty_coverage.years_coverage");
                if (!MetrixStringHelper.isNullOrEmpty(effDt) && !(MetrixStringHelper.isNullOrEmpty(daysCov) && MetrixStringHelper.isNullOrEmpty(monthsCov) && MetrixStringHelper.isNullOrEmpty(yearsCov))) {
                    endDt = effDt;

                    if (!MetrixStringHelper.isNullOrEmpty(daysCov))
                        endDt = MetrixDateTimeHelper.adjustDate(endDt, MetrixDateTimeHelper.DATE_FORMAT, Calendar.DATE, Integer.valueOf(daysCov));

                    if (!MetrixStringHelper.isNullOrEmpty(monthsCov))
                        endDt = MetrixDateTimeHelper.adjustDate(endDt, MetrixDateTimeHelper.DATE_FORMAT, Calendar.MONTH, Integer.valueOf(monthsCov));

                    if (!MetrixStringHelper.isNullOrEmpty(yearsCov))
                        endDt = MetrixDateTimeHelper.adjustDate(endDt, MetrixDateTimeHelper.DATE_FORMAT, Calendar.YEAR, Integer.valueOf(yearsCov));

                    endDt = MetrixDateTimeHelper.adjustDate(endDt, MetrixDateTimeHelper.DATE_FORMAT, Calendar.DATE, -1);
                    row.put("custom.end_dt", endDt);
                } else
                    row.put("custom.end_dt", getMessage(messageMap, "NoEndDt"));

                String inWarranty = row.get("warranty_coverage.in_warranty");
                if (MetrixStringHelper.isNullOrEmpty(inWarranty)) {
                    if (!MetrixStringHelper.isNullOrEmpty(effDt) && !MetrixStringHelper.isNullOrEmpty(endDt)) {
                        Calendar effDate = MetrixDateTimeHelper.getDate(MetrixDateTimeHelper.DATE_FORMAT, effDt);
                        Calendar endDate = MetrixDateTimeHelper.getDate(MetrixDateTimeHelper.DATE_FORMAT, endDt);
                        endDate.add(Calendar.DAY_OF_MONTH, 1);
                        Calendar currDate = Calendar.getInstance();

                        if ((effDate.before(currDate) || effDate.equals(currDate)) && (endDate.after(currDate) || (endDate.equals(currDate))))
                            row.put("custom.warranty_status", getMessage(messageMap, "WarrantyIn"));
                        else
                            row.put("custom.warranty_status", getMessage(messageMap, "WarrantyOut"));
                    } else
                        row.put("custom.warranty_status", getMessage(messageMap, "WarrantyOut"));
                } else if (MetrixStringHelper.valueIsEqual(inWarranty, "Y"))
                    row.put("custom.warranty_status", getMessage(messageMap, "WarrantyIn"));
                else
                    row.put("custom.warranty_status", getMessage(messageMap, "WarrantyOut"));

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

    private static String getMessage(Map<String, String> stringMap, String key) {
        if(!stringMap.containsKey(key))
            stringMap.put(key, AndroidResourceHelper.getMessage(key));

        return stringMap.get(key);
    }
}
