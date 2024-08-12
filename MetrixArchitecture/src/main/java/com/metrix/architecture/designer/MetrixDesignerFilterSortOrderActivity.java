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

public class MetrixDesignerFilterSortOrderActivity extends MetrixDesignerActivity {
    private DynamicListView mListView;
    private TextView mTitle;
    private Button mFilterSortToggle, mSave, mFinish;
    private String mFilterBtnText, mSortBtnText, mFilterSortCodeValue, mFilterSortCurrentText, mScreenName;
    private FilterSortOrderAdapter mFilterSortOrderAdapter;
    private MetrixDesignerResourceData mFSOResourceData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mFSOResourceData = (MetrixDesignerResourceData)MetrixPublicCache.instance.getItem("MetrixDesignerFilterSortOrderActivityResourceData");

        setContentView(mFSOResourceData.LayoutResourceID);

        mFilterBtnText = MetrixDatabaseManager.getFieldStringValue("select message_text from mm_message_def_view where message_type = 'CODE' and message_id in (select message_id from metrix_code_table where code_name = 'MM_FILTER_SORT_ITEM_TYPE' and code_value = 'FILTER')");
        mSortBtnText = MetrixDatabaseManager.getFieldStringValue("select message_text from mm_message_def_view where message_type = 'CODE' and message_id in (select message_id from metrix_code_table where code_name = 'MM_FILTER_SORT_ITEM_TYPE' and code_value = 'SORT')");
        mFilterSortCodeValue = getIntent().getStringExtra("filterSortCodeValue");
        if (MetrixStringHelper.isNullOrEmpty(mFilterSortCodeValue))
            mFilterSortCodeValue = "FILTER";
        mFilterSortCurrentText = (MetrixStringHelper.valueIsEqual(mFilterSortCodeValue, "SORT")) ? mSortBtnText : mFilterBtnText;

        mListView = (DynamicListView)findViewById(mFSOResourceData.ListViewResourceID);
    }

    @Override
    public void onStart() {
        super.onStart();

        helpText = mFSOResourceData.HelpTextString;

        mHeadingText = getIntent().getStringExtra("headingText");
        mScreenName = MetrixCurrentKeysHelper.getKeyValue("mm_screen", "screen_name");
        if (mActionBarTitle != null) {
            mActionBarTitle.setText(mHeadingText);
        }

        mListView.setLongClickable(mAllowChanges);

        mFilterSortToggle = (Button) findViewById(mFSOResourceData.getExtraResourceID("R.id.filter_sort_toggle"));
        mFilterSortToggle.setOnClickListener(this);
        mFilterSortToggle.setText(mFilterSortCurrentText);

        mSave = (Button) findViewById(mFSOResourceData.getExtraResourceID("R.id.save"));
        mSave.setEnabled(mAllowChanges);
        mSave.setOnClickListener(this);

        mFinish = (Button) findViewById(mFSOResourceData.getExtraResourceID("R.id.finish"));
        mFinish.setOnClickListener(this);

        mTitle = (TextView) findViewById(mFSOResourceData.getExtraResourceID("R.id.filter_sort_order_label"));
        String fullTitle = AndroidResourceHelper.getMessage("FilterSortOrder1Args", mScreenName);
        mTitle.setText(fullTitle);

        TextView mScrInfo = (TextView) findViewById(mFSOResourceData.getExtraResourceID("R.id.filter_sort_order_tip"));
        AndroidResourceHelper.setResourceValues(mScrInfo, "FilterSortOrderTip");
        AndroidResourceHelper.setResourceValues(mSave, "Save");
        AndroidResourceHelper.setResourceValues(mFinish, "Finish");

        populateList();
    }

    private void populateList() {
        StringBuilder query = new StringBuilder();
        query.append("select mm_filter_sort_item.metrix_row_id, mm_filter_sort_item.item_id, mm_filter_sort_item.item_name, mm_filter_sort_item.display_order from mm_filter_sort_item");
        query.append(" where mm_filter_sort_item.screen_id = " + MetrixCurrentKeysHelper.getKeyValue("mm_screen", "screen_id"));
        query.append(" and mm_filter_sort_item.item_type = '" + mFilterSortCodeValue + "'");
        query.append(" and mm_filter_sort_item.display_order > 0");
        query.append(" order by mm_filter_sort_item.display_order asc");

        List<HashMap<String, String>> table = new ArrayList<HashMap<String, String>>();
        MetrixCursor cursor = null;
        try {
            cursor = MetrixDatabaseManager.rawQueryMC(query.toString(), null);

            if (cursor == null || !cursor.moveToFirst()) {
                return;
            }

            while (cursor.isAfterLast() == false) {
                HashMap<String, String> row = new HashMap<String, String>();
                row.put("mm_filter_sort_item.metrix_row_id", cursor.getString(0));
                row.put("mm_filter_sort_item.item_id", cursor.getString(1));
                row.put("mm_filter_sort_item.item_name", cursor.getString(2));
                row.put("mm_filter_sort_item.display_order", cursor.getString(3));
                table.add(row);

                cursor.moveToNext();
            }

            mFilterSortOrderAdapter = new FilterSortOrderAdapter(this, table, mFSOResourceData.ListViewItemResourceID, mFSOResourceData.ExtraResourceIDs, mAllowChanges);
            mListView.setListWithEnabling(table, mAllowChanges);
            mListView.setAdapter(mFilterSortOrderAdapter);
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
        if (viewId == mFSOResourceData.getExtraResourceID("R.id.filter_sort_toggle")) {
            // Save off current state before rebuilding the list, when allowed
            if (mAllowChanges)
                processAndSaveChanges();

            if (MetrixStringHelper.valueIsEqual(mFilterSortCodeValue, "FILTER")) {
                mFilterSortCodeValue = "SORT";
                mFilterSortCurrentText = mSortBtnText;
            } else {
                mFilterSortCodeValue = "FILTER";
                mFilterSortCurrentText = mFilterBtnText;
            }
            mFilterSortToggle.setText(mFilterSortCurrentText);
            populateList();
        } else if (viewId == mFSOResourceData.getExtraResourceID("R.id.save")) {
            if (mAllowChanges) {
                processAndSaveChanges();

                Intent intent = MetrixActivityHelper.createActivityIntent(this, MetrixDesignerFilterSortOrderActivity.class);
                intent.putExtra("headingText", mHeadingText);
                intent.putExtra("filterSortCodeValue", mFilterSortCodeValue);
                MetrixActivityHelper.startNewActivityAndFinish(this, intent);
            }
        } else if (viewId == mFSOResourceData.getExtraResourceID("R.id.finish")) {
            if (mAllowChanges) {
                processAndSaveChanges();
            }
            // allow pass through, even if changes aren't allowed
            Intent intent = MetrixActivityHelper.createActivityIntent(this, MetrixDesignerScreenPropActivity.class);
            intent.putExtra("headingText", mHeadingText);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            MetrixActivityHelper.startNewActivity(this, intent);
        }
    }

    private void processAndSaveChanges() {
        if (mListView.mListData != null && mListView.mListData.size() > 0) {
            ArrayList<MetrixSqlData> itemsToUpdate = new ArrayList<MetrixSqlData>();
            int i = 1;
            for (HashMap<String, String> row : mListView.mListData) {
                String oldPosition = row.get("mm_filter_sort_item.display_order");
                String newPosition = String.valueOf(i);

                if (!oldPosition.equals(newPosition)) {
                    // then we need to do an update
                    String listMetrixRowID = row.get("mm_filter_sort_item.metrix_row_id");
                    String listItemID = row.get("mm_filter_sort_item.item_id");

                    MetrixSqlData data = new MetrixSqlData("mm_filter_sort_item", MetrixTransactionTypes.UPDATE, "metrix_row_id="+ listMetrixRowID);
                    data.dataFields.add(new DataField("metrix_row_id", listMetrixRowID));
                    data.dataFields.add(new DataField("item_id", listItemID));
                    data.dataFields.add(new DataField("display_order", newPosition));
                    itemsToUpdate.add(data);
                }

                i++;
            }

            if (itemsToUpdate.size() > 0) {
                MetrixTransaction transactionInfo = new MetrixTransaction();
                MetrixUpdateManager.update(itemsToUpdate, true, transactionInfo, AndroidResourceHelper.getMessage("FilterSortOrder"), this);
            }
        }
    }

    public static class FilterSortOrderAdapter extends DynamicListAdapter {
        static ViewHolder holder;

        public FilterSortOrderAdapter(Context context, List<HashMap<String, String>> table, int listViewItemResourceID, HashMap<String, Integer> lviElemResIDs, boolean allowChanges) {
            super(context, table, listViewItemResourceID, lviElemResIDs, allowChanges);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View vi = convertView;
            if (convertView == null) {
                vi = mInflater.inflate(mListViewItemResourceID, parent, false);

                holder = new ViewHolder();
                holder.mMetrixRowID = (TextView) vi.findViewById(mListViewItemElementResourceIDs.get("R.id.mm_filter_sort_item__metrix_row_id"));
                holder.mItemID = (TextView) vi.findViewById(mListViewItemElementResourceIDs.get("R.id.mm_filter_sort_item__item_id"));
                holder.mItemName = (TextView) vi.findViewById(mListViewItemElementResourceIDs.get("R.id.mm_filter_sort_item__item_name"));
                holder.mOrder = (TextView) vi.findViewById(mListViewItemElementResourceIDs.get("R.id.mm_filter_sort_item__display_order"));

                vi.setTag(holder);
            } else {
                holder = (ViewHolder) vi.getTag();
            }

            HashMap<String, String> dataRow = mListData.get(position);
            holder.mMetrixRowID.setText(dataRow.get("mm_filter_sort_item.metrix_row_id"));
            holder.mItemID.setText(dataRow.get("mm_filter_sort_item.item_id"));
            holder.mItemName.setText(dataRow.get("mm_filter_sort_item.item_name"));
            holder.mOrder.setText(dataRow.get("mm_filter_sort_item.display_order"));

            return vi;
        }

        static class ViewHolder {
            TextView mMetrixRowID;
            TextView mItemID;
            TextView mItemName;
            TextView mOrder;
        }
    }
}