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
import com.metrix.architecture.utilities.MetrixDateTimeHelper;
import com.metrix.architecture.utilities.MetrixStringHelper;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

// This class exists to ensure that the architecture does not interpret the corresponding tab child as a codeless screen.
// It will also provide manager-class functionality for this particular tab.
public class CustTabHistory {
    public static void populateHistoryList(Activity activity, String screenName, int screenID, RecyclerView recyclerView, MetrixTabScreenManager.TabChildInfo tci, String maxRows, String searchCriteria) {

        try {
            List<HashMap<String, String>> table = getHistoryListData(screenName, screenID, maxRows, null);
            table = MetrixListScreenManager.performScriptListPopulation(activity, screenID, screenName, table);

            tci.mCount = table.size();

            if (recyclerView != null) {
                final MetadataRecyclerViewAdapter adapter = new MetadataRecyclerViewAdapter(activity, table, R.layout.list_item_basic,
                        R.id.table_layout, R.layout.list_item_table_row, R.color.IFSGold, 0, null, 0,
                        R.id.sliver, null, screenID, "mma_task_history_view.metrix_row_id", null);

                recyclerView.setAdapter(adapter);

                tci.mListAdapter = adapter;
            } else {
                tci.mListAdapter.updateData(table);
            }

        } catch (Exception e) {
            LogManager.getInstance().error(e);
        }
    }

    public static void updateHistoryList(Activity activity, String screenName, int screenID, MetrixTabScreenManager.TabChildInfo tci, String maxRows, String searchCriteria) {

        List<HashMap<String, String>> table = getHistoryListData(screenName, screenID, maxRows, null);
        table = MetrixListScreenManager.applySearchCriteria(table, searchCriteria);
        table = MetrixListScreenManager.performScriptListPopulation(activity, screenID, screenName, table);

        if (tci.mListAdapter != null) {
            tci.mListAdapter.updateData(table);
        }
    }

    private static List<HashMap<String, String>> getHistoryListData(String screenName, int screenID, String maxRows, String searchCriteria) {

        String placeId = MetrixCurrentKeysHelper.getKeyValue("place", "place_id");
        String query = MetrixListScreenManager.generateListQuery("mma_task_history_view",
                "mma_task_history_view.place_id_cust = '" + placeId + "' order by mma_task_history_view.plan_start_dttm desc",
                searchCriteria, screenID);

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

                String plannedStart = row.get("mma_task_history_view.plan_start_dttm");
                String plannedEnd = row.get("mma_task_history_view.plan_end_dttm");

                SimpleDateFormat simpleFormat = new SimpleDateFormat();
                DateFormat dateFormat = MetrixDateTimeHelper.getDateTimeFormat(MetrixDateTimeHelper.DATE_TIME_FORMAT_WITH_SECONDS);
                simpleFormat = (SimpleDateFormat) dateFormat;
                String formatString = simpleFormat.toPattern();

                if (formatString.contains(" ")) {
                    String[] dateFormatPieces = formatString.split(" ");
                    // Only Date and Time without am/pm
                    if (dateFormatPieces.length == 2) {
                        formatString = formatString.replace(":ss", "").replace(".ss", "");
                        simpleFormat.applyLocalizedPattern(formatString);
                    } // Date / Time / AM&PM needs to remove the seconds
                    else if (dateFormatPieces.length == 3) {
                        formatString = formatString.replace(":ss", "").replace(".ss", "");
                        simpleFormat.applyLocalizedPattern(formatString);
                    }
                }

                Date startDate = simpleFormat.parse(plannedStart);
                Date endDate = simpleFormat.parse(plannedEnd);

                String formattedStartDateTime = simpleFormat.format(startDate);
                String formattedEndDateTime = simpleFormat.format(endDate);

                String[] formattedStartPieces = formattedStartDateTime.split(" ");
                String[] formattedEndPieces = formattedEndDateTime.split(" ");

                String formattedStartDate = formattedStartPieces[0];
                String formattedStartTime = "";
                if (formattedStartPieces.length == 3)
                    formattedStartTime = formattedStartPieces[1] + formattedStartPieces[2];
                else
                    formattedStartTime = formattedStartPieces[1];

                String formattedEndTime = "";
                if (formattedEndPieces.length == 3)
                    formattedEndTime = formattedEndPieces[1] + formattedEndPieces[2];
                else
                    formattedEndTime = formattedEndPieces[1];

                String formattedTimeValue = formattedStartTime.toLowerCase() + " - " + formattedEndTime.toLowerCase();

                row.put("custom.formatted_time", String.format("%1$s  %2$s", formattedStartDate, formattedTimeValue));

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
