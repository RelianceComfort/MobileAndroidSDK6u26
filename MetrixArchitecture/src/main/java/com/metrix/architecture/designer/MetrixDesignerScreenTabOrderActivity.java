package com.metrix.architecture.designer;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.metrix.architecture.constants.MetrixTransactionTypes;
import com.metrix.architecture.database.MetrixCursor;
import com.metrix.architecture.database.MetrixDatabaseManager;
import com.metrix.architecture.managers.MetrixUpdateManager;
import com.metrix.architecture.metadata.MetrixSqlData;
import com.metrix.architecture.metadata.MetrixTransaction;
import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.architecture.utilities.DataField;
import com.metrix.architecture.utilities.MetrixActivityHelper;
import com.metrix.architecture.utilities.MetrixCurrentKeysHelper;
import com.metrix.architecture.utilities.MetrixPublicCache;
import com.metrix.architecture.utilities.MetrixStringHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MetrixDesignerScreenTabOrderActivity extends MetrixDesignerActivity {
    private DynamicListView mListView;
    private TextView mTitle;
    private Button mSave, mFinish;
    private String mScreenName;
    private ScreenTabOrderAdapter mScreenTabOrderAdapter;
    private MetrixDesignerResourceData mScreenTabOrderResourceData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mScreenTabOrderResourceData = (MetrixDesignerResourceData) MetrixPublicCache.instance.getItem("MetrixDesignerScreenTabOrderActivityResourceData");
        setContentView(mScreenTabOrderResourceData.LayoutResourceID);
        mListView = (DynamicListView) findViewById(mScreenTabOrderResourceData.ListViewResourceID);
    }

    @Override
    public void onStart() {
        super.onStart();

        helpText = mScreenTabOrderResourceData.HelpTextString;

        mHeadingText = getIntent().getStringExtra("headingText");
        mScreenName = MetrixCurrentKeysHelper.getKeyValue("mm_screen", "screen_name");
        if (mActionBarTitle != null) {
            mActionBarTitle.setText(mHeadingText);
        }

        mTitle = (TextView) findViewById(mScreenTabOrderResourceData.getExtraResourceID("R.id.zzmd_screen_tab_order_title"));
        String fullTitle = AndroidResourceHelper.getMessage("ScreenTabOrder1Args", mScreenName);
        mTitle.setText(fullTitle);

        mListView.setLongClickable(mAllowChanges);

        mSave = (Button) findViewById(mScreenTabOrderResourceData.getExtraResourceID("R.id.save"));
        mSave.setEnabled(mAllowChanges);
        mSave.setOnClickListener(this);

        mFinish = (Button) findViewById(mScreenTabOrderResourceData.getExtraResourceID("R.id.finish"));
        mFinish.setOnClickListener(this);

        TextView mScrInfo = (TextView) findViewById(mScreenTabOrderResourceData.getExtraResourceID("R.id.screen_info_metrix_designer_screen_tab_order"));

        AndroidResourceHelper.setResourceValues(mScrInfo, "ScnInfoMxDesScnTabOrd");
        AndroidResourceHelper.setResourceValues(mSave, "Save");
        AndroidResourceHelper.setResourceValues(mFinish, "Finish");

        populateList();
    }

    private void populateList() {
        StringBuilder query = new StringBuilder();
        query.append("SELECT mm_screen.metrix_row_id, mm_screen.screen_id, mm_screen.tab_order, mm_screen.screen_name from mm_screen");
        query.append(" WHERE mm_screen.tab_parent_id = " + MetrixCurrentKeysHelper.getKeyValue("mm_screen", "screen_id"));
        query.append(" AND mm_screen.tab_order > 0");
        query.append(" ORDER BY mm_screen.tab_order ASC");

        List<HashMap<String, String>> table = new ArrayList<HashMap<String, String>>();
        MetrixCursor cursor = null;
        try {
            cursor = MetrixDatabaseManager.rawQueryMC(query.toString(), null);

            if (cursor == null || !cursor.moveToFirst()) {
                return;
            }

            while (cursor.isAfterLast() == false) {
                HashMap<String, String> row = new HashMap<String, String>();
                row.put("mm_screen.metrix_row_id", cursor.getString(0));
                row.put("mm_screen.screen_id", cursor.getString(1));
                row.put("mm_screen.tab_order", cursor.getString(2));
                row.put("mm_screen.screen_name", cursor.getString(3));
                table.add(row);

                cursor.moveToNext();
            }

            mScreenTabOrderAdapter = new ScreenTabOrderAdapter(this, table, mScreenTabOrderResourceData.ListViewItemResourceID, mScreenTabOrderResourceData.ExtraResourceIDs, mAllowChanges);
            mListView.setListWithEnabling(table, mAllowChanges);
            mListView.setAdapter(mScreenTabOrderAdapter);
            mListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    @Override
    public void onClick(View v) {
        super.onClick(v);

        int viewId = v.getId();
        if (viewId == mScreenTabOrderResourceData.getExtraResourceID("R.id.save")) {
            if (mAllowChanges) {
                processAndSaveChanges();
                Intent intent = MetrixActivityHelper.createActivityIntent(this, MetrixDesignerScreenTabOrderActivity.class);
                intent.putExtra("headingText", mHeadingText);
                MetrixActivityHelper.startNewActivityAndFinish(this, intent);
            }
        } else if (viewId == mScreenTabOrderResourceData.getExtraResourceID("R.id.finish")) {
            if (mAllowChanges) {
                processAndSaveChanges();
            }
            // allow pass through, even if changes aren't allowed (popping this activity off of stack)
            Intent intent = MetrixActivityHelper.createActivityIntent(this, MetrixDesignerScreenPropActivity.class);
            intent.putExtra("headingText", mHeadingText);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            MetrixActivityHelper.startNewActivity(this, intent);
        }
    }

    private void processAndSaveChanges() {
        if (mListView.mListData != null && mListView.mListData.size() > 0) {
            ArrayList<MetrixSqlData> screensToUpdate = new ArrayList<MetrixSqlData>();
            ArrayList<HashMap<String, String>> resortedTabScreenData = new ArrayList<HashMap<String, String>>();
            HashMap<String, String> firstTabScreenItem = new HashMap<String, String>();
            for (HashMap<String, String> row : mListView.mListData) {
                String originalTabOrder = row.get("mm_screen.tab_order");
                if (MetrixStringHelper.valueIsEqual(originalTabOrder, "1"))
                    firstTabScreenItem = row;
                else
                    resortedTabScreenData.add(row);
            }
            // tab child that started first must remain first
            resortedTabScreenData.add(0, firstTabScreenItem);

            int i = 1;
            for (HashMap<String, String> row : resortedTabScreenData) {
                String oldPosition = row.get("mm_screen.tab_order");
                String newPosition = String.valueOf(i);

                if (!oldPosition.equals(newPosition)) {
                    // then we need to do an update
                    String listMetrixRowID = row.get("mm_screen.metrix_row_id");
                    String listScreenID = row.get("mm_screen.screen_id");

                    MetrixSqlData data = new MetrixSqlData("mm_screen", MetrixTransactionTypes.UPDATE, "metrix_row_id="+ listMetrixRowID);
                    data.dataFields.add(new DataField("metrix_row_id", listMetrixRowID));
                    data.dataFields.add(new DataField("screen_id", listScreenID));
                    data.dataFields.add(new DataField("tab_order", newPosition));
                    screensToUpdate.add(data);
                }

                i++;
            }

            if (screensToUpdate.size() > 0) {
                MetrixTransaction transactionInfo = new MetrixTransaction();
                MetrixUpdateManager.update(screensToUpdate, true, transactionInfo, AndroidResourceHelper.getMessage("Screen_Tab_Order"), this);
            }
        }
    }

    public static class ScreenTabOrderAdapter extends DynamicListAdapter {
        static ViewHolder holder;

        public ScreenTabOrderAdapter(Context context, List<HashMap<String, String>> table, int listViewItemResourceID, HashMap<String, Integer> lviElemResIDs, boolean allowChanges) {
            super(context, table, listViewItemResourceID, lviElemResIDs, allowChanges);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View vi = convertView;
            if (convertView == null) {
                vi = mInflater.inflate(mListViewItemResourceID, parent, false);

                holder = new ViewHolder();
                holder.mMetrixRowID = (TextView) vi.findViewById(mListViewItemElementResourceIDs.get("R.id.mm_screen__metrix_row_id"));
                holder.mScreenID = (TextView) vi.findViewById(mListViewItemElementResourceIDs.get("R.id.mm_screen__screen_id"));
                holder.mScreenName = (TextView) vi.findViewById(mListViewItemElementResourceIDs.get("R.id.mm_screen__screen_name"));
                holder.mTabOrder = (TextView) vi.findViewById(mListViewItemElementResourceIDs.get("R.id.mm_screen__tab_order"));

                vi.setTag(holder);
            } else {
                holder = (ViewHolder) vi.getTag();
            }

            HashMap<String, String> dataRow = mListData.get(position);
            holder.mMetrixRowID.setText(dataRow.get("mm_screen.metrix_row_id"));
            holder.mScreenID.setText(dataRow.get("mm_screen.screen_id"));
            holder.mScreenName.setText(dataRow.get("mm_screen.screen_name"));
            holder.mTabOrder.setText(dataRow.get("mm_screen.tab_order"));

            return vi;
        }

        static class ViewHolder {
            TextView mMetrixRowID;
            TextView mScreenID;
            TextView mScreenName;
            TextView mTabOrder;
        }
    }
}

